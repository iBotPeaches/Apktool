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

package org.jf.dexlib2.immutable;

import com.google.common.collect.ImmutableSet;
import org.jf.dexlib2.base.BaseAnnotation;
import org.jf.dexlib2.iface.Annotation;
import org.jf.dexlib2.iface.AnnotationElement;
import org.jf.util.ImmutableConverter;
import org.jf.util.ImmutableUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;

public class ImmutableAnnotation extends BaseAnnotation {
    protected final int visibility;
    @Nonnull protected final String type;
    @Nonnull protected final ImmutableSet<? extends ImmutableAnnotationElement> elements;

    public ImmutableAnnotation(int visibility,
                               @Nonnull String type,
                               @Nullable Collection<? extends AnnotationElement> elements) {
        this.visibility = visibility;
        this.type = type;
        this.elements = ImmutableAnnotationElement.immutableSetOf(elements);
    }

    public ImmutableAnnotation(int visibility,
                               @Nonnull String type,
                               @Nullable ImmutableSet<? extends ImmutableAnnotationElement> elements) {
        this.visibility = visibility;
        this.type = type;
        this.elements = ImmutableUtils.nullToEmptySet(elements);
    }

    public static ImmutableAnnotation of(Annotation annotation) {
        if (annotation instanceof  ImmutableAnnotation) {
            return (ImmutableAnnotation)annotation;
        }
        return new ImmutableAnnotation(
                annotation.getVisibility(),
                annotation.getType(),
                annotation.getElements());
    }

    @Override public int getVisibility() { return visibility; }
    @Nonnull @Override public String getType() { return type; }
    @Nonnull @Override public ImmutableSet<? extends ImmutableAnnotationElement> getElements() { return elements; }

    @Nonnull
    public static ImmutableSet<ImmutableAnnotation> immutableSetOf(@Nullable Iterable<? extends Annotation> list) {
        return CONVERTER.toSet(list);
    }

    private static final ImmutableConverter<ImmutableAnnotation, Annotation> CONVERTER =
            new ImmutableConverter<ImmutableAnnotation, Annotation>() {
                @Override
                protected boolean isImmutable(@Nonnull Annotation item) {
                    return item instanceof ImmutableAnnotation;
                }

                @Nonnull
                @Override
                protected ImmutableAnnotation makeImmutable(@Nonnull Annotation item) {
                    return ImmutableAnnotation.of(item);
                }
            };
}
