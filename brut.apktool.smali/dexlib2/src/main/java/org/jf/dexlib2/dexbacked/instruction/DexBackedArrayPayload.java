/*
 * Copyright 2012, Google Inc.
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

package org.jf.dexlib2.dexbacked.instruction;

import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.dexbacked.util.FixedSizeList;
import org.jf.dexlib2.iface.instruction.formats.ArrayPayload;
import org.jf.util.ExceptionWithContext;

import javax.annotation.Nonnull;
import java.util.List;

public class DexBackedArrayPayload extends DexBackedInstruction implements ArrayPayload {
    public static final Opcode OPCODE = Opcode.ARRAY_PAYLOAD;

    public final int elementWidth;
    public final int elementCount;

    private static final int ELEMENT_WIDTH_OFFSET = 2;
    private static final int ELEMENT_COUNT_OFFSET = 4;
    private static final int ELEMENTS_OFFSET = 8;

    public DexBackedArrayPayload(@Nonnull DexBackedDexFile dexFile,
                                 int instructionStart) {
        super(dexFile, OPCODE, instructionStart);

        elementWidth = dexFile.readUshort(instructionStart + ELEMENT_WIDTH_OFFSET);
        elementCount = dexFile.readSmallUint(instructionStart + ELEMENT_COUNT_OFFSET);
    }

    @Override public int getElementWidth() { return elementWidth; }

    @Nonnull
    @Override
    public List<Number> getArrayElements() {
        final int elementsStart = instructionStart + ELEMENTS_OFFSET;

        abstract class ReturnedList extends FixedSizeList<Number> {
            @Override public int size() { return elementCount; }
        }

        switch (elementWidth) {
            case 1:
                return new ReturnedList() {
                    @Nonnull
                    @Override
                    public Number readItem(int index) {
                        return dexFile.readByte(elementsStart + index);
                    }
                };
            case 2:
                return new ReturnedList() {
                    @Nonnull
                    @Override
                    public Number readItem(int index) {
                        return dexFile.readShort(elementsStart + index*2);
                    }
                };
            case 4:
                return new ReturnedList() {
                    @Nonnull
                    @Override
                    public Number readItem(int index) {
                        return dexFile.readInt(elementsStart + index*4);
                    }
                };
            case 8:
                return new ReturnedList() {
                    @Nonnull
                    @Override
                    public Number readItem(int index) {
                        return dexFile.readLong(elementsStart + index*8);
                    }
                };
            default:
                throw new ExceptionWithContext("Invalid element width: %d", elementWidth);
        }
    }

    @Override
    public int getCodeUnits() {
        return 4 + (elementWidth*elementCount + 1) / 2;
    }
}
