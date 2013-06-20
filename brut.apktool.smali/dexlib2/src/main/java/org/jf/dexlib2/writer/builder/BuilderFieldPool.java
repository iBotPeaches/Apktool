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

package org.jf.dexlib2.writer.builder;

import com.google.common.collect.Maps;
import org.jf.dexlib2.iface.reference.FieldReference;
import org.jf.dexlib2.immutable.reference.ImmutableFieldReference;
import org.jf.dexlib2.writer.FieldSection;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;

public class BuilderFieldPool
        implements FieldSection<BuilderStringReference, BuilderTypeReference, BuilderFieldReference, BuilderField> {
    @Nonnull private final BuilderContext context;
    @Nonnull private final ConcurrentMap<FieldReference, BuilderFieldReference> internedItems =
            Maps.newConcurrentMap();

    BuilderFieldPool(@Nonnull BuilderContext context) {
        this.context = context;
    }

    @Nonnull BuilderFieldReference internField(@Nonnull String definingClass, String name, String type) {
        ImmutableFieldReference fieldReference = new ImmutableFieldReference(definingClass, name, type);
        return internField(fieldReference);
    }

    @Nonnull public BuilderFieldReference internField(@Nonnull FieldReference fieldReference) {
        BuilderFieldReference ret = internedItems.get(fieldReference);
        if (ret != null) {
            return ret;
        }

        BuilderFieldReference dexPoolFieldReference = new BuilderFieldReference(
                context.typePool.internType(fieldReference.getDefiningClass()),
                context.stringPool.internString(fieldReference.getName()),
                context.typePool.internType(fieldReference.getType()));
        ret = internedItems.putIfAbsent(dexPoolFieldReference, dexPoolFieldReference);
        return ret==null?dexPoolFieldReference:ret;
    }

    @Nonnull @Override
    public BuilderTypeReference getDefiningClass(@Nonnull BuilderFieldReference key) {
        return key.definingClass;
    }

    @Nonnull @Override public BuilderTypeReference getFieldType(@Nonnull BuilderFieldReference key) {
        return key.fieldType;
    }

    @Nonnull @Override public BuilderStringReference getName(@Nonnull BuilderFieldReference key) {
        return key.name;
    }

    @Override public int getFieldIndex(@Nonnull BuilderField builderField) {
        return builderField.fieldReference.getIndex();
    }

    @Override public int getItemIndex(@Nonnull BuilderFieldReference key) {
        return key.index;
    }

    @Nonnull @Override public Collection<? extends Entry<? extends BuilderFieldReference, Integer>> getItems() {
        return new BuilderMapEntryCollection<BuilderFieldReference>(internedItems.values()) {
            @Override protected int getValue(@Nonnull BuilderFieldReference key) {
                return key.index;
            }

            @Override protected int setValue(@Nonnull BuilderFieldReference key, int value) {
                int prev = key.index;
                key.index = value;
                return prev;
            }
        };
    }
}
