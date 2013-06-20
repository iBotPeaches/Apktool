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

import org.jf.dexlib2.base.BaseTryBlock;
import org.jf.dexlib2.dexbacked.raw.CodeItem;
import org.jf.dexlib2.dexbacked.util.VariableSizeList;

import javax.annotation.Nonnull;
import java.util.List;

public class DexBackedTryBlock extends BaseTryBlock<DexBackedExceptionHandler> {
    @Nonnull public final DexBackedDexFile dexFile;
    private final int tryItemOffset;
    private final int handlersStartOffset;

    public DexBackedTryBlock(@Nonnull DexBackedDexFile dexFile,
                             int tryItemOffset,
                             int handlersStartOffset) {
        this.dexFile = dexFile;
        this.tryItemOffset = tryItemOffset;
        this.handlersStartOffset = handlersStartOffset;
    }

    @Override public int getStartCodeAddress() {
        return dexFile.readSmallUint(tryItemOffset + CodeItem.TryItem.START_ADDRESS_OFFSET);
    }

    @Override public int getCodeUnitCount() {
        return dexFile.readUshort(tryItemOffset + CodeItem.TryItem.CODE_UNIT_COUNT_OFFSET);
    }

    @Nonnull
    @Override
    public List<? extends DexBackedExceptionHandler> getExceptionHandlers() {
        DexReader reader = dexFile.readerAt(
                handlersStartOffset + dexFile.readUshort(tryItemOffset + CodeItem.TryItem.HANDLER_OFFSET));
        final int encodedSize = reader.readSleb128();

        if (encodedSize > 0) {
            //no catch-all
            return new VariableSizeList<DexBackedTypedExceptionHandler>(dexFile, reader.getOffset(), encodedSize) {
                @Nonnull
                @Override
                protected DexBackedTypedExceptionHandler readNextItem(@Nonnull DexReader reader, int index) {
                    return new DexBackedTypedExceptionHandler(reader);
                }
            };
        } else {
            //with catch-all
            final int sizeWithCatchAll = (-1 * encodedSize) + 1;
            return new VariableSizeList<DexBackedExceptionHandler>(dexFile, reader.getOffset(), sizeWithCatchAll) {
                @Nonnull
                @Override
                protected DexBackedExceptionHandler readNextItem(@Nonnull DexReader dexReader, int index) {
                    if (index == sizeWithCatchAll-1) {
                        return new DexBackedCatchAllExceptionHandler(dexReader);
                    } else {
                        return new DexBackedTypedExceptionHandler(dexReader);
                    }
                }
            };
        }
    }
}
