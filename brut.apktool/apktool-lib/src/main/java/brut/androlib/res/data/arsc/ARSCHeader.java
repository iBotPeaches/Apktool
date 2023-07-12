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
import org.apache.commons.io.input.CountingInputStream;

import java.io.EOFException;
import java.io.IOException;

public class ARSCHeader {
    public final short type;
    public final int headerSize;
    public final int chunkSize;
    public final int startPosition;
    public final int endPosition;

    public ARSCHeader(short type, int headerSize, int chunkSize, int headerStart) {
        this.type = type;
        this.headerSize = headerSize;
        this.chunkSize = chunkSize;
        this.startPosition = headerStart;
        this.endPosition = headerStart + chunkSize;
    }

    public static ARSCHeader read(ExtDataInput in, CountingInputStream countIn) throws IOException {
        short type;
        int start = countIn.getCount();
        try {
            type = in.readShort();
        } catch (EOFException ex) {
            return new ARSCHeader(RES_NONE_TYPE, 0, 0, countIn.getCount());
        }
        return new ARSCHeader(type, in.readShort(), in.readInt(), start);
    }

    public void skipChunk(ExtDataInput in) throws IOException {
        in.skipBytes(chunkSize - headerSize);
    }

    public final static short RES_NONE_TYPE = -1;
    public final static short RES_NULL_TYPE = 0x0000;
    public final static short RES_STRING_POOL_TYPE = 0x0001;
    public final static short RES_TABLE_TYPE = 0x0002;
    public final static short RES_XML_TYPE = 0x0003;

    // RES_TABLE_TYPE Chunks
    public final static short XML_TYPE_PACKAGE = 0x0200;
    public final static short XML_TYPE_TYPE = 0x0201;
    public final static short XML_TYPE_SPEC_TYPE = 0x0202;
    public final static short XML_TYPE_LIBRARY = 0x0203;
    public final static short XML_TYPE_OVERLAY = 0x0204;
    public final static short XML_TYPE_OVERLAY_POLICY = 0x0205;
    public final static short XML_TYPE_STAGED_ALIAS = 0x0206;

    // RES_XML_TYPE Chunks
    public final static short RES_XML_FIRST_CHUNK_TYPE = 0x0100;
    public final static short RES_XML_START_NAMESPACE_TYPE = 0x0100;
    public final static short RES_XML_END_NAMESPACE_TYPE = 0x0101;
    public final static short RES_XML_START_ELEMENT_TYPE = 0x0102;
    public final static short RES_XML_END_ELEMENT_TYPE = 0x0103;
    public final static short RES_XML_CDATA_TYPE = 0x0104;
    public final static short RES_XML_LAST_CHUNK_TYPE = 0x017f;
    public final static short RES_XML_RESOURCE_MAP_TYPE = 0x0180;
}
