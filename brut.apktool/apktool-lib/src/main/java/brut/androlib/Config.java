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

public class Config {
    public enum DecodeSources { FULL, ONLY_MAIN_CLASSES, NONE }
    public enum DecodeResources { FULL, ONLY_MANIFEST, NONE }
    public enum DecodeResolve { DEFAULT, GREEDY, LAZY }
    public enum DecodeAssets { FULL, NONE }

    private final String mVersion;

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
    private DecodeResources mDecodeResources;
    private DecodeResolve mDecodeResolve;
    private boolean mKeepBrokenResources;
    private boolean mAnalysisMode;
    private DecodeAssets mDecodeAssets;

    // Build options
    private boolean mNoApk;
    private boolean mNoCrunch;
    private boolean mCopyOriginal;
    private boolean mDebuggable;
    private boolean mNetSecConf;
    private String mAaptBinary;

    public Config(String version) {
        mVersion = version;

        // Common options
        mJobs = Math.min(Runtime.getRuntime().availableProcessors(), 8);
        mFrameworkDirectory = null;
        mFrameworkTag = null;
        mLibraryFiles = null;
        mForced = false;
        mVerbose = false;

        // Decode options
        mDecodeSources = DecodeSources.FULL;
        mBaksmaliDebugMode = true;
        mDecodeResources = DecodeResources.FULL;
        mDecodeResolve = DecodeResolve.DEFAULT;
        mKeepBrokenResources = false;
        mAnalysisMode = false;
        mDecodeAssets = DecodeAssets.FULL;

        // Build options
        mNoApk = false;
        mNoCrunch = false;
        mCopyOriginal = false;
        mDebuggable = false;
        mNetSecConf = false;
        mAaptBinary = null;
    }

    public String getVersion() {
        return mVersion;
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
        mFrameworkDirectory = frameworkDirectory;
    }

    public String getFrameworkTag() {
        return mFrameworkTag;
    }

    public void setFrameworkTag(String frameworkTag) {
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

    public DecodeResources getDecodeResources() {
        return mDecodeResources;
    }

    public void setDecodeResources(DecodeResources decodeResources) {
        assert decodeResources != null;
        mDecodeResources = decodeResources;
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

    public DecodeAssets getDecodeAssets() {
        return mDecodeAssets;
    }

    public void setDecodeAssets(DecodeAssets decodeAssets) {
        assert decodeAssets != null;
        mDecodeAssets = decodeAssets;
    }

    // Build options

    public boolean isNoApk() {
        return mNoApk;
    }

    public void setNoApk(boolean noApk) {
        mNoApk = noApk;
    }

    public boolean isNoCrunch() {
        return mNoCrunch;
    }

    public void setNoCrunch(boolean noCrunch) {
        mNoCrunch = noCrunch;
    }

    public boolean isCopyOriginal() {
        return mCopyOriginal;
    }

    public void setCopyOriginal(boolean copyOriginal) {
        mCopyOriginal = copyOriginal;
    }

    public boolean isDebuggable() {
        return mDebuggable;
    }

    public void setDebuggable(boolean debuggable) {
        mDebuggable = debuggable;
    }

    public boolean isNetSecConf() {
        return mNetSecConf;
    }

    public void setNetSecConf(boolean netSecConf) {
        mNetSecConf = netSecConf;
    }

    public String getAaptBinary() {
        return mAaptBinary;
    }

    public void setAaptBinary(String aaptBinary) {
        mAaptBinary = aaptBinary;
    }
}
