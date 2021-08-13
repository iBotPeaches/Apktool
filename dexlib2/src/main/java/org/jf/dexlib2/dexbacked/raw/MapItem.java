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

public class MapItem {
    public static final int ITEM_SIZE = 12;

    public static final int TYPE_OFFSET = 0;
    public static final int SIZE_OFFSET = 4;
    public static final int OFFSET_OFFSET = 8;

    private final DexBackedDexFile dexFile;
    private final int offset;

    public MapItem(DexBackedDexFile dexFile, int offset) {
        this.dexFile = dexFile;
        this.offset = offset;
    }

    public int getType() {
        return dexFile.getDataBuffer().readUshort(offset + TYPE_OFFSET);
    }

    @Nonnull
    public String getName() {
        return ItemType.getItemTypeName(getType());
    }

    public int getItemCount() {
        return dexFile.getDataBuffer().readSmallUint(offset + SIZE_OFFSET);
    }

    public int getOffset() {
        return dexFile.getDataBuffer().readSmallUint(offset + OFFSET_OFFSET);
    }

    @Nonnull
    public static SectionAnnotator makeAnnotator(@Nonnull DexAnnotator annotator, @Nonnull MapItem mapItem) {
        return new SectionAnnotator(annotator, mapItem) {
            @Nonnull @Override public String getItemName() {
                return "map_item";
            }

            @Override
            protected void annotateItem(@Nonnull AnnotatedBytes out, int itemIndex, @Nullable String itemIdentity) {
                int itemType = dexFile.getBuffer().readUshort(out.getCursor());
                out.annotate(2, "type = 0x%x: %s", itemType, ItemType.getItemTypeName(itemType));

                out.annotate(2, "unused");

                int size = dexFile.getBuffer().readSmallUint(out.getCursor());
                out.annotate(4, "size = %d", size);

                int offset = dexFile.getBuffer().readSmallUint(out.getCursor());
                out.annotate(4, "offset = 0x%x", offset);
            }

            @Override public void annotateSection(@Nonnull AnnotatedBytes out) {
                out.moveTo(sectionOffset);
                int mapItemCount = dexFile.getBuffer().readSmallUint(out.getCursor());
                out.annotate(4, "size = %d", mapItemCount);

                super.annotateSectionInner(out, mapItemCount);
            }
        };
    }
}
