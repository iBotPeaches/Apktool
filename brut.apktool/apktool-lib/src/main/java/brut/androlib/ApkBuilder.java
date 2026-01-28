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
import brut.androlib.res.AaptInvoker;
import brut.androlib.res.AaptManager;
import brut.androlib.res.data.ResChunkHeader;
import brut.androlib.res.table.ResConfig;
import brut.androlib.res.xml.ResXmlUtils;
import brut.androlib.smali.SmaliBuilder;
import brut.common.BrutException;
import brut.directory.Directory;
import brut.directory.DirectoryException;
import brut.directory.ExtFile;
import brut.directory.FileDirectory;
import brut.directory.ZipRODirectory;
import brut.util.BackgroundWorker;
import brut.util.BinaryDataInputStream;
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
    private SmaliBuilder mSmaliBuilder;
    private AaptInvoker mAaptInvoker;
    private BackgroundWorker mWorker;

    public ApkBuilder(File apkDir, Config config) {
        mApkDir = new ExtFile(apkDir);
        mConfig = config;
        mFirstError = new AtomicReference<>();
    }

    public void build(File outApk) throws AndrolibException {
        if (mConfig.getJobs() > 1) {
            mWorker = new BackgroundWorker(mConfig.getJobs() - 1);
        }
        try {
            mApkInfo = ApkInfo.load(mApkDir);
            String minSdkVersion = mApkInfo.getSdkInfo().getMinSdkVersion();
            mSmaliBuilder = new SmaliBuilder(minSdkVersion != null
                ? SdkInfo.parseSdkInt(minSdkVersion) : 0);
            mAaptInvoker = new AaptInvoker(mApkInfo, mConfig);

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

            LOGGER.info("Using Apktool " + mConfig.getVersion() + " on " + apkName
                    + (mWorker != null ? " with " + mConfig.getJobs() + " threads" : ""));

            buildSources(outDir);
            buildResources(outDir);

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
                    fileName = dirName.substring(dirName.indexOf('_') + 1)
                        .replace('@', File.separatorChar) + ".dex";
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
        File outFile = new File(outDir, fileName);

        if (!mConfig.isForced() && !isFileNewer(inFile, outFile)) {
            LOGGER.info(fileName + " has not changed.");
            return;
        }

        LOGGER.info("Copying raw " + fileName + "...");
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
        File dexFile = new File(outDir, fileName);

        if (!mConfig.isForced() && !isFileNewer(smaliDir, dexFile)) {
            LOGGER.info(dirName + " has not changed.");
            return;
        }

        LOGGER.info("Smaling " + dirName + " folder into " + fileName + "...");
        mSmaliBuilder.build(smaliDir, dexFile);
    }

    private void buildResources(File outDir) throws AndrolibException {
        File manifest = new File(mApkDir, "AndroidManifest.xml");
        if (!manifest.isFile()) {
            return;
        }

        // Check if manifest is binary XML.
        boolean isBinaryManifest;
        try (BinaryDataInputStream in = new BinaryDataInputStream(Files.newInputStream(manifest.toPath()))) {
            isBinaryManifest = ResChunkHeader.read(in).type == ResChunkHeader.RES_XML_TYPE;
        } catch (IOException ex) {
            throw new AndrolibException(ex);
        }

        // Copy raw manifest if it's binary XML.
        if (isBinaryManifest) {
            copyManifestRaw(outDir, manifest);
        }

        // Copy raw resources if possible.
        File arscFile = new File(mApkDir, "resources.arsc");
        if (arscFile.isFile()) {
            copyResourcesRaw(outDir, arscFile);
            return;
        }

        // We cannot build if manifest is binary XML.
        if (isBinaryManifest) {
            return;
        }

        // Build only manifest if no resources.
        File resDir = new File(mApkDir, "res");
        if (!resDir.isDirectory()) {
            buildManifestOnly(outDir, manifest);
            return;
        }

        // Build manifest and resources.
        buildResourcesFully(outDir, manifest, resDir);
    }

    private void copyManifestRaw(File outDir, File manifest) throws AndrolibException {
        if (!mConfig.isForced()
                && !isFileNewer(manifest, new File(outDir, "AndroidManifest.xml"))) {
            LOGGER.info("AndroidManifest.xml has not changed.");
            return;
        }

        LOGGER.info("Copying raw AndroidManifest.xml...");
        try {
            Directory in = mApkDir.getDirectory();

            in.copyToDir(outDir, "AndroidManifest.xml");
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    private void copyResourcesRaw(File outDir, File arscFile) throws AndrolibException {
        if (!mConfig.isForced()
                && !isFileNewer(arscFile, new File(outDir, "resources.arsc"))) {
            LOGGER.info("resources.arsc has not changed.");
            return;
        }

        LOGGER.info("Copying raw resources.arsc...");
        try {
            Directory in = mApkDir.getDirectory();

            in.copyToDir(outDir, "resources.arsc");
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    private void buildManifestOnly(File outDir, File manifest) throws AndrolibException {
        if (!mConfig.isForced()
                && !isFileNewer(manifest, new File(outDir, "AndroidManifest.xml"))) {
            LOGGER.info("AndroidManifest.xml has not changed.");
            return;
        }

        // Back up manifest for editing.
        File manifestOrig = new File(manifest.getPath() + ".orig");
        try {
            OS.cpfile(manifest, manifestOrig);
        } catch (BrutException ex) {
            throw new AndrolibException(ex);
        }

        ResXmlUtils.fixingPublicAttrsInProviderAttributes(manifest);

        if (mConfig.isDebuggable()) {
            LOGGER.info("Setting 'debuggable' attribute to 'true' in AndroidManifest.xml...");
            ResXmlUtils.setApplicationDebugTagTrue(manifest);
        }

        File tmpFile;
        try {
            tmpFile = File.createTempFile("APKTOOL", null);
            OS.rmfile(tmpFile);
        } catch (IOException ex) {
            throw new AndrolibException(ex);
        }

        LOGGER.info("Building AndroidManifest.xml with " + AaptManager.getBinaryName() + "...");
        mAaptInvoker.invoke(tmpFile, manifest, null);

        try (ZipRODirectory tmpDir = new ZipRODirectory(tmpFile)) {
            tmpDir.copyToDir(outDir, "AndroidManifest.xml");
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        } finally {
            OS.rmfile(tmpFile);
        }

        // Restore original manifest.
        try {
            OS.mvfile(manifestOrig, manifest);
        } catch (BrutException ex) {
            throw new AndrolibException(ex);
        }
    }

    private void buildResourcesFully(File outDir, File manifest, File resDir) throws AndrolibException {
        if (!mConfig.isForced()
                && !isFileNewer(manifest, new File(outDir, "AndroidManifest.xml"))
                && !isFileNewer(resDir, new File(outDir, "res"))) {
            LOGGER.info("AndroidManifest.xml and resources have not changed.");
            return;
        }

        // Back up manifest for editing.
        File manifestOrig = new File(manifest.getPath() + ".orig");
        try {
            OS.cpfile(manifest, manifestOrig);
        } catch (BrutException ex) {
            throw new AndrolibException(ex);
        }

        ResXmlUtils.fixingPublicAttrsInProviderAttributes(manifest);

        if (mConfig.isDebuggable()) {
            LOGGER.info("Setting 'debuggable' attribute to 'true' in AndroidManifest.xml...");
            ResXmlUtils.setApplicationDebugTagTrue(manifest);
        }

        if (mConfig.isNetSecConf()) {
            LOGGER.info("Adding permissive network security config in manifest...");
            File netSecConfOrig = new File(mApkDir, "res/xml/network_security_config.xml");
            OS.mkdir(netSecConfOrig.getParentFile());
            ResXmlUtils.modNetworkSecurityConfig(netSecConfOrig);
            ResXmlUtils.setNetworkSecurityConfig(manifest);

            String targetSdkVersion = mApkInfo.getSdkInfo().getTargetSdkVersion();
            if (targetSdkVersion != null && SdkInfo.parseSdkInt(targetSdkVersion) < ResConfig.SDK_NOUGAT) {
                LOGGER.warning("Target SDK version is lower than 24, Network Security Configuration might be ignored!");
            }
        }

        File tmpFile;
        try {
            tmpFile = File.createTempFile("APKTOOL", null);
            OS.rmfile(tmpFile);
        } catch (IOException ex) {
            throw new AndrolibException(ex);
        }

        LOGGER.info("Building resources with " + AaptManager.getBinaryName() + "...");
        mAaptInvoker.invoke(tmpFile, manifest, resDir);

        try (ZipRODirectory tmpDir = new ZipRODirectory(tmpFile)) {
            tmpDir.copyToDir(outDir, "AndroidManifest.xml", "resources.arsc", "res");
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        } finally {
            OS.rmfile(tmpFile);
        }

        // Restore original manifest.
        try {
            OS.mvfile(manifestOrig, manifest);
        } catch (BrutException ex) {
            throw new AndrolibException(ex);
        }
    }

    private void copyOriginalFiles(File outDir) throws AndrolibException {
        if (!mConfig.isCopyOriginal()) {
            return;
        }

        File originalDir = new File(mApkDir, "original");
        if (!originalDir.isDirectory()) {
            return;
        }

        LOGGER.info("Copying original files...");
        try {
            FileDirectory in = new FileDirectory(originalDir);

            for (String fileName : in.getFiles(true)) {
                if (ApkInfo.ORIGINAL_FILES_PATTERN.matcher(fileName).matches()) {
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
            for (String dirName : ApkInfo.RAW_DIRS) {
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

    private boolean isFileNewer(File file, File reference) {
        return !reference.exists() || BrutIO.recursiveModifiedTime(file) > BrutIO.recursiveModifiedTime(reference);
    }
}
