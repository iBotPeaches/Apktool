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

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.jf.dexlib2.AccessFlags;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.iface.*;
import org.jf.dexlib2.iface.instruction.*;
import org.jf.dexlib2.iface.instruction.formats.*;
import org.jf.dexlib2.iface.reference.FieldReference;
import org.jf.dexlib2.iface.reference.MethodReference;
import org.jf.dexlib2.iface.reference.Reference;
import org.jf.dexlib2.iface.reference.TypeReference;
import org.jf.dexlib2.immutable.instruction.*;
import org.jf.dexlib2.immutable.reference.ImmutableFieldReference;
import org.jf.dexlib2.immutable.reference.ImmutableMethodReference;
import org.jf.dexlib2.util.MethodUtil;
import org.jf.dexlib2.util.ReferenceUtil;
import org.jf.dexlib2.util.TypeUtils;
import org.jf.util.BitSetUtils;
import org.jf.util.ExceptionWithContext;
import org.jf.util.SparseArray;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.BitSet;
import java.util.List;

/**
 * The MethodAnalyzer performs several functions. It "analyzes" the instructions and infers the register types
 * for each register, it can deodex odexed instructions, and it can verify the bytecode. The analysis and verification
 * are done in two separate passes, because the analysis has to process instructions multiple times in some cases, and
 * there's no need to perform the verification multiple times, so we wait until the method is fully analyzed and then
 * verify it.
 *
 * Before calling the analyze() method, you must have initialized the ClassPath by calling
 * ClassPath.InitializeClassPath
 */
public class MethodAnalyzer {
    @Nonnull private final Method method;
    @Nonnull private final MethodImplementation methodImpl;

    private final int paramRegisterCount;

    @Nonnull private final ClassPath classPath;
    @Nullable private final InlineMethodResolver inlineResolver;

    // This contains all the AnalyzedInstruction instances, keyed by the code unit address of the instruction
    @Nonnull private final SparseArray<AnalyzedInstruction> analyzedInstructions =
            new SparseArray<AnalyzedInstruction>(0);

    // Which instructions have been analyzed, keyed by instruction index
    @Nonnull private final BitSet analyzedState;

    @Nullable private AnalysisException analysisException = null;

    //This is a dummy instruction that occurs immediately before the first real instruction. We can initialize the
    //register types for this instruction to the parameter types, in order to have them propagate to all of its
    //successors, e.g. the first real instruction, the first instructions in any exception handlers covering the first
    //instruction, etc.
    private final AnalyzedInstruction startOfMethod;

    public MethodAnalyzer(@Nonnull ClassPath classPath, @Nonnull Method method,
                          @Nullable InlineMethodResolver inlineResolver) {
        this.classPath = classPath;
        this.inlineResolver = inlineResolver;

        this.method = method;

        MethodImplementation methodImpl = method.getImplementation();
        if (methodImpl == null) {
            throw new IllegalArgumentException("The method has no implementation");
        }

        this.methodImpl = methodImpl;

        //override AnalyzedInstruction and provide custom implementations of some of the methods, so that we don't
        //have to handle the case this special case of instruction being null, in the main class
        startOfMethod = new AnalyzedInstruction(null, -1, methodImpl.getRegisterCount()) {
            public boolean setsRegister() {
                return false;
            }

            @Override
            public boolean setsWideRegister() {
                return false;
            }

            @Override
            public boolean setsRegister(int registerNumber) {
                return false;
            }

            @Override
            public int getDestinationRegister() {
                assert false;
                return -1;
            }
        };

        buildInstructionList();

        analyzedState = new BitSet(analyzedInstructions.size());
        paramRegisterCount = MethodUtil.getParameterRegisterCount(method);
        analyze();
    }

    private void analyze() {
        Method method = this.method;
        MethodImplementation methodImpl = this.methodImpl;

        int totalRegisters = methodImpl.getRegisterCount();
        int parameterRegisters = paramRegisterCount;

        int nonParameterRegisters = totalRegisters - parameterRegisters;

        //if this isn't a static method, determine which register is the "this" register and set the type to the
        //current class
        if (!MethodUtil.isStatic(method)) {
            int thisRegister = totalRegisters - parameterRegisters;

            //if this is a constructor, then set the "this" register to an uninitialized reference of the current class
            if (MethodUtil.isConstructor(method)) {
                setPostRegisterTypeAndPropagateChanges(startOfMethod, thisRegister,
                        RegisterType.getRegisterType(RegisterType.UNINIT_THIS,
                                classPath.getClass(method.getDefiningClass())));
            } else {
                setPostRegisterTypeAndPropagateChanges(startOfMethod, thisRegister,
                        RegisterType.getRegisterType(RegisterType.REFERENCE,
                                classPath.getClass(method.getDefiningClass())));
            }

            propagateParameterTypes(totalRegisters-parameterRegisters+1);
        } else {
            propagateParameterTypes(totalRegisters-parameterRegisters);
        }

        RegisterType uninit = RegisterType.getRegisterType(RegisterType.UNINIT, null);
        for (int i=0; i<nonParameterRegisters; i++) {
            setPostRegisterTypeAndPropagateChanges(startOfMethod, i, uninit);
        }

        BitSet instructionsToAnalyze = new BitSet(analyzedInstructions.size());

        //make sure all of the "first instructions" are marked for processing
        for (AnalyzedInstruction successor: startOfMethod.successors) {
            instructionsToAnalyze.set(successor.instructionIndex);
        }

        BitSet undeodexedInstructions = new BitSet(analyzedInstructions.size());

        do {
            boolean didSomething = false;

            while (!instructionsToAnalyze.isEmpty()) {
                for(int i=instructionsToAnalyze.nextSetBit(0); i>=0; i=instructionsToAnalyze.nextSetBit(i+1)) {
                    instructionsToAnalyze.clear(i);
                    if (analyzedState.get(i)) {
                        continue;
                    }
                    AnalyzedInstruction instructionToAnalyze = analyzedInstructions.valueAt(i);
                    try {
                        if (instructionToAnalyze.originalInstruction.getOpcode().odexOnly()) {
                            //if we had deodexed an odex instruction in a previous pass, we might have more specific
                            //register information now, so let's restore the original odexed instruction and
                            //re-deodex it
                            instructionToAnalyze.restoreOdexedInstruction();
                        }

                        if (!analyzeInstruction(instructionToAnalyze)) {
                            undeodexedInstructions.set(i);
                            continue;
                        } else {
                            didSomething = true;
                            undeodexedInstructions.clear(i);
                        }
                    } catch (AnalysisException ex) {
                        this.analysisException = ex;
                        int codeAddress = getInstructionAddress(instructionToAnalyze);
                        ex.codeAddress = codeAddress;
                        ex.addContext(String.format("opcode: %s", instructionToAnalyze.instruction.getOpcode().name));
                        ex.addContext(String.format("code address: %d", codeAddress));
                        ex.addContext(String.format("method: %s", ReferenceUtil.getReferenceString(method)));
                        break;
                    }

                    analyzedState.set(instructionToAnalyze.getInstructionIndex());

                    for (AnalyzedInstruction successor: instructionToAnalyze.successors) {
                        instructionsToAnalyze.set(successor.getInstructionIndex());
                    }
                }
                if (analysisException != null) {
                    break;
                }
            }

            if (!didSomething) {
                break;
            }

            if (!undeodexedInstructions.isEmpty()) {
                for (int i=undeodexedInstructions.nextSetBit(0); i>=0; i=undeodexedInstructions.nextSetBit(i+1)) {
                    instructionsToAnalyze.set(i);
                }
            }
        } while (true);

        //Now, go through and fix up any unresolvable odex instructions. These are usually odex instructions
        //that operate on a null register, and thus always throw an NPE. They can also be any sort of odex instruction
        //that occurs after an unresolvable odex instruction. We deodex if possible, or replace with an
        //UnresolvableOdexInstruction
        for (int i=0; i< analyzedInstructions.size(); i++) {
            AnalyzedInstruction analyzedInstruction = analyzedInstructions.valueAt(i);

            Instruction instruction = analyzedInstruction.getInstruction();

            if (instruction.getOpcode().odexOnly()) {
                int objectRegisterNumber;
                switch (instruction.getOpcode().format) {
                    case Format10x:
                        analyzeReturnVoidBarrier(analyzedInstruction, false);
                        continue;
                    case Format21c:
                    case Format22c:
                        analyzePutGetVolatile(analyzedInstruction, false);
                        continue;
                    case Format35c:
                        analyzeInvokeDirectEmpty(analyzedInstruction, false);
                        continue;
                    case Format3rc:
                        analyzeInvokeObjectInitRange(analyzedInstruction, false);
                        continue;
                    case Format22cs:
                        objectRegisterNumber = ((Instruction22cs)instruction).getRegisterB();
                        break;
                    case Format35mi:
                    case Format35ms:
                        objectRegisterNumber = ((FiveRegisterInstruction)instruction).getRegisterC();
                        break;
                    case Format3rmi:
                    case Format3rms:
                        objectRegisterNumber = ((RegisterRangeInstruction)instruction).getStartRegister();
                        break;
                    default:
                        continue;
                }

                analyzedInstruction.setDeodexedInstruction(
                        new UnresolvedOdexInstruction(instruction, objectRegisterNumber));
            }
        }
    }

    private void propagateParameterTypes(int parameterStartRegister) {
        int i=0;
        for (MethodParameter parameter: method.getParameters()) {
            if (TypeUtils.isWideType(parameter)) {
                setPostRegisterTypeAndPropagateChanges(startOfMethod, parameterStartRegister + i++,
                        RegisterType.getWideRegisterType(parameter, true));
                setPostRegisterTypeAndPropagateChanges(startOfMethod, parameterStartRegister + i++,
                        RegisterType.getWideRegisterType(parameter, false));
            } else {
                setPostRegisterTypeAndPropagateChanges(startOfMethod, parameterStartRegister + i++,
                        RegisterType.getRegisterType(classPath, parameter));
            }
        }
    }

    public List<AnalyzedInstruction> getAnalyzedInstructions() {
        return analyzedInstructions.getValues();
    }

    public List<Instruction> getInstructions() {
        return Lists.transform(analyzedInstructions.getValues(), new Function<AnalyzedInstruction, Instruction>() {
            @Nullable @Override public Instruction apply(@Nullable AnalyzedInstruction input) {
                if (input == null) {
                    return null;
                }
                return input.instruction;
            }
        });
    }

    @Nullable
    public AnalysisException getAnalysisException() {
        return analysisException;
    }

    public int getParamRegisterCount() {
        return paramRegisterCount;
    }

    public int getInstructionAddress(@Nonnull AnalyzedInstruction instruction) {
        return analyzedInstructions.keyAt(instruction.instructionIndex);
    }

    private void setDestinationRegisterTypeAndPropagateChanges(@Nonnull AnalyzedInstruction analyzedInstruction,
                                                               @Nonnull RegisterType registerType) {
        setPostRegisterTypeAndPropagateChanges(analyzedInstruction, analyzedInstruction.getDestinationRegister(),
                registerType);
    }

    private void setPostRegisterTypeAndPropagateChanges(@Nonnull AnalyzedInstruction analyzedInstruction,
                                                        int registerNumber, @Nonnull RegisterType registerType) {

        BitSet changedInstructions = new BitSet(analyzedInstructions.size());

        if (!analyzedInstruction.setPostRegisterType(registerNumber, registerType)) {
            return;
        }

        propagateRegisterToSuccessors(analyzedInstruction, registerNumber, changedInstructions);

        //Using a for loop inside the while loop optimizes for the common case of the successors of an instruction
        //occurring after the instruction. Any successors that occur prior to the instruction will be picked up on
        //the next iteration of the while loop.
        //This could also be done recursively, but in large methods it would likely cause very deep recursion,
        //which requires the user to specify a larger stack size. This isn't really a problem, but it is slightly
        //annoying.
        while (!changedInstructions.isEmpty()) {
            for (int instructionIndex=changedInstructions.nextSetBit(0);
                     instructionIndex>=0;
                     instructionIndex=changedInstructions.nextSetBit(instructionIndex+1)) {

                changedInstructions.clear(instructionIndex);

                propagateRegisterToSuccessors(analyzedInstructions.valueAt(instructionIndex), registerNumber,
                        changedInstructions);
            }
        }

        if (registerType.category == RegisterType.LONG_LO) {
            checkWidePair(registerNumber, analyzedInstruction);
            setPostRegisterTypeAndPropagateChanges(analyzedInstruction, registerNumber+1, RegisterType.LONG_HI_TYPE);
        } else if (registerType.category == RegisterType.DOUBLE_LO) {
            checkWidePair(registerNumber, analyzedInstruction);
            setPostRegisterTypeAndPropagateChanges(analyzedInstruction, registerNumber+1, RegisterType.DOUBLE_HI_TYPE);
        }
    }

    private void propagateRegisterToSuccessors(@Nonnull AnalyzedInstruction instruction, int registerNumber,
                                               @Nonnull BitSet changedInstructions) {
        RegisterType postRegisterType = instruction.getPostInstructionRegisterType(registerNumber);
        for (AnalyzedInstruction successor: instruction.successors) {
            if (successor.mergeRegister(registerNumber, postRegisterType, analyzedState)) {
                changedInstructions.set(successor.instructionIndex);
            }
        }
    }

    private void buildInstructionList() {
        int registerCount = methodImpl.getRegisterCount();

        ImmutableList<Instruction> instructions = ImmutableList.copyOf(methodImpl.getInstructions());

        analyzedInstructions.ensureCapacity(instructions.size());

        //first, create all the instructions and populate the instructionAddresses array
        int currentCodeAddress = 0;
        for (int i=0; i<instructions.size(); i++) {
            Instruction instruction = instructions.get(i);
            analyzedInstructions.append(currentCodeAddress, new AnalyzedInstruction(instruction, i, registerCount));
            assert analyzedInstructions.indexOfKey(currentCodeAddress) == i;
            currentCodeAddress += instruction.getCodeUnits();
        }

        //next, populate the exceptionHandlers array. The array item for each instruction that can throw an exception
        //and is covered by a try block should be set to a list of the first instructions of each exception handler
        //for the try block covering the instruction
        List<? extends TryBlock<? extends ExceptionHandler>> tries = methodImpl.getTryBlocks();
        int triesIndex = 0;
        TryBlock currentTry = null;
        AnalyzedInstruction[] currentExceptionHandlers = null;
        AnalyzedInstruction[][] exceptionHandlers = new AnalyzedInstruction[instructions.size()][];

        if (tries != null) {
            for (int i=0; i< analyzedInstructions.size(); i++) {
                AnalyzedInstruction instruction = analyzedInstructions.valueAt(i);
                Opcode instructionOpcode = instruction.instruction.getOpcode();
                currentCodeAddress = getInstructionAddress(instruction);

                //check if we have gone past the end of the current try
                if (currentTry != null) {
                    if (currentTry.getStartCodeAddress() + currentTry.getCodeUnitCount()  <= currentCodeAddress) {
                        currentTry = null;
                        triesIndex++;
                    }
                }

                //check if the next try is applicable yet
                if (currentTry == null && triesIndex < tries.size()) {
                    TryBlock<? extends ExceptionHandler> tryBlock = tries.get(triesIndex);
                    if (tryBlock.getStartCodeAddress() <= currentCodeAddress) {
                        assert(tryBlock.getStartCodeAddress() + tryBlock.getCodeUnitCount() > currentCodeAddress);

                        currentTry = tryBlock;

                        currentExceptionHandlers = buildExceptionHandlerArray(tryBlock);
                    }
                }

                //if we're inside a try block, and the instruction can throw an exception, then add the exception handlers
                //for the current instruction
                if (currentTry != null && instructionOpcode.canThrow()) {
                    exceptionHandlers[i] = currentExceptionHandlers;
                }
            }
        }

        //finally, populate the successors and predecessors for each instruction. We start at the fake "StartOfMethod"
        //instruction and follow the execution path. Any unreachable code won't have any predecessors or successors,
        //and no reachable code will have an unreachable predessor or successor
        assert analyzedInstructions.size() > 0;
        BitSet instructionsToProcess = new BitSet(instructions.size());

        addPredecessorSuccessor(startOfMethod, analyzedInstructions.valueAt(0), exceptionHandlers, instructionsToProcess);
        while (!instructionsToProcess.isEmpty()) {
            int currentInstructionIndex = instructionsToProcess.nextSetBit(0);
            instructionsToProcess.clear(currentInstructionIndex);

            AnalyzedInstruction instruction = analyzedInstructions.valueAt(currentInstructionIndex);
            Opcode instructionOpcode = instruction.instruction.getOpcode();
            int instructionCodeAddress = getInstructionAddress(instruction);

            if (instruction.instruction.getOpcode().canContinue()) {
                if (currentInstructionIndex == analyzedInstructions.size() - 1) {
                    throw new AnalysisException("Execution can continue past the last instruction");
                }

                AnalyzedInstruction nextInstruction = analyzedInstructions.valueAt(currentInstructionIndex+1);
                addPredecessorSuccessor(instruction, nextInstruction, exceptionHandlers, instructionsToProcess);
            }

            if (instruction.instruction instanceof OffsetInstruction) {
                OffsetInstruction offsetInstruction = (OffsetInstruction)instruction.instruction;

                if (instructionOpcode == Opcode.PACKED_SWITCH || instructionOpcode == Opcode.SPARSE_SWITCH) {
                    SwitchPayload switchPayload = (SwitchPayload)analyzedInstructions.get(instructionCodeAddress +
                                    offsetInstruction.getCodeOffset()).instruction;
                    for (SwitchElement switchElement: switchPayload.getSwitchElements()) {
                        AnalyzedInstruction targetInstruction = analyzedInstructions.get(instructionCodeAddress +
                                switchElement.getOffset());

                        addPredecessorSuccessor(instruction, targetInstruction, exceptionHandlers,
                                instructionsToProcess);
                    }
                } else if (instructionOpcode != Opcode.FILL_ARRAY_DATA) {
                    int targetAddressOffset = offsetInstruction.getCodeOffset();
                    AnalyzedInstruction targetInstruction = analyzedInstructions.get(instructionCodeAddress +
                            targetAddressOffset);
                    addPredecessorSuccessor(instruction, targetInstruction, exceptionHandlers, instructionsToProcess);
                }
            }
        }
    }

    private void addPredecessorSuccessor(@Nonnull AnalyzedInstruction predecessor,
                                         @Nonnull AnalyzedInstruction successor,
                                         @Nonnull AnalyzedInstruction[][] exceptionHandlers,
                                         @Nonnull BitSet instructionsToProcess) {
        addPredecessorSuccessor(predecessor, successor, exceptionHandlers, instructionsToProcess, false);
    }

    private void addPredecessorSuccessor(@Nonnull AnalyzedInstruction predecessor,
                                         @Nonnull AnalyzedInstruction successor,
                                         @Nonnull AnalyzedInstruction[][] exceptionHandlers,
                                         @Nonnull BitSet instructionsToProcess, boolean allowMoveException) {

        if (!allowMoveException && successor.instruction.getOpcode() == Opcode.MOVE_EXCEPTION) {
            throw new AnalysisException("Execution can pass from the " + predecessor.instruction.getOpcode().name +
                    " instruction at code address 0x" + Integer.toHexString(getInstructionAddress(predecessor)) +
                    " to the move-exception instruction at address 0x" +
                    Integer.toHexString(getInstructionAddress(successor)));
        }

        if (!successor.addPredecessor(predecessor)) {
            return;
        }

        predecessor.addSuccessor(successor);
        instructionsToProcess.set(successor.getInstructionIndex());


        //if the successor can throw an instruction, then we need to add the exception handlers as additional
        //successors to the predecessor (and then apply this same logic recursively if needed)
        //Technically, we should handle the monitor-exit instruction as a special case. The exception is actually
        //thrown *after* the instruction executes, instead of "before" the instruction executes, lke for any other
        //instruction. But since it doesn't modify any registers, we can treat it like any other instruction.
        AnalyzedInstruction[] exceptionHandlersForSuccessor = exceptionHandlers[successor.instructionIndex];
        if (exceptionHandlersForSuccessor != null) {
            //the item for this instruction in exceptionHandlersForSuccessor should only be set if this instruction
            //can throw an exception
            assert successor.instruction.getOpcode().canThrow();

            for (AnalyzedInstruction exceptionHandler: exceptionHandlersForSuccessor) {
                addPredecessorSuccessor(predecessor, exceptionHandler, exceptionHandlers, instructionsToProcess, true);
            }
        }
    }

    @Nonnull
    private AnalyzedInstruction[] buildExceptionHandlerArray(@Nonnull TryBlock<? extends ExceptionHandler> tryBlock) {
        List<? extends ExceptionHandler> exceptionHandlers = tryBlock.getExceptionHandlers();

        AnalyzedInstruction[] handlerInstructions = new AnalyzedInstruction[exceptionHandlers.size()];
        for (int i=0; i<exceptionHandlers.size(); i++) {
            handlerInstructions[i] = analyzedInstructions.get(exceptionHandlers.get(i).getHandlerCodeAddress());
        }

        return handlerInstructions;
    }

    /**
     * @return false if analyzedInstruction is an odex instruction that couldn't be deodexed, due to its
     * object register being null
     */
    private boolean analyzeInstruction(@Nonnull AnalyzedInstruction analyzedInstruction) {
        Instruction instruction = analyzedInstruction.instruction;

        switch (instruction.getOpcode()) {
            case NOP:
                return true;
            case MOVE:
            case MOVE_FROM16:
            case MOVE_16:
            case MOVE_WIDE:
            case MOVE_WIDE_FROM16:
            case MOVE_WIDE_16:
            case MOVE_OBJECT:
            case MOVE_OBJECT_FROM16:
            case MOVE_OBJECT_16:
                analyzeMove(analyzedInstruction);
                return true;
            case MOVE_RESULT:
            case MOVE_RESULT_WIDE:
            case MOVE_RESULT_OBJECT:
                analyzeMoveResult(analyzedInstruction);
                return true;
            case MOVE_EXCEPTION:
                analyzeMoveException(analyzedInstruction);
                return true;
            case RETURN_VOID:
            case RETURN:
            case RETURN_WIDE:
            case RETURN_OBJECT:
                return true;
            case RETURN_VOID_BARRIER:
                analyzeReturnVoidBarrier(analyzedInstruction);
                return true;
            case CONST_4:
            case CONST_16:
            case CONST:
            case CONST_HIGH16:
                analyzeConst(analyzedInstruction);
                return true;
            case CONST_WIDE_16:
            case CONST_WIDE_32:
            case CONST_WIDE:
            case CONST_WIDE_HIGH16:
                analyzeWideConst(analyzedInstruction);
                return true;
            case CONST_STRING:
            case CONST_STRING_JUMBO:
                analyzeConstString(analyzedInstruction);
                return true;
            case CONST_CLASS:
                analyzeConstClass(analyzedInstruction);
                return true;
            case MONITOR_ENTER:
            case MONITOR_EXIT:
                return true;
            case CHECK_CAST:
                analyzeCheckCast(analyzedInstruction);
                return true;
            case INSTANCE_OF:
                analyzeInstanceOf(analyzedInstruction);
                return true;
            case ARRAY_LENGTH:
                analyzeArrayLength(analyzedInstruction);
                return true;
            case NEW_INSTANCE:
                analyzeNewInstance(analyzedInstruction);
                return true;
            case NEW_ARRAY:
                analyzeNewArray(analyzedInstruction);
                return true;
            case FILLED_NEW_ARRAY:
            case FILLED_NEW_ARRAY_RANGE:
                return true;
            case FILL_ARRAY_DATA:
                return true;
            case THROW:
            case GOTO:
            case GOTO_16:
            case GOTO_32:
                return true;
            case PACKED_SWITCH:
            case SPARSE_SWITCH:
                return true;
            case CMPL_FLOAT:
            case CMPG_FLOAT:
            case CMPL_DOUBLE:
            case CMPG_DOUBLE:
            case CMP_LONG:
                analyzeFloatWideCmp(analyzedInstruction);
                return true;
            case IF_EQ:
            case IF_NE:
            case IF_LT:
            case IF_GE:
            case IF_GT:
            case IF_LE:
            case IF_EQZ:
            case IF_NEZ:
            case IF_LTZ:
            case IF_GEZ:
            case IF_GTZ:
            case IF_LEZ:
                return true;
            case AGET:
                analyze32BitPrimitiveAget(analyzedInstruction, RegisterType.INTEGER_TYPE);
                return true;
            case AGET_BOOLEAN:
                analyze32BitPrimitiveAget(analyzedInstruction, RegisterType.BOOLEAN_TYPE);
                return true;
            case AGET_BYTE:
                analyze32BitPrimitiveAget(analyzedInstruction, RegisterType.BYTE_TYPE);
                return true;
            case AGET_CHAR:
                analyze32BitPrimitiveAget(analyzedInstruction, RegisterType.CHAR_TYPE);
                return true;
            case AGET_SHORT:
                analyze32BitPrimitiveAget(analyzedInstruction, RegisterType.SHORT_TYPE);
                return true;
            case AGET_WIDE:
                analyzeAgetWide(analyzedInstruction);
                return true;
            case AGET_OBJECT:
                analyzeAgetObject(analyzedInstruction);
                return true;
            case APUT:
            case APUT_BOOLEAN:
            case APUT_BYTE:
            case APUT_CHAR:
            case APUT_SHORT:
            case APUT_WIDE:
            case APUT_OBJECT:
                return true;
            case IGET:
                analyze32BitPrimitiveIgetSget(analyzedInstruction, RegisterType.INTEGER_TYPE);
                return true;
            case IGET_BOOLEAN:
                analyze32BitPrimitiveIgetSget(analyzedInstruction, RegisterType.BOOLEAN_TYPE);
                return true;
            case IGET_BYTE:
                analyze32BitPrimitiveIgetSget(analyzedInstruction, RegisterType.BYTE_TYPE);
                return true;
            case IGET_CHAR:
                analyze32BitPrimitiveIgetSget(analyzedInstruction, RegisterType.CHAR_TYPE);
                return true;
            case IGET_SHORT:
                analyze32BitPrimitiveIgetSget(analyzedInstruction, RegisterType.SHORT_TYPE);
                return true;
            case IGET_WIDE:
            case IGET_OBJECT:
                analyzeIgetSgetWideObject(analyzedInstruction);
                return true;
            case IPUT:
            case IPUT_BOOLEAN:
            case IPUT_BYTE:
            case IPUT_CHAR:
            case IPUT_SHORT:
            case IPUT_WIDE:
            case IPUT_OBJECT:
                return true;
            case SGET:
                analyze32BitPrimitiveIgetSget(analyzedInstruction, RegisterType.INTEGER_TYPE);
                return true;
            case SGET_BOOLEAN:
                analyze32BitPrimitiveIgetSget(analyzedInstruction, RegisterType.BOOLEAN_TYPE);
                return true;
            case SGET_BYTE:
                analyze32BitPrimitiveIgetSget(analyzedInstruction, RegisterType.BYTE_TYPE);
                return true;
            case SGET_CHAR:
                analyze32BitPrimitiveIgetSget(analyzedInstruction, RegisterType.CHAR_TYPE);
                return true;
            case SGET_SHORT:
                analyze32BitPrimitiveIgetSget(analyzedInstruction, RegisterType.SHORT_TYPE);
                return true;
            case SGET_WIDE:
            case SGET_OBJECT:
                analyzeIgetSgetWideObject(analyzedInstruction);
                return true;
            case SPUT:
            case SPUT_BOOLEAN:
            case SPUT_BYTE:
            case SPUT_CHAR:
            case SPUT_SHORT:
            case SPUT_WIDE:
            case SPUT_OBJECT:
                return true;
            case INVOKE_VIRTUAL:
            case INVOKE_SUPER:
                return true;
            case INVOKE_DIRECT:
                analyzeInvokeDirect(analyzedInstruction);
                return true;
            case INVOKE_STATIC:
            case INVOKE_INTERFACE:
            case INVOKE_VIRTUAL_RANGE:
            case INVOKE_SUPER_RANGE:
                return true;
            case INVOKE_DIRECT_RANGE:
                analyzeInvokeDirectRange(analyzedInstruction);
                return true;
            case INVOKE_STATIC_RANGE:
            case INVOKE_INTERFACE_RANGE:
                return true;
            case NEG_INT:
            case NOT_INT:
                analyzeUnaryOp(analyzedInstruction, RegisterType.INTEGER_TYPE);
                return true;
            case NEG_LONG:
            case NOT_LONG:
                analyzeUnaryOp(analyzedInstruction, RegisterType.LONG_LO_TYPE);
                return true;
            case NEG_FLOAT:
                analyzeUnaryOp(analyzedInstruction, RegisterType.FLOAT_TYPE);
                return true;
            case NEG_DOUBLE:
                analyzeUnaryOp(analyzedInstruction, RegisterType.DOUBLE_LO_TYPE);
                return true;
            case INT_TO_LONG:
                analyzeUnaryOp(analyzedInstruction, RegisterType.LONG_LO_TYPE);
                return true;
            case INT_TO_FLOAT:
                analyzeUnaryOp(analyzedInstruction, RegisterType.FLOAT_TYPE);
                return true;
            case INT_TO_DOUBLE:
                analyzeUnaryOp(analyzedInstruction, RegisterType.DOUBLE_LO_TYPE);
                return true;
            case LONG_TO_INT:
            case DOUBLE_TO_INT:
                analyzeUnaryOp(analyzedInstruction, RegisterType.INTEGER_TYPE);
                return true;
            case LONG_TO_FLOAT:
            case DOUBLE_TO_FLOAT:
                analyzeUnaryOp(analyzedInstruction, RegisterType.FLOAT_TYPE);
                return true;
            case LONG_TO_DOUBLE:
                analyzeUnaryOp(analyzedInstruction, RegisterType.DOUBLE_LO_TYPE);
                return true;
            case FLOAT_TO_INT:
                analyzeUnaryOp(analyzedInstruction, RegisterType.INTEGER_TYPE);
                return true;
            case FLOAT_TO_LONG:
                analyzeUnaryOp(analyzedInstruction, RegisterType.LONG_LO_TYPE);
                return true;
            case FLOAT_TO_DOUBLE:
                analyzeUnaryOp(analyzedInstruction, RegisterType.DOUBLE_LO_TYPE);
                return true;
            case DOUBLE_TO_LONG:
                analyzeUnaryOp(analyzedInstruction, RegisterType.LONG_LO_TYPE);
                return true;
            case INT_TO_BYTE:
                analyzeUnaryOp(analyzedInstruction, RegisterType.BYTE_TYPE);
                return true;
            case INT_TO_CHAR:
                analyzeUnaryOp(analyzedInstruction, RegisterType.CHAR_TYPE);
                return true;
            case INT_TO_SHORT:
                analyzeUnaryOp(analyzedInstruction, RegisterType.SHORT_TYPE);
                return true;
            case ADD_INT:
            case SUB_INT:
            case MUL_INT:
            case DIV_INT:
            case REM_INT:
            case SHL_INT:
            case SHR_INT:
            case USHR_INT:
                analyzeBinaryOp(analyzedInstruction, RegisterType.INTEGER_TYPE, false);
                return true;
            case AND_INT:
            case OR_INT:
            case XOR_INT:
                analyzeBinaryOp(analyzedInstruction, RegisterType.INTEGER_TYPE, true);
                return true;
            case ADD_LONG:
            case SUB_LONG:
            case MUL_LONG:
            case DIV_LONG:
            case REM_LONG:
            case AND_LONG:
            case OR_LONG:
            case XOR_LONG:
            case SHL_LONG:
            case SHR_LONG:
            case USHR_LONG:
                analyzeBinaryOp(analyzedInstruction, RegisterType.LONG_LO_TYPE, false);
                return true;
            case ADD_FLOAT:
            case SUB_FLOAT:
            case MUL_FLOAT:
            case DIV_FLOAT:
            case REM_FLOAT:
                analyzeBinaryOp(analyzedInstruction, RegisterType.FLOAT_TYPE, false);
                return true;
            case ADD_DOUBLE:
            case SUB_DOUBLE:
            case MUL_DOUBLE:
            case DIV_DOUBLE:
            case REM_DOUBLE:
                analyzeBinaryOp(analyzedInstruction, RegisterType.DOUBLE_LO_TYPE, false);
                return true;
            case ADD_INT_2ADDR:
            case SUB_INT_2ADDR:
            case MUL_INT_2ADDR:
            case DIV_INT_2ADDR:
            case REM_INT_2ADDR:
            case SHL_INT_2ADDR:
            case SHR_INT_2ADDR:
            case USHR_INT_2ADDR:
                analyzeBinary2AddrOp(analyzedInstruction, RegisterType.INTEGER_TYPE, false);
                return true;
            case AND_INT_2ADDR:
            case OR_INT_2ADDR:
            case XOR_INT_2ADDR:
                analyzeBinary2AddrOp(analyzedInstruction, RegisterType.INTEGER_TYPE, true);
                return true;
            case ADD_LONG_2ADDR:
            case SUB_LONG_2ADDR:
            case MUL_LONG_2ADDR:
            case DIV_LONG_2ADDR:
            case REM_LONG_2ADDR:
            case AND_LONG_2ADDR:
            case OR_LONG_2ADDR:
            case XOR_LONG_2ADDR:
            case SHL_LONG_2ADDR:
            case SHR_LONG_2ADDR:
            case USHR_LONG_2ADDR:
                analyzeBinary2AddrOp(analyzedInstruction, RegisterType.LONG_LO_TYPE, false);
                return true;
            case ADD_FLOAT_2ADDR:
            case SUB_FLOAT_2ADDR:
            case MUL_FLOAT_2ADDR:
            case DIV_FLOAT_2ADDR:
            case REM_FLOAT_2ADDR:
                analyzeBinary2AddrOp(analyzedInstruction, RegisterType.FLOAT_TYPE, false);
                return true;
            case ADD_DOUBLE_2ADDR:
            case SUB_DOUBLE_2ADDR:
            case MUL_DOUBLE_2ADDR:
            case DIV_DOUBLE_2ADDR:
            case REM_DOUBLE_2ADDR:
                analyzeBinary2AddrOp(analyzedInstruction, RegisterType.DOUBLE_LO_TYPE, false);
                return true;
            case ADD_INT_LIT16:
            case RSUB_INT:
            case MUL_INT_LIT16:
            case DIV_INT_LIT16:
            case REM_INT_LIT16:
                analyzeLiteralBinaryOp(analyzedInstruction, RegisterType.INTEGER_TYPE, false);
                return true;
            case AND_INT_LIT16:
            case OR_INT_LIT16:
            case XOR_INT_LIT16:
                analyzeLiteralBinaryOp(analyzedInstruction, RegisterType.INTEGER_TYPE, true);
                return true;
            case ADD_INT_LIT8:
            case RSUB_INT_LIT8:
            case MUL_INT_LIT8:
            case DIV_INT_LIT8:
            case REM_INT_LIT8:
            case SHL_INT_LIT8:
                analyzeLiteralBinaryOp(analyzedInstruction, RegisterType.INTEGER_TYPE, false);
                return true;
            case AND_INT_LIT8:
            case OR_INT_LIT8:
            case XOR_INT_LIT8:
                analyzeLiteralBinaryOp(analyzedInstruction, RegisterType.INTEGER_TYPE, true);
                return true;
            case SHR_INT_LIT8:
                analyzeLiteralBinaryOp(analyzedInstruction, getDestTypeForLiteralShiftRight(analyzedInstruction, true),
                        false);
                return true;
            case USHR_INT_LIT8:
                analyzeLiteralBinaryOp(analyzedInstruction, getDestTypeForLiteralShiftRight(analyzedInstruction, false),
                        false);
                return true;

            /*odexed instructions*/
            case IGET_VOLATILE:
            case IPUT_VOLATILE:
            case SGET_VOLATILE:
            case SPUT_VOLATILE:
            case IGET_OBJECT_VOLATILE:
            case IGET_WIDE_VOLATILE:
            case IPUT_WIDE_VOLATILE:
            case SGET_WIDE_VOLATILE:
            case SPUT_WIDE_VOLATILE:
                analyzePutGetVolatile(analyzedInstruction);
                return true;
            case THROW_VERIFICATION_ERROR:
                return true;
            case EXECUTE_INLINE:
                analyzeExecuteInline(analyzedInstruction);
                return true;
            case EXECUTE_INLINE_RANGE:
                analyzeExecuteInlineRange(analyzedInstruction);
                return true;
            case INVOKE_DIRECT_EMPTY:
                analyzeInvokeDirectEmpty(analyzedInstruction);
                return true;
            case INVOKE_OBJECT_INIT_RANGE:
                analyzeInvokeObjectInitRange(analyzedInstruction);
                return true;
            case IGET_QUICK:
            case IGET_WIDE_QUICK:
            case IGET_OBJECT_QUICK:
            case IPUT_QUICK:
            case IPUT_WIDE_QUICK:
            case IPUT_OBJECT_QUICK:
                return analyzeIputIgetQuick(analyzedInstruction);
            case INVOKE_VIRTUAL_QUICK:
                return analyzeInvokeVirtualQuick(analyzedInstruction, false, false);
            case INVOKE_SUPER_QUICK:
                return analyzeInvokeVirtualQuick(analyzedInstruction, true, false);
            case INVOKE_VIRTUAL_QUICK_RANGE:
                return analyzeInvokeVirtualQuick(analyzedInstruction, false, true);
            case INVOKE_SUPER_QUICK_RANGE:
                return analyzeInvokeVirtualQuick(analyzedInstruction, true, true);
            case IPUT_OBJECT_VOLATILE:
            case SGET_OBJECT_VOLATILE:
            case SPUT_OBJECT_VOLATILE:
                analyzePutGetVolatile(analyzedInstruction);
                return true;
            default:
                assert false;
                return true;
        }
    }

    private static final BitSet Primitive32BitCategories = BitSetUtils.bitSetOfIndexes(
            RegisterType.NULL,
            RegisterType.ONE,
            RegisterType.BOOLEAN,
            RegisterType.BYTE,
            RegisterType.POS_BYTE,
            RegisterType.SHORT,
            RegisterType.POS_SHORT,
            RegisterType.CHAR,
            RegisterType.INTEGER,
            RegisterType.FLOAT);

    private static final BitSet WideLowCategories = BitSetUtils.bitSetOfIndexes(
            RegisterType.LONG_LO,
            RegisterType.DOUBLE_LO);

    private static final BitSet WideHighCategories = BitSetUtils.bitSetOfIndexes(
            RegisterType.LONG_HI,
            RegisterType.DOUBLE_HI);

    private static final BitSet ReferenceOrUninitCategories = BitSetUtils.bitSetOfIndexes(
            RegisterType.NULL,
            RegisterType.UNINIT_REF,
            RegisterType.UNINIT_THIS,
            RegisterType.REFERENCE);

    private static final BitSet BooleanCategories = BitSetUtils.bitSetOfIndexes(
            RegisterType.NULL,
            RegisterType.ONE,
            RegisterType.BOOLEAN);

    private void analyzeMove(@Nonnull AnalyzedInstruction analyzedInstruction) {
        TwoRegisterInstruction instruction = (TwoRegisterInstruction)analyzedInstruction.instruction;

        RegisterType sourceRegisterType = analyzedInstruction.getPreInstructionRegisterType(instruction.getRegisterB());
        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction, sourceRegisterType);
    }

    private void analyzeMoveResult(@Nonnull AnalyzedInstruction analyzedInstruction) {
        AnalyzedInstruction previousInstruction = analyzedInstructions.valueAt(analyzedInstruction.instructionIndex-1);
        if (!previousInstruction.instruction.getOpcode().setsResult()) {
            throw new AnalysisException(analyzedInstruction.instruction.getOpcode().name + " must occur after an " +
                    "invoke-*/fill-new-array instruction");
        }

        RegisterType resultRegisterType;
        ReferenceInstruction invokeInstruction = (ReferenceInstruction)previousInstruction.instruction;
        Reference reference = invokeInstruction.getReference();

        if (reference instanceof MethodReference) {
            resultRegisterType = RegisterType.getRegisterType(classPath, ((MethodReference)reference).getReturnType());
        } else {
            resultRegisterType = RegisterType.getRegisterType(classPath, (TypeReference)reference);
        }

        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction, resultRegisterType);
    }

    private void analyzeMoveException(@Nonnull AnalyzedInstruction analyzedInstruction) {
        int instructionAddress = getInstructionAddress(analyzedInstruction);

        RegisterType exceptionType = RegisterType.UNKNOWN_TYPE;

        for (TryBlock<? extends ExceptionHandler> tryBlock: methodImpl.getTryBlocks()) {
            for (ExceptionHandler handler: tryBlock.getExceptionHandlers()) {

                if (handler.getHandlerCodeAddress() == instructionAddress) {
                    String type = handler.getExceptionType();
                    if (type == null) {
                        exceptionType = RegisterType.getRegisterType(RegisterType.REFERENCE,
                                classPath.getClass("Ljava/lang/Throwable;"));
                    } else {
                        exceptionType = RegisterType.getRegisterType(RegisterType.REFERENCE, classPath.getClass(type))
                                .merge(exceptionType);
                    }
                }
            }
        }

        if (exceptionType.category == RegisterType.UNKNOWN) {
            throw new AnalysisException("move-exception must be the first instruction in an exception handler block");
        }

        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction, exceptionType);
    }

    private void analyzeReturnVoidBarrier(AnalyzedInstruction analyzedInstruction) {
        analyzeReturnVoidBarrier(analyzedInstruction, true);
    }

    private void analyzeReturnVoidBarrier(@Nonnull AnalyzedInstruction analyzedInstruction, boolean analyzeResult) {
        Instruction10x deodexedInstruction = new ImmutableInstruction10x(Opcode.RETURN_VOID);

        analyzedInstruction.setDeodexedInstruction(deodexedInstruction);

        if (analyzeResult) {
            analyzeInstruction(analyzedInstruction);
        }
    }

    private void analyzeConst(@Nonnull AnalyzedInstruction analyzedInstruction) {
        NarrowLiteralInstruction instruction = (NarrowLiteralInstruction)analyzedInstruction.instruction;

        //we assume that the literal value is a valid value for the given instruction type, because it's impossible
        //to store an invalid literal with the instruction. so we don't need to check the type of the literal
        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction,
                RegisterType.getRegisterTypeForLiteral(instruction.getNarrowLiteral()));
    }

    private void analyzeWideConst(@Nonnull AnalyzedInstruction analyzedInstruction) {
        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction, RegisterType.LONG_LO_TYPE);
    }

    private void analyzeConstString(@Nonnull AnalyzedInstruction analyzedInstruction) {
        TypeProto stringClass = classPath.getClass("Ljava/lang/String;");
        RegisterType stringType = RegisterType.getRegisterType(RegisterType.REFERENCE, stringClass);
        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction, stringType);
    }

    private void analyzeConstClass(@Nonnull AnalyzedInstruction analyzedInstruction) {
        TypeProto classClass = classPath.getClass("Ljava/lang/Class;");
        RegisterType classType = RegisterType.getRegisterType(RegisterType.REFERENCE, classClass);
        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction, classType);
    }

    private void analyzeCheckCast(@Nonnull AnalyzedInstruction analyzedInstruction) {
        ReferenceInstruction instruction = (ReferenceInstruction)analyzedInstruction.instruction;
        TypeReference reference = (TypeReference)instruction.getReference();
        RegisterType castRegisterType = RegisterType.getRegisterType(classPath, reference);
        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction, castRegisterType);
    }

    private void analyzeInstanceOf(@Nonnull AnalyzedInstruction analyzedInstruction) {
        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction, RegisterType.BOOLEAN_TYPE);
    }

    private void analyzeArrayLength(@Nonnull AnalyzedInstruction analyzedInstruction) {
        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction, RegisterType.INTEGER_TYPE);
    }

    private void analyzeNewInstance(@Nonnull AnalyzedInstruction analyzedInstruction) {
        ReferenceInstruction instruction = (ReferenceInstruction)analyzedInstruction.instruction;

        int register = ((OneRegisterInstruction)analyzedInstruction.instruction).getRegisterA();
        RegisterType destRegisterType = analyzedInstruction.getPostInstructionRegisterType(register);
        if (destRegisterType.category != RegisterType.UNKNOWN) {
            //the post-instruction destination register will only be set if we have already analyzed this instruction
            //at least once. If this is the case, then the uninit reference has already been propagated to all
            //successors and nothing else needs to be done.
            assert destRegisterType.category == RegisterType.UNINIT_REF;
            return;
        }

        TypeReference typeReference = (TypeReference)instruction.getReference();

        RegisterType classType = RegisterType.getRegisterType(classPath, typeReference);

        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction,
                RegisterType.getRegisterType(RegisterType.UNINIT_REF, classType.type));
    }

    private void analyzeNewArray(@Nonnull AnalyzedInstruction analyzedInstruction) {
        ReferenceInstruction instruction = (ReferenceInstruction)analyzedInstruction.instruction;

        TypeReference type = (TypeReference)instruction.getReference();
        if (type.getType().charAt(0) != '[') {
            throw new AnalysisException("new-array used with non-array type");
        }

        RegisterType arrayType = RegisterType.getRegisterType(classPath, type);

        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction, arrayType);
    }

    private void analyzeFloatWideCmp(@Nonnull AnalyzedInstruction analyzedInstruction) {
        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction, RegisterType.BYTE_TYPE);
    }

    private void analyze32BitPrimitiveAget(@Nonnull AnalyzedInstruction analyzedInstruction,
                                           @Nonnull RegisterType registerType) {
        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction, registerType);
    }

    private void analyzeAgetWide(@Nonnull AnalyzedInstruction analyzedInstruction) {
        ThreeRegisterInstruction instruction = (ThreeRegisterInstruction)analyzedInstruction.instruction;

        RegisterType arrayRegisterType = analyzedInstruction.getPreInstructionRegisterType(instruction.getRegisterB());
        if (arrayRegisterType.category != RegisterType.NULL) {
            if (arrayRegisterType.category != RegisterType.REFERENCE ||
                    !(arrayRegisterType.type instanceof ArrayProto)) {
                throw new AnalysisException("aget-wide used with non-array register: %s", arrayRegisterType.toString());
            }
            ArrayProto arrayProto = (ArrayProto)arrayRegisterType.type;

            if (arrayProto.dimensions != 1) {
                throw new AnalysisException("aget-wide used with multi-dimensional array: %s",
                        arrayRegisterType.toString());
            }

            char arrayBaseType = arrayProto.getElementType().charAt(0);
            if (arrayBaseType == 'J') {
                setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction, RegisterType.LONG_LO_TYPE);
            } else if (arrayBaseType == 'D') {
                setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction, RegisterType.DOUBLE_LO_TYPE);
            } else {
                throw new AnalysisException("aget-wide used with narrow array: %s", arrayRegisterType);
            }
        } else {
            // If the array register is null, we can assume that the destination register was meant to be a wide type.
            // This is the same behavior as dalvik's verifier
            setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction, RegisterType.LONG_LO_TYPE);
        }
    }

    private void analyzeAgetObject(@Nonnull AnalyzedInstruction analyzedInstruction) {
        ThreeRegisterInstruction instruction = (ThreeRegisterInstruction)analyzedInstruction.instruction;

        RegisterType arrayRegisterType = analyzedInstruction.getPreInstructionRegisterType(instruction.getRegisterB());
        if (arrayRegisterType.category != RegisterType.NULL) {
            if (arrayRegisterType.category != RegisterType.REFERENCE ||
                    !(arrayRegisterType.type instanceof ArrayProto)) {
                throw new AnalysisException("aget-object used with non-array register: %s",
                        arrayRegisterType.toString());
            }

            ArrayProto arrayProto = (ArrayProto)arrayRegisterType.type;

            String elementType = arrayProto.getImmediateElementType();

            setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction,
                    RegisterType.getRegisterType(RegisterType.REFERENCE, classPath.getClass(elementType)));
        } else {
            // If the array register is null, we can assume that the destination register was meant to be a reference
            // type, so we set the destination to NULL. This is the same behavior as dalvik's verifier
            setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction, RegisterType.NULL_TYPE);
        }
    }

    private void analyze32BitPrimitiveIgetSget(@Nonnull AnalyzedInstruction analyzedInstruction,
                                               @Nonnull RegisterType registerType) {
        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction, registerType);
    }

    private void analyzeIgetSgetWideObject(@Nonnull AnalyzedInstruction analyzedInstruction) {
        ReferenceInstruction referenceInstruction = (ReferenceInstruction)analyzedInstruction.instruction;

        FieldReference fieldReference = (FieldReference)referenceInstruction.getReference();

        RegisterType fieldType = RegisterType.getRegisterType(classPath, fieldReference.getType());
        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction, fieldType);
    }

    private void analyzeInvokeDirect(@Nonnull AnalyzedInstruction analyzedInstruction) {
        FiveRegisterInstruction instruction = (FiveRegisterInstruction)analyzedInstruction.instruction;
        analyzeInvokeDirectCommon(analyzedInstruction, instruction.getRegisterC());
    }

    private void analyzeInvokeDirectRange(@Nonnull AnalyzedInstruction analyzedInstruction) {
        RegisterRangeInstruction instruction = (RegisterRangeInstruction)analyzedInstruction.instruction;
        analyzeInvokeDirectCommon(analyzedInstruction, instruction.getStartRegister());
    }

    private void analyzeInvokeDirectCommon(@Nonnull AnalyzedInstruction analyzedInstruction, int objectRegister) {
        //the only time that an invoke instruction changes a register type is when using invoke-direct on a
        //constructor (<init>) method, which changes the uninitialized reference (and any register that the same
        //uninit reference has been copied to) to an initialized reference

        ReferenceInstruction instruction = (ReferenceInstruction)analyzedInstruction.instruction;

        MethodReference methodReference = (MethodReference)instruction.getReference();

        if (!methodReference.getName().equals("<init>")) {
            return;
        }

        RegisterType objectRegisterType = analyzedInstruction.getPreInstructionRegisterType(objectRegister);

        if (objectRegisterType.category != RegisterType.UNINIT_REF &&
                objectRegisterType.category != RegisterType.UNINIT_THIS) {
            return;
        }

        setPostRegisterTypeAndPropagateChanges(analyzedInstruction, objectRegister,
                RegisterType.getRegisterType(RegisterType.REFERENCE, objectRegisterType.type));

        for (int i=0; i<analyzedInstruction.postRegisterMap.length; i++) {
            RegisterType postInstructionRegisterType = analyzedInstruction.postRegisterMap[i];
            if (postInstructionRegisterType.category == RegisterType.UNKNOWN) {
                RegisterType preInstructionRegisterType =
                        analyzedInstruction.getPreInstructionRegisterType(i);

                if (preInstructionRegisterType.category == RegisterType.UNINIT_REF ||
                        preInstructionRegisterType.category == RegisterType.UNINIT_THIS) {
                    RegisterType registerType;
                    if (preInstructionRegisterType.equals(objectRegisterType)) {
                        registerType = analyzedInstruction.postRegisterMap[objectRegister];
                    } else {
                        registerType = preInstructionRegisterType;
                    }

                    setPostRegisterTypeAndPropagateChanges(analyzedInstruction, i, registerType);
                }
            }
        }
    }

    private void analyzeUnaryOp(@Nonnull AnalyzedInstruction analyzedInstruction,
                                @Nonnull RegisterType destRegisterType) {
        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction, destRegisterType);
    }

    private void analyzeBinaryOp(@Nonnull AnalyzedInstruction analyzedInstruction,
                                 @Nonnull RegisterType destRegisterType, boolean checkForBoolean) {
        if (checkForBoolean) {
            ThreeRegisterInstruction instruction = (ThreeRegisterInstruction)analyzedInstruction.instruction;

            RegisterType source1RegisterType =
                    analyzedInstruction.getPreInstructionRegisterType(instruction.getRegisterB());
            RegisterType source2RegisterType =
                    analyzedInstruction.getPreInstructionRegisterType(instruction.getRegisterC());

            if (BooleanCategories.get(source1RegisterType.category) &&
                    BooleanCategories.get(source2RegisterType.category)) {
                destRegisterType = RegisterType.BOOLEAN_TYPE;
            }
        }

        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction, destRegisterType);
    }

    private void analyzeBinary2AddrOp(@Nonnull AnalyzedInstruction analyzedInstruction,
                                      @Nonnull RegisterType destRegisterType, boolean checkForBoolean) {
        if (checkForBoolean) {
            TwoRegisterInstruction instruction = (TwoRegisterInstruction)analyzedInstruction.instruction;

            RegisterType source1RegisterType =
                    analyzedInstruction.getPreInstructionRegisterType(instruction.getRegisterA());
            RegisterType source2RegisterType =
                    analyzedInstruction.getPreInstructionRegisterType(instruction.getRegisterB());

            if (BooleanCategories.get(source1RegisterType.category) &&
                    BooleanCategories.get(source2RegisterType.category)) {
                destRegisterType = RegisterType.BOOLEAN_TYPE;
            }
        }

        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction, destRegisterType);
    }

    private void analyzeLiteralBinaryOp(@Nonnull AnalyzedInstruction analyzedInstruction,
                                        @Nonnull RegisterType destRegisterType, boolean checkForBoolean) {
        if (checkForBoolean) {
            TwoRegisterInstruction instruction = (TwoRegisterInstruction)analyzedInstruction.instruction;

            RegisterType sourceRegisterType =
                    analyzedInstruction.getPreInstructionRegisterType(instruction.getRegisterB());

            if (BooleanCategories.get(sourceRegisterType.category)) {
                int literal = ((NarrowLiteralInstruction)analyzedInstruction.instruction).getNarrowLiteral();
                if (literal == 0 || literal == 1) {
                    destRegisterType = RegisterType.BOOLEAN_TYPE;
                }
            }
        }

        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction, destRegisterType);
    }

    private RegisterType getDestTypeForLiteralShiftRight(@Nonnull AnalyzedInstruction analyzedInstruction, boolean signedShift) {
        TwoRegisterInstruction instruction = (TwoRegisterInstruction)analyzedInstruction.instruction;

        RegisterType sourceRegisterType = getAndCheckSourceRegister(analyzedInstruction, instruction.getRegisterB(),
                Primitive32BitCategories);
        long literalShift = ((NarrowLiteralInstruction)analyzedInstruction.instruction).getNarrowLiteral();

        if (literalShift == 0) {
            return sourceRegisterType;
        }

        RegisterType destRegisterType;
        if (!signedShift) {
            destRegisterType = RegisterType.INTEGER_TYPE;
        } else {
            destRegisterType = sourceRegisterType;
        }

        literalShift = literalShift & 0x1f;

        switch (sourceRegisterType.category) {
            case RegisterType.INTEGER:
            case RegisterType.FLOAT:
                if (!signedShift) {
                    if (literalShift > 24) {
                        return RegisterType.POS_BYTE_TYPE;
                    }
                    if (literalShift >= 16) {
                        return RegisterType.CHAR_TYPE;
                    }
                } else {
                    if (literalShift >= 24) {
                        return RegisterType.BYTE_TYPE;
                    }
                    if (literalShift >= 16) {
                        return RegisterType.SHORT_TYPE;
                    }
                }
                break;
            case RegisterType.SHORT:
                if (signedShift && literalShift >= 8) {
                    return RegisterType.BYTE_TYPE;
                }
                break;
            case RegisterType.POS_SHORT:
                if (literalShift >= 8) {
                    return RegisterType.POS_BYTE_TYPE;
                }
                break;
            case RegisterType.CHAR:
                if (literalShift > 8) {
                    return RegisterType.POS_BYTE_TYPE;
                }
                break;
            case RegisterType.BYTE:
                break;
            case RegisterType.POS_BYTE:
                return RegisterType.POS_BYTE_TYPE;
            case RegisterType.NULL:
            case RegisterType.ONE:
            case RegisterType.BOOLEAN:
                return RegisterType.NULL_TYPE;
            default:
                assert false;
        }

        return destRegisterType;
    }


    private void analyzeExecuteInline(@Nonnull AnalyzedInstruction analyzedInstruction) {
        if (inlineResolver == null) {
            throw new AnalysisException("Cannot analyze an odexed instruction unless we are deodexing");
        }

        Instruction35mi instruction = (Instruction35mi)analyzedInstruction.instruction;
        Method resolvedMethod = inlineResolver.resolveExecuteInline(analyzedInstruction);

        Opcode deodexedOpcode;
        int acccessFlags = resolvedMethod.getAccessFlags();
        if (AccessFlags.STATIC.isSet(acccessFlags)) {
            deodexedOpcode = Opcode.INVOKE_STATIC;
        } else if (AccessFlags.PRIVATE.isSet(acccessFlags)) {
            deodexedOpcode = Opcode.INVOKE_DIRECT;
        } else {
            deodexedOpcode = Opcode.INVOKE_VIRTUAL;
        }

        Instruction35c deodexedInstruction = new ImmutableInstruction35c(deodexedOpcode, instruction.getRegisterCount(),
                instruction.getRegisterC(), instruction.getRegisterD(), instruction.getRegisterE(),
                instruction.getRegisterF(), instruction.getRegisterG(), resolvedMethod);

        analyzedInstruction.setDeodexedInstruction(deodexedInstruction);
        analyzeInstruction(analyzedInstruction);
    }

    private void analyzeExecuteInlineRange(@Nonnull AnalyzedInstruction analyzedInstruction) {
        if (inlineResolver == null) {
            throw new AnalysisException("Cannot analyze an odexed instruction unless we are deodexing");
        }

        Instruction3rmi instruction = (Instruction3rmi)analyzedInstruction.instruction;
        Method resolvedMethod = inlineResolver.resolveExecuteInline(analyzedInstruction);

        Opcode deodexedOpcode;
        int acccessFlags = resolvedMethod.getAccessFlags();
        if (AccessFlags.STATIC.isSet(acccessFlags)) {
            deodexedOpcode = Opcode.INVOKE_STATIC_RANGE;
        } else if (AccessFlags.PRIVATE.isSet(acccessFlags)) {
            deodexedOpcode = Opcode.INVOKE_DIRECT_RANGE;
        } else {
            deodexedOpcode = Opcode.INVOKE_VIRTUAL_RANGE;
        }

        Instruction3rc deodexedInstruction = new ImmutableInstruction3rc(deodexedOpcode, instruction.getStartRegister(),
                instruction.getRegisterCount(), resolvedMethod);

        analyzedInstruction.setDeodexedInstruction(deodexedInstruction);
        analyzeInstruction(analyzedInstruction);
    }

    private void analyzeInvokeDirectEmpty(@Nonnull AnalyzedInstruction analyzedInstruction) {
        analyzeInvokeDirectEmpty(analyzedInstruction, true);
    }

    private void analyzeInvokeDirectEmpty(@Nonnull AnalyzedInstruction analyzedInstruction, boolean analyzeResult) {
        Instruction35c instruction = (Instruction35c)analyzedInstruction.instruction;

        Instruction35c deodexedInstruction = new ImmutableInstruction35c(Opcode.INVOKE_DIRECT,
                instruction.getRegisterCount(), instruction.getRegisterC(), instruction.getRegisterD(),
                instruction.getRegisterE(), instruction.getRegisterF(), instruction.getRegisterG(),
                instruction.getReference());

        analyzedInstruction.setDeodexedInstruction(deodexedInstruction);

        if (analyzeResult) {
            analyzeInstruction(analyzedInstruction);
        }
    }

    private void analyzeInvokeObjectInitRange(@Nonnull AnalyzedInstruction analyzedInstruction) {
        analyzeInvokeObjectInitRange(analyzedInstruction, true);
    }

    private void analyzeInvokeObjectInitRange(@Nonnull AnalyzedInstruction analyzedInstruction, boolean analyzeResult) {
        Instruction3rc instruction = (Instruction3rc)analyzedInstruction.instruction;

        Instruction deodexedInstruction;

        int startRegister = instruction.getStartRegister();
        int registerCount = instruction.getRegisterCount();
        if (registerCount == 1 && startRegister < 16) {
            deodexedInstruction = new ImmutableInstruction35c(Opcode.INVOKE_DIRECT,
                    registerCount, startRegister, 0, 0, 0, 0, instruction.getReference());
        } else {
            deodexedInstruction = new ImmutableInstruction3rc(Opcode.INVOKE_DIRECT_RANGE,
                    startRegister, registerCount, instruction.getReference());
        }

        analyzedInstruction.setDeodexedInstruction(deodexedInstruction);

        if (analyzeResult) {
            analyzeInstruction(analyzedInstruction);
        }
    }

    private boolean analyzeIputIgetQuick(@Nonnull AnalyzedInstruction analyzedInstruction) {
        Instruction22cs instruction = (Instruction22cs)analyzedInstruction.instruction;

        int fieldOffset = instruction.getFieldOffset();
        RegisterType objectRegisterType = getAndCheckSourceRegister(analyzedInstruction, instruction.getRegisterB(),
                ReferenceOrUninitCategories);

        if (objectRegisterType.category == RegisterType.NULL) {
            return false;
        }

        TypeProto objectRegisterTypeProto = objectRegisterType.type;
        assert objectRegisterTypeProto != null;

        TypeProto classTypeProto = classPath.getClass(objectRegisterTypeProto.getType());
        FieldReference resolvedField = classTypeProto.getFieldByOffset(fieldOffset);

        if (resolvedField == null) {
            throw new AnalysisException("Could not resolve the field in class %s at offset %d",
                    objectRegisterType.type.getType(), fieldOffset);
        }

        ClassDef thisClass = classPath.getClassDef(method.getDefiningClass());

        if (!canAccessClass(thisClass, classPath.getClassDef(resolvedField.getDefiningClass()))) {

            // the class is not accessible. So we start looking at objectRegisterTypeProto (which may be different
            // than resolvedField.getDefiningClass()), and walk up the class hierarchy.
            ClassDef fieldClass = classPath.getClassDef(objectRegisterTypeProto.getType());
            while (!canAccessClass(thisClass, fieldClass)) {
                String superclass = fieldClass.getSuperclass();
                if (superclass == null) {
                    throw new ExceptionWithContext("Couldn't find accessible class while resolving field %s",
                            ReferenceUtil.getShortFieldDescriptor(resolvedField));
                }

                fieldClass = classPath.getClassDef(superclass);
            }

            // fieldClass is now the first accessible class found. Now. we need to make sure that the field is
            // actually valid for this class
            resolvedField = classPath.getClass(fieldClass.getType()).getFieldByOffset(fieldOffset);
            if (resolvedField == null) {
                throw new ExceptionWithContext("Couldn't find accessible class while resolving field %s",
                        ReferenceUtil.getShortFieldDescriptor(resolvedField));
            }
            resolvedField = new ImmutableFieldReference(fieldClass.getType(), resolvedField.getName(),
                    resolvedField.getType());
        }

        String fieldType = resolvedField.getType();

        Opcode opcode = OdexedFieldInstructionMapper.getAndCheckDeodexedOpcodeForOdexedOpcode(fieldType,
                instruction.getOpcode());

        Instruction22c deodexedInstruction = new ImmutableInstruction22c(opcode, (byte)instruction.getRegisterA(),
                (byte)instruction.getRegisterB(), resolvedField);
        analyzedInstruction.setDeodexedInstruction(deodexedInstruction);

        analyzeInstruction(analyzedInstruction);

        return true;
    }

    private boolean analyzeInvokeVirtualQuick(@Nonnull AnalyzedInstruction analyzedInstruction, boolean isSuper,
                                              boolean isRange) {
        int methodIndex;
        int objectRegister;

        if (isRange) {
            Instruction3rms instruction = (Instruction3rms)analyzedInstruction.instruction;
            methodIndex = instruction.getVtableIndex();
            objectRegister = instruction.getStartRegister();
        } else {
            Instruction35ms instruction = (Instruction35ms)analyzedInstruction.instruction;
            methodIndex = instruction.getVtableIndex();
            objectRegister = instruction.getRegisterC();
        }

        RegisterType objectRegisterType = getAndCheckSourceRegister(analyzedInstruction, objectRegister,
                ReferenceOrUninitCategories);
        TypeProto objectRegisterTypeProto = objectRegisterType.type;

        if (objectRegisterType.category == RegisterType.NULL) {
            return false;
        }

        assert objectRegisterTypeProto != null;

        MethodReference resolvedMethod;
        if (isSuper) {
            // invoke-super is only used for the same class that we're currently in
            TypeProto typeProto = classPath.getClass(method.getDefiningClass());
            TypeProto superType;

            String superclassType = typeProto.getSuperclass();
            if (superclassType != null) {
                superType = classPath.getClass(superclassType);
            } else {
                // This is either java.lang.Object, or an UnknownClassProto
                superType = typeProto;
            }

            resolvedMethod = superType.getMethodByVtableIndex(methodIndex);
        } else{
            resolvedMethod = objectRegisterTypeProto.getMethodByVtableIndex(methodIndex);
        }

        if (resolvedMethod == null) {
            throw new AnalysisException("Could not resolve the method in class %s at index %d",
                    objectRegisterType.type.getType(), methodIndex);
        }

        // no need to check class access for invoke-super. A class can obviously access its superclass.
        ClassDef thisClass = classPath.getClassDef(method.getDefiningClass());

        if (!isSuper && !canAccessClass(thisClass, classPath.getClassDef(resolvedMethod.getDefiningClass()))) {

            // the class is not accessible. So we start looking at objectRegisterTypeProto (which may be different
            // than resolvedMethod.getDefiningClass()), and walk up the class hierarchy.
            ClassDef methodClass = classPath.getClassDef(objectRegisterTypeProto.getType());
            while (!canAccessClass(thisClass, methodClass)) {
                String superclass = methodClass.getSuperclass();
                if (superclass == null) {
                    throw new ExceptionWithContext("Couldn't find accessible class while resolving method %s",
                            ReferenceUtil.getShortMethodDescriptor(resolvedMethod));
                }

                methodClass = classPath.getClassDef(superclass);
            }

            // methodClass is now the first accessible class found. Now. we need to make sure that the method is
            // actually valid for this class
            resolvedMethod = classPath.getClass(methodClass.getType()).getMethodByVtableIndex(methodIndex);
            if (resolvedMethod == null) {
                throw new ExceptionWithContext("Couldn't find accessible class while resolving method %s",
                        ReferenceUtil.getShortMethodDescriptor(resolvedMethod));
            }
            resolvedMethod = new ImmutableMethodReference(methodClass.getType(), resolvedMethod.getName(),
                    resolvedMethod.getParameterTypes(), resolvedMethod.getReturnType());
        }

        Instruction deodexedInstruction;
        if (isRange) {
            Instruction3rms instruction = (Instruction3rms)analyzedInstruction.instruction;
            Opcode opcode;
            if (isSuper) {
                opcode = Opcode.INVOKE_SUPER_RANGE;
            } else {
                opcode = Opcode.INVOKE_VIRTUAL_RANGE;
            }

            deodexedInstruction = new ImmutableInstruction3rc(opcode, instruction.getStartRegister(),
                    instruction.getRegisterCount(), resolvedMethod);
        } else {
            Instruction35ms instruction = (Instruction35ms)analyzedInstruction.instruction;
            Opcode opcode;
            if (isSuper) {
                opcode = Opcode.INVOKE_SUPER;
            } else {
                opcode = Opcode.INVOKE_VIRTUAL;
            }

            deodexedInstruction = new ImmutableInstruction35c(opcode, instruction.getRegisterCount(),
                    instruction.getRegisterC(), instruction.getRegisterD(), instruction.getRegisterE(),
                    instruction.getRegisterF(), instruction.getRegisterG(), resolvedMethod);
        }

        analyzedInstruction.setDeodexedInstruction(deodexedInstruction);
        analyzeInstruction(analyzedInstruction);

        return true;
    }

    private boolean canAccessClass(@Nonnull ClassDef accessorClassDef, @Nonnull ClassDef accesseeClassDef) {
        if (AccessFlags.PUBLIC.isSet(accesseeClassDef.getAccessFlags())) {
            return true;
        }

        // Classes can only be public or package private. Any private or protected inner classes are actually
        // package private.
        return getPackage(accesseeClassDef.getType()).equals(getPackage(accessorClassDef.getType()));
    }

    private static String getPackage(String className) {
        int lastSlash = className.lastIndexOf('/');
        if (lastSlash < 0) {
            return "";
        }
        return className.substring(1, lastSlash);
    }

    private boolean analyzePutGetVolatile(@Nonnull AnalyzedInstruction analyzedInstruction) {
        return analyzePutGetVolatile(analyzedInstruction, true);
    }

    private boolean analyzePutGetVolatile(@Nonnull AnalyzedInstruction analyzedInstruction, boolean analyzeResult) {
        FieldReference field = (FieldReference)((ReferenceInstruction)analyzedInstruction.instruction).getReference();
        String fieldType = field.getType();

        Opcode originalOpcode = analyzedInstruction.instruction.getOpcode();

        Opcode opcode = OdexedFieldInstructionMapper.getAndCheckDeodexedOpcodeForOdexedOpcode(fieldType,
                originalOpcode);

        Instruction deodexedInstruction;

        if (originalOpcode.isOdexedStaticVolatile()) {
            OneRegisterInstruction instruction = (OneRegisterInstruction)analyzedInstruction.instruction;
            deodexedInstruction = new ImmutableInstruction21c(opcode, instruction.getRegisterA(), field);
        } else {
            TwoRegisterInstruction instruction = (TwoRegisterInstruction)analyzedInstruction.instruction;

            deodexedInstruction = new ImmutableInstruction22c(opcode, instruction.getRegisterA(),
                    instruction.getRegisterB(), field);
        }

        analyzedInstruction.setDeodexedInstruction(deodexedInstruction);

        if (analyzeResult) {
            analyzeInstruction(analyzedInstruction);
        }
        return true;
    }

    @Nonnull
    private static RegisterType getAndCheckSourceRegister(@Nonnull AnalyzedInstruction analyzedInstruction,
                                                          int registerNumber, BitSet validCategories) {
        assert registerNumber >= 0 && registerNumber < analyzedInstruction.postRegisterMap.length;

        RegisterType registerType = analyzedInstruction.getPreInstructionRegisterType(registerNumber);

        checkRegister(registerType, registerNumber, validCategories);

        if (validCategories == WideLowCategories) {
            checkRegister(registerType, registerNumber, WideLowCategories);
            checkWidePair(registerNumber, analyzedInstruction);

            RegisterType secondRegisterType = analyzedInstruction.getPreInstructionRegisterType(registerNumber + 1);
            checkRegister(secondRegisterType, registerNumber+1, WideHighCategories);
        }

        return registerType;
    }

    private static void checkRegister(RegisterType registerType, int registerNumber, BitSet validCategories) {
        if (!validCategories.get(registerType.category)) {
            throw new AnalysisException(String.format("Invalid register type %s for register v%d.",
                    registerType.toString(), registerNumber));
        }
    }

    private static void checkWidePair(int registerNumber, AnalyzedInstruction analyzedInstruction) {
        if (registerNumber + 1 >= analyzedInstruction.postRegisterMap.length) {
            throw new AnalysisException(String.format("v%d cannot be used as the first register in a wide register" +
                    "pair because it is the last register.", registerNumber));
        }
    }
}