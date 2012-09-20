/*
 * [The "BSD licence"]
 * Copyright (c) 2010 Ben Gruver (JesusFreke)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.jf.baksmali.Adaptors.EncodedValue;

import org.jf.baksmali.Adaptors.ReferenceFormatter;
import org.jf.util.IndentingWriter;
import org.jf.baksmali.Renderers.*;
import org.jf.dexlib.EncodedValue.*;

import java.io.IOException;

public abstract class EncodedValueAdaptor {
    public static void writeTo(IndentingWriter writer, EncodedValue encodedValue) throws IOException {
        switch (encodedValue.getValueType()) {
            case VALUE_ANNOTATION:
                AnnotationEncodedValueAdaptor.writeTo(writer, (AnnotationEncodedValue)encodedValue);
                return;
            case VALUE_ARRAY:
                ArrayEncodedValueAdaptor.writeTo(writer, (ArrayEncodedValue)encodedValue);
                return;
            case VALUE_BOOLEAN:
                BooleanRenderer.writeTo(writer, ((BooleanEncodedValue)encodedValue).value);
                return;
            case VALUE_BYTE:
                ByteRenderer.writeTo(writer, ((ByteEncodedValue)encodedValue).value);
                return;
            case VALUE_CHAR:
                CharRenderer.writeTo(writer, ((CharEncodedValue)encodedValue).value);
                return;
            case VALUE_DOUBLE:
                DoubleRenderer.writeTo(writer, ((DoubleEncodedValue)encodedValue).value);
                return;
            case VALUE_ENUM:
                EnumEncodedValueAdaptor.writeTo(writer, ((EnumEncodedValue)encodedValue).value);
                return;
            case VALUE_FIELD:
                ReferenceFormatter.writeFieldReference(writer, ((FieldEncodedValue)encodedValue).value);
                return;
            case VALUE_FLOAT:
                FloatRenderer.writeTo(writer, ((FloatEncodedValue)encodedValue).value);
                return;
            case VALUE_INT:
                IntegerRenderer.writeTo(writer, ((IntEncodedValue)encodedValue).value);
                return;
            case VALUE_LONG:
                LongRenderer.writeTo(writer, ((LongEncodedValue)encodedValue).value);
                return;
            case VALUE_METHOD:
                ReferenceFormatter.writeMethodReference(writer, ((MethodEncodedValue)encodedValue).value);
                return;
            case VALUE_NULL:
                writer.write("null");
                return;
            case VALUE_SHORT:
                ShortRenderer.writeTo(writer, ((ShortEncodedValue)encodedValue).value);
                return;
            case VALUE_STRING:
                ReferenceFormatter.writeStringReference(writer, ((StringEncodedValue)encodedValue).value);
                return;
            case VALUE_TYPE:
                ReferenceFormatter.writeTypeReference(writer, ((TypeEncodedValue)encodedValue).value);
        }
    }
}
