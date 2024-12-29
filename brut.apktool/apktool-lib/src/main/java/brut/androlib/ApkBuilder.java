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
import brut.androlib.apk.ApkInfo;
import brut.androlib.apk.UsesFramework;
import brut.androlib.res.Framework;
import brut.androlib.res.data.ResConfigFlags;
import brut.androlib.res.xml.ResXmlUtils;
import brut.androlib.src.SmaliBuilder;
import brut.common.BrutException;
import brut.common.InvalidUnknownFileException;
import brut.common.RootUnknownFileException;
import brut.common.TraversalUnknownFileException;
import brut.directory.Directory;
import brut.directory.DirectoryException;
import brut.directory.ExtFile;
import brut.directory.ZipUtils;
import brut.util.AaptManager;
import brut.util.BrutIO;
import brut.util.OS;

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
    private final AtomicReference<AndrolibException> mBuildError;

    private ApkInfo mApkInfo;
    private int mMinSdkVersion;
    private BackgroundWorker mWorker;

    public ApkBuilder(ExtFile apkDir, Config config) {
        mApkDir = apkDir;
        mConfig = config;
        mBuildError = new AtomicReference<>(null);
    }

    public void build(File outApk) throws AndrolibException {
        if (mConfig.getJobs() > 1) {
            mWorker = new BackgroundWorker(mConfig.getJobs() - 1);
        }
        try {
            mApkInfo = ApkInfo.load(mApkDir);

            String minSdkVersion = mApkInfo.getMinSdkVersion();
            if (minSdkVersion != null) {
                mMinSdkVersion = mApkInfo.getMinSdkVersionFromAndroidCodename(minSdkVersion);
            }

            if (outApk == null) {
                String outFileName = mApkInfo.apkFileName;
                if (outFileName == null) {
                    outFileName = "out.apk";
                }
                outApk = new File(mApkDir, "dist/" + outFileName);
            }

            File outDir = new File(mApkDir, "build/apk");
            OS.mkdir(outDir);

            File manifest = new File(mApkDir, "AndroidManifest.xml");
            File manifestOrig = new File(mApkDir, "AndroidManifest.xml.orig");

            LOGGER.info("Using Apktool " + ApktoolProperties.getVersion() + " on " + outApk.getName()
                        + (mWorker != null ? " with " + mConfig.getJobs() + " threads" : ""));

            buildSources(outDir);
            backupManifestFile(manifest, manifestOrig);
            buildResources(outDir, manifest);

            if (mWorker != null) {
                mWorker.waitForFinish();
                if (mBuildError.get() != null) {
                    throw mBuildError.get();
                }
            }

            if (!mConfig.isNoApk()) {
                if (outApk.exists()) {
                    OS.rmfile(outApk);
                } else {
                    File parentDir = outApk.getParentFile();
                    if (parentDir != null) {
                        OS.mkdir(parentDir);
                    }
                }

                copyOriginalFiles(outDir);

                LOGGER.info("Building apk file...");

                try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(outApk.toPath()))) {
                    // zip aapt output files
                    try {
                        ZipUtils.zipDir(outDir, out, mApkInfo.doNotCompress);
                    } catch (IOException ex) {
                        throw new AndrolibException(ex);
                    }

                    // zip remaining standard files
                    importRawFiles(out);

                    // zip unknown files
                    importUnknownFiles(out);
                } catch (IOException ex) {
                    throw new AndrolibException(ex);
                }

                LOGGER.info("Built apk into: " + outApk.getPath());
            }

            // we copied the AndroidManifest.xml to AndroidManifest.xml.orig so we can edit it
            // lets restore the unedited one, to not change the original
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
        if (!copySourcesRaw(outDir, "classes.dex")) {
            buildSourcesSmali(outDir, "smali", "classes.dex");
        }

        try {
            Directory in = mApkDir.getDirectory();

            // loop through any smali_ directories for multi-dex apks
            for (String dirName : in.getDirs().keySet()) {
                if (dirName.startsWith("smali_")) {
                    String fileName = dirName.substring(dirName.indexOf("_") + 1) + ".dex";
                    if (!copySourcesRaw(outDir, fileName)) {
                        buildSourcesSmali(outDir, dirName, fileName);
                    }
                }
            }

            // loop through any classes#.dex files for multi-dex apks
            for (String fileName : in.getFiles()) {
                // skip classes.dex because we have handled it
                if (fileName.endsWith(".dex") && !fileName.equals("classes.dex")) {
                    copySourcesRaw(outDir, fileName);
                }
            }
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    private boolean copySourcesRaw(File outDir, String fileName) throws AndrolibException {
        File working = new File(mApkDir, fileName);
        if (!working.isFile()) {
            return false;
        }

        File stored = new File(outDir, fileName);
        if (!mConfig.isForceBuildAll() && !isModified(working, stored)) {
            return true;
        }

        LOGGER.info("Copying raw " + fileName + " file...");
        try {
            BrutIO.copyAndClose(Files.newInputStream(working.toPath()), Files.newOutputStream(stored.toPath()));
        } catch (IOException ex) {
            throw new AndrolibException(ex);
        }
        return true;
    }

    private void buildSourcesSmali(File outDir, String dirName, String fileName) throws AndrolibException {
        if (mWorker != null) {
            mWorker.submit(() -> {
                if (mBuildError.get() == null) {
                    try {
                        buildSourcesSmaliJob(outDir, dirName, fileName);
                    } catch (AndrolibException ex) {
                        mBuildError.compareAndSet(null, ex);
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

        File dex = new File(outDir, fileName);
        if (!mConfig.isForceBuildAll()) {
            LOGGER.info("Checking whether sources have changed...");
            if (!isModified(smaliDir, dex)) {
                return;
            }
        }
        OS.rmfile(dex);

        LOGGER.info("Smaling " + dirName + " folder into " + fileName + "...");
        int apiLevel = mConfig.getApiLevel() > 0 ? mConfig.getApiLevel() : mMinSdkVersion;
        SmaliBuilder builder = new SmaliBuilder(smaliDir, apiLevel);
        builder.build(dex);
    }

    private void backupManifestFile(File manifest, File manifestOrig) throws AndrolibException {
        // if we decoded in "raw", we cannot patch AndroidManifest
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
            LOGGER.fine("Could not find AndroidManifest.xml");
            return;
        }

        if (new File(mApkDir, "resources.arsc").isFile()) {
            copyResourcesRaw(outDir, manifest);
        } else if (new File(mApkDir, "res").isDirectory()) {
            buildResourcesFull(outDir, manifest);
        } else {
            LOGGER.fine("Could not find resources");
            buildManifest(outDir, manifest);
        }
    }

    private void copyResourcesRaw(File outDir, File manifest) throws AndrolibException {
        if (!mConfig.isForceBuildAll()) {
            LOGGER.info("Checking whether resources have changed...");
            if (!isModified(manifest, new File(outDir, "AndroidManifest.xml"))
                    && !isModified(new File(mApkDir, "resources.arsc"), new File(outDir, "resources.arsc"))
                    && !isModified(newFiles(mApkDir, ApkInfo.RESOURCES_DIRNAMES),
                        newFiles(outDir, ApkInfo.RESOURCES_DIRNAMES))) {
                return;
            }
        }

        LOGGER.info("Copying raw resources...");
        try {
            Directory in = mApkDir.getDirectory();

            in.copyToDir(outDir, "AndroidManifest.xml");
            in.copyToDir(outDir, "resources.arsc");
            in.copyToDir(outDir, ApkInfo.RESOURCES_DIRNAMES);
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    private void buildResourcesFull(File outDir, File manifest) throws AndrolibException {
        File resourcesFile = new File(outDir.getParentFile(), "resources.zip");
        if (!mConfig.isForceBuildAll()) {
            LOGGER.info("Checking whether resources have changed...");
            if (!isModified(manifest, new File(outDir, "AndroidManifest.xml"))
                    && !isModified(newFiles(mApkDir, ApkInfo.RESOURCES_DIRNAMES),
                        newFiles(outDir, ApkInfo.RESOURCES_DIRNAMES))
                    && (mConfig.getAaptVersion() == 1 || resourcesFile.isFile())) {
                return;
            }
        }
        OS.rmfile(resourcesFile);

        if (mConfig.isDebugMode()) {
            if (mConfig.getAaptVersion() == 2) {
                LOGGER.info("Setting 'debuggable' attribute to 'true' in AndroidManifest.xml");
                ResXmlUtils.setApplicationDebugTagTrue(manifest);
            } else {
                ResXmlUtils.removeApplicationDebugTag(manifest);
            }
        }

        if (mConfig.isNetSecConf()) {
            String targetSdkVersion = mApkInfo.getTargetSdkVersion();
            if (targetSdkVersion != null) {
                if (Integer.parseInt(targetSdkVersion) < ResConfigFlags.SDK_NOUGAT) {
                    LOGGER.warning("Target SDK version is lower than 24! Network Security Configuration might be ignored!");
                }
            }

            File netSecConfOrig = new File(mApkDir, "res/xml/network_security_config.xml");
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

        File resDir = new File(mApkDir, "res");
        File ninePatch = new File(mApkDir, "9patch");
        if (!ninePatch.isDirectory()) {
            ninePatch = null;
        }

        LOGGER.info("Building resources with " + AaptManager.getAaptName(mConfig.getAaptVersion()) + "...");

        try {
            AaptInvoker invoker = new AaptInvoker(mApkInfo, mConfig);
            invoker.invoke(tmpFile, manifest, resDir, ninePatch, null, getIncludeFiles());

            Directory tmpDir = tmpFile.getDirectory();
            tmpDir.copyToDir(outDir, "AndroidManifest.xml");
            tmpDir.copyToDir(outDir, "resources.arsc");
            tmpDir.copyToDir(outDir, ApkInfo.RESOURCES_DIRNAMES);
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        } finally {
            OS.rmfile(tmpFile);
        }
    }

    private void buildManifest(File outDir, File manifest) throws AndrolibException {
        if (!mConfig.isForceBuildAll()) {
            LOGGER.info("Checking whether AndroidManifest.xml has changed...");
            if (!isModified(manifest, new File(outDir, "AndroidManifest.xml"))) {
                return;
            }
        }

        ExtFile tmpFile;
        try {
            tmpFile = new ExtFile(File.createTempFile("APKTOOL", null));
        } catch (IOException ex) {
            throw new AndrolibException(ex);
        }
        OS.rmfile(tmpFile);

        File ninePatch = new File(mApkDir, "9patch");
        if (!ninePatch.isDirectory()) {
            ninePatch = null;
        }

        LOGGER.info("Building AndroidManifest.xml with " + AaptManager.getAaptName(mConfig.getAaptVersion()) + "...");

        try {
            AaptInvoker invoker = new AaptInvoker(mApkInfo, mConfig);
            invoker.invoke(tmpFile, manifest, null, ninePatch, null, getIncludeFiles());

            Directory tmpDir = tmpFile.getDirectory();
            tmpDir.copyToDir(outDir, "AndroidManifest.xml");
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        } catch (AndrolibException ex) {
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
        if (!mConfig.isCopyOriginalFiles()) {
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

    private void importRawFiles(ZipOutputStream out) throws AndrolibException {
        for (String dirName : ApkInfo.RAW_DIRNAMES) {
            File rawDir = new File(mApkDir, dirName);
            if (!rawDir.isDirectory()) {
                continue;
            }

            LOGGER.info("Importing " + dirName + "...");
            try {
                ZipUtils.zipDir(mApkDir, dirName, out, mApkInfo.doNotCompress);
            } catch (IOException ex) {
                throw new AndrolibException(ex);
            }
        }
    }

    private void importUnknownFiles(ZipOutputStream out) throws AndrolibException {
        File unknownDir = new File(mApkDir, "unknown");
        if (!unknownDir.isDirectory()) {
            return;
        }

        LOGGER.info("Importing unknown files...");
        try {
            ZipUtils.zipDir(unknownDir, out, mApkInfo.doNotCompress);
        } catch (IOException ex) {
            throw new AndrolibException(ex);
        }
    }

    private File[] getIncludeFiles() throws AndrolibException {
        UsesFramework usesFramework = mApkInfo.usesFramework;
        if (usesFramework == null) {
            return null;
        }

        List<Integer> ids = usesFramework.ids;
        if (ids == null || ids.isEmpty()) {
            return null;
        }

        Framework framework = new Framework(mConfig);
        String tag = usesFramework.tag;
        File[] files = new File[ids.size()];
        int i = 0;
        for (int id : ids) {
            files[i++] = framework.getApkFile(id, tag);
        }
        return files;
    }

    private boolean isModified(File working, File stored) {
        return !stored.exists() || BrutIO.recursiveModifiedTime(working) > BrutIO.recursiveModifiedTime(stored);
    }

    private boolean isModified(File[] working, File[] stored) {
        for (File file : stored) {
            if (!file.exists()) {
                return true;
            }
        }
        return BrutIO.recursiveModifiedTime(working) > BrutIO.recursiveModifiedTime(stored);
    }

    private File[] newFiles(File dir, String[] names) {
        File[] files = new File[names.length];
        for (int i = 0; i < names.length; i++) {
            files[i] = new File(dir, names[i]);
        }
        return files;
    }
}
