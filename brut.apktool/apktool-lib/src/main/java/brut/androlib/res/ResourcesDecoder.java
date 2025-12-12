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
import brut.androlib.meta.ResourcesInfo;
import brut.androlib.meta.SdkInfo;
import brut.androlib.meta.VersionInfo;
import brut.androlib.res.decoder.*;
import brut.androlib.res.table.*;
import brut.androlib.res.table.value.ResBag;
import brut.androlib.res.table.value.ResFileReference;
import brut.androlib.res.xml.ResXmlUtils;
import brut.androlib.res.xml.ValuesXmlSerializable;
import brut.directory.Directory;
import brut.directory.DirectoryException;
import brut.directory.ExtFile;
import brut.xmlpull.MXSerializer;
import org.xmlpull.v1.XmlSerializer;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;

public class ResourcesDecoder {
    private static final Logger LOGGER = Logger.getLogger(ResourcesDecoder.class.getName());

    private final ApkInfo mApkInfo;
    private final Config mConfig;
    private final ResTable mTable;
    private final Map<String, String> mResFileMapping;

    public ResourcesDecoder(ApkInfo apkInfo, Config config) {
        mApkInfo = apkInfo;
        mConfig = config;
        mTable = new ResTable(apkInfo, config);
        mResFileMapping = new HashMap<>();
    }

    public ResTable getTable() {
        return mTable;
    }

    public Map<String, String> getResFileMapping() {
        return mResFileMapping;
    }

    public void decodeResources(File apkDir) throws AndrolibException {
        if (!mApkInfo.hasResources()) {
            return;
        }

        mTable.loadMainPackage();

        Map<ResFileDecoder.Type, ResStreamDecoder> decoders = new HashMap<>();
        decoders.put(ResFileDecoder.Type.UNKNOWN, new ResRawStreamDecoder());
        decoders.put(ResFileDecoder.Type.PNG_9PATCH, new ResNinePatchStreamDecoder());

        BinaryXmlResourceParser parser = new BinaryXmlResourceParser(mTable);
        XmlSerializer serial = newXmlSerializer();
        decoders.put(ResFileDecoder.Type.BINARY_XML, new ResXmlPullStreamDecoder(parser, serial));

        ResFileDecoder fileDecoder = new ResFileDecoder(decoders);
        Directory inDir, outDir;

        try {
            inDir = mApkInfo.getApkFile().getDirectory();
            outDir = new ExtFile(apkDir).getDirectory();
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }

        ResPackage pkg = mTable.getMainPackage();

        LOGGER.info("Decoding value resources...");
        for (ResEntry entry : new ArrayList<>(pkg.listEntries())) {
            if (entry.getValue() instanceof ResBag) {
                ((ResBag) entry.getValue()).resolveKeys();
            }
        }

        LOGGER.info("Decoding file resources...");
        for (ResEntry entry : new ArrayList<>(pkg.listEntries())) {
            if (entry.getValue() instanceof ResFileReference) {
                fileDecoder.decode(entry, inDir, outDir, mResFileMapping);
            }
        }

        LOGGER.info("Generating values XMLs...");
        Map<ResType, List<ResEntry>> valuesEntries = new HashMap<>();
        for (ResEntry entry : pkg.listEntries()) {
            if (entry.getValue() instanceof ValuesXmlSerializable) {
                ResType type = entry.getType();
                List<ResEntry> entries = valuesEntries.get(type);
                if (entries == null) {
                    entries = new ArrayList<>();
                    valuesEntries.put(type, entries);
                }
                entries.add(entry);
            }
        }
        for (Map.Entry<ResType, List<ResEntry>> entry : valuesEntries.entrySet()) {
            generateValuesXml(pkg, entry.getKey(), entry.getValue(), outDir, serial);
        }

        generatePublicXml(pkg, outDir, serial);
        generateOverlayableXml(pkg, outDir, serial);

        AndrolibException ex = parser.getFirstError();
        if (ex != null) {
            throw ex;
        }
    }

    private XmlSerializer newXmlSerializer() throws AndrolibException {
        try {
            XmlSerializer serial = new MXSerializer();
            serial.setFeature(MXSerializer.FEATURE_ATTR_VALUE_NO_ESCAPE, true);
            serial.setProperty(MXSerializer.PROPERTY_DEFAULT_ENCODING, "utf-8");
            serial.setProperty(MXSerializer.PROPERTY_INDENTATION, "    ");
            serial.setProperty(MXSerializer.PROPERTY_LINE_SEPARATOR, System.lineSeparator());
            return serial;
        } catch (IllegalArgumentException | IllegalStateException ex) {
            throw new AndrolibException(ex);
        }
    }

    private void generateValuesXml(ResPackage pkg, ResType type, List<ResEntry> entries,
                                   Directory outDir, XmlSerializer serial) throws AndrolibException {
        String path = "res/values" + type.getConfig().getQualifiers() + "/"
                + type.getName() + (type.getName().endsWith("s") ? "" : "s") + ".xml";
        entries.sort(Comparator.comparing(ResEntry::getId));

        try (OutputStream out = outDir.getFileOutput(path)) {
            serial.setOutput(out, null);
            serial.startDocument(null, null);
            serial.startTag(null, "resources");

            for (ResEntry entry : entries) {
                // We already verified the value, cast directly.
                ((ValuesXmlSerializable) entry.getValue()).serializeToValuesXml(serial, entry);
            }

            serial.endTag(null, "resources");
            serial.endDocument();
            serial.flush();
        } catch (DirectoryException | IOException ex) {
            throw new AndrolibException("Could not generate: " + path, ex);
        }
    }

    private void generatePublicXml(ResPackage pkg, Directory outDir, XmlSerializer serial)
            throws AndrolibException {
        String path = "res/values/public.xml";
        List<ResEntrySpec> specs = new ArrayList<>(pkg.listEntrySpecs());
        specs.sort(Comparator.comparing(ResEntrySpec::getId));

        try (OutputStream out = outDir.getFileOutput(path)) {
            serial.setOutput(out, null);
            serial.startDocument(null, null);
            serial.startTag(null, "resources");

            for (ResEntrySpec spec : specs) {
                serial.startTag(null, "public");
                serial.attribute(null, "type", spec.getTypeName());
                serial.attribute(null, "name", spec.getName());
                serial.attribute(null, "id", spec.getId().toString());
                serial.endTag(null, "public");
            }

            serial.endTag(null, "resources");
            serial.endDocument();
            serial.flush();
        } catch (DirectoryException | IOException ex) {
            throw new AndrolibException("Could not generate: " + path, ex);
        }
    }

    private void generateOverlayableXml(ResPackage pkg, Directory outDir, XmlSerializer serial)
            throws AndrolibException {
        List<ResOverlayable> overlayables = new ArrayList<>(pkg.listOverlayables());
        if (overlayables.isEmpty()) {
            return;
        }

        String path = "res/values/overlayable.xml";
        overlayables.sort(Comparator.comparing(ResOverlayable::getName));

        try (OutputStream out = outDir.getFileOutput(path)) {
            serial.setOutput(out, null);
            serial.startDocument(null, null);
            serial.startTag(null, "resources");

            for (ResOverlayable overlayable : overlayables) {
                overlayable.serializeToXml(serial);
            }

            serial.endTag(null, "resources");
            serial.endDocument();
            serial.flush();
        } catch (DirectoryException | IOException ex) {
            throw new AndrolibException("Could not generate: " + path, ex);
        }
    }

    public void decodeManifest(File apkDir) throws AndrolibException {
        if (!mApkInfo.hasManifest()) {
            return;
        }

        BinaryXmlResourceParser parser = new BinaryXmlResourceParser(mTable);
        XmlSerializer serial = newXmlSerializer();
        ResStreamDecoder decoder = new AndroidManifestPullStreamDecoder(parser, serial);

        Directory inDir, outDir;
        try {
            inDir = mApkInfo.getApkFile().getDirectory();
            outDir = new ExtFile(apkDir).getDirectory();

            LOGGER.info("Decoding AndroidManifest.xml with "
                    + (mTable.isMainPackageLoaded() ? "resources" : "only framework resources") + "...");

            try (
                InputStream in = inDir.getFileInput("AndroidManifest.xml");
                OutputStream out = outDir.getFileOutput("AndroidManifest.xml")
            ) {
                decoder.decode(in, out);
            }
        } catch (DirectoryException | IOException ex) {
            throw new AndrolibException(ex);
        }

        File manifest = new File(apkDir, "AndroidManifest.xml");

        if (mTable.isMainPackageLoaded()) {
            mTable.updateApkInfo();

            // We temporarily filled packageName in ResourcesInfo with the package from
            // AndroidManifest.xml. Check if actual resources package differs from the
            // manifest package and update packageName, otherwise clear packageName.
            ResourcesInfo resourcesInfo = mApkInfo.getResourcesInfo();
            String manifestPackage = resourcesInfo.getPackageName();
            String resourcesPackage = mTable.getMainPackage().getName();
            if (resourcesPackage != null && !resourcesPackage.equals(manifestPackage)) {
                resourcesInfo.setPackageName(resourcesPackage);
            } else {
                // Renaming not needed: resources package is null or identical.
                resourcesInfo.setPackageName(null);
            }

            // Resolve sdkInfo from resources.
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

            // Resolve versionInfo from resources.
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
        } else {
            // Renaming not possible: manifest decoded without resources.
            mApkInfo.getResourcesInfo().setPackageName(null);
        }

        if (!mConfig.isAnalysisMode()) {
            // Remove versionCode and versionName, it will be passed to aapt as a parameter
            // via apktool.yml.
            ResXmlUtils.removeManifestVersions(manifest);
        }

        // Record feature flags.
        String[] flags = ResXmlUtils.pullManifestFeatureFlags(manifest);
        if (flags != null) {
            Map<String, Boolean> featureFlags = mApkInfo.getFeatureFlags();
            for (String flag : flags) {
                boolean value;
                if (flag.startsWith("!")) {
                    flag = flag.substring(1);
                    value = false;
                } else {
                    value = true;
                }
                featureFlags.put(flag, value);
            }
        }
    }
}
