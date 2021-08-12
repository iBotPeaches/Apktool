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

package org.jf.dexlib2.dexbacked.raw;

import org.jf.dexlib2.ValueType;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.dexbacked.DexReader;
import org.jf.dexlib2.dexbacked.value.DexBackedEncodedValue;
import org.jf.dexlib2.util.AnnotatedBytes;

import javax.annotation.Nonnull;

public class EncodedValue {
    public static void annotateEncodedValue(
            @Nonnull DexBackedDexFile dexFile,
            @Nonnull AnnotatedBytes out,
            @Nonnull DexReader reader) {
        int valueArgType = reader.readUbyte();

        int valueArg = valueArgType >>> 5;
        int valueType = valueArgType & 0x1f;

        switch (valueType) {
            case ValueType.BYTE:
            case ValueType.SHORT:
            case ValueType.CHAR:
            case ValueType.INT:
            case ValueType.LONG:
            case ValueType.FLOAT:
            case ValueType.DOUBLE:
            case ValueType.METHOD_TYPE:
            case ValueType.METHOD_HANDLE:
            case ValueType.STRING:
            case ValueType.TYPE:
            case ValueType.FIELD:
            case ValueType.METHOD:
            case ValueType.ENUM:
                out.annotate(1, "valueArg = %d, valueType = 0x%x: %s", valueArg, valueType,
                        ValueType.getValueTypeName(valueType));
                reader.setOffset(reader.getOffset() - 1);
                out.annotate(valueArg + 1, "value = %s", asString(dexFile, reader));
                break;
            case ValueType.ARRAY:
                out.annotate(1, "valueArg = %d, valueType = 0x%x: array", valueArg, valueType);
                annotateEncodedArray(dexFile, out, reader);
                break;
            case ValueType.ANNOTATION:
                out.annotate(1, "valueArg = %d, valueType = 0x%x: annotation", valueArg, valueType);
                annotateEncodedAnnotation(dexFile, out, reader);
                break;
            case ValueType.NULL:
                out.annotate(1, "valueArg = %d, valueType = 0x%x: null", valueArg, valueType);
                break;
            case ValueType.BOOLEAN:
                out.annotate(1, "valueArg = %d, valueType = 0x%x: boolean, value=%s", valueArg, valueType, valueArg==1);
                break;
            default:
                throw new IllegalArgumentException(String.format("Invalid encoded value type 0x%x at offset 0x%x", valueType,
                        reader.getOffset()));
        }
    }

    public static void annotateEncodedAnnotation(
            @Nonnull DexBackedDexFile dexFile,
            @Nonnull AnnotatedBytes out,
            @Nonnull DexReader reader) {
        assert out.getCursor() == reader.getOffset();

        int typeIndex = reader.readSmallUleb128();
        out.annotateTo(reader.getOffset(), TypeIdItem.getReferenceAnnotation(dexFile, typeIndex));

        int size = reader.readSmallUleb128();
        out.annotateTo(reader.getOffset(), "size: %d", size);

        for (int i=0; i<size; i++) {
            out.annotate(0, "element[%d]", i);
            out.indent();

            int nameIndex = reader.readSmallUleb128();
            out.annotateTo(reader.getOffset(), "name = %s",
                    StringIdItem.getReferenceAnnotation(dexFile, nameIndex));

            annotateEncodedValue(dexFile, out, reader);

            out.deindent();
        }
    }

    public static void annotateEncodedArray(
            @Nonnull DexBackedDexFile dexFile,
            @Nonnull AnnotatedBytes out,
            @Nonnull DexReader reader) {
        assert out.getCursor() == reader.getOffset();

        int size = reader.readSmallUleb128();
        out.annotateTo(reader.getOffset(), "size: %d", size);

        for (int i=0; i<size; i++) {
            out.annotate(0, "element[%d]", i);
            out.indent();

            annotateEncodedValue(dexFile, out, reader);

            out.deindent();
        }
    }

    public static String asString(@Nonnull DexBackedDexFile dexFile, @Nonnull DexReader reader) {
        int valueArgType = reader.readUbyte();

        int valueArg = valueArgType >>> 5;
        int valueType = valueArgType & 0x1f;

        switch (valueType) {
            case ValueType.BYTE:
                int intValue = reader.readByte();
                return String.format("0x%x", intValue);
            case ValueType.SHORT:
                intValue = reader.readSizedInt(valueArg+1);
                return String.format("0x%x", intValue);
            case ValueType.CHAR:
                intValue = reader.readSizedSmallUint(valueArg+1);
                return String.format("0x%x", intValue);
            case ValueType.INT:
                intValue = reader.readSizedInt(valueArg+1);
                return String.format("0x%x", intValue);
            case ValueType.LONG:
                long longValue = reader.readSizedLong(valueArg+1);
                return String.format("0x%x", longValue);
            case ValueType.FLOAT:
                float floatValue = Float.intBitsToFloat(reader.readSizedRightExtendedInt(valueArg + 1));
                return String.format("%f", floatValue);
            case ValueType.DOUBLE:
                double doubleValue = Double.longBitsToDouble(reader.readSizedRightExtendedLong(valueArg + 1));
                return String.format("%f", doubleValue);
            case ValueType.METHOD_TYPE:
                int protoIndex = reader.readSizedSmallUint(valueArg + 1);
                return ProtoIdItem.getReferenceAnnotation(dexFile, protoIndex);
            case ValueType.STRING:
                int stringIndex = reader.readSizedSmallUint(valueArg + 1);
                return StringIdItem.getReferenceAnnotation(dexFile, stringIndex, true);
            case ValueType.TYPE:
                int typeIndex = reader.readSizedSmallUint(valueArg+1);
                return TypeIdItem.getReferenceAnnotation(dexFile, typeIndex);
            case ValueType.FIELD:
                int fieldIndex = reader.readSizedSmallUint(valueArg+1);
                return FieldIdItem.getReferenceAnnotation(dexFile, fieldIndex);
            case ValueType.METHOD:
                int methodIndex = reader.readSizedSmallUint(valueArg+1);
                return MethodIdItem.getReferenceAnnotation(dexFile, methodIndex);
            case ValueType.ENUM:
                fieldIndex = reader.readSizedSmallUint(valueArg+1);
                return FieldIdItem.getReferenceAnnotation(dexFile, fieldIndex);
            case ValueType.ARRAY:
            case ValueType.ANNOTATION:
            case ValueType.METHOD_HANDLE:
                reader.setOffset(reader.getOffset() - 1);
                return DexBackedEncodedValue.readFrom(dexFile, reader).toString();
            case ValueType.NULL:
                return "null";
            case ValueType.BOOLEAN:
                return Boolean.toString(valueArg == 1);
            default:
                throw new IllegalArgumentException(String.format("Invalid encoded value type 0x%x at offset 0x%x",
                        valueType, reader.getOffset()));
        }
    }
}
