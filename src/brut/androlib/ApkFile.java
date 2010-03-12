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
import brut.directory.ZipRODirectory;
import brut.util.OS;
import java.io.File;
import java.io.IOException;
import org.apache.commons.io.IOUtils;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public class ApkFile {
    private final AndrolibResources mAndRes;
    private final File mApkFile;
    private final AndrolibSmali mSmali;

    private ResTable mResTable;

    public ApkFile(AndrolibResources andRes, String apkFileName) {
        this(andRes, new File(apkFileName));
    }

    public ApkFile(AndrolibResources andRes, File apkFile) {
        mAndRes = andRes;
        mApkFile = apkFile;
        mSmali = new AndrolibSmali();
    }

    public void decode(String outDirName) throws AndrolibException {
        decode(new File(outDirName));
    }

    public void decode(File outDir) throws AndrolibException {
        if (outDir.exists()) {
            throw new AndrolibException("Output directory already exists: " +
                outDir.getAbsolutePath());
        }
        outDir.mkdirs();

        File smaliDir = new File(outDir.getPath() + "/smali");

        mAndRes.decode(getResTable(), mApkFile, outDir);
        mSmali.baksmali(mApkFile, smaliDir);
        mAndRes.tagSmaliResIDs(getResTable(), smaliDir);

        try {
            Directory in = new ZipRODirectory(mApkFile);
            Directory out = new FileDirectory(outDir);
            if (in.containsDir("assets")) {
                Directory in2 = in.getDir("assets");
                Directory out2 = out.createDir("assets");
                for (String fileName : in2.getFiles(true)) {
                    IOUtils.copy(in2.getFileInput(fileName),
                        out2.getFileOutput(fileName));
                }
            }
            if (in.containsDir("lib")) {
                Directory in2 = in.getDir("lib");
                Directory out2 = out.createDir("lib");
                for (String fileName : in2.getFiles(true)) {
                    IOUtils.copy(in2.getFileInput(fileName),
                        out2.getFileOutput(fileName));
                }
            }
        } catch (DirectoryException ex) {
            throw new AndrolibException("Could not decode apk", ex);
        } catch (IOException ex) {
            throw new AndrolibException("Could not decode apk", ex);
        }
    }

    public void build(String inDir) throws AndrolibException {
        build(new File(inDir));
    }

    public void build(File inDir) throws AndrolibException {
        try {
            File smaliDir = new File(inDir.getPath() + "/smali");

            mAndRes.aaptPackage(
                mApkFile,
                new File(inDir.getPath() + "/AndroidManifest.xml"),
                new File(inDir.getPath() + "/res")
            );
            mAndRes.updateSmaliResIDs(getResTable(), smaliDir);

            File tmpDir = OS.createTempDirectory();
            try {
                mSmali.smali(smaliDir,
                    new File(tmpDir.getPath() + "/classes.dex"));
                if (new File(inDir.getPath() + "/assets").exists()) {
                    OS.cpdir(inDir.getPath() + "/assets",
                        tmpDir.getPath() + "/assets");
                }
                if (new File(inDir.getPath() + "/lib").exists()) {
                    OS.cpdir(inDir.getPath() + "/lib",
                        tmpDir.getPath() + "/lib");
                }

                mAndRes.aaptPackage(
                    mApkFile,
                    null,
                    null,
                    tmpDir,
                    true
                );

            } finally {
                OS.rmdir(tmpDir);
            }
        } catch (BrutException ex) {
            throw new AndrolibException(
                "Could not build apk for dir: " + inDir.getAbsolutePath(), ex);
        }
    }

    public ResTable getResTable() throws AndrolibException {
        if (mResTable == null) {
            mResTable = mAndRes.getResTable(mApkFile);
        }
        return mResTable;
    }
}
