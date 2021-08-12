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

package org.jf.dexlib2.iface.reference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * This class represents a reference to a method handle
 */
public interface MethodHandleReference extends Reference, Comparable<MethodHandleReference> {
    /**
     * Gets the method handle type.
     *
     * @return One of the MethodHandleType values
     */
    int getMethodHandleType();

    /**
     * Gets the member that is being referenced by this method handle.
     *
     * @return A MethodReference or FieldReference, depending on the method handle type
     */
    @Nonnull Reference getMemberReference();

    /**
     * Returns a hashcode for this MethodHandleReference.
     *
     * This hashCode is defined to be the following:
     *
     * <pre>
     * {@code
     * int hashCode =  getMethodHandleType();
     * hashCode = hashCode*31 + getMemberReference().hashCode();
     * }</pre>
     *
     * @return The hash code value for this MethodHandleReference
     */
    @Override int hashCode();

    /**
     * Compares this MethodHandleReference to another MethodHandleReference for equality.
     *
     * This MethodHandleReference is equal to another MethodHandleReference if all of its fields are equal. That is, if
     * the return values of getMethodHandleType() and getMemberReference() are all equal.
     *
     * @param o The object to be compared for equality with this MethodHandleReference
     * @return true if the specified object is equal to this MethodHandleReference
     */
    @Override boolean equals(@Nullable Object o);

    /**
     * Compare this MethodHandleReference to another MethodHandleReference.
     *
     * The comparison is based on the comparison of the return values of getMethodHandleType() and getMemberReference()
     * in that order.
     *
     * @param o The MethodHandleReference to compare with this MethodHandleReference
     * @return An integer representing the result of the comparison
     */
    @Override int compareTo(@Nonnull MethodHandleReference o);
}
