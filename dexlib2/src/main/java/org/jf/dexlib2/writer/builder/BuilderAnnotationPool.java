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

package org.jf.dexlib2.writer.builder;

import com.google.common.collect.Maps;
import org.jf.dexlib2.iface.Annotation;
import org.jf.dexlib2.writer.AnnotationSection;
import org.jf.dexlib2.writer.builder.BuilderEncodedValues.BuilderEncodedValue;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;

class BuilderAnnotationPool extends BaseBuilderPool implements AnnotationSection<BuilderStringReference,
        BuilderTypeReference, BuilderAnnotation, BuilderAnnotationElement, BuilderEncodedValue> {
    @Nonnull private final ConcurrentMap<Annotation, BuilderAnnotation> internedItems =
            Maps.newConcurrentMap();

    public BuilderAnnotationPool(@Nonnull DexBuilder dexBuilder) {
        super(dexBuilder);
    }

    @Nonnull public BuilderAnnotation internAnnotation(@Nonnull Annotation annotation) {
        BuilderAnnotation ret = internedItems.get(annotation);
        if (ret != null) {
            return ret;
        }

        BuilderAnnotation dexBuilderAnnotation = new BuilderAnnotation(
                annotation.getVisibility(),
                dexBuilder.typeSection.internType(annotation.getType()),
                dexBuilder.internAnnotationElements(annotation.getElements()));
        ret = internedItems.putIfAbsent(dexBuilderAnnotation, dexBuilderAnnotation);
        return ret==null?dexBuilderAnnotation:ret;
    }

    @Override public int getVisibility(@Nonnull BuilderAnnotation key) {
        return key.visibility;
    }

    @Nonnull @Override public BuilderTypeReference getType(@Nonnull BuilderAnnotation key) {
        return key.type;
    }

    @Nonnull @Override
    public Collection<? extends BuilderAnnotationElement> getElements(@Nonnull BuilderAnnotation key) {
        return key.elements;
    }

    @Nonnull @Override
    public BuilderStringReference getElementName(@Nonnull BuilderAnnotationElement element) {
        return element.name;
    }

    @Nonnull @Override
    public BuilderEncodedValue getElementValue(@Nonnull BuilderAnnotationElement element) {
        return element.value;
    }

    @Override public int getItemOffset(@Nonnull BuilderAnnotation key) {
        return key.offset;
    }

    @Nonnull @Override public Collection<? extends Entry<? extends BuilderAnnotation, Integer>> getItems() {
        return new BuilderMapEntryCollection<BuilderAnnotation>(internedItems.values()) {
            @Override protected int getValue(@Nonnull BuilderAnnotation key) {
                return key.offset;
            }

            @Override protected int setValue(@Nonnull BuilderAnnotation key, int value) {
                int prev = key.offset;
                key.offset = value;
                return prev;
            }
        };
    }
}
