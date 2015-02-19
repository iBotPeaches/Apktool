/*
 * Copyright 2012, Google Inc.
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

package org.jf.dexlib2.writer;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.ReferenceType;
import org.jf.dexlib2.builder.MethodImplementationBuilder;
import org.jf.dexlib2.builder.instruction.BuilderInstruction10x;
import org.jf.dexlib2.builder.instruction.BuilderInstruction21c;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.iface.*;
import org.jf.dexlib2.iface.debug.DebugItem;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.ReferenceInstruction;
import org.jf.dexlib2.iface.instruction.formats.Instruction21c;
import org.jf.dexlib2.iface.reference.Reference;
import org.jf.dexlib2.iface.reference.StringReference;
import org.jf.dexlib2.immutable.instruction.ImmutableInstruction10x;
import org.jf.dexlib2.writer.builder.DexBuilder;
import org.jf.dexlib2.writer.io.MemoryDataStore;
import org.junit.Assert;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.List;

public class JumboStringConversionTest {
    @Test
    public void testJumboStringConversion() throws IOException {
        DexBuilder dexBuilder = DexBuilder.makeDexBuilder(15);

        MethodImplementationBuilder methodBuilder = new MethodImplementationBuilder(1);
        for (int i=0; i<66000; i++) {
            methodBuilder.addInstruction(new BuilderInstruction21c(Opcode.CONST_STRING, 0,
                    dexBuilder.internStringReference(String.format("%08d", i))));
        }
        methodBuilder.addInstruction(new BuilderInstruction10x(Opcode.RETURN_VOID));

        dexBuilder.internClassDef(
                "Ltest;",
                0,
                "Ljava/lang/Object;",
                null,
                null,
                ImmutableSet.<Annotation>of(),
                null,
                ImmutableList.of(
                        dexBuilder.internMethod(
                                "Ltest;",
                                "test",
                                null,
                                "V",
                                0,
                                ImmutableSet.<Annotation>of(),
                                methodBuilder.getMethodImplementation())));

        MemoryDataStore dexStore = new MemoryDataStore();
        dexBuilder.writeTo(dexStore);

        DexBackedDexFile dexFile = new DexBackedDexFile(new Opcodes(15, false), dexStore.getData());

        ClassDef classDef = Iterables.getFirst(dexFile.getClasses(), null);
        Assert.assertNotNull(classDef);

        Method method = Iterables.getFirst(classDef.getMethods(), null);
        Assert.assertNotNull(method);

        MethodImplementation impl = method.getImplementation();
        Assert.assertNotNull(impl);

        List<? extends Instruction> instructions = Lists.newArrayList(impl.getInstructions());
        Assert.assertEquals(66001, instructions.size());

        for (int i=0; i<65536; i++) {
            Assert.assertEquals(Opcode.CONST_STRING, instructions.get(i).getOpcode());
            Assert.assertEquals(String.format("%08d", i),
                    ((StringReference)((ReferenceInstruction)instructions.get(i)).getReference()).getString());
        }
        for (int i=65536; i<66000; i++) {
            Assert.assertEquals(Opcode.CONST_STRING_JUMBO, instructions.get(i).getOpcode());
            Assert.assertEquals(String.format("%08d", i),
                    ((StringReference)((ReferenceInstruction)instructions.get(i)).getReference()).getString());
        }
        Assert.assertEquals(Opcode.RETURN_VOID, instructions.get(66000).getOpcode());
    }


    @Test
    public void testJumboStringConversion_NonMethodBuilder() throws IOException {
        DexBuilder dexBuilder = DexBuilder.makeDexBuilder(15);

        final List<Instruction> instructions = Lists.newArrayList();
        for (int i=0; i<66000; i++) {
            final StringReference ref = dexBuilder.internStringReference(String.format("%08d", i));

            instructions.add(new Instruction21c() {
                @Override public int getRegisterA() {
                    return 0;
                }

                @Nonnull @Override public Reference getReference() {
                    return ref;
                }

                @Override public int getReferenceType() { return ReferenceType.STRING; }

                @Override public Opcode getOpcode() {
                    return Opcode.CONST_STRING;
                }

                @Override public int getCodeUnits() {
                    return getOpcode().format.size / 2;
                }
            });
        }
        instructions.add(new ImmutableInstruction10x(Opcode.RETURN_VOID));

        MethodImplementation methodImpl = new MethodImplementation() {
            @Override public int getRegisterCount() {
                return 1;
            }

            @Nonnull @Override public Iterable<? extends Instruction> getInstructions() {
                return instructions;
            }

            @Nonnull @Override public List<? extends TryBlock<? extends ExceptionHandler>> getTryBlocks() {
                return ImmutableList.of();
            }

            @Nonnull @Override public Iterable<? extends DebugItem> getDebugItems() {
                return ImmutableList.of();
            }
        };

        dexBuilder.internClassDef(
                "Ltest;",
                0,
                "Ljava/lang/Object;",
                null,
                null,
                ImmutableSet.<Annotation>of(),
                null,
                ImmutableList.of(
                        dexBuilder.internMethod(
                                "Ltest;",
                                "test",
                                null,
                                "V",
                                0,
                                ImmutableSet.<Annotation>of(),
                                methodImpl)));

        MemoryDataStore dexStore = new MemoryDataStore();
        dexBuilder.writeTo(dexStore);

        DexBackedDexFile dexFile = new DexBackedDexFile(new Opcodes(15, false), dexStore.getData());

        ClassDef classDef = Iterables.getFirst(dexFile.getClasses(), null);
        Assert.assertNotNull(classDef);

        Method method = Iterables.getFirst(classDef.getMethods(), null);
        Assert.assertNotNull(method);

        MethodImplementation impl = method.getImplementation();
        Assert.assertNotNull(impl);

        List<? extends Instruction> actualInstructions = Lists.newArrayList(impl.getInstructions());
        Assert.assertEquals(66001, actualInstructions.size());

        for (int i=0; i<65536; i++) {
            Assert.assertEquals(Opcode.CONST_STRING, actualInstructions.get(i).getOpcode());
            Assert.assertEquals(String.format("%08d", i),
                    ((StringReference)((ReferenceInstruction)actualInstructions.get(i)).getReference()).getString());
        }
        for (int i=65536; i<66000; i++) {
            Assert.assertEquals(Opcode.CONST_STRING_JUMBO, actualInstructions.get(i).getOpcode());
            Assert.assertEquals(String.format("%08d", i),
                    ((StringReference)((ReferenceInstruction)actualInstructions.get(i)).getReference()).getString());
        }
        Assert.assertEquals(Opcode.RETURN_VOID, actualInstructions.get(66000).getOpcode());
    }
}
