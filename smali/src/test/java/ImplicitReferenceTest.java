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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import junit.framework.Assert;
import org.antlr.runtime.RecognitionException;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.ValueType;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.Field;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.formats.Instruction21c;
import org.jf.dexlib2.iface.instruction.formats.Instruction35c;
import org.jf.dexlib2.iface.reference.FieldReference;
import org.jf.dexlib2.iface.reference.MethodReference;
import org.jf.dexlib2.iface.value.FieldEncodedValue;
import org.jf.dexlib2.iface.value.MethodEncodedValue;
import org.jf.dexlib2.iface.value.TypeEncodedValue;
import org.jf.smali.SmaliTestUtils;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Tests for method/field references that use an implicit type
 */
public class ImplicitReferenceTest extends SmaliTestUtils {
    @Test
    public void testImplicitMethodReference() throws RecognitionException, IOException {
        ClassDef classDef = SmaliTestUtils.compileSmali("" +
                ".class public LHelloWorld;\n" +
                ".super Ljava/lang/Object;\n" +
                ".method public static main([Ljava/lang/String;)V\n" +
                "    .registers 1\n" +
                "    invoke-static {p0}, toString()V\n" +
                "    invoke-static {p0}, V()V\n" +
                "    invoke-static {p0}, I()V\n" +
                "    return-void\n" +
                ".end method");

        Method mainMethod = null;
        for (Method method: classDef.getMethods()) {
            if (method.getName().equals("main")) {
                mainMethod = method;
            }
        }
        Assert.assertNotNull(mainMethod);

        MethodImplementation methodImpl = mainMethod.getImplementation();
        Assert.assertNotNull(methodImpl);

        List<Instruction> instructions = Lists.newArrayList(methodImpl.getInstructions());

        Instruction35c instruction = (Instruction35c)instructions.get(0);
        Assert.assertNotNull(instruction);
        Assert.assertEquals(Opcode.INVOKE_STATIC, instruction.getOpcode());
        MethodReference method = (MethodReference)instruction.getReference();
        Assert.assertEquals(classDef.getType(), method.getDefiningClass());
        Assert.assertEquals("toString", method.getName());

        instruction = (Instruction35c)instructions.get(1);
        Assert.assertNotNull(instruction);
        Assert.assertEquals(Opcode.INVOKE_STATIC, instruction.getOpcode());
        method = (MethodReference)instruction.getReference();
        Assert.assertEquals(classDef.getType(), method.getDefiningClass());
        Assert.assertEquals("V", method.getName());

        instruction = (Instruction35c)instructions.get(2);
        Assert.assertNotNull(instruction);
        Assert.assertEquals(Opcode.INVOKE_STATIC, instruction.getOpcode());
        method = (MethodReference)instruction.getReference();
        Assert.assertEquals(classDef.getType(), method.getDefiningClass());
        Assert.assertEquals("I", method.getName());
    }

    @Test
    public void testImplicitMethodLiteral() throws RecognitionException, IOException {
        ClassDef classDef = SmaliTestUtils.compileSmali("" +
                ".class public LHelloWorld;\n" +
                ".super Ljava/lang/Object;\n" +
                ".field public static field1:Ljava/lang/reflect/Method; = toString()V\n" +
                ".field public static field2:Ljava/lang/reflect/Method; = V()V\n" +
                ".field public static field3:Ljava/lang/reflect/Method; = I()V\n" +
                ".field public static field4:Ljava/lang/Class; = I");

        Map<String, Field> fields = Maps.newHashMap();
        for (Field field: classDef.getFields()) {
            fields.put(field.getName(), field);
        }

        Field field = fields.get("field1");
        Assert.assertNotNull(field);
        Assert.assertNotNull(field.getInitialValue());
        Assert.assertEquals(ValueType.METHOD, field.getInitialValue().getValueType());
        MethodEncodedValue methodEncodedValue = (MethodEncodedValue)field.getInitialValue();
        Assert.assertEquals(classDef.getType(), methodEncodedValue.getValue().getDefiningClass());
        Assert.assertEquals("toString", methodEncodedValue.getValue().getName());

        field = fields.get("field2");
        Assert.assertNotNull(field);
        Assert.assertNotNull(field.getInitialValue());
        Assert.assertEquals(ValueType.METHOD, field.getInitialValue().getValueType());
        methodEncodedValue = (MethodEncodedValue)field.getInitialValue();
        Assert.assertEquals(classDef.getType(), methodEncodedValue.getValue().getDefiningClass());
        Assert.assertEquals("V", methodEncodedValue.getValue().getName());

        field = fields.get("field3");
        Assert.assertNotNull(field);
        Assert.assertNotNull(field.getInitialValue());
        Assert.assertEquals(ValueType.METHOD, field.getInitialValue().getValueType());
        methodEncodedValue = (MethodEncodedValue)field.getInitialValue();
        Assert.assertEquals(classDef.getType(), methodEncodedValue.getValue().getDefiningClass());
        Assert.assertEquals("I", methodEncodedValue.getValue().getName());

        field = fields.get("field4");
        Assert.assertNotNull(field);
        Assert.assertNotNull(field.getInitialValue());
        Assert.assertEquals(ValueType.TYPE, field.getInitialValue().getValueType());
        TypeEncodedValue typeEncodedValue = (TypeEncodedValue)field.getInitialValue();
        Assert.assertEquals("I", typeEncodedValue.getValue());
    }

    @Test
    public void testImplicitFieldReference() throws RecognitionException, IOException {
        ClassDef classDef = SmaliTestUtils.compileSmali("" +
                ".class public LHelloWorld;\n" +
                ".super Ljava/lang/Object;\n" +
                ".method public static main([Ljava/lang/String;)V\n" +
                "    .registers 1\n" +
                "    sget-object v0, someField:I\n" +
                "    sget-object v0, V:I\n" +
                "    sget-object v0, I:I\n" +
                "    return-void\n" +
                ".end method");

        Method mainMethod = null;
        for (Method method: classDef.getMethods()) {
            if (method.getName().equals("main")) {
                mainMethod = method;
            }
        }
        Assert.assertNotNull(mainMethod);

        MethodImplementation methodImpl = mainMethod.getImplementation();
        Assert.assertNotNull(methodImpl);

        List<Instruction> instructions = Lists.newArrayList(methodImpl.getInstructions());

        Instruction21c instruction = (Instruction21c)instructions.get(0);
        Assert.assertNotNull(instruction);
        Assert.assertEquals(Opcode.SGET_OBJECT, instruction.getOpcode());
        FieldReference field = (FieldReference)instruction.getReference();
        Assert.assertEquals(classDef.getType(), field.getDefiningClass());
        Assert.assertEquals("someField", field.getName());

        instruction = (Instruction21c)instructions.get(1);
        Assert.assertNotNull(instruction);
        Assert.assertEquals(Opcode.SGET_OBJECT, instruction.getOpcode());
        field = (FieldReference)instruction.getReference();
        Assert.assertEquals(classDef.getType(), field.getDefiningClass());
        Assert.assertEquals("V", field.getName());

        instruction = (Instruction21c)instructions.get(2);
        Assert.assertNotNull(instruction);
        Assert.assertEquals(Opcode.SGET_OBJECT, instruction.getOpcode());
        field = (FieldReference)instruction.getReference();
        Assert.assertEquals(classDef.getType(), field.getDefiningClass());
        Assert.assertEquals("I", field.getName());
    }

    @Test
    public void testImplicitFieldLiteral() throws RecognitionException, IOException {
        ClassDef classDef = SmaliTestUtils.compileSmali("" +
                ".class public LHelloWorld;\n" +
                ".super Ljava/lang/Object;\n" +
                ".field public static field1:Ljava/lang/reflect/Field; = someField:I\n" +
                ".field public static field2:Ljava/lang/reflect/Field; = V:I\n" +
                ".field public static field3:Ljava/lang/reflect/Field; = I:I\n");

        Map<String, Field> fields = Maps.newHashMap();
        for (Field field: classDef.getFields()) {
            fields.put(field.getName(), field);
        }

        Field field = fields.get("field1");
        Assert.assertNotNull(field);
        Assert.assertNotNull(field.getInitialValue());
        Assert.assertEquals(ValueType.FIELD, field.getInitialValue().getValueType());
        FieldEncodedValue fieldEncodedValue = (FieldEncodedValue)field.getInitialValue();
        Assert.assertEquals(classDef.getType(), fieldEncodedValue.getValue().getDefiningClass());
        Assert.assertEquals("someField", fieldEncodedValue.getValue().getName());

        field = fields.get("field2");
        Assert.assertNotNull(field);
        Assert.assertNotNull(field.getInitialValue());
        Assert.assertEquals(ValueType.FIELD, field.getInitialValue().getValueType());
        fieldEncodedValue = (FieldEncodedValue)field.getInitialValue();
        Assert.assertEquals(classDef.getType(), fieldEncodedValue.getValue().getDefiningClass());
        Assert.assertEquals("V", fieldEncodedValue.getValue().getName());

        field = fields.get("field3");
        Assert.assertNotNull(field);
        Assert.assertNotNull(field.getInitialValue());
        Assert.assertEquals(ValueType.FIELD, field.getInitialValue().getValueType());
        fieldEncodedValue = (FieldEncodedValue)field.getInitialValue();
        Assert.assertEquals(classDef.getType(), fieldEncodedValue.getValue().getDefiningClass());
        Assert.assertEquals("I", fieldEncodedValue.getValue().getName());
    }
}
