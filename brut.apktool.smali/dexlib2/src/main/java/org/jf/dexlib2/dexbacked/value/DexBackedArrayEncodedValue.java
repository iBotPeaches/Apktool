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

package org.jf.dexlib2.dexbacked.value;

import org.jf.dexlib2.base.value.BaseArrayEncodedValue;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.dexbacked.DexReader;
import org.jf.dexlib2.dexbacked.util.VariableSizeList;
import org.jf.dexlib2.iface.value.ArrayEncodedValue;
import org.jf.dexlib2.iface.value.EncodedValue;

import javax.annotation.Nonnull;
import java.util.List;

public class DexBackedArrayEncodedValue extends BaseArrayEncodedValue implements ArrayEncodedValue {
    @Nonnull public final DexBackedDexFile dexFile;
    private final int elementCount;
    private final int encodedArrayOffset;

    public DexBackedArrayEncodedValue(@Nonnull DexReader reader) {
        this.dexFile = reader.dexBuf;
        this.elementCount = reader.readSmallUleb128();
        this.encodedArrayOffset = reader.getOffset();
        skipElementsFrom(reader, elementCount);
    }

    public static void skipFrom(@Nonnull DexReader reader) {
        int elementCount = reader.readSmallUleb128();
        skipElementsFrom(reader, elementCount);
    }

    private static void skipElementsFrom(@Nonnull DexReader reader, int elementCount) {
        for (int i=0; i<elementCount; i++) {
            DexBackedEncodedValue.skipFrom(reader);
        }
    }

    @Nonnull
    @Override
    public List<? extends EncodedValue> getValue() {
        return new VariableSizeList<EncodedValue>(dexFile, encodedArrayOffset, elementCount) {
            @Nonnull
            @Override
            protected EncodedValue readNextItem(@Nonnull DexReader dexReader, int index) {
                return DexBackedEncodedValue.readFrom(dexReader);
            }
        };
    }
}
