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
package brut.androlib;

import brut.androlib.exceptions.AndrolibException;
import brut.common.BrutException;
import brut.util.AaptManager;
import brut.util.OSDetection;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

public final class Config {
    private static final Logger LOGGER = Logger.getLogger(Config.class.getName());

    public static final short DECODE_SOURCES_NONE = 0x0000;
    public static final short DECODE_SOURCES_SMALI = 0x0001;
    public static final short DECODE_SOURCES_SMALI_ONLY_MAIN_CLASSES = 0x0010;

    public static final short DECODE_RESOURCES_NONE = 0x0100;
    public static final short DECODE_RESOURCES_FULL = 0x0101;

    public static final short FORCE_DECODE_MANIFEST_NONE = 0x0000;
    public static final short FORCE_DECODE_MANIFEST_FULL = 0x0001;

    public static final short DECODE_ASSETS_NONE = 0x0000;
    public static final short DECODE_ASSETS_FULL = 0x0001;

    public static final short DECODE_RES_RESOLVE_REMOVE = 0x0000;
    public static final short DECODE_RES_RESOLVE_DUMMY = 0x0001;
    public static final short DECODE_RES_RESOLVE_RETAIN = 0x0002;

    private static final String DEFAULT_FRAMEWORK_DIRECTORY;

    static {
        File parent = new File(System.getProperty("user.home"));
        Path path;
        if (OSDetection.isMacOSX()) {
            path = Paths.get(parent.getAbsolutePath(), "Library", "apktool", "framework");
        } else if (OSDetection.isWindows()) {
            path = Paths.get(parent.getAbsolutePath(), "AppData", "Local", "apktool", "framework");
        } else {
            String xdgDataFolder = System.getenv("XDG_DATA_HOME");
            if (xdgDataFolder != null) {
                path = Paths.get(xdgDataFolder, "apktool", "framework");
            } else {
                path = Paths.get(parent.getAbsolutePath(), ".local", "share", "apktool", "framework");
            }
        }
        DEFAULT_FRAMEWORK_DIRECTORY = path.toString();
    }

    // Build options
    private boolean mForceBuildAll;
    private boolean mForceDeleteFramework;
    private boolean mDebugMode;
    private boolean mNetSecConf;
    private boolean mVerbose;
    private boolean mCopyOriginalFiles;
    private boolean mUpdateFiles;
    private boolean mNoCrunch;
    private boolean mNoApk;

    // Decode options
    private short mDecodeSources;
    private short mDecodeResources;
    private short mForceDecodeManifest;
    private short mDecodeAssets;
    private short mDecodeResolveMode;
    private int mApiLevel;
    private boolean mAnalysisMode;
    private boolean mForceDelete;
    private boolean mKeepBrokenResources;
    private boolean mBaksmaliDebugMode;

    // Common options
    private int mJobs;
    private String mFrameworkDirectory;
    private String mFrameworkTag;
    private File mAaptBinary;
    private int mAaptVersion;

    public Config() {
        mForceBuildAll = false;
        mForceDeleteFramework = false;
        mDebugMode = false;
        mNetSecConf = false;
        mVerbose = false;
        mCopyOriginalFiles = false;
        mUpdateFiles = false;
        mNoCrunch = false;
        mNoApk = false;

        mDecodeSources = DECODE_SOURCES_SMALI;
        mDecodeResources = DECODE_RESOURCES_FULL;
        mForceDecodeManifest = FORCE_DECODE_MANIFEST_NONE;
        mDecodeAssets = DECODE_ASSETS_FULL;
        mDecodeResolveMode = DECODE_RES_RESOLVE_REMOVE;
        mApiLevel = 0;
        mAnalysisMode = false;
        mForceDelete = false;
        mKeepBrokenResources = false;
        mBaksmaliDebugMode = true;

        mJobs = Math.min(Runtime.getRuntime().availableProcessors(), 8);
        mFrameworkDirectory = DEFAULT_FRAMEWORK_DIRECTORY;
        mFrameworkTag = null;
        mAaptBinary = null;
        mAaptVersion = 2;
    }

    // Build options

    public boolean isForceBuildAll() {
        return mForceBuildAll;
    }

    public void setForceBuildAll(boolean forceBuildAll) {
        mForceBuildAll = forceBuildAll;
    }

    public boolean isForceDeleteFramework() {
        return mForceDeleteFramework;
    }

    public void setForceDeleteFramework(boolean forceDeleteFramework) {
        mForceDeleteFramework = forceDeleteFramework;
    }

    public boolean isDebugMode() {
        return mDebugMode;
    }

    public void setDebugMode(boolean debugMode) {
        mDebugMode = debugMode;
    }

    public boolean isNetSecConf() {
        return mNetSecConf;
    }

    public void setNetSecConf(boolean netSecConf) {
        mNetSecConf = netSecConf;
    }

    public boolean isVerbose() {
        return mVerbose;
    }

    public void setVerbose(boolean verbose) {
        mVerbose = verbose;
    }

    public boolean isCopyOriginalFiles() {
        return mCopyOriginalFiles;
    }

    public void setCopyOriginalFiles(boolean copyOriginalFiles) {
        mCopyOriginalFiles = copyOriginalFiles;
    }

    public boolean isUpdateFiles() {
        return mUpdateFiles;
    }

    public void setUpdateFiles(boolean updateFiles) {
        mUpdateFiles = updateFiles;
    }

    public boolean isNoCrunch() {
        return mNoCrunch;
    }

    public void setNoCrunch(boolean noCrunch) {
        mNoCrunch = noCrunch;
    }

    public boolean isNoApk() {
        return mNoApk;
    }

    public void setNoApk(boolean noApk) {
        mNoApk = noApk;
    }

    // Decode options

    public short getDecodeSources() {
        return mDecodeSources;
    }

    public void setDecodeSources(short decodeSources) throws AndrolibException {
        switch (decodeSources) {
            case DECODE_SOURCES_NONE:
            case DECODE_SOURCES_SMALI:
            case DECODE_SOURCES_SMALI_ONLY_MAIN_CLASSES:
                mDecodeSources = decodeSources;
                break;
            default:
                throw new AndrolibException("Invalid decode sources mode: " + decodeSources);
        }
    }

    public short getDecodeResources() {
        return mDecodeResources;
    }

    public void setDecodeResources(short decodeResources) throws AndrolibException {
        switch (decodeResources) {
            case DECODE_RESOURCES_NONE:
            case DECODE_RESOURCES_FULL:
                mDecodeResources = decodeResources;
                break;
            default:
                throw new AndrolibException("Invalid decode resources mode: " + decodeResources);
        }
    }

    public short getForceDecodeManifest() {
        return mForceDecodeManifest;
    }

    public void setForceDecodeManifest(short forceDecodeManifest) throws AndrolibException {
        switch (forceDecodeManifest) {
            case FORCE_DECODE_MANIFEST_NONE:
            case FORCE_DECODE_MANIFEST_FULL:
                mForceDecodeManifest = forceDecodeManifest;
                break;
            default:
                throw new AndrolibException("Invalid force decode manifest mode: " + forceDecodeManifest);
        }
    }

    public short getDecodeAssets() {
        return mDecodeAssets;
    }

    public void setDecodeAssets(short decodeAssets) throws AndrolibException {
        switch (decodeAssets) {
            case DECODE_ASSETS_NONE:
            case DECODE_ASSETS_FULL:
                mDecodeAssets = decodeAssets;
                break;
            default:
                throw new AndrolibException("Invalid decode asset mode: " + decodeAssets);
        }
    }

    public short getDecodeResolveMode() {
        return mDecodeResolveMode;
    }

    public void setDecodeResolveMode(short decodeResolveMode) throws AndrolibException {
        switch (decodeResolveMode) {
            case DECODE_RES_RESOLVE_REMOVE:
            case DECODE_RES_RESOLVE_DUMMY:
            case DECODE_RES_RESOLVE_RETAIN:
                mDecodeResolveMode = decodeResolveMode;
                break;
            default:
                throw new AndrolibException("Invalid decode resolve mode: " + decodeResolveMode);
        }
    }

    public int getApiLevel() {
        return mApiLevel;
    }

    public void setApiLevel(int apiLevel) {
        mApiLevel = apiLevel;
    }

    public boolean isAnalysisMode() {
        return mAnalysisMode;
    }

    public void setAnalysisMode(boolean analysisMode) {
        mAnalysisMode = analysisMode;
    }

    public boolean isForceDelete() {
        return mForceDelete;
    }

    public void setForceDelete(boolean forceDelete) {
        mForceDelete = forceDelete;
    }

    public boolean isKeepBrokenResources() {
        return mKeepBrokenResources;
    }

    public void setKeepBrokenResources(boolean keepBrokenResources) {
        mKeepBrokenResources = keepBrokenResources;
    }

    public boolean isBaksmaliDebugMode() {
        return mBaksmaliDebugMode;
    }

    public void setBaksmaliDebugMode(boolean baksmaliDebugMode) {
        mBaksmaliDebugMode = baksmaliDebugMode;
    }

    // Common options

    public int getJobs() {
        return mJobs;
    }

    public void setJobs(int jobs) {
        mJobs = jobs;
    }

    public String getFrameworkDirectory() {
        return mFrameworkDirectory;
    }

    public void setFrameworkDirectory(String frameworkDirectory) {
        mFrameworkDirectory = frameworkDirectory != null
            ? frameworkDirectory : DEFAULT_FRAMEWORK_DIRECTORY;
    }

    public String getFrameworkTag() {
        return mFrameworkTag;
    }

    public void setFrameworkTag(String frameworkTag) {
        mFrameworkTag = frameworkTag;
    }

    public File getAaptBinary() {
        return mAaptBinary;
    }

    public void setAaptBinary(File aaptBinary) throws AndrolibException {
        try {
            mAaptBinary = aaptBinary;
            mAaptVersion = AaptManager.getAaptVersion(aaptBinary);
        } catch (BrutException ex) {
            throw new AndrolibException(ex);
        }
    }

    public int getAaptVersion() {
        return mAaptVersion;
    }

    public void setAaptVersion(int aaptVersion) {
        mAaptVersion = aaptVersion;
    }
}
