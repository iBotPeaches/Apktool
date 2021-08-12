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

package org.jf.dexlib2.base.reference;

import com.google.common.primitives.Ints;
import org.jf.dexlib2.formatter.DexFormatter;
import org.jf.dexlib2.iface.reference.FieldReference;
import org.jf.dexlib2.iface.reference.MethodHandleReference;
import org.jf.dexlib2.iface.reference.MethodReference;
import org.jf.dexlib2.iface.reference.Reference;

import javax.annotation.Nonnull;

public abstract class BaseMethodHandleReference extends BaseReference implements MethodHandleReference {
    @Override
    public int hashCode() {
        int hashCode =  getMethodHandleType();
        hashCode = hashCode*31 + getMemberReference().hashCode();
        return hashCode;
    }

    @Override
    public boolean equals(Object o) {
        if (o != null && o instanceof MethodHandleReference) {
            MethodHandleReference other = (MethodHandleReference) o;
            return getMethodHandleType() == other.getMethodHandleType() &&
                    getMemberReference().equals(other.getMemberReference());
        }
        return false;
    }

    @Override
    public int compareTo(@Nonnull MethodHandleReference o) {
        int res = Ints.compare(getMethodHandleType(), o.getMethodHandleType());
        if (res != 0) return res;

        Reference reference = getMemberReference();
        if (reference instanceof FieldReference) {
            // "This should never happen", but if it does, we'll arbitrarily say a field reference compares less than
            // a method reference
            if (!(o.getMemberReference() instanceof FieldReference)) {
                return -1;
            }
            return ((FieldReference) reference).compareTo((FieldReference) o.getMemberReference());
        } else {
            if (!(o.getMemberReference() instanceof MethodReference)) {
                return 1;
            }
            return ((MethodReference) reference).compareTo((MethodReference) o.getMemberReference());
        }
    }

    @Override public String toString() {
        return DexFormatter.INSTANCE.getMethodHandle(this);
    }
}
