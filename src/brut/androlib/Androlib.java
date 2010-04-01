/*
 *  Copyright 2010 Ryszard Wiśniewski <brut.alll@gmail.com>.
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
 *  under the License.
 */

package brut.androlib;

import brut.androlib.res.AndrolibResources;
import brut.androlib.res.data.ResTable;
import brut.androlib.res.util.ExtFile;
import brut.common.BrutException;
import brut.directory.*;
import brut.util.BrutIO;
import brut.util.OS;
import java.io.*;
import java.util.logging.Logger;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public class Androlib {
    private final AndrolibResources mAndRes = new AndrolibResources();
    private final AndrolibSmali mSmali = new AndrolibSmali();

    public ResTable getResTable(ExtFile apkFile) throws AndrolibException {
        LOGGER.info("Decoding resource table...");
        return mAndRes.getResTable(apkFile);
    }

    public void decodeSourcesRaw(ExtFile apkFile, File outDir)
            throws AndrolibException {
        try {
            Directory apk = apkFile.getDirectory();
            if (apk.containsFile("classes.dex")) {
                LOGGER.info("Copying raw classes.dex file...");
                apkFile.getDirectory().copyToDir(outDir, "classes.dex");
            }
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    public void decodeSourcesSmali(File apkFile, File outDir)
            throws AndrolibException {
        try {
            File smaliDir = new File(outDir, SMALI_DIRNAME);
            OS.rmdir(smaliDir);
            smaliDir.mkdirs();
            LOGGER.info("Baksmaling...");
            mSmali.baksmali(apkFile, smaliDir);
        } catch (BrutException ex) {
            throw new AndrolibException(ex);
        }
    }

    public void decodeResourcesRaw(ExtFile apkFile, File outDir)
            throws AndrolibException {
        try {
            Directory apk = apkFile.getDirectory();
            if (apk.containsFile("resources.arsc")) {
                LOGGER.info("Copying raw resources...");
                apkFile.getDirectory().copyToDir(
                    outDir, APK_RESOURCES_FILENAMES);
            }
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    public void decodeResourcesFull(ExtFile apkFile, File outDir,
            ResTable resTable) throws AndrolibException {
        LOGGER.info("Decoding resources...");
        mAndRes.decode(resTable, apkFile, outDir);
    }

    public void decodeRawFiles(ExtFile apkFile, File outDir)
            throws AndrolibException {
        LOGGER.info("Copying assets and libs...");
        try {
            Directory in = apkFile.getDirectory();
            if (in.containsDir("assets")) {
                in.copyToDir(outDir, "assets");
            }
            if (in.containsDir("lib")) {
                in.copyToDir(outDir, "lib");
            }
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    public void build(File appDir, boolean forceBuildAll)
            throws AndrolibException {
        build(new ExtFile(appDir), forceBuildAll);
    }

    public void build(ExtFile appDir, boolean forceBuildAll)
            throws AndrolibException {
        new File(appDir, APK_DIRNAME).mkdirs();
        buildSources(appDir, forceBuildAll);
        buildResources(appDir, forceBuildAll);
        buildLib(appDir, forceBuildAll);
        buildApk(appDir);
    }

    public void buildSources(File appDir, boolean forceBuildAll)
            throws AndrolibException {
        if (! buildSourcesRaw(appDir, forceBuildAll)
                && ! buildSourcesSmali(appDir, forceBuildAll)) {
            LOGGER.warning("Could not find sources");
        }
    }

    public boolean buildSourcesRaw(File appDir, boolean forceBuildAll)
            throws AndrolibException {
        try {
            File working = new File(appDir, "classes.dex");
            if (! working.exists()) {
                return false;
            }
            File stored = new File(appDir, APK_DIRNAME + "/classes.dex");
            if (forceBuildAll || isModified(working, stored)) {
                LOGGER.info("Copying classes.dex file...");
                BrutIO.copyAndClose(new FileInputStream(working),
                    new FileOutputStream(stored));
            }
            return true;
        } catch (IOException ex) {
            throw new AndrolibException(ex);
        }
    }

    public boolean buildSourcesSmali(File appDir, boolean forceBuildAll)
            throws AndrolibException {
        File smaliDir = new File(appDir, "smali");
        if (! smaliDir.exists()) {
            return false;
        }
        File dex = new File(appDir, APK_DIRNAME + "/classes.dex");
        if (! forceBuildAll) {
            LOGGER.info("Checking whether sources has changed...");
        }
        if (forceBuildAll || isModified(smaliDir, dex)) {
            LOGGER.info("Smaling...");
            dex.delete();
            mSmali.smali(smaliDir, dex);
        }
        return true;
    }

    public void buildResources(ExtFile appDir, boolean forceBuildAll)
            throws AndrolibException {
        if (! buildResourcesRaw(appDir, forceBuildAll)
                && ! buildResourcesFull(appDir, forceBuildAll)) {
            LOGGER.warning("Could not find resources");
        }
    }

    public boolean buildResourcesRaw(ExtFile appDir, boolean forceBuildAll)
            throws AndrolibException {
        try {
            if (! new File(appDir, "resources.arsc").exists()) {
                return false;
            }
            File apkDir = new File(appDir, APK_DIRNAME);
            if (! forceBuildAll) {
                LOGGER.info("Checking whether resources has changed...");
            }
            if (forceBuildAll || isModified(
                    newFiles(APK_RESOURCES_FILENAMES, appDir),
                    newFiles(APK_RESOURCES_FILENAMES, apkDir))) {
                LOGGER.info("Copying raw resources");
                appDir.getDirectory()
                    .copyToDir(apkDir, APK_RESOURCES_FILENAMES);
            }
            return true;
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    public boolean buildResourcesFull(File appDir, boolean forceBuildAll)
            throws AndrolibException {
        try {
            if (! forceBuildAll) {
                LOGGER.info("Checking whether resources has changed...");
            }
            File apkDir = new File(appDir, APK_DIRNAME);
            if (forceBuildAll || isModified(
                    newFiles(APP_RESOURCES_FILENAMES, appDir),
                    newFiles(APK_RESOURCES_FILENAMES, apkDir))) {
                LOGGER.info("Building resources...");

                File apkFile = File.createTempFile("APKTOOL", null);
                apkFile.delete();

                File ninePatch = new File(appDir, "9patch");
                if (! ninePatch.exists()) {
                    ninePatch = null;
                }
                mAndRes.aaptPackage(
                    apkFile,
                    new File(appDir, "AndroidManifest.xml"),
                    new File(appDir, "res"), ninePatch, null, false,
                    mAndRes.detectWhetherAppIsFramework(appDir)
                );

                new ExtFile(apkFile).getDirectory()
                    .copyToDir(apkDir, APK_RESOURCES_FILENAMES);
            }
            return true;
        } catch (IOException ex) {
            throw new AndrolibException(ex);
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    public void buildLib(File appDir, boolean forceBuildAll)
            throws AndrolibException {
        File working = new File(appDir, "lib");
        if (! working.exists()) {
            return;
        }
        File stored = new File(appDir, APK_DIRNAME + "/lib");
        if (forceBuildAll || isModified(working, stored)) {
            LOGGER.info("Copying libs...");
            try {
                OS.rmdir(stored);
                OS.cpdir(working, stored);
            } catch (BrutException ex) {
                throw new AndrolibException(ex);
            }
        }
    }

    public void buildApk(File appDir) throws AndrolibException {
        LOGGER.info("Building apk file");
        File outApk = new File(appDir, OUT_APK_FILENAME);
        if (outApk.exists()) {
            outApk.delete();
        } else {
            File outDir = outApk.getParentFile();
            if (! outDir.exists()) {
                outDir.mkdirs();
            }
        }
        File assetDir = new File(appDir, "assets");
        if (! assetDir.exists()) {
            assetDir = null;
        }
        mAndRes.aaptPackage(outApk, null, null,
            new File(appDir, APK_DIRNAME), assetDir, false, false);
    }

    private boolean isModified(File working, File stored) {
        if (! stored.exists()) {
            return true;
        }
        return BrutIO.recursiveModifiedTime(working) >
            BrutIO.recursiveModifiedTime(stored);
    }

    private boolean isModified(File[] working, File[] stored) {
        for (int i = 0; i < stored.length; i++) {
            if (! stored[i].exists()) {
                return true;
            }
        }
        return BrutIO.recursiveModifiedTime(working) >
            BrutIO.recursiveModifiedTime(stored);
    }

    private File[] newFiles(String[] names, File dir) {
        File[] files = new File[names.length];
        for (int i = 0; i < names.length; i++) {
            files[i] = new File(dir, names[i]);
        }
        return files;
    }

    private final static Logger LOGGER =
        Logger.getLogger(Androlib.class.getName());

    private final static String SMALI_DIRNAME = "smali";
    private final static String APK_DIRNAME = "build/apk";
    private final static String OUT_APK_FILENAME = "dist/out.apk";
    private final static String[] APK_RESOURCES_FILENAMES =
        new String[]{"resources.arsc", "AndroidManifest.xml", "res"};
    private final static String[] APP_RESOURCES_FILENAMES =
        new String[]{"AndroidManifest.xml", "res"};
}
