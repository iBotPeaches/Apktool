/*
 * Copyright 20116, Google Inc.
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
import java.util.List;

/**
 * This class represents a reference to a method prototype.
 */
public interface MethodProtoReference extends Reference, Comparable<MethodProtoReference> {
    /**
     * Gets a list of the types of the parameters of this method prototype.
     *
     * @return A list of the parameter types of this method prototype, as strings.
     */
    @Nonnull List<? extends CharSequence> getParameterTypes();

    /**
     * Gets the return type of the referenced method prototype.
     *
     * @return The return type of the referenced method prototype.
     */
    @Nonnull String getReturnType();

    /**
     * Returns a hashcode for this MethodProtoReference.
     *
     * This hashCode is defined to be the following:
     *
     * <pre>
     * {@code
     * int hashCode =  getReturnType().hashCode();
     * hashCode = hashCode*31 + CharSequenceUtils.listHashCode(getParameters());
     * }</pre>
     *
     * @return The hash code value for this ProtoReference
     */
    @Override int hashCode();

    /**
     * Compares this MethodProtoReference to another MethodProtoReference for equality.
     *
     * This MethodProtoReference is equal to another MethodProtoReference if all of it's "fields" are equal. That is, if
     * the return values of getReturnType() and getParameterTypes() are all equal.
     *
     * Equality for getParameters() should be tested by comparing the string representation of each element. I.e.
     * CharSequenceUtils.listEquals(this.getParameterTypes(), other.getParameterTypes())
     *
     * @param o The object to be compared for equality with this MethodProtoReference
     * @return true if the specified object is equal to this MethodProtoReference
     */
    @Override boolean equals(@Nullable Object o);

    /**
     * Compare this MethodProtoReference to another MethodProtoReference.
     *
     * The comparison is based on the comparison of the return values of getReturnType() and getParameters(),
     * in that order. getParameters() should be compared using the semantics of
     * org.jf.util.CollectionUtils.compareAsList()
     *
     * @param o The MethodReference to compare with this MethodProtoReference
     * @return An integer representing the result of the comparison
     */
    @Override int compareTo(@Nonnull MethodProtoReference o);
}
