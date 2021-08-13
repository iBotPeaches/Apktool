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

package org.jf.dexlib2.dexbacked.reference;

import org.jf.dexlib2.base.reference.BaseTypeReference;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.dexbacked.raw.TypeIdItem;

import javax.annotation.Nonnull;

public class DexBackedTypeReference extends BaseTypeReference {
    @Nonnull public final DexBackedDexFile dexFile;
    public final int typeIndex;

    public DexBackedTypeReference(@Nonnull DexBackedDexFile dexFile,
                                  int typeIndex) {
        this.dexFile = dexFile;
        this.typeIndex = typeIndex;
    }

    @Nonnull public String getType() {
        return dexFile.getTypeSection().get(typeIndex);
    }


    /**
     * Calculate and return the private size of a type reference.
     *
     * Calculated as: descriptor_idx
     *
     * @return size in bytes
     */
    public int getSize() {
        return TypeIdItem.ITEM_SIZE; //uint for descriptor_idx
    }

    @Override
    public void validateReference() throws InvalidReferenceException {
        if (typeIndex < 0 || typeIndex >= dexFile.getTypeSection().size()) {
            throw new InvalidReferenceException("type@" + typeIndex);
        }
    }
}
