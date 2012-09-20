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

public class BooleanEncodedValue extends EncodedValue {
    /**
     * The dupliton values
     */
    public static final BooleanEncodedValue TrueValue = new BooleanEncodedValue(true);
    public static final BooleanEncodedValue FalseValue = new BooleanEncodedValue(false);

    public final boolean value;

    /**
     * Constructs a new <code>BooleanEncodedValue</code> with the given value
     * @param value The value
     */
    private BooleanEncodedValue(boolean value) {
        this.value = value;
    }

    /**
     * Gets the <code>BooleanEncodedValue</code> for the given valueArg value. The high 3 bits of the first byte should
     * be passed as the valueArg parameter
     * @param valueArg The high 3 bits of the first byte of this encoded value
     * @return the <code>BooleanEncodedValue</code> for the given valueArg value
     */
    protected static BooleanEncodedValue getBooleanEncodedValue(byte valueArg) {
        if (valueArg == 0) {
            return FalseValue;
        } else if (valueArg == 1) {
            return TrueValue;
        }
        throw new RuntimeException("valueArg must be either 0 or 1");
    }

    /**
     * Gets the <code>BooleanEncodedValue</code> for the given boolean value
     * @param value the boolean value
     * @return the <code>BooleanEncodedValue</code> for the given boolean value
     */
    public static BooleanEncodedValue getBooleanEncodedValue(boolean value) {
        if (value) {
            return TrueValue;
        }
        return FalseValue;
    }

    /** {@inheritDoc} */
    public void writeValue(AnnotatedOutput out) {
        if (out.annotates()) {
            out.annotate("value_type=" + ValueType.VALUE_BOOLEAN.name() + ",value=" + Boolean.toString(value));
        }
        out.writeByte(ValueType.VALUE_BOOLEAN.value | ((value?1:0) << 5));
    }

    /** {@inheritDoc} */
    public int placeValue(int offset) {
        return offset + 1;
    }

    /** {@inheritDoc} */
    protected int compareValue(EncodedValue o) {
        BooleanEncodedValue other = (BooleanEncodedValue)o;
        if (value == other.value)
            return 0;
        if (value)
            return 1;
        return -1;
    }

    /** {@inheritDoc} */
    public ValueType getValueType() {
        return ValueType.VALUE_BOOLEAN;
    }

    @Override
    public int hashCode() {
        return value?1:0;
    }
}
