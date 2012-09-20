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

public class MethodIdItem extends Item<MethodIdItem> implements Convertible<MethodIdItem> {
    private int hashCode = 0;

    private TypeIdItem classType;
    private ProtoIdItem methodPrototype;
    private StringIdItem methodName;

    /**
     * Creates a new uninitialized <code>MethodIdItem</code>
     * @param dexFile The <code>DexFile</code> that this item belongs to
     */
    protected MethodIdItem(DexFile dexFile) {
        super(dexFile);
    }

    /**
     * Creates a new <code>MethodIdItem</code> for the given class, type and name
     * @param dexFile The <code>DexFile</code> that this item belongs to
     * @param classType the class that the method is a member of
     * @param methodPrototype the type of the method
     * @param methodName the name of the method
     */
    private MethodIdItem(DexFile dexFile, TypeIdItem classType, ProtoIdItem methodPrototype, StringIdItem methodName) {
        this(dexFile);
        this.classType = classType;
        this.methodPrototype = methodPrototype;
        this.methodName = methodName;
    }

    /**
     * Returns a <code>MethodIdItem</code> for the given values, and that has been interned into
     * the given <code>DexFile</code>
     * @param dexFile The <code>DexFile</code> that this item belongs to
     * @param classType the class that the method is a member of
     * @param methodPrototype the type of the method
     * @param methodName the name of the method
     * @return a <code>MethodIdItem</code> for the given values, and that has been interned into
     * the given <code>DexFile</code>
     */
    public static MethodIdItem internMethodIdItem(DexFile dexFile, TypeIdItem classType,
                                                       ProtoIdItem methodPrototype, StringIdItem methodName) {
        MethodIdItem methodIdItem = new MethodIdItem(dexFile, classType, methodPrototype, methodName);
        return dexFile.MethodIdsSection.intern(methodIdItem);
    }

    /**
     * Looks up a <code>MethodIdItem</code> from the given <code>DexFile</code> for the given
     * values
     * @param dexFile The <code>DexFile</code> that this item belongs to
     * @param classType the class that the method is a member of
     * @param methodPrototype the type of the method
     * @param methodName the name of the method
     * @return a <code>MethodIdItem</code> from the given <code>DexFile</code> for the given
     * values, or null if it doesn't exist
     */
    public static MethodIdItem lookupMethodIdItem(DexFile dexFile, TypeIdItem classType,
                                                       ProtoIdItem methodPrototype, StringIdItem methodName) {
        MethodIdItem methodIdItem = new MethodIdItem(dexFile, classType, methodPrototype, methodName);
        return dexFile.MethodIdsSection.getInternedItem(methodIdItem);
    }

    /** {@inheritDoc} */
    protected void readItem(Input in, ReadContext readContext) {
        classType = dexFile.TypeIdsSection.getItemByIndex(in.readShort());
        methodPrototype = dexFile.ProtoIdsSection.getItemByIndex(in.readShort());
        methodName = dexFile.StringIdsSection.getItemByIndex(in.readInt());
    }

    /** {@inheritDoc} */
    protected int placeItem(int offset) {
        return offset + 8;
    }

    /** {@inheritDoc} */
    protected void writeItem(AnnotatedOutput out) {
        if (out.annotates()) {
            out.annotate(2, "class_type: " + classType.getTypeDescriptor());
            out.annotate(2, "method_prototype: " + methodPrototype.getPrototypeString());
            out.annotate(4, "method_name: " + methodName.getStringValue());
        }

        int classIndex = classType.getIndex();
        if (classIndex > 0xffff) {
            throw new RuntimeException(String.format("Error writing method_id_item for %s. The type index of " +
                    "defining class %s is too large", getMethodString(), classType.getTypeDescriptor()));
        }
        out.writeShort(classIndex);

        int prototypeIndex = methodPrototype.getIndex();
        if (prototypeIndex > 0xffff) {
            throw new RuntimeException(String.format("Error writing method_id_item for %0. The prototype index of " +
                    "method prototype %s is too large", getMethodString(), methodPrototype.getPrototypeString()));
        }
        out.writeShort(prototypeIndex);

        out.writeInt(methodName.getIndex());
    }

    /** {@inheritDoc} */
    public ItemType getItemType() {
        return ItemType.TYPE_METHOD_ID_ITEM;
    }

    /** {@inheritDoc} */
    public String getConciseIdentity() {
        return "method_id_item: " + getMethodString();
    }

    /** {@inheritDoc} */
    public int compareTo(MethodIdItem o) {
        int result = classType.compareTo(o.classType);
        if (result != 0) {
            return result;
        }

        result = methodName.compareTo(o.methodName);
        if (result != 0) {
            return result;
        }

        return methodPrototype.compareTo(o.methodPrototype);
    }

    private String cachedMethodString = null;
    /**
     * @return a string formatted like LclassName;->methodName(TTTT..)R
     */
    public String getMethodString() {
        if (cachedMethodString == null) {
            String classType = this.classType.getTypeDescriptor();
            String methodName = this.methodName.getStringValue();
            String prototypeString = methodPrototype.getPrototypeString();

            StringBuilder sb = new StringBuilder(classType.length() + methodName.length() + prototypeString.length() +
                    2);
            sb.append(classType);
            sb.append("->");
            sb.append(methodName);
            sb.append(prototypeString);
            cachedMethodString = sb.toString();
        }
        return cachedMethodString;
    }

    private String cachedShortMethodString = null;
    /**
     * @return a string formatted like methodName(TTTT..)R
     */
    public String getShortMethodString() {
        if (cachedShortMethodString == null) {
            String methodName = this.methodName.getStringValue();
            String prototypeString = methodPrototype.getPrototypeString();

            StringBuilder sb = new StringBuilder(methodName.length() + prototypeString.length());
            sb.append(methodName);
            sb.append(prototypeString);
            cachedShortMethodString = sb.toString();
        }
        return cachedShortMethodString;
    }

    /**
     * @return the method prototype
     */
    public ProtoIdItem getPrototype() {
        return methodPrototype;
    }

    /**
     * @return the name of the method
     */
    public StringIdItem getMethodName() {
        return methodName;
    }

    /**
     * @return the class this method is a member of
     */
    public TypeIdItem getContainingClass() {
        return classType;
    }

    /**
     * calculate and cache the hashcode
     */
    private void calcHashCode() {
        hashCode = classType.hashCode();
        hashCode = 31 * hashCode + methodPrototype.hashCode();
        hashCode = 31 * hashCode + methodName.hashCode();
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
        MethodIdItem other = (MethodIdItem)o;
        return (classType == other.classType &&
                methodPrototype == other.methodPrototype &&
                methodName == other.methodName);
    }

    public MethodIdItem convert() {
        return this;
    }
}
