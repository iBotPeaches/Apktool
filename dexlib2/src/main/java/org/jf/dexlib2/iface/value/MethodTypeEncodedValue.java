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

package org.jf.dexlib2.iface.value;

import org.jf.dexlib2.iface.reference.MethodProtoReference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * This class represents an encoded method type value.
 */
public interface MethodTypeEncodedValue extends EncodedValue {
    /**
     * Gets the method type value.
     *
     * @return the method type value as a MethodProtoReference
     */
    @Nonnull MethodProtoReference getValue();

    /**
     * Returns a hashcode for this MethodTypeEncodedValue.
     *
     * This hashCode is defined to be the following:
     *
     * <pre>
     * {@code
     * int hashCode = getValue.hashCode();
     * }</pre>
     *
     * @return The hash code value for this MethodTypeEncodedValue
     */
    @Override int hashCode();

    /**
     * Compares this MethodTypeEncodedValue to another MethodTypeEncodedValue for equality.
     *
     * This MethodTypeEncodedValue is equal to another MethodTypeEncodedValue if the values returned by getValue()
     * are equal.
     *
     * @param o The object to be compared for equality with this MethodTypeEncodedValue
     * @return true if the specified object is equal to this MethodTypeEncodedValue
     */
    @Override boolean equals(@Nullable Object o);

    /**
     * Compare this MethodTypeEncodedValue to another EncodedValue.
     *
     * The comparison is first done on the return values of getValueType(). If the other value is another
     * MethodTypeEncodedValue, the return values of getValue() are compared.
     *
     * @param o The EncodedValue to compare with this MethodTypeEncodedValue
     * @return An integer representing the result of the comparison
     */
    @Override int compareTo(@Nonnull EncodedValue o);
}
