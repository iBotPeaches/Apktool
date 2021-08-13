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

package org.jf.dexlib2.writer.pool;

import org.jf.dexlib2.iface.Field;
import org.jf.dexlib2.iface.reference.FieldReference;
import org.jf.dexlib2.writer.FieldSection;

import javax.annotation.Nonnull;

public class FieldPool extends BaseIndexPool<FieldReference>
        implements FieldSection<CharSequence, CharSequence, FieldReference, Field> {

    public FieldPool(@Nonnull DexPool dexPool) {
        super(dexPool);
    }

    public void intern(@Nonnull FieldReference field) {
        Integer prev = internedItems.put(field, 0);
        if (prev == null) {
            dexPool.typeSection.intern(field.getDefiningClass());
            dexPool.stringSection.intern(field.getName());
            dexPool.typeSection.intern(field.getType());
        }
    }

    @Nonnull @Override public CharSequence getDefiningClass(@Nonnull FieldReference fieldReference) {
        return fieldReference.getDefiningClass();
    }

    @Nonnull @Override public CharSequence getFieldType(@Nonnull FieldReference fieldReference) {
        return fieldReference.getType();
    }

    @Nonnull @Override public CharSequence getName(@Nonnull FieldReference fieldReference) {
        return fieldReference.getName();
    }

    @Override public int getFieldIndex(@Nonnull Field field) {
        return getItemIndex(field);
    }
}
