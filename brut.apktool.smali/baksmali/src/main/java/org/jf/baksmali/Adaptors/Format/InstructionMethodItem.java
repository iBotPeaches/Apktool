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

package org.jf.baksmali.Adaptors.Format;

import org.jf.baksmali.Adaptors.MethodDefinition;
import org.jf.baksmali.Adaptors.MethodItem;
import org.jf.baksmali.Adaptors.ReferenceFormatter;
import org.jf.baksmali.Renderers.LongRenderer;
import org.jf.dexlib2.ReferenceType;
import org.jf.dexlib2.VerificationError;
import org.jf.dexlib2.dexbacked.DexBackedDexFile.InvalidItemIndex;
import org.jf.dexlib2.iface.instruction.*;
import org.jf.dexlib2.iface.instruction.formats.Instruction20bc;
import org.jf.dexlib2.iface.instruction.formats.UnknownInstruction;
import org.jf.dexlib2.iface.reference.Reference;
import org.jf.util.IndentingWriter;

import javax.annotation.Nonnull;
import java.io.IOException;

public class InstructionMethodItem<T extends Instruction> extends MethodItem {
    @Nonnull protected final MethodDefinition methodDef;
    @Nonnull protected final T instruction;

    public InstructionMethodItem(@Nonnull MethodDefinition methodDef, int codeAddress, @Nonnull T instruction) {
        super(codeAddress);
        this.methodDef = methodDef;
        this.instruction = instruction;
    }

    public double getSortOrder() {
        //instructions should appear after everything except an "end try" label and .catch directive
        return 100;
    }

    @Override
    public boolean writeTo(IndentingWriter writer) throws IOException {
        boolean invalidReference = false;
        if (instruction instanceof ReferenceInstruction) {
            try {
                Reference reference = ((ReferenceInstruction)instruction).getReference();
            } catch (InvalidItemIndex ex) {
                invalidReference = true;

                writer.write("#invalid ");
                writer.write(ReferenceType.toString(instruction.getOpcode().referenceType));
                writer.write(" index: ");
                writer.printSignedIntAsDec(ex.getInvalidIndex());
                writer.write("\n#");
            }
        }

        switch (instruction.getOpcode().format) {
            case Format10t:
                writeOpcode(writer);
                writer.write(' ');
                writeTargetLabel(writer);
                return true;
            case Format10x:
                if (instruction instanceof UnknownInstruction) {
                    writer.write("#unknown opcode: 0x");
                    writer.printUnsignedLongAsHex(((UnknownInstruction)instruction).getOriginalOpcode());
                    writer.write('\n');
                }
                writeOpcode(writer);
                return true;
            case Format11n:
                writeOpcode(writer);
                writer.write(' ');
                writeFirstRegister(writer);
                writer.write(", ");
                writeLiteral(writer);
                return true;
            case Format11x:
                writeOpcode(writer);
                writer.write(' ');
                writeFirstRegister(writer);
                return true;
            case Format12x:
                writeOpcode(writer);
                writer.write(' ');
                writeFirstRegister(writer);
                writer.write(", ");
                writeSecondRegister(writer);
                return true;
            case Format20bc:
                writeOpcode(writer);
                writer.write(' ');
                writeVerificationErrorType(writer);
                writer.write(", ");
                writeReference(writer);
                return true;
            case Format20t:
            case Format30t:
                writeOpcode(writer);
                writer.write(' ');
                writeTargetLabel(writer);
                return true;
            case Format21c:
            case Format31c:
                writeOpcode(writer);
                writer.write(' ');
                writeFirstRegister(writer);
                writer.write(", ");
                writeReference(writer);
                return true;
            case Format21ih:
            case Format21lh:
            case Format21s:
            case Format31i:
            case Format51l:
                writeOpcode(writer);
                writer.write(' ');
                writeFirstRegister(writer);
                writer.write(", ");
                writeLiteral(writer);
                return true;
            case Format21t:
            case Format31t:
                writeOpcode(writer);
                writer.write(' ');
                writeFirstRegister(writer);
                writer.write(", ");
                writeTargetLabel(writer);
                return true;
            case Format22b:
            case Format22s:
                writeOpcode(writer);
                writer.write(' ');
                writeFirstRegister(writer);
                writer.write(", ");
                writeSecondRegister(writer);
                writer.write(", ");
                writeLiteral(writer);
                return true;
            case Format22c:
                writeOpcode(writer);
                writer.write(' ');
                writeFirstRegister(writer);
                writer.write(", ");
                writeSecondRegister(writer);
                writer.write(", ");
                writeReference(writer);
                return true;
            case Format22cs:
                writeOpcode(writer);
                writer.write(' ');
                writeFirstRegister(writer);
                writer.write(", ");
                writeSecondRegister(writer);
                writer.write(", ");
                writeFieldOffset(writer);
                return true;
            case Format22t:
                writeOpcode(writer);
                writer.write(' ');
                writeFirstRegister(writer);
                writer.write(", ");
                writeSecondRegister(writer);
                writer.write(", ");
                writeTargetLabel(writer);
                return true;
            case Format22x:
            case Format32x:
                writeOpcode(writer);
                writer.write(' ');
                writeFirstRegister(writer);
                writer.write(", ");
                writeSecondRegister(writer);
                return true;
            case Format23x:
                writeOpcode(writer);
                writer.write(' ');
                writeFirstRegister(writer);
                writer.write(", ");
                writeSecondRegister(writer);
                writer.write(", ");
                writeThirdRegister(writer);
                return true;
            case Format35c:
                writeOpcode(writer);
                writer.write(' ');
                writeInvokeRegisters(writer);
                writer.write(", ");
                writeReference(writer);
                return true;
            case Format35mi:
                writeOpcode(writer);
                writer.write(' ');
                writeInvokeRegisters(writer);
                writer.write(", ");
                writeInlineIndex(writer);
                return true;
            case Format35ms:
                writeOpcode(writer);
                writer.write(' ');
                writeInvokeRegisters(writer);
                writer.write(", ");
                writeVtableIndex(writer);
                return true;
            case Format3rc:
                writeOpcode(writer);
                writer.write(' ');
                writeInvokeRangeRegisters(writer);
                writer.write(", ");
                writeReference(writer);
                return true;
            case Format3rmi:
                writeOpcode(writer);
                writer.write(' ');
                writeInvokeRangeRegisters(writer);
                writer.write(", ");
                writeInlineIndex(writer);
                return true;
            case Format3rms:
                writeOpcode(writer);
                writer.write(' ');
                writeInvokeRangeRegisters(writer);
                writer.write(", ");
                writeVtableIndex(writer);
                return true;
        }
        assert false;
        return false;
    }

    protected void writeOpcode(IndentingWriter writer) throws IOException {
        writer.write(instruction.getOpcode().name);
    }

    protected void writeTargetLabel(IndentingWriter writer) throws IOException {
        //this method is overridden by OffsetInstructionMethodItem, and should only be called for the formats that
        //have a target
        throw new RuntimeException();
    }

    protected void writeRegister(IndentingWriter writer, int registerNumber) throws IOException {
        methodDef.registerFormatter.writeTo(writer, registerNumber);
    }

    protected void writeFirstRegister(IndentingWriter writer) throws IOException {
        writeRegister(writer, ((OneRegisterInstruction)instruction).getRegisterA());
    }

    protected void writeSecondRegister(IndentingWriter writer) throws IOException {
        writeRegister(writer, ((TwoRegisterInstruction)instruction).getRegisterB());
    }

    protected void writeThirdRegister(IndentingWriter writer) throws IOException {
        writeRegister(writer, ((ThreeRegisterInstruction) instruction).getRegisterC());
    }

    protected void writeInvokeRegisters(IndentingWriter writer) throws IOException {
        FiveRegisterInstruction instruction = (FiveRegisterInstruction)this.instruction;
        final int regCount = instruction.getRegisterCount();

        writer.write('{');
        switch (regCount) {
            case 1:
                writeRegister(writer, instruction.getRegisterC());
                break;
            case 2:
                writeRegister(writer, instruction.getRegisterC());
                writer.write(", ");
                writeRegister(writer, instruction.getRegisterD());
                break;
            case 3:
                writeRegister(writer, instruction.getRegisterC());
                writer.write(", ");
                writeRegister(writer, instruction.getRegisterD());
                writer.write(", ");
                writeRegister(writer, instruction.getRegisterE());
                break;
            case 4:
                writeRegister(writer, instruction.getRegisterC());
                writer.write(", ");
                writeRegister(writer, instruction.getRegisterD());
                writer.write(", ");
                writeRegister(writer, instruction.getRegisterE());
                writer.write(", ");
                writeRegister(writer, instruction.getRegisterF());
                break;
            case 5:
                writeRegister(writer, instruction.getRegisterC());
                writer.write(", ");
                writeRegister(writer, instruction.getRegisterD());
                writer.write(", ");
                writeRegister(writer, instruction.getRegisterE());
                writer.write(", ");
                writeRegister(writer, instruction.getRegisterF());
                writer.write(", ");
                writeRegister(writer, instruction.getRegisterG());
                break;
        }
        writer.write('}');
    }

    protected void writeInvokeRangeRegisters(IndentingWriter writer) throws IOException {
        RegisterRangeInstruction instruction = (RegisterRangeInstruction)this.instruction;

        int regCount = instruction.getRegisterCount();
        if (regCount == 0) {
            writer.write("{}");
        } else {
            int startRegister = instruction.getStartRegister();
            methodDef.registerFormatter.writeRegisterRange(writer, startRegister, startRegister+regCount-1);
        }
    }

    protected void writeLiteral(IndentingWriter writer) throws IOException {
        LongRenderer.writeSignedIntOrLongTo(writer, ((WideLiteralInstruction)instruction).getWideLiteral());
    }


    protected void writeFieldOffset(IndentingWriter writer) throws IOException {
        writer.write("field@0x");
        writer.printUnsignedLongAsHex(((FieldOffsetInstruction)instruction).getFieldOffset());
    }

    protected void writeInlineIndex(IndentingWriter writer) throws IOException {
        writer.write("inline@");
        writer.printSignedIntAsDec(((InlineIndexInstruction)instruction).getInlineIndex());
    }

    protected void writeVtableIndex(IndentingWriter writer) throws IOException {
        writer.write("vtable@");
        writer.printSignedIntAsDec(((VtableIndexInstruction)instruction).getVtableIndex());
    }

    protected void writeReference(IndentingWriter writer) throws IOException {
        try {
            ReferenceFormatter.writeReference(writer, instruction.getOpcode().referenceType,
                    ((ReferenceInstruction)instruction).getReference());
        } catch (InvalidItemIndex ex) {
            writer.write(ReferenceType.toString(instruction.getOpcode().referenceType));
            writer.write("@");
            writer.printSignedIntAsDec(ex.getInvalidIndex());
        }
    }

    protected void writeVerificationErrorType(IndentingWriter writer) throws IOException {
        int verificationError = ((Instruction20bc)instruction).getVerificationError();
        writer.write(VerificationError.getVerificationErrorName(verificationError));
    }
}
