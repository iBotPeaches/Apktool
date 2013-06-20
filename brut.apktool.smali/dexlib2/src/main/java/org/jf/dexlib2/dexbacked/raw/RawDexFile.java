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

import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.dexbacked.BaseDexBuffer;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.dexbacked.util.FixedSizeList;
import org.jf.dexlib2.util.AnnotatedBytes;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;

public class RawDexFile extends DexBackedDexFile {
    @Nonnull public final HeaderItem headerItem;

    public RawDexFile(@Nonnull Opcodes opcodes, @Nonnull BaseDexBuffer buf) {
        super(opcodes, buf);
        this.headerItem = new HeaderItem(this);
    }

    public RawDexFile(@Nonnull Opcodes opcodes, @Nonnull byte[] buf) {
        super(opcodes, buf);
        this.headerItem = new HeaderItem(this);
    }

    @Nonnull
    public byte[] readByteRange(int start, int length) {
        return Arrays.copyOfRange(getBuf(), start, start+length);
    }

    public int getMapOffset() {
        return headerItem.getMapOffset();
    }

    @Nullable
    public MapItem getMapItemForSection(int itemType) {
        for (MapItem mapItem: getMapItems()) {
            if (mapItem.getType() == itemType) {
                return mapItem;
            }
        }
        return null;
    }

    public List<MapItem> getMapItems() {
        final int mapOffset = getMapOffset();
        final int mapSize = readSmallUint(mapOffset);

        return new FixedSizeList<MapItem>() {
            @Override
            public MapItem readItem(int index) {
                int mapItemOffset = mapOffset + 4 + index * MapItem.ITEM_SIZE;
                return new MapItem(RawDexFile.this, mapItemOffset);
            }

            @Override public int size() {
                return mapSize;
            }
        };
    }

    public void writeAnnotations(@Nonnull Writer out, @Nonnull AnnotatedBytes annotatedBytes) throws IOException {
        annotatedBytes.writeAnnotations(out, getBuf());
    }
}
