/*
 * Copyright 2018, Google Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 * Neither the name of Google Inc. nor the names of its
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

import org.jf.dexlib2.MethodHandleType;
import org.jf.dexlib2.iface.reference.FieldReference;
import org.jf.dexlib2.iface.reference.MethodHandleReference;
import org.jf.dexlib2.iface.reference.MethodReference;
import org.jf.dexlib2.writer.MethodHandleSection;
import org.jf.util.ExceptionWithContext;

import javax.annotation.Nonnull;

public class MethodHandlePool extends BaseIndexPool<MethodHandleReference>
        implements MethodHandleSection<MethodHandleReference, FieldReference, MethodReference> {
    public MethodHandlePool(@Nonnull DexPool dexPool) {
        super(dexPool);
    }

    public void intern(MethodHandleReference methodHandleReference) {
        Integer prev = internedItems.put(methodHandleReference, 0);
        if (prev == null) {
            switch (methodHandleReference.getMethodHandleType()) {
                case MethodHandleType.STATIC_PUT:
                case MethodHandleType.STATIC_GET:
                case MethodHandleType.INSTANCE_PUT:
                case MethodHandleType.INSTANCE_GET:
                    dexPool.fieldSection.intern((FieldReference) methodHandleReference.getMemberReference());
                    break;
                case MethodHandleType.INVOKE_STATIC:
                case MethodHandleType.INVOKE_INSTANCE:
                case MethodHandleType.INVOKE_CONSTRUCTOR:
                case MethodHandleType.INVOKE_DIRECT:
                case MethodHandleType.INVOKE_INTERFACE:
                    dexPool.methodSection.intern((MethodReference) methodHandleReference.getMemberReference());
                    break;
                default:
                    throw new ExceptionWithContext(
                            "Invalid method handle type: %d", methodHandleReference.getMethodHandleType());
            }
        }
    }

    @Override
    public FieldReference getFieldReference(MethodHandleReference methodHandleReference) {
        return (FieldReference) methodHandleReference.getMemberReference();
    }

    @Override
    public MethodReference getMethodReference(MethodHandleReference methodHandleReference) {
        return (MethodReference) methodHandleReference.getMemberReference();
    }
}
