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
import org.jf.dexlib2.iface.reference.CallSiteReference;
import org.jf.dexlib2.writer.CallSiteSection;
import org.jf.dexlib2.writer.builder.BuilderEncodedValues.BuilderArrayEncodedValue;
import org.jf.dexlib2.writer.util.CallSiteUtil;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

public class BuilderCallSitePool extends BaseBuilderPool
        implements CallSiteSection<BuilderCallSiteReference, BuilderArrayEncodedValue> {
    @Nonnull private final ConcurrentMap<CallSiteReference, BuilderCallSiteReference> internedItems =
            Maps.newConcurrentMap();

    public BuilderCallSitePool(@Nonnull DexBuilder dexBuilder) {
        super(dexBuilder);
    }

    @Nonnull public BuilderCallSiteReference internCallSite(@Nonnull CallSiteReference callSiteReference) {
        BuilderCallSiteReference internedCallSite = internedItems.get(callSiteReference);
        if (internedCallSite != null) {
            return internedCallSite;
        }
        BuilderArrayEncodedValue encodedCallSite = dexBuilder.encodedArraySection.internArrayEncodedValue(
                CallSiteUtil.getEncodedCallSite(callSiteReference));
        internedCallSite = new BuilderCallSiteReference(callSiteReference.getName(), encodedCallSite);
        BuilderCallSiteReference existing = internedItems.putIfAbsent(internedCallSite, internedCallSite);
        return existing == null ? internedCallSite : existing;
    }

    @Override
    public BuilderArrayEncodedValue getEncodedCallSite(BuilderCallSiteReference callSiteReference) {
        return callSiteReference.encodedCallSite;
    }

    @Override
    public int getItemIndex(@Nonnull BuilderCallSiteReference builderCallSite) {
        return builderCallSite.index;
    }

    @Nonnull
    @Override
    public Collection<? extends Map.Entry<? extends BuilderCallSiteReference, Integer>> getItems() {
        return new BuilderMapEntryCollection<BuilderCallSiteReference>(internedItems.values()) {
            @Override
            protected int getValue(@Nonnull BuilderCallSiteReference builderCallSiteReference) {
                return builderCallSiteReference.index;
            }

            @Override
            protected int setValue(@Nonnull BuilderCallSiteReference builderCallSiteReference, int value) {
                int prev = builderCallSiteReference.index;
                builderCallSiteReference.index = value;
                return prev;
            }
        };
    }

    @Override
    public int getItemCount() {
        return internedItems.size();
    }
}
