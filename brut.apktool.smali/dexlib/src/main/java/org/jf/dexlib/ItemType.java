/*
 * [The "BSD licence"]
 * Copyright (c) 2010 Ben Gruver (JesusFreke)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.jf.dexlib;

import java.util.TreeMap;

/**
 * Enumeration of all the top-level item types.
 */
public enum ItemType {
    TYPE_HEADER_ITEM(               0x0000, 17, 4, "header_item"),
    TYPE_STRING_ID_ITEM(            0x0001, 0,  4, "string_id_item"),
    TYPE_TYPE_ID_ITEM(              0x0002, 1,  4, "type_id_item"),
    TYPE_PROTO_ID_ITEM(             0x0003, 2,  4, "proto_id_item"),
    TYPE_FIELD_ID_ITEM(             0x0004, 3,  4, "field_id_item"),
    TYPE_METHOD_ID_ITEM(            0x0005, 4,  4, "method_id_item"),
    TYPE_CLASS_DEF_ITEM(            0x0006, 5,  4, "class_def_item"),
    TYPE_MAP_LIST(                  0x1000, 16, 4, "map_list"),
    TYPE_TYPE_LIST(                 0x1001, 6,  4, "type_list"),
    TYPE_ANNOTATION_SET_REF_LIST(   0x1002, 7,  4, "annotation_set_ref_list"),
    TYPE_ANNOTATION_SET_ITEM(       0x1003, 8,  4, "annotation_set_item"),
    TYPE_CLASS_DATA_ITEM(           0x2000, 9,  1, "class_data_item"),
    TYPE_CODE_ITEM(                 0x2001, 10, 4, "code_item"),
    TYPE_STRING_DATA_ITEM(          0x2002, 11, 1, "string_data_item"),
    TYPE_DEBUG_INFO_ITEM(           0x2003, 12, 1, "debug_info_item"),
    TYPE_ANNOTATION_ITEM(           0x2004, 13, 1, "annotation_item"),
    TYPE_ENCODED_ARRAY_ITEM(        0x2005, 14, 1, "encoded_array_item"),
    TYPE_ANNOTATIONS_DIRECTORY_ITEM(0x2006, 15, 4, "annotations_directory_item");

    /** A map to facilitate looking up an <code>ItemType</code> by ordinal */
    private final static TreeMap<Integer, ItemType> itemTypeIntegerMap;

    /** builds the <code>itemTypeIntegerMap</code> object */
    static {
	    itemTypeIntegerMap = new TreeMap<Integer, ItemType>();

        for (ItemType itemType: ItemType.values()) {
            itemTypeIntegerMap.put(itemType.MapValue, itemType);
        }
    }

    

    /**
     * value when represented in a MapItem
     */
    public final int MapValue;

    /**
     * name of the type
     */
    public final String TypeName;

    /**
     * index for this item's section
     */
    public final int SectionIndex;

    /**
     * the alignment for this item type
     */
    public final int ItemAlignment;
    /**
     * Constructs an instance.
     *
     * @param mapValue value when represented in a MapItem
     * @param sectionIndex index for this item's section
     * @param itemAlignment the byte alignment required by this item 
     * @param typeName non-null; name of the type
     */
    private ItemType(int mapValue, int sectionIndex, int itemAlignment, String typeName) {
        this.MapValue = mapValue;
        this.SectionIndex = sectionIndex;
        this.ItemAlignment = itemAlignment;
        this.TypeName = typeName;
    }

    /**
     * Converts an int value to the corresponding ItemType enum value,
     * or null if the value isn't a valid ItemType value
     *
     * @param itemType the int value to convert to an ItemType
     * @return the ItemType enum value corresponding to itemType, or null
     * if not a valid ItemType value
     */
    public static ItemType fromInt(int itemType) {
        return itemTypeIntegerMap.get(itemType);
    }

    /**
     * Returns true if this is an indexed item, or false if its an offsetted item
     * @return true if this is an indexed item, or false if its an offsetted item
     */
    public boolean isIndexedItem() {
        return MapValue <= 0x1000;
    }
}