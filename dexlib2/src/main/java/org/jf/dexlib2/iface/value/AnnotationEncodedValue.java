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

package org.jf.dexlib2.iface.value;

import org.jf.dexlib2.iface.AnnotationElement;
import org.jf.dexlib2.iface.BasicAnnotation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;

/**
 * This class represents an encoded annotation value.
 */
public interface AnnotationEncodedValue extends EncodedValue, BasicAnnotation {
    /**
     * Gets the type of this annotation.
     *
     * This will be the type descriptor of the class that defines this annotation.
     *
     * @return The type of this annotation
     */
    @Nonnull String getType();

    /**
     * Gets a set of the name/value elements associated with this annotation.
     *
     * The elements in the returned set will be unique by name.
     *
     * @return A set of AnnotationElements
     */
    @Nonnull Set<? extends AnnotationElement> getElements();

    /**
     * Returns a hashcode for this AnnotationEncodedValue.
     *
     * This hashCode is defined to be the following:
     *
     * <pre>
     * {@code
     * int hashCode = getType().hashCode();
     * hashCode = hashCode*31 + getElements().hashCode();
     * }</pre>
     *
     * @return The hash code value for this AnnotationEncodedValue
     */
    @Override int hashCode();

    /**
     * Compares this AnnotationEncodedValue to another AnnotationEncodedValue for equality.
     *
     * This AnnotationEncodedValue is equal to another AnnotationEncodedValue if all of it's "fields" are equal. That
     * is, if the return values getType() and getElements() are both equal.
     *
     * @param o The object to be compared for equality with this AnnotationEncodedValue
     * @return true if the specified object is equal to this AnnotationEncodedValue
     */
    @Override boolean equals(@Nullable Object o);

    /**
     * Compare this AnnotationEncodedValue to another EncodedValue.
     *
     * The comparison is based on the value of getType() and getElements(), in that order. When
     * comparing the set of elements, the comparison is done with the semantics of
     * org.jf.util.CollectionUtils.compareAsSet(), using the natural ordering of AnnotationElement.
     *
     * @param o The EncodedValue to compare with this AnnotationEncodedValue
     * @return An integer representing the result of the comparison
     */
    @Override int compareTo(@Nonnull EncodedValue o);
}
