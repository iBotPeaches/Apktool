/*
 * [The "BSD licence"]
 * Copyright (c) 2010 Ben Gruver
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

package org.jf.dexlib.Code.Analysis;

import org.jf.dexlib.Code.Opcode;

public class OdexedFieldInstructionMapper {
    private static Opcode[][][][] opcodeMap = new Opcode[][][][] {
            //get opcodes
            new Opcode[][][] {
                    //iget quick
                    new Opcode[][] {
                            //odexed
                            new Opcode[] {
                                /*Z*/   Opcode.IGET_QUICK,
                                /*B*/   Opcode.IGET_QUICK,
                                /*S*/   Opcode.IGET_QUICK,
                                /*C*/   Opcode.IGET_QUICK,
                                /*I,F*/ Opcode.IGET_QUICK,
                                /*J,D*/ Opcode.IGET_WIDE_QUICK,
                                /*L,[*/ Opcode.IGET_OBJECT_QUICK
                            },
                            //deodexed
                            new Opcode[] {
                                /*Z*/   Opcode.IGET_BOOLEAN,
                                /*B*/   Opcode.IGET_BYTE,
                                /*S*/   Opcode.IGET_SHORT,
                                /*C*/   Opcode.IGET_CHAR,
                                /*I,F*/ Opcode.IGET,
                                /*J,D*/ Opcode.IGET_WIDE,
                                /*L,[*/ Opcode.IGET_OBJECT
                            }
                    },
                    //iget volatile
                    new Opcode[][] {
                            //odexed
                            new Opcode[] {
                                /*Z*/   Opcode.IGET_VOLATILE,
                                /*B*/   Opcode.IGET_VOLATILE,
                                /*S*/   Opcode.IGET_VOLATILE,
                                /*C*/   Opcode.IGET_VOLATILE,
                                /*I,F*/ Opcode.IGET_VOLATILE,
                                /*J,D*/ Opcode.IGET_WIDE_VOLATILE,
                                /*L,[*/ Opcode.IGET_OBJECT_VOLATILE
                            },
                            //deodexed
                            new Opcode[] {
                                /*Z*/   Opcode.IGET_BOOLEAN,
                                /*B*/   Opcode.IGET_BYTE,
                                /*S*/   Opcode.IGET_SHORT,
                                /*C*/   Opcode.IGET_CHAR,
                                /*I,F*/ Opcode.IGET,
                                /*J,D*/ Opcode.IGET_WIDE,
                                /*L,[*/ Opcode.IGET_OBJECT
                            }
                    },
                    //sget volatile
                    new Opcode[][] {
                            //odexed
                            new Opcode[] {
                                /*Z*/   Opcode.SGET_VOLATILE,
                                /*B*/   Opcode.SGET_VOLATILE,
                                /*S*/   Opcode.SGET_VOLATILE,
                                /*C*/   Opcode.SGET_VOLATILE,
                                /*I,F*/ Opcode.SGET_VOLATILE,
                                /*J,D*/ Opcode.SGET_WIDE_VOLATILE,
                                /*L,[*/ Opcode.SGET_OBJECT_VOLATILE
                            },
                            //deodexed
                            new Opcode[] {
                                /*Z*/   Opcode.SGET_BOOLEAN,
                                /*B*/   Opcode.SGET_BYTE,
                                /*S*/   Opcode.SGET_SHORT,
                                /*C*/   Opcode.SGET_CHAR,
                                /*I,F*/ Opcode.SGET,
                                /*J,D*/ Opcode.SGET_WIDE,
                                /*L,[*/ Opcode.SGET_OBJECT
                            }
                    }
            },
            //put opcodes
            new Opcode[][][] {
                    //iput quick
                    new Opcode[][] {
                            //odexed
                            new Opcode[] {
                                /*Z*/   Opcode.IPUT_QUICK,
                                /*B*/   Opcode.IPUT_QUICK,
                                /*S*/   Opcode.IPUT_QUICK,
                                /*C*/   Opcode.IPUT_QUICK,
                                /*I,F*/ Opcode.IPUT_QUICK,
                                /*J,D*/ Opcode.IPUT_WIDE_QUICK,
                                /*L,[*/ Opcode.IPUT_OBJECT_QUICK
                            },
                            //deodexed
                            new Opcode[] {
                                /*Z*/   Opcode.IPUT_BOOLEAN,
                                /*B*/   Opcode.IPUT_BYTE,
                                /*S*/   Opcode.IPUT_SHORT,
                                /*C*/   Opcode.IPUT_CHAR,
                                /*I,F*/ Opcode.IPUT,
                                /*J,D*/ Opcode.IPUT_WIDE,
                                /*L,[*/ Opcode.IPUT_OBJECT
                            }
                    },
                    //iput volatile
                    new Opcode[][] {
                            //odexed
                            new Opcode[] {
                                /*Z*/   Opcode.IPUT_VOLATILE,
                                /*B*/   Opcode.IPUT_VOLATILE,
                                /*S*/   Opcode.IPUT_VOLATILE,
                                /*C*/   Opcode.IPUT_VOLATILE,
                                /*I,F*/ Opcode.IPUT_VOLATILE,
                                /*J,D*/ Opcode.IPUT_WIDE_VOLATILE,
                                /*L,[*/ Opcode.IPUT_OBJECT_VOLATILE
                            },
                            //deodexed
                            new Opcode[] {
                                /*Z*/   Opcode.IPUT_BOOLEAN,
                                /*B*/   Opcode.IPUT_BYTE,
                                /*S*/   Opcode.IPUT_SHORT,
                                /*C*/   Opcode.IPUT_CHAR,
                                /*I,F*/ Opcode.IPUT,
                                /*J,D*/ Opcode.IPUT_WIDE,
                                /*L,[*/ Opcode.IPUT_OBJECT
                            }
                    },
                    //sput volatile
                    new Opcode[][] {
                            //odexed
                            new Opcode[] {
                                /*Z*/   Opcode.SPUT_VOLATILE,
                                /*B*/   Opcode.SPUT_VOLATILE,
                                /*S*/   Opcode.SPUT_VOLATILE,
                                /*C*/   Opcode.SPUT_VOLATILE,
                                /*I,F*/ Opcode.SPUT_VOLATILE,
                                /*J,D*/ Opcode.SPUT_WIDE_VOLATILE,
                                /*L,[*/ Opcode.SPUT_OBJECT_VOLATILE
                            },
                            //deodexed
                            new Opcode[] {
                                /*Z*/   Opcode.SPUT_BOOLEAN,
                                /*B*/   Opcode.SPUT_BYTE,
                                /*S*/   Opcode.SPUT_SHORT,
                                /*C*/   Opcode.SPUT_CHAR,
                                /*I,F*/ Opcode.SPUT,
                                /*J,D*/ Opcode.SPUT_WIDE,
                                /*L,[*/ Opcode.SPUT_OBJECT
                            }
                    }
            }
    };

    private static Opcode[][][][] jumboOpcodeMap = new Opcode[][][][] {
            //get opcodes
            new Opcode[][][] {
                    //iget volatile
                    new Opcode[][] {
                            //odexed
                            new Opcode[] {
                                /*Z*/   Opcode.IGET_VOLATILE_JUMBO,
                                /*B*/   Opcode.IGET_VOLATILE_JUMBO,
                                /*S*/   Opcode.IGET_VOLATILE_JUMBO,
                                /*C*/   Opcode.IGET_VOLATILE_JUMBO,
                                /*I,F*/ Opcode.IGET_VOLATILE_JUMBO,
                                /*J,D*/ Opcode.IGET_WIDE_VOLATILE_JUMBO,
                                /*L,[*/ Opcode.IGET_OBJECT_VOLATILE_JUMBO
                            },
                            //deodexed
                            new Opcode[] {
                                /*Z*/   Opcode.IGET_BOOLEAN_JUMBO,
                                /*B*/   Opcode.IGET_BYTE_JUMBO,
                                /*S*/   Opcode.IGET_SHORT_JUMBO,
                                /*C*/   Opcode.IGET_CHAR_JUMBO,
                                /*I,F*/ Opcode.IGET_JUMBO,
                                /*J,D*/ Opcode.IGET_WIDE_JUMBO,
                                /*L,[*/ Opcode.IGET_OBJECT_JUMBO
                            }
                    },
                    //sget volatile
                    new Opcode[][] {
                            //odexed
                            new Opcode[] {
                                /*Z*/   Opcode.SGET_VOLATILE_JUMBO,
                                /*B*/   Opcode.SGET_VOLATILE_JUMBO,
                                /*S*/   Opcode.SGET_VOLATILE_JUMBO,
                                /*C*/   Opcode.SGET_VOLATILE_JUMBO,
                                /*I,F*/ Opcode.SGET_VOLATILE_JUMBO,
                                /*J,D*/ Opcode.SGET_WIDE_VOLATILE_JUMBO,
                                /*L,[*/ Opcode.SGET_OBJECT_VOLATILE_JUMBO
                            },
                            //deodexed
                            new Opcode[] {
                                /*Z*/   Opcode.SGET_BOOLEAN_JUMBO,
                                /*B*/   Opcode.SGET_BYTE_JUMBO,
                                /*S*/   Opcode.SGET_SHORT_JUMBO,
                                /*C*/   Opcode.SGET_CHAR_JUMBO,
                                /*I,F*/ Opcode.SGET_JUMBO,
                                /*J,D*/ Opcode.SGET_WIDE_JUMBO,
                                /*L,[*/ Opcode.SGET_OBJECT_JUMBO
                            }
                    }
            },
            //put opcodes
            new Opcode[][][] {
                    //iput volatile
                    new Opcode[][] {
                            //odexed
                            new Opcode[] {
                                /*Z*/   Opcode.IPUT_VOLATILE_JUMBO,
                                /*B*/   Opcode.IPUT_VOLATILE_JUMBO,
                                /*S*/   Opcode.IPUT_VOLATILE_JUMBO,
                                /*C*/   Opcode.IPUT_VOLATILE_JUMBO,
                                /*I,F*/ Opcode.IPUT_VOLATILE_JUMBO,
                                /*J,D*/ Opcode.IPUT_WIDE_VOLATILE_JUMBO,
                                /*L,[*/ Opcode.IPUT_OBJECT_VOLATILE_JUMBO
                            },
                            //deodexed
                            new Opcode[] {
                                /*Z*/   Opcode.IPUT_BOOLEAN_JUMBO,
                                /*B*/   Opcode.IPUT_BYTE_JUMBO,
                                /*S*/   Opcode.IPUT_SHORT_JUMBO,
                                /*C*/   Opcode.IPUT_CHAR_JUMBO,
                                /*I,F*/ Opcode.IPUT_JUMBO,
                                /*J,D*/ Opcode.IPUT_WIDE_JUMBO,
                                /*L,[*/ Opcode.IPUT_OBJECT_JUMBO
                            }
                    },
                    //sput volatile
                    new Opcode[][] {
                            //odexed
                            new Opcode[] {
                                /*Z*/   Opcode.SPUT_VOLATILE_JUMBO,
                                /*B*/   Opcode.SPUT_VOLATILE_JUMBO,
                                /*S*/   Opcode.SPUT_VOLATILE_JUMBO,
                                /*C*/   Opcode.SPUT_VOLATILE_JUMBO,
                                /*I,F*/ Opcode.SPUT_VOLATILE_JUMBO,
                                /*J,D*/ Opcode.SPUT_WIDE_VOLATILE_JUMBO,
                                /*L,[*/ Opcode.SPUT_OBJECT_VOLATILE_JUMBO
                            },
                            //deodexed
                            new Opcode[] {
                                /*Z*/   Opcode.SPUT_BOOLEAN_JUMBO,
                                /*B*/   Opcode.SPUT_BYTE_JUMBO,
                                /*S*/   Opcode.SPUT_SHORT_JUMBO,
                                /*C*/   Opcode.SPUT_CHAR_JUMBO,
                                /*I,F*/ Opcode.SPUT_JUMBO,
                                /*J,D*/ Opcode.SPUT_WIDE_JUMBO,
                                /*L,[*/ Opcode.SPUT_OBJECT_JUMBO
                            }
                    }
            }
    };

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
            case 'F':
                return 4;
            case 'J':
            case 'D':
                return 5;
            case 'L':
            case '[':
                return 6;
            default:
        }
        throw new RuntimeException(String.format("Unknown type %s: ", type));
    }

    private static int getOpcodeSubtype(Opcode opcode) {
        if (opcode.isOdexedInstanceQuick()) {
            return 0;
        } else if (opcode.isOdexedInstanceVolatile()) {
            return 1;
        } else if (opcode.isOdexedStaticVolatile()) {
            return 2;
        }
        throw new RuntimeException(String.format("Not an odexed field access opcode: %s", opcode.name));
    }

    static Opcode getAndCheckDeodexedOpcodeForOdexedOpcode(String fieldType, Opcode odexedOpcode) {
        boolean jumbo = odexedOpcode.isJumboOpcode();
        int opcodeType = odexedOpcode.setsRegister()?0:1;
        int opcodeSubType = getOpcodeSubtype(odexedOpcode);
        int typeIndex = getTypeIndex(fieldType.charAt(0));

        Opcode correctOdexedOpcode, deodexedOpcode;

        if (jumbo) {
            correctOdexedOpcode = jumboOpcodeMap[opcodeType][opcodeSubType-1][0][typeIndex];
            deodexedOpcode = jumboOpcodeMap[opcodeType][opcodeSubType-1][1][typeIndex];
        } else {
            correctOdexedOpcode = opcodeMap[opcodeType][opcodeSubType][0][typeIndex];
            deodexedOpcode = opcodeMap[opcodeType][opcodeSubType][1][typeIndex];
        }

        if (correctOdexedOpcode != odexedOpcode) {
            throw new ValidationException(String.format("Incorrect field type \"%s\" for %s", fieldType,
                    odexedOpcode.name));
        }

        return deodexedOpcode;
    }
}


