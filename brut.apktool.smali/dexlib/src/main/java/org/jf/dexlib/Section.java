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

import org.jf.dexlib.Util.AlignmentUtils;
import org.jf.dexlib.Util.AnnotatedOutput;
import org.jf.dexlib.Util.Input;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public abstract class Section<T extends Item> {
    /**
     * A list of the items that this section contains.
     * If the section has been placed, this list should be in the order that the items
     * will written to the dex file
     */
    protected final ArrayList<T> items;

    /**
     * A HashMap of the items in this section. This is used when interning items, to determine
     * if this section already has an item equivalent to the one that is being interned.
     * Both the key and the value should be the same object
     */
    protected HashMap<T,T> uniqueItems = null;

    /**
     * The offset of this section within the <code>DexFile</code>
     */
    protected int offset = 0;

    /**
     * The type of item that this section holds
     */
    public final ItemType ItemType;

    /**
     * The <code>DexFile</code> that this section belongs to
     */
    public final DexFile DexFile;

    /**
     * Create a new section
     * @param dexFile The <code>DexFile</code> that this section belongs to
     * @param itemType The itemType that this section will hold
     */
    protected Section(DexFile dexFile, ItemType itemType) {
        this.DexFile = dexFile;
        items = new ArrayList<T>();
        this.ItemType = itemType;
    }

    /**
     * Finalize the location of all items, and place them starting at the given offset
     * @param offset The offset where this section should be placed
     * @return the offset of the byte immediate after the last item in this section
     */
    protected int placeAt(int offset) {
        if (items.size() > 0) {
            offset = AlignmentUtils.alignOffset(offset, ItemType.ItemAlignment);
            assert !DexFile.getInplace() || offset == this.offset;
            this.offset = offset;

            for (int i=0; i < items.size(); i++) {
                T item = items.get(i);
                assert item != null;
                offset = AlignmentUtils.alignOffset(offset, ItemType.ItemAlignment);
                offset = item.placeAt(offset, i);
            }
        } else {
            this.offset = 0;
        }

        return offset;
    }

    /**
     * Write the items to the given <code>AnnotatedOutput</code>
     * @param out the <code>AnnotatedOutput</code> object to write to
     */
    protected void writeTo(AnnotatedOutput out) {
        out.annotate(0, " ");
        out.annotate(0, "-----------------------------");
        out.annotate(0, this.ItemType.TypeName + " section");
        out.annotate(0, "-----------------------------");
        out.annotate(0, " ");

        for (Item item: items) {
            assert item!=null;
            out.alignTo(ItemType.ItemAlignment);
            item.writeTo(out);
            out.annotate(0, " ");
        }
    }

    /**
     * Read the specified number of items from the given <code>Input</code> object
     * @param size The number of items to read
     * @param in The <code>Input</code> object to read from
     * @param readContext a <code>ReadContext</code> object to hold information that is
     * only needed while reading in a file
     */
    protected void readFrom(int size, Input in, ReadContext readContext) {
        //readItems() expects that the list will already be the correct size, so add null items
        //until we reach the specified size
        items.ensureCapacity(size);
        for (int i = items.size(); i < size; i++) {
            items.add(null);
        }

        in.alignTo(ItemType.ItemAlignment);
        offset = in.getCursor();

        //call the subclass's method that actually reads in the items
        readItems(in, readContext);
    }

    /**
     * This method in the concrete item subclass should read in all the items from the given <code>Input</code>
     * object, using any pre-created items as applicable (i.e. items that were created prior to reading in the
     * section, by other items requesting items from this section that they reference by index/offset)
     * @param in the <code>Input</code>
     * @param readContext a <code>ReadContext</code> object to hold information that is
     * only needed while reading in a file
     */
    protected abstract void readItems(Input in, ReadContext readContext);

    /**
     * Gets the offset where the first item in this section is placed
     * @return the ofset where the first item in this section is placed
     */
    public int getOffset() {
        return offset;
    }

    /**
     * Gets a the items contained in this section as a read-only list
     * @return A read-only <code>List</code> object containing the items in this section
     */
    public List<T> getItems() {
        return Collections.unmodifiableList(items);
    }

    /**
     * This method checks if an item that is equivalent to the given item has already been added. If found,
     * it returns that item. If not found, it adds the given item to this section and returns it.
     * @param item the item to intern
     * @return An item from this section that is equivalent to the given item. It may or may not be the same
     * as the item passed to this method.
     */
    protected T intern(T item) {
        if (item == null) {
            return null;
        }
        T internedItem = getInternedItem(item);
        if (internedItem == null) {
            uniqueItems.put(item, item);
            items.add(item);
            return item;
        }
        return internedItem;
    }

    /**
     * Returns the interned item that is equivalent to the given item, or null
     * @param item the item to check
     * @return the interned item that is equivalent to the given item, or null
     */
    protected T getInternedItem(T item) {
        if (uniqueItems == null) {
            buildInternedItemMap();
        }
        return uniqueItems.get(item);
    }

    /**
     * Builds the interned item map from the items that are in this section
     */
    private void buildInternedItemMap() {
        uniqueItems = new HashMap<T,T>();
        for (T item: items) {
            assert item != null;
            uniqueItems.put(item, item);
        }
    }

    /**
     * Sorts the items in the section
     */
    protected void sortSection() {
        Collections.sort(items);
    }
}