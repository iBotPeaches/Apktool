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
package brut.androlib.res.data.arsc;

import brut.util.ExtDataInput;

import java.io.EOFException;
import java.io.IOException;
import java.math.BigInteger;
import java.util.logging.Logger;

public class ARSCHeader {
    private static final Logger LOGGER = Logger.getLogger(ARSCHeader.class.getName());

    public static final short RES_NONE_TYPE = -1;
    public static final short RES_NULL_TYPE = 0x0000;
    public static final short RES_STRING_POOL_TYPE = 0x0001;
    public static final short RES_TABLE_TYPE = 0x0002;
    public static final short RES_XML_TYPE = 0x0003;

    // RES_TABLE_TYPE Chunks
    public static final short XML_TYPE_PACKAGE = 0x0200;
    public static final short XML_TYPE_TYPE = 0x0201;
    public static final short XML_TYPE_SPEC_TYPE = 0x0202;
    public static final short XML_TYPE_LIBRARY = 0x0203;
    public static final short XML_TYPE_OVERLAY = 0x0204;
    public static final short XML_TYPE_OVERLAY_POLICY = 0x0205;
    public static final short XML_TYPE_STAGED_ALIAS = 0x0206;

    // RES_XML_TYPE Chunks
    public static final short RES_XML_FIRST_CHUNK_TYPE = 0x0100;
    public static final short RES_XML_START_NAMESPACE_TYPE = 0x0100;
    public static final short RES_XML_END_NAMESPACE_TYPE = 0x0101;
    public static final short RES_XML_START_ELEMENT_TYPE = 0x0102;
    public static final short RES_XML_END_ELEMENT_TYPE = 0x0103;
    public static final short RES_XML_CDATA_TYPE = 0x0104;
    public static final short RES_XML_LAST_CHUNK_TYPE = 0x017f;
    public static final short RES_XML_RESOURCE_MAP_TYPE = 0x0180;

    public final short type;
    public final int headerSize;
    public final int chunkSize;
    public final long startPosition;
    public final long endPosition;

    public ARSCHeader(short type, int headerSize, int chunkSize, long headerStart) {
        this.type = type;
        this.headerSize = headerSize;
        this.chunkSize = chunkSize;
        this.startPosition = headerStart;
        this.endPosition = headerStart + chunkSize;
    }

    public static ARSCHeader read(ExtDataInput in) throws IOException {
        short type;
        long start = in.position();
        try {
            type = in.readShort();
        } catch (EOFException ex) {
            return new ARSCHeader(RES_NONE_TYPE, 0, 0, in.position());
        }
        return new ARSCHeader(type, in.readShort(), in.readInt(), start);
    }

    public void checkForUnreadHeader(ExtDataInput in) throws IOException {
        // Some applications lie about the reported size of their chunk header. Trusting the chunkSize is misleading
        // So compare to what we actually read in the header vs reported and skip the rest.
        // However, this runs after each chunk and not every chunk reading has a specific distinction between the
        // header and the body.
        int actualHeaderSize = (int) (in.position() - startPosition);
        int exceedingSize = headerSize - actualHeaderSize;
        if (exceedingSize > 0) {
            byte[] buf = new byte[exceedingSize];
            in.readFully(buf);
            BigInteger exceedingBI = new BigInteger(1, buf);

            if (exceedingBI.equals(BigInteger.ZERO)) {
                LOGGER.fine(String.format("Chunk header size (%d), read (%d), but exceeding bytes are all zero.",
                    headerSize, actualHeaderSize
                ));
            } else {
                LOGGER.warning(String.format("Chunk header size (%d), read (%d). Exceeding bytes: 0x%X.",
                    headerSize, actualHeaderSize, exceedingBI
                ));
            }
        }
    }

    public void skipChunk(ExtDataInput in) throws IOException {
        in.skipBytes(chunkSize - headerSize);
    }
}
