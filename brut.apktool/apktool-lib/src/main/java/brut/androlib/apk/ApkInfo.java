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
package brut.androlib.apk;

import brut.androlib.ApktoolProperties;
import brut.androlib.exceptions.AndrolibException;
import brut.androlib.res.data.ResConfigFlags;
import brut.directory.DirectoryException;
import brut.directory.ExtFile;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Pattern;

public class ApkInfo implements YamlSerializable {
    public static final String[] RESOURCES_DIRNAMES = { "res", "r", "R" };
    public static final String[] RAW_DIRNAMES = { "assets", "lib" };

    public static final Pattern ORIGINAL_FILENAMES_PATTERN = Pattern.compile(
        "AndroidManifest\\.xml|META-INF/[^/]+\\.(RSA|SF|MF)|stamp-cert-sha256");

    public static final Pattern STANDARD_FILENAMES_PATTERN = Pattern.compile(
        "[^/]+\\.dex|resources\\.arsc|(" + String.join("|", RESOURCES_DIRNAMES) + "|" +
        String.join("|", RAW_DIRNAMES) + ")/.*|" + ORIGINAL_FILENAMES_PATTERN.pattern());

    // only set when loaded from a file (not a stream)
    private transient ExtFile mApkFile;

    public String version;
    public String apkFileName;
    public boolean isFrameworkApk;
    public UsesFramework usesFramework;
    public Map<String, String> sdkInfo = new LinkedHashMap<>();
    public PackageInfo packageInfo = new PackageInfo();
    public VersionInfo versionInfo = new VersionInfo();
    public Map<String, Boolean> featureFlags = new LinkedHashMap<>();
    public boolean sharedLibrary;
    public boolean sparseResources;
    public boolean compactEntries;
    public List<String> doNotCompress = new ArrayList<>();

    public ApkInfo() {
        version = ApktoolProperties.getVersion();
    }

    public ApkInfo(ExtFile apkFile) {
        this();
        setApkFile(apkFile);
    }

    public ExtFile getApkFile() {
        return mApkFile;
    }

    public void setApkFile(ExtFile apkFile) {
        mApkFile = apkFile;
        if (apkFileName == null) {
            apkFileName = apkFile.getName();
        }
    }

    public boolean hasSources() throws AndrolibException {
        if (mApkFile == null) {
            return false;
        }
        try {
            return mApkFile.getDirectory().containsFile("classes.dex");
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    public boolean hasManifest() throws AndrolibException {
        if (mApkFile == null) {
            return false;
        }
        try {
            return mApkFile.getDirectory().containsFile("AndroidManifest.xml");
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    public boolean hasResources() throws AndrolibException {
        if (mApkFile == null) {
            return false;
        }
        try {
            return mApkFile.getDirectory().containsFile("resources.arsc");
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    public String checkTargetSdkVersionBounds() {
        int target = mapSdkShorthandToVersion(getTargetSdkVersion());
        int min = getMinSdkVersion() != null ? mapSdkShorthandToVersion(getMinSdkVersion()) : 0;
        int max = getMaxSdkVersion() != null ? mapSdkShorthandToVersion(getMaxSdkVersion()) : target;

        return Integer.toString(Math.max(min, Math.min(max, target)));
    }

    public String getMinSdkVersion() {
        return sdkInfo.get("minSdkVersion");
    }

    public void setMinSdkVersion(String minSdkVersion) {
        sdkInfo.put("minSdkVersion", minSdkVersion);
    }

    public String getMaxSdkVersion() {
        return sdkInfo.get("maxSdkVersion");
    }

    public void setMaxSdkVersion(String maxSdkVersion) {
        sdkInfo.put("maxSdkVersion", maxSdkVersion);
    }

    public String getTargetSdkVersion() {
        return sdkInfo.get("targetSdkVersion");
    }

    public void setTargetSdkVersion(String targetSdkVersion) {
        sdkInfo.put("targetSdkVersion", targetSdkVersion);
    }

    public int getMinSdkVersionFromAndroidCodename(String sdkVersion) {
        int sdkNumber = mapSdkShorthandToVersion(sdkVersion);

        if (sdkNumber == ResConfigFlags.SDK_BASE) {
            return Integer.parseInt(getMinSdkVersion());
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
                return ResConfigFlags.SDK_UPSIDEDOWN_CAKE;
            case "VANILLAICECREAM":
            case "VANILLA_ICE_CREAM":
                return ResConfigFlags.SDK_VANILLA_ICE_CREAM;
            case "BAKLAVA":
                return ResConfigFlags.SDK_BAKLAVA;
            case "SDK_CUR_DEVELOPMENT":
                return ResConfigFlags.SDK_DEVELOPMENT;
            default:
                return Integer.parseInt(sdkVersion);
        }
    }

    public void addFeatureFlag(String flag, boolean value) {
        featureFlags.put(flag, value);
    }

    public void save(File file) throws AndrolibException {
        try (YamlWriter writer = new YamlWriter(Files.newOutputStream(file.toPath()))) {
            write(writer);
        } catch (FileNotFoundException ex) {
            throw new AndrolibException("File not found");
        } catch (Exception ex) {
            throw new AndrolibException(ex);
        }
    }

    public static ApkInfo load(InputStream in) throws AndrolibException {
        YamlReader reader = new YamlReader(in);
        ApkInfo apkInfo = new ApkInfo();
        reader.readRoot(apkInfo);
        return apkInfo;
    }

    public static ApkInfo load(ExtFile apkDir) throws AndrolibException {
        try (InputStream in = apkDir.getDirectory().getFileInput("apktool.yml")) {
            ApkInfo apkInfo = ApkInfo.load(in);
            apkInfo.setApkFile(apkDir);
            return apkInfo;
        } catch (DirectoryException | IOException ex) {
            throw new AndrolibException(ex);
        }
    }

    @Override
    public void readItem(YamlReader reader) throws AndrolibException {
        YamlLine line = reader.getLine();
        switch (line.getKey()) {
            case "version": {
                version = line.getValue();
                break;
            }
            case "apkFileName": {
                apkFileName = line.getValue();
                break;
            }
            case "isFrameworkApk": {
                isFrameworkApk = line.getValueBool();
                break;
            }
            case "usesFramework": {
                usesFramework = new UsesFramework();
                reader.readObject(usesFramework);
                break;
            }
            case "sdkInfo": {
                sdkInfo.clear();
                reader.readStringMap(sdkInfo);
                break;
            }
            case "packageInfo": {
                packageInfo = new PackageInfo();
                reader.readObject(packageInfo);
                break;
            }
            case "versionInfo": {
                versionInfo = new VersionInfo();
                reader.readObject(versionInfo);
                break;
            }
            case "featureFlags": {
                featureFlags.clear();
                reader.readBoolMap(featureFlags);
                break;
            }
            case "sharedLibrary": {
                sharedLibrary = line.getValueBool();
                break;
            }
            case "sparseResources": {
                sparseResources = line.getValueBool();
                break;
            }
            case "compactEntries": {
                compactEntries = line.getValueBool();
                break;
            }
            case "doNotCompress": {
                doNotCompress.clear();
                reader.readStringList(doNotCompress);
                break;
            }
        }
    }

    @Override
    public void write(YamlWriter writer) {
        writer.writeString("version", version);
        writer.writeString("apkFileName", apkFileName);
        writer.writeBool("isFrameworkApk", isFrameworkApk);
        writer.writeObject("usesFramework", usesFramework);
        writer.writeMap("sdkInfo", sdkInfo);
        writer.writeObject("packageInfo", packageInfo);
        writer.writeObject("versionInfo", versionInfo);
        if (!featureFlags.isEmpty()) {
            writer.writeMap("featureFlags", featureFlags);
        }
        writer.writeBool("sharedLibrary", sharedLibrary);
        writer.writeBool("sparseResources", sparseResources);
        writer.writeBool("compactEntries", compactEntries);
        if (!doNotCompress.isEmpty()) {
            writer.writeList("doNotCompress", doNotCompress);
        }
    }
}
