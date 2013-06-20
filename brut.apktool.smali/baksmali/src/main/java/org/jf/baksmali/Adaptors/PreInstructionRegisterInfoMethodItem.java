/*
 * [The "BSD licence"]
 * Copyright (c) 2010 Ben Gruver
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

package org.jf.baksmali.Adaptors;

import org.jf.baksmali.baksmaliOptions;
import org.jf.dexlib2.analysis.AnalyzedInstruction;
import org.jf.dexlib2.analysis.MethodAnalyzer;
import org.jf.dexlib2.analysis.RegisterType;
import org.jf.dexlib2.iface.instruction.*;
import org.jf.util.IndentingWriter;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.BitSet;

public class PreInstructionRegisterInfoMethodItem extends MethodItem {
    private final int registerInfo;
    @Nonnull private final MethodAnalyzer methodAnalyzer;
    @Nonnull private final RegisterFormatter registerFormatter;
    @Nonnull private final AnalyzedInstruction analyzedInstruction;

    public PreInstructionRegisterInfoMethodItem(int registerInfo,
                                                @Nonnull MethodAnalyzer methodAnalyzer,
                                                @Nonnull RegisterFormatter registerFormatter,
                                                @Nonnull AnalyzedInstruction analyzedInstruction,
                                                int codeAddress) {
        super(codeAddress);
        this.registerInfo = registerInfo;
        this.methodAnalyzer = methodAnalyzer;
        this.registerFormatter = registerFormatter;
        this.analyzedInstruction = analyzedInstruction;
    }

    @Override
    public double getSortOrder() {
        return 99.9;
    }

    @Override
    public boolean writeTo(IndentingWriter writer) throws IOException {
        int registerCount = analyzedInstruction.getRegisterCount();
        BitSet registers = new BitSet(registerCount);
        BitSet mergeRegisters = null;

        if ((registerInfo & baksmaliOptions.ALL) != 0) {
            registers.set(0, registerCount);
        } else {
            if ((registerInfo & baksmaliOptions.ALLPRE) != 0) {
                registers.set(0, registerCount);
            } else {
                if ((registerInfo & baksmaliOptions.ARGS) != 0) {
                    addArgsRegs(registers);
                }
                if ((registerInfo & baksmaliOptions.MERGE) != 0) {
                    if (analyzedInstruction.isBeginningInstruction()) {
                        addParamRegs(registers, registerCount);
                    }
                    mergeRegisters = new BitSet(registerCount);
                    addMergeRegs(mergeRegisters, registerCount);
                } else if ((registerInfo & baksmaliOptions.FULLMERGE) != 0 &&
                        (analyzedInstruction.isBeginningInstruction())) {
                    addParamRegs(registers, registerCount);
                }
            }
        }

        if ((registerInfo & baksmaliOptions.FULLMERGE) != 0) {
            if (mergeRegisters == null) {
                mergeRegisters = new BitSet(registerCount);
                addMergeRegs(mergeRegisters, registerCount);
            }
            registers.or(mergeRegisters);
        } else if (mergeRegisters != null) {
            registers.or(mergeRegisters);
            mergeRegisters = null;
        }

        return writeRegisterInfo(writer, registers, mergeRegisters);
    }

    private void addArgsRegs(BitSet registers) {
        if (analyzedInstruction.getInstruction() instanceof RegisterRangeInstruction) {
            RegisterRangeInstruction instruction = (RegisterRangeInstruction)analyzedInstruction.getInstruction();

            registers.set(instruction.getStartRegister(),
                    instruction.getStartRegister() + instruction.getRegisterCount());
        } else if (analyzedInstruction.getInstruction() instanceof FiveRegisterInstruction) {
            FiveRegisterInstruction instruction = (FiveRegisterInstruction)analyzedInstruction.getInstruction();
            int regCount = instruction.getRegisterCount();
            switch (regCount) {
                case 5:
                    registers.set(instruction.getRegisterG());
                    //fall through
                case 4:
                    registers.set(instruction.getRegisterF());
                    //fall through
                case 3:
                    registers.set(instruction.getRegisterE());
                    //fall through
                case 2:
                    registers.set(instruction.getRegisterD());
                    //fall through
                case 1:
                    registers.set(instruction.getRegisterC());
            }
        } else if (analyzedInstruction.getInstruction() instanceof ThreeRegisterInstruction) {
            ThreeRegisterInstruction instruction = (ThreeRegisterInstruction)analyzedInstruction.getInstruction();
            registers.set(instruction.getRegisterA());
            registers.set(instruction.getRegisterB());
            registers.set(instruction.getRegisterC());
        } else if (analyzedInstruction.getInstruction() instanceof TwoRegisterInstruction) {
            TwoRegisterInstruction instruction = (TwoRegisterInstruction)analyzedInstruction.getInstruction();
            registers.set(instruction.getRegisterA());
            registers.set(instruction.getRegisterB());
        } else if (analyzedInstruction.getInstruction() instanceof OneRegisterInstruction) {
            OneRegisterInstruction instruction = (OneRegisterInstruction)analyzedInstruction.getInstruction();
            registers.set(instruction.getRegisterA());
        }
    }

    private void addMergeRegs(BitSet registers, int registerCount) {
        if (analyzedInstruction.getPredecessorCount() <= 1) {
            //in the common case of an instruction that only has a single predecessor which is the previous
            //instruction, the pre-instruction registers will always match the previous instruction's
            //post-instruction registers
            return;
        }

        for (int registerNum=0; registerNum<registerCount; registerNum++) {
            RegisterType mergedRegisterType = analyzedInstruction.getPreInstructionRegisterType(registerNum);

            for (AnalyzedInstruction predecessor: analyzedInstruction.getPredecessors()) {
                RegisterType predecessorRegisterType = predecessor.getPostInstructionRegisterType(registerNum);
                if (predecessorRegisterType.category != RegisterType.UNKNOWN &&
                        !predecessorRegisterType.equals(mergedRegisterType)) {
                    registers.set(registerNum);
                }
            }
        }
    }

    private void addParamRegs(BitSet registers, int registerCount) {
        int parameterRegisterCount = methodAnalyzer.getParamRegisterCount();
        registers.set(registerCount-parameterRegisterCount, registerCount);
    }

    private void writeFullMerge(IndentingWriter writer, int registerNum) throws IOException {
        registerFormatter.writeTo(writer, registerNum);
        writer.write('=');
        analyzedInstruction.getPreInstructionRegisterType(registerNum).writeTo(writer);
        writer.write(":merge{");

        boolean first = true;

        for (AnalyzedInstruction predecessor: analyzedInstruction.getPredecessors()) {
            RegisterType predecessorRegisterType = predecessor.getPostInstructionRegisterType(registerNum);

            if (!first) {
                writer.write(',');
            }

            if (predecessor.getInstructionIndex() == -1) {
                //the fake "StartOfMethod" instruction
                writer.write("Start:");
            } else {
                writer.write("0x");
                writer.printUnsignedLongAsHex(methodAnalyzer.getInstructionAddress(predecessor));
                writer.write(':');
            }
            predecessorRegisterType.writeTo(writer);

            first = false;
        }
        writer.write('}');
    }

    private boolean writeRegisterInfo(IndentingWriter writer, BitSet registers,
                                      BitSet fullMergeRegisters) throws IOException {
        boolean firstRegister = true;
        boolean previousWasFullMerge = false;
        int registerNum = registers.nextSetBit(0);
        if (registerNum < 0) {
            return false;
        }

        writer.write('#');
        for (; registerNum >= 0; registerNum = registers.nextSetBit(registerNum + 1)) {
            boolean fullMerge = fullMergeRegisters!=null && fullMergeRegisters.get(registerNum);
            if (fullMerge) {
                if (!firstRegister) {
                    writer.write('\n');
                    writer.write('#');
                }
                writeFullMerge(writer, registerNum);
                previousWasFullMerge = true;
            } else {
                if (previousWasFullMerge) {
                    writer.write('\n');
                    writer.write('#');
                    previousWasFullMerge = false;
                }

                RegisterType registerType = analyzedInstruction.getPreInstructionRegisterType(registerNum);

                registerFormatter.writeTo(writer, registerNum);
                writer.write('=');

                registerType.writeTo(writer);
                writer.write(';');
            }

            firstRegister = false;
        }
        return true;
    }
}
