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
import org.jf.dexlib.Util.Input;

public class IndexedSection<T extends Item> extends Section<T> {

    /**
     * Create a new indexed section
     * @param dexFile The <code>DexFile</code> that this section belongs to
     * @param itemType The itemType that this section will hold
     */
    public IndexedSection(DexFile dexFile, ItemType itemType) {
        super(dexFile, itemType);
    }

    /** {@inheritDoc} */
    protected void readItems(Input in, ReadContext readContext) {
        for (int i = 0; i < items.size(); i++) {
            T item = (T)ItemFactory.makeItem(ItemType, DexFile);
            items.set(i, item);
            item.readFrom(in, i, readContext);
        }
    }

    /**
     * Gets the item at the specified index in this section, or null if the index is -1
     * @param index the index of the item to get
     * @return the item at the specified index in this section, or null if the index is -1
     */
    public T getOptionalItemByIndex(int index) {
        if (index == -1) {
            return null;
        }

        return getItemByIndex(index);
    }

    /**
     * Gets the item at the specified index in this section
     * @param index the index of the item to get
     * @return the item at the specified index in this section
     */
    public T getItemByIndex(int index) {
        try {
            //if index is out of bounds, just let it throw an exception
            return items.get(index);
        } catch (Exception ex) {
            throw ExceptionWithContext.withContext(ex, "Error occured while retrieving the " + this.ItemType.TypeName +
                    " item at index " + index);
        }
    }
}