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

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import org.jf.dexlib2.writer.DexWriter;
import org.jf.dexlib2.writer.TypeListSection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;

class BuilderTypeListPool extends BaseBuilderPool implements TypeListSection<BuilderTypeReference, BuilderTypeList> {
    @Nonnull private final ConcurrentMap<List<? extends CharSequence>, BuilderTypeList> internedItems =
            Maps.newConcurrentMap();

    public BuilderTypeListPool(@Nonnull DexBuilder dexBuilder) {
        super(dexBuilder);
    }

    @Nonnull public BuilderTypeList internTypeList(@Nullable List<? extends CharSequence> types) {
        if (types == null || types.size() == 0) {
            return BuilderTypeList.EMPTY;
        }

        BuilderTypeList ret = internedItems.get(types);
        if (ret != null) {
            return ret;
        }

        BuilderTypeList typeList = new BuilderTypeList(
                ImmutableList.copyOf(Iterables.transform(types, new Function<CharSequence, BuilderTypeReference>() {
                    @Nonnull @Override public BuilderTypeReference apply(CharSequence input) {
                        return dexBuilder.typeSection.internType(input.toString());
                    }
                })));

        ret = internedItems.putIfAbsent(typeList, typeList);
        return ret==null?typeList:ret;
    }

    @Override public int getNullableItemOffset(@Nullable BuilderTypeList key) {
        return (key==null||key.size()==0)?DexWriter.NO_OFFSET:key.offset;
    }

    @Nonnull @Override
    public Collection<? extends BuilderTypeReference> getTypes(@Nullable BuilderTypeList key) {
        return key==null?BuilderTypeList.EMPTY:key.types;
    }

    @Override public int getItemOffset(@Nonnull BuilderTypeList key) {
        return key.offset;
    }

    @Nonnull @Override public Collection<? extends Entry<? extends BuilderTypeList, Integer>> getItems() {
        return new BuilderMapEntryCollection<BuilderTypeList>(internedItems.values()) {
            @Override protected int getValue(@Nonnull BuilderTypeList key) {
                return key.offset;
            }

            @Override protected int setValue(@Nonnull BuilderTypeList key, int value) {
                int prev = key.offset;
                key.offset = value;
                return prev;
            }
        };
    }
}
