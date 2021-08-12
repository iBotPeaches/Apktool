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

import org.jf.dexlib2.iface.AnnotationElement;

import javax.annotation.Nonnull;
import java.util.Comparator;

public abstract class BaseAnnotationElement implements AnnotationElement {
    @Override
    public int hashCode() {
        int hashCode = getName().hashCode();
        return hashCode*31 + getValue().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o != null && o instanceof AnnotationElement) {
            AnnotationElement other = (AnnotationElement)o;
            return getName().equals(other.getName()) &&
                   getValue().equals(other.getValue());
        }
        return false;
    }

    @Override
    public int compareTo(AnnotationElement o) {
        int res = getName().compareTo(o.getName());
        if (res != 0) return res;
        return getValue().compareTo(o.getValue());
    }

    public static final Comparator<AnnotationElement> BY_NAME = new Comparator<AnnotationElement>() {
        @Override
        public int compare(@Nonnull AnnotationElement element1, @Nonnull AnnotationElement element2) {
            return element1.getName().compareTo(element2.getName());
        }
    };
}
