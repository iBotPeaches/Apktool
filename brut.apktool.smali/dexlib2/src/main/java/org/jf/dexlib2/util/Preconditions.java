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

import org.jf.dexlib2.Format;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.ReferenceType;
import org.jf.dexlib2.VerificationError;
import org.jf.dexlib2.iface.reference.*;

public class Preconditions {
    public static void checkFormat(Opcode opcode, Format expectedFormat) {
        if (opcode.format != expectedFormat) {
            throw new IllegalArgumentException(
                    String.format("Invalid opcode %s for %s", opcode.name, expectedFormat.name()));
        }
    }

    public static int checkNibbleRegister(int register) {
        if ((register & 0xFFFFFFF0) != 0) {
            throw new IllegalArgumentException(
                    String.format("Invalid register: v%d. Must be between v0 and v15, inclusive.", register));
        }
        return register;
    }

    public static int checkByteRegister(int register) {
        if ((register & 0xFFFFFF00) != 0) {
            throw new IllegalArgumentException(
                    String.format("Invalid register: v%d. Must be between v0 and v255, inclusive.", register));
        }
        return register;
    }

    public static int checkShortRegister(int register) {
        if ((register & 0xFFFF0000) != 0) {
            throw new IllegalArgumentException(
                    String.format("Invalid register: v%d. Must be between v0 and v65535, inclusive.", register));
        }
        return register;
    }

    public static int checkNibbleLiteral(int literal) {
        if (literal < -8 || literal > 7) {
            throw new IllegalArgumentException(
                    String.format("Invalid literal value: %d. Must be between -8 and 7, inclusive.", literal));
        }
        return literal;
    }

    public static int checkByteLiteral(int literal) {
        if (literal < -128 || literal > 127) {
            throw new IllegalArgumentException(
                    String.format("Invalid literal value: %d. Must be between -128 and 127, inclusive.", literal));
        }
        return literal;
    }

    public static int checkShortLiteral(int literal) {
        if (literal < -32768 || literal > 32767) {
            throw new IllegalArgumentException(
                    String.format("Invalid literal value: %d. Must be between -32768 and 32767, inclusive.", literal));
        }
        return literal;
    }

    public static int checkIntegerHatLiteral(int literal) {
        if ((literal & 0xFFFF) != 0) {
            throw new IllegalArgumentException(
                    String.format("Invalid literal value: %d. Low 16 bits must be zeroed out.", literal));
        }
        return literal;
    }

    public static long checkLongHatLiteral(long literal) {
        if ((literal & 0xFFFFFFFFFFFFL) != 0) {
            throw new IllegalArgumentException(
                    String.format("Invalid literal value: %d. Low 48 bits must be zeroed out.", literal));
        }
        return literal;
    }

    public static int checkByteCodeOffset(int offset) {
        if (offset < -128 || offset > 127) {
            throw new IllegalArgumentException(
                    String.format("Invalid code offset: %d. Must be between -128 and 127, inclusive.", offset));
        }
        return offset;
    }

    public static int checkShortCodeOffset(int offset) {
        if (offset < -32768 || offset > 32767) {
            throw new IllegalArgumentException(
                    String.format("Invalid code offset: %d. Must be between -32768 and 32767, inclusive.", offset));
        }
        return offset;
    }

    public static int check35cRegisterCount(int registerCount) {
        if (registerCount < 0 || registerCount > 5) {
            throw new IllegalArgumentException(
                    String.format("Invalid register count: %d. Must be between 0 and 5, inclusive.", registerCount));
        }
        return registerCount;
    }

    public static int check25xParameterRegisterCount(int registerCount) {
        if (registerCount < 0 || registerCount > 4) {
            throw new IllegalArgumentException(
                    String.format("Invalid parameter register count: %d. " +
                            "Must be between 0 and 4, inclusive.", registerCount));
        }
        return registerCount;
    }

    public static int checkRegisterRangeCount(int registerCount) {
        if ((registerCount & 0xFFFFFF00) != 0) {
            throw new IllegalArgumentException(
                    String.format("Invalid register count: %d. Must be between 0 and 255, inclusive.", registerCount));
        }
        return registerCount;
    }

    public static void checkValueArg(int valueArg, int maxValue) {
        if (valueArg > maxValue) {
            if (maxValue == 0) {
                throw new IllegalArgumentException(
                        String.format("Invalid value_arg value %d for an encoded_value. Expecting 0",
                                valueArg));
            }
            throw new IllegalArgumentException(
                    String.format("Invalid value_arg value %d for an encoded_value. Expecting 0..%d, inclusive",
                            valueArg, maxValue));
        }
    }

    public static int checkFieldOffset(int fieldOffset) {
        if (fieldOffset < 0 || fieldOffset > 65535) {
            throw new IllegalArgumentException(
                    String.format("Invalid field offset: 0x%x. Must be between 0x0000 and 0xFFFF inclusive",
                            fieldOffset));
        }
        return fieldOffset;
    }

    public static int checkVtableIndex(int vtableIndex) {
        if (vtableIndex < 0 || vtableIndex > 65535) {
            throw new IllegalArgumentException(
                    String.format("Invalid vtable index: %d. Must be between 0 and 65535, inclusive", vtableIndex));
        }
        return vtableIndex;
    }

    public static int checkInlineIndex(int inlineIndex) {
        if (inlineIndex < 0 || inlineIndex > 65535) {
            throw new IllegalArgumentException(
                    String.format("Invalid inline index: %d. Must be between 0 and 65535, inclusive", inlineIndex));
        }
        return inlineIndex;
    }

    public static int checkVerificationError(int verificationError) {
        if (!VerificationError.isValidVerificationError(verificationError)) {
            throw new IllegalArgumentException(
                    String.format("Invalid verification error value: %d. Must be between 1 and 9, inclusive",
                            verificationError));
        }
        return verificationError;
    }

    public static <T extends Reference> T checkReference(int referenceType, T reference) {
        switch (referenceType) {
            case ReferenceType.STRING:
                if (!(reference instanceof StringReference)) {
                    throw new IllegalArgumentException("Invalid reference type, expecting a string reference");
                }
                break;
            case ReferenceType.TYPE:
                if (!(reference instanceof TypeReference)) {
                    throw new IllegalArgumentException("Invalid reference type, expecting a type reference");
                }
                break;
            case ReferenceType.FIELD:
                if (!(reference instanceof FieldReference)) {
                    throw new IllegalArgumentException("Invalid reference type, expecting a field reference");
                }
                break;
            case ReferenceType.METHOD:
                if (!(reference instanceof MethodReference)) {
                    throw new IllegalArgumentException("Invalid reference type, expecting a method reference");
                }
                break;
            default:
                throw new IllegalArgumentException(String.format("Not a valid reference type: %d", referenceType));
        }
        return reference;
    }
}
