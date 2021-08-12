/*
 * Copyright 2021, Google Inc.
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

package org.jf.dexlib2.formatter;

import org.jf.dexlib2.MethodHandleType;
import org.jf.dexlib2.ValueType;
import org.jf.dexlib2.iface.AnnotationElement;
import org.jf.dexlib2.iface.reference.*;
import org.jf.dexlib2.iface.value.*;

import java.io.IOException;
import java.io.Writer;
import java.util.Set;

/**
 * This class handles formatting and writing various types of items in a dex file to a Writer.
 */
public class DexFormattedWriter extends Writer {

    protected final Writer writer;

    public DexFormattedWriter(Writer writer) {
        this.writer = writer;
    }

    /**
     * Write the method descriptor for the given {@link MethodReference}.
     */
    public void writeMethodDescriptor(MethodReference methodReference) throws IOException {
        writeType(methodReference.getDefiningClass());
        writer.write("->");
        writeSimpleName(methodReference.getName());
        writer.write('(');
        for (CharSequence paramType: methodReference.getParameterTypes()) {
            writeType(paramType);
        }
        writer.write(')');
        writeType(methodReference.getReturnType());
    }

    /**
     * Write the short method descriptor for the given {@link MethodReference}.
     *
     * <p>The short method descriptor elides the class that the field is a member of.
     */
    public void writeShortMethodDescriptor(MethodReference methodReference) throws IOException {
        writeSimpleName(methodReference.getName());
        writer.write('(');
        for (CharSequence paramType: methodReference.getParameterTypes()) {
            writeType(paramType);
        }
        writer.write(')');
        writeType(methodReference.getReturnType());
    }

    /**
     * Write the method proto descriptor for the given {@link MethodProtoReference}.
     */
    public void writeMethodProtoDescriptor(MethodProtoReference protoReference) throws IOException {
        writer.write('(');
        for (CharSequence paramType : protoReference.getParameterTypes()) {
            writeType(paramType);
        }
        writer.write(')');
        writeType(protoReference.getReturnType());
    }

    /**
     * Write the field descriptor for the given {@link FieldReference}.
     */
    public void writeFieldDescriptor(FieldReference fieldReference) throws IOException {
        writeType(fieldReference.getDefiningClass());
        writer.write("->");
        writeSimpleName(fieldReference.getName());
        writer.write(':');
        writeType(fieldReference.getType());
    }

    /**
     * Write the short field descriptor for the given {@link FieldReference}.
     *
     * <p>The short field descriptor typically elides the class that the field is a member of.
     */
    public void writeShortFieldDescriptor(FieldReference fieldReference) throws IOException {
        writeSimpleName(fieldReference.getName());
        writer.write(':');
        writeType(fieldReference.getType());
    }

    /**
     * Write the given {@link MethodHandleReference}.
     */
    public void writeMethodHandle(MethodHandleReference methodHandleReference) throws IOException {
        writer.write(MethodHandleType.toString(methodHandleReference.getMethodHandleType()));
        writer.write('@');

        Reference memberReference = methodHandleReference.getMemberReference();
        if (memberReference instanceof MethodReference) {
            writeMethodDescriptor((MethodReference)memberReference);
        } else {
            writeFieldDescriptor((FieldReference)memberReference);
        }
    }

    /**
     * Write the given {@link CallSiteReference}.
     */
    public void writeCallSite(CallSiteReference callSiteReference) throws IOException {
        writeSimpleName(callSiteReference.getName());
        writer.write('(');
        writeQuotedString(callSiteReference.getMethodName());
        writer.write(", ");
        writeMethodProtoDescriptor(callSiteReference.getMethodProto());

        for (EncodedValue encodedValue : callSiteReference.getExtraArguments()) {
            writer.write(", ");
            writeEncodedValue(encodedValue);
        }
        writer.write(")@");
        MethodHandleReference methodHandle = callSiteReference.getMethodHandle();
        if (methodHandle.getMethodHandleType() != MethodHandleType.INVOKE_STATIC) {
            throw new IllegalArgumentException("The linker method handle for a call site must be of type invoke-static");
        }
        writeMethodDescriptor((MethodReference)callSiteReference.getMethodHandle().getMemberReference());
    }

    /**
     * Write the given type.
     */
    public void writeType(CharSequence type) throws IOException {
        for (int i = 0; i < type.length(); i++) {
            char c = type.charAt(i);
            if (c == 'L') {
                writeClass(type.subSequence(i, type.length()));
                return;
            } else if (c == '[') {
                writer.write(c);
            } else if (c == 'Z' ||
                    c == 'B' ||
                    c == 'S' ||
                    c == 'C' ||
                    c == 'I' ||
                    c == 'J' ||
                    c == 'F' ||
                    c == 'D' ||
                    c == 'V') {
                writer.write(c);

                if (i != type.length() - 1) {
                    throw new IllegalArgumentException(
                            String.format("Invalid type string: %s", type));
                }
                return;
            } else {
                throw new IllegalArgumentException(
                        String.format("Invalid type string: %s", type));
            }
        }

        // Any valid type would have returned from within the loop.
        throw new IllegalArgumentException(
                String.format("Invalid type string: %s", type));
    }

    protected void writeClass(CharSequence type) throws IOException {
        assert type.charAt(0) == 'L';

        writer.write(type.charAt(0));

        int startIndex = 1;
        int i;
        for (i = startIndex; i < type.length(); i++) {
            char c = type.charAt(i);

            if (c == '/') {
                if (i == startIndex) {
                    throw new IllegalArgumentException(
                            String.format("Invalid type string: %s", type));
                }

                writeSimpleName(type.subSequence(startIndex, i));
                writer.write(type.charAt(i));
                startIndex = i+1;
            } else if (c == ';') {
                if (i == startIndex) {
                    throw new IllegalArgumentException(
                            String.format("Invalid type string: %s", type));
                }

                writeSimpleName(type.subSequence(startIndex, i));
                writer.write(type.charAt(i));
                break;
            }
        }

        if (i != type.length() - 1 || type.charAt(i) != ';') {
            throw new IllegalArgumentException(
                    String.format("Invalid type string: %s", type));
        }
    }

    /**
     * Writes the given simple name.
     *
     * @param simpleName The <a href="https://source.android.com/devices/tech/dalvik/dex-format#simplename">simple name</a>
     *                   to write.
     */
    protected void writeSimpleName(CharSequence simpleName) throws IOException {
        writer.append(simpleName);
    }

    /**
     * Write the given quoted string.
     *
     * <p>This includes the beginning and ending quotation marks, and the string value is be escaped as necessary.
     */
    public void writeQuotedString(CharSequence charSequence) throws IOException {
        writer.write('"');

        String string = charSequence.toString();
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);

            if ((c >= ' ') && (c < 0x7f)) {
                if ((c == '\'') || (c == '\"') || (c == '\\')) {
                    writer.write('\\');
                }
                writer.write(c);
                continue;
            } else if (c <= 0x7f) {
                switch (c) {
                    case '\n': writer.write("\\n"); continue;
                    case '\r': writer.write("\\r"); continue;
                    case '\t': writer.write("\\t"); continue;
                }
            }

            writer.write("\\u");
            writer.write(Character.forDigit(c >> 12, 16));
            writer.write(Character.forDigit((c >> 8) & 0x0f, 16));
            writer.write(Character.forDigit((c >> 4) & 0x0f, 16));
            writer.write(Character.forDigit(c & 0x0f, 16));
        }

        writer.write('"');
    }

    /**
     * Write the given {@link EncodedValue}.
     */
    public void writeEncodedValue(EncodedValue encodedValue) throws IOException {
        switch (encodedValue.getValueType()) {
            case ValueType.BOOLEAN:
                writer.write(Boolean.toString(((BooleanEncodedValue) encodedValue).getValue()));
                break;
            case ValueType.BYTE:
                writer.write(
                        String.format("0x%x", ((ByteEncodedValue)encodedValue).getValue()));
                break;
            case ValueType.CHAR:
                writer.write(
                        String.format("0x%x", (int)((CharEncodedValue)encodedValue).getValue()));
                break;
            case ValueType.SHORT:
                writer.write(
                        String.format("0x%x", ((ShortEncodedValue)encodedValue).getValue()));
                break;
            case ValueType.INT:
                writer.write(
                        String.format("0x%x", ((IntEncodedValue)encodedValue).getValue()));
                break;
            case ValueType.LONG:
                writer.write(
                        String.format("0x%x", ((LongEncodedValue)encodedValue).getValue()));
                break;
            case ValueType.FLOAT:
                writer.write(Float.toString(((FloatEncodedValue)encodedValue).getValue()));
                break;
            case ValueType.DOUBLE:
                writer.write(Double.toString(((DoubleEncodedValue)encodedValue).getValue()));
                break;
            case ValueType.ANNOTATION:
                writeAnnotation((AnnotationEncodedValue)encodedValue);
                break;
            case ValueType.ARRAY:
                writeArray((ArrayEncodedValue)encodedValue);
                break;
            case ValueType.STRING:
                writeQuotedString(((StringEncodedValue)encodedValue).getValue());
                break;
            case ValueType.FIELD:
                writeFieldDescriptor(((FieldEncodedValue)encodedValue).getValue());
                break;
            case ValueType.ENUM:
                writeFieldDescriptor(((EnumEncodedValue)encodedValue).getValue());
                break;
            case ValueType.METHOD:
                writeMethodDescriptor(((MethodEncodedValue)encodedValue).getValue());
                break;
            case ValueType.TYPE:
                writeType(((TypeEncodedValue)encodedValue).getValue());
                break;
            case ValueType.METHOD_TYPE:
                writeMethodProtoDescriptor(((MethodTypeEncodedValue)encodedValue).getValue());
                break;
            case ValueType.METHOD_HANDLE:
                writeMethodHandle(((MethodHandleEncodedValue)encodedValue).getValue());
                break;
            case ValueType.NULL:
                writer.write("null");
                break;
            default:
                throw new IllegalArgumentException("Unknown encoded value type");
        }
    }

    /**
     * Write the given {@link AnnotationEncodedValue}.
     */
    protected void writeAnnotation(AnnotationEncodedValue annotation) throws IOException {
        writer.write("Annotation[");
        writeType(annotation.getType());

        Set<? extends AnnotationElement> elements = annotation.getElements();
        for (AnnotationElement element: elements) {
            writer.write(", ");
            writeSimpleName(element.getName());
            writer.write('=');
            writeEncodedValue(element.getValue());
        }

        writer.write(']');
    }

    /**
     * Write the given {@link ArrayEncodedValue}.
     */
    protected void writeArray(ArrayEncodedValue array) throws IOException {
        writer.write("Array[");

        boolean first = true;
        for (EncodedValue element: array.getValue()) {
            if (first) {
                first = false;
            } else {
                writer.write(", ");
            }
            writeEncodedValue(element);
        }

        writer.write(']');
    }

    /**
     * Write the given {@link Reference}.
     */
    public void writeReference(Reference reference) throws IOException {
        if (reference instanceof StringReference) {
            writeQuotedString((StringReference) reference);
        } else if (reference instanceof TypeReference) {
            writeType((TypeReference) reference);
        } else if (reference instanceof FieldReference) {
            writeFieldDescriptor((FieldReference) reference);
        } else if (reference instanceof MethodReference) {
            writeMethodDescriptor((MethodReference) reference);
        } else if (reference instanceof MethodProtoReference) {
            writeMethodProtoDescriptor((MethodProtoReference) reference);
        } else if (reference instanceof MethodHandleReference) {
            writeMethodHandle((MethodHandleReference) reference);
        } else if (reference instanceof CallSiteReference) {
            writeCallSite((CallSiteReference) reference);
        } else {
            throw new IllegalArgumentException(String.format("Not a known reference type: %s", reference.getClass()));
        }
    }

    @Override public void write(int c) throws IOException {
        writer.write(c);
    }

    @Override public void write(char[] cbuf) throws IOException {
        writer.write(cbuf);
    }

    @Override public void write(char[] cbuf, int off, int len) throws IOException {
        writer.write(cbuf, off, len);
    }

    @Override public void write(String str) throws IOException {
        writer.write(str);
    }

    @Override public void write(String str, int off, int len) throws IOException {
        writer.write(str, off, len);
    }

    @Override public Writer append(CharSequence csq) throws IOException {
        return writer.append(csq);
    }

    @Override public Writer append(CharSequence csq, int start, int end) throws IOException {
        return writer.append(csq, start, end);
    }

    @Override public Writer append(char c) throws IOException {
        return writer.append(c);
    }

    @Override public void flush() throws IOException {
        writer.flush();
    }

    @Override public void close() throws IOException {
        writer.close();
    }
}
