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

package org.jf.dexlib.EncodedValue;

import org.jf.dexlib.DexFile;
import org.jf.dexlib.StringIdItem;
import org.jf.dexlib.TypeIdItem;
import org.jf.dexlib.Util.AnnotatedOutput;
import org.jf.dexlib.Util.Input;
import org.jf.dexlib.Util.Leb128Utils;

/**
 * An <code>AnnotationEncodedSubValue</code> is identical to an <code>AnnotationEncodedValue</code>, except that it
 * doesn't have the initial valueType/valueArg byte. This is used in the <code>AnnotationItem</code> object
 */
public class AnnotationEncodedSubValue extends EncodedValue {
    private int hashCode = 0;

    public final TypeIdItem annotationType;
    public final StringIdItem[] names;
    public final EncodedValue[] values;

    /**
    * Constructs a new <code>AnnotationEncodedSubValue</code> by reading the value from the given <code>Input</code>
    * object.
    * @param dexFile The <code>DexFile</code> that is being read in
    * @param in The <code>Input</code> object to read from
    */
    public AnnotationEncodedSubValue(DexFile dexFile, Input in) {
        annotationType = dexFile.TypeIdsSection.getItemByIndex(in.readUnsignedLeb128());
        names = new StringIdItem[in.readUnsignedLeb128()];
        values = new EncodedValue[names.length];

        for (int i=0; i<names.length; i++) {
            names[i] = dexFile.StringIdsSection.getItemByIndex(in.readUnsignedLeb128());
            values[i] = EncodedValue.readEncodedValue(dexFile, in);
        }
    }

    /**
     * Constructs a new <code>AnnotationEncodedValue</code> with the given values. names and values must be the same
     * length, and must be sorted according to the name
     * @param annotationType The type of the annotation
     * @param names An array of the names of the elements of the annotation
     * @param values An array of the values of the elements on the annotation
     */
    public AnnotationEncodedSubValue(TypeIdItem annotationType, StringIdItem[] names, EncodedValue[] values) {
        this.annotationType = annotationType;
        if (names.length != values.length) {
            throw new RuntimeException("The names and values parameters must be the same length");
        }
        this.names = names;
        this.values = values;
    }

    /** {@inheritDoc} */
    public void writeValue(AnnotatedOutput out) {
        out.annotate("annotation_type: " + annotationType.getTypeDescriptor());
        out.writeUnsignedLeb128(annotationType.getIndex());
        out.annotate("element_count: 0x" + Integer.toHexString(names.length) + " (" + names.length + ")");
        out.writeUnsignedLeb128(names.length);

        for (int i=0; i<names.length; i++) {
            out.annotate(0, "[" + i + "] annotation_element");
            out.indent();
            out.annotate("element_name: " + names[i].getStringValue());
            out.writeUnsignedLeb128(names[i].getIndex());
            out.annotate(0, "element_value:");
            out.indent();
            values[i].writeValue(out);
            out.deindent();
            out.deindent();
        }
    }

    /** {@inheritDoc} */
    public int placeValue(int offset) {
        offset = offset + Leb128Utils.unsignedLeb128Size(annotationType.getIndex());
        offset = offset + Leb128Utils.unsignedLeb128Size(names.length);

        for (int i=0; i<names.length; i++) {
            offset = offset + Leb128Utils.unsignedLeb128Size(names[i].getIndex());
            offset = values[i].placeValue(offset);
        }

        return offset;
    }

    /** {@inheritDoc} */
    protected int compareValue(EncodedValue o) {
        AnnotationEncodedSubValue other = (AnnotationEncodedSubValue)o;

        int comp = annotationType.compareTo(other.annotationType);
        if (comp != 0) {
            return comp;
        }

        comp = names.length - other.names.length;
        if (comp != 0) {
            return comp;
        }

        for (int i=0; i<names.length; i++) {
            comp = names[i].compareTo(other.names[i]);
            if (comp != 0) {
                return comp;
            }

            comp = values[i].compareTo(other.values[i]);
            if (comp != 0) {
                return comp;
            }
        }

        return comp;
    }

    /** {@inheritDoc} */
    public ValueType getValueType() {
        return ValueType.VALUE_ANNOTATION;
    }

    /**
     * calculate and cache the hashcode
     */
    private void calcHashCode() {
        hashCode = annotationType.hashCode();

        for (int i=0; i<names.length; i++) {
            hashCode = 31 * hashCode + names[i].hashCode();
            hashCode = 31 * hashCode + values[i].hashCode();
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
}
