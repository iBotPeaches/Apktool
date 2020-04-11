/*
 *  Copyright (C) 2010 Ryszard Wiśniewski <brut.alll@gmail.com>
 *  Copyright (C) 2010 Connor Tumbleson <connor.tumbleson@gmail.com>
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
package brut.androlib.res.data;

import brut.androlib.AndrolibException;
import brut.androlib.err.UndefinedResObject;
import brut.androlib.meta.VersionInfo;
import brut.androlib.res.AndrolibResources;
import brut.androlib.res.data.value.ResValue;
import java.util.*;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public class ResTable {
    private final AndrolibResources mAndRes;

    private final Map<Integer, ResPackage> mPackagesById = new HashMap<Integer, ResPackage>();
    private final Map<String, ResPackage> mPackagesByName = new HashMap<String, ResPackage>();
    private final Set<ResPackage> mMainPackages = new LinkedHashSet<ResPackage>();
    private final Set<ResPackage> mFramePackages = new LinkedHashSet<ResPackage>();

    private String mPackageRenamed;
    private String mPackageOriginal;
    private int mPackageId;
    private boolean mAnalysisMode = false;
    private boolean mSharedLibrary = false;
    private boolean mSparseResources = false;

    private Map<String, String> mSdkInfo = new LinkedHashMap<>();
    private VersionInfo mVersionInfo = new VersionInfo();

    public ResTable() {
        mAndRes = null;
    }

    public ResTable(AndrolibResources andRes) {
        mAndRes = andRes;
    }

    public ResResSpec getResSpec(int resID) throws AndrolibException {
        // The pkgId is 0x00. That means a shared library is using its
        // own resource, so lie to the caller replacing with its own
        // packageId
        if (resID >> 24 == 0) {
            int pkgId = (mPackageId == 0 ? 2 : mPackageId);
            resID = (0xFF000000 & (pkgId << 24)) | resID;
        }
        return getResSpec(new ResID(resID));
    }

    public ResResSpec getResSpec(ResID resID) throws AndrolibException {
        return getPackage(resID.package_).getResSpec(resID);
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
        if (mAndRes != null) {
            return mAndRes.loadFrameworkPkg(this, id, mAndRes.apkOptions.frameworkTag);
        }
        throw new UndefinedResObject(String.format("package: id=%d", id));
    }

    public ResPackage getHighestSpecPackage() throws AndrolibException {
        int id = 0;
        int value = 0;
        for (ResPackage resPackage : mPackagesById.values()) {
            if (resPackage.getResSpecCount() > value && !resPackage.getName().equalsIgnoreCase("android")) {
                value = resPackage.getResSpecCount();
                id = resPackage.getId();
            }
        }
        // if id is still 0, we only have one pkgId which is "android" -> 1
        return (id == 0) ? getPackage(1) : getPackage(id);
    }

    public ResPackage getCurrentResPackage() throws AndrolibException {
        ResPackage pkg = mPackagesById.get(mPackageId);

        if (pkg != null) {
            return pkg;
        } else {
            if (mMainPackages.size() == 1) {
                return mMainPackages.iterator().next();
            }
            return getHighestSpecPackage();
        }
    }

    public ResPackage getPackage(String name) throws AndrolibException {
        ResPackage pkg = mPackagesByName.get(name);
        if (pkg == null) {
            throw new UndefinedResObject("package: name=" + name);
        }
        return pkg;
    }

    public boolean hasPackage(int id) {
        return mPackagesById.containsKey(id);
    }

    public boolean hasPackage(String name) {
        return mPackagesByName.containsKey(name);
    }

    public ResValue getValue(String package_, String type, String name) throws AndrolibException {
        return getPackage(package_).getType(type).getResSpec(name).getDefaultResource().getValue();
    }

    public void addPackage(ResPackage pkg, boolean main) throws AndrolibException {
        Integer id = pkg.getId();
        if (mPackagesById.containsKey(id)) {
            throw new AndrolibException("Multiple packages: id=" + id.toString());
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

    public void setAnalysisMode(boolean mode) {
        mAnalysisMode = mode;
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
        mSharedLibrary = flag;
    }

    public void setSparseResources(boolean flag) {
        mSparseResources = flag;
    }

    public void clearSdkInfo() {
        mSdkInfo.clear();
    }

    public void addSdkInfo(String key, String value) {
        mSdkInfo.put(key, value);
    }

    public void setVersionName(String versionName) {
        mVersionInfo.versionName = versionName;
    }

    public void setVersionCode(String versionCode) {
        mVersionInfo.versionCode = versionCode;
    }

    public VersionInfo getVersionInfo() {
        return mVersionInfo;
    }

    public Map<String, String> getSdkInfo() {
        return mSdkInfo;
    }

    public boolean getAnalysisMode() {
        return mAnalysisMode;
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

    public boolean getSharedLibrary() {
        return mSharedLibrary;
    }

    public boolean getSparseResources() {
        return mSparseResources;
    }
}
