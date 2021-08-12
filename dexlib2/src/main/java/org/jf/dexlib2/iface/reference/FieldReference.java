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

/**
 * This class represents a reference to a field.
 */
public interface FieldReference extends Reference, Comparable<FieldReference> {
    /**
     * Gets the type of the class that defines the referenced field.
     *
     * @return The type of the class that defines the referenced field
     */
    @Nonnull String getDefiningClass();

    /**
     * Gets the name of the referenced field.
     *
     * @return The name of the referenced field
     */
    @Nonnull String getName();

    /**
     * Gets the type of the referenced field.
     *
     * @return The type of the referenced field
     */
    @Nonnull String getType();

    /**
     * Returns a hashcode for this FieldReference.
     *
     * This hashCode is defined to be the following:
     *
     * <pre>
     * {@code
     * int hashCode =  getDefiningClass().hashCode();
     * hashCode = hashCode*31 + getName().hashCode();
     * hashCode = hashCode*31 + getType().hashCode();
     * }</pre>
     *
     * @return The hash code value for this FieldReference
     */
    @Override int hashCode();

    /**
     * Compares this FieldReference to another FieldReference for equality.
     *
     * This FieldReference is equal to another FieldReference if all of it's "fields" are equal. That is, if
     * the return values of getDefiningClass(), getName() and getType() are all equal.
     *
     * @param o The object to be compared for equality with this FieldReference
     * @return true if the specified object is equal to this FieldReference
     */
    @Override boolean equals(@Nullable Object o);

    /**
     * Compare this FieldReference to another FieldReference.
     *
     * The comparison is based on the comparison of the return values of getDefiningClass(), getName() and
     * getType(), in that order.
     *
     * @param o The FieldReference to compare with this FieldReference
     * @return An integer representing the result of the comparison
     */
    @Override int compareTo(@Nonnull FieldReference o);
}
