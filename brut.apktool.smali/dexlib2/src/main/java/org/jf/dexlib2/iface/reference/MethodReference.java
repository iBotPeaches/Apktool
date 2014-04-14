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

package org.jf.dexlib2.iface.reference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * This class represents a reference to a method.
 */
public interface MethodReference extends Reference, Comparable<MethodReference> {
    /**
     * Gets the type of the class that defines the referenced method.
     *
     * @return The type of the class that defines the referenced method
     */
    @Nonnull String getDefiningClass();

    /**
     * Gets the name of the referenced method.
     *
     * @return The name of the referenced method
     */
    @Nonnull String getName();

    /**
     * Gets a list of the types of the parameters of this method.
     *
     * @return A list of the parameter types of this method, as strings.
     */
    @Nonnull List<? extends CharSequence> getParameterTypes();

    /**
     * Gets the return type of the referenced method.
     *
     * @return The return type of the referenced method.
     */
    @Nonnull String getReturnType();

    /**
     * Returns a hashcode for this MethodReference.
     *
     * This hashCode is defined to be the following:
     *
     * <pre>
     * {@code
     * int hashCode =  getDefiningClass().hashCode();
     * hashCode = hashCode*31 + getName().hashCode();
     * hashCode = hashCode*31 + getReturnType().hashCode();
     * hashCode = hashCode*31 + CharSequenceUtils.listHashCode(getParameters());
     * }</pre>
     *
     * @return The hash code value for this MethodReference
     */
    @Override int hashCode();

    /**
     * Compares this MethodReference to another MethodReference for equality.
     *
     * This MethodReference is equal to another MethodReference if all of it's "fields" are equal. That is, if
     * the return values of getDefiningClass(), getName(), getReturnType() and getParameterTypes() are all equal.
     *
     * Equality for getParameters() should be tested by comparing the string representation of each element. I.e.
     * CharSequenceUtils.listEquals(this.getParameterTypes(), other.getParameterTypes())
     *
     * @param o The object to be compared for equality with this MethodReference
     * @return true if the specified object is equal to this MethodReference
     */
    @Override boolean equals(@Nullable Object o);

    /**
     * Compare this MethodReference to another MethodReference.
     *
     * The comparison is based on the comparison of the return values of getDefiningClass(), getName(),
     * getReturnType() and getParameters(), in that order. getParameters() should be compared using the semantics
     * of org.jf.util.CollectionUtils.compareAsList()
     *
     * @param o The MethodReference to compare with this MethodReference
     * @return An integer representing the result of the comparison
     */
    @Override int compareTo(@Nonnull MethodReference o);
}
