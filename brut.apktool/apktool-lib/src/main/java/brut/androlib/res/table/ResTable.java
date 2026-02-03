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
package brut.androlib.res.table;

import brut.androlib.Config;
import brut.androlib.exceptions.AndrolibException;
import brut.androlib.exceptions.UndefinedResObjectException;
import brut.androlib.meta.ApkInfo;
import brut.androlib.res.Framework;
import brut.androlib.res.decoder.BinaryResourceParser;
import brut.common.Log;
import brut.directory.DirectoryException;
import brut.directory.ExtFile;
import brut.directory.ZipRODirectory;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ResTable {
    private static final String TAG = ResTable.class.getName();

    public static final int SYS_PACKAGE_ID = 0x01;
    public static final int APP_PACKAGE_ID = 0x7F;

    private final ApkInfo mApkInfo;
    private final Config mConfig;
    private final Map<Integer, ResPackageGroup> mPackageGroups;
    private final List<Integer> mLibPackageIds;
    private final List<Integer> mFramePackageIds;
    private final Map<Integer, String> mDynamicRefTable;
    private int mNextPackageId;
    private ResPackage mMainPackage;

    public ResTable(ApkInfo apkInfo, Config config) {
        assert apkInfo != null && config != null;
        mApkInfo = apkInfo;
        mConfig = config;
        mPackageGroups = new LinkedHashMap<>();
        mLibPackageIds = new ArrayList<>();
        mFramePackageIds = new ArrayList<>();
        mDynamicRefTable = new LinkedHashMap<>();
        mNextPackageId = SYS_PACKAGE_ID + 1;
    }

    public ApkInfo getApkInfo() {
        return mApkInfo;
    }

    public Config getConfig() {
        return mConfig;
    }

    public ResPackage getMainPackage() {
        return mMainPackage;
    }

    public Collection<Integer> getLibPackageIds() {
        return mLibPackageIds;
    }

    public Collection<Integer> getFramePackageIds() {
        return mFramePackageIds;
    }

    public void load() throws AndrolibException {
        if (mMainPackage != null) {
            return;
        }

        Log.i(TAG, "Loading resource table...");
        ExtFile apkFile = mApkInfo.getApkFile();

        ZipRODirectory zipDir;
        try {
            zipDir = (ZipRODirectory) apkFile.getDirectory();
        } catch (DirectoryException ex) {
            throw new AndrolibException("Could not open apk file: " + apkFile, ex);
        }

        loadPackagesFromApk(apkFile, zipDir, true);

        ResPackageGroup pkgGroup;
        if (mPackageGroups.isEmpty()) {
            // Empty resources.arsc, create a dummy package group.
            pkgGroup = new ResPackageGroup(this, 0, "");
            mPackageGroups.put(0, pkgGroup);
        } else if (mPackageGroups.containsKey(APP_PACKAGE_ID)) {
            // Prefer the standard app package group.
            pkgGroup = mPackageGroups.get(APP_PACKAGE_ID);
        } else {
            // Fall back to the first package group in the table.
            pkgGroup = mPackageGroups.values().iterator().next();
        }

        mMainPackage = pkgGroup.getBasePackage();
    }

    private void loadPackagesFromApk(File apkFile, ZipRODirectory zipDir, boolean isMainPackage)
            throws AndrolibException {
        try {
            if (!zipDir.containsFile("resources.arsc")) {
                throw new AndrolibException("Could not find resources.arsc in file: " + apkFile);
            }

            try (InputStream in = zipDir.getFileInput("resources.arsc")) {
                BinaryResourceParser parser = isMainPackage
                    ? new BinaryResourceParser(this, mConfig.isKeepBrokenResources(), mConfig.isDecodeResolveGreedy())
                    : new BinaryResourceParser(this, true, true);
                parser.parse(in);

                // Only flag the app for the main package.
                if (isMainPackage) {
                    if (parser.isSparseEntries()) {
                        mApkInfo.getResourcesInfo().setSparseEntries(true);
                    }
                    if (parser.isCompactEntries()) {
                        mApkInfo.getResourcesInfo().setCompactEntries(true);
                    }
                }
            }
        } catch (DirectoryException | IOException ex) {
            throw new AndrolibException("Could not load resources.arsc from file: " + apkFile, ex);
        }
    }

    public boolean hasPackageGroup(int id) {
        return mPackageGroups.containsKey(id);
    }

    public ResPackageGroup getPackageGroup(int id) throws UndefinedResObjectException {
        ResPackageGroup pkgGroup = mPackageGroups.get(id);
        if (pkgGroup == null) {
            throw new UndefinedResObjectException(String.format("package group: id=0x%02x", id));
        }
        return pkgGroup;
    }

    public ResPackageGroup addPackageGroup(int id, String name) throws AndrolibException {
        ResPackageGroup pkgGroup = mPackageGroups.get(id);
        if (pkgGroup != null) {
            throw new AndrolibException(String.format("Repeated package group: id=0x%02x, name=%s", id, name));
        }

        // If the package ID is 0x00 and the main package is loaded, that means that a shared library is being loaded,
        // so we change it to the reference package ID defined in the dynamic reference table, or assign it the next
        // available ID.
        if (id == 0 && mMainPackage != null) {
            id = getDynamicRefPackageId(name);
            if (id == 0) {
                id = mNextPackageId++;
            }
        }

        pkgGroup = new ResPackageGroup(this, id, name);
        mPackageGroups.put(id, pkgGroup);
        return pkgGroup;
    }

    public int getPackageGroupCount() {
        return mPackageGroups.size();
    }

    public Collection<ResPackageGroup> listPackageGroups() {
        return mPackageGroups.values();
    }

    public ResPackageGroup resolvePackageGroup(int id) throws AndrolibException {
        ResPackageGroup pkgGroup = mPackageGroups.get(id);
        if (pkgGroup == null) {
            pkgGroup = loadLibraryById(id);
            if (pkgGroup == null) {
                pkgGroup = loadFrameworkById(id);
            }
        }
        return pkgGroup;
    }

    private ResPackageGroup loadLibraryById(int id) throws AndrolibException {
        String name = mDynamicRefTable.get(id);
        String[] libFiles = mConfig.getLibraryFiles();
        if (name == null || libFiles == null) {
            return null;
        }

        File apkFile = null;
        for (String libEntry : libFiles) {
            String[] parts = libEntry.split(":", 2);
            if (parts.length == 2 && name.equals(parts[0])) {
                apkFile = new File(parts[1]);
                break;
            }
        }
        if (apkFile == null) {
            return null;
        }

        loadPackagesFromApk(apkFile);

        ResPackageGroup pkgGroup = mPackageGroups.get(id);
        if (pkgGroup == null) {
            throw new AndrolibException(String.format("Library package not found: id=0x%02x", id));
        }

        mLibPackageIds.add(id);
        return pkgGroup;
    }

    private ResPackageGroup loadFrameworkById(int id) throws AndrolibException {
        File apkFile = new Framework(mConfig).getApkFile(id);
        loadPackagesFromApk(apkFile);

        ResPackageGroup pkgGroup = mPackageGroups.get(id);
        if (pkgGroup == null) {
            throw new AndrolibException(String.format("Framework package not found: id=0x%02x", id));
        }

        mFramePackageIds.add(id);
        return pkgGroup;
    }

    private void loadPackagesFromApk(File apkFile) throws AndrolibException {
        Log.i(TAG, "Loading resource table from file: " + apkFile);

        try (ZipRODirectory zipDir = new ZipRODirectory(apkFile)) {
            loadPackagesFromApk(apkFile, zipDir, false);
        } catch (DirectoryException ex) {
            throw new AndrolibException("Could not open apk file: " + apkFile, ex);
        }
    }

    public ResEntrySpec resolve(ResId resId) throws AndrolibException {
        return resolvePackageGroup(resId.pkgId()).getEntrySpec(resId.typeId(), resId.entryId());
    }

    public ResEntry resolveEntry(ResId resId) throws AndrolibException {
        return resolvePackageGroup(resId.pkgId()).getEntry(resId.typeId(), resId.entryId());
    }

    public String getDynamicRefPackageName(int id) {
        String name = mDynamicRefTable.get(id);
        if (name == null) {
            Log.w(TAG, "Dynamic ref package name not defined for package ID: 0x02x", id);
        }
        return name;
    }

    public int getDynamicRefPackageId(String name) {
        for (Map.Entry<Integer, String> entry : mDynamicRefTable.entrySet()) {
            if (name.equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        Log.w(TAG, "Dynamic ref package ID not defined for package: " + name);
        return 0;
    }

    public void addDynamicRefPackage(int id, String name) {
        // Ensure the package ID isn't already mapped to a different name.
        String existing = mDynamicRefTable.get(id);
        if (existing != null) {
            if (!existing.equals(name)) {
                Log.w(TAG, "Repeated dynamic ref package ID: %s (assigned to name: %s)", id, existing);
                return;
            }
            // Identical mappings are normal.
            return;
        }

        // Ensure the package name isn't already mapped to a different ID.
        for (Map.Entry<Integer, String> entry : mDynamicRefTable.entrySet()) {
            if (name.equals(entry.getValue())) {
                Log.w(TAG, "Repeated dynamic ref package name: %s (assigned to ID: %s)", name, entry.getKey());
                return;
            }
        }

        mDynamicRefTable.put(id, name);
    }
}
