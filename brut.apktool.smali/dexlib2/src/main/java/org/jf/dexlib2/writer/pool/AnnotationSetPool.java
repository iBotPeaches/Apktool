/*
 * Copyright 2013, Google Inc.
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

package org.jf.dexlib2.writer.pool;

import org.jf.dexlib2.iface.Annotation;
import org.jf.dexlib2.writer.AnnotationSetSection;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Set;

public class AnnotationSetPool extends BaseNullableOffsetPool<Set<? extends Annotation>>
        implements AnnotationSetSection<Annotation, Set<? extends Annotation>> {
    @Nonnull private final AnnotationPool annotationPool;

    public AnnotationSetPool(@Nonnull AnnotationPool annotationPool) {
        this.annotationPool = annotationPool;
    }

    public void intern(@Nonnull Set<? extends Annotation> annotationSet) {
        if (annotationSet.size() > 0) {
            Integer prev = internedItems.put(annotationSet, 0);
            if (prev == null) {
                for (Annotation annotation: annotationSet) {
                    annotationPool.intern(annotation);
                }
            }
        }
    }

    @Nonnull @Override public Collection<? extends Annotation> getAnnotations(
            @Nonnull Set<? extends Annotation> annotations) {
        return annotations;
    }
}
