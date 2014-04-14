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

package org.jf.dexlib2.base.reference;

import org.jf.dexlib2.iface.reference.TypeReference;

import javax.annotation.Nonnull;

public abstract class BaseTypeReference implements TypeReference {
    @Override
    public int hashCode() {
        return getType().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o != null) {
            if (o instanceof TypeReference) {
                return getType().equals(((TypeReference)o).getType());
            }
            if (o instanceof CharSequence) {
                return getType().equals(o.toString());
            }
        }
        return false;
    }

    @Override
    public int compareTo(@Nonnull CharSequence o) {
        return getType().compareTo(o.toString());
    }

    @Override public int length() { return getType().length(); }
    @Override public char charAt(int index) { return getType().charAt(index); }
    @Override public CharSequence subSequence(int start, int end) { return getType().subSequence(start, end); }
    @Override @Nonnull public String toString() { return getType(); }
}
