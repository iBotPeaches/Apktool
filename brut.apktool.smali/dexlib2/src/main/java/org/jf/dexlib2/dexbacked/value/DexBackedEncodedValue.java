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

package org.jf.dexlib2.dexbacked.value;

import org.jf.dexlib2.ValueType;
import org.jf.dexlib2.dexbacked.DexReader;
import org.jf.dexlib2.iface.value.EncodedValue;
import org.jf.dexlib2.immutable.value.*;
import org.jf.dexlib2.util.Preconditions;
import org.jf.util.ExceptionWithContext;

import javax.annotation.Nonnull;

public abstract class DexBackedEncodedValue {
    @Nonnull
    public static EncodedValue readFrom(@Nonnull DexReader reader) {
        int startOffset = reader.getOffset();

        try {
            int b = reader.readUbyte();
            int valueType = b & 0x1f;
            int valueArg = b >>> 5;

            switch (valueType) {
                case ValueType.BYTE:
                    Preconditions.checkValueArg(valueArg, 0);
                    return new ImmutableByteEncodedValue((byte)reader.readByte());
                case ValueType.SHORT:
                    Preconditions.checkValueArg(valueArg, 1);
                    return new ImmutableShortEncodedValue((short)reader.readSizedInt(valueArg + 1));
                case ValueType.CHAR:
                    Preconditions.checkValueArg(valueArg, 1);
                    return new ImmutableCharEncodedValue((char)reader.readSizedSmallUint(valueArg + 1));
                case ValueType.INT:
                    Preconditions.checkValueArg(valueArg, 3);
                    return new ImmutableIntEncodedValue(reader.readSizedInt(valueArg + 1));
                case ValueType.LONG:
                    Preconditions.checkValueArg(valueArg, 7);
                    return new ImmutableLongEncodedValue(reader.readSizedLong(valueArg + 1));
                case ValueType.FLOAT:
                    Preconditions.checkValueArg(valueArg, 3);
                    return new ImmutableFloatEncodedValue(Float.intBitsToFloat(
                            reader.readSizedRightExtendedInt(valueArg + 1)));
                case ValueType.DOUBLE:
                    Preconditions.checkValueArg(valueArg, 7);
                    return new ImmutableDoubleEncodedValue(Double.longBitsToDouble(
                            reader.readSizedRightExtendedLong(valueArg + 1)));
                case ValueType.STRING:
                    Preconditions.checkValueArg(valueArg, 3);
                    return new DexBackedStringEncodedValue(reader, valueArg);
                case ValueType.TYPE:
                    Preconditions.checkValueArg(valueArg, 3);
                    return new DexBackedTypeEncodedValue(reader, valueArg);
                case ValueType.FIELD:
                    Preconditions.checkValueArg(valueArg, 3);
                    return new DexBackedFieldEncodedValue(reader, valueArg);
                case ValueType.METHOD:
                    Preconditions.checkValueArg(valueArg, 3);
                    return new DexBackedMethodEncodedValue(reader, valueArg);
                case ValueType.ENUM:
                    Preconditions.checkValueArg(valueArg, 3);
                    return new DexBackedEnumEncodedValue(reader, valueArg);
                case ValueType.ARRAY:
                    Preconditions.checkValueArg(valueArg, 0);
                    return new DexBackedArrayEncodedValue(reader);
                case ValueType.ANNOTATION:
                    Preconditions.checkValueArg(valueArg, 0);
                    return new DexBackedAnnotationEncodedValue(reader);
                case ValueType.NULL:
                    Preconditions.checkValueArg(valueArg, 0);
                    return ImmutableNullEncodedValue.INSTANCE;
                case ValueType.BOOLEAN:
                    Preconditions.checkValueArg(valueArg, 1);
                    return ImmutableBooleanEncodedValue.forBoolean(valueArg == 1);
                default:
                    throw new ExceptionWithContext("Invalid encoded_value type: 0x%x", valueType);
            }
        } catch (Exception ex) {
            throw ExceptionWithContext.withContext(ex, "Error while reading encoded value at offset 0x%x", startOffset);
        }
    }

    public static void skipFrom(@Nonnull DexReader reader) {
        int startOffset = reader.getOffset();

        try {
            int b = reader.readUbyte();
            int valueType = b & 0x1f;

            switch (valueType) {
                case ValueType.BYTE:
                    reader.skipByte();
                    break;
                case ValueType.SHORT:
                case ValueType.CHAR:
                case ValueType.INT:
                case ValueType.LONG:
                case ValueType.FLOAT:
                case ValueType.DOUBLE:
                case ValueType.STRING:
                case ValueType.TYPE:
                case ValueType.FIELD:
                case ValueType.METHOD:
                case ValueType.ENUM:
                    int valueArg = b >>> 5;
                    reader.moveRelative(valueArg+1);
                    break;
                case ValueType.ARRAY:
                    DexBackedArrayEncodedValue.skipFrom(reader);
                    break;
                case ValueType.ANNOTATION:
                    DexBackedAnnotationEncodedValue.skipFrom(reader);
                    break;
                case ValueType.NULL:
                case ValueType.BOOLEAN:
                    break;
                default:
                    throw new ExceptionWithContext("Invalid encoded_value type: 0x%x", valueType);
            }
        } catch (Exception ex) {
            throw ExceptionWithContext.withContext(ex, "Error while skipping encoded value at offset 0x%x",
                    startOffset);
        }
    }
}
