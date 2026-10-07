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
package brut.androlib.res;

import brut.androlib.Config;
import brut.androlib.exceptions.AndrolibException;
import brut.androlib.meta.ApkInfo;
import brut.androlib.meta.ResourcesInfo;
import brut.androlib.meta.UsesFramework;
import brut.androlib.res.Framework;
import brut.androlib.res.table.ResTable;
import brut.common.Log;
import brut.util.SystemUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AaptInvoker {
    private static final String TAG = AaptInvoker.class.getName();

    private final ApkInfo mApkInfo;
    private final Config mConfig;

    public AaptInvoker(ApkInfo apkInfo, Config config) {
        mApkInfo = apkInfo;
        mConfig = config;
    }

    public void invoke(Path outFile, Path manifest, Path resDir) throws AndrolibException {
        String binPath = mConfig.getAaptBinary();
        Path binFile;
        if (binPath != null && !binPath.isEmpty()) {
            binFile = Paths.get(binPath);
        } else {
            try {
                binFile = AaptManager.getBinaryFile();
            } catch (AndrolibException ex) {
                binFile = Paths.get(AaptManager.getBinaryName());
                Log.w(TAG, binFile + ": " + ex.getMessage() + " (defaulting to $PATH binary)");
            }
        }

        List<String> cmd = new ArrayList<>();
        Path resZip = null;

        if (resDir != null) {
            resZip = resDir.resolveSibling("build/resources.zip");
            try {
                Files.deleteIfExists(resZip);
            } catch (IOException ex) {
                throw new AndrolibException(ex);
            }

            // Compile the files into flat arsc files.
            cmd.add(binFile.toString());
            cmd.add("compile");

            if (mConfig.isVerbose()) {
                cmd.add("-v");
            }

            cmd.add("-o");
            cmd.add(resZip.toString());

            cmd.add("--dir");
            cmd.add(resDir.toString());

            // Treats error that used to be valid in aapt1 as warnings in aapt2.
            cmd.add("--legacy");

            if (mConfig.isNoCrunch()) {
                cmd.add("--no-crunch");
            }
            if (!mApkInfo.getFeatureFlags().isEmpty()) {
                List<String> featureFlags = new ArrayList<>();
                for (String flag : mApkInfo.getFeatureFlags()) {
                    featureFlags.add(flag + "=true");
                }
                cmd.add("--feature-flags");
                cmd.add(String.join(",", featureFlags));
            }

            try {
                SystemUtils.execute(cmd.toArray(new String[0]));
                Log.d(TAG, "aapt2 compile command ran: " + cmd.toString());
            } catch (IOException | InterruptedException ex) {
                throw new AndrolibException(ex);
            }

            cmd.clear();
        }

        if (manifest == null) {
            return;
        }

        // Link resources to the final apk.
        cmd.add(binFile.toString());
        cmd.add("link");

        if (mConfig.isVerbose()) {
            cmd.add("-v");
        }

        cmd.add("-o");
        cmd.add(outFile.toString());

        cmd.add("--manifest");
        cmd.add(manifest.toString());

        ResourcesInfo resourcesInfo = mApkInfo.getResourcesInfo();

        if (resourcesInfo.getPackageId() >= 0) {
            int pkgId = resourcesInfo.getPackageId();
            if (pkgId == 0) {
                cmd.add("--shared-lib");
            } else if (pkgId > ResTable.SYS_PACKAGE_ID) {
                cmd.add("--package-id");
                cmd.add(Integer.toString(pkgId));
                if (pkgId < ResTable.APP_PACKAGE_ID) {
                    cmd.add("--allow-reserved-package-id");
                }
            }
        }
        if (resourcesInfo.getPackageName() != null) {
            cmd.add("--rename-resources-package");
            cmd.add(resourcesInfo.getPackageName());
        }
        if (resourcesInfo.isSparseEntries()) {
            cmd.add("--enable-sparse-encoding");
        }
        if (resourcesInfo.isCompactEntries()) {
            cmd.add("--enable-compact-entries");
        }
        if (resourcesInfo.isKeepRawValues()) {
            cmd.add("--keep-raw-values");
        }
        if (!mApkInfo.getFeatureFlags().isEmpty()) {
            List<String> featureFlags = new ArrayList<>();
            for (String flag : mApkInfo.getFeatureFlags()) {
                featureFlags.add(flag + "=true");
            }
            cmd.add("--feature-flags");
            cmd.add(String.join(",", featureFlags));
        }

        // Disable automatic changes.
        cmd.add("--no-auto-version");
        cmd.add("--no-version-vectors");
        cmd.add("--no-version-transitions");
        cmd.add("--no-resource-deduping");
        cmd.add("--no-compile-sdk-metadata");

        // #3427 - Ignore stricter parsing during aapt2.
        cmd.add("--warn-manifest-validation");

        for (Path includeFile : getIncludeFiles()) {
            cmd.add("-I");
            cmd.add(includeFile.toString());
        }
        if (resZip != null) {
            cmd.add(resZip.toString());
        }

        try {
            SystemUtils.execute(cmd.toArray(new String[0]));
            Log.d(TAG, "aapt2 link command ran: " + cmd.toString());
        } catch (IOException | InterruptedException ex) {
            throw new AndrolibException(ex);
        }
    }

    private List<Path> getIncludeFiles() throws AndrolibException {
        List<Path> files = new ArrayList<>();

        UsesFramework usesFramework = mApkInfo.getUsesFramework();
        List<Integer> frameworkIds = usesFramework.getIds();
        if (!frameworkIds.isEmpty()) {
            Framework framework = new Framework(mConfig);
            String tag = usesFramework.getTag();
            for (Integer id : frameworkIds) {
                files.add(framework.getFile(id, tag));
            }
        }

        List<String> usesLibrary = mApkInfo.getUsesLibrary();
        if (!usesLibrary.isEmpty()) {
            Map<String, String[]> libraryFiles = mConfig.getLibraryFiles();
            for (String name : usesLibrary) {
                String[] fileNames = libraryFiles.get(name);
                if (fileNames != null) {
                    for (String fileName : fileNames) {
                        files.add(Paths.get(fileName));
                    }
                } else {
                    Log.w(TAG, "Shared library was not provided: " + name);
                }
            }
        }

        return files;
    }
}
