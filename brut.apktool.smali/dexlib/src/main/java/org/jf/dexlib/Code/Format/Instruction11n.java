/*
 * [The "BSD licence"]
 * Copyright (c) 2010 Ben Gruver (JesusFreke)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.jf.dexlib.Code.Format;

import org.jf.dexlib.Code.Instruction;
import org.jf.dexlib.Code.LiteralInstruction;
import org.jf.dexlib.Code.Opcode;
import org.jf.dexlib.Code.SingleRegisterInstruction;
import org.jf.dexlib.DexFile;
import org.jf.dexlib.Util.AnnotatedOutput;
import org.jf.dexlib.Util.NumberUtils;

public class Instruction11n extends Instruction implements SingleRegisterInstruction, LiteralInstruction {
    public static final InstructionFactory Factory = new Factory();
    private byte regA;
    private byte litB;

    public Instruction11n(Opcode opcode, byte regA, byte litB) {
        super(opcode);

        if (regA >= 1 << 4) {
            throw new RuntimeException("The register number must be less than v16");
        }

        if (litB < -(1 << 3) ||
                litB >= 1 << 3) {
            throw new RuntimeException("The literal value must be between -8 and 7 inclusive");
        }

        this.regA = regA;
        this.litB = litB;
    }

    private Instruction11n(Opcode opcode, byte[] buffer, int bufferIndex) {
        super(opcode);

        this.regA = NumberUtils.decodeLowUnsignedNibble(buffer[bufferIndex + 1]);
        this.litB = NumberUtils.decodeHighSignedNibble(buffer[bufferIndex + 1]);
    }

    public void writeInstruction(AnnotatedOutput out, int currentCodeAddress) {
        out.writeByte(opcode.value);
        out.writeByte((litB << 4) | regA);
    }

    public Format getFormat() {
        return Format.Format11n;
    }

    public int getRegisterA() {
        return regA;
    }

    public long getLiteral() {
        return litB;
    }

    private static class Factory implements Instruction.InstructionFactory {
        public Instruction makeInstruction(DexFile dexFile, Opcode opcode, byte[] buffer, int bufferIndex) {
            return new Instruction11n(opcode, buffer, bufferIndex);
        }
    }
}