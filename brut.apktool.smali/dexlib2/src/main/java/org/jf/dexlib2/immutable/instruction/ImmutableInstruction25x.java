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

package org.jf.dexlib2.immutable.instruction;

import org.jf.dexlib2.Format;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.iface.instruction.formats.Instruction25x;
import org.jf.dexlib2.util.Preconditions;

import javax.annotation.Nonnull;

public class ImmutableInstruction25x extends ImmutableInstruction implements Instruction25x {
    public static final Format FORMAT = Format.Format25x;

    protected final int parameterRegisterCount;
    protected final int registerClosure;
    protected final int registerD;
    protected final int registerE;
    protected final int registerF;
    protected final int registerG;

    public ImmutableInstruction25x(@Nonnull Opcode opcode,
            int parameterRegisterCount,
            int registerClosure,
            int registerD,
            int registerE,
            int registerF,
            int registerG) {
        super(opcode);
        this.parameterRegisterCount =
                Preconditions.check25xParameterRegisterCount(parameterRegisterCount);
        this.registerClosure = Preconditions.checkNibbleRegister(registerClosure);
        this.registerD = (parameterRegisterCount>0) ?
                Preconditions.checkNibbleRegister(registerD) : 0;
        this.registerE = (parameterRegisterCount>1) ?
                Preconditions.checkNibbleRegister(registerE) : 0;
        this.registerF = (parameterRegisterCount>2) ?
                Preconditions.checkNibbleRegister(registerF) : 0;
        this.registerG = (parameterRegisterCount>3) ?
                Preconditions.checkNibbleRegister(registerG) : 0;
    }

    public static ImmutableInstruction25x of(Instruction25x instruction) {
        if (instruction instanceof ImmutableInstruction25x) {
            return (ImmutableInstruction25x)instruction;
        }
        return new ImmutableInstruction25x(
                instruction.getOpcode(),
                instruction.getRegisterCount(),
                instruction.getRegisterFixedC(),
                instruction.getRegisterParameterD(),
                instruction.getRegisterParameterE(),
                instruction.getRegisterParameterF(),
                instruction.getRegisterParameterG());
    }


    @Override public int getParameterRegisterCount() { return parameterRegisterCount; }
    @Override public int getRegisterCount() { return parameterRegisterCount + 1; }

    @Override public int getRegisterFixedC() { return registerClosure; }
    @Override public int getRegisterParameterD() { return registerD; }
    @Override public int getRegisterParameterE() { return registerE; }
    @Override public int getRegisterParameterF() { return registerF; }
    @Override public int getRegisterParameterG() { return registerG; }

    @Override public Format getFormat() { return FORMAT; }
}
