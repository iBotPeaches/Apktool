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

package org.jf.dexlib2.writer;

import org.jf.util.ExceptionWithContext;

import javax.annotation.Nonnull;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class DexDataWriter extends BufferedOutputStream {
    /**
     * The position within the file that we will write to next. This is only updated when the buffer is flushed to the
     * outputStream.
     */
    private int filePosition;

    /**
     * A temporary buffer that can be used for larger writes. Can be replaced with a larger buffer if needed.
     * Must be at least 8 bytes
     */
    private byte[] tempBuf = new byte[8];

    /** A buffer of 0s to use for writing alignment values */
    private byte[] zeroBuf = new byte[3];

    /**
     * Construct a new DexWriter instance that writes to output.
     *
     * @param output An OutputStream to write the data to.
     * @param filePosition The position within the file that OutputStream will write to.
     */
    public DexDataWriter(@Nonnull OutputStream output, int filePosition) {
        this(output, filePosition, 256 * 1024);
    }

    public DexDataWriter(@Nonnull OutputStream output, int filePosition, int bufferSize) {
        super(output, bufferSize);

        this.filePosition = filePosition;
    }

    @Override
    public void write(int b) throws IOException {
        filePosition++;
        super.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        filePosition += len;
        super.write(b, off, len);
    }

    public void writeLong(long value) throws IOException {
        writeInt((int)value);
        writeInt((int)(value >> 32));
    }

    public static void writeInt(OutputStream out, int value) throws IOException {
        out.write(value);
        out.write(value >> 8);
        out.write(value >> 16);
        out.write(value >> 24);
    }

    public void writeInt(int value) throws IOException {
        writeInt(this, value);
    }

    public void writeShort(int value) throws IOException {
        if (value < Short.MIN_VALUE || value > Short.MAX_VALUE) {
            throw new ExceptionWithContext("Short value out of range: %d", value);
        }
        write(value);
        write(value >> 8);
    }

    public void writeUshort(int value) throws IOException {
        if (value < 0 || value > 0xFFFF) {
            throw new ExceptionWithContext("Unsigned short value out of range: %d", value);
        }
        write(value);
        write(value >> 8);
    }

    public void writeUbyte(int value) throws IOException {
        if (value < 0 || value > 0xFF) {
            throw new ExceptionWithContext("Unsigned byte value out of range: %d", value);
        }
        write(value);
    }

    public static void writeUleb128(OutputStream out, int value) throws IOException {
        while ((value & 0xffffffffL) > 0x7f) {
            out.write((value & 0x7f) | 0x80);
            value >>>= 7;
        }
        out.write(value);
    }

    public void writeUleb128(int value) throws IOException {
        writeUleb128(this, value);
    }

    public static void writeSleb128(OutputStream out, int value) throws IOException {
        if (value >= 0) {
            while (value > 0x3f) {
                out.write((value & 0x7f) | 0x80);
                value >>>= 7;
            }
            out.write(value & 0x7f);
        } else {
            while (value < -0x40) {
                out.write((value & 0x7f) | 0x80);
                value >>= 7;
            }
            out.write(value & 0x7f);
        }
    }

    public void writeSleb128(int value) throws IOException {
        writeSleb128(this, value);
    }

    public void writeEncodedValueHeader(int valueType, int valueArg) throws IOException {
        write(valueType | (valueArg << 5));
    }

    public void writeEncodedInt(int valueType, int value) throws IOException {
        int index = 0;
        if (value >= 0) {
            while (value > 0x7f) {
                tempBuf[index++] = (byte)value;
                value >>= 8;
            }
        } else {
            while (value < -0x80) {
                tempBuf[index++] = (byte)value;
                value >>= 8;
            }
        }
        tempBuf[index++] = (byte)value;
        writeEncodedValueHeader(valueType, index-1);
        write(tempBuf, 0, index);
    }

    public void writeEncodedLong(int valueType, long value) throws IOException {
        int index = 0;
        if (value >= 0) {
            while (value > 0x7f) {
                tempBuf[index++] = (byte)value;
                value >>= 8;
            }
        } else {
            while (value < -0x80) {
                tempBuf[index++] = (byte)value;
                value >>= 8;
            }
        }
        tempBuf[index++] = (byte)value;
        writeEncodedValueHeader(valueType, index-1);
        write(tempBuf, 0, index);
    }

    public void writeEncodedUint(int valueType, int value) throws IOException {
        int index = 0;
        do {
            tempBuf[index++] = (byte)value;
            value >>>= 8;
        } while (value != 0);
        writeEncodedValueHeader(valueType, index-1);
        write(tempBuf, 0, index);
    }

    public void writeEncodedFloat(int valueType, float value) throws IOException {
        writeRightZeroExtendedInt(valueType, Float.floatToRawIntBits(value));
    }

    protected void writeRightZeroExtendedInt(int valueType, int value) throws IOException {
        int index = 3;
        do {
            tempBuf[index--] = (byte)((value & 0xFF000000) >>> 24);
            value <<= 8;
        } while (value != 0);

        int firstElement = index+1;
        int encodedLength = 4-firstElement;
        writeEncodedValueHeader(valueType, encodedLength - 1);
        write(tempBuf, firstElement, encodedLength);
    }

    public void writeEncodedDouble(int valueType, double value) throws IOException {
        writeRightZeroExtendedLong(valueType, Double.doubleToRawLongBits(value));
    }

    protected void writeRightZeroExtendedLong(int valueType, long value) throws IOException {
        int index = 7;
        do {
            tempBuf[index--] = (byte)((value & 0xFF00000000000000L) >>> 56);
            value <<= 8;
        } while (value != 0);

        int firstElement = index+1;
        int encodedLength = 8-firstElement;
        writeEncodedValueHeader(valueType, encodedLength - 1);
        write(tempBuf, firstElement, encodedLength);
    }

    public void writeString(String string) throws IOException {
        int len = string.length();

        // make sure we have enough room in the temporary buffer
        if (tempBuf.length <= string.length()*3) {
            tempBuf = new byte[string.length()*3];
        }

        final byte[] buf = tempBuf;

        int bufPos = 0;
        for (int i = 0; i < len; i++) {
            char c = string.charAt(i);
            if ((c != 0) && (c < 0x80)) {
                buf[bufPos++] = (byte)c;
            } else if (c < 0x800) {
                buf[bufPos++] = (byte)(((c >> 6) & 0x1f) | 0xc0);
                buf[bufPos++] = (byte)((c & 0x3f) | 0x80);
            } else {
                buf[bufPos++] = (byte)(((c >> 12) & 0x0f) | 0xe0);
                buf[bufPos++] = (byte)(((c >> 6) & 0x3f) | 0x80);
                buf[bufPos++] = (byte)((c & 0x3f) | 0x80);
            }
        }
        write(buf, 0, bufPos);
    }

    public void align() throws IOException {
        int zeros = (-getPosition()) & 3;
        if (zeros > 0) {
            write(zeroBuf, 0, zeros);
        }
    }

    public int getPosition() {
        return filePosition;
    }
}
