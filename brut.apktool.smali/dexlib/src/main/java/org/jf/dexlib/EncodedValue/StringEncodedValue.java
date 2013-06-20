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
import org.jf.dexlib.Util.AnnotatedOutput;
import org.jf.dexlib.Util.EncodedValueUtils;
import org.jf.dexlib.Util.Input;
import org.jf.util.StringUtils;

public class StringEncodedValue extends EncodedValue {
    public final StringIdItem value;

    /**
     * Constructs a new <code>StringEncodedValue</code> by reading the string index from the given <code>Input</code>
     * object. The <code>Input</code>'s cursor should be set to the 2nd byte of the encoded value, and the high 3 bits
     * of the first byte should be passed as the valueArg parameter
     * @param dexFile The <code>DexFile</code> that is being read in
     * @param in The <code>Input</code> object to read from
     * @param valueArg The high 3 bits of the first byte of this encoded value
     */
    protected StringEncodedValue(DexFile dexFile, Input in, byte valueArg) {
        int index = (int)EncodedValueUtils.decodeUnsignedIntegralValue(in.readBytes(valueArg+1));
        value = dexFile.StringIdsSection.getItemByIndex(index);
    }

    /**
     * Constructs a new <code>StringEncodedValue</code> with the given <code>StringIdItem</code> value
     * @param value The <code>StringIdItem</code> value
     */
    public StringEncodedValue(StringIdItem value) {
        this.value = value;
    }

    /** {@inheritDoc} */
    public void writeValue(AnnotatedOutput out) {
        byte[] bytes = EncodedValueUtils.encodeUnsignedIntegralValue(value.getIndex());

        if (out.annotates()) {
            out.annotate(1, "value_type=" + ValueType.VALUE_STRING.name() + ",value_arg=" + (bytes.length - 1));
            out.annotate(bytes.length, "value: \"" + StringUtils.escapeString(value.getStringValue()) + "\"");
        }

        out.writeByte(ValueType.VALUE_STRING.value | ((bytes.length - 1) << 5));
        out.write(bytes);
    }

    /** {@inheritDoc} */
    public int placeValue(int offset) {
        return offset + EncodedValueUtils.getRequiredBytesForUnsignedIntegralValue(value.getIndex()) + 1;
    }

    /** {@inheritDoc} */
    protected int compareValue(EncodedValue o) {
        StringEncodedValue other = (StringEncodedValue)o;

        return value.compareTo(other.value);
    }

    /** {@inheritDoc} */
    public ValueType getValueType() {
        return ValueType.VALUE_STRING;
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
