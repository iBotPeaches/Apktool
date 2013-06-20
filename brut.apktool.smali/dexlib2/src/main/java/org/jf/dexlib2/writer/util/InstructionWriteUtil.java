/*
 * Copyright 2013, Google Inc.
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

package org.jf.dexlib2.writer.util;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.jf.dexlib2.Format;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.ReferenceType;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.ReferenceInstruction;
import org.jf.dexlib2.iface.instruction.SwitchElement;
import org.jf.dexlib2.iface.instruction.SwitchPayload;
import org.jf.dexlib2.iface.instruction.formats.*;
import org.jf.dexlib2.iface.reference.*;
import org.jf.dexlib2.immutable.instruction.*;
import org.jf.dexlib2.util.InstructionUtil;
import org.jf.dexlib2.util.MethodUtil;
import org.jf.dexlib2.writer.InstructionFactory;
import org.jf.util.ExceptionWithContext;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class InstructionWriteUtil<Insn extends Instruction, StringRef extends StringReference,
        BaseReference extends Reference> {
    private final StringIndexProvider<StringRef> stringIndexProvider;
    private final InstructionFactory<? extends Insn, BaseReference> instructionFactory;
    private final Iterable<? extends Insn> originalInstructions;

    private List<Insn> instructions;
    private ArrayList<Integer> codeOffsetShifts;
    private HashMap<Integer,Format> offsetToNewInstructionMap;

    private int codeUnitCount;
    private int outParamCount;

    public static interface StringIndexProvider<StringRef extends StringReference> {
        int getItemIndex(@Nonnull StringRef reference);
    }

    public InstructionWriteUtil(@Nonnull Iterable<? extends Insn> instructions,
                                @Nonnull StringIndexProvider<StringRef> stringIndexProvider,
                                @Nonnull InstructionFactory<? extends Insn, BaseReference> instructionFactory) {
        this.stringIndexProvider = stringIndexProvider;
        this.instructionFactory = instructionFactory;
        this.originalInstructions = instructions;
        calculateMaxOutParamCount();
        findCodeOffsetShifts();
        modifyInstructions();
    }

    private void calculateMaxOutParamCount() {
        for (Insn instruction: originalInstructions) {
            codeUnitCount += instruction.getCodeUnits();
            if (instruction.getOpcode().referenceType == ReferenceType.METHOD) {
                ReferenceInstruction refInsn = (ReferenceInstruction)instruction;
                MethodReference methodRef = (MethodReference)refInsn.getReference();
                int paramCount = MethodUtil.getParameterRegisterCount(methodRef, InstructionUtil.isInvokeStatic(instruction.getOpcode()));
                if (paramCount > outParamCount) {
                    outParamCount = paramCount;
                }
            }
        }
    }
    public Iterable<? extends Insn> getInstructions() {
        if (instructions != null) {
            return instructions;
        } else {
            return originalInstructions;
        }
    }

    public int getCodeUnitCount() {
        return codeUnitCount;
    }

    public int getOutParamCount() {
        return outParamCount;
    }

    private int targetOffsetShift(int instrOffset, int targetOffset) {
        int targetOffsetShift = 0;
        if (codeOffsetShifts != null) {
            int instrShift = codeOffsetShift(instrOffset);
            int targetShift = codeOffsetShift(instrOffset+targetOffset);
            targetOffsetShift = targetShift - instrShift;
        }
        return targetOffsetShift;
    }

    public int codeOffsetShift(int offset) {
        int shift = 0;
        if (codeOffsetShifts != null) {
            int numCodeOffsetShifts = codeOffsetShifts.size();
            if (numCodeOffsetShifts > 0) {
                if (offset >= codeOffsetShifts.get(numCodeOffsetShifts-1)) {
                    shift = numCodeOffsetShifts;
                } else if (numCodeOffsetShifts>1) {
                    for (int i=1;i<numCodeOffsetShifts;i++) {
                        if (offset >= codeOffsetShifts.get(i-1) && offset < codeOffsetShifts.get(i)) {
                            shift = i;
                            break;
                        }
                    }
                }
            }
        }
        return shift;
    }

    /*
     * This method creates a list of code offsets of instructions, whose (and subsequent instructions')
     * code offset will get shifted by one code unit with respect to previous instruction(s).
     * This happens when the previous instruction has to be changed to a larger sized one
     * to fit the new value or payload instruction has to be prepended by nop to ensure alignment.
     */
    private void findCodeOffsetShifts() {
        // first, process const-string to const-string/jumbo conversions
        int currentCodeOffset = 0;
        codeOffsetShifts = Lists.newArrayList();
        offsetToNewInstructionMap = Maps.newHashMap();

        for (Instruction instruction: originalInstructions) {
            if (instruction.getOpcode().equals(Opcode.CONST_STRING)) {
                ReferenceInstruction refInstr = (ReferenceInstruction) instruction;
                int referenceIndex = stringIndexProvider.getItemIndex((StringRef)refInstr.getReference());
                if (referenceIndex > 0xFFFF) {
                    codeOffsetShifts.add(currentCodeOffset+instruction.getCodeUnits());
                    offsetToNewInstructionMap.put(currentCodeOffset, Opcode.CONST_STRING_JUMBO.format);
                }
            }
            currentCodeOffset += instruction.getCodeUnits();
        }

        // next, let's check if this caused any conversions in goto instructions due to changes in offset values
        // since code offset delta is equivalent to the position of instruction's code offset in the shift list,
        // we use it as a position here
        // we also check if we will have to insert nops to ensure 4-byte alignment for switch statements and packed arrays
        boolean shiftsInserted;
        do {
            currentCodeOffset = 0;
            shiftsInserted = false;
            for (Instruction instruction: originalInstructions) {
                if (instruction.getOpcode().format.equals(Format.Format10t) && !offsetToNewInstructionMap.containsKey(currentCodeOffset)) {
                    int targetOffset = ((Instruction10t)instruction).getCodeOffset();
                    int codeOffsetDelta = codeOffsetShift(currentCodeOffset);
                    int newTargetOffset = targetOffset + targetOffsetShift(currentCodeOffset, targetOffset);
                    if ((byte)newTargetOffset != newTargetOffset) {
                        if ((short)newTargetOffset != newTargetOffset) {
                            // handling very small (negligible) possibility of goto becoming goto/32
                            // we insert extra 1 code unit shift referring to the same position
                            // this will cause subsequent code offsets to be shifted by 2 code units
                            codeOffsetShifts.add(codeOffsetDelta, currentCodeOffset+instruction.getCodeUnits());
                            offsetToNewInstructionMap.put(currentCodeOffset, Format.Format30t);
                        } else {
                            offsetToNewInstructionMap.put(currentCodeOffset, Format.Format20t);
                        }
                        codeOffsetShifts.add(codeOffsetDelta, currentCodeOffset+instruction.getCodeUnits());
                        shiftsInserted = true;
                    }
                } else if (instruction.getOpcode().format.equals(Format.Format20t) && !offsetToNewInstructionMap.containsKey(currentCodeOffset)) {
                    int targetOffset = ((Instruction20t)instruction).getCodeOffset();
                    int codeOffsetDelta = codeOffsetShift(currentCodeOffset);
                    int newTargetOffset = targetOffset + targetOffsetShift(currentCodeOffset, targetOffset);
                    if ((short)newTargetOffset != newTargetOffset) {
                        codeOffsetShifts.add(codeOffsetDelta, currentCodeOffset+instruction.getCodeUnits());
                        offsetToNewInstructionMap.put(currentCodeOffset, Format.Format30t);
                        shiftsInserted = true;
                    }
                } else if (instruction.getOpcode().format.equals(Format.ArrayPayload)
                        || instruction.getOpcode().format.equals(Format.SparseSwitchPayload)
                        || instruction.getOpcode().format.equals(Format.PackedSwitchPayload)) {
                    int codeOffsetDelta = codeOffsetShift(currentCodeOffset);
                    if ((currentCodeOffset+codeOffsetDelta)%2 != 0) {
                        if (codeOffsetShifts.contains(currentCodeOffset)) {
                            codeOffsetShifts.remove(codeOffsetDelta-1);
                            offsetToNewInstructionMap.remove(currentCodeOffset);
                        } else {
                            codeOffsetShifts.add(codeOffsetDelta, currentCodeOffset);
                            offsetToNewInstructionMap.put(currentCodeOffset, Format.Format10x);
                            shiftsInserted = true;
                        }
                    }
                }
                currentCodeOffset += instruction.getCodeUnits();
            }
        } while (shiftsInserted);
        
        codeUnitCount += codeOffsetShifts.size();
    }

    private void modifyInstructions() {
        if (codeOffsetShifts == null) {
            return;
        }

        instructions = Lists.newArrayList();
        int currentCodeOffset = 0;
        for (Insn instruction: originalInstructions) {
            Insn modifiedInstruction = null;
            switch (instruction.getOpcode().format) {
                case Format10t: {
                    Instruction10t instr = (Instruction10t)instruction;
                    int targetOffset = instr.getCodeOffset();
                    int newTargetOffset = targetOffset + targetOffsetShift(currentCodeOffset, targetOffset);
                    Format newInstructionFormat = offsetToNewInstructionMap.get(currentCodeOffset);
                    if (newInstructionFormat != null) {
                        if (newInstructionFormat.equals(Format.Format30t)) {
                            modifiedInstruction = instructionFactory.makeInstruction30t(Opcode.GOTO_32, newTargetOffset);
                        } else if (newInstructionFormat.equals(Format.Format20t)) {
                            modifiedInstruction = instructionFactory.makeInstruction20t(Opcode.GOTO_16, newTargetOffset);
                        }
                    } else if (newTargetOffset != targetOffset) {
                        modifiedInstruction = instructionFactory.makeInstruction10t(instr.getOpcode(), newTargetOffset);
                    }
                    break;
                }
                case Format20t: {
                    Instruction20t instr = (Instruction20t)instruction;
                    int targetOffset = instr.getCodeOffset();
                    int newTargetOffset = targetOffset + targetOffsetShift(currentCodeOffset, targetOffset);
                    Format newInstructionFormat = offsetToNewInstructionMap.get(currentCodeOffset);
                    if (newInstructionFormat != null && newInstructionFormat.equals(Format.Format30t)) {
                        modifiedInstruction = instructionFactory.makeInstruction30t(Opcode.GOTO_32, newTargetOffset);
                    } else if (newTargetOffset != targetOffset) {
                        modifiedInstruction = instructionFactory.makeInstruction20t(Opcode.GOTO_16, newTargetOffset);
                    }
                    break;
                }
                case Format21c: {
                    Instruction21c instr = (Instruction21c)instruction;
                    if (instr.getOpcode().equals(Opcode.CONST_STRING)) {
                        int referenceIndex = stringIndexProvider.getItemIndex((StringRef)instr.getReference());
                        if (referenceIndex > 0xFFFF) {
                            modifiedInstruction = instructionFactory.makeInstruction31c(Opcode.CONST_STRING_JUMBO,
                                    instr.getRegisterA(), (BaseReference)instr.getReference());
                        }
                    }
                    break;
                }
                case Format21t: {
                    Instruction21t instr = (Instruction21t)instruction;
                    int targetOffset = instr.getCodeOffset();
                    int newTargetOffset = targetOffset + targetOffsetShift(currentCodeOffset, targetOffset);
                    if (newTargetOffset != targetOffset) {
                        modifiedInstruction = instructionFactory.makeInstruction21t(instr.getOpcode(),
                                instr.getRegisterA(), newTargetOffset);
                    }
                    break;
                }
                case Format22t: {
                    Instruction22t instr = (Instruction22t)instruction;
                    int targetOffset = instr.getCodeOffset();
                    int newTargetOffset = targetOffset + targetOffsetShift(currentCodeOffset, targetOffset);
                    if (newTargetOffset != targetOffset) {
                        modifiedInstruction = instructionFactory.makeInstruction22t(instr.getOpcode(),
                                instr.getRegisterA(), instr.getRegisterB(), newTargetOffset);
                    }
                    break;
                }
                case Format30t: {
                    Instruction30t instr = (Instruction30t)instruction;
                    int targetOffset = instr.getCodeOffset();
                    int newTargetOffset = targetOffset + targetOffsetShift(currentCodeOffset, targetOffset);
                    if (newTargetOffset != targetOffset) {
                        modifiedInstruction = instructionFactory.makeInstruction30t(instr.getOpcode(), newTargetOffset);
                    }
                    break;
                }
                case Format31t: {
                    Instruction31t instr = (Instruction31t)instruction;
                    int targetOffset = instr.getCodeOffset();
                    int newTargetOffset = targetOffset + targetOffsetShift(currentCodeOffset, targetOffset);
                    if (newTargetOffset != targetOffset) {
                        modifiedInstruction = instructionFactory.makeInstruction31t(instr.getOpcode(),
                                instr.getRegisterA(), newTargetOffset);
                    }
                    break;
                }
                case SparseSwitchPayload: {
                    alignPayload(currentCodeOffset);
                    int switchInstructionOffset = findSwitchInstructionOffset(currentCodeOffset);
                    SwitchPayload payload = (SwitchPayload)instruction;
                    if (isSwitchTargetOffsetChanged(payload, switchInstructionOffset)) {
                        List<SwitchElement> newSwitchElements = modifySwitchElements(payload, switchInstructionOffset);
                        modifiedInstruction = instructionFactory.makeSparseSwitchPayload(newSwitchElements);
                    }
                    break;
                }
                case PackedSwitchPayload: {
                    alignPayload(currentCodeOffset);
                    int switchInstructionOffset = findSwitchInstructionOffset(currentCodeOffset);
                    SwitchPayload payload = (SwitchPayload)instruction;
                    if (isSwitchTargetOffsetChanged(payload, switchInstructionOffset)) {
                        List<SwitchElement> newSwitchElements = modifySwitchElements(payload, switchInstructionOffset);
                        modifiedInstruction = instructionFactory.makePackedSwitchPayload(newSwitchElements);
                    }
                    break;
                }
                case ArrayPayload: {
                    alignPayload(currentCodeOffset);
                    break;
                }
            }
            
            if (modifiedInstruction != null) {
                instructions.add(modifiedInstruction);
            } else {
                instructions.add(instruction);
            }
            
            currentCodeOffset += instruction.getCodeUnits();
        }
    }
    
    private void alignPayload(int codeOffset) {
        Format newInstructionFormat = offsetToNewInstructionMap.get(codeOffset);
        if (newInstructionFormat != null && newInstructionFormat.equals(Format.Format10x)) {
            instructions.add(instructionFactory.makeInstruction10x(Opcode.NOP));
        }
    }
    
    private int findSwitchInstructionOffset(int payloadOffset) {
        int currentCodeOffset = 0;
        int switchInstructionOffset = -1;
        for (Instruction instruction: originalInstructions) {
            if (instruction.getOpcode().equals(Opcode.PACKED_SWITCH)
                    || instruction.getOpcode().equals(Opcode.SPARSE_SWITCH)) {
                int targetOffset = currentCodeOffset + ((Instruction31t)instruction).getCodeOffset();
                if (targetOffset == payloadOffset) {
                    if (switchInstructionOffset < 0) {
                        switchInstructionOffset = currentCodeOffset;
                    } else {
                        throw new ExceptionWithContext("Multiple switch instructions refer to the same switch payload!");
                    }
                }
            }
            currentCodeOffset += instruction.getCodeUnits();
        }
        return switchInstructionOffset;
    }

    private boolean isSwitchTargetOffsetChanged(SwitchPayload payload, int switchInstructionOffset) {
        for (SwitchElement switchElement: payload.getSwitchElements()) {
            if (targetOffsetShift(switchInstructionOffset, switchElement.getOffset()) != 0) {
                return true;
            }
        }
        return false;
    }

    private ArrayList<SwitchElement> modifySwitchElements(SwitchPayload payload, int switchInstructionOffset) {
        ArrayList<SwitchElement> switchElements = Lists.newArrayList();
        for (SwitchElement switchElement: payload.getSwitchElements()) {
            int targetOffset = switchElement.getOffset();
            int newTargetOffset = targetOffset + targetOffsetShift(switchInstructionOffset, targetOffset);
            if (newTargetOffset != targetOffset) {
                ImmutableSwitchElement immuSwitchElement = new ImmutableSwitchElement(switchElement.getKey(), newTargetOffset);
                switchElements.add(immuSwitchElement);
            } else {
                switchElements.add(switchElement);
            }
        }
        return switchElements;
    }
    
}


