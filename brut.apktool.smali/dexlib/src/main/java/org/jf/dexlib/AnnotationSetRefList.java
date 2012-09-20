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

import java.util.List;

public class AnnotationSetRefList extends Item<AnnotationSetRefList> {
    private int hashCode = 0;

    private AnnotationSetItem[] annotationSets;

     /**
     * Creates a new uninitialized <code>AnnotationSetRefList</code>
     * @param dexFile The <code>DexFile</code> that this item belongs to
     */
    protected AnnotationSetRefList(DexFile dexFile) {
        super(dexFile);
    }

    /**
     * Creates a new <code>AnnotationSetRefList</code> for the given annotation sets
     * @param dexFile The <code>DexFile</code> that this item belongs to
     * @param annotationSets The annotationSets for this <code>AnnotationSetRefList</code>
     */
    private AnnotationSetRefList(DexFile dexFile, AnnotationSetItem[] annotationSets) {
        super(dexFile);
        this.annotationSets = annotationSets;
    }

    /**
     * Returns an <code>AnnotationSetRefList</code> for the given annotation sets, and that has been interned into the
     * given <code>DexFile</code>
     * @param dexFile The <code>DexFile</code> that this item belongs to
     * @param annotationSets The annotation sets for this <code>AnnotationSetRefList</code>
     * @return an <code>AnnotationSetItem</code> for the given annotations
     */
    public static AnnotationSetRefList internAnnotationSetRefList(DexFile dexFile,
                                                                       List<AnnotationSetItem> annotationSets) {
        AnnotationSetItem[] annotationSetsArray = new AnnotationSetItem[annotationSets.size()];
        annotationSets.toArray(annotationSetsArray);
        AnnotationSetRefList annotationSetRefList = new AnnotationSetRefList(dexFile, annotationSetsArray);
        return dexFile.AnnotationSetRefListsSection.intern(annotationSetRefList);
    }

    /** {@inheritDoc} */
    protected void readItem(Input in, ReadContext readContext) {
        annotationSets = new AnnotationSetItem[in.readInt()];

        for (int i=0; i<annotationSets.length; i++) {
            annotationSets[i] = (AnnotationSetItem)readContext.getOptionalOffsettedItemByOffset(
                    ItemType.TYPE_ANNOTATION_SET_ITEM, in.readInt());
        }
    }

    /** {@inheritDoc} */
    protected int placeItem(int offset) {
        return offset + 4 + annotationSets.length * 4;
    }

    /** {@inheritDoc} */
    protected void writeItem(AnnotatedOutput out) {
        if (out.annotates()) {
            out.annotate(4, "size: 0x" + Integer.toHexString(annotationSets.length) + " (" + annotationSets.length +
                    ")");
            for (AnnotationSetItem annotationSetItem: annotationSets) {
                out.annotate(4, "annotation_set_off: 0x" + Integer.toHexString(annotationSetItem.getOffset()));
            }
        }
        out.writeInt(annotationSets.length);
        for (AnnotationSetItem annotationSetItem: annotationSets) {
            out.writeInt(annotationSetItem.getOffset());
        }
    }

    /** {@inheritDoc} */
    public ItemType getItemType() {
        return ItemType.TYPE_ANNOTATION_SET_REF_LIST;
    }

    /** {@inheritDoc} */
    public String getConciseIdentity() {
        return "annotation_set_item @0x" + Integer.toHexString(getOffset());
    }

    /** {@inheritDoc} */
    public int compareTo(AnnotationSetRefList o) {
        int comp = annotationSets.length - o.annotationSets.length;
        if (comp != 0) {
            return comp;
        }

        for (int i=0; i<annotationSets.length; i++) {
            comp = annotationSets[i].compareTo(o.annotationSets[i]);
            if (comp != 0) {
                return comp;
            }
        }

        return comp;
    }

    /**
     * @return An array of the <code>AnnotationSetItem</code> objects that make up this
     * <code>AnnotationSetRefList</code>
     */
    public AnnotationSetItem[] getAnnotationSets() {
        return annotationSets;
    }

    /**
     * calculate and cache the hashcode
     */
    private void calcHashCode() {
        hashCode = 0;
        for (AnnotationSetItem annotationSetItem: annotationSets) {
            hashCode = hashCode * 31 + annotationSetItem.hashCode();
        }
    }

    @Override
    public int hashCode() {
        //there's a small possibility that the actual hash code will be 0. If so, we'll
        //just end up recalculating it each time
        if (hashCode == 0)
            calcHashCode();
        return hashCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this==o) {
            return true;
        }
        if (o==null || !this.getClass().equals(o.getClass())) {
            return false;
        }

        AnnotationSetRefList other = (AnnotationSetRefList)o;
        return (this.compareTo(other) == 0);
    }
}
