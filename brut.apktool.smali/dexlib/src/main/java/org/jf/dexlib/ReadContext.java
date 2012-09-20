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

import org.jf.dexlib.Util.ExceptionWithContext;
import org.jf.dexlib.Util.SparseArray;

import java.util.List;


/**
 * This class stores context information that is only needed when reading in a dex file
 * Namely, it handles "pre-creating" items when an item needs to resolve some other item
 * that it references, and keeps track of those pre-created items, so the corresponding section
 * for the pre-created items uses them, instead of creating new items
 */
public class ReadContext {
    private SparseArray<TypeListItem> typeListItems = new SparseArray<TypeListItem>(0);
    private SparseArray<AnnotationSetRefList> annotationSetRefLists = new SparseArray<AnnotationSetRefList>(0);
    private SparseArray<AnnotationSetItem> annotationSetItems = new SparseArray<AnnotationSetItem>(0);
    private SparseArray<ClassDataItem> classDataItems = new SparseArray<ClassDataItem>(0);
    private SparseArray<CodeItem> codeItems = new SparseArray<CodeItem>(0);
    private SparseArray<StringDataItem> stringDataItems = new SparseArray<StringDataItem>(0);
    private SparseArray<DebugInfoItem> debugInfoItems = new SparseArray<DebugInfoItem>(0);
    private SparseArray<AnnotationItem> annotationItems = new SparseArray<AnnotationItem>(0);
    private SparseArray<EncodedArrayItem> encodedArrayItems = new SparseArray<EncodedArrayItem>(0);
    private SparseArray<AnnotationDirectoryItem> annotationDirectoryItems = new SparseArray<AnnotationDirectoryItem>(0);

    private SparseArray[] itemsByType = new SparseArray[] {
            null, //string_id_item
            null, //type_id_item
            null, //proto_id_item
            null, //field_id_item
            null, //method_id_item
            null, //class_def_item
            typeListItems,
            annotationSetRefLists,
            annotationSetItems,
            classDataItems,
            codeItems,
            stringDataItems,
            debugInfoItems,
            annotationItems,
            encodedArrayItems,
            annotationDirectoryItems,
            null, //map_list
            null //header_item
    };


    /**
     * The section sizes that are passed in while reading HeaderItem/MapItem, via the
     * addSection method.
     */
    private int[] sectionSizes = new int[18];

    /**
     * The section offsets that are passed in while reading MapItem/HeaderItem, via the
     * addSection method.
     */
    private int[] sectionOffsets = new int[18];

    /**
     * Creates a new ReadContext instance.
     */
    public ReadContext() {
        for (int i=0; i<18; i++) {
            sectionSizes[i] = -1;
            sectionOffsets[i] = -1;
        }
    }

    /**
     * Gets the offsetted item of the specified type for the given offset. This method does not support retrieving an
     * optional item where a value of 0 indicates "not present". Use getOptionalOffsettedItemByOffset instead.
     *
     * @param itemType The type of item to get
     * @param offset The offset of the item
     * @return the offsetted item of the specified type at the specified offset
     */
    public Item getOffsettedItemByOffset(ItemType itemType, int offset) {
        assert !itemType.isIndexedItem();

        SparseArray<Item> sa = itemsByType[itemType.SectionIndex];
        Item item = sa.get(offset);
        if (item == null) {
            throw new ExceptionWithContext(String.format("Could not find the %s item at offset %#x",
                    itemType.TypeName, offset));
        }
        return item;
    }

    /**
     * Gets the optional offsetted item of the specified type for the given offset
     *
     * @param itemType The type of item to get
     * @param offset The offset of the item
     * @return the offsetted item of the specified type at the specified offset, or null if the offset is 0
     */
    public Item getOptionalOffsettedItemByOffset(ItemType itemType, int offset) {
        assert !itemType.isIndexedItem();

        assert !itemType.isIndexedItem();

        SparseArray<Item> sa = itemsByType[itemType.SectionIndex];
        Item item = sa.get(offset);
        if (item == null && offset != 0) {
            throw new ExceptionWithContext(String.format("Could not find the %s item at offset %#x",
                    itemType.TypeName, offset));
        }
        return item;
    }

    /**
     * Adds the size and offset information for the given offset
     * @param itemType the item type of the section
     * @param sectionSize the size of the section
     * @param sectionOffset the offset of the section
     */
    public void addSection(final ItemType itemType, int sectionSize, int sectionOffset) {
        int storedSectionSize = sectionSizes[itemType.SectionIndex];
        if (storedSectionSize == -1) {
            sectionSizes[itemType.SectionIndex] = sectionSize;
        } else {
            if (storedSectionSize  != sectionSize) {
                throw new RuntimeException("The section size in the header and map for item type "
                        + itemType + " do not match");
            }
        }

        int storedSectionOffset = sectionOffsets[itemType.SectionIndex];
        if (storedSectionOffset == -1) {
            sectionOffsets[itemType.SectionIndex] = sectionOffset;
        } else {
            if (storedSectionOffset != sectionOffset) {
                throw new RuntimeException("The section offset in the header and map for item type "
                        + itemType + " do not match");
            }
        }
    }

    /**
     * Sets the items for the specified section. This should be called by an offsetted section
     * after it is finished reading in all its items.
     * @param itemType the item type of the section. This must be an offsetted item type
     * @param items the full list of items in the section, ordered by offset
     */
    public void setItemsForSection(ItemType itemType, List<? extends Item> items) {
        assert !itemType.isIndexedItem();

        SparseArray<Item> sa = itemsByType[itemType.SectionIndex];

        sa.ensureCapacity(items.size());
        for (Item item: items) {
            sa.append(item.getOffset(), item);
        }
    }

    /**
     * @param itemType the item type of the section
     * @return the size of the given section as it was read in from the map item
     */
    public int getSectionSize(ItemType itemType) {
        return sectionSizes[itemType.SectionIndex];
    }

    /**
     * @param itemType the item type of the section
     * @return the offset of the given section as it was read in from the map item
     */
    public int getSectionOffset(ItemType itemType) {
        return sectionOffsets[itemType.SectionIndex];
    }
}
