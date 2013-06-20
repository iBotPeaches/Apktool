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
import org.jf.dexlib2.base.BaseAnnotationElement;
import org.jf.dexlib2.iface.AnnotationElement;
import org.jf.dexlib2.iface.value.EncodedValue;
import org.jf.dexlib2.immutable.value.ImmutableEncodedValue;
import org.jf.dexlib2.immutable.value.ImmutableEncodedValueFactory;
import org.jf.util.ImmutableConverter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ImmutableAnnotationElement extends BaseAnnotationElement {
    @Nonnull protected final String name;
    @Nonnull protected final ImmutableEncodedValue value;

    public ImmutableAnnotationElement(@Nonnull String name,
                                      @Nonnull EncodedValue value) {
        this.name = name;
        this.value = ImmutableEncodedValueFactory.of(value);
    }

    public ImmutableAnnotationElement(@Nonnull String name,
                                      @Nonnull ImmutableEncodedValue value) {
        this.name = name;
        this.value = value;
    }

    public static ImmutableAnnotationElement of(AnnotationElement annotationElement) {
        if (annotationElement instanceof ImmutableAnnotationElement) {
            return (ImmutableAnnotationElement)annotationElement;
        }
        return new ImmutableAnnotationElement(
                annotationElement.getName(),
                annotationElement.getValue());
    }

    @Nonnull @Override public String getName() { return name; }
    @Nonnull @Override public EncodedValue getValue() { return value; }

    @Nonnull
    public static ImmutableSet<ImmutableAnnotationElement> immutableSetOf(
            @Nullable Iterable<? extends AnnotationElement> list) {
        return CONVERTER.toSet(list);
    }

    private static final ImmutableConverter<ImmutableAnnotationElement, AnnotationElement> CONVERTER =
            new ImmutableConverter<ImmutableAnnotationElement, AnnotationElement>() {
                @Override
                protected boolean isImmutable(@Nonnull AnnotationElement item) {
                    return item instanceof ImmutableAnnotationElement;
                }

                @Nonnull
                @Override
                protected ImmutableAnnotationElement makeImmutable(@Nonnull AnnotationElement item) {
                    return ImmutableAnnotationElement.of(item);
                }
            };
}
