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
package brut.androlib.src;

import brut.androlib.AndrolibException;
import brut.androlib.mod.IndentingWriter;
import java.io.*;
import org.jf.baksmali.Adaptors.ClassDefinition;
import org.jf.baksmali.baksmali;
import org.jf.dexlib.ClassDefItem;
import org.jf.dexlib.Code.Analysis.ClassPath;
import org.jf.dexlib.DexFile;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public class SmaliDecoder {

    public static void decode(File apkFile, File outDir, boolean debug)
            throws AndrolibException {
        new SmaliDecoder(apkFile, outDir, debug).decode();
    }

    private SmaliDecoder(File apkFile, File outDir, boolean debug) {
        mApkFile = apkFile;
        mOutDir = outDir;
        mDebug = debug;
    }

    private void decode() throws AndrolibException {
        try {
            baksmali.useLocalsDirective = true;
            baksmali.useSequentialLabels = true;
            if (mDebug) {
                baksmali.registerInfo = org.jf.baksmali.main.DIFFPRE;
                ClassPath.dontLoadClassPath = true;
            }

            DexFile dexFile = new DexFile(mApkFile);
            for (ClassDefItem classDefItem :
                    dexFile.ClassDefsSection.getItems()) {
                decodeClassDefItem(classDefItem);
            }
        } catch (IOException ex) {
            throw new AndrolibException(ex);
        }
    }

    private void decodeClassDefItem(ClassDefItem classDefItem)
            throws AndrolibException, IOException {
        TypeName name = TypeName.fromInternalName(
            classDefItem.getClassType().getTypeDescriptor());
        File outFile = new File(mOutDir, name.getFilePath(true)
            + (mDebug ? ".java" : ".smali"));

        if (outFile.exists()) {
            throw new AndrolibException(
                "File already exists: " + outFile);
        }
        outFile.getParentFile().mkdirs();

        IndentingWriter indentWriter =
            new IndentingWriter(new FileWriter(outFile));

        if (mDebug) {
            indentWriter.write("package " + name.package_ + "; class "
                + name.getName(true, true) + " {/*\n\n");
        }
        new ClassDefinition(classDefItem).writeTo(indentWriter);
        if (mDebug) {
            indentWriter.write("\n*/}\n");
        }
        indentWriter.close();
    }

    private final File mApkFile;
    private final File mOutDir;
    private final boolean mDebug;
}
