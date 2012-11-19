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
import org.jf.dexlib.Code.SingleRegisterInstruction;
import org.jf.dexlib.DexFile;
import org.jf.dexlib.Item;
import org.jf.dexlib.TypeIdItem;
import org.jf.dexlib.Util.AnnotatedOutput;

public class Instruction21c extends InstructionWithReference implements SingleRegisterInstruction,
        InstructionWithJumboVariant {
    public static final Instruction.InstructionFactory Factory = new Factory();
    private byte regA;

    public Instruction21c(Opcode opcode, short regA, Item referencedItem) {
        super(opcode, referencedItem);

        if (regA >= 1 << 8) {
            throw new RuntimeException("The register number must be less than v256");
        }

        if (opcode == Opcode.NEW_INSTANCE) {
            assert referencedItem instanceof TypeIdItem;
            if (((TypeIdItem)referencedItem).getTypeDescriptor().charAt(0) != 'L') {
                throw new RuntimeException("Only class references can be used with the new-instance opcode");
            }
        }

        this.regA = (byte)regA;
    }

    private Instruction21c(DexFile dexFile, Opcode opcode, byte[] buffer, int bufferIndex) {
        super(dexFile, opcode, buffer, bufferIndex);

        if (opcode == Opcode.NEW_INSTANCE &&
                ((TypeIdItem)this.getReferencedItem()).getTypeDescriptor().charAt(0) != 'L') {
            
            throw new RuntimeException("Only class references can be used with the new-instance opcode");
        }

        this.regA = buffer[bufferIndex + 1];
    }

    protected void writeInstruction(AnnotatedOutput out, int currentCodeAddress) {
        if(getReferencedItem().getIndex() > 0xFFFF) {
            if (opcode.hasJumboOpcode()) {
                throw new RuntimeException(String.format("%s index is too large. Use the %s instruction instead.",
                        opcode.referenceType.name(), opcode.getJumboOpcode().name));
            } else {
                throw new RuntimeException(String.format("%s index is too large", opcode.referenceType.name()));
            }
        }

        out.writeByte(opcode.value);
        out.writeByte(regA);
        out.writeShort(getReferencedItem().getIndex());
    }

    public Format getFormat() {
        return Format.Format21c;
    }

    public int getRegisterA() {
        return regA & 0xFF;
    }

    public Instruction makeJumbo() {
        Opcode jumboOpcode = opcode.getJumboOpcode();
        if (jumboOpcode == null) {
            return null;
        }

        if (jumboOpcode.format == Format.Format31c) {
            return new Instruction31c(jumboOpcode, (short)getRegisterA(), getReferencedItem());
        }

        return new Instruction41c(jumboOpcode, getRegisterA(), getReferencedItem());
    }

    private static class Factory implements Instruction.InstructionFactory {
        public Instruction makeInstruction(DexFile dexFile, Opcode opcode, byte[] buffer, int bufferIndex) {
            return new Instruction21c(dexFile, opcode, buffer, bufferIndex);
        }
    }
}
