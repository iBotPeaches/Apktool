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

import org.jf.dexlib2.iface.AnnotationElement;
import org.jf.dexlib2.iface.value.AnnotationEncodedValue;
import org.jf.util.IndentingWriter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Collection;

public abstract class AnnotationEncodedValueAdaptor {

    public static void writeTo(@Nonnull IndentingWriter writer,
                               @Nonnull AnnotationEncodedValue annotationEncodedValue,
                               @Nullable String containingClass) throws IOException {
        writer.write(".subannotation ");
        writer.write(annotationEncodedValue.getType());
        writer.write('\n');

        writeElementsTo(writer, annotationEncodedValue.getElements(), containingClass);
        writer.write(".end subannotation");
    }

    public static void writeElementsTo(@Nonnull IndentingWriter writer,
                                       @Nonnull Collection<? extends AnnotationElement> annotationElements,
                                       @Nullable String containingClass) throws IOException {
        writer.indent(4);
        for (AnnotationElement annotationElement: annotationElements) {
            writer.write(annotationElement.getName());
            writer.write(" = ");
            EncodedValueAdaptor.writeTo(writer, annotationElement.getValue(), containingClass);
            writer.write('\n');
        }
        writer.deindent(4);
    }
}
