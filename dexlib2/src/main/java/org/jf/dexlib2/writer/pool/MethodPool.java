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

package org.jf.dexlib2.writer.pool;

import org.jf.dexlib2.iface.reference.MethodProtoReference;
import org.jf.dexlib2.iface.reference.MethodReference;
import org.jf.dexlib2.writer.MethodSection;

import javax.annotation.Nonnull;

public class MethodPool extends BaseIndexPool<MethodReference>
        implements MethodSection<CharSequence, CharSequence, MethodProtoReference, MethodReference, PoolMethod> {

    public MethodPool(@Nonnull DexPool dexPool) {
        super(dexPool);
    }

    public void intern(@Nonnull MethodReference method) {
        Integer prev = internedItems.put(method, 0);
        if (prev == null) {
            dexPool.typeSection.intern(method.getDefiningClass());
            dexPool.protoSection.intern(new PoolMethodProto(method));
            dexPool.stringSection.intern(method.getName());
        }
    }

    @Nonnull @Override public MethodReference getMethodReference(@Nonnull PoolMethod poolMethod) {
        return poolMethod;
    }

    @Nonnull @Override public CharSequence getDefiningClass(@Nonnull MethodReference methodReference) {
        return methodReference.getDefiningClass();
    }

    @Nonnull @Override public MethodProtoReference getPrototype(@Nonnull MethodReference methodReference) {
        return new PoolMethodProto(methodReference);
    }

    @Nonnull @Override public MethodProtoReference getPrototype(@Nonnull PoolMethod poolMethod) {
        return new PoolMethodProto(poolMethod);
    }

    @Nonnull @Override public CharSequence getName(@Nonnull MethodReference methodReference) {
        return methodReference.getName();
    }

    @Override public int getMethodIndex(@Nonnull PoolMethod poolMethod) {
        return getItemIndex(poolMethod);
    }
}
