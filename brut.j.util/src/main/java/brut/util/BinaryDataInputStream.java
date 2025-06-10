/*
 *  Copyright (C) 2010 Ryszard Wiśniewski <brut.alll@gmail.com>
 *  Copyright (C) 2010 Connor Tumbleson <connor.tumbleson@gmail.com>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package brut.util;

import com.google.common.io.ByteStreams;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.nio.ByteOrder;

public class BinaryDataInputStream extends FilterInputStream implements DataInput {
    private final ByteOrder mByteOrder;
    private final long mLimit;
    private long mPosition;
    private long mMark;

    public BinaryDataInputStream(byte[] in) {
        this(new ByteArrayInputStream(in), in.length);
    }

    public BinaryDataInputStream(byte[] in, ByteOrder bo) {
        this(new ByteArrayInputStream(in), bo, in.length);
    }

    public BinaryDataInputStream(InputStream in) {
        this(in, ByteOrder.LITTLE_ENDIAN, Long.MAX_VALUE);
    }

    public BinaryDataInputStream(InputStream in, long limit) {
        this(in, ByteOrder.LITTLE_ENDIAN, limit);
    }

    public BinaryDataInputStream(InputStream in, ByteOrder bo) {
        this(in, bo, Long.MAX_VALUE);
    }

    public BinaryDataInputStream(InputStream in, ByteOrder bo, long limit) {
        super(in);
        mByteOrder = bo;
        mLimit = limit;
        mMark = -1;
    }

    public ByteOrder order() {
        return mByteOrder;
    }

    public long limit() {
        return mLimit;
    }

    public long position() {
        return mPosition;
    }

    public long remaining() {
        return mLimit - mPosition;
    }

    public long jumpTo(long pos) throws IOException {
        long expected = pos - mPosition;
        if (expected == 0) {
            return 0;
        }
        if (expected < 0) {
            throw new IOException(String.format(
                "Illegal backwards jump from %d to %d", mPosition, pos));
        }
        long skipped = skip(expected);
        if (skipped != expected) {
            throw new IOException(String.format(
                "Jump failed: skipped %d bytes (expected: %d)", skipped, expected));
        }
        return skipped;
    }

    public void skipByte() throws IOException {
        //noinspection ResultOfMethodCallIgnored
        readByte();
    }

    public void skipShort() throws IOException {
        //noinspection ResultOfMethodCallIgnored
        readShort();
    }

    public void skipInt() throws IOException {
        //noinspection ResultOfMethodCallIgnored
        readInt();
    }

    public void skipLong() throws IOException {
        //noinspection ResultOfMethodCallIgnored
        readLong();
    }

    public byte[] readBytes(int len) throws IOException {
        byte[] buf = new byte[len];
        readFully(buf);
        return buf;
    }

    public short[] readShortArray(int len) throws IOException {
        short[] arr = new short[len];
        for (int i = 0; i < len; i++) {
            arr[i] = readShort();
        }
        return arr;
    }

    public int[] readIntArray(int len) throws IOException {
        int[] arr = new int[len];
        for (int i = 0; i < len; i++) {
            arr[i] = readInt();
        }
        return arr;
    }

    public long[] readLongArray(int len) throws IOException {
        long[] arr = new long[len];
        for (int i = 0; i < len; i++) {
            arr[i] = readLong();
        }
        return arr;
    }

    public String readAscii(int len) throws IOException {
        char[] buf = new char[len];
        int pos = 0;
        while (len-- > 0) {
            char ch = (char) readUnsignedByte();
            if (ch == 0) {
                break;
            }
            buf[pos++] = ch;
        }
        if (len > 0) {
            skipBytes(len);
        }
        return new String(buf, 0, pos);
    }

    public String readUtf16(int len) throws IOException {
        char[] buf = new char[len];
        int pos = 0;
        while (len-- > 0) {
            char ch = readChar();
            if (ch == 0) {
                break;
            }
            buf[pos++] = ch;
        }
        if (len > 0) {
            skipBytes(len * 2);
        }
        return new String(buf, 0, pos);
    }

    // DataInput

    @Override
    public void readFully(byte[] b) throws IOException {
        ByteStreams.readFully(this, b);
    }

    @Override
    public void readFully(byte[] b, int off, int len) throws IOException {
        ByteStreams.readFully(this, b, off, len);
    }

    @Override
    public int skipBytes(int n) throws IOException {
        return (int) skip(n);
    }

    @Override
    public boolean readBoolean() throws IOException {
        return readUnsignedByte() != 0;
    }

    @Override
    public byte readByte() throws IOException {
        return (byte) readUnsignedByte();
    }

    @Override
    public int readUnsignedByte() throws IOException {
        int b = read();
        if (b == -1) {
            throw new EOFException();
        }
        return b;
    }

    @Override
    public short readShort() throws IOException {
        return (short) readUnsignedShort();
    }

    @Override
    public int readUnsignedShort() throws IOException {
        byte b1 = readByte();
        byte b2 = readByte();
        return mByteOrder == ByteOrder.LITTLE_ENDIAN
            ? Ints.fromBytes((byte) 0, (byte) 0, b2, b1)
            : Ints.fromBytes(b1, b2, (byte) 0, (byte) 0);
    }

    @Override
    public char readChar() throws IOException {
        return (char) readUnsignedShort();
    }

    @Override
    public int readInt() throws IOException {
        byte b1 = readByte();
        byte b2 = readByte();
        byte b3 = readByte();
        byte b4 = readByte();
        return mByteOrder == ByteOrder.LITTLE_ENDIAN
            ? Ints.fromBytes(b4, b3, b2, b1)
            : Ints.fromBytes(b1, b2, b3, b4);
    }

    @Override
    public long readLong() throws IOException {
        byte b1 = readByte();
        byte b2 = readByte();
        byte b3 = readByte();
        byte b4 = readByte();
        byte b5 = readByte();
        byte b6 = readByte();
        byte b7 = readByte();
        byte b8 = readByte();
        return mByteOrder == ByteOrder.LITTLE_ENDIAN
            ? Longs.fromBytes(b8, b7, b6, b5, b4, b3, b2, b1)
            : Longs.fromBytes(b1, b2, b3, b4, b5, b6, b7, b8);
    }

    @Override
    public float readFloat() throws IOException {
        return Float.intBitsToFloat(readInt());
    }

    @Override
    public double readDouble() throws IOException {
        return Double.longBitsToDouble(readLong());
    }

    @Override
    public String readLine() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String readUTF() throws IOException {
        throw new UnsupportedOperationException();
    }

    // InputStream

    @Override
    public int read() throws IOException {
        if (remaining() == 0) {
            return -1;
        }
        int b = in.read();
        if (b != -1) {
            ++mPosition;
        }
        return b;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        long remain = remaining();
        if (remain == 0) {
            return -1;
        }
        if (len > remain) {
            len = (int) remain;
        }
        int read = in.read(b, off, len);
        if (read > 0) {
            mPosition += read;
        }
        return read;
    }

    @Override
    public long skip(long n) throws IOException {
        long remain = remaining();
        if (remain == 0) {
            return 0;
        }
        if (n > remain) {
            n = remain;
        }
        // For many reasons, skip() may end up skipping less bytes
        // than requested. Try harder.
        long skipped = 0;
        long s;
        while (skipped < n && (s = in.skip(n - skipped)) > 0) {
            skipped += s;
        }
        mPosition += skipped;
        return skipped;
    }

    @Override
    public int available() throws IOException {
        return (int) Math.min(in.available(), remaining());
    }

    @Override
    public synchronized void mark(int readlimit) {
        // We can't throw an exception here, so mark even if mark isn't
        // supported, since reset won't work anyway.
        in.mark(readlimit);
        mMark = mPosition;
    }

    @Override
    public synchronized void reset() throws IOException {
        if (!markSupported()) {
            throw new IOException("Mark not supported");
        }
        if (mMark == -1) {
            throw new IOException("Mark not set");
        }
        in.reset();
        mPosition = mMark;
    }
}
