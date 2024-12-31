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

import brut.androlib.apk.ApkInfo;
import brut.androlib.exceptions.AndrolibException;
import brut.common.BrutException;
import brut.util.AaptManager;
import brut.util.OS;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;
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
        String aaptPath;
        boolean customAapt;

        if (mConfig.getAaptBinary() != null) {
            aaptPath = mConfig.getAaptBinary().getPath();
            customAapt = true;
        } else {
            try {
                aaptPath = AaptManager.getAaptBinary(mConfig.getAaptVersion()).getPath();
                customAapt = false;
            } catch (BrutException ex) {
                aaptPath = AaptManager.getAaptName(mConfig.getAaptVersion());
                customAapt = true;
                LOGGER.warning(aaptPath + ": " + ex.getMessage() + " (defaulting to $PATH binary)");
            }
        }

        switch (mConfig.getAaptVersion()) {
            case 2:
                invokeAapt2(apkFile, manifest, resDir, rawDir, assetDir, include, aaptPath, customAapt);
                break;
            default:
                invokeAapt1(apkFile, manifest, resDir, rawDir, assetDir, include, aaptPath, customAapt);
                break;
        }
    }

    private void invokeAapt2(File apkFile, File manifest, File resDir, File rawDir, File assetDir, File[] include,
                             String aaptPath, boolean customAapt) throws AndrolibException {
        List<String> cmd;
        File resourcesZip = null;

        if (resDir != null) {
            resourcesZip = Paths.get(resDir.getParent(), "build", "resources.zip").toFile();

            if (!resourcesZip.exists()) {
                // Compile the files into flat arsc files
                cmd = new ArrayList<>();
                cmd.add(aaptPath);
                cmd.add("compile");

                cmd.add("--dir");
                cmd.add(resDir.getAbsolutePath());

                // Treats error that used to be valid in aapt1 as warnings in aapt2
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

        // Link them into the final apk, reusing our old command after clearing for the aapt2 binary
        cmd = new ArrayList<>();
        cmd.add(aaptPath);
        cmd.add("link");

        cmd.add("-o");
        cmd.add(apkFile.getAbsolutePath());

        if (mApkInfo.packageInfo.forcedPackageId != null && !mApkInfo.packageInfo.forcedPackageId.equals("1")
                && !mApkInfo.sharedLibrary) {
            cmd.add("--allow-reserved-package-id");
            cmd.add("--package-id");
            cmd.add(mApkInfo.packageInfo.forcedPackageId);
        }
        if (mApkInfo.sharedLibrary) {
            cmd.add("--shared-lib");
        }
        if (mApkInfo.getMinSdkVersion() != null) {
            cmd.add("--min-sdk-version");
            cmd.add(mApkInfo.getMinSdkVersion());
        }
        if (mApkInfo.getTargetSdkVersion() != null) {
            cmd.add("--target-sdk-version");
            cmd.add(mApkInfo.checkTargetSdkVersionBounds());
        }
        if (mApkInfo.packageInfo.renameManifestPackage != null) {
            cmd.add("--rename-manifest-package");
            cmd.add(mApkInfo.packageInfo.renameManifestPackage);

            cmd.add("--rename-instrumentation-target-package");
            cmd.add(mApkInfo.packageInfo.renameManifestPackage);
        }
        if (mApkInfo.versionInfo.versionCode != null) {
            cmd.add("--version-code");
            cmd.add(mApkInfo.versionInfo.versionCode);
        }
        if (mApkInfo.versionInfo.versionName != null) {
            cmd.add("--version-name");
            cmd.add(mApkInfo.versionInfo.versionName);
        }

        // Disable automatic changes
        cmd.add("--no-auto-version");
        cmd.add("--no-version-vectors");
        cmd.add("--no-version-transitions");
        cmd.add("--no-resource-deduping");
        cmd.add("--no-compile-sdk-metadata");

        // #3427 - Ignore stricter parsing during aapt2
        cmd.add("--warn-manifest-validation");

        if (mApkInfo.sparseResources) {
            cmd.add("--enable-sparse-encoding");
        }
        if (mApkInfo.compactEntries) {
            cmd.add("--enable-compact-entries");
        }
        if (mApkInfo.isFrameworkApk) {
            cmd.add("-x");
        }
        if (!mApkInfo.featureFlags.isEmpty()) {
            List<String> featureFlags = new ArrayList<>();
            for (Map.Entry<String, Boolean> entry : mApkInfo.featureFlags.entrySet()) {
                featureFlags.add(entry.getKey() + "=" + entry.getValue());
            }
            cmd.add("--feature-flags");
            cmd.add(String.join(",", featureFlags));
        }
        if (include != null) {
            for (File file : include) {
                cmd.add("-I");
                cmd.add(file.getPath());
            }
        }

        cmd.add("--manifest");
        cmd.add(manifest.getAbsolutePath());

        if (assetDir != null) {
            cmd.add("-A");
            cmd.add(assetDir.getAbsolutePath());
        }
        if (rawDir != null) {
            cmd.add("-R");
            cmd.add(rawDir.getAbsolutePath());
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

    private void invokeAapt1(File apkFile, File manifest, File resDir, File rawDir, File assetDir, File[] include,
                             String aaptPath, boolean customAapt) throws AndrolibException {
        List<String> cmd = new ArrayList<>();
        cmd.add(aaptPath);
        cmd.add("p");

        if (mConfig.isVerbose()) { // output aapt verbose
            cmd.add("-v");
        }
        if (mConfig.isUpdateFiles()) {
            cmd.add("-u");
        }
        if (mConfig.isDebugMode()) { // inject debuggable="true" into manifest
            cmd.add("--debug-mode");
        }
        if (mConfig.isNoCrunch()) {
            cmd.add("--no-crunch");
        }
        // force package id so that some frameworks build with correct id
        // disable if user adds own aapt (can't know if they have this feature)
        if (mApkInfo.packageInfo.forcedPackageId != null && !mApkInfo.sharedLibrary && !customAapt) {
            cmd.add("--forced-package-id");
            cmd.add(mApkInfo.packageInfo.forcedPackageId);
        }
        if (mApkInfo.sharedLibrary) {
            cmd.add("--shared-lib");
        }
        if (mApkInfo.getMinSdkVersion() != null) {
            cmd.add("--min-sdk-version");
            cmd.add(mApkInfo.getMinSdkVersion());
        }
        if (mApkInfo.getTargetSdkVersion() != null) {
            cmd.add("--target-sdk-version");

            // Ensure that targetSdkVersion is between minSdkVersion/maxSdkVersion if
            // they are specified.
            cmd.add(mApkInfo.checkTargetSdkVersionBounds());
        }
        if (mApkInfo.getMaxSdkVersion() != null) {
            cmd.add("--max-sdk-version");
            cmd.add(mApkInfo.getMaxSdkVersion());

            // if we have max sdk version, set --max-res-version,
            // so we can ignore anything over that during build.
            cmd.add("--max-res-version");
            cmd.add(mApkInfo.getMaxSdkVersion());
        }
        if (mApkInfo.packageInfo.renameManifestPackage != null) {
            cmd.add("--rename-manifest-package");
            cmd.add(mApkInfo.packageInfo.renameManifestPackage);
        }
        if (mApkInfo.versionInfo.versionCode != null) {
            cmd.add("--version-code");
            cmd.add(mApkInfo.versionInfo.versionCode);
        }
        if (mApkInfo.versionInfo.versionName != null) {
            cmd.add("--version-name");
            cmd.add(mApkInfo.versionInfo.versionName);
        }
        cmd.add("--no-version-vectors");
        cmd.add("-F");
        cmd.add(apkFile.getAbsolutePath());

        if (mApkInfo.isFrameworkApk) {
            cmd.add("-x");
        }
        if (include != null) {
            for (File file : include) {
                cmd.add("-I");
                cmd.add(file.getPath());
            }
        }
        if (resDir != null) {
            cmd.add("-S");
            cmd.add(resDir.getAbsolutePath());
        }
        if (manifest != null) {
            cmd.add("-M");
            cmd.add(manifest.getAbsolutePath());
        }
        if (assetDir != null) {
            cmd.add("-A");
            cmd.add(assetDir.getAbsolutePath());
        }
        if (rawDir != null) {
            cmd.add(rawDir.getAbsolutePath());
        }
        try {
            OS.exec(cmd.toArray(new String[0]));
            LOGGER.fine("command ran: ");
            LOGGER.fine(cmd.toString());
        } catch (BrutException ex) {
            throw new AndrolibException(ex);
        }
    }
}
