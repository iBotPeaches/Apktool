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

import brut.androlib.Config;
import brut.androlib.exceptions.AndrolibException;
import brut.androlib.exceptions.UndefinedResObjectException;
import brut.androlib.meta.ApkInfo;
import brut.androlib.meta.UsesFramework;
import brut.androlib.res.Framework;
import brut.androlib.res.data.value.ResValue;
import brut.androlib.res.decoder.ARSCDecoder;
import brut.directory.DirectoryException;
import brut.directory.ExtFile;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;

public class ResTable {
    private static final Logger LOGGER = Logger.getLogger(ResTable.class.getName());

    private final ApkInfo mApkInfo;
    private final Config mConfig;
    private final Map<Integer, ResPackage> mPackagesById;
    private final Map<String, ResPackage> mPackagesByName;
    private ResPackage mMainPackage;
    private final Set<ResPackage> mLibPackages;
    private final Set<ResPackage> mFramePackages;
    private final Map<Integer, String> mDynamicRefTable;

    public ResTable(ApkInfo apkInfo, Config config) {
        mApkInfo = apkInfo;
        mConfig = config;
        mPackagesById = new HashMap<>();
        mPackagesByName = new HashMap<>();
        mLibPackages = new HashSet<>();
        mFramePackages = new HashSet<>();
        mDynamicRefTable = new LinkedHashMap<>();
    }

    public ApkInfo getApkInfo() {
        return mApkInfo;
    }

    public Config getConfig() {
        return mConfig;
    }

    public boolean isMainPackageLoaded() {
        return mMainPackage != null;
    }

    public ResPackage getMainPackage() throws AndrolibException {
        if (mMainPackage == null) {
            throw new AndrolibException("Main package has not been loaded");
        }
        return mMainPackage;
    }

    public void loadMainPackage() throws AndrolibException {
        LOGGER.info("Loading resource table...");
        File apkFile = mApkInfo.getApkFile();
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
                pkg = selectPackageWithMostResSpecs(pkgs);
                break;
        }

        registerPackage(pkg);
        mMainPackage = pkg;
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

    private ResPackage selectPackageWithMostResSpecs(ResPackage[] pkgs) {
        int count = 0;
        int index = 0;

        for (int i = 0; i < pkgs.length; i++) {
            ResPackage pkg = pkgs[i];
            if (pkg.getResSpecCount() > count && !pkg.getName().equals("android")) {
                count = pkg.getResSpecCount();
                index = i;
            }
        }

        return pkgs[index];
    }

    private void registerPackage(ResPackage pkg) throws AndrolibException {
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
    }

    public ResPackage getCurrentPackage() throws AndrolibException {
        if (mMainPackage == null) {
            // if no main package, we directly get "android" instead
            return getPackage(1);
        }

        return mMainPackage;
    }

    public ResPackage getPackage(String name) throws AndrolibException {
        ResPackage pkg = mPackagesByName.get(name);
        if (pkg == null) {
            throw new UndefinedResObjectException("package: name=" + name);
        }

        return pkg;
    }

    public ResPackage getPackage(int id) throws AndrolibException {
        ResPackage pkg = mPackagesById.get(id);
        if (pkg == null) {
            pkg = loadLibraryPackage(id);
            if (pkg == null) {
                pkg = loadFrameworkPackage(id);
            }
        }

        return pkg;
    }

    private ResPackage loadLibraryPackage(int id) throws AndrolibException {
        String name = mDynamicRefTable.get(id);
        String[] libFiles = mConfig.getLibraryFiles();
        File apkFile = null;

        if (name != null && libFiles != null) {
            for (String libEntry : libFiles) {
                String[] parts = libEntry.split(":", 2);
                if (parts.length == 2 && name.equals(parts[0])) {
                    apkFile = new File(parts[1]);
                    break;
                }
            }
        }
        if (apkFile == null) {
            return null;
        }

        ResPackage pkg = loadResPackageFromApk(apkFile, true);
        if (id != pkg.getId()) {
            throw new AndrolibException("Expected pkg of id: " + id + ", got: " + pkg.getId());
        }
        if (!name.equals(pkg.getName())) {
            throw new AndrolibException("Expected pkg of name: " + name + ", got: " + pkg.getName());
        }

        registerPackage(pkg);
        mLibPackages.add(pkg);
        return pkg;
    }

    private ResPackage loadFrameworkPackage(int id) throws AndrolibException {
        Framework framework = new Framework(mConfig);
        File apkFile = framework.getApkFile(id, mConfig.getFrameworkTag());

        ResPackage pkg = loadResPackageFromApk(apkFile, true);
        if (id != pkg.getId()) {
            throw new AndrolibException("Expected pkg of id: " + id + ", got: " + pkg.getId());
        }

        registerPackage(pkg);
        mFramePackages.add(pkg);
        return pkg;
    }

    private ResPackage loadResPackageFromApk(File apkFile, boolean keepBrokenResources)
            throws AndrolibException {
        LOGGER.info("Loading resource table from file: " + apkFile);
        ResPackage[] pkgs = loadResPackagesFromApk(apkFile, keepBrokenResources);
        ResPackage pkg;

        switch (pkgs.length) {
            case 0:
                throw new AndrolibException("Arsc file with zero packages");
            case 1:
                pkg = pkgs[0];
                break;
            default:
                pkg = selectPackageWithMostResSpecs(pkgs);
                break;
        }

        return pkg;
    }

    public ResResSpec getResSpec(int resId) throws AndrolibException {
        if (resId >> 24 == 0) {
            // The package ID is 0x00. That means that a shared library is accessing its own
            // local resource, so we fix up this resource with the calling package ID.
            resId = (resId & 0xFFFFFF) | (mMainPackage.getId() << 24);
        }
        return getResSpec(new ResID(resId));
    }

    public ResResSpec getResSpec(ResID resId) throws AndrolibException {
        return getPackage(resId.getPackageId()).getResSpec(resId);
    }

    public ResValue getValue(String pkg, String type, String name) throws AndrolibException {
        return getPackage(pkg).getType(type).getResSpec(name).getDefaultResource().getValue();
    }

    public void addDynamicRefPackage(int pkgId, String pkgName) {
        mDynamicRefTable.put(pkgId, pkgName);
    }

    public int getDynamicRefPackageId(String pkgName) {
        for (Map.Entry<Integer, String> entry : mDynamicRefTable.entrySet()) {
            if (pkgName.equals(entry.getValue())) {
                return entry.getKey();
            }
        }

        return 0;
    }

    public void updateApkInfo() {
        if (mMainPackage != null) {
            mApkInfo.getPackageInfo().setForcedPackageId(Integer.toString(mMainPackage.getId()));
        }

        if (!mFramePackages.isEmpty()) {
            UsesFramework usesFramework = mApkInfo.getUsesFramework();
            List<Integer> frameworkIds = usesFramework.getIds();
            int[] ids = new int[mFramePackages.size()];

            int i = 0;
            for (ResPackage pkg : mFramePackages) {
                ids[i++] = pkg.getId();
            }
            Arrays.sort(ids);

            for (int id : ids) {
                frameworkIds.add(id);
            }

            usesFramework.setTag(mConfig.getFrameworkTag());
        }

        if (!mLibPackages.isEmpty()) {
            List<String> usesLibrary = mApkInfo.getUsesLibrary();
            int[] ids = new int[mLibPackages.size()];

            int i = 0;
            for (ResPackage pkg : mLibPackages) {
                ids[i++] = pkg.getId();
            }
            Arrays.sort(ids);

            for (int id : ids) {
                usesLibrary.add(mDynamicRefTable.get(id));
            }
        }
    }
}
