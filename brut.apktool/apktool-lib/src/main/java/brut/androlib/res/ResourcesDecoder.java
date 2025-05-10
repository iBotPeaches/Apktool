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

import brut.androlib.Config;
import brut.androlib.exceptions.AndrolibException;
import brut.androlib.meta.ApkInfo;
import brut.androlib.meta.PackageInfo;
import brut.androlib.meta.SdkInfo;
import brut.androlib.meta.VersionInfo;
import brut.androlib.res.data.*;
import brut.androlib.res.decoder.*;
import brut.androlib.res.xml.ResValuesXmlSerializable;
import brut.androlib.res.xml.ResXmlUtils;
import brut.directory.Directory;
import brut.directory.DirectoryException;
import brut.directory.ExtFile;
import brut.xmlpull.MXSerializer;
import com.google.common.collect.Sets;
import org.xmlpull.v1.XmlSerializer;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;

public class ResourcesDecoder {
    private static final Logger LOGGER = Logger.getLogger(ResourcesDecoder.class.getName());

    private static final Set<String> IGNORED_PACKAGES = Sets.newHashSet(
        "android", "com.htc", "com.lge", "com.lge.internal", "yi", "flyme", "air.com.adobe.appentry",
        "FFFFFFFFFFFFFFFFFFFFFF"
    );

    private final ApkInfo mApkInfo;
    private final Config mConfig;
    private final ResTable mResTable;
    private final Map<String, String> mResFileMapping;

    public ResourcesDecoder(ApkInfo apkInfo, Config config) {
        mApkInfo = apkInfo;
        mConfig = config;
        mResTable = new ResTable(apkInfo, config);
        mResFileMapping = new HashMap<>();
    }

    public ResTable getResTable() throws AndrolibException {
        if (!mApkInfo.hasManifest() && !mApkInfo.hasResources()) {
            throw new AndrolibException(
                "Apk doesn't contain either AndroidManifest.xml file or resources.arsc file");
        }
        return mResTable;
    }

    public Map<String, String> getResFileMapping() {
        return mResFileMapping;
    }

    public void decodeResources(File apkDir) throws AndrolibException {
        if (!mApkInfo.hasResources()) {
            return;
        }

        mResTable.loadMainPackage();

        ResStreamDecoderContainer decoders = new ResStreamDecoderContainer();
        decoders.setDecoder("raw", new ResRawStreamDecoder());
        decoders.setDecoder("9patch", new Res9patchStreamDecoder());

        AXmlResourceParser axmlParser = new AXmlResourceParser(mResTable);
        XmlSerializer xmlSerializer = newXmlSerializer();
        decoders.setDecoder("xml", new ResXmlPullStreamDecoder(axmlParser, xmlSerializer));

        ResFileDecoder fileDecoder = new ResFileDecoder(decoders);
        Directory inDir, outDir;

        try {
            inDir = mApkInfo.getApkFile().getDirectory();
            outDir = new ExtFile(apkDir).getDirectory().createDir("res");
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }

        ResPackage pkg = mResTable.getMainPackage();

        LOGGER.info("Decoding file-resources...");
        for (ResResource res : pkg.listFiles()) {
            fileDecoder.decode(res, inDir, outDir, mResFileMapping);
        }

        LOGGER.info("Decoding values */* XMLs...");
        for (ResValuesFile valuesFile : pkg.listValuesFiles()) {
            generateValuesFile(valuesFile, outDir, xmlSerializer);
        }

        generatePublicXml(pkg, outDir, xmlSerializer);

        AndrolibException decodeError = axmlParser.getFirstError();
        if (decodeError != null) {
            throw decodeError;
        }
    }

    private XmlSerializer newXmlSerializer() throws AndrolibException {
        try {
            XmlSerializer serial = new MXSerializer();
            serial.setFeature(MXSerializer.FEATURE_ATTR_VALUE_NO_ESCAPE, true);
            serial.setProperty(MXSerializer.PROPERTY_DEFAULT_ENCODING, "utf-8");
            serial.setProperty(MXSerializer.PROPERTY_INDENTATION, "    ");
            serial.setProperty(MXSerializer.PROPERTY_LINE_SEPARATOR, System.getProperty("line.separator"));
            return serial;
        } catch (IllegalArgumentException | IllegalStateException ex) {
            throw new AndrolibException(ex);
        }
    }

    private void generateValuesFile(ResValuesFile valuesFile, Directory resDir, XmlSerializer serial)
            throws AndrolibException {
        try (OutputStream out = resDir.getFileOutput(valuesFile.getPath())) {
            serial.setOutput(out, null);
            serial.startDocument(null, null);
            serial.startTag(null, "resources");

            for (ResResource res : valuesFile.listResources()) {
                if (valuesFile.isSynthesized(res)) {
                    continue;
                }
                ((ResValuesXmlSerializable) res.getValue()).serializeToResValuesXml(serial, res);
            }

            serial.endTag(null, "resources");
            serial.endDocument();
            serial.flush();
        } catch (DirectoryException | IOException ex) {
            throw new AndrolibException("Could not generate: " + valuesFile.getPath(), ex);
        }
    }

    private void generatePublicXml(ResPackage pkg, Directory resDir, XmlSerializer serial)
            throws AndrolibException {
        try (OutputStream out = resDir.getFileOutput("values/public.xml")) {
            serial.setOutput(out, null);
            serial.startDocument(null, null);
            serial.startTag(null, "resources");

            List<ResResSpec> specs = pkg.listResSpecs();
            specs.sort(Comparator.comparing(ResResSpec::getId));

            for (ResResSpec spec : specs) {
                serial.startTag(null, "public");
                serial.attribute(null, "type", spec.getType().getName());
                serial.attribute(null, "name", spec.getName());
                serial.attribute(null, "id", spec.getId().toString());
                serial.endTag(null, "public");
            }

            serial.endTag(null, "resources");
            serial.endDocument();
            serial.flush();
        } catch (DirectoryException | IOException ex) {
            throw new AndrolibException("Could not generate public.xml file", ex);
        }
    }

    public void decodeManifest(File apkDir) throws AndrolibException {
        if (!mApkInfo.hasManifest()) {
            return;
        }

        AXmlResourceParser axmlParser = new AndroidManifestResourceParser(mResTable);
        XmlSerializer xmlSerializer = newXmlSerializer();
        ResStreamDecoder fileDecoder = new AndroidManifestPullStreamDecoder(axmlParser, xmlSerializer);

        Directory inDir, outDir;
        try {
            inDir = mApkInfo.getApkFile().getDirectory();
            outDir = new ExtFile(apkDir).getDirectory();

            if (mResTable.isMainPackageLoaded()) {
                LOGGER.info("Decoding AndroidManifest.xml with resources...");
            } else {
                LOGGER.info("Decoding AndroidManifest.xml with only framework resources...");
            }

            try (
                InputStream in = inDir.getFileInput("AndroidManifest.xml");
                OutputStream out = outDir.getFileOutput("AndroidManifest.xml")
            ) {
                fileDecoder.decode(in, out);
            }
        } catch (DirectoryException | IOException ex) {
            throw new AndrolibException(ex);
        }

        File manifest = new File(apkDir, "AndroidManifest.xml");

        if (mResTable.isMainPackageLoaded()) {
            mResTable.updateApkInfo();

            // resolve sdkInfo from resources
            SdkInfo sdkInfo = mApkInfo.getSdkInfo();
            if (!sdkInfo.isEmpty()) {
                String minSdkVersion = sdkInfo.getMinSdkVersion();
                if (minSdkVersion != null) {
                    String refValue = ResXmlUtils.pullValueFromIntegers(apkDir, minSdkVersion);
                    if (refValue != null) {
                        sdkInfo.setMinSdkVersion(refValue);
                    }
                }
                String targetSdkVersion = sdkInfo.getTargetSdkVersion();
                if (targetSdkVersion != null) {
                    String refValue = ResXmlUtils.pullValueFromIntegers(apkDir, targetSdkVersion);
                    if (refValue != null) {
                        sdkInfo.setTargetSdkVersion(refValue);
                    }
                }
                String maxSdkVersion = sdkInfo.getMaxSdkVersion();
                if (maxSdkVersion != null) {
                    String refValue = ResXmlUtils.pullValueFromIntegers(apkDir, maxSdkVersion);
                    if (refValue != null) {
                        sdkInfo.setMaxSdkVersion(refValue);
                    }
                }
            }

            // resolve versionInfo from resources
            VersionInfo versionInfo = mApkInfo.getVersionInfo();
            if (!versionInfo.isEmpty()) {
                String versionName = versionInfo.getVersionName();
                if (versionName != null) {
                    String refValue = ResXmlUtils.pullValueFromStrings(apkDir, versionName);
                    if (refValue != null) {
                        versionInfo.setVersionName(refValue);
                    }
                }
            }

            if (!mConfig.isAnalysisMode()) {
                // check if manifest package has to be renamed to main resource package
                PackageInfo packageInfo = mApkInfo.getPackageInfo();
                String manifestPackage = packageInfo.getRenameManifestPackage();
                String mainResPackage = mResTable.getMainPackage().getName();

                if (mainResPackage != null && !mainResPackage.equals(manifestPackage)
                        && !IGNORED_PACKAGES.contains(mainResPackage)) {
                    LOGGER.info("Renaming manifest package from " + manifestPackage + " to " + mainResPackage + "...");
                    ResXmlUtils.renameManifestPackage(manifest, mainResPackage);
                } else {
                    // renaming not needed: main resource package is null/identical/ignored
                    packageInfo.setRenameManifestPackage(null);
                }
            }
        } else {
            // renaming not possible: manifest decoded without resources
            mApkInfo.getPackageInfo().setRenameManifestPackage(null);
        }

        if (!mConfig.isAnalysisMode()) {
            // remove versionCode and versionName,
            // it will be passed as a parameter to aapt via apktool.yml
            ResXmlUtils.removeManifestVersions(manifest);
        }

        // record feature flags
        String[] flags = ResXmlUtils.pullManifestFeatureFlags(manifest);
        if (flags != null) {
            Map<String, Boolean> featureFlags = mApkInfo.getFeatureFlags();
            for (String flag : flags) {
                featureFlags.put(flag, true);
            }
        }
    }
}
