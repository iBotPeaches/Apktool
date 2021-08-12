/*
 * Copyright 2013, Google Inc.
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

package org.jf.dexlib2.analysis;

import org.jf.dexlib2.Opcode;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class OdexedFieldInstructionMapper {

    private static final int GET = 0;
    private static final int PUT = 1;

    private static final int INSTANCE = 0;
    private static final int STATIC = 1;

    private static final int PRIMITIVE = 0;
    private static final int WIDE = 1;
    private static final int REFERENCE = 2;

    private static class FieldOpcode {
        public final char type;
        public final boolean isStatic;
        @Nonnull public final Opcode normalOpcode;
        @Nullable public final Opcode quickOpcode;
        @Nullable public final Opcode volatileOpcode;

        public FieldOpcode(char type, @Nonnull Opcode normalOpcode, @Nullable Opcode quickOpcode,
                           @Nullable Opcode volatileOpcode) {
            this.type = type;
            this.isStatic = false;
            this.normalOpcode = normalOpcode;
            this.quickOpcode = quickOpcode;
            this.volatileOpcode = volatileOpcode;
        }

        public FieldOpcode(char type, boolean isStatic, @Nonnull Opcode normalOpcode, @Nullable Opcode volatileOpcode) {
            this.type = type;
            this.isStatic = isStatic;
            this.normalOpcode = normalOpcode;
            this.quickOpcode = null;
            this.volatileOpcode = volatileOpcode;
        }

        public FieldOpcode(char type, @Nonnull Opcode normalOpcode, @Nullable Opcode quickOpcode) {
            this.type = type;
            this.isStatic = false;
            this.normalOpcode = normalOpcode;
            this.quickOpcode = quickOpcode;
            this.volatileOpcode = null;
        }
    }

    private static final FieldOpcode[] dalvikFieldOpcodes = new FieldOpcode[] {
            new FieldOpcode('Z', Opcode.IGET_BOOLEAN, Opcode.IGET_QUICK, Opcode.IGET_VOLATILE),
            new FieldOpcode('B', Opcode.IGET_BYTE, Opcode.IGET_QUICK, Opcode.IGET_VOLATILE),
            new FieldOpcode('S', Opcode.IGET_SHORT, Opcode.IGET_QUICK, Opcode.IGET_VOLATILE),
            new FieldOpcode('C', Opcode.IGET_CHAR, Opcode.IGET_QUICK, Opcode.IGET_VOLATILE),
            new FieldOpcode('I', Opcode.IGET, Opcode.IGET_QUICK, Opcode.IGET_VOLATILE),
            new FieldOpcode('F', Opcode.IGET, Opcode.IGET_QUICK, Opcode.IGET_VOLATILE),
            new FieldOpcode('J', Opcode.IGET_WIDE, Opcode.IGET_WIDE_QUICK, Opcode.IGET_WIDE_VOLATILE),
            new FieldOpcode('D', Opcode.IGET_WIDE, Opcode.IGET_WIDE_QUICK, Opcode.IGET_WIDE_VOLATILE),
            new FieldOpcode('L', Opcode.IGET_OBJECT, Opcode.IGET_OBJECT_QUICK, Opcode.IGET_OBJECT_VOLATILE),
            new FieldOpcode('[', Opcode.IGET_OBJECT, Opcode.IGET_OBJECT_QUICK, Opcode.IGET_OBJECT_VOLATILE),

            new FieldOpcode('Z', Opcode.IPUT_BOOLEAN, Opcode.IPUT_QUICK, Opcode.IPUT_VOLATILE),
            new FieldOpcode('B', Opcode.IPUT_BYTE, Opcode.IPUT_QUICK, Opcode.IPUT_VOLATILE),
            new FieldOpcode('S', Opcode.IPUT_SHORT, Opcode.IPUT_QUICK, Opcode.IPUT_VOLATILE),
            new FieldOpcode('C', Opcode.IPUT_CHAR, Opcode.IPUT_QUICK, Opcode.IPUT_VOLATILE),
            new FieldOpcode('I', Opcode.IPUT, Opcode.IPUT_QUICK, Opcode.IPUT_VOLATILE),
            new FieldOpcode('F', Opcode.IPUT, Opcode.IPUT_QUICK, Opcode.IPUT_VOLATILE),
            new FieldOpcode('J', Opcode.IPUT_WIDE, Opcode.IPUT_WIDE_QUICK, Opcode.IPUT_WIDE_VOLATILE),
            new FieldOpcode('D', Opcode.IPUT_WIDE, Opcode.IPUT_WIDE_QUICK, Opcode.IPUT_WIDE_VOLATILE),
            new FieldOpcode('L', Opcode.IPUT_OBJECT, Opcode.IPUT_OBJECT_QUICK, Opcode.IPUT_OBJECT_VOLATILE),
            new FieldOpcode('[', Opcode.IPUT_OBJECT, Opcode.IPUT_OBJECT_QUICK, Opcode.IPUT_OBJECT_VOLATILE),

            new FieldOpcode('Z', true, Opcode.SPUT_BOOLEAN, Opcode.SPUT_VOLATILE),
            new FieldOpcode('B', true, Opcode.SPUT_BYTE, Opcode.SPUT_VOLATILE),
            new FieldOpcode('S', true, Opcode.SPUT_SHORT, Opcode.SPUT_VOLATILE),
            new FieldOpcode('C', true, Opcode.SPUT_CHAR, Opcode.SPUT_VOLATILE),
            new FieldOpcode('I', true, Opcode.SPUT, Opcode.SPUT_VOLATILE),
            new FieldOpcode('F', true, Opcode.SPUT, Opcode.SPUT_VOLATILE),
            new FieldOpcode('J', true, Opcode.SPUT_WIDE, Opcode.SPUT_WIDE_VOLATILE),
            new FieldOpcode('D', true, Opcode.SPUT_WIDE, Opcode.SPUT_WIDE_VOLATILE),
            new FieldOpcode('L', true, Opcode.SPUT_OBJECT, Opcode.SPUT_OBJECT_VOLATILE),
            new FieldOpcode('[', true, Opcode.SPUT_OBJECT, Opcode.SPUT_OBJECT_VOLATILE),

            new FieldOpcode('Z', true, Opcode.SGET_BOOLEAN, Opcode.SGET_VOLATILE),
            new FieldOpcode('B', true, Opcode.SGET_BYTE, Opcode.SGET_VOLATILE),
            new FieldOpcode('S', true, Opcode.SGET_SHORT, Opcode.SGET_VOLATILE),
            new FieldOpcode('C', true, Opcode.SGET_CHAR, Opcode.SGET_VOLATILE),
            new FieldOpcode('I', true, Opcode.SGET, Opcode.SGET_VOLATILE),
            new FieldOpcode('F', true, Opcode.SGET, Opcode.SGET_VOLATILE),
            new FieldOpcode('J', true, Opcode.SGET_WIDE, Opcode.SGET_WIDE_VOLATILE),
            new FieldOpcode('D', true, Opcode.SGET_WIDE, Opcode.SGET_WIDE_VOLATILE),
            new FieldOpcode('L', true, Opcode.SGET_OBJECT, Opcode.SGET_OBJECT_VOLATILE),
            new FieldOpcode('[', true, Opcode.SGET_OBJECT, Opcode.SGET_OBJECT_VOLATILE),
    };

    private static final FieldOpcode[] artFieldOpcodes = new FieldOpcode[] {
            new FieldOpcode('Z', Opcode.IGET_BOOLEAN, Opcode.IGET_BOOLEAN_QUICK),
            new FieldOpcode('B', Opcode.IGET_BYTE, Opcode.IGET_BYTE_QUICK),
            new FieldOpcode('S', Opcode.IGET_SHORT, Opcode.IGET_SHORT_QUICK),
            new FieldOpcode('C', Opcode.IGET_CHAR, Opcode.IGET_CHAR_QUICK),
            new FieldOpcode('I', Opcode.IGET, Opcode.IGET_QUICK),
            new FieldOpcode('F', Opcode.IGET, Opcode.IGET_QUICK),
            new FieldOpcode('J', Opcode.IGET_WIDE, Opcode.IGET_WIDE_QUICK),
            new FieldOpcode('D', Opcode.IGET_WIDE, Opcode.IGET_WIDE_QUICK),
            new FieldOpcode('L', Opcode.IGET_OBJECT, Opcode.IGET_OBJECT_QUICK),
            new FieldOpcode('[', Opcode.IGET_OBJECT, Opcode.IGET_OBJECT_QUICK),

            new FieldOpcode('Z', Opcode.IPUT_BOOLEAN, Opcode.IPUT_BOOLEAN_QUICK),
            new FieldOpcode('B', Opcode.IPUT_BYTE, Opcode.IPUT_BYTE_QUICK),
            new FieldOpcode('S', Opcode.IPUT_SHORT, Opcode.IPUT_SHORT_QUICK),
            new FieldOpcode('C', Opcode.IPUT_CHAR, Opcode.IPUT_CHAR_QUICK),
            new FieldOpcode('I', Opcode.IPUT, Opcode.IPUT_QUICK),
            new FieldOpcode('F', Opcode.IPUT, Opcode.IPUT_QUICK),
            new FieldOpcode('J', Opcode.IPUT_WIDE, Opcode.IPUT_WIDE_QUICK),
            new FieldOpcode('D', Opcode.IPUT_WIDE, Opcode.IPUT_WIDE_QUICK),
            new FieldOpcode('L', Opcode.IPUT_OBJECT, Opcode.IPUT_OBJECT_QUICK),
            new FieldOpcode('[', Opcode.IPUT_OBJECT, Opcode.IPUT_OBJECT_QUICK)
    };

    private final FieldOpcode[][][] opcodeMap = new FieldOpcode[2][2][10];
    private final Map<Opcode, Integer> opcodeValueTypeMap = new HashMap<Opcode, Integer>(30);

    private static int getValueType(char type) {
        switch (type) {
            case 'Z':
            case 'B':
            case 'S':
            case 'C':
            case 'I':
            case 'F':
                return PRIMITIVE;
            case 'J':
            case 'D':
                return WIDE;
            case 'L':
            case '[':
                return REFERENCE;
        }
        throw new RuntimeException(String.format("Unknown type %s: ", type));
    }

    private static int getTypeIndex(char type) {
        switch (type) {
            case 'Z':
                return 0;
            case 'B':
                return 1;
            case 'S':
                return 2;
            case 'C':
                return 3;
            case 'I':
                return 4;
            case 'F':
                return 5;
            case 'J':
                return 6;
            case 'D':
                return 7;
            case 'L':
                return 8;
            case '[':
                return 9;
        }
        throw new RuntimeException(String.format("Unknown type %s: ", type));
    }

    private static boolean isGet(@Nonnull Opcode opcode) {
        return (opcode.flags & Opcode.SETS_REGISTER) != 0;
    }

    private static boolean isStatic(@Nonnull Opcode opcode) {
        return (opcode.flags & Opcode.STATIC_FIELD_ACCESSOR) != 0;
    }

    public OdexedFieldInstructionMapper(boolean isArt) {
        FieldOpcode[] opcodes;
        if (isArt) {
            opcodes = artFieldOpcodes;
        } else {
            opcodes = dalvikFieldOpcodes;
        }

        for (FieldOpcode fieldOpcode: opcodes) {
            opcodeMap[isGet(fieldOpcode.normalOpcode)?GET:PUT]
                    [isStatic(fieldOpcode.normalOpcode)?STATIC:INSTANCE]
                    [getTypeIndex(fieldOpcode.type)] = fieldOpcode;

            if (fieldOpcode.quickOpcode != null) {
                opcodeValueTypeMap.put(fieldOpcode.quickOpcode, getValueType(fieldOpcode.type));
            }
            if (fieldOpcode.volatileOpcode != null) {
                opcodeValueTypeMap.put(fieldOpcode.volatileOpcode, getValueType(fieldOpcode.type));
            }
        }
    }

    @Nonnull
    public Opcode getAndCheckDeodexedOpcode(@Nonnull String fieldType, @Nonnull Opcode odexedOpcode) {
        FieldOpcode fieldOpcode = opcodeMap[isGet(odexedOpcode)?GET:PUT]
                [isStatic(odexedOpcode)?STATIC:INSTANCE]
                [getTypeIndex(fieldType.charAt(0))];

        if (!isCompatible(odexedOpcode, fieldOpcode.type)) {
            throw new AnalysisException(String.format("Incorrect field type \"%s\" for %s", fieldType,
                    odexedOpcode.name));
        }

        return fieldOpcode.normalOpcode;
    }

    private boolean isCompatible(Opcode opcode, char type) {
        Integer valueType = opcodeValueTypeMap.get(opcode);
        if (valueType == null) {
            throw new RuntimeException("Unexpected opcode: " + opcode.name);
        }
        return valueType == getValueType(type);
    }
}


