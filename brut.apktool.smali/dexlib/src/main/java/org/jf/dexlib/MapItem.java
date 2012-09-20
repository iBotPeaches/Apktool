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

import org.jf.dexlib.Util.AnnotatedOutput;
import org.jf.dexlib.Util.Input;

/**
 * This item represents a map_list item from the dex specification. It contains a
 * SectionInfo instance for every section in the DexFile, with the number of items
 * in and offset of that section.
 */
public class MapItem extends Item<MapItem> {
    /**
     * This item is read in immediately after the HeaderItem, and the section info contained
     * by this item is added to the ReadContext object, which is used when reading in the other
     * sections in the dex file.
     *
     * This item should be placed last. It depends on the fact that the other sections
     * in the file have been placed.
     */

    /**
     * Create a new uninitialized <code>MapItem</code>
     * @param dexFile The <code>DexFile</code> that this item belongs to
     */
    protected MapItem(final DexFile dexFile) {
        super(dexFile);
    }

    /** {@inheritDoc} */
    protected int placeItem(int offset) {
        Section[] sections = dexFile.getOrderedSections();
        //the list returned by getOrderedSections doesn't contain the header
        //or map section, so add 2 to the length
        return offset + 4 + (sections.length + 2) * 12;
    }

    /** {@inheritDoc} */
    protected void readItem(Input in, ReadContext readContext) {
        int size = in.readInt();

        for (int i=0; i<size; i++) {
            ItemType itemType = ItemType.fromInt(in.readShort());

            //unused
            in.readShort();

            int sectionSize = in.readInt();
            int sectionOffset = in.readInt();

            readContext.addSection(itemType, sectionSize, sectionOffset);
        }
    }

    /** {@inheritDoc} */
    protected void writeItem(AnnotatedOutput out) {
        assert getOffset() > 0;
        Section[] sections = dexFile.getOrderedSections();

        out.annotate("map_size: 0x" + Integer.toHexString(sections.length + 2) + " (" +
                Integer.toString(sections.length + 2) + ")");
        out.writeInt(sections.length + 2);

        int index = 0;
        out.annotate(0, "[" + index++ + "]");
        out.indent();
        writeSectionInfo(out, ItemType.TYPE_HEADER_ITEM, 1, 0);
        out.deindent();

        for (Section section: dexFile.getOrderedSections()) {
            out.annotate(0, "[" + index++ + "]");
            out.indent();
            writeSectionInfo(out, section.ItemType, section.getItems().size(), section.getOffset());
            out.deindent();
        }

        out.annotate(0, "[" + index++ + "]");
        out.indent();
        writeSectionInfo(out, ItemType.TYPE_MAP_LIST, 1, dexFile.MapItem.getOffset());
        out.deindent();
    }

    private void writeSectionInfo(AnnotatedOutput out, ItemType itemType, int sectionSize, int sectionOffset) {
        if (out.annotates()) {
            out.annotate(2, "item_type: " + itemType);
            out.annotate(2, "unused");
            out.annotate(4, "section_size: 0x" + Integer.toHexString(sectionSize) + " (" + sectionSize + ")");
            out.annotate(4, "section_off: 0x" + Integer.toHexString(sectionOffset));
        }

        out.writeShort(itemType.MapValue);
        out.writeShort(0);
        out.writeInt(sectionSize);
        out.writeInt(sectionOffset);
    }

    /** {@inheritDoc} */
    public ItemType getItemType() {
        return ItemType.TYPE_MAP_LIST;
    }

    /** {@inheritDoc} */
    public int compareTo(MapItem o) {
        return 0;
    }

    /** {@inheritDoc} */
    public String getConciseIdentity() {
        return "map_item";
    }
}
