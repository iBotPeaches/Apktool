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
import brut.androlib.res.data.ResUnknownFiles;
import brut.androlib.src.SmaliDecoder;
import brut.directory.Directory;
import brut.directory.ExtFile;
import brut.androlib.res.xml.ResXmlPatcher;
import brut.common.BrutException;
import brut.directory.DirectoryException;
import brut.util.OS;
import com.android.tools.smali.dexlib2.iface.DexFile;
import com.google.common.base.Strings;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class ApkDecoder {

    private final static Logger LOGGER = Logger.getLogger(ApkDecoder.class.getName());

    private final Config config;
    private final AndrolibResources mAndRes;
    private final ExtFile mApkFile;
    private ResTable mResTable;
    protected final ResUnknownFiles mResUnknownFiles;
    private Collection<String> mUncompressedFiles;
    private int mMinSdkVersion = 0;

    private final static String SMALI_DIRNAME = "smali";
    private final static String UNK_DIRNAME = "unknown";
    private final static String[] APK_RESOURCES_FILENAMES = new String[] {
        "resources.arsc", "AndroidManifest.xml", "res", "r", "R" };
    private final static String[] APK_MANIFEST_FILENAMES = new String[] {
        "AndroidManifest.xml" };
    private final static String[] APK_STANDARD_ALL_FILENAMES = new String[] {
        "classes.dex", "AndroidManifest.xml", "resources.arsc", "res", "r", "R",
        "lib", "libs", "assets", "META-INF", "kotlin" };
    private final static Pattern NO_COMPRESS_PATTERN = Pattern.compile("(" +
        "jpg|jpeg|png|gif|wav|mp2|mp3|ogg|aac|mpg|mpeg|mid|midi|smf|jet|rtttl|imy|xmf|mp4|" +
        "m4a|m4v|3gp|3gpp|3g2|3gpp2|amr|awb|wma|wmv|webm|webp|mkv)$");

    public ApkDecoder(ExtFile apkFile) {
        this(Config.getDefaultConfig(), apkFile);
    }

    public ApkDecoder(Config config, ExtFile apkFile) {
        this.config = config;
        mAndRes = new AndrolibResources(config);
        mResUnknownFiles = new ResUnknownFiles();
        mApkFile = apkFile;
    }

    public ApkDecoder(File apkFile) {
        this(new ExtFile(apkFile));
    }

    public ApkDecoder(Config config, File apkFile) {
        this(config, new ExtFile(apkFile));
    }

    public void decode(File outDir) throws AndrolibException, IOException, DirectoryException {
        try {
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

            LOGGER.info("Using Apktool " + ApktoolProperties.getVersion() + " on " + mApkFile.getName());

            if (hasResources()) {
                switch (config.decodeResources) {
                    case Config.DECODE_RESOURCES_NONE:
                        decodeResourcesRaw(mApkFile, outDir);
                        if (config.forceDecodeManifest == Config.FORCE_DECODE_MANIFEST_FULL) {
                            // done after raw decoding of resources because copyToDir overwrites dest files
                            if (hasManifest()) {
                                decodeManifestWithResources(mApkFile, outDir, getResTable());
                            }
                        }
                        break;
                    case Config.DECODE_RESOURCES_FULL:
                        if (hasManifest()) {
                            decodeManifestWithResources(mApkFile, outDir, getResTable());
                        }
                        decodeResourcesFull(mApkFile, outDir, getResTable());
                        break;
                }
            } else {
                // if there's no resources.arsc, decode the manifest without looking
                // up attribute references
                if (hasManifest()) {
                    if (config.decodeResources == Config.DECODE_RESOURCES_FULL
                            || config.forceDecodeManifest == Config.FORCE_DECODE_MANIFEST_FULL) {
                        decodeManifestFull(mApkFile, outDir, getResTable());
                    }
                    else {
                        decodeManifestRaw(mApkFile, outDir);
                    }
                }
            }

            if (hasSources()) {
                switch (config.decodeSources) {
                    case Config.DECODE_SOURCES_NONE:
                        decodeSourcesRaw(mApkFile, outDir, "classes.dex");
                        break;
                    case Config.DECODE_SOURCES_SMALI:
                    case Config.DECODE_SOURCES_SMALI_ONLY_MAIN_CLASSES:
                        decodeSourcesSmali(mApkFile, outDir, "classes.dex");
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
                                    decodeSourcesRaw(mApkFile, outDir, file);
                                    break;
                                case Config.DECODE_SOURCES_SMALI:
                                    decodeSourcesSmali(mApkFile, outDir, file);
                                    break;
                                case Config.DECODE_SOURCES_SMALI_ONLY_MAIN_CLASSES:
                                    if (file.startsWith("classes") && file.endsWith(".dex")) {
                                        decodeSourcesSmali(mApkFile, outDir, file);
                                    } else {
                                        decodeSourcesRaw(mApkFile, outDir, file);
                                    }
                                    break;
                            }
                        }
                    }
                }
            }

            decodeRawFiles(mApkFile, outDir);
            decodeUnknownFiles(mApkFile, outDir);
            mUncompressedFiles = new ArrayList<>();
            recordUncompressedFiles(mApkFile, mUncompressedFiles);
            writeOriginalFiles(mApkFile, outDir);
            writeMetaFile(outDir);
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
            mResTable = mAndRes.getResTable(mApkFile, hasResources);
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
        mAndRes.close();
    }

    public void writeMetaFile(File outDir)
        throws AndrolibException {
        MetaInfo meta = new MetaInfo();
        meta.version = ApktoolProperties.getVersion();
        meta.apkFileName = mApkFile.getName();

        if (mResTable != null) {
            meta.isFrameworkApk = mResTable.isFrameworkApk();
            putUsesFramework(meta);
            putSdkInfo(outDir, meta);
            putPackageInfo(meta);
            putVersionInfo(outDir, meta);
            putSharedLibraryInfo(meta);
            putSparseResourcesInfo(meta);
        } else {
            putMinSdkInfo(meta);
        }
        putUnknownInfo(meta);
        putFileCompressionInfo(meta);

        try {
            meta.save(new File(outDir, "apktool.yml"));
        } catch (IOException ex) {
            throw new AndrolibException(ex);
        }
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

    private void putSdkInfo(File outDir, MetaInfo meta) {
        Map<String, String> info = mResTable.getSdkInfo();
        if (info.size() > 0) {
            String refValue;
            if (info.get("minSdkVersion") != null) {
                refValue = ResXmlPatcher.pullValueFromIntegers(outDir, info.get("minSdkVersion"));
                if (refValue != null) {
                    info.put("minSdkVersion", refValue);
                }
            }
            if (info.get("targetSdkVersion") != null) {
                refValue = ResXmlPatcher.pullValueFromIntegers(outDir, info.get("targetSdkVersion"));
                if (refValue != null) {
                    info.put("targetSdkVersion", refValue);
                }
            }
            if (info.get("maxSdkVersion") != null) {
                refValue = ResXmlPatcher.pullValueFromIntegers(outDir, info.get("maxSdkVersion"));
                if (refValue != null) {
                    info.put("maxSdkVersion", refValue);
                }
            }
            meta.sdkInfo = info;
        }
    }

    private void putMinSdkInfo(MetaInfo meta) {
        if (mMinSdkVersion > 0) {
            Map<String, String> sdkInfo = new LinkedHashMap<>();
            sdkInfo.put("minSdkVersion", Integer.toString(mMinSdkVersion));
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

    private void putVersionInfo(File outDir, MetaInfo meta) {
        VersionInfo info = mResTable.getVersionInfo();
        String refValue = ResXmlPatcher.pullValueFromStrings(outDir, info.versionName);
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
        meta.unknownFiles = mResUnknownFiles.getUnknownFiles();
    }

    private void putFileCompressionInfo(MetaInfo meta) {
        if (mUncompressedFiles != null && !mUncompressedFiles.isEmpty()) {
            meta.doNotCompress = mUncompressedFiles;
        }
    }

    public void decodeSourcesRaw(ExtFile apkFile, File outDir, String filename)
        throws AndrolibException {
        try {
            LOGGER.info("Copying raw " + filename + " file...");
            apkFile.getDirectory().copyToDir(outDir, filename);
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    public void decodeSourcesSmali(File apkFile, File outDir, String filename)
        throws AndrolibException {
        try {
            File smaliDir;
            if (filename.equalsIgnoreCase("classes.dex")) {
                smaliDir = new File(outDir, SMALI_DIRNAME);
            } else {
                smaliDir = new File(outDir, SMALI_DIRNAME + "_" + filename.substring(0, filename.indexOf(".")));
            }
            OS.rmdir(smaliDir);
            //noinspection ResultOfMethodCallIgnored
            smaliDir.mkdirs();
            LOGGER.info("Baksmaling " + filename + "...");
            DexFile dexFile = SmaliDecoder.decode(apkFile, smaliDir, filename,
                config.baksmaliDebugMode, config.apiLevel);
            int minSdkVersion = dexFile.getOpcodes().api;
            if (mMinSdkVersion == 0 || mMinSdkVersion > minSdkVersion) {
                mMinSdkVersion = minSdkVersion;
            }
        } catch (BrutException ex) {
            throw new AndrolibException(ex);
        }
    }

    public void decodeManifestRaw(ExtFile apkFile, File outDir)
        throws AndrolibException {
        try {
            LOGGER.info("Copying raw manifest...");
            apkFile.getDirectory().copyToDir(outDir, APK_MANIFEST_FILENAMES);
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    public void decodeManifestFull(ExtFile apkFile, File outDir, ResTable resTable)
        throws AndrolibException {
        mAndRes.decodeManifest(resTable, apkFile, outDir);
    }

    public void decodeResourcesRaw(ExtFile apkFile, File outDir)
        throws AndrolibException {
        try {
            LOGGER.info("Copying raw resources...");
            apkFile.getDirectory().copyToDir(outDir, APK_RESOURCES_FILENAMES);
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    public void decodeResourcesFull(ExtFile apkFile, File outDir, ResTable resTable)
        throws AndrolibException {
        mAndRes.decode(resTable, apkFile, outDir);
    }

    public void decodeManifestWithResources(ExtFile apkFile, File outDir, ResTable resTable)
        throws AndrolibException {
        mAndRes.decodeManifestWithResources(resTable, apkFile, outDir);
    }

    public void decodeRawFiles(ExtFile apkFile, File outDir)
        throws AndrolibException {
        LOGGER.info("Copying assets and libs...");
        try {
            Directory in = apkFile.getDirectory();

            if (config.decodeAssets == Config.DECODE_ASSETS_FULL) {
                if (in.containsDir("assets")) {
                    in.copyToDir(outDir, "assets");
                }
            }
            if (in.containsDir("lib")) {
                in.copyToDir(outDir, "lib");
            }
            if (in.containsDir("libs")) {
                in.copyToDir(outDir, "libs");
            }
            if (in.containsDir("kotlin")) {
                in.copyToDir(outDir, "kotlin");
            }
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    private boolean isAPKFileNames(String file) {
        for (String apkFile : APK_STANDARD_ALL_FILENAMES) {
            if (apkFile.equals(file) || file.startsWith(apkFile + "/")) {
                return true;
            }
        }
        return false;
    }

    public void decodeUnknownFiles(ExtFile apkFile, File outDir)
        throws AndrolibException {
        LOGGER.info("Copying unknown files...");
        File unknownOut = new File(outDir, UNK_DIRNAME);
        try {
            Directory unk = apkFile.getDirectory();

            // loop all items in container recursively, ignoring any that are pre-defined by aapt
            Set<String> files = unk.getFiles(true);
            for (String file : files) {
                if (!isAPKFileNames(file) && !file.endsWith(".dex")) {

                    // copy file out of archive into special "unknown" folder
                    unk.copyToDir(unknownOut, file);
                    // let's record the name of the file, and its compression type
                    // so that we may re-include it the same way
                    mResUnknownFiles.addUnknownFileInfo(file, String.valueOf(unk.getCompressionLevel(file)));
                }
            }
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    public void writeOriginalFiles(ExtFile apkFile, File outDir)
        throws AndrolibException {
        LOGGER.info("Copying original files...");
        File originalDir = new File(outDir, "original");
        if (!originalDir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            originalDir.mkdirs();
        }

        try {
            Directory in = apkFile.getDirectory();
            if (in.containsFile("AndroidManifest.xml")) {
                in.copyToDir(originalDir, "AndroidManifest.xml");
            }
            if (in.containsFile("stamp-cert-sha256")) {
                in.copyToDir(originalDir, "stamp-cert-sha256");
            }
            if (in.containsDir("META-INF")) {
                in.copyToDir(originalDir, "META-INF");

                if (in.containsDir("META-INF/services")) {
                    // If the original APK contains the folder META-INF/services folder
                    // that is used for service locators (like coroutines on android),
                    // copy it to the destination folder so it does not get dropped.
                    LOGGER.info("Copying META-INF/services directory");
                    in.copyToDir(outDir, "META-INF/services");
                }
            }
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    public void recordUncompressedFiles(ExtFile apkFile, Collection<String> uncompressedFilesOrExts) throws AndrolibException {
        try {
            Directory unk = apkFile.getDirectory();
            Set<String> files = unk.getFiles(true);

            for (String file : files) {
                if (isAPKFileNames(file) && unk.getCompressionLevel(file) == 0) {
                    String extOrFile = "";
                    if (unk.getSize(file) != 0) {
                        extOrFile = FilenameUtils.getExtension(file);
                    }

                    if (extOrFile.isEmpty() || !NO_COMPRESS_PATTERN.matcher(extOrFile).find()) {
                        extOrFile = file;
                        if (mAndRes.mResFileMapping.containsKey(extOrFile)) {
                            extOrFile = mAndRes.mResFileMapping.get(extOrFile);
                        }
                    }
                    if (!uncompressedFilesOrExts.contains(extOrFile)) {
                        uncompressedFilesOrExts.add(extOrFile);
                    }
                }
            }
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }
}
