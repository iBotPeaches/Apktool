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

package org.jf.dexlib2.builder;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.jf.dexlib2.DebugItemType;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.builder.debug.*;
import org.jf.dexlib2.builder.instruction.*;
import org.jf.dexlib2.iface.ExceptionHandler;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.TryBlock;
import org.jf.dexlib2.iface.debug.*;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.SwitchElement;
import org.jf.dexlib2.iface.instruction.formats.*;
import org.jf.dexlib2.iface.reference.TypeReference;
import org.jf.util.ExceptionWithContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class MutableMethodImplementation implements MethodImplementation {
    private final int registerCount;
    final ArrayList<MethodLocation> instructionList = Lists.newArrayList(new MethodLocation(null, 0, 0));
    private final ArrayList<BuilderTryBlock> tryBlocks = Lists.newArrayList();
    private boolean fixInstructions = true;

    public MutableMethodImplementation(@Nonnull MethodImplementation methodImplementation) {
        this.registerCount = methodImplementation.getRegisterCount();

        int codeAddress = 0;
        int index = 0;

        for (Instruction instruction: methodImplementation.getInstructions()) {
            codeAddress += instruction.getCodeUnits();
            index++;

            instructionList.add(new MethodLocation(null, codeAddress, index));
        }

        final int[] codeAddressToIndex = new int[codeAddress+1];
        Arrays.fill(codeAddressToIndex, -1);

        for (int i=0; i<instructionList.size(); i++) {
            codeAddressToIndex[instructionList.get(i).codeAddress] = i;
        }

        List<Task> switchPayloadTasks = Lists.newArrayList();
        index = 0;
        for (final Instruction instruction: methodImplementation.getInstructions()) {
            final MethodLocation location = instructionList.get(index);
            final Opcode opcode = instruction.getOpcode();
            if (opcode == Opcode.PACKED_SWITCH_PAYLOAD || opcode == Opcode.SPARSE_SWITCH_PAYLOAD) {
                switchPayloadTasks.add(new Task() {
                    @Override public void perform() {
                        convertAndSetInstruction(location, codeAddressToIndex, instruction);
                    }
                });
            } else {
                convertAndSetInstruction(location, codeAddressToIndex, instruction);
            }
            index++;
        }

        // the switch payload instructions must be converted last, so that any switch statements that refer to them
        // have created the referring labels that we look for
        for (Task switchPayloadTask: switchPayloadTasks) {
            switchPayloadTask.perform();
        }

        for (DebugItem debugItem: methodImplementation.getDebugItems()) {
            int debugCodeAddress = debugItem.getCodeAddress();
            int locationIndex = mapCodeAddressToIndex(codeAddressToIndex, debugCodeAddress);
            MethodLocation debugLocation = instructionList.get(locationIndex);
            BuilderDebugItem builderDebugItem = convertDebugItem(debugItem);
            debugLocation.getDebugItems().add(builderDebugItem);
            builderDebugItem.location = debugLocation;
        }

        for (TryBlock<? extends ExceptionHandler> tryBlock: methodImplementation.getTryBlocks()) {
            Label startLabel = newLabel(codeAddressToIndex, tryBlock.getStartCodeAddress());
            Label endLabel = newLabel(codeAddressToIndex, tryBlock.getStartCodeAddress() + tryBlock.getCodeUnitCount());

            for (ExceptionHandler exceptionHandler: tryBlock.getExceptionHandlers()) {
                tryBlocks.add(new BuilderTryBlock(startLabel, endLabel,
                        exceptionHandler.getExceptionTypeReference(),
                        newLabel(codeAddressToIndex, exceptionHandler.getHandlerCodeAddress())));
            }
        }
    }

    private interface Task {
        void perform();
    }

    public MutableMethodImplementation(int registerCount) {
        this.registerCount = registerCount;
    }

    @Override public int getRegisterCount() {
        return registerCount;
    }

    @Nonnull
    public List<BuilderInstruction> getInstructions() {
        if (fixInstructions) {
            fixInstructions();
        }

        return new AbstractList<BuilderInstruction>() {
            @Override public BuilderInstruction get(int i) {
                if (i >= size()) {
                    throw new IndexOutOfBoundsException();
                }
                if (fixInstructions) {
                    fixInstructions();
                }
                return instructionList.get(i).instruction;
            }

            @Override public int size() {
                if (fixInstructions) {
                    fixInstructions();
                }
                // don't include the last MethodLocation, which always has a null instruction
                return instructionList.size() - 1;
            }
        };
    }

    @Nonnull @Override public List<BuilderTryBlock> getTryBlocks() {
        if (fixInstructions) {
            fixInstructions();
        }
        return Collections.unmodifiableList(tryBlocks);
    }

    @Nonnull @Override public Iterable<? extends DebugItem> getDebugItems() {
        if (fixInstructions) {
            fixInstructions();
        }
        return Iterables.concat(
                Iterables.transform(instructionList, new Function<MethodLocation, Iterable<? extends DebugItem>>() {
                    @Nullable @Override public Iterable<? extends DebugItem> apply(@Nullable MethodLocation input) {
                        assert input != null;
                        if (fixInstructions) {
                            throw new IllegalStateException("This iterator was invalidated by a change to" +
                                    " this MutableMethodImplementation.");
                        }
                        return input.getDebugItems();
                    }
                }));
    }

    public void addCatch(@Nullable TypeReference type, @Nonnull Label from,
                         @Nonnull Label to, @Nonnull Label handler) {
        tryBlocks.add(new BuilderTryBlock(from, to, type, handler));
    }

    public void addCatch(@Nullable String type, @Nonnull Label from, @Nonnull Label to,
                         @Nonnull Label handler) {
        tryBlocks.add(new BuilderTryBlock(from, to, type, handler));
    }

    public void addCatch(@Nonnull Label from, @Nonnull Label to, @Nonnull Label handler) {
        tryBlocks.add(new BuilderTryBlock(from, to, handler));
    }

    public void addInstruction(int index, BuilderInstruction instruction) {
        // the end check here is intentially >= rather than >, because the list always includes an "empty"
        // (null instruction) MethodLocation at the end. To add an instruction to the end of the list, the user would
        // provide the index of this empty item, which would be size() - 1.
        if (index >= instructionList.size()) {
            throw new IndexOutOfBoundsException();
        }

        if (index == instructionList.size() - 1) {
            addInstruction(instruction);
            return;
        }
        int codeAddress = instructionList.get(index).getCodeAddress();
        MethodLocation newLoc = new MethodLocation(instruction, codeAddress, index);
        instructionList.add(index, newLoc);
        instruction.location = newLoc;

        codeAddress += instruction.getCodeUnits();

        for (int i=index+1; i<instructionList.size(); i++) {
            MethodLocation location = instructionList.get(i);
            location.index++;
            location.codeAddress = codeAddress;
            if (location.instruction != null) {
                codeAddress += location.instruction.getCodeUnits();
            } else {
                // only the last MethodLocation should have a null instruction
                assert i == instructionList.size()-1;
            }
        }

        this.fixInstructions = true;
    }

    public void addInstruction(@Nonnull BuilderInstruction instruction) {
        MethodLocation last = instructionList.get(instructionList.size()-1);
        last.instruction = instruction;
        instruction.location = last;

        int nextCodeAddress = last.codeAddress + instruction.getCodeUnits();
        instructionList.add(new MethodLocation(null, nextCodeAddress, instructionList.size()));

        this.fixInstructions = true;
    }

    public void replaceInstruction(int index, @Nonnull BuilderInstruction replacementInstruction) {
        if (index >= instructionList.size() - 1) {
            throw new IndexOutOfBoundsException();
        }

        MethodLocation replaceLocation = instructionList.get(index);
        replacementInstruction.location = replaceLocation;
        BuilderInstruction old = replaceLocation.instruction;
        assert old != null;
        old.location = null;
        replaceLocation.instruction = replacementInstruction;

        // TODO: factor out index/address fix up loop
        int codeAddress = replaceLocation.codeAddress + replaceLocation.instruction.getCodeUnits();
        for (int i=index+1; i<instructionList.size(); i++) {
            MethodLocation location = instructionList.get(i);
            location.codeAddress = codeAddress;

            Instruction instruction = location.getInstruction();
            if (instruction != null) {
                codeAddress += instruction.getCodeUnits();
            } else {
                assert i == instructionList.size() - 1;
            }
        }

        this.fixInstructions = true;
    }

    public void removeInstruction(int index) {
        if (index >= instructionList.size() - 1) {
            throw new IndexOutOfBoundsException();
        }

        MethodLocation toRemove = instructionList.get(index);
        toRemove.instruction = null;
        MethodLocation next = instructionList.get(index+1);
        toRemove.mergeInto(next);

        instructionList.remove(index);
        int codeAddress = toRemove.codeAddress;
        for (int i=index; i<instructionList.size(); i++) {
            MethodLocation location = instructionList.get(i);
            location.index = i;
            location.codeAddress = codeAddress;

            Instruction instruction = location.getInstruction();
            if (instruction != null) {
                codeAddress += instruction.getCodeUnits();
            } else {
                assert i == instructionList.size() - 1;
            }
        }

        this.fixInstructions = true;
    }

    public void swapInstructions(int index1, int index2) {
        if (index1 >= instructionList.size() - 1 || index2 >= instructionList.size() - 1) {
            throw new IndexOutOfBoundsException();
        }
        MethodLocation first = instructionList.get(index1);
        MethodLocation second = instructionList.get(index2);

        // only the last MethodLocation may have a null instruction
        assert first.instruction != null;
        assert second.instruction != null;

        first.instruction.location = second;
        second.instruction.location = first;

        {
            BuilderInstruction tmp = second.instruction;
            second.instruction = first.instruction;
            first.instruction = tmp;
        }

        if (index2 < index1) {
            int tmp = index2;
            index2 = index1;
            index1 = tmp;
        }

        int codeAddress = first.codeAddress + first.instruction.getCodeUnits();
        for (int i=index1+1; i<=index2; i++) {
            MethodLocation location = instructionList.get(i);
            location.codeAddress = codeAddress;

            Instruction instruction = location.instruction;
            assert instruction != null;
            codeAddress += location.instruction.getCodeUnits();
        }

        this.fixInstructions = true;
    }

    @Nullable
    private BuilderInstruction getFirstNonNop(int startIndex) {

        for (int i=startIndex; i<instructionList.size()-1; i++) {
            BuilderInstruction instruction = instructionList.get(i).instruction;
            assert instruction != null;
            if (instruction.getOpcode() != Opcode.NOP) {
                return instruction;
            }
        }
        return null;
    }

    private void fixInstructions() {
        HashSet<MethodLocation> payloadLocations = Sets.newHashSet();

        for (MethodLocation location: instructionList) {
            BuilderInstruction instruction = location.instruction;
            if (instruction != null) {
                switch (instruction.getOpcode()) {
                    case SPARSE_SWITCH:
                    case PACKED_SWITCH: {
                        MethodLocation targetLocation =
                                ((BuilderOffsetInstruction)instruction).getTarget().getLocation();
                        BuilderInstruction targetInstruction = targetLocation.instruction;
                        if (targetInstruction == null) {
                            throw new IllegalStateException(String.format("Switch instruction at address/index " +
                                    "0x%x/%d points to the end of the method.", location.codeAddress, location.index));
                        }

                        if (targetInstruction.getOpcode() == Opcode.NOP) {
                            targetInstruction = getFirstNonNop(targetLocation.index+1);
                        }
                        if (targetInstruction == null || !(targetInstruction instanceof BuilderSwitchPayload)) {
                            throw new IllegalStateException(String.format("Switch instruction at address/index " +
                                    "0x%x/%d does not refer to a payload instruction.",
                                    location.codeAddress, location.index));
                        }
                        if ((instruction.opcode == Opcode.PACKED_SWITCH &&
                                targetInstruction.getOpcode() != Opcode.PACKED_SWITCH_PAYLOAD) ||
                            (instruction.opcode == Opcode.SPARSE_SWITCH &&
                                targetInstruction.getOpcode() != Opcode.SPARSE_SWITCH_PAYLOAD)) {
                            throw new IllegalStateException(String.format("Switch instruction at address/index " +
                                    "0x%x/%d refers to the wrong type of payload instruction.",
                                    location.codeAddress, location.index));
                        }

                        if (!payloadLocations.add(targetLocation)) {
                            throw new IllegalStateException("Multiple switch instructions refer to the same payload. " +
                                    "This is not currently supported. Please file a bug :)");
                        }

                        ((BuilderSwitchPayload)targetInstruction).referrer = location;
                        break;
                    }
                }
            }
        }

        boolean madeChanges;
        do {
            madeChanges = false;

            for (int index=0; index<instructionList.size(); index++) {
                MethodLocation location = instructionList.get(index);
                BuilderInstruction instruction = location.instruction;
                if (instruction != null) {
                    switch (instruction.getOpcode()) {
                        case GOTO: {
                            int offset = ((BuilderOffsetInstruction)instruction).internalGetCodeOffset();
                            if (offset < Byte.MIN_VALUE || offset > Byte.MAX_VALUE) {
                                BuilderOffsetInstruction replacement;
                                if (offset < Short.MIN_VALUE || offset > Short.MAX_VALUE) {
                                    replacement = new BuilderInstruction30t(Opcode.GOTO_32,
                                            ((BuilderOffsetInstruction)instruction).getTarget());
                                } else {
                                    replacement = new BuilderInstruction20t(Opcode.GOTO_16,
                                            ((BuilderOffsetInstruction)instruction).getTarget());
                                }
                                replaceInstruction(location.index, replacement);
                                madeChanges = true;
                            }
                            break;
                        }
                        case GOTO_16: {
                            int offset = ((BuilderOffsetInstruction)instruction).internalGetCodeOffset();
                            if (offset < Short.MIN_VALUE || offset > Short.MAX_VALUE) {
                                BuilderOffsetInstruction replacement =  new BuilderInstruction30t(Opcode.GOTO_32,
                                            ((BuilderOffsetInstruction)instruction).getTarget());
                                replaceInstruction(location.index, replacement);
                                madeChanges = true;
                            }
                            break;
                        }
                        case SPARSE_SWITCH_PAYLOAD:
                        case PACKED_SWITCH_PAYLOAD:
                            if (((BuilderSwitchPayload)instruction).referrer == null) {
                                // if the switch payload isn't referenced, just remove it
                                removeInstruction(index);
                                index--;
                                madeChanges = true;
                                break;
                            }
                            // intentional fall-through
                        case ARRAY_PAYLOAD: {
                            if ((location.codeAddress & 0x01) != 0) {
                                int previousIndex = location.index - 1;
                                MethodLocation previousLocation = instructionList.get(previousIndex);
                                Instruction previousInstruction = previousLocation.instruction;
                                assert previousInstruction != null;
                                if (previousInstruction.getOpcode() == Opcode.NOP) {
                                    removeInstruction(previousIndex);
                                    index--;
                                } else {
                                    addInstruction(location.index, new BuilderInstruction10x(Opcode.NOP));
                                    index++;
                                }
                                madeChanges = true;
                            }
                            break;
                        }
                    }
                }
            }
        } while (madeChanges);

        fixInstructions = false;
    }

    private int mapCodeAddressToIndex(@Nonnull int[] codeAddressToIndex, int codeAddress) {
        int index;
        do {
            index = codeAddressToIndex[codeAddress];
            if (index < 0) {
                codeAddress--;
            } else {
                return index;
            }
        } while (true);
    }

    @Nonnull
    private Label newLabel(@Nonnull int[] codeAddressToIndex, int codeAddress) {
        MethodLocation referent = instructionList.get(mapCodeAddressToIndex(codeAddressToIndex, codeAddress));
        return referent.addNewLabel();
    }

    private static class SwitchPayloadReferenceLabel extends Label {
        @Nonnull public MethodLocation switchLocation;
    }

    @Nonnull
    public Label newSwitchPayloadReferenceLabel(@Nonnull MethodLocation switchLocation,
                                                @Nonnull int[] codeAddressToIndex, int codeAddress) {
        MethodLocation referent = instructionList.get(mapCodeAddressToIndex(codeAddressToIndex, codeAddress));
        SwitchPayloadReferenceLabel label = new SwitchPayloadReferenceLabel();
        label.switchLocation = switchLocation;
        referent.getLabels().add(label);
        return label;
    }

    private void setInstruction(@Nonnull MethodLocation location, @Nonnull BuilderInstruction instruction) {
        location.instruction = instruction;
        instruction.location = location;
    }

    private void convertAndSetInstruction(@Nonnull MethodLocation location, int[] codeAddressToIndex,
                                          @Nonnull Instruction instruction) {
        switch (instruction.getOpcode().format) {
            case Format10t:
                setInstruction(location, newBuilderInstruction10t(location.codeAddress,
                        codeAddressToIndex,
                        (Instruction10t) instruction));
                return;
            case Format10x:
                setInstruction(location, newBuilderInstruction10x((Instruction10x) instruction));
                return;
            case Format11n:
                setInstruction(location, newBuilderInstruction11n((Instruction11n) instruction));
                return;
            case Format11x:
                setInstruction(location, newBuilderInstruction11x((Instruction11x) instruction));
                return;
            case Format12x:
                setInstruction(location, newBuilderInstruction12x((Instruction12x) instruction));
                return;
            case Format20bc:
                setInstruction(location, newBuilderInstruction20bc((Instruction20bc) instruction));
                return;
            case Format20t:
                setInstruction(location, newBuilderInstruction20t(location.codeAddress,
                        codeAddressToIndex,
                        (Instruction20t) instruction));
                return;
            case Format21c:
                setInstruction(location, newBuilderInstruction21c((Instruction21c) instruction));
                return;
            case Format21ih:
                setInstruction(location, newBuilderInstruction21ih((Instruction21ih) instruction));
                return;
            case Format21lh:
                setInstruction(location, newBuilderInstruction21lh((Instruction21lh) instruction));
                return;
            case Format21s:
                setInstruction(location, newBuilderInstruction21s((Instruction21s) instruction));
                return;
            case Format21t:
                setInstruction(location, newBuilderInstruction21t(location.codeAddress,
                        codeAddressToIndex,
                        (Instruction21t) instruction));
                return;
            case Format22b:
                setInstruction(location, newBuilderInstruction22b((Instruction22b) instruction));
                return;
            case Format22c:
                setInstruction(location, newBuilderInstruction22c((Instruction22c) instruction));
                return;
            case Format22s:
                setInstruction(location, newBuilderInstruction22s((Instruction22s) instruction));
                return;
            case Format22t:
                setInstruction(location, newBuilderInstruction22t(location.codeAddress,
                        codeAddressToIndex,
                        (Instruction22t) instruction));
                return;
            case Format22x:
                setInstruction(location, newBuilderInstruction22x((Instruction22x) instruction));
                return;
            case Format23x:
                setInstruction(location, newBuilderInstruction23x((Instruction23x) instruction));
                return;
            case Format25x:
                setInstruction(location, newBuilderInstruction25x((Instruction25x) instruction));
                return;
            case Format30t:
                setInstruction(location, newBuilderInstruction30t(location.codeAddress,
                        codeAddressToIndex,
                        (Instruction30t) instruction));
                return;
            case Format31c:
                setInstruction(location, newBuilderInstruction31c((Instruction31c) instruction));
                return;
            case Format31i:
                setInstruction(location, newBuilderInstruction31i((Instruction31i) instruction));
                return;
            case Format31t:
                setInstruction(location, newBuilderInstruction31t(location, codeAddressToIndex,
                        (Instruction31t) instruction));
                return;
            case Format32x:
                setInstruction(location, newBuilderInstruction32x((Instruction32x) instruction));
                return;
            case Format35c:
                setInstruction(location, newBuilderInstruction35c((Instruction35c) instruction));
                return;
            case Format3rc:
                setInstruction(location, newBuilderInstruction3rc((Instruction3rc)instruction));
                return;
            case Format51l:
                setInstruction(location, newBuilderInstruction51l((Instruction51l)instruction));
                return;
            case PackedSwitchPayload:
                setInstruction(location,
                        newBuilderPackedSwitchPayload(location, codeAddressToIndex, (PackedSwitchPayload)instruction));
                return;
            case SparseSwitchPayload:
                setInstruction(location,
                        newBuilderSparseSwitchPayload(location, codeAddressToIndex, (SparseSwitchPayload)instruction));
                return;
            case ArrayPayload:
                setInstruction(location, newBuilderArrayPayload((ArrayPayload)instruction));
                return;
            default:
                throw new ExceptionWithContext("Instruction format %s not supported", instruction.getOpcode().format);
        }
    }

    @Nonnull
    private BuilderInstruction10t newBuilderInstruction10t(int codeAddress, int[] codeAddressToIndex,
                                                           @Nonnull Instruction10t instruction) {
        return new BuilderInstruction10t(
                instruction.getOpcode(),
                newLabel(codeAddressToIndex, codeAddress + instruction.getCodeOffset()));
    }

    @Nonnull
    private BuilderInstruction10x newBuilderInstruction10x(@Nonnull Instruction10x instruction) {
        return new BuilderInstruction10x(
                instruction.getOpcode());
    }

    @Nonnull
    private BuilderInstruction11n newBuilderInstruction11n(@Nonnull Instruction11n instruction) {
        return new BuilderInstruction11n(
                instruction.getOpcode(),
                instruction.getRegisterA(),
                instruction.getNarrowLiteral());
    }

    @Nonnull
    private BuilderInstruction11x newBuilderInstruction11x(@Nonnull Instruction11x instruction) {
        return new BuilderInstruction11x(
                instruction.getOpcode(),
                instruction.getRegisterA());
    }

    @Nonnull
    private BuilderInstruction12x newBuilderInstruction12x(@Nonnull Instruction12x instruction) {
        return new BuilderInstruction12x(
                instruction.getOpcode(),
                instruction.getRegisterA(),
                instruction.getRegisterB());
    }

    @Nonnull
    private BuilderInstruction20bc newBuilderInstruction20bc(@Nonnull Instruction20bc instruction) {
        return new BuilderInstruction20bc(
                instruction.getOpcode(),
                instruction.getVerificationError(),
                instruction.getReference());
    }

    @Nonnull
    private BuilderInstruction20t newBuilderInstruction20t(int codeAddress, int[] codeAddressToIndex,
                                                           @Nonnull Instruction20t instruction) {
        return new BuilderInstruction20t(
                instruction.getOpcode(),
                newLabel(codeAddressToIndex, codeAddress + instruction.getCodeOffset()));
    }

    @Nonnull
    private BuilderInstruction21c newBuilderInstruction21c(@Nonnull Instruction21c instruction) {
        return new BuilderInstruction21c(
                instruction.getOpcode(),
                instruction.getRegisterA(),
                instruction.getReference());
    }

    @Nonnull
    private BuilderInstruction21ih newBuilderInstruction21ih(@Nonnull Instruction21ih instruction) {
        return new BuilderInstruction21ih(
                instruction.getOpcode(),
                instruction.getRegisterA(),
                instruction.getNarrowLiteral());
    }

    @Nonnull
    private BuilderInstruction21lh newBuilderInstruction21lh(@Nonnull Instruction21lh instruction) {
        return new BuilderInstruction21lh(
                instruction.getOpcode(),
                instruction.getRegisterA(),
                instruction.getWideLiteral());
    }

    @Nonnull
    private BuilderInstruction21s newBuilderInstruction21s(@Nonnull Instruction21s instruction) {
        return new BuilderInstruction21s(
                instruction.getOpcode(),
                instruction.getRegisterA(),
                instruction.getNarrowLiteral());
    }

    @Nonnull
    private BuilderInstruction21t newBuilderInstruction21t(int codeAddress, int[] codeAddressToIndex,
                                                           @Nonnull Instruction21t instruction) {
        return new BuilderInstruction21t(
                instruction.getOpcode(),
                instruction.getRegisterA(),
                newLabel(codeAddressToIndex, codeAddress + instruction.getCodeOffset()));
    }

    @Nonnull
    private BuilderInstruction22b newBuilderInstruction22b(@Nonnull Instruction22b instruction) {
        return new BuilderInstruction22b(
                instruction.getOpcode(),
                instruction.getRegisterA(),
                instruction.getRegisterB(),
                instruction.getNarrowLiteral());
    }

    @Nonnull
    private BuilderInstruction22c newBuilderInstruction22c(@Nonnull Instruction22c instruction) {
        return new BuilderInstruction22c(
                instruction.getOpcode(),
                instruction.getRegisterA(),
                instruction.getRegisterB(),
                instruction.getReference());
    }

    @Nonnull
    private BuilderInstruction22s newBuilderInstruction22s(@Nonnull Instruction22s instruction) {
        return new BuilderInstruction22s(
                instruction.getOpcode(),
                instruction.getRegisterA(),
                instruction.getRegisterB(),
                instruction.getNarrowLiteral());
    }

    @Nonnull
    private BuilderInstruction22t newBuilderInstruction22t(int codeAddress, int[] codeAddressToIndex,
                                                           @Nonnull Instruction22t instruction) {
        return new BuilderInstruction22t(
                instruction.getOpcode(),
                instruction.getRegisterA(),
                instruction.getRegisterB(),
                newLabel(codeAddressToIndex, codeAddress + instruction.getCodeOffset()));
    }

    @Nonnull
    private BuilderInstruction22x newBuilderInstruction22x(@Nonnull Instruction22x instruction) {
        return new BuilderInstruction22x(
                instruction.getOpcode(),
                instruction.getRegisterA(),
                instruction.getRegisterB());
    }

    @Nonnull
    private BuilderInstruction23x newBuilderInstruction23x(@Nonnull Instruction23x instruction) {
        return new BuilderInstruction23x(
                instruction.getOpcode(),
                instruction.getRegisterA(),
                instruction.getRegisterB(),
                instruction.getRegisterC());
    }

    @Nonnull
    private BuilderInstruction30t newBuilderInstruction30t(int codeAddress, int[] codeAddressToIndex,
                                                           @Nonnull Instruction30t instruction) {
        return new BuilderInstruction30t(
                instruction.getOpcode(),
                newLabel(codeAddressToIndex, codeAddress + instruction.getCodeOffset()));
    }

    @Nonnull
    private BuilderInstruction31c newBuilderInstruction31c(@Nonnull Instruction31c instruction) {
        return new BuilderInstruction31c(
                instruction.getOpcode(),
                instruction.getRegisterA(),
                instruction.getReference());
    }

    @Nonnull
    private BuilderInstruction31i newBuilderInstruction31i(@Nonnull Instruction31i instruction) {
        return new BuilderInstruction31i(
                instruction.getOpcode(),
                instruction.getRegisterA(),
                instruction.getNarrowLiteral());
    }

    @Nonnull
    private BuilderInstruction31t newBuilderInstruction31t(@Nonnull MethodLocation location , int[] codeAddressToIndex,
                                                           @Nonnull Instruction31t instruction) {
        int codeAddress = location.getCodeAddress();
        Label newLabel;
        if (instruction.getOpcode() != Opcode.FILL_ARRAY_DATA) {
            // if it's a sparse switch or packed switch
            newLabel = newSwitchPayloadReferenceLabel(location, codeAddressToIndex, codeAddress + instruction.getCodeOffset());
        } else {
            newLabel = newLabel(codeAddressToIndex, codeAddress + instruction.getCodeOffset());
        }
        return new BuilderInstruction31t(
                instruction.getOpcode(),
                instruction.getRegisterA(),
                newLabel);
    }

    @Nonnull
    private BuilderInstruction32x newBuilderInstruction32x(@Nonnull Instruction32x instruction) {
        return new BuilderInstruction32x(
                instruction.getOpcode(),
                instruction.getRegisterA(),
                instruction.getRegisterB());
    }

    @Nonnull
    private BuilderInstruction35c newBuilderInstruction35c(@Nonnull Instruction35c instruction) {
        return new BuilderInstruction35c(
                instruction.getOpcode(),
                instruction.getRegisterCount(),
                instruction.getRegisterC(),
                instruction.getRegisterD(),
                instruction.getRegisterE(),
                instruction.getRegisterF(),
                instruction.getRegisterG(),
                instruction.getReference());
    }

    @Nonnull
    private BuilderInstruction25x newBuilderInstruction25x(@Nonnull Instruction25x instruction) {
        return new BuilderInstruction25x(
                instruction.getOpcode(),
                instruction.getParameterRegisterCount(),
                instruction.getRegisterFixedC(),
                instruction.getRegisterParameterD(),
                instruction.getRegisterParameterE(),
                instruction.getRegisterParameterF(),
                instruction.getRegisterParameterG());
    }

    @Nonnull
    private BuilderInstruction3rc newBuilderInstruction3rc(@Nonnull Instruction3rc instruction) {
        return new BuilderInstruction3rc(
                instruction.getOpcode(),
                instruction.getStartRegister(),
                instruction.getRegisterCount(),
                instruction.getReference());
    }

    @Nonnull
    private BuilderInstruction51l newBuilderInstruction51l(@Nonnull Instruction51l instruction) {
        return new BuilderInstruction51l(
                instruction.getOpcode(),
                instruction.getRegisterA(),
                instruction.getWideLiteral());
    }

    @Nullable
    private MethodLocation findSwitchForPayload(@Nonnull MethodLocation payloadLocation) {
        MethodLocation location = payloadLocation;
        MethodLocation switchLocation = null;
        do {
            for (Label label: location.getLabels()) {
                if (label instanceof SwitchPayloadReferenceLabel) {
                    if (switchLocation != null) {
                        throw new IllegalStateException("Multiple switch instructions refer to the same payload. " +
                                "This is not currently supported. Please file a bug :)");
                    }
                    switchLocation = ((SwitchPayloadReferenceLabel)label).switchLocation;
                }
            }

            // A switch instruction can refer to the payload instruction itself, or to a nop before the payload
            // instruction.
            // We need to search for all occurrences of a switch reference, so we can detect when multiple switch
            // statements refer to the same payload
            // TODO: confirm that it could refer to the first NOP in a series of NOPs preceding the payload
            if (location.index == 0) {
                return switchLocation;
            }
            location = instructionList.get(location.index - 1);
            if (location.instruction == null || location.instruction.getOpcode() != Opcode.NOP) {
                return switchLocation;
            }
        } while (true);
    }

    @Nonnull
    private BuilderPackedSwitchPayload newBuilderPackedSwitchPayload(@Nonnull MethodLocation location,
                                                                     @Nonnull int[] codeAddressToIndex,
                                                                     @Nonnull PackedSwitchPayload instruction) {
        List<? extends SwitchElement> switchElements = instruction.getSwitchElements();
        if (switchElements.size() == 0) {
            return new BuilderPackedSwitchPayload(0, null);
        }

        MethodLocation switchLocation = findSwitchForPayload(location);
        int baseAddress;
        if (switchLocation == null) {
            baseAddress = 0;
        } else {
            baseAddress = switchLocation.codeAddress;
        }

        List<Label> labels = Lists.newArrayList();
        for (SwitchElement element: switchElements) {
            labels.add(newLabel(codeAddressToIndex, element.getOffset() + baseAddress));
        }

        return new BuilderPackedSwitchPayload(switchElements.get(0).getKey(), labels);
    }

    @Nonnull
    private BuilderSparseSwitchPayload newBuilderSparseSwitchPayload(@Nonnull MethodLocation location,
                                                                     @Nonnull int[] codeAddressToIndex,
                                                                     @Nonnull SparseSwitchPayload instruction) {
        List<? extends SwitchElement> switchElements = instruction.getSwitchElements();
        if (switchElements.size() == 0) {
            return new BuilderSparseSwitchPayload(null);
        }

        MethodLocation switchLocation = findSwitchForPayload(location);
        int baseAddress;
        if (switchLocation == null) {
            baseAddress = 0;
        } else {
            baseAddress = switchLocation.codeAddress;
        }

        List<SwitchLabelElement> labelElements = Lists.newArrayList();
        for (SwitchElement element: switchElements) {
            labelElements.add(new SwitchLabelElement(element.getKey(),
                    newLabel(codeAddressToIndex, element.getOffset() + baseAddress)));
        }

        return new BuilderSparseSwitchPayload(labelElements);
    }

    @Nonnull
    private BuilderArrayPayload newBuilderArrayPayload(@Nonnull ArrayPayload instruction) {
        return new BuilderArrayPayload(instruction.getElementWidth(), instruction.getArrayElements());
    }

    @Nonnull
    private BuilderDebugItem convertDebugItem(@Nonnull DebugItem debugItem) {
        switch (debugItem.getDebugItemType()) {
            case DebugItemType.START_LOCAL: {
                StartLocal startLocal = (StartLocal)debugItem;
                return new BuilderStartLocal(startLocal.getRegister(), startLocal.getNameReference(),
                        startLocal.getTypeReference(), startLocal.getSignatureReference());
            }
            case DebugItemType.END_LOCAL: {
                EndLocal endLocal = (EndLocal)debugItem;
                return new BuilderEndLocal(endLocal.getRegister());
            }
            case DebugItemType.RESTART_LOCAL: {
                RestartLocal restartLocal = (RestartLocal)debugItem;
                return new BuilderRestartLocal(restartLocal.getRegister());
            }
            case DebugItemType.PROLOGUE_END:
                return new BuilderPrologueEnd();
            case DebugItemType.EPILOGUE_BEGIN:
                return new BuilderEpilogueBegin();
            case DebugItemType.LINE_NUMBER: {
                LineNumber lineNumber = (LineNumber)debugItem;
                return new BuilderLineNumber(lineNumber.getLineNumber());
            }
            case DebugItemType.SET_SOURCE_FILE: {
                SetSourceFile setSourceFile = (SetSourceFile)debugItem;
                return new BuilderSetSourceFile(setSourceFile.getSourceFileReference());
            }
            default:
                throw new ExceptionWithContext("Invalid debug item type: " + debugItem.getDebugItemType());
        }
    }
}
