/*
 * Copyright 2015, Google Inc.
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

package org.jf.baksmali;

import junit.framework.Assert;

import org.antlr.runtime.RecognitionException;
import org.jf.baksmali.Adaptors.ClassDefinition;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.smali.SmaliTestUtils;
import org.jf.util.IndentingWriter;
import org.jf.util.TextUtils;

import java.io.IOException;
import java.io.StringWriter;

public class BaksmaliTestUtils {
    public static void assertSmaliCompiledEquals(String source, String expected,
            baksmaliOptions options, boolean stripComments) throws IOException,
            RecognitionException {
        ClassDef classDef = SmaliTestUtils.compileSmali(source, options.apiLevel,
                options.experimental);

        StringWriter stringWriter = new StringWriter();
        IndentingWriter writer = new IndentingWriter(stringWriter);
        ClassDefinition classDefinition = new ClassDefinition(options, classDef);
        classDefinition.writeTo(writer);
        writer.close();

        // Remove unnecessary whitespace and optionally strip all comments from smali file
        String normalizedExpected = expected;
        if (stripComments) {
            normalizedExpected = TextUtils.stripComments(normalizedExpected);
        }
        normalizedExpected = TextUtils.normalizeWhitespace(normalizedExpected);

        String normalizedActual = stringWriter.toString();
        if (stripComments) {
            normalizedActual = TextUtils.stripComments(normalizedActual);
        }
        normalizedActual = TextUtils.normalizeWhitespace(normalizedActual);

        // Assert that normalized strings are now equal
        Assert.assertEquals(normalizedExpected, normalizedActual);
    }

    public static void assertSmaliCompiledEquals(String source, String expected,
            baksmaliOptions options) throws IOException, RecognitionException {
        assertSmaliCompiledEquals(source, expected, options, false);
    }

    public static void assertSmaliCompiledEquals(String source, String expected)
            throws IOException, RecognitionException {
        baksmaliOptions options = new baksmaliOptions();
        assertSmaliCompiledEquals(source, expected, options);
    }

    // Static helpers class; do not instantiate.
    private BaksmaliTestUtils() { throw new AssertionError(); }
}
