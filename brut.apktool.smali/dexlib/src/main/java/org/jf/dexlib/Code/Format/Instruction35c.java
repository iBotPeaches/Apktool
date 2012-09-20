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

import org.jf.dexlib.Code.FiveRegisterInstruction;
import org.jf.dexlib.Code.Instruction;
import org.jf.dexlib.Code.InstructionWithReference;
import org.jf.dexlib.Code.Opcode;
import org.jf.dexlib.DexFile;
import org.jf.dexlib.Item;
import org.jf.dexlib.MethodIdItem;
import org.jf.dexlib.TypeIdItem;
import org.jf.dexlib.Util.AnnotatedOutput;
import org.jf.dexlib.Util.NumberUtils;

import static org.jf.dexlib.Code.Opcode.*;

public class Instruction35c extends InstructionWithReference implements FiveRegisterInstruction {
    public static final Instruction.InstructionFactory Factory = new Factory();
    private byte regCount;
    private byte regA;
    private byte regD;
    private byte regE;
    private byte regF;
    private byte regG;

    public Instruction35c(Opcode opcode, int regCount, byte regD, byte regE, byte regF, byte regG,
                          byte regA, Item referencedItem) {
        super(opcode, referencedItem);

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

        checkItem(opcode, referencedItem, regCount);

        this.regCount = (byte)regCount;
        this.regA = regA;
        this.regD = regD;
        this.regE = regE;
        this.regF = regF;
        this.regG = regG;
    }

    protected Instruction35c(DexFile dexFile, Opcode opcode, byte[] buffer, int bufferIndex) {
        super(dexFile, opcode, buffer, bufferIndex);

        this.regCount = NumberUtils.decodeHighUnsignedNibble(buffer[bufferIndex + 1]);
        this.regA = NumberUtils.decodeLowUnsignedNibble(buffer[bufferIndex + 1]);
        this.regD = NumberUtils.decodeLowUnsignedNibble(buffer[bufferIndex + 4]);
        this.regE = NumberUtils.decodeHighUnsignedNibble(buffer[bufferIndex + 4]);
        this.regF = NumberUtils.decodeLowUnsignedNibble(buffer[bufferIndex + 5]);
        this.regG = NumberUtils.decodeHighUnsignedNibble(buffer[bufferIndex + 5]);

        if (getRegCount() > 5) {
            throw new RuntimeException("regCount cannot be greater than 5");
        }

        checkItem(opcode, getReferencedItem(), getRegCount());
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
        out.writeByte((regCount << 4) | regA);
        out.writeShort(getReferencedItem().getIndex());
        out.writeByte((regE << 4) | regD);
        out.writeByte((regG << 4) | regF);
    }

    public Format getFormat() {
        return Format.Format35c;
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

    private static void checkItem(Opcode opcode, Item item, int regCount) {
        if (opcode == FILLED_NEW_ARRAY) {
            //check data for filled-new-array opcode
            String type = ((TypeIdItem) item).getTypeDescriptor();
            if (type.charAt(0) != '[') {
                throw new RuntimeException("The type must be an array type");
            }
            if (type.charAt(1) == 'J' || type.charAt(1) == 'D') {
                throw new RuntimeException("The type cannot be an array of longs or doubles");
            }
        } else if (opcode.value >= INVOKE_VIRTUAL.value && opcode.value <= INVOKE_INTERFACE.value ||
                opcode == INVOKE_DIRECT_EMPTY) {
            //check data for invoke-* opcodes
            MethodIdItem methodIdItem = (MethodIdItem) item;
            int parameterRegisterCount = methodIdItem.getPrototype().getParameterRegisterCount();
            if (opcode != INVOKE_STATIC) {
                parameterRegisterCount++;
            }
            if (parameterRegisterCount != regCount) {
                throw new RuntimeException("regCount does not match the number of arguments of the method");
            }
        }
    }

    private static class Factory implements Instruction.InstructionFactory {
        public Instruction makeInstruction(DexFile dexFile, Opcode opcode, byte[] buffer, int bufferIndex) {
            return new Instruction35c(dexFile, opcode, buffer, bufferIndex);
        }
    }
}