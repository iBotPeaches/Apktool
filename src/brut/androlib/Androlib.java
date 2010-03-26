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
import brut.util.OS;
import java.io.*;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public class Androlib {
    private final AndrolibResources mAndRes = new AndrolibResources();
    private final AndrolibSmali mSmali = new AndrolibSmali();

    public void buildAll() throws AndrolibException {
        clean();
        new File("build/apk").mkdirs();
        buildCopyRawFiles();
        buildResources();
        buildClasses();
        buildPackage();
    }

    public void clean() throws AndrolibException {
        try {
            OS.rmdir("build");
            OS.rmdir("dist");
        } catch (BrutException ex) {
            throw new AndrolibException(ex);
        }
    }

    public void buildCopyRawFiles() throws AndrolibException {
        try {
            if (new File("assets").exists()) {
                OS.cpdir("assets", "build/apk/assets");
            }
            if (new File("lib").exists()) {
                OS.cpdir("lib", "build/apk/lib");
            }
        } catch (BrutException ex) {
            throw new AndrolibException(ex);
        }
    }

    public void buildResources() throws AndrolibException {
        if (new File("resources.arsc").exists()) {
            try {
                new File("build/apk/res").mkdirs();
                DirUtil.copyToDir(new FileDirectory("res"),
                    new FileDirectory("build/apk/res"));
                IOUtils.copy(new FileInputStream("resources.arsc"),
                    new FileOutputStream("build/apk/resources.arsc"));
                IOUtils.copy(new FileInputStream("AndroidManifest.xml"),
                    new FileOutputStream("build/apk/AndroidManifest.xml"));
            } catch (IOException ex) {
                throw new AndrolibException(ex);
            } catch (DirectoryException ex) {
                throw new AndrolibException(ex);
            }
            return;
        }

        File apkFile;
        try {
            apkFile = File.createTempFile("APKTOOL", null);
        } catch (IOException ex) {
            throw new AndrolibException(ex);
        }
        apkFile.delete();

        mAndRes.aaptPackage(
            apkFile,
            new File("AndroidManifest.xml"),
            new File("res")
        );
//        mAndRes.updateSmaliResIDs(
//            mAndRes.getResTable(apkFile), new File("smali"));

        try {
            DirUtil.copyToDir(new ZipRODirectory(apkFile),
                new FileDirectory("build/apk"));
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }

        apkFile.delete();
    }

    public void buildClasses() throws AndrolibException {
        new AndrolibSmali().smali("smali", "build/apk/classes.dex");
    }

    public void buildPackage() throws AndrolibException {
        File outApk = new File("dist/out.apk");
        if (outApk.exists()) {
            outApk.delete();
        } else {
            File outDir = outApk.getParentFile();
            if (! outDir.exists()) {
                outDir.mkdirs();
            }
        }
        mAndRes.aaptPackage(outApk, null, null,
            new File("build/apk"), false);
    }

    public ResTable getResTable(ExtFile apkFile) throws AndrolibException {
        LOGGER.info("Decoding resource table...");
        return mAndRes.getResTable(apkFile);
    }

    public void decodeSourcesRaw(ExtFile apkFile, File outDir)
            throws AndrolibException {
        LOGGER.info("Copying raw classes.dex file...");
        try {
            apkFile.getDirectory().copyToDir(outDir, "classes.dex");
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
            LOGGER.info("Copying raw resources...");
            apkFile.getDirectory().copyToDir(outDir,
                new String[]{"AndroidManifest.xml", "resources.arsc", "res"});
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

    private final static Logger LOGGER =
        Logger.getLogger(Androlib.class.getName());

    private final static String SMALI_DIRNAME = "smali";
}
