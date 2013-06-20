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
import org.jf.dexlib2.base.reference.BaseMethodReference;
import org.jf.dexlib2.iface.reference.MethodReference;
import org.jf.dexlib2.writer.MethodSection;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;

class BuilderMethodPool implements MethodSection<BuilderStringReference, BuilderTypeReference,
        BuilderProtoReference, BuilderMethodReference, BuilderMethod>{
    @Nonnull private final BuilderContext context;
    @Nonnull private final ConcurrentMap<MethodReference, BuilderMethodReference> internedItems =
            Maps.newConcurrentMap();

    BuilderMethodPool(@Nonnull BuilderContext context) {
        this.context = context;
    }

    @Nonnull public BuilderMethodReference internMethod(@Nonnull MethodReference methodReference) {
        BuilderMethodReference ret = internedItems.get(methodReference);
        if (ret != null) {
            return ret;
        }

        BuilderMethodReference dexPoolMethodReference = new BuilderMethodReference(
                context.typePool.internType(methodReference.getDefiningClass()),
                context.stringPool.internString(methodReference.getName()),
                context.protoPool.internProto(methodReference));
        ret = internedItems.putIfAbsent(dexPoolMethodReference, dexPoolMethodReference);
        return ret==null?dexPoolMethodReference:ret;
    }

    @Nonnull public BuilderMethodReference internMethod(@Nonnull String definingClass, @Nonnull String name,
                                                        @Nonnull List<? extends CharSequence> parameters,
                                                        @Nonnull String returnType) {
        return internMethod(new MethodKey(definingClass, name, parameters, returnType));
    }

    @Nonnull @Override
    public BuilderTypeReference getDefiningClass(@Nonnull BuilderMethodReference key) {
        return key.definingClass; 
    }

    @Nonnull @Override
    public BuilderProtoReference getPrototype(@Nonnull BuilderMethodReference key) {
        return key.proto;
    }

    @Nonnull @Override public BuilderProtoReference getPrototype(@Nonnull BuilderMethod builderMethod) {
        return builderMethod.methodReference.proto;
    }

    @Nonnull @Override public BuilderStringReference getName(@Nonnull BuilderMethodReference key) {
        return key.name;
    }

    @Override public int getMethodIndex(@Nonnull BuilderMethod builderMethod) {
        return builderMethod.methodReference.index;
    }

    @Override public int getItemIndex(@Nonnull BuilderMethodReference key) {
        return key.index;
    }

    @Nonnull @Override public Collection<? extends Entry<? extends BuilderMethodReference, Integer>> getItems() {
        return new BuilderMapEntryCollection<BuilderMethodReference>(internedItems.values()) {
            @Override protected int getValue(@Nonnull BuilderMethodReference key) {
                return key.index;
            }

            @Override protected int setValue(@Nonnull BuilderMethodReference key, int value) {
                int prev = key.index;
                key.index = value;
                return prev;
            }
        };
    }

    private static class MethodKey extends BaseMethodReference implements MethodReference {
        @Nonnull private final String definingClass;
        @Nonnull private final String name;
        @Nonnull private final List<? extends CharSequence> parameterTypes;
        @Nonnull private final String returnType;

        public MethodKey(@Nonnull String definingClass, @Nonnull String name,
                         @Nonnull List<? extends CharSequence> parameterTypes, @Nonnull String returnType) {
            this.definingClass = definingClass;
            this.name = name;
            this.parameterTypes = parameterTypes;
            this.returnType = returnType;
        }

        @Nonnull @Override public String getDefiningClass() {
            return definingClass;
        }

        @Nonnull @Override public String getName() {
            return name;
        }

        @Nonnull @Override public List<? extends CharSequence> getParameterTypes() {
            return parameterTypes;
        }

        @Nonnull @Override public String getReturnType() {
            return returnType;
        }
    }
}
