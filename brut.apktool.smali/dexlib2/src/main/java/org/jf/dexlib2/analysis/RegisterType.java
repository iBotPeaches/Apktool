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


import org.jf.util.ExceptionWithContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Writer;

public class RegisterType {
    public final byte category;
    @Nullable public final TypeProto type;

    private RegisterType(byte category, @Nullable TypeProto type) {
        assert ((category == REFERENCE || category == UNINIT_REF || category == UNINIT_THIS) && type != null) ||
               ((category != REFERENCE && category != UNINIT_REF && category != UNINIT_THIS) && type == null);

        this.category = category;
        this.type = type;
    }

    @Override
    public String toString() {
        return "(" + CATEGORY_NAMES[category] + (type==null?"":("," + type)) + ")";
    }

    public void writeTo(Writer writer) throws IOException {
        writer.write('(');
        writer.write(CATEGORY_NAMES[category]);
        if (type != null) {
            writer.write(',');
            writer.write(type.getType());
        }
        writer.write(')');
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RegisterType that = (RegisterType) o;

        if (category != that.category) {
            return false;
        }

        // These require strict reference equality. Every instance represents a unique
        // reference that can't be merged with a different one, even if they have the same type.
        if (category == UNINIT_REF || category == UNINIT_THIS) {
            return false;
        }
        return (type != null ? type.equals(that.type) : that.type == null);
    }

    @Override
    public int hashCode() {
        int result = category;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        return result;
    }
    
    // The Unknown category denotes a register type that hasn't been determined yet
    public static final byte UNKNOWN = 0;
    // The Uninit category is for registers that haven't been set yet. e.g. the non-parameter registers in a method
    // start out as unint
    public static final byte UNINIT = 1;
    public static final byte NULL = 2;
    public static final byte ONE = 3;
    public static final byte BOOLEAN = 4;
    public static final byte BYTE = 5;
    public static final byte POS_BYTE = 6;
    public static final byte SHORT = 7;
    public static final byte POS_SHORT = 8;
    public static final byte CHAR = 9;
    public static final byte INTEGER = 10;
    public static final byte FLOAT = 11;
    public static final byte LONG_LO = 12;
    public static final byte LONG_HI = 13;
    public static final byte DOUBLE_LO = 14;
    public static final byte DOUBLE_HI = 15;
    // The UninitRef category is used after a new-instance operation, and before the corresponding <init> is called
    public static final byte UNINIT_REF = 16;
    // The UninitThis category is used the "this" register inside an <init> method, before the superclass' <init>
    // method is called
    public static final byte UNINIT_THIS = 17;
    public static final byte REFERENCE = 18;
    // This is used when there are multiple incoming execution paths that have incompatible register types. For
    // example if the register's type is an Integer on one incoming code path, but is a Reference type on another
    // incomming code path. There is no register type that can hold either an Integer or a Reference.
    public static final byte CONFLICTED = 19;

    public static final String[] CATEGORY_NAMES = new String[] {
            "Unknown",
            "Uninit",
            "Null",
            "One",
            "Boolean",
            "Byte",
            "PosByte",
            "Short",
            "PosShort",
            "Char",
            "Integer",
            "Float",
            "LongLo",
            "LongHi",
            "DoubleLo",
            "DoubleHi",
            "UninitRef",
            "UninitThis",
            "Reference",
            "Conflicted"
    };

    //this table is used when merging register types. For example, if a particular register can be either a BYTE
    //or a Char, then the "merged" type of that register would be Integer, because it is the "smallest" type can
    //could hold either type of value.
    protected static byte[][] mergeTable  =
    {
            /*              UNKNOWN      UNINIT      NULL        ONE,        BOOLEAN     BYTE        POS_BYTE    SHORT       POS_SHORT   CHAR        INTEGER,    FLOAT,      LONG_LO     LONG_HI     DOUBLE_LO   DOUBLE_HI   UNINIT_REF  UNINIT_THIS REFERENCE   CONFLICTED*/
            /*UNKNOWN*/    {UNKNOWN,     UNINIT,     NULL,       ONE,        BOOLEAN,    BYTE,       POS_BYTE,   SHORT,      POS_SHORT,  CHAR,       INTEGER,    FLOAT,      LONG_LO,    LONG_HI,    DOUBLE_LO,  DOUBLE_HI,  UNINIT_REF, UNINIT_THIS,REFERENCE,  CONFLICTED},
            /*UNINIT*/     {UNINIT,      UNINIT,     CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED},
            /*NULL*/       {NULL,        CONFLICTED, NULL,       BOOLEAN,    BOOLEAN,    BYTE,       POS_BYTE,   SHORT,      POS_SHORT,  CHAR,       INTEGER,    FLOAT,      CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, REFERENCE,  CONFLICTED},
            /*ONE*/        {ONE,         CONFLICTED, BOOLEAN,    ONE,        BOOLEAN,    BYTE,       POS_BYTE,   SHORT,      POS_SHORT,  CHAR,       INTEGER,    FLOAT,      CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED},
            /*BOOLEAN*/    {BOOLEAN,     CONFLICTED, BOOLEAN,    BOOLEAN,    BOOLEAN,    BYTE,       POS_BYTE,   SHORT,      POS_SHORT,  CHAR,       INTEGER,    FLOAT,      CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED},
            /*BYTE*/       {BYTE,        CONFLICTED, BYTE,       BYTE,       BYTE,       BYTE,       BYTE,       SHORT,      SHORT,      INTEGER,    INTEGER,    FLOAT,      CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED},
            /*POS_BYTE*/   {POS_BYTE,    CONFLICTED, POS_BYTE,   POS_BYTE,   POS_BYTE,   BYTE,       POS_BYTE,   SHORT,      POS_SHORT,  CHAR,       INTEGER,    FLOAT,      CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED},
            /*SHORT*/      {SHORT,       CONFLICTED, SHORT,      SHORT,      SHORT,      SHORT,      SHORT,      SHORT,      SHORT,      INTEGER,    INTEGER,    FLOAT,      CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED},
            /*POS_SHORT*/  {POS_SHORT,   CONFLICTED, POS_SHORT,  POS_SHORT,  POS_SHORT,  SHORT,      POS_SHORT,  SHORT,      POS_SHORT,  CHAR,       INTEGER,    FLOAT,      CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED},
            /*CHAR*/       {CHAR,        CONFLICTED, CHAR,       CHAR,       CHAR,       INTEGER,    CHAR,       INTEGER,    CHAR,       CHAR,       INTEGER,    FLOAT,      CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED},
            /*INTEGER*/    {INTEGER,     CONFLICTED, INTEGER,    INTEGER,    INTEGER,    INTEGER,    INTEGER,    INTEGER,    INTEGER,    INTEGER,    INTEGER,    INTEGER,    CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED},
            /*FLOAT*/      {FLOAT,       CONFLICTED, FLOAT,      FLOAT,      FLOAT,      FLOAT,      FLOAT,      FLOAT,      FLOAT,      FLOAT,      INTEGER,    FLOAT,      CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED},
            /*LONG_LO*/    {LONG_LO,     CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, LONG_LO,    CONFLICTED, LONG_LO,    CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED},
            /*LONG_HI*/    {LONG_HI,     CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, LONG_HI,    CONFLICTED, LONG_HI,    CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED},
            /*DOUBLE_LO*/  {DOUBLE_LO,   CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, LONG_LO,    CONFLICTED, DOUBLE_LO,  CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED},
            /*DOUBLE_HI*/  {DOUBLE_HI,   CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, LONG_HI,    CONFLICTED, DOUBLE_HI,  CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED},
            /*UNINIT_REF*/ {UNINIT_REF,  CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED},
            /*UNINIT_THIS*/{UNINIT_THIS, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, UNINIT_THIS,CONFLICTED, CONFLICTED},
            /*REFERENCE*/  {REFERENCE,   CONFLICTED, REFERENCE,  CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, REFERENCE,  CONFLICTED},
            /*CONFLICTED*/ {CONFLICTED,  CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED, CONFLICTED}
    };


    public static final RegisterType UNKNOWN_TYPE = new RegisterType(UNKNOWN, null);
    public static final RegisterType UNINIT_TYPE = new RegisterType(UNINIT, null);
    public static final RegisterType NULL_TYPE = new RegisterType(NULL, null);
    public static final RegisterType ONE_TYPE = new RegisterType(ONE, null);
    public static final RegisterType BOOLEAN_TYPE = new RegisterType(BOOLEAN, null);
    public static final RegisterType BYTE_TYPE = new RegisterType(BYTE, null);
    public static final RegisterType POS_BYTE_TYPE = new RegisterType(POS_BYTE, null);
    public static final RegisterType SHORT_TYPE = new RegisterType(SHORT, null);
    public static final RegisterType POS_SHORT_TYPE = new RegisterType(POS_SHORT, null);
    public static final RegisterType CHAR_TYPE = new RegisterType(CHAR, null);
    public static final RegisterType INTEGER_TYPE = new RegisterType(INTEGER, null);
    public static final RegisterType FLOAT_TYPE = new RegisterType(FLOAT, null);
    public static final RegisterType LONG_LO_TYPE = new RegisterType(LONG_LO, null);
    public static final RegisterType LONG_HI_TYPE = new RegisterType(LONG_HI, null);
    public static final RegisterType DOUBLE_LO_TYPE = new RegisterType(DOUBLE_LO, null);
    public static final RegisterType DOUBLE_HI_TYPE = new RegisterType(DOUBLE_HI, null);
    public static final RegisterType CONFLICTED_TYPE = new RegisterType(CONFLICTED, null);

    @Nonnull
    public static RegisterType getWideRegisterType(@Nonnull CharSequence type, boolean firstRegister) {
        switch (type.charAt(0)) {
            case 'J':
                if (firstRegister) {
                    return getRegisterType(LONG_LO, null);
                } else {
                    return getRegisterType(LONG_HI, null);
                }
            case 'D':
                if (firstRegister) {
                    return getRegisterType(DOUBLE_LO, null);
                } else {
                    return getRegisterType(DOUBLE_HI, null);
                }
            default:
                throw new ExceptionWithContext("Cannot use this method for narrow register type: %s", type);
        }
    }

    @Nonnull
    public static RegisterType getRegisterType(@Nonnull ClassPath classPath, @Nonnull CharSequence type) {
        switch (type.charAt(0)) {
            case 'Z':
                return BOOLEAN_TYPE;
            case 'B':
                return BYTE_TYPE;
            case 'S':
                return SHORT_TYPE;
            case 'C':
                return CHAR_TYPE;
            case 'I':
                return INTEGER_TYPE;
            case 'F':
                return FLOAT_TYPE;
            case 'J':
                return LONG_LO_TYPE;
            case 'D':
                return DOUBLE_LO_TYPE;
            case 'L':
            case '[':
                return getRegisterType(REFERENCE, classPath.getClass(type));
            default:
                throw new ExceptionWithContext("Invalid type: " + type);
        }
    }

    @Nonnull
    public static RegisterType getRegisterTypeForLiteral(int literalValue) {
        if (literalValue < -32768) {
            return INTEGER_TYPE;
        }
        if (literalValue < -128) {
            return SHORT_TYPE;
        }
        if (literalValue < 0) {
            return BYTE_TYPE;
        }
        if (literalValue == 0) {
            return NULL_TYPE;
        }
        if (literalValue == 1) {
            return ONE_TYPE;
        }
        if (literalValue < 128) {
            return POS_BYTE_TYPE;
        }
        if (literalValue < 32768) {
            return POS_SHORT_TYPE;
        }
        if (literalValue < 65536) {
            return CHAR_TYPE;
        }
        return INTEGER_TYPE;
    }

    @Nonnull
    public RegisterType merge(@Nonnull RegisterType other) {
        if (other.equals(this)) {
            return this;
        }

        byte mergedCategory = mergeTable[this.category][other.category];

        TypeProto mergedType = null;
        if (mergedCategory == REFERENCE) {
            TypeProto type = this.type;
            if (type != null) {
                if (other.type != null) {
                    mergedType = type.getCommonSuperclass(other.type);
                } else {
                    mergedType = type;
                }
            } else {
                mergedType = other.type;
            }
        } else if (mergedCategory == UNINIT_REF || mergedCategory == UNINIT_THIS) {
            if (this.category == UNKNOWN) {
                return other;
            }
            assert other.category == UNKNOWN;
            return this;
        }

        if (mergedType != null) {
            if (mergedType.equals(this.type)) {
                return this;
            }
            if (mergedType.equals(other.type)) {
                return other;
            }
        }
        return RegisterType.getRegisterType(mergedCategory, mergedType);
    }

    @Nonnull
    public static RegisterType getRegisterType(byte category, @Nullable TypeProto typeProto) {
        switch (category) {
            case UNKNOWN:
                return UNKNOWN_TYPE;
            case UNINIT:
                return UNINIT_TYPE;
            case NULL:
                return NULL_TYPE;
            case ONE:
                return ONE_TYPE;
            case BOOLEAN:
                return BOOLEAN_TYPE;
            case BYTE:
                return BYTE_TYPE;
            case POS_BYTE:
                return POS_BYTE_TYPE;
            case SHORT:
                return SHORT_TYPE;
            case POS_SHORT:
                return POS_SHORT_TYPE;
            case CHAR:
                return CHAR_TYPE;
            case INTEGER:
                return INTEGER_TYPE;
            case FLOAT:
                return FLOAT_TYPE;
            case LONG_LO:
                return LONG_LO_TYPE;
            case LONG_HI:
                return LONG_HI_TYPE;
            case DOUBLE_LO:
                return DOUBLE_LO_TYPE;
            case DOUBLE_HI:
                return DOUBLE_HI_TYPE;
            case CONFLICTED:
                return CONFLICTED_TYPE;
        }

        return new RegisterType(category, typeProto);
    }
}
