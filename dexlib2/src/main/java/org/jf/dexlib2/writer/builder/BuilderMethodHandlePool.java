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
import org.jf.dexlib2.MethodHandleType;
import org.jf.dexlib2.iface.reference.FieldReference;
import org.jf.dexlib2.iface.reference.MethodHandleReference;
import org.jf.dexlib2.iface.reference.MethodReference;
import org.jf.dexlib2.writer.MethodHandleSection;
import org.jf.util.ExceptionWithContext;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

public class BuilderMethodHandlePool extends BaseBuilderPool
        implements MethodHandleSection<BuilderMethodHandleReference, BuilderFieldReference, BuilderMethodReference> {
    @Nonnull private final ConcurrentMap<MethodHandleReference, BuilderMethodHandleReference> internedItems =
            Maps.newConcurrentMap();

    public BuilderMethodHandlePool(@Nonnull DexBuilder dexBuilder) {
        super(dexBuilder);
    }

    public BuilderMethodHandleReference internMethodHandle(MethodHandleReference methodHandleReference) {
        BuilderMethodHandleReference internedMethodHandle = internedItems.get(methodHandleReference);
        if (internedMethodHandle != null) {
            return internedMethodHandle;
        }

        BuilderReference memberReference;
        switch (methodHandleReference.getMethodHandleType()) {
            case MethodHandleType.STATIC_PUT:
            case MethodHandleType.STATIC_GET:
            case MethodHandleType.INSTANCE_PUT:
            case MethodHandleType.INSTANCE_GET:
                memberReference = dexBuilder.internFieldReference(
                        (FieldReference) methodHandleReference.getMemberReference());
                break;
            case MethodHandleType.INVOKE_STATIC:
            case MethodHandleType.INVOKE_INSTANCE:
            case MethodHandleType.INVOKE_CONSTRUCTOR:
            case MethodHandleType.INVOKE_DIRECT:
            case MethodHandleType.INVOKE_INTERFACE:
                memberReference = dexBuilder.internMethodReference(
                        (MethodReference) methodHandleReference.getMemberReference());
                break;
            default:
                throw new ExceptionWithContext("Invalid method handle type: %d",
                        methodHandleReference.getMethodHandleType());
        }

        internedMethodHandle = new BuilderMethodHandleReference(methodHandleReference.getMethodHandleType(),
                memberReference);
        BuilderMethodHandleReference prev = internedItems.putIfAbsent(internedMethodHandle, internedMethodHandle);
        return prev == null ? internedMethodHandle : prev;
    }

    @Override
    public BuilderFieldReference getFieldReference(BuilderMethodHandleReference methodHandleReference) {
        return (BuilderFieldReference) methodHandleReference.getMemberReference();
    }

    @Override
    public BuilderMethodReference getMethodReference(BuilderMethodHandleReference methodHandleReference) {
        return (BuilderMethodReference) methodHandleReference.getMemberReference();
    }

    @Override
    public int getItemIndex(@Nonnull BuilderMethodHandleReference builderMethodHandleReference) {
        return builderMethodHandleReference.index;
    }

    @Nonnull
    @Override
    public Collection<? extends Map.Entry<? extends BuilderMethodHandleReference, Integer>> getItems() {
        return new BuilderMapEntryCollection<BuilderMethodHandleReference>(internedItems.values()) {
            @Override
            protected int getValue(@Nonnull BuilderMethodHandleReference builderMethodHandleReference) {
                return builderMethodHandleReference.index;
            }

            @Override
            protected int setValue(@Nonnull BuilderMethodHandleReference builderMethodHandleReference, int value) {
                int prev = builderMethodHandleReference.index;
                builderMethodHandleReference.index = value;
                return prev;
            }
        };
    }

    @Override
    public int getItemCount() {
        return internedItems.size();
    }
}
