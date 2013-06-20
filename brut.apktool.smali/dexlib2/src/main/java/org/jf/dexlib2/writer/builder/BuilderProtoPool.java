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
import org.jf.dexlib2.iface.reference.MethodReference;
import org.jf.dexlib2.util.MethodUtil;
import org.jf.dexlib2.writer.ProtoSection;
import org.jf.util.CharSequenceUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;

class BuilderProtoPool
        implements ProtoSection<BuilderStringReference, BuilderTypeReference, BuilderProtoReference, BuilderTypeList> {
    @Nonnull private final BuilderContext context;
    @Nonnull private final ConcurrentMap<ProtoKey, BuilderProtoReference> internedItems =
            Maps.newConcurrentMap();

    BuilderProtoPool(@Nonnull BuilderContext context) {
        this.context = context;
    }

    @Nonnull public BuilderProtoReference internProto(@Nonnull List<? extends CharSequence> parameters,
                                                      @Nonnull String returnType) {
        ProtoKey key = new Key(parameters, returnType);
        BuilderProtoReference ret = internedItems.get(key);
        if (ret != null) {
            return ret;
        }

        BuilderProtoReference protoReference = new BuilderProtoReference(
                context.stringPool.internString(MethodUtil.getShorty(parameters, returnType)),
                context.typeListPool.internTypeList(parameters),
                context.typePool.internType(returnType));
        ret = internedItems.putIfAbsent(protoReference, protoReference);
        return ret==null?protoReference:ret;
    }

    @Nonnull public BuilderProtoReference internProto(@Nonnull MethodReference methodReference) {
        return internProto(methodReference.getParameterTypes(), methodReference.getReturnType());
    }

    @Nonnull @Override public BuilderStringReference getShorty(@Nonnull BuilderProtoReference key) {
        return key.shorty;
    }

    @Nonnull @Override public BuilderTypeReference getReturnType(@Nonnull BuilderProtoReference key) {
        return key.returnType;
    }

    @Nullable @Override public BuilderTypeList getParameters(@Nonnull BuilderProtoReference key) {
        return key.parameterTypes;
    }

    @Override public int getItemIndex(@Nonnull BuilderProtoReference key) {
        return key.index;
    }

    @Nonnull @Override public Collection<? extends Entry<? extends BuilderProtoReference, Integer>> getItems() {
        return new BuilderMapEntryCollection<BuilderProtoReference>(internedItems.values()) {
            @Override protected int getValue(@Nonnull BuilderProtoReference key) {
                return key.index;
            }

            @Override protected int setValue(@Nonnull BuilderProtoReference key, int value) {
                int prev = key.index;
                key.index = value;
                return prev;
            }
        };
    }

    // a placeholder interface to unify the temporary probing key and the BuilderProtoReference class
    interface ProtoKey {
        @Nonnull List<? extends CharSequence> getParameterTypes();
        @Nonnull String getReturnType();
    }

    // a temporary lightweight class to allow a quick probe if the given prototype has already been interned
    private static class Key implements ProtoKey {
        @Nonnull private final List<? extends CharSequence> parameters;
        @Nonnull private final String returnType;

        public Key(@Nonnull List<? extends CharSequence> parameters, @Nonnull String returnType) {
            this.parameters = parameters;
            this.returnType = returnType;
        }

        @Nonnull public List<? extends CharSequence> getParameterTypes() {
            return parameters;
        }

        @Nonnull public String getReturnType() {
            return returnType;
        }

        @Override public int hashCode() {
            int hashCode = returnType.hashCode();
            return hashCode*31 + parameters.hashCode();
        }

        @Override public boolean equals(Object o) {
            if (o != null && o instanceof ProtoKey) {
                ProtoKey other = (ProtoKey)o;
                return getReturnType().equals(other.getReturnType()) &&
                        CharSequenceUtils.listEquals(getParameterTypes(), other.getParameterTypes());
            }
            return false;
        }
    }
}
