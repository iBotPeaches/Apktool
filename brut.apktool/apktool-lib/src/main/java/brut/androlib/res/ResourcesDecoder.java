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
import brut.androlib.res.util.ExtMXSerializer;
import brut.androlib.res.util.ExtXmlSerializer;
import brut.androlib.res.xml.ResValuesXmlSerializable;
import brut.androlib.res.xml.ResXmlPatcher;
import brut.directory.Directory;
import brut.directory.DirectoryException;
import brut.directory.ExtFile;
import brut.directory.FileDirectory;
import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.logging.Logger;

public class ResourcesDecoder {
    private final static Logger LOGGER = Logger.getLogger(ResourcesDecoder.class.getName());

    private final Config mConfig;
    private final ExtFile mApkFile;
    private final ResTable mResTable;
    private final ApkInfo mApkInfo;
    private final Map<String, String> mResFileMapping = new HashMap<>();

    private final static String[] IGNORED_PACKAGES = new String[] {
        "android", "com.htc", "com.lge", "com.lge.internal", "yi", "flyme", "air.com.adobe.appentry",
        "FFFFFFFFFFFFFFFFFFFFFF" };

    public ResourcesDecoder(Config config, ExtFile apkFile) {
        mConfig = config;
        mApkFile = apkFile;
        mApkInfo = new ApkInfo();
        mApkInfo.setApkFileName(apkFile.getName());
        mResTable = new ResTable(mConfig, mApkInfo);
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

    public ResTable getResTable() throws AndrolibException {
        if (! (hasManifest() || hasResources())) {
            throw new AndrolibException(
                "Apk doesn't contain either AndroidManifest.xml file or resources.arsc file");
        }
        return mResTable;
    }

    public ApkInfo getApkInfo() {
        return mApkInfo;
    }

    public  Map<String, String> getResFileMapping() {
        return mResFileMapping;
    }

    public void decodeManifest(File outDir) throws AndrolibException {
        if (hasManifest()) {
            decodeManifest(getResTable(), mApkFile, outDir);
            if (hasResources()) {
                if (!mConfig.analysisMode) {
                    // Remove versionName / versionCode (aapt API 16)
                    //
                    // check for a mismatch between resources.arsc package and the package listed in AndroidManifest
                    // also remove the android::versionCode / versionName from manifest for rebuild
                    // this is a required change to prevent aapt warning about conflicting versions
                    // it will be passed as a parameter to aapt like "--min-sdk-version" via apktool.yml
                    adjustPackageManifest(getResTable(), outDir.getAbsolutePath() + File.separator + "AndroidManifest.xml");

                    ResXmlPatcher.removeManifestVersions(new File(
                        outDir.getAbsolutePath() + File.separator + "AndroidManifest.xml"));

                    // update apk info
                    mApkInfo.packageInfo.forcedPackageId = String.valueOf(mResTable.getPackageId());
                }
            }
        }
    }

    public void updateApkInfo(File outDir) throws AndrolibException {
        mResTable.initApkInfo(mApkInfo, outDir);
    }

    private void decodeManifest(ResTable resTable, ExtFile apkFile, File outDir)
        throws AndrolibException {

        AXmlResourceParser axmlParser = new AndroidManifestResourceParser(resTable);
        XmlPullStreamDecoder fileDecoder = new XmlPullStreamDecoder(axmlParser, getResXmlSerializer());

        Directory inApk, out;
        try {
            inApk = apkFile.getDirectory();
            out = new FileDirectory(outDir);

            if (hasResources()) {
                LOGGER.info("Decoding AndroidManifest.xml with resources...");
            } else {
                LOGGER.info("Decoding AndroidManifest.xml with only framework resources...");
            }
            InputStream inputStream = inApk.getFileInput("AndroidManifest.xml");
            OutputStream outputStream = out.getFileOutput("AndroidManifest.xml");
            fileDecoder.decodeManifest(inputStream, outputStream);

        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    private void adjustPackageManifest(ResTable resTable, String filePath)
        throws AndrolibException {

        // compare resources.arsc package name to the one present in AndroidManifest
        ResPackage resPackage = resTable.getCurrentResPackage();
        String pkgOriginal = resPackage.getName();
        String pkgRenamed = resTable.getPackageRenamed();

        resTable.setPackageId(resPackage.getId());
        resTable.setPackageOriginal(pkgOriginal);

        // 1) Check if pkgOriginal is null (empty resources.arsc)
        // 2) Check if pkgRenamed is null
        // 3) Check if pkgOriginal === mPackageRenamed
        // 4) Check if pkgOriginal is ignored via IGNORED_PACKAGES
        if (pkgOriginal == null || pkgRenamed == null || pkgOriginal.equalsIgnoreCase(pkgRenamed)
            || (Arrays.asList(IGNORED_PACKAGES).contains(pkgOriginal))) {
            LOGGER.info("Regular manifest package...");
        } else {
            LOGGER.info("Renamed manifest package found! Replacing " + pkgRenamed + " with " + pkgOriginal);
            ResXmlPatcher.renameManifestPackage(new File(filePath), pkgOriginal);
        }
    }

    private ExtMXSerializer getResXmlSerializer() {
        ExtMXSerializer serial = new ExtMXSerializer();
        serial.setProperty(ExtXmlSerializer.PROPERTY_SERIALIZER_INDENTATION, "    ");
        serial.setProperty(ExtXmlSerializer.PROPERTY_SERIALIZER_LINE_SEPARATOR, System.getProperty("line.separator"));
        serial.setProperty(ExtXmlSerializer.PROPERTY_DEFAULT_ENCODING, "utf-8");
        serial.setDisabledAttrEscape(true);
        return serial;
    }

    public void loadMainPkg() throws AndrolibException {
        mResTable.loadMainPkg(mApkFile);
    }

    public ResTable decodeResources(File outDir) throws AndrolibException {
        if (hasResources()) {
            loadMainPkg();
            decodeResources(getResTable(), mApkFile, outDir);
        }
        return mResTable;
    }

    private void decodeResources(ResTable resTable, ExtFile apkFile, File outDir)
        throws AndrolibException {

        ResStreamDecoderContainer decoders = new ResStreamDecoderContainer();
        decoders.setDecoder("raw", new ResRawStreamDecoder());
        decoders.setDecoder("9patch", new Res9patchStreamDecoder());

        AXmlResourceParser axmlParser = new AXmlResourceParser(resTable);
        decoders.setDecoder("xml", new XmlPullStreamDecoder(axmlParser, getResXmlSerializer()));

        ResFileDecoder fileDecoder = new ResFileDecoder(decoders);
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

        AndrolibException decodeError = axmlParser.getFirstError();
        if (decodeError != null) {
            throw decodeError;
        }
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
}
