package org.jf.dexlib2.writer.io;

import com.google.common.collect.Lists;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 * A deferred output stream that is stored in memory
 */
public class MemoryDeferredOutputStream extends DeferredOutputStream {
    private static final int DEFAULT_BUFFER_SIZE = 16 * 1024;

    private final List<byte[]> buffers = Lists.newArrayList();
    private byte[] currentBuffer;
    private int currentPosition;

    public MemoryDeferredOutputStream() {
        this(DEFAULT_BUFFER_SIZE);
    }

    public MemoryDeferredOutputStream(int bufferSize) {
        currentBuffer = new byte[bufferSize];
    }

    @Override public void writeTo(OutputStream output) throws IOException {
        for (byte[] buffer: buffers) {
            output.write(buffer);
        }
        if (currentPosition > 0) {
            output.write(currentBuffer, 0, currentPosition);
        }
        buffers.clear();
        currentPosition = 0;
    }

    @Override public void write(int i) throws IOException {
        if (remaining() == 0) {
            buffers.add(currentBuffer);
            currentBuffer = new byte[currentBuffer.length];
            currentPosition = 0;
        }
        currentBuffer[currentPosition++] = (byte)i;
    }

    @Override public void write(byte[] bytes) throws IOException {
        write(bytes, 0, bytes.length);
    }

    @Override public void write(byte[] bytes, int offset, int length) throws IOException {
        int remaining = remaining();
        int written = 0;
        while (length - written > 0) {
            int toWrite = Math.min(remaining, (length - written));
            System.arraycopy(bytes, offset + written, currentBuffer, currentPosition, toWrite);
            written += toWrite;
            currentPosition += toWrite;

            remaining = remaining();
            if (remaining == 0) {
                buffers.add(currentBuffer);
                currentBuffer = new byte[currentBuffer.length];
                currentPosition = 0;
                remaining = currentBuffer.length;
            }
        }
    }

    private int remaining() {
        return currentBuffer.length - currentPosition;
    }

    @Nonnull
    public static DeferredOutputStreamFactory getFactory() {
        return getFactory(DEFAULT_BUFFER_SIZE);
    }

    @Nonnull
    public static DeferredOutputStreamFactory getFactory(final int bufferSize) {
        return new DeferredOutputStreamFactory() {
            @Override public DeferredOutputStream makeDeferredOutputStream() {
                return new MemoryDeferredOutputStream(bufferSize);
            }
        };
    }
}
