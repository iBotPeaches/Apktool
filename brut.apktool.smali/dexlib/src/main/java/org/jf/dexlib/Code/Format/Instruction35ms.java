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
 * INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.jf.dexlib.Code.Format;

import org.jf.dexlib.Code.FiveRegisterInstruction;
import org.jf.dexlib.Code.Instruction;
import org.jf.dexlib.Code.OdexedInvokeVirtual;
import org.jf.dexlib.Code.Opcode;
import org.jf.dexlib.DexFile;
import org.jf.dexlib.Util.AnnotatedOutput;
import org.jf.dexlib.Util.NumberUtils;


public class Instruction35ms extends Instruction implements FiveRegisterInstruction, OdexedInvokeVirtual {
    public static final Instruction.InstructionFactory Factory = new Factory();
    private byte regCount;
    private byte regA;
    private byte regD;
    private byte regE;
    private byte regF;
    private byte regG;
    private short vtableIndex;

    public Instruction35ms(Opcode opcode, int regCount, byte regD, byte regE, byte regF, byte regG,
                          byte regA, int vtableIndex) {
        super(opcode);
        if (regCount > 5) {
            throw new RuntimeException("regCount cannot be greater than 5");
        }

        if (regD >= 1 << 4 ||
                regE >= 1 << 4 ||
                regF >= 1 << 4 ||
                regG >= 1 << 4 ||
                regA >= 1 << 4) {
            throw new RuntimeException("All register args must fit in 4 bits");
        }

        if (vtableIndex >= 1 << 16) {
            throw new RuntimeException("The method index must be less than 65536");
        }

        this.regCount = (byte)regCount;
        this.regA = regA;
        this.regD = regD;
        this.regE = regE;
        this.regF = regF;
        this.regG = regG;
        this.vtableIndex = (short)vtableIndex;
    }

    private Instruction35ms(Opcode opcode, byte[] buffer, int bufferIndex) {
        super(opcode);

        this.regCount = NumberUtils.decodeHighUnsignedNibble(buffer[bufferIndex + 1]);
        this.regA = NumberUtils.decodeLowUnsignedNibble(buffer[bufferIndex + 1]);
        this.regD = NumberUtils.decodeLowUnsignedNibble(buffer[bufferIndex + 4]);
        this.regE = NumberUtils.decodeHighUnsignedNibble(buffer[bufferIndex + 4]);
        this.regF = NumberUtils.decodeLowUnsignedNibble(buffer[bufferIndex + 5]);
        this.regG = NumberUtils.decodeHighUnsignedNibble(buffer[bufferIndex + 5]);
        this.vtableIndex = (short)NumberUtils.decodeUnsignedShort(buffer, bufferIndex + 2);
    }

    protected void writeInstruction(AnnotatedOutput out, int currentCodeAddress) {
        out.writeByte(opcode.value);
        out.writeByte((regCount << 4) | regA);
        out.writeShort(vtableIndex);
        out.writeByte((regE << 4) | regD);
        out.writeByte((regG << 4) | regF);
    }

    public Format getFormat() {
        return Format.Format35ms;
    }

    public int getRegCount() {
        return regCount;
    }

    public byte getRegisterA() {
        return regA;
    }

    public byte getRegisterD() {
        return regD;
    }

    public byte getRegisterE() {
        return regE;
    }

    public byte getRegisterF() {
        return regF;
    }

    public byte getRegisterG() {
        return regG;
    }

    public int getVtableIndex() {
        return vtableIndex & 0xFFFF;
    }

    private static class Factory implements Instruction.InstructionFactory {
        public Instruction makeInstruction(DexFile dexFile, Opcode opcode, byte[] buffer, int bufferIndex) {
            return new Instruction35ms(opcode, buffer, bufferIndex);
        }
    }
}

