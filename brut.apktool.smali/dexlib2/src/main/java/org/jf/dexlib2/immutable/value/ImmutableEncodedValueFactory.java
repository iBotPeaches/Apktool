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

package org.jf.dexlib2.immutable.value;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import org.jf.dexlib2.ValueType;
import org.jf.dexlib2.iface.value.*;
import org.jf.util.ExceptionWithContext;
import org.jf.util.ImmutableConverter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ImmutableEncodedValueFactory {
    @Nonnull
    public static ImmutableEncodedValue of(@Nonnull EncodedValue encodedValue) {
        switch (encodedValue.getValueType()) {
            case ValueType.BYTE:
                return ImmutableByteEncodedValue.of((ByteEncodedValue)encodedValue);
            case ValueType.SHORT:
                return ImmutableShortEncodedValue.of((ShortEncodedValue)encodedValue);
            case ValueType.CHAR:
                return ImmutableCharEncodedValue.of((CharEncodedValue)encodedValue);
            case ValueType.INT:
                return ImmutableIntEncodedValue.of((IntEncodedValue)encodedValue);
            case ValueType.LONG:
                return ImmutableLongEncodedValue.of((LongEncodedValue)encodedValue);
            case ValueType.FLOAT:
                return ImmutableFloatEncodedValue.of((FloatEncodedValue)encodedValue);
            case ValueType.DOUBLE:
                return ImmutableDoubleEncodedValue.of((DoubleEncodedValue)encodedValue);
            case ValueType.STRING:
                return ImmutableStringEncodedValue.of((StringEncodedValue)encodedValue);
            case ValueType.TYPE:
                return ImmutableTypeEncodedValue.of((TypeEncodedValue)encodedValue);
            case ValueType.FIELD:
                return ImmutableFieldEncodedValue.of((FieldEncodedValue)encodedValue);
            case ValueType.METHOD:
                return ImmutableMethodEncodedValue.of((MethodEncodedValue)encodedValue);
            case ValueType.ENUM:
                return ImmutableEnumEncodedValue.of((EnumEncodedValue)encodedValue);
            case ValueType.ARRAY:
                return ImmutableArrayEncodedValue.of((ArrayEncodedValue)encodedValue);
            case ValueType.ANNOTATION:
                return ImmutableAnnotationEncodedValue.of((AnnotationEncodedValue)encodedValue);
            case ValueType.NULL:
                return ImmutableNullEncodedValue.INSTANCE;
            case ValueType.BOOLEAN:
                return ImmutableBooleanEncodedValue.of((BooleanEncodedValue)encodedValue);
            default:
                Preconditions.checkArgument(false);
                return null;
        }
    }

    @Nonnull
    public static EncodedValue defaultValueForType(String type) {
        switch (type.charAt(0)) {
            case 'Z':
                return ImmutableBooleanEncodedValue.FALSE_VALUE;
            case 'B':
                return new ImmutableByteEncodedValue((byte)0);
            case 'S':
                return new ImmutableShortEncodedValue((short)0);
            case 'C':
                return new ImmutableCharEncodedValue((char)0);
            case 'I':
                return new ImmutableIntEncodedValue(0);
            case 'J':
                return new ImmutableLongEncodedValue(0);
            case 'F':
                return new ImmutableFloatEncodedValue(0);
            case 'D':
                return new ImmutableDoubleEncodedValue(0);
            case 'L':
            case '[':
                return ImmutableNullEncodedValue.INSTANCE;
            default:
                throw new ExceptionWithContext("Unrecognized type: %s", type);
        }
    }

    @Nullable
    public static ImmutableEncodedValue ofNullable(@Nullable EncodedValue encodedValue) {
        if (encodedValue == null) {
            return null;
        }
        return of(encodedValue);
    }

    @Nonnull
    public static ImmutableList<ImmutableEncodedValue> immutableListOf
            (@Nullable Iterable<? extends EncodedValue> list) {
        return CONVERTER.toList(list);
    }

    private static final ImmutableConverter<ImmutableEncodedValue, EncodedValue> CONVERTER =
            new ImmutableConverter<ImmutableEncodedValue, EncodedValue>() {
                @Override
                protected boolean isImmutable(@Nonnull EncodedValue item) {
                    return item instanceof ImmutableEncodedValue;
                }

                @Nonnull
                @Override
                protected ImmutableEncodedValue makeImmutable(@Nonnull EncodedValue item) {
                    return of(item);
                }
            };
}
