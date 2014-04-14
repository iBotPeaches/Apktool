/*
 * [The "BSD licence"]
 * Copyright (c) 2010 Ben Gruver (JesusFreke)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.jf.baksmali;

import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.dexbacked.raw.RawDexFile;
import org.jf.dexlib2.dexbacked.raw.util.DexAnnotator;
import org.jf.util.ConsoleUtil;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

public class dump {
    public static void dump(DexBackedDexFile dexFile, String dumpFileName, int apiLevel) throws IOException {
        if (dumpFileName != null) {
            Writer writer = null;

            try {
                writer = new BufferedWriter(new FileWriter(dumpFileName));

                int consoleWidth = ConsoleUtil.getConsoleWidth();
                if (consoleWidth <= 0) {
                    consoleWidth = 120;
                }

                RawDexFile rawDexFile = new RawDexFile(new Opcodes(apiLevel), dexFile);
                DexAnnotator annotator = new DexAnnotator(rawDexFile, consoleWidth);
                annotator.writeAnnotations(writer);
            } catch (IOException ex) {
                System.err.println("There was an error while dumping the dex file to " + dumpFileName);
                ex.printStackTrace(System.err);
            } finally {
                if (writer != null) {
                    try {
                        writer.close();
                    } catch (IOException ex) {
                        System.err.println("There was an error while closing the dump file " + dumpFileName);
                        ex.printStackTrace(System.err);
                    }
                }
            }
        }
    }
}
