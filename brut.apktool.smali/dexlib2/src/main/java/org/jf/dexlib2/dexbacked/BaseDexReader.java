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
import org.jf.util.Utf8Utils;

import javax.annotation.Nonnull;

public class BaseDexReader<T extends BaseDexBuffer> {
    @Nonnull public final T dexBuf;
    private int offset;

    public BaseDexReader(@Nonnull T dexBuf, int offset) {
        this.dexBuf = dexBuf;
        this.offset = offset;
    }

    public int getOffset() { return offset; }
    public void setOffset(int offset) { this.offset = offset; }

    /** {@inheritDoc} */
    public int readSleb128() {
        int end = offset;
        int currentByteValue;
        int result;
        byte[] buf = dexBuf.buf;

        result = buf[end++] & 0xff;
        if (result <= 0x7f) {
            result = (result << 25) >> 25;
        } else {
            currentByteValue = buf[end++] & 0xff;
            result = (result & 0x7f) | ((currentByteValue & 0x7f) << 7);
            if (currentByteValue <= 0x7f) {
                result = (result << 18) >> 18;
            } else {
                currentByteValue = buf[end++] & 0xff;
                result |= (currentByteValue & 0x7f) << 14;
                if (currentByteValue <= 0x7f) {
                    result = (result << 11) >> 11;
                } else {
                    currentByteValue = buf[end++] & 0xff;
                    result |= (currentByteValue & 0x7f) << 21;
                    if (currentByteValue <= 0x7f) {
                        result = (result << 4) >> 4;
                    } else {
                        currentByteValue = buf[end++] & 0xff;
                        if (currentByteValue > 0x7f) {
                            throw new ExceptionWithContext(
                                    "Invalid sleb128 integer encountered at offset 0x%x", offset);
                        }
                        result |= currentByteValue << 28;
                    }
                }
            }
        }

        offset = end;
        return result;
    }

    public int readSmallUleb128() {
        return readUleb128(false);
    }

    private int readUleb128(boolean allowLarge) {
        int end = offset;
        int currentByteValue;
        int result;
        byte[] buf = dexBuf.buf;

        result = buf[end++] & 0xff;
        if (result > 0x7f) {
            currentByteValue = buf[end++] & 0xff;
            result = (result & 0x7f) | ((currentByteValue & 0x7f) << 7);
            if (currentByteValue > 0x7f) {
                currentByteValue = buf[end++] & 0xff;
                result |= (currentByteValue & 0x7f) << 14;
                if (currentByteValue > 0x7f) {
                    currentByteValue = buf[end++] & 0xff;
                    result |= (currentByteValue & 0x7f) << 21;
                    if (currentByteValue > 0x7f) {
                        currentByteValue = buf[end++];

                        // MSB shouldn't be set on last byte
                        if (currentByteValue < 0) {
                            throw new ExceptionWithContext(
                                    "Invalid uleb128 integer encountered at offset 0x%x", offset);
                        } else if ((currentByteValue & 0xf) > 0x07) {
                            if (!allowLarge) {
                                // for non-large uleb128s, we assume most significant bit of the result will not be
                                // set, so that it can fit into a signed integer without wrapping
                                throw new ExceptionWithContext(
                                        "Encountered valid uleb128 that is out of range at offset 0x%x", offset);
                            }
                        }
                        result |= currentByteValue << 28;
                    }
                }
            }
        }

        offset = end;
        return result;
    }

    /**
     * Reads a "large" uleb128. That is, one that may legitimately be greater than a signed int.
     *
     * The value is returned as if it were signed. i.e. a value of 0xFFFFFFFF would be returned as -1. It is up to the
     * caller to handle the value appropriately.
     */
    public int readLargeUleb128() {
       return readUleb128(true);
    }

    /**
     * Reads a "big" uleb128 that can legitimately be > 2^31. The value is returned as a signed integer, with the
     * expected semantics of re-interpreting an unsigned value as a signed value.
     *
     * @return The unsigned value, reinterpreted as a signed int
     */
    public int readBigUleb128() {
        int end = offset;
        int currentByteValue;
        int result;
        byte[] buf = dexBuf.buf;

        result = buf[end++] & 0xff;
        if (result > 0x7f) {
            currentByteValue = buf[end++] & 0xff;
            result = (result & 0x7f) | ((currentByteValue & 0x7f) << 7);
            if (currentByteValue > 0x7f) {
                currentByteValue = buf[end++] & 0xff;
                result |= (currentByteValue & 0x7f) << 14;
                if (currentByteValue > 0x7f) {
                    currentByteValue = buf[end++] & 0xff;
                    result |= (currentByteValue & 0x7f) << 21;
                    if (currentByteValue > 0x7f) {
                        currentByteValue = buf[end++];

                        // MSB shouldn't be set on last byte
                        if (currentByteValue < 0) {
                            throw new ExceptionWithContext(
                                    "Invalid uleb128 integer encountered at offset 0x%x", offset);
                        }
                        result |= currentByteValue << 28;
                    }
                }
            }
        }

        offset = end;
        return result;
    }

    public void skipUleb128() {
        int end = offset;
        byte currentByteValue;
        byte[] buf = dexBuf.buf;

        currentByteValue = buf[end++];
        if (currentByteValue < 0) { // if the MSB is set
            currentByteValue = buf[end++];
            if (currentByteValue < 0) { // if the MSB is set
                currentByteValue = buf[end++];
                if (currentByteValue < 0) { // if the MSB is set
                    currentByteValue = buf[end++];
                    if (currentByteValue < 0) { // if the MSB is set
                        currentByteValue = buf[end++];
                        if (currentByteValue < 0) {
                            throw new ExceptionWithContext(
                                    "Invalid uleb128 integer encountered at offset 0x%x", offset);
                        }
                    }
                }
            }
        }

        offset = end;
    }

    public int readSmallUint() {
        int o = offset;
        int result = dexBuf.readSmallUint(o);
        offset = o + 4;
        return result;
    }

    public int readOptionalUint() {
        int o = offset;
        int result = dexBuf.readOptionalUint(o);
        offset = o + 4;
        return result;
    }

    public int peekUshort() {
        return dexBuf.readUshort(offset);
    }

    public int readUshort() {
        int o = offset;
        int result = dexBuf.readUshort(offset);
        offset = o + 2;
        return result;
    }

    public int peekUbyte() {
        return dexBuf.readUbyte(offset);
    }

    public int readUbyte() {
        int o = offset;
        int result = dexBuf.readUbyte(offset);
        offset = o + 1;
        return result;
    }

    public long readLong() {
        int o = offset;
        long result = dexBuf.readLong(offset);
        offset = o + 8;
        return result;
    }

    public int readInt() {
        int o = offset;
        int result = dexBuf.readInt(offset);
        offset = o + 4;
        return result;
    }

    public int readShort() {
        int o = offset;
        int result = dexBuf.readShort(offset);
        offset = o + 2;
        return result;
    }

    public int readByte() {
        int o = offset;
        int result = dexBuf.readByte(offset);
        offset = o + 1;
        return result;
    }

    public void skipByte() { offset++; }
    public void moveRelative(int i) { offset += i; }

    public int readSmallUint(int offset) { return dexBuf.readSmallUint(offset); }
    public int readUshort(int offset) { return dexBuf.readUshort(offset); }
    public int readUbyte(int offset) { return dexBuf.readUbyte(offset); }
    public long readLong(int offset) { return dexBuf.readLong(offset); }
    public int readInt(int offset) { return dexBuf.readInt(offset); }
    public int readShort(int offset) { return dexBuf.readShort(offset); }
    public int readByte(int offset) { return dexBuf.readByte(offset); }

    public int readSizedInt(int bytes) {
        int o = offset;
        byte[] buf = dexBuf.buf;

        int result;
        switch (bytes) {
            case 4:
                result = (buf[o] & 0xff) |
                        ((buf[o+1] & 0xff) << 8) |
                        ((buf[o+2] & 0xff) << 16) |
                        (buf[o+3] << 24);
                break;
            case 3:
                result = (buf[o] & 0xff) |
                        ((buf[o+1] & 0xff) << 8) |
                        ((buf[o+2]) << 16);
                break;
            case 2:
                result = (buf[o] & 0xff) |
                        ((buf[o+1]) << 8);
                break;
            case 1:
                result = buf[o];
                break;
            default:
                throw new ExceptionWithContext("Invalid size %d for sized int at offset 0x%x", bytes, offset);
        }
        offset = o + bytes;
        return result;
    }

    public int readSizedSmallUint(int bytes) {
        int o = offset;
        byte[] buf = dexBuf.buf;

        int result = 0;
        switch (bytes) {
            case 4:
                int b = buf[o+3];
                if (b < 0) {
                    throw new ExceptionWithContext(
                            "Encountered valid sized uint that is out of range at offset 0x%x", offset);
                }
                result = b << 24;
                // fall-through
            case 3:
                result |= (buf[o+2] & 0xff) << 16;
                // fall-through
            case 2:
                result |= (buf[o+1] & 0xff) << 8;
                // fall-through
            case 1:
                result |= (buf[o] & 0xff);
                break;
            default:
                throw new ExceptionWithContext("Invalid size %d for sized uint at offset 0x%x", bytes, offset);
        }
        offset = o + bytes;
        return result;
    }

    public int readSizedRightExtendedInt(int bytes) {
        int o = offset;
        byte[] buf = dexBuf.buf;

        int result;
        switch (bytes) {
            case 4:
                result = (buf[o] & 0xff) |
                        ((buf[o+1] & 0xff) << 8) |
                        ((buf[o+2] & 0xff) << 16) |
                        (buf[o+3] << 24);
                break;
            case 3:
                result = (buf[o] & 0xff) << 8 |
                        ((buf[o+1] & 0xff) << 16) |
                        (buf[o+2] << 24);
                break;
            case 2:
                result = (buf[o] & 0xff) << 16 |
                        (buf[o+1] << 24);
                break;
            case 1:
                result = buf[o] << 24;
                break;
            default:
                throw new ExceptionWithContext(
                        "Invalid size %d for sized, right extended int at offset 0x%x", bytes, offset);
        }
        offset = o + bytes;
        return result;
    }

    public long readSizedRightExtendedLong(int bytes) {
        int o = offset;
        byte[] buf = dexBuf.buf;

        long result;
        switch (bytes) {
            case 8:
                result = (buf[o] & 0xff) |
                        ((buf[o+1] & 0xff) << 8) |
                        ((buf[o+2] & 0xff) << 16) |
                        ((buf[o+3] & 0xffL) << 24) |
                        ((buf[o+4] & 0xffL) << 32) |
                        ((buf[o+5] & 0xffL) << 40) |
                        ((buf[o+6] & 0xffL) << 48) |
                        (((long)buf[o+7]) << 56);
                break;
            case 7:
                result = ((buf[o] & 0xff)) << 8 |
                        ((buf[o+1] & 0xff) << 16) |
                        ((buf[o+2] & 0xffL) << 24) |
                        ((buf[o+3] & 0xffL) << 32) |
                        ((buf[o+4] & 0xffL) << 40) |
                        ((buf[o+5] & 0xffL) << 48) |
                        (((long)buf[o+6]) << 56);
                break;
            case 6:
                result = ((buf[o] & 0xff)) << 16 |
                        ((buf[o+1] & 0xffL) << 24) |
                        ((buf[o+2] & 0xffL) << 32) |
                        ((buf[o+3] & 0xffL) << 40) |
                        ((buf[o+4] & 0xffL) << 48) |
                        (((long)buf[o+5]) << 56);
                break;
            case 5:
                result = ((buf[o] & 0xffL)) << 24 |
                        ((buf[o+1] & 0xffL) << 32) |
                        ((buf[o+2] & 0xffL) << 40) |
                        ((buf[o+3] & 0xffL) << 48) |
                        (((long)buf[o+4]) << 56);
                break;
            case 4:
                result = ((buf[o] & 0xffL)) << 32 |
                        ((buf[o+1] & 0xffL) << 40) |
                        ((buf[o+2] & 0xffL) << 48) |
                        (((long)buf[o+3]) << 56);
                break;
            case 3:
                result = ((buf[o] & 0xffL)) << 40 |
                        ((buf[o+1] & 0xffL) << 48) |
                        (((long)buf[o+2]) << 56);
                break;
            case 2:
                result = ((buf[o] & 0xffL)) << 48 |
                        (((long)buf[o+1]) << 56);
                break;
            case 1:
                result = ((long)buf[o]) << 56;
                break;
            default:
                throw new ExceptionWithContext(
                        "Invalid size %d for sized, right extended long at offset 0x%x", bytes, offset);
        }
        offset = o + bytes;
        return result;
    }

    public long readSizedLong(int bytes) {
        int o = offset;
        byte[] buf = dexBuf.buf;

        long result;
        switch (bytes) {
            case 8:
                result = (buf[o] & 0xff) |
                        ((buf[o+1] & 0xff) << 8) |
                        ((buf[o+2] & 0xff) << 16) |
                        ((buf[o+3] & 0xffL) << 24) |
                        ((buf[o+4] & 0xffL) << 32) |
                        ((buf[o+5] & 0xffL) << 40) |
                        ((buf[o+6] & 0xffL) << 48) |
                        (((long)buf[o+7]) << 56);
                break;
            case 7:
                result = (buf[o] & 0xff) |
                        ((buf[o+1] & 0xff) << 8) |
                        ((buf[o+2] & 0xff) << 16) |
                        ((buf[o+3] & 0xffL) << 24) |
                        ((buf[o+4] & 0xffL) << 32) |
                        ((buf[o+5] & 0xffL) << 40) |
                        ((long)(buf[o+6]) << 48);
                break;
            case 6:
                result = (buf[o] & 0xff) |
                        ((buf[o+1] & 0xff) << 8) |
                        ((buf[o+2] & 0xff) << 16) |
                        ((buf[o+3] & 0xffL) << 24) |
                        ((buf[o+4] & 0xffL) << 32) |
                        ((long)(buf[o+5]) << 40);
                break;
            case 5:
                result = (buf[o] & 0xff) |
                        ((buf[o+1] & 0xff) << 8) |
                        ((buf[o+2] & 0xff) << 16) |
                        ((buf[o+3] & 0xffL) << 24) |
                        ((long)(buf[o+4]) << 32);
                break;
            case 4:
                result = (buf[o] & 0xff) |
                        ((buf[o+1] & 0xff) << 8) |
                        ((buf[o+2] & 0xff) << 16) |
                        (((long)buf[o+3]) << 24);
                break;
            case 3:
                result = (buf[o] & 0xff) |
                        ((buf[o+1] & 0xff) << 8) |
                        (buf[o+2] << 16);
                break;
            case 2:
                result = (buf[o] & 0xff) |
                        (buf[o+1] << 8);
                break;
            case 1:
                result = buf[o];
                break;
            default:
                throw new ExceptionWithContext("Invalid size %d for sized long at offset 0x%x", bytes, offset);
        }

        offset = o + bytes;
        return result;
    }

    public String readString(int utf16Length) {
        int[] ret = new int[1];
        String value = Utf8Utils.utf8BytesWithUtf16LengthToString(dexBuf.buf, offset, utf16Length, ret);
        offset += ret[0];
        return value;
    }
}
