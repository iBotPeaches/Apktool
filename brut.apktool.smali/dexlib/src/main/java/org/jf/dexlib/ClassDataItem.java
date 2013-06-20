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

import com.google.common.base.Preconditions;
import org.jf.dexlib.Util.*;
import org.jf.util.ExceptionWithContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class ClassDataItem extends Item<ClassDataItem> {
    @Nullable
    private EncodedField[] staticFields = null;
    @Nullable
    private EncodedField[] instanceFields = null;
    @Nullable
    private EncodedMethod[] directMethods = null;
    @Nullable
    private EncodedMethod[] virtualMethods = null;

    /**
     * Creates a new uninitialized <code>ClassDataItem</code>
     * @param dexFile The <code>DexFile</code> that this item belongs to
     */
    public ClassDataItem(final DexFile dexFile) {
        super(dexFile);
    }

    /**
     * Creates a new <code>ClassDataItem</code> with the given values
     * @param dexFile The <code>DexFile</code> that this item belongs to
     * @param staticFields The static fields for this class
     * @param instanceFields The instance fields for this class
     * @param directMethods The direct methods for this class
     * @param virtualMethods The virtual methods for this class
     */
    private ClassDataItem(DexFile dexFile, @Nullable EncodedField[] staticFields,
                          @Nullable EncodedField[] instanceFields, @Nullable EncodedMethod[] directMethods,
                          @Nullable EncodedMethod[] virtualMethods) {
        super(dexFile);
        this.staticFields = staticFields;
        this.instanceFields = instanceFields;
        this.directMethods = directMethods;
        this.virtualMethods = virtualMethods;
    }

    /**
     * Creates a new <code>ClassDataItem</code> with the given values
     * @param dexFile The <code>DexFile</code> that this item belongs to
     * @param staticFields The static fields for this class
     * @param instanceFields The instance fields for this class
     * @param directMethods The direct methods for this class
     * @param virtualMethods The virtual methods for this class
     * @return a new <code>ClassDataItem</code> with the given values
     */
    public static ClassDataItem internClassDataItem(DexFile dexFile, @Nullable List<EncodedField> staticFields,
                                                    @Nullable List<EncodedField> instanceFields,
                                                    @Nullable List<EncodedMethod> directMethods,
                                                    @Nullable List<EncodedMethod> virtualMethods) {
        EncodedField[] staticFieldsArray = null;
        EncodedField[] instanceFieldsArray = null;
        EncodedMethod[] directMethodsArray = null;
        EncodedMethod[] virtualMethodsArray = null;

        if (staticFields != null && staticFields.size() > 0) {
            SortedSet<EncodedField> staticFieldsSet = new TreeSet<EncodedField>();
            for (EncodedField staticField: staticFields) {
                if (staticFieldsSet.contains(staticField)) {
                    System.err.println(String.format("Ignoring duplicate static field definition: %s",
                            staticField.field.getFieldString()));
                    continue;
                }
                staticFieldsSet.add(staticField);
            }

            staticFieldsArray = new EncodedField[staticFieldsSet.size()];
            staticFieldsArray = staticFieldsSet.toArray(staticFieldsArray);
        }

        if (instanceFields != null && instanceFields.size() > 0) {
            SortedSet<EncodedField> instanceFieldsSet = new TreeSet<EncodedField>();
            for (EncodedField instanceField: instanceFields) {
                if (instanceFieldsSet.contains(instanceField)) {
                    System.err.println(String.format("Ignoring duplicate instance field definition: %s",
                            instanceField.field.getFieldString()));
                    continue;
                }
                instanceFieldsSet.add(instanceField);
            }

            instanceFieldsArray = new EncodedField[instanceFieldsSet.size()];
            instanceFieldsArray = instanceFieldsSet.toArray(instanceFieldsArray);
        }

        TreeSet<EncodedMethod> directMethodSet = new TreeSet<EncodedMethod>();

        if (directMethods != null && directMethods.size() > 0) {
            for (EncodedMethod directMethod: directMethods) {
                if (directMethodSet.contains(directMethod)) {
                    System.err.println(String.format("Ignoring duplicate direct method definition: %s",
                            directMethod.method.getMethodString()));
                    continue;
                }
                directMethodSet.add(directMethod);
            }

            directMethodsArray = new EncodedMethod[directMethodSet.size()];
            directMethodsArray = directMethodSet.toArray(directMethodsArray);
        }

        if (virtualMethods != null && virtualMethods.size() > 0) {
            TreeSet<EncodedMethod> virtualMethodSet = new TreeSet<EncodedMethod>();
            for (EncodedMethod virtualMethod: virtualMethods) {
                if (directMethodSet.contains(virtualMethod)) {
                    // If both a direct and virtual definition is present, dalvik's behavior seems to be undefined,
                    // so we can't gracefully handle this case, like we can if the duplicates are all direct or all
                    // virtual -- in which case, we ignore all but the first definition
                    throw new RuntimeException(String.format("Duplicate direct+virtual method definition: %s",
                            virtualMethod.method.getMethodString()));
                }
                if (virtualMethodSet.contains(virtualMethod)) {
                    System.err.println(String.format("Ignoring duplicate virtual method definition: %s",
                            virtualMethod.method.getMethodString()));
                    continue;
                }
                virtualMethodSet.add(virtualMethod);
            }

            virtualMethodsArray = new EncodedMethod[virtualMethodSet.size()];
            virtualMethodsArray = virtualMethodSet.toArray(virtualMethodsArray);
        }

        ClassDataItem classDataItem = new ClassDataItem(dexFile, staticFieldsArray, instanceFieldsArray,
                directMethodsArray, virtualMethodsArray);
        return dexFile.ClassDataSection.intern(classDataItem);
    }

    /** {@inheritDoc} */
    protected void readItem(Input in, ReadContext readContext) {
        int staticFieldsCount = in.readUnsignedLeb128();
        int instanceFieldsCount = in.readUnsignedLeb128();
        int directMethodsCount = in.readUnsignedLeb128();
        int virtualMethodsCount = in.readUnsignedLeb128();

        if (staticFieldsCount > 0) {
            staticFields = new EncodedField[staticFieldsCount];
            EncodedField previousEncodedField = null;
            for (int i=0; i<staticFieldsCount; i++) {
                try {
                    staticFields[i] = previousEncodedField = new EncodedField(dexFile, in, previousEncodedField);
                } catch (Exception ex) {
                    throw ExceptionWithContext.withContext(ex, "Error while reading static field at index " + i);
                }
            }
        }

        if (instanceFieldsCount > 0) {
            instanceFields = new EncodedField[instanceFieldsCount];
            EncodedField previousEncodedField = null;
            for (int i=0; i<instanceFieldsCount; i++) {
                try {
                    instanceFields[i] = previousEncodedField = new EncodedField(dexFile, in, previousEncodedField);
                } catch (Exception ex) {
                    throw ExceptionWithContext.withContext(ex, "Error while reading instance field at index " + i);
                }
            }
        }

        if (directMethodsCount > 0) {
            directMethods = new EncodedMethod[directMethodsCount];
            EncodedMethod previousEncodedMethod = null;
            for (int i=0; i<directMethodsCount; i++) {
                try {
                    directMethods[i] = previousEncodedMethod = new EncodedMethod(dexFile, readContext, in,
                            previousEncodedMethod);
                } catch (Exception ex) {
                    throw ExceptionWithContext.withContext(ex, "Error while reading direct method at index " + i);
                }
            }
        }

        if (virtualMethodsCount > 0) {
            virtualMethods = new EncodedMethod[virtualMethodsCount];
            EncodedMethod previousEncodedMethod = null;
            for (int i=0; i<virtualMethodsCount; i++) {
                try {
                    virtualMethods[i] = previousEncodedMethod = new EncodedMethod(dexFile, readContext, in,
                            previousEncodedMethod);
                } catch (Exception ex) {
                    throw ExceptionWithContext.withContext(ex, "Error while reading virtual method at index " + i);
                }
            }
        }
    }

    /** {@inheritDoc} */
    protected int placeItem(int offset) {
        offset += Leb128Utils.unsignedLeb128Size(getStaticFieldCount());
        offset += Leb128Utils.unsignedLeb128Size(getInstanceFieldCount());
        offset += Leb128Utils.unsignedLeb128Size(getDirectMethodCount());
        offset += Leb128Utils.unsignedLeb128Size(getVirtualMethodCount());

        if (staticFields != null) {
            EncodedField previousEncodedField = null;
            for (EncodedField encodedField: staticFields) {
                offset = encodedField.place(offset, previousEncodedField);
                previousEncodedField = encodedField;
            }
        }

        if (instanceFields != null) {
            EncodedField previousEncodedField = null;
            for (EncodedField encodedField: instanceFields) {
                offset = encodedField.place(offset, previousEncodedField);
                previousEncodedField = encodedField;
            }
        }

        if (directMethods != null) {
            EncodedMethod previousEncodedMethod = null;
            for (EncodedMethod encodedMethod: directMethods) {
                offset = encodedMethod.place(offset, previousEncodedMethod);
                previousEncodedMethod = encodedMethod;
            }
        }

        if (virtualMethods != null) {
            EncodedMethod previousEncodedMethod = null;
            for (EncodedMethod encodedMethod: virtualMethods) {
                offset = encodedMethod.place(offset, previousEncodedMethod);
                previousEncodedMethod = encodedMethod;
            }
        }

        return offset;
    }

    /** {@inheritDoc} */
    protected void writeItem(AnnotatedOutput out) {
        if (out.annotates()) {
            int staticFieldCount = getStaticFieldCount();
            out.annotate("static_fields_size: 0x" + Integer.toHexString(staticFieldCount) + " (" +
                    staticFieldCount + ")");
            out.writeUnsignedLeb128(staticFieldCount);

            int instanceFieldCount = getInstanceFieldCount();
            out.annotate("instance_fields_size: 0x" + Integer.toHexString(instanceFieldCount) + " (" +
                    instanceFieldCount + ")");
            out.writeUnsignedLeb128(instanceFieldCount);

            int directMethodCount = getDirectMethodCount();
            out.annotate("direct_methods_size: 0x" + Integer.toHexString(directMethodCount) + " (" +
                    directMethodCount + ")");
            out.writeUnsignedLeb128(directMethodCount);

            int virtualMethodCount = getVirtualMethodCount();
            out.annotate("virtual_methods_size: 0x" + Integer.toHexString(virtualMethodCount) + " (" +
                    virtualMethodCount + ")");
            out.writeUnsignedLeb128(virtualMethodCount);


            if (staticFields != null) {
                int index = 0;
                EncodedField previousEncodedField = null;
                for (EncodedField encodedField: staticFields) {
                    out.annotate("[" + index++ + "] static_field");
                    out.indent();
                    encodedField.writeTo(out, previousEncodedField);
                    out.deindent();
                    previousEncodedField = encodedField;
                }
            }

            if (instanceFields != null) {
                int index = 0;
                EncodedField previousEncodedField = null;
                for (EncodedField encodedField: instanceFields) {
                    out.annotate("[" + index++ + "] instance_field");
                    out.indent();
                    encodedField.writeTo(out, previousEncodedField);
                    out.deindent();
                    previousEncodedField = encodedField;
                }
            }

            if (directMethods != null) {
                int index = 0;
                EncodedMethod previousEncodedMethod = null;
                for (EncodedMethod encodedMethod: directMethods) {
                    out.annotate("[" + index++ + "] direct_method");
                    out.indent();
                    encodedMethod.writeTo(out, previousEncodedMethod);
                    out.deindent();
                    previousEncodedMethod = encodedMethod;
                }
            }

            if (virtualMethods != null) {
                int index = 0;
                EncodedMethod previousEncodedMethod = null;
                for (EncodedMethod encodedMethod: virtualMethods) {
                    out.annotate("[" + index++ + "] virtual_method");
                    out.indent();
                    encodedMethod.writeTo(out, previousEncodedMethod);
                    out.deindent();
                    previousEncodedMethod = encodedMethod;
                }
            }
        } else {
            out.writeUnsignedLeb128(getStaticFieldCount());
            out.writeUnsignedLeb128(getInstanceFieldCount());
            out.writeUnsignedLeb128(getDirectMethodCount());
            out.writeUnsignedLeb128(getVirtualMethodCount());

            if (staticFields != null) {
                EncodedField previousEncodedField = null;
                for (EncodedField encodedField: staticFields) {
                    encodedField.writeTo(out, previousEncodedField);
                    previousEncodedField = encodedField;
                }
            }


            if (instanceFields != null) {
                EncodedField previousEncodedField = null;
                for (EncodedField encodedField: instanceFields) {
                    encodedField.writeTo(out, previousEncodedField);
                    previousEncodedField = encodedField;
                }
            }

            if (directMethods != null) {
                EncodedMethod previousEncodedMethod = null;
                for (EncodedMethod encodedMethod: directMethods) {
                    encodedMethod.writeTo(out, previousEncodedMethod);
                    previousEncodedMethod = encodedMethod;
                }
            }

            if (virtualMethods != null) {
                EncodedMethod previousEncodedMethod = null;
                for (EncodedMethod encodedMethod: virtualMethods) {
                    encodedMethod.writeTo(out, previousEncodedMethod);
                    previousEncodedMethod = encodedMethod;
                }
            }
        }
    }

    /** {@inheritDoc} */
    public ItemType getItemType() {
        return ItemType.TYPE_CLASS_DATA_ITEM;
    }

    /** {@inheritDoc} */
    public String getConciseIdentity() {
        TypeIdItem parentType = getParentType();
        if (parentType == null) {
            return "class_data_item @0x" + Integer.toHexString(getOffset());
        }
        return "class_data_item @0x" + Integer.toHexString(getOffset()) + " (" + parentType.getTypeDescriptor() +")";
    }

    /** {@inheritDoc} */
    public int compareTo(ClassDataItem other) {
        Preconditions.checkNotNull(other);

        // An empty CodeDataItem may be shared by multiple ClassDefItems, so we can't use parent in this case
        if (isEmpty()) {
            if (other.isEmpty()) {
                return 0;
            }
            return -1;
        }
        if (other.isEmpty()) {
            return 1;
        }

        TypeIdItem parentType = getParentType();
        TypeIdItem otherParentType= other.getParentType();
        if (parentType == null) {
            if (otherParentType == null) {
                return 0;
            }
            return -1;
        }
        if (otherParentType == null) {
            return 1;
        }
        return parentType.compareTo(otherParentType);
    }

    @Override
    public int hashCode() {
        // If the item has a single parent, we can use the re-use the identity (hash) of that parent
        TypeIdItem parentType = getParentType();
        if (parentType != null) {
            return parentType.hashCode();
        }
        return 0;
    }

    /**
     * Returns the parent type for a non-empty ClassDataItem, or null for an empty one (which could be referenced by
     * multiple ClassDefItem parents)
     *
     * Only an empty ClassDataItem may have multiple parents.
     *
     * @return The parent type for this ClassDefItem, or null if it may have multiple parents
     */
    @Nullable
    public TypeIdItem getParentType() {
        if (staticFields != null && staticFields.length > 0) {
            return staticFields[0].field.getContainingClass();
        }
        if (instanceFields != null && instanceFields.length > 0) {
            return instanceFields[0].field.getContainingClass();
        }
        if (directMethods != null && directMethods.length > 0) {
            return directMethods[0].method.getContainingClass();
        }
        if (virtualMethods != null && virtualMethods.length > 0) {
            return virtualMethods[0].method.getContainingClass();
        }
        return null;
    }

    /**
     * @return the static fields for this class
     */
    @Nonnull
    public List<EncodedField> getStaticFields() {
        if (staticFields == null) {
            return Collections.emptyList();
        }
        return ReadOnlyArrayList.of(staticFields);
    }

    /**
     * @return the instance fields for this class
     */
    @Nonnull
    public List<EncodedField> getInstanceFields() {
        if (instanceFields == null) {
            return Collections.emptyList();
        }
        return ReadOnlyArrayList.of(instanceFields);
    }

    /**
     * @return the direct methods for this class
     */
    @Nonnull
    public List<EncodedMethod> getDirectMethods() {
        if (directMethods == null) {
            return Collections.emptyList();
        }
        return ReadOnlyArrayList.of(directMethods);
    }

    /**
     * @return the virtual methods for this class
     */
    @Nonnull
    public List<EncodedMethod> getVirtualMethods() {
        if (virtualMethods == null) {
            return Collections.emptyList();
        }
        return ReadOnlyArrayList.of(virtualMethods);
    }

    /**
     * @return The number of static fields in this <code>ClassDataItem</code>
     */
    public int getStaticFieldCount() {
        if (staticFields == null) {
            return 0;
        }
        return staticFields.length;
    }

    /**
     * @return The number of instance fields in this <code>ClassDataItem</code>
     */
    public int getInstanceFieldCount() {
        if (instanceFields == null) {
            return 0;
        }
        return instanceFields.length;
    }

    /**
     * @return The number of direct methods in this <code>ClassDataItem</code>
     */
    public int getDirectMethodCount() {
        if (directMethods == null) {
            return 0;
        }
        return directMethods.length;
    }

    /**
     * @return The number of virtual methods in this <code>ClassDataItem</code>
     */
    public int getVirtualMethodCount() {
        if (virtualMethods == null) {
            return 0;
        }
        return virtualMethods.length;
    }

    /**
     * @return true if this is an empty ClassDataItem
     */
    public boolean isEmpty() {
        return (getStaticFieldCount() + getInstanceFieldCount() +
                getDirectMethodCount() + getVirtualMethodCount()) == 0;
    }

    /**
     * Performs a binary search for the definition of the specified direct method
     * @param methodIdItem The MethodIdItem of the direct method to search for
     * @return The EncodedMethod for the specified direct method, or null if not found
     */
    public EncodedMethod findDirectMethodByMethodId(MethodIdItem methodIdItem) {
        return findMethodByMethodIdInternal(methodIdItem.index, directMethods);
    }

    /**
     * Performs a binary search for the definition of the specified virtual method
     * @param methodIdItem The MethodIdItem of the virtual method to search for
     * @return The EncodedMethod for the specified virtual method, or null if not found
     */
    public EncodedMethod findVirtualMethodByMethodId(MethodIdItem methodIdItem) {
        return findMethodByMethodIdInternal(methodIdItem.index, virtualMethods);
    }

    /**
     * Performs a binary search for the definition of the specified method. It can be either direct or virtual
     * @param methodIdItem The MethodIdItem of the virtual method to search for
     * @return The EncodedMethod for the specified virtual method, or null if not found
     */
    public EncodedMethod findMethodByMethodId(MethodIdItem methodIdItem) {
        EncodedMethod encodedMethod = findMethodByMethodIdInternal(methodIdItem.index, directMethods);
        if (encodedMethod != null) {
            return encodedMethod;
        }

        return findMethodByMethodIdInternal(methodIdItem.index, virtualMethods);
    }

    private static EncodedMethod findMethodByMethodIdInternal(int methodIdItemIndex, EncodedMethod[] encodedMethods) {
        int min = 0;
        int max = encodedMethods.length;

        while (min<max) {
            int index = (min+max)>>1;

            EncodedMethod encodedMethod = encodedMethods[index];

            int encodedMethodIndex = encodedMethod.method.getIndex();
            if (encodedMethodIndex == methodIdItemIndex) {
                return encodedMethod;
            } else if (encodedMethodIndex < methodIdItemIndex) {
                if (min == index) {
                    break;
                }
                min = index;
            } else {
                if (max == index) {
                    break;
                }
                max = index;
            }
        }

        return null;
    }

    public static class EncodedField implements Comparable<EncodedField> {
        /**
         * The <code>FieldIdItem</code> that this <code>EncodedField</code> is associated with
         */
        public final FieldIdItem field;

        /**
         * The access flags for this field
         */
        public final int accessFlags;

        /**
         * Constructs a new <code>EncodedField</code> with the given values
         * @param field The <code>FieldIdItem</code> that this <code>EncodedField</code> is associated with
         * @param accessFlags The access flags for this field
         */
        public EncodedField(FieldIdItem field, int accessFlags) {
            this.field = field;
            this.accessFlags = accessFlags;
        }

        /**
         * This is used internally to construct a new <code>EncodedField</code> while reading in a <code>DexFile</code>
         * @param dexFile The <code>DexFile</code> that is being read in
         * @param in the Input object to read the <code>EncodedField</code> from
         * @param previousEncodedField The previous <code>EncodedField</code> in the list containing this
         * <code>EncodedField</code>.
         */
        private EncodedField(DexFile dexFile, Input in, @Nullable EncodedField previousEncodedField) {
            int previousIndex = previousEncodedField==null?0:previousEncodedField.field.getIndex();
            field = dexFile.FieldIdsSection.getItemByIndex(in.readUnsignedLeb128() + previousIndex);
            accessFlags = in.readUnsignedLeb128();
        }

        /**
         * Writes the <code>EncodedField</code> to the given <code>AnnotatedOutput</code> object
         * @param out the <code>AnnotatedOutput</code> object to write to
         * @param previousEncodedField The previous <code>EncodedField</code> in the list containing this
         * <code>EncodedField</code>.
         */
        private void writeTo(AnnotatedOutput out, EncodedField previousEncodedField) {
            int previousIndex = previousEncodedField==null?0:previousEncodedField.field.getIndex();

            if (out.annotates()) {
                out.annotate("field: " + field.getFieldString());
                out.writeUnsignedLeb128(field.getIndex() - previousIndex);
                out.annotate("access_flags: " + AccessFlags.formatAccessFlagsForField(accessFlags));
                out.writeUnsignedLeb128(accessFlags);
            }else {
                out.writeUnsignedLeb128(field.getIndex() - previousIndex);
                out.writeUnsignedLeb128(accessFlags);
            }
        }

        /**
         * Calculates the size of this <code>EncodedField</code> and returns the offset
         * immediately following it
         * @param offset the offset of this <code>EncodedField</code> in the <code>DexFile</code>
         * @param previousEncodedField The previous <code>EncodedField</code> in the list containing this
         * <code>EncodedField</code>.
         * @return the offset immediately following this <code>EncodedField</code>
         */
        private int place(int offset, EncodedField previousEncodedField) {
            int previousIndex = previousEncodedField==null?0:previousEncodedField.field.getIndex();

            offset += Leb128Utils.unsignedLeb128Size(field.getIndex() - previousIndex);
            offset += Leb128Utils.unsignedLeb128Size(accessFlags);
            return  offset;
        }

        /**
         * Compares this <code>EncodedField</code> to another, based on the comparison of the associated
         * <code>FieldIdItem</code>
         * @param other The <code>EncodedField</code> to compare against
         * @return a standard integer comparison value indicating the relationship
         */
        public int compareTo(EncodedField other)
        {
            return field.compareTo(other.field);
        }

        /**
         * Determines if this <code>EncodedField</code> is equal to other, based on the equality of the associated
         * <code>FieldIdItem</code>
         * @param other The <code>EncodedField</code> to test for equality
         * @return true if other is equal to this instance, otherwise false
         */
        public boolean equals(Object other) {
            if (other instanceof EncodedField) {
                return compareTo((EncodedField)other) == 0;
            }
            return false;
        }

        /**
         * @return true if this is a static field
         */
        public boolean isStatic() {
            return (accessFlags & AccessFlags.STATIC.getValue()) != 0;
        }
    }

    public static class EncodedMethod implements Comparable<EncodedMethod> {
        /**
         * The <code>MethodIdItem</code> that this <code>EncodedMethod</code> is associated with
         */
        public final MethodIdItem method;

        /**
         * The access flags for this method
         */
        public final int accessFlags;

        /**
         * The <code>CodeItem</code> containing the code for this method, or null if there is no code for this method
         * (i.e. an abstract method)
         */
        public final CodeItem codeItem;

        /**
         * Constructs a new <code>EncodedMethod</code> with the given values
         * @param method The <code>MethodIdItem</code> that this <code>EncodedMethod</code> is associated with
         * @param accessFlags The access flags for this method
         * @param codeItem The <code>CodeItem</code> containing the code for this method, or null if there is no code
         * for this method (i.e. an abstract method)
         */
        public EncodedMethod(MethodIdItem method, int accessFlags, CodeItem codeItem) {
            this.method = method;
            this.accessFlags = accessFlags;
            this.codeItem = codeItem;
            if (codeItem != null) {
                codeItem.setParent(this);
            }
        }

        /**
         * This is used internally to construct a new <code>EncodedMethod</code> while reading in a <code>DexFile</code>
         * @param dexFile The <code>DexFile</code> that is being read in
         * @param readContext a <code>ReadContext</code> object to hold information that is only needed while reading
         * in a file
         * @param in the Input object to read the <code>EncodedMethod</code> from
         * @param previousEncodedMethod The previous <code>EncodedMethod</code> in the list containing this
         * <code>EncodedMethod</code>.
         */
        public EncodedMethod(DexFile dexFile, ReadContext readContext, Input in, EncodedMethod previousEncodedMethod) {
            int previousIndex = previousEncodedMethod==null?0:previousEncodedMethod.method.getIndex();
            method = dexFile.MethodIdsSection.getItemByIndex(in.readUnsignedLeb128() + previousIndex);
            accessFlags = in.readUnsignedLeb128();
            if (dexFile.skipInstructions()) {
                in.readUnsignedLeb128();
                codeItem = null;
            } else {
                codeItem = (CodeItem)readContext.getOptionalOffsettedItemByOffset(ItemType.TYPE_CODE_ITEM,
                        in.readUnsignedLeb128());
            }
            if (codeItem != null) {
                codeItem.setParent(this);
            }
        }

        /**
         * Writes the <code>EncodedMethod</code> to the given <code>AnnotatedOutput</code> object
         * @param out the <code>AnnotatedOutput</code> object to write to
         * @param previousEncodedMethod The previous <code>EncodedMethod</code> in the list containing this
         * <code>EncodedMethod</code>.
         */
        private void writeTo(AnnotatedOutput out, EncodedMethod previousEncodedMethod) {
            int previousIndex = previousEncodedMethod==null?0:previousEncodedMethod.method.getIndex();

            if (out.annotates()) {
                out.annotate("method: " + method.getMethodString());
                out.writeUnsignedLeb128(method.getIndex() - previousIndex);
                out.annotate("access_flags: " + AccessFlags.formatAccessFlagsForMethod(accessFlags));
                out.writeUnsignedLeb128(accessFlags);
                if (codeItem != null) {
                    out.annotate("code_off: 0x" + Integer.toHexString(codeItem.getOffset()));
                    out.writeUnsignedLeb128(codeItem.getOffset());
                } else {
                    out.annotate("code_off: 0x0");
                    out.writeUnsignedLeb128(0);
                }
            }else {
                out.writeUnsignedLeb128(method.getIndex() - previousIndex);
                out.writeUnsignedLeb128(accessFlags);
                out.writeUnsignedLeb128(codeItem==null?0:codeItem.getOffset());
            }
        }

        /**
         * Calculates the size of this <code>EncodedMethod</code> and returns the offset
         * immediately following it
         * @param offset the offset of this <code>EncodedMethod</code> in the <code>DexFile</code>
         * @param previousEncodedMethod The previous <code>EncodedMethod</code> in the list containing this
         * <code>EncodedMethod</code>.
         * @return the offset immediately following this <code>EncodedField</code>
         */
        private int place(int offset, EncodedMethod previousEncodedMethod) {
            int previousIndex = previousEncodedMethod==null?0:previousEncodedMethod.method.getIndex();

            offset += Leb128Utils.unsignedLeb128Size(method.getIndex() - previousIndex);
            offset += Leb128Utils.unsignedLeb128Size(accessFlags);
            offset += codeItem==null?1:Leb128Utils.unsignedLeb128Size(codeItem.getOffset());
            return  offset;
        }

        /**
         * Compares this <code>EncodedMethod</code> to another, based on the comparison of the associated
         * <code>MethodIdItem</code>
         * @param other The <code>EncodedMethod</code> to compare against
         * @return a standard integer comparison value indicating the relationship
         */
        public int compareTo(EncodedMethod other) {
            return method.compareTo(other.method);
        }

        /**
         * Determines if this <code>EncodedMethod</code> is equal to other, based on the equality of the associated
         * <code>MethodIdItem</code>
         * @param other The <code>EncodedMethod</code> to test for equality
         * @return true if other is equal to this instance, otherwise false
         */
        public boolean equals(Object other) {
            if (other instanceof EncodedMethod) {
                return compareTo((EncodedMethod)other) == 0;
            }
            return false;
        }

        /**
         * @return true if this is a direct method
         */
        public boolean isDirect() {
            return ((accessFlags & (AccessFlags.STATIC.getValue() | AccessFlags.PRIVATE.getValue() |
                    AccessFlags.CONSTRUCTOR.getValue())) != 0);
        }
    }
}
