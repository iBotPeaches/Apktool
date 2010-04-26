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

import brut.androlib.util.DexFileBuilder;
import brut.directory.DirectoryException;
import brut.directory.FileDirectory;
import java.io.File;
import java.io.IOException;
import org.jf.baksmali.baksmali;
import org.jf.dexlib.DexFile;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public class AndrolibSmali {
    public void baksmali(File apkFile, File dir) throws AndrolibException {
        baksmali(apkFile.getAbsolutePath(), dir.getAbsolutePath());
    }

    public void baksmali(String apkFile, String dir) throws AndrolibException {
        try {
            DexFile dexFile = new DexFile(apkFile);
            baksmali.disassembleDexFile(apkFile, dexFile, false, dir, new String[]{}, "", null, false, true, true, true, false, 0, false, false);
        } catch (IOException ex) {
            throw new AndrolibException(ex);
        }
    }

    public void smali(String dir, String dexFile) throws AndrolibException {
        smali(new File(dir), new File(dexFile));
    }

    public void smali(File dir, File dexFile) throws AndrolibException {
        try {
            DexFileBuilder builder = new DexFileBuilder();
            for (String fileName : new FileDirectory(dir).getFiles(true)) {
                builder.addSmaliFile(new File(dir + "/" + fileName));
            }
            builder.writeTo(dexFile);
        } catch (DirectoryException ex) {
            throw new AndrolibException(
                "Could not smali " + dir + " to " + dexFile, ex);
        }
    }
}
