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

import org.jf.dexlib.Util.AnnotatedOutput;
import org.jf.dexlib.Util.Input;
import org.jf.util.StringUtils;

import javax.annotation.Nullable;

public class StringIdItem extends Item<StringIdItem> {
    private StringDataItem stringDataItem;

   /**
     * Creates a new uninitialized <code>StringIdItem</code>
     * @param dexFile The <code>DexFile</code> that this item belongs to
     */
    protected StringIdItem(DexFile dexFile) {
        super(dexFile);
    }

    /**
     * Creates a new <code>StringIdItem</code> for the given <code>StringDataItem</code>
     * @param dexFile The <code>DexFile</code> that this item belongs to
     * @param stringDataItem The <code>StringDataItem</code> that this <code>StringIdItem</code> represents
     */
    protected StringIdItem(DexFile dexFile, StringDataItem stringDataItem) {
        super(dexFile);
        this.stringDataItem = stringDataItem;
    }

    /**
     * Returns a <code>StringIdItem</code> for the given values, and that has been interned into
     * the given <code>DexFile</code>
     * @param dexFile The <code>DexFile</code> that this item will belong to
     * @param stringValue The string value that this item represents
     * @return a <code>StringIdItem</code> for the given values, and that has been interned into
     * the given <code>DexFile</code>
     */
    public static StringIdItem internStringIdItem(DexFile dexFile, String stringValue) {
        StringDataItem stringDataItem = StringDataItem.internStringDataItem(dexFile, stringValue);
        if (stringDataItem == null) {
            return null;
        }
        StringIdItem stringIdItem = new StringIdItem(dexFile, stringDataItem);
        return dexFile.StringIdsSection.intern(stringIdItem);
    }

    /**
     * Looks up the <code>StringIdItem</code> from the given <code>DexFile</code> for the given
     * string value
     * @param dexFile the <code>Dexfile</code> to find the string value in
     * @param stringValue The string value to look up
     * @return a <code>StringIdItem</code> from the given <code>DexFile</code> for the given
     * string value, or null if it doesn't exist
     */
    public static StringIdItem lookupStringIdItem(DexFile dexFile, String stringValue) {
        StringDataItem stringDataItem = StringDataItem.lookupStringDataItem(dexFile, stringValue);
        if (stringDataItem == null) {
            return null;
        }
        StringIdItem stringIdItem = new StringIdItem(dexFile, stringDataItem);
        return dexFile.StringIdsSection.getInternedItem(stringIdItem);
    }

    /** {@inheritDoc} */
    protected void readItem(Input in, ReadContext readContext) {
        int stringDataOffset = in.readInt();

        stringDataItem = (StringDataItem)readContext.getOffsettedItemByOffset(ItemType.TYPE_STRING_DATA_ITEM,
                stringDataOffset);
    }

    /** {@inheritDoc} */
    protected int placeItem(int offset) {
        return offset + 4;
    }

    /** {@inheritDoc} */
    protected void writeItem(AnnotatedOutput out) {
        if (out.annotates()) {
            out.annotate(4, stringDataItem.getConciseIdentity());
        }

        out.writeInt(stringDataItem.getOffset());
    }

    /** {@inheritDoc} */
    public ItemType getItemType() {
        return ItemType.TYPE_STRING_ID_ITEM;
    }

    /** {@inheritDoc} */
    public String getConciseIdentity() {
        return "string_id_item: " + StringUtils.escapeString(getStringValue());
    }

    /** {@inheritDoc} */
    public int compareTo(StringIdItem o) {
        //sort by the string value
        return getStringValue().compareTo(o.getStringValue());
    }

    /**
     * Get the <code>String</code> value that this <code>StringIdItem</code> represents
     * @return the <code>String</code> value that this <code>StringIdItem</code> represents
     */
    public String getStringValue() {
        return stringDataItem.getStringValue();
    }

    /**
     * Get the <code>String</code> value that the given <code>StringIdItem</code> represents
     * @param stringIdItem The <code>StringIdItem</code> to get the string value of
     * @return the <code>String</code> value that the given <code>StringIdItem</code> represents
     */
    @Nullable
    public static String getStringValue(@Nullable StringIdItem stringIdItem) {
        return stringIdItem==null?null:stringIdItem.getStringValue();
    }

    /**
     * Get the <code>StringDataItem</code> that this <code>StringIdItem</code> references
     * @return the <code>StringDataItem</code> that this <code>StringIdItem</code> references
     */
    public StringDataItem getStringDataItem() {
        return stringDataItem;
    }

    @Override
    public int hashCode() {
        return stringDataItem.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this==o) {
            return true;
        }
        if (o==null || !this.getClass().equals(o.getClass())) {
            return false;
        }

        //This assumes that the referenced items have been interned in both objects.
        //This is a valid assumption because all outside code must use the static
        //"getInterned..." style methods to make new items, and any item created
        //internally is guaranteed to be interned
        StringIdItem other = (StringIdItem)o;
        return stringDataItem == other.stringDataItem;
    }
}
