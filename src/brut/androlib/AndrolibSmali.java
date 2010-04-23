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

import brut.directory.DirectoryException;
import brut.directory.FileDirectory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import org.antlr.runtime.ANTLRInputStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.CommonTreeNodeStream;
import org.jf.baksmali.baksmali;
import org.jf.dexlib.CodeItem;
import org.jf.dexlib.DexFile;
import org.jf.dexlib.Util.ByteArrayAnnotatedOutput;
import org.jf.smali.smaliLexer;
import org.jf.smali.smaliParser;
import org.jf.smali.smaliTreeWalker;

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

    public void smali(File dir, File dexFile) throws AndrolibException {
        smali(dir.getAbsolutePath(), dexFile.getAbsolutePath());
    }

    public void smali(String dir, String dexFileName) throws AndrolibException {
        try {
            DexFile dexFile = new DexFile();

            for (String fileName : new FileDirectory(dir).getFiles(true)) {
                if (! assembleSmaliFile(
                        new File(dir + "/" + fileName), dexFile)) {
                    throw new AndrolibException(
                        "Could not smali file: " + fileName);
                }
            }

            dexFile.place();
            for (CodeItem codeItem: dexFile.CodeItemsSection.getItems()) {
                codeItem.fixInstructions(true, true);
            }

            dexFile.place();

            ByteArrayAnnotatedOutput out = new ByteArrayAnnotatedOutput();
            dexFile.writeTo(out);
            byte[] bytes = out.toByteArray();

            DexFile.calcSignature(bytes);
            DexFile.calcChecksum(bytes);

            FileOutputStream fileOutputStream = new FileOutputStream(dexFileName);
            fileOutputStream.write(bytes);
            fileOutputStream.close();
        } catch (IOException ex) {
            throw new AndrolibException("Could not smali files", ex);
        } catch (DirectoryException ex) {
            throw new AndrolibException("Could not smali files", ex);
        } catch (RecognitionException ex) {
            throw new AndrolibException("Could not smali files", ex);
        }
    }

    private static boolean assembleSmaliFile(File smaliFile, DexFile dexFile) throws IOException, RecognitionException {
        ANTLRInputStream input = new ANTLRInputStream(new FileInputStream(smaliFile));
        input.name = smaliFile.getAbsolutePath();

        smaliLexer lexer = new smaliLexer(input);

        CommonTokenStream tokens = new CommonTokenStream(lexer);
        smaliParser parser = new smaliParser(tokens);

        smaliParser.smali_file_return result = parser.smali_file();

        if (parser.getNumberOfSyntaxErrors() > 0 || lexer.getNumberOfLexerErrors() > 0) {
            return false;
        }

        CommonTree t = (CommonTree) result.getTree();

        CommonTreeNodeStream treeStream = new CommonTreeNodeStream(t);
        treeStream.setTokenStream(tokens);

        smaliTreeWalker dexGen = new smaliTreeWalker(treeStream);

        dexGen.dexFile = dexFile;
        dexGen.smali_file();

        if (dexGen.getNumberOfSyntaxErrors() > 0) {
            return false;
        }

        return true;
    }
}
