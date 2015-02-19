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

package org.jf.dexlib2.analysis;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.jf.dexlib2.AccessFlags;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.DexFile;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.formats.Instruction35c;
import org.jf.dexlib2.iface.reference.MethodReference;
import org.jf.dexlib2.immutable.ImmutableClassDef;
import org.jf.dexlib2.immutable.ImmutableDexFile;
import org.jf.dexlib2.immutable.ImmutableMethod;
import org.jf.dexlib2.immutable.ImmutableMethodImplementation;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction10x;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction35mi;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class CustomMethodInlineTableTest {
    @Test
    public void testCustomMethodInlineTable_Virtual() {
        List<ImmutableInstruction> instructions = Lists.newArrayList(
                new ImmutableInstruction35mi(Opcode.EXECUTE_INLINE, 1, 0, 0, 0, 0, 0, 0),
                new ImmutableInstruction10x(Opcode.RETURN_VOID));

        ImmutableMethodImplementation methodImpl = new ImmutableMethodImplementation(1, instructions, null, null);
        ImmutableMethod method = new ImmutableMethod("Lblah;", "blah", null, "V", AccessFlags.PUBLIC.getValue(), null,
                methodImpl);

        ClassDef classDef = new ImmutableClassDef("Lblah;", AccessFlags.PUBLIC.getValue(), "Ljava/lang/Object;", null,
                null, null, null, null, null, ImmutableList.of(method));

        DexFile dexFile = new ImmutableDexFile(ImmutableList.of(classDef));

        ClassPath classPath = ClassPath.fromClassPath(ImmutableList.<String>of(), ImmutableList.<String>of(), dexFile,
                15, false);
        InlineMethodResolver inlineMethodResolver = new CustomInlineMethodResolver(classPath, "Lblah;->blah()V");
        MethodAnalyzer methodAnalyzer = new MethodAnalyzer(classPath, method, inlineMethodResolver);

        Instruction deodexedInstruction = methodAnalyzer.getInstructions().get(0);
        Assert.assertEquals(Opcode.INVOKE_VIRTUAL, deodexedInstruction.getOpcode());

        MethodReference methodReference = (MethodReference)((Instruction35c)deodexedInstruction).getReference();
        Assert.assertEquals(method, methodReference);
    }

    @Test
    public void testCustomMethodInlineTable_Static() {
        List<ImmutableInstruction> instructions = Lists.newArrayList(
                new ImmutableInstruction35mi(Opcode.EXECUTE_INLINE, 1, 0, 0, 0, 0, 0, 0),
                new ImmutableInstruction10x(Opcode.RETURN_VOID));

        ImmutableMethodImplementation methodImpl = new ImmutableMethodImplementation(1, instructions, null, null);
        ImmutableMethod method = new ImmutableMethod("Lblah;", "blah", null, "V", AccessFlags.STATIC.getValue(), null,
                methodImpl);

        ClassDef classDef = new ImmutableClassDef("Lblah;", AccessFlags.PUBLIC.getValue(), "Ljava/lang/Object;", null,
                null, null, null, null, ImmutableList.of(method), null);

        DexFile dexFile = new ImmutableDexFile(ImmutableList.of(classDef));

        ClassPath classPath = ClassPath.fromClassPath(ImmutableList.<String>of(), ImmutableList.<String>of(), dexFile,
                15, false);
        InlineMethodResolver inlineMethodResolver = new CustomInlineMethodResolver(classPath, "Lblah;->blah()V");
        MethodAnalyzer methodAnalyzer = new MethodAnalyzer(classPath, method, inlineMethodResolver);

        Instruction deodexedInstruction = methodAnalyzer.getInstructions().get(0);
        Assert.assertEquals(Opcode.INVOKE_STATIC, deodexedInstruction.getOpcode());

        MethodReference methodReference = (MethodReference)((Instruction35c)deodexedInstruction).getReference();
        Assert.assertEquals(method, methodReference);
    }

    @Test
    public void testCustomMethodInlineTable_Direct() {
        List<ImmutableInstruction> instructions = Lists.newArrayList(
                new ImmutableInstruction35mi(Opcode.EXECUTE_INLINE, 1, 0, 0, 0, 0, 0, 0),
                new ImmutableInstruction10x(Opcode.RETURN_VOID));

        ImmutableMethodImplementation methodImpl = new ImmutableMethodImplementation(1, instructions, null, null);
        ImmutableMethod method = new ImmutableMethod("Lblah;", "blah", null, "V", AccessFlags.PRIVATE.getValue(), null,
                methodImpl);

        ClassDef classDef = new ImmutableClassDef("Lblah;", AccessFlags.PUBLIC.getValue(), "Ljava/lang/Object;", null,
                null, null, null, null, ImmutableList.of(method), null);

        DexFile dexFile = new ImmutableDexFile(ImmutableList.of(classDef));

        ClassPath classPath = ClassPath.fromClassPath(ImmutableList.<String>of(), ImmutableList.<String>of(), dexFile,
                15, false);
        InlineMethodResolver inlineMethodResolver = new CustomInlineMethodResolver(classPath, "Lblah;->blah()V");
        MethodAnalyzer methodAnalyzer = new MethodAnalyzer(classPath, method, inlineMethodResolver);

        Instruction deodexedInstruction = methodAnalyzer.getInstructions().get(0);
        Assert.assertEquals(Opcode.INVOKE_DIRECT, deodexedInstruction.getOpcode());

        MethodReference methodReference = (MethodReference)((Instruction35c)deodexedInstruction).getReference();
        Assert.assertEquals(method, methodReference);
    }
}
