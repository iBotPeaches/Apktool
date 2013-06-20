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

package org.jf.dexlib.Code.Analysis;

import org.jf.dexlib.*;
import org.jf.dexlib.Code.*;
import org.jf.dexlib.Code.Format.*;
import org.jf.dexlib.Util.AccessFlags;
import org.jf.util.ExceptionWithContext;
import org.jf.dexlib.Util.SparseArray;

import java.util.BitSet;
import java.util.EnumSet;
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
    private final ClassDataItem.EncodedMethod encodedMethod;

    private final DeodexUtil deodexUtil;

    private SparseArray<AnalyzedInstruction> instructions;

    private static final int NOT_ANALYZED = 0;
    private static final int ANALYZED = 1;
    private static final int VERIFIED = 2;
    private int analyzerState = NOT_ANALYZED;

    private BitSet analyzedInstructions;

    private ValidationException validationException = null;

    //This is a dummy instruction that occurs immediately before the first real instruction. We can initialize the
    //register types for this instruction to the parameter types, in order to have them propagate to all of its
    //successors, e.g. the first real instruction, the first instructions in any exception handlers covering the first
    //instruction, etc.
    private AnalyzedInstruction startOfMethod;

    public MethodAnalyzer(ClassDataItem.EncodedMethod encodedMethod, boolean deodex,
                          InlineMethodResolver inlineResolver) {
        if (encodedMethod == null) {
            throw new IllegalArgumentException("encodedMethod cannot be null");
        }
        if (encodedMethod.codeItem == null || encodedMethod.codeItem.getInstructions().length == 0) {
            throw new IllegalArgumentException("The method has no code");
        }
        this.encodedMethod = encodedMethod;

        if (deodex) {
            if (inlineResolver != null) {
                this.deodexUtil = new DeodexUtil(encodedMethod.method.getDexFile(), inlineResolver);
            } else {
                this.deodexUtil = new DeodexUtil(encodedMethod.method.getDexFile());
            }
        } else {
            this.deodexUtil = null;
        }

        //override AnalyzedInstruction and provide custom implementations of some of the methods, so that we don't
        //have to handle the case this special case of instruction being null, in the main class
        startOfMethod = new AnalyzedInstruction(null, -1, encodedMethod.codeItem.getRegisterCount()) {
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
            };
        };

        buildInstructionList();

        analyzedInstructions = new BitSet(instructions.size());
    }

    public boolean isAnalyzed() {
        return analyzerState >= ANALYZED;
    }

    public boolean isVerified() {
        return analyzerState == VERIFIED;
    }

    public void analyze() {
        assert encodedMethod != null;
        assert encodedMethod.codeItem != null;

        if (analyzerState >= ANALYZED) {
            //the instructions have already been analyzed, so there is nothing to do
            return;
        }

        CodeItem codeItem = encodedMethod.codeItem;
        MethodIdItem methodIdItem = encodedMethod.method;

        int totalRegisters = codeItem.getRegisterCount();
        int parameterRegisters = methodIdItem.getPrototype().getParameterRegisterCount();

        int nonParameterRegisters = totalRegisters - parameterRegisters;

        for (AnalyzedInstruction instruction: instructions.getValues()) {
            instruction.dead = true;
        }

        //if this isn't a static method, determine which register is the "this" register and set the type to the
        //current class
        if ((encodedMethod.accessFlags & AccessFlags.STATIC.getValue()) == 0) {
            nonParameterRegisters--;
            int thisRegister = totalRegisters - parameterRegisters - 1;

            //if this is a constructor, then set the "this" register to an uninitialized reference of the current class
            if ((encodedMethod.accessFlags & AccessFlags.CONSTRUCTOR.getValue()) != 0) {
                setPostRegisterTypeAndPropagateChanges(startOfMethod, thisRegister,
                        RegisterType.getRegisterType(RegisterType.Category.UninitThis,
                            ClassPath.getClassDef(methodIdItem.getContainingClass())));
            } else {
                setPostRegisterTypeAndPropagateChanges(startOfMethod, thisRegister,
                        RegisterType.getRegisterType(RegisterType.Category.Reference,
                            ClassPath.getClassDef(methodIdItem.getContainingClass())));
            }
        }

        TypeListItem parameters = methodIdItem.getPrototype().getParameters();
        if (parameters != null) {
            RegisterType[] parameterTypes = getParameterTypes(parameters, parameterRegisters);
            for (int i=0; i<parameterTypes.length; i++) {
                RegisterType registerType = parameterTypes[i];
                int registerNum = (totalRegisters - parameterRegisters) + i;
                setPostRegisterTypeAndPropagateChanges(startOfMethod, registerNum, registerType);
            }
        }

        RegisterType uninit = RegisterType.getRegisterType(RegisterType.Category.Uninit, null);
        for (int i=0; i<nonParameterRegisters; i++) {
            setPostRegisterTypeAndPropagateChanges(startOfMethod, i, uninit);
        }

        BitSet instructionsToAnalyze = new BitSet(instructions.size());

        //make sure all of the "first instructions" are marked for processing
        for (AnalyzedInstruction successor: startOfMethod.successors) {
            instructionsToAnalyze.set(successor.instructionIndex);
        }

        BitSet undeodexedInstructions = new BitSet(instructions.size());

        do {
            boolean didSomething = false;

            while (!instructionsToAnalyze.isEmpty()) {
                for(int i=instructionsToAnalyze.nextSetBit(0); i>=0; i=instructionsToAnalyze.nextSetBit(i+1)) {
                    instructionsToAnalyze.clear(i);
                    if (analyzedInstructions.get(i)) {
                        continue;
                    }
                    AnalyzedInstruction instructionToAnalyze = instructions.valueAt(i);
                    instructionToAnalyze.dead = false;
                    try {
                        if (instructionToAnalyze.originalInstruction.opcode.odexOnly()) {
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
                    } catch (ValidationException ex) {
                        this.validationException = ex;
                        int codeAddress = getInstructionAddress(instructionToAnalyze);
                        ex.setCodeAddress(codeAddress);
                        ex.addContext(String.format("opcode: %s", instructionToAnalyze.instruction.opcode.name));
                        ex.addContext(String.format("CodeAddress: %d", codeAddress));
                        ex.addContext(String.format("Method: %s", encodedMethod.method.getMethodString()));
                        break;
                    }

                    analyzedInstructions.set(instructionToAnalyze.getInstructionIndex());

                    for (AnalyzedInstruction successor: instructionToAnalyze.successors) {
                        instructionsToAnalyze.set(successor.getInstructionIndex());
                    }
                }
                if (validationException != null) {
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
        for (int i=0; i<instructions.size(); i++) {
            AnalyzedInstruction analyzedInstruction = instructions.valueAt(i);

            Instruction instruction = analyzedInstruction.getInstruction();

            if (instruction.opcode.odexOnly()) {
                int objectRegisterNumber;
                switch (instruction.getFormat()) {
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
                        objectRegisterNumber = ((FiveRegisterInstruction)instruction).getRegisterD();
                        break;
                    case Format3rmi:
                    case Format3rms:
                        objectRegisterNumber = ((RegisterRangeInstruction)instruction).getStartRegister();
                        break;
                    default:
                        continue;
                }

                analyzedInstruction.setDeodexedInstruction(new UnresolvedOdexInstruction(instruction, objectRegisterNumber));
            }
        }

        analyzerState = ANALYZED;
    }

    public void verify() {
        if (analyzerState < ANALYZED) {
            throw new ExceptionWithContext("You must call analyze() before calling verify().");
        }

        if (analyzerState == VERIFIED) {
            //we've already verified the bytecode. nothing to do
            return;
        }

        BitSet instructionsToVerify = new BitSet(instructions.size());
        BitSet verifiedInstructions = new BitSet(instructions.size());

        //make sure all of the "first instructions" are marked for processing
        for (AnalyzedInstruction successor: startOfMethod.successors) {
            instructionsToVerify.set(successor.instructionIndex);
        }

        while (!instructionsToVerify.isEmpty()) {
            for (int i=instructionsToVerify.nextSetBit(0); i>=0; i=instructionsToVerify.nextSetBit(i+1)) {
                instructionsToVerify.clear(i);
                if (verifiedInstructions.get(i)) {
                    continue;
                }
                AnalyzedInstruction instructionToVerify = instructions.valueAt(i);
                try {
                    verifyInstruction(instructionToVerify);
                } catch (ValidationException ex) {
                    this.validationException = ex;
                    int codeAddress = getInstructionAddress(instructionToVerify);
                    ex.setCodeAddress(codeAddress);
                    ex.addContext(String.format("opcode: %s", instructionToVerify.instruction.opcode.name));
                    ex.addContext(String.format("CodeAddress: %d", codeAddress));
                    ex.addContext(String.format("Method: %s", encodedMethod.method.getMethodString()));
                    break;
                }

                verifiedInstructions.set(instructionToVerify.getInstructionIndex());

                for (AnalyzedInstruction successor: instructionToVerify.successors) {
                    instructionsToVerify.set(successor.getInstructionIndex());
                }
            }
            if (validationException != null) {
                break;
            }
        }

        analyzerState = VERIFIED;
    }

    private int getThisRegister() {
        assert (encodedMethod.accessFlags & AccessFlags.STATIC.getValue()) == 0;

        CodeItem codeItem = encodedMethod.codeItem;
        assert codeItem != null;

        MethodIdItem methodIdItem = encodedMethod.method;
        assert methodIdItem != null;

        int totalRegisters = codeItem.getRegisterCount();
        if (totalRegisters == 0) {
            throw new ValidationException("A non-static method must have at least 1 register");
        }

        int parameterRegisters = methodIdItem.getPrototype().getParameterRegisterCount();

        return totalRegisters - parameterRegisters - 1;
    }

    private boolean isInstanceConstructor() {
        return (encodedMethod.accessFlags & AccessFlags.STATIC.getValue()) == 0 &&
               (encodedMethod.accessFlags & AccessFlags.CONSTRUCTOR.getValue()) != 0;
    }

    private boolean isStaticConstructor() {
        return (encodedMethod.accessFlags & AccessFlags.STATIC.getValue()) != 0 &&
               (encodedMethod.accessFlags & AccessFlags.CONSTRUCTOR.getValue()) != 0;
    }

    public AnalyzedInstruction getStartOfMethod() {
        return startOfMethod;
    }

    /**
     * @return a read-only list containing the instructions for tihs method.
     */
    public List<AnalyzedInstruction> getInstructions() {
        return instructions.getValues();
    }

    public ClassDataItem.EncodedMethod getMethod() {
        return this.encodedMethod;
    }

    public ValidationException getValidationException() {
        return validationException;
    }

    private static RegisterType[] getParameterTypes(TypeListItem typeListItem, int parameterRegisterCount) {
        assert typeListItem != null;
        assert parameterRegisterCount == typeListItem.getRegisterCount();

        RegisterType[] registerTypes = new RegisterType[parameterRegisterCount];

        int registerNum = 0;
        for (TypeIdItem type: typeListItem.getTypes()) {
            if (type.getRegisterCount() == 2) {
                registerTypes[registerNum++] = RegisterType.getWideRegisterTypeForTypeIdItem(type, true);
                registerTypes[registerNum++] = RegisterType.getWideRegisterTypeForTypeIdItem(type, false);
            } else {
                registerTypes[registerNum++] = RegisterType.getRegisterTypeForTypeIdItem(type);
            }
        }

        return registerTypes;
    }

    public int getInstructionAddress(AnalyzedInstruction instruction) {
        return instructions.keyAt(instruction.instructionIndex);
    }

    private void setDestinationRegisterTypeAndPropagateChanges(AnalyzedInstruction analyzedInstruction,
                                                               RegisterType registerType) {
        setPostRegisterTypeAndPropagateChanges(analyzedInstruction, analyzedInstruction.getDestinationRegister(),
                registerType);
    }

    private void setPostRegisterTypeAndPropagateChanges(AnalyzedInstruction analyzedInstruction, int registerNumber,
                                                RegisterType registerType) {

        BitSet changedInstructions = new BitSet(instructions.size());

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

                propagateRegisterToSuccessors(instructions.valueAt(instructionIndex), registerNumber,
                        changedInstructions);
            }
        }

        if (registerType.category == RegisterType.Category.LongLo) {
            checkWidePair(registerNumber, analyzedInstruction);
            setPostRegisterTypeAndPropagateChanges(analyzedInstruction, registerNumber+1,
                    RegisterType.getRegisterType(RegisterType.Category.LongHi, null));
        } else if (registerType.category == RegisterType.Category.DoubleLo) {
            checkWidePair(registerNumber, analyzedInstruction);
            setPostRegisterTypeAndPropagateChanges(analyzedInstruction, registerNumber+1,
                    RegisterType.getRegisterType(RegisterType.Category.DoubleHi, null));
        }
    }

    private void propagateRegisterToSuccessors(AnalyzedInstruction instruction, int registerNumber,
                                               BitSet changedInstructions) {
        RegisterType postRegisterType = instruction.getPostInstructionRegisterType(registerNumber);
        for (AnalyzedInstruction successor: instruction.successors) {
            if (successor.mergeRegister(registerNumber, postRegisterType, analyzedInstructions)) {
                changedInstructions.set(successor.instructionIndex);
            }
        }
    }

    private void buildInstructionList() {
        assert encodedMethod != null;
        assert encodedMethod.codeItem != null;
        int registerCount = encodedMethod.codeItem.getRegisterCount();

        Instruction[] insns = encodedMethod.codeItem.getInstructions();

        instructions = new SparseArray<AnalyzedInstruction>(insns.length);

        //first, create all the instructions and populate the instructionAddresses array
        int currentCodeAddress = 0;
        for (int i=0; i<insns.length; i++) {
            instructions.append(currentCodeAddress, new AnalyzedInstruction(insns[i], i, registerCount));
            assert instructions.indexOfKey(currentCodeAddress) == i;
            currentCodeAddress += insns[i].getSize(currentCodeAddress);
        }

        //next, populate the exceptionHandlers array. The array item for each instruction that can throw an exception
        //and is covered by a try block should be set to a list of the first instructions of each exception handler
        //for the try block covering the instruction
        CodeItem.TryItem[] tries = encodedMethod.codeItem.getTries();
        int triesIndex = 0;
        CodeItem.TryItem currentTry = null;
        AnalyzedInstruction[] currentExceptionHandlers = null;
        AnalyzedInstruction[][] exceptionHandlers = new AnalyzedInstruction[insns.length][];

        if (tries != null) {
            for (int i=0; i<instructions.size(); i++) {
                AnalyzedInstruction instruction = instructions.valueAt(i);
                Opcode instructionOpcode = instruction.instruction.opcode;
                currentCodeAddress = getInstructionAddress(instruction);

                //check if we have gone past the end of the current try
                if (currentTry != null) {
                    if (currentTry.getStartCodeAddress() + currentTry.getTryLength() <= currentCodeAddress) {
                        currentTry = null;
                        triesIndex++;
                    }
                }

                //check if the next try is applicable yet
                if (currentTry == null && triesIndex < tries.length) {
                    CodeItem.TryItem tryItem = tries[triesIndex];
                    if (tryItem.getStartCodeAddress() <= currentCodeAddress) {
                        assert(tryItem.getStartCodeAddress() + tryItem.getTryLength() > currentCodeAddress);

                        currentTry = tryItem;

                        currentExceptionHandlers = buildExceptionHandlerArray(tryItem);
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
        assert instructions.size() > 0;
        BitSet instructionsToProcess = new BitSet(insns.length);

        addPredecessorSuccessor(startOfMethod, instructions.valueAt(0), exceptionHandlers, instructionsToProcess);
        while (!instructionsToProcess.isEmpty()) {
            int currentInstructionIndex = instructionsToProcess.nextSetBit(0);
            instructionsToProcess.clear(currentInstructionIndex);

            AnalyzedInstruction instruction = instructions.valueAt(currentInstructionIndex);
            Opcode instructionOpcode = instruction.instruction.opcode;
            int instructionCodeAddress = getInstructionAddress(instruction);

            if (instruction.instruction.opcode.canContinue()) {
                if (instruction.instruction.opcode != Opcode.NOP ||
                    !instruction.instruction.getFormat().variableSizeFormat) {

                    if (currentInstructionIndex == instructions.size() - 1) {
                        throw new ValidationException("Execution can continue past the last instruction");
                    }

                    AnalyzedInstruction nextInstruction = instructions.valueAt(currentInstructionIndex+1);
                    addPredecessorSuccessor(instruction, nextInstruction, exceptionHandlers, instructionsToProcess);
                }
            }

            if (instruction.instruction instanceof OffsetInstruction) {
                OffsetInstruction offsetInstruction = (OffsetInstruction)instruction.instruction;

                if (instructionOpcode == Opcode.PACKED_SWITCH || instructionOpcode == Opcode.SPARSE_SWITCH) {
                    MultiOffsetInstruction switchDataInstruction =
                            (MultiOffsetInstruction)instructions.get(instructionCodeAddress +
                                    offsetInstruction.getTargetAddressOffset()).instruction;
                    for (int targetAddressOffset: switchDataInstruction.getTargets()) {
                        AnalyzedInstruction targetInstruction = instructions.get(instructionCodeAddress +
                                targetAddressOffset);

                        addPredecessorSuccessor(instruction, targetInstruction, exceptionHandlers,
                                instructionsToProcess);
                    }
                } else {
                    int targetAddressOffset = offsetInstruction.getTargetAddressOffset();
                    AnalyzedInstruction targetInstruction = instructions.get(instructionCodeAddress +
                            targetAddressOffset);
                    addPredecessorSuccessor(instruction, targetInstruction, exceptionHandlers, instructionsToProcess);
                }
            }
        }
    }

    private void addPredecessorSuccessor(AnalyzedInstruction predecessor, AnalyzedInstruction successor,
                                                AnalyzedInstruction[][] exceptionHandlers,
                                                BitSet instructionsToProcess) {
        addPredecessorSuccessor(predecessor, successor, exceptionHandlers, instructionsToProcess, false);
    }

    private void addPredecessorSuccessor(AnalyzedInstruction predecessor, AnalyzedInstruction successor,
                                                AnalyzedInstruction[][] exceptionHandlers,
                                                BitSet instructionsToProcess, boolean allowMoveException) {

        if (!allowMoveException && successor.instruction.opcode == Opcode.MOVE_EXCEPTION) {
            throw new ValidationException("Execution can pass from the " + predecessor.instruction.opcode.name +
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
            assert successor.instruction.opcode.canThrow();

            for (AnalyzedInstruction exceptionHandler: exceptionHandlersForSuccessor) {
                addPredecessorSuccessor(predecessor, exceptionHandler, exceptionHandlers, instructionsToProcess, true);
            }
        }
    }

    private AnalyzedInstruction[] buildExceptionHandlerArray(CodeItem.TryItem tryItem) {
        int exceptionHandlerCount = tryItem.encodedCatchHandler.handlers.length;
        int catchAllHandler = tryItem.encodedCatchHandler.getCatchAllHandlerAddress();
        if (catchAllHandler != -1) {
            exceptionHandlerCount++;
        }

        AnalyzedInstruction[] exceptionHandlers = new AnalyzedInstruction[exceptionHandlerCount];
        for (int i=0; i<tryItem.encodedCatchHandler.handlers.length; i++) {
            exceptionHandlers[i] = instructions.get(tryItem.encodedCatchHandler.handlers[i].getHandlerAddress());
        }

        if (catchAllHandler != -1) {
            exceptionHandlers[exceptionHandlers.length - 1] = instructions.get(catchAllHandler);
        }

        return exceptionHandlers;
    }

    /**
     * @return false if analyzedInstruction is an odex instruction that couldn't be deodexed, due to its
     * object register being null
     */
    private boolean analyzeInstruction(AnalyzedInstruction analyzedInstruction) {
        Instruction instruction = analyzedInstruction.instruction;

        switch (instruction.opcode) {
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
                analyzeConst(analyzedInstruction);
                return true;
            case CONST_HIGH16:
                analyzeConstHigh16(analyzedInstruction);
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
                analyzeArrayDataOrSwitch(analyzedInstruction);
            case THROW:
            case GOTO:
            case GOTO_16:
            case GOTO_32:
                return true;
            case PACKED_SWITCH:
            case SPARSE_SWITCH:
                analyzeArrayDataOrSwitch(analyzedInstruction);
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
                analyze32BitPrimitiveAget(analyzedInstruction, RegisterType.Category.Integer);
                return true;
            case AGET_BOOLEAN:
                analyze32BitPrimitiveAget(analyzedInstruction, RegisterType.Category.Boolean);
                return true;
            case AGET_BYTE:
                analyze32BitPrimitiveAget(analyzedInstruction, RegisterType.Category.Byte);
                return true;
            case AGET_CHAR:
                analyze32BitPrimitiveAget(analyzedInstruction, RegisterType.Category.Char);
                return true;
            case AGET_SHORT:
                analyze32BitPrimitiveAget(analyzedInstruction, RegisterType.Category.Short);
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
                analyze32BitPrimitiveIget(analyzedInstruction, RegisterType.Category.Integer);
                return true;
            case IGET_BOOLEAN:
                analyze32BitPrimitiveIget(analyzedInstruction, RegisterType.Category.Boolean);
                return true;
            case IGET_BYTE:
                analyze32BitPrimitiveIget(analyzedInstruction, RegisterType.Category.Byte);
                return true;
            case IGET_CHAR:
                analyze32BitPrimitiveIget(analyzedInstruction, RegisterType.Category.Char);
                return true;
            case IGET_SHORT:
                analyze32BitPrimitiveIget(analyzedInstruction, RegisterType.Category.Short);
                return true;
            case IGET_WIDE:
            case IGET_OBJECT:
                analyzeIgetWideObject(analyzedInstruction);
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
                analyze32BitPrimitiveSget(analyzedInstruction, RegisterType.Category.Integer);
                return true;
            case SGET_BOOLEAN:
                analyze32BitPrimitiveSget(analyzedInstruction, RegisterType.Category.Boolean);
                return true;
            case SGET_BYTE:
                analyze32BitPrimitiveSget(analyzedInstruction, RegisterType.Category.Byte);
                return true;
            case SGET_CHAR:
                analyze32BitPrimitiveSget(analyzedInstruction, RegisterType.Category.Char);
                return true;
            case SGET_SHORT:
                analyze32BitPrimitiveSget(analyzedInstruction, RegisterType.Category.Short);
                return true;
            case SGET_WIDE:
            case SGET_OBJECT:
                analyzeSgetWideObject(analyzedInstruction);
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
                analyzeUnaryOp(analyzedInstruction, RegisterType.Category.Integer);
                return true;
            case NEG_LONG:
            case NOT_LONG:
                analyzeUnaryOp(analyzedInstruction, RegisterType.Category.LongLo);
                return true;
            case NEG_FLOAT:
                analyzeUnaryOp(analyzedInstruction, RegisterType.Category.Float);
                return true;
            case NEG_DOUBLE:
                analyzeUnaryOp(analyzedInstruction, RegisterType.Category.DoubleLo);
                return true;
            case INT_TO_LONG:
                analyzeUnaryOp(analyzedInstruction, RegisterType.Category.LongLo);
                return true;
            case INT_TO_FLOAT:
                analyzeUnaryOp(analyzedInstruction, RegisterType.Category.Float);
                return true;
            case INT_TO_DOUBLE:
                analyzeUnaryOp(analyzedInstruction, RegisterType.Category.DoubleLo);
                return true;
            case LONG_TO_INT:
            case DOUBLE_TO_INT:
                analyzeUnaryOp(analyzedInstruction, RegisterType.Category.Integer);
                return true;
            case LONG_TO_FLOAT:
            case DOUBLE_TO_FLOAT:
                analyzeUnaryOp(analyzedInstruction, RegisterType.Category.Float);
                return true;
            case LONG_TO_DOUBLE:
                analyzeUnaryOp(analyzedInstruction, RegisterType.Category.DoubleLo);
                return true;
            case FLOAT_TO_INT:
                analyzeUnaryOp(analyzedInstruction, RegisterType.Category.Integer);
                return true;
            case FLOAT_TO_LONG:
                analyzeUnaryOp(analyzedInstruction, RegisterType.Category.LongLo);
                return true;
            case FLOAT_TO_DOUBLE:
                analyzeUnaryOp(analyzedInstruction, RegisterType.Category.DoubleLo);
                return true;
            case DOUBLE_TO_LONG:
                analyzeUnaryOp(analyzedInstruction, RegisterType.Category.LongLo);
                return true;
            case INT_TO_BYTE:
                analyzeUnaryOp(analyzedInstruction, RegisterType.Category.Byte);
                return true;
            case INT_TO_CHAR:
                analyzeUnaryOp(analyzedInstruction, RegisterType.Category.Char);
                return true;
            case INT_TO_SHORT:
                analyzeUnaryOp(analyzedInstruction, RegisterType.Category.Short);
                return true;
            case ADD_INT:
            case SUB_INT:
            case MUL_INT:
            case DIV_INT:
            case REM_INT:
            case SHL_INT:
            case SHR_INT:
            case USHR_INT:
                analyzeBinaryOp(analyzedInstruction, RegisterType.Category.Integer, false);
                return true;
            case AND_INT:
            case OR_INT:
            case XOR_INT:
                analyzeBinaryOp(analyzedInstruction, RegisterType.Category.Integer, true);
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
                analyzeBinaryOp(analyzedInstruction, RegisterType.Category.LongLo, false);
                return true;
            case ADD_FLOAT:
            case SUB_FLOAT:
            case MUL_FLOAT:
            case DIV_FLOAT:
            case REM_FLOAT:
                analyzeBinaryOp(analyzedInstruction, RegisterType.Category.Float, false);
                return true;
            case ADD_DOUBLE:
            case SUB_DOUBLE:
            case MUL_DOUBLE:
            case DIV_DOUBLE:
            case REM_DOUBLE:
                analyzeBinaryOp(analyzedInstruction, RegisterType.Category.DoubleLo, false);
                return true;
            case ADD_INT_2ADDR:
            case SUB_INT_2ADDR:
            case MUL_INT_2ADDR:
            case DIV_INT_2ADDR:
            case REM_INT_2ADDR:
            case SHL_INT_2ADDR:
            case SHR_INT_2ADDR:
            case USHR_INT_2ADDR:
                analyzeBinary2AddrOp(analyzedInstruction, RegisterType.Category.Integer, false);
                return true;
            case AND_INT_2ADDR:
            case OR_INT_2ADDR:
            case XOR_INT_2ADDR:
                analyzeBinary2AddrOp(analyzedInstruction, RegisterType.Category.Integer, true);
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
                analyzeBinary2AddrOp(analyzedInstruction, RegisterType.Category.LongLo, false);
                return true;
            case ADD_FLOAT_2ADDR:
            case SUB_FLOAT_2ADDR:
            case MUL_FLOAT_2ADDR:
            case DIV_FLOAT_2ADDR:
            case REM_FLOAT_2ADDR:
                analyzeBinary2AddrOp(analyzedInstruction, RegisterType.Category.Float, false);
                return true;
            case ADD_DOUBLE_2ADDR:
            case SUB_DOUBLE_2ADDR:
            case MUL_DOUBLE_2ADDR:
            case DIV_DOUBLE_2ADDR:
            case REM_DOUBLE_2ADDR:
                analyzeBinary2AddrOp(analyzedInstruction, RegisterType.Category.DoubleLo, false);
                return true;
            case ADD_INT_LIT16:
            case RSUB_INT:
            case MUL_INT_LIT16:
            case DIV_INT_LIT16:
            case REM_INT_LIT16:
                analyzeLiteralBinaryOp(analyzedInstruction, RegisterType.Category.Integer, false);
                return true;
            case AND_INT_LIT16:
            case OR_INT_LIT16:
            case XOR_INT_LIT16:
                analyzeLiteralBinaryOp(analyzedInstruction, RegisterType.Category.Integer, true);
                return true;
            case ADD_INT_LIT8:
            case RSUB_INT_LIT8:
            case MUL_INT_LIT8:
            case DIV_INT_LIT8:
            case REM_INT_LIT8:
            case SHL_INT_LIT8:
                analyzeLiteralBinaryOp(analyzedInstruction, RegisterType.Category.Integer, false);
                return true;
            case AND_INT_LIT8:
            case OR_INT_LIT8:
            case XOR_INT_LIT8:
                analyzeLiteralBinaryOp(analyzedInstruction, RegisterType.Category.Integer, true);
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


    private void verifyInstruction(AnalyzedInstruction analyzedInstruction) {
        Instruction instruction = analyzedInstruction.instruction;

        switch (instruction.opcode) {
            case NOP:
                return;
            case MOVE:
            case MOVE_FROM16:
            case MOVE_16:
                verifyMove(analyzedInstruction, Primitive32BitCategories);
                return;
            case MOVE_WIDE:
            case MOVE_WIDE_FROM16:
            case MOVE_WIDE_16:
                verifyMove(analyzedInstruction, WideLowCategories);
                return;
            case MOVE_OBJECT:
            case MOVE_OBJECT_FROM16:
            case MOVE_OBJECT_16:
                verifyMove(analyzedInstruction, ReferenceOrUninitCategories);
                return;
            case MOVE_RESULT:
                verifyMoveResult(analyzedInstruction, Primitive32BitCategories);
                return;
            case MOVE_RESULT_WIDE:
                verifyMoveResult(analyzedInstruction, WideLowCategories);
                return;
            case MOVE_RESULT_OBJECT:
                verifyMoveResult(analyzedInstruction, ReferenceCategories);
                return;
            case MOVE_EXCEPTION:
                verifyMoveException(analyzedInstruction);
                return;
            case RETURN_VOID:
            case RETURN_VOID_BARRIER:
                verifyReturnVoid(analyzedInstruction);
                return;
            case RETURN:
                verifyReturn(analyzedInstruction, Primitive32BitCategories);
                return;
            case RETURN_WIDE:
                verifyReturn(analyzedInstruction, WideLowCategories);
                return;
            case RETURN_OBJECT:
                verifyReturn(analyzedInstruction, ReferenceCategories);
                return;
            case CONST_4:
            case CONST_16:
            case CONST:
            case CONST_HIGH16:
            case CONST_WIDE_16:
            case CONST_WIDE_32:
            case CONST_WIDE:
            case CONST_WIDE_HIGH16:
            case CONST_STRING:
            case CONST_STRING_JUMBO:
                return;
            case CONST_CLASS:
                verifyConstClass(analyzedInstruction);
                return;
            case MONITOR_ENTER:
            case MONITOR_EXIT:
                verifyMonitor(analyzedInstruction);
                return;
            case CHECK_CAST:
                verifyCheckCast(analyzedInstruction);
                return;
            case INSTANCE_OF:
                verifyInstanceOf(analyzedInstruction);
                return;
            case ARRAY_LENGTH:
                verifyArrayLength(analyzedInstruction);
                return;
            case NEW_INSTANCE:
                verifyNewInstance(analyzedInstruction);
                return;
            case NEW_ARRAY:
                verifyNewArray(analyzedInstruction);
                return;
            case FILLED_NEW_ARRAY:
                verifyFilledNewArray(analyzedInstruction);
                return;
            case FILLED_NEW_ARRAY_RANGE:
                verifyFilledNewArrayRange(analyzedInstruction);
                return;
            case FILL_ARRAY_DATA:
                verifyFillArrayData(analyzedInstruction);
                return;
            case THROW:
                verifyThrow(analyzedInstruction);
                return;
            case GOTO:
            case GOTO_16:
            case GOTO_32:
                return;
            case PACKED_SWITCH:
                verifySwitch(analyzedInstruction, Format.PackedSwitchData);
                return;
            case SPARSE_SWITCH:
                verifySwitch(analyzedInstruction, Format.SparseSwitchData);
                return;
            case CMPL_FLOAT:
            case CMPG_FLOAT:
                verifyFloatWideCmp(analyzedInstruction, Primitive32BitCategories);
                return;
            case CMPL_DOUBLE:
            case CMPG_DOUBLE:
            case CMP_LONG:
                verifyFloatWideCmp(analyzedInstruction, WideLowCategories);
                return;
            case IF_EQ:
            case IF_NE:
                verifyIfEqNe(analyzedInstruction);
                return;
            case IF_LT:
            case IF_GE:
            case IF_GT:
            case IF_LE:
                verifyIf(analyzedInstruction);
                return;
            case IF_EQZ:
            case IF_NEZ:
                verifyIfEqzNez(analyzedInstruction);
                return;
            case IF_LTZ:
            case IF_GEZ:
            case IF_GTZ:
            case IF_LEZ:
                verifyIfz(analyzedInstruction);
                return;
            case AGET:
                verify32BitPrimitiveAget(analyzedInstruction, RegisterType.Category.Integer);
                return;
            case AGET_BOOLEAN:
                verify32BitPrimitiveAget(analyzedInstruction, RegisterType.Category.Boolean);
                return;
            case AGET_BYTE:
                verify32BitPrimitiveAget(analyzedInstruction, RegisterType.Category.Byte);
                return;
            case AGET_CHAR:
                verify32BitPrimitiveAget(analyzedInstruction, RegisterType.Category.Char);
                return;
            case AGET_SHORT:
                verify32BitPrimitiveAget(analyzedInstruction, RegisterType.Category.Short);
                return;
            case AGET_WIDE:
                verifyAgetWide(analyzedInstruction);
                return;
            case AGET_OBJECT:
                verifyAgetObject(analyzedInstruction);
                return;
            case APUT:
                verify32BitPrimitiveAput(analyzedInstruction, RegisterType.Category.Integer);
                return;
            case APUT_BOOLEAN:
                verify32BitPrimitiveAput(analyzedInstruction, RegisterType.Category.Boolean);
                return;
            case APUT_BYTE:
                verify32BitPrimitiveAput(analyzedInstruction, RegisterType.Category.Byte);
                return;
            case APUT_CHAR:
                verify32BitPrimitiveAput(analyzedInstruction, RegisterType.Category.Char);
                return;
            case APUT_SHORT:
                verify32BitPrimitiveAput(analyzedInstruction, RegisterType.Category.Short);
                return;
            case APUT_WIDE:
                verifyAputWide(analyzedInstruction);
                return;
            case APUT_OBJECT:
                verifyAputObject(analyzedInstruction);
                return;
            case IGET:
                verify32BitPrimitiveIget(analyzedInstruction, RegisterType.Category.Integer);
                return;
            case IGET_BOOLEAN:
                verify32BitPrimitiveIget(analyzedInstruction, RegisterType.Category.Boolean);
                return;
            case IGET_BYTE:
                verify32BitPrimitiveIget(analyzedInstruction, RegisterType.Category.Byte);
                return;
            case IGET_CHAR:
                verify32BitPrimitiveIget(analyzedInstruction, RegisterType.Category.Char);
                return;
            case IGET_SHORT:
                verify32BitPrimitiveIget(analyzedInstruction, RegisterType.Category.Short);
                return;
            case IGET_WIDE:
                verifyIgetWide(analyzedInstruction);
                return;
            case IGET_OBJECT:
                verifyIgetObject(analyzedInstruction);
                return;
            case IPUT:
                verify32BitPrimitiveIput(analyzedInstruction, RegisterType.Category.Integer);
                return;
            case IPUT_BOOLEAN:
                verify32BitPrimitiveIput(analyzedInstruction, RegisterType.Category.Boolean);
                return;
            case IPUT_BYTE:
                verify32BitPrimitiveIput(analyzedInstruction, RegisterType.Category.Byte);
                return;
            case IPUT_CHAR:
                verify32BitPrimitiveIput(analyzedInstruction, RegisterType.Category.Char);
                return;
            case IPUT_SHORT:
                verify32BitPrimitiveIput(analyzedInstruction, RegisterType.Category.Short);
                return;
            case IPUT_WIDE:
                verifyIputWide(analyzedInstruction);
                return;
            case IPUT_OBJECT:
                verifyIputObject(analyzedInstruction);
                return;
            case SGET:
                verify32BitPrimitiveSget(analyzedInstruction, RegisterType.Category.Integer);
                return;
            case SGET_BOOLEAN:
                verify32BitPrimitiveSget(analyzedInstruction, RegisterType.Category.Boolean);
                return;
            case SGET_BYTE:
                verify32BitPrimitiveSget(analyzedInstruction, RegisterType.Category.Byte);
                return;
            case SGET_CHAR:
                verify32BitPrimitiveSget(analyzedInstruction, RegisterType.Category.Char);
                return;
            case SGET_SHORT:
                verify32BitPrimitiveSget(analyzedInstruction, RegisterType.Category.Short);
                return;
            case SGET_WIDE:
                verifySgetWide(analyzedInstruction);
                return;
            case SGET_OBJECT:
                verifySgetObject(analyzedInstruction);
                return;
            case SPUT:
                verify32BitPrimitiveSput(analyzedInstruction, RegisterType.Category.Integer);
                return;
            case SPUT_BOOLEAN:
                verify32BitPrimitiveSput(analyzedInstruction, RegisterType.Category.Boolean);
                return;
            case SPUT_BYTE:
                verify32BitPrimitiveSput(analyzedInstruction, RegisterType.Category.Byte);
                return;
            case SPUT_CHAR:
                verify32BitPrimitiveSput(analyzedInstruction, RegisterType.Category.Char);
                return;
            case SPUT_SHORT:
                verify32BitPrimitiveSput(analyzedInstruction, RegisterType.Category.Short);
                return;
            case SPUT_WIDE:
                verifySputWide(analyzedInstruction);
                return;
            case SPUT_OBJECT:
                verifySputObject(analyzedInstruction);
                return;
            case INVOKE_VIRTUAL:
                verifyInvoke(analyzedInstruction, INVOKE_VIRTUAL);
                return;
            case INVOKE_SUPER:
                verifyInvoke(analyzedInstruction, INVOKE_SUPER);
                return;
            case INVOKE_DIRECT:
                verifyInvoke(analyzedInstruction, INVOKE_DIRECT);
                return;
            case INVOKE_STATIC:
                verifyInvoke(analyzedInstruction, INVOKE_STATIC);
                return;
            case INVOKE_INTERFACE:
                verifyInvoke(analyzedInstruction, INVOKE_INTERFACE);
                return;
            case INVOKE_VIRTUAL_RANGE:
                verifyInvokeRange(analyzedInstruction, INVOKE_VIRTUAL);
                return;
            case INVOKE_SUPER_RANGE:
                verifyInvokeRange(analyzedInstruction, INVOKE_SUPER);
                return;
            case INVOKE_DIRECT_RANGE:
                verifyInvokeRange(analyzedInstruction, INVOKE_DIRECT);
                return;
            case INVOKE_STATIC_RANGE:
                verifyInvokeRange(analyzedInstruction, INVOKE_STATIC);
                return;
            case INVOKE_INTERFACE_RANGE:
                verifyInvokeRange(analyzedInstruction, INVOKE_INTERFACE);
                return;
            case NEG_INT:
            case NOT_INT:
                verifyUnaryOp(analyzedInstruction, Primitive32BitCategories);
                return;
            case NEG_LONG:
            case NOT_LONG:
                verifyUnaryOp(analyzedInstruction, WideLowCategories);
                return;
            case NEG_FLOAT:
                verifyUnaryOp(analyzedInstruction, Primitive32BitCategories);
                return;
            case NEG_DOUBLE:
                verifyUnaryOp(analyzedInstruction, WideLowCategories);
                return;
            case INT_TO_LONG:
                verifyUnaryOp(analyzedInstruction, Primitive32BitCategories);
                return;
            case INT_TO_FLOAT:
                verifyUnaryOp(analyzedInstruction, Primitive32BitCategories);
                return;
            case INT_TO_DOUBLE:
                verifyUnaryOp(analyzedInstruction, Primitive32BitCategories);
                return;
            case LONG_TO_INT:
            case DOUBLE_TO_INT:
                verifyUnaryOp(analyzedInstruction, WideLowCategories);
                return;
            case LONG_TO_FLOAT:
            case DOUBLE_TO_FLOAT:
                verifyUnaryOp(analyzedInstruction, WideLowCategories);
                return;
            case LONG_TO_DOUBLE:
                verifyUnaryOp(analyzedInstruction, WideLowCategories);
                return;
            case FLOAT_TO_INT:
                verifyUnaryOp(analyzedInstruction, Primitive32BitCategories);
                return;
            case FLOAT_TO_LONG:
                verifyUnaryOp(analyzedInstruction, Primitive32BitCategories);
                return;
            case FLOAT_TO_DOUBLE:
                verifyUnaryOp(analyzedInstruction, Primitive32BitCategories);
                return;
            case DOUBLE_TO_LONG:
                verifyUnaryOp(analyzedInstruction, WideLowCategories);
                return;
            case INT_TO_BYTE:
                verifyUnaryOp(analyzedInstruction, Primitive32BitCategories);
                return;
            case INT_TO_CHAR:
                verifyUnaryOp(analyzedInstruction, Primitive32BitCategories);
                return;
            case INT_TO_SHORT:
                verifyUnaryOp(analyzedInstruction, Primitive32BitCategories);
                return;
            case ADD_INT:
            case SUB_INT:
            case MUL_INT:
            case DIV_INT:
            case REM_INT:
            case SHL_INT:
            case SHR_INT:
            case USHR_INT:
            case AND_INT:
            case OR_INT:
            case XOR_INT:
                verifyBinaryOp(analyzedInstruction, Primitive32BitCategories, Primitive32BitCategories);
                return;
            case ADD_LONG:
            case SUB_LONG:
            case MUL_LONG:
            case DIV_LONG:
            case REM_LONG:
            case AND_LONG:
            case OR_LONG:
            case XOR_LONG:
                verifyBinaryOp(analyzedInstruction, WideLowCategories, WideLowCategories);
                return;
            case SHL_LONG:
            case SHR_LONG:
            case USHR_LONG:
                verifyBinaryOp(analyzedInstruction, WideLowCategories, Primitive32BitCategories);
                return;
            case ADD_FLOAT:
            case SUB_FLOAT:
            case MUL_FLOAT:
            case DIV_FLOAT:
            case REM_FLOAT:
                verifyBinaryOp(analyzedInstruction, Primitive32BitCategories, Primitive32BitCategories);
                return;
            case ADD_DOUBLE:
            case SUB_DOUBLE:
            case MUL_DOUBLE:
            case DIV_DOUBLE:
            case REM_DOUBLE:
                verifyBinaryOp(analyzedInstruction, WideLowCategories, WideLowCategories);
                return;
            case ADD_INT_2ADDR:
            case SUB_INT_2ADDR:
            case MUL_INT_2ADDR:
            case DIV_INT_2ADDR:
            case REM_INT_2ADDR:
            case SHL_INT_2ADDR:
            case SHR_INT_2ADDR:
            case USHR_INT_2ADDR:
            case AND_INT_2ADDR:
            case OR_INT_2ADDR:
            case XOR_INT_2ADDR:
                verifyBinary2AddrOp(analyzedInstruction, Primitive32BitCategories, Primitive32BitCategories);
                return;
            case ADD_LONG_2ADDR:
            case SUB_LONG_2ADDR:
            case MUL_LONG_2ADDR:
            case DIV_LONG_2ADDR:
            case REM_LONG_2ADDR:
            case AND_LONG_2ADDR:
            case OR_LONG_2ADDR:
            case XOR_LONG_2ADDR:
                verifyBinary2AddrOp(analyzedInstruction, WideLowCategories, WideLowCategories);
                return;
            case SHL_LONG_2ADDR:
            case SHR_LONG_2ADDR:
            case USHR_LONG_2ADDR:
                verifyBinary2AddrOp(analyzedInstruction, WideLowCategories, Primitive32BitCategories);
                return;
            case ADD_FLOAT_2ADDR:
            case SUB_FLOAT_2ADDR:
            case MUL_FLOAT_2ADDR:
            case DIV_FLOAT_2ADDR:
            case REM_FLOAT_2ADDR:
                verifyBinary2AddrOp(analyzedInstruction, Primitive32BitCategories, Primitive32BitCategories);
                return;
            case ADD_DOUBLE_2ADDR:
            case SUB_DOUBLE_2ADDR:
            case MUL_DOUBLE_2ADDR:
            case DIV_DOUBLE_2ADDR:
            case REM_DOUBLE_2ADDR:
                verifyBinary2AddrOp(analyzedInstruction, WideLowCategories, WideLowCategories);
                return;
            case ADD_INT_LIT16:
            case RSUB_INT:
            case MUL_INT_LIT16:
            case DIV_INT_LIT16:
            case REM_INT_LIT16:
                verifyLiteralBinaryOp(analyzedInstruction);
                return;
            case AND_INT_LIT16:
            case OR_INT_LIT16:
            case XOR_INT_LIT16:
                verifyLiteralBinaryOp(analyzedInstruction);
                return;
            case ADD_INT_LIT8:
            case RSUB_INT_LIT8:
            case MUL_INT_LIT8:
            case DIV_INT_LIT8:
            case REM_INT_LIT8:
            case SHL_INT_LIT8:
                verifyLiteralBinaryOp(analyzedInstruction);
                return;
            case AND_INT_LIT8:
            case OR_INT_LIT8:
            case XOR_INT_LIT8:
                verifyLiteralBinaryOp(analyzedInstruction);
                return;
            case SHR_INT_LIT8:
                verifyLiteralBinaryOp(analyzedInstruction);
                return;
            case USHR_INT_LIT8:
                verifyLiteralBinaryOp(analyzedInstruction);
                return;
            case IGET_VOLATILE:
            case IPUT_VOLATILE:
            case SGET_VOLATILE:
            case SPUT_VOLATILE:
            case IGET_OBJECT_VOLATILE:
            case IGET_WIDE_VOLATILE:
            case IPUT_WIDE_VOLATILE:
            case SGET_WIDE_VOLATILE:
            case SPUT_WIDE_VOLATILE:
            case THROW_VERIFICATION_ERROR:
            case EXECUTE_INLINE:
            case EXECUTE_INLINE_RANGE:
            case INVOKE_DIRECT_EMPTY:
            case INVOKE_OBJECT_INIT_RANGE:
            case IGET_QUICK:
            case IGET_WIDE_QUICK:
            case IGET_OBJECT_QUICK:
            case IPUT_QUICK:
            case IPUT_WIDE_QUICK:
            case IPUT_OBJECT_QUICK:
            case INVOKE_VIRTUAL_QUICK:
            case INVOKE_SUPER_QUICK:
            case INVOKE_VIRTUAL_QUICK_RANGE:
            case INVOKE_SUPER_QUICK_RANGE:
            case IPUT_OBJECT_VOLATILE:
            case SGET_OBJECT_VOLATILE:
            case SPUT_OBJECT_VOLATILE:
                //TODO: throw validation exception?
            default:
                assert false;
                return;
        }
    }

    private static final EnumSet<RegisterType.Category> Primitive32BitCategories = EnumSet.of(
            RegisterType.Category.Null,
            RegisterType.Category.One,
            RegisterType.Category.Boolean,
            RegisterType.Category.Byte,
            RegisterType.Category.PosByte,
            RegisterType.Category.Short,
            RegisterType.Category.PosShort,
            RegisterType.Category.Char,
            RegisterType.Category.Integer,
            RegisterType.Category.Float);

    private static final EnumSet<RegisterType.Category> WideLowCategories = EnumSet.of(
            RegisterType.Category.LongLo,
            RegisterType.Category.DoubleLo);

    private static final EnumSet<RegisterType.Category> WideHighCategories = EnumSet.of(
            RegisterType.Category.LongHi,
            RegisterType.Category.DoubleHi);

    private static final EnumSet<RegisterType.Category> ReferenceCategories = EnumSet.of(
            RegisterType.Category.Null,
            RegisterType.Category.Reference);

    private static final EnumSet<RegisterType.Category> ReferenceOrUninitThisCategories = EnumSet.of(
            RegisterType.Category.Null,
            RegisterType.Category.UninitThis,
            RegisterType.Category.Reference);

    private static final EnumSet<RegisterType.Category> ReferenceOrUninitCategories = EnumSet.of(
            RegisterType.Category.Null,
            RegisterType.Category.UninitRef,
            RegisterType.Category.UninitThis,
            RegisterType.Category.Reference);

    private static final EnumSet<RegisterType.Category> ReferenceAndPrimitive32BitCategories = EnumSet.of(
            RegisterType.Category.Null,
            RegisterType.Category.One,
            RegisterType.Category.Boolean,
            RegisterType.Category.Byte,
            RegisterType.Category.PosByte,
            RegisterType.Category.Short,
            RegisterType.Category.PosShort,
            RegisterType.Category.Char,
            RegisterType.Category.Integer,
            RegisterType.Category.Float,
            RegisterType.Category.Reference);

    private static final EnumSet<RegisterType.Category> BooleanCategories = EnumSet.of(
            RegisterType.Category.Null,
            RegisterType.Category.One,
            RegisterType.Category.Boolean);

    private void analyzeMove(AnalyzedInstruction analyzedInstruction) {
        TwoRegisterInstruction instruction = (TwoRegisterInstruction)analyzedInstruction.instruction;

        RegisterType sourceRegisterType = analyzedInstruction.getPreInstructionRegisterType(instruction.getRegisterB());
        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction, sourceRegisterType);
    }

    private void verifyMove(AnalyzedInstruction analyzedInstruction, EnumSet validCategories) {
        TwoRegisterInstruction instruction = (TwoRegisterInstruction)analyzedInstruction.instruction;

        getAndCheckSourceRegister(analyzedInstruction, instruction.getRegisterB(), validCategories);
    }

    private void analyzeMoveResult(AnalyzedInstruction analyzedInstruction) {
        AnalyzedInstruction previousInstruction = instructions.valueAt(analyzedInstruction.instructionIndex-1);
        if (!previousInstruction.instruction.opcode.setsResult()) {
            throw new ValidationException(analyzedInstruction.instruction.opcode.name + " must occur after an " +
                    "invoke-*/fill-new-array instruction");
        }

        RegisterType resultRegisterType;
        InstructionWithReference invokeInstruction = (InstructionWithReference)previousInstruction.instruction;
        Item item = invokeInstruction.getReferencedItem();

        if (item.getItemType() == ItemType.TYPE_METHOD_ID_ITEM) {
            resultRegisterType = RegisterType.getRegisterTypeForTypeIdItem(
                    ((MethodIdItem)item).getPrototype().getReturnType());
        } else {
            assert item.getItemType() == ItemType.TYPE_TYPE_ID_ITEM;
            resultRegisterType = RegisterType.getRegisterTypeForTypeIdItem((TypeIdItem)item);
        }

        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction, resultRegisterType);
    }

    private void verifyMoveResult(AnalyzedInstruction analyzedInstruction,
                                     EnumSet<RegisterType.Category> allowedCategories) {
        if (analyzedInstruction.instructionIndex == 0) {
            throw new ValidationException(analyzedInstruction.instruction.opcode.name + " cannot be the first " +
                    "instruction in a method. It must occur after an invoke-*/fill-new-array instruction");
        }

        AnalyzedInstruction previousInstruction = instructions.valueAt(analyzedInstruction.instructionIndex-1);

        if (!previousInstruction.instruction.opcode.setsResult()) {
            throw new ValidationException(analyzedInstruction.instruction.opcode.name + " must occur after an " +
                    "invoke-*/fill-new-array instruction");
        }

        //TODO: does dalvik allow a move-result after an invoke with a void return type?
        RegisterType resultRegisterType;

        InstructionWithReference invokeInstruction = (InstructionWithReference)previousInstruction.getInstruction();
        Item item = invokeInstruction.getReferencedItem();

        if (item instanceof MethodIdItem) {
            resultRegisterType = RegisterType.getRegisterTypeForTypeIdItem(
                    ((MethodIdItem)item).getPrototype().getReturnType());
        } else {
            assert item instanceof TypeIdItem;
            resultRegisterType = RegisterType.getRegisterTypeForTypeIdItem((TypeIdItem)item);
        }

        if (!allowedCategories.contains(resultRegisterType.category)) {
            throw new ValidationException(String.format("Wrong move-result* instruction for return value %s",
                    resultRegisterType.toString()));
        }
    }

    private void analyzeMoveException(AnalyzedInstruction analyzedInstruction) {
        CodeItem.TryItem[] tries = encodedMethod.codeItem.getTries();
        int instructionAddress = getInstructionAddress(analyzedInstruction);

        if (tries == null) {
            throw new ValidationException("move-exception must be the first instruction in an exception handler block");
        }

        RegisterType exceptionType = null;

        for (CodeItem.TryItem tryItem: encodedMethod.codeItem.getTries()) {
            if (tryItem.encodedCatchHandler.getCatchAllHandlerAddress() == instructionAddress) {
                exceptionType = RegisterType.getRegisterType(RegisterType.Category.Reference,
                        ClassPath.getClassDef("Ljava/lang/Throwable;"));
                break;
            }
            for (CodeItem.EncodedTypeAddrPair handler: tryItem.encodedCatchHandler.handlers) {
                if (handler.getHandlerAddress() == instructionAddress) {
                    exceptionType = RegisterType.getRegisterTypeForTypeIdItem(handler.exceptionType)
                            .merge(exceptionType);
                }
            }
        }

        if (exceptionType == null) {
            throw new ValidationException("move-exception must be the first instruction in an exception handler block");
        }

        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction, exceptionType);
    }

    private void verifyMoveException(AnalyzedInstruction analyzedInstruction) {
        CodeItem.TryItem[] tries = encodedMethod.codeItem.getTries();
        int instructionAddress = getInstructionAddress(analyzedInstruction);

        if (tries == null) {
            throw new ValidationException("move-exception must be the first instruction in an exception handler block");
        }

        RegisterType exceptionType = null;

        for (CodeItem.TryItem tryItem: encodedMethod.codeItem.getTries()) {
            if (tryItem.encodedCatchHandler.getCatchAllHandlerAddress() == instructionAddress) {
                exceptionType = RegisterType.getRegisterType(RegisterType.Category.Reference,
                        ClassPath.getClassDef("Ljava/lang/Throwable;"));
                break;
            }
            for (CodeItem.EncodedTypeAddrPair handler: tryItem.encodedCatchHandler.handlers) {
                if (handler.getHandlerAddress() == instructionAddress) {
                    exceptionType = RegisterType.getRegisterTypeForTypeIdItem(handler.exceptionType)
                            .merge(exceptionType);
                }
            }
        }

        if (exceptionType == null) {
            throw new ValidationException("move-exception must be the first instruction in an exception handler block");
        }

        //TODO: check if the type is a throwable. Should we throw a ValidationException or print a warning? (does dalvik validate that it's a throwable? It doesn't in CodeVerify.c, but it might check in DexSwapVerify.c)
        if (exceptionType.category != RegisterType.Category.Reference) {
            throw new ValidationException(String.format("Exception type %s is not a reference type",
                    exceptionType.toString()));
        }
    }

    private void analyzeReturnVoidBarrier(AnalyzedInstruction analyzedInstruction) {
        analyzeReturnVoidBarrier(analyzedInstruction, true);
    }

    private void analyzeReturnVoidBarrier(AnalyzedInstruction analyzedInstruction, boolean analyzeResult) {
        Instruction10x instruction = (Instruction10x)analyzedInstruction.instruction;

        Instruction10x deodexedInstruction = new Instruction10x(Opcode.RETURN_VOID);

        analyzedInstruction.setDeodexedInstruction(deodexedInstruction);

        if (analyzeResult) {
            analyzeInstruction(analyzedInstruction);
        }
    }

    private void verifyReturnVoid(AnalyzedInstruction analyzedInstruction) {
        TypeIdItem returnType = encodedMethod.method.getPrototype().getReturnType();
        if (returnType.getTypeDescriptor().charAt(0) != 'V') {
            //TODO: could add which return-* variation should be used instead
            throw new ValidationException("Cannot use return-void with a non-void return type (" +
                returnType.getTypeDescriptor() + ")");
        }
    }

    private void verifyReturn(AnalyzedInstruction analyzedInstruction, EnumSet validCategories) {
        /*if (this.isInstanceConstructor()) {
            checkConstructorReturn(analyzedInstruction);
        }*/

        SingleRegisterInstruction instruction = (SingleRegisterInstruction)analyzedInstruction.instruction;
        int returnRegister = instruction.getRegisterA();
        RegisterType returnRegisterType = getAndCheckSourceRegister(analyzedInstruction, returnRegister,
                validCategories);

        TypeIdItem returnType = encodedMethod.method.getPrototype().getReturnType();
        if (returnType.getTypeDescriptor().charAt(0) == 'V') {
            throw new ValidationException("Cannot use return with a void return type. Use return-void instead");
        }

        RegisterType methodReturnRegisterType = RegisterType.getRegisterTypeForTypeIdItem(returnType);

        if (!validCategories.contains(methodReturnRegisterType.category)) {
            //TODO: could add which return-* variation should be used instead
            throw new ValidationException(String.format("Cannot use %s with return type %s",
                    analyzedInstruction.instruction.opcode.name, returnType.getTypeDescriptor()));
        }

        if (validCategories == ReferenceCategories) {
            if (methodReturnRegisterType.type.isInterface()) {
                if (returnRegisterType.category != RegisterType.Category.Null &&
                    !returnRegisterType.type.implementsInterface(methodReturnRegisterType.type)) {
                    //TODO: how to handle warnings?
                }
            } else {
                if (returnRegisterType.category == RegisterType.Category.Reference &&
                    !returnRegisterType.type.extendsClass(methodReturnRegisterType.type)) {

                    throw new ValidationException(String.format("The return value in register v%d (%s) is not " +
                            "compatible with the method's return type %s", returnRegister,
                            returnRegisterType.type.getClassType(), methodReturnRegisterType.type.getClassType()));
                }
            }
        }
    }

    private void analyzeConst(AnalyzedInstruction analyzedInstruction) {
        LiteralInstruction instruction = (LiteralInstruction)analyzedInstruction.instruction;

        RegisterType newDestinationRegisterType = RegisterType.getRegisterTypeForLiteral(instruction.getLiteral());

        //we assume that the literal value is a valid value for the given instruction type, because it's impossible
        //to store an invalid literal with the instruction. so we don't need to check the type of the literal
        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction, newDestinationRegisterType);
    }

    private void analyzeConstHigh16(AnalyzedInstruction analyzedInstruction) {
        //the literal value stored in the instruction is a 16-bit value. When shifted left by 16, it will always be an
        //integer
        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction,
                RegisterType.getRegisterType(RegisterType.Category.Integer, null));
    }

    private void analyzeWideConst(AnalyzedInstruction analyzedInstruction) {
        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction,
                RegisterType.getRegisterType(RegisterType.Category.LongLo, null));
    }

    private void analyzeConstString(AnalyzedInstruction analyzedInstruction) {
        ClassPath.ClassDef stringClassDef = ClassPath.getClassDef("Ljava/lang/String;");
        RegisterType stringType = RegisterType.getRegisterType(RegisterType.Category.Reference, stringClassDef);
        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction, stringType);
    }

    private void analyzeConstClass(AnalyzedInstruction analyzedInstruction) {
        ClassPath.ClassDef classClassDef = ClassPath.getClassDef("Ljava/lang/Class;");
        RegisterType classType = RegisterType.getRegisterType(RegisterType.Category.Reference, classClassDef);

        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction, classType);
    }


    private void verifyConstClass(AnalyzedInstruction analyzedInstruction) {
        ClassPath.ClassDef classClassDef = ClassPath.getClassDef("Ljava/lang/Class;");
        RegisterType classType = RegisterType.getRegisterType(RegisterType.Category.Reference, classClassDef);

        InstructionWithReference instruction = (InstructionWithReference)analyzedInstruction.instruction;
        Item item = instruction.getReferencedItem();
        assert item.getItemType() == ItemType.TYPE_TYPE_ID_ITEM;

        //TODO: need to check class access
        //make sure the referenced class is resolvable
        ClassPath.getClassDef((TypeIdItem)item);
    }

    private void verifyMonitor(AnalyzedInstruction analyzedInstruction) {
        SingleRegisterInstruction instruction = (SingleRegisterInstruction)analyzedInstruction.instruction;
        getAndCheckSourceRegister(analyzedInstruction, instruction.getRegisterA(), ReferenceCategories);
    }

    private void analyzeCheckCast(AnalyzedInstruction analyzedInstruction) {
        InstructionWithReference instruction = (InstructionWithReference)analyzedInstruction.instruction;

        Item item = instruction.getReferencedItem();
        assert item.getItemType() == ItemType.TYPE_TYPE_ID_ITEM;

        RegisterType castRegisterType = RegisterType.getRegisterTypeForTypeIdItem((TypeIdItem)item);
        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction, castRegisterType);
    }

    private void verifyCheckCast(AnalyzedInstruction analyzedInstruction) {
        {
            //ensure the "source" register is a reference type
            SingleRegisterInstruction instruction = (SingleRegisterInstruction)analyzedInstruction.instruction;

            RegisterType registerType = getAndCheckSourceRegister(analyzedInstruction, instruction.getRegisterA(),
                    ReferenceCategories);
        }

        {
            //resolve and verify the class that we're casting to
            InstructionWithReference instruction = (InstructionWithReference)analyzedInstruction.instruction;

            Item item = instruction.getReferencedItem();
            assert item.getItemType() == ItemType.TYPE_TYPE_ID_ITEM;

            //TODO: need to check class access
            RegisterType castRegisterType = RegisterType.getRegisterTypeForTypeIdItem((TypeIdItem)item);
            if (castRegisterType.category != RegisterType.Category.Reference) {
                //TODO: verify that dalvik allows a non-reference type..
                //TODO: print a warning, but don't re-throw the exception. dalvik allows a non-reference type during validation (but throws an exception at runtime)
            }
        }
    }

    private void analyzeInstanceOf(AnalyzedInstruction analyzedInstruction) {
        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction,
                RegisterType.getRegisterType(RegisterType.Category.Boolean, null));
    }

    private void verifyInstanceOf(AnalyzedInstruction analyzedInstruction) {
        {
            //ensure the register that is being checks is a reference type
            TwoRegisterInstruction instruction = (TwoRegisterInstruction)analyzedInstruction.instruction;

            getAndCheckSourceRegister(analyzedInstruction, instruction.getRegisterB(), ReferenceCategories);
        }

        {
            //resolve and verify the class that we're checking against
            InstructionWithReference instruction = (InstructionWithReference)analyzedInstruction.instruction;

            Item item = instruction.getReferencedItem();
            assert  item.getItemType() == ItemType.TYPE_TYPE_ID_ITEM;
            RegisterType registerType = RegisterType.getRegisterTypeForTypeIdItem((TypeIdItem)item);
            if (registerType.category != RegisterType.Category.Reference) {
                throw new ValidationException(String.format("Cannot use instance-of with a non-reference type %s",
                        registerType.toString()));
            }

            //TODO: is it valid to use an array type?
            //TODO: could probably do an even more sophisticated check, where we check the possible register types against the specified type. In some cases, we could determine that it always fails, and print a warning to that effect.
        }
    }

    private void analyzeArrayLength(AnalyzedInstruction analyzedInstruction) {
        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction,
                RegisterType.getRegisterType(RegisterType.Category.Integer, null));
    }

    private void verifyArrayLength(AnalyzedInstruction analyzedInstruction) {
        TwoRegisterInstruction instruction = (TwoRegisterInstruction)analyzedInstruction.instruction;

        int arrayRegisterNumber = instruction.getRegisterB();
        RegisterType arrayRegisterType = getAndCheckSourceRegister(analyzedInstruction, arrayRegisterNumber,
                ReferenceCategories);

        if (arrayRegisterType.type != null) {
            if (arrayRegisterType.type.getClassType().charAt(0) != '[') {
                throw new ValidationException(String.format("Cannot use array-length with non-array type %s",
                        arrayRegisterType.type.getClassType()));
            }
            assert arrayRegisterType.type instanceof ClassPath.ArrayClassDef;
        }
    }

    private void analyzeNewInstance(AnalyzedInstruction analyzedInstruction) {
        InstructionWithReference instruction = (InstructionWithReference)analyzedInstruction.instruction;

        int register = ((SingleRegisterInstruction)analyzedInstruction.instruction).getRegisterA();
        RegisterType destRegisterType = analyzedInstruction.getPostInstructionRegisterType(register);
        if (destRegisterType.category != RegisterType.Category.Unknown) {
            assert destRegisterType.category == RegisterType.Category.UninitRef;

            //the post-instruction destination register will only be set if we have already analyzed this instruction
            //at least once. If this is the case, then the uninit reference has already been propagated to all
            //successors and nothing else needs to be done.
            return;
        }

        Item item = instruction.getReferencedItem();
        assert item.getItemType() == ItemType.TYPE_TYPE_ID_ITEM;

        RegisterType classType = RegisterType.getRegisterTypeForTypeIdItem((TypeIdItem)item);

        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction,
                RegisterType.getUnitializedReference(classType.type));
    }

    private void verifyNewInstance(AnalyzedInstruction analyzedInstruction) {
        InstructionWithReference instruction = (InstructionWithReference)analyzedInstruction.instruction;

        int register = ((SingleRegisterInstruction)analyzedInstruction.instruction).getRegisterA();
        RegisterType destRegisterType = analyzedInstruction.postRegisterMap[register];
        if (destRegisterType.category != RegisterType.Category.Unknown) {
            assert destRegisterType.category == RegisterType.Category.UninitRef;

            //the "post-instruction" destination register will only be set if we've gone over
            //this instruction at least once before. If this is the case, then we need to check
            //all the other registers, and make sure that none of them contain the same
            //uninitialized reference that is in the destination register.

            for (int i=0; i<analyzedInstruction.postRegisterMap.length; i++) {
                if (i==register) {
                    continue;
                }

                if (analyzedInstruction.getPreInstructionRegisterType(i) == destRegisterType) {
                    throw new ValidationException(String.format("Register v%d contains an uninitialized reference " +
                            "that was created by this new-instance instruction.", i));
                }
            }

            return;
        }

        Item item = instruction.getReferencedItem();
        assert item.getItemType() == ItemType.TYPE_TYPE_ID_ITEM;

        //TODO: need to check class access
        RegisterType classType = RegisterType.getRegisterTypeForTypeIdItem((TypeIdItem)item);
        if (classType.category != RegisterType.Category.Reference) {
            throw new ValidationException(String.format("Cannot use new-instance with a non-reference type %s",
                    classType.toString()));
        }

        if (((TypeIdItem)item).getTypeDescriptor().charAt(0) == '[') {
            throw new ValidationException("Cannot use array type \"" + ((TypeIdItem)item).getTypeDescriptor() +
                    "\" with new-instance. Use new-array instead.");
        }
    }

    private void analyzeNewArray(AnalyzedInstruction analyzedInstruction) {
        InstructionWithReference instruction = (InstructionWithReference)analyzedInstruction.instruction;

        Item item = instruction.getReferencedItem();
        assert item.getItemType() == ItemType.TYPE_TYPE_ID_ITEM;

        RegisterType arrayType = RegisterType.getRegisterTypeForTypeIdItem((TypeIdItem)item);
        assert arrayType.type instanceof ClassPath.ArrayClassDef;

        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction, arrayType);
    }

    private void verifyNewArray(AnalyzedInstruction analyzedInstruction) {
        {
            TwoRegisterInstruction instruction = (TwoRegisterInstruction)analyzedInstruction.instruction;
            getAndCheckSourceRegister(analyzedInstruction, instruction.getRegisterB(), Primitive32BitCategories);
        }

        InstructionWithReference instruction = (InstructionWithReference)analyzedInstruction.instruction;

        Item item = instruction.getReferencedItem();
        assert item.getItemType() == ItemType.TYPE_TYPE_ID_ITEM;

        RegisterType arrayType = RegisterType.getRegisterTypeForTypeIdItem((TypeIdItem)item);
        assert arrayType.type instanceof ClassPath.ArrayClassDef;

        if (arrayType.category != RegisterType.Category.Reference) {
            throw new ValidationException(String.format("Cannot use new-array with a non-reference type %s",
                    arrayType.toString()));
        }
        if (arrayType.type.getClassType().charAt(0) != '[') {
            throw new ValidationException("Cannot use non-array type \"" + arrayType.type.getClassType() +
                    "\" with new-array. Use new-instance instead.");
        }
    }

    private void verifyFilledNewArrayCommon(AnalyzedInstruction analyzedInstruction,
                                               RegisterIterator registerIterator) {
        InstructionWithReference instruction = (InstructionWithReference)analyzedInstruction.instruction;

        RegisterType arrayType;
        RegisterType arrayImmediateElementType;

        Item item = instruction.getReferencedItem();
        assert  item.getItemType() == ItemType.TYPE_TYPE_ID_ITEM;

        ClassPath.ClassDef classDef = ClassPath.getClassDef((TypeIdItem)item);

        if (classDef.getClassType().charAt(0) != '[') {
            throw new ValidationException("Cannot use non-array type \"" + classDef.getClassType() +
                "\" with new-array. Use new-instance instead.");
        }

        ClassPath.ArrayClassDef arrayClassDef = (ClassPath.ArrayClassDef)classDef;
        arrayType = RegisterType.getRegisterType(RegisterType.Category.Reference, classDef);
        arrayImmediateElementType = RegisterType.getRegisterTypeForType(
                arrayClassDef.getImmediateElementClass().getClassType());
        String baseElementType = arrayClassDef.getBaseElementClass().getClassType();
        if (baseElementType.charAt(0) == 'J' || baseElementType.charAt(0) == 'D') {
            throw new ValidationException("Cannot use filled-new-array to create an array of wide values " +
                    "(long or double)");
        }

        do {
            int register = registerIterator.getRegister();
            RegisterType elementType = analyzedInstruction.getPreInstructionRegisterType(register);
            assert elementType != null;

            if (!elementType.canBeAssignedTo(arrayImmediateElementType)) {
                throw new ValidationException("Register v" + Integer.toString(register) + " is of type " +
                        elementType.toString() + " and is incompatible with the array type " +
                        arrayType.type.getClassType());
            }
        } while (registerIterator.moveNext());
    }

    private void verifyFilledNewArray(AnalyzedInstruction analyzedInstruction) {
        FiveRegisterInstruction instruction = (FiveRegisterInstruction)analyzedInstruction.instruction;
        verifyFilledNewArrayCommon(analyzedInstruction, new Format35cRegisterIterator(instruction));
    }

    private void verifyFilledNewArrayRange(AnalyzedInstruction analyzedInstruction) {
        RegisterRangeInstruction instruction = (RegisterRangeInstruction)analyzedInstruction.instruction;

        //instruction.getStartRegister() and instruction.getRegCount() both return an int value, but are actually
        //unsigned 16 bit values, so we don't have to worry about overflowing an int when adding them together
        if (instruction.getStartRegister() + instruction.getRegCount() >= 1<<16) {
            throw new ValidationException(String.format("Invalid register range {v%d .. v%d}. The ending register " +
                    "is larger than the largest allowed register of v65535.",
                    instruction.getStartRegister(),
                    instruction.getStartRegister() + instruction.getRegCount() - 1));
        }

        verifyFilledNewArrayCommon(analyzedInstruction, new Format3rcRegisterIterator(instruction));
    }

    private void verifyFillArrayData(AnalyzedInstruction analyzedInstruction) {
        SingleRegisterInstruction instruction = (SingleRegisterInstruction)analyzedInstruction.instruction;

        int register = instruction.getRegisterA();
        RegisterType registerType = analyzedInstruction.getPreInstructionRegisterType(register);
        assert registerType != null;

        if (registerType.category == RegisterType.Category.Null) {
            return;
        }

        if (registerType.category != RegisterType.Category.Reference) {
            throw new ValidationException(String.format("Cannot use fill-array-data with non-array register v%d of " +
                    "type %s", register, registerType.toString()));
        }

        assert registerType.type instanceof ClassPath.ArrayClassDef;
        ClassPath.ArrayClassDef arrayClassDef = (ClassPath.ArrayClassDef)registerType.type;

        if (arrayClassDef.getArrayDimensions() != 1) {
            throw new ValidationException(String.format("Cannot use fill-array-data with array type %s. It can only " +
                    "be used with a one-dimensional array of primitives.", arrayClassDef.getClassType()));
        }

        int elementWidth;
        switch (arrayClassDef.getBaseElementClass().getClassType().charAt(0)) {
            case 'Z':
            case 'B':
                elementWidth = 1;
                break;
            case 'C':
            case 'S':
                elementWidth = 2;
                break;
            case 'I':
            case 'F':
                elementWidth = 4;
                break;
            case 'J':
            case 'D':
                elementWidth = 8;
                break;
            default:
                throw new ValidationException(String.format("Cannot use fill-array-data with array type %s. It can " +
                        "only be used with a one-dimensional array of primitives.", arrayClassDef.getClassType()));
        }


        int arrayDataAddressOffset = ((OffsetInstruction)analyzedInstruction.instruction).getTargetAddressOffset();
        int arrayDataCodeAddress = getInstructionAddress(analyzedInstruction) + arrayDataAddressOffset;
        AnalyzedInstruction arrayDataInstruction = this.instructions.get(arrayDataCodeAddress);
        if (arrayDataInstruction == null || arrayDataInstruction.instruction.getFormat() != Format.ArrayData) {
            throw new ValidationException(String.format("Could not find an array data structure at code address 0x%x",
                    arrayDataCodeAddress));
        }

        ArrayDataPseudoInstruction arrayDataPseudoInstruction =
                (ArrayDataPseudoInstruction)arrayDataInstruction.instruction;

        if (elementWidth != arrayDataPseudoInstruction.getElementWidth()) {
            throw new ValidationException(String.format("The array data at code address 0x%x does not have the " +
                    "correct element width for array type %s. Expecting element width %d, got element width %d.",
                    arrayDataCodeAddress, arrayClassDef.getClassType(), elementWidth,
                    arrayDataPseudoInstruction.getElementWidth()));
        }
    }

    private void verifyThrow(AnalyzedInstruction analyzedInstruction) {
        int register = ((SingleRegisterInstruction)analyzedInstruction.instruction).getRegisterA();

        RegisterType registerType = analyzedInstruction.getPreInstructionRegisterType(register);
        assert registerType != null;

        if (registerType.category == RegisterType.Category.Null) {
            return;
        }

        if (registerType.category != RegisterType.Category.Reference) {
            throw new ValidationException(String.format("Cannot use throw with non-reference type %s in register v%d",
                    registerType.toString(), register));
        }

        assert registerType.type != null;

        if (!registerType.type.extendsClass(ClassPath.getClassDef("Ljava/lang/Throwable;"))) {
            throw new ValidationException(String.format("Cannot use throw with non-throwable type %s in register v%d",
                    registerType.type.getClassType(), register));
        }
    }

    private void analyzeArrayDataOrSwitch(AnalyzedInstruction analyzedInstruction) {
        int dataAddressOffset = ((OffsetInstruction)analyzedInstruction.instruction).getTargetAddressOffset();

        int dataCodeAddress = this.getInstructionAddress(analyzedInstruction) + dataAddressOffset;
        AnalyzedInstruction dataAnalyzedInstruction = instructions.get(dataCodeAddress);

        if (dataAnalyzedInstruction != null) {
            dataAnalyzedInstruction.dead = false;

            //if there is a preceding nop, it's deadness should be the same
            AnalyzedInstruction priorInstruction =
                    instructions.valueAt(dataAnalyzedInstruction.getInstructionIndex()-1);
            if (priorInstruction.getInstruction().opcode == Opcode.NOP &&
                    !priorInstruction.getInstruction().getFormat().variableSizeFormat) {

                priorInstruction.dead = false;
            }
        }
    }

    private void verifySwitch(AnalyzedInstruction analyzedInstruction, Format expectedSwitchDataFormat) {
        int register = ((SingleRegisterInstruction)analyzedInstruction.instruction).getRegisterA();
        int switchCodeAddressOffset = ((OffsetInstruction)analyzedInstruction.instruction).getTargetAddressOffset();

        getAndCheckSourceRegister(analyzedInstruction, register, Primitive32BitCategories);

        int switchDataCodeAddress = this.getInstructionAddress(analyzedInstruction) + switchCodeAddressOffset;
        AnalyzedInstruction switchDataAnalyzedInstruction = instructions.get(switchDataCodeAddress);

        if (switchDataAnalyzedInstruction == null ||
            switchDataAnalyzedInstruction.instruction.getFormat() != expectedSwitchDataFormat) {
            throw new ValidationException(String.format("There is no %s structure at code address 0x%x",
                    expectedSwitchDataFormat.name(), switchDataCodeAddress));
        }
    }

    private void analyzeFloatWideCmp(AnalyzedInstruction analyzedInstruction) {
        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction,
                RegisterType.getRegisterType(RegisterType.Category.Byte, null));
    }

    private void verifyFloatWideCmp(AnalyzedInstruction analyzedInstruction, EnumSet validCategories) {
        ThreeRegisterInstruction instruction = (ThreeRegisterInstruction)analyzedInstruction.instruction;

        getAndCheckSourceRegister(analyzedInstruction, instruction.getRegisterB(), validCategories);
        getAndCheckSourceRegister(analyzedInstruction, instruction.getRegisterC(), validCategories);
    }

    private void verifyIfEqNe(AnalyzedInstruction analyzedInstruction) {
        TwoRegisterInstruction instruction = (TwoRegisterInstruction)analyzedInstruction.instruction;

        RegisterType registerType1 = analyzedInstruction.getPreInstructionRegisterType(instruction.getRegisterA());
        assert registerType1 != null;

        RegisterType registerType2 = analyzedInstruction.getPreInstructionRegisterType(instruction.getRegisterB());
        assert registerType2 != null;

        if (!(
                (ReferenceCategories.contains(registerType1.category) &&
                ReferenceCategories.contains(registerType2.category))
                    ||
                (Primitive32BitCategories.contains(registerType1.category) &&
                Primitive32BitCategories.contains(registerType2.category))
              )) {

            throw new ValidationException(String.format("%s cannot be used on registers of dissimilar types %s and " +
                    "%s. They must both be a reference type or a primitive 32 bit type.",
                    analyzedInstruction.instruction.opcode.name, registerType1.toString(), registerType2.toString()));
        }
    }

    private void verifyIf(AnalyzedInstruction analyzedInstruction) {
        TwoRegisterInstruction instruction = (TwoRegisterInstruction)analyzedInstruction.instruction;

        getAndCheckSourceRegister(analyzedInstruction, instruction.getRegisterA(), Primitive32BitCategories);
        getAndCheckSourceRegister(analyzedInstruction, instruction.getRegisterB(), Primitive32BitCategories);
    }

    private void verifyIfEqzNez(AnalyzedInstruction analyzedInstruction) {
        SingleRegisterInstruction instruction = (SingleRegisterInstruction)analyzedInstruction.instruction;

        getAndCheckSourceRegister(analyzedInstruction, instruction.getRegisterA(),
                ReferenceAndPrimitive32BitCategories);
    }

    private void verifyIfz(AnalyzedInstruction analyzedInstruction) {
        SingleRegisterInstruction instruction = (SingleRegisterInstruction)analyzedInstruction.instruction;

        getAndCheckSourceRegister(analyzedInstruction, instruction.getRegisterA(), Primitive32BitCategories);
    }

    private void analyze32BitPrimitiveAget(AnalyzedInstruction analyzedInstruction,
                                             RegisterType.Category instructionCategory) {
        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction,
                RegisterType.getRegisterType(instructionCategory, null));
    }

    private void verify32BitPrimitiveAget(AnalyzedInstruction analyzedInstruction,
                                             RegisterType.Category instructionCategory) {
        ThreeRegisterInstruction instruction = (ThreeRegisterInstruction)analyzedInstruction.instruction;

        getAndCheckSourceRegister(analyzedInstruction, instruction.getRegisterC(), Primitive32BitCategories);

        RegisterType arrayRegisterType = analyzedInstruction.getPreInstructionRegisterType(instruction.getRegisterB());
        assert arrayRegisterType != null;

        if (arrayRegisterType.category != RegisterType.Category.Null) {
            if (arrayRegisterType.category != RegisterType.Category.Reference) {
                throw new ValidationException(String.format("Cannot use %s with non-array type %s",
                        analyzedInstruction.instruction.opcode.name, arrayRegisterType.category.toString()));
            }

            assert arrayRegisterType.type != null;
            if (arrayRegisterType.type.getClassType().charAt(0) != '[') {
                throw new ValidationException(String.format("Cannot use %s with non-array type %s",
                        analyzedInstruction.instruction.opcode.name, arrayRegisterType.type.getClassType()));
            }

            assert arrayRegisterType.type instanceof ClassPath.ArrayClassDef;
            ClassPath.ArrayClassDef arrayClassDef = (ClassPath.ArrayClassDef)arrayRegisterType.type;

            if (arrayClassDef.getArrayDimensions() != 1) {
                throw new ValidationException(String.format("Cannot use %s with multi-dimensional array type %s",
                        analyzedInstruction.instruction.opcode.name, arrayRegisterType.type.getClassType()));
            }

            RegisterType arrayBaseType =
                    RegisterType.getRegisterTypeForType(arrayClassDef.getBaseElementClass().getClassType());
            if (!checkArrayFieldAssignment(arrayBaseType.category, instructionCategory)) {
                throw new ValidationException(String.format("Cannot use %s with array type %s. Incorrect array type " +
                        "for the instruction.", analyzedInstruction.instruction.opcode.name,
                        arrayRegisterType.type.getClassType()));
            }
        }
    }

    private void analyzeAgetWide(AnalyzedInstruction analyzedInstruction) {
        ThreeRegisterInstruction instruction = (ThreeRegisterInstruction)analyzedInstruction.instruction;

        RegisterType arrayRegisterType = analyzedInstruction.getPreInstructionRegisterType(instruction.getRegisterB());
        assert arrayRegisterType != null;

        if (arrayRegisterType.category != RegisterType.Category.Null) {
            assert arrayRegisterType.type != null;
            if (arrayRegisterType.type.getClassType().charAt(0) != '[') {
                throw new ValidationException(String.format("Cannot use aget-wide with non-array type %s",
                        arrayRegisterType.type.getClassType()));
            }

            assert arrayRegisterType.type instanceof ClassPath.ArrayClassDef;
            ClassPath.ArrayClassDef arrayClassDef = (ClassPath.ArrayClassDef)arrayRegisterType.type;

            char arrayBaseType = arrayClassDef.getBaseElementClass().getClassType().charAt(0);
            if (arrayBaseType == 'J') {
                setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction,
                        RegisterType.getRegisterType(RegisterType.Category.LongLo, null));
            } else if (arrayBaseType == 'D') {
                setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction,
                        RegisterType.getRegisterType(RegisterType.Category.DoubleLo, null));
            } else {
                throw new ValidationException(String.format("Cannot use aget-wide with array type %s. Incorrect " +
                        "array type for the instruction.", arrayRegisterType.type.getClassType()));
            }
        } else {
            setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction,
                        RegisterType.getRegisterType(RegisterType.Category.LongLo, null));
        }
    }

    private void verifyAgetWide(AnalyzedInstruction analyzedInstruction) {
        ThreeRegisterInstruction instruction = (ThreeRegisterInstruction)analyzedInstruction.instruction;

        getAndCheckSourceRegister(analyzedInstruction, instruction.getRegisterC(), Primitive32BitCategories);

        RegisterType arrayRegisterType = analyzedInstruction.getPreInstructionRegisterType(instruction.getRegisterB());
        assert arrayRegisterType != null;

        if (arrayRegisterType.category != RegisterType.Category.Null) {
            if (arrayRegisterType.category != RegisterType.Category.Reference) {
                throw new ValidationException(String.format("Cannot use aget-wide with non-array type %s",
                        arrayRegisterType.category.toString()));
            }

            assert arrayRegisterType.type != null;
            if (arrayRegisterType.type.getClassType().charAt(0) != '[') {
                throw new ValidationException(String.format("Cannot use aget-wide with non-array type %s",
                        arrayRegisterType.type.getClassType()));
            }

            assert arrayRegisterType.type instanceof ClassPath.ArrayClassDef;
            ClassPath.ArrayClassDef arrayClassDef = (ClassPath.ArrayClassDef)arrayRegisterType.type;

            if (arrayClassDef.getArrayDimensions() != 1) {
                throw new ValidationException(String.format("Cannot use aget-wide with multi-dimensional array type %s",
                        arrayRegisterType.type.getClassType()));
            }

            char arrayBaseType = arrayClassDef.getBaseElementClass().getClassType().charAt(0);
            if (arrayBaseType != 'J' && arrayBaseType != 'D') {
                throw new ValidationException(String.format("Cannot use aget-wide with array type %s. Incorrect " +
                        "array type for the instruction.", arrayRegisterType.type.getClassType()));
            }
        }
    }

    private void analyzeAgetObject(AnalyzedInstruction analyzedInstruction) {
        ThreeRegisterInstruction instruction = (ThreeRegisterInstruction)analyzedInstruction.instruction;

        RegisterType arrayRegisterType = analyzedInstruction.getPreInstructionRegisterType(instruction.getRegisterB());
        assert arrayRegisterType != null;

        if (arrayRegisterType.category != RegisterType.Category.Null) {
            assert arrayRegisterType.type != null;
            if (arrayRegisterType.type.getClassType().charAt(0) != '[') {
                throw new ValidationException(String.format("Cannot use aget-object with non-array type %s",
                        arrayRegisterType.type.getClassType()));
            }

            assert arrayRegisterType.type instanceof ClassPath.ArrayClassDef;
            ClassPath.ArrayClassDef arrayClassDef = (ClassPath.ArrayClassDef)arrayRegisterType.type;

            ClassPath.ClassDef elementClassDef = arrayClassDef.getImmediateElementClass();
            char elementTypePrefix = elementClassDef.getClassType().charAt(0);
            if (elementTypePrefix != 'L' && elementTypePrefix != '[') {
                throw new ValidationException(String.format("Cannot use aget-object with array type %s. Incorrect " +
                        "array type for the instruction.", arrayRegisterType.type.getClassType()));
            }

            setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction,
                    RegisterType.getRegisterType(RegisterType.Category.Reference, elementClassDef));
        } else {
            setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction,
                    RegisterType.getRegisterType(RegisterType.Category.Null, null));
        }
    }

    private void verifyAgetObject(AnalyzedInstruction analyzedInstruction) {
        ThreeRegisterInstruction instruction = (ThreeRegisterInstruction)analyzedInstruction.instruction;

        getAndCheckSourceRegister(analyzedInstruction, instruction.getRegisterC(), Primitive32BitCategories);

        RegisterType arrayRegisterType = analyzedInstruction.getPreInstructionRegisterType(instruction.getRegisterB());
        assert arrayRegisterType != null;

        if (arrayRegisterType.category != RegisterType.Category.Null) {
            if (arrayRegisterType.category != RegisterType.Category.Reference) {
                throw new ValidationException(String.format("Cannot use aget-object with non-array type %s",
                        arrayRegisterType.category.toString()));
            }

            assert arrayRegisterType.type != null;
            if (arrayRegisterType.type.getClassType().charAt(0) != '[') {
                throw new ValidationException(String.format("Cannot use aget-object with non-array type %s",
                        arrayRegisterType.type.getClassType()));
            }

            assert arrayRegisterType.type instanceof ClassPath.ArrayClassDef;
            ClassPath.ArrayClassDef arrayClassDef = (ClassPath.ArrayClassDef)arrayRegisterType.type;

            ClassPath.ClassDef elementClassDef = arrayClassDef.getImmediateElementClass();
            char elementTypePrefix = elementClassDef.getClassType().charAt(0);
            if (elementTypePrefix != 'L' && elementTypePrefix != '[') {
                throw new ValidationException(String.format("Cannot use aget-object with array type %s. Incorrect " +
                        "array type for the instruction.", arrayRegisterType.type.getClassType()));
            }
        }
    }

    private void verify32BitPrimitiveAput(AnalyzedInstruction analyzedInstruction,
                                             RegisterType.Category instructionCategory) {
        ThreeRegisterInstruction instruction = (ThreeRegisterInstruction)analyzedInstruction.instruction;

        getAndCheckSourceRegister(analyzedInstruction, instruction.getRegisterC(), Primitive32BitCategories);

        RegisterType sourceRegisterType = analyzedInstruction.getPreInstructionRegisterType(instruction.getRegisterA());
        assert sourceRegisterType != null;
        RegisterType instructionRegisterType = RegisterType.getRegisterType(instructionCategory, null);
        if (!sourceRegisterType.canBeAssignedTo(instructionRegisterType)) {
            throw new ValidationException(String.format("Cannot use %s with source register type %s.",
                    analyzedInstruction.instruction.opcode.name, sourceRegisterType.toString()));
        }


        RegisterType arrayRegisterType = analyzedInstruction.getPreInstructionRegisterType(instruction.getRegisterB());
        assert arrayRegisterType != null;

        if (arrayRegisterType.category != RegisterType.Category.Null) {
            if (arrayRegisterType.category != RegisterType.Category.Reference) {
                throw new ValidationException(String.format("Cannot use %s with non-array type %s",
                        analyzedInstruction.instruction.opcode.name, arrayRegisterType.category.toString()));
            }

            assert arrayRegisterType.type != null;
            if (arrayRegisterType.type.getClassType().charAt(0) != '[') {
                throw new ValidationException(String.format("Cannot use %s with non-array type %s",
                        analyzedInstruction.instruction.opcode.name, arrayRegisterType.type.getClassType()));
            }

            assert arrayRegisterType.type instanceof ClassPath.ArrayClassDef;
            ClassPath.ArrayClassDef arrayClassDef = (ClassPath.ArrayClassDef)arrayRegisterType.type;

            if (arrayClassDef.getArrayDimensions() != 1) {
                throw new ValidationException(String.format("Cannot use %s with multi-dimensional array type %s",
                        analyzedInstruction.instruction.opcode.name, arrayRegisterType.type.getClassType()));
            }

            RegisterType arrayBaseType =
                    RegisterType.getRegisterTypeForType(arrayClassDef.getBaseElementClass().getClassType());
            if (!checkArrayFieldAssignment(arrayBaseType.category, instructionCategory)) {
                throw new ValidationException(String.format("Cannot use %s with array type %s. Incorrect array type " +
                        "for the instruction.", analyzedInstruction.instruction.opcode.name,
                        arrayRegisterType.type.getClassType()));
            }
        }
    }

    private void verifyAputWide(AnalyzedInstruction analyzedInstruction) {
        ThreeRegisterInstruction instruction = (ThreeRegisterInstruction)analyzedInstruction.instruction;

        getAndCheckSourceRegister(analyzedInstruction, instruction.getRegisterC(), Primitive32BitCategories);
        getAndCheckSourceRegister(analyzedInstruction, instruction.getRegisterA(), WideLowCategories);

        RegisterType arrayRegisterType = analyzedInstruction.getPreInstructionRegisterType(instruction.getRegisterB());
        assert arrayRegisterType != null;

        if (arrayRegisterType.category != RegisterType.Category.Null) {
            if (arrayRegisterType.category != RegisterType.Category.Reference) {
                throw new ValidationException(String.format("Cannot use aput-wide with non-array type %s",
                        arrayRegisterType.category.toString()));
            }

            assert arrayRegisterType.type != null;
            if (arrayRegisterType.type.getClassType().charAt(0) != '[') {
                throw new ValidationException(String.format("Cannot use aput-wide with non-array type %s",
                        arrayRegisterType.type.getClassType()));
            }

            assert arrayRegisterType.type instanceof ClassPath.ArrayClassDef;
            ClassPath.ArrayClassDef arrayClassDef = (ClassPath.ArrayClassDef)arrayRegisterType.type;

            if (arrayClassDef.getArrayDimensions() != 1) {
                throw new ValidationException(String.format("Cannot use aput-wide with multi-dimensional array type %s",
                        arrayRegisterType.type.getClassType()));
            }

            char arrayBaseType = arrayClassDef.getBaseElementClass().getClassType().charAt(0);
            if (arrayBaseType != 'J' && arrayBaseType != 'D') {
                throw new ValidationException(String.format("Cannot use aput-wide with array type %s. Incorrect " +
                        "array type for the instruction.", arrayRegisterType.type.getClassType()));
            }
        }
    }

    private void verifyAputObject(AnalyzedInstruction analyzedInstruction) {
        ThreeRegisterInstruction instruction = (ThreeRegisterInstruction)analyzedInstruction.instruction;

        getAndCheckSourceRegister(analyzedInstruction, instruction.getRegisterC(), Primitive32BitCategories);

        RegisterType sourceRegisterType = analyzedInstruction.getPreInstructionRegisterType(instruction.getRegisterA());
        assert sourceRegisterType != null;

        //TODO: ensure sourceRegisterType is a Reference type?

        RegisterType arrayRegisterType = analyzedInstruction.getPreInstructionRegisterType(instruction.getRegisterB());
        assert arrayRegisterType != null;

        if (arrayRegisterType.category != RegisterType.Category.Null) {
            //don't check the source type against the array type, just make sure it is an array of reference types

            if (arrayRegisterType.category != RegisterType.Category.Reference) {
                throw new ValidationException(String.format("Cannot use aget-object with non-array type %s",
                        arrayRegisterType.category.toString()));
            }

            assert arrayRegisterType.type != null;
            if (arrayRegisterType.type.getClassType().charAt(0) != '[') {
                throw new ValidationException(String.format("Cannot use aget-object with non-array type %s",
                        arrayRegisterType.type.getClassType()));
            }

            assert arrayRegisterType.type instanceof ClassPath.ArrayClassDef;
            ClassPath.ArrayClassDef arrayClassDef = (ClassPath.ArrayClassDef)arrayRegisterType.type;

            ClassPath.ClassDef elementClassDef = arrayClassDef.getImmediateElementClass();
            char elementTypePrefix = elementClassDef.getClassType().charAt(0);
            if (elementTypePrefix != 'L' && elementTypePrefix != '[') {
                throw new ValidationException(String.format("Cannot use aget-object with array type %s. Incorrect " +
                        "array type for the instruction.", arrayRegisterType.type.getClassType()));
            }
        }
    }

    private void analyze32BitPrimitiveIget(AnalyzedInstruction analyzedInstruction,
                                             RegisterType.Category instructionCategory) {
        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction,
                RegisterType.getRegisterType(instructionCategory, null));
    }

    private void verify32BitPrimitiveIget(AnalyzedInstruction analyzedInstruction,
                                             RegisterType.Category instructionCategory) {
        TwoRegisterInstruction instruction = (TwoRegisterInstruction)analyzedInstruction.instruction;

        RegisterType objectRegisterType = getAndCheckSourceRegister(analyzedInstruction, instruction.getRegisterB(),
                ReferenceOrUninitThisCategories);

        //TODO: check access
        Item referencedItem = ((InstructionWithReference)analyzedInstruction.instruction).getReferencedItem();
        assert referencedItem instanceof FieldIdItem;
        FieldIdItem field = (FieldIdItem)referencedItem;

        if (objectRegisterType.category != RegisterType.Category.Null &&
            !objectRegisterType.type.extendsClass(ClassPath.getClassDef(field.getContainingClass()))) {
            throw new ValidationException(String.format("Cannot access field %s through type %s",
                    field.getFieldString(), objectRegisterType.type.getClassType()));
        }

        RegisterType fieldType = RegisterType.getRegisterTypeForTypeIdItem(field.getFieldType());

        if (!checkArrayFieldAssignment(fieldType.category, instructionCategory)) {
                throw new ValidationException(String.format("Cannot use %s with field %s. Incorrect field type " +
                        "for the instruction.", analyzedInstruction.instruction.opcode.name,
                        field.getFieldString()));
        }
    }

    private void analyzeIgetWideObject(AnalyzedInstruction analyzedInstruction) {
        TwoRegisterInstruction instruction = (TwoRegisterInstruction)analyzedInstruction.instruction;

        Item referencedItem = ((InstructionWithReference)analyzedInstruction.instruction).getReferencedItem();
        assert referencedItem instanceof FieldIdItem;
        FieldIdItem field = (FieldIdItem)referencedItem;

        RegisterType fieldType = RegisterType.getRegisterTypeForTypeIdItem(field.getFieldType());
        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction, fieldType);
    }

    private void verifyIgetWide(AnalyzedInstruction analyzedInstruction) {
        TwoRegisterInstruction instruction = (TwoRegisterInstruction)analyzedInstruction.instruction;

        RegisterType objectRegisterType = getAndCheckSourceRegister(analyzedInstruction, instruction.getRegisterB(),
                ReferenceOrUninitThisCategories);

        //TODO: check access
        Item referencedItem = ((InstructionWithReference)analyzedInstruction.instruction).getReferencedItem();
        assert referencedItem instanceof FieldIdItem;
        FieldIdItem field = (FieldIdItem)referencedItem;

        if (objectRegisterType.category != RegisterType.Category.Null &&
            !objectRegisterType.type.extendsClass(ClassPath.getClassDef(field.getContainingClass()))) {
            throw new ValidationException(String.format("Cannot access field %s through type %s",
                    field.getFieldString(), objectRegisterType.type.getClassType()));
        }

        RegisterType fieldType = RegisterType.getRegisterTypeForTypeIdItem(field.getFieldType());

        if (!WideLowCategories.contains(fieldType.category)) {
            throw new ValidationException(String.format("Cannot use %s with field %s. Incorrect field type " +
                    "for the instruction.", analyzedInstruction.instruction.opcode.name,
                    field.getFieldString()));
        }
    }

    private void verifyIgetObject(AnalyzedInstruction analyzedInstruction) {
        TwoRegisterInstruction instruction = (TwoRegisterInstruction)analyzedInstruction.instruction;

        RegisterType objectRegisterType = getAndCheckSourceRegister(analyzedInstruction, instruction.getRegisterB(),
                ReferenceOrUninitThisCategories);

        //TODO: check access
        Item referencedItem = ((InstructionWithReference)analyzedInstruction.instruction).getReferencedItem();
        assert referencedItem instanceof FieldIdItem;
        FieldIdItem field = (FieldIdItem)referencedItem;

        if (objectRegisterType.category != RegisterType.Category.Null &&
            !objectRegisterType.type.extendsClass(ClassPath.getClassDef(field.getContainingClass()))) {
            throw new ValidationException(String.format("Cannot access field %s through type %s",
                    field.getFieldString(), objectRegisterType.type.getClassType()));
        }

        RegisterType fieldType = RegisterType.getRegisterTypeForTypeIdItem(field.getFieldType());

        if (fieldType.category != RegisterType.Category.Reference) {
            throw new ValidationException(String.format("Cannot use %s with field %s. Incorrect field type " +
                        "for the instruction.", analyzedInstruction.instruction.opcode.name,
                        field.getFieldString()));
        }
    }

    private void verify32BitPrimitiveIput(AnalyzedInstruction analyzedInstruction,
                                             RegisterType.Category instructionCategory) {
        TwoRegisterInstruction instruction = (TwoRegisterInstruction)analyzedInstruction.instruction;

        RegisterType objectRegisterType = getAndCheckSourceRegister(analyzedInstruction, instruction.getRegisterB(),
                ReferenceOrUninitThisCategories);

        RegisterType sourceRegisterType = analyzedInstruction.getPreInstructionRegisterType(instruction.getRegisterA());
        assert sourceRegisterType != null;

        //per CodeVerify.c in dalvik:
        //java generates synthetic functions that write byte values into boolean fields
        if (sourceRegisterType.category == RegisterType.Category.Byte &&
            instructionCategory == RegisterType.Category.Boolean) {

            sourceRegisterType = RegisterType.getRegisterType(RegisterType.Category.Boolean, null);
        }

        RegisterType instructionRegisterType = RegisterType.getRegisterType(instructionCategory, null);
        if (!sourceRegisterType.canBeAssignedTo(instructionRegisterType)) {
            throw new ValidationException(String.format("Cannot use %s with source register type %s.",
                    analyzedInstruction.instruction.opcode.name, sourceRegisterType.toString()));
        }


        //TODO: check access
        Item referencedItem = ((InstructionWithReference)analyzedInstruction.instruction).getReferencedItem();
        assert referencedItem instanceof FieldIdItem;
        FieldIdItem field = (FieldIdItem)referencedItem;

        if (objectRegisterType.category != RegisterType.Category.Null &&
            !objectRegisterType.type.extendsClass(ClassPath.getClassDef(field.getContainingClass()))) {
            throw new ValidationException(String.format("Cannot access field %s through type %s",
                    field.getFieldString(), objectRegisterType.type.getClassType()));
        }

        RegisterType fieldType = RegisterType.getRegisterTypeForTypeIdItem(field.getFieldType());

        if (!checkArrayFieldAssignment(fieldType.category, instructionCategory)) {
                throw new ValidationException(String.format("Cannot use %s with field %s. Incorrect field type " +
                        "for the instruction.", analyzedInstruction.instruction.opcode.name,
                        field.getFieldString()));
        }
    }

    private void verifyIputWide(AnalyzedInstruction analyzedInstruction) {
        TwoRegisterInstruction instruction = (TwoRegisterInstruction)analyzedInstruction.instruction;

        RegisterType objectRegisterType = getAndCheckSourceRegister(analyzedInstruction, instruction.getRegisterB(),
                ReferenceOrUninitThisCategories);

        getAndCheckSourceRegister(analyzedInstruction, instruction.getRegisterA(), WideLowCategories);

        //TODO: check access
        Item referencedItem = ((InstructionWithReference)analyzedInstruction.instruction).getReferencedItem();
        assert referencedItem instanceof FieldIdItem;
        FieldIdItem field = (FieldIdItem)referencedItem;

        if (objectRegisterType.category != RegisterType.Category.Null &&
                !objectRegisterType.type.extendsClass(ClassPath.getClassDef(field.getContainingClass()))) {
            throw new ValidationException(String.format("Cannot access field %s through type %s",
                    field.getFieldString(), objectRegisterType.type.getClassType()));
        }

        RegisterType fieldType = RegisterType.getRegisterTypeForTypeIdItem(field.getFieldType());

        if (!WideLowCategories.contains(fieldType.category)) {
            throw new ValidationException(String.format("Cannot use %s with field %s. Incorrect field type " +
                    "for the instruction.", analyzedInstruction.instruction.opcode.name,
                    field.getFieldString()));
        }
    }

    private void verifyIputObject(AnalyzedInstruction analyzedInstruction) {
        TwoRegisterInstruction instruction = (TwoRegisterInstruction)analyzedInstruction.instruction;

        RegisterType objectRegisterType = getAndCheckSourceRegister(analyzedInstruction, instruction.getRegisterB(),
                ReferenceOrUninitThisCategories);

        RegisterType sourceRegisterType = getAndCheckSourceRegister(analyzedInstruction, instruction.getRegisterA(),
                ReferenceCategories);

        //TODO: check access
        Item referencedItem = ((InstructionWithReference)analyzedInstruction.instruction).getReferencedItem();
        assert referencedItem instanceof FieldIdItem;
        FieldIdItem field = (FieldIdItem)referencedItem;

        if (objectRegisterType.category != RegisterType.Category.Null &&
            !objectRegisterType.type.extendsClass(ClassPath.getClassDef(field.getContainingClass()))) {
            throw new ValidationException(String.format("Cannot access field %s through type %s",
                    field.getFieldString(), objectRegisterType.type.getClassType()));
        }

        RegisterType fieldType = RegisterType.getRegisterTypeForTypeIdItem(field.getFieldType());

        if (fieldType.category != RegisterType.Category.Reference) {
            throw new ValidationException(String.format("Cannot use %s with field %s. Incorrect field type " +
                        "for the instruction.", analyzedInstruction.instruction.opcode.name,
                        field.getFieldString()));
        }

        if (sourceRegisterType.category != RegisterType.Category.Null &&
            !fieldType.type.isInterface() &&
            !sourceRegisterType.type.extendsClass(fieldType.type)) {

            throw new ValidationException(String.format("Cannot store a value of type %s into a field of type %s",
                    sourceRegisterType.type.getClassType(), fieldType.type.getClassType()));
        }
    }

    private void analyze32BitPrimitiveSget(AnalyzedInstruction analyzedInstruction,
                                             RegisterType.Category instructionCategory) {
        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction,
                RegisterType.getRegisterType(instructionCategory, null));
    }

    private void verify32BitPrimitiveSget(AnalyzedInstruction analyzedInstruction,
                                             RegisterType.Category instructionCategory) {
        //TODO: check access
        Item referencedItem = ((InstructionWithReference)analyzedInstruction.instruction).getReferencedItem();
        assert referencedItem instanceof FieldIdItem;
        FieldIdItem field = (FieldIdItem)referencedItem;

        RegisterType fieldType = RegisterType.getRegisterTypeForTypeIdItem(field.getFieldType());

        if (!checkArrayFieldAssignment(fieldType.category, instructionCategory)) {
                throw new ValidationException(String.format("Cannot use %s with field %s. Incorrect field type " +
                        "for the instruction.", analyzedInstruction.instruction.opcode.name,
                        field.getFieldString()));
        }
    }

    private void analyzeSgetWideObject(AnalyzedInstruction analyzedInstruction) {
        Item referencedItem = ((InstructionWithReference)analyzedInstruction.instruction).getReferencedItem();
        assert referencedItem instanceof FieldIdItem;
        FieldIdItem field = (FieldIdItem)referencedItem;

        RegisterType fieldType = RegisterType.getRegisterTypeForTypeIdItem(field.getFieldType());
        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction, fieldType);
    }

    private void verifySgetWide(AnalyzedInstruction analyzedInstruction) {
        //TODO: check access
        Item referencedItem = ((InstructionWithReference)analyzedInstruction.instruction).getReferencedItem();
        assert referencedItem instanceof FieldIdItem;
        FieldIdItem field = (FieldIdItem)referencedItem;

        RegisterType fieldType = RegisterType.getRegisterTypeForTypeIdItem(field.getFieldType());


        if (fieldType.category != RegisterType.Category.LongLo &&
            fieldType.category != RegisterType.Category.DoubleLo) {

            throw new ValidationException(String.format("Cannot use %s with field %s. Incorrect field type " +
                    "for the instruction.", analyzedInstruction.instruction.opcode.name,
                    field.getFieldString()));
        }
    }

    private void verifySgetObject(AnalyzedInstruction analyzedInstruction) {
        //TODO: check access
        Item referencedItem = ((InstructionWithReference)analyzedInstruction.instruction).getReferencedItem();
        assert referencedItem instanceof FieldIdItem;
        FieldIdItem field = (FieldIdItem)referencedItem;

        RegisterType fieldType = RegisterType.getRegisterTypeForTypeIdItem(field.getFieldType());

        if (fieldType.category != RegisterType.Category.Reference) {
                throw new ValidationException(String.format("Cannot use %s with field %s. Incorrect field type " +
                        "for the instruction.", analyzedInstruction.instruction.opcode.name,
                        field.getFieldString()));
        }
    }

    private void verify32BitPrimitiveSput(AnalyzedInstruction analyzedInstruction,
                                             RegisterType.Category instructionCategory) {
        SingleRegisterInstruction instruction = (SingleRegisterInstruction)analyzedInstruction.instruction;

        RegisterType sourceRegisterType = analyzedInstruction.getPreInstructionRegisterType(instruction.getRegisterA());
        assert sourceRegisterType != null;

        //per CodeVerify.c in dalvik:
        //java generates synthetic functions that write byte values into boolean fields
        if (sourceRegisterType.category == RegisterType.Category.Byte &&
            instructionCategory == RegisterType.Category.Boolean) {

            sourceRegisterType = RegisterType.getRegisterType(RegisterType.Category.Boolean, null);
        }

        RegisterType instructionRegisterType = RegisterType.getRegisterType(instructionCategory, null);
        if (!sourceRegisterType.canBeAssignedTo(instructionRegisterType)) {
            throw new ValidationException(String.format("Cannot use %s with source register type %s.",
                    analyzedInstruction.instruction.opcode.name, sourceRegisterType.toString()));
        }

        //TODO: check access
        Item referencedItem = ((InstructionWithReference)analyzedInstruction.instruction).getReferencedItem();
        assert referencedItem instanceof FieldIdItem;
        FieldIdItem field = (FieldIdItem)referencedItem;

        RegisterType fieldType = RegisterType.getRegisterTypeForTypeIdItem(field.getFieldType());

        if (!checkArrayFieldAssignment(fieldType.category, instructionCategory)) {
                throw new ValidationException(String.format("Cannot use %s with field %s. Incorrect field type " +
                        "for the instruction.", analyzedInstruction.instruction.opcode.name,
                        field.getFieldString()));
        }
    }

    private void verifySputWide(AnalyzedInstruction analyzedInstruction) {
        SingleRegisterInstruction instruction = (SingleRegisterInstruction)analyzedInstruction.instruction;


        getAndCheckSourceRegister(analyzedInstruction, instruction.getRegisterA(), WideLowCategories);

        //TODO: check access
        Item referencedItem = ((InstructionWithReference)analyzedInstruction.instruction).getReferencedItem();
        assert referencedItem instanceof FieldIdItem;
        FieldIdItem field = (FieldIdItem)referencedItem;

        RegisterType fieldType = RegisterType.getRegisterTypeForTypeIdItem(field.getFieldType());

        if (!WideLowCategories.contains(fieldType.category)) {
                throw new ValidationException(String.format("Cannot use %s with field %s. Incorrect field type " +
                        "for the instruction.", analyzedInstruction.instruction.opcode.name,
                        field.getFieldString()));
        }
    }

    private void verifySputObject(AnalyzedInstruction analyzedInstruction) {
        SingleRegisterInstruction instruction = (SingleRegisterInstruction)analyzedInstruction.instruction;

        RegisterType sourceRegisterType = getAndCheckSourceRegister(analyzedInstruction, instruction.getRegisterA(),
                ReferenceCategories);

        //TODO: check access
        Item referencedItem = ((InstructionWithReference)analyzedInstruction.instruction).getReferencedItem();
        assert referencedItem instanceof FieldIdItem;
        FieldIdItem field = (FieldIdItem)referencedItem;

        RegisterType fieldType = RegisterType.getRegisterTypeForTypeIdItem(field.getFieldType());

        if (fieldType.category != RegisterType.Category.Reference) {
            throw new ValidationException(String.format("Cannot use %s with field %s. Incorrect field type " +
                        "for the instruction.", analyzedInstruction.instruction.opcode.name,
                        field.getFieldString()));
        }

        if (sourceRegisterType.category != RegisterType.Category.Null &&
            !fieldType.type.isInterface() &&
            !sourceRegisterType.type.extendsClass(fieldType.type)) {

            throw new ValidationException(String.format("Cannot store a value of type %s into a field of type %s",
                    sourceRegisterType.type.getClassType(), fieldType.type.getClassType()));
        }
    }

    private void analyzeInvokeDirect(AnalyzedInstruction analyzedInstruction) {
        FiveRegisterInstruction instruction = (FiveRegisterInstruction)analyzedInstruction.instruction;
        analyzeInvokeDirectCommon(analyzedInstruction, new Format35cRegisterIterator(instruction));
    }

    private void verifyInvoke(AnalyzedInstruction analyzedInstruction, int invokeType) {
        FiveRegisterInstruction instruction = (FiveRegisterInstruction)analyzedInstruction.instruction;
        verifyInvokeCommon(analyzedInstruction, false, invokeType, new Format35cRegisterIterator(instruction));
    }

    private void analyzeInvokeDirectRange(AnalyzedInstruction analyzedInstruction) {
        RegisterRangeInstruction instruction = (RegisterRangeInstruction)analyzedInstruction.instruction;
        analyzeInvokeDirectCommon(analyzedInstruction, new Format3rcRegisterIterator(instruction));
    }

    private void verifyInvokeRange(AnalyzedInstruction analyzedInstruction, int invokeType) {
        RegisterRangeInstruction instruction = (RegisterRangeInstruction)analyzedInstruction.instruction;
        verifyInvokeCommon(analyzedInstruction, true, invokeType, new Format3rcRegisterIterator(instruction));
    }

    private static final int INVOKE_VIRTUAL = 0x01;
    private static final int INVOKE_SUPER = 0x02;
    private static final int INVOKE_DIRECT = 0x04;
    private static final int INVOKE_INTERFACE = 0x08;
    private static final int INVOKE_STATIC = 0x10;

    private void analyzeInvokeDirectCommon(AnalyzedInstruction analyzedInstruction, RegisterIterator registers) {
        //the only time that an invoke instruction changes a register type is when using invoke-direct on a
        //constructor (<init>) method, which changes the uninitialized reference (and any register that the same
        //uninit reference has been copied to) to an initialized reference

        InstructionWithReference instruction = (InstructionWithReference)analyzedInstruction.instruction;

        Item item = instruction.getReferencedItem();
        assert item.getItemType() == ItemType.TYPE_METHOD_ID_ITEM;
        MethodIdItem methodIdItem = (MethodIdItem)item;

        if (!methodIdItem.getMethodName().getStringValue().equals("<init>")) {
            return;
        }

        RegisterType objectRegisterType;
        //the object register is always the first register
        int objectRegister = registers.getRegister();

        objectRegisterType = analyzedInstruction.getPreInstructionRegisterType(objectRegister);
        assert objectRegisterType != null;

        if (objectRegisterType.category != RegisterType.Category.UninitRef &&
                objectRegisterType.category != RegisterType.Category.UninitThis) {
            return;
        }

        setPostRegisterTypeAndPropagateChanges(analyzedInstruction, objectRegister,
                RegisterType.getRegisterType(RegisterType.Category.Reference, objectRegisterType.type));

        for (int i=0; i<analyzedInstruction.postRegisterMap.length; i++) {
            RegisterType postInstructionRegisterType = analyzedInstruction.postRegisterMap[i];
            if (postInstructionRegisterType.category == RegisterType.Category.Unknown) {
                RegisterType preInstructionRegisterType =
                        analyzedInstruction.getPreInstructionRegisterType(i);

                if (preInstructionRegisterType.category == RegisterType.Category.UninitRef ||
                    preInstructionRegisterType.category == RegisterType.Category.UninitThis) {

                    RegisterType registerType;
                    if (preInstructionRegisterType == objectRegisterType) {
                        registerType = analyzedInstruction.postRegisterMap[objectRegister];
                    } else {
                        registerType = preInstructionRegisterType;
                    }

                    setPostRegisterTypeAndPropagateChanges(analyzedInstruction, i, registerType);
                }
            }
        }
    }

    private void verifyInvokeCommon(AnalyzedInstruction analyzedInstruction, boolean isRange, int invokeType,
                                       RegisterIterator registers) {
        InstructionWithReference instruction = (InstructionWithReference)analyzedInstruction.instruction;

        //TODO: check access

        Item item = instruction.getReferencedItem();
        assert item.getItemType() == ItemType.TYPE_METHOD_ID_ITEM;
        MethodIdItem methodIdItem = (MethodIdItem)item;

        TypeIdItem methodClass = methodIdItem.getContainingClass();
        boolean isInit = false;

        if (methodIdItem.getMethodName().getStringValue().charAt(0) == '<') {
            if ((invokeType & INVOKE_DIRECT) != 0) {
                isInit = true;
            } else {
                throw new ValidationException(String.format("Cannot call constructor %s with %s",
                        methodIdItem.getMethodString(), analyzedInstruction.instruction.opcode.name));
            }
        }

        ClassPath.ClassDef methodClassDef = ClassPath.getClassDef(methodClass);
        if ((invokeType & INVOKE_INTERFACE) != 0) {
            if (!methodClassDef.isInterface()) {
                throw new ValidationException(String.format("Cannot call method %s with %s. %s is not an interface " +
                        "class.", methodIdItem.getMethodString(), analyzedInstruction.instruction.opcode.name,
                        methodClassDef.getClassType()));
            }
        } else {
            if (methodClassDef.isInterface()) {
                throw new ValidationException(String.format("Cannot call method %s with %s. %s is an interface class." +
                        " Use invoke-interface or invoke-interface/range instead.", methodIdItem.getMethodString(),
                        analyzedInstruction.instruction.opcode.name, methodClassDef.getClassType()));
            }
        }

        if ((invokeType & INVOKE_SUPER) != 0) {
            ClassPath.ClassDef currentMethodClassDef = ClassPath.getClassDef(encodedMethod.method.getContainingClass());
            if (currentMethodClassDef.getSuperclass() == null) {
                throw new ValidationException(String.format("Cannot call method %s with %s. %s has no superclass",
                        methodIdItem.getMethodString(), analyzedInstruction.instruction.opcode.name,
                        methodClassDef.getSuperclass().getClassType()));
            }

            if (!currentMethodClassDef.getSuperclass().extendsClass(methodClassDef)) {
                throw new ValidationException(String.format("Cannot call method %s with %s. %s is not an ancestor " +
                        "of the current class %s", methodIdItem.getMethodString(),
                        analyzedInstruction.instruction.opcode.name, methodClass.getTypeDescriptor(),
                        encodedMethod.method.getContainingClass().getTypeDescriptor()));
            }

            if (!currentMethodClassDef.getSuperclass().hasVirtualMethod(methodIdItem.getShortMethodString())) {
                throw new ValidationException(String.format("Cannot call method %s with %s. The superclass %s has" +
                        "no such method", methodIdItem.getMethodString(),
                        analyzedInstruction.instruction.opcode.name, methodClassDef.getSuperclass().getClassType()));
            }
        }

        assert isRange || registers.getCount() <= 5;

        TypeListItem typeListItem = methodIdItem.getPrototype().getParameters();
        int methodParameterRegisterCount;
        if (typeListItem == null) {
            methodParameterRegisterCount = 0;
        } else {
            methodParameterRegisterCount = typeListItem.getRegisterCount();
        }

        if ((invokeType & INVOKE_STATIC) == 0) {
            methodParameterRegisterCount++;
        }

        if (methodParameterRegisterCount != registers.getCount()) {
            throw new ValidationException(String.format("The number of registers does not match the number of " +
                    "parameters for method %s. Expecting %d registers, got %d.", methodIdItem.getMethodString(),
                    methodParameterRegisterCount + 1, registers.getCount()));
        }

        RegisterType objectRegisterType = null;
        int objectRegister = 0;
        if ((invokeType & INVOKE_STATIC) == 0) {
            objectRegister = registers.getRegister();
            registers.moveNext();

            objectRegisterType = analyzedInstruction.getPreInstructionRegisterType(objectRegister);
            assert objectRegisterType != null;
            if (objectRegisterType.category == RegisterType.Category.UninitRef ||
                    objectRegisterType.category == RegisterType.Category.UninitThis) {

                if (!isInit) {
                    throw new ValidationException(String.format("Cannot invoke non-<init> method %s on uninitialized " +
                            "reference type %s", methodIdItem.getMethodString(),
                            objectRegisterType.type.getClassType()));
                }
            } else if (objectRegisterType.category == RegisterType.Category.Reference) {
                if (isInit) {
                    throw new ValidationException(String.format("Cannot invoke %s on initialized reference type %s",
                            methodIdItem.getMethodString(), objectRegisterType.type.getClassType()));
                }
            } else if (objectRegisterType.category == RegisterType.Category.Null) {
                if (isInit) {
                    throw new ValidationException(String.format("Cannot invoke %s on a null reference",
                            methodIdItem.getMethodString()));
                }
            }
            else {
                throw new ValidationException(String.format("Cannot invoke %s on non-reference type %s",
                        methodIdItem.getMethodString(), objectRegisterType.toString()));
            }

            if (isInit) {
                if (objectRegisterType.type.getSuperclass() == methodClassDef) {
                    if (!encodedMethod.method.getMethodName().getStringValue().equals("<init>")) {
                        throw new ValidationException(String.format("Cannot call %s on type %s. The object type must " +
                                "match the method type exactly", methodIdItem.getMethodString(),
                                objectRegisterType.type.getClassType()));
                    }
                }
            }

            if ((invokeType & INVOKE_INTERFACE) == 0 && objectRegisterType.category != RegisterType.Category.Null &&
                    !objectRegisterType.type.extendsClass(methodClassDef)) {

               throw new ValidationException(String.format("Cannot call method %s on an object of type %s, which " +
                       "does not extend %s.", methodIdItem.getMethodString(), objectRegisterType.type.getClassType(),
                        methodClassDef.getClassType()));
            }
        }

        if (typeListItem != null) {
            List<TypeIdItem> parameterTypes = typeListItem.getTypes();
            int parameterTypeIndex = 0;
            while (!registers.pastEnd()) {
                assert parameterTypeIndex < parameterTypes.size();
                RegisterType parameterType =
                        RegisterType.getRegisterTypeForTypeIdItem(parameterTypes.get(parameterTypeIndex));

                int register = registers.getRegister();

                RegisterType parameterRegisterType;
                if (WideLowCategories.contains(parameterType.category)) {
                    parameterRegisterType = getAndCheckSourceRegister(analyzedInstruction, register, WideLowCategories);

                    if (!registers.moveNext()) {
                        throw new ValidationException(String.format("No 2nd register specified for wide register pair v%d",
                                parameterTypeIndex+1));
                    }
                    int nextRegister = registers.getRegister();

                    if (nextRegister != register + 1) {
                        throw new ValidationException(String.format("Invalid wide register pair (v%d, v%d). Registers " +
                                "must be consecutive.", register, nextRegister));
                    }
                } else {
                    parameterRegisterType = analyzedInstruction.getPreInstructionRegisterType(register);
                }

                assert parameterRegisterType != null;

                if (!parameterRegisterType.canBeAssignedTo(parameterType)) {
                    throw new ValidationException(
                            String.format("Invalid register type %s for parameter %d %s.",
                                    parameterRegisterType.toString(), parameterTypeIndex+1,
                                    parameterType.toString()));
                }

                parameterTypeIndex++;
                registers.moveNext();
            }
        }
    }

    private void analyzeUnaryOp(AnalyzedInstruction analyzedInstruction, RegisterType.Category destRegisterCategory) {
        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction,
                RegisterType.getRegisterType(destRegisterCategory, null));
    }

    private void verifyUnaryOp(AnalyzedInstruction analyzedInstruction, EnumSet validSourceCategories) {
        TwoRegisterInstruction instruction = (TwoRegisterInstruction)analyzedInstruction.instruction;

        getAndCheckSourceRegister(analyzedInstruction, instruction.getRegisterB(), validSourceCategories);
    }

    private void analyzeBinaryOp(AnalyzedInstruction analyzedInstruction, RegisterType.Category destRegisterCategory,
                                boolean checkForBoolean) {
        if (checkForBoolean) {
            ThreeRegisterInstruction instruction = (ThreeRegisterInstruction)analyzedInstruction.instruction;

            RegisterType source1RegisterType =
                    analyzedInstruction.getPreInstructionRegisterType(instruction.getRegisterB());
            RegisterType source2RegisterType =
                    analyzedInstruction.getPreInstructionRegisterType(instruction.getRegisterC());

            if (BooleanCategories.contains(source1RegisterType.category) &&
                BooleanCategories.contains(source2RegisterType.category)) {

                destRegisterCategory = RegisterType.Category.Boolean;
            }
        }

        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction,
                RegisterType.getRegisterType(destRegisterCategory, null));
    }

    private void verifyBinaryOp(AnalyzedInstruction analyzedInstruction, EnumSet validSource1Categories,
                                EnumSet validSource2Categories) {
        ThreeRegisterInstruction instruction = (ThreeRegisterInstruction)analyzedInstruction.instruction;

        getAndCheckSourceRegister(analyzedInstruction, instruction.getRegisterB(), validSource1Categories);
        getAndCheckSourceRegister(analyzedInstruction, instruction.getRegisterC(), validSource2Categories);
    }

    private void analyzeBinary2AddrOp(AnalyzedInstruction analyzedInstruction,
                                      RegisterType.Category destRegisterCategory, boolean checkForBoolean) {
        if (checkForBoolean) {
            TwoRegisterInstruction instruction = (TwoRegisterInstruction)analyzedInstruction.instruction;

            RegisterType source1RegisterType =
                    analyzedInstruction.getPreInstructionRegisterType(instruction.getRegisterA());
            RegisterType source2RegisterType =
                    analyzedInstruction.getPreInstructionRegisterType(instruction.getRegisterB());

            if (BooleanCategories.contains(source1RegisterType.category) &&
                BooleanCategories.contains(source2RegisterType.category)) {

                destRegisterCategory = RegisterType.Category.Boolean;
            }
        }

        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction,
                RegisterType.getRegisterType(destRegisterCategory, null));
    }

    private void verifyBinary2AddrOp(AnalyzedInstruction analyzedInstruction, EnumSet validSource1Categories,
                                EnumSet validSource2Categories) {
        TwoRegisterInstruction instruction = (TwoRegisterInstruction)analyzedInstruction.instruction;

        getAndCheckSourceRegister(analyzedInstruction, instruction.getRegisterA(), validSource1Categories);
        getAndCheckSourceRegister(analyzedInstruction, instruction.getRegisterB(), validSource2Categories);
    }

    private void analyzeLiteralBinaryOp(AnalyzedInstruction analyzedInstruction,
                                        RegisterType.Category destRegisterCategory, boolean checkForBoolean) {
        if (checkForBoolean) {
            TwoRegisterInstruction instruction = (TwoRegisterInstruction)analyzedInstruction.instruction;

            RegisterType sourceRegisterType =
                    analyzedInstruction.getPreInstructionRegisterType(instruction.getRegisterB());

            if (BooleanCategories.contains(sourceRegisterType.category)) {
                long literal = ((LiteralInstruction)analyzedInstruction.instruction).getLiteral();
                if (literal == 0 || literal == 1) {
                    destRegisterCategory = RegisterType.Category.Boolean;
                }
            }
        }

        setDestinationRegisterTypeAndPropagateChanges(analyzedInstruction,
                RegisterType.getRegisterType(destRegisterCategory, null));
    }

    private void verifyLiteralBinaryOp(AnalyzedInstruction analyzedInstruction) {
        TwoRegisterInstruction instruction = (TwoRegisterInstruction)analyzedInstruction.instruction;

        getAndCheckSourceRegister(analyzedInstruction, instruction.getRegisterB(), Primitive32BitCategories);
    }

    private RegisterType.Category getDestTypeForLiteralShiftRight(AnalyzedInstruction analyzedInstruction,
                                                                  boolean signedShift) {
        TwoRegisterInstruction instruction = (TwoRegisterInstruction)analyzedInstruction.instruction;

        RegisterType sourceRegisterType = getAndCheckSourceRegister(analyzedInstruction, instruction.getRegisterB(),
                Primitive32BitCategories);
        long literalShift = ((LiteralInstruction)analyzedInstruction.instruction).getLiteral();

        if (literalShift == 0) {
            return sourceRegisterType.category;
        }

        RegisterType.Category destRegisterCategory;
        if (!signedShift) {
            destRegisterCategory = RegisterType.Category.Integer;
        } else {
            destRegisterCategory = sourceRegisterType.category;
        }

        if (literalShift >= 32) {
            //TODO: add warning
            return destRegisterCategory;
        }

        switch (sourceRegisterType.category) {
            case Integer:
            case Float:
                if (!signedShift) {
                    if (literalShift > 24) {
                        return RegisterType.Category.PosByte;
                    }
                    if (literalShift >= 16) {
                        return RegisterType.Category.Char;
                    }
                } else {
                    if (literalShift >= 24) {
                        return RegisterType.Category.Byte;
                    }
                    if (literalShift >= 16) {
                        return RegisterType.Category.Short;
                    }
                }
                break;
            case Short:
                if (signedShift && literalShift >= 8) {
                    return RegisterType.Category.Byte;
                }
                break;
            case PosShort:
                if (literalShift >= 8) {
                    return RegisterType.Category.PosByte;
                }
                break;
            case Char:
                if (literalShift > 8) {
                    return RegisterType.Category.PosByte;
                }
                break;
            case Byte:
                break;
            case PosByte:
                return RegisterType.Category.PosByte;
            case Null:
            case One:
            case Boolean:
                return RegisterType.Category.Null;
            default:
                assert false;
        }

        return destRegisterCategory;
    }


    private void analyzeExecuteInline(AnalyzedInstruction analyzedInstruction) {
        if (deodexUtil == null) {
            throw new ValidationException("Cannot analyze an odexed instruction unless we are deodexing");
        }

        Instruction35mi instruction = (Instruction35mi)analyzedInstruction.instruction;

        DeodexUtil.InlineMethod inlineMethod = deodexUtil.lookupInlineMethod(analyzedInstruction);
        MethodIdItem inlineMethodIdItem = inlineMethod.getMethodIdItem(deodexUtil);
        if (inlineMethodIdItem == null) {
            throw new ValidationException(String.format("Cannot load inline method with index %d",
                    instruction.getInlineIndex()));
        }

        Opcode deodexedOpcode = null;
        switch (inlineMethod.methodType) {
            case DeodexUtil.Direct:
                deodexedOpcode = Opcode.INVOKE_DIRECT;
                break;
            case DeodexUtil.Static:
                deodexedOpcode = Opcode.INVOKE_STATIC;
                break;
            case DeodexUtil.Virtual:
                deodexedOpcode = Opcode.INVOKE_VIRTUAL;
                break;
            default:
                assert false;
        }

        Instruction35c deodexedInstruction = new Instruction35c(deodexedOpcode, instruction.getRegCount(),
                instruction.getRegisterD(), instruction.getRegisterE(), instruction.getRegisterF(),
                instruction.getRegisterG(), instruction.getRegisterA(), inlineMethodIdItem);

        analyzedInstruction.setDeodexedInstruction(deodexedInstruction);

        analyzeInstruction(analyzedInstruction);
    }

    private void analyzeExecuteInlineRange(AnalyzedInstruction analyzedInstruction) {
        if (deodexUtil == null) {
            throw new ValidationException("Cannot analyze an odexed instruction unless we are deodexing");
        }

        Instruction3rmi instruction = (Instruction3rmi)analyzedInstruction.instruction;

        DeodexUtil.InlineMethod inlineMethod = deodexUtil.lookupInlineMethod(analyzedInstruction);
        MethodIdItem inlineMethodIdItem = inlineMethod.getMethodIdItem(deodexUtil);
        if (inlineMethodIdItem == null) {
            throw new ValidationException(String.format("Cannot load inline method with index %d",
                    instruction.getInlineIndex()));
        }

        Opcode deodexedOpcode = null;
        switch (inlineMethod.methodType) {
            case DeodexUtil.Direct:
                deodexedOpcode = Opcode.INVOKE_DIRECT_RANGE;
                break;
            case DeodexUtil.Static:
                deodexedOpcode = Opcode.INVOKE_STATIC_RANGE;
                break;
            case DeodexUtil.Virtual:
                deodexedOpcode = Opcode.INVOKE_VIRTUAL_RANGE;
                break;
            default:
                assert false;
        }

        Instruction3rc deodexedInstruction = new Instruction3rc(deodexedOpcode, (short)instruction.getRegCount(),
                instruction.getStartRegister(), inlineMethodIdItem);

        analyzedInstruction.setDeodexedInstruction(deodexedInstruction);

        analyzeInstruction(analyzedInstruction);
    }

    private void analyzeInvokeDirectEmpty(AnalyzedInstruction analyzedInstruction) {
        analyzeInvokeDirectEmpty(analyzedInstruction, true);
    }

    private void analyzeInvokeDirectEmpty(AnalyzedInstruction analyzedInstruction, boolean analyzeResult) {
        Instruction35c instruction = (Instruction35c)analyzedInstruction.instruction;

        Instruction35c deodexedInstruction = new Instruction35c(Opcode.INVOKE_DIRECT, instruction.getRegCount(),
                instruction.getRegisterD(), instruction.getRegisterE(), instruction.getRegisterF(),
                instruction.getRegisterG(), instruction.getRegisterA(), instruction.getReferencedItem());

        analyzedInstruction.setDeodexedInstruction(deodexedInstruction);

        if (analyzeResult) {
            analyzeInstruction(analyzedInstruction);
        }
    }

    private void analyzeInvokeObjectInitRange(AnalyzedInstruction analyzedInstruction) {
        analyzeInvokeObjectInitRange(analyzedInstruction, true);
    }

    private void analyzeInvokeObjectInitRange(AnalyzedInstruction analyzedInstruction, boolean analyzeResult) {
        Instruction3rc instruction = (Instruction3rc)analyzedInstruction.instruction;

        Instruction3rc deodexedInstruction = new Instruction3rc(Opcode.INVOKE_DIRECT_RANGE,
                (short)instruction.getRegCount(), instruction.getStartRegister(), instruction.getReferencedItem());

        analyzedInstruction.setDeodexedInstruction(deodexedInstruction);

        if (analyzeResult) {
            analyzeInstruction(analyzedInstruction);
        }
    }

    private boolean analyzeIputIgetQuick(AnalyzedInstruction analyzedInstruction) {
        Instruction22cs instruction = (Instruction22cs)analyzedInstruction.instruction;

        int fieldOffset = instruction.getFieldOffset();
        RegisterType objectRegisterType = getAndCheckSourceRegister(analyzedInstruction, instruction.getRegisterB(),
                ReferenceOrUninitCategories);

        if (objectRegisterType.category == RegisterType.Category.Null) {
            return false;
        }

        FieldIdItem fieldIdItem = deodexUtil.lookupField(objectRegisterType.type, fieldOffset);
        if (fieldIdItem == null) {
            throw new ValidationException(String.format("Could not resolve the field in class %s at offset %d",
                    objectRegisterType.type.getClassType(), fieldOffset));
        }

        String fieldType = fieldIdItem.getFieldType().getTypeDescriptor();

        Opcode opcode = OdexedFieldInstructionMapper.getAndCheckDeodexedOpcodeForOdexedOpcode(fieldType, instruction.opcode);

        Instruction22c deodexedInstruction = new Instruction22c(opcode, (byte)instruction.getRegisterA(),
                (byte)instruction.getRegisterB(), fieldIdItem);
        analyzedInstruction.setDeodexedInstruction(deodexedInstruction);

        analyzeInstruction(analyzedInstruction);

        return true;
    }

    private boolean analyzeInvokeVirtualQuick(AnalyzedInstruction analyzedInstruction, boolean isSuper,
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
            objectRegister = instruction.getRegisterD();
        }

        RegisterType objectRegisterType = getAndCheckSourceRegister(analyzedInstruction, objectRegister,
                ReferenceOrUninitCategories);

        if (objectRegisterType.category == RegisterType.Category.Null) {
            return false;
        }

        MethodIdItem methodIdItem = null;
        if (isSuper) {
            ClassPath.ClassDef classDef = ClassPath.getClassDef(this.encodedMethod.method.getContainingClass(), false);
            assert classDef != null;

            if (classDef.getSuperclass() != null) {
                methodIdItem = deodexUtil.lookupVirtualMethod(classDef.getSuperclass(), methodIndex);
            }

            if (methodIdItem == null) {
                //it's possible that the pre-odexed instruction had used the method from the current class instead
                //of from the superclass (although the superclass method is still what would actually be called).
                //And so the MethodIdItem for the superclass method may not be in the dex file. Let's try to get the
                //MethodIdItem for the method in the current class instead
                methodIdItem = deodexUtil.lookupVirtualMethod(classDef, methodIndex);
            }
        } else{
            methodIdItem = deodexUtil.lookupVirtualMethod(objectRegisterType.type, methodIndex);
        }

        if (methodIdItem == null) {
            throw new ValidationException(String.format("Could not resolve the method in class %s at index %d",
                    objectRegisterType.type.getClassType(), methodIndex));
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

            deodexedInstruction = new Instruction3rc(opcode, (short)instruction.getRegCount(),
                    instruction.getStartRegister(), methodIdItem);
        } else {
            Instruction35ms instruction = (Instruction35ms)analyzedInstruction.instruction;
            Opcode opcode;
            if (isSuper) {
                opcode = Opcode.INVOKE_SUPER;
            } else {
                opcode = Opcode.INVOKE_VIRTUAL;
            }

            deodexedInstruction = new Instruction35c(opcode, instruction.getRegCount(),
                    instruction.getRegisterD(), instruction.getRegisterE(), instruction.getRegisterF(),
                    instruction.getRegisterG(), instruction.getRegisterA(), methodIdItem);
        }

        analyzedInstruction.setDeodexedInstruction(deodexedInstruction);
        analyzeInstruction(analyzedInstruction);

        return true;
    }

    private boolean analyzePutGetVolatile(AnalyzedInstruction analyzedInstruction) {
        return analyzePutGetVolatile(analyzedInstruction, true);
    }

    private boolean analyzePutGetVolatile(AnalyzedInstruction analyzedInstruction, boolean analyzeResult) {
        FieldIdItem fieldIdItem =
                (FieldIdItem)(((InstructionWithReference)analyzedInstruction.instruction).getReferencedItem());

        String fieldType = fieldIdItem.getFieldType().getTypeDescriptor();

        Opcode opcode = OdexedFieldInstructionMapper.getAndCheckDeodexedOpcodeForOdexedOpcode(fieldType,
                analyzedInstruction.instruction.opcode);

        Instruction deodexedInstruction;

        if (analyzedInstruction.instruction.opcode.isOdexedStaticVolatile()) {
            SingleRegisterInstruction instruction = (SingleRegisterInstruction)analyzedInstruction.instruction;
            deodexedInstruction = new Instruction21c(opcode, (byte)instruction.getRegisterA(), fieldIdItem);
        } else {
            TwoRegisterInstruction instruction = (TwoRegisterInstruction)analyzedInstruction.instruction;

            deodexedInstruction = new Instruction22c(opcode, (byte)instruction.getRegisterA(),
                (byte)instruction.getRegisterB(), fieldIdItem);
        }

        analyzedInstruction.setDeodexedInstruction(deodexedInstruction);

        if (analyzeResult) {
            analyzeInstruction(analyzedInstruction);
        }
        return true;
    }

    private static boolean checkArrayFieldAssignment(RegisterType.Category arrayFieldCategory,
                                                  RegisterType.Category instructionCategory) {
        if (arrayFieldCategory == instructionCategory) {
            return true;
        }

        if ((arrayFieldCategory == RegisterType.Category.Integer &&
             instructionCategory == RegisterType.Category.Float) ||
            (arrayFieldCategory == RegisterType.Category.Float &&
             instructionCategory == RegisterType.Category.Integer)) {
            return true;
        }
        return false;
    }

    private static RegisterType getAndCheckSourceRegister(AnalyzedInstruction analyzedInstruction, int registerNumber,
                                            EnumSet validCategories) {
        assert registerNumber >= 0 && registerNumber < analyzedInstruction.postRegisterMap.length;

        RegisterType registerType = analyzedInstruction.getPreInstructionRegisterType(registerNumber);
        assert registerType != null;

        checkRegister(registerType, registerNumber, validCategories);

        if (validCategories == WideLowCategories) {
            checkRegister(registerType, registerNumber, WideLowCategories);
            checkWidePair(registerNumber, analyzedInstruction);

            RegisterType secondRegisterType = analyzedInstruction.getPreInstructionRegisterType(registerNumber + 1);
            assert secondRegisterType != null;
            checkRegister(secondRegisterType, registerNumber+1, WideHighCategories);
        }

        return registerType;
    }

    private static void checkRegister(RegisterType registerType, int registerNumber, EnumSet validCategories) {
        if (!validCategories.contains(registerType.category)) {
            throw new ValidationException(String.format("Invalid register type %s for register v%d.",
                    registerType.toString(), registerNumber));
        }
    }

    private static void checkWidePair(int registerNumber, AnalyzedInstruction analyzedInstruction) {
        if (registerNumber + 1 >= analyzedInstruction.postRegisterMap.length) {
            throw new ValidationException(String.format("v%d cannot be used as the first register in a wide register" +
                    "pair because it is the last register.", registerNumber));
        }
    }

    private static interface RegisterIterator {
        int getRegister();
        boolean moveNext();
        int getCount();
        boolean pastEnd();
    }

    private static class Format35cRegisterIterator implements RegisterIterator {
        private final int registerCount;
        private final int[] registers;
        private int currentRegister = 0;

        public Format35cRegisterIterator(FiveRegisterInstruction instruction) {
            registerCount = instruction.getRegCount();
            registers = new int[]{instruction.getRegisterD(), instruction.getRegisterE(),
                                  instruction.getRegisterF(), instruction.getRegisterG(),
                                  instruction.getRegisterA()};
        }

        public int getRegister() {
            return registers[currentRegister];
        }

        public boolean moveNext() {
            currentRegister++;
            return !pastEnd();
        }

        public int getCount() {
            return registerCount;
        }

        public boolean pastEnd() {
            return currentRegister >= registerCount;
        }
    }

    private static class Format3rcRegisterIterator implements RegisterIterator {
        private final int startRegister;
        private final int registerCount;
        private int currentRegister = 0;

        public Format3rcRegisterIterator(RegisterRangeInstruction instruction) {
            startRegister = instruction.getStartRegister();
            registerCount = instruction.getRegCount();
        }

        public int getRegister() {
            return startRegister + currentRegister;
        }

        public boolean moveNext() {
            currentRegister++;
            return !pastEnd();
        }

        public int getCount() {
            return registerCount;
        }

        public boolean pastEnd() {
            return currentRegister >= registerCount;
        }
    }
}