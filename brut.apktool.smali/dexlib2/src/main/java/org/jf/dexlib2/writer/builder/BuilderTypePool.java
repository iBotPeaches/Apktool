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
import org.jf.dexlib2.writer.DexWriter;
import org.jf.dexlib2.writer.TypeSection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;

class BuilderTypePool implements TypeSection<BuilderStringReference, BuilderTypeReference, BuilderTypeReference> {
    @Nonnull private final BuilderContext context;
    @Nonnull private final ConcurrentMap<String, BuilderTypeReference> internedItems = Maps.newConcurrentMap();

    BuilderTypePool(@Nonnull BuilderContext context) {
        this.context = context;
    }

    @Nonnull public BuilderTypeReference internType(@Nonnull String type) {
        BuilderTypeReference ret = internedItems.get(type);
        if (ret != null) {
            return ret;
        }
        BuilderStringReference stringRef = context.stringPool.internString(type);
        BuilderTypeReference typeReference = new BuilderTypeReference(stringRef);
        ret = internedItems.putIfAbsent(type, typeReference);
        return ret==null?typeReference:ret;
    }

    @Nullable public BuilderTypeReference internNullableType(@Nullable String type) {
        if (type == null) {
            return null;
        }
        return internType(type);
    }

    @Nonnull @Override public BuilderStringReference getString(@Nonnull BuilderTypeReference key) {
        return key.stringReference;
    }

    @Override public int getNullableItemIndex(@Nullable BuilderTypeReference key) {
        return key==null?DexWriter.NO_INDEX:key.index;
    }

    @Override public int getItemIndex(@Nonnull BuilderTypeReference key) {
        return key.getIndex();
    }

    @Nonnull @Override public Collection<? extends Entry<? extends BuilderTypeReference, Integer>> getItems() {
        return new BuilderMapEntryCollection<BuilderTypeReference>(internedItems.values()) {
            @Override protected int getValue(@Nonnull BuilderTypeReference key) {
                return key.index;
            }

            @Override protected int setValue(@Nonnull BuilderTypeReference key, int value) {
                int prev = key.index;
                key.index = value;
                return prev;
            }
        };
    }
}
