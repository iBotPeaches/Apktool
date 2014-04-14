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

import org.jf.dexlib2.dexbacked.raw.util.DexAnnotator;
import org.jf.dexlib2.util.AnnotatedBytes;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class AnnotationDirectoryItem {
    public static final int CLASS_ANNOTATIONS_OFFSET  = 0;
    public static final int FIELD_SIZE_OFFSET = 4;
    public static final int ANNOTATED_METHOD_SIZE_OFFSET = 8;
    public static final int ANNOTATED_PARAMETERS_SIZE = 12;

    @Nonnull
    public static SectionAnnotator makeAnnotator(@Nonnull DexAnnotator annotator, @Nonnull MapItem mapItem) {
        return new SectionAnnotator(annotator, mapItem) {
            @Nonnull @Override public String getItemName() {
                return "annotation_directory_item";
            }

            @Override public int getItemAlignment() {
                return 4;
            }

            @Override
            protected void annotateItem(@Nonnull AnnotatedBytes out, int itemIndex, @Nullable String itemIdentity) {
                int classAnnotationsOffset = dexFile.readSmallUint(out.getCursor());
                out.annotate(4, "class_annotations_off = %s",
                        AnnotationSetItem.getReferenceAnnotation(dexFile, classAnnotationsOffset));

                int fieldsSize = dexFile.readSmallUint(out.getCursor());
                out.annotate(4, "fields_size = %d", fieldsSize);

                int annotatedMethodsSize = dexFile.readSmallUint(out.getCursor());
                out.annotate(4, "annotated_methods_size = %d", annotatedMethodsSize);

                int annotatedParameterSize = dexFile.readSmallUint(out.getCursor());
                out.annotate(4, "annotated_parameters_size = %d", annotatedParameterSize);

                if (fieldsSize > 0) {
                    out.annotate(0, "field_annotations:");
                    out.indent();
                    for (int i=0; i<fieldsSize; i++) {
                        out.annotate(0, "field_annotation[%d]", i);
                        out.indent();
                        int fieldIndex = dexFile.readSmallUint(out.getCursor());
                        out.annotate(4, "%s", FieldIdItem.getReferenceAnnotation(dexFile, fieldIndex));
                        int annotationOffset = dexFile.readSmallUint(out.getCursor());
                        out.annotate(4, "%s", AnnotationSetItem.getReferenceAnnotation(dexFile, annotationOffset));
                        out.deindent();
                    }
                    out.deindent();
                }

                if (annotatedMethodsSize > 0) {
                    out.annotate(0, "method_annotations:");
                    out.indent();
                    for (int i=0; i<annotatedMethodsSize; i++) {
                        out.annotate(0, "method_annotation[%d]", i);
                        out.indent();
                        int methodIndex = dexFile.readSmallUint(out.getCursor());
                        out.annotate(4, "%s", MethodIdItem.getReferenceAnnotation(dexFile, methodIndex));
                        int annotationOffset = dexFile.readSmallUint(out.getCursor());
                        out.annotate(4, "%s", AnnotationSetItem.getReferenceAnnotation(dexFile, annotationOffset));
                        out.deindent();
                    }
                    out.deindent();
                }

                if (annotatedParameterSize > 0) {
                    out.annotate(0, "parameter_annotations:");
                    out.indent();
                    for (int i=0; i<annotatedParameterSize; i++) {
                        out.annotate(0, "parameter_annotation[%d]", i);
                        out.indent();
                        int methodIndex = dexFile.readSmallUint(out.getCursor());
                        out.annotate(4, "%s", MethodIdItem.getReferenceAnnotation(dexFile, methodIndex));
                        int annotationOffset = dexFile.readSmallUint(out.getCursor());
                        out.annotate(4, "%s", AnnotationSetRefList.getReferenceAnnotation(dexFile, annotationOffset));
                        out.deindent();
                    }
                    out.deindent();
                }
            }
        };
    }
}
