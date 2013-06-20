/*
 * Copyright 2012, Google Inc.
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

import org.jf.dexlib2.ReferenceType;
import org.jf.dexlib2.iface.reference.*;
import org.jf.util.ExceptionWithContext;

import javax.annotation.Nonnull;

public class ImmutableReferenceFactory {
    @Nonnull
    public static ImmutableReference of(Reference reference) {
        if (reference instanceof StringReference) {
            return ImmutableStringReference.of((StringReference)reference);
        }
        if (reference instanceof TypeReference) {
            return ImmutableTypeReference.of((TypeReference)reference);
        }
        if (reference instanceof FieldReference) {
            return ImmutableFieldReference.of((FieldReference)reference);
        }
        if (reference instanceof MethodReference) {
            return ImmutableMethodReference.of((MethodReference)reference);
        }
        throw new ExceptionWithContext("Invalid reference type");
    }

    @Nonnull
    public static ImmutableReference of(int referenceType, Reference reference) {
        switch (referenceType) {
            case ReferenceType.STRING:
                return ImmutableStringReference.of((StringReference)reference);
            case ReferenceType.TYPE:
                return ImmutableTypeReference.of((TypeReference)reference);
            case ReferenceType.FIELD:
                return ImmutableFieldReference.of((FieldReference)reference);
            case ReferenceType.METHOD:
                return ImmutableMethodReference.of((MethodReference)reference);
        }
        throw new ExceptionWithContext("Invalid reference type: %d", referenceType);
    }
}
