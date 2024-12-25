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
import brut.androlib.apk.ApkInfo;
import brut.androlib.exceptions.AndrolibException;
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

    public void loadMainPkg() throws AndrolibException {
        mResTable.loadMainPkg(mApkInfo.getApkFile());
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

            if (mApkInfo.hasResources()) {
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

        if (mApkInfo.hasResources() && !mConfig.isAnalysisMode()) {
            // Remove versionName / versionCode (aapt API 16)
            //
            // check for a mismatch between resources.arsc package and the package listed in AndroidManifest
            // also remove the android::versionCode / versionName from manifest for rebuild
            // this is a required change to prevent aapt warning about conflicting versions
            // it will be passed as a parameter to aapt like "--min-sdk-version" via apktool.yml
            adjustPackageManifest(manifest);

            ResXmlUtils.removeManifestVersions(manifest);

            // update apk info
            mApkInfo.packageInfo.forcedPackageId = String.valueOf(mResTable.getPackageId());
        }

        // record feature flags
        String[] featureFlags = ResXmlUtils.pullManifestFeatureFlags(manifest);
        if (featureFlags != null) {
            for (String flag : featureFlags) {
                mApkInfo.addFeatureFlag(flag, true);
            }
        }
    }

    public void updateApkInfo(File apkDir) throws AndrolibException {
        mResTable.initApkInfo(mApkInfo, apkDir);
    }

    private void adjustPackageManifest(File manifest) throws AndrolibException {
        // compare resources.arsc package name to the one present in AndroidManifest
        ResPackage resPackage = mResTable.getCurrentResPackage();
        String pkgOriginal = resPackage.getName();
        String pkgRenamed = mResTable.getPackageRenamed();

        mResTable.setPackageId(resPackage.getId());
        mResTable.setPackageOriginal(pkgOriginal);

        // 1) Check if pkgOriginal is null (empty resources.arsc)
        // 2) Check if pkgRenamed is null
        // 3) Check if pkgOriginal === mPackageRenamed
        // 4) Check if pkgOriginal is ignored via IGNORED_PACKAGES
        if (pkgOriginal == null || pkgRenamed == null || pkgOriginal.equals(pkgRenamed)
                || IGNORED_PACKAGES.contains(pkgOriginal)) {
            LOGGER.info("Regular manifest package...");
        } else {
            LOGGER.info("Renamed manifest package found! Replacing " + pkgRenamed + " with " + pkgOriginal);
            ResXmlUtils.renameManifestPackage(manifest, pkgOriginal);
        }
    }

    public void decodeResources(File apkDir) throws AndrolibException {
        if (!mApkInfo.hasResources()) {
            return;
        }

        loadMainPkg();

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

        for (ResPackage pkg : mResTable.listMainPackages()) {
            LOGGER.info("Decoding file-resources...");
            for (ResResource res : pkg.listFiles()) {
                fileDecoder.decode(res, inDir, outDir, mResFileMapping);
            }

            LOGGER.info("Decoding values */* XMLs...");
            for (ResValuesFile valuesFile : pkg.listValuesFiles()) {
                generateValuesFile(valuesFile, outDir, xmlSerializer);
            }

            generatePublicXml(pkg, outDir, xmlSerializer);
        }

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
}
