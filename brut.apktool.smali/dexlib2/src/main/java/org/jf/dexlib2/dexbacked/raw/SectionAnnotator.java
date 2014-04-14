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

import com.google.common.collect.Maps;
import org.jf.dexlib2.dexbacked.raw.util.DexAnnotator;
import org.jf.dexlib2.util.AnnotatedBytes;
import org.jf.util.AlignmentUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

public abstract class SectionAnnotator {
    @Nonnull public final DexAnnotator annotator;
    @Nonnull public final RawDexFile dexFile;
    public final int itemType;
    public final int sectionOffset;
    public final int itemCount;

    private Map<Integer, String> itemIdentities = Maps.newHashMap();

    public SectionAnnotator(@Nonnull DexAnnotator annotator, @Nonnull MapItem mapItem) {
        this.annotator = annotator;
        this.dexFile = annotator.dexFile;
        this.itemType = mapItem.getType();

        this.sectionOffset = mapItem.getOffset();
        this.itemCount = mapItem.getItemCount();
    }

    @Nonnull public abstract String getItemName();
    protected abstract void annotateItem(@Nonnull AnnotatedBytes out, int itemIndex, @Nullable String itemIdentity);

    /**
     * Write out annotations for this section
     *
     * @param out The AnnotatedBytes object to annotate to
     */
    public void annotateSection(@Nonnull AnnotatedBytes out) {
        out.moveTo(sectionOffset);
        annotateSectionInner(out, itemCount);
    }

    protected void annotateSectionInner(@Nonnull AnnotatedBytes out, int itemCount) {
        String itemName = getItemName();
        int itemAlignment = getItemAlignment();
        if (itemCount > 0) {
            out.annotate(0, "");
            out.annotate(0, "-----------------------------");
            out.annotate(0, "%s section", itemName);
            out.annotate(0, "-----------------------------");
            out.annotate(0, "");

            for (int i=0; i<itemCount; i++) {
                out.moveTo(AlignmentUtils.alignOffset(out.getCursor(), itemAlignment));

                String itemIdentity = getItemIdentity(out.getCursor());
                if (itemIdentity != null) {
                    out.annotate(0, "[%d] %s: %s", i, itemName, itemIdentity);
                } else {
                    out.annotate(0, "[%d] %s", i, itemName);
                }
                out.indent();
                annotateItem(out, i, itemIdentity);
                out.deindent();
            }
        }
    }

    @Nullable private String getItemIdentity(int itemOffset) {
        return itemIdentities.get(itemOffset);
    }

    public void setItemIdentity(int itemOffset, String identity) {
        itemIdentities.put(itemOffset, identity);
    }

    public int getItemAlignment() {
        return 1;
    }
}
