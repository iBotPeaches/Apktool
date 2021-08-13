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

package org.jf.dexlib2.analysis;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.iface.instruction.*;
import org.jf.dexlib2.iface.instruction.formats.Instruction22c;
import org.jf.dexlib2.iface.reference.MethodReference;
import org.jf.dexlib2.iface.reference.Reference;
import org.jf.dexlib2.iface.reference.TypeReference;
import org.jf.util.ExceptionWithContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class AnalyzedInstruction implements Comparable<AnalyzedInstruction> {
    /**
     * The MethodAnalyzer containing this instruction
     */
    @Nonnull
    protected final MethodAnalyzer methodAnalyzer;

    /**
     * The actual instruction
     */
    @Nonnull
    protected Instruction instruction;

    /**
     * The index of the instruction, where the first instruction in the method is at index 0, and so on
     */
    protected final int instructionIndex;

    /**
     * Instructions that can pass on execution to this one during normal execution
     */
    @Nonnull
    protected final TreeSet<AnalyzedInstruction> predecessors = new TreeSet<AnalyzedInstruction>();

    /**
     * Instructions that can execution could pass on to next during normal execution
     */
    @Nonnull
    protected final LinkedList<AnalyzedInstruction> successors = new LinkedList<AnalyzedInstruction>();

    /**
     * This contains the register types *before* the instruction has executed
     */
    @Nonnull
    protected final RegisterType[] preRegisterMap;

    /**
     * This contains the register types *after* the instruction has executed
     */
    @Nonnull
    protected final RegisterType[] postRegisterMap;

    /**
     * This contains optional register type overrides for register types from predecessors
     */
    @Nullable
    protected Map<PredecessorOverrideKey, RegisterType> predecessorRegisterOverrides = null;

    /**
     * When deodexing, we might need to deodex this instruction multiple times, when we merge in new register
     * information. When this happens, we need to restore the original (odexed) instruction, so we can deodex it again
     */
    protected final Instruction originalInstruction;

    public AnalyzedInstruction(@Nonnull MethodAnalyzer methodAnalyzer, @Nonnull Instruction instruction,
                               int instructionIndex, int registerCount) {
        this.methodAnalyzer = methodAnalyzer;
        this.instruction = instruction;
        this.originalInstruction = instruction;
        this.instructionIndex = instructionIndex;
        this.postRegisterMap = new RegisterType[registerCount];
        this.preRegisterMap = new RegisterType[registerCount];
        RegisterType unknown = RegisterType.getRegisterType(RegisterType.UNKNOWN, null);
        for (int i=0; i<registerCount; i++) {
            preRegisterMap[i] = unknown;
            postRegisterMap[i] = unknown;
        }
    }

    public int getInstructionIndex() {
        return instructionIndex;
    }

    public int getPredecessorCount() {
        return predecessors.size();
    }

    public SortedSet<AnalyzedInstruction> getPredecessors() {
        return Collections.unmodifiableSortedSet(predecessors);
    }

    public RegisterType getPredecessorRegisterType(@Nonnull AnalyzedInstruction predecessor, int registerNumber) {
        if (predecessorRegisterOverrides != null) {
            RegisterType override = predecessorRegisterOverrides.get(
                    new PredecessorOverrideKey(predecessor, registerNumber));
            if (override != null) {
                return override;
            }
        }
        return predecessor.postRegisterMap[registerNumber];
    }

    protected boolean addPredecessor(AnalyzedInstruction predecessor) {
        return predecessors.add(predecessor);
    }

    protected void addSuccessor(AnalyzedInstruction successor) {
        successors.add(successor);
    }

    protected void setDeodexedInstruction(Instruction instruction) {
        assert originalInstruction.getOpcode().odexOnly();
        this.instruction = instruction;
    }

    protected void restoreOdexedInstruction() {
        assert originalInstruction.getOpcode().odexOnly();
        instruction = originalInstruction;
    }

    @Nonnull
    public List<AnalyzedInstruction> getSuccessors() {
        return Collections.unmodifiableList(successors);
    }

    @Nonnull
    public Instruction getInstruction() {
        return instruction;
    }

    @Nonnull
    public Instruction getOriginalInstruction() {
        return originalInstruction;
    }

    /**
     * Is this instruction a "beginning instruction". A beginning instruction is defined to be an instruction
     * that can be the first successfully executed instruction in the method. The first instruction is always a
     * beginning instruction. If the first instruction can throw an exception, and is covered by a try block, then
     * the first instruction of any exception handler for that try block is also a beginning instruction. And likewise,
     * if any of those instructions can throw an exception and are covered by try blocks, the first instruction of the
     * corresponding exception handler is a beginning instruction, etc.
     *
     * To determine this, we simply check if the first predecessor is the fake "StartOfMethod" instruction, which has
     * an instruction index of -1.
     * @return a boolean value indicating whether this instruction is a beginning instruction
     */
    public boolean isBeginningInstruction() {
        //if this instruction has no predecessors, it is either the fake "StartOfMethod" instruction or it is an
        //unreachable instruction.
        if (predecessors.size() == 0) {
            return false;
        }
        return predecessors.first().instructionIndex == -1;
    }

    /*
     * Merges the given register type into the specified pre-instruction register, and also sets the post-instruction
     * register type accordingly if it isn't a destination register for this instruction
     * @param registerNumber Which register to set
     * @param registerType The register type
     * @returns true If the post-instruction register type was changed. This might be false if either the specified
     * register is a destination register for this instruction, or if the pre-instruction register type didn't change
     * after merging in the given register type
     */
    protected boolean mergeRegister(int registerNumber, RegisterType registerType, BitSet verifiedInstructions,
                                    boolean override) {
        assert registerNumber >= 0 && registerNumber < postRegisterMap.length;
        assert registerType != null;

        RegisterType oldRegisterType = preRegisterMap[registerNumber];

        RegisterType mergedRegisterType;
        if (override) {
            mergedRegisterType = getMergedPreRegisterTypeFromPredecessors(registerNumber);
        } else {
            mergedRegisterType = oldRegisterType.merge(registerType);
        }

        if (mergedRegisterType.equals(oldRegisterType)) {
            return false;
        }

        preRegisterMap[registerNumber] = mergedRegisterType;
        verifiedInstructions.clear(instructionIndex);

        if (!setsRegister(registerNumber)) {
            postRegisterMap[registerNumber] = mergedRegisterType;
            return true;
        }

        return false;
    }

    /**
     * Iterates over the predecessors of this instruction, and merges all the post-instruction register types for the
     * given register. Any dead, unreachable, or odexed predecessor is ignored. This takes into account any overridden
     * predecessor register types
     *
     * @param registerNumber the register number
     * @return The register type resulting from merging the post-instruction register types from all predecessors
     */
    @Nonnull
    protected RegisterType getMergedPreRegisterTypeFromPredecessors(int registerNumber) {
        RegisterType mergedRegisterType = null;
        for (AnalyzedInstruction predecessor: predecessors) {
            RegisterType predecessorRegisterType = getPredecessorRegisterType(predecessor, registerNumber);
            if (predecessorRegisterType != null) {
                if (mergedRegisterType == null) {
                    mergedRegisterType = predecessorRegisterType;
                } else {
                    mergedRegisterType = predecessorRegisterType.merge(mergedRegisterType);
                }
            }
        }
        if (mergedRegisterType == null) {
            // This is a start-of-method or unreachable instruction.
            throw new IllegalStateException();
        }
        return mergedRegisterType;
    }
    /**
     * Sets the "post-instruction" register type as indicated.
     * @param registerNumber Which register to set
     * @param registerType The "post-instruction" register type
     * @return true if the given register type is different than the existing post-instruction register type
     */
    protected boolean setPostRegisterType(int registerNumber, RegisterType registerType) {
        assert registerNumber >= 0 && registerNumber < postRegisterMap.length;
        assert registerType != null;

        RegisterType oldRegisterType = postRegisterMap[registerNumber];
        if (oldRegisterType.equals(registerType)) {
            return false;
        }

        postRegisterMap[registerNumber] = registerType;
        return true;
    }

    /**
     * Adds an override for a register type from a predecessor.
     *
     * This is used to set the register type for only one branch from a conditional jump.
     *
     * @param predecessor Which predecessor is being overridden
     * @param registerNumber The register number of the register being overridden
     * @param registerType The overridden register type
     * @param verifiedInstructions A bit vector of instructions that have been verified
     *
     * @return true if the post-instruction register type for this instruction changed as a result of this override
     */
    protected boolean overridePredecessorRegisterType(@Nonnull AnalyzedInstruction predecessor, int registerNumber,
                                                      @Nonnull RegisterType registerType, BitSet verifiedInstructions) {
        if (predecessorRegisterOverrides == null) {
            predecessorRegisterOverrides = Maps.newHashMap();
        }
        predecessorRegisterOverrides.put(new PredecessorOverrideKey(predecessor, registerNumber), registerType);

        RegisterType mergedType = getMergedPreRegisterTypeFromPredecessors(registerNumber);

        if (preRegisterMap[registerNumber].equals(mergedType)) {
            return false;
        }

        preRegisterMap[registerNumber] = mergedType;
        verifiedInstructions.clear(instructionIndex);

        if (!setsRegister(registerNumber)) {
            if (!postRegisterMap[registerNumber].equals(mergedType)) {
                postRegisterMap[registerNumber] = mergedType;
                return true;
            }
        }

        return false;
    }

    public boolean isInvokeInit() {
        if (!instruction.getOpcode().canInitializeReference()) {
            return false;
        }

        ReferenceInstruction instruction = (ReferenceInstruction)this.instruction;

        Reference reference = instruction.getReference();
        if (reference instanceof MethodReference) {
            return ((MethodReference)reference).getName().equals("<init>");
        }

        return false;
    }

    /**
     * Determines if this instruction sets the given register, or alters its type
     *
     * @param registerNumber The register to check
     * @return true if this instruction sets the given register or alters its type
     */
    public boolean setsRegister(int registerNumber) {
        // This method could be implemented by calling getSetRegisters and checking if registerNumber is in the result
        // However, this is a frequently called method, and this is a more efficient implementation, because it doesn't
        // allocate a new list, and it can potentially exit earlier

        if (isInvokeInit()) {
            // When constructing a new object, the register type will be an uninitialized reference after the
            // new-instance instruction, but becomes an initialized reference once the <init> method is called. So even
            // though invoke instructions don't normally change any registers, calling an <init> method will change the
            // type of its object register. If the uninitialized reference has been copied to other registers, they will
            // be initialized as well, so we need to check for that too
            int destinationRegister;
            if (instruction instanceof FiveRegisterInstruction) {
                assert ((FiveRegisterInstruction)instruction).getRegisterCount() > 0;
                destinationRegister = ((FiveRegisterInstruction)instruction).getRegisterC();
            } else {
                assert instruction instanceof RegisterRangeInstruction;
                RegisterRangeInstruction rangeInstruction = (RegisterRangeInstruction)instruction;
                assert rangeInstruction.getRegisterCount() > 0;
                destinationRegister = rangeInstruction.getStartRegister();
            }

            RegisterType preInstructionDestRegisterType = getPreInstructionRegisterType(destinationRegister);
            if (preInstructionDestRegisterType.category == RegisterType.UNKNOWN) {
                // We never let an uninitialized reference propagate past an invoke-init if the object register type is
                // unknown This is because the uninitialized reference may be an alias to the reference being
                // initialized, but we can't know that until the object register's type is known
                RegisterType preInstructionRegisterType = getPreInstructionRegisterType(registerNumber);
                if (preInstructionRegisterType.category == RegisterType.UNINIT_REF ||
                        preInstructionRegisterType.category == RegisterType.UNINIT_THIS) {
                    return true;
                }
            }

            if (preInstructionDestRegisterType.category != RegisterType.UNINIT_REF &&
                    preInstructionDestRegisterType.category != RegisterType.UNINIT_THIS) {
                return false;
            }

            if (registerNumber == destinationRegister) {
                return true;
            }

            //check if the uninit ref has been copied to another register
            return preInstructionDestRegisterType.equals(getPreInstructionRegisterType(registerNumber));
        }

        // On art, the optimizer will often nop out a check-cast instruction after an instance-of instruction.
        // Normally, check-cast is where the register type actually changes.
        // In order to correctly handle this case, we have to propagate the narrowed register type to the appropriate
        // branch of the following if-eqz/if-nez
        if (instructionIndex > 0 &&
                methodAnalyzer.getClassPath().isArt() &&
                getPredecessorCount() == 1 &&
                (instruction.getOpcode() == Opcode.IF_EQZ || instruction.getOpcode() == Opcode.IF_NEZ)) {

            AnalyzedInstruction prevInstruction = predecessors.first();
            if (prevInstruction.instruction.getOpcode() == Opcode.INSTANCE_OF &&
                    MethodAnalyzer.canPropagateTypeAfterInstanceOf(
                            prevInstruction, this, methodAnalyzer.getClassPath())) {
                Instruction22c instanceOfInstruction = (Instruction22c)prevInstruction.instruction;

                if (registerNumber == instanceOfInstruction.getRegisterB()) {
                    return true;
                }

                // Additionally, there may be a move instruction just before the instance-of, in order to put the value
                // into a register that is addressable by the instance-of. In this case, we also need to propagate the
                // new register type for the original register that the value was moved from.
                // In some cases, the instance-of may have multiple predecessors. In this case, we should only do the
                // propagation if all predecessors are move-object instructions for the same source register
                // TODO: do we need to do some sort of additional check that these multiple move-object predecessors actually refer to the same value?
                if (instructionIndex > 1) {
                    int originalSourceRegister = -1;

                    RegisterType newType = null;

                    for (AnalyzedInstruction prevPrevAnalyzedInstruction : prevInstruction.predecessors) {
                        Opcode opcode = prevPrevAnalyzedInstruction.instruction.getOpcode();
                        if (opcode == Opcode.MOVE_OBJECT || opcode == Opcode.MOVE_OBJECT_16 ||
                                opcode == Opcode.MOVE_OBJECT_FROM16) {
                            TwoRegisterInstruction moveInstruction =
                                    ((TwoRegisterInstruction)prevPrevAnalyzedInstruction.instruction);
                            RegisterType originalType =
                                    prevPrevAnalyzedInstruction.getPostInstructionRegisterType(
                                            moveInstruction.getRegisterB());
                            if (moveInstruction.getRegisterA() != instanceOfInstruction.getRegisterB()) {
                                originalSourceRegister = -1;
                                break;
                            }
                            if (originalType.type == null) {
                                originalSourceRegister = -1;
                                break;
                            }

                            if (newType == null) {
                                newType = RegisterType.getRegisterType(methodAnalyzer.getClassPath(),
                                        (TypeReference)instanceOfInstruction.getReference());
                            }

                            if (MethodAnalyzer.isNotWideningConversion(originalType, newType)) {
                                if (originalSourceRegister != -1) {
                                    if (originalSourceRegister != moveInstruction.getRegisterB()) {
                                        originalSourceRegister = -1;
                                        break;
                                    }
                                } else {
                                    originalSourceRegister = moveInstruction.getRegisterB();
                                }
                            }
                        } else {
                            originalSourceRegister = -1;
                            break;
                        }
                    }
                    if (originalSourceRegister != -1 && registerNumber == originalSourceRegister) {
                        return true;
                    }
                }
            }
        }

        if (!instruction.getOpcode().setsRegister()) {
            return false;
        }
        int destinationRegister = getDestinationRegister();

        if (registerNumber == destinationRegister) {
            return true;
        }
        if (instruction.getOpcode().setsWideRegister() && registerNumber == (destinationRegister + 1)) {
            return true;
        }
        return false;
    }

    public List<Integer> getSetRegisters() {
        List<Integer> setRegisters = Lists.newArrayList();

        if (instruction.getOpcode().setsRegister()) {
            setRegisters.add(getDestinationRegister());
        }
        if (instruction.getOpcode().setsWideRegister()) {
            setRegisters.add(getDestinationRegister() + 1);
        }

        if (isInvokeInit()) {
            //When constructing a new object, the register type will be an uninitialized reference after the new-instance
            //instruction, but becomes an initialized reference once the <init> method is called. So even though invoke
            //instructions don't normally change any registers, calling an <init> method will change the type of its
            //object register. If the uninitialized reference has been copied to other registers, they will be initialized
            //as well, so we need to check for that too

            int destinationRegister;
            if (instruction instanceof FiveRegisterInstruction) {
                destinationRegister = ((FiveRegisterInstruction)instruction).getRegisterC();
                assert ((FiveRegisterInstruction)instruction).getRegisterCount() > 0;
            } else {
                assert instruction instanceof RegisterRangeInstruction;
                RegisterRangeInstruction rangeInstruction = (RegisterRangeInstruction)instruction;
                assert rangeInstruction.getRegisterCount() > 0;
                destinationRegister = rangeInstruction.getStartRegister();
            }

            RegisterType preInstructionDestRegisterType = getPreInstructionRegisterType(destinationRegister);
            if (preInstructionDestRegisterType.category == RegisterType.UNINIT_REF ||
                    preInstructionDestRegisterType.category == RegisterType.UNINIT_THIS) {
                setRegisters.add(destinationRegister);

                RegisterType objectRegisterType = preRegisterMap[destinationRegister];
                for (int i = 0; i < preRegisterMap.length; i++) {
                    if (i == destinationRegister) {
                        continue;
                    }

                    RegisterType preInstructionRegisterType = preRegisterMap[i];

                    if (preInstructionRegisterType.equals(objectRegisterType)) {
                        setRegisters.add(i);
                    } else if (preInstructionRegisterType.category == RegisterType.UNINIT_REF ||
                            preInstructionRegisterType.category == RegisterType.UNINIT_THIS) {
                        RegisterType postInstructionRegisterType = postRegisterMap[i];
                        if (postInstructionRegisterType.category == RegisterType.UNKNOWN) {
                            setRegisters.add(i);
                        }
                    }
                }
            } else if (preInstructionDestRegisterType.category == RegisterType.UNKNOWN) {
                // We never let an uninitialized reference propagate past an invoke-init if the object register type is
                // unknown This is because the uninitialized reference may be an alias to the reference being
                // initialized, but we can't know that until the object register's type is known

                for (int i = 0; i < preRegisterMap.length; i++) {
                    RegisterType registerType = preRegisterMap[i];
                    if (registerType.category == RegisterType.UNINIT_REF ||
                            registerType.category == RegisterType.UNINIT_THIS) {
                        setRegisters.add(i);
                    }
                }
            }
        }

        // On art, the optimizer will often nop out a check-cast instruction after an instance-of instruction.
        // Normally, check-cast is where the register type actually changes.
        // In order to correctly handle this case, we have to propagate the narrowed register type to the appropriate
        // branch of the following if-eqz/if-nez
        if (instructionIndex > 0 &&
                methodAnalyzer.getClassPath().isArt() &&
                getPredecessorCount() == 1 &&
                (instruction.getOpcode() == Opcode.IF_EQZ || instruction.getOpcode() == Opcode.IF_NEZ)) {

            AnalyzedInstruction prevInstruction = predecessors.first();
            if (prevInstruction.instruction.getOpcode() == Opcode.INSTANCE_OF &&
                    MethodAnalyzer.canPropagateTypeAfterInstanceOf(
                            prevInstruction, this, methodAnalyzer.getClassPath())) {
                Instruction22c instanceOfInstruction = (Instruction22c)prevInstruction.instruction;
                setRegisters.add(instanceOfInstruction.getRegisterB());

                // Additionally, there may be a move instruction just before the instance-of, in order to put the value
                // into a register that is addressable by the instance-of. In this case, we also need to propagate the
                // new register type for the original register that the value was moved from.
                // In some cases, the instance-of may have multiple predecessors. In this case, we should only do the
                // propagation if all predecessors are move-object instructions for the same source register
                // TODO: do we need to do some sort of additional check that these multiple move-object predecessors actually refer to the same value?
                if (instructionIndex > 1) {
                    int originalSourceRegister = -1;

                    RegisterType newType = null;

                    for (AnalyzedInstruction prevPrevAnalyzedInstruction : prevInstruction.predecessors) {
                        Opcode opcode = prevPrevAnalyzedInstruction.instruction.getOpcode();
                        if (opcode == Opcode.MOVE_OBJECT || opcode == Opcode.MOVE_OBJECT_16 ||
                                opcode == Opcode.MOVE_OBJECT_FROM16) {
                            TwoRegisterInstruction moveInstruction =
                                    ((TwoRegisterInstruction)prevPrevAnalyzedInstruction.instruction);
                            RegisterType originalType =
                                    prevPrevAnalyzedInstruction.getPostInstructionRegisterType(
                                            moveInstruction.getRegisterB());
                            if (moveInstruction.getRegisterA() != instanceOfInstruction.getRegisterB()) {
                                originalSourceRegister = -1;
                                break;
                            }
                            if (originalType.type == null) {
                                originalSourceRegister = -1;
                                break;
                            }

                            if (newType == null) {
                                newType = RegisterType.getRegisterType(methodAnalyzer.getClassPath(),
                                        (TypeReference)instanceOfInstruction.getReference());
                            }

                            if (MethodAnalyzer.isNotWideningConversion(originalType, newType)) {
                                if (originalSourceRegister != -1) {
                                    if (originalSourceRegister != moveInstruction.getRegisterB()) {
                                        originalSourceRegister = -1;
                                        break;
                                    }
                                } else {
                                    originalSourceRegister = moveInstruction.getRegisterB();
                                }
                            }
                        } else {
                            originalSourceRegister = -1;
                            break;
                        }
                    }
                    if (originalSourceRegister != -1) {
                        setRegisters.add(originalSourceRegister);
                    }
                }
            }
        }

        return setRegisters;
    }

    public int getDestinationRegister() {
        if (!this.instruction.getOpcode().setsRegister()) {
            throw new ExceptionWithContext("Cannot call getDestinationRegister() for an instruction that doesn't " +
                    "store a value");
        }
        return ((OneRegisterInstruction)instruction).getRegisterA();
    }

    public int getRegisterCount() {
        return postRegisterMap.length;
    }

    @Nonnull
    public RegisterType getPostInstructionRegisterType(int registerNumber) {
        return postRegisterMap[registerNumber];
    }

    @Nonnull
    public RegisterType getPreInstructionRegisterType(int registerNumber) {
        return preRegisterMap[registerNumber];
    }

    public int compareTo(@Nonnull AnalyzedInstruction analyzedInstruction) {
        if (instructionIndex < analyzedInstruction.instructionIndex) {
            return -1;
        } else if (instructionIndex == analyzedInstruction.instructionIndex) {
            return 0;
        } else {
            return 1;
        }
    }

    private static class PredecessorOverrideKey {
        public final AnalyzedInstruction analyzedInstruction;
        public final int registerNumber;

        public PredecessorOverrideKey(AnalyzedInstruction analyzedInstruction, int registerNumber) {
            this.analyzedInstruction = analyzedInstruction;
            this.registerNumber = registerNumber;
        }

        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PredecessorOverrideKey that = (PredecessorOverrideKey)o;
            return com.google.common.base.Objects.equal(registerNumber, that.registerNumber) &&
                    Objects.equal(analyzedInstruction, that.analyzedInstruction);
        }

        @Override public int hashCode() {
            return Objects.hashCode(analyzedInstruction, registerNumber);
        }
    }
}

