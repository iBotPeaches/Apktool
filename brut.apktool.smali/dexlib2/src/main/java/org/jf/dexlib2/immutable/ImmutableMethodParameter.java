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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.jf.dexlib2.base.BaseMethodParameter;
import org.jf.dexlib2.iface.Annotation;
import org.jf.dexlib2.iface.MethodParameter;
import org.jf.util.ImmutableConverter;
import org.jf.util.ImmutableUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;

public class ImmutableMethodParameter extends BaseMethodParameter {
    @Nonnull protected final String type;
    @Nonnull protected final ImmutableSet<? extends ImmutableAnnotation> annotations;
    @Nullable protected final String name;

    public ImmutableMethodParameter(@Nonnull String type,
                                    @Nullable Set<? extends Annotation> annotations,
                                    @Nullable String name) {
        this.type = type;
        this.annotations = ImmutableAnnotation.immutableSetOf(annotations);
        this.name = name;
    }

    public ImmutableMethodParameter(@Nonnull String type,
                                    @Nullable ImmutableSet<? extends ImmutableAnnotation> annotations,
                                    @Nullable String name) {
        this.type = type;
        this.annotations = ImmutableUtils.nullToEmptySet(annotations);
        this.name = name;
    }

    public static ImmutableMethodParameter of(MethodParameter methodParameter) {
        if (methodParameter instanceof ImmutableMethodParameter) {
            return (ImmutableMethodParameter)methodParameter;
        }
        return new ImmutableMethodParameter(
                methodParameter.getType(),
                methodParameter.getAnnotations(),
                methodParameter.getName());
    }

    @Nonnull @Override public String getType() { return type; }
    @Nonnull @Override public Set<? extends Annotation> getAnnotations() { return annotations; }
    @Nullable @Override public String getName() { return name; }

    //TODO: iterate over the annotations to get the signature
    @Nullable @Override public String getSignature() { return null; }

    @Nonnull
    public static ImmutableList<ImmutableMethodParameter> immutableListOf(
            @Nullable Iterable<? extends MethodParameter> list) {
        return CONVERTER.toList(list);
    }

    private static final ImmutableConverter<ImmutableMethodParameter, MethodParameter> CONVERTER =
            new ImmutableConverter<ImmutableMethodParameter, MethodParameter>() {
                @Override
                protected boolean isImmutable(@Nonnull MethodParameter item) {
                    return item instanceof ImmutableMethodParameter;
                }

                @Nonnull
                @Override
                protected ImmutableMethodParameter makeImmutable(@Nonnull MethodParameter item) {
                    return ImmutableMethodParameter.of(item);
                }
            };
}
