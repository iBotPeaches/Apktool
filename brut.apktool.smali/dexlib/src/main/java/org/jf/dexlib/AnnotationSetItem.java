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

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class AnnotationSetItem extends Item<AnnotationSetItem> {
    private int hashCode = 0;

    private AnnotationItem[] annotations;

    /**
     * Creates a new uninitialized <code>AnnotationSetItem</code>
     * @param dexFile The <code>DexFile</code> that this item belongs to
     */
    protected AnnotationSetItem(DexFile dexFile) {
        super(dexFile);
    }

    /**
     * Creates a new <code>AnnotationSetItem</code> for the given annotations
     * @param dexFile The <code>DexFile</code> that this item belongs to
     * @param annotations The annotations for this <code>AnnotationSetItem</code>
     */
    private AnnotationSetItem(DexFile dexFile, AnnotationItem[] annotations) {
        super(dexFile);
        this.annotations = annotations;
    }

    /**
     * Returns an <code>AnnotationSetItem</code> for the given annotations, and that has been interned into the given
     * <code>DexFile</code>
     * @param dexFile The <code>DexFile</code> that this item belongs to
     * @param annotations The annotations for this <code>AnnotationSetItem</code>
     * @return an <code>AnnotationSetItem</code> for the given annotations
     */
    public static AnnotationSetItem internAnnotationSetItem(DexFile dexFile, List<AnnotationItem> annotations) {
        AnnotationSetItem annotationSetItem;
        if (annotations == null) {
            annotationSetItem = new AnnotationSetItem(dexFile, new AnnotationItem[0]);
        } else {
            AnnotationItem[] annotationsArray = new AnnotationItem[annotations.size()];
            annotations.toArray(annotationsArray);
            annotationSetItem = new AnnotationSetItem(dexFile, annotationsArray);
        }
        return dexFile.AnnotationSetsSection.intern(annotationSetItem);
    }

    /** {@inheritDoc} */
    protected void readItem(Input in, ReadContext readContext) {
        annotations = new AnnotationItem[in.readInt()];

        for (int i=0; i<annotations.length; i++) {
            annotations[i] = (AnnotationItem)readContext.getOffsettedItemByOffset(ItemType.TYPE_ANNOTATION_ITEM,
                    in.readInt());
        }
    }

    /** {@inheritDoc} */
    protected int placeItem(int offset) {
        return offset + 4 + annotations.length * 4;
    }

    /** {@inheritDoc} */
    protected void writeItem(AnnotatedOutput out) {
        Arrays.sort(annotations, new Comparator<AnnotationItem>() {
            public int compare(AnnotationItem annotationItem, AnnotationItem annotationItem2) {
                int annotationItemIndex = annotationItem.getEncodedAnnotation().annotationType.getIndex();
                int annotationItemIndex2 = annotationItem2.getEncodedAnnotation().annotationType.getIndex();
                if (annotationItemIndex < annotationItemIndex2) {
                    return -1;
                } else if (annotationItemIndex == annotationItemIndex2) {
                    return 0;
                }
                return 1;
            }
        });


        if (out.annotates()) {
            out.annotate(4, "size: 0x" + Integer.toHexString(annotations.length) + " (" + annotations.length + ")");
            for (AnnotationItem annotationItem: annotations) {
                out.annotate(4, "annotation_off: 0x" + Integer.toHexString(annotationItem.getOffset()) + " - " +
                        annotationItem.getEncodedAnnotation().annotationType.getTypeDescriptor());
            }
        }
        out.writeInt(annotations.length);
        for (AnnotationItem annotationItem: annotations) {
            out.writeInt(annotationItem.getOffset());
        }
    }

    /** {@inheritDoc} */
    public ItemType getItemType() {
        return ItemType.TYPE_ANNOTATION_SET_ITEM;
    }

    /** {@inheritDoc} */
    public String getConciseIdentity() {
        return "annotation_set_item @0x" + Integer.toHexString(getOffset());
    }

/** {@inheritDoc} */
    public int compareTo(AnnotationSetItem o) {
        if (o == null) {
            return 1;
        }

        int comp = annotations.length - o.annotations.length;
        if (comp == 0) {
            for (int i=0; i<annotations.length; i++) {
                comp = annotations[i].compareTo(o.annotations[i]);
                if (comp != 0) {
                    return comp;
                }
            }
        }
        return comp;
    }

    /**
     * @return An array of the <code>AnnotationItem</code> objects in this <code>AnnotationSetItem</code>
     */
    public AnnotationItem[] getAnnotations() {
        return annotations;
    }

    /**
     * calculate and cache the hashcode
     */
    private void calcHashCode() {
        hashCode = 0;
        for (AnnotationItem annotationItem: annotations) {
            hashCode = hashCode * 31 + annotationItem.hashCode();
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

        AnnotationSetItem other = (AnnotationSetItem)o;
        return (this.compareTo(other) == 0);
    }
}
