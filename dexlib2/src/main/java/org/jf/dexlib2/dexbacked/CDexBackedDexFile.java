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

import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.dexbacked.raw.CdexHeaderItem;
import org.jf.dexlib2.dexbacked.raw.HeaderItem;
import org.jf.dexlib2.util.DexUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.UnsupportedEncodingException;

public class CDexBackedDexFile extends DexBackedDexFile {
    public CDexBackedDexFile(@Nullable Opcodes opcodes, @Nonnull byte[] buf, int offset, boolean verifyMagic) {
        super(opcodes, buf, offset, verifyMagic);
    }

    public CDexBackedDexFile(@Nullable Opcodes opcodes, @Nonnull DexBuffer buf) {
        super(opcodes, buf);
    }

    public CDexBackedDexFile(@Nullable Opcodes opcodes, @Nonnull byte[] buf, int offset) {
        super(opcodes, buf, offset);
    }

    public CDexBackedDexFile(@Nullable Opcodes opcodes, @Nonnull byte[] buf) {
        super(opcodes, buf);
    }

    public static boolean isCdex(byte[] buf, int offset) {
        if (offset + 4 > buf.length) {
            return false;
        }

        byte[] cdexMagic;
        try {
            cdexMagic = "cdex".getBytes("US-ASCII");
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
        return buf[offset] == cdexMagic[0] &&
                buf[offset+1] == cdexMagic[1] &&
                buf[offset+2] == cdexMagic[2] &&
                buf[offset+3] == cdexMagic[3];
    }

    @Override
    protected int getVersion(byte[] buf, int offset, boolean verifyMagic) {
        if (verifyMagic) {
            return DexUtil.verifyCdexHeader(buf, offset);
        } else {
            return CdexHeaderItem.getVersion(buf, offset);
        }
    }

    @Override
    protected Opcodes getDefaultOpcodes(int version) {
        // There is currently only 1 possible cdex version, which was introduced in api 28.
        return Opcodes.forApi(28);
    }

    @Override
    public int getBaseDataOffset() {
        return getBuffer().readSmallUint(HeaderItem.DATA_START_OFFSET);
    }

    public int getDebugInfoOffsetsPos() {
        return getBuffer().readSmallUint(CdexHeaderItem.DEBUG_INFO_OFFSETS_POS_OFFSET);
    }

    public int getDebugInfoOffsetsTableOffset() {
        return getBuffer().readSmallUint(CdexHeaderItem.DEBUG_INFO_OFFSETS_TABLE_OFFSET);
    }

    public int getDebugInfoBase() {
        return getBuffer().readSmallUint(CdexHeaderItem.DEBUG_INFO_BASE);
    }

    @Override
    protected DexBackedMethodImplementation createMethodImplementation(
            @Nonnull DexBackedDexFile dexFile, @Nonnull DexBackedMethod method, int codeOffset) {
        return new CDexBackedMethodImplementation(dexFile, method, codeOffset);
    }
}
