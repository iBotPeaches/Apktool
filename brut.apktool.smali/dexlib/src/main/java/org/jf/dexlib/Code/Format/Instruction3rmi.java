/*
 * Copyright 2011, Google Inc.
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

package org.jf.dexlib.Code.Format;

import org.jf.dexlib.Code.*;
import org.jf.dexlib.DexFile;
import org.jf.dexlib.Util.AnnotatedOutput;
import org.jf.dexlib.Util.NumberUtils;

public class Instruction3rmi extends Instruction implements RegisterRangeInstruction, OdexedInvokeInline {
 public static final InstructionFactory Factory = new Factory();
    private byte regCount;
    private short startReg;
    private short inlineIndex;

    public Instruction3rmi(Opcode opcode, short regCount, int startReg, int inlineIndex) {
        super(opcode);

        if (regCount >= 1 << 8) {
            throw new RuntimeException("regCount must be less than 256");
        }
        if (regCount < 0) {
            throw new RuntimeException("regCount cannot be negative");
        }

        if (startReg >= 1 << 16) {
            throw new RuntimeException("The beginning register of the range must be less than 65536");
        }
        if (startReg < 0) {
            throw new RuntimeException("The beginning register of the range cannot be negative");
        }

        if (inlineIndex >= 1 << 16) {
            throw new RuntimeException("The method index must be less than 65536");
        }

        this.regCount = (byte)regCount;
        this.startReg = (short)startReg;
        this.inlineIndex = (short)inlineIndex;
    }

    private Instruction3rmi(Opcode opcode, byte[] buffer, int bufferIndex) {
        super(opcode);

        this.regCount = (byte)NumberUtils.decodeUnsignedByte(buffer[bufferIndex + 1]);
        this.inlineIndex = (short)NumberUtils.decodeUnsignedShort(buffer, bufferIndex + 2);
        this.startReg = (short)NumberUtils.decodeUnsignedShort(buffer, bufferIndex + 4);
    }

    protected void writeInstruction(AnnotatedOutput out, int currentCodeAddress) {
        out.writeByte(opcode.value);
        out.writeByte(regCount);
        out.writeShort(inlineIndex);
        out.writeShort(startReg);
    }

    public Format getFormat() {
        return Format.Format3rmi;
    }

    public int getRegCount() {
        return (short)(regCount & 0xFF);
    }

    public int getStartRegister() {
        return startReg & 0xFFFF;
    }

    public int getInlineIndex() {
        return inlineIndex & 0xFFFF;
    }

    private static class Factory implements InstructionFactory {
        public Instruction makeInstruction(DexFile dexFile, Opcode opcode, byte[] buffer, int bufferIndex) {
            return new Instruction3rmi(opcode, buffer, bufferIndex);
        }
    }
}
