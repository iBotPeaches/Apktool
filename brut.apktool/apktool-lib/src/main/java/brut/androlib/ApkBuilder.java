/*
 *  Copyright (C) 2010 Ryszard Wiśniewski <brut.alll@gmail.com>
 *  Copyright (C) 2010 Connor Tumbleson <connor.tumbleson@gmail.com>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package brut.androlib;

import brut.androlib.exceptions.AndrolibException;
import brut.androlib.meta.ApkInfo;
import brut.androlib.meta.SdkInfo;
import brut.androlib.meta.UsesFramework;
import brut.androlib.res.AaptInvoker;
import brut.androlib.res.AaptManager;
import brut.androlib.res.Framework;
import brut.androlib.res.table.ResConfig;
import brut.androlib.res.xml.ResXmlUtils;
import brut.androlib.smali.SmaliBuilder;
import brut.common.BrutException;
import brut.directory.Directory;
import brut.directory.DirectoryException;
import brut.directory.ExtFile;
import brut.util.BackgroundWorker;
import brut.util.BrutIO;
import brut.util.OS;
import brut.util.ZipUtils;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import java.util.zip.ZipOutputStream;

public class ApkBuilder {
    private static final Logger LOGGER = Logger.getLogger(ApkBuilder.class.getName());

    private final ExtFile mApkDir;
    private final Config mConfig;
    private final AtomicReference<AndrolibException> mFirstError;

    private ApkInfo mApkInfo;
    private int mMinSdkVersion;
    private BackgroundWorker mWorker;

    public ApkBuilder(ExtFile apkDir, Config config) {
        mApkDir = apkDir;
        mConfig = config;
        mFirstError = new AtomicReference<>(null);
    }

    public void build(File outApk) throws AndrolibException {
        if (mConfig.getJobs() > 1) {
            mWorker = new BackgroundWorker(mConfig.getJobs() - 1);
        }
        try {
            mApkInfo = ApkInfo.load(mApkDir);

            String minSdkVersion = mApkInfo.getSdkInfo().getMinSdkVersion();
            if (minSdkVersion != null) {
                mMinSdkVersion = SdkInfo.parseSdkInt(minSdkVersion);
            }

            String apkName = mApkInfo.getApkFileName();
            if (apkName == null) {
                apkName = "out.apk";
            }
            if (mConfig.isNoApk()) {
                outApk = null;
            } else if (outApk == null) {
                outApk = new File(mApkDir, "dist/" + apkName);
            }

            File outDir = new File(mApkDir, "build/apk");
            OS.mkdir(outDir);

            File manifest = new File(mApkDir, "AndroidManifest.xml");
            File manifestOrig = new File(mApkDir, "AndroidManifest.xml.orig");

            LOGGER.info("Using Apktool " + mConfig.getVersion() + " on " + apkName
                    + (mWorker != null ? " with " + mConfig.getJobs() + " threads" : ""));

            buildSources(outDir);
            backupManifestFile(manifest, manifestOrig);
            buildResources(outDir, manifest);

            if (mWorker != null) {
                mWorker.waitForFinish();
                if (mFirstError.get() != null) {
                    throw mFirstError.get();
                }
            }

            copyOriginalFiles(outDir);
            if (outApk != null) {
                buildApkFile(outDir, outApk);
            }

            // We copied AndroidManifest.xml to AndroidManifest.xml.orig for editing.
            // Rename the AndroidManifest.xml.orig back to AndroidManifest.xml.
            if (manifestOrig.isFile()) {
                try {
                    OS.mvfile(manifestOrig, manifest);
                } catch (BrutException ex) {
                    throw new AndrolibException(ex);
                }
            }
        } finally {
            if (mWorker != null) {
                mWorker.shutdownNow();
            }
        }
    }

    private void buildSources(File outDir) throws AndrolibException {
        try {
            Directory in = mApkDir.getDirectory();

            // Copy raw dex files.
            Set<String> dexFiles = new HashSet<>();
            for (String fileName : in.getFiles()) {
                if (fileName.endsWith(".dex")) {
                    copySourcesRaw(outDir, fileName);
                    dexFiles.add(fileName);
                }
            }

            // Build smali dirs.
            for (String dirName : in.getDirs().keySet()) {
                String fileName;
                if (dirName.equals("smali")) {
                    fileName = "classes.dex";
                } else if (dirName.startsWith("smali_")) {
                    fileName = dirName.substring(dirName.indexOf('_') + 1) + ".dex";
                } else {
                    continue;
                }

                if (!dexFiles.contains(fileName)) {
                    buildSourcesSmali(outDir, dirName, fileName);
                }
            }
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    private void copySourcesRaw(File outDir, String fileName) throws AndrolibException {
        File inFile = new File(mApkDir, fileName);
        if (!inFile.isFile()) {
            return;
        }

        File outFile = new File(outDir, fileName);
        if (!mConfig.isForced() && !isFileNewer(inFile, outFile)) {
            LOGGER.info("File " + fileName + " has not changed.");
            return;
        }

        LOGGER.info("Copying raw " + fileName + " file...");
        try {
            BrutIO.copyAndClose(Files.newInputStream(inFile.toPath()), Files.newOutputStream(outFile.toPath()));
        } catch (IOException ex) {
            throw new AndrolibException(ex);
        }
    }

    private void buildSourcesSmali(File outDir, String dirName, String fileName) throws AndrolibException {
        if (mWorker != null) {
            mWorker.submit(() -> {
                if (mFirstError.get() == null) {
                    try {
                        buildSourcesSmaliJob(outDir, dirName, fileName);
                    } catch (AndrolibException ex) {
                        mFirstError.compareAndSet(null, ex);
                    }
                }
            });
        } else {
            buildSourcesSmaliJob(outDir, dirName, fileName);
        }
    }

    private void buildSourcesSmaliJob(File outDir, String dirName, String fileName) throws AndrolibException {
        File smaliDir = new File(mApkDir, dirName);
        if (!smaliDir.isDirectory()) {
            return;
        }

        File dexFile = new File(outDir, fileName);
        if (!mConfig.isForced() && !isFileNewer(smaliDir, dexFile)) {
            LOGGER.info("Sources in " + dirName + " have not changed.");
            return;
        }
        OS.rmfile(dexFile);

        LOGGER.info("Smaling " + dirName + " folder into " + fileName + "...");
        SmaliBuilder builder = new SmaliBuilder(smaliDir, mMinSdkVersion);
        builder.build(dexFile);
    }

    private void backupManifestFile(File manifest, File manifestOrig) throws AndrolibException {
        // We cannot patch AndroidManifest.xml if it was not decoded.
        if (new File(mApkDir, "resources.arsc").isFile()) {
            return;
        }

        if (!manifest.isFile()) {
            return;
        }

        OS.rmfile(manifestOrig);

        try {
            OS.cpfile(manifest, manifestOrig);
            ResXmlUtils.fixingPublicAttrsInProviderAttributes(manifest);
        } catch (BrutException ex) {
            throw new AndrolibException(ex);
        }
    }

    private void buildResources(File outDir, File manifest) throws AndrolibException {
        if (!manifest.isFile()) {
            return;
        }

        // Copy raw resources.
        File arscFile = new File(mApkDir, "resources.arsc");
        if (arscFile.isFile()) {
            copyResourcesRaw(outDir, manifest, arscFile);
            return;
        }

        // Build resources.
        File resDir = new File(mApkDir, "res");
        if (resDir.isDirectory()) {
            buildResourcesFull(outDir, manifest, resDir);
            return;
        }

        // Build manifest only.
        LOGGER.fine("Could not find resources.");
        buildManifest(outDir, manifest);
    }

    private void copyResourcesRaw(File outDir, File manifest, File arscFile) throws AndrolibException {
        if (!mConfig.isForced()
                && !isFileNewer(manifest, new File(outDir, "AndroidManifest.xml"))
                && !isFileNewer(arscFile, new File(outDir, "resources.arsc"))) {
            LOGGER.info("Resources have not changed.");
            return;
        }

        LOGGER.info("Copying raw resources...");
        try {
            Directory in = mApkDir.getDirectory();

            in.copyToDir(outDir, "AndroidManifest.xml", "resources.arsc");
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    private void buildResourcesFull(File outDir, File manifest, File resDir) throws AndrolibException {
        File resZip = new File(outDir.getParentFile(), "resources.zip");
        if (!mConfig.isForced() && resZip.isFile()
                && !isFileNewer(manifest, new File(outDir, "AndroidManifest.xml"))
                && !isFileNewer(resDir, new File(outDir, "res"))) {
            LOGGER.info("Resources have not changed.");
            return;
        }
        OS.rmfile(resZip);

        if (mConfig.isDebuggable()) {
            LOGGER.info("Setting 'debuggable' attribute to 'true' in AndroidManifest.xml");
            ResXmlUtils.setApplicationDebugTagTrue(manifest);
        }

        if (mConfig.isNetSecConf()) {
            String targetSdkVersion = mApkInfo.getSdkInfo().getTargetSdkVersion();
            if (targetSdkVersion != null && SdkInfo.parseSdkInt(targetSdkVersion) < ResConfig.SDK_NOUGAT) {
                LOGGER.warning("Target SDK version is lower than 24! Network Security Configuration might be ignored!");
            }

            File netSecConfOrig = new File(mApkDir, "res/xml/network_security_config.xml");
            OS.mkdir(netSecConfOrig.getParentFile());
            ResXmlUtils.modNetworkSecurityConfig(netSecConfOrig);
            ResXmlUtils.setNetworkSecurityConfig(manifest);
            LOGGER.info("Added permissive network security config in manifest");
        }

        ExtFile tmpFile;
        try {
            tmpFile = new ExtFile(File.createTempFile("APKTOOL", null));
        } catch (IOException ex) {
            throw new AndrolibException(ex);
        }
        OS.rmfile(tmpFile);

        File npDir = new File(mApkDir, "9patch");
        if (!npDir.isDirectory()) {
            npDir = null;
        }

        LOGGER.info("Building resources with " + AaptManager.getBinaryName() + "...");
        try {
            AaptInvoker invoker = new AaptInvoker(mApkInfo, mConfig);
            invoker.invoke(tmpFile, manifest, resDir, npDir, null, getIncludeFiles());

            Directory tmpDir = tmpFile.getDirectory();
            tmpDir.copyToDir(outDir, "AndroidManifest.xml", "resources.arsc", "res");
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        } finally {
            OS.rmfile(tmpFile);
        }
    }

    private void buildManifest(File outDir, File manifest) throws AndrolibException {
        if (!mConfig.isForced()
                && !isFileNewer(manifest, new File(outDir, "AndroidManifest.xml"))) {
            LOGGER.info("AndroidManifest.xml has not changed.");
            return;
        }

        ExtFile tmpFile;
        try {
            tmpFile = new ExtFile(File.createTempFile("APKTOOL", null));
        } catch (IOException ex) {
            throw new AndrolibException(ex);
        }
        OS.rmfile(tmpFile);

        File npDir = new File(mApkDir, "9patch");
        if (!npDir.isDirectory()) {
            npDir = null;
        }

        LOGGER.info("Building AndroidManifest.xml with " + AaptManager.getBinaryName() + "...");
        try {
            AaptInvoker invoker = new AaptInvoker(mApkInfo, mConfig);
            invoker.invoke(tmpFile, manifest, null, npDir, null, getIncludeFiles());

            Directory tmpDir = tmpFile.getDirectory();
            tmpDir.copyToDir(outDir, "AndroidManifest.xml");
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        } catch (AndrolibException ignored) {
            LOGGER.warning("Parse AndroidManifest.xml failed, treat it as raw file.");
            copyManifestRaw(outDir);
        } finally {
            OS.rmfile(tmpFile);
        }
    }

    private void copyManifestRaw(File outDir) throws AndrolibException {
        LOGGER.info("Copying raw manifest...");
        try {
            Directory in = mApkDir.getDirectory();

            in.copyToDir(outDir, "AndroidManifest.xml");
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    private void copyOriginalFiles(File outDir) throws AndrolibException {
        if (!mConfig.isCopyOriginal()) {
            return;
        }

        ExtFile originalDir = new ExtFile(mApkDir, "original");
        if (!originalDir.isDirectory()) {
            return;
        }

        LOGGER.info("Copying original files...");
        try {
            Directory in = originalDir.getDirectory();

            for (String fileName : in.getFiles(true)) {
                if (ApkInfo.ORIGINAL_FILENAMES_PATTERN.matcher(fileName).matches()) {
                    in.copyToDir(outDir, fileName);
                }
            }
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    private void buildApkFile(File outDir, File outApk) throws AndrolibException {
        if (outApk.exists()) {
            OS.rmfile(outApk);
        } else {
            File parentDir = outApk.getParentFile();
            if (parentDir != null) {
                OS.mkdir(parentDir);
            }
        }

        // Convert to set for fast lookup.
        Set<String> doNotCompress = new HashSet<>(mApkInfo.getDoNotCompress());

        LOGGER.info("Building apk file...");
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(outApk.toPath()))) {
            // Zip aapt output files.
            ZipUtils.zipDir(outDir, out, doNotCompress);

            // Zip standard raw files.
            for (String dirName : ApkInfo.RAW_DIRNAMES) {
                File rawDir = new File(mApkDir, dirName);
                if (rawDir.isDirectory()) {
                    LOGGER.info("Importing " + dirName + "...");
                    ZipUtils.zipDir(mApkDir, dirName, out, doNotCompress);
                }
            }

            // Zip unknown files.
            File unknownDir = new File(mApkDir, "unknown");
            if (unknownDir.isDirectory()) {
                LOGGER.info("Importing unknown files...");
                ZipUtils.zipDir(unknownDir, out, doNotCompress);
            }
        } catch (IOException ex) {
            throw new AndrolibException(ex);
        }
        LOGGER.info("Built apk into: " + outApk.getPath());
    }

    private File[] getIncludeFiles() throws AndrolibException {
        List<File> files = new ArrayList<>();

        UsesFramework usesFramework = mApkInfo.getUsesFramework();
        List<Integer> frameworkIds = usesFramework.getIds();
        if (!frameworkIds.isEmpty()) {
            Framework framework = new Framework(mConfig);
            String tag = usesFramework.getTag();
            for (Integer id : frameworkIds) {
                files.add(framework.getApkFile(id, tag));
            }
        }

        List<String> usesLibrary = mApkInfo.getUsesLibrary();
        if (!usesLibrary.isEmpty()) {
            String[] libFiles = mConfig.getLibraryFiles();
            for (String name : usesLibrary) {
                File apkFile = null;
                if (libFiles != null) {
                    for (String libEntry : libFiles) {
                        String[] parts = libEntry.split(":", 2);
                        if (parts.length == 2 && name.equals(parts[0])) {
                            apkFile = new File(parts[1]);
                            break;
                        }
                    }
                }
                if (apkFile != null) {
                    files.add(apkFile);
                } else {
                    LOGGER.warning("Shared library was not provided: " + name);
                }
            }
        }

        return files.toArray(new File[0]);
    }

    private boolean isFileNewer(File file, File reference) {
        return !reference.exists() || BrutIO.recursiveModifiedTime(file) > BrutIO.recursiveModifiedTime(reference);
    }
}
