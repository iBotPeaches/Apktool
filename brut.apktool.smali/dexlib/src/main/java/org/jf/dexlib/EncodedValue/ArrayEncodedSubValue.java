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
import org.jf.dexlib.Util.AnnotatedOutput;
import org.jf.dexlib.Util.Input;
import org.jf.dexlib.Util.Leb128Utils;

/**
 * An <code>ArrayEncodedSubValue</code> is identical to an <code>ArrayEncodedValue</code>, except that it
 * doesn't have the initial valueType/valueArg byte. This is used in the <code>EncodedArrayItem</code> object
 */
public class ArrayEncodedSubValue extends EncodedValue {
 private int hashCode = 0;

    public final EncodedValue[] values;

    /**
     * Constructs a new <code>ArrayEncodedSubValue</code> by reading the value from the given <code>Input</code> object.
     * The <code>Input</code>'s cursor should be set to the 2nd byte of the encoded value
     * @param dexFile The <code>DexFile</code> that is being read in
     * @param in The <code>Input</code> object to read from
     */
    public ArrayEncodedSubValue(DexFile dexFile, Input in) {
        values = new EncodedValue[in.readUnsignedLeb128()];

        for (int i=0; i<values.length; i++) {
            values[i] = EncodedValue.readEncodedValue(dexFile, in);
        }
    }

    /**
     * Constructs a new <code>ArrayEncodedSubValue</code> with the given values
     * @param values The array values
     */
    public ArrayEncodedSubValue(EncodedValue[] values) {
        this.values = values;
    }

    /** {@inheritDoc} */
    public void writeValue(AnnotatedOutput out) {
        if (out.annotates())
        {
            out.annotate("array_size: 0x" + Integer.toHexString(values.length) + " (" + values.length + ")");
            out.writeUnsignedLeb128(values.length);
            int index = 0;
            for (EncodedValue encodedValue: values) {
                out.annotate(0, "[" + index++ + "] array_element");
                out.indent();
                encodedValue.writeValue(out);
                out.deindent();
            }
        } else {
            out.writeUnsignedLeb128(values.length);
            for (EncodedValue encodedValue: values) {
                encodedValue.writeValue(out);
            }
        }
    }

    /** {@inheritDoc} */
    public int placeValue(int offset) {
        offset = offset + Leb128Utils.unsignedLeb128Size(values.length);
        for (EncodedValue encodedValue: values) {
            offset = encodedValue.placeValue(offset);
        }

        return offset;
    }

    /** {@inheritDoc} */
    protected int compareValue(EncodedValue o) {
        ArrayEncodedSubValue other = (ArrayEncodedSubValue)o;

        int comp = values.length - other.values.length;
        if (comp != 0) {
            return comp;
        }

        for (int i=0; i<values.length; i++) {
            comp = values[i].compareTo(other.values[i]);
            if (comp != 0) {
                return comp;
            }
        }

        return comp;
    }

    /** {@inheritDoc} */
    public ValueType getValueType() {
        return ValueType.VALUE_ARRAY;
    }

    /**
     * calculate and cache the hashcode
     */
    private void calcHashCode() {
        hashCode = 0;

        for (EncodedValue encodedValue: values) {
            hashCode = 31 * hashCode + encodedValue.hashCode();
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
