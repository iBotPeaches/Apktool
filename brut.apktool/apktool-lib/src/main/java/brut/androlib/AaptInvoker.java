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

import brut.androlib.exceptions.AndrolibException;
import brut.androlib.apk.ApkInfo;
import brut.common.BrutException;
import brut.util.AaptManager;
import brut.util.OS;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;

public class AaptInvoker {
    private final Config mConfig;
    private final ApkInfo mApkInfo;

    private final static Logger LOGGER = Logger.getLogger(AaptInvoker.class.getName());

    public AaptInvoker(Config config, ApkInfo apkInfo) {
        mConfig = config;
        mApkInfo = apkInfo;
    }

    private File getAaptBinaryFile() throws AndrolibException {
        try {
            if (getAaptVersion() == 2) {
                return AaptManager.getAapt2();
            }
            return AaptManager.getAapt1();
        } catch (BrutException ex) {
            throw new AndrolibException(ex);
        }
    }

    private int getAaptVersion() {
        return mConfig.isAapt2() ? 2 : 1;
    }

    private File createDoNotCompressExtensionsFile(ApkInfo apkInfo) throws AndrolibException {
        if (apkInfo.doNotCompress == null || apkInfo.doNotCompress.isEmpty()) {
            return null;
        }

        File doNotCompressFile;
        try {
            doNotCompressFile = File.createTempFile("APKTOOL", null);
            doNotCompressFile.deleteOnExit();

            BufferedWriter fileWriter = new BufferedWriter(new FileWriter(doNotCompressFile));
            for (String extension : apkInfo.doNotCompress) {
                fileWriter.write(extension);
                fileWriter.newLine();
            }
            fileWriter.close();

            return doNotCompressFile;
        } catch (IOException ex) {
            throw new AndrolibException(ex);
        }
    }

    private void invokeAapt2(File apkFile, File manifest, File resDir, File rawDir, File assetDir, File[] include,
                             List<String> cmd, boolean customAapt) throws AndrolibException {

        List<String> compileCommand = new ArrayList<>(cmd);
        File resourcesZip = null;

        if (resDir != null) {
            File buildDir = new File(resDir.getParent(), "build");
            resourcesZip = new File(buildDir, "resources.zip");
        }

        if (resDir != null && !resourcesZip.exists()) {

            // Compile the files into flat arsc files
            cmd.add("compile");

            cmd.add("--dir");
            cmd.add(resDir.getAbsolutePath());

            // Treats error that used to be valid in aapt1 as warnings in aapt2
            cmd.add("--legacy");

            File buildDir = new File(resDir.getParent(), "build");
            resourcesZip = new File(buildDir, "resources.zip");

            cmd.add("-o");
            cmd.add(resourcesZip.getAbsolutePath());

            if (mConfig.verbose) {
                cmd.add("-v");
            }

            if (mConfig.noCrunch) {
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

        if (manifest == null) {
            return;
        }

        // Link them into the final apk, reusing our old command after clearing for the aapt2 binary
        cmd = new ArrayList<>(compileCommand);
        cmd.add("link");

        cmd.add("-o");
        cmd.add(apkFile.getAbsolutePath());

        if (mApkInfo.packageInfo.forcedPackageId != null && ! mApkInfo.sharedLibrary) {
            cmd.add("--package-id");
            cmd.add(mApkInfo.packageInfo.forcedPackageId);
        }

        if (mApkInfo.sharedLibrary) {
            cmd.add("--shared-lib");
        }

        if (mApkInfo.getMinSdkVersion() != null) {
            cmd.add("--min-sdk-version");
            cmd.add(mApkInfo.getMinSdkVersion() );
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

        cmd.add("--allow-reserved-package-id");

        cmd.add("--no-compile-sdk-metadata");

        // #3427 - Ignore stricter parsing during aapt2
        cmd.add("--warn-manifest-validation");

        if (mApkInfo.sparseResources) {
            cmd.add("--enable-sparse-encoding");
        }

        if (mApkInfo.isFrameworkApk) {
            cmd.add("-x");
        }

        if (mApkInfo.doNotCompress != null && !customAapt) {
            // Use custom -e option to avoid limits on commandline length.
            // Can only be used when custom aapt binary is not used.
            String extensionsFilePath =
                Objects.requireNonNull(createDoNotCompressExtensionsFile(mApkInfo)).getAbsolutePath();
            cmd.add("-e");
            cmd.add(extensionsFilePath);
        } else if (mApkInfo.doNotCompress != null) {
            for (String file : mApkInfo.doNotCompress) {
                cmd.add("-0");
                cmd.add(file);
            }
        }

        if (!mApkInfo.resourcesAreCompressed) {
            cmd.add("-0");
            cmd.add("arsc");
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

        if (mConfig.verbose) {
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
                             List<String> cmd, boolean customAapt) throws AndrolibException {

        cmd.add("p");

        if (mConfig.verbose) { // output aapt verbose
            cmd.add("-v");
        }
        if (mConfig.updateFiles) {
            cmd.add("-u");
        }
        if (mConfig.debugMode) { // inject debuggable="true" into manifest
            cmd.add("--debug-mode");
        }
        if (mConfig.noCrunch) {
            cmd.add("--no-crunch");
        }
        // force package id so that some frameworks build with correct id
        // disable if user adds own aapt (can't know if they have this feature)
        if (mApkInfo.packageInfo.forcedPackageId != null && ! customAapt && ! mApkInfo.sharedLibrary) {
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

        if (mApkInfo.doNotCompress != null && !customAapt) {
            // Use custom -e option to avoid limits on commandline length.
            // Can only be used when custom aapt binary is not used.
            String extensionsFilePath =
                Objects.requireNonNull(createDoNotCompressExtensionsFile(mApkInfo)).getAbsolutePath();
            cmd.add("-e");
            cmd.add(extensionsFilePath);
        } else if (mApkInfo.doNotCompress != null) {
            for (String file : mApkInfo.doNotCompress) {
                cmd.add("-0");
                cmd.add(file);
            }
        }

        if (!mApkInfo.resourcesAreCompressed) {
            cmd.add("-0");
            cmd.add("arsc");
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

    public void invokeAapt(File apkFile, File manifest, File resDir, File rawDir, File assetDir, File[] include)
            throws AndrolibException {

        String aaptPath = mConfig.aaptPath;
        boolean customAapt = !aaptPath.isEmpty();
        List<String> cmd = new ArrayList<>();

        try {
            String aaptCommand = AaptManager.getAaptExecutionCommand(aaptPath, getAaptBinaryFile());
            cmd.add(aaptCommand);
        } catch (BrutException ex) {
            LOGGER.warning("aapt: " + ex.getMessage() + " (defaulting to $PATH binary)");
            cmd.add(AaptManager.getAaptBinaryName(getAaptVersion()));
        }

        if (mConfig.isAapt2()) {
            invokeAapt2(apkFile, manifest, resDir, rawDir, assetDir, include, cmd, customAapt);
            return;
        }
        invokeAapt1(apkFile, manifest, resDir, rawDir, assetDir, include, cmd, customAapt);
    }
}
