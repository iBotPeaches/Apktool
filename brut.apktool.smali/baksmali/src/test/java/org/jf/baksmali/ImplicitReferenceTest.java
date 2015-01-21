/*
 * Copyright 2014, Google Inc.
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
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;

public class ImplicitReferenceTest {
    @Test
    public void testImplicitMethodReferences() throws IOException, RecognitionException {
        ClassDef classDef = SmaliTestUtils.compileSmali("" +
                ".class public LHelloWorld;\n" +
                ".super Ljava/lang/Object;\n" +
                ".method public static main([Ljava/lang/String;)V\n" +
                "    .registers 1\n" +
                "    invoke-static {p0}, LHelloWorld;->toString()V\n" +
                "    invoke-static {p0}, LHelloWorld;->V()V\n" +
                "    invoke-static {p0}, LHelloWorld;->I()V\n" +
                "    return-void\n" +
                ".end method");

        String expected = "" +
                ".class public LHelloWorld;\n" +
                ".super Ljava/lang/Object;\n" +
                "# direct methods\n" +
                ".method public static main([Ljava/lang/String;)V\n" +
                ".registers 1\n" +
                "invoke-static {p0}, toString()V\n" +
                "invoke-static {p0}, V()V\n" +
                "invoke-static {p0}, I()V\n" +
                "return-void\n" +
                ".end method\n";

        baksmaliOptions options = new baksmaliOptions();
        options.useImplicitReferences = true;

        StringWriter stringWriter = new StringWriter();
        IndentingWriter writer = new IndentingWriter(stringWriter);
        ClassDefinition classDefinition = new ClassDefinition(options, classDef);
        classDefinition.writeTo(writer);
        writer.close();

        Assert.assertEquals(TextUtils.normalizeWhitespace(expected),
                TextUtils.normalizeWhitespace(stringWriter.toString()));
    }

    @Test
    public void testExplicitMethodReferences() throws IOException, RecognitionException {
        ClassDef classDef = SmaliTestUtils.compileSmali("" +
                ".class public LHelloWorld;\n" +
                ".super Ljava/lang/Object;\n" +
                ".method public static main([Ljava/lang/String;)V\n" +
                "    .registers 1\n" +
                "    invoke-static {p0}, LHelloWorld;->toString()V\n" +
                "    invoke-static {p0}, LHelloWorld;->V()V\n" +
                "    invoke-static {p0}, LHelloWorld;->I()V\n" +
                "    return-void\n" +
                ".end method");

        String expected = "" +
                ".class public LHelloWorld;\n" +
                ".super Ljava/lang/Object;\n" +
                "# direct methods\n" +
                ".method public static main([Ljava/lang/String;)V\n" +
                "    .registers 1\n" +
                "    invoke-static {p0}, LHelloWorld;->toString()V\n" +
                "    invoke-static {p0}, LHelloWorld;->V()V\n" +
                "    invoke-static {p0}, LHelloWorld;->I()V\n" +
                "    return-void\n" +
                ".end method\n";

        baksmaliOptions options = new baksmaliOptions();
        options.useImplicitReferences = false;

        StringWriter stringWriter = new StringWriter();
        IndentingWriter writer = new IndentingWriter(stringWriter);
        ClassDefinition classDefinition = new ClassDefinition(options, classDef);
        classDefinition.writeTo(writer);
        writer.close();

        Assert.assertEquals(TextUtils.normalizeWhitespace(expected),
                TextUtils.normalizeWhitespace(stringWriter.toString()));
    }

    @Test
    public void testImplicitMethodLiterals() throws IOException, RecognitionException {
        ClassDef classDef = SmaliTestUtils.compileSmali("" +
                ".class public LHelloWorld;\n" +
                ".super Ljava/lang/Object;\n" +
                ".field public static field1:Ljava/lang/reflect/Method; = LHelloWorld;->toString()V\n" +
                ".field public static field2:Ljava/lang/reflect/Method; = LHelloWorld;->V()V\n" +
                ".field public static field3:Ljava/lang/reflect/Method; = LHelloWorld;->I()V\n" +
                ".field public static field4:Ljava/lang/Class; = I");

        String expected = "" +
                ".class public LHelloWorld;\n" +
                ".super Ljava/lang/Object;\n" +
                "# static fields\n" +
                ".field public static field1:Ljava/lang/reflect/Method; = toString()V\n" +
                ".field public static field2:Ljava/lang/reflect/Method; = V()V\n" +
                ".field public static field3:Ljava/lang/reflect/Method; = I()V\n" +
                ".field public static field4:Ljava/lang/Class; = I\n";

        baksmaliOptions options = new baksmaliOptions();
        options.useImplicitReferences = true;

        StringWriter stringWriter = new StringWriter();
        IndentingWriter writer = new IndentingWriter(stringWriter);
        ClassDefinition classDefinition = new ClassDefinition(options, classDef);
        classDefinition.writeTo(writer);
        writer.close();

        Assert.assertEquals(TextUtils.normalizeWhitespace(expected),
                TextUtils.normalizeWhitespace(stringWriter.toString()));
    }

    @Test
    public void testExplicitMethodLiterals() throws IOException, RecognitionException {
        ClassDef classDef = SmaliTestUtils.compileSmali("" +
                ".class public LHelloWorld;\n" +
                ".super Ljava/lang/Object;\n" +
                ".field public static field1:Ljava/lang/reflect/Method; = LHelloWorld;->toString()V\n" +
                ".field public static field2:Ljava/lang/reflect/Method; = LHelloWorld;->V()V\n" +
                ".field public static field3:Ljava/lang/reflect/Method; = LHelloWorld;->I()V\n" +
                ".field public static field4:Ljava/lang/Class; = I");

        String expected = "" +
                ".class public LHelloWorld;\n" +
                ".super Ljava/lang/Object;\n" +
                "# static fields\n" +
                ".field public static field1:Ljava/lang/reflect/Method; = LHelloWorld;->toString()V\n" +
                ".field public static field2:Ljava/lang/reflect/Method; = LHelloWorld;->V()V\n" +
                ".field public static field3:Ljava/lang/reflect/Method; = LHelloWorld;->I()V\n" +
                ".field public static field4:Ljava/lang/Class; = I\n";

        baksmaliOptions options = new baksmaliOptions();
        options.useImplicitReferences = false;

        StringWriter stringWriter = new StringWriter();
        IndentingWriter writer = new IndentingWriter(stringWriter);
        ClassDefinition classDefinition = new ClassDefinition(options, classDef);
        classDefinition.writeTo(writer);
        writer.close();

        Assert.assertEquals(TextUtils.normalizeWhitespace(expected),
                TextUtils.normalizeWhitespace(stringWriter.toString()));
    }

    @Test
    public void testImplicitFieldReferences() throws IOException, RecognitionException {
        ClassDef classDef = SmaliTestUtils.compileSmali("" +
                ".class public LHelloWorld;\n" +
                ".super Ljava/lang/Object;\n" +
                ".method public static main([Ljava/lang/String;)V\n" +
                "    .registers 1\n" +
                "    sget v0, LHelloWorld;->someField:I\n" +
                "    sget v0, LHelloWorld;->I:I\n" +
                "    sget v0, LHelloWorld;->V:I\n" +
                "    return-void\n" +
                ".end method");

        String expected = "" +
                ".class public LHelloWorld;\n" +
                ".super Ljava/lang/Object;\n" +
                "# direct methods\n" +
                ".method public static main([Ljava/lang/String;)V\n" +
                "    .registers 1\n" +
                "    sget p0, someField:I\n" +
                "    sget p0, I:I\n" +
                "    sget p0, V:I\n" +
                "    return-void\n" +
                ".end method\n";

        baksmaliOptions options = new baksmaliOptions();
        options.useImplicitReferences = true;

        StringWriter stringWriter = new StringWriter();
        IndentingWriter writer = new IndentingWriter(stringWriter);
        ClassDefinition classDefinition = new ClassDefinition(options, classDef);
        classDefinition.writeTo(writer);
        writer.close();

        Assert.assertEquals(TextUtils.normalizeWhitespace(expected),
                TextUtils.normalizeWhitespace(stringWriter.toString()));
    }

    @Test
    public void testExplicitFieldReferences() throws IOException, RecognitionException {
        ClassDef classDef = SmaliTestUtils.compileSmali("" +
                ".class public LHelloWorld;\n" +
                ".super Ljava/lang/Object;\n" +
                ".method public static main([Ljava/lang/String;)V\n" +
                "    .registers 1\n" +
                "    sget v0, LHelloWorld;->someField:I\n" +
                "    sget v0, LHelloWorld;->I:I\n" +
                "    sget v0, LHelloWorld;->V:I\n" +
                "    return-void\n" +
                ".end method");

        String expected = "" +
                ".class public LHelloWorld;\n" +
                ".super Ljava/lang/Object;\n" +
                "# direct methods\n" +
                ".method public static main([Ljava/lang/String;)V\n" +
                "    .registers 1\n" +
                "    sget p0, LHelloWorld;->someField:I\n" +
                "    sget p0, LHelloWorld;->I:I\n" +
                "    sget p0, LHelloWorld;->V:I\n" +
                "    return-void\n" +
                ".end method\n";

        baksmaliOptions options = new baksmaliOptions();
        options.useImplicitReferences = false;

        StringWriter stringWriter = new StringWriter();
        IndentingWriter writer = new IndentingWriter(stringWriter);
        ClassDefinition classDefinition = new ClassDefinition(options, classDef);
        classDefinition.writeTo(writer);
        writer.close();

        Assert.assertEquals(TextUtils.normalizeWhitespace(expected),
                TextUtils.normalizeWhitespace(stringWriter.toString()));
    }

    @Test
    public void testImplicitFieldLiterals() throws IOException, RecognitionException {
        ClassDef classDef = SmaliTestUtils.compileSmali("" +
                ".class public LHelloWorld;\n" +
                ".super Ljava/lang/Object;\n" +
                ".field public static field1:Ljava/lang/reflect/Field; = LHelloWorld;->someField:I\n" +
                ".field public static field2:Ljava/lang/reflect/Field; = LHelloWorld;->V:I\n" +
                ".field public static field3:Ljava/lang/reflect/Field; = LHelloWorld;->I:I");

        String expected = "" +
                ".class public LHelloWorld;\n" +
                ".super Ljava/lang/Object;\n" +
                "# static fields\n" +
                ".field public static field1:Ljava/lang/reflect/Field; = someField:I\n" +
                ".field public static field2:Ljava/lang/reflect/Field; = V:I\n" +
                ".field public static field3:Ljava/lang/reflect/Field; = I:I\n";

        baksmaliOptions options = new baksmaliOptions();
        options.useImplicitReferences = true;

        StringWriter stringWriter = new StringWriter();
        IndentingWriter writer = new IndentingWriter(stringWriter);
        ClassDefinition classDefinition = new ClassDefinition(options, classDef);
        classDefinition.writeTo(writer);
        writer.close();

        Assert.assertEquals(TextUtils.normalizeWhitespace(expected),
                TextUtils.normalizeWhitespace(stringWriter.toString()));
    }

    @Test
    public void testExplicitFieldLiterals() throws IOException, RecognitionException {
        ClassDef classDef = SmaliTestUtils.compileSmali("" +
                ".class public LHelloWorld;\n" +
                ".super Ljava/lang/Object;\n" +
                ".field public static field1:Ljava/lang/reflect/Field; = LHelloWorld;->someField:I\n" +
                ".field public static field2:Ljava/lang/reflect/Field; = LHelloWorld;->V:I\n" +
                ".field public static field3:Ljava/lang/reflect/Field; = LHelloWorld;->I:I");

        String expected = "" +
                ".class public LHelloWorld;\n" +
                ".super Ljava/lang/Object;\n" +
                "# static fields\n" +
                ".field public static field1:Ljava/lang/reflect/Field; = LHelloWorld;->someField:I\n" +
                ".field public static field2:Ljava/lang/reflect/Field; = LHelloWorld;->V:I\n" +
                ".field public static field3:Ljava/lang/reflect/Field; = LHelloWorld;->I:I\n";

        baksmaliOptions options = new baksmaliOptions();
        options.useImplicitReferences = false;

        StringWriter stringWriter = new StringWriter();
        IndentingWriter writer = new IndentingWriter(stringWriter);
        ClassDefinition classDefinition = new ClassDefinition(options, classDef);
        classDefinition.writeTo(writer);
        writer.close();

        Assert.assertEquals(TextUtils.normalizeWhitespace(expected),
                TextUtils.normalizeWhitespace(stringWriter.toString()));
    }
}
