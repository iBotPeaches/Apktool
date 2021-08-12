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

package org.jf.dexlib2.dexbacked;

import org.jf.util.ExceptionWithContext;

import javax.annotation.Nonnull;
import java.util.Arrays;

public class DexBuffer {
    @Nonnull final byte[] buf;
    final int baseOffset;

    public DexBuffer(@Nonnull byte[] buf) {
        this(buf, 0);
    }
    public DexBuffer(@Nonnull byte[] buf, int offset) {
        this.buf = buf;
        this.baseOffset = offset;
    }

    public int readSmallUint(int offset) {
        byte[] buf = this.buf;
        offset += baseOffset;
        int result = (buf[offset] & 0xff) |
                ((buf[offset+1] & 0xff) << 8) |
                ((buf[offset+2] & 0xff) << 16) |
                ((buf[offset+3]) << 24);
        if (result < 0) {
            throw new ExceptionWithContext("Encountered small uint that is out of range at offset 0x%x", offset);
        }
        return result;
    }

    public int readOptionalUint(int offset) {
        byte[] buf = this.buf;
        offset += baseOffset;
        int result = (buf[offset] & 0xff) |
                ((buf[offset+1] & 0xff) << 8) |
                ((buf[offset+2] & 0xff) << 16) |
                ((buf[offset+3]) << 24);
        if (result < -1) {
            throw new ExceptionWithContext("Encountered optional uint that is out of range at offset 0x%x", offset);
        }
        return result;
    }

    public int readUshort(int offset) {
        byte[] buf = this.buf;
        offset += baseOffset;
        return (buf[offset] & 0xff) |
                ((buf[offset+1] & 0xff) << 8);
    }

    public int readUbyte(int offset) {
        return buf[offset + baseOffset] & 0xff;
    }

    public long readLong(int offset) {
        byte[] buf = this.buf;
        offset += baseOffset;
        return (buf[offset] & 0xff) |
                ((buf[offset+1] & 0xff) << 8) |
                ((buf[offset+2] & 0xff) << 16) |
                ((buf[offset+3] & 0xffL) << 24) |
                ((buf[offset+4] & 0xffL) << 32) |
                ((buf[offset+5] & 0xffL) << 40) |
                ((buf[offset+6] & 0xffL) << 48) |
                (((long)buf[offset+7]) << 56);
    }

    public int readLongAsSmallUint(int offset) {
        byte[] buf = this.buf;
        offset += baseOffset;
        long result = (buf[offset] & 0xff) |
                ((buf[offset+1] & 0xff) << 8) |
                ((buf[offset+2] & 0xff) << 16) |
                ((buf[offset+3] & 0xffL) << 24) |
                ((buf[offset+4] & 0xffL) << 32) |
                ((buf[offset+5] & 0xffL) << 40) |
                ((buf[offset+6] & 0xffL) << 48) |
                (((long)buf[offset+7]) << 56);
        if (result < 0 || result > Integer.MAX_VALUE) {
            throw new ExceptionWithContext("Encountered out-of-range ulong at offset 0x%x", offset);
        }
        return (int)result;
    }

    public int readInt(int offset) {
        byte[] buf = this.buf;
        offset += baseOffset;
        return (buf[offset] & 0xff) |
                ((buf[offset+1] & 0xff) << 8) |
                ((buf[offset+2] & 0xff) << 16) |
                (buf[offset+3] << 24);
    }

    public int readShort(int offset) {
        byte[] buf = this.buf;
        offset += baseOffset;
        return (buf[offset] & 0xff) |
                (buf[offset+1] << 8);
    }

    public int readByte(int offset) {
        return buf[baseOffset + offset];
    }

    @Nonnull
    public byte[] readByteRange(int start, int length) {
        return Arrays.copyOfRange(buf, baseOffset + start, baseOffset + start + length);
    }

    @Nonnull
    public DexReader<? extends DexBuffer> readerAt(int offset) {
        return new DexReader<DexBuffer>(this, offset);
    }

    @Nonnull
    public byte[] getBuf() {
        return buf;
    }

    public int getBaseOffset() {
        return baseOffset;
    }
}
