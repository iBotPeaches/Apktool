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

package org.jf.dexlib;

import org.jf.dexlib.Code.Format.*;
import org.jf.dexlib.Code.*;
import org.jf.dexlib.Debug.DebugInstructionIterator;
import org.jf.dexlib.Debug.DebugOpcode;
import org.jf.dexlib.Util.*;

import java.util.ArrayList;
import java.util.List;

public class CodeItem extends Item<CodeItem> {
    private int registerCount;
    private int inWords;
    private int outWords;
    private DebugInfoItem debugInfo;
    private Instruction[] instructions;
    private TryItem[] tries;
    private EncodedCatchHandler[] encodedCatchHandlers;

    private ClassDataItem.EncodedMethod parent;

    /**
     * Creates a new uninitialized <code>CodeItem</code>
     * @param dexFile The <code>DexFile</code> that this item belongs to
     */
    public CodeItem(DexFile dexFile) {
        super(dexFile);
    }

    /**
     * Creates a new <code>CodeItem</code> with the given values.
     * @param dexFile The <code>DexFile</code> that this item belongs to
     * @param registerCount the number of registers that the method containing this code uses
     * @param inWords the number of 2-byte words that the parameters to the method containing this code take
     * @param outWords the maximum number of 2-byte words for the arguments of any method call in this code
     * @param debugInfo the debug information for this code/method
     * @param instructions the instructions for this code item
     * @param tries an array of the tries defined for this code/method
     * @param encodedCatchHandlers an array of the exception handlers defined for this code/method
     */
    private CodeItem(DexFile dexFile,
                    int registerCount,
                    int inWords,
                    int outWords,
                    DebugInfoItem debugInfo,
                    Instruction[] instructions,
                    TryItem[] tries,
                    EncodedCatchHandler[] encodedCatchHandlers) {
        super(dexFile);

        this.registerCount = registerCount;
        this.inWords = inWords;
        this.outWords = outWords;
        this.debugInfo = debugInfo;
        if (debugInfo != null) {
            debugInfo.setParent(this);
        }

        this.instructions = instructions;
        this.tries = tries;
        this.encodedCatchHandlers = encodedCatchHandlers;
    }

    /**
     * Returns a new <code>CodeItem</code> with the given values.
     * @param dexFile The <code>DexFile</code> that this item belongs to
     * @param registerCount the number of registers that the method containing this code uses
     * @param inWords the number of 2-byte words that the parameters to the method containing this code take
     * @param outWords the maximum number of 2-byte words for the arguments of any method call in this code
     * @param debugInfo the debug information for this code/method
     * @param instructions the instructions for this code item
     * @param tries a list of the tries defined for this code/method or null if none
     * @param encodedCatchHandlers a list of the exception handlers defined for this code/method or null if none
     * @return a new <code>CodeItem</code> with the given values.
     */
    public static CodeItem internCodeItem(DexFile dexFile,
                    int registerCount,
                    int inWords,
                    int outWords,
                    DebugInfoItem debugInfo,
                    List<Instruction> instructions,
                    List<TryItem> tries,
                    List<EncodedCatchHandler> encodedCatchHandlers) {
        TryItem[] triesArray = null;
        EncodedCatchHandler[] encodedCatchHandlersArray = null;
        Instruction[] instructionsArray = null;

        if (tries != null && tries.size() > 0) {
            triesArray = new TryItem[tries.size()];
            tries.toArray(triesArray);
        }

        if (encodedCatchHandlers != null && encodedCatchHandlers.size() > 0) {
            encodedCatchHandlersArray = new EncodedCatchHandler[encodedCatchHandlers.size()];
            encodedCatchHandlers.toArray(encodedCatchHandlersArray);
        }

        if (instructions != null && instructions.size() > 0) {
            instructionsArray = new Instruction[instructions.size()];
            instructions.toArray(instructionsArray);
        }

        CodeItem codeItem = new CodeItem(dexFile, registerCount, inWords, outWords, debugInfo, instructionsArray,
                triesArray, encodedCatchHandlersArray);
        return dexFile.CodeItemsSection.intern(codeItem);
    }

    /** {@inheritDoc} */
    protected void readItem(Input in, ReadContext readContext) {
        this.registerCount = in.readShort();
        this.inWords = in.readShort();
        this.outWords = in.readShort();
        int triesCount = in.readShort();
        this.debugInfo = (DebugInfoItem)readContext.getOptionalOffsettedItemByOffset(ItemType.TYPE_DEBUG_INFO_ITEM,
                in.readInt());
        if (this.debugInfo != null) {
            this.debugInfo.setParent(this);
        }

        int instructionCount = in.readInt();

        final ArrayList<Instruction> instructionList = new ArrayList<Instruction>();

        byte[] encodedInstructions = in.readBytes(instructionCount * 2);
        InstructionIterator.IterateInstructions(dexFile, encodedInstructions,
                new InstructionIterator.ProcessInstructionDelegate() {
                    public void ProcessInstruction(int codeAddress, Instruction instruction) {
                        instructionList.add(instruction);
                    }
                });

        this.instructions = new Instruction[instructionList.size()];
        instructionList.toArray(instructions);

        if (triesCount > 0) {
            in.alignTo(4);

            //we need to read in the catch handlers first, so save the offset to the try items for future reference
            int triesOffset = in.getCursor();
            in.setCursor(triesOffset + 8 * triesCount);

            //read in the encoded catch handlers
            int encodedHandlerStart = in.getCursor();
            int handlerCount = in.readUnsignedLeb128();
            SparseArray<EncodedCatchHandler> handlerMap = new SparseArray<EncodedCatchHandler>(handlerCount);
            encodedCatchHandlers = new EncodedCatchHandler[handlerCount];
            for (int i=0; i<handlerCount; i++) {
                try {
                    int position = in.getCursor() - encodedHandlerStart;
                    encodedCatchHandlers[i] = new EncodedCatchHandler(dexFile, in);
                    handlerMap.append(position, encodedCatchHandlers[i]);
                } catch (Exception ex) {
                    throw ExceptionWithContext.withContext(ex, "Error while reading EncodedCatchHandler at index " + i);
                }
            }
            int codeItemEnd = in.getCursor();

            //now go back and read the tries
            in.setCursor(triesOffset);
            tries = new TryItem[triesCount];
            for (int i=0; i<triesCount; i++) {
                try {
                    tries[i] = new TryItem(in, handlerMap);
                } catch (Exception ex) {
                    throw ExceptionWithContext.withContext(ex, "Error while reading TryItem at index " + i);
                }
            }

            //and now back to the end of the code item
            in.setCursor(codeItemEnd);
        }
    }

    /** {@inheritDoc} */
    protected int placeItem(int offset) {
        offset += 16 + getInstructionsLength() * 2;

        if (tries != null && tries.length > 0) {
            offset = AlignmentUtils.alignOffset(offset, 4);

            offset += tries.length * 8;
            int encodedCatchHandlerBaseOffset = offset;
            offset += Leb128Utils.unsignedLeb128Size(encodedCatchHandlers.length);
            for (EncodedCatchHandler encodedCatchHandler: encodedCatchHandlers) {
                offset = encodedCatchHandler.place(offset, encodedCatchHandlerBaseOffset);
            }
        }
        return offset;
    }

    /** {@inheritDoc} */
    protected void writeItem(final AnnotatedOutput out) {
        int instructionsLength = getInstructionsLength();

        if (out.annotates()) {
            out.annotate(0, parent.method.getMethodString());
            out.annotate(2, "registers_size: 0x" + Integer.toHexString(registerCount) + " (" + registerCount + ")");
            out.annotate(2, "ins_size: 0x" + Integer.toHexString(inWords) + " (" + inWords + ")");
            out.annotate(2, "outs_size: 0x" + Integer.toHexString(outWords) + " (" + outWords + ")");
            int triesLength = tries==null?0:tries.length;
            out.annotate(2, "tries_size: 0x" + Integer.toHexString(triesLength) + " (" + triesLength + ")");
            if (debugInfo == null) {
                out.annotate(4, "debug_info_off:");
            } else {
                out.annotate(4, "debug_info_off: 0x" + Integer.toHexString(debugInfo.getOffset()));
            }
            out.annotate(4, "insns_size: 0x" + Integer.toHexString(instructionsLength) + " (" +
                    (instructionsLength) + ")");
        }

        out.writeShort(registerCount);
        out.writeShort(inWords);
        out.writeShort(outWords);
        if (tries == null) {
            out.writeShort(0);
        } else {
            out.writeShort(tries.length);
        }
        if (debugInfo == null) {
            out.writeInt(0);
        } else {
            out.writeInt(debugInfo.getOffset());
        }

        out.writeInt(instructionsLength);

        int currentCodeAddress = 0;
        for (Instruction instruction: instructions) {
            currentCodeAddress = instruction.write(out, currentCodeAddress);
        }

        if (tries != null && tries.length > 0) {
            if (out.annotates()) {
                if ((currentCodeAddress % 2) != 0) {
                    out.annotate("padding");
                    out.writeShort(0);
                }

                int index = 0;
                for (TryItem tryItem: tries) {
                    out.annotate(0, "[0x" + Integer.toHexString(index++) + "] try_item");
                    out.indent();
                    tryItem.writeTo(out);
                    out.deindent();
                }

                out.annotate("handler_count: 0x" + Integer.toHexString(encodedCatchHandlers.length) + "(" +
                        encodedCatchHandlers.length + ")");
                out.writeUnsignedLeb128(encodedCatchHandlers.length);

                index = 0;
                for (EncodedCatchHandler encodedCatchHandler: encodedCatchHandlers) {
                    out.annotate(0, "[" + Integer.toHexString(index++) + "] encoded_catch_handler");
                    out.indent();
                    encodedCatchHandler.writeTo(out);
                    out.deindent();
                }
            } else {
                if ((currentCodeAddress % 2) != 0) {
                    out.writeShort(0);
                }

                for (TryItem tryItem: tries) {
                    tryItem.writeTo(out);
                }

                out.writeUnsignedLeb128(encodedCatchHandlers.length);

                for (EncodedCatchHandler encodedCatchHandler: encodedCatchHandlers) {
                    encodedCatchHandler.writeTo(out);
                }
            }
        }
    }

    /** {@inheritDoc} */
    public ItemType getItemType() {
        return ItemType.TYPE_CODE_ITEM;
    }

    /** {@inheritDoc} */
    public String getConciseIdentity() {
        if (this.parent == null) {
            return "code_item @0x" + Integer.toHexString(getOffset());
        }
        return "code_item @0x" + Integer.toHexString(getOffset()) + " (" + parent.method.getMethodString() + ")";
    }

    /** {@inheritDoc} */
    public int compareTo(CodeItem other) {
        if (parent == null) {
            if (other.parent == null) {
                return 0;
            }
            return -1;
        }
        if (other.parent == null) {
            return 1;
        }
        return parent.method.compareTo(other.parent.method);
    }

    /**
     * @return the register count
     */
    public int getRegisterCount() {
        return registerCount;
    }

    /**
     * @return an array of the instructions in this code item
     */
    public Instruction[] getInstructions() {
        return instructions;
    }

    /**
     * @return an array of the <code>TryItem</code> objects in this <code>CodeItem</code>
     */
    public TryItem[] getTries() {
        return tries;
    }

    /**
     * @return an array of the <code>EncodedCatchHandler</code> objects in this <code>CodeItem</code>
     */
    public EncodedCatchHandler[] getHandlers() {
        return encodedCatchHandlers;
    }

    /**
     * @return the <code>DebugInfoItem</code> associated with this <code>CodeItem</code>
     */
    public DebugInfoItem getDebugInfo() {
        return debugInfo;
    }

    /**
     * @return the number of 2-byte words that the parameters to the method containing this code take
     */
    public int getInWords() {
        return inWords;
    }

    /**
     * @return the maximum number of 2-byte words for the arguments of any method call in this code
     */
    public int getOutWords() {
        return outWords;
    }

    /**
     * Sets the <code>MethodIdItem</code> of the method that this <code>CodeItem</code> is associated with
     * @param encodedMethod the <code>EncodedMethod</code> of the method that this <code>CodeItem</code> is associated
     * with
     */
    protected void setParent(ClassDataItem.EncodedMethod encodedMethod) {
        this.parent = encodedMethod;
    }

    /**
     * @return the MethodIdItem of the method that this CodeItem belongs to
     */
    public ClassDataItem.EncodedMethod getParent() {
        return parent;
    }

    /**
     * Used by OdexUtil to update this <code>CodeItem</code> with a deodexed version of the instructions
     * @param newInstructions the new instructions to use for this code item
     */
    public void updateCode(Instruction[] newInstructions) {
        this.instructions = newInstructions;
    }

    /**
     * @return The length of the instructions in this CodeItem, in 2-byte code blocks
     */
    private int getInstructionsLength() {
        int currentCodeAddress = 0;
        for (Instruction instruction: instructions) {
            currentCodeAddress += instruction.getSize(currentCodeAddress);
        }
        return currentCodeAddress;
    }

    /**
     * Go through the instructions and perform any of the following fixes that are applicable
     * - Replace const-string instruction with const-string/jumbo, when the string index is too big
     * - Replace goto and goto/16 with a larger version of goto, when the target is too far away
     * TODO: we should be able to replace if-* instructions with targets that are too far away with a negated if followed by a goto/32 to the original target
     * TODO: remove multiple nops that occur before a switch/array data pseudo instruction. In some cases, multiple smali-baksmali cycles with changes in between could cause nops to start piling up
     * TODO: in case of non-range invoke with a jumbo-sized method reference, we could check if the registers are sequential, and replace it with the jumbo variant (which only takes a register range)
     *
     * The above fixes are applied iteratively, until no more fixes have been performed
     */
    public void fixInstructions(boolean fixJumbo, boolean fixGoto) {
        try {
            boolean didSomething = false;

            do
            {
                didSomething = false;

                int currentCodeAddress = 0;
                for (int i=0; i<instructions.length; i++) {
                    Instruction instruction = instructions[i];

                    try {
                        if (fixGoto && instruction.opcode == Opcode.GOTO) {
                            int codeAddress = ((OffsetInstruction)instruction).getTargetAddressOffset();

                            if (((byte) codeAddress) != codeAddress) {
                                //the address doesn't fit within a byte, we need to upgrade to a goto/16 or goto/32

                                if ((short) codeAddress == codeAddress) {
                                    //the address fits in a short, so upgrade to a goto/16
                                    replaceInstructionAtAddress(currentCodeAddress,
                                            new Instruction20t(Opcode.GOTO_16, codeAddress));
                                }
                                else {
                                    //The address won't fit into a short, we have to upgrade to a goto/32
                                    replaceInstructionAtAddress(currentCodeAddress,
                                            new Instruction30t(Opcode.GOTO_32, codeAddress));
                                }
                                didSomething = true;
                                break;
                            }
                        } else if (fixGoto && instruction.opcode == Opcode.GOTO_16) {
                            int codeAddress = ((OffsetInstruction)instruction).getTargetAddressOffset();

                            if (((short) codeAddress) != codeAddress) {
                                //the address doesn't fit within a short, we need to upgrade to a goto/32
                                replaceInstructionAtAddress(currentCodeAddress,
                                        new Instruction30t(Opcode.GOTO_32, codeAddress));
                                didSomething = true;
                                break;
                            }
                        } else if (fixJumbo && instruction.opcode.hasJumboOpcode()) {
                            InstructionWithReference referenceInstruction = (InstructionWithReference)instruction;
                            if (referenceInstruction.getReferencedItem().getIndex() > 0xFFFF) {

                                InstructionWithJumboVariant instructionWithJumboVariant =
                                        (InstructionWithJumboVariant)referenceInstruction;

                                Instruction jumboInstruction = instructionWithJumboVariant.makeJumbo();
                                if (jumboInstruction != null) {
                                    replaceInstructionAtAddress(currentCodeAddress,
                                            instructionWithJumboVariant.makeJumbo());
                                    didSomething = true;
                                    break;
                                }
                            }
                        }

                        currentCodeAddress += instruction.getSize(currentCodeAddress);
                    } catch (Exception ex) {
                        throw ExceptionWithContext.withContext(ex, "Error while attempting to fix " +
                                instruction.opcode.name + " instruction at address " + currentCodeAddress);
                    }
                }
            }while(didSomething);
        } catch (Exception ex) {
            throw this.addExceptionContext(ex);
        }
    }

    private void replaceInstructionAtAddress(int codeAddress, Instruction replacementInstruction) {
        Instruction originalInstruction = null;

        int[] originalInstructionCodeAddresses = new int[instructions.length+1];
        SparseIntArray originalSwitchAddressByOriginalSwitchDataAddress = new SparseIntArray();

        int currentCodeAddress = 0;
        int instructionIndex = 0;
        int i;
        for (i=0; i<instructions.length; i++) {
            Instruction instruction = instructions[i];

            if (currentCodeAddress == codeAddress) {
                originalInstruction = instruction;
                instructionIndex = i;
            }

            if (instruction.opcode == Opcode.PACKED_SWITCH || instruction.opcode == Opcode.SPARSE_SWITCH) {
                OffsetInstruction offsetInstruction = (OffsetInstruction)instruction;

                int switchDataAddress = currentCodeAddress + offsetInstruction.getTargetAddressOffset();
                if (originalSwitchAddressByOriginalSwitchDataAddress.indexOfKey(switchDataAddress) < 0) {
                    originalSwitchAddressByOriginalSwitchDataAddress.put(switchDataAddress, currentCodeAddress);
                }
            }

            originalInstructionCodeAddresses[i] = currentCodeAddress;
            currentCodeAddress += instruction.getSize(currentCodeAddress);
        }
        //add the address just past the end of the last instruction, to help when fixing up try blocks that end
        //at the end of the method
        originalInstructionCodeAddresses[i] = currentCodeAddress;

        if (originalInstruction == null) {
            throw new RuntimeException("There is no instruction at address " + codeAddress);
        }

        instructions[instructionIndex] = replacementInstruction;

        //if we're replacing the instruction with one of the same size, we don't have to worry about fixing
        //up any address
        if (originalInstruction.getSize(codeAddress) == replacementInstruction.getSize(codeAddress)) {
            return;
        }

        final SparseIntArray originalAddressByNewAddress = new SparseIntArray();
        final SparseIntArray newAddressByOriginalAddress = new SparseIntArray();

        currentCodeAddress = 0;
        for (i=0; i<instructions.length; i++) {
            Instruction instruction = instructions[i];

            int originalAddress = originalInstructionCodeAddresses[i];
            originalAddressByNewAddress.append(currentCodeAddress, originalAddress);
            newAddressByOriginalAddress.append(originalAddress, currentCodeAddress);

            currentCodeAddress += instruction.getSize(currentCodeAddress);
        }

        //add the address just past the end of the last instruction, to help when fixing up try blocks that end
        //at the end of the method
        originalAddressByNewAddress.append(currentCodeAddress, originalInstructionCodeAddresses[i]);
        newAddressByOriginalAddress.append(originalInstructionCodeAddresses[i], currentCodeAddress);

        //update any "offset" instructions, or switch data instructions
        currentCodeAddress = 0;
        for (i=0; i<instructions.length; i++) {
            Instruction instruction = instructions[i];

            if (instruction instanceof OffsetInstruction) {
                OffsetInstruction offsetInstruction = (OffsetInstruction)instruction;

                assert originalAddressByNewAddress.indexOfKey(currentCodeAddress) >= 0;
                int originalAddress = originalAddressByNewAddress.get(currentCodeAddress);

                int originalInstructionTarget = originalAddress + offsetInstruction.getTargetAddressOffset();

                assert newAddressByOriginalAddress.indexOfKey(originalInstructionTarget) >= 0;
                int newInstructionTarget = newAddressByOriginalAddress.get(originalInstructionTarget);

                int newCodeAddress = (newInstructionTarget - currentCodeAddress);

                if (newCodeAddress != offsetInstruction.getTargetAddressOffset()) {
                    offsetInstruction.updateTargetAddressOffset(newCodeAddress);
                }
            } else if (instruction instanceof MultiOffsetInstruction) {
                MultiOffsetInstruction multiOffsetInstruction = (MultiOffsetInstruction)instruction;

                assert originalAddressByNewAddress.indexOfKey(currentCodeAddress) >= 0;
                int originalDataAddress = originalAddressByNewAddress.get(currentCodeAddress);

                int originalSwitchAddress =
                        originalSwitchAddressByOriginalSwitchDataAddress.get(originalDataAddress, -1);
                if (originalSwitchAddress == -1) {
                    //TODO: maybe we could just remove the unreferenced switch data?
                    throw new RuntimeException("This method contains an unreferenced switch data block at address " +
                            + currentCodeAddress + " and can't be automatically fixed.");
                }

                assert newAddressByOriginalAddress.indexOfKey(originalSwitchAddress) >= 0;
                int newSwitchAddress = newAddressByOriginalAddress.get(originalSwitchAddress);

                int[] targets = multiOffsetInstruction.getTargets();
                for (int t=0; t<targets.length; t++) {
                    int originalTargetCodeAddress = originalSwitchAddress + targets[t];
                    assert newAddressByOriginalAddress.indexOfKey(originalTargetCodeAddress) >= 0;
                    int newTargetCodeAddress = newAddressByOriginalAddress.get(originalTargetCodeAddress);
                    int newCodeAddress = newTargetCodeAddress - newSwitchAddress;
                    if (newCodeAddress != targets[t]) {
                        multiOffsetInstruction.updateTarget(t, newCodeAddress);
                    }
                }
            }
            currentCodeAddress += instruction.getSize(currentCodeAddress);
        }

        if (debugInfo != null) {
            final byte[] encodedDebugInfo = debugInfo.getEncodedDebugInfo();

            ByteArrayInput debugInput = new ByteArrayInput(encodedDebugInfo);

            DebugInstructionFixer debugInstructionFixer = new DebugInstructionFixer(encodedDebugInfo,
                newAddressByOriginalAddress);
            DebugInstructionIterator.IterateInstructions(debugInput, debugInstructionFixer);

            if (debugInstructionFixer.result != null) {
                debugInfo.setEncodedDebugInfo(debugInstructionFixer.result);
            }
        }

        if (encodedCatchHandlers != null) {
            for (EncodedCatchHandler encodedCatchHandler: encodedCatchHandlers) {
                if (encodedCatchHandler.catchAllHandlerAddress != -1) {
                    assert newAddressByOriginalAddress.indexOfKey(encodedCatchHandler.catchAllHandlerAddress) >= 0;
                    encodedCatchHandler.catchAllHandlerAddress =
                            newAddressByOriginalAddress.get(encodedCatchHandler.catchAllHandlerAddress);
                }

                for (EncodedTypeAddrPair handler: encodedCatchHandler.handlers) {
                    assert newAddressByOriginalAddress.indexOfKey(handler.handlerAddress) >= 0;
                    handler.handlerAddress = newAddressByOriginalAddress.get(handler.handlerAddress);
                }
            }
        }

        if (this.tries != null) {
            for (TryItem tryItem: tries) {
                int startAddress = tryItem.startCodeAddress;
                int endAddress = tryItem.startCodeAddress + tryItem.tryLength;

                assert newAddressByOriginalAddress.indexOfKey(startAddress) >= 0;
                tryItem.startCodeAddress = newAddressByOriginalAddress.get(startAddress);

                assert newAddressByOriginalAddress.indexOfKey(endAddress) >= 0;
                tryItem.tryLength = newAddressByOriginalAddress.get(endAddress) - tryItem.startCodeAddress;
            }
        }
    }

    private class DebugInstructionFixer extends DebugInstructionIterator.ProcessRawDebugInstructionDelegate {
        private int currentCodeAddress = 0;
        private SparseIntArray newAddressByOriginalAddress;
        private final byte[] originalEncodedDebugInfo;
        public byte[] result = null;

        public DebugInstructionFixer(byte[] originalEncodedDebugInfo, SparseIntArray newAddressByOriginalAddress) {
            this.newAddressByOriginalAddress = newAddressByOriginalAddress;
            this.originalEncodedDebugInfo = originalEncodedDebugInfo;
        }


        @Override
        public void ProcessAdvancePC(int startDebugOffset, int debugInstructionLength, int codeAddressDelta) {
            currentCodeAddress += codeAddressDelta;

            if (result != null) {
                return;
            }

            int newCodeAddress = newAddressByOriginalAddress.get(currentCodeAddress, -1);

            //The address might not point to an actual instruction in some cases, for example, if an AdvancePC
            //instruction was inserted just before a "special" instruction, to fix up the addresses for a previous
            //instruction replacement.
            //In this case, it should be safe to skip, because there will be another AdvancePC/SpecialOpcode that will
            //bump up the address to point to a valid instruction before anything (line/local/etc.) is emitted
            if (newCodeAddress == -1) {
                return;
            }

            if (newCodeAddress != currentCodeAddress) {
                int newCodeAddressDelta = newCodeAddress - (currentCodeAddress - codeAddressDelta);
                assert newCodeAddressDelta > 0;
                int codeAddressDeltaLeb128Size = Leb128Utils.unsignedLeb128Size(newCodeAddressDelta);

                //if the length of the new code address delta is the same, we can use the existing buffer
                if (codeAddressDeltaLeb128Size + 1 == debugInstructionLength) {
                    result = originalEncodedDebugInfo;
                    Leb128Utils.writeUnsignedLeb128(newCodeAddressDelta, result, startDebugOffset+1);
                } else {
                    //The length of the new code address delta is different, so create a new buffer with enough
                    //additional space to accomodate the new code address delta value.
                    result = new byte[originalEncodedDebugInfo.length + codeAddressDeltaLeb128Size -
                            (debugInstructionLength - 1)];

                    System.arraycopy(originalEncodedDebugInfo, 0, result, 0, startDebugOffset);

                    result[startDebugOffset] = DebugOpcode.DBG_ADVANCE_PC.value;
                    Leb128Utils.writeUnsignedLeb128(newCodeAddressDelta, result, startDebugOffset+1);

                    System.arraycopy(originalEncodedDebugInfo, startDebugOffset + debugInstructionLength, result,
                            startDebugOffset + codeAddressDeltaLeb128Size + 1,
                            originalEncodedDebugInfo.length - (startDebugOffset + codeAddressDeltaLeb128Size + 1));
                }
            }
        }

        @Override
        public void ProcessSpecialOpcode(int startDebugOffset, int debugOpcode, int lineDelta,
                                         int codeAddressDelta) {
            currentCodeAddress += codeAddressDelta;
            if (result != null) {
                return;
            }

            int newCodeAddress = newAddressByOriginalAddress.get(currentCodeAddress, -1);
            assert newCodeAddress != -1;

            if (newCodeAddress != currentCodeAddress) {
                int newCodeAddressDelta = newCodeAddress - (currentCodeAddress - codeAddressDelta);
                assert newCodeAddressDelta > 0;

                //if the new code address delta won't fit in the special opcode, we need to insert
                //an additional DBG_ADVANCE_PC opcode
                if (lineDelta < 2 && newCodeAddressDelta > 16 || lineDelta > 1 && newCodeAddressDelta > 15) {
                    int additionalCodeAddressDelta = newCodeAddress - currentCodeAddress;
                    int additionalCodeAddressDeltaLeb128Size = Leb128Utils.signedLeb128Size(additionalCodeAddressDelta);

                    //create a new buffer with enough additional space for the new opcode
                    result = new byte[originalEncodedDebugInfo.length + additionalCodeAddressDeltaLeb128Size + 1];

                    System.arraycopy(originalEncodedDebugInfo, 0, result, 0, startDebugOffset);
                    result[startDebugOffset] = 0x01; //DBG_ADVANCE_PC
                    Leb128Utils.writeUnsignedLeb128(additionalCodeAddressDelta, result, startDebugOffset+1);
                    System.arraycopy(originalEncodedDebugInfo, startDebugOffset, result,
                            startDebugOffset+additionalCodeAddressDeltaLeb128Size+1,
                            result.length - (startDebugOffset+additionalCodeAddressDeltaLeb128Size+1));
                } else {
                    result = originalEncodedDebugInfo;
                    result[startDebugOffset] = DebugInfoBuilder.calculateSpecialOpcode(lineDelta,
                            newCodeAddressDelta);
                }
            }
        }
    }

    public static class TryItem {
        /**
         * The address (in 2-byte words) within the code where the try block starts
         */
        private int startCodeAddress;

        /**
         * The number of 2-byte words that the try block covers
         */
        private int tryLength;

        /**
         * The associated exception handler
         */
        public final EncodedCatchHandler encodedCatchHandler;

        /**
         * Construct a new <code>TryItem</code> with the given values
         * @param startCodeAddress the code address within the code where the try block starts
         * @param tryLength the number of code blocks that the try block covers
         * @param encodedCatchHandler the associated exception handler
         */
        public TryItem(int startCodeAddress, int tryLength, EncodedCatchHandler encodedCatchHandler) {
            this.startCodeAddress = startCodeAddress;
            this.tryLength = tryLength;
            this.encodedCatchHandler = encodedCatchHandler;
        }

        /**
         * This is used internally to construct a new <code>TryItem</code> while reading in a <code>DexFile</code>
         * @param in the Input object to read the <code>TryItem</code> from
         * @param encodedCatchHandlers a SparseArray of the EncodedCatchHandlers for this <code>CodeItem</code>. The
         * key should be the offset of the EncodedCatchHandler from the beginning of the encoded_catch_handler_list
         * structure.
         */
        private TryItem(Input in, SparseArray<EncodedCatchHandler> encodedCatchHandlers) {
            startCodeAddress = in.readInt();
            tryLength = in.readShort();

            encodedCatchHandler = encodedCatchHandlers.get(in.readShort());
            if (encodedCatchHandler == null) {
                throw new RuntimeException("Could not find the EncodedCatchHandler referenced by this TryItem");
            }
        }

        /**
         * Writes the <code>TryItem</code> to the given <code>AnnotatedOutput</code> object
         * @param out the <code>AnnotatedOutput</code> object to write to
         */
        private void writeTo(AnnotatedOutput out) {
            if (out.annotates()) {
                out.annotate(4, "start_addr: 0x" + Integer.toHexString(startCodeAddress));
                out.annotate(2, "try_length: 0x" + Integer.toHexString(tryLength) + " (" + tryLength +
                        ")");
                out.annotate(2, "handler_off: 0x" + Integer.toHexString(encodedCatchHandler.getOffsetInList()));
            }

            out.writeInt(startCodeAddress);
            out.writeShort(tryLength);
            out.writeShort(encodedCatchHandler.getOffsetInList());
        }

        /**
         * @return The address (in 2-byte words) within the code where the try block starts
         */
        public int getStartCodeAddress() {
            return startCodeAddress;
        }

        /**
         * @return The number of code blocks that the try block covers
         */
        public int getTryLength() {
            return tryLength;
        }
    }

    public static class EncodedCatchHandler {
        /**
         * An array of the individual exception handlers
         */
        public final EncodedTypeAddrPair[] handlers;

        /**
         * The address within the code (in 2-byte words) for the catch all handler, or -1 if there is no catch all
         * handler
         */
        private int catchAllHandlerAddress;

        private int baseOffset;
        private int offset;

        /**
         * Constructs a new <code>EncodedCatchHandler</code> with the given values
         * @param handlers an array of the individual exception handlers
         * @param catchAllHandlerAddress The address within the code (in 2-byte words) for the catch all handler, or -1
         * if there is no catch all handler
         */
        public EncodedCatchHandler(EncodedTypeAddrPair[] handlers, int catchAllHandlerAddress) {
            this.handlers = handlers;
            this.catchAllHandlerAddress = catchAllHandlerAddress;
        }

        /**
         * This is used internally to construct a new <code>EncodedCatchHandler</code> while reading in a
         * <code>DexFile</code>
         * @param dexFile the <code>DexFile</code> that is being read in
         * @param in the Input object to read the <code>EncodedCatchHandler</code> from
         */
        private EncodedCatchHandler(DexFile dexFile, Input in) {
            int handlerCount = in.readSignedLeb128();

            if (handlerCount < 0) {
                handlers = new EncodedTypeAddrPair[-1 * handlerCount];
            } else {
                handlers = new EncodedTypeAddrPair[handlerCount];
            }

            for (int i=0; i<handlers.length; i++) {
                try {
                    handlers[i] = new EncodedTypeAddrPair(dexFile, in);
                } catch (Exception ex) {
                    throw ExceptionWithContext.withContext(ex, "Error while reading EncodedTypeAddrPair at index " + i);
                }
            }

            if (handlerCount <= 0) {
                catchAllHandlerAddress = in.readUnsignedLeb128();
            } else {
                catchAllHandlerAddress = -1;
            }
        }

        /**
         * @return the "Catch All" handler address for this <code>EncodedCatchHandler</code>, or -1 if there
         * is no "Catch All" handler
         */
        public int getCatchAllHandlerAddress() {
            return catchAllHandlerAddress;
        }

        /**
         * @return the offset of this <code>EncodedCatchHandler</code> from the beginning of the
         * encoded_catch_handler_list structure
         */
        private int getOffsetInList() {
            return offset-baseOffset;
        }

        /**
         * Places the <code>EncodedCatchHandler</code>, storing the offset and baseOffset, and returning the offset
         * immediately following this <code>EncodedCatchHandler</code>
         * @param offset the offset of this <code>EncodedCatchHandler</code> in the <code>DexFile</code>
         * @param baseOffset the offset of the beginning of the encoded_catch_handler_list structure in the
         * <code>DexFile</code>
         * @return the offset immediately following this <code>EncodedCatchHandler</code>
         */
        private int place(int offset, int baseOffset) {
            this.offset = offset;
            this.baseOffset = baseOffset;

            int size = handlers.length;
            if (catchAllHandlerAddress > -1) {
                size *= -1;
                offset += Leb128Utils.unsignedLeb128Size(catchAllHandlerAddress);
            }
            offset += Leb128Utils.signedLeb128Size(size);

            for (EncodedTypeAddrPair handler: handlers) {
                offset += handler.getSize();
            }
            return offset;
        }

        /**
         * Writes the <code>EncodedCatchHandler</code> to the given <code>AnnotatedOutput</code> object
         * @param out the <code>AnnotatedOutput</code> object to write to
         */
        private void writeTo(AnnotatedOutput out) {
            if (out.annotates()) {
                out.annotate("size: 0x" + Integer.toHexString(handlers.length) + " (" + handlers.length + ")");

                int size = handlers.length;
                if (catchAllHandlerAddress > -1) {
                    size = size * -1;
                }
                out.writeSignedLeb128(size);

                int index = 0;
                for (EncodedTypeAddrPair handler: handlers) {
                    out.annotate(0, "[" + index++ + "] encoded_type_addr_pair");
                    out.indent();
                    handler.writeTo(out);
                    out.deindent();
                }

                if (catchAllHandlerAddress > -1) {
                    out.annotate("catch_all_addr: 0x" + Integer.toHexString(catchAllHandlerAddress));
                    out.writeUnsignedLeb128(catchAllHandlerAddress);
                }
            } else {
                int size = handlers.length;
                if (catchAllHandlerAddress > -1) {
                    size = size * -1;
                }
                out.writeSignedLeb128(size);

                for (EncodedTypeAddrPair handler: handlers) {
                    handler.writeTo(out);
                }

                if (catchAllHandlerAddress > -1) {
                    out.writeUnsignedLeb128(catchAllHandlerAddress);
                }
            }
        }

        @Override
        public int hashCode() {
            int hash = 0;
            for (EncodedTypeAddrPair handler: handlers) {
                hash = hash * 31 + handler.hashCode();
            }
            hash = hash * 31 + catchAllHandlerAddress;
            return hash;
        }

        @Override
        public boolean equals(Object o) {
            if (this==o) {
                return true;
            }
            if (o==null || !this.getClass().equals(o.getClass())) {
                return false;
            }

            EncodedCatchHandler other = (EncodedCatchHandler)o;
            if (handlers.length != other.handlers.length || catchAllHandlerAddress != other.catchAllHandlerAddress) {
                return false;
            }

            for (int i=0; i<handlers.length; i++) {
                if (!handlers[i].equals(other.handlers[i])) {
                    return false;
                }
            }

            return true;
        }
    }

    public static class EncodedTypeAddrPair {
        /**
         * The type of the <code>Exception</code> that this handler handles
         */
        public final TypeIdItem exceptionType;

        /**
         * The address (in 2-byte words) in the code of the handler
         */
        private int handlerAddress;

        /**
         * Constructs a new <code>EncodedTypeAddrPair</code> with the given values
         * @param exceptionType the type of the <code>Exception</code> that this handler handles
         * @param handlerAddress the address (in 2-byte words) in the code of the handler
         */
        public EncodedTypeAddrPair(TypeIdItem exceptionType, int handlerAddress) {
            this.exceptionType = exceptionType;
            this.handlerAddress = handlerAddress;
        }

        /**
         * This is used internally to construct a new <code>EncodedTypeAddrPair</code> while reading in a
         * <code>DexFile</code>
         * @param dexFile the <code>DexFile</code> that is being read in
         * @param in the Input object to read the <code>EncodedCatchHandler</code> from
         */
        private EncodedTypeAddrPair(DexFile dexFile, Input in) {
            exceptionType = dexFile.TypeIdsSection.getItemByIndex(in.readUnsignedLeb128());
            handlerAddress = in.readUnsignedLeb128();
        }

        /**
         * @return the size of this <code>EncodedTypeAddrPair</code>
         */
        private int getSize() {
            return Leb128Utils.unsignedLeb128Size(exceptionType.getIndex()) +
                   Leb128Utils.unsignedLeb128Size(handlerAddress);
        }

        /**
         * Writes the <code>EncodedTypeAddrPair</code> to the given <code>AnnotatedOutput</code> object
         * @param out the <code>AnnotatedOutput</code> object to write to
         */
        private void writeTo(AnnotatedOutput out) {
            if (out.annotates()) {
                out.annotate("exception_type: " + exceptionType.getTypeDescriptor());
                out.writeUnsignedLeb128(exceptionType.getIndex());

                out.annotate("handler_addr: 0x" + Integer.toHexString(handlerAddress));
                out.writeUnsignedLeb128(handlerAddress);
            } else {
                out.writeUnsignedLeb128(exceptionType.getIndex());
                out.writeUnsignedLeb128(handlerAddress);
            }
        }

        public int getHandlerAddress() {
            return handlerAddress;
        }

        @Override
        public int hashCode() {
            return exceptionType.hashCode() * 31 + handlerAddress;
        }

        @Override
        public boolean equals(Object o) {
            if (this==o) {
                return true;
            }
            if (o==null || !this.getClass().equals(o.getClass())) {
                return false;
            }

            EncodedTypeAddrPair other = (EncodedTypeAddrPair)o;
            return exceptionType == other.exceptionType && handlerAddress == other.handlerAddress;
        }
    }
}
