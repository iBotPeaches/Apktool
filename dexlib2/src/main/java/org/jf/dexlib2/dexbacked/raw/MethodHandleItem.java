/*
 * Copyright 2018, Google Inc.
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

import org.jf.dexlib2.MethodHandleType;
import org.jf.dexlib2.dexbacked.raw.util.DexAnnotator;
import org.jf.dexlib2.util.AnnotatedBytes;
import org.jf.util.ExceptionWithContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class MethodHandleItem {
    public static final int ITEM_SIZE = 8;

    public static final int METHOD_HANDLE_TYPE_OFFSET = 0;
    public static final int MEMBER_ID_OFFSET = 4;

    @Nonnull
    public static SectionAnnotator makeAnnotator(@Nonnull DexAnnotator annotator, @Nonnull MapItem mapItem) {
        return new SectionAnnotator(annotator, mapItem) {
            @Nonnull @Override public String getItemName() {
                return "method_handle_item";
            }

            @Override
            protected void annotateItem(@Nonnull AnnotatedBytes out, int itemIndex, @Nullable String itemIdentity) {
                int methodHandleType = dexFile.getBuffer().readUshort(out.getCursor());
                out.annotate(2, "type = %s", MethodHandleType.toString(methodHandleType));
                out.annotate(2, "unused");

                int fieldOrMethodId = dexFile.getBuffer().readUshort(out.getCursor());
                String fieldOrMethodDescriptor;
                switch (methodHandleType) {
                    case MethodHandleType.STATIC_PUT:
                    case MethodHandleType.STATIC_GET:
                    case MethodHandleType.INSTANCE_PUT:
                    case MethodHandleType.INSTANCE_GET:
                        fieldOrMethodDescriptor = FieldIdItem.getReferenceAnnotation(dexFile, fieldOrMethodId);
                        break;
                    case MethodHandleType.INVOKE_STATIC:
                    case MethodHandleType.INVOKE_INSTANCE:
                    case MethodHandleType.INVOKE_CONSTRUCTOR:
                    case MethodHandleType.INVOKE_DIRECT:
                    case MethodHandleType.INVOKE_INTERFACE:
                        fieldOrMethodDescriptor = MethodIdItem.getReferenceAnnotation(dexFile, fieldOrMethodId);
                        break;
                    default:
                        throw new ExceptionWithContext("Invalid method handle type: %d", methodHandleType);
                }

                out.annotate(2, "field_or_method_id = %s", fieldOrMethodDescriptor);
                out.annotate(2, "unused");
            }
        };
    }



}
