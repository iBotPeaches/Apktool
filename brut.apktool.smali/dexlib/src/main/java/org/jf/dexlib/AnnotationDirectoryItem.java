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
import org.jf.dexlib.Util.AnnotatedOutput;
import org.jf.dexlib.Util.ExceptionWithContext;
import org.jf.dexlib.Util.Input;
import org.jf.dexlib.Util.ReadOnlyArrayList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class AnnotationDirectoryItem extends Item<AnnotationDirectoryItem> {
    @Nullable
    private AnnotationSetItem classAnnotations;
    @Nullable
    private FieldAnnotation[] fieldAnnotations;
    @Nullable
    private MethodAnnotation[] methodAnnotations;
    @Nullable
    private ParameterAnnotation[] parameterAnnotations;

    /**
     * Creates a new uninitialized <code>AnnotationDirectoryItem</code>
     * @param dexFile The <code>DexFile</code> that this item belongs to
     */
    protected AnnotationDirectoryItem(DexFile dexFile) {
        super(dexFile);
    }

    /**
     * Creates a new <code>AnnotationDirectoryItem</code> with the given values
     * @param dexFile The <code>DexFile</code> that this item belongs to
     * @param classAnnotations The annotations associated with the overall class
     * @param fieldAnnotations A list of <code>FieldAnnotation</code> objects that contain the field annotations for
     * this class
     * @param methodAnnotations A list of <code>MethodAnnotation</code> objects that contain the method annotations for
     * this class
     * @param parameterAnnotations A list of <code>ParameterAnnotation</code> objects that contain the parameter
     * annotations for the methods in this class
     */
    private AnnotationDirectoryItem(DexFile dexFile, @Nullable AnnotationSetItem classAnnotations,
                                    @Nullable List<FieldAnnotation> fieldAnnotations,
                                    @Nullable List<MethodAnnotation> methodAnnotations,
                                    @Nullable List<ParameterAnnotation> parameterAnnotations) {
        super(dexFile);
        this.classAnnotations = classAnnotations;

        if (fieldAnnotations == null || fieldAnnotations.size() == 0) {
            this.fieldAnnotations = null;
        } else {
            this.fieldAnnotations = new FieldAnnotation[fieldAnnotations.size()];
            this.fieldAnnotations = fieldAnnotations.toArray(this.fieldAnnotations);
            Arrays.sort(this.fieldAnnotations);
        }

        if (methodAnnotations == null || methodAnnotations.size() == 0) {
            this.methodAnnotations = null;
        } else {
            this.methodAnnotations = new MethodAnnotation[methodAnnotations.size()];
            this.methodAnnotations = methodAnnotations.toArray(this.methodAnnotations);
            Arrays.sort(this.methodAnnotations);
        }

        if (parameterAnnotations == null || parameterAnnotations.size() == 0) {
            this.parameterAnnotations = null;
        } else {
            this.parameterAnnotations = new ParameterAnnotation[parameterAnnotations.size()];
            this.parameterAnnotations = parameterAnnotations.toArray(this.parameterAnnotations);
            Arrays.sort(this.parameterAnnotations);
        }
    }

    /**
     * Returns an <code>AnnotationDirectoryItem</code> for the given values, and that has been interned into the given
     * <code>DexFile</code>
     * @param dexFile The <code>DexFile</code> that this item belongs to
     * @param classAnnotations The annotations associated with the class
     * @param fieldAnnotations A list of <code>FieldAnnotation</code> objects containing the field annotations
     * @param methodAnnotations A list of <code>MethodAnnotation</code> objects containing the method annotations
     * @param parameterAnnotations A list of <code>ParameterAnnotation</code> objects containin the parameter
     * annotations
     * @return an <code>AnnotationItem</code> for the given values, and that has been interned into the given
     * <code>DexFile</code>
     */
    public static AnnotationDirectoryItem internAnnotationDirectoryItem(DexFile dexFile,
                                    AnnotationSetItem classAnnotations,
                                    List<FieldAnnotation> fieldAnnotations,
                                    List<MethodAnnotation> methodAnnotations,
                                    List<ParameterAnnotation> parameterAnnotations) {
        AnnotationDirectoryItem annotationDirectoryItem = new AnnotationDirectoryItem(dexFile, classAnnotations,
                fieldAnnotations, methodAnnotations, parameterAnnotations);
        return dexFile.AnnotationDirectoriesSection.intern(annotationDirectoryItem);
    }

    /** {@inheritDoc} */
    protected void readItem(Input in, ReadContext readContext) {
        classAnnotations = (AnnotationSetItem)readContext.getOptionalOffsettedItemByOffset(
                ItemType.TYPE_ANNOTATION_SET_ITEM, in.readInt());

        int fieldAnnotationCount = in.readInt();
        if (fieldAnnotationCount > 0) {
            fieldAnnotations = new FieldAnnotation[fieldAnnotationCount];
        } else {
            fieldAnnotations = null;
        }

        int methodAnnotationCount = in.readInt();
        if (methodAnnotationCount > 0) {
            methodAnnotations = new MethodAnnotation[methodAnnotationCount];
        } else {
            methodAnnotations = null;
        }

        int parameterAnnotationCount = in.readInt();
        if (parameterAnnotationCount > 0) {
            parameterAnnotations = new ParameterAnnotation[parameterAnnotationCount];
        } else {
            parameterAnnotations = null;
        }

        if (fieldAnnotations != null) {
            for (int i=0; i<fieldAnnotations.length; i++) {
                try {
                    FieldIdItem fieldIdItem = dexFile.FieldIdsSection.getItemByIndex(in.readInt());
                    AnnotationSetItem fieldAnnotationSet = (AnnotationSetItem)readContext.getOffsettedItemByOffset(
                            ItemType.TYPE_ANNOTATION_SET_ITEM, in.readInt());
                    fieldAnnotations[i] = new FieldAnnotation(fieldIdItem, fieldAnnotationSet);
                } catch (Exception ex) {
                    throw ExceptionWithContext.withContext(ex,
                            "Error occured while reading FieldAnnotation at index " + i);
                }
            }
        }

        if (methodAnnotations != null) {
            for (int i=0; i<methodAnnotations.length; i++) {
                try {
                    MethodIdItem methodIdItem = dexFile.MethodIdsSection.getItemByIndex(in.readInt());
                    AnnotationSetItem methodAnnotationSet = (AnnotationSetItem)readContext.getOffsettedItemByOffset(
                            ItemType.TYPE_ANNOTATION_SET_ITEM, in.readInt());
                    methodAnnotations[i] = new MethodAnnotation(methodIdItem, methodAnnotationSet);
                } catch (Exception ex) {
                    throw ExceptionWithContext.withContext(ex,
                            "Error occured while reading MethodAnnotation at index " + i);
                }
            }
        }

        if (parameterAnnotations != null) {
            for (int i=0; i<parameterAnnotations.length; i++) {
                try {
                    MethodIdItem methodIdItem = dexFile.MethodIdsSection.getItemByIndex(in.readInt());
                    AnnotationSetRefList paramaterAnnotationSet = (AnnotationSetRefList)readContext.getOffsettedItemByOffset(
                            ItemType.TYPE_ANNOTATION_SET_REF_LIST, in.readInt());
                    parameterAnnotations[i] = new ParameterAnnotation(methodIdItem, paramaterAnnotationSet);
                } catch (Exception ex) {
                    throw ExceptionWithContext.withContext(ex,
                            "Error occured while reading ParameterAnnotation at index " + i);
                }
            }
        }
    }

    /** {@inheritDoc} */
    protected int placeItem(int offset) {
        return offset + 16 + (
                (fieldAnnotations==null?0:fieldAnnotations.length) +
                (methodAnnotations==null?0:methodAnnotations.length) +
                (parameterAnnotations==null?0:parameterAnnotations.length)) * 8;
    }

    /** {@inheritDoc} */
    protected void writeItem(AnnotatedOutput out) {
        if (out.annotates()) {
            TypeIdItem parentType = getParentType();
            if (parentType != null) {
                out.annotate(0, parentType.getTypeDescriptor());
            }
            if (classAnnotations != null) {
                out.annotate(4, "class_annotations_off: 0x" + Integer.toHexString(classAnnotations.getOffset()));
            } else {
                out.annotate(4, "class_annotations_off:");
            }

            int length = fieldAnnotations==null?0:fieldAnnotations.length;
            out.annotate(4, "annotated_fields_size: 0x" + Integer.toHexString(length) + " (" +
                    length + ")");
            length = methodAnnotations==null?0:methodAnnotations.length;
            out.annotate(4, "annotated_methods_size: 0x" + Integer.toHexString(length) + " (" +
                    length + ")");
            length = parameterAnnotations==null?0:parameterAnnotations.length;
            out.annotate(4, "annotated_parameters_size: 0x" + Integer.toHexString(length) + " (" +
                    length + ")");

            int index;
            if (fieldAnnotations != null) {
               index = 0;
                for (FieldAnnotation fieldAnnotation: fieldAnnotations) {
                    out.annotate(0, "[" + index++ + "] field_annotation");

                    out.indent();
                    out.annotate(4, "field: " + fieldAnnotation.field.getFieldName().getStringValue() + ":" +
                            fieldAnnotation.field.getFieldType().getTypeDescriptor());
                    out.annotate(4, "annotations_off: 0x" +
                            Integer.toHexString(fieldAnnotation.annotationSet.getOffset()));
                    out.deindent();
                }
            }

            if (methodAnnotations != null) {
                index = 0;
                for (MethodAnnotation methodAnnotation: methodAnnotations) {
                    out.annotate(0, "[" + index++ + "] method_annotation");
                    out.indent();
                    out.annotate(4, "method: " + methodAnnotation.method.getMethodString());
                    out.annotate(4, "annotations_off: 0x" +
                            Integer.toHexString(methodAnnotation.annotationSet.getOffset()));
                    out.deindent();
                }
            }

            if (parameterAnnotations != null) {
                index = 0;
                for (ParameterAnnotation parameterAnnotation: parameterAnnotations) {
                    out.annotate(0, "[" + index++ + "] parameter_annotation");
                    out.indent();
                    out.annotate(4, "method: " + parameterAnnotation.method.getMethodString());
                    out.annotate(4, "annotations_off: 0x" +
                            Integer.toHexString(parameterAnnotation.annotationSet.getOffset()));
                }
            }
        }

        out.writeInt(classAnnotations==null?0:classAnnotations.getOffset());
        out.writeInt(fieldAnnotations==null?0:fieldAnnotations.length);
        out.writeInt(methodAnnotations==null?0:methodAnnotations.length);
        out.writeInt(parameterAnnotations==null?0:parameterAnnotations.length);

        if (fieldAnnotations != null) {
            for (FieldAnnotation fieldAnnotation: fieldAnnotations) {
                out.writeInt(fieldAnnotation.field.getIndex());
                out.writeInt(fieldAnnotation.annotationSet.getOffset());
            }
        }

        if (methodAnnotations != null) {
            for (MethodAnnotation methodAnnotation: methodAnnotations) {
                out.writeInt(methodAnnotation.method.getIndex());
                out.writeInt(methodAnnotation.annotationSet.getOffset());
            }
        }

        if (parameterAnnotations != null) {
            for (ParameterAnnotation parameterAnnotation: parameterAnnotations) {
                out.writeInt(parameterAnnotation.method.getIndex());
                out.writeInt(parameterAnnotation.annotationSet.getOffset());
            }
        }
    }

    /** {@inheritDoc} */
    public ItemType getItemType() {
        return ItemType.TYPE_ANNOTATIONS_DIRECTORY_ITEM;
    }

    /** {@inheritDoc} */
    public String getConciseIdentity() {
        TypeIdItem parentType = getParentType();
        if (parentType == null) {
            return "annotation_directory_item @0x" + Integer.toHexString(getOffset());
        }
        return "annotation_directory_item @0x" + Integer.toHexString(getOffset()) +
               " (" + parentType.getTypeDescriptor() + ")";
    }

    /** {@inheritDoc} */
    public int compareTo(AnnotationDirectoryItem o) {
        Preconditions.checkNotNull(o);

        TypeIdItem parentType = getParentType();
        TypeIdItem otherParentType = o.getParentType();
        if (parentType != null) {
            if (otherParentType != null) {
                return parentType.compareTo(otherParentType);
            }
            return 1;
        }
        if (otherParentType != null) {
            return -1;
        }

        if (classAnnotations != null) {
            if (o.classAnnotations != null) {
                return classAnnotations.compareTo(o.classAnnotations);
            }
            return 1;
        }
        return -1;
    }

    /**
     * Returns the parent type for an AnnotationDirectoryItem that is guaranteed to have a single parent, or null
     * for one that may be referenced by multiple classes.
     *
     * Specifically, the AnnotationDirectoryItem may be referenced by multiple classes if it has only class annotations,
     * but not field/method/parameter annotations.
     *
     * @return The parent type for this AnnotationDirectoryItem, or null if it may have multiple parents
     */
    @Nullable
    public TypeIdItem getParentType() {
        if (fieldAnnotations != null && fieldAnnotations.length > 0) {
            return fieldAnnotations[0].field.getContainingClass();
        }
        if (methodAnnotations != null && methodAnnotations.length > 0) {
            return methodAnnotations[0].method.getContainingClass();
        }
        if (parameterAnnotations != null && parameterAnnotations.length > 0) {
            return parameterAnnotations[0].method.getContainingClass();
        }
        return null;
    }

    /**
     * @return An <code>AnnotationSetItem</code> containing the annotations associated with this class, or null
     * if there are no class annotations
     */
    @Nullable
    public AnnotationSetItem getClassAnnotations() {
        return classAnnotations;
    }

    /**
     * Get a list of the field annotations in this <code>AnnotationDirectoryItem</code>
     * @return A list of FieldAnnotation objects, or null if there are no field annotations
     */
    @Nonnull
    public List<FieldAnnotation> getFieldAnnotations() {
        if (fieldAnnotations == null) {
            return Collections.emptyList();
        }
        return ReadOnlyArrayList.of(fieldAnnotations);
    }

    /**
     * Get a list of the method annotations in this <code>AnnotationDirectoryItem</code>
     * @return A list of MethodAnnotation objects, or null if there are no method annotations
     */
    @Nonnull
    public List<MethodAnnotation> getMethodAnnotations() {
        if (methodAnnotations == null) {
            return Collections.emptyList();
        }
        return ReadOnlyArrayList.of(methodAnnotations);
    }

    /**
     * Get a list of the parameter annotations in this <code>AnnotationDirectoryItem</code>
     * @return A list of ParameterAnnotation objects, or null if there are no parameter annotations
     */
    @Nonnull
    public List<ParameterAnnotation> getParameterAnnotations() {
        if (parameterAnnotations == null) {
            return Collections.emptyList();
        }
        return ReadOnlyArrayList.of(parameterAnnotations);
    }

    /**
     * Gets the field annotations for the given field, or null if no annotations are defined for that field
     * @param fieldIdItem The field to get the annotations for
     * @return An <code>AnnotationSetItem</code> containing the field annotations, or null if none are found
     */
    @Nullable
    public AnnotationSetItem getFieldAnnotations(FieldIdItem fieldIdItem) {
        if (fieldAnnotations == null) {
            return null;
        }
        int index = Arrays.binarySearch(fieldAnnotations, fieldIdItem);
        if (index < 0) {
            return null;
        }
        return fieldAnnotations[index].annotationSet;
    }

    /**
     * Gets the method annotations for the given method, or null if no annotations are defined for that method
     * @param methodIdItem The method to get the annotations for
     * @return An <code>AnnotationSetItem</code> containing the method annotations, or null if none are found
     */
    @Nullable
    public AnnotationSetItem getMethodAnnotations(MethodIdItem methodIdItem) {
        if (methodAnnotations == null) {
            return null;
        }
        int index = Arrays.binarySearch(methodAnnotations, methodIdItem);
        if (index < 0) {
            return null;
        }
        return methodAnnotations[index].annotationSet;
    }

    /**
     * Gets the parameter annotations for the given method, or null if no parameter annotations are defined for that
     * method
     * @param methodIdItem The method to get the parameter annotations for
     * @return An <code>AnnotationSetRefList</code> containing the parameter annotations, or null if none are found
     */
    @Nullable
    public AnnotationSetRefList getParameterAnnotations(MethodIdItem methodIdItem) {
        if (parameterAnnotations == null) {
            return null;
        }
        int index = Arrays.binarySearch(parameterAnnotations, methodIdItem);
        if (index < 0) {
            return null;
        }
        return parameterAnnotations[index].annotationSet;
    }

    /**
     *
     */
    public int getClassAnnotationCount() {
        if (classAnnotations == null) {
            return 0;
        }
        AnnotationItem[] annotations = classAnnotations.getAnnotations();
        return annotations.length;
    }

    /**
     * @return The number of field annotations in this <code>AnnotationDirectoryItem</code>
     */
    public int getFieldAnnotationCount() {
        if (fieldAnnotations == null) {
            return 0;
        }
        return fieldAnnotations.length;
    }

    /**
     * @return The number of method annotations in this <code>AnnotationDirectoryItem</code>
     */
    public int getMethodAnnotationCount() {
        if (methodAnnotations == null) {
            return 0;
        }
        return methodAnnotations.length;
    }

    /**
     * @return The number of parameter annotations in this <code>AnnotationDirectoryItem</code>
     */
    public int getParameterAnnotationCount() {
        if (parameterAnnotations == null) {
            return 0;
        }
        return parameterAnnotations.length;
    }

    @Override
    public int hashCode() {
        // If the item has a single parent, we can use the re-use the identity (hash) of that parent
        TypeIdItem parentType = getParentType();
        if (parentType != null) {
            return parentType.hashCode();
        }
        if (classAnnotations != null) {
            return classAnnotations.hashCode();
        }
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this==o) {
            return true;
        }
        if (o==null || !this.getClass().equals(o.getClass())) {
            return false;
        }

        AnnotationDirectoryItem other = (AnnotationDirectoryItem)o;
        return (this.compareTo(other) == 0);
    }

    public static class FieldAnnotation implements Comparable<Convertible<FieldIdItem>>, Convertible<FieldIdItem> {
        public final FieldIdItem field;
        public final AnnotationSetItem annotationSet;

        public FieldAnnotation(FieldIdItem field, AnnotationSetItem annotationSet) {
            this.field = field;
            this.annotationSet = annotationSet;
        }

        public int compareTo(Convertible<FieldIdItem> other) {
            return field.compareTo(other.convert());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            return compareTo((FieldAnnotation)o) == 0;
        }

        @Override
        public int hashCode() {
            return field.hashCode() + 31 * annotationSet.hashCode();
        }

        public FieldIdItem convert() {
            return field;
        }
    }

    public static class MethodAnnotation implements Comparable<Convertible<MethodIdItem>>, Convertible<MethodIdItem> {
        public final MethodIdItem method;
        public final AnnotationSetItem annotationSet;

        public MethodAnnotation(MethodIdItem method, AnnotationSetItem annotationSet) {
            this.method = method;
            this.annotationSet = annotationSet;
        }

        public int compareTo(Convertible<MethodIdItem> other) {
            return method.compareTo(other.convert());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            return compareTo((MethodAnnotation)o) == 0;
        }

        @Override
        public int hashCode() {
            return method.hashCode() + 31 * annotationSet.hashCode();
        }

        public MethodIdItem convert() {
            return method;
        }
    }

    public static class ParameterAnnotation implements Comparable<Convertible<MethodIdItem>>,
            Convertible<MethodIdItem> {
        public final MethodIdItem method;
        public final AnnotationSetRefList annotationSet;

        public ParameterAnnotation(MethodIdItem method, AnnotationSetRefList annotationSet) {
            this.method = method;
            this.annotationSet = annotationSet;
        }

        public int compareTo(Convertible<MethodIdItem> other) {
            return method.compareTo(other.convert());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            return compareTo((ParameterAnnotation)o) == 0;
        }

        @Override
        public int hashCode() {
            return method.hashCode() + 31 * annotationSet.hashCode();
        }

        public MethodIdItem convert() {
            return method;
        }
    }
}
