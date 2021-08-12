/*
 * Copyright 2018, Google Inc.
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
import org.jf.dexlib2.iface.value.ArrayEncodedValue;
import org.jf.dexlib2.writer.EncodedArraySection;
import org.jf.dexlib2.writer.builder.BuilderEncodedValues.BuilderArrayEncodedValue;
import org.jf.dexlib2.writer.builder.BuilderEncodedValues.BuilderEncodedValue;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

public class BuilderEncodedArrayPool extends BaseBuilderPool implements
        EncodedArraySection<BuilderArrayEncodedValue, BuilderEncodedValue> {
    @Nonnull private final ConcurrentMap<ArrayEncodedValue, BuilderArrayEncodedValue> internedItems =
            Maps.newConcurrentMap();

    public BuilderEncodedArrayPool(@Nonnull DexBuilder dexBuilder) {
        super(dexBuilder);
    }

    @Nonnull public BuilderArrayEncodedValue internArrayEncodedValue(@Nonnull ArrayEncodedValue arrayEncodedValue) {
        BuilderArrayEncodedValue builderArrayEncodedValue = internedItems.get(arrayEncodedValue);
        if (builderArrayEncodedValue != null) {
            return builderArrayEncodedValue;
        }

        builderArrayEncodedValue = (BuilderArrayEncodedValue)dexBuilder.internEncodedValue(arrayEncodedValue);
        BuilderArrayEncodedValue previous = internedItems.putIfAbsent(
                builderArrayEncodedValue, builderArrayEncodedValue);
        return previous == null ? builderArrayEncodedValue : previous;
    }

    @Override
    public int getItemOffset(@Nonnull BuilderArrayEncodedValue builderArrayEncodedValue) {
        return builderArrayEncodedValue.offset;
    }

    @Nonnull
    @Override
    public Collection<? extends Map.Entry<? extends BuilderArrayEncodedValue, Integer>> getItems() {
        return new BuilderMapEntryCollection<BuilderArrayEncodedValue>(internedItems.values()) {
            @Override
            protected int getValue(@Nonnull BuilderArrayEncodedValue builderArrayEncodedValue) {
                return builderArrayEncodedValue.offset;
            }

            @Override
            protected int setValue(@Nonnull BuilderArrayEncodedValue key, int value) {
                int prev = key.offset;
                key.offset = value;
                return prev;
            }
        };
    }

    @Override
    public List<? extends BuilderEncodedValue> getEncodedValueList(BuilderArrayEncodedValue builderArrayEncodedValue) {
        return builderArrayEncodedValue.elements;
    }
}
