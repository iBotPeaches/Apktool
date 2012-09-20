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

import javax.annotation.Nullable;

public class TypeIdItem extends Item<TypeIdItem> {
    private StringIdItem typeDescriptor;

    /**
     * Creates a new uninitialized <code>TypeIdItem</code>
     * @param dexFile The <code>DexFile</code> that this item belongs to
     */
    protected TypeIdItem(DexFile dexFile) {
        super(dexFile);
    }

    /**
     * Creates a new <code>TypeIdItem</code> for the given <code>StringIdItem</code>
     * @param dexFile The <code>DexFile</code> that this item will belong to
     * @param typeDescriptor The <code>StringIdItem</code> containing the type descriptor that
     * this <code>TypeIdItem</code> represents
     */
    private TypeIdItem(DexFile dexFile, StringIdItem typeDescriptor) {
        super(dexFile);
        this.typeDescriptor = typeDescriptor;
    }

    /**
     * Returns a <code>TypeIdItem</code> for the given values, and that has been interned into
     * the given <code>DexFile</code>
     * @param dexFile The <code>DexFile</code> that this item will belong to
     * @param typeDescriptor The <code>StringIdItem</code> containing the type descriptor that
     * this <code>TypeIdItem</code> represents
     * @return a <code>TypeIdItem</code> for the given values, and that has been interned into
     * the given <code>DexFile</code>
     */
    public static TypeIdItem internTypeIdItem(DexFile dexFile, StringIdItem typeDescriptor) {
        TypeIdItem typeIdItem = new TypeIdItem(dexFile, typeDescriptor);
        return dexFile.TypeIdsSection.intern(typeIdItem);
    }

    /**
     * Returns a <code>TypeIdItem</code> for the given values, and that has been interned into
     * the given <code>DexFile</code>
     * @param dexFile The <code>DexFile</code> that this item will belong to
     * @param typeDescriptor The string containing the type descriptor that this
     * <code>TypeIdItem</code> represents
     * @return a <code>TypeIdItem</code> for the given values, and that has been interned into
     * the given <code>DexFile</code>
     */
    public static TypeIdItem internTypeIdItem(DexFile dexFile, String typeDescriptor) {
        StringIdItem stringIdItem = StringIdItem.internStringIdItem(dexFile, typeDescriptor);
        if (stringIdItem == null) {
            return null;
        }
        TypeIdItem typeIdItem = new TypeIdItem(dexFile, stringIdItem);
        return dexFile.TypeIdsSection.intern(typeIdItem);
    }

    /**
     * Looks up the <code>TypeIdItem</code> from the given <code>DexFile</code> for the given
     * type descriptor
     * @param dexFile the <code>Dexfile</code> to find the type in
     * @param typeDescriptor The string containing the type descriptor to look up
     * @return a <code>TypeIdItem</code> from the given <code>DexFile</code> for the given
     * type descriptor, or null if it doesn't exist
     */
    public static TypeIdItem lookupTypeIdItem(DexFile dexFile, String typeDescriptor) {
        StringIdItem stringIdItem = StringIdItem.lookupStringIdItem(dexFile, typeDescriptor);
        if (stringIdItem == null) {
            return null;
        }
        TypeIdItem typeIdItem = new TypeIdItem(dexFile, stringIdItem);
        return dexFile.TypeIdsSection.getInternedItem(typeIdItem);
    }

    /** {@inheritDoc} */
    protected void readItem(Input in, ReadContext readContext) {
        int stringIdIndex = in.readInt();
        this.typeDescriptor = dexFile.StringIdsSection.getItemByIndex(stringIdIndex);
    }

    /** {@inheritDoc} */
    protected int placeItem(int offset) {
        return offset + 4;
    }

    /** {@inheritDoc} */
    protected void writeItem(AnnotatedOutput out) {
        if (out.annotates()) {
            out.annotate(4, typeDescriptor.getConciseIdentity());
        }

        out.writeInt(typeDescriptor.getIndex());
    }

    /** {@inheritDoc} */
    public ItemType getItemType() {
        return ItemType.TYPE_TYPE_ID_ITEM;
    }

    /** {@inheritDoc} */
    public String getConciseIdentity() {
        return "type_id_item: " + getTypeDescriptor();
    }

    /** {@inheritDoc} */
    public int compareTo(TypeIdItem o) {
        //sort by the index of the StringIdItem
        return typeDescriptor.compareTo(o.typeDescriptor);
    }

    /**
     * Returns the type descriptor as a <code>String</code> for this type
     * @return the type descriptor as a <code>String</code> for this type
     */
    public String getTypeDescriptor() {
        return typeDescriptor.getStringValue();
    }

    /**
     * Returns the type descriptor as a <code>String</code> for the given type
     * @param typeIdItem The <code>TypeIdItem</code> to get the type descriptor of
     * @return the type descriptor as a <code>String</code> for the gvien type
     */
    @Nullable
    public static String getTypeDescriptor(@Nullable TypeIdItem typeIdItem) {
        return typeIdItem==null?null:typeIdItem.getTypeDescriptor();
    }

    /**
     * Returns the "shorty" representation of this type, used to create the shorty prototype string for a method
     * @return the "shorty" representation of this type, used to create the shorty prototype string for a method
     */
    public String toShorty() {
        String type = getTypeDescriptor();
        if (type.length() > 1) {
            return "L";
        } else {
            return type;
        }
    }

    /**
     * Calculates the number of 2-byte registers that an instance of this type requires
     * @return The number of 2-byte registers that an instance of this type requires
     */
    public int getRegisterCount() {
        String type = this.getTypeDescriptor();
        /** Only the long and double primitive types are 2 words,
         * everything else is a single word
         */
        if (type.charAt(0) == 'J' || type.charAt(0) == 'D') {
            return 2;
        } else {
            return 1;
        }
    }

    @Override
    public int hashCode() {
        return typeDescriptor.hashCode();
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
        TypeIdItem other = (TypeIdItem)o;
        return typeDescriptor == other.typeDescriptor;
    }
}
