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
import brut.androlib.meta.UsesFramework;
import brut.androlib.meta.VersionInfo;
import brut.androlib.res.decoder.*;
import brut.androlib.res.table.*;
import brut.androlib.res.table.value.ResBag;
import brut.androlib.res.table.value.ResFileReference;
import brut.androlib.res.xml.ResXmlUtils;
import brut.androlib.res.xml.ResXmlSerializer;
import brut.androlib.res.xml.ValuesXmlSerializable;
import brut.common.Log;
import brut.directory.Directory;
import brut.directory.DirectoryException;
import brut.directory.FileDirectory;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.tuple.Pair;

import java.io.*;
import java.util.*;

public class ResDecoder {
    private static final String TAG = ResDecoder.class.getName();

    private final ApkInfo mApkInfo;
    private final Config mConfig;
    private final ResTable mTable;
    private final Map<String, String> mResFileMap;

    public ResDecoder(ApkInfo apkInfo, Config config) {
        mApkInfo = apkInfo;
        mConfig = config;
        mTable = new ResTable(apkInfo, config);
        mResFileMap = new HashMap<>();
    }

    public ResTable getTable() {
        return mTable;
    }

    public Map<String, String> getResFileMap() {
        return mResFileMap;
    }

    public void decodeResources(File apkDir) throws AndrolibException {
        if (!mApkInfo.hasResources()) {
            return;
        }

        mTable.load();

        Map<ResFileDecoder.Type, ResStreamDecoder> decoders = new HashMap<>();
        decoders.put(ResFileDecoder.Type.UNKNOWN, new ResRawStreamDecoder());
        decoders.put(ResFileDecoder.Type.PNG_9PATCH, new ResNinePatchStreamDecoder());

        BinaryXmlResourceParser parser = new BinaryXmlResourceParser(
            mTable, mConfig.isIgnoreRawValues(), mConfig.isDecodeResolveLazy());
        ResXmlSerializer serial = new ResXmlSerializer(true);
        decoders.put(ResFileDecoder.Type.BINARY_XML, new ResXmlPullStreamDecoder(parser, serial));

        ResFileDecoder fileDecoder = new ResFileDecoder(decoders);
        Directory inDir, outDir;

        try {
            inDir = mApkInfo.getApkFile().getDirectory();
            outDir = new FileDirectory(apkDir);
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }

        ResPackage pkg = mTable.getMainPackage();

        Log.i(TAG, "Decoding value resources...");
        for (ResEntry entry : Lists.newArrayList(pkg.getGroup().listEntries())) {
            if (entry.getValue() instanceof ResBag) {
                ((ResBag) entry.getValue()).resolveKeys();
            }
        }

        Log.i(TAG, "Decoding file resources...");
        for (ResEntry entry : Lists.newArrayList(pkg.getGroup().listEntries())) {
            if (entry.getValue() instanceof ResFileReference) {
                fileDecoder.decode(entry, inDir, outDir, mResFileMap);
            }
        }

        // Disable auto-escaping in generated XMLs.
        serial = new ResXmlSerializer(false);

        Log.i(TAG, "Generating values XMLs...");
        generateValuesXmls(pkg, outDir, serial);
        generatePublicXml(pkg, outDir, serial);
        generateStagingXmls(pkg, outDir, serial);
        generateOverlayableXml(pkg, outDir, serial);

        AndrolibException ex = parser.getFirstError();
        if (ex != null) {
            throw ex;
        }
    }

    private void generateValuesXmls(ResPackage pkg, Directory outDir, ResXmlSerializer serial)
            throws AndrolibException {
        // Group entries by type name + qualifiers, ignoring alias duplicates in sub-packages.
        Map<Pair<String, String>, List<ResEntry>> entriesMap = new HashMap<>();
        for (ResEntry entry : pkg.getGroup().listEntries()) {
            if (entry.getValue() instanceof ValuesXmlSerializable && !pkg.isAlias(entry.getResId())) {
                ResType type = entry.getType();
                Pair<String, String> key = Pair.of(type.getName(), type.getConfig().toQualifiers());
                entriesMap.computeIfAbsent(key, t -> new ArrayList<>()).add(entry);
            }
        }

        // Generate a values XML per type name + qualifiers.
        for (Map.Entry<Pair<String, String>, List<ResEntry>> mapEntry : entriesMap.entrySet()) {
            Pair<String, String> key = mapEntry.getKey();
            String typeName = key.getLeft();
            String qualifiers = key.getRight();

            List<ResEntry> entries = mapEntry.getValue();
            entries.sort(Comparator.comparing(ResEntry::getResId));

            String outFileName = "res/values" + qualifiers + "/"
                               + (typeName.endsWith("s") ? typeName : typeName + "s") + ".xml";
            try (OutputStream out = outDir.getFileOutput(outFileName)) {
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
                throw new AndrolibException("Could not generate: " + outFileName, ex);
            }
        }
    }

    private void generatePublicXml(ResPackage pkg, Directory outDir, ResXmlSerializer serial)
            throws AndrolibException {
        List<ResEntrySpec> specs = Lists.newArrayList(pkg.listEntrySpecs());
        specs.sort(Comparator.comparing(ResEntrySpec::getResId));

        String outFileName = "res/values/public.xml";
        try (OutputStream out = outDir.getFileOutput(outFileName)) {
            serial.setOutput(out, null);
            serial.startDocument(null, null);
            serial.startTag(null, "resources");

            for (ResEntrySpec spec : specs) {
                serial.startTag(null, "public");
                serial.attribute(null, "type", spec.getTypeSpec().getName());
                serial.attribute(null, "name", spec.getName());
                serial.attribute(null, "id", spec.getResId().toString());
                serial.endTag(null, "public");
            }

            serial.endTag(null, "resources");
            serial.endDocument();
            serial.flush();
        } catch (DirectoryException | IOException ex) {
            throw new AndrolibException("Could not generate: " + outFileName, ex);
        }
    }

    private void generateStagingXmls(ResPackage pkg, Directory outDir, ResXmlSerializer serial)
            throws AndrolibException {
        if (pkg.getGroup().getPackageCount() <= 1) {
            return;
        }

        // Collect and sort all entry specs defined in sub-packages.
        List<ResPackage> subPkgs = Lists.newArrayList(pkg.getGroup().listSubPackages());
        List<ResEntrySpec> subSpecs = new ArrayList<>();
        for (ResPackage subPkg : subPkgs) {
            subSpecs.addAll(subPkg.listEntrySpecs());
        }
        subSpecs.sort(Comparator.comparing(ResEntrySpec::getResId));

        // Separate the entry specs into groups by type.
        Map<ResTypeSpec, List<ResEntrySpec>> stagingGroups = new LinkedHashMap<>();
        Map<ResTypeSpec, List<ResEntrySpec>> finalGroups = new LinkedHashMap<>();
        Map<ResTypeSpec, List<ResEntrySpec>> prevMap = null;
        List<ResEntrySpec> currList = null;
        ResId prevId = null;
        for (ResEntrySpec spec : subSpecs) {
            ResId currId = spec.getResId();
            Map<ResTypeSpec, List<ResEntrySpec>> currMap = pkg.isAlias(currId) ? finalGroups : stagingGroups;

            if (prevId == null || currId.typeId() != prevId.typeId() || currMap != prevMap) {
                currList = new ArrayList<>();
                currMap.put(spec.getTypeSpec(), currList);
                prevMap = currMap;
            }

            currList.add(spec);
            prevId = currId;
        }

        // Generate values XML for the staging groups.
        if (!stagingGroups.isEmpty()) {
            String outFileName = "res/values/public-staging.xml";
            try (OutputStream out = outDir.getFileOutput(outFileName)) {
                serial.setOutput(out, null);
                serial.startDocument(null, null);
                serial.startTag(null, "resources");

                for (Map.Entry<ResTypeSpec, List<ResEntrySpec>> mapEntry : stagingGroups.entrySet()) {
                    ResTypeSpec typeSpec = mapEntry.getKey();
                    ResId firstId = ResId.of(typeSpec.getPackage().getId(), typeSpec.getId(), 0);

                    serial.startTag(null, "staging-public-group");
                    serial.attribute(null, "type", mapEntry.getKey().getName());
                    serial.attribute(null, "first-id", firstId.toString());

                    int lastEntryId = 0;
                    for (ResEntrySpec spec : mapEntry.getValue()) {
                        int entryId = spec.getId();
                        while (lastEntryId++ < entryId) {
                            serial.startTag(null, "public");
                            serial.attribute(null, "name", "removed_");
                            serial.endTag(null, "public");
                        }

                        serial.startTag(null, "public");
                        serial.attribute(null, "name", spec.getName());
                        serial.endTag(null, "public");
                    }

                    serial.endTag(null, "staging-public-group");
                }

                serial.endTag(null, "resources");
                serial.endDocument();
                serial.flush();
            } catch (DirectoryException | IOException ex) {
                throw new AndrolibException("Could not generate: " + outFileName, ex);
            }
        }

        // Generate values XML for the finalized groups.
        if (!finalGroups.isEmpty()) {
            String outFileName = "res/values/public-final.xml";
            try (OutputStream out = outDir.getFileOutput(outFileName)) {
                serial.setOutput(out, null);
                serial.startDocument(null, null);
                serial.startTag(null, "resources");

                for (Map.Entry<ResTypeSpec, List<ResEntrySpec>> mapEntry : finalGroups.entrySet()) {
                    ResTypeSpec typeSpec = mapEntry.getKey();
                    ResId firstId = ResId.of(typeSpec.getPackage().getId(), typeSpec.getId(), 0);

                    serial.startTag(null, "staging-public-group-final");
                    serial.attribute(null, "type", mapEntry.getKey().getName());
                    serial.attribute(null, "first-id", firstId.toString());

                    int lastEntryId = 0;
                    for (ResEntrySpec spec : mapEntry.getValue()) {
                        int entryId = spec.getId();
                        while (lastEntryId++ < entryId) {
                            serial.startTag(null, "public");
                            serial.attribute(null, "name", "removed_");
                            serial.endTag(null, "public");
                        }

                        serial.startTag(null, "public");
                        serial.attribute(null, "name", spec.getName());
                        serial.endTag(null, "public");
                    }

                    serial.endTag(null, "staging-public-group-final");
                }

                serial.endTag(null, "resources");
                serial.endDocument();
                serial.flush();
            } catch (DirectoryException | IOException ex) {
                throw new AndrolibException("Could not generate: " + outFileName, ex);
            }
        }
    }

    private void generateOverlayableXml(ResPackage pkg, Directory outDir, ResXmlSerializer serial)
            throws AndrolibException {
        if (pkg.getOverlayableCount() == 0) {
            return;
        }

        List<ResOverlayable> overlayables = Lists.newArrayList(pkg.listOverlayables());
        overlayables.sort(Comparator.comparing(ResOverlayable::getName));

        String outFileName = "res/values/overlayable.xml";
        try (OutputStream out = outDir.getFileOutput(outFileName)) {
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
            throw new AndrolibException("Could not generate: " + outFileName, ex);
        }
    }

    public void decodeManifest(File apkDir) throws AndrolibException {
        if (!mApkInfo.hasManifest()) {
            return;
        }

        BinaryXmlResourceParser parser = new BinaryXmlResourceParser(
            mTable, mConfig.isIgnoreRawValues(), mConfig.isDecodeResolveLazy());
        ResXmlSerializer serial = new ResXmlSerializer(true);
        ManifestPullEventHandler handler = new ManifestPullEventHandler(mApkInfo, !mConfig.isAnalysisMode());
        ResXmlPullStreamDecoder decoder = new ResXmlPullStreamDecoder(parser, serial, handler);

        ResPackage pkg = mTable.getMainPackage();

        Directory inDir, outDir;
        try {
            inDir = mApkInfo.getApkFile().getDirectory();
            outDir = new FileDirectory(apkDir);

            Log.i(TAG, "Decoding AndroidManifest.xml with " + (pkg != null ? "resources" : "only framework resources")
                     + "...");

            try (
                InputStream in = inDir.getFileInput("AndroidManifest.xml");
                OutputStream out = outDir.getFileOutput("AndroidManifest.xml")
            ) {
                decoder.decode(in, out);
            }
        } catch (DirectoryException | IOException ex) {
            throw new AndrolibException(ex);
        }

        // Update apk info.
        ResourcesInfo resourcesInfo = mApkInfo.getResourcesInfo();

        // Flag the app if it preserved raw attribute values.
        if (parser.hasRawValues() && !mConfig.isIgnoreRawValues()) {
            resourcesInfo.setKeepRawValues(true);
        }

        if (pkg != null) {
            resourcesInfo.setPackageId(pkg.getId());

            // We temporarily filled packageName in ResourcesInfo with the package from AndroidManifest.xml.
            // Check if actual resources package differs from the manifest package and update packageName,
            // otherwise clear packageName.
            String manifestPackage = resourcesInfo.getPackageName();
            String resourcesPackage = pkg.getName();
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

            // Record framework package IDs used by the resource table.
            List<Integer> framePackageIds = Lists.newArrayList(mTable.getFramePackageIds());
            if (!framePackageIds.isEmpty()) {
                UsesFramework usesFramework = mApkInfo.getUsesFramework();
                List<Integer> frameworkIds = usesFramework.getIds();
                framePackageIds.sort(null);
                for (int id : framePackageIds) {
                    frameworkIds.add(id);
                }
                usesFramework.setTag(mConfig.getFrameworkTag());
            }

            // Record library package names used by the resource table.
            List<Integer> libPackageIds = Lists.newArrayList(mTable.getLibPackageIds());
            if (!libPackageIds.isEmpty()) {
                List<String> usesLibrary = mApkInfo.getUsesLibrary();
                libPackageIds.sort(null);
                for (int id : libPackageIds) {
                    usesLibrary.add(mTable.getDynamicRefPackageName(id));
                }
            }
        } else {
            // Renaming not possible: manifest decoded without resources.
            resourcesInfo.setPackageName(null);
        }

        File manifest = new File(apkDir, "AndroidManifest.xml");

        if (!mConfig.isAnalysisMode()) {
            // Remove versionCode and versionName, it will be passed to aapt as a parameter via apktool.yml.
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
