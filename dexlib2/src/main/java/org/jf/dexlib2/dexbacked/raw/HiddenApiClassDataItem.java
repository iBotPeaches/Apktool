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

import org.jf.dexlib2.HiddenApiRestriction;
import org.jf.dexlib2.dexbacked.DexBuffer;
import org.jf.dexlib2.dexbacked.DexReader;
import org.jf.dexlib2.dexbacked.raw.util.DexAnnotator;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.Field;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.util.AnnotatedBytes;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class HiddenApiClassDataItem {
    public static final int SIZE_OFFSET = 0x0;
    public static final int OFFSETS_LIST_OFFSET = 0x4;

    public static final int OFFSET_ITEM_SIZE = 0x4;


    @Nonnull
    public static SectionAnnotator makeAnnotator(@Nonnull DexAnnotator annotator, @Nonnull MapItem mapItem) {
        return new SectionAnnotator(annotator, mapItem) {
            @Nonnull @Override public String getItemName() {
                return "hiddenapi_class_data_item";
            }

            @Override
            protected void annotateItem(@Nonnull AnnotatedBytes out, int itemIndex, @Nullable String itemIdentity) {
                int startOffset = out.getCursor();

                out.annotate(4, "size = 0x%x", dexFile.getDataBuffer().readSmallUint(out.getCursor()));

                int index = 0;
                for (ClassDef classDef : dexFile.getClasses()) {
                    out.annotate(0, "[%d] %s", index, classDef);
                    out.indent();

                    int offset = dexFile.getDataBuffer().readSmallUint(out.getCursor());
                    if (offset == 0) {
                        out.annotate(4, "offset = 0x%x", offset);
                    } else {
                        out.annotate(4, "offset = 0x%x (absolute offset: 0x%x)", offset, startOffset + offset);
                    }

                    int nextOffset = out.getCursor();
                    if (offset > 0) {
                        out.deindent();

                        out.moveTo(startOffset + offset);

                        DexReader<? extends DexBuffer> reader = dexFile.getBuffer().readerAt(out.getCursor());

                        for (Field field : classDef.getStaticFields()) {
                            out.annotate(0, "%s:", field);
                            out.indent();
                            int restrictions = reader.readSmallUleb128();
                            out.annotateTo(reader.getOffset(), "restriction = 0x%x: %s",
                                    restrictions,
                                    HiddenApiRestriction.formatHiddenRestrictions(restrictions));
                            out.deindent();
                        }
                        for (Field field : classDef.getInstanceFields()) {
                            out.annotate(0, "%s:", field);
                            out.indent();
                            int restrictions = reader.readSmallUleb128();
                            out.annotateTo(reader.getOffset(), "restriction = 0x%x: %s",
                                    restrictions,
                                    HiddenApiRestriction.formatHiddenRestrictions(restrictions));
                            out.deindent();
                        }
                        for (Method method : classDef.getDirectMethods()) {
                            out.annotate(0, "%s:", method);
                            out.indent();
                            int restrictions = reader.readSmallUleb128();
                            out.annotateTo(reader.getOffset(), "restriction = 0x%x: %s",
                                    restrictions,
                                    HiddenApiRestriction.formatHiddenRestrictions(restrictions));
                            out.deindent();
                        }
                        for (Method method : classDef.getVirtualMethods()) {
                            out.annotate(0, "%s:", method);
                            out.indent();
                            int restrictions = reader.readSmallUleb128();
                            out.annotateTo(reader.getOffset(), "restriction = 0x%x: %s",
                                    restrictions,
                                    HiddenApiRestriction.formatHiddenRestrictions(restrictions));
                            out.deindent();
                        }

                        out.indent();
                    }

                    out.moveTo(nextOffset);

                    out.deindent();

                    index++;
                }
            }
        };
    }
}
