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

import com.google.common.io.CountingInputStream;
import com.google.common.io.LittleEndianDataInputStream;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.FilterInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.logging.Logger;

public class ExtDataInputStream extends FilterInputStream implements ExtDataInput {
    private static final Logger LOGGER = Logger.getLogger(ExtDataInputStream.class.getName());

    private final DataInput mDelegate;
    private final CountingInputStream mCountIn;

    public static ExtDataInputStream bigEndian(InputStream in) {
        CountingInputStream countIn = new CountingInputStream(in);
        DataInput delegate = new DataInputStream(countIn);
        return new ExtDataInputStream(delegate, countIn);
    }

    public static ExtDataInputStream littleEndian(InputStream in) {
        CountingInputStream countIn = new CountingInputStream(in);
        DataInput delegate = new LittleEndianDataInputStream(countIn);
        return new ExtDataInputStream(delegate, countIn);
    }

    private ExtDataInputStream(DataInput delegate, CountingInputStream countIn) {
        super((InputStream) delegate);
        mDelegate = delegate;
        mCountIn = countIn;
    }

    public void jumpTo(long expectedPosition) throws IOException {
        long position = this.position();
        if (position > expectedPosition) {
            throw new IOException(String.format("Jumping backwards from %d to %d", position, expectedPosition));
        }
        if (position < expectedPosition) {
            long skipped = skip(expectedPosition - position);
            if (skipped != expectedPosition - position) {
                throw new IOException(String.format("Jump failed: expected %d, got %d", expectedPosition - position, skipped));
            }
        }
    }

    // ExtDataInput

    @Override
    public long position() {
        return mCountIn.getCount();
    }

    @Override
    public void skipShort() throws IOException {
        //noinspection ResultOfMethodCallIgnored
        readShort();
    }

    @Override
    public void skipInt() throws IOException {
        //noinspection ResultOfMethodCallIgnored
        readInt();
    }

    @Override
    public void skipCheckShort(short expected) throws IOException {
        short got = readShort();
        if (got != expected) {
            throw new IOException(String.format("Expected: 0x%08x, got: 0x%08x", expected, got));
        }
    }

    @Override
    public void skipCheckByte(byte expected) throws IOException {
        byte got = readByte();
        if (got != expected) {
            throw new IOException(String.format("Expected: 0x%08x, got: 0x%08x", expected, got));
        }
    }

    @Override
    public int[] readIntArray(int len) throws IOException {
        int[] arr = new int[len];
        for (int i = 0; i < len; i++) {
            arr[i] = readInt();
        }
        return arr;
    }

    @Override
    public int[] readSafeIntArray(int len, long maxPosition) throws IOException {
        int[] arr = new int[len];
        for (int i = 0; i < len; i++) {
            // #3236 - In some applications we have more strings than fit into the block. This function takes
            // an expected max position and if we are past it, we return early during processing.
            if (position() >= maxPosition) {
                LOGGER.warning(String.format("Bad string block: string entry is at %d, past end at %d",
                    position(), maxPosition));
                return arr;
            }

            arr[i] = readInt();
        }
        return arr;
    }

    @Override
    public String readNullEndedString(int len, boolean fixed) throws IOException {
        StringBuilder str = new StringBuilder(16);
        while (len-- != 0) {
            short ch = readShort();
            if (ch == 0) {
                break;
            }
            str.append((char) ch);
        }
        if (fixed) {
            skipBytes(len * 2);
        }
        return str.toString();
    }

    // DataInput

    @Override
    public void readFully(byte[] b) throws IOException {
        mDelegate.readFully(b);
    }

    @Override
    public void readFully(byte[] b, int off, int len) throws IOException {
        mDelegate.readFully(b, off, len);
    }

    /**
     * The general contract of DataInput doesn't guarantee all the bytes requested will be skipped
     * and failure can occur for many reasons. We override this to try harder to skip all the bytes
     * requested (this is similar to DataInputStream's wrapper).
     */
    @Override
    public int skipBytes(int n) throws IOException {
        int total = 0;
        int cur;
        while (total < n && (cur = mDelegate.skipBytes(n - total)) > 0) {
            total += cur;
        }
        return total;
    }

    @Override
    public boolean readBoolean() throws IOException {
        return mDelegate.readBoolean();
    }

    @Override
    public byte readByte() throws IOException {
        return mDelegate.readByte();
    }

    @Override
    public int readUnsignedByte() throws IOException {
        return mDelegate.readUnsignedByte();
    }

    @Override
    public short readShort() throws IOException {
        return mDelegate.readShort();
    }

    @Override
    public int readUnsignedShort() throws IOException {
        return mDelegate.readUnsignedShort();
    }

    @Override
    public char readChar() throws IOException {
        return mDelegate.readChar();
    }

    @Override
    public int readInt() throws IOException {
        return mDelegate.readInt();
    }

    @Override
    public long readLong() throws IOException {
        return mDelegate.readLong();
    }

    @Override
    public float readFloat() throws IOException {
        return mDelegate.readFloat();
    }

    @Override
    public double readDouble() throws IOException {
        return mDelegate.readDouble();
    }

    @Override
    public String readLine() throws IOException {
        return mDelegate.readLine();
    }

    @Override
    public String readUTF() throws IOException {
        return mDelegate.readUTF();
    }
}
