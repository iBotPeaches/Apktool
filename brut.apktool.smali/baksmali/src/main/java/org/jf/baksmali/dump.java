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

import org.jf.dexlib.DexFile;
import org.jf.dexlib.Util.ByteArrayAnnotatedOutput;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

public class dump {
    public static void dump(DexFile dexFile, String dumpFileName, String outputDexFileName, boolean sort)
            throws IOException {

        if (sort) {
            //sort all items, to guarantee a unique ordering
            dexFile.setSortAllItems(true);
        } else {
            //don't change the order
            dexFile.setInplace(true);
        }

        ByteArrayAnnotatedOutput out = new ByteArrayAnnotatedOutput();

        if (dumpFileName != null) {
            out.enableAnnotations(120, true);
        }

        dexFile.place();
        dexFile.writeTo(out);

        //write the dump
        if (dumpFileName != null) {
            out.finishAnnotating();
            FileWriter writer = null;


            try {
                writer = new FileWriter(dumpFileName);
                out.writeAnnotationsTo(writer);
            } catch (IOException ex) {
                System.err.println("\n\nThere was an error while dumping the dex file to " + dumpFileName);
                ex.printStackTrace();
            } finally {
                if (writer != null) {
                    try {
                        writer.close();
                    } catch (IOException ex) {
                        System.err.println("\n\nThere was an error while closing the dump file " + dumpFileName);
                        ex.printStackTrace();
                    }
                }
            }
        }

        //rewrite the dex file
        if (outputDexFileName != null) {
            byte[] bytes = out.toByteArray();

            DexFile.calcSignature(bytes);
            DexFile.calcChecksum(bytes);

            FileOutputStream fileOutputStream = null;
            try {
                fileOutputStream = new FileOutputStream(outputDexFileName);
                fileOutputStream.write(bytes);
            } catch (IOException ex) {
                System.err.println("\n\nThere was an error while writing the dex file " + outputDexFileName);
                ex.printStackTrace();
            } finally {
                if (fileOutputStream != null) {
                    try {
                        fileOutputStream.close();
                    } catch (IOException ex) {
                        System.err.println("\n\nThere was an error while closing the dex file " + outputDexFileName);
                        ex.printStackTrace();
                    }
                }
            }
        }
    }
}
