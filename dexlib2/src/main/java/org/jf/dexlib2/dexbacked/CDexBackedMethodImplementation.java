/*
 * Copyright 2019, Google Inc.
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

package org.jf.dexlib2.dexbacked;

import javax.annotation.Nonnull;

import static org.jf.dexlib2.dexbacked.raw.CodeItem.*;

public class CDexBackedMethodImplementation extends DexBackedMethodImplementation {

    public CDexBackedMethodImplementation(
            @Nonnull DexBackedDexFile dexFile, @Nonnull DexBackedMethod method, int codeOffset) {
        super(dexFile, method, codeOffset);
    }

    public int getInsCount() {
        int insCount = (dexFile.getDataBuffer().readUshort(codeOffset) >> CDEX_INS_COUNT_SHIFT) & 0xf;

        if ((getPreheaderFlags() & CDEX_PREHEADER_FLAG_INS_COUNT) != 0) {
            int preheaderCount = 1;

            if ((getPreheaderFlags() & CDEX_PREHEADER_FLAG_INSTRUCTIONS_SIZE) != 0) {
                preheaderCount+=2;
            }
            if ((getPreheaderFlags() & CDEX_PREHEADER_FLAG_REGISTER_COUNT) != 0) {
                preheaderCount++;
            }
            insCount += dexFile.getDataBuffer().readUshort(codeOffset - 2 * preheaderCount);
        }
        return insCount;
    }

    @Override
    public int getRegisterCount() {
        int registerCount = (dexFile.getDataBuffer().readUshort(codeOffset) >> CDEX_REGISTER_COUNT_SHIFT) & 0xf;

        registerCount += getInsCount();
        if ((getPreheaderFlags() & CDEX_PREHEADER_FLAG_REGISTER_COUNT) != 0) {
            int preheaderCount = 1;
            if ((getPreheaderFlags() & CDEX_PREHEADER_FLAG_INSTRUCTIONS_SIZE) > 0) {
                preheaderCount += 2;
            }
            registerCount += dexFile.getDataBuffer().readUshort(codeOffset - 2 * preheaderCount);
        }
        return registerCount;
    }

    @Override
    protected int getInstructionsSize() {
        int instructionsSize = dexFile.getDataBuffer().readUshort(
                codeOffset + CDEX_INSTRUCTIONS_SIZE_AND_PREHEADER_FLAGS_OFFSET) >>
                CDEX_INSTRUCTIONS_SIZE_SHIFT;

        if ((getPreheaderFlags() & CDEX_PREHEADER_FLAG_INSTRUCTIONS_SIZE) != 0) {
            instructionsSize += dexFile.getDataBuffer().readUshort(codeOffset - 2);
            instructionsSize += dexFile.getDataBuffer().readUshort(codeOffset - 4) << 16;
        }
        return instructionsSize;
    }

    @Override
    protected int getInstructionsStartOffset() {
        return codeOffset + 4;
    }

    private int getPreheaderFlags() {
        return dexFile.getDataBuffer().readUshort(codeOffset + CDEX_INSTRUCTIONS_SIZE_AND_PREHEADER_FLAGS_OFFSET) &
                CDEX_PREHEADER_FLAGS_MASK;
    }

    @Override
    protected int getTriesSize() {
        int triesCount = (dexFile.getDataBuffer().readUshort(codeOffset) >> CDEX_TRIES_SIZE_SHIFT) & 0xf;
        if ((getPreheaderFlags() & CDEX_PREHEADER_FLAG_TRIES_COUNT) != 0) {
            int preheaderCount = Integer.bitCount(getPreheaderFlags());
            if ((getPreheaderFlags() & CDEX_PREHEADER_FLAG_INSTRUCTIONS_SIZE) != 0) {
                // The instructions size preheader is 2 shorts
                preheaderCount++;
            }
            triesCount += dexFile.getDataBuffer().readUshort(codeOffset - 2 * preheaderCount);
        }
        return triesCount;
    }

    @Override
    protected int getDebugOffset() {
        CDexBackedDexFile cdexFile = ((CDexBackedDexFile) dexFile);

        int debugTableItemOffset = (method.methodIndex / 16) * 4;
        int bitIndex = method.methodIndex % 16;

        int debugInfoOffsetsPos = cdexFile.getDebugInfoOffsetsPos();
        int debugTableOffset = debugInfoOffsetsPos + cdexFile.getDebugInfoOffsetsTableOffset();

        int debugOffsetsOffset = cdexFile.getDataBuffer().readSmallUint(debugTableOffset + debugTableItemOffset);

        DexReader reader = cdexFile.getDataBuffer().readerAt(debugInfoOffsetsPos + debugOffsetsOffset);

        int bitMask = reader.readUbyte() << 8;
        bitMask += reader.readUbyte();

        if ((bitMask & (1 << bitIndex)) == 0) {
            return 0;
        }

        int offsetCount = Integer.bitCount(bitMask & 0xFFFF >> (16-bitIndex));
        int baseDebugOffset = cdexFile.getDebugInfoBase();
        for (int i=0; i<offsetCount; i++) {
            baseDebugOffset += reader.readBigUleb128();
        }
        baseDebugOffset += reader.readBigUleb128();
        return baseDebugOffset;
    }
}
