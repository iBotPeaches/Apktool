/*
 * Copyright 2012, Google Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 *     * Neither the name of Google Inc. nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.jf.dexlib2.dexbacked;

import com.google.common.collect.ImmutableSet;
import org.jf.dexlib2.HiddenApiRestriction;
import org.jf.dexlib2.base.reference.BaseFieldReference;
import org.jf.dexlib2.dexbacked.raw.FieldIdItem;
import org.jf.dexlib2.dexbacked.reference.DexBackedFieldReference;
import org.jf.dexlib2.dexbacked.util.AnnotationsDirectory;
import org.jf.dexlib2.dexbacked.util.EncodedArrayItemIterator;
import org.jf.dexlib2.dexbacked.value.DexBackedEncodedValue;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.Field;
import org.jf.dexlib2.iface.value.EncodedValue;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.Set;

public class DexBackedField extends BaseFieldReference implements Field {
    @Nonnull public final DexBackedDexFile dexFile;
    @Nonnull public final ClassDef classDef;

    public final int accessFlags;
    @Nullable public final EncodedValue initialValue;
    public final int annotationSetOffset;

    public final int fieldIndex;
    private final int startOffset;
    private final int initialValueOffset;
    private final int hiddenApiRestrictions;

    private int fieldIdItemOffset;

    public DexBackedField(@Nonnull DexBackedDexFile dexFile,
                          @Nonnull DexReader reader,
                          @Nonnull DexBackedClassDef classDef,
                          int previousFieldIndex,
                          @Nonnull EncodedArrayItemIterator staticInitialValueIterator,
                          @Nonnull AnnotationsDirectory.AnnotationIterator annotationIterator,
                          int hiddenApiRestrictions) {
        this.dexFile = dexFile;
        this.classDef = classDef;

        // large values may be used for the index delta, which cause the cumulative index to overflow upon
        // addition, effectively allowing out of order entries.
        startOffset = reader.getOffset();
        int fieldIndexDiff = reader.readLargeUleb128();
        this.fieldIndex = fieldIndexDiff + previousFieldIndex;
        this.accessFlags = reader.readSmallUleb128();

        this.annotationSetOffset = annotationIterator.seekTo(fieldIndex);
        initialValueOffset = staticInitialValueIterator.getReaderOffset();
        this.initialValue = staticInitialValueIterator.getNextOrNull();
        this.hiddenApiRestrictions = hiddenApiRestrictions;
    }

    public DexBackedField(@Nonnull DexBackedDexFile dexFile,
                          @Nonnull DexReader reader,
                          @Nonnull DexBackedClassDef classDef,
                          int previousFieldIndex,
                          @Nonnull AnnotationsDirectory.AnnotationIterator annotationIterator,
                          int hiddenApiRestrictions) {
        this.dexFile = dexFile;
        this.classDef = classDef;

        // large values may be used for the index delta, which cause the cumulative index to overflow upon
        // addition, effectively allowing out of order entries.
        startOffset = reader.getOffset();
        int fieldIndexDiff = reader.readLargeUleb128();
        this.fieldIndex = fieldIndexDiff + previousFieldIndex;
        this.accessFlags = reader.readSmallUleb128();

        this.annotationSetOffset = annotationIterator.seekTo(fieldIndex);
        initialValueOffset = 0;
        this.initialValue = null;
        this.hiddenApiRestrictions = hiddenApiRestrictions;
    }

    @Nonnull
    @Override
    public String getName() {
        return dexFile.getStringSection().get(
                dexFile.getBuffer().readSmallUint(getFieldIdItemOffset() + FieldIdItem.NAME_OFFSET));
    }

    @Nonnull
    @Override
    public String getType() {
        return dexFile.getTypeSection().get(
                dexFile.getBuffer().readUshort(getFieldIdItemOffset() + FieldIdItem.TYPE_OFFSET));
    }

    @Nonnull @Override public String getDefiningClass() { return classDef.getType(); }
    @Override public int getAccessFlags() { return accessFlags; }
    @Nullable @Override public EncodedValue getInitialValue() { return initialValue; }

    @Nonnull
    @Override
    public Set<? extends DexBackedAnnotation> getAnnotations() {
        return AnnotationsDirectory.getAnnotations(dexFile, annotationSetOffset);
    }

    @Nonnull
    @Override
    public Set<HiddenApiRestriction> getHiddenApiRestrictions() {
        if (hiddenApiRestrictions == DexBackedClassDef.NO_HIDDEN_API_RESTRICTIONS) {
            return ImmutableSet.of();
        } else {
            return EnumSet.copyOf(HiddenApiRestriction.getAllFlags(hiddenApiRestrictions));
        }
    }

    /**
     * Skips the reader over the specified number of encoded_field structures
     *
     * @param reader The reader to skip
     * @param count The number of encoded_field structures to skip over
     */
    public static void skipFields(@Nonnull DexReader reader, int count) {
        for (int i=0; i<count; i++) {
            reader.skipUleb128();
            reader.skipUleb128();
        }
    }

    private int getFieldIdItemOffset() {
        if (fieldIdItemOffset == 0) {
            fieldIdItemOffset = dexFile.getFieldSection().getOffset(fieldIndex);
        }
        return fieldIdItemOffset;
    }

    /**
     * Calculate and return the private size of a field definition.
     *
     * Calculated as: field_idx_diff + access_flags + annotations overhead +
     * initial value size + field reference size
     *
     * @return size in bytes
     */
    public int getSize() {
        int size = 0;
        DexReader reader = dexFile.getBuffer().readerAt(startOffset);
        reader.readLargeUleb128(); //field_idx_diff
        reader.readSmallUleb128(); //access_flags
        size += reader.getOffset() - startOffset;

        Set<? extends DexBackedAnnotation> annotations = getAnnotations();
        if (!annotations.isEmpty()) {
            size += 2 * 4; //2 * uint overhead from field_annotation
        }

        if (initialValueOffset > 0) {
            reader.setOffset(initialValueOffset);
            if (initialValue != null) {
                DexBackedEncodedValue.skipFrom(reader);
                size += reader.getOffset() - initialValueOffset;
            }
        }

        DexBackedFieldReference fieldRef = new DexBackedFieldReference(dexFile, fieldIndex);
        size += fieldRef.getSize();

        return size;
    }
}
