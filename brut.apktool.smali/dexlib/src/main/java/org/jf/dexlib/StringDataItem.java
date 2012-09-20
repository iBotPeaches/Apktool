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
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.jf.dexlib;

import org.jf.dexlib.Util.AnnotatedOutput;
import org.jf.dexlib.Util.Input;
import org.jf.dexlib.Util.Leb128Utils;
import org.jf.dexlib.Util.Utf8Utils;

public class StringDataItem extends Item<StringDataItem> {
    private int hashCode = 0;

    private String stringValue;

    /**
     * Creates a new uninitialized <code>StringDataItem</code>
     * @param dexFile The <code>DexFile</code> that this item belongs to
     */
    protected StringDataItem(DexFile dexFile) {
        super(dexFile);
    }

    /**
     * Creates a new <code>StringDataItem</code> for the given string
     * @param dexFile The <code>DexFile</code> that this item belongs to
     * @param stringValue The string value that this item represents
     */
    private StringDataItem(DexFile dexFile, String stringValue) {
        super(dexFile);

        this.stringValue = stringValue;
    }

    /**
     * Returns a <code>StringDataItem</code> for the given values, and that has been interned into
     * the given <code>DexFile</code>
     * @param dexFile The <code>DexFile</code> that this item belongs to
     * @param value The string value that this item represents
     * @return a <code>StringDataItem</code> for the given values, and that has been interned into
     * the given <code>DexFile</code>
     */
    public static StringDataItem internStringDataItem(DexFile dexFile, String value) {
        StringDataItem StringDataItem = new StringDataItem(dexFile, value);
        return dexFile.StringDataSection.intern(StringDataItem);
    }

    /**
     * Looks up the <code>StringDataItem</code> from the given <code>DexFile</code> for the given
     * string value
     * @param dexFile the <code>Dexfile</code> to find the string value in
     * @param value The string value to look up
     * @return a <code>StringDataItem</code> from the given <code>DexFile</code> for the given
     * string value, or null if it doesn't exist
     **/
    public static StringDataItem lookupStringDataItem(DexFile dexFile, String value) {
        StringDataItem StringDataItem = new StringDataItem(dexFile, value);
        return dexFile.StringDataSection.getInternedItem(StringDataItem);
    }

    /** {@inheritDoc} */
    protected void readItem(Input in, ReadContext readContext) {
        in.readUnsignedLeb128(); //string length
        stringValue = in.realNullTerminatedUtf8String();
    }

    /** {@inheritDoc} */
    protected int placeItem(int offset) {
        return offset + Leb128Utils.unsignedLeb128Size(stringValue.length()) +
                Utf8Utils.stringToUtf8Bytes(stringValue).length + 1;
    }

    /** {@inheritDoc} */
    protected void writeItem(AnnotatedOutput out) {
        byte[] encodedValue = Utf8Utils.stringToUtf8Bytes(stringValue);
        if (out.annotates()) {
            out.annotate("string_size: 0x" + Integer.toHexString(stringValue.length()) + " (" + stringValue.length() +
                    ")");
            out.writeUnsignedLeb128(stringValue.length());

            out.annotate(encodedValue.length + 1, "string_data: \"" + Utf8Utils.escapeString(stringValue) + "\"");
        } else {
            out.writeUnsignedLeb128(stringValue.length());
        }
        out.write(encodedValue);
        out.writeByte(0);
    }

    /** {@inheritDoc} */
    public ItemType getItemType() {
        return ItemType.TYPE_STRING_DATA_ITEM;
    }

    /** {@inheritDoc} */
    public String getConciseIdentity() {
        return "string_data_item: \"" + Utf8Utils.escapeString(getStringValue()) + "\"";
    }

    /** {@inheritDoc} */
    public int compareTo(StringDataItem o) {
        return getStringValue().compareTo(o.getStringValue());
    }

    /**
     * Get the string value of this item as a <code>String</code>
     * @return the string value of this item as a String
     */
    public String getStringValue() {
        return stringValue;
    }

    /**
     * calculate and cache the hashcode
     */
    private void calcHashCode() {
        hashCode = getStringValue().hashCode();
    }

    @Override
    public int hashCode() {
        //there's a small possibility that the actual hash code will be 0. If so, we'll
        //just end up recalculating it each time
        if (hashCode == 0)
            calcHashCode();
        return hashCode;
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
        StringDataItem other = (StringDataItem)o;
        return getStringValue().equals(other.getStringValue());
    }
}
