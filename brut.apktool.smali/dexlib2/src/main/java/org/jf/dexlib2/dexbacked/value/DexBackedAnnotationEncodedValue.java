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

import org.jf.dexlib2.base.value.BaseAnnotationEncodedValue;
import org.jf.dexlib2.dexbacked.DexBackedAnnotationElement;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.dexbacked.DexReader;
import org.jf.dexlib2.dexbacked.util.VariableSizeSet;
import org.jf.dexlib2.iface.value.AnnotationEncodedValue;

import javax.annotation.Nonnull;
import java.util.Set;

public class DexBackedAnnotationEncodedValue extends BaseAnnotationEncodedValue implements AnnotationEncodedValue {
    @Nonnull public final DexBackedDexFile dexFile;
    @Nonnull public final String type;
    private final int elementCount;
    private final int elementsOffset;

    public DexBackedAnnotationEncodedValue(@Nonnull DexReader reader) {
        this.dexFile = reader.dexBuf;
        this.type = dexFile.getType(reader.readSmallUleb128());
        this.elementCount = reader.readSmallUleb128();
        this.elementsOffset = reader.getOffset();
        skipElements(reader, elementCount);
    }

    public static void skipFrom(@Nonnull DexReader reader) {
        reader.skipUleb128(); // type
        int elementCount = reader.readSmallUleb128();
        skipElements(reader, elementCount);
    }

    private static void skipElements(@Nonnull DexReader reader, int elementCount) {
        for (int i=0; i<elementCount; i++) {
            reader.skipUleb128();
            DexBackedEncodedValue.skipFrom(reader);
        }
    }

    @Nonnull @Override public String getType() { return type; }

    @Nonnull
    @Override
    public Set<? extends DexBackedAnnotationElement> getElements() {
        return new VariableSizeSet<DexBackedAnnotationElement>(dexFile, elementsOffset, elementCount) {
            @Nonnull
            @Override
            protected DexBackedAnnotationElement readNextItem(@Nonnull DexReader dexReader, int index) {
                return new DexBackedAnnotationElement(dexReader);
            }
        };
    }
}
