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
import brut.androlib.meta.*;
import brut.androlib.res.Framework;
import brut.common.BrutException;
import brut.util.OS;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class AaptInvoker {
    private static final Logger LOGGER = Logger.getLogger(AaptInvoker.class.getName());

    private final ApkInfo mApkInfo;
    private final Config mConfig;

    public AaptInvoker(ApkInfo apkInfo, Config config) {
        mApkInfo = apkInfo;
        mConfig = config;
    }

    public void invoke(File outApk, File manifest, File resDir) throws AndrolibException {
        SdkInfo sdkInfo = mApkInfo.getSdkInfo();
        VersionInfo versionInfo = mApkInfo.getVersionInfo();
        ResourcesInfo resourcesInfo = mApkInfo.getResourcesInfo();

        String aaptPath = mConfig.getAaptBinary();
        if (aaptPath == null || aaptPath.isEmpty()) {
            try {
                aaptPath = AaptManager.getBinaryFile().getPath();
            } catch (AndrolibException ex) {
                aaptPath = AaptManager.getBinaryName();
                LOGGER.warning(aaptPath + ": " + ex.getMessage() + " (defaulting to $PATH binary)");
            }
        }

        List<String> cmd = new ArrayList<>();
        File resZip = null;

        if (resDir != null) {
            resZip = new File(resDir.getParent(), "build/resources.zip");
            OS.rmfile(resZip);

            // Compile the files into flat arsc files.
            cmd.add(aaptPath);
            cmd.add("compile");

            cmd.add("--dir");
            cmd.add(resDir.getPath());

            // Treats error that used to be valid in aapt1 as warnings in aapt2.
            cmd.add("--legacy");

            cmd.add("-o");
            cmd.add(resZip.getPath());

            if (mConfig.isVerbose()) {
                cmd.add("-v");
            }

            if (mConfig.isNoCrunch()) {
                cmd.add("--no-crunch");
            }

            try {
                OS.exec(cmd.toArray(new String[0]));
                LOGGER.fine("aapt2 compile command ran: ");
                LOGGER.fine(cmd.toString());
            } catch (BrutException ex) {
                throw new AndrolibException(ex);
            }

            cmd.clear();
        }

        if (manifest == null) {
            return;
        }

        // Link resources to the final apk.
        cmd.add(aaptPath);
        cmd.add("link");

        cmd.add("-o");
        cmd.add(outApk.getPath());

        cmd.add("--manifest");
        cmd.add(manifest.getPath());

        if (sdkInfo.getMinSdkVersion() != null) {
            cmd.add("--min-sdk-version");
            cmd.add(sdkInfo.getMinSdkVersion());
        }
        if (sdkInfo.getTargetSdkVersion() != null) {
            cmd.add("--target-sdk-version");
            cmd.add(sdkInfo.getTargetSdkVersion());
        }
        if (versionInfo.getVersionCode() != null) {
            cmd.add("--version-code");
            cmd.add(versionInfo.getVersionCode());
        }
        if (versionInfo.getVersionName() != null) {
            cmd.add("--version-name");
            cmd.add(versionInfo.getVersionName());
        }
        if (resourcesInfo.getPackageId() != null) {
            int pkgId = Integer.parseInt(resourcesInfo.getPackageId());
            if (pkgId == 0) {
                cmd.add("--shared-lib");
            } else if (pkgId > 1) {
                cmd.add("--package-id");
                cmd.add(Integer.toString(pkgId));
                if (pkgId < 0x7F) {
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
            for (Map.Entry<String, Boolean> entry : mApkInfo.getFeatureFlags().entrySet()) {
                featureFlags.add(entry.getKey() + "=" + entry.getValue());
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

        for (File includeFile : getIncludeFiles()) {
            cmd.add("-I");
            cmd.add(includeFile.getPath());
        }
        if (mConfig.isVerbose()) {
            cmd.add("-v");
        }
        if (resZip != null) {
            cmd.add(resZip.getPath());
        }

        try {
            OS.exec(cmd.toArray(new String[0]));
            LOGGER.fine("aapt2 link command ran: ");
            LOGGER.fine(cmd.toString());
        } catch (BrutException ex) {
            throw new AndrolibException(ex);
        }
    }

    private List<File> getIncludeFiles() throws AndrolibException {
        List<File> files = new ArrayList<>();

        UsesFramework usesFramework = mApkInfo.getUsesFramework();
        List<Integer> frameworkIds = usesFramework.getIds();
        if (!frameworkIds.isEmpty()) {
            Framework framework = new Framework(mConfig);
            String tag = usesFramework.getTag();
            for (Integer id : frameworkIds) {
                files.add(framework.getApkFile(id, tag));
            }
        }

        List<String> usesLibrary = mApkInfo.getUsesLibrary();
        if (!usesLibrary.isEmpty()) {
            String[] libFiles = mConfig.getLibraryFiles();
            for (String name : usesLibrary) {
                File libFile = null;
                if (libFiles != null) {
                    for (String libEntry : libFiles) {
                        String[] parts = libEntry.split(":", 2);
                        if (parts.length == 2 && name.equals(parts[0])) {
                            libFile = new File(parts[1]);
                            break;
                        }
                    }
                }
                if (libFile != null) {
                    files.add(libFile);
                } else {
                    LOGGER.warning("Shared library was not provided: " + name);
                }
            }
        }

        return files;
    }
}
