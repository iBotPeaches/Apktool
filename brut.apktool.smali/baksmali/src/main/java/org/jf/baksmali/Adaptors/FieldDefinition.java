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

package org.jf.baksmali.Adaptors;

import org.jf.baksmali.Adaptors.EncodedValue.EncodedValueAdaptor;
import org.jf.util.IndentingWriter;
import org.jf.dexlib.AnnotationSetItem;
import org.jf.dexlib.ClassDataItem;
import org.jf.dexlib.EncodedValue.EncodedValue;
import org.jf.dexlib.EncodedValue.NullEncodedValue;
import org.jf.dexlib.Util.AccessFlags;

import java.io.IOException;

public class FieldDefinition {
    public static void writeTo(IndentingWriter writer, ClassDataItem.EncodedField encodedField,
                                                EncodedValue initialValue, AnnotationSetItem annotationSet,
                                                boolean setInStaticConstructor) throws IOException {

        String fieldTypeDescriptor = encodedField.field.getFieldType().getTypeDescriptor();

        if (setInStaticConstructor &&
            encodedField.isStatic() &&
            (encodedField.accessFlags & AccessFlags.FINAL.getValue()) != 0 &&
            initialValue != null &&
            (
                //it's a primitive type, or it's an array/reference type and the initial value isn't null
                fieldTypeDescriptor.length() == 1 ||
                initialValue != NullEncodedValue.NullValue
            )) {

            writer.write("#the value of this static final field might be set in the static constructor\n");
        }

        writer.write(".field ");
        writeAccessFlags(writer, encodedField);
        writer.write(encodedField.field.getFieldName().getStringValue());
        writer.write(':');
        writer.write(encodedField.field.getFieldType().getTypeDescriptor());
        if (initialValue != null) {
            writer.write(" = ");
            EncodedValueAdaptor.writeTo(writer, initialValue);
        }

        writer.write('\n');

        if (annotationSet != null) {
            writer.indent(4);
            AnnotationFormatter.writeTo(writer, annotationSet);
            writer.deindent(4);
            writer.write(".end field\n");
        }
    }

    private static void writeAccessFlags(IndentingWriter writer, ClassDataItem.EncodedField encodedField)
                                                                                                 throws IOException {
        for (AccessFlags accessFlag: AccessFlags.getAccessFlagsForField(encodedField.accessFlags)) {
            writer.write(accessFlag.toString());
            writer.write(' ');
        }
    }
}
