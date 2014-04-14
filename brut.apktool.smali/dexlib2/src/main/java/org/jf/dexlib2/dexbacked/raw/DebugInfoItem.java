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

import org.jf.dexlib2.DebugItemType;
import org.jf.dexlib2.dexbacked.DexReader;
import org.jf.dexlib2.dexbacked.raw.util.DexAnnotator;
import org.jf.dexlib2.util.AnnotatedBytes;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class DebugInfoItem {

    @Nonnull
    public static SectionAnnotator makeAnnotator(@Nonnull DexAnnotator annotator, @Nonnull MapItem mapItem) {
        return new SectionAnnotator(annotator, mapItem) {
            @Nonnull @Override public String getItemName() {
                return "debug_info_item";
            }

            @Override
            public void annotateItem(@Nonnull AnnotatedBytes out, int itemIndex, @Nullable String itemIdentity) {
                DexReader reader = dexFile.readerAt(out.getCursor());

                int lineStart = reader.readSmallUleb128();
                out.annotateTo(reader.getOffset(), "line_start = %d", lineStart);

                int parametersSize = reader.readSmallUleb128();
                out.annotateTo(reader.getOffset(), "parameters_size = %d", parametersSize);

                if (parametersSize > 0) {
                    out.annotate(0, "parameters:");
                    out.indent();
                    for (int i=0; i<parametersSize; i++) {
                        int paramaterIndex = reader.readSmallUleb128() - 1;
                        out.annotateTo(reader.getOffset(), "%s",
                                StringIdItem.getOptionalReferenceAnnotation(dexFile, paramaterIndex, true));
                    }
                    out.deindent();
                }

                out.annotate(0, "debug opcodes:");
                out.indent();

                int codeAddress = 0;
                int lineNumber = lineStart;

                loop: while (true) {
                    int opcode = reader.readUbyte();
                    switch (opcode) {
                        case DebugItemType.END_SEQUENCE: {
                            out.annotateTo(reader.getOffset(), "DBG_END_SEQUENCE");
                            break loop;
                        }
                        case DebugItemType.ADVANCE_PC: {
                            out.annotateTo(reader.getOffset(), "DBG_ADVANCE_PC");
                            out.indent();
                            int addressDiff = reader.readSmallUleb128();
                            codeAddress += addressDiff;
                            out.annotateTo(reader.getOffset(), "addr_diff = +0x%x: 0x%x", addressDiff,
                                    codeAddress);
                            out.deindent();
                            break;
                        }
                        case DebugItemType.ADVANCE_LINE: {
                            out.annotateTo(reader.getOffset(), "DBG_ADVANCE_LINE");
                            out.indent();
                            int lineDiff = reader.readSleb128();
                            lineNumber += lineDiff;
                            out.annotateTo(reader.getOffset(), "line_diff = +%d: %d", Math.abs(lineDiff), lineNumber);
                            out.deindent();
                            break;
                        }
                        case DebugItemType.START_LOCAL: {
                            out.annotateTo(reader.getOffset(), "DBG_START_LOCAL");
                            out.indent();
                            int registerNum = reader.readSmallUleb128();
                            out.annotateTo(reader.getOffset(), "register_num = v%d", registerNum);
                            int nameIndex = reader.readSmallUleb128() - 1;
                            out.annotateTo(reader.getOffset(), "name_idx = %s",
                                    StringIdItem.getOptionalReferenceAnnotation(dexFile, nameIndex, true));
                            int typeIndex = reader.readSmallUleb128() - 1;
                            out.annotateTo(reader.getOffset(), "type_idx = %s",
                                    TypeIdItem.getOptionalReferenceAnnotation(dexFile, typeIndex));
                            out.deindent();
                            break;
                        }
                        case DebugItemType.START_LOCAL_EXTENDED: {
                            out.annotateTo(reader.getOffset(), "DBG_START_LOCAL_EXTENDED");
                            out.indent();
                            int registerNum = reader.readSmallUleb128();
                            out.annotateTo(reader.getOffset(), "register_num = v%d", registerNum);
                            int nameIndex = reader.readSmallUleb128() - 1;
                            out.annotateTo(reader.getOffset(), "name_idx = %s",
                                    StringIdItem.getOptionalReferenceAnnotation(dexFile, nameIndex, true));
                            int typeIndex = reader.readSmallUleb128() - 1;
                            out.annotateTo(reader.getOffset(), "type_idx = %s",
                                    TypeIdItem.getOptionalReferenceAnnotation(dexFile, typeIndex));
                            int sigIndex = reader.readSmallUleb128() - 1;
                            out.annotateTo(reader.getOffset(), "sig_idx = %s",
                                    StringIdItem.getOptionalReferenceAnnotation(dexFile, sigIndex, true));
                            out.deindent();
                            break;
                        }
                        case DebugItemType.END_LOCAL: {
                            out.annotateTo(reader.getOffset(), "DBG_END_LOCAL");
                            out.indent();
                            int registerNum = reader.readSmallUleb128();
                            out.annotateTo(reader.getOffset(), "register_num = v%d", registerNum);
                            out.deindent();
                            break;
                        }
                        case DebugItemType.RESTART_LOCAL: {
                            out.annotateTo(reader.getOffset(), "DBG_RESTART_LOCAL");
                            out.indent();
                            int registerNum = reader.readSmallUleb128();
                            out.annotateTo(reader.getOffset(), "register_num = v%d", registerNum);
                            out.deindent();
                            break;
                        }
                        case DebugItemType.PROLOGUE_END: {
                            out.annotateTo(reader.getOffset(), "DBG_SET_PROLOGUE_END");
                            break;
                        }
                        case DebugItemType.EPILOGUE_BEGIN: {
                            out.annotateTo(reader.getOffset(), "DBG_SET_EPILOGUE_BEGIN");
                            break;
                        }
                        case DebugItemType.SET_SOURCE_FILE: {
                            out.annotateTo(reader.getOffset(), "DBG_SET_FILE");
                            out.indent();
                            int nameIdx = reader.readSmallUleb128() - 1;
                            out.annotateTo(reader.getOffset(), "name_idx = %s",
                                    StringIdItem.getOptionalReferenceAnnotation(dexFile, nameIdx));
                            out.deindent();
                            break;
                        }
                        default:
                            int adjusted = opcode - 0x0A;
                            int addressDiff = adjusted / 15;
                            int lineDiff = (adjusted % 15) - 4;
                            codeAddress += addressDiff;
                            lineNumber += lineDiff;
                            out.annotateTo(reader.getOffset(), "address_diff = +0x%x:0x%x, line_diff = +%d:%d, ",
                                    addressDiff, codeAddress, lineDiff, lineNumber);
                            break;
                    }
                }
                out.deindent();
            }
        };
    }
}
