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

import org.jf.baksmali.Adaptors.EncodedValue.AnnotationEncodedValueAdaptor;
import org.jf.util.IndentingWriter;
import org.jf.dexlib.AnnotationItem;
import org.jf.dexlib.AnnotationSetItem;

import java.io.IOException;


public class AnnotationFormatter {

    public static void writeTo(IndentingWriter writer, AnnotationSetItem annotationSet) throws IOException {
        boolean first = true;
        for (AnnotationItem annotationItem: annotationSet.getAnnotations()) {
            if (!first) {
                writer.write('\n');
            }
            first = false;

            writeTo(writer, annotationItem);
        }
    }

    public static void writeTo(IndentingWriter writer, AnnotationItem annotationItem) throws IOException {
        writer.write(".annotation ");
        writer.write(annotationItem.getVisibility().visibility);
        writer.write(' ');
        ReferenceFormatter.writeTypeReference(writer, annotationItem.getEncodedAnnotation().annotationType);
        writer.write('\n');

        AnnotationEncodedValueAdaptor.writeElementsTo(writer, annotationItem.getEncodedAnnotation());

        writer.write(".end annotation\n");
    }
}
