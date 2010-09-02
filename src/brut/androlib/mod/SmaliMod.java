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
package brut.androlib.mod;

import java.io.*;
import org.antlr.runtime.*;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.CommonTreeNodeStream;
import org.jf.dexlib.DexFile;
import org.jf.smali.*;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public class SmaliMod {

    private static boolean assembleSmaliFile(File smaliFile, DexFile dexFile, boolean verboseErrors, boolean oldLexer,
                                             boolean printTokens)
            throws Exception {
        CommonTokenStream tokens;


        boolean lexerErrors = false;
        LexerErrorInterface lexer;

        if (oldLexer) {
            ANTLRFileStream input = new ANTLRFileStream(smaliFile.getAbsolutePath(), "UTF-8");
            input.name = smaliFile.getAbsolutePath();

            lexer = new smaliLexer(input);
            tokens = new CommonTokenStream((TokenSource)lexer);
        } else {
            FileInputStream fis = new FileInputStream(smaliFile.getAbsolutePath());
            InputStreamReader reader = new InputStreamReader(fis, "UTF-8");

            lexer = new smaliFlexLexer(reader);
            ((smaliFlexLexer)lexer).setSourceFile(smaliFile);
            tokens = new CommonTokenStream((TokenSource)lexer);
        }

        if (printTokens) {
            tokens.getTokens();

            for (int i=0; i<tokens.size(); i++) {
                Token token = tokens.get(i);
                if (token.getChannel() == smaliLexer.HIDDEN) {
                    continue;
                }

                System.out.println(smaliParser.tokenNames[token.getType()] + ": " + token.getText());
            }
        }

        smaliParser parser = new smaliParser(tokens);
        parser.setVerboseErrors(verboseErrors);

        smaliParser.smali_file_return result = parser.smali_file();

        if (parser.getNumberOfSyntaxErrors() > 0 || lexer.getNumberOfSyntaxErrors() > 0) {
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
