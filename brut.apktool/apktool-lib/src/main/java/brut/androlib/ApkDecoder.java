/**
 *  Copyright 2014 Ryszard Wiśniewski <brut.alll@gmail.com>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package brut.androlib;

import brut.androlib.err.InFileNotFoundException;
import brut.androlib.err.OutDirExistsException;
import brut.androlib.res.AndrolibResources;
import brut.androlib.res.data.ResPackage;
import brut.androlib.res.data.ResTable;
import brut.androlib.res.util.ExtFile;
import brut.common.BrutException;
import brut.directory.DirectoryException;
import brut.util.OS;
import com.google.common.base.Strings;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public class ApkDecoder {
    public ApkDecoder() {
        this(new Androlib());
    }

    public ApkDecoder(Androlib androlib) {
        mAndrolib = androlib;
    }

    public ApkDecoder(File apkFile) {
        this(apkFile, new Androlib());
    }

    public ApkDecoder(File apkFile, Androlib androlib) {
        mAndrolib = androlib;
        setApkFile(apkFile);
    }

    public void setApkFile(File apkFile) {
        mApkFile = new ExtFile(apkFile);
        mResTable = null;
    }

    public void setOutDir(File outDir) throws AndrolibException {
        mOutDir = outDir;
    }

    public void setApi(int api) {
        mApi = api;
    }

    public void decode() throws AndrolibException, IOException, DirectoryException {
        File outDir = getOutDir();

        if (!mForceDelete && outDir.exists()) {
            throw new OutDirExistsException();
        }

        if (!mApkFile.isFile() || !mApkFile.canRead()) {
            throw new InFileNotFoundException();
        }

        try {
            OS.rmdir(outDir);
        } catch (BrutException ex) {
            throw new AndrolibException(ex);
        }
        outDir.mkdirs();

        LOGGER.info("Using Apktool " + Androlib.getVersion() + " on " + mApkFile.getName());

        if (hasResources()) {
            setTargetSdkVersion();
            setAnalysisMode(mAnalysisMode, true);
            setCompressionMode();

            switch (mDecodeResources) {
                case DECODE_RESOURCES_NONE:
                    mAndrolib.decodeResourcesRaw(mApkFile, outDir);
                    break;
                case DECODE_RESOURCES_FULL:
                    if (hasManifest()) {
                        mAndrolib.decodeManifestWithResources(mApkFile, outDir, getResTable());
                    }
                    mAndrolib.decodeResourcesFull(mApkFile, outDir, getResTable());
                    break;
            }
        } else {
            // if there's no resources.asrc, decode the manifest without looking
            // up attribute references
            if (hasManifest()) {
                switch (mDecodeResources) {
                    case DECODE_RESOURCES_NONE:
                        mAndrolib.decodeManifestRaw(mApkFile, outDir);
                        break;
                    case DECODE_RESOURCES_FULL:
                        mAndrolib.decodeManifestFull(mApkFile, outDir,
                                getResTable());
                        break;
                }
            }
        }

        if (hasSources()) {
            switch (mDecodeSources) {
                case DECODE_SOURCES_NONE:
                    mAndrolib.decodeSourcesRaw(mApkFile, outDir, "classes.dex");
                    break;
                case DECODE_SOURCES_SMALI:
                    mAndrolib.decodeSourcesSmali(mApkFile, outDir, "classes.dex", mDebug, mDebugLinePrefix, mBakDeb, mApi);
                    break;
                case DECODE_SOURCES_JAVA:
                    mAndrolib.decodeSourcesJava(mApkFile, outDir, mDebug);
                    break;
            }
        }

        if (hasMultipleSources()) {
            // foreach unknown dex file in root, lets disassemble it
            Set<String> files = mApkFile.getDirectory().getFiles(true);
            for (String file : files) {
                if (file.endsWith(".dex")) {
                    if (! file.equalsIgnoreCase("classes.dex")) {
                        switch(mDecodeSources) {
                            case DECODE_SOURCES_NONE:
                                mAndrolib.decodeSourcesRaw(mApkFile, outDir, file);
                                break;
                            case DECODE_SOURCES_SMALI:
                                mAndrolib.decodeSourcesSmali(mApkFile, outDir, file, mDebug, mDebugLinePrefix, mBakDeb, mApi);
                                break;
                            case DECODE_SOURCES_JAVA:
                                mAndrolib.decodeSourcesJava(mApkFile, outDir, mDebug);
                                break;
                        }
                    }
                }
            }
        }

        mAndrolib.decodeRawFiles(mApkFile, outDir);
        mAndrolib.decodeUnknownFiles(mApkFile, outDir, mResTable);
        mAndrolib.writeOriginalFiles(mApkFile, outDir);
        writeMetaFile();
    }

    public void setDecodeSources(short mode) throws AndrolibException {
        if (mode != DECODE_SOURCES_NONE && mode != DECODE_SOURCES_SMALI && mode != DECODE_SOURCES_JAVA) {
            throw new AndrolibException("Invalid decode sources mode: " + mode);
        }
        mDecodeSources = mode;
    }

    public void setDecodeResources(short mode) throws AndrolibException {
        if (mode != DECODE_RESOURCES_NONE && mode != DECODE_RESOURCES_FULL) {
            throw new AndrolibException("Invalid decode resources mode");
        }
        mDecodeResources = mode;
    }

    public void setDebugMode(boolean debug) {
        mDebug = debug;
    }

    public void setAnalysisMode(boolean mode, boolean pass) throws AndrolibException{
        mAnalysisMode = mode;

        // only set mResTable, once it exists
        if (pass) {
            if (mResTable == null) {
                mResTable = getResTable();
            }
            mResTable.setAnalysisMode(mode);
        }
    }

    public void setCompressionMode() throws AndrolibException, IOException {
        // read the resources.arsc checking for STORED vs DEFLATE
        // this will determine whether we compress on rebuild or not.
        ZipFile zf = new ZipFile(mApkFile.getAbsolutePath());
        ZipEntry ze = zf.getEntry("resources.arsc");
        if (ze != null) {
            int compression = ze.getMethod();
            mCompressResources = (compression == ZipEntry.DEFLATED);
        }
        zf.close();
    }

    public void setTargetSdkVersion() throws AndrolibException, IOException {
        if (mResTable == null) {
            mResTable = mAndrolib.getResTable(mApkFile);
        }

        Map<String, String> sdkInfo = mResTable.getSdkInfo();
        if (sdkInfo.get("targetSdkVersion") != null) {
            mApi = Integer.parseInt(sdkInfo.get("targetSdkVersion"));
        }
    }

    public void setDebugLinePrefix(String debugLinePrefix) {
        mDebugLinePrefix = debugLinePrefix;
    }

    public void setBaksmaliDebugMode(boolean bakdeb) {
        mBakDeb = bakdeb;
    }

    public void setForceDelete(boolean forceDelete) {
        mForceDelete = forceDelete;
    }

    public void setFrameworkTag(String tag) throws AndrolibException {
        mAndrolib.apkOptions.frameworkTag = tag;
    }

    public void setKeepBrokenResources(boolean keepBrokenResources) {
        mKeepBrokenResources = keepBrokenResources;
    }

    public void setFrameworkDir(String dir) {
        mAndrolib.apkOptions.frameworkFolderLocation = dir;
    }

    public ResTable getResTable() throws AndrolibException {
        if (mResTable == null) {
            boolean hasResources = hasResources();
            boolean hasManifest = hasManifest();
            if (! (hasManifest || hasResources)) {
                throw new AndrolibException(
                        "Apk doesn't contain either AndroidManifest.xml file or resources.arsc file");
            }
            AndrolibResources.sKeepBroken = mKeepBrokenResources;
            mResTable = mAndrolib.getResTable(mApkFile, hasResources);
        }
        return mResTable;
    }

    public boolean hasSources() throws AndrolibException {
        try {
            return mApkFile.getDirectory().containsFile("classes.dex");
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    public boolean hasMultipleSources() throws AndrolibException {
        try {
            Set<String> files = mApkFile.getDirectory().getFiles(true);
            for (String file : files) {
                if (file.endsWith(".dex")) {
                    if (! file.equalsIgnoreCase("classes.dex")) {
                        return true;
                    }
                }
            }

            return false;
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    public boolean hasManifest() throws AndrolibException {
        try {
            return mApkFile.getDirectory().containsFile("AndroidManifest.xml");
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    public boolean hasResources() throws AndrolibException {
        try {
            return mApkFile.getDirectory().containsFile("resources.arsc");
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    public final static short DECODE_SOURCES_NONE = 0x0000;
    public final static short DECODE_SOURCES_SMALI = 0x0001;
    public final static short DECODE_SOURCES_JAVA = 0x0002;

    public final static short DECODE_RESOURCES_NONE = 0x0100;
    public final static short DECODE_RESOURCES_FULL = 0x0101;

    private File getOutDir() throws AndrolibException {
        if (mOutDir == null) {
            throw new AndrolibException("Out dir not set");
        }
        return mOutDir;
    }

    private void writeMetaFile() throws AndrolibException {
        Map<String, Object> meta = new LinkedHashMap<String, Object>();
        meta.put("version", Androlib.getVersion());
        meta.put("apkFileName", mApkFile.getName());

        if (mDecodeResources != DECODE_RESOURCES_NONE && (hasManifest() || hasResources())) {
            meta.put("isFrameworkApk", mAndrolib.isFrameworkApk(getResTable()));
            putUsesFramework(meta);
            putSdkInfo(meta);
            putPackageInfo(meta);
            putVersionInfo(meta);
            putCompressionInfo(meta);
            putSharedLibraryInfo(meta);
        }
        putUnknownInfo(meta);

        mAndrolib.writeMetaFile(mOutDir, meta);
    }

    private void putUsesFramework(Map<String, Object> meta) throws AndrolibException {
        Set<ResPackage> pkgs = getResTable().listFramePackages();
        if (pkgs.isEmpty()) {
            return;
        }

        Integer[] ids = new Integer[pkgs.size()];
        int i = 0;
        for (ResPackage pkg : pkgs) {
            ids[i++] = pkg.getId();
        }
        Arrays.sort(ids);

        Map<String, Object> uses = new LinkedHashMap<String, Object>();
        uses.put("ids", ids);

        if (mAndrolib.apkOptions.frameworkTag != null) {
            uses.put("tag", mAndrolib.apkOptions.frameworkTag);
        }

        meta.put("usesFramework", uses);
    }

    private void putSdkInfo(Map<String, Object> meta) throws AndrolibException {
        Map<String, String> info = getResTable().getSdkInfo();
        if (info.size() > 0) {
            meta.put("sdkInfo", info);
        }
    }

    private void putPackageInfo(Map<String, Object> meta) throws AndrolibException {
        String renamed = getResTable().getPackageRenamed();
        String original = getResTable().getPackageOriginal();
        int id = getResTable().getPackageId();

        HashMap<String, String> packages = new HashMap<String, String>();

        if (Strings.isNullOrEmpty(original)) {
            return;
        }

        // only put rename-manifest-package into apktool.yml, if the change will be required
        if (!renamed.equalsIgnoreCase(original)) {
            packages.put("rename-manifest-package", renamed);
        }
        packages.put("forced-package-id", String.valueOf(id));
        meta.put("packageInfo", packages);
    }

    private void putVersionInfo(Map<String, Object> meta) throws AndrolibException {
        Map<String, String> info = getResTable().getVersionInfo();
        if (info.size() > 0) {
            meta.put("versionInfo", info);
        }
    }

    private void putUnknownInfo(Map<String, Object> meta) throws AndrolibException {
        Map<String,String> info = mAndrolib.mResUnknownFiles.getUnknownFiles();
        if (info.size() > 0) {
            meta.put("unknownFiles", info);
        }
    }

    private void putCompressionInfo(Map<String, Object> meta) throws AndrolibException {
        meta.put("compressionType", getCompressionType());
    }

    private void putSharedLibraryInfo(Map<String, Object> meta) throws AndrolibException {
        meta.put("sharedLibrary", mResTable.getSharedLibrary());
    }

    private boolean getCompressionType() {
        return mCompressResources;
    }

    private final Androlib mAndrolib;

    private final static Logger LOGGER = Logger.getLogger(Androlib.class.getName());

    private ExtFile mApkFile;
    private File mOutDir;
    private ResTable mResTable;
    private short mDecodeSources = DECODE_SOURCES_SMALI;
    private short mDecodeResources = DECODE_RESOURCES_FULL;
    private String mDebugLinePrefix = "a=0;// ";
    private boolean mDebug = false;
    private boolean mForceDelete = false;
    private boolean mKeepBrokenResources = false;
    private boolean mBakDeb = true;
    private boolean mCompressResources = false;
    private boolean mAnalysisMode = false;
    private int mApi = 15;
}
