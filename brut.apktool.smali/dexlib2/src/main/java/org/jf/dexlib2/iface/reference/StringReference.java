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
 * This class represents a reference to an arbitrary string.
 *
 * When possible, elsewhere in the interface, a string is represented directly by its value. A StringReference is only
 * used  in those cases where a generic Reference is needed
 *
 * This type also acts as a CharSequence wrapper around the referenced string value. As per the CharSequence contract,
 * calling toString() on a StringReference yields the referenced string value. This is the same value returned by
 * getString().
 */
public interface StringReference extends Reference, CharSequence, Comparable<CharSequence> {
    /**
     * Gets the referenced string.
     *
     * @return the referenced string
     */
    @Nonnull String getString();

    /**
     * Returns a hashcode for this StringReference.
     *
     * This is defined to be getString().hashCode().
     *
     * @return The hash code value for this StringReference
     */
    @Override int hashCode();

    /**
     * Compares this StringReference to another CharSequence for equality.
     *
     * String StringReference is equal to a CharSequence iff this.getString().equals(other.toString()).
     *
     * Equivalently, This StringReference is equal to another StringReference iff
     * this.getString().equals(other.getString()).
     *
     * @param o The object to be compared for equality with this TypeReference
     * @return true if the specified object is equal to this TypeReference
     */
    @Override boolean equals(@Nullable Object o);

    /**
     * Compare this StringReference to another StringReference, or more generally to another CharSequence.
     *
     * The comparison is defined to be this.getString().compareTo(other.toString()).
     *
     * @param o The CharSequence to compare with this StringReference
     * @return An integer representing the result of the comparison
     */
    @Override int compareTo(@Nonnull CharSequence o);
}
