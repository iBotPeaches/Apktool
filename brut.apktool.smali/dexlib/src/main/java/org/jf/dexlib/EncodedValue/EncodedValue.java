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

public abstract class EncodedValue implements Comparable<EncodedValue> {
    /**
     * Writes this <code>EncodedValue</code> to the given <code>AnnotatedOutput</code> object
     * @param out the <code>AnnotatedOutput</code> object to write to
     */
    public abstract void writeValue(AnnotatedOutput out);

    /**
     * Calculates the size of this encoded value and returns offset + size;
     * @param offset The offset to place this encoded value
     * @return the offset immediately after this encoded value
     */
    public abstract int placeValue(int offset);


    public static EncodedValue readEncodedValue(DexFile dexFile, Input in) {
        Byte b = in.readByte();
        ValueType valueType = ValueType.fromByte((byte)(b & 0x1f));
        byte valueArg = (byte)((b & 0xFF) >> 5);

        switch (valueType) {
            case VALUE_BYTE:
                return new ByteEncodedValue(in);
            case VALUE_SHORT:
                return new ShortEncodedValue(in, valueArg);
            case VALUE_CHAR:
                return new CharEncodedValue(in, valueArg);
            case VALUE_INT:
                return new IntEncodedValue(in, valueArg);
            case VALUE_LONG:
                return new LongEncodedValue(in, valueArg);
            case VALUE_FLOAT:
                return new FloatEncodedValue(in, valueArg);
            case VALUE_DOUBLE:
                return new DoubleEncodedValue(in, valueArg);
            case VALUE_STRING:
                return new StringEncodedValue(dexFile, in, valueArg);
            case VALUE_TYPE:
                return new TypeEncodedValue(dexFile, in, valueArg);
            case VALUE_FIELD:
                return new FieldEncodedValue(dexFile, in, valueArg);
            case VALUE_METHOD:
                return new MethodEncodedValue(dexFile, in, valueArg);
            case VALUE_ENUM:
                return new EnumEncodedValue(dexFile, in, valueArg);
            case VALUE_ARRAY:
                return new ArrayEncodedValue(dexFile, in);
            case VALUE_ANNOTATION:
                return new AnnotationEncodedValue(dexFile, in);
            case VALUE_NULL:
                return NullEncodedValue.NullValue;
            case VALUE_BOOLEAN:
                return BooleanEncodedValue.getBooleanEncodedValue(valueArg);
        }
        return null;
    }

    /** {@inheritDoc} */
    public int compareTo(EncodedValue o) {
        int comp = getValueType().compareTo(o.getValueType());
        if (comp == 0) {
            comp = compareValue(o);
        }
        return comp;
    }

    /**
     * Compare the value of this <code>EncodedValue</code> against the value of the given <EncodedValue>, which
     * is guaranteed to be of the same type as this <code>EncodedValue</code>
     * @param o The <code>EncodedValue</code> to compare against
     * @return A standard comparison integer value
     */
    protected abstract int compareValue(EncodedValue o);

    /**
     * @return the <code>ValueType</code> representing the type of this <code>EncodedValue</code>
     */
    public abstract ValueType getValueType();

    @Override
    public boolean equals(Object o) {
        if (this==o) {
            return true;
        }
        if (o==null || !(o instanceof EncodedValue)) {
            return false;
        }

        return this.compareTo((EncodedValue)o) == 0;
    }
}
