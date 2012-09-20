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
import org.jf.dexlib.Util.ReadOnlyArrayList;

import java.util.List;

public class TypeListItem extends Item<TypeListItem> {
    private int hashCode = 0;

    private TypeIdItem[] typeList;

    /**
     * Creates a new uninitialized <code>TypeListItem</code>
     * @param dexFile The <code>DexFile</code> that this item belongs to
     */
    protected TypeListItem(DexFile dexFile) {
        super(dexFile);
    }

    /**
     * Creates a new <code>TypeListItem</code> for the given string
     * @param dexFile The <code>DexFile</code> that this item belongs to
     * @param typeList A list of the types that this <code>TypeListItem</code> represents
     */
    private TypeListItem(DexFile dexFile, TypeIdItem[] typeList) {
        super(dexFile);

        this.typeList = typeList;
    }

    /**
     * Returns a <code>TypeListItem</code> for the given values, and that has been interned into
     * the given <code>DexFile</code>
     * @param dexFile The <code>DexFile</code> that this item belongs to
     * @param typeList A list of the types that this <code>TypeListItem</code> represents
     * @return a <code>TypeListItem</code> for the given values, and that has been interned into
     * the given <code>DexFile</code>
     */
    public static TypeListItem internTypeListItem(DexFile dexFile, List<TypeIdItem> typeList) {
        TypeIdItem[] typeArray = new TypeIdItem[typeList.size()];
        typeList.toArray(typeArray);
        TypeListItem typeListItem = new TypeListItem(dexFile, typeArray);
        return dexFile.TypeListsSection.intern(typeListItem);
    }

    /**
     * Looks up the <code>TypeListItem</code> from the given <code>DexFile</code> for the given
     * list of types
     * @param dexFile the <code>Dexfile</code> to find the type in
     * @param typeList A list of the types that the <code>TypeListItem</code> represents
     * @return a <code>TypeListItem</code> from the given <code>DexFile</code> for the given
     * list of types, or null if it doesn't exist
     */
    public static TypeListItem lookupTypeListItem(DexFile dexFile, List<TypeIdItem> typeList) {
        TypeIdItem[] typeArray = new TypeIdItem[typeList.size()];
        typeList.toArray(typeArray);
        TypeListItem typeListItem = new TypeListItem(dexFile, typeArray);
        return dexFile.TypeListsSection.getInternedItem(typeListItem);
    }

    /** {@inheritDoc} */
    protected void readItem(Input in, ReadContext readContext) {
        int size = in.readInt();
        typeList = new TypeIdItem[size];
        for (int i=0; i<size; i++) {
            int typeIndex = in.readShort();
            typeList[i] = dexFile.TypeIdsSection.getItemByIndex(typeIndex);
        }
    }

    /** {@inheritDoc} */
    protected int placeItem(int offset) {
        return offset + 4 + typeList.length * 2;
    }

    /** {@inheritDoc} */
    protected void writeItem(AnnotatedOutput out) {
        //yes, the code to write the item is duplicated. This eliminates the need to iterate over the list twice

        if (out.annotates()) {
            out.annotate(4, "size: 0x" + Integer.toHexString(typeList.length) + " (" + typeList.length +")");

            for (TypeIdItem typeIdItem: typeList) {
                out.annotate(2, "type_id_item: " + typeIdItem.getTypeDescriptor());
            }
        }
        out.writeInt(typeList.length);
        for (TypeIdItem typeIdItem: typeList) {
            int typeIndex = typeIdItem.getIndex();
            if (typeIndex > 0xffff) {
                throw new RuntimeException(String.format("Error writing type_list entry. The type index of " +
                    "type %s is too large", typeIdItem.getTypeDescriptor()));
            }
            out.writeShort(typeIndex);
        }
    }

    /** {@inheritDoc} */
    public ItemType getItemType() {
        return ItemType.TYPE_TYPE_LIST;
    }

    /** {@inheritDoc} */
    public String getConciseIdentity() {
        return "type_list: " + getTypeListString("");
    }

    /** {@inheritDoc} */
    public int compareTo(TypeListItem o) {
        if (o == null) {
            return 1;
        }

        int thisSize = typeList.length;
        int otherSize = o.typeList.length;
        int size = Math.min(thisSize, otherSize);

        for (int i = 0; i < size; i++) {
            int result = typeList[i].compareTo(o.typeList[i]);
            if (result != 0) {
                return result;
            }
        }

        if (thisSize < otherSize) {
            return -1;
        } else if (thisSize > otherSize) {
            return 1;
        } else {
            return 0;
        }
    }

    /**
     * @return the number of registers required for this <code>TypeListItem</code>
     */
    public int getRegisterCount() {
        int wordCount = 0;
        for (TypeIdItem typeIdItem: typeList) {
            wordCount += typeIdItem.getRegisterCount();
        }
        return wordCount;
    }

    /**
     * @return a string consisting of the type descriptors in this <code>TypeListItem</code>
     * that are separated by the given separator
     * @param separator the separator between each type
     */
    public String getTypeListString(String separator) {
        int size = 0;
        for (TypeIdItem typeIdItem: typeList) {
            size += typeIdItem.getTypeDescriptor().length();
            size += separator.length();
        }

        StringBuilder sb = new StringBuilder(size);
        for (TypeIdItem typeIdItem: typeList) {
            sb.append(typeIdItem.getTypeDescriptor());
            sb.append(separator);
        }
        if (typeList.length > 0) {
            sb.delete(sb.length() - separator.length(), sb.length());
        }
        return sb.toString();
    }

    /**
     * @return a string consisting of the shorty form of the type descriptors in this
     * <code>TypeListItem</code> that are directly concatenated together
     */
    public String getShortyString() {
        StringBuilder sb = new StringBuilder();
        for (TypeIdItem typeIdItem: typeList) {
            sb.append(typeIdItem.toShorty());
        }
        return sb.toString();
    }

    /**
     * @param index the index of the <code>TypeIdItem</code> to get
     * @return the <code>TypeIdItem</code> at the given index
     */
    public TypeIdItem getTypeIdItem(int index) {
        return typeList[index];
    }

    /**
     * @return the number of types in this <code>TypeListItem</code>
     */
    public int getTypeCount() {
        return typeList.length;
    }

    /**
     * @return an array of the <code>TypeIdItems</code> in this <code>TypeListItem</code>
     */
    public List<TypeIdItem> getTypes() {
        return new ReadOnlyArrayList<TypeIdItem>(typeList);
    }

    /**
     * Helper method to allow easier "inline" retrieval of of the list of TypeIdItems
     * @param typeListItem the typeListItem to return the types of (can be null)
     * @return an array of the <code>TypeIdItems</code> in the specified <code>TypeListItem</code>, or null if the
     * TypeListItem is null
     */
    public static List<TypeIdItem> getTypes(TypeListItem typeListItem) {
        return typeListItem==null?null:typeListItem.getTypes();
    }

    /**
     * calculate and cache the hashcode
     */
    private void calcHashCode() {
        int hashCode = 1;

        for (TypeIdItem typeIdItem: typeList) {
            hashCode = 31 * hashCode + typeIdItem.hashCode();
        }
        this.hashCode = hashCode;
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
        TypeListItem other = (TypeListItem)o;
        if (typeList.length != other.typeList.length) {
            return false;
        }

        for (int i=0; i<typeList.length; i++) {
            if (typeList[i] != other.typeList[i]) {
                return false;
            }
        }
        return true;
    }
}
