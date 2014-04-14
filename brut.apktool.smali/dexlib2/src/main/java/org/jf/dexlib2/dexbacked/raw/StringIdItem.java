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

import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.dexbacked.raw.util.DexAnnotator;
import org.jf.dexlib2.util.AnnotatedBytes;
import org.jf.util.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class StringIdItem {
    public static final int ITEM_SIZE = 4;

    @Nonnull
    public static SectionAnnotator makeAnnotator(@Nonnull DexAnnotator annotator, @Nonnull MapItem mapItem) {
        return new SectionAnnotator(annotator, mapItem) {
            @Nonnull @Override public String getItemName() {
                return "string_id_item";
            }

            @Override
            public void annotateItem(@Nonnull AnnotatedBytes out, int itemIndex, @Nullable String itemIdentity) {
                int stringDataOffset = dexFile.readSmallUint(out.getCursor());
                try {
                    String stringValue = dexFile.getString(itemIndex);
                    out.annotate(4, "string_data_item[0x%x]: \"%s\"", stringDataOffset,
                            StringUtils.escapeString(stringValue));
                    return;
                } catch (Exception ex) {
                    System.err.print("Error while resolving string value at index: ");
                    System.err.print(itemIndex);
                    ex.printStackTrace(System.err);
                }

                out.annotate(4, "string_id_item[0x%x]", stringDataOffset);
            }
        };
    }

    @Nonnull
    public static String getReferenceAnnotation(@Nonnull DexBackedDexFile dexFile, int stringIndex) {
        return getReferenceAnnotation(dexFile, stringIndex, false);

    }

    public static String getReferenceAnnotation(@Nonnull DexBackedDexFile dexFile, int stringIndex, boolean quote) {
        try {
            String string = dexFile.getString(stringIndex);
            if (quote) {
                string = String.format("\"%s\"", StringUtils.escapeString(string));
            }
            return String.format("string_id_item[%d]: %s", stringIndex, string);
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
        }
        return String.format("string_id_item[%d]", stringIndex);
    }


    @Nonnull
    public static String getOptionalReferenceAnnotation(@Nonnull DexBackedDexFile dexFile, int stringIndex) {
        return getOptionalReferenceAnnotation(dexFile, stringIndex, false);

    }

    public static String getOptionalReferenceAnnotation(@Nonnull DexBackedDexFile dexFile, int stringIndex,
                                                        boolean quote) {
        if (stringIndex == -1) {
            return "string_id_item[NO_INDEX]";
        }
        return getReferenceAnnotation(dexFile, stringIndex, quote);
    }

    public static String[] getStrings(@Nonnull RawDexFile dexFile) {
        MapItem mapItem = dexFile.getMapItemForSection(ItemType.STRING_ID_ITEM);
        if (mapItem == null) {
            return new String[0];
        }

        int stringCount = mapItem.getItemCount();
        String[] ret = new String[stringCount];
        for (int i=0; i<stringCount; i++) {
            ret[i] = dexFile.getString(i);
        }
        return ret;
    }
}
