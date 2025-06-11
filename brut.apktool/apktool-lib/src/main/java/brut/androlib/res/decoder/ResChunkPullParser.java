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
package brut.androlib.res.decoder;

import brut.androlib.res.decoder.data.ResChunkHeader;
import brut.util.BinaryDataInputStream;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteOrder;

public class ResChunkPullParser {
    private static final long OFFSET_ENDED = -1;

    private final BinaryDataInputStream mIn;
    private final long mOffset;
    private final int mSize;
    private long mChunkOffset;
    private ResChunkHeader mChunkHeader;

    public ResChunkPullParser(BinaryDataInputStream in) {
        this(in, Integer.MAX_VALUE);
    }

    public ResChunkPullParser(BinaryDataInputStream in, int size) {
        assert in.order() == ByteOrder.LITTLE_ENDIAN;
        mIn = in;
        mOffset = in.position();
        mSize = size;
    }

    public BinaryDataInputStream stream() {
        return mIn;
    }

    public boolean isChunk() {
        return mChunkHeader != null;
    }

    public long chunkStart() {
        if (mChunkHeader == null) {
            throw new IllegalStateException();
        }
        return mChunkOffset;
    }

    public int chunkType() {
        if (mChunkHeader == null) {
            throw new IllegalStateException();
        }
        return mChunkHeader.type;
    }

    public String chunkName() {
        if (mChunkHeader == null) {
            throw new IllegalStateException();
        }
        return ResChunkHeader.nameOf(mChunkHeader.type);
    }

    public int chunkSize() {
        if (mChunkHeader == null) {
            throw new IllegalStateException();
        }
        return mChunkHeader.size;
    }

    public long chunkEnd() {
        if (mChunkHeader == null) {
            throw new IllegalStateException();
        }
        return mChunkOffset + mChunkHeader.size;
    }

    public int headerSize() {
        if (mChunkHeader == null) {
            throw new IllegalStateException();
        }
        return mChunkHeader.headerSize;
    }

    public long headerEnd() {
        if (mChunkHeader == null) {
            throw new IllegalStateException();
        }
        return mChunkOffset + mChunkHeader.headerSize;
    }

    public int dataSize() {
        if (mChunkHeader == null) {
            throw new IllegalStateException();
        }
        return mChunkHeader.size - mChunkHeader.headerSize;
    }

    public boolean next() throws IOException {
        if (mChunkOffset == OFFSET_ENDED) {
            return false;
        }

        // Jump to the next chunk.
        if (mChunkHeader != null) {
            skipChunk();
            mChunkHeader = null;
        }

        if (mIn.position() >= mOffset + mSize) {
            // End of chunks due to size limit.
            mChunkOffset = OFFSET_ENDED;
            return false;
        }

        // Read the chunk header at the current position.
        try {
            mChunkOffset = mIn.position();
            ResChunkHeader chunkHeader = ResChunkHeader.read(mIn);

            if (chunkHeader.headerSize < ResChunkHeader.HEADER_SIZE
                    || chunkHeader.size < chunkHeader.headerSize) {
                throw new IOException(String.format(
                    "Invalid chunk header: type=0x%04x, headerSize=%d, size=%d",
                    chunkHeader.type, chunkHeader.headerSize, chunkHeader.size));
            }

            mChunkHeader = chunkHeader;
            return true;
        } catch (EOFException ignored) {
            // End of chunks due to end of stream.
            mChunkOffset = OFFSET_ENDED;
            return false;
        } catch (IOException ex) {
            throw new IOException("Error while reading chunk header.", ex);
        }
    }

    public int skipChunk() throws IOException {
        if (mChunkHeader == null) {
            throw new IllegalStateException();
        }
        try {
            long position = mIn.position();
            long chunkEnd = chunkEnd();
            if (position == chunkEnd) {
                return 0;
            }
            if (position > chunkEnd) {
                throw new IOException("Stream advanced past chunk end.");
            }
            return mIn.skipBytes((int) (chunkEnd - position));
        } catch (EOFException ignored) {
            throw new EOFException("Unexpected EOF while skipping chunk.");
        } catch (IOException ex) {
            throw new IOException("Error while skipping chunk.", ex);
        }
    }

    public int skipHeader() throws IOException {
        if (mChunkHeader == null) {
            throw new IllegalStateException();
        }
        try {
            long position = mIn.position();
            long headerEnd = headerEnd();
            if (position == headerEnd) {
                return 0;
            }
            if (position > headerEnd) {
                throw new IOException("Stream advanced past chunk header end.");
            }
            return mIn.skipBytes((int) (headerEnd - position));
        } catch (EOFException ignored) {
            throw new EOFException("Unexpected EOF while skipping chunk header.");
        } catch (IOException ex) {
            throw new IOException("Error while skipping chunk header.", ex);
        }
    }
}
