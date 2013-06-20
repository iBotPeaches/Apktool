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

import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.dexbacked.raw.util.DexAnnotator;
import org.jf.dexlib2.util.AnnotatedBytes;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class AnnotationSetItem {
    public static final int SIZE_OFFSET = 0;
    public static final int LIST_OFFSET = 4;

    @Nonnull
    public static SectionAnnotator makeAnnotator(@Nonnull DexAnnotator annotator, @Nonnull MapItem mapItem) {
        return new SectionAnnotator(annotator, mapItem) {
            @Nonnull @Override public String getItemName() {
                return "annotation_set_item";
            }

            @Override
            public void annotateItem(@Nonnull AnnotatedBytes out, int itemIndex, @Nullable String itemIdentity) {
                int size = dexFile.readSmallUint(out.getCursor());
                out.annotate(4, "size = %d", size);

                for (int i=0; i<size; i++) {
                    int annotationOffset = dexFile.readSmallUint(out.getCursor());
                    out.annotate(4, AnnotationItem.getReferenceAnnotation(dexFile, annotationOffset));
                }
            }

            @Override public int getItemAlignment() {
                return 4;
            }
        };
    }

    @Nonnull
    public static String getReferenceAnnotation(@Nonnull DexBackedDexFile dexFile, int annotationSetOffset) {
        if (annotationSetOffset == 0) {
            return "annotation_set_item[NO_OFFSET]";
        }
        return String.format("annotation_set_item[0x%x]", annotationSetOffset);
    }
}
