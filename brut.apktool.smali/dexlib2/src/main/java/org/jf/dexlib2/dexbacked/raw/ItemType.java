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

package org.jf.dexlib2.dexbacked.raw;

import javax.annotation.Nonnull;

public class ItemType {
    public static final int HEADER_ITEM = 0x0000;
    public static final int STRING_ID_ITEM = 0x0001;
    public static final int TYPE_ID_ITEM = 0x0002;
    public static final int PROTO_ID_ITEM = 0x0003;
    public static final int FIELD_ID_ITEM = 0x0004;
    public static final int METHOD_ID_ITEM = 0x0005;
    public static final int CLASS_DEF_ITEM = 0x0006;
    public static final int MAP_LIST = 0x1000;
    public static final int TYPE_LIST = 0x1001;
    public static final int ANNOTATION_SET_REF_LIST = 0x1002;
    public static final int ANNOTATION_SET_ITEM = 0x1003;
    public static final int CLASS_DATA_ITEM = 0x2000;
    public static final int CODE_ITEM = 0x2001;
    public static final int STRING_DATA_ITEM = 0x2002;
    public static final int DEBUG_INFO_ITEM = 0x2003;
    public static final int ANNOTATION_ITEM = 0x2004;
    public static final int ENCODED_ARRAY_ITEM = 0x2005;
    public static final int ANNOTATION_DIRECTORY_ITEM = 0x2006;

    @Nonnull
    public static String getItemTypeName(int itemType) {
        switch (itemType) {
            case HEADER_ITEM: return "header_item";
            case STRING_ID_ITEM: return "string_id_item";
            case TYPE_ID_ITEM: return "type_id_item";
            case PROTO_ID_ITEM: return "proto_id_item";
            case FIELD_ID_ITEM: return "field_id_item";
            case METHOD_ID_ITEM: return "method_id_item";
            case CLASS_DEF_ITEM: return "class_def_item";
            case MAP_LIST: return "map_list";
            case TYPE_LIST: return "type_list";
            case ANNOTATION_SET_REF_LIST: return "annotation_set_ref_list";
            case ANNOTATION_SET_ITEM: return "annotation_set_item";
            case CLASS_DATA_ITEM: return "class_data_item";
            case CODE_ITEM: return "code_item";
            case STRING_DATA_ITEM: return "string_data_item";
            case DEBUG_INFO_ITEM: return "debug_info_item";
            case ANNOTATION_ITEM: return "annotation_item";
            case ENCODED_ARRAY_ITEM: return "encoded_array_item";
            case ANNOTATION_DIRECTORY_ITEM: return "annotation_directory_item";
            default: return "unknown dex item type";
        }
    }
}
