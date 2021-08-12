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

import org.jf.dexlib2.base.reference.BaseFieldReference;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.dexbacked.raw.FieldIdItem;

import javax.annotation.Nonnull;

public class DexBackedFieldReference extends BaseFieldReference {
    @Nonnull public final DexBackedDexFile dexFile;
    private final int fieldIndex;

    public DexBackedFieldReference(@Nonnull DexBackedDexFile dexFile, int fieldIndex) {
        this.dexFile = dexFile;
        this.fieldIndex = fieldIndex;
    }

    @Nonnull
    @Override
    public String getDefiningClass() {
        return dexFile.getTypeSection().get(
                dexFile.getBuffer().readUshort(
                        dexFile.getFieldSection().getOffset(fieldIndex) + FieldIdItem.CLASS_OFFSET));
    }

    @Nonnull
    @Override
    public String getName() {
        return dexFile.getStringSection().get(dexFile.getBuffer().readSmallUint(
                dexFile.getFieldSection().getOffset(fieldIndex) + FieldIdItem.NAME_OFFSET));
    }

    @Nonnull
    @Override
    public String getType() {
        return dexFile.getTypeSection().get(dexFile.getBuffer().readUshort(
                dexFile.getFieldSection().getOffset(fieldIndex) + FieldIdItem.TYPE_OFFSET));
    }

    /**
     * Calculate and return the private size of a field reference.
     *
     * Calculated as: class_idx + type_idx + name_idx
     *
     * @return size in bytes
     */
    public int getSize() {
        return FieldIdItem.ITEM_SIZE;
    }

    @Override
    public void validateReference() throws InvalidReferenceException {
        if (fieldIndex < 0 || fieldIndex >= dexFile.getFieldSection().size()) {
            throw new InvalidReferenceException("field@" + fieldIndex);
        }
    }
}
