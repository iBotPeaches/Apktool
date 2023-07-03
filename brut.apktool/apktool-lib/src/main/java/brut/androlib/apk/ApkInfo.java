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
import brut.directory.FileDirectory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.introspector.PropertyUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class ApkInfo {
    public String version;

    private String mApkFileName;
    public boolean isFrameworkApk;
    public UsesFramework usesFramework;
    private Map<String, String> mSdkInfo = new LinkedHashMap<>();
    public PackageInfo packageInfo = new PackageInfo();
    public VersionInfo versionInfo = new VersionInfo();
    public boolean resourcesAreCompressed;
    public boolean sharedLibrary;
    public boolean sparseResources;
    public Map<String, String> unknownFiles;
    public Collection<String> doNotCompress;

    public ApkInfo() {
        this.version = ApktoolProperties.getVersion();
    }

    private static Yaml getYaml() {
        DumperOptions dumpOptions = new DumperOptions();
        dumpOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

        EscapedStringRepresenter representer = new EscapedStringRepresenter();
        PropertyUtils propertyUtils = representer.getPropertyUtils();
        propertyUtils.setSkipMissingProperties(true);

        LoaderOptions loaderOptions = new LoaderOptions();
        loaderOptions.setCodePointLimit(10 * 1024 * 1024); // 10mb

        return new Yaml(new ClassSafeConstructor(), representer, dumpOptions, loaderOptions);
    }

    public void save(Writer output) {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        getYaml().dump(this, output);
    }

    public String checkTargetSdkVersionBounds() {
        int target = mapSdkShorthandToVersion(getTargetSdkVersion());

        int min = (getMinSdkVersion() != null) ? mapSdkShorthandToVersion(getMinSdkVersion()) : 0;
        int max = (getMaxSdkVersion() != null) ? mapSdkShorthandToVersion(getMaxSdkVersion()) : target;

        target = Math.min(max, target);
        target = Math.max(min, target);
        return Integer.toString(target);
    }

    public String getApkFileName() {
        return mApkFileName;
    }

    public void setApkFileName(String apkFileName) {
        mApkFileName = apkFileName;
    }

    public Map<String, String> getSdkInfo() {
        return mSdkInfo;
    }

    public void setSdkInfo(Map<String, String> sdkInfo) {
        mSdkInfo = sdkInfo;
    }

    public String getMinSdkVersion() {
        return mSdkInfo.get("minSdkVersion");
    }

    public String getMaxSdkVersion() {
        return mSdkInfo.get("maxSdkVersion");
    }

    public String getTargetSdkVersion() {
        return mSdkInfo.get("targetSdkVersion");
    }

    public int getMinSdkVersionFromAndroidCodename(String sdkVersion) {
        int sdkNumber = mapSdkShorthandToVersion(sdkVersion);

        if (sdkNumber == ResConfigFlags.SDK_BASE) {
            return Integer.parseInt(mSdkInfo.get("minSdkVersion"));
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

    public void save(File file) throws IOException {
        try(
                FileOutputStream fos = new FileOutputStream(file);
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
                Writer writer = new BufferedWriter(outputStreamWriter)
        ) {
            save(writer);
        }
    }

    public static ApkInfo load(InputStream is) {
        return getYaml().loadAs(is, ApkInfo.class);
    }

    public static ApkInfo load(File appDir)
        throws AndrolibException {
        try(
            InputStream in = new FileDirectory(appDir).getFileInput("apktool.yml")
        ) {
            return ApkInfo.load(in);
        } catch (DirectoryException | IOException ex) {
            throw new AndrolibException(ex);
        }
    }
}
