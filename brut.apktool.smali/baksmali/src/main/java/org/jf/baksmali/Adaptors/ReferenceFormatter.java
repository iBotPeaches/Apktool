/*
 * [The "BSD licence"]
 * Copyright (c) 2010 Ben Gruver
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

import org.jf.util.IndentingWriter;
import org.jf.dexlib.*;
import org.jf.dexlib.Util.Utf8Utils;

import java.io.IOException;

public class ReferenceFormatter {
    public static void writeReference(IndentingWriter writer, Item item) throws IOException {
        switch (item.getItemType()) {
            case TYPE_METHOD_ID_ITEM:
                writeMethodReference(writer, (MethodIdItem)item);
                return;
            case TYPE_FIELD_ID_ITEM:
                writeFieldReference(writer, (FieldIdItem)item);
                return;
            case TYPE_STRING_ID_ITEM:
                writeStringReference(writer, (StringIdItem)item);
                return;
            case TYPE_TYPE_ID_ITEM:
                writeTypeReference(writer, (TypeIdItem)item);
                return;
        }
    }

    public static void writeMethodReference(IndentingWriter writer, MethodIdItem item) throws IOException {
        writer.write(item.getContainingClass().getTypeDescriptor());
        writer.write("->");
        writer.write(item.getMethodName().getStringValue());
        writer.write(item.getPrototype().getPrototypeString());
    }

    public static void writeFieldReference(IndentingWriter writer, FieldIdItem item) throws IOException {
        writer.write(item.getContainingClass().getTypeDescriptor());
        writer.write("->");
        writer.write(item.getFieldName().getStringValue());
        writer.write(':');
        writer.write(item.getFieldType().getTypeDescriptor());
    }

    public static void writeStringReference(IndentingWriter writer, StringIdItem item) throws IOException {
        writer.write('"');
        Utf8Utils.writeEscapedString(writer, item.getStringValue());
        writer.write('"');
    }

    public static void writeTypeReference(IndentingWriter writer, TypeIdItem item) throws IOException {
        writer.write(item.getTypeDescriptor());
    }
}
