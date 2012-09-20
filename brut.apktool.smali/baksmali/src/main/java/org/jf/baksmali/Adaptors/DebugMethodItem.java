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

package org.jf.baksmali.Adaptors;

import org.jf.dexlib.Util.Utf8Utils;
import org.jf.util.IndentingWriter;
import org.jf.dexlib.CodeItem;
import org.jf.dexlib.StringIdItem;
import org.jf.dexlib.TypeIdItem;

import java.io.IOException;

public abstract class DebugMethodItem extends MethodItem {
    private final double sortOrder;

    public DebugMethodItem(int codeAddress, double sortOrder) {
        super(codeAddress);
        this.sortOrder = sortOrder;
    }

    public double getSortOrder() {
        return sortOrder;
    }

    protected static void writeLine(IndentingWriter writer, int line) throws IOException {
        writer.write(".line ");
        writer.printSignedIntAsDec(line);
    }

    protected static void writeEndPrologue(IndentingWriter writer) throws IOException {
        writer.write(".prologue");
    }

    protected static void writeBeginEpilogue(IndentingWriter writer) throws IOException {
        writer.write(".epilogue");
    }

    protected static void writeStartLocal(IndentingWriter writer, CodeItem codeItem, int register,
                                          StringIdItem name, TypeIdItem type, StringIdItem signature)
                                          throws IOException {
        writer.write(".local ");
        RegisterFormatter.writeTo(writer, codeItem, register);
        writer.write(", ");
        writer.write(name.getStringValue());
        writer.write(':');
        writer.write(type.getTypeDescriptor());
        if (signature != null) {
            writer.write(",\"");
            writer.write(signature.getStringValue());
            writer.write('"');
        }
    }

    protected static void writeEndLocal(IndentingWriter writer, CodeItem codeItem, int register, StringIdItem name,
                                       TypeIdItem type, StringIdItem signature) throws IOException {
        writer.write(".end local ");
        RegisterFormatter.writeTo(writer, codeItem, register);

        if (name != null) {
            writer.write("           #");
            writer.write(name.getStringValue());
            writer.write(':');
            writer.write(type.getTypeDescriptor());
            if (signature != null) {
                writer.write(",\"");
                writer.write(signature.getStringValue());
                writer.write('"');
            }
        }
    }


    protected static void writeRestartLocal(IndentingWriter writer, CodeItem codeItem, int register,
                                         StringIdItem name, TypeIdItem type, StringIdItem signature)
                                         throws IOException {
        writer.write(".restart local ");
        RegisterFormatter.writeTo(writer, codeItem, register);

        if (name != null) {
            writer.write("       #");
            writer.write(name.getStringValue());
            writer.write(':');
            writer.write(type.getTypeDescriptor());
            if (signature != null) {
                writer.write(",\"");
                writer.write(signature.getStringValue());
                writer.write('"');
            }
        }
    }

    protected static void writeSetFile(IndentingWriter writer, String fileName) throws IOException {
        writer.write(".source \"");
        Utf8Utils.writeEscapedString(writer, fileName);
        writer.write('"');
    }
}
