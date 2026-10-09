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
package brut.androlib.smali;

import brut.androlib.exceptions.AndrolibException;
import brut.common.Log;
import brut.util.IOUtils;
import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.writer.builder.DexBuilder;
import com.android.tools.smali.dexlib2.writer.io.FileDataStore;
import com.android.tools.smali.smali.smaliFlexLexer;
import com.android.tools.smali.smali.smaliParser;
import com.android.tools.smali.smali.smaliTreeWalker;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.Token;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.CommonTreeNodeStream;

import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.stream.Stream;

public class SmaliBuilder {
    private static final String TAG = SmaliBuilder.class.getName();

    private static final boolean VERBOSE_ERRORS = false;
    private static final boolean PRINT_TOKENS = false;

    private final int mApiLevel;

    public SmaliBuilder(int apiLevel) {
        // #3641 - Limit opcode API level to 29 or below (dex version up to 039).
        mApiLevel = Math.min(apiLevel, 29);
    }

    public void build(Path smaliDir, Path dexFile) throws AndrolibException {
        try {
            DexBuilder dexBuilder = new DexBuilder(mApiLevel > 0 ? Opcodes.forApi(mApiLevel) : Opcodes.getDefault());

            try (Stream<Path> stream = Files.walk(smaliDir)) {
                Iterator<Path> it = stream.filter(Files::isRegularFile).iterator();
                while (it.hasNext()) {
                    Path smaliFile = it.next();

                    if (!IOUtils.getFileExtension(smaliFile).equals("smali")) {
                        Log.w(TAG, "Unknown file type, ignoring: " + smaliFile);
                        continue;
                    }

                    boolean success;
                    Throwable cause;
                    try {
                        success = buildFile(smaliFile, dexBuilder);
                        cause = null;
                    } catch (Exception ex) {
                        success = false;
                        cause = ex;
                    }
                    if (!success) {
                        AndrolibException ex = new AndrolibException("Could not smali file: " + smaliFile);
                        if (cause != null) {
                            ex.initCause(cause);
                        }
                        throw ex;
                    }
                }
            }

            if (!Files.deleteIfExists(dexFile)) {
                IOUtils.createParentDirectories(dexFile);
            }
            dexBuilder.writeTo(new FileDataStore(dexFile.toFile()));
        } catch (IOException | RuntimeException ex) {
            throw new AndrolibException("Could not smali folder: " + smaliDir.getFileName(), ex);
        }
    }

    private boolean buildFile(Path smaliFile, DexBuilder dexBuilder) throws IOException, RecognitionException {
        try (InputStreamReader reader = new InputStreamReader(
                Files.newInputStream(smaliFile), StandardCharsets.UTF_8)) {
            smaliFlexLexer lexer = new smaliFlexLexer(reader, mApiLevel);
            lexer.setSourceFile(smaliFile.toFile());

            CommonTokenStream tokens = new CommonTokenStream(lexer);

            if (PRINT_TOKENS) {
                for (Token token : tokens.getTokens()) {
                    if (token.getChannel() != smaliParser.HIDDEN) {
                        System.out.println(smaliParser.tokenNames[token.getType()] + ": " + token.getText());
                    }
                }
            }

            smaliParser parser = new smaliParser(tokens);
            parser.setApiLevel(mApiLevel);
            parser.setVerboseErrors(VERBOSE_ERRORS);

            smaliParser.smali_file_return result = parser.smali_file();

            if (parser.getNumberOfSyntaxErrors() > 0 || lexer.getNumberOfSyntaxErrors() > 0) {
                return false;
            }

            CommonTree tree = result.getTree();
            CommonTreeNodeStream treeStream = new CommonTreeNodeStream(tree);
            treeStream.setTokenStream(tokens);

            smaliTreeWalker treeWalker = new smaliTreeWalker(treeStream);
            treeWalker.setApiLevel(mApiLevel);
            treeWalker.setVerboseErrors(VERBOSE_ERRORS);
            treeWalker.setDexBuilder(dexBuilder);
            treeWalker.smali_file();

            return treeWalker.getNumberOfSyntaxErrors() == 0;
        }
    }
}
