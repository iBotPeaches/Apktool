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
import org.jf.dexlib.Code.InstructionWithReference;
import org.jf.dexlib.Code.Opcode;
import org.jf.dexlib.Code.TwoRegisterInstruction;
import org.jf.dexlib.DexFile;
import org.jf.dexlib.Item;
import org.jf.dexlib.Util.AnnotatedOutput;
import org.jf.dexlib.Util.NumberUtils;

public class Instruction22c extends InstructionWithReference implements TwoRegisterInstruction,
        InstructionWithJumboVariant {
    public static final Instruction.InstructionFactory Factory = new Factory();
    private byte regA;
    private byte regB;

    public Instruction22c(Opcode opcode, byte regA, byte regB, Item referencedItem) {
        super(opcode, referencedItem);

        if (regA >= 1 << 4 ||
                regB >= 1 << 4) {
            throw new RuntimeException("The register number must be less than v16");
        }

        this.regA = regA;
        this.regB = regB;
    }

    private Instruction22c(DexFile dexFile, Opcode opcode, byte[] buffer, int bufferIndex) {
        super(dexFile, opcode, buffer, bufferIndex);

        this.regA = NumberUtils.decodeLowUnsignedNibble(buffer[bufferIndex + 1]);
        this.regB = NumberUtils.decodeHighUnsignedNibble(buffer[bufferIndex + 1]);
    }

    protected void writeInstruction(AnnotatedOutput out, int currentCodeAddress) {
        if(getReferencedItem().getIndex() > 0xFFFF) {
            if (opcode.hasJumboOpcode()) {
                throw new RuntimeException(String.format("%s index is too large. Use the %s instruction instead.",
                        opcode.referenceType.name(), opcode.getJumboOpcode().name));
            } else {
                throw new RuntimeException(String.format("%s index is too large.", opcode.referenceType.name()));
            }
        }

        out.writeByte(opcode.value);
        out.writeByte((regB << 4) | regA);
        out.writeShort(getReferencedItem().getIndex());
    }

    public Format getFormat() {
        return Format.Format22c;
    }

    public int getRegisterA() {
        return regA;
    }

    public int getRegisterB() {
        return regB;
    }

    public Instruction makeJumbo() {
        Opcode jumboOpcode = opcode.getJumboOpcode();
        if (jumboOpcode == null) {
            return null;
        }

        return new Instruction52c(jumboOpcode, getRegisterA(), getRegisterB(), getReferencedItem());
    }

    private static class Factory implements Instruction.InstructionFactory {
        public Instruction makeInstruction(DexFile dexFile, Opcode opcode, byte[] buffer, int bufferIndex) {
            return new Instruction22c(dexFile, opcode, buffer, bufferIndex);
        }
    }
}
