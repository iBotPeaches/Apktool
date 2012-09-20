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

import org.jf.dexlib.EncodedValue.AnnotationEncodedSubValue;
import org.jf.dexlib.Util.AnnotatedOutput;
import org.jf.dexlib.Util.Input;

public class AnnotationItem extends Item<AnnotationItem> {
    private int hashCode = 0;

    private AnnotationVisibility visibility;
    private AnnotationEncodedSubValue annotationValue;

    /**
     * Creates a new uninitialized <code>AnnotationItem</code>
     * @param dexFile The <code>DexFile</code> that this item belongs to
     */
    protected AnnotationItem(DexFile dexFile) {
        super(dexFile);
    }

    /**
     * Creates a new <code>AnnotationItem</code> with the given values
     * @param dexFile The <code>DexFile</code> that this item belongs to
     * @param visibility The visibility of this annotation
     * @param annotationValue The value of this annotation
     */
    private AnnotationItem(DexFile dexFile, AnnotationVisibility visibility,
                           AnnotationEncodedSubValue annotationValue) {
        super(dexFile);
        this.visibility = visibility;
        this.annotationValue = annotationValue;
    }

    /**
     * Returns an <code>AnnotationItem</code> for the given values, and that has been interned into the given
     * <code>DexFile</code>
     * @param dexFile The <code>DexFile</code> that this item belongs to
     * @param visibility The visibility of this annotation
     * @param annotationValue The value of this annotation
     * @return an <code>AnnotationItem</code> for the given values, and that has been interned into the given
     * <code>DexFile</code>
     */
    public static AnnotationItem internAnnotationItem(DexFile dexFile, AnnotationVisibility visibility,
                           AnnotationEncodedSubValue annotationValue) {
        AnnotationItem annotationItem = new AnnotationItem(dexFile, visibility, annotationValue);
        return dexFile.AnnotationsSection.intern(annotationItem);
    }

    /** {@inheritDoc} */
    protected void readItem(Input in, ReadContext readContext) {
        visibility = AnnotationVisibility.fromByte(in.readByte());
        annotationValue = new AnnotationEncodedSubValue(dexFile, in);
    }

    /** {@inheritDoc} */
    protected int placeItem(int offset) {
        return annotationValue.placeValue(offset + 1);
    }

    /** {@inheritDoc} */
    protected void writeItem(AnnotatedOutput out) {
        if (out.annotates()) {
            out.annotate("visibility: " + visibility.name());
            out.writeByte(visibility.value);
            annotationValue.writeValue(out);
        }else {
            out.writeByte(visibility.value);
            annotationValue.writeValue(out);
        }
    }

    /** {@inheritDoc} */
    public ItemType getItemType() {
        return ItemType.TYPE_ANNOTATION_ITEM;
    }

    /** {@inheritDoc} */
    public String getConciseIdentity() {
        return "annotation_item @0x" + Integer.toHexString(getOffset());
    }

    /** {@inheritDoc} */
    public int compareTo(AnnotationItem o) {
        int comp = visibility.value - o.visibility.value;
        if (comp == 0) {
            comp = annotationValue.compareTo(o.annotationValue);
        }
        return comp;
    }

    /**
     * @return The visibility of this annotation
     */
    public AnnotationVisibility getVisibility() {
        return visibility;
    }

    /**
     * @return The encoded annotation value of this annotation
     */
    public AnnotationEncodedSubValue getEncodedAnnotation() {
        return annotationValue;
    }

    /**
     * calculate and cache the hashcode
     */
    private void calcHashCode() {
        hashCode = visibility.value;
        hashCode = hashCode * 31 + annotationValue.hashCode();
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

        AnnotationItem other = (AnnotationItem)o;
        return visibility == other.visibility && annotationValue.equals(other.annotationValue);
    }
}
