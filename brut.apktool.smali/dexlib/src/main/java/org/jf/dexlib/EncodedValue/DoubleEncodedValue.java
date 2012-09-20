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

import org.jf.dexlib.Util.AnnotatedOutput;
import org.jf.dexlib.Util.EncodedValueUtils;
import org.jf.dexlib.Util.Input;

public class DoubleEncodedValue extends EncodedValue {
    public final double value;

    /**
     * Constructs a new <code>DoubleEncodedValue</code> by reading the value from the given <code>Input</code> object.
     * The <code>Input</code>'s cursor should be set to the 2nd byte of the encoded value, and the high 3 bits of
     * the first byte should be passed as the valueArg parameter
     * @param in The <code>Input</code> object to read from
     * @param valueArg The high 3 bits of the first byte of this encoded value
     */
    protected DoubleEncodedValue(Input in, byte valueArg) {
        long longValue = EncodedValueUtils.decodeRightZeroExtendedValue(in.readBytes(valueArg + 1));
        value = Double.longBitsToDouble(longValue);
    }

    /**
     * Constructs a new <code>DoubleEncodedValue</code> with the given value
     * @param value The value
     */
    public DoubleEncodedValue(double value) {
        this.value = value;
    }

    /** {@inheritDoc} */
    public void writeValue(AnnotatedOutput out) {
        byte[] bytes = EncodedValueUtils.encodeRightZeroExtendedValue(Double.doubleToRawLongBits(value));

        if (out.annotates()) {
            out.annotate(1, "value_type=" + ValueType.VALUE_DOUBLE.name() + ",value_arg=" + (bytes.length - 1));
            out.annotate(bytes.length, "value: " + value);
        }

        out.writeByte(ValueType.VALUE_DOUBLE.value | ((bytes.length - 1) << 5));
        out.write(bytes);
    }

    /** {@inheritDoc} */
    public int placeValue(int offset) {
        return offset + 1 + EncodedValueUtils.getRequiredBytesForRightZeroExtendedValue(
                 Double.doubleToRawLongBits(value));
    }

    /** {@inheritDoc} */
    protected int compareValue(EncodedValue o) {
        DoubleEncodedValue other = (DoubleEncodedValue)o;

        return Double.compare(value, other.value);
    }

    /** {@inheritDoc} */
    public ValueType getValueType() {
        return ValueType.VALUE_DOUBLE;
    }

    @Override
    public int hashCode() {
        return (int)Double.doubleToRawLongBits(value);
    }
}

