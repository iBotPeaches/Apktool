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
import brut.directory.FileDirectory;

import java.io.*;
import java.util.*;

public class ApkInfo implements YamlSerializable {
    private transient ExtFile mApkFile;

    public String version;
    public String apkFileName;
    public boolean isFrameworkApk;
    public UsesFramework usesFramework;
    private Map<String, String> sdkInfo = new LinkedHashMap<>();
    public PackageInfo packageInfo = new PackageInfo();
    public VersionInfo versionInfo = new VersionInfo();
    public boolean resourcesAreCompressed;
    public boolean sharedLibrary;
    public boolean sparseResources;
    public Map<String, String> unknownFiles = new LinkedHashMap<>();
    public List<String> doNotCompress;

    /** @deprecated use {@link #resourcesAreCompressed} */
    public boolean compressionType;

    public ApkInfo() {
        this(null);
    }

    public ApkInfo(ExtFile apkFile) {
        this.version = ApktoolProperties.getVersion();
        if (apkFile != null) {
            setApkFile(apkFile);
        }
    }

    public ExtFile getApkFile() {
        return mApkFile;
    }

    public void setApkFile(ExtFile apkFile) {
        mApkFile = apkFile;
        if (this.apkFileName == null) {
            this.apkFileName = apkFile.getName();
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

    public boolean hasMultipleSources() throws AndrolibException {
        if (mApkFile == null) {
            return false;
        }
        try {
            Set<String> files = mApkFile.getDirectory().getFiles(false);
            for (String file : files) {
                if (file.endsWith(".dex")) {
                    if (!file.equalsIgnoreCase("classes.dex")) {
                        return true;
                    }
                }
            }

            return false;
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    public void addUnknownFileInfo(String file, String value) {
        unknownFiles.put(file, value);
    }

    public String checkTargetSdkVersionBounds() {
        int target = mapSdkShorthandToVersion(getTargetSdkVersion());

        int min = (getMinSdkVersion() != null) ? mapSdkShorthandToVersion(getMinSdkVersion()) : 0;
        int max = (getMaxSdkVersion() != null) ? mapSdkShorthandToVersion(getMaxSdkVersion()) : target;

        target = Math.min(max, target);
        target = Math.max(min, target);
        return Integer.toString(target);
    }

    public Map<String, String> getSdkInfo() {
        return sdkInfo;
    }

    public void setSdkInfo(Map<String, String> sdkInfo) {
        this.sdkInfo = sdkInfo;
    }

    public void setSdkInfoField(String key, String value) {
        sdkInfo.put(key, value);
    }

    public String getMinSdkVersion() {
        return sdkInfo.get("minSdkVersion");
    }

    public String getMaxSdkVersion() {
        return sdkInfo.get("maxSdkVersion");
    }

    public String getTargetSdkVersion() {
        return sdkInfo.get("targetSdkVersion");
    }

    public int getMinSdkVersionFromAndroidCodename(String sdkVersion) {
        int sdkNumber = mapSdkShorthandToVersion(sdkVersion);

        if (sdkNumber == ResConfigFlags.SDK_BASE) {
            return Integer.parseInt(sdkInfo.get("minSdkVersion"));
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
            case "SDK_CUR_DEVELOPMENT":
                return ResConfigFlags.SDK_DEVELOPMENT;
            default:
                return Integer.parseInt(sdkVersion);
        }
    }

    public void save(File file) throws AndrolibException {
        try (YamlWriter writer = new YamlWriter(new FileOutputStream(file))) {
            write(writer);
        } catch (FileNotFoundException e) {
            throw new AndrolibException("File not found");
        } catch (Exception e) {
            throw new AndrolibException(e);
        }
    }

    public static ApkInfo load(InputStream is) throws AndrolibException {
        YamlReader reader = new YamlReader(is);
        ApkInfo apkInfo = new ApkInfo();
        reader.readRoot(apkInfo);
        return apkInfo;
    }

    public static ApkInfo load(File appDir) throws AndrolibException {
        try (InputStream in = new FileDirectory(appDir).getFileInput("apktool.yml")) {
            ApkInfo apkInfo = ApkInfo.load(in);
            apkInfo.setApkFile(new ExtFile(appDir));
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
                this.version = line.getValue();
                break;
            }
            case "apkFileName": {
                this.apkFileName = line.getValue();
                break;
            }
            case "isFrameworkApk": {
                this.isFrameworkApk = line.getValueBool();
                break;
            }
            case "usesFramework": {
                this.usesFramework = new UsesFramework();
                reader.readObject(usesFramework);
                break;
            }
            case "sdkInfo": {
                reader.readMap(sdkInfo);
                break;
            }
            case "packageInfo": {
                this.packageInfo = new PackageInfo();
                reader.readObject(packageInfo);
                break;
            }
            case "versionInfo": {
                this.versionInfo = new VersionInfo();
                reader.readObject(versionInfo);
                break;
            }
            case "compressionType":
            case "resourcesAreCompressed": {
                this.resourcesAreCompressed = line.getValueBool();
                break;
            }
            case "sharedLibrary": {
                this.sharedLibrary = line.getValueBool();
                break;
            }
            case "sparseResources": {
                this.sparseResources = line.getValueBool();
                break;
            }
            case "unknownFiles": {
                this.unknownFiles = new LinkedHashMap<>();
                reader.readMap(unknownFiles);
                break;
            }
            case "doNotCompress": {
                this.doNotCompress = new ArrayList<>();
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
        writer.writeStringMap("sdkInfo", sdkInfo);
        writer.writeObject("packageInfo", packageInfo);
        writer.writeObject("versionInfo", versionInfo);
        writer.writeBool("resourcesAreCompressed", resourcesAreCompressed);
        writer.writeBool("sharedLibrary", sharedLibrary);
        writer.writeBool("sparseResources", sparseResources);
        if (unknownFiles.size() > 0) {
            writer.writeStringMap("unknownFiles", unknownFiles);
        }
        writer.writeList("doNotCompress", doNotCompress);
    }
}
