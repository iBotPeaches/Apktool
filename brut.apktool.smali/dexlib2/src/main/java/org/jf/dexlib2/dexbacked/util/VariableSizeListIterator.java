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

import javax.annotation.Nonnull;
import java.util.ListIterator;
import java.util.NoSuchElementException;

public abstract class VariableSizeListIterator<T> implements ListIterator<T> {
    @Nonnull private DexReader reader;
    protected final int size;
    private final int startOffset;

    private int index;

    protected VariableSizeListIterator(@Nonnull DexBackedDexFile dexFile, int offset, int size) {
        this.reader = dexFile.readerAt(offset);
        this.startOffset = offset;
        this.size = size;
    }

    /**
     * Reads the next item from reader.
     *
     * @param reader The {@code DexReader} to read the next item from
     * @param index The index of the item being read. This is guaranteed to be less than {@code size}
     * @return The item that was read
     */
    protected abstract T readNextItem(@Nonnull DexReader reader, int index);

    public int getReaderOffset() {
        return reader.getOffset();
    }

    @Override
    public boolean hasNext() {
        return index < size;
    }

    @Override
    public T next() {
        if (index >= size) {
            throw new NoSuchElementException();
        }
        return readNextItem(reader, index++);
    }

    @Override
    public boolean hasPrevious() {
        return index > 0;
    }

    @Override
    public T previous() {
        int targetIndex = index-1;
        reader.setOffset(startOffset);
        index = 0;
        while (index < targetIndex) {
            readNextItem(reader, index++);
        }
        return readNextItem(reader, index++);
    }

    @Override
    public int nextIndex() {
        return index;
    }

    @Override
    public int previousIndex() {
        return index - 1;
    }

    @Override public void remove() { throw new UnsupportedOperationException(); }
    @Override public void set(T t) { throw new UnsupportedOperationException(); }
    @Override public void add(T t) { throw new UnsupportedOperationException(); }
}
