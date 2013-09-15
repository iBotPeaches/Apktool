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
import org.jf.dexlib2.dexbacked.DexReader;
import org.jf.dexlib2.dexbacked.raw.util.DexAnnotator;
import org.jf.dexlib2.util.AnnotatedBytes;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ClassDataItem {
    @Nonnull
    public static SectionAnnotator makeAnnotator(@Nonnull DexAnnotator annotator, @Nonnull MapItem mapItem) {
        return new SectionAnnotator(annotator, mapItem) {
            private SectionAnnotator codeItemAnnotator = null;

            @Override public void annotateSection(@Nonnull AnnotatedBytes out) {
                codeItemAnnotator = annotator.getAnnotator(ItemType.CODE_ITEM);
                super.annotateSection(out);
            }


            @Nonnull @Override public String getItemName() {
                return "class_data_item";
            }

            @Override
            protected void annotateItem(@Nonnull AnnotatedBytes out, int itemIndex, @Nullable String itemIdentity) {
                DexReader reader = dexFile.readerAt(out.getCursor());

                int staticFieldsSize = reader.readSmallUleb128();
                out.annotateTo(reader.getOffset(), "static_fields_size = %d", staticFieldsSize);

                int instanceFieldsSize = reader.readSmallUleb128();
                out.annotateTo(reader.getOffset(), "instance_fields_size = %d", instanceFieldsSize);

                int directMethodsSize = reader.readSmallUleb128();
                out.annotateTo(reader.getOffset(), "direct_methods_size = %d", directMethodsSize);

                int virtualMethodsSize = reader.readSmallUleb128();
                out.annotateTo(reader.getOffset(), "virtual_methods_size = %d", virtualMethodsSize);

                int previousIndex = 0;
                if (staticFieldsSize > 0) {
                    out.annotate(0, "static_fields:");
                    out.indent();
                    for (int i=0; i<staticFieldsSize; i++) {
                        out.annotate(0, "static_field[%d]", i);
                        out.indent();
                        previousIndex = annotateEncodedField(out, dexFile, reader, previousIndex);
                        out.deindent();
                    }
                    out.deindent();
                }

                if (instanceFieldsSize > 0) {
                    out.annotate(0, "instance_fields:");
                    out.indent();
                    previousIndex = 0;
                    for (int i=0; i<instanceFieldsSize; i++) {
                        out.annotate(0, "instance_field[%d]", i);
                        out.indent();
                        previousIndex = annotateEncodedField(out, dexFile, reader, previousIndex);
                        out.deindent();
                    }
                    out.deindent();
                }

                if (directMethodsSize > 0) {
                    out.annotate(0, "direct_methods:");
                    out.indent();
                    previousIndex = 0;
                    for (int i=0; i<directMethodsSize; i++) {
                        out.annotate(0, "direct_method[%d]", i);
                        out.indent();
                        previousIndex = annotateEncodedMethod(out, dexFile, reader, previousIndex);
                        out.deindent();
                    }
                    out.deindent();
                }

                if (virtualMethodsSize > 0) {
                    out.annotate(0, "virtual_methods:");
                    out.indent();
                    previousIndex = 0;
                    for (int i=0; i<virtualMethodsSize; i++) {
                        out.annotate(0, "virtual_method[%d]", i);
                        out.indent();
                        previousIndex = annotateEncodedMethod(out, dexFile, reader, previousIndex);
                        out.deindent();
                    }
                    out.deindent();
                }
            }

            private int annotateEncodedField(@Nonnull AnnotatedBytes out, @Nonnull RawDexFile dexFile,
                                             @Nonnull DexReader reader, int previousIndex) {
                // large values may be used for the index delta, which cause the cumulative index to overflow upon
                // addition, effectively allowing out of order entries.
                int indexDelta = reader.readLargeUleb128();
                int fieldIndex = previousIndex + indexDelta;
                out.annotateTo(reader.getOffset(), "field_idx_diff = %d: %s", indexDelta,
                        FieldIdItem.getReferenceAnnotation(dexFile, fieldIndex));

                int accessFlags = reader.readSmallUleb128();
                out.annotateTo(reader.getOffset(), "access_flags = 0x%x: %s", accessFlags,
                        Joiner.on('|').join(AccessFlags.getAccessFlagsForField(accessFlags)));

                return fieldIndex;
            }

            private int annotateEncodedMethod(@Nonnull AnnotatedBytes out, @Nonnull RawDexFile dexFile,
                                              @Nonnull DexReader reader, int previousIndex) {
                // large values may be used for the index delta, which cause the cumulative index to overflow upon
                // addition, effectively allowing out of order entries.
                int indexDelta = reader.readLargeUleb128();
                int methodIndex = previousIndex + indexDelta;
                out.annotateTo(reader.getOffset(), "method_idx_diff = %d: %s", indexDelta,
                        MethodIdItem.getReferenceAnnotation(dexFile, methodIndex));

                int accessFlags = reader.readSmallUleb128();
                out.annotateTo(reader.getOffset(), "access_flags = 0x%x: %s", accessFlags,
                        Joiner.on('|').join(AccessFlags.getAccessFlagsForMethod(accessFlags)));

                int codeOffset = reader.readSmallUleb128();
                if (codeOffset == 0) {
                    out.annotateTo(reader.getOffset(), "code_off = code_item[NO_OFFSET]");
                } else {
                    out.annotateTo(reader.getOffset(), "code_off = code_item[0x%x]", codeOffset);
                    addCodeItemIdentity(codeOffset, MethodIdItem.asString(dexFile, methodIndex));
                }

                return methodIndex;
            }

            private void addCodeItemIdentity(int codeItemOffset, String methodString) {
                if (codeItemAnnotator != null) {
                    codeItemAnnotator.setItemIdentity(codeItemOffset, methodString);
                }
            }
        };
    }
}
