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
import brut.common.BrutException;
import brut.util.OS;

import java.io.File;
import java.nio.file.Paths;
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

    public void invoke(File apkFile, File manifest, File resDir, File rawDir, File assetDir, File[] include)
            throws AndrolibException {
        String aaptPath = mConfig.getAaptBinary();
        if (aaptPath == null || aaptPath.isEmpty()) {
            try {
                aaptPath = AaptManager.getBinaryFile().getPath();
            } catch (AndrolibException ex) {
                aaptPath = AaptManager.getBinaryName();
                LOGGER.warning(aaptPath + ": " + ex.getMessage() + " (defaulting to $PATH binary)");
            }
        }

        List<String> cmd;
        File resourcesZip = null;

        if (resDir != null) {
            resourcesZip = Paths.get(resDir.getParent(), "build", "resources.zip").toFile();

            if (!resourcesZip.exists()) {
                // Compile the files into flat arsc files.
                cmd = new ArrayList<>();
                cmd.add(aaptPath);
                cmd.add("compile");

                cmd.add("--dir");
                cmd.add(resDir.getAbsolutePath());

                // Treats error that used to be valid in aapt1 as warnings in aapt2.
                cmd.add("--legacy");

                cmd.add("-o");
                cmd.add(resourcesZip.getAbsolutePath());

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
            }
        }

        if (manifest == null) {
            return;
        }

        // Link resources to the final apk.
        cmd = new ArrayList<>();
        cmd.add(aaptPath);
        cmd.add("link");

        cmd.add("-o");
        cmd.add(apkFile.getAbsolutePath());

        cmd.add("--manifest");
        cmd.add(manifest.getAbsolutePath());

        if (mApkInfo.getSdkInfo().getMinSdkVersion() != null) {
            cmd.add("--min-sdk-version");
            cmd.add(mApkInfo.getSdkInfo().getMinSdkVersion());
        }
        if (mApkInfo.getSdkInfo().getTargetSdkVersion() != null) {
            cmd.add("--target-sdk-version");
            cmd.add(mApkInfo.getSdkInfo().getTargetSdkVersion());
        }
        if (mApkInfo.getVersionInfo().getVersionCode() != null) {
            cmd.add("--version-code");
            cmd.add(mApkInfo.getVersionInfo().getVersionCode());
        }
        if (mApkInfo.getVersionInfo().getVersionName() != null) {
            cmd.add("--version-name");
            cmd.add(mApkInfo.getVersionInfo().getVersionName());
        }
        if (mApkInfo.getResourcesInfo().getPackageId() != null) {
            int pkgId = Integer.parseInt(mApkInfo.getResourcesInfo().getPackageId());
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
        if (mApkInfo.getResourcesInfo().getPackageName() != null) {
            cmd.add("--rename-resources-package");
            cmd.add(mApkInfo.getResourcesInfo().getPackageName());
        }
        if (mApkInfo.getResourcesInfo().isSparseEntries()) {
            cmd.add("--enable-sparse-encoding");
        }
        if (mApkInfo.getResourcesInfo().isCompactEntries()) {
            cmd.add("--enable-compact-entries");
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

        if (rawDir != null) {
            cmd.add("-R");
            cmd.add(rawDir.getAbsolutePath());
        }
        if (assetDir != null) {
            cmd.add("-A");
            cmd.add(assetDir.getAbsolutePath());
        }
        if (include != null) {
            for (File file : include) {
                cmd.add("-I");
                cmd.add(file.getPath());
            }
        }
        if (mConfig.isVerbose()) {
            cmd.add("-v");
        }
        if (resourcesZip != null) {
            cmd.add(resourcesZip.getAbsolutePath());
        }

        try {
            OS.exec(cmd.toArray(new String[0]));
            LOGGER.fine("aapt2 link command ran: ");
            LOGGER.fine(cmd.toString());
        } catch (BrutException ex) {
            throw new AndrolibException(ex);
        }
    }
}
