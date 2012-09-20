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

public class FieldIdItem extends Item<FieldIdItem> implements Convertible<FieldIdItem> {
    private int hashCode = 0;

    private TypeIdItem classType;
    private TypeIdItem fieldType;
    private StringIdItem fieldName;

    /**
     * Creates a new uninitialized <code>FieldIdItem</code>
     * @param dexFile The <code>DexFile</code> that this item belongs to
     */
    protected FieldIdItem(DexFile dexFile) {
        super(dexFile);
    }

    /**
     * Creates a new <code>FieldIdItem</code> for the given class, type and name
     * @param dexFile The <code>DexFile</code> that this item belongs to
     * @param classType the class that the field is a member of
     * @param fieldType the type of the field
     * @param fieldName the name of the field
     */
    private FieldIdItem(DexFile dexFile, TypeIdItem classType, TypeIdItem fieldType, StringIdItem fieldName) {
        this(dexFile);

        assert classType.dexFile == dexFile;
        assert fieldType.dexFile == dexFile;
        assert fieldName.dexFile == dexFile;

        this.classType = classType;
        this.fieldType = fieldType;
        this.fieldName = fieldName;
    }

    /**
     * Returns a <code>FieldIdItem</code> for the given values, and that has been interned into
     * the given <code>DexFile</code>
     * @param dexFile The <code>DexFile</code> that this item belongs to
     * @param classType the class that the field is a member of
     * @param fieldType the type of the field
     * @param fieldName the name of the field
     * @return a <code>FieldIdItem</code> for the given values, and that has been interned into
     * the given <code>DexFile</code>
     */
    public static FieldIdItem internFieldIdItem(DexFile dexFile, TypeIdItem classType, TypeIdItem fieldType,
                                              StringIdItem fieldName) {
        FieldIdItem fieldIdItem = new FieldIdItem(dexFile, classType, fieldType, fieldName);
        return dexFile.FieldIdsSection.intern(fieldIdItem);
    }

    /**
     * Looks up a <code>FieldIdItem</code> from the given <code>DexFile</code> for the given
     * values
     * @param dexFile The <code>DexFile</code> that this item belongs to
     * @param classType the class that the field is a member of
     * @param fieldType the type of the field
     * @param fieldName the name of the field
     * @return a <code>FieldIdItem</code> from the given <code>DexFile</code> for the given
     * values, or null if it doesn't exist
     */
    public static FieldIdItem lookupFieldIdItem(DexFile dexFile, TypeIdItem classType, TypeIdItem fieldType,
                                              StringIdItem fieldName) {
        FieldIdItem fieldIdItem = new FieldIdItem(dexFile, classType, fieldType, fieldName);
        return dexFile.FieldIdsSection.getInternedItem(fieldIdItem);
    }

    /** {@inheritDoc} */
    protected void readItem(Input in, ReadContext readContext) {
        classType = dexFile.TypeIdsSection.getItemByIndex(in.readShort());
        fieldType = dexFile.TypeIdsSection.getItemByIndex(in.readShort());
        fieldName = dexFile.StringIdsSection.getItemByIndex(in.readInt());
    }

    /** {@inheritDoc} */
    protected int placeItem(int offset) {
        return offset + 8;
    }

    /** {@inheritDoc} */
    protected void writeItem(AnnotatedOutput out) {
        if (out.annotates()) {
            out.annotate(2, "class_type: " + classType.getTypeDescriptor());
            out.annotate(2, "field_type: " + fieldType.getTypeDescriptor());
            out.annotate(4, "field_name: " + fieldName.getStringValue());
        }

        int classIndex = classType.getIndex();
        if (classIndex > 0xffff) {
            throw new RuntimeException(String.format("Error writing field_id_item for %s. The type index of " +
                    "defining class %s is too large", getFieldString(), classType.getTypeDescriptor()));
        }
        out.writeShort(classIndex);

        int typeIndex = fieldType.getIndex();
        if (typeIndex > 0xffff) {
            throw new RuntimeException(String.format("Error writing field_id_item for %s. The type index of field " +
                    "type %s is too large", getFieldString(), fieldType.getTypeDescriptor()));
        }
        out.writeShort(typeIndex);

        out.writeInt(fieldName.getIndex());
    }

    /** {@inheritDoc} */
    public ItemType getItemType() {
        return ItemType.TYPE_FIELD_ID_ITEM;
    }

    /** {@inheritDoc} */
    public String getConciseIdentity() {
        return getFieldString();
    }

    /** {@inheritDoc} */
    public int compareTo(FieldIdItem o) {
        int result = classType.compareTo(o.classType);
        if (result != 0) {
            return result;
        }

        result = fieldName.compareTo(o.fieldName);
        if (result != 0) {
            return result;
        }

        return fieldType.compareTo(o.fieldType);
    }

    /**
     * @return the class that this field is a member of
     */
    public TypeIdItem getContainingClass() {
        return classType;
    }

    /**
     * @return the type of this field
     */
    public TypeIdItem getFieldType() {
        return fieldType;
    }

    /**
     * @return the field name
     */
    public StringIdItem getFieldName() {
        return fieldName;
    }

    String cachedFieldString = null;
    /**
     * @return a string formatted like LclassName;->fieldName:fieldType
     */
    public String getFieldString() {
        if (cachedFieldString == null) {
            String typeDescriptor = classType.getTypeDescriptor();
            String fieldName = this.fieldName.getStringValue();
            String fieldType = this.fieldType.getTypeDescriptor();

            StringBuffer sb = new StringBuffer(typeDescriptor.length() + fieldName.length() + fieldType.length() + 3);
            sb.append(typeDescriptor);
            sb.append("->");
            sb.append(fieldName);
            sb.append(":");
            sb.append(fieldType);
            cachedFieldString = sb.toString();
        }
        return cachedFieldString;
    }

    String cachedShortFieldString = null;
    /**
     * @return a "short" string containing just the field name and type, formatted like fieldName:fieldType
     */
    public String getShortFieldString() {
        if (cachedShortFieldString == null) {
            String fieldName = this.fieldName.getStringValue();
            String fieldType = this.fieldType.getTypeDescriptor();

            StringBuffer sb = new StringBuffer(fieldName.length() + fieldType.length() + 1);
            sb.append(fieldName);
            sb.append(":");
            sb.append(fieldType);
            cachedShortFieldString = sb.toString();
        }
        return cachedShortFieldString;
    }


    /**
     * calculate and cache the hashcode
     */
    private void calcHashCode() {
        hashCode = classType.hashCode();
        hashCode = 31 * hashCode + fieldType.hashCode();
        hashCode = 31 * hashCode + fieldName.hashCode();
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
        FieldIdItem other = (FieldIdItem)o;
        return (classType == other.classType &&
                fieldType == other.fieldType &&
                fieldName == other.fieldName);
    }

    public FieldIdItem convert() {
        return this;
    }
}