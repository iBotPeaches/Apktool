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

package org.jf.dexlib2.base.value;

import com.google.common.primitives.Ints;
import org.jf.dexlib2.ValueType;
import org.jf.dexlib2.formatter.DexFormatter;
import org.jf.dexlib2.iface.value.AnnotationEncodedValue;
import org.jf.dexlib2.iface.value.EncodedValue;
import org.jf.util.CollectionUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class BaseAnnotationEncodedValue implements AnnotationEncodedValue {
    @Override
    public int hashCode() {
        int hashCode = getType().hashCode();
        return hashCode * 31 + getElements().hashCode();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (o instanceof AnnotationEncodedValue) {
            AnnotationEncodedValue other = (AnnotationEncodedValue)o;
            return getType().equals(other.getType()) &&
                    getElements().equals(other.getElements());
        }
        return false;
    }

    @Override
    public int compareTo(@Nonnull EncodedValue o) {
        int res = Ints.compare(getValueType(), o.getValueType());
        if (res != 0) return res;
        AnnotationEncodedValue other = (AnnotationEncodedValue)o;
        res = getType().compareTo(other.getType());
        if (res != 0) return res;
        return CollectionUtils.compareAsSet(getElements(), other.getElements());
    }

    public int getValueType() {
        return ValueType.ANNOTATION;
    }

    @Override public String toString() {
        return DexFormatter.INSTANCE.getEncodedValue(this);
    }
}
