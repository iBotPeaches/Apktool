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

public class ProtoIdItem {
    public static final int ITEM_SIZE = 12;

    public static final int SHORTY_OFFSET = 0;
    public static final int RETURN_TYPE_OFFSET = 4;
    public static final int PARAMETERS_OFFSET = 8;

    @Nonnull
    public static SectionAnnotator makeAnnotator(@Nonnull DexAnnotator annotator, @Nonnull MapItem mapItem) {
        return new SectionAnnotator(annotator, mapItem) {
            @Nonnull @Override public String getItemName() {
                return "proto_id_item";
            }

            @Override
            protected void annotateItem(@Nonnull AnnotatedBytes out, int itemIndex, @Nullable String itemIdentity) {
                int shortyIndex = dexFile.readSmallUint(out.getCursor());
                out.annotate(4, "shorty_idx = %s", StringIdItem.getReferenceAnnotation(dexFile, shortyIndex));

                int returnTypeIndex = dexFile.readSmallUint(out.getCursor());
                out.annotate(4, "return_type_idx = %s", TypeIdItem.getReferenceAnnotation(dexFile, returnTypeIndex));

                int parametersOffset = dexFile.readSmallUint(out.getCursor());
                out.annotate(4, "parameters_off = %s", TypeListItem.getReferenceAnnotation(dexFile, parametersOffset));
            }
        };
    }

    @Nonnull
    public static String getReferenceAnnotation(@Nonnull DexBackedDexFile dexFile, int protoIndex) {
        try {
            String protoString = asString(dexFile, protoIndex);
            return String.format("proto_id_item[%d]: %s", protoIndex, protoString);
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
        }
        return String.format("proto_id_item[%d]", protoIndex);
    }

    @Nonnull
    public static String asString(@Nonnull DexBackedDexFile dexFile, int protoIndex) {
        int offset = dexFile.getProtoIdItemOffset(protoIndex);

        StringBuilder sb = new StringBuilder();
        sb.append("(");

        int parametersOffset = dexFile.readSmallUint(offset + PARAMETERS_OFFSET);
        sb.append(TypeListItem.asString(dexFile, parametersOffset));
        sb.append(")");

        int returnTypeIndex = dexFile.readSmallUint(offset + RETURN_TYPE_OFFSET);
        String returnType = dexFile.getType(returnTypeIndex);
        sb.append(returnType);

        return sb.toString();
    }

    public static String[] getProtos(@Nonnull RawDexFile dexFile) {
        MapItem mapItem = dexFile.getMapItemForSection(ItemType.PROTO_ID_ITEM);
        if (mapItem == null) {
            return new String[0];
        }

        int protoCount = mapItem.getItemCount();
        String[] ret = new String[protoCount];
        for (int i=0; i<protoCount; i++) {
            ret[i] = asString(dexFile, i);
        }
        return ret;
    }
}
