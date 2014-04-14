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
import org.jf.dexlib2.writer.StringSection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;

class BuilderStringPool implements StringSection<BuilderStringReference, BuilderStringReference> {
    @Nonnull private final ConcurrentMap<String, BuilderStringReference> internedItems = Maps.newConcurrentMap();

    @Nonnull BuilderStringReference internString(@Nonnull String string) {
        BuilderStringReference ret = internedItems.get(string);
        if (ret != null) {
            return ret;
        }
        BuilderStringReference stringReference = new BuilderStringReference(string);
        ret = internedItems.putIfAbsent(string, stringReference);
        return ret==null?stringReference:ret;
    }

    @Nullable BuilderStringReference internNullableString(@Nullable String string) {
        if (string == null) {
            return null;
        }
        return internString(string);
    }

    @Override public int getNullableItemIndex(@Nullable BuilderStringReference key) {
        return key==null?DexWriter.NO_INDEX:key.index;
    }

    @Override public int getItemIndex(@Nonnull BuilderStringReference key) {
        return key.index;
    }

    @Override public boolean hasJumboIndexes() {
        return internedItems.size() > 65536;
    }

    @Nonnull @Override public Collection<? extends Entry<? extends BuilderStringReference, Integer>> getItems() {
        return new BuilderMapEntryCollection<BuilderStringReference>(internedItems.values()) {
            @Override protected int getValue(@Nonnull BuilderStringReference key) {
                return key.index;
            }

            @Override protected int setValue(@Nonnull BuilderStringReference key, int value) {
                int prev = key.index;
                key.index = value;
                return prev;
            }
        };
    }
}
