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

package org.jf.dexlib2.base;

import com.google.common.primitives.Ints;
import org.jf.dexlib2.iface.Annotation;
import org.jf.util.CollectionUtils;

import java.util.Comparator;

public abstract class BaseAnnotation implements Annotation {
    @Override
    public int hashCode() {
        int hashCode = getVisibility();
        hashCode = hashCode*31 + getType().hashCode();
        return hashCode*31 + getElements().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Annotation) {
            Annotation other = (Annotation)o;
            return (getVisibility() == other.getVisibility()) &&
                   getType().equals(other.getType()) &&
                   getElements().equals(other.getElements());
        }
        return false;
    }

    @Override
    public int compareTo(Annotation o) {
        int res = Ints.compare(getVisibility(), o.getVisibility());
        if (res != 0) return res;
        res = getType().compareTo(o.getType());
        if (res != 0) return res;
        return CollectionUtils.compareAsSet(getElements(), o.getElements());
    }

    public static final Comparator<? super Annotation> BY_TYPE = new Comparator<Annotation>() {
        @Override
        public int compare(Annotation annotation1, Annotation annotation2) {
            return annotation1.getType().compareTo(annotation2.getType());
        }
    };
}
