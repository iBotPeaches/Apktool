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

package org.jf.dexlib.Util;

import org.jf.dexlib.EncodedValue.*;
import org.jf.dexlib.TypeIdItem;

public class TypeUtils
{
    public static EncodedValue makeDefaultValueForType(String type) {
        switch (type.charAt(0)) {
            case 'Z':
                return BooleanEncodedValue.FalseValue;
            case 'B':
                return new ByteEncodedValue((byte)0);
            case 'S':
                return new ShortEncodedValue((short)0);
            case 'C':
                return new CharEncodedValue((char)0);
            case 'I':
                return new IntEncodedValue(0);
            case 'J':
                return new LongEncodedValue(0);
            case 'F':
                return new FloatEncodedValue(0);
            case 'D':
                return new DoubleEncodedValue(0);
            case 'L':
            case '[':
                return NullEncodedValue.NullValue;
        }
        return null;
    }

    public static EncodedValue makeDefaultValueForType(TypeIdItem type) {
        return makeDefaultValueForType(type.getTypeDescriptor());
    }
}
