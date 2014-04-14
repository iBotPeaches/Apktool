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

import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.OneRegisterInstruction;
import org.jf.dexlib2.iface.instruction.WideLiteralInstruction;

import java.util.List;

public class SyntheticAccessorFSM {
    %% machine SyntheticAccessorFSM;
    %% write data;

    // math type constants
    public static final int ADD = SyntheticAccessorResolver.ADD_ASSIGNMENT;
    public static final int SUB = SyntheticAccessorResolver.SUB_ASSIGNMENT;
    public static final int MUL = SyntheticAccessorResolver.MUL_ASSIGNMENT;
    public static final int DIV = SyntheticAccessorResolver.DIV_ASSIGNMENT;
    public static final int REM = SyntheticAccessorResolver.REM_ASSIGNMENT;
    public static final int AND = SyntheticAccessorResolver.AND_ASSIGNMENT;
    public static final int OR = SyntheticAccessorResolver.OR_ASSIGNMENT;
    public static final int XOR = SyntheticAccessorResolver.XOR_ASSIGNMENT;
    public static final int SHL = SyntheticAccessorResolver.SHL_ASSIGNMENT;
    public static final int SHR = SyntheticAccessorResolver.SHR_ASSIGNMENT;
    public static final int USHR = SyntheticAccessorResolver.USHR_ASSIGNMENT;

    public static final int INT = 0;
    public static final int LONG = 1;
    public static final int FLOAT = 2;
    public static final int DOUBLE = 3;

    public static final int POSITIVE_ONE = 1;
    public static final int NEGATIVE_ONE = -1;
    public static final int OTHER = 0;

    public static int test(List<? extends Instruction> instructions) {
        int accessorType = -1;
        int cs, p = 0;
        int pe = instructions.size();

        // one of the math type constants representing the type of math operation being performed
        int mathOp = -1;

        // for increments an decrements, the type of value the math operation is on
        int mathType = -1;

        // for increments and decrements, the value of the constant that is used
        long constantValue = 0;

        // The source register for the put instruction
        int putRegister = -1;
        // The return register;
        int returnRegister = -1;

        %%{
            import "Opcodes.rl";
            alphtype short;
            getkey instructions.get(p).getOpcode().value;

            get = (0x52 .. 0x58) | (0x60 .. 0x66); # all igets/sgets

            # all iputs/sputs
            put = ((0x59 .. 0x5f) | (0x67 .. 0x6d)) @ {
                putRegister = ((OneRegisterInstruction)instructions.get(p)).getRegisterA();
            };

            invoke = (0x6e .. 0x72) | (0x74 .. 0x78); # all invokes

            # all numeric const instructions
            const_literal = (0x12 .. 0x19) @ {
                constantValue = ((WideLiteralInstruction)instructions.get(p)).getWideLiteral();
            };

            add_const = (add_int_lit8 | add_int_lit16) @ {
                mathType = INT;
                mathOp = ADD;
                constantValue = ((WideLiteralInstruction)instructions.get(p)).getWideLiteral();
            };

            arbitrary_add = (((add_int | add_int_2addr) @ { mathType = INT; }) |
                             ((add_long | add_long_2addr) @ { mathType = LONG; }) |
                             ((add_float | add_float_2addr) @ { mathType = FLOAT; }) |
                             ((add_double | add_double_2addr) @ {mathType = DOUBLE; })) @ {
                mathOp = ADD;
            };
            arbitrary_sub = (((sub_int | sub_int_2addr) @ { mathType = INT; }) |
                             ((sub_long | sub_long_2addr) @ { mathType = LONG; }) |
                             ((sub_float | sub_float_2addr) @ { mathType = FLOAT; }) |
                             ((sub_double | sub_double_2addr) @ {mathType = DOUBLE; })) @ {
                mathOp = SUB;
            };
            arbitrary_mul = (mul_int | mul_int_2addr | mul_long | mul_long_2addr |
                            mul_float | mul_float_2addr | mul_double | mul_double_2addr) @ {
                mathOp = MUL;
            };
            arbitrary_div = (div_int | div_int_2addr | div_long | div_long_2addr |
                            div_float | div_float_2addr | div_double | div_double_2addr) @ {
                mathOp = DIV;
            };
            arbitrary_rem = (rem_int | rem_int_2addr | rem_long | rem_long_2addr |
                            rem_float | rem_float_2addr | rem_double | rem_double_2addr) @ {
                mathOp = REM;
            };
            arbitrary_and = (and_int | and_int_2addr | and_long | and_long_2addr) @ {
                mathOp = AND;
            };
            arbitrary_or = (or_int | or_int_2addr | or_long | or_long_2addr) @ {
                mathOp = OR;
            };
            arbitrary_xor = (xor_int | xor_int_2addr | xor_long | xor_long_2addr) @ {
                mathOp = XOR;
            };
            arbitrary_shl = (shl_int | shl_int_2addr | shl_long | shl_long_2addr) @ {
                mathOp = SHL;
            };
            arbitrary_shr = (shr_int | shr_int_2addr | shr_long | shr_long_2addr) @ {
                mathOp = SHR;
            };
            arbitrary_ushr = (ushr_int | ushr_int_2addr | ushr_long | ushr_long_2addr) @ {
                mathOp = USHR;
            };

            type_conversion = 0x81 .. 0x8f; # all type-conversion opcodes

            return_something = (return | return_wide | return_object) @ {
                returnRegister = ((OneRegisterInstruction)instructions.get(p)).getRegisterA();
            };

            any_move_result = move_result | move_result_wide | move_result_object;

            get_accessor = get return_something @ {
                accessorType = SyntheticAccessorResolver.GETTER; fbreak;
            };

            put_accessor = put return_something @ {
                accessorType = SyntheticAccessorResolver.SETTER; fbreak;
            };

            invoke_accessor = invoke (return_void | (any_move_result return_something)) @ {
                accessorType = SyntheticAccessorResolver.METHOD; fbreak;
            };

            increment_accessor = get add_const type_conversion? put return_something @ {
                accessorType = getIncrementType(mathOp, mathType, constantValue, putRegister, returnRegister);
            };

            alt_increment_accessor = get const_literal (arbitrary_add | arbitrary_sub) put return_something @ {
                accessorType = getIncrementType(mathOp, mathType, constantValue, putRegister, returnRegister);
            };

            math_assignment_accessor = get type_conversion?
                                       (arbitrary_add | arbitrary_sub | arbitrary_mul | arbitrary_div | arbitrary_rem |
                                        arbitrary_and | arbitrary_or | arbitrary_xor | arbitrary_shl | arbitrary_shr |
                                        arbitrary_ushr)
                                        type_conversion{0,2} put return_something @ {
                accessorType = mathOp; fbreak;
            };

            main := get_accessor |
                    put_accessor |
                    invoke_accessor |
                    increment_accessor |
                    alt_increment_accessor |
                    math_assignment_accessor;

            write init;
            write exec;
        }%%

        return accessorType;
    }

    private static int getIncrementType(int mathOp, int mathType, long constantValue, int putRegister,
            int returnRegister) {
        boolean isPrefix = putRegister == returnRegister;

        boolean negativeConstant = false;

        switch (mathType) {
            case INT:
            case LONG: {
                if (constantValue == 1) {
                    negativeConstant = false;
                } else if (constantValue == -1) {
                    negativeConstant = true;
                } else {
                    return -1;
                }
                break;
            }
            case FLOAT: {
                float val = Float.intBitsToFloat((int)constantValue);
                if (val == 1) {
                    negativeConstant = false;
                } else if (val == -1) {
                    negativeConstant = true;
                } else {
                    return -1;
                }
                break;
            }
            case DOUBLE: {
                double val = Double.longBitsToDouble(constantValue);
                if (val == 1) {
                    negativeConstant = false;
                } else if (val == -1) {
                    negativeConstant = true;
                } else {
                    return -1;
                }
                break;
            }
        }

        boolean isAdd = ((mathOp == ADD) && !negativeConstant) ||
                        ((mathOp == SUB) && negativeConstant);

        if (isPrefix) {
            if (isAdd) {
                return SyntheticAccessorResolver.PREFIX_INCREMENT;
            } else {
                return SyntheticAccessorResolver.PREFIX_DECREMENT;
            }
        } else {
            if (isAdd) {
                return SyntheticAccessorResolver.POSTFIX_INCREMENT;
            } else {
                return SyntheticAccessorResolver.POSTFIX_DECREMENT;
            }
        }
    }
}