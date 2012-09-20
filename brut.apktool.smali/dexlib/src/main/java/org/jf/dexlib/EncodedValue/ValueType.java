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

import org.jf.dexlib.Util.SparseArray;

public enum ValueType {

    VALUE_BYTE((byte) 0x00),
    VALUE_SHORT((byte) 0x02),
    VALUE_CHAR((byte) 0x03),
    VALUE_INT((byte) 0x04),
    VALUE_LONG((byte) 0x06),
    VALUE_FLOAT((byte) 0x10),
    VALUE_DOUBLE((byte) 0x11),
    VALUE_STRING((byte) 0x17),
    VALUE_TYPE((byte) 0x18),
    VALUE_FIELD((byte) 0x19),
    VALUE_METHOD((byte) 0x1a),
    VALUE_ENUM((byte) 0x1b),
    VALUE_ARRAY((byte) 0x1c),
    VALUE_ANNOTATION((byte) 0x1d),
    VALUE_NULL((byte) 0x1e),
    VALUE_BOOLEAN((byte) 0x1f);

    /**
     * A map to facilitate looking up a <code>ValueType</code> by byte value
     */
    private final static SparseArray<ValueType> valueTypeIntegerMap;

    static {
        /** build the <code>valueTypeIntegerMap</code> object */
        valueTypeIntegerMap = new SparseArray<ValueType>(16);

        for (ValueType valueType : ValueType.values()) {
            valueTypeIntegerMap.put(valueType.value, valueType);
        }
    }

    /**
     * The byte value for this ValueType
     */
    public final byte value;

    private ValueType(byte value) {
        this.value = value;
    }

    /**
     * Converts a byte value to the corresponding ValueType enum value,
     * or null if the value isn't a valid ValueType value
     *
     * @param valueType the byte value to convert to a ValueType
     * @return the ValueType enum value corresponding to valueType, or null
     *         if not a valid ValueType value
     */
    public static ValueType fromByte(byte valueType) {
        return valueTypeIntegerMap.get(valueType);
    }
}