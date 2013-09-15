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

package org.jf.dexlib2.builder;

import com.google.common.collect.Lists;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.builder.instruction.*;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.OffsetInstruction;
import org.jf.dexlib2.iface.instruction.formats.Instruction31t;
import org.jf.dexlib2.iface.instruction.formats.PackedSwitchPayload;
import org.jf.dexlib2.iface.instruction.formats.SparseSwitchPayload;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class PayloadAlignmentTest {

    @Test
    public void testPayloadAlignmentRemoveNop() {
        MethodImplementationBuilder implBuilder = new MethodImplementationBuilder(10);

        implBuilder.addInstruction(new BuilderInstruction10x(Opcode.NOP));
        implBuilder.addInstruction(new BuilderArrayPayload(4, null));

        List<? extends Instruction> instructions =
                Lists.newArrayList(implBuilder.getMethodImplementation().getInstructions());

        Assert.assertEquals(instructions.size(), 1);

        Instruction instruction = instructions.get(0);

        Assert.assertEquals(instruction.getOpcode(), Opcode.ARRAY_PAYLOAD);
    }

    @Test
    public void testPayloadAlignmentAddNop() {
        MethodImplementationBuilder implBuilder = new MethodImplementationBuilder(10);

        implBuilder.addInstruction(new BuilderInstruction12x(Opcode.MOVE, 0, 0));
        implBuilder.addInstruction(new BuilderArrayPayload(4, null));

        List<? extends Instruction> instructions =
                Lists.newArrayList(implBuilder.getMethodImplementation().getInstructions());

        Assert.assertEquals(instructions.size(), 3);

        Instruction instruction = instructions.get(0);
        Assert.assertEquals(instruction.getOpcode(), Opcode.MOVE);

        instruction = instructions.get(1);
        Assert.assertEquals(instruction.getOpcode(), Opcode.NOP);

        instruction = instructions.get(2);
        Assert.assertEquals(instruction.getOpcode(), Opcode.ARRAY_PAYLOAD);
    }

    @Test
    public void testPayloadAlignmentRemoveNopWithReferent() {
        MethodImplementationBuilder implBuilder = new MethodImplementationBuilder(10);

        Label label = implBuilder.getLabel("array_payload");
        implBuilder.addInstruction(new BuilderInstruction31t(Opcode.FILL_ARRAY_DATA, 0, label));
        implBuilder.addInstruction(new BuilderInstruction12x(Opcode.MOVE, 0, 0));
        implBuilder.addInstruction(new BuilderInstruction12x(Opcode.MOVE, 0, 0));
        implBuilder.addInstruction(new BuilderInstruction12x(Opcode.MOVE, 0, 0));
        implBuilder.addInstruction(new BuilderInstruction10x(Opcode.NOP));
        implBuilder.addLabel("array_payload");
        implBuilder.addInstruction(new BuilderArrayPayload(4, null));

        List<Instruction> instructions = Lists.newArrayList(implBuilder.getMethodImplementation().getInstructions());

        checkInstructions(instructions,
                new Opcode[]{Opcode.FILL_ARRAY_DATA,
                        Opcode.MOVE,
                        Opcode.MOVE,
                        Opcode.MOVE,
                        Opcode.ARRAY_PAYLOAD});

        Instruction31t referent = (Instruction31t)instructions.get(0);
        Assert.assertEquals(6, referent.getCodeOffset());
    }

    @Test
    public void testPayloadAlignmentAddNopWithReferent() {
        MethodImplementationBuilder implBuilder = new MethodImplementationBuilder(10);

        Label label = implBuilder.getLabel("array_payload");
        implBuilder.addInstruction(new BuilderInstruction31t(Opcode.FILL_ARRAY_DATA, 0, label));
        implBuilder.addInstruction(new BuilderInstruction12x(Opcode.MOVE, 0, 0));
        implBuilder.addInstruction(new BuilderInstruction12x(Opcode.MOVE, 0, 0));
        implBuilder.addInstruction(new BuilderInstruction12x(Opcode.MOVE, 0, 0));
        implBuilder.addInstruction(new BuilderInstruction12x(Opcode.MOVE, 0, 0));
        implBuilder.addLabel("array_payload");
        implBuilder.addInstruction(new BuilderArrayPayload(4, null));

        List<Instruction> instructions = Lists.newArrayList(implBuilder.getMethodImplementation().getInstructions());

        checkInstructions(instructions,
                new Opcode[]{Opcode.FILL_ARRAY_DATA,
                    Opcode.MOVE,
                    Opcode.MOVE,
                    Opcode.MOVE,
                    Opcode.MOVE,
                    Opcode.NOP,
                    Opcode.ARRAY_PAYLOAD});

        Instruction31t referent = (Instruction31t)instructions.get(0);
        Assert.assertEquals(8, referent.getCodeOffset());
    }

    private static void checkInstructions(List<Instruction> instructions, Opcode[] expectedOpcodes) {
        Assert.assertEquals(expectedOpcodes.length, instructions.size());

        for (int i=0; i<expectedOpcodes.length; i++) {
            Assert.assertEquals(instructions.get(i).getOpcode(), expectedOpcodes[i]);
        }
    }

    @Test
    public void testPackedSwitchAlignment() {
        MethodImplementationBuilder implBuilder = new MethodImplementationBuilder(10);

        implBuilder.addLabel("switch_target_1");
        implBuilder.addInstruction(new BuilderInstruction10t(Opcode.GOTO, implBuilder.getLabel("goto_target")));

        implBuilder.addLabel("switch_payload");
        implBuilder.addInstruction(new BuilderPackedSwitchPayload(0, Lists.newArrayList(
                implBuilder.getLabel("switch_target_1"),
                implBuilder.getLabel("switch_target_2"),
                implBuilder.getLabel("switch_target_3"))));

        implBuilder.addLabel("goto_target");
        implBuilder.addInstruction(new BuilderInstruction10x(Opcode.NOP));
        implBuilder.addInstruction(new BuilderInstruction10x(Opcode.NOP));

        implBuilder.addLabel("switch_target_2");
        implBuilder.addInstruction(new BuilderInstruction10x(Opcode.NOP));

        implBuilder.addLabel("switch_target_3");
        implBuilder.addInstruction(new BuilderInstruction10x(Opcode.NOP));

        implBuilder.addInstruction(new BuilderInstruction31t(Opcode.PACKED_SWITCH, 0,
                implBuilder.getLabel("switch_payload")));

        List<Instruction> instructions = Lists.newArrayList(implBuilder.getMethodImplementation().getInstructions());

        checkInstructions(instructions,
                new Opcode[]{Opcode.GOTO,
                        Opcode.NOP,
                        Opcode.PACKED_SWITCH_PAYLOAD,
                        Opcode.NOP,
                        Opcode.NOP,
                        Opcode.NOP,
                        Opcode.NOP,
                        Opcode.PACKED_SWITCH});

        OffsetInstruction gotoInstruction = (OffsetInstruction)instructions.get(0);
        Assert.assertEquals(12, gotoInstruction.getCodeOffset());

        PackedSwitchPayload payload = (PackedSwitchPayload)instructions.get(2);
        Assert.assertEquals(3, payload.getSwitchElements().size());
        Assert.assertEquals(-16, payload.getSwitchElements().get(0).getOffset());
        Assert.assertEquals(-2, payload.getSwitchElements().get(1).getOffset());
        Assert.assertEquals(-1, payload.getSwitchElements().get(2).getOffset());

        OffsetInstruction referent = (OffsetInstruction)instructions.get(7);
        Assert.assertEquals(-14, referent.getCodeOffset());
    }

    @Test
    public void testSparseSwitchAlignment() {
        MethodImplementationBuilder implBuilder = new MethodImplementationBuilder(10);

        implBuilder.addLabel("switch_target_1");
        implBuilder.addInstruction(new BuilderInstruction10t(Opcode.GOTO, implBuilder.getLabel("goto_target")));

        implBuilder.addLabel("switch_payload");
        implBuilder.addInstruction(new BuilderSparseSwitchPayload(Lists.newArrayList(
                new SwitchLabelElement(0, implBuilder.getLabel("switch_target_1")),
                new SwitchLabelElement(5, implBuilder.getLabel("switch_target_2")),
                new SwitchLabelElement(10, implBuilder.getLabel("switch_target_3")))));

        implBuilder.addLabel("goto_target");
        implBuilder.addInstruction(new BuilderInstruction10x(Opcode.NOP));
        implBuilder.addInstruction(new BuilderInstruction10x(Opcode.NOP));

        implBuilder.addLabel("switch_target_2");
        implBuilder.addInstruction(new BuilderInstruction10x(Opcode.NOP));

        implBuilder.addLabel("switch_target_3");
        implBuilder.addInstruction(new BuilderInstruction10x(Opcode.NOP));

        implBuilder.addInstruction(new BuilderInstruction31t(Opcode.SPARSE_SWITCH, 0,
                implBuilder.getLabel("switch_payload")));

        List<Instruction> instructions = Lists.newArrayList(implBuilder.getMethodImplementation().getInstructions());

        checkInstructions(instructions,
                new Opcode[]{Opcode.GOTO,
                        Opcode.NOP,
                        Opcode.SPARSE_SWITCH_PAYLOAD,
                        Opcode.NOP,
                        Opcode.NOP,
                        Opcode.NOP,
                        Opcode.NOP,
                        Opcode.SPARSE_SWITCH});

        OffsetInstruction gotoInstruction = (OffsetInstruction)instructions.get(0);
        Assert.assertEquals(16, gotoInstruction.getCodeOffset());

        SparseSwitchPayload payload = (SparseSwitchPayload)instructions.get(2);
        Assert.assertEquals(3, payload.getSwitchElements().size());
        Assert.assertEquals(-20, payload.getSwitchElements().get(0).getOffset());
        Assert.assertEquals(-2, payload.getSwitchElements().get(1).getOffset());
        Assert.assertEquals(-1, payload.getSwitchElements().get(2).getOffset());

        OffsetInstruction referent = (OffsetInstruction)instructions.get(7);
        Assert.assertEquals(-18, referent.getCodeOffset());
    }
}
