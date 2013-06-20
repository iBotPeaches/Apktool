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
import org.jf.dexlib2.VerificationError;
import org.jf.dexlib2.dexbacked.DexReader;
import org.jf.dexlib2.dexbacked.instruction.DexBackedInstruction;
import org.jf.dexlib2.dexbacked.raw.util.DexAnnotator;
import org.jf.dexlib2.iface.instruction.*;
import org.jf.dexlib2.iface.instruction.formats.*;
import org.jf.dexlib2.util.AnnotatedBytes;
import org.jf.dexlib2.util.ReferenceUtil;
import org.jf.util.NumberUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class CodeItem {
    public static final int REGISTERS_OFFSET = 0;
    public static final int INS_OFFSET = 2;
    public static final int OUTS_OFFSET = 4;
    public static final int TRIES_SIZE_OFFSET = 6;
    public static final int DEBUG_INFO_OFFSET = 8;
    public static final int INSTRUCTION_COUNT_OFFSET = 12;
    public static final int INSTRUCTION_START_OFFSET = 16;

    public static class TryItem {
        public static final int ITEM_SIZE = 8;

        public static final int START_ADDRESS_OFFSET = 0;
        public static final int CODE_UNIT_COUNT_OFFSET = 4;
        public static final int HANDLER_OFFSET = 6;
    }

    @Nonnull
    public static SectionAnnotator makeAnnotator(@Nonnull DexAnnotator annotator, @Nonnull MapItem mapItem) {
        return new SectionAnnotator(annotator, mapItem) {
            private SectionAnnotator debugInfoAnnotator = null;

            @Override public void annotateSection(@Nonnull AnnotatedBytes out) {
                debugInfoAnnotator = annotator.getAnnotator(ItemType.DEBUG_INFO_ITEM);
                super.annotateSection(out);
            }

            @Nonnull @Override public String getItemName() {
                return "code_item";
            }

            @Override public int getItemAlignment() {
                return 4;
            }

            @Override
            public void annotateItem(@Nonnull AnnotatedBytes out, int itemIndex, @Nullable String itemIdentity) {
                DexReader reader = dexFile.readerAt(out.getCursor());

                int registers = reader.readUshort();
                out.annotate(2, "registers_size = %d", registers);

                int inSize = reader.readUshort();
                out.annotate(2, "ins_size = %d", inSize);

                int outSize = reader.readUshort();
                out.annotate(2, "outs_size = %d", outSize);

                int triesCount = reader.readUshort();
                out.annotate(2, "tries_size = %d", triesCount);

                int debugInfoOffset = reader.readSmallUint();
                out.annotate(4, "debug_info_off = 0x%x", debugInfoOffset);

                if (debugInfoOffset != 0) {
                    addDebugInfoIdentity(debugInfoOffset, itemIdentity);
                }

                int instructionSize = reader.readSmallUint();
                out.annotate(4, "insns_size = 0x%x", instructionSize);

                out.annotate(0, "instructions:");
                out.indent();

                int end = reader.getOffset() + instructionSize*2;
                while (reader.getOffset() < end) {
                    Instruction instruction = DexBackedInstruction.readFrom(reader);

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

                    assert reader.getOffset() == out.getCursor();
                }
                out.deindent();

                if (triesCount > 0) {
                    if ((reader.getOffset() % 4) != 0) {
                        reader.readUshort();
                        out.annotate(2, "padding");
                    }

                    out.annotate(0, "try_items:");
                    out.indent();
                    for (int i=0; i<triesCount; i++) {
                        out.annotate(0, "try_item[%d]:", i);
                        out.indent();
                        int startAddr = reader.readSmallUint();
                        out.annotate(4, "start_addr = 0x%x", startAddr);

                        int instructionCount = reader.readUshort();
                        out.annotate(2, "insn_count = 0x%x", instructionCount);

                        int handlerOffset = reader.readUshort();
                        out.annotate(2, "handler_off = 0x%x", handlerOffset);
                        out.deindent();
                    }
                    out.deindent();

                    int handlerListCount = reader.readSmallUleb128();
                    out.annotate(0, "encoded_catch_handler_list:");
                    out.annotateTo(reader.getOffset(), "size = %d", handlerListCount);
                    out.indent();
                    for (int i=0; i<handlerListCount; i++) {
                        out.annotate(0, "encoded_catch_handler[%d]", i);
                        out.indent();
                        int handlerCount = reader.readSleb128();
                        out.annotateTo(reader.getOffset(), "size = %d", handlerCount);
                        boolean hasCatchAll = handlerCount <= 0;
                        handlerCount = Math.abs(handlerCount);
                        if (handlerCount != 0) {
                            out.annotate(0, "handlers:");
                            out.indent();
                            for (int j=0; j<handlerCount; j++) {
                                out.annotate(0, "encoded_type_addr_pair[%d]", i);
                                out.indent();
                                int typeIndex = reader.readSmallUleb128();
                                out.annotateTo(reader.getOffset(), TypeIdItem.getReferenceAnnotation(dexFile, typeIndex));

                                int handlerAddress = reader.readSmallUleb128();
                                out.annotateTo(reader.getOffset(), "addr = 0x%x", handlerAddress);
                                out.deindent();
                            }
                            out.deindent();
                        }
                        if (hasCatchAll) {
                            int catchAllAddress = reader.readSmallUleb128();
                            out.annotateTo(reader.getOffset(), "catch_all_addr = 0x%x", catchAllAddress);
                        }
                        out.deindent();
                    }
                    out.deindent();
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

                String reference = ReferenceUtil.getReferenceString(instruction.getReference());

                out.annotate(6, String.format("%s {%s}, %s",
                        instruction.getOpcode().name, Joiner.on(", ").join(args), reference));
            }

            private void annotateInstruction3rc(@Nonnull AnnotatedBytes out, @Nonnull Instruction3rc instruction) {
                int startRegister = instruction.getStartRegister();
                int endRegister = startRegister + instruction.getRegisterCount() - 1;
                String reference = ReferenceUtil.getReferenceString(instruction.getReference());
                out.annotate(6, String.format("%s {%s .. %s}, %s",
                        instruction.getOpcode().name, formatRegister(startRegister), formatRegister(endRegister),
                        reference));
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
                    args.add(VerificationError.getVerificationErrorName(
                            ((VerificationErrorInstruction)instruction).getVerificationError()));
                }

                if (instruction instanceof ReferenceInstruction) {
                    args.add(ReferenceUtil.getReferenceString(
                            ((ReferenceInstruction)instruction).getReference()));
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
                out.annotate(0, "elements:");
                out.indent();
                for (int i=0; i<elements.size(); i++) {
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
        };
    }
}
