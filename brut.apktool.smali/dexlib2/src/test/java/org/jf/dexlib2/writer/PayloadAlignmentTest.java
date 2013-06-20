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
import org.jf.dexlib2.iface.reference.Reference;
import org.jf.dexlib2.iface.reference.StringReference;
import org.jf.dexlib2.immutable.ImmutableMethodImplementation;
import org.jf.dexlib2.immutable.instruction.*;
import org.jf.dexlib2.writer.util.InstructionWriteUtil;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.util.ArrayList;

public class PayloadAlignmentTest {
    private MockStringIndexProvider mockStringIndexProvider;

    private class InsnWriteUtil extends InstructionWriteUtil<Instruction, StringReference, Reference> {
        public InsnWriteUtil(@Nonnull MethodImplementation implementation) {
            super(implementation.getInstructions(), mockStringIndexProvider, ImmutableInstructionFactory.INSTANCE);
        }
    }

    @Before
    public void setup() {
        mockStringIndexProvider = new MockStringIndexProvider();
    }

    @Test
    public void testArrayPayloadAlignment() {
        ArrayList<ImmutableInstruction> instructions = Lists.newArrayList();

        // add misaligned array payload
        instructions.add(new ImmutableInstruction10x(Opcode.NOP));
        instructions.add(new ImmutableArrayPayload(4, null));

        ImmutableMethodImplementation methodImplementation = new ImmutableMethodImplementation(1, instructions, null, null);
        InsnWriteUtil writeUtil = new InsnWriteUtil(methodImplementation);

        int codeOffset = 0;
        for (Instruction instr: writeUtil.getInstructions()) {
            if (instr.getOpcode().equals(Opcode.ARRAY_PAYLOAD)) {
                Assert.assertEquals("array payload was not aligned properly", codeOffset%2, 0);
                break;
            }
            codeOffset += instr.getCodeUnits();
        }
    }

    @Test
    public void testPackedSwitchAlignment() {
        ArrayList<ImmutableInstruction> instructions = Lists.newArrayList();
        // add misaligned packed switch payload
        ArrayList<SwitchElement> switchElements = Lists.newArrayList();
        switchElements.add(new ImmutableSwitchElement(0, 5));
        instructions.add(new ImmutableInstruction10x(Opcode.NOP));
        instructions.add(new ImmutablePackedSwitchPayload(switchElements));

        ImmutableMethodImplementation methodImplementation = new ImmutableMethodImplementation(1, instructions, null, null);
        InsnWriteUtil writeUtil = new InsnWriteUtil(methodImplementation);

        int codeOffset = 0;
        for (Instruction instr: writeUtil.getInstructions()) {
            if (instr.getOpcode().equals(Opcode.PACKED_SWITCH_PAYLOAD)) {
                Assert.assertEquals("packed switch payload was not aligned properly", codeOffset%2, 0);
                break;
            }
            codeOffset += instr.getCodeUnits();
        }
    }

    @Test
    public void testSparseSwitchAlignment() {
        ArrayList<ImmutableInstruction> instructions = Lists.newArrayList();

        // add misaligned sparse switch payload
        ArrayList<SwitchElement> switchElements = Lists.newArrayList();
        switchElements.add(new ImmutableSwitchElement(0, 5));

        instructions.add(new ImmutableInstruction10x(Opcode.NOP));
        instructions.add(new ImmutableSparseSwitchPayload(switchElements));

        ImmutableMethodImplementation methodImplementation = new ImmutableMethodImplementation(1, instructions, null, null);
        InsnWriteUtil writeUtil = new InsnWriteUtil(methodImplementation);

        int codeOffset = 0;
        for (Instruction instr: writeUtil.getInstructions()) {
            if (instr.getOpcode().equals(Opcode.SPARSE_SWITCH_PAYLOAD)) {
                Assert.assertEquals("packed switch payload was not aligned properly", codeOffset%2, 0);
                break;
            }
            codeOffset += instr.getCodeUnits();
        }
    }
}
