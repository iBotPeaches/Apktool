/*
 * Copyright 2014, Google Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 *     * Neither the name of Google Inc. nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.jf.smali;

import com.google.common.collect.Iterables;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.TokenSource;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.CommonTreeNodeStream;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.writer.builder.DexBuilder;
import org.jf.dexlib2.writer.io.MemoryDataStore;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

public class SmaliTestUtils {

    public static ClassDef compileSmali(String smaliText) throws RecognitionException, IOException {
        return compileSmali(smaliText, 15, false);
    }

    public static ClassDef compileSmali(String smaliText, int apiLevel, boolean experimental)
            throws RecognitionException, IOException {
        CommonTokenStream tokens;
        LexerErrorInterface lexer;
        DexBuilder dexBuilder = DexBuilder.makeDexBuilder(apiLevel);

        Reader reader = new StringReader(smaliText);

        lexer = new smaliFlexLexer(reader);
        tokens = new CommonTokenStream((TokenSource)lexer);

        smaliParser parser = new smaliParser(tokens);
        parser.setVerboseErrors(true);
        parser.setAllowOdex(false);
        parser.setApiLevel(apiLevel, experimental);

        smaliParser.smali_file_return result = parser.smali_file();

        if(parser.getNumberOfSyntaxErrors() > 0 || lexer.getNumberOfSyntaxErrors() > 0) {
            throw new RuntimeException("Error occured while compiling text");
        }

        CommonTree t = result.getTree();

        CommonTreeNodeStream treeStream = new CommonTreeNodeStream(t);
        treeStream.setTokenStream(tokens);

        smaliTreeWalker dexGen = new smaliTreeWalker(treeStream);
        dexGen.setApiLevel(apiLevel, experimental);
        dexGen.setVerboseErrors(true);
        dexGen.setDexBuilder(dexBuilder);
        dexGen.smali_file();

        if (dexGen.getNumberOfSyntaxErrors() > 0) {
            throw new RuntimeException("Error occured while compiling text");
        }

        MemoryDataStore dataStore = new MemoryDataStore();

        dexBuilder.writeTo(dataStore);

        DexBackedDexFile dexFile = new DexBackedDexFile(
                new Opcodes(apiLevel, experimental), dataStore.getData());

        return Iterables.getFirst(dexFile.getClasses(), null);
    }
}
