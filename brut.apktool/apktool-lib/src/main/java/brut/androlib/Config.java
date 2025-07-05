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
import brut.util.OSDetection;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class Config {
    public enum DecodeSources { NONE, FULL, ONLY_MAIN_CLASSES }
    public enum DecodeResources { NONE, FULL, ONLY_MANIFEST }
    public enum DecodeAssets { NONE, FULL }
    public enum DecodeResolve { REMOVE, DUMMY, KEEP }

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

    // Common options
    private int mJobs;
    private String mFrameworkDirectory;
    private String mFrameworkTag;
    private String[] mLibraryFiles;
    private boolean mForced;
    private boolean mVerbose;

    // Decode options
    private DecodeSources mDecodeSources;
    private boolean mBaksmaliDebugMode;
    private int mBaksmaliApiLevel;
    private DecodeResources mDecodeResources;
    private DecodeResolve mDecodeResolve;
    private boolean mAnalysisMode;
    private boolean mKeepBrokenResources;
    private DecodeAssets mDecodeAssets;

    // Build options
    private boolean mDebugMode;
    private boolean mNetSecConf;
    private boolean mCopyOriginalFiles;
    private boolean mNoCrunch;
    private boolean mNoApk;
    private File mAaptBinary;
    private int mAaptVersion;

    public Config() {
        // Common options
        mJobs = Math.min(Runtime.getRuntime().availableProcessors(), 8);
        mFrameworkDirectory = DEFAULT_FRAMEWORK_DIRECTORY;
        mFrameworkTag = null;
        mLibraryFiles = null;
        mForced = false;
        mVerbose = false;

        // Decode options
        mDecodeSources = DecodeSources.FULL;
        mBaksmaliDebugMode = true;
        mBaksmaliApiLevel = 0;
        mDecodeResources = DecodeResources.FULL;
        mDecodeResolve = DecodeResolve.REMOVE;
        mAnalysisMode = false;
        mKeepBrokenResources = false;
        mDecodeAssets = DecodeAssets.FULL;

        // Build options
        mDebugMode = false;
        mNetSecConf = false;
        mCopyOriginalFiles = false;
        mNoCrunch = false;
        mNoApk = false;
        mAaptBinary = null;
        mAaptVersion = 2;
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
        if (frameworkDirectory == null || frameworkDirectory.isEmpty()) {
            frameworkDirectory = DEFAULT_FRAMEWORK_DIRECTORY;
        }
        mFrameworkDirectory = frameworkDirectory;
    }

    public String getFrameworkTag() {
        return mFrameworkTag;
    }

    public void setFrameworkTag(String frameworkTag) {
        if (frameworkTag == null || frameworkTag.isEmpty()) {
            frameworkTag = null;
        }
        mFrameworkTag = frameworkTag;
    }

    public String[] getLibraryFiles() {
        return mLibraryFiles;
    }

    public void setLibraryFiles(String[] libraryFiles) {
        mLibraryFiles = libraryFiles;
    }

    public boolean isForced() {
        return mForced;
    }

    public void setForced(boolean forced) {
        mForced = forced;
    }

    public boolean isVerbose() {
        return mVerbose;
    }

    public void setVerbose(boolean verbose) {
        mVerbose = verbose;
    }

    // Decode options

    public DecodeSources getDecodeSources() {
        return mDecodeSources;
    }

    public void setDecodeSources(DecodeSources decodeSources) {
        assert decodeSources != null;
        mDecodeSources = decodeSources;
    }

    public boolean isBaksmaliDebugMode() {
        return mBaksmaliDebugMode;
    }

    public void setBaksmaliDebugMode(boolean baksmaliDebugMode) {
        mBaksmaliDebugMode = baksmaliDebugMode;
    }

    public int getBaksmaliApiLevel() {
        return mBaksmaliApiLevel;
    }

    public void setBaksmaliApiLevel(int baksmaliApiLevel) {
        mBaksmaliApiLevel = baksmaliApiLevel;
    }

    public DecodeResources getDecodeResources() {
        return mDecodeResources;
    }

    public void setDecodeResources(DecodeResources decodeResources) {
        assert decodeResources != null;
        mDecodeResources = decodeResources;
    }

    public DecodeAssets getDecodeAssets() {
        return mDecodeAssets;
    }

    public void setDecodeAssets(DecodeAssets decodeAssets) {
        assert decodeAssets != null;
        mDecodeAssets = decodeAssets;
    }

    public DecodeResolve getDecodeResolve() {
        return mDecodeResolve;
    }

    public void setDecodeResolve(DecodeResolve decodeResolve) {
        assert decodeResolve != null;
        mDecodeResolve = decodeResolve;
    }

    public boolean isKeepBrokenResources() {
        return mKeepBrokenResources;
    }

    public void setKeepBrokenResources(boolean keepBrokenResources) {
        mKeepBrokenResources = keepBrokenResources;
    }

    public boolean isAnalysisMode() {
        return mAnalysisMode;
    }

    public void setAnalysisMode(boolean analysisMode) {
        mAnalysisMode = analysisMode;
    }

    // Build options

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

    public boolean isCopyOriginalFiles() {
        return mCopyOriginalFiles;
    }

    public void setCopyOriginalFiles(boolean copyOriginalFiles) {
        mCopyOriginalFiles = copyOriginalFiles;
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

    public File getAaptBinary() {
        return mAaptBinary;
    }

    public void setAaptBinary(File aaptBinary) throws AndrolibException {
        mAaptBinary = aaptBinary;
        mAaptVersion = AaptManager.getAaptVersion(aaptBinary);
    }

    public int getAaptVersion() {
        return mAaptVersion;
    }

    public void setAaptVersion(int aaptVersion) {
        mAaptVersion = aaptVersion;
    }
}
