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

package org.jf.dexlib2.iface;

import org.jf.dexlib2.iface.value.EncodedValue;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * This class represents an individual name/value element in an annotation
 */
public interface AnnotationElement extends Comparable<AnnotationElement> {
    /**
     * Gets the name of the element.
     *
     * @return The name of the element.
     */
    @Nonnull String getName();

    /**
     * Gets the value of the element.
     *
     * @return The value of the element
     */
    @Nonnull EncodedValue getValue();

    /**
     * Returns a hashcode for this AnnotationElement.
     *
     * This hashCode is defined to be the following:
     *
     * <pre>
     * {@code
     * int hashCode = getName().hashCode();
     * hashCode = hashCode*31 + getValue().hashCode();
     * }</pre>
     *
     * @return The hash code value for this AnnotationElement
     */
    @Override int hashCode();

    /**
     * Compares this AnnotationElement to another AnnotationElement for equality.
     *
     * This AnnotationElement is equal to another AnnotationElement if all of it's "fields" are equal. That is, if
     * the return values of getName() and getValue() are both equal.
     *
     * @param o The object to be compared for equality with this AnnotationElement
     * @return true if the specified object is equal to this AnnotationElement
     */
    @Override boolean equals(@Nullable Object o);

    /**
     * Compares this AnnotationElement to another AnnotationElement.
     *
     * The comparison is based on the value of getName() and getValue(), in that order.
     *
     * @param o The AnnotationElement to compare with this AnnotationElement
     * @return An integer representing the result of the comparison
     */
    @Override int compareTo(AnnotationElement o);
}
