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

import org.jf.dexlib2.formatter.DexFormatter;
import org.jf.dexlib2.iface.reference.CallSiteReference;

public abstract class BaseCallSiteReference extends BaseReference implements CallSiteReference {
    @Override
    public int hashCode() {
        int hashCode = getName().hashCode();
        hashCode = hashCode*31 + getMethodHandle().hashCode();
        hashCode = hashCode*31 + getMethodName().hashCode();
        hashCode = hashCode*31 + getMethodProto().hashCode();
        hashCode = hashCode*31 + getExtraArguments().hashCode();
        return hashCode;
    }

    @Override
    public boolean equals(Object o) {
        if (o != null && o instanceof CallSiteReference) {
            CallSiteReference other = (CallSiteReference) o;
            return getMethodHandle().equals(other.getMethodHandle()) &&
                    getMethodName().equals(other.getMethodName()) &&
                    getMethodProto().equals(other.getMethodProto()) &&
                    getExtraArguments().equals(other.getExtraArguments());
        }
        return false;
    }

    @Override public String toString() {
        return DexFormatter.INSTANCE.getCallSite(this);
    }
}
