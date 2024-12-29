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
package brut.androlib.res.data;

import brut.androlib.ApkDecoder;
import brut.androlib.Config;
import brut.androlib.exceptions.AndrolibException;
import brut.androlib.exceptions.UndefinedResObjectException;
import brut.androlib.apk.ApkInfo;
import brut.androlib.apk.UsesFramework;
import brut.androlib.res.Framework;
import brut.androlib.res.data.value.ResValue;
import brut.androlib.res.decoder.ARSCDecoder;
import brut.androlib.res.xml.ResXmlUtils;
import brut.directory.DirectoryException;
import brut.directory.ExtFile;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;

public class ResTable {
    private static final Logger LOGGER = Logger.getLogger(ApkDecoder.class.getName());

    private final ApkInfo mApkInfo;
    private final Config mConfig;
    private final Map<Integer, ResPackage> mPackagesById;
    private final Map<String, ResPackage> mPackagesByName;
    private final Set<ResPackage> mMainPackages;
    private final Set<ResPackage> mFramePackages;

    private String mPackageRenamed;
    private String mPackageOriginal;
    private int mPackageId;
    private boolean mMainPkgLoaded;

    public ResTable(ApkInfo apkInfo, Config config) {
        mApkInfo = apkInfo;
        mConfig = config;
        mPackagesById = new HashMap<>();
        mPackagesByName = new HashMap<>();
        mMainPackages = new LinkedHashSet<>();
        mFramePackages = new LinkedHashSet<>();
    }

    public ApkInfo getApkInfo() {
        return mApkInfo;
    }

    public Config getConfig() {
        return mConfig;
    }

    public boolean isMainPkgLoaded() {
        return mMainPkgLoaded;
    }

    public ResResSpec getResSpec(int resId) throws AndrolibException {
        // The pkgId is 0x00. That means a shared library is using its
        // own resource, so lie to the caller replacing with its own
        // packageId
        if (resId >> 24 == 0) {
            int pkgId = mPackageId == 0 ? 2 : mPackageId;
            resId = (0xFF000000 & (pkgId << 24)) | resId;
        }
        return getResSpec(new ResID(resId));
    }

    public ResResSpec getResSpec(ResID resId) throws AndrolibException {
        return getPackage(resId.getPackageId()).getResSpec(resId);
    }

    public Set<ResPackage> listMainPackages() {
        return mMainPackages;
    }

    public Set<ResPackage> listFramePackages() {
        return mFramePackages;
    }

    public ResPackage getPackage(int id) throws AndrolibException {
        ResPackage pkg = mPackagesById.get(id);
        if (pkg != null) {
            return pkg;
        }

        pkg = loadFrameworkPkg(id);
        addPackage(pkg, false);
        return pkg;
    }

    private ResPackage selectPkgWithMostResSpecs(ResPackage[] pkgs) {
        int id = 0;
        int value = 0;
        int index = 0;

        for (int i = 0; i < pkgs.length; i++) {
            ResPackage resPackage = pkgs[i];
            if (resPackage.getResSpecCount() > value && !resPackage.getName().equals("android")) {
                value = resPackage.getResSpecCount();
                id = resPackage.getId();
                index = i;
            }
        }

        // if id is still 0, we only have one pkgId which is "android" -> 1
        return pkgs[id == 0 ? 0 : index];
    }

    public void loadMainPkg(File apkFile) throws AndrolibException {
        LOGGER.info("Loading resource table...");
        ResPackage[] pkgs = loadResPackagesFromApk(apkFile, mConfig.isKeepBrokenResources());
        ResPackage pkg;

        switch (pkgs.length) {
            case 0:
                pkg = new ResPackage(this, 0, null);
                break;
            case 1:
                pkg = pkgs[0];
                break;
            case 2:
                LOGGER.warning("Skipping package group: " + pkgs[0].getName());
                pkg = pkgs[1];
                break;
            default:
                pkg = selectPkgWithMostResSpecs(pkgs);
                break;
        }

        addPackage(pkg, true);
        mMainPkgLoaded = true;
    }

    private ResPackage loadFrameworkPkg(int id) throws AndrolibException {
        Framework framework = new Framework(mConfig);
        File apkFile = framework.getApkFile(id, mConfig.getFrameworkTag());

        LOGGER.info("Loading resource table from file: " + apkFile);
        ResPackage[] pkgs = loadResPackagesFromApk(apkFile, true);

        ResPackage pkg;
        if (pkgs.length > 1) {
            pkg = selectPkgWithMostResSpecs(pkgs);
        } else if (pkgs.length == 1) {
            pkg = pkgs[0];
        } else {
            throw new AndrolibException("Arsc files with zero packages");
        }

        if (pkg.getId() != id) {
            throw new AndrolibException("Expected pkg of id: " + id + ", got: " + pkg.getId());
        }
        return pkg;
    }

    private ResPackage[] loadResPackagesFromApk(File apkFile, boolean keepBrokenResources)
            throws AndrolibException {
        try (
            ExtFile inFile = new ExtFile(apkFile);
            BufferedInputStream in = new BufferedInputStream(
                inFile.getDirectory().getFileInput("resources.arsc"))
        ) {
            ARSCDecoder decoder = new ARSCDecoder(in, this, false, keepBrokenResources);
            return decoder.decode().getPackages();
        } catch (DirectoryException | IOException ex) {
            throw new AndrolibException("Could not load resources.arsc from file: " + apkFile, ex);
        }
    }

    public ResPackage getHighestSpecPackage() throws AndrolibException {
        int id = 0;
        int value = 0;

        for (ResPackage resPackage : mPackagesById.values()) {
            if (resPackage.getResSpecCount() > value && !resPackage.getName().equals("android")) {
                id = resPackage.getId();
                value = resPackage.getResSpecCount();
            }
        }

        // if id is still 0, we only have one pkgId which is "android" -> 1
        return getPackage(id == 0 ? 1 : id);
    }

    public ResPackage getCurrentResPackage() throws AndrolibException {
        ResPackage pkg = mPackagesById.get(mPackageId);
        if (pkg != null) {
            return pkg;
        }

        if (mMainPackages.size() == 1) {
            return mMainPackages.iterator().next();
        }

        return getHighestSpecPackage();
    }

    public ResPackage getPackage(String name) throws AndrolibException {
        ResPackage pkg = mPackagesByName.get(name);
        if (pkg == null) {
            throw new UndefinedResObjectException("package: name=" + name);
        }

        return pkg;
    }

    public ResValue getValue(String pkg, String type, String name) throws AndrolibException {
        return getPackage(pkg).getType(type).getResSpec(name).getDefaultResource().getValue();
    }

    public void addPackage(ResPackage pkg, boolean main) throws AndrolibException {
        Integer id = pkg.getId();
        if (mPackagesById.containsKey(id)) {
            throw new AndrolibException("Multiple packages: id=" + id);
        }
        String name = pkg.getName();
        if (mPackagesByName.containsKey(name)) {
            throw new AndrolibException("Multiple packages: name=" + name);
        }

        mPackagesById.put(id, pkg);
        mPackagesByName.put(name, pkg);

        if (main) {
            mMainPackages.add(pkg);
        } else {
            mFramePackages.add(pkg);
        }
    }

    public void setPackageRenamed(String pkg) {
        mPackageRenamed = pkg;
    }

    public void setPackageOriginal(String pkg) {
        mPackageOriginal = pkg;
    }

    public void setPackageId(int id) {
        mPackageId = id;
    }

    public void setSharedLibrary(boolean flag) {
        mApkInfo.sharedLibrary = flag;
    }

    public void setSparseResources(boolean flag) {
        mApkInfo.sparseResources = flag;
    }

    public void setCompactEntries(boolean flag) {
        mApkInfo.compactEntries = flag;
    }

    public void addSdkInfo(String key, String value) {
        mApkInfo.sdkInfo.put(key, value);
    }

    public void setVersionName(String versionName) {
        mApkInfo.versionInfo.versionName = versionName;
    }

    public void setVersionCode(String versionCode) {
        mApkInfo.versionInfo.versionCode = versionCode;
    }

    public String getPackageRenamed() {
        return mPackageRenamed;
    }

    public String getPackageOriginal() {
        return mPackageOriginal;
    }

    public int getPackageId() {
        return mPackageId;
    }

    public boolean getSparseResources() {
        return mApkInfo.sparseResources;
    }

    private boolean isFrameworkApk() {
        for (ResPackage pkg : mMainPackages) {
            if (pkg.getId() > 0 && pkg.getId() < 64) {
                return true;
            }
        }
        return false;
    }

    public void initApkInfo(ApkInfo apkInfo, File apkDir) throws AndrolibException {
        apkInfo.isFrameworkApk = isFrameworkApk();
        apkInfo.usesFramework = getUsesFramework();

        if (!mApkInfo.sdkInfo.isEmpty()) {
            updateSdkInfoFromResources(apkDir);
        }

        initPackageInfo();
        loadVersionName(apkDir);
    }

    private UsesFramework getUsesFramework() {
        UsesFramework info = new UsesFramework();
        Integer[] ids = new Integer[mFramePackages.size()];

        int i = 0;
        for (ResPackage pkg : mFramePackages) {
            ids[i++] = pkg.getId();
        }

        Arrays.sort(ids);
        info.ids = Arrays.asList(ids);
        info.tag = mConfig.getFrameworkTag();

        return info;
    }

    private void updateSdkInfoFromResources(File apkDir) {
        String minSdkVersion = mApkInfo.getMinSdkVersion();
        if (minSdkVersion != null) {
            String refValue = ResXmlUtils.pullValueFromIntegers(apkDir, minSdkVersion);
            if (refValue != null) {
                mApkInfo.setMinSdkVersion(refValue);
            }
        }

        String targetSdkVersion = mApkInfo.getTargetSdkVersion();
        if (targetSdkVersion != null) {
            String refValue = ResXmlUtils.pullValueFromIntegers(apkDir, targetSdkVersion);
            if (refValue != null) {
                mApkInfo.setTargetSdkVersion(refValue);
            }
        }

        String maxSdkVersion = mApkInfo.getMaxSdkVersion();
        if (maxSdkVersion != null) {
            String refValue = ResXmlUtils.pullValueFromIntegers(apkDir, maxSdkVersion);
            if (refValue != null) {
                mApkInfo.setMaxSdkVersion(refValue);
            }
        }
    }

    private void initPackageInfo() throws AndrolibException {
        String original = getPackageOriginal();
        if (original == null || original.isEmpty()) {
            return;
        }

        // only put rename-manifest-package into apktool.yml, if the change will be required
        String renamed = getPackageRenamed();
        if (renamed != null && !renamed.equals(original)) {
            mApkInfo.packageInfo.renameManifestPackage = renamed;
        }

        int id;
        try {
            id = getPackage(renamed).getId();
        } catch (UndefinedResObjectException ex) {
            id = getPackageId();
        }
        mApkInfo.packageInfo.forcedPackageId = String.valueOf(id);
    }

    private void loadVersionName(File apkDir) {
        String versionName = mApkInfo.versionInfo.versionName;
        String refValue = ResXmlUtils.pullValueFromStrings(apkDir, versionName);
        if (refValue != null) {
            mApkInfo.versionInfo.versionName = refValue;
        }
    }
}
