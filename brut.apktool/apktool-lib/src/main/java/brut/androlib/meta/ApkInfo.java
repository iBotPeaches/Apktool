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
import com.google.common.annotations.VisibleForTesting;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

public class ApkInfo implements YamlSerializable {
    public static final String[] RAW_DIRS = { "assets", "lib" };

    public static final Pattern CLASSES_FILES_PATTERN = Pattern.compile("classes([2-9]|[1-9][0-9]+)?\\.dex");

    public static final Pattern ORIGINAL_FILES_PATTERN = Pattern.compile(
        "AndroidManifest\\.xml|META-INF/[^/]+\\.(RSA|SF|MF)|stamp-cert-sha256");

    public static final Pattern STANDARD_FILES_PATTERN = Pattern.compile(
        "resources\\.arsc|(" + String.join("|", RAW_DIRS) + ")/.*|"
      + CLASSES_FILES_PATTERN.pattern() + "|" + ORIGINAL_FILES_PATTERN.pattern());

    private String mVersion;
    private String mApkFileName;
    private final UsesFramework mUsesFramework;
    private final List<String> mUsesLibrary;
    private final SdkInfo mSdkInfo;
    private final VersionInfo mVersionInfo;
    private final ResourcesInfo mResourcesInfo;
    private final Set<String> mFeatureFlags;
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
        mFeatureFlags = new TreeSet<>();
        mDoNotCompress = new ArrayList<>();
    }

    @VisibleForTesting
    static ApkInfo load(InputStream in) throws IOException {
        try (YamlPullParser parser = new YamlPullParser(in)) {
            ApkInfo apkInfo = new ApkInfo();
            parser.readObject(apkInfo);
            return apkInfo;
        }
    }

    public static ApkInfo load(File file) throws IOException {
        try (YamlPullParser parser = new YamlPullParser(Files.newInputStream(file.toPath()))) {
            ApkInfo apkInfo = new ApkInfo();
            parser.readObject(apkInfo);
            return apkInfo;
        }
    }

    public void save(File file) throws IOException {
        try (YamlSerializer serial = new YamlSerializer(Files.newOutputStream(file.toPath()))) {
            serialize(serial);
        }
    }

    @Override
    public void onEntry(YamlPullParser parser) throws IOException {
        switch (parser.getKey()) {
            case "version":
                mVersion = parser.getString();
                break;
            case "apkFileName":
                mApkFileName = parser.getString();
                // Sanity check for potential malicious input.
                if (mApkFileName.equals(".") || mApkFileName.equals("..") || mApkFileName.indexOf('/') != -1
                        || mApkFileName.indexOf('\\') != -1) {
                    throw new SecurityException("Malicious value for apkFileName: " + mApkFileName);
                }
                break;
            case "usesFramework":
                mUsesFramework.clear();
                parser.readObject(mUsesFramework);
                break;
            case "usesLibrary":
                mUsesLibrary.clear();
                parser.readStringSeq(mUsesLibrary);
                break;
            case "sdkInfo":
                mSdkInfo.clear();
                parser.readObject(mSdkInfo);
                break;
            case "versionInfo":
                mVersionInfo.clear();
                parser.readObject(mVersionInfo);
                break;
            case "resourcesInfo":
                mResourcesInfo.clear();
                parser.readObject(mResourcesInfo);
                break;
            case "featureFlags":
                mFeatureFlags.clear();
                parser.readStringSeq(mFeatureFlags);
                break;
            case "doNotCompress":
                mDoNotCompress.clear();
                parser.readStringSeq(mDoNotCompress);
                break;
        }
    }

    @Override
    public void serialize(YamlSerializer serial) throws IOException {
        serial.writeString("version", mVersion);
        serial.writeString("apkFileName", mApkFileName);
        if (!mUsesFramework.isEmpty()) {
            serial.writeObject("usesFramework", mUsesFramework);
        }
        if (!mUsesLibrary.isEmpty()) {
            serial.writeStringSeq("usesLibrary", mUsesLibrary);
        }
        if (!mSdkInfo.isEmpty()) {
            serial.writeObject("sdkInfo", mSdkInfo);
        }
        if (!mVersionInfo.isEmpty()) {
            serial.writeObject("versionInfo", mVersionInfo);
        }
        if (!mResourcesInfo.isEmpty()) {
            serial.writeObject("resourcesInfo", mResourcesInfo);
        }
        if (!mFeatureFlags.isEmpty()) {
            serial.writeStringSeq("featureFlags", mFeatureFlags);
        }
        if (!mDoNotCompress.isEmpty()) {
            serial.writeStringSeq("doNotCompress", mDoNotCompress);
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

    public Set<String> getFeatureFlags() {
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
