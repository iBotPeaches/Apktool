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
package brut.androlib.res;

import brut.androlib.exceptions.AndrolibException;
import brut.androlib.Config;
import brut.androlib.meta.MetaInfo;
import brut.androlib.meta.PackageInfo;
import brut.androlib.meta.VersionInfo;
import brut.androlib.res.data.*;
import brut.androlib.res.decoder.*;
import brut.androlib.res.util.ExtMXSerializer;
import brut.androlib.res.util.ExtXmlSerializer;
import brut.androlib.res.xml.ResValuesXmlSerializable;
import brut.androlib.res.xml.ResXmlPatcher;
import brut.common.BrutException;
import brut.directory.*;
import brut.util.*;
import org.apache.commons.io.IOUtils;
import org.xmlpull.v1.XmlSerializer;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;

final public class AndrolibResources {

    private final Config config;

    public Map<String, String> mResFileMapping = new HashMap<>();

    private final static Logger LOGGER = Logger.getLogger(AndrolibResources.class.getName());

    private ExtFile mFramework = null;

    private String mMinSdkVersion = null;
    private String mMaxSdkVersion = null;
    private String mTargetSdkVersion = null;
    private String mVersionCode = null;
    private String mVersionName = null;
    private String mPackageRenamed = null;
    private String mPackageId = null;

    private boolean mSharedLibrary = false;
    private boolean mSparseResources = false;

    private final static String[] IGNORED_PACKAGES = new String[] {
        "android", "com.htc", "com.lge", "com.lge.internal", "yi", "flyme", "air.com.adobe.appentry",
        "FFFFFFFFFFFFFFFFFFFFFF" };

    public AndrolibResources(Config config) {
        this.config = config;
    }

    public AndrolibResources() {
        this.config = Config.getDefaultConfig();
    }

    public ResTable getResTable(ExtFile apkFile, boolean loadMainPkg)
            throws AndrolibException {
        ResTable resTable = new ResTable(this);
        if (loadMainPkg) {
            loadMainPkg(resTable, apkFile);
        }
        return resTable;
    }

    public ResPackage loadMainPkg(ResTable resTable, ExtFile apkFile)
            throws AndrolibException {
        LOGGER.info("Loading resource table...");
        ResPackage[] pkgs = getResPackagesFromApk(apkFile, resTable, config.keepBrokenResources);
        ResPackage pkg;

        switch (pkgs.length) {
            case 0:
                pkg = new ResPackage(resTable, 0, null);
                break;
            case 1:
                pkg = pkgs[0];
                break;
            case 2:
                LOGGER.warning("Skipping package group: " + pkgs[0].getName());
                pkg = pkgs[1];
                break;
            default:
                pkg = selectPkgWithMostResSpecs(pkgs);
                break;
        }

        resTable.addPackage(pkg, true);
        return pkg;
    }

    public ResPackage selectPkgWithMostResSpecs(ResPackage[] pkgs) {
        int id = 0;
        int value = 0;
        int index = 0;

        for (int i = 0; i < pkgs.length; i++) {
            ResPackage resPackage = pkgs[i];
            if (resPackage.getResSpecCount() > value && ! resPackage.getName().equalsIgnoreCase("android")) {
                value = resPackage.getResSpecCount();
                id = resPackage.getId();
                index = i;
            }
        }

        // if id is still 0, we only have one pkgId which is "android" -> 1
        return (id == 0) ? pkgs[0] : pkgs[index];
    }

    public ResPackage loadFrameworkPkg(ResTable resTable, int id)
            throws AndrolibException {
        Framework framework = new Framework(config);
        File apk = framework.getFrameworkApk(id, config.frameworkTag);

        LOGGER.info("Loading resource table from file: " + apk);
        mFramework = new ExtFile(apk);
        ResPackage[] pkgs = getResPackagesFromApk(mFramework, resTable, true);

        ResPackage pkg;
        if (pkgs.length > 1) {
            pkg = selectPkgWithMostResSpecs(pkgs);
        } else if (pkgs.length == 0) {
            throw new AndrolibException("Arsc files with zero or multiple packages");
        } else {
            pkg = pkgs[0];
        }

        if (pkg.getId() != id) {
            throw new AndrolibException("Expected pkg of id: " + id + ", got: " + pkg.getId());
        }

        resTable.addPackage(pkg, false);
        return pkg;
    }

    public void decodeManifest(ResTable resTable, ExtFile apkFile, File outDir)
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

    public void adjustPackageManifest(ResTable resTable, String filePath)
            throws AndrolibException {

        // compare resources.arsc package name to the one present in AndroidManifest
        ResPackage resPackage = resTable.getCurrentResPackage();
        String pkgOriginal = resPackage.getName();
        mPackageRenamed = resTable.getPackageRenamed();

        resTable.setPackageId(resPackage.getId());
        resTable.setPackageOriginal(pkgOriginal);

        // 1) Check if pkgOriginal is null (empty resources.arsc)
        // 2) Check if pkgOriginal === mPackageRenamed
        // 3) Check if pkgOriginal is ignored via IGNORED_PACKAGES
        if (pkgOriginal == null || pkgOriginal.equalsIgnoreCase(mPackageRenamed)
                || (Arrays.asList(IGNORED_PACKAGES).contains(pkgOriginal))) {
            LOGGER.info("Regular manifest package...");
        } else {
            LOGGER.info("Renamed manifest package found! Replacing " + mPackageRenamed + " with " + pkgOriginal);
            ResXmlPatcher.renameManifestPackage(new File(filePath), pkgOriginal);
        }
    }

    public void decodeManifestWithResources(ResTable resTable, ExtFile apkFile, File outDir)
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
            if (!config.analysisMode) {

                // check for a mismatch between resources.arsc package and the package listed in AndroidManifest
                // also remove the android::versionCode / versionName from manifest for rebuild
                // this is a required change to prevent aapt warning about conflicting versions
                // it will be passed as a parameter to aapt like "--min-sdk-version" via apktool.yml
                adjustPackageManifest(resTable, outDir.getAbsolutePath() + File.separator + "AndroidManifest.xml");

                ResXmlPatcher.removeManifestVersions(new File(
                        outDir.getAbsolutePath() + File.separator + "AndroidManifest.xml"));

                mPackageId = String.valueOf(resTable.getPackageId());
            }
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    public void decode(ResTable resTable, ExtFile apkFile, File outDir)
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

    public void setSdkInfo(Map<String, String> map) {
        if (map != null) {
            mMinSdkVersion = map.get("minSdkVersion");
            mTargetSdkVersion = map.get("targetSdkVersion");
            mMaxSdkVersion = map.get("maxSdkVersion");
        }
    }

    public void setVersionInfo(VersionInfo versionInfo) {
        if (versionInfo != null) {
            mVersionCode = versionInfo.versionCode;
            mVersionName = versionInfo.versionName;
        }
    }

    public void setPackageRenamed(PackageInfo packageInfo) {
        if (packageInfo != null) {
            mPackageRenamed = packageInfo.renameManifestPackage;
        }
    }

    public void setPackageId(PackageInfo packageInfo) {
        if (packageInfo != null) {
            mPackageId = packageInfo.forcedPackageId;
        }
    }

    public void setSharedLibrary(boolean flag) {
        mSharedLibrary = flag;
    }

    public void setSparseResources(boolean flag) {
        mSparseResources = flag;
    }

    public String checkTargetSdkVersionBounds() {
        int target = mapSdkShorthandToVersion(mTargetSdkVersion);

        int min = (mMinSdkVersion != null) ? mapSdkShorthandToVersion(mMinSdkVersion) : 0;
        int max = (mMaxSdkVersion != null) ? mapSdkShorthandToVersion(mMaxSdkVersion) : target;

        target = Math.min(max, target);
        target = Math.max(min, target);
        return Integer.toString(target);
    }

    private File createDoNotCompressExtensionsFile(Config config) throws AndrolibException {
        if (config.doNotCompress == null || config.doNotCompress.isEmpty()) {
            return null;
        }

        File doNotCompressFile;
        try {
            doNotCompressFile = File.createTempFile("APKTOOL", null);
            doNotCompressFile.deleteOnExit();

            BufferedWriter fileWriter = new BufferedWriter(new FileWriter(doNotCompressFile));
            for (String extension : config.doNotCompress) {
                fileWriter.write(extension);
                fileWriter.newLine();
            }
            fileWriter.close();

            return doNotCompressFile;
        } catch (IOException ex) {
            throw new AndrolibException(ex);
        }
    }

    private void aapt2Package(File apkFile, File manifest, File resDir, File rawDir, File assetDir, File[] include,
                              List<String> cmd, boolean customAapt)
            throws AndrolibException {

        List<String> compileCommand = new ArrayList<>(cmd);
        File resourcesZip = null;

        if (resDir != null) {
            File buildDir = new File(resDir.getParent(), "build");
            resourcesZip = new File(buildDir, "resources.zip");
        }

        if (resDir != null && !resourcesZip.exists()) {

            // Compile the files into flat arsc files
            cmd.add("compile");

            cmd.add("--dir");
            cmd.add(resDir.getAbsolutePath());

            // Treats error that used to be valid in aapt1 as warnings in aapt2
            cmd.add("--legacy");

            File buildDir = new File(resDir.getParent(), "build");
            resourcesZip = new File(buildDir, "resources.zip");

            cmd.add("-o");
            cmd.add(resourcesZip.getAbsolutePath());

            if (config.verbose) {
                cmd.add("-v");
            }

            if (config.noCrunch) {
                cmd.add("--no-crunch");
            }

            try {
                OS.exec(cmd.toArray(new String[0]));
                LOGGER.fine("aapt2 compile command ran: ");
                LOGGER.fine(cmd.toString());
            } catch (BrutException ex) {
                throw new AndrolibException(ex);
            }
        }

        if (manifest == null) {
            return;
        }

        // Link them into the final apk, reusing our old command after clearing for the aapt2 binary
        cmd = new ArrayList<>(compileCommand);
        cmd.add("link");

        cmd.add("-o");
        cmd.add(apkFile.getAbsolutePath());

        if (mPackageId != null && ! mSharedLibrary) {
            cmd.add("--package-id");
            cmd.add(mPackageId);
        }

        if (mSharedLibrary) {
            cmd.add("--shared-lib");
        }

        if (mMinSdkVersion != null) {
            cmd.add("--min-sdk-version");
            cmd.add(mMinSdkVersion);
        }

        if (mTargetSdkVersion != null) {
            cmd.add("--target-sdk-version");
            cmd.add(checkTargetSdkVersionBounds());
        }

        if (mPackageRenamed != null) {
            cmd.add("--rename-manifest-package");
            cmd.add(mPackageRenamed);

            cmd.add("--rename-instrumentation-target-package");
            cmd.add(mPackageRenamed);
        }

        if (mVersionCode != null) {
            cmd.add("--version-code");
            cmd.add(mVersionCode);
        }

        if (mVersionName != null) {
            cmd.add("--version-name");
            cmd.add(mVersionName);
        }

        // Disable automatic changes
        cmd.add("--no-auto-version");
        cmd.add("--no-version-vectors");
        cmd.add("--no-version-transitions");
        cmd.add("--no-resource-deduping");

        cmd.add("--allow-reserved-package-id");

        if (mSparseResources) {
            cmd.add("--enable-sparse-encoding");
        }

        if (config.isFramework) {
            cmd.add("-x");
        }

        if (config.doNotCompress != null && !customAapt) {
            // Use custom -e option to avoid limits on commandline length.
            // Can only be used when custom aapt binary is not used.
            String extensionsFilePath =
                Objects.requireNonNull(createDoNotCompressExtensionsFile(config)).getAbsolutePath();
            cmd.add("-e");
            cmd.add(extensionsFilePath);
        } else if (config.doNotCompress != null) {
            for (String file : config.doNotCompress) {
                cmd.add("-0");
                cmd.add(file);
            }
        }

        if (!config.resourcesAreCompressed) {
            cmd.add("-0");
            cmd.add("arsc");
        }

        if (include != null) {
            for (File file : include) {
                cmd.add("-I");
                cmd.add(file.getPath());
            }
        }

        cmd.add("--manifest");
        cmd.add(manifest.getAbsolutePath());

        if (assetDir != null) {
            cmd.add("-A");
            cmd.add(assetDir.getAbsolutePath());
        }

        if (rawDir != null) {
            cmd.add("-R");
            cmd.add(rawDir.getAbsolutePath());
        }

        if (config.verbose) {
            cmd.add("-v");
        }

        if (resourcesZip != null) {
            cmd.add(resourcesZip.getAbsolutePath());
        }

        try {
            OS.exec(cmd.toArray(new String[0]));
            LOGGER.fine("aapt2 link command ran: ");
            LOGGER.fine(cmd.toString());
        } catch (BrutException ex) {
            throw new AndrolibException(ex);
        }
    }

    private void aapt1Package(File apkFile, File manifest, File resDir, File rawDir, File assetDir, File[] include,
                              List<String> cmd, boolean customAapt)
            throws AndrolibException {

        cmd.add("p");

        if (config.verbose) { // output aapt verbose
            cmd.add("-v");
        }
        if (config.updateFiles) {
            cmd.add("-u");
        }
        if (config.debugMode) { // inject debuggable="true" into manifest
            cmd.add("--debug-mode");
        }
        if (config.noCrunch) {
            cmd.add("--no-crunch");
        }
        // force package id so that some frameworks build with correct id
        // disable if user adds own aapt (can't know if they have this feature)
        if (mPackageId != null && ! customAapt && ! mSharedLibrary) {
            cmd.add("--forced-package-id");
            cmd.add(mPackageId);
        }
        if (mSharedLibrary) {
            cmd.add("--shared-lib");
        }
        if (mMinSdkVersion != null) {
            cmd.add("--min-sdk-version");
            cmd.add(mMinSdkVersion);
        }
        if (mTargetSdkVersion != null) {
            cmd.add("--target-sdk-version");

            // Ensure that targetSdkVersion is between minSdkVersion/maxSdkVersion if
            // they are specified.
            cmd.add(checkTargetSdkVersionBounds());
        }
        if (mMaxSdkVersion != null) {
            cmd.add("--max-sdk-version");
            cmd.add(mMaxSdkVersion);

            // if we have max sdk version, set --max-res-version
            // so we can ignore anything over that during build.
            cmd.add("--max-res-version");
            cmd.add(mMaxSdkVersion);
        }
        if (mPackageRenamed != null) {
            cmd.add("--rename-manifest-package");
            cmd.add(mPackageRenamed);
        }
        if (mVersionCode != null) {
            cmd.add("--version-code");
            cmd.add(mVersionCode);
        }
        if (mVersionName != null) {
            cmd.add("--version-name");
            cmd.add(mVersionName);
        }
        cmd.add("--no-version-vectors");
        cmd.add("-F");
        cmd.add(apkFile.getAbsolutePath());

        if (config.isFramework) {
            cmd.add("-x");
        }

        if (config.doNotCompress != null && !customAapt) {
            // Use custom -e option to avoid limits on commandline length.
            // Can only be used when custom aapt binary is not used.
            String extensionsFilePath =
                Objects.requireNonNull(createDoNotCompressExtensionsFile(config)).getAbsolutePath();
            cmd.add("-e");
            cmd.add(extensionsFilePath);
        } else if (config.doNotCompress != null) {
            for (String file : config.doNotCompress) {
                cmd.add("-0");
                cmd.add(file);
            }
        }

        if (!config.resourcesAreCompressed) {
            cmd.add("-0");
            cmd.add("arsc");
        }

        if (include != null) {
            for (File file : include) {
                cmd.add("-I");
                cmd.add(file.getPath());
            }
        }
        if (resDir != null) {
            cmd.add("-S");
            cmd.add(resDir.getAbsolutePath());
        }
        if (manifest != null) {
            cmd.add("-M");
            cmd.add(manifest.getAbsolutePath());
        }
        if (assetDir != null) {
            cmd.add("-A");
            cmd.add(assetDir.getAbsolutePath());
        }
        if (rawDir != null) {
            cmd.add(rawDir.getAbsolutePath());
        }
        try {
            OS.exec(cmd.toArray(new String[0]));
            LOGGER.fine("command ran: ");
            LOGGER.fine(cmd.toString());
        } catch (BrutException ex) {
            throw new AndrolibException(ex);
        }
    }

    public void aaptPackage(File apkFile, File manifest, File resDir, File rawDir, File assetDir, File[] include)
            throws AndrolibException {

        String aaptPath = config.aaptPath;
        boolean customAapt = !aaptPath.isEmpty();
        List<String> cmd = new ArrayList<>();

        try {
            String aaptCommand = AaptManager.getAaptExecutionCommand(aaptPath, getAaptBinaryFile());
            cmd.add(aaptCommand);
        } catch (BrutException ex) {
            LOGGER.warning("aapt: " + ex.getMessage() + " (defaulting to $PATH binary)");
            cmd.add(AaptManager.getAaptBinaryName(getAaptVersion()));
        }

        if (config.isAapt2()) {
            aapt2Package(apkFile, manifest, resDir, rawDir, assetDir, include, cmd, customAapt);
            return;
        }
        aapt1Package(apkFile, manifest, resDir, rawDir, assetDir, include, cmd, customAapt);
    }

    public void zipPackage(File apkFile, File rawDir, File assetDir)
            throws AndrolibException {

        try {
            ZipUtils.zipFolders(rawDir, apkFile, assetDir, config.doNotCompress);
        } catch (IOException | BrutException ex) {
            throw new AndrolibException(ex);
        }
    }

    public int getMinSdkVersionFromAndroidCodename(MetaInfo meta, String sdkVersion) {
        int sdkNumber = mapSdkShorthandToVersion(sdkVersion);

        if (sdkNumber == ResConfigFlags.SDK_BASE) {
            return Integer.parseInt(meta.sdkInfo.get("minSdkVersion"));
        }
        return sdkNumber;
    }

    private int mapSdkShorthandToVersion(String sdkVersion) {
        switch (sdkVersion.toUpperCase()) {
            case "M":
                return ResConfigFlags.SDK_MNC;
            case "N":
                return ResConfigFlags.SDK_NOUGAT;
            case "O":
                return ResConfigFlags.SDK_OREO;
            case "P":
                return ResConfigFlags.SDK_P;
            case "Q":
                return ResConfigFlags.SDK_Q;
            case "R":
                return ResConfigFlags.SDK_R;
            case "S":
                return ResConfigFlags.SDK_S;
            case "SV2":
                return ResConfigFlags.SDK_S_V2;
            case "T":
            case "TIRAMISU":
                return ResConfigFlags.SDK_TIRAMISU;
            case "UPSIDEDOWNCAKE":
            case "UPSIDE_DOWN_CAKE":
            case "VANILLAICECREAM":
            case "VANILLA_ICE_CREAM":
                return ResConfigFlags.SDK_DEVELOPMENT;
            default:
                return Integer.parseInt(sdkVersion);
        }
    }

    public boolean detectWhetherAppIsFramework(File appDir)
            throws AndrolibException {
        File publicXml = new File(appDir, "res/values/public.xml");
        if (! publicXml.exists()) {
            return false;
        }

        Iterator<String> it;
        try {
            it = IOUtils.lineIterator(new FileReader(new File(appDir,
                    "res/values/public.xml")));
        } catch (FileNotFoundException ex) {
            throw new AndrolibException(
                    "Could not detect whether app is framework one", ex);
        }
        it.next();
        it.next();
        return it.next().contains("0x01");
    }

    public Duo<ResFileDecoder, AXmlResourceParser> getResFileDecoder() {
        ResStreamDecoderContainer decoders = new ResStreamDecoderContainer();
        decoders.setDecoder("raw", new ResRawStreamDecoder());
        decoders.setDecoder("9patch", new Res9patchStreamDecoder());

        AXmlResourceParser axmlParser = new AXmlResourceParser();
        axmlParser.setAttrDecoder(new ResAttrDecoder());
        decoders.setDecoder("xml", new XmlPullStreamDecoder(axmlParser, getResXmlSerializer()));

        return new Duo<>(new ResFileDecoder(decoders), axmlParser);
    }

    public Duo<ResFileDecoder, AXmlResourceParser> getManifestFileDecoder(boolean withResources) {
        ResStreamDecoderContainer decoders = new ResStreamDecoderContainer();

        AXmlResourceParser axmlParser = new AndroidManifestResourceParser();
        if (withResources) {
            axmlParser.setAttrDecoder(new ResAttrDecoder());
        }
        decoders.setDecoder("xml", new XmlPullStreamDecoder(axmlParser, getResXmlSerializer()));

        return new Duo<>(new ResFileDecoder(decoders), axmlParser);
    }

    public ExtMXSerializer getResXmlSerializer() {
        ExtMXSerializer serial = new ExtMXSerializer();
        serial.setProperty(ExtXmlSerializer.PROPERTY_SERIALIZER_INDENTATION, "    ");
        serial.setProperty(ExtXmlSerializer.PROPERTY_SERIALIZER_LINE_SEPARATOR, System.getProperty("line.separator"));
        serial.setProperty(ExtXmlSerializer.PROPERTY_DEFAULT_ENCODING, "utf-8");
        serial.setDisabledAttrEscape(true);
        return serial;
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

    private ResPackage[] getResPackagesFromApk(ExtFile apkFile, ResTable resTable, boolean keepBrokenResources)
            throws AndrolibException {
        try {
            Directory dir = apkFile.getDirectory();
            try (BufferedInputStream bfi = new BufferedInputStream(dir.getFileInput("resources.arsc"))) {
                return ARSCDecoder.decode(bfi, false, keepBrokenResources, resTable).getPackages();
            }
        } catch (DirectoryException | IOException ex) {
            throw new AndrolibException("Could not load resources.arsc from file: " + apkFile, ex);
        }
    }


    private File getAaptBinaryFile() throws AndrolibException {
        try {
            if (getAaptVersion() == 2) {
                return AaptManager.getAapt2();
            }
            return AaptManager.getAapt1();
        } catch (BrutException ex) {
            throw new AndrolibException(ex);
        }
    }

    private int getAaptVersion() {
        return config.isAapt2() ? 2 : 1;
    }

    public void close() throws IOException {
        if (mFramework != null) {
            mFramework.close();
        }
    }
}
