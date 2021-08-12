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

package org.jf.dexlib2.dexbacked.util;

import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.dexbacked.DexReader;
import org.jf.dexlib2.dexbacked.value.DexBackedEncodedValue;
import org.jf.dexlib2.iface.value.EncodedValue;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class EncodedArrayItemIterator {
    public static final EncodedArrayItemIterator EMPTY = new EncodedArrayItemIterator() {
        @Nullable @Override public EncodedValue getNextOrNull() { return null; }
        @Override public void skipNext() {}
        @Override public int getReaderOffset() { return 0; }
        @Override public int getItemCount() { return 0; }
    };

    @Nullable public abstract EncodedValue getNextOrNull();
    public abstract void skipNext();
    public abstract int getReaderOffset();
    public abstract int getItemCount();

    @Nonnull
    public static EncodedArrayItemIterator newOrEmpty(@Nonnull DexBackedDexFile dexFile, int offset) {
        if (offset == 0) {
            return EMPTY;
        }
        return new EncodedArrayItemIteratorImpl(dexFile, offset);
    }

    private static class EncodedArrayItemIteratorImpl extends EncodedArrayItemIterator {
        @Nonnull private final DexReader reader;
        @Nonnull private final DexBackedDexFile dexFile;
        private final int size;
        private int index = 0;

        public EncodedArrayItemIteratorImpl(@Nonnull DexBackedDexFile dexFile, int offset) {
            this.dexFile = dexFile;
            this.reader = dexFile.getDataBuffer().readerAt(offset);
            this.size = reader.readSmallUleb128();
        }

        @Nullable
        public EncodedValue getNextOrNull() {
            if (index < size) {
                index++;
                return DexBackedEncodedValue.readFrom(dexFile, reader);
            }
            return null;
        }

        @Override
        public void skipNext() {
            if (index < size) {
                index++;
                DexBackedEncodedValue.skipFrom(reader);
            }
        }

        @Override
        public int getReaderOffset() {
            return reader.getOffset();
        }

        @Override
        public int getItemCount() {
            return size;
        }
    }
}
