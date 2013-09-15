package org.jf.dexlib2.writer.io;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

public class MemoryDataStore implements DexDataStore {
    private byte[] buf;

    public MemoryDataStore() {
        this(1024 * 1024);
    }

    public MemoryDataStore(int initialCapacity) {
        buf = new byte[initialCapacity];
    }

    public byte[] getData() {
        return buf;
    }

    @Nonnull @Override public OutputStream outputAt(final int offset) {
        return new OutputStream() {
            private int position = offset;
            @Override public void write(int b) throws IOException {
                growBufferIfNeeded(position);
                buf[position++] = (byte)b;
            }

            @Override public void write(byte[] b) throws IOException {
                growBufferIfNeeded(position + b.length);
                System.arraycopy(b, 0, buf, position, b.length);
                position += b.length;
            }

            @Override public void write(byte[] b, int off, int len) throws IOException {
                growBufferIfNeeded(position + len);
                System.arraycopy(b, off, buf, position, len);
                position += len;
            }
        };
    }

    private void growBufferIfNeeded(int index) {
        if (index < buf.length) {
            return;
        }
        buf = Arrays.copyOf(buf, (int)((index + 1) * 1.2));
    }

    @Nonnull @Override public InputStream readAt(final int offset) {
        return new InputStream() {
            private int position = offset;

            @Override public int read() throws IOException {
                if (position >= buf.length) {
                    return -1;
                }
                return buf[position++];
            }

            @Override public int read(byte[] b) throws IOException {
                int readLength = Math.min(b.length, buf.length - position);
                if (readLength <= 0) {
                    if (position >= buf.length) {
                        return -1;
                    }
                    return 0;
                }
                System.arraycopy(buf, position, b, 0, readLength);
                position += readLength;
                return readLength;
            }

            @Override public int read(byte[] b, int off, int len) throws IOException {
                int readLength = Math.min(len, buf.length - position);
                if (readLength <= 0) {
                    if (position >= buf.length) {
                        return -1;
                    }
                    return 0;
                }
                System.arraycopy(buf, position, b, 0, readLength);
                position += readLength;
                return readLength;
            }

            @Override public long skip(long n) throws IOException {
                int skipLength = (int)Math.min(n, buf.length - position);
                position += skipLength;
                return skipLength;
            }

            @Override public int available() throws IOException {
                return buf.length - position;
            }
        };
    }

    @Override public void close() throws IOException {
        // no-op
    }
}
