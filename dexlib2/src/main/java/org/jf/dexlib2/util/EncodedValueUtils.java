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

package org.jf.dexlib2.util;

import org.jf.dexlib2.ValueType;
import org.jf.dexlib2.formatter.DexFormatter;
import org.jf.dexlib2.iface.AnnotationElement;
import org.jf.dexlib2.iface.value.*;
import org.jf.util.StringUtils;

import java.io.IOException;
import java.io.Writer;
import java.util.Set;

/**
 * Some utilities for generating human-readable strings for encoded values.
 */
public final class EncodedValueUtils {
    public static boolean isDefaultValue(EncodedValue encodedValue) {
        switch (encodedValue.getValueType()) {
            case ValueType.BOOLEAN:
                return !((BooleanEncodedValue)encodedValue).getValue();
            case ValueType.BYTE:
                return ((ByteEncodedValue)encodedValue).getValue() == 0;
            case ValueType.CHAR:
                return ((CharEncodedValue)encodedValue).getValue() == 0;
            case ValueType.DOUBLE:
                return ((DoubleEncodedValue)encodedValue).getValue() == 0;
            case ValueType.FLOAT:
                return ((FloatEncodedValue)encodedValue).getValue() == 0;
            case ValueType.INT:
                return ((IntEncodedValue)encodedValue).getValue() == 0;
            case ValueType.LONG:
                return ((LongEncodedValue)encodedValue).getValue() == 0;
            case ValueType.NULL:
                return true;
            case ValueType.SHORT:
                return ((ShortEncodedValue)encodedValue).getValue() == 0;
        }
        return false;
    }

    /**
     * @deprecated use {@link DexFormatter} instead.
     */
    @Deprecated
    public static void writeEncodedValue(Writer writer, EncodedValue encodedValue) throws IOException {
        switch (encodedValue.getValueType()) {
            case ValueType.BOOLEAN:
                writer.write(Boolean.toString(((BooleanEncodedValue) encodedValue).getValue()));
                break;
            case ValueType.BYTE:
                writer.write(Byte.toString(((ByteEncodedValue)encodedValue).getValue()));
                break;
            case ValueType.CHAR:
                writer.write(Integer.toString(((CharEncodedValue)encodedValue).getValue()));
                break;
            case ValueType.SHORT:
                writer.write(Short.toString(((ShortEncodedValue)encodedValue).getValue()));
                break;
            case ValueType.INT:
                writer.write(Integer.toString(((IntEncodedValue)encodedValue).getValue()));
                break;
            case ValueType.LONG:
                writer.write(Long.toString(((LongEncodedValue)encodedValue).getValue()));
                break;
            case ValueType.FLOAT:
                writer.write(Float.toString(((FloatEncodedValue)encodedValue).getValue()));
                break;
            case ValueType.DOUBLE:
                writer.write(Double.toString(((DoubleEncodedValue)encodedValue).getValue()));
                break;
            case ValueType.ANNOTATION:
                writeAnnotation(writer, (AnnotationEncodedValue)encodedValue);
                break;
            case ValueType.ARRAY:
                writeArray(writer, (ArrayEncodedValue)encodedValue);
                break;
            case ValueType.STRING:
                writer.write('"');
                StringUtils.writeEscapedString(writer, ((StringEncodedValue)encodedValue).getValue());
                writer.write('"');
                break;
            case ValueType.FIELD:
                ReferenceUtil.writeFieldDescriptor(writer, ((FieldEncodedValue)encodedValue).getValue());
                break;
            case ValueType.ENUM:
                ReferenceUtil.writeFieldDescriptor(writer, ((EnumEncodedValue)encodedValue).getValue());
                break;
            case ValueType.METHOD:
                ReferenceUtil.writeMethodDescriptor(writer, ((MethodEncodedValue)encodedValue).getValue());
                break;
            case ValueType.TYPE:
                writer.write(((TypeEncodedValue)encodedValue).getValue());
                break;
            case ValueType.METHOD_TYPE:
                ReferenceUtil.writeMethodProtoDescriptor(writer, ((MethodTypeEncodedValue)encodedValue).getValue());
                break;
            case ValueType.METHOD_HANDLE:
                ReferenceUtil.writeMethodHandle(writer, ((MethodHandleEncodedValue)encodedValue).getValue());
                break;
            case ValueType.NULL:
                writer.write("null");
                break;
            default:
                throw new IllegalArgumentException("Unknown encoded value type");
        }
    }

    private static void writeAnnotation(Writer writer, AnnotationEncodedValue annotation) throws IOException {
        writer.write("Annotation[");
        writer.write(annotation.getType());

        Set<? extends AnnotationElement> elements = annotation.getElements();
        for (AnnotationElement element: elements) {
            writer.write(", ");
            writer.write(element.getName());
            writer.write('=');
            writeEncodedValue(writer, element.getValue());
        }

        writer.write(']');
    }

    private static void writeArray(Writer writer, ArrayEncodedValue array) throws IOException {
        writer.write("Array[");

        boolean first = true;
        for (EncodedValue element: array.getValue()) {
            if (first) {
                first = false;
            } else {
                writer.write(", ");
            }
            writeEncodedValue(writer, element);
        }

        writer.write(']');
    }

    private EncodedValueUtils() {}
}
