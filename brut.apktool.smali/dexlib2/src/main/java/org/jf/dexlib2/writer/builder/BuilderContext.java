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

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import org.jf.dexlib2.ValueType;
import org.jf.dexlib2.iface.AnnotationElement;
import org.jf.dexlib2.iface.value.*;
import org.jf.dexlib2.writer.builder.BuilderEncodedValues.*;
import org.jf.util.ExceptionWithContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;

class BuilderContext {
    // keep our own local references to the various pools, using the Builder specific pool type;
    @Nonnull final BuilderStringPool stringPool;
    @Nonnull final BuilderTypePool typePool;
    @Nonnull final BuilderFieldPool fieldPool;
    @Nonnull final BuilderMethodPool methodPool;
    @Nonnull final BuilderProtoPool protoPool;
    @Nonnull final BuilderClassPool classPool;

    @Nonnull final BuilderTypeListPool typeListPool;
    @Nonnull final BuilderAnnotationPool annotationPool;
    @Nonnull final BuilderAnnotationSetPool annotationSetPool;


    BuilderContext() {
        this.stringPool = new BuilderStringPool();
        this.typePool = new BuilderTypePool(this);
        this.fieldPool = new BuilderFieldPool(this);
        this.methodPool = new BuilderMethodPool(this);
        this.protoPool = new BuilderProtoPool(this);
        this.classPool = new BuilderClassPool();

        this.typeListPool = new BuilderTypeListPool(this);
        this.annotationPool = new BuilderAnnotationPool(this);
        this.annotationSetPool = new BuilderAnnotationSetPool(this);
    }

    @Nonnull Set<? extends BuilderAnnotationElement> internAnnotationElements(
            @Nonnull Set<? extends AnnotationElement> elements) {
        return ImmutableSet.copyOf(
                Iterators.transform(elements.iterator(),
                        new Function<AnnotationElement, BuilderAnnotationElement>() {
                            @Nullable @Override
                            public BuilderAnnotationElement apply(AnnotationElement input) {
                                return internAnnotationElement(input);
                            }
                        }));
    }

    @Nonnull private BuilderAnnotationElement internAnnotationElement(@Nonnull AnnotationElement annotationElement) {
        return new BuilderAnnotationElement(stringPool.internString(annotationElement.getName()),
                internEncodedValue(annotationElement.getValue()));
    }

    @Nullable BuilderEncodedValue internNullableEncodedValue(@Nullable EncodedValue encodedValue) {
        if (encodedValue == null) {
            return null;
        }
        return internEncodedValue(encodedValue);
    }

    @Nonnull private BuilderEncodedValue internEncodedValue(@Nonnull EncodedValue encodedValue) {
        switch (encodedValue.getValueType()) {
            case ValueType.ANNOTATION:
                return internAnnotationEncodedValue((AnnotationEncodedValue)encodedValue);
            case ValueType.ARRAY:
                return internArrayEncodedValue((ArrayEncodedValue)encodedValue);
            case ValueType.BOOLEAN:
                boolean value = ((BooleanEncodedValue)encodedValue).getValue();
                return value?BuilderBooleanEncodedValue.TRUE_VALUE:BuilderBooleanEncodedValue.FALSE_VALUE;
            case ValueType.BYTE:
                return new BuilderByteEncodedValue(((ByteEncodedValue)encodedValue).getValue());
            case ValueType.CHAR:
                return new BuilderCharEncodedValue(((CharEncodedValue)encodedValue).getValue());
            case ValueType.DOUBLE:
                return new BuilderDoubleEncodedValue(((DoubleEncodedValue)encodedValue).getValue());
            case ValueType.ENUM:
                return internEnumEncodedValue((EnumEncodedValue)encodedValue);
            case ValueType.FIELD:
                return internFieldEncodedValue((FieldEncodedValue)encodedValue);
            case ValueType.FLOAT:
                return new BuilderFloatEncodedValue(((FloatEncodedValue)encodedValue).getValue());
            case ValueType.INT:
                return new BuilderIntEncodedValue(((IntEncodedValue)encodedValue).getValue());
            case ValueType.LONG:
                return new BuilderLongEncodedValue(((LongEncodedValue)encodedValue).getValue());
            case ValueType.METHOD:
                return internMethodEncodedValue((MethodEncodedValue)encodedValue);
            case ValueType.NULL:
                return BuilderNullEncodedValue.INSTANCE;
            case ValueType.SHORT:
                return new BuilderShortEncodedValue(((ShortEncodedValue)encodedValue).getValue());
            case ValueType.STRING:
                return internStringEncodedValue((StringEncodedValue)encodedValue);
            case ValueType.TYPE:
                return internTypeEncodedValue((TypeEncodedValue)encodedValue);
            default:
                throw new ExceptionWithContext("Unexpected encoded value type: %d", encodedValue.getValueType());
        }
    }

    @Nonnull private BuilderAnnotationEncodedValue internAnnotationEncodedValue(@Nonnull AnnotationEncodedValue value) {
        return new BuilderAnnotationEncodedValue(
                typePool.internType(value.getType()),
                internAnnotationElements(value.getElements()));
    }

    @Nonnull private BuilderArrayEncodedValue internArrayEncodedValue(@Nonnull ArrayEncodedValue value) {
        return new BuilderArrayEncodedValue(
                ImmutableList.copyOf(
                        Iterators.transform(value.getValue().iterator(),
                                new Function<EncodedValue, BuilderEncodedValue>() {
                                    @Nullable @Override public BuilderEncodedValue apply(EncodedValue input) {
                                        return internEncodedValue(input);
                                    }
                                })));
    }

    @Nonnull private BuilderEnumEncodedValue internEnumEncodedValue(@Nonnull EnumEncodedValue value) {
        return new BuilderEnumEncodedValue(fieldPool.internField(value.getValue()));
    }

    @Nonnull private BuilderFieldEncodedValue internFieldEncodedValue(@Nonnull FieldEncodedValue value) {
        return new BuilderFieldEncodedValue(fieldPool.internField(value.getValue()));
    }

    @Nonnull private BuilderMethodEncodedValue internMethodEncodedValue(@Nonnull MethodEncodedValue value) {
        return new BuilderMethodEncodedValue(methodPool.internMethod(value.getValue()));
    }

    @Nonnull private BuilderStringEncodedValue internStringEncodedValue(@Nonnull StringEncodedValue string) {
        return new BuilderStringEncodedValue(stringPool.internString(string.getValue()));
    }

    @Nonnull private BuilderTypeEncodedValue internTypeEncodedValue(@Nonnull TypeEncodedValue type) {
        return new BuilderTypeEncodedValue(typePool.internType(type.getValue()));
    }
}
