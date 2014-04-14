/*
 * Copyright 2013, Google Inc.
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

package org.jf.dexlib2.writer;

import org.jf.dexlib2.DebugItemType;
import org.jf.util.ExceptionWithContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;

public class DebugWriter<StringKey extends CharSequence, TypeKey extends CharSequence> {
    @Nonnull private final StringSection<StringKey, ?> stringSection;
    @Nonnull private final TypeSection<StringKey, TypeKey, ?> typeSection;
    @Nonnull private final DexDataWriter writer;
    private int currentAddress;
    private int currentLine;

    DebugWriter(@Nonnull StringSection<StringKey, ?> stringSection,
                @Nonnull TypeSection<StringKey, TypeKey, ?> typeSection,
                @Nonnull DexDataWriter writer) {
        this.stringSection = stringSection;
        this.typeSection = typeSection;
        this.writer = writer;
    }

    void reset(int startLine) {
        this.currentAddress = 0;
        this.currentLine = startLine;
    }

    public void writeStartLocal(int codeAddress, int register,
                                @Nullable StringKey name,
                                @Nullable TypeKey type,
                                @Nullable StringKey signature) throws IOException {
        int nameIndex = stringSection.getNullableItemIndex(name);
        int typeIndex = typeSection.getNullableItemIndex(type);
        int signatureIndex = stringSection.getNullableItemIndex(signature);

        writeAdvancePC(codeAddress);
        if (signatureIndex == DexWriter.NO_INDEX) {
            writer.write(DebugItemType.START_LOCAL);
            writer.writeUleb128(register);
            writer.writeUleb128(nameIndex + 1);
            writer.writeUleb128(typeIndex + 1);
        } else {
            writer.write(DebugItemType.START_LOCAL_EXTENDED);
            writer.writeUleb128(register);
            writer.writeUleb128(nameIndex + 1);
            writer.writeUleb128(typeIndex + 1);
            writer.writeUleb128(signatureIndex + 1);
        }
    }

    public void writeEndLocal(int codeAddress, int register) throws IOException {
        writeAdvancePC(codeAddress);
        writer.write(DebugItemType.END_LOCAL);
        writer.writeUleb128(register);
    }

    public void writeRestartLocal(int codeAddress, int register) throws IOException {
        writeAdvancePC(codeAddress);
        writer.write(DebugItemType.RESTART_LOCAL);
        writer.writeUleb128(register);
    }

    public void writePrologueEnd(int codeAddress) throws IOException {
        writeAdvancePC(codeAddress);
        writer.write(DebugItemType.PROLOGUE_END);
    }

    public void writeEpilogueBegin(int codeAddress) throws IOException {
        writeAdvancePC(codeAddress);
        writer.write(DebugItemType.EPILOGUE_BEGIN);
    }

    public void writeLineNumber(int codeAddress, int lineNumber) throws IOException {
        int lineDelta = lineNumber - currentLine;
        int addressDelta = codeAddress - currentAddress;

        if (addressDelta < 0) {
            throw new ExceptionWithContext("debug info items must have non-decreasing code addresses");
        }
        if (lineDelta < -4 || lineDelta > 10) {
            writeAdvanceLine(lineNumber);
            lineDelta = 0;
        } // no else is intentional here. we might need to advance the PC as well as the line
        if ((lineDelta < 2 && addressDelta > 16) || (lineDelta > 1 && addressDelta > 15)) {
            writeAdvancePC(codeAddress);
            addressDelta = 0;
        }

        // we need to emit the special opcode even if both lineDelta and addressDelta are 0, otherwise a positions
        // entry isn't generated
        writeSpecialOpcode(lineDelta, addressDelta);
    }

    public void writeSetSourceFile(int codeAddress, @Nullable StringKey sourceFile) throws IOException {
        writeAdvancePC(codeAddress);
        writer.write(DebugItemType.SET_SOURCE_FILE);
        writer.writeUleb128(stringSection.getNullableItemIndex(sourceFile) + 1);
    }

    private void writeAdvancePC(int address) throws IOException {
        int addressDelta = address - currentAddress;

        if (addressDelta > 0) {
            writer.write(1);
            writer.writeUleb128(addressDelta);
            currentAddress = address;
        } /*else if (addressDelta < 0) {
            throw new ExceptionWithContext("debug info items must have non-decreasing code addresses");
        }*/
    }

    private void writeAdvanceLine(int line) throws IOException {
        int lineDelta = line - currentLine;
        if (lineDelta != 0) {
            writer.write(2);
            writer.writeSleb128(lineDelta);
            currentLine = line;
        }
    }

    private static final int LINE_BASE     = -4;
    private static final int LINE_RANGE    = 15;
    private static final int FIRST_SPECIAL = 0x0a;

    private void writeSpecialOpcode(int lineDelta, int addressDelta) throws IOException {
        writer.write((byte)(FIRST_SPECIAL + (addressDelta * LINE_RANGE) + (lineDelta - LINE_BASE)));
        currentLine += lineDelta;
        currentAddress += addressDelta;
    }
}
