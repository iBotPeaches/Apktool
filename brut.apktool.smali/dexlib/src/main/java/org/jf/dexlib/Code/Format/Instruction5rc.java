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

import org.jf.dexlib.Code.Instruction;
import org.jf.dexlib.Code.InstructionWithReference;
import org.jf.dexlib.Code.Opcode;
import org.jf.dexlib.Code.RegisterRangeInstruction;
import org.jf.dexlib.DexFile;
import org.jf.dexlib.Item;
import org.jf.dexlib.MethodIdItem;
import org.jf.dexlib.TypeIdItem;
import org.jf.dexlib.Util.AnnotatedOutput;
import org.jf.dexlib.Util.NumberUtils;

import static org.jf.dexlib.Code.Opcode.*;

public class Instruction5rc extends InstructionWithJumboReference implements RegisterRangeInstruction {
    public static final InstructionFactory Factory = new Factory();
    private short regCount;
    private short startReg;

    public Instruction5rc(Opcode opcode, int regCount, int startReg, Item referencedItem) {
        super(opcode, referencedItem);

        if (regCount >= 1 << 16) {
            throw new RuntimeException("regCount must be less than 65536");
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

        this.regCount = (short)regCount;
        this.startReg = (short)startReg;

        checkItem(opcode, referencedItem, regCount);
    }

    private Instruction5rc(DexFile dexFile, Opcode opcode, byte[] buffer, int bufferIndex) {
        super(dexFile, opcode, buffer, bufferIndex);

        this.regCount = (short)NumberUtils.decodeUnsignedShort(buffer, bufferIndex + 6);
        this.startReg = (short)NumberUtils.decodeUnsignedShort(buffer, bufferIndex + 8);

        checkItem(opcode, getReferencedItem(), getRegCount());
    }

    protected void writeInstruction(AnnotatedOutput out, int currentCodeAddress) {
        out.writeByte(0xff);
        out.writeByte(opcode.value);
        out.writeInt(this.getReferencedItem().getIndex());
        out.writeShort(regCount);
        out.writeShort(startReg);
    }

    public Format getFormat() {
        return Format.Format5rc;
    }

    public int getRegCount() {
        return regCount & 0xFFFF;
    }

    public int getStartRegister() {
        return startReg & 0xFFFF;
    }

    private static void checkItem(Opcode opcode, Item item, int regCount) {
        if (opcode == FILLED_NEW_ARRAY_JUMBO) {
            //check data for filled-new-array/jumbo opcode
            String type = ((TypeIdItem) item).getTypeDescriptor();
            if (type.charAt(0) != '[') {
                throw new RuntimeException("The type must be an array type");
            }
            if (type.charAt(1) == 'J' || type.charAt(1) == 'D') {
                throw new RuntimeException("The type cannot be an array of longs or doubles");
            }
        } else if (opcode.value >= INVOKE_VIRTUAL_JUMBO.value && opcode.value <= INVOKE_INTERFACE_JUMBO.value ||
                opcode == INVOKE_OBJECT_INIT_JUMBO) {
            //check data for invoke-*/range opcodes
            MethodIdItem methodIdItem = (MethodIdItem) item;
            int parameterRegisterCount = methodIdItem.getPrototype().getParameterRegisterCount();
            if (opcode != INVOKE_STATIC_JUMBO) {
                parameterRegisterCount++;
            }
            if (parameterRegisterCount != regCount) {
                throw new RuntimeException("regCount does not match the number of arguments of the method");
            }
        }
    }

    private static class Factory implements InstructionFactory {
        public Instruction makeInstruction(DexFile dexFile, Opcode opcode, byte[] buffer, int bufferIndex) {
            return new Instruction5rc(dexFile, opcode, buffer, bufferIndex);
        }
    }
}
