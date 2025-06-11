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
package brut.androlib.res.decoder.data;

import brut.util.BinaryDataInputStream;

import java.io.IOException;

public final class ResChunkHeader {
    public static final int HEADER_SIZE = 8;

    public static final int RES_NULL_TYPE = 0x0000;
    public static final int RES_STRING_POOL_TYPE = 0x0001;
    public static final int RES_TABLE_TYPE = 0x0002;
    public static final int RES_XML_TYPE = 0x0003;

    // Chunk types in RES_XML_TYPE
    public static final int RES_XML_FIRST_CHUNK_TYPE = 0x0100;
    public static final int RES_XML_START_NAMESPACE_TYPE = 0x0100;
    public static final int RES_XML_END_NAMESPACE_TYPE = 0x0101;
    public static final int RES_XML_START_ELEMENT_TYPE = 0x0102;
    public static final int RES_XML_END_ELEMENT_TYPE = 0x0103;
    public static final int RES_XML_CDATA_TYPE = 0x0104;
    public static final int RES_XML_LAST_CHUNK_TYPE = 0x017F;
    public static final int RES_XML_RESOURCE_MAP_TYPE = 0x0180;

    // Chunk types in RES_TABLE_TYPE
    public static final int RES_TABLE_PACKAGE_TYPE = 0x0200;
    public static final int RES_TABLE_TYPE_TYPE = 0x0201;
    public static final int RES_TABLE_TYPE_SPEC_TYPE = 0x0202;
    public static final int RES_TABLE_LIBRARY_TYPE = 0x0203;
    public static final int RES_TABLE_OVERLAYABLE_TYPE = 0x0204;
    public static final int RES_TABLE_OVERLAYABLE_POLICY_TYPE = 0x0205;
    public static final int RES_TABLE_STAGED_ALIAS_TYPE = 0x0206;

    public final int type;
    public final int headerSize;
    public final int size;

    public ResChunkHeader(int type, int headerSize, int size) {
        this.type = type;
        this.headerSize = headerSize;
        this.size = size;
    }

    public static ResChunkHeader read(BinaryDataInputStream in) throws IOException {
        int type = in.readUnsignedShort();
        int headerSize = in.readUnsignedShort();
        int size = in.readInt();

        return new ResChunkHeader(type, headerSize, size);
    }

    public static String nameOf(int type) {
        switch (type) {
            case RES_NULL_TYPE:
                return "RES_NULL_TYPE";
            case RES_STRING_POOL_TYPE:
                return "RES_STRING_POOL_TYPE";
            case RES_TABLE_TYPE:
                return "RES_TABLE_TYPE";
            case RES_XML_TYPE:
                return "RES_XML_TYPE";
            case RES_XML_START_NAMESPACE_TYPE:
                return "RES_XML_START_NAMESPACE_TYPE";
            case RES_XML_END_NAMESPACE_TYPE:
                return "RES_XML_END_NAMESPACE_TYPE";
            case RES_XML_START_ELEMENT_TYPE:
                return "RES_XML_START_ELEMENT_TYPE";
            case RES_XML_END_ELEMENT_TYPE:
                return "RES_XML_END_ELEMENT_TYPE";
            case RES_XML_CDATA_TYPE:
                return "RES_XML_CDATA_TYPE";
            case RES_XML_RESOURCE_MAP_TYPE:
                return "RES_XML_RESOURCE_MAP_TYPE";
            case RES_TABLE_PACKAGE_TYPE:
                return "RES_TABLE_PACKAGE_TYPE";
            case RES_TABLE_TYPE_TYPE:
                return "RES_TABLE_TYPE_TYPE";
            case RES_TABLE_TYPE_SPEC_TYPE:
                return "RES_TABLE_TYPE_SPEC_TYPE";
            case RES_TABLE_LIBRARY_TYPE:
                return "RES_TABLE_LIBRARY_TYPE";
            case RES_TABLE_OVERLAYABLE_TYPE:
                return "RES_TABLE_OVERLAYABLE_TYPE";
            case RES_TABLE_OVERLAYABLE_POLICY_TYPE:
                return "RES_TABLE_OVERLAYABLE_POLICY_TYPE";
            case RES_TABLE_STAGED_ALIAS_TYPE:
                return "RES_TABLE_STAGED_ALIAS_TYPE";
            default:
                return String.format("0x%04x", type);
        }
    }
}
