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

package org.jf.dexlib2.builder;

import com.google.common.collect.Lists;
import junit.framework.Assert;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.builder.instruction.BuilderInstruction10t;
import org.jf.dexlib2.builder.instruction.BuilderInstruction10x;
import org.jf.dexlib2.iface.ExceptionHandler;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.TryBlock;
import org.jf.dexlib2.iface.debug.DebugItem;
import org.jf.dexlib2.iface.debug.LineNumber;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.OffsetInstruction;
import org.junit.Test;

import java.util.List;

public class FixOffsetsTest {
    @Test
    public void testFixOffsets() {
        MethodImplementationBuilder builder = new MethodImplementationBuilder(1);

        Label firstGotoTarget = builder.getLabel("firstGotoTarget");
        builder.addInstruction(new BuilderInstruction10t(Opcode.GOTO, firstGotoTarget));

        builder.addLineNumber(1);

        for (int i=0; i<250; i++) {
            builder.addInstruction(new BuilderInstruction10x(Opcode.NOP));
        }

        builder.addLabel("tryStart");

        builder.addLineNumber(2);

        for (int i=0; i<250; i++) {
            builder.addInstruction(new BuilderInstruction10x(Opcode.NOP));
        }

        builder.addLineNumber(3);

        Label secondGotoTarget = builder.getLabel("secondGotoTarget");
        builder.addInstruction(new BuilderInstruction10t(Opcode.GOTO, secondGotoTarget));


        builder.addLineNumber(4);
        builder.addLabel("handler");

        for (int i=0; i<500; i++) {
            builder.addInstruction(new BuilderInstruction10x(Opcode.NOP));
        }

        builder.addLineNumber(5);

        builder.addLabel("tryEnd");

        builder.addLabel("firstGotoTarget");
        builder.addLabel("secondGotoTarget");
        builder.addInstruction(new BuilderInstruction10x(Opcode.RETURN_VOID));

        Label tryStart = builder.getLabel("tryStart");
        Label tryEnd = builder.getLabel("tryEnd");
        Label handler = builder.getLabel("handler");

        builder.addCatch(tryStart, tryEnd, handler);

        MethodImplementation impl = builder.getMethodImplementation();

        List<? extends Instruction> instructions = Lists.newArrayList(impl.getInstructions());
        Assert.assertEquals(1003, instructions.size());

        Assert.assertEquals(Opcode.GOTO_16, instructions.get(0).getOpcode());
        Assert.assertEquals(1004, ((OffsetInstruction)instructions.get(0)).getCodeOffset());

        Assert.assertEquals(Opcode.GOTO_16, instructions.get(501).getOpcode());
        Assert.assertEquals(502, ((OffsetInstruction)instructions.get(501)).getCodeOffset());

        List<? extends TryBlock<? extends ExceptionHandler>> exceptionHandlers = impl.getTryBlocks();

        Assert.assertEquals(1, exceptionHandlers.size());
        Assert.assertEquals(252, exceptionHandlers.get(0).getStartCodeAddress());
        Assert.assertEquals(752, exceptionHandlers.get(0).getCodeUnitCount());

        Assert.assertEquals(1, exceptionHandlers.get(0).getExceptionHandlers().size());

        ExceptionHandler exceptionHandler = exceptionHandlers.get(0).getExceptionHandlers().get(0);
        Assert.assertEquals(504, exceptionHandler.getHandlerCodeAddress());

        List<DebugItem> debugItems = Lists.newArrayList(impl.getDebugItems());

        Assert.assertEquals(5, debugItems.size());

        Assert.assertEquals(1, ((LineNumber)debugItems.get(0)).getLineNumber());
        Assert.assertEquals(2, debugItems.get(0).getCodeAddress());

        Assert.assertEquals(2, ((LineNumber)debugItems.get(1)).getLineNumber());
        Assert.assertEquals(252, debugItems.get(1).getCodeAddress());

        Assert.assertEquals(3, ((LineNumber)debugItems.get(2)).getLineNumber());
        Assert.assertEquals(502, debugItems.get(2).getCodeAddress());

        Assert.assertEquals(4, ((LineNumber)debugItems.get(3)).getLineNumber());
        Assert.assertEquals(504, debugItems.get(3).getCodeAddress());

        Assert.assertEquals(5, ((LineNumber)debugItems.get(4)).getLineNumber());
        Assert.assertEquals(1004, debugItems.get(4).getCodeAddress());
    }
}
