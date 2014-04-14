/*
 * Copyright 2012, Google Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 *     * Neither the name of Google Inc. nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.jf.dexlib2.util;

import org.jf.dexlib2.ValueType;
import org.jf.dexlib2.iface.value.*;

public final class EncodedValueUtils {
    public static boolean isDefaultValue(EncodedValue encodedValue) {
        switch (encodedValue.getValueType()) {
            case ValueType.BOOLEAN:
                return !((BooleanEncodedValue)encodedValue).getValue();
            case ValueType.BYTE:
                return ((ByteEncodedValue)encodedValue).getValue() == 0;
            case ValueType.CHAR:
                return ((CharEncodedValue)encodedValue).getValue() == 0;
            case ValueType.DOUBLE:
                return ((DoubleEncodedValue)encodedValue).getValue() == 0;
            case ValueType.FLOAT:
                return ((FloatEncodedValue)encodedValue).getValue() == 0;
            case ValueType.INT:
                return ((IntEncodedValue)encodedValue).getValue() == 0;
            case ValueType.LONG:
                return ((LongEncodedValue)encodedValue).getValue() == 0;
            case ValueType.NULL:
                return true;
            case ValueType.SHORT:
                return ((ShortEncodedValue)encodedValue).getValue() == 0;
        }
        return false;
    }

    private EncodedValueUtils() {}
}
