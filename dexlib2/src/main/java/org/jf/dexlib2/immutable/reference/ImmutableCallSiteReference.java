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

package org.jf.dexlib2.immutable.reference;

import com.google.common.collect.ImmutableList;
import org.jf.dexlib2.base.reference.BaseCallSiteReference;
import org.jf.dexlib2.iface.reference.CallSiteReference;
import org.jf.dexlib2.iface.reference.MethodHandleReference;
import org.jf.dexlib2.iface.reference.MethodProtoReference;
import org.jf.dexlib2.iface.value.EncodedValue;
import org.jf.dexlib2.immutable.value.ImmutableEncodedValue;
import org.jf.dexlib2.immutable.value.ImmutableEncodedValueFactory;
import org.jf.util.ImmutableUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class ImmutableCallSiteReference extends BaseCallSiteReference implements ImmutableReference {
    @Nonnull protected final String name;
    @Nonnull protected final ImmutableMethodHandleReference methodHandle;
    @Nonnull protected final String methodName;
    @Nonnull protected final ImmutableMethodProtoReference methodProto;
    @Nonnull protected final ImmutableList<? extends ImmutableEncodedValue> extraArguments;

    public ImmutableCallSiteReference(@Nonnull String name, @Nonnull MethodHandleReference methodHandle,
                                      @Nonnull String methodName, @Nonnull MethodProtoReference methodProto,
                                      @Nonnull Iterable<? extends EncodedValue> extraArguments) {
        this.name = name;
        this.methodHandle = ImmutableMethodHandleReference.of(methodHandle);
        this.methodName = methodName;
        this.methodProto = ImmutableMethodProtoReference.of(methodProto);
        this.extraArguments = ImmutableEncodedValueFactory.immutableListOf(extraArguments);
    }

    public ImmutableCallSiteReference(@Nonnull String name, @Nonnull ImmutableMethodHandleReference methodHandle,
                                      @Nonnull String methodName, @Nonnull ImmutableMethodProtoReference methodProto,
                                      @Nullable ImmutableList<? extends ImmutableEncodedValue> extraArguments) {
        this.name = name;
        this.methodHandle = methodHandle;
        this.methodName = methodName;
        this.methodProto = methodProto;
        this.extraArguments = ImmutableUtils.nullToEmptyList(extraArguments);
    }

    @Nonnull
    public static ImmutableCallSiteReference of(@Nonnull CallSiteReference callSiteReference) {
        if (callSiteReference instanceof ImmutableCallSiteReference) {
            return (ImmutableCallSiteReference) callSiteReference;
        }
        return new ImmutableCallSiteReference(callSiteReference.getName(),
                ImmutableMethodHandleReference.of(callSiteReference.getMethodHandle()),
                callSiteReference.getMethodName(),
                ImmutableMethodProtoReference.of(callSiteReference.getMethodProto()),
                ImmutableEncodedValueFactory.immutableListOf(callSiteReference.getExtraArguments()));
    }

    @Nonnull @Override public String getName() { return name; }
    @Nonnull @Override public MethodHandleReference getMethodHandle() { return methodHandle; }
    @Nonnull @Override public String getMethodName() { return methodName; }
    @Nonnull @Override public MethodProtoReference getMethodProto() { return methodProto; }
    @Nonnull @Override public List<? extends EncodedValue> getExtraArguments() { return extraArguments; }
}
