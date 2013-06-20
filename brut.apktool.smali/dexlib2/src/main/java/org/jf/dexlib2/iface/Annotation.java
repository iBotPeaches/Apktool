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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;

/**
 * This class represents a specific instance of an annotation applied to a class/field/method/parameter
 */
public interface Annotation extends BasicAnnotation, Comparable<Annotation> {
    /**
     * Gets the visibility of this annotation.
     *
     * This will be one of the AnnotationVisibility.* constants.
     *
     * @return The visibility of this annotation
     */
    int getVisibility();

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
     * The elements in the returned set will be unique with respect to the element name.
     *
     * @return A set of AnnotationElements
     */
    @Nonnull Set<? extends AnnotationElement> getElements();

    /**
     * Returns a hashcode for this Annotation.
     *
     * This hashCode is defined to be the following:
     *
     * <pre>
     * {@code
     * int hashCode = getVisibility();
     * hashCode = hashCode*31 + getType().hashCode();
     * hashCode = hashCode*31 + getElements().hashCode();
     * }</pre>
     *
     * @return The hash code value for this Annotation
     */
    @Override int hashCode();

    /**
     * Compares this Annotation to another Annotation for equality.
     *
     * This Annotation is equal to another Annotation if all of it's "fields" are equal. That is, if the return values
     * of getVisibility(), getType(), and getElements() are all equal.
     *
     * @param o The object to be compared for equality with this Annotation
     * @return true if the specified object is equal to this Annotation
     */
    @Override boolean equals(@Nullable Object o);

    /**
     * Compares this Annotation to another Annotation.
     *
     * The comparison is based on the value of getVisibility(), getType() and getElements(), in that order. When
     * comparing the set of elements, the comparison is done with the semantics of
     * org.jf.util.CollectionUtils.compareAsSet(), using the natural ordering of AnnotationElement.
     *
     * @param o The Annotation to compare with this Annotation
     * @return An integer representing the result of the comparison
     */
    @Override int compareTo(Annotation o);
}
