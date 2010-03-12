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
import brut.common.BrutException;
import brut.directory.Directory;
import brut.directory.DirectoryException;
import brut.directory.FileDirectory;
import brut.directory.Util;
import brut.directory.ZipRODirectory;
import brut.util.OS;
import java.io.File;
import java.io.IOException;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public class Androlib {
    private final String mAndroidJar;
    private final AndrolibResources mAndRes;
    private final AndrolibSmali mSmali;

    public Androlib(String androidJar) {
        mAndroidJar = androidJar;
        mAndRes = new AndrolibResources(mAndroidJar);
        mSmali = new AndrolibSmali();
    }

    public void decode(String apkFileName, String outDirName)
            throws AndrolibException {
        decode(new File(apkFileName), new File(outDirName));
    }

    public void decode(File apkFile, File outDir)
            throws AndrolibException {
        try {
            OS.rmdir(outDir);
        } catch (BrutException ex) {
            throw new AndrolibException(ex);
        }
        outDir.mkdirs();
        
        ResTable resTable = mAndRes.getResTable(apkFile);

        mAndRes.decode(resTable, apkFile, outDir);

        File smaliDir = new File(outDir.getPath() + "/smali");
        mSmali.baksmali(apkFile, smaliDir);
        mAndRes.tagSmaliResIDs(resTable, smaliDir);

        try {
            Directory in = new ZipRODirectory(apkFile);
            Directory out = new FileDirectory(outDir);
            if (in.containsDir("assets")) {
                Util.copyFiles(in.getDir("assets"), out.createDir("assets"));
            }
            if (in.containsDir("lib")) {
                Util.copyFiles(in.getDir("lib"), out.createDir("lib"));
            }
        } catch (DirectoryException ex) {
            throw new AndrolibException("Could not decode apk", ex);
        }
    }

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
        mAndRes.updateSmaliResIDs(
            mAndRes.getResTable(apkFile), new File("smali"));

        try {
            Util.copyFiles(new ZipRODirectory(apkFile),
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
        File distDir = new File("dist");
        if (! distDir.exists()) {
            distDir.mkdirs();
        }
        mAndRes.aaptPackage(new File("dist/out.apk"), null, null,
            new File("build/apk"), false);
    }
}
