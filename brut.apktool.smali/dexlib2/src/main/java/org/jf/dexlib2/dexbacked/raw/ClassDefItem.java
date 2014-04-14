/*
 * Copyright 2013, Google Inc.
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

package org.jf.dexlib2.dexbacked.raw;

import com.google.common.base.Joiner;
import org.jf.dexlib2.AccessFlags;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.dexbacked.raw.util.DexAnnotator;
import org.jf.dexlib2.util.AnnotatedBytes;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ClassDefItem {
    public static final int ITEM_SIZE = 32;

    public static final int CLASS_OFFSET = 0;
    public static final int ACCESS_FLAGS_OFFSET = 4;
    public static final int SUPERCLASS_OFFSET = 8;
    public static final int INTERFACES_OFFSET = 12;
    public static final int SOURCE_FILE_OFFSET = 16;
    public static final int ANNOTATIONS_OFFSET = 20;
    public static final int CLASS_DATA_OFFSET = 24;
    public static final int STATIC_VALUES_OFFSET = 28;

    @Nonnull
    public static SectionAnnotator makeAnnotator(@Nonnull DexAnnotator annotator, @Nonnull MapItem mapItem) {
        return new SectionAnnotator(annotator, mapItem) {
            private SectionAnnotator classDataAnnotator = null;

            @Override public void annotateSection(@Nonnull AnnotatedBytes out) {
                classDataAnnotator = annotator.getAnnotator(ItemType.CLASS_DATA_ITEM);
                super.annotateSection(out);
            }

            @Nonnull @Override public String getItemName() {
                return "class_def_item";
            }

            @Override
            protected void annotateItem(@Nonnull AnnotatedBytes out, int itemIndex, @Nullable String itemIdentity) {
                int classIndex = dexFile.readSmallUint(out.getCursor());
                out.annotate(4, "class_idx = %s", TypeIdItem.getReferenceAnnotation(dexFile, classIndex));

                int accessFlags = dexFile.readInt(out.getCursor());
                out.annotate(4, "access_flags = 0x%x: %s", accessFlags,
                        Joiner.on('|').join(AccessFlags.getAccessFlagsForClass(accessFlags)));

                int superclassIndex = dexFile.readOptionalUint(out.getCursor());
                out.annotate(4, "superclass_idx = %s",
                        TypeIdItem.getOptionalReferenceAnnotation(dexFile, superclassIndex));

                int interfacesOffset = dexFile.readSmallUint(out.getCursor());
                out.annotate(4, "interfaces_off = %s", TypeListItem.getReferenceAnnotation(dexFile, interfacesOffset));

                int sourceFileIdx = dexFile.readOptionalUint(out.getCursor());
                out.annotate(4, "source_file_idx = %s", StringIdItem.getOptionalReferenceAnnotation(dexFile,
                        sourceFileIdx));

                int annotationsOffset = dexFile.readSmallUint(out.getCursor());
                if (annotationsOffset == 0) {
                    out.annotate(4, "annotations_off = annotations_directory_item[NO_OFFSET]");
                } else {
                    out.annotate(4, "annotations_off = annotations_directory_item[0x%x]", annotationsOffset);
                }

                int classDataOffset = dexFile.readSmallUint(out.getCursor());
                if (classDataOffset == 0) {
                    out.annotate(4, "class_data_off = class_data_item[NO_OFFSET]");
                } else {
                    out.annotate(4, "class_data_off = class_data_item[0x%x]", classDataOffset);
                    addClassDataIdentity(classDataOffset, dexFile.getType(classIndex));
                }

                int staticValuesOffset = dexFile.readSmallUint(out.getCursor());
                if (staticValuesOffset == 0) {
                    out.annotate(4, "static_values_off = encoded_array_item[NO_OFFSET]");
                } else {
                    out.annotate(4, "static_values_off = encoded_array_item[0x%x]", staticValuesOffset);
                }
            }

            private void addClassDataIdentity(int classDataOffset, String classType) {
                if (classDataAnnotator != null) {
                    classDataAnnotator.setItemIdentity(classDataOffset, classType);
                }
            }
        };
    }

    @Nonnull
    public static String asString(@Nonnull DexBackedDexFile dexFile, int classIndex) {
        int offset = dexFile.getClassDefItemOffset(classIndex);
        int typeIndex = dexFile.readSmallUint(offset + CLASS_OFFSET);
        return dexFile.getType(typeIndex);
    }

    public static String[] getClasses(@Nonnull RawDexFile dexFile) {
        MapItem mapItem = dexFile.getMapItemForSection(ItemType.CLASS_DEF_ITEM);
        if (mapItem == null) {
            return new String[0];
        }

        int classCount = mapItem.getItemCount();
        String[] ret = new String[classCount];
        for (int i=0; i<classCount; i++) {
            ret[i] = asString(dexFile, i);
        }
        return ret;
    }
}
