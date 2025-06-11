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
import brut.androlib.meta.ApkInfo;
import brut.androlib.meta.UsesFramework;
import brut.androlib.res.Framework;
import brut.androlib.res.decoder.BinaryResourceParser;
import brut.directory.Directory;
import brut.directory.DirectoryException;
import brut.directory.ExtFile;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;

public class ResTable {
    private static final Logger LOGGER = Logger.getLogger(ResTable.class.getName());

    private final ApkInfo mApkInfo;
    private final Config mConfig;
    private final Map<Integer, ResPackage> mPackages;
    private final Set<ResPackage> mLibPackages;
    private final Set<ResPackage> mFramePackages;
    private final Map<Integer, String> mDynamicRefTable;
    private ResPackage mMainPackage;

    public ResTable(ApkInfo apkInfo, Config config) {
        mApkInfo = apkInfo;
        mConfig = config;
        mPackages = new HashMap<>();
        mLibPackages = new HashSet<>();
        mFramePackages = new HashSet<>();
        mDynamicRefTable = new LinkedHashMap<>(); // must preserve order
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
        List<ResPackage> pkgs = loadResPackagesFromApk(apkFile, mConfig.isKeepBrokenResources());

        ResPackage pkg;
        if (pkgs.isEmpty()) {
            // Empty resources.arsc, create a dummy package.
            pkg = new ResPackage(this, 0, null);
        } else {
            pkg = selectPackageWithMostEntrySpecs(pkgs);
        }

        registerPackage(pkg);
        mMainPackage = pkg;
    }

    private List<ResPackage> loadResPackagesFromApk(File apkFile, boolean keepBrokenResources)
            throws AndrolibException {
        try (ExtFile file = new ExtFile(apkFile)) {
            Directory dir = file.getDirectory();
            if (!dir.containsFile("resources.arsc")) {
                throw new AndrolibException("Could not find resources.arsc in file: " + apkFile);
            }

            try (InputStream in = dir.getFileInput("resources.arsc")) {
                BinaryResourceParser parser = new BinaryResourceParser(this, keepBrokenResources, false);
                parser.parse(in);
                return parser.getPackages();
            }
        } catch (DirectoryException | IOException ex) {
            throw new AndrolibException("Could not load resources.arsc from file: " + apkFile, ex);
        }
    }

    private ResPackage selectPackageWithMostEntrySpecs(List<ResPackage> pkgs) {
        ResPackage ret = pkgs.get(0);
        int count = 0;

        for (ResPackage pkg : pkgs) {
            if (pkg.getEntrySpecCount() > count) {
                count = pkg.getEntrySpecCount();
                ret = pkg;
            }
        }

        return ret;
    }

    private void registerPackage(ResPackage pkg) throws AndrolibException {
        int id = pkg.getId();
        String name = pkg.getName();

        for (Map.Entry<Integer, ResPackage> entry : mPackages.entrySet()) {
            if (id == entry.getKey()) {
                throw new AndrolibException(String.format(
                    "Repeated package ID: %d (assigned to name: %s)",
                    id, entry.getValue().getName()));
            }
            if (name.equals(entry.getValue().getName())) {
                throw new AndrolibException(String.format(
                    "Repeated package name: %s (assigned to ID: %d)",
                    name, entry.getKey()));
            }
        }

        mPackages.put(id, pkg);
    }

    public ResPackage getCurrentPackage() throws AndrolibException {
        if (mMainPackage == null) {
            // If no main package, we directly get "android" instead.
            return getPackage(1);
        }

        return mMainPackage;
    }

    public ResPackage getPackage(int id) throws AndrolibException {
        if (id == 0 && mMainPackage != null) {
            // The package ID is 0x00. That means that a shared library is accessing its own
            // local resource, so we fix up this resource with the calling package ID.
            id = mMainPackage.getId();
        }

        ResPackage pkg = mPackages.get(id);
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
        if (pkg.getId() != id) {
            throw new AndrolibException(String.format(
                "Unexpected package ID: %d (expected: %d)", pkg.getId(), id));
        }
        if (!pkg.getName().equals(name)) {
            throw new AndrolibException(String.format(
                "Unexpected package name: %s (expected: %s)", pkg.getName(), id));
        }

        registerPackage(pkg);
        mLibPackages.add(pkg);
        return pkg;
    }

    private ResPackage loadFrameworkPackage(int id) throws AndrolibException {
        File apkFile = new Framework(mConfig).getApkFile(id);

        ResPackage pkg = loadResPackageFromApk(apkFile, true);
        if (pkg.getId() != id) {
            throw new AndrolibException(String.format(
                "Unexpected package ID: %d (expected: %d)", pkg.getId(), id));
        }

        registerPackage(pkg);
        mFramePackages.add(pkg);
        return pkg;
    }

    private ResPackage loadResPackageFromApk(File apkFile, boolean keepBrokenResources)
            throws AndrolibException {
        LOGGER.info("Loading resource table from file: " + apkFile);
        List<ResPackage> pkgs = loadResPackagesFromApk(apkFile, keepBrokenResources);
        if (pkgs.isEmpty()) {
            throw new AndrolibException("No packages in resources.arsc in file: " + apkFile);
        }

        return selectPackageWithMostEntrySpecs(pkgs);
    }

    public ResEntrySpec getEntrySpec(ResId id) throws AndrolibException {
        return getPackage(id.getPackageId()).getEntrySpec(id);
    }

    public ResEntry getDefaultEntry(ResId id) throws AndrolibException {
        return getPackage(id.getPackageId()).getDefaultEntry(id);
    }

    public ResEntry getEntry(ResId id, ResConfig config) throws AndrolibException {
        return getPackage(id.getPackageId()).getEntry(id, config);
    }

    public void addDynamicRefPackage(int id, String name) {
        assert id != 0;
        for (Map.Entry<Integer, String> entry : mDynamicRefTable.entrySet()) {
            if (id == entry.getKey()) {
                if (name.equals(entry.getValue())) {
                    // Duplicate definitions are normal.
                    return;
                }
                LOGGER.warning(String.format(
                    "Repeated dynamic ref package ID: %d (assigned to name: %s)",
                    id, entry.getValue()));
                return;
            }
            if (name.equals(entry.getValue())) {
                LOGGER.warning(String.format(
                    "Repeated dynamic ref package name: %s (assigned to ID: %d)",
                    name, entry.getKey()));
                return;
            }
        }

        mDynamicRefTable.put(id, name);
    }

    public int getDynamicRefPackageId(String name) {
        for (Map.Entry<Integer, String> entry : mDynamicRefTable.entrySet()) {
            if (name.equals(entry.getValue())) {
                return entry.getKey();
            }
        }

        LOGGER.warning("Package ID not defined for dynamic ref package: " + name);
        return 0;
    }

    public void updateApkInfo() {
        if (mMainPackage != null) {
            mApkInfo.getResourcesInfo().setPackageId(Integer.toString(mMainPackage.getId()));
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
