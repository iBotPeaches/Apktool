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
import brut.androlib.exceptions.InFileNotFoundException;
import brut.androlib.exceptions.OutDirExistsException;
import brut.androlib.exceptions.UndefinedResObjectException;
import brut.androlib.meta.MetaInfo;
import brut.androlib.meta.PackageInfo;
import brut.androlib.meta.UsesFramework;
import brut.androlib.meta.VersionInfo;
import brut.androlib.res.AndrolibResources;
import brut.androlib.res.data.ResPackage;
import brut.androlib.res.data.ResTable;
import brut.directory.ExtFile;
import brut.androlib.res.xml.ResXmlPatcher;
import brut.common.BrutException;
import brut.directory.DirectoryException;
import brut.util.OS;
import com.google.common.base.Strings;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

public class ApkDecoder {

    private final static Logger LOGGER = Logger.getLogger(ApkDecoder.class.getName());
    private final Config config;
    private final Androlib mAndrolib;
    private final ExtFile mApkFile;
    private File mOutDir;
    private ResTable mResTable;
    private Collection<String> mUncompressedFiles;

    public ApkDecoder(ExtFile apkFile) {
        this(Config.getDefaultConfig(), apkFile);
    }

    public ApkDecoder(Config config, ExtFile apkFile) {
        this.config = config;
        mAndrolib = new Androlib(config);
        mApkFile = apkFile;
    }

    public ApkDecoder(File apkFile) {
        this(new ExtFile(apkFile));
    }

    public ApkDecoder(Config config, File apkFile) {
        this(config, new ExtFile(apkFile));
    }

    public void setOutDir(File outDir) {
        mOutDir = outDir;
    }

    public void decode() throws AndrolibException, IOException, DirectoryException {
        try {
            File outDir = getOutDir();

            if (!config.forceDelete && outDir.exists()) {
                throw new OutDirExistsException();
            }

            if (!mApkFile.isFile() || !mApkFile.canRead()) {
                throw new InFileNotFoundException();
            }

            try {
                OS.rmdir(outDir);
            } catch (BrutException ex) {
                throw new AndrolibException(ex);
            }
            outDir.mkdirs();

            LOGGER.info("Using Apktool " + Androlib.getVersion() + " on " + mApkFile.getName());

            if (hasResources()) {
                switch (config.decodeResources) {
                    case Config.DECODE_RESOURCES_NONE:
                        mAndrolib.decodeResourcesRaw(mApkFile, outDir);
                        if (config.forceDecodeManifest == Config.FORCE_DECODE_MANIFEST_FULL) {
                            // done after raw decoding of resources because copyToDir overwrites dest files
                            if (hasManifest()) {
                                mAndrolib.decodeManifestWithResources(mApkFile, outDir, getResTable());
                            }
                        }
                        break;
                    case Config.DECODE_RESOURCES_FULL:
                        if (hasManifest()) {
                            mAndrolib.decodeManifestWithResources(mApkFile, outDir, getResTable());
                        }
                        mAndrolib.decodeResourcesFull(mApkFile, outDir, getResTable());
                        break;
                }
            } else {
                // if there's no resources.arsc, decode the manifest without looking
                // up attribute references
                if (hasManifest()) {
                    if (config.decodeResources == Config.DECODE_RESOURCES_FULL
                            || config.forceDecodeManifest == Config.FORCE_DECODE_MANIFEST_FULL) {
                        mAndrolib.decodeManifestFull(mApkFile, outDir, getResTable());
                    }
                    else {
                        mAndrolib.decodeManifestRaw(mApkFile, outDir);
                    }
                }
            }

            if (hasSources()) {
                switch (config.decodeSources) {
                    case Config.DECODE_SOURCES_NONE:
                        mAndrolib.decodeSourcesRaw(mApkFile, outDir, "classes.dex");
                        break;
                    case Config.DECODE_SOURCES_SMALI:
                    case Config.DECODE_SOURCES_SMALI_ONLY_MAIN_CLASSES:
                        mAndrolib.decodeSourcesSmali(mApkFile, outDir, "classes.dex");
                        break;
                }
            }

            if (hasMultipleSources()) {
                // foreach unknown dex file in root, lets disassemble it
                Set<String> files = mApkFile.getDirectory().getFiles(true);
                for (String file : files) {
                    if (file.endsWith(".dex")) {
                        if (! file.equalsIgnoreCase("classes.dex")) {
                            switch(config.decodeSources) {
                                case Config.DECODE_SOURCES_NONE:
                                    mAndrolib.decodeSourcesRaw(mApkFile, outDir, file);
                                    break;
                                case Config.DECODE_SOURCES_SMALI:
                                    mAndrolib.decodeSourcesSmali(mApkFile, outDir, file);
                                    break;
                                case Config.DECODE_SOURCES_SMALI_ONLY_MAIN_CLASSES:
                                    if (file.startsWith("classes") && file.endsWith(".dex")) {
                                        mAndrolib.decodeSourcesSmali(mApkFile, outDir, file);
                                    } else {
                                        mAndrolib.decodeSourcesRaw(mApkFile, outDir, file);
                                    }
                                    break;
                            }
                        }
                    }
                }
            }

            mAndrolib.decodeRawFiles(mApkFile, outDir);
            mAndrolib.decodeUnknownFiles(mApkFile, outDir);
            mUncompressedFiles = new ArrayList<>();
            mAndrolib.recordUncompressedFiles(mApkFile, mUncompressedFiles);
            mAndrolib.writeOriginalFiles(mApkFile, outDir);
            writeMetaFile();
        } finally {
            try {
                mApkFile.close();
            } catch (IOException ignored) {}
        }
    }

    public ResTable getResTable() throws AndrolibException {
        if (mResTable == null) {
            boolean hasResources = hasResources();
            boolean hasManifest = hasManifest();
            if (! (hasManifest || hasResources)) {
                throw new AndrolibException(
                        "Apk doesn't contain either AndroidManifest.xml file or resources.arsc file");
            }
            mResTable = mAndrolib.getResTable(mApkFile, hasResources);
            mResTable.setAnalysisMode(config.analysisMode);
        }
        return mResTable;
    }

    public boolean hasSources() throws AndrolibException {
        try {
            return mApkFile.getDirectory().containsFile("classes.dex");
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    public boolean hasMultipleSources() throws AndrolibException {
        try {
            Set<String> files = mApkFile.getDirectory().getFiles(false);
            for (String file : files) {
                if (file.endsWith(".dex")) {
                    if (! file.equalsIgnoreCase("classes.dex")) {
                        return true;
                    }
                }
            }

            return false;
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    public boolean hasManifest() throws AndrolibException {
        try {
            return mApkFile.getDirectory().containsFile("AndroidManifest.xml");
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    public boolean hasResources() throws AndrolibException {
        try {
            return mApkFile.getDirectory().containsFile("resources.arsc");
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    public void close() throws IOException {
        if (mAndrolib != null) {
            mAndrolib.close();
        }
    }

    private File getOutDir() throws AndrolibException {
        if (mOutDir == null) {
            throw new AndrolibException("Out dir not set");
        }
        return mOutDir;
    }

    private void writeMetaFile() throws AndrolibException {
        MetaInfo meta = new MetaInfo();
        meta.version = Androlib.getVersion();
        meta.apkFileName = mApkFile.getName();

        if (mResTable != null) {
            meta.isFrameworkApk = mAndrolib.isFrameworkApk(mResTable);
            putUsesFramework(meta);
            putSdkInfo(meta);
            putPackageInfo(meta);
            putVersionInfo(meta);
            putSharedLibraryInfo(meta);
            putSparseResourcesInfo(meta);
        } else {
            putMinSdkInfo(meta);
        }
        putUnknownInfo(meta);
        putFileCompressionInfo(meta);

        mAndrolib.writeMetaFile(mOutDir, meta);
    }

    private void putUsesFramework(MetaInfo meta) {
        Set<ResPackage> pkgs = mResTable.listFramePackages();
        if (pkgs.isEmpty()) {
            return;
        }

        Integer[] ids = new Integer[pkgs.size()];
        int i = 0;
        for (ResPackage pkg : pkgs) {
            ids[i++] = pkg.getId();
        }
        Arrays.sort(ids);

        meta.usesFramework = new UsesFramework();
        meta.usesFramework.ids = Arrays.asList(ids);

        if (config.frameworkTag != null) {
            meta.usesFramework.tag = config.frameworkTag;
        }
    }

    private void putSdkInfo(MetaInfo meta) {
        Map<String, String> info = mResTable.getSdkInfo();
        if (info.size() > 0) {
            String refValue;
            if (info.get("minSdkVersion") != null) {
                refValue = ResXmlPatcher.pullValueFromIntegers(mOutDir, info.get("minSdkVersion"));
                if (refValue != null) {
                    info.put("minSdkVersion", refValue);
                }
            }
            if (info.get("targetSdkVersion") != null) {
                refValue = ResXmlPatcher.pullValueFromIntegers(mOutDir, info.get("targetSdkVersion"));
                if (refValue != null) {
                    info.put("targetSdkVersion", refValue);
                }
            }
            if (info.get("maxSdkVersion") != null) {
                refValue = ResXmlPatcher.pullValueFromIntegers(mOutDir, info.get("maxSdkVersion"));
                if (refValue != null) {
                    info.put("maxSdkVersion", refValue);
                }
            }
            meta.sdkInfo = info;
        }
    }

    private void putMinSdkInfo(MetaInfo meta) {
        int minSdkVersion = mAndrolib.getMinSdkVersion();
        if (minSdkVersion > 0) {
            Map<String, String> sdkInfo = new LinkedHashMap<>();
            sdkInfo.put("minSdkVersion", Integer.toString(minSdkVersion));
            meta.sdkInfo = sdkInfo;
        }
    }

    private void putPackageInfo(MetaInfo meta) throws AndrolibException {
        String renamed = mResTable.getPackageRenamed();
        String original = mResTable.getPackageOriginal();

        int id = mResTable.getPackageId();
        try {
            id = mResTable.getPackage(renamed).getId();
        } catch (UndefinedResObjectException ignored) {}

        if (Strings.isNullOrEmpty(original)) {
            return;
        }

        meta.packageInfo = new PackageInfo();

        // only put rename-manifest-package into apktool.yml, if the change will be required
        if (!renamed.equalsIgnoreCase(original)) {
            meta.packageInfo.renameManifestPackage = renamed;
        }
        meta.packageInfo.forcedPackageId = String.valueOf(id);
    }

    private void putVersionInfo(MetaInfo meta) {
        VersionInfo info = mResTable.getVersionInfo();
        String refValue = ResXmlPatcher.pullValueFromStrings(mOutDir, info.versionName);
        if (refValue != null) {
            info.versionName = refValue;
        }
        meta.versionInfo = info;
    }

    private void putSharedLibraryInfo(MetaInfo meta) {
        meta.sharedLibrary = mResTable.getSharedLibrary();
    }

    private void putSparseResourcesInfo(MetaInfo meta) {
        meta.sparseResources = mResTable.getSparseResources();
    }

    private void putUnknownInfo(MetaInfo meta) {
        meta.unknownFiles = mAndrolib.mResUnknownFiles.getUnknownFiles();
    }

    private void putFileCompressionInfo(MetaInfo meta) {
        if (mUncompressedFiles != null && !mUncompressedFiles.isEmpty()) {
            meta.doNotCompress = mUncompressedFiles;
        }
    }
}
