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
package brut.androlib.meta;

import brut.androlib.exceptions.AndrolibException;
import brut.directory.DirectoryException;
import brut.directory.ExtFile;
import brut.yaml.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class ApkInfo implements YamlSerializable {
    public static final String[] RESOURCES_DIRNAMES = { "res", "r", "R" };
    public static final String[] RAW_DIRNAMES = { "assets", "lib" };

    public static final Pattern ORIGINAL_FILENAMES_PATTERN = Pattern.compile(
        "AndroidManifest\\.xml|META-INF/[^/]+\\.(RSA|SF|MF)|stamp-cert-sha256");

    public static final Pattern STANDARD_FILENAMES_PATTERN = Pattern.compile(
        "[^/]+\\.dex|resources\\.arsc|(" + String.join("|", RESOURCES_DIRNAMES) + "|"
            + String.join("|", RAW_DIRNAMES) + ")/.*|" + ORIGINAL_FILENAMES_PATTERN.pattern());

    private String mVersion;
    private String mApkFileName;
    private final UsesFramework mUsesFramework;
    private final List<String> mUsesLibrary;
    private final SdkInfo mSdkInfo;
    private final VersionInfo mVersionInfo;
    private final ResourcesInfo mResourcesInfo;
    private final Map<String, Boolean> mFeatureFlags;
    private final List<String> mDoNotCompress;

    // Only set when loaded from a file (not a stream).
    private ExtFile mApkFile;

    public ApkInfo() {
        mVersion = null;
        mApkFileName = null;
        mUsesFramework = new UsesFramework();
        mUsesLibrary = new ArrayList<>();
        mSdkInfo = new SdkInfo();
        mVersionInfo = new VersionInfo();
        mResourcesInfo = new ResourcesInfo();
        mFeatureFlags = new LinkedHashMap<>();
        mDoNotCompress = new ArrayList<>();
    }

    public ApkInfo(ExtFile apkFile) {
        this();
        setApkFile(apkFile);
    }

    public static ApkInfo load(InputStream in) {
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

    public void save(File file) throws AndrolibException {
        try (YamlWriter writer = new YamlWriter(Files.newOutputStream(file.toPath()))) {
            write(writer);
        } catch (IOException ex) {
            throw new AndrolibException(ex);
        }
    }

    @Override
    public void readItem(YamlReader reader) {
        YamlLine line = reader.getLine();
        switch (line.getKey()) {
            case "version": {
                mVersion = line.getValue();
                break;
            }
            case "apkFileName": {
                mApkFileName = line.getValue();
                break;
            }
            case "usesFramework": {
                mUsesFramework.clear();
                reader.readObject(mUsesFramework);
                break;
            }
            case "usesLibrary": {
                mUsesLibrary.clear();
                reader.readStringList(mUsesLibrary);
                break;
            }
            case "sdkInfo": {
                mSdkInfo.clear();
                reader.readObject(mSdkInfo);
                break;
            }
            case "versionInfo": {
                mVersionInfo.clear();
                reader.readObject(mVersionInfo);
                break;
            }
            case "resourcesInfo": {
                mResourcesInfo.clear();
                reader.readObject(mResourcesInfo);
                break;
            }
            case "featureFlags": {
                mFeatureFlags.clear();
                reader.readBoolMap(mFeatureFlags);
                break;
            }
            case "doNotCompress": {
                mDoNotCompress.clear();
                reader.readStringList(mDoNotCompress);
                break;
            }
        }
    }

    @Override
    public void write(YamlWriter writer) {
        writer.writeString("version", mVersion);
        writer.writeString("apkFileName", mApkFileName);
        if (!mUsesFramework.isEmpty()) {
            writer.writeObject("usesFramework", mUsesFramework);
        }
        if (!mUsesLibrary.isEmpty()) {
            writer.writeList("usesLibrary", mUsesLibrary);
        }
        if (!mSdkInfo.isEmpty()) {
            writer.writeObject("sdkInfo", mSdkInfo);
        }
        if (!mVersionInfo.isEmpty()) {
            writer.writeObject("versionInfo", mVersionInfo);
        }
        if (!mResourcesInfo.isEmpty()) {
            writer.writeObject("resourcesInfo", mResourcesInfo);
        }
        if (!mFeatureFlags.isEmpty()) {
            writer.writeMap("featureFlags", mFeatureFlags);
        }
        if (!mDoNotCompress.isEmpty()) {
            writer.writeList("doNotCompress", mDoNotCompress);
        }
    }

    public String getVersion() {
        return mVersion;
    }

    public void setVersion(String version) {
        mVersion = version;
    }

    public String getApkFileName() {
        return mApkFileName;
    }

    public void setApkFileName(String apkFileName) {
        mApkFileName = apkFileName;
    }

    public UsesFramework getUsesFramework() {
        return mUsesFramework;
    }

    public List<String> getUsesLibrary() {
        return mUsesLibrary;
    }

    public SdkInfo getSdkInfo() {
        return mSdkInfo;
    }

    public VersionInfo getVersionInfo() {
        return mVersionInfo;
    }

    public ResourcesInfo getResourcesInfo() {
        return mResourcesInfo;
    }

    public Map<String, Boolean> getFeatureFlags() {
        return mFeatureFlags;
    }

    public List<String> getDoNotCompress() {
        return mDoNotCompress;
    }

    public ExtFile getApkFile() {
        return mApkFile;
    }

    public void setApkFile(ExtFile apkFile) {
        mApkFile = apkFile;
        if (mApkFileName == null) {
            mApkFileName = apkFile.getName();
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
}
