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

package org.jf.dexlib2.dexbacked.raw;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import org.jf.dexlib2.ReferenceType;
import org.jf.dexlib2.VerificationError;
import org.jf.dexlib2.dexbacked.CDexBackedDexFile;
import org.jf.dexlib2.dexbacked.DexReader;
import org.jf.dexlib2.dexbacked.instruction.DexBackedInstruction;
import org.jf.dexlib2.dexbacked.raw.util.DexAnnotator;
import org.jf.dexlib2.formatter.DexFormatter;
import org.jf.dexlib2.iface.instruction.*;
import org.jf.dexlib2.iface.instruction.formats.*;
import org.jf.dexlib2.iface.reference.Reference;
import org.jf.dexlib2.iface.reference.StringReference;
import org.jf.dexlib2.util.AnnotatedBytes;
import org.jf.util.ExceptionWithContext;
import org.jf.util.NumberUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class CodeItem {
    public static final int REGISTERS_OFFSET = 0;
    public static final int INS_OFFSET = 2;
    public static final int OUTS_OFFSET = 4;
    public static final int TRIES_SIZE_OFFSET = 6;
    public static final int DEBUG_INFO_OFFSET = 8;
    public static final int INSTRUCTION_COUNT_OFFSET = 12;
    public static final int INSTRUCTION_START_OFFSET = 16;

    public static int CDEX_TRIES_SIZE_SHIFT = 0;
    public static int CDEX_OUTS_COUNT_SHIFT = 4;
    public static int CDEX_INS_COUNT_SHIFT = 8;
    public static int CDEX_REGISTER_COUNT_SHIFT = 12;

    public static int CDEX_INSTRUCTIONS_SIZE_AND_PREHEADER_FLAGS_OFFSET = 2;
    public static int CDEX_INSTRUCTIONS_SIZE_SHIFT = 5;
    public static int CDEX_PREHEADER_FLAGS_MASK = 0x1f;
    public static int CDEX_PREHEADER_FLAG_REGISTER_COUNT = 1 << 0;
    public static int CDEX_PREHEADER_FLAG_INS_COUNT = 1 << 1;
    public static int CDEX_PREHEADER_FLAG_OUTS_COUNT = 1 << 2;
    public static int CDEX_PREHEADER_FLAG_TRIES_COUNT = 1 << 3;
    public static int CDEX_PREHEADER_FLAG_INSTRUCTIONS_SIZE = 1 << 4;

    public static class TryItem {
        public static final int ITEM_SIZE = 8;

        public static final int START_ADDRESS_OFFSET = 0;
        public static final int CODE_UNIT_COUNT_OFFSET = 4;
        public static final int HANDLER_OFFSET = 6;
    }

    public static SectionAnnotator makeAnnotator(@Nonnull DexAnnotator annotator, @Nonnull MapItem mapItem) {
        if (annotator.dexFile instanceof CDexBackedDexFile) {
            return makeAnnotatorForCDex(annotator, mapItem);
        } else {
            return makeAnnotatorForDex(annotator, mapItem);
        }
    }

    @Nonnull
    private static SectionAnnotator makeAnnotatorForDex(@Nonnull DexAnnotator annotator, @Nonnull MapItem mapItem) {
        return new CodeItemAnnotator(annotator, mapItem);
    }

    @Nonnull
    private static SectionAnnotator makeAnnotatorForCDex(@Nonnull DexAnnotator annotator, @Nonnull MapItem mapItem) {
        return new CodeItemAnnotator(annotator, mapItem) {

            private List<Integer> sortedItems;

            @Override public void annotateSection(@Nonnull AnnotatedBytes out) {
                sortedItems = new ArrayList<>(itemIdentities.keySet());
                sortedItems.sort(Integer::compareTo);

                //debugInfoAnnotator = annotator.getAnnotator(ItemType.DEBUG_INFO_ITEM);
                out.moveTo(sectionOffset);
                annotateSectionInner(out, itemIdentities.size());
            }

            @Override
            protected int getItemOffset(int itemIndex, int currentOffset) {
                return sortedItems.get(itemIndex);
            }

            @Override
            protected PreInstructionInfo annotatePreInstructionFields(
                    @Nonnull AnnotatedBytes out, @Nonnull DexReader reader, @Nullable String itemIdentity) {
                int sizeFields = reader.readUshort();

                int triesCount = (sizeFields >> CDEX_TRIES_SIZE_SHIFT) & 0xf;
                int outsCount = (sizeFields >> CDEX_OUTS_COUNT_SHIFT) & 0xf;
                int insCount = (sizeFields >> CDEX_INS_COUNT_SHIFT) & 0xf;
                int registerCount = (sizeFields >> CDEX_REGISTER_COUNT_SHIFT) & 0xf;

                int startOffset = out.getCursor();

                out.annotate(2, "tries_size = %d", triesCount);
                out.annotate(0, "outs_size = %d", outsCount);
                out.annotate(0, "ins_size = %d", insCount);
                out.annotate(0, "registers_size = %d", registerCount);

                int instructionsSizeAndPreheaderFlags = reader.readUshort();

                int instructionsSize = instructionsSizeAndPreheaderFlags >> CDEX_INSTRUCTIONS_SIZE_SHIFT;

                out.annotate(2, "insns_size = %d", instructionsSize);

                int instructionsStartOffset = out.getCursor();
                int preheaderOffset = startOffset;

                int totalTriesCount = triesCount;
                int totalInstructionsSize = instructionsSize;

                if ((instructionsSizeAndPreheaderFlags & CDEX_PREHEADER_FLAGS_MASK) != 0) {
                    int preheaderCount = Integer.bitCount(
                            instructionsSizeAndPreheaderFlags & CDEX_PREHEADER_FLAGS_MASK);
                    if ((instructionsSizeAndPreheaderFlags & CDEX_PREHEADER_FLAG_INSTRUCTIONS_SIZE) != 0) {
                        // The instructions size preheader is 2 shorts
                        preheaderCount++;
                    }

                    out.moveTo((startOffset - 2 * preheaderCount));
                    out.deindent();
                    out.annotate(0, "[preheader for next code_item]");
                    out.indent();
                    out.moveTo(instructionsStartOffset);
                }

                if ((instructionsSizeAndPreheaderFlags & CDEX_PREHEADER_FLAG_INSTRUCTIONS_SIZE) != 0) {
                    out.annotate(0, "insns_size_preheader_flag=1");
                    preheaderOffset -= 2;
                    reader.setOffset(preheaderOffset);
                    int extraInstructionsSize = reader.readUshort();
                    preheaderOffset -= 2;
                    reader.setOffset(preheaderOffset);
                    extraInstructionsSize += reader.readUshort();

                    out.moveTo(preheaderOffset);
                    totalInstructionsSize += extraInstructionsSize;
                    out.annotate(2, "insns_size = %d + %d = %d",
                            instructionsSize, extraInstructionsSize, instructionsSize + extraInstructionsSize);
                    out.moveTo(instructionsStartOffset);
                }

                if ((instructionsSizeAndPreheaderFlags & CDEX_PREHEADER_FLAG_REGISTER_COUNT) != 0) {
                    out.annotate(0, "registers_size_preheader_flag=1");
                    preheaderOffset -= 2;
                    out.moveTo(preheaderOffset);
                    reader.setOffset(preheaderOffset);
                    int extraRegisterCount = reader.readUshort();
                    out.annotate(2, "registers_size = %d + %d = %d",
                            registerCount, extraRegisterCount, registerCount + extraRegisterCount);
                    out.moveTo(instructionsStartOffset);
                }
                if ((instructionsSizeAndPreheaderFlags & CDEX_PREHEADER_FLAG_INS_COUNT) != 0) {
                    out.annotate(0, "ins_size_preheader_flag=1");
                    preheaderOffset -= 2;
                    out.moveTo(preheaderOffset);
                    reader.setOffset(preheaderOffset);
                    int extraInsCount = reader.readUshort();
                    out.annotate(2, "ins_size = %d + %d = %d",
                            insCount, extraInsCount, insCount + extraInsCount);
                    out.moveTo(instructionsStartOffset);
                }
                if ((instructionsSizeAndPreheaderFlags & CDEX_PREHEADER_FLAG_OUTS_COUNT) != 0) {
                    out.annotate(0, "outs_size_preheader_flag=1");
                    preheaderOffset -= 2;
                    out.moveTo(preheaderOffset);
                    reader.setOffset(preheaderOffset);
                    int extraOutsCount = reader.readUshort();
                    out.annotate(2, "outs_size = %d + %d = %d",
                            outsCount, extraOutsCount, outsCount + extraOutsCount);
                    out.moveTo(instructionsStartOffset);
                }
                if ((instructionsSizeAndPreheaderFlags & CDEX_PREHEADER_FLAG_TRIES_COUNT) != 0) {
                    out.annotate(0, "tries_size_preheader_flag=1");
                    preheaderOffset -= 2;
                    out.moveTo(preheaderOffset);
                    reader.setOffset(preheaderOffset);
                    int extraTriesCount = reader.readUshort();
                    totalTriesCount += extraTriesCount;
                    out.annotate(2, "tries_size = %d + %d = %d",
                            triesCount, extraTriesCount, triesCount + extraTriesCount);
                    out.moveTo(instructionsStartOffset);
                }

                reader.setOffset(instructionsStartOffset);

                return new PreInstructionInfo(totalTriesCount, totalInstructionsSize);
            }
        };
    }

    private static class CodeItemAnnotator extends SectionAnnotator {
        private SectionAnnotator debugInfoAnnotator;

        public CodeItemAnnotator(@Nonnull DexAnnotator annotator, @Nonnull MapItem mapItem) {
            super(annotator, mapItem);
        }

        @Nonnull @Override public String getItemName() {
            return "code_item";
        }

        @Override public int getItemAlignment() {
            return 4;
        }

        protected class PreInstructionInfo {
            public int triesCount;
            public int instructionSize;

            public PreInstructionInfo(int triesCount, int instructionSize) {
                this.triesCount = triesCount;
                this.instructionSize = instructionSize;
            }
        }

        protected PreInstructionInfo annotatePreInstructionFields(
                @Nonnull AnnotatedBytes out, @Nonnull DexReader reader, @Nullable String itemIdentity) {

            int registers = reader.readUshort();
            out.annotate(2, "registers_size = %d", registers);

            int inSize = reader.readUshort();
            out.annotate(2, "ins_size = %d", inSize);

            int outSize = reader.readUshort();
            out.annotate(2, "outs_size = %d", outSize);

            int triesCount = reader.readUshort();
            out.annotate(2, "tries_size = %d", triesCount);

            int debugInfoOffset = reader.readInt();
            out.annotate(4, "debug_info_off = 0x%x", debugInfoOffset);

            if (debugInfoOffset > 0) {
                addDebugInfoIdentity(debugInfoOffset, itemIdentity);
            }

            int instructionSize = reader.readSmallUint();
            out.annotate(4, "insns_size = 0x%x", instructionSize);

            return new PreInstructionInfo(triesCount, instructionSize);
        }

        protected void annotateInstructions(
                @Nonnull AnnotatedBytes out,
                @Nonnull DexReader reader,
                int instructionSize) {

            out.annotate(0, "instructions:");
            out.indent();

            out.setLimit(out.getCursor(), out.getCursor() + instructionSize * 2);

            int end = reader.getOffset() + instructionSize*2;
            try {
                while (reader.getOffset() < end) {
                    Instruction instruction = DexBackedInstruction.readFrom(dexFile, reader);

                    // if we read past the end of the instruction list
                    if (reader.getOffset() > end) {
                        out.annotateTo(end, "truncated instruction");
                        reader.setOffset(end);
                    } else {
                        switch (instruction.getOpcode().format) {
                            case Format10x:
                                annotateInstruction10x(out, instruction);
                                break;
                            case Format35c:
                                annotateInstruction35c(out, (Instruction35c)instruction);
                                break;
                            case Format3rc:
                                annotateInstruction3rc(out, (Instruction3rc)instruction);
                                break;
                            case ArrayPayload:
                                annotateArrayPayload(out, (ArrayPayload)instruction);
                                break;
                            case PackedSwitchPayload:
                                annotatePackedSwitchPayload(out, (PackedSwitchPayload)instruction);
                                break;
                            case SparseSwitchPayload:
                                annotateSparseSwitchPayload(out, (SparseSwitchPayload)instruction);
                                break;
                            default:
                                annotateDefaultInstruction(out, instruction);
                                break;
                        }
                    }

                    assert reader.getOffset() == out.getCursor();
                }
            } catch (ExceptionWithContext ex) {
                ex.printStackTrace(System.err);
                out.annotate(0, "annotation error: %s", ex.getMessage());
                out.moveTo(end);
                reader.setOffset(end);
            } finally {
                out.clearLimit();
                out.deindent();
            }
        }

        protected void annotatePostInstructionFields(@Nonnull AnnotatedBytes out,
                                                     @Nonnull DexReader reader,
                                                     int triesCount) {
            if (triesCount > 0) {
                if ((reader.getOffset() % 4) != 0) {
                    reader.readUshort();
                    out.annotate(2, "padding");
                }

                out.annotate(0, "try_items:");
                out.indent();
                try {
                    for (int i = 0; i < triesCount; i++) {
                        out.annotate(0, "try_item[%d]:", i);
                        out.indent();
                        try {
                            int startAddr = reader.readSmallUint();
                            out.annotate(4, "start_addr = 0x%x", startAddr);

                            int instructionCount = reader.readUshort();
                            out.annotate(2, "insn_count = 0x%x", instructionCount);

                            int handlerOffset = reader.readUshort();
                            out.annotate(2, "handler_off = 0x%x", handlerOffset);
                        } finally {
                            out.deindent();
                        }
                    }
                } finally {
                    out.deindent();
                }

                int handlerListCount = reader.readSmallUleb128();
                out.annotate(0, "encoded_catch_handler_list:");
                out.annotateTo(reader.getOffset(), "size = %d", handlerListCount);
                out.indent();
                try {
                    for (int i = 0; i < handlerListCount; i++) {
                        out.annotate(0, "encoded_catch_handler[%d]", i);
                        out.indent();
                        try {
                            int handlerCount = reader.readSleb128();
                            out.annotateTo(reader.getOffset(), "size = %d", handlerCount);
                            boolean hasCatchAll = handlerCount <= 0;
                            handlerCount = Math.abs(handlerCount);
                            if (handlerCount != 0) {
                                out.annotate(0, "handlers:");
                                out.indent();
                                try {
                                    for (int j = 0; j < handlerCount; j++) {
                                        out.annotate(0, "encoded_type_addr_pair[%d]", i);
                                        out.indent();
                                        try {
                                            int typeIndex = reader.readSmallUleb128();
                                            out.annotateTo(reader.getOffset(), TypeIdItem.getReferenceAnnotation(dexFile, typeIndex));

                                            int handlerAddress = reader.readSmallUleb128();
                                            out.annotateTo(reader.getOffset(), "addr = 0x%x", handlerAddress);
                                        } finally {
                                            out.deindent();
                                        }
                                    }
                                } finally {
                                    out.deindent();
                                }
                            }
                            if (hasCatchAll) {
                                int catchAllAddress = reader.readSmallUleb128();
                                out.annotateTo(reader.getOffset(), "catch_all_addr = 0x%x", catchAllAddress);
                            }
                        } finally {
                            out.deindent();
                        }
                    }
                } finally {
                    out.deindent();
                }
            }
        }

        @Override
        public void annotateItem(@Nonnull AnnotatedBytes out, int itemIndex, @Nullable String itemIdentity) {
            try {
                DexReader reader = dexFile.getBuffer().readerAt(out.getCursor());

                PreInstructionInfo info = annotatePreInstructionFields(out, reader, itemIdentity);
                annotateInstructions(out, reader, info.instructionSize);
                annotatePostInstructionFields(out, reader, info.triesCount);
            } catch (ExceptionWithContext ex) {
                out.annotate(0, "annotation error: %s", ex.getMessage());
            }
        }

        private String formatRegister(int registerNum) {
            return String.format("v%d", registerNum);
        }

        private void annotateInstruction10x(@Nonnull AnnotatedBytes out, @Nonnull Instruction instruction) {
            out.annotate(2, instruction.getOpcode().name);
        }

        private void annotateInstruction35c(@Nonnull AnnotatedBytes out, @Nonnull Instruction35c instruction) {
            List<String> args = Lists.newArrayList();

            int registerCount = instruction.getRegisterCount();
            if (registerCount == 1) {
                args.add(formatRegister(instruction.getRegisterC()));
            } else if (registerCount == 2) {
                args.add(formatRegister(instruction.getRegisterC()));
                args.add(formatRegister(instruction.getRegisterD()));
            } else if (registerCount == 3) {
                args.add(formatRegister(instruction.getRegisterC()));
                args.add(formatRegister(instruction.getRegisterD()));
                args.add(formatRegister(instruction.getRegisterE()));
            } else if (registerCount == 4) {
                args.add(formatRegister(instruction.getRegisterC()));
                args.add(formatRegister(instruction.getRegisterD()));
                args.add(formatRegister(instruction.getRegisterE()));
                args.add(formatRegister(instruction.getRegisterF()));
            } else if (registerCount == 5) {
                args.add(formatRegister(instruction.getRegisterC()));
                args.add(formatRegister(instruction.getRegisterD()));
                args.add(formatRegister(instruction.getRegisterE()));
                args.add(formatRegister(instruction.getRegisterF()));
                args.add(formatRegister(instruction.getRegisterG()));
            }

            out.annotate(6, String.format("%s {%s}, %s",
                    instruction.getOpcode().name, Joiner.on(", ").join(args), instruction.getReference()));
        }

        private void annotateInstruction3rc(@Nonnull AnnotatedBytes out, @Nonnull Instruction3rc instruction) {
            int startRegister = instruction.getStartRegister();
            int endRegister = startRegister + instruction.getRegisterCount() - 1;
            out.annotate(6, String.format("%s {%s .. %s}, %s",
                    instruction.getOpcode().name, formatRegister(startRegister), formatRegister(endRegister),
                    instruction.getReference()));
        }

        private void annotateDefaultInstruction(@Nonnull AnnotatedBytes out, @Nonnull Instruction instruction) {
            List<String> args = Lists.newArrayList();

            if (instruction instanceof OneRegisterInstruction) {
                args.add(formatRegister(((OneRegisterInstruction)instruction).getRegisterA()));
                if (instruction instanceof TwoRegisterInstruction) {
                    args.add(formatRegister(((TwoRegisterInstruction)instruction).getRegisterB()));
                    if (instruction instanceof ThreeRegisterInstruction) {
                        args.add(formatRegister(((ThreeRegisterInstruction)instruction).getRegisterC()));
                    }
                }
            }  else if (instruction instanceof VerificationErrorInstruction) {
                String verificationError = VerificationError.getVerificationErrorName(
                        ((VerificationErrorInstruction) instruction).getVerificationError());
                if (verificationError != null) {
                    args.add(verificationError);
                } else {
                    args.add("invalid verification error type");
                }
            }

            if (instruction instanceof ReferenceInstruction) {
                ReferenceInstruction referenceInstruction = ((ReferenceInstruction)instruction);
                Reference reference = ((ReferenceInstruction)instruction).getReference();

                String referenceString;
                if (referenceInstruction.getReferenceType() == ReferenceType.STRING) {
                    referenceString = DexFormatter.INSTANCE.getQuotedString((StringReference)reference);
                } else {
                    referenceString = referenceInstruction.getReference().toString();
                }

                args.add(referenceString);
            } else if (instruction instanceof OffsetInstruction) {
                int offset = ((OffsetInstruction)instruction).getCodeOffset();
                String sign = offset>=0?"+":"-";
                args.add(String.format("%s0x%x", sign, Math.abs(offset)));
            } else if (instruction instanceof NarrowLiteralInstruction) {
                int value = ((NarrowLiteralInstruction)instruction).getNarrowLiteral();
                if (NumberUtils.isLikelyFloat(value)) {
                    args.add(String.format("%d # %f", value, Float.intBitsToFloat(value)));
                } else {
                    args.add(String.format("%d", value));
                }
            } else if (instruction instanceof WideLiteralInstruction) {
                long value = ((WideLiteralInstruction)instruction).getWideLiteral();
                if (NumberUtils.isLikelyDouble(value)) {
                    args.add(String.format("%d # %f", value, Double.longBitsToDouble(value)));
                } else {
                    args.add(String.format("%d", value));
                }
            } else if (instruction instanceof FieldOffsetInstruction) {
                int fieldOffset = ((FieldOffsetInstruction)instruction).getFieldOffset();
                args.add(String.format("field@0x%x", fieldOffset));
            } else if (instruction instanceof VtableIndexInstruction) {
                int vtableIndex = ((VtableIndexInstruction)instruction).getVtableIndex();
                args.add(String.format("vtable@%d", vtableIndex));
            } else if (instruction instanceof InlineIndexInstruction) {
                int inlineIndex = ((InlineIndexInstruction)instruction).getInlineIndex();
                args.add(String.format("inline@%d", inlineIndex));
            }

            out.annotate(instruction.getCodeUnits()*2, "%s %s",
                    instruction.getOpcode().name, Joiner.on(", ").join(args));
        }

        private void annotateArrayPayload(@Nonnull AnnotatedBytes out, @Nonnull ArrayPayload instruction) {
            List<Number> elements = instruction.getArrayElements();
            int elementWidth = instruction.getElementWidth();

            out.annotate(2, instruction.getOpcode().name);
            out.indent();
            out.annotate(2, "element_width = %d", elementWidth);
            out.annotate(4, "size = %d", elements.size());
            if (elements.size() > 0) {
                out.annotate(0, "elements:");
            }
            out.indent();
            if (elements.size() > 0) {
                for (int i = 0; i < elements.size(); i++) {
                    if (elementWidth == 8) {
                        long value = elements.get(i).longValue();
                        if (NumberUtils.isLikelyDouble(value)) {
                            out.annotate(elementWidth, "element[%d] = %d # %f", i, value, Double.longBitsToDouble(value));
                        } else {
                            out.annotate(elementWidth, "element[%d] = %d", i, value);
                        }
                    } else {
                        int value = elements.get(i).intValue();
                        if (NumberUtils.isLikelyFloat(value)) {
                            out.annotate(elementWidth, "element[%d] = %d # %f", i, value, Float.intBitsToFloat(value));
                        } else {
                            out.annotate(elementWidth, "element[%d] = %d", i, value);
                        }
                    }
                }
            }
            if (out.getCursor() % 2 != 0) {
                out.annotate(1, "padding");
            }
            out.deindent();
            out.deindent();
        }

        private void annotatePackedSwitchPayload(@Nonnull AnnotatedBytes out,
                                                 @Nonnull PackedSwitchPayload instruction) {
            List<? extends SwitchElement> elements = instruction.getSwitchElements();

            out.annotate(2, instruction.getOpcode().name);
            out.indent();

            out.annotate(2, "size = %d", elements.size());
            if (elements.size() == 0) {
                out.annotate(4, "first_key");
            } else {
                out.annotate(4, "first_key = %d", elements.get(0).getKey());
                out.annotate(0, "targets:");
                out.indent();
                for (int i=0; i<elements.size(); i++) {
                    out.annotate(4, "target[%d] = %d", i, elements.get(i).getOffset());
                }
                out.deindent();
            }
            out.deindent();
        }

        private void annotateSparseSwitchPayload(@Nonnull AnnotatedBytes out,
                                                 @Nonnull SparseSwitchPayload instruction) {
            List<? extends SwitchElement> elements = instruction.getSwitchElements();

            out.annotate(2, instruction.getOpcode().name);
            out.indent();
            out.annotate(2, "size = %d", elements.size());
            if (elements.size() > 0) {
                out.annotate(0, "keys:");
                out.indent();
                for (int i=0; i<elements.size(); i++) {
                    out.annotate(4, "key[%d] = %d", i, elements.get(i).getKey());
                }
                out.deindent();
                out.annotate(0, "targets:");
                out.indent();
                for (int i=0; i<elements.size(); i++) {
                    out.annotate(4, "target[%d] = %d", i, elements.get(i).getOffset());
                }
                out.deindent();
            }
            out.deindent();
        }

        private void addDebugInfoIdentity(int debugInfoOffset, String methodString) {
            if (debugInfoAnnotator != null) {
                debugInfoAnnotator.setItemIdentity(debugInfoOffset, methodString);
            }
        }
    }
}
