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

import org.jf.dexlib2.dexbacked.DexReader;
import org.jf.dexlib2.util.AnnotatedBytes;
import org.jf.util.ExceptionWithContext;

import javax.annotation.Nonnull;

public class EncodedValue {
    public static void annotateEncodedValue(@Nonnull AnnotatedBytes out, @Nonnull DexReader reader) {
        int valueArgType = reader.readUbyte();

        int valueArg = valueArgType >>> 5;
        int valueType = valueArgType & 0x1f;

        switch (valueType) {
            case 0x00:
                out.annotate(1, "valueArg = %d, valueType = 0x%x: byte", valueArg, valueType);
                int intValue = reader.readByte();
                out.annotate(1, "value = 0x%x", intValue);
                break;
            case 0x02:
                out.annotate(1, "valueArg = %d, valueType = 0x%x: short", valueArg, valueType);
                intValue = reader.readSizedInt(valueArg+1);
                out.annotate(valueArg + 1, "value = 0x%x", intValue);
                break;
            case 0x03:
                out.annotate(1, "valueArg = %d, valueType = 0x%x: char", valueArg, valueType);
                intValue = reader.readSizedSmallUint(valueArg+1);
                out.annotate(valueArg+1, "value = 0x%x", intValue);
                break;
            case 0x04:
                out.annotate(1, "valueArg = %d, valueType = 0x%x: int", valueArg, valueType);
                intValue = reader.readSizedInt(valueArg+1);
                out.annotate(valueArg+1, "value = 0x%x", intValue);
                break;
            case 0x06:
                out.annotate(1, "valueArg = %d, valueType = 0x%x: long", valueArg, valueType);
                long longValue = reader.readSizedLong(valueArg+1);
                out.annotate(valueArg+1, "value = 0x%x", longValue);
                break;
            case 0x10:
                out.annotate(1, "valueArg = %d, valueType = 0x%x: float", valueArg, valueType);
                float floatValue = Float.intBitsToFloat(reader.readSizedRightExtendedInt(valueArg + 1));
                out.annotate(valueArg+1, "value = %f", floatValue);
                break;
            case 0x11:
                out.annotate(1, "valueArg = %d, valueType = 0x%x: double", valueArg, valueType);
                double doubleValue = Double.longBitsToDouble(reader.readSizedRightExtendedLong(valueArg + 1));
                out.annotate(valueArg+1, "value = %f", doubleValue);
                break;
            case 0x17:
                out.annotate(1, "valueArg = %d, valueType = 0x%x: string", valueArg, valueType);
                int stringIndex = reader.readSizedSmallUint(valueArg + 1);
                out.annotate(valueArg+1, "value = %s",
                        StringIdItem.getReferenceAnnotation(reader.dexBuf, stringIndex, true));
                break;
            case 0x18:
                out.annotate(1, "valueArg = %d, valueType = 0x%x: type", valueArg, valueType);
                int typeIndex = reader.readSizedSmallUint(valueArg+1);
                out.annotate(valueArg+1, "value = %s", TypeIdItem.getReferenceAnnotation(reader.dexBuf, typeIndex));
                break;
            case 0x19:
                out.annotate(1, "valueArg = %d, valueType = 0x%x: field", valueArg, valueType);
                int fieldIndex = reader.readSizedSmallUint(valueArg+1);
                out.annotate(valueArg+1, "value = %s", FieldIdItem.getReferenceAnnotation(reader.dexBuf, fieldIndex));
                break;
            case 0x1a:
                out.annotate(1, "valueArg = %d, valueType = 0x%x: method", valueArg, valueType);
                int methodIndex = reader.readSizedSmallUint(valueArg+1);
                out.annotate(valueArg+1, "value = %s", MethodIdItem.getReferenceAnnotation(reader.dexBuf, methodIndex));
                break;
            case 0x1b:
                out.annotate(1, "valueArg = %d, valueType = 0x%x: enum", valueArg, valueType);
                fieldIndex = reader.readSizedSmallUint(valueArg+1);
                out.annotate(valueArg+1, "value = %s", FieldIdItem.getReferenceAnnotation(reader.dexBuf, fieldIndex));
                break;
            case 0x1c:
                out.annotate(1, "valueArg = %d, valueType = 0x%x: array", valueArg, valueType);
                annotateEncodedArray(out, reader);
                break;
            case 0x1d:
                out.annotate(1, "valueArg = %d, valueType = 0x%x: annotation", valueArg, valueType);
                annotateEncodedAnnotation(out, reader);
                break;
            case 0x1e:
                out.annotate(1, "valueArg = %d, valueType = 0x%x: null", valueArg, valueType);
                break;
            case 0x1f:
                out.annotate(1, "valueArg = %d, valueType = 0x%x: boolean, value=%s", valueArg, valueType, valueArg==1);
                break;
            default:
                throw new ExceptionWithContext("Invalid encoded value type 0x%x at offset 0x%x", valueType,
                        out.getCursor());
        }
    }

    public static void annotateEncodedAnnotation(@Nonnull AnnotatedBytes out, @Nonnull DexReader reader) {
        assert out.getCursor() == reader.getOffset();

        int typeIndex = reader.readSmallUleb128();
        out.annotateTo(reader.getOffset(), TypeIdItem.getReferenceAnnotation(reader.dexBuf, typeIndex));

        int size = reader.readSmallUleb128();
        out.annotateTo(reader.getOffset(), "size: %d", size);

        for (int i=0; i<size; i++) {
            out.annotate(0, "element[%d]", i);
            out.indent();

            int nameIndex = reader.readSmallUleb128();
            out.annotateTo(reader.getOffset(), "name = %s",
                    StringIdItem.getReferenceAnnotation(reader.dexBuf, nameIndex));

            annotateEncodedValue(out, reader);

            out.deindent();
        }
    }

    public static void annotateEncodedArray(@Nonnull AnnotatedBytes out, @Nonnull DexReader reader) {
        assert out.getCursor() == reader.getOffset();

        int size = reader.readSmallUleb128();
        out.annotateTo(reader.getOffset(), "size: %d", size);

        for (int i=0; i<size; i++) {
            out.annotate(0, "element[%d]", i);
            out.indent();

            annotateEncodedValue(out, reader);

            out.deindent();
        }
    }
}
