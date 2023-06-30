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
import brut.androlib.apk.ApkInfo;
import brut.androlib.res.data.*;
import brut.androlib.res.decoder.*;
import brut.androlib.res.util.ExtMXSerializer;
import brut.androlib.res.util.ExtXmlSerializer;
import brut.androlib.res.xml.ResValuesXmlSerializable;
import brut.androlib.res.xml.ResXmlPatcher;
import brut.androlib.src.SmaliDecoder;
import brut.directory.Directory;
import brut.directory.ExtFile;
import brut.common.BrutException;
import brut.directory.DirectoryException;
import brut.directory.FileDirectory;
import brut.util.Duo;
import brut.util.OS;
import com.android.tools.smali.dexlib2.iface.DexFile;
import org.apache.commons.io.FilenameUtils;
import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class ApkDecoder {

    private final static Logger LOGGER = Logger.getLogger(ApkDecoder.class.getName());

    private final Config mConfig;
    private final ExtFile mApkFile;
    private final ApkInfo mApkInfo;
    private final ResTable mResTable;
    protected final ResUnknownFiles mResUnknownFiles;
    private int mMinSdkVersion = 0;
    private final Map<String, String> mResFileMapping = new HashMap<>();


    private final static String SMALI_DIRNAME = "smali";
    private final static String UNK_DIRNAME = "unknown";
    private final static String[] APK_RESOURCES_FILENAMES = new String[] {
        "resources.arsc", "res", "r", "R" };
    private final static String[] APK_MANIFEST_FILENAMES = new String[] {
        "AndroidManifest.xml" };
    private final static String[] APK_STANDARD_ALL_FILENAMES = new String[] {
        "classes.dex", "AndroidManifest.xml", "resources.arsc", "res", "r", "R",
        "lib", "libs", "assets", "META-INF", "kotlin" };
    private final static Pattern NO_COMPRESS_PATTERN = Pattern.compile("(" +
        "jpg|jpeg|png|gif|wav|mp2|mp3|ogg|aac|mpg|mpeg|mid|midi|smf|jet|rtttl|imy|xmf|mp4|" +
        "m4a|m4v|3gp|3gpp|3g2|3gpp2|amr|awb|wma|wmv|webm|webp|mkv)$");

    private final static String[] IGNORED_PACKAGES = new String[] {
        "android", "com.htc", "com.lge", "com.lge.internal", "yi", "flyme", "air.com.adobe.appentry",
        "FFFFFFFFFFFFFFFFFFFFFF" };

    public ApkDecoder(ExtFile apkFile) {
        this(Config.getDefaultConfig(), apkFile);
    }

    public ApkDecoder(Config config, ExtFile apkFile) {
        mConfig = config;
        //mAndRes = new AndrolibResources(config);
        mResUnknownFiles = new ResUnknownFiles();
        mApkFile = apkFile;
        mApkInfo = new ApkInfo();
        mApkInfo.setApkFileName(apkFile.getName());
        mResTable = new ResTable(mConfig, mApkInfo);
    }

    public ApkDecoder(File apkFile) {
        this(new ExtFile(apkFile));
    }

    public ApkDecoder(Config config, File apkFile) {
        this(config, new ExtFile(apkFile));
    }

    public void decode(File outDir) throws AndrolibException, IOException, DirectoryException {
        try {
            if (!mConfig.forceDelete && outDir.exists()) {
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

            decodeManifest(outDir);
            decodeResources(outDir);

            // update apk info
            if (mResTable != null) {
                mResTable.initApkInfo(mApkInfo, outDir);
                if (mConfig.frameworkTag != null) {
                    mApkInfo.usesFramework.tag = mConfig.frameworkTag;
                }
            } else {
                if (mMinSdkVersion > 0) {
                    mApkInfo.setSdkInfo(getMinSdkInfo());
                }
            }

            if (hasSources()) {
                switch (mConfig.decodeSources) {
                    case Config.DECODE_SOURCES_NONE:
                        copySourcesRaw(outDir, "classes.dex");
                        break;
                    case Config.DECODE_SOURCES_SMALI:
                    case Config.DECODE_SOURCES_SMALI_ONLY_MAIN_CLASSES:
                        decodeSourcesSmali(outDir, "classes.dex");
                        break;
                }
            }

            if (hasMultipleSources()) {
                // foreach unknown dex file in root, lets disassemble it
                Set<String> files = mApkFile.getDirectory().getFiles(true);
                for (String file : files) {
                    if (file.endsWith(".dex")) {
                        if (! file.equalsIgnoreCase("classes.dex")) {
                            switch(mConfig.decodeSources) {
                                case Config.DECODE_SOURCES_NONE:
                                    copySourcesRaw(outDir, file);
                                    break;
                                case Config.DECODE_SOURCES_SMALI:
                                    decodeSourcesSmali(outDir, file);
                                    break;
                                case Config.DECODE_SOURCES_SMALI_ONLY_MAIN_CLASSES:
                                    if (file.startsWith("classes") && file.endsWith(".dex")) {
                                        decodeSourcesSmali(outDir, file);
                                    } else {
                                        copySourcesRaw(outDir, file);
                                    }
                                    break;
                            }
                        }
                    }
                }
            }

            copyRawFiles(outDir);
            copyUnknownFiles(outDir);
            Collection<String> mUncompressedFiles = new ArrayList<>();
            recordUncompressedFiles(mUncompressedFiles);
            copyOriginalFiles(outDir);
            writeApkInfo(outDir);
        } finally {
            try {
                mApkFile.close();
            } catch (IOException ignored) {}
        }
    }

    public ResTable getResTable() throws AndrolibException {
        if (! (hasManifest() || hasResources())) {
            throw new AndrolibException(
                "Apk doesn't contain either AndroidManifest.xml file or resources.arsc file");
        }
        if (hasResources() && !mResTable.isMainPkgLoaded()) {
            mResTable.loadMainPkg(mApkFile);
        }
        return mResTable;
    }

    private boolean hasSources() throws AndrolibException {
        try {
            return mApkFile.getDirectory().containsFile("classes.dex");
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    private boolean hasMultipleSources() throws AndrolibException {
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

    private boolean hasManifest() throws AndrolibException {
        try {
            return mApkFile.getDirectory().containsFile("AndroidManifest.xml");
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    private boolean hasResources() throws AndrolibException {
        try {
            return mApkFile.getDirectory().containsFile("resources.arsc");
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    public void close() throws IOException {
        //mAndRes.close();
    }

    private void writeApkInfo(File outDir) throws AndrolibException {
        try {
            mApkInfo.save(new File(outDir, "apktool.yml"));
        } catch (IOException ex) {
            throw new AndrolibException(ex);
        }
    }

    private Map<String, String> getMinSdkInfo() {
        Map<String, String> sdkInfo = new LinkedHashMap<>();
        sdkInfo.put("minSdkVersion", Integer.toString(mMinSdkVersion));
        return sdkInfo;
    }

    private void copySourcesRaw(File outDir, String filename)
        throws AndrolibException {
        try {
            LOGGER.info("Copying raw " + filename + " file...");
            mApkFile.getDirectory().copyToDir(outDir, filename);
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    private void decodeSourcesSmali(File outDir, String filename)
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
            DexFile dexFile = SmaliDecoder.decode(mApkFile, smaliDir, filename,
                mConfig.baksmaliDebugMode, mConfig.apiLevel);
            int minSdkVersion = dexFile.getOpcodes().api;
            if (mMinSdkVersion == 0 || mMinSdkVersion > minSdkVersion) {
                mMinSdkVersion = minSdkVersion;
            }
        } catch (BrutException ex) {
            throw new AndrolibException(ex);
        }
    }

    private void decodeManifest(File outDir) throws AndrolibException {
        if (hasManifest()) {
            if (mConfig.decodeResources == Config.DECODE_RESOURCES_FULL ||
                mConfig.forceDecodeManifest == Config.FORCE_DECODE_MANIFEST_FULL) {
                if (hasResources()) {
                    decodeManifestWithResources(getResTable(), mApkFile, outDir);
                    if (!mConfig.analysisMode) {
                        // update apk info
                        mApkInfo.packageInfo.forcedPackageId = String.valueOf(mResTable.getPackageId());
                    }
                } else {
                    // if there's no resources.arsc, decode the manifest without looking
                    // up attribute references
                    decodeManifest(getResTable(), mApkFile, outDir);
                }
            }
            else {
                try {
                    LOGGER.info("Copying raw manifest...");
                    mApkFile.getDirectory().copyToDir(outDir, APK_MANIFEST_FILENAMES);
                } catch (DirectoryException ex) {
                    throw new AndrolibException(ex);
                }
            }
        }
    }

    private void decodeManifest(ResTable resTable, ExtFile apkFile, File outDir)
        throws AndrolibException {

        Duo<ResFileDecoder, AXmlResourceParser> duo = getManifestFileDecoder(false);
        ResFileDecoder fileDecoder = duo.m1;

        // Set ResAttrDecoder
        duo.m2.setAttrDecoder(new ResAttrDecoder());
        ResAttrDecoder attrDecoder = duo.m2.getAttrDecoder();

        // Fake ResPackage
        attrDecoder.setCurrentPackage(new ResPackage(resTable, 0, null));

        Directory inApk, out;
        try {
            inApk = apkFile.getDirectory();
            out = new FileDirectory(outDir);

            LOGGER.info("Decoding AndroidManifest.xml with only framework resources...");
            fileDecoder.decodeManifest(inApk, "AndroidManifest.xml", out, "AndroidManifest.xml");

        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    private void decodeManifestWithResources(ResTable resTable, ExtFile apkFile, File outDir)
        throws AndrolibException {

        Duo<ResFileDecoder, AXmlResourceParser> duo = getManifestFileDecoder(true);
        ResFileDecoder fileDecoder = duo.m1;
        ResAttrDecoder attrDecoder = duo.m2.getAttrDecoder();

        attrDecoder.setCurrentPackage(resTable.listMainPackages().iterator().next());

        Directory inApk, out;
        try {
            inApk = apkFile.getDirectory();
            out = new FileDirectory(outDir);
            LOGGER.info("Decoding AndroidManifest.xml with resources...");

            fileDecoder.decodeManifest(inApk, "AndroidManifest.xml", out, "AndroidManifest.xml");

            // Remove versionName / versionCode (aapt API 16)
            if (!mConfig.analysisMode) {

                // check for a mismatch between resources.arsc package and the package listed in AndroidManifest
                // also remove the android::versionCode / versionName from manifest for rebuild
                // this is a required change to prevent aapt warning about conflicting versions
                // it will be passed as a parameter to aapt like "--min-sdk-version" via apktool.yml
                adjustPackageManifest(resTable, outDir.getAbsolutePath() + File.separator + "AndroidManifest.xml");

                ResXmlPatcher.removeManifestVersions(new File(
                    outDir.getAbsolutePath() + File.separator + "AndroidManifest.xml"));
            }
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    private void adjustPackageManifest(ResTable resTable, String filePath)
        throws AndrolibException {

        // compare resources.arsc package name to the one present in AndroidManifest
        ResPackage resPackage = resTable.getCurrentResPackage();
        String pkgOriginal = resPackage.getName();
        String packageRenamed = resTable.getPackageRenamed();

        resTable.setPackageId(resPackage.getId());
        resTable.setPackageOriginal(pkgOriginal);

        // 1) Check if pkgOriginal is null (empty resources.arsc)
        // 2) Check if pkgOriginal === mPackageRenamed
        // 3) Check if pkgOriginal is ignored via IGNORED_PACKAGES
        if (pkgOriginal == null || pkgOriginal.equalsIgnoreCase(packageRenamed)
            || (Arrays.asList(IGNORED_PACKAGES).contains(pkgOriginal))) {
            LOGGER.info("Regular manifest package...");
        } else {
            LOGGER.info("Renamed manifest package found! Replacing " + packageRenamed + " with " + pkgOriginal);
            ResXmlPatcher.renameManifestPackage(new File(filePath), pkgOriginal);
        }
    }

    private Duo<ResFileDecoder, AXmlResourceParser> getManifestFileDecoder(boolean withResources) {
        ResStreamDecoderContainer decoders = new ResStreamDecoderContainer();

        AXmlResourceParser axmlParser = new AndroidManifestResourceParser();
        if (withResources) {
            axmlParser.setAttrDecoder(new ResAttrDecoder());
        }
        decoders.setDecoder("xml", new XmlPullStreamDecoder(axmlParser, getResXmlSerializer()));

        return new Duo<>(new ResFileDecoder(decoders), axmlParser);
    }

    private ExtMXSerializer getResXmlSerializer() {
        ExtMXSerializer serial = new ExtMXSerializer();
        serial.setProperty(ExtXmlSerializer.PROPERTY_SERIALIZER_INDENTATION, "    ");
        serial.setProperty(ExtXmlSerializer.PROPERTY_SERIALIZER_LINE_SEPARATOR, System.getProperty("line.separator"));
        serial.setProperty(ExtXmlSerializer.PROPERTY_DEFAULT_ENCODING, "utf-8");
        serial.setDisabledAttrEscape(true);
        return serial;
    }

    private void decodeResources(File outDir) throws AndrolibException {
        if (hasResources()) {
            switch (mConfig.decodeResources) {
                case Config.DECODE_RESOURCES_NONE:
                    try {
                        LOGGER.info("Copying raw resources...");
                        mApkFile.getDirectory().copyToDir(outDir, APK_RESOURCES_FILENAMES);
                    } catch (DirectoryException ex) {
                        throw new AndrolibException(ex);
                    }
                    break;
                case Config.DECODE_RESOURCES_FULL:
                    decodeResources(getResTable(), mApkFile, outDir);
                    break;
            }
        }
    }

    private void decodeResources(ResTable resTable, ExtFile apkFile, File outDir)
        throws AndrolibException {
        Duo<ResFileDecoder, AXmlResourceParser> duo = getResFileDecoder();
        ResFileDecoder fileDecoder = duo.m1;
        ResAttrDecoder attrDecoder = duo.m2.getAttrDecoder();

        attrDecoder.setCurrentPackage(resTable.listMainPackages().iterator().next());
        Directory in, out;

        try {
            out = new FileDirectory(outDir);
            in = apkFile.getDirectory();
            out = out.createDir("res");
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }

        ExtMXSerializer xmlSerializer = getResXmlSerializer();
        for (ResPackage pkg : resTable.listMainPackages()) {
            attrDecoder.setCurrentPackage(pkg);

            LOGGER.info("Decoding file-resources...");
            for (ResResource res : pkg.listFiles()) {
                fileDecoder.decode(res, in, out, mResFileMapping);
            }

            LOGGER.info("Decoding values */* XMLs...");
            for (ResValuesFile valuesFile : pkg.listValuesFiles()) {
                generateValuesFile(valuesFile, out, xmlSerializer);
            }
            generatePublicXml(pkg, out, xmlSerializer);
        }

        AndrolibException decodeError = duo.m2.getFirstError();
        if (decodeError != null) {
            throw decodeError;
        }
    }

    private Duo<ResFileDecoder, AXmlResourceParser> getResFileDecoder() {
        ResStreamDecoderContainer decoders = new ResStreamDecoderContainer();
        decoders.setDecoder("raw", new ResRawStreamDecoder());
        decoders.setDecoder("9patch", new Res9patchStreamDecoder());

        AXmlResourceParser axmlParser = new AXmlResourceParser();
        axmlParser.setAttrDecoder(new ResAttrDecoder());
        decoders.setDecoder("xml", new XmlPullStreamDecoder(axmlParser, getResXmlSerializer()));

        return new Duo<>(new ResFileDecoder(decoders), axmlParser);
    }

    private void generateValuesFile(ResValuesFile valuesFile, Directory out,
                                    ExtXmlSerializer serial) throws AndrolibException {
        try {
            OutputStream outStream = out.getFileOutput(valuesFile.getPath());
            serial.setOutput((outStream), null);
            serial.startDocument(null, null);
            serial.startTag(null, "resources");

            for (ResResource res : valuesFile.listResources()) {
                if (valuesFile.isSynthesized(res)) {
                    continue;
                }
                ((ResValuesXmlSerializable) res.getValue()).serializeToResValuesXml(serial, res);
            }

            serial.endTag(null, "resources");
            serial.newLine();
            serial.endDocument();
            serial.flush();
            outStream.close();
        } catch (IOException | DirectoryException ex) {
            throw new AndrolibException("Could not generate: " + valuesFile.getPath(), ex);
        }
    }

    private void generatePublicXml(ResPackage pkg, Directory out,
                                   XmlSerializer serial) throws AndrolibException {
        try {
            OutputStream outStream = out.getFileOutput("values/public.xml");
            serial.setOutput(outStream, null);
            serial.startDocument(null, null);
            serial.startTag(null, "resources");

            for (ResResSpec spec : pkg.listResSpecs()) {
                serial.startTag(null, "public");
                serial.attribute(null, "type", spec.getType().getName());
                serial.attribute(null, "name", spec.getName());
                serial.attribute(null, "id", String.format("0x%08x", spec.getId().id));
                serial.endTag(null, "public");
            }

            serial.endTag(null, "resources");
            serial.endDocument();
            serial.flush();
            outStream.close();
        } catch (IOException | DirectoryException ex) {
            throw new AndrolibException("Could not generate public.xml file", ex);
        }
    }

    private void copyRawFiles(File outDir)
        throws AndrolibException {
        LOGGER.info("Copying assets and libs...");
        try {
            Directory in = mApkFile.getDirectory();

            if (mConfig.decodeAssets == Config.DECODE_ASSETS_FULL) {
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

    private void copyUnknownFiles(File outDir)
        throws AndrolibException {
        LOGGER.info("Copying unknown files...");
        File unknownOut = new File(outDir, UNK_DIRNAME);
        try {
            Directory unk = mApkFile.getDirectory();

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
            // update apk info
            mApkInfo.unknownFiles = mResUnknownFiles.getUnknownFiles();
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    private void copyOriginalFiles(File outDir)
        throws AndrolibException {
        LOGGER.info("Copying original files...");
        File originalDir = new File(outDir, "original");
        if (!originalDir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            originalDir.mkdirs();
        }

        try {
            Directory in = mApkFile.getDirectory();
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
                    // copy it to the destination folder, so it does not get dropped.
                    LOGGER.info("Copying META-INF/services directory");
                    in.copyToDir(outDir, "META-INF/services");
                }
            }
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    private void recordUncompressedFiles(Collection<String> uncompressedFilesOrExts) throws AndrolibException {
        try {
            Directory unk = mApkFile.getDirectory();
            Set<String> files = unk.getFiles(true);

            for (String file : files) {
                if (isAPKFileNames(file) && unk.getCompressionLevel(file) == 0) {
                    String extOrFile = "";
                    if (unk.getSize(file) != 0) {
                        extOrFile = FilenameUtils.getExtension(file);
                    }

                    if (extOrFile.isEmpty() || !NO_COMPRESS_PATTERN.matcher(extOrFile).find()) {
                        extOrFile = file;
                        if (mResFileMapping.containsKey(extOrFile)) {
                            extOrFile = mResFileMapping.get(extOrFile);
                        }
                    }
                    if (!uncompressedFilesOrExts.contains(extOrFile)) {
                        uncompressedFilesOrExts.add(extOrFile);
                    }
                }
            }
            // update apk info
            if (!uncompressedFilesOrExts.isEmpty()) {
                mApkInfo.doNotCompress = uncompressedFilesOrExts;
            }

        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }
}
