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

import com.google.common.collect.Lists;
import junit.framework.Assert;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.SwitchElement;
import org.jf.dexlib2.iface.instruction.formats.*;
import org.jf.dexlib2.iface.reference.Reference;
import org.jf.dexlib2.iface.reference.StringReference;
import org.jf.dexlib2.immutable.ImmutableMethodImplementation;
import org.jf.dexlib2.immutable.instruction.*;
import org.jf.dexlib2.immutable.reference.ImmutableStringReference;
import org.jf.dexlib2.writer.util.InstructionWriteUtil;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.util.ArrayList;

public class JumboStringConversionTest {
    private static final int MIN_NUM_JUMBO_STRINGS = 2;

    private MockStringIndexProvider mockStringIndexProvider;
    ArrayList<String> mJumboStrings;

    private class InsnWriteUtil extends InstructionWriteUtil<Instruction, StringReference, Reference> {
        public InsnWriteUtil(@Nonnull MethodImplementation implementation) {
            super(implementation.getInstructions(), mockStringIndexProvider, ImmutableInstructionFactory.INSTANCE);
        }
    }

    @Before
    public void setup() {
        mockStringIndexProvider = new MockStringIndexProvider();
        StringBuilder stringBuilder = new StringBuilder("a");
        mJumboStrings = Lists.newArrayList();
        int index = 0;

        // populate StringPool, make sure there are more than 64k+MIN_NUM_JUMBO_STRINGS strings
        while (mJumboStrings.size()<MIN_NUM_JUMBO_STRINGS) {
            for (int pos=stringBuilder.length()-1;pos>=0;pos--) {
                for (char ch='a';ch<='z';ch++) {
                    stringBuilder.setCharAt(pos, ch);
                    mockStringIndexProvider.intern(stringBuilder.toString(), index++);
                    if (mockStringIndexProvider.getNumItems()>0xFFFF) {
                        mJumboStrings.add(stringBuilder.toString());
                    }
                }
            }

            stringBuilder.setLength(stringBuilder.length()+1);
            for (int pos=0;pos<stringBuilder.length();pos++) {
                stringBuilder.setCharAt(pos, 'a');
            }
        }
    }

    @Test
    public void testInstruction21c() {
        ArrayList<ImmutableInstruction> instructions = Lists.newArrayList();
        instructions.add(new ImmutableInstruction21c(Opcode.CONST_STRING, 0,
                new ImmutableStringReference(mJumboStrings.get(0))));

        ImmutableMethodImplementation methodImplementation =
                new ImmutableMethodImplementation(1, instructions, null, null);
        InsnWriteUtil writeUtil = new InsnWriteUtil(methodImplementation);

        for (Instruction instr: writeUtil.getInstructions()) {
            Assert.assertEquals("Jumbo string conversion was not performed!",
                    instr.getOpcode(), Opcode.CONST_STRING_JUMBO);
        }
    }

    private ArrayList<ImmutableInstruction> createSimpleInstructionList() {
        ArrayList<ImmutableInstruction> instructions = Lists.newArrayList();
        instructions.add(new ImmutableInstruction21c(Opcode.CONST_STRING, 0, new ImmutableStringReference(mJumboStrings.get(0))));
        instructions.add(new ImmutableInstruction21c(Opcode.CONST_STRING, 0, new ImmutableStringReference(mJumboStrings.get(1))));
        instructions.add(new ImmutableInstruction10x(Opcode.NOP));

        ArrayList<SwitchElement> switchElements = Lists.newArrayList();
        switchElements.add(new ImmutableSwitchElement(0, 5));
        instructions.add(new ImmutablePackedSwitchPayload(switchElements));
        instructions.add(new ImmutableSparseSwitchPayload(switchElements));

        return instructions;
    }

    @Test
    public void testInstruction10tSimple() {
        ArrayList<ImmutableInstruction> instructions = createSimpleInstructionList();
        instructions.add(1, new ImmutableInstruction10t(Opcode.GOTO, 3));

        ImmutableMethodImplementation methodImplementation = new ImmutableMethodImplementation(1, instructions, null, null);
        InsnWriteUtil writeUtil = new InsnWriteUtil(methodImplementation);

        for (Instruction instr: writeUtil.getInstructions()) {
            if (instr instanceof Instruction10t) {
                Instruction10t instruction = (Instruction10t) instr;
                Assert.assertEquals("goto (Format10t) target was not modified properly", instruction.getCodeOffset(), 4);
                break;
            }
        }
    }

    @Test
    public void testInstruction20tSimple() {
        ArrayList<ImmutableInstruction> instructions = createSimpleInstructionList();
        instructions.add(1, new ImmutableInstruction20t(Opcode.GOTO_16, 4));

        ImmutableMethodImplementation methodImplementation = new ImmutableMethodImplementation(1, instructions, null, null);
        InsnWriteUtil writeUtil = new InsnWriteUtil(methodImplementation);

        for (Instruction instr: writeUtil.getInstructions()) {
            if (instr instanceof Instruction20t) {
                Instruction20t instruction = (Instruction20t) instr;
                Assert.assertEquals("goto/16 (Format20t) target was not modified properly", instruction.getCodeOffset(), 5);
                break;
            }
        }
    }

    @Test
    public void testInstruction30t() {
        ArrayList<ImmutableInstruction> instructions = createSimpleInstructionList();
        instructions.add(1, new ImmutableInstruction30t(Opcode.GOTO_32, 5));

        ImmutableMethodImplementation methodImplementation = new ImmutableMethodImplementation(1, instructions, null, null);
        InsnWriteUtil writeUtil = new InsnWriteUtil(methodImplementation);

        for (Instruction instr: writeUtil.getInstructions()) {
            if (instr instanceof Instruction30t) {
                Instruction30t instruction = (Instruction30t) instr;
                Assert.assertEquals("goto/32 (Format30t) target was not modified properly", instruction.getCodeOffset(), 6);
                break;
            }
        }
    }

    @Test
    public void testInstruction21t() {
        ArrayList<ImmutableInstruction> instructions = createSimpleInstructionList();
        instructions.add(1, new ImmutableInstruction21t(Opcode.IF_EQZ, 0, 4));

        ImmutableMethodImplementation methodImplementation = new ImmutableMethodImplementation(1, instructions, null, null);
        InsnWriteUtil writeUtil = new InsnWriteUtil(methodImplementation);

        for (Instruction instr: writeUtil.getInstructions()) {
            if (instr instanceof Instruction21t) {
                Instruction21t instruction = (Instruction21t) instr;
                Assert.assertEquals("branch instruction (Format21t) target was not modified properly", instruction.getCodeOffset(), 5);
                break;
            }
        }
    }

    @Test
    public void testInstruction22t() {
        ArrayList<ImmutableInstruction> instructions = createSimpleInstructionList();
        instructions.add(1, new ImmutableInstruction22t(Opcode.IF_EQ, 0, 1, 4));

        ImmutableMethodImplementation methodImplementation = new ImmutableMethodImplementation(1, instructions, null, null);
        InsnWriteUtil writeUtil = new InsnWriteUtil(methodImplementation);

        for (Instruction instr: writeUtil.getInstructions()) {
            if (instr instanceof Instruction22t) {
                Instruction22t instruction = (Instruction22t) instr;
                Assert.assertEquals("branch instruction (Format22t) target was not modified properly", instruction.getCodeOffset(), 5);
                break;
            }
        }
    }

    @Test
    public void testInstruction31t() {
        ArrayList<ImmutableInstruction> instructions = createSimpleInstructionList();
        instructions.add(1, new ImmutableInstruction31t(Opcode.PACKED_SWITCH, 0, 5));

        ImmutableMethodImplementation methodImplementation = new ImmutableMethodImplementation(1, instructions, null, null);
        InsnWriteUtil writeUtil = new InsnWriteUtil(methodImplementation);

        for (Instruction instr: writeUtil.getInstructions()) {
            if (instr instanceof Instruction31t) {
                Instruction31t instruction = (Instruction31t) instr;
                Assert.assertEquals("branch instruction (Format31t) target was not modified properly", instruction.getCodeOffset(), 6);
                break;
            }
        }
    }

    @Test
    public void testPackedSwitchPayload() {
        ArrayList<ImmutableInstruction> instructions = createSimpleInstructionList();
        instructions.add(1, new ImmutableInstruction31t(Opcode.PACKED_SWITCH, 0, 6));

        ImmutableMethodImplementation methodImplementation = new ImmutableMethodImplementation(1, instructions, null, null);
        InsnWriteUtil writeUtil = new InsnWriteUtil(methodImplementation);

        for (Instruction instr: writeUtil.getInstructions()) {
            if (instr instanceof PackedSwitchPayload) {
                PackedSwitchPayload instruction = (PackedSwitchPayload) instr;
                for (SwitchElement switchElement: instruction.getSwitchElements()) {
                    Assert.assertEquals("packed switch payload offset was not modified properly", switchElement.getOffset(), 6);
                }
                break;
            }
        }
    }

    @Test
    public void testSparseSwitchPayload() {
        ArrayList<ImmutableInstruction> instructions = createSimpleInstructionList();
        instructions.add(1, new ImmutableInstruction31t(Opcode.SPARSE_SWITCH, 0, 12));

        ImmutableMethodImplementation methodImplementation = new ImmutableMethodImplementation(1, instructions, null, null);
        InsnWriteUtil writeUtil = new InsnWriteUtil(methodImplementation);

        for (Instruction instr: writeUtil.getInstructions()) {
            if (instr instanceof SparseSwitchPayload) {
                SparseSwitchPayload instruction = (SparseSwitchPayload) instr;
                for (SwitchElement switchElement: instruction.getSwitchElements()) {
                    Assert.assertEquals("packed switch payload offset was not modified properly", switchElement.getOffset(), 6);
                }
                break;
            }
        }
    }

    @Test
    public void testArrayPayloadAlignment() {
        ArrayList<ImmutableInstruction> instructions = createSimpleInstructionList();
        // add misaligned array payload
        instructions.add(new ImmutableInstruction10x(Opcode.NOP));
        instructions.add(new ImmutableArrayPayload(4, null));

        ImmutableMethodImplementation methodImplementation = new ImmutableMethodImplementation(1, instructions, null, null);
        InsnWriteUtil writeUtil = new InsnWriteUtil(methodImplementation);

        int codeOffset = 0;
        for (Instruction instr: writeUtil.getInstructions()) {
            if (codeOffset == 21) {
                Assert.assertEquals("array payload was not aligned properly", instr.getOpcode(), Opcode.NOP);
                break;
            }
            codeOffset += instr.getCodeUnits();
        }
    }

    @Test
    public void testPackedSwitchAlignment() {
        ArrayList<ImmutableInstruction> instructions = createSimpleInstructionList();
        // packed switch instruction is already misaligned

        ImmutableMethodImplementation methodImplementation = new ImmutableMethodImplementation(1, instructions, null, null);
        InsnWriteUtil writeUtil = new InsnWriteUtil(methodImplementation);

        int codeOffset = 0;
        for (Instruction instr: writeUtil.getInstructions()) {
            if (codeOffset == 7) {
                Assert.assertEquals("packed switch payload was not aligned properly", instr.getOpcode(), Opcode.NOP);
                break;
            }
            codeOffset += instr.getCodeUnits();
        }
    }

    @Test
    public void testSparseSwitchAlignment() {
        ArrayList<ImmutableInstruction> instructions = createSimpleInstructionList();
        // insert a nop to mis-align sparse switch payload
        instructions.add(4, new ImmutableInstruction10x(Opcode.NOP));

        ImmutableMethodImplementation methodImplementation = new ImmutableMethodImplementation(1, instructions, null, null);
        InsnWriteUtil writeUtil = new InsnWriteUtil(methodImplementation);

        int codeOffset = 0;
        for (Instruction instr: writeUtil.getInstructions()) {
            if (codeOffset == 15) {
                Assert.assertEquals("packed switch payload was not aligned properly", instr.getOpcode(), Opcode.NOP);
                break;
            }
            codeOffset += instr.getCodeUnits();
        }
    }

    @Test
    public void testGotoToGoto16() {
        ArrayList<ImmutableInstruction> instructions = Lists.newArrayList();
        instructions.add(new ImmutableInstruction10t(Opcode.GOTO, 127));
        instructions.add(new ImmutableInstruction21c(Opcode.CONST_STRING, 0, new ImmutableStringReference(mJumboStrings.get(0))));
        for (int i=0;i<127;i++) {
            instructions.add(new ImmutableInstruction10x(Opcode.NOP));
        }

        ImmutableMethodImplementation methodImplementation = new ImmutableMethodImplementation(1, instructions, null, null);
        InsnWriteUtil writeUtil = new InsnWriteUtil(methodImplementation);

        Instruction instr = writeUtil.getInstructions().iterator().next();
        Assert.assertEquals("goto was not converted to goto/16 properly", instr.getOpcode(), Opcode.GOTO_16);
    }

    @Test
    public void testGoto16ToGoto32() {
        ArrayList<ImmutableInstruction> instructions = Lists.newArrayList();
        instructions.add(new ImmutableInstruction20t(Opcode.GOTO_16, Short.MAX_VALUE));
        instructions.add(new ImmutableInstruction21c(Opcode.CONST_STRING, 0, new ImmutableStringReference(mJumboStrings.get(0))));
        for (int i=0;i<Short.MAX_VALUE;i++) {
            instructions.add(new ImmutableInstruction10x(Opcode.NOP));
        }

        ImmutableMethodImplementation methodImplementation = new ImmutableMethodImplementation(1, instructions, null, null);
        InsnWriteUtil writeUtil = new InsnWriteUtil(methodImplementation);

        Instruction instr = writeUtil.getInstructions().iterator().next();
        Assert.assertEquals("goto/16 was not converted to goto/32 properly", instr.getOpcode(), Opcode.GOTO_32);
    }

    @Test
    public void testGotoIterative() {
        ArrayList<ImmutableInstruction> instructions = Lists.newArrayList();

        instructions.add(new ImmutableInstruction10t(Opcode.GOTO, 126));
        instructions.add(new ImmutableInstruction10t(Opcode.GOTO, 127));
        instructions.add(new ImmutableInstruction21c(Opcode.CONST_STRING, 0, new ImmutableStringReference(mJumboStrings.get(0))));
        for (int i=0;i<122;i++) {
            instructions.add(new ImmutableInstruction10x(Opcode.NOP));
        }
        instructions.add(new ImmutableInstruction21c(Opcode.CONST_STRING, 0, new ImmutableStringReference(mJumboStrings.get(1))));
        instructions.add(new ImmutableInstruction10x(Opcode.NOP));

        // this misaligned array payload will cause nop insertion on the first pass and its removal on the second pass
        instructions.add(new ImmutableInstruction10x(Opcode.NOP));
        instructions.add(new ImmutableArrayPayload(4, null));

        ImmutableMethodImplementation methodImplementation = new ImmutableMethodImplementation(1, instructions, null, null);
        InsnWriteUtil writeUtil = new InsnWriteUtil(methodImplementation);

        Instruction instr = writeUtil.getInstructions().iterator().next();
        Assert.assertEquals("goto was not converted to goto/16 properly", instr.getOpcode(), Opcode.GOTO_16);

        int codeOffset = 0;
        for (Instruction instruction: writeUtil.getInstructions()) {
            if (instruction instanceof ArrayPayload) {
                Assert.assertEquals("packed switch payload was not aligned properly", codeOffset%2, 0);
            }
            codeOffset += instruction.getCodeUnits();
        }
    }
}
