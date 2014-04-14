
// line 1 "SyntheticAccessorFSM.rl"
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
    
// line 42 "SyntheticAccessorFSM.rl"
    
// line 47 "/home/jesusfreke/projects/smali/dexlib2/src/main/java/org/jf/dexlib2/util/SyntheticAccessorFSM.java"
private static byte[] init__SyntheticAccessorFSM_actions_0()
{
	return new byte [] {
	    0,    1,    0,    1,    1,    1,    2,    1,   13,    1,   14,    1,
	   15,    1,   16,    1,   17,    1,   18,    1,   19,    1,   20,    1,
	   21,    1,   25,    2,    3,    7,    2,    4,    7,    2,    5,    7,
	    2,    6,    7,    2,    8,   12,    2,    9,   12,    2,   10,   12,
	    2,   11,   12,    2,   22,   23,    2,   22,   24,    2,   22,   25,
	    2,   22,   26,    2,   22,   27,    2,   22,   28
	};
}

private static final byte _SyntheticAccessorFSM_actions[] = init__SyntheticAccessorFSM_actions_0();


private static short[] init__SyntheticAccessorFSM_key_offsets_0()
{
	return new short [] {
	    0,    0,   12,   82,   98,  102,  104,  166,  172,  174,  180,  184,
	  190,  192,  196,  198,  201,  203
	};
}

private static final short _SyntheticAccessorFSM_key_offsets[] = init__SyntheticAccessorFSM_key_offsets_0();


private static short[] init__SyntheticAccessorFSM_trans_keys_0()
{
	return new short [] {
	   82,   88,   89,   95,   96,  102,  103,  109,  110,  114,  116,  120,
	  145,  146,  147,  148,  149,  150,  151,  152,  153,  154,  155,  156,
	  157,  158,  159,  160,  161,  162,  163,  164,  165,  166,  167,  168,
	  169,  170,  171,  172,  173,  174,  175,  177,  179,  180,  181,  182,
	  183,  184,  185,  186,  187,  188,  190,  191,  192,  193,  194,  195,
	  196,  197,  198,  199,  201,  202,  203,  204,  206,  207,  208,  216,
	   15,   17,   18,   25,  129,  143,  144,  176,  178,  205,  144,  145,
	  155,  156,  166,  167,  171,  172,  176,  177,  187,  188,  198,  199,
	  203,  204,   89,   95,  103,  109,   15,   17,  145,  146,  147,  148,
	  149,  150,  151,  152,  153,  154,  155,  156,  157,  158,  159,  160,
	  161,  162,  163,  164,  165,  166,  167,  168,  169,  170,  171,  172,
	  173,  174,  175,  177,  179,  180,  181,  182,  183,  184,  185,  186,
	  187,  188,  190,  191,  192,  193,  194,  195,  196,  197,  198,  199,
	  201,  202,  203,  204,  206,  207,  144,  176,  178,  205,   89,   95,
	  103,  109,  129,  143,   15,   17,   89,   95,  103,  109,  129,  143,
	   89,   95,  103,  109,   89,   95,  103,  109,  129,  143,   15,   17,
	   89,   95,  103,  109,   15,   17,   14,   10,   12,   15,   17,    0
	};
}

private static final short _SyntheticAccessorFSM_trans_keys[] = init__SyntheticAccessorFSM_trans_keys_0();


private static byte[] init__SyntheticAccessorFSM_single_lengths_0()
{
	return new byte [] {
	    0,    0,   60,   16,    0,    0,   58,    0,    0,    0,    0,    0,
	    0,    0,    0,    1,    0,    0
	};
}

private static final byte _SyntheticAccessorFSM_single_lengths[] = init__SyntheticAccessorFSM_single_lengths_0();


private static byte[] init__SyntheticAccessorFSM_range_lengths_0()
{
	return new byte [] {
	    0,    6,    5,    0,    2,    1,    2,    3,    1,    3,    2,    3,
	    1,    2,    1,    1,    1,    0
	};
}

private static final byte _SyntheticAccessorFSM_range_lengths[] = init__SyntheticAccessorFSM_range_lengths_0();


private static short[] init__SyntheticAccessorFSM_index_offsets_0()
{
	return new short [] {
	    0,    0,    7,   73,   90,   93,   95,  156,  160,  162,  166,  169,
	  173,  175,  178,  180,  183,  185
	};
}

private static final short _SyntheticAccessorFSM_index_offsets[] = init__SyntheticAccessorFSM_index_offsets_0();


private static byte[] init__SyntheticAccessorFSM_indicies_0()
{
	return new byte [] {
	    0,    2,    0,    2,    3,    3,    1,    8,    9,   10,   11,   12,
	   13,   14,   15,   16,   17,   18,   19,    9,   10,   11,   12,   13,
	   14,   15,   16,   17,   20,   21,    9,   10,   11,   22,   23,    9,
	   10,   11,    8,   10,   11,   12,   13,   14,   15,   16,   17,   18,
	   19,   10,   11,   12,   13,   14,   15,   16,   17,   20,   21,   10,
	   11,   22,   23,   10,   11,   24,   24,    4,    5,    6,    7,    9,
	    1,   25,   26,   27,   28,   29,   30,   31,   32,   25,   26,   27,
	   28,   29,   30,   31,   32,    1,   33,   33,    1,   34,    1,    8,
	    9,   10,   11,   12,   13,   14,   15,   16,   17,   18,   19,    9,
	   10,   11,   12,   13,   14,   15,   16,   17,   20,   21,    9,   10,
	   11,   22,   23,    9,   10,   11,    8,   10,   11,   12,   13,   14,
	   15,   16,   17,   18,   19,   10,   11,   12,   13,   14,   15,   16,
	   17,   20,   21,   10,   11,   22,   23,   10,   11,    7,    9,    1,
	   35,   35,   36,    1,   37,    1,   35,   35,   38,    1,   35,   35,
	    1,   39,   39,   40,    1,   41,    1,   39,   39,    1,   42,    1,
	   44,   43,    1,   45,    1,    1,    0
	};
}

private static final byte _SyntheticAccessorFSM_indicies[] = init__SyntheticAccessorFSM_indicies_0();


private static byte[] init__SyntheticAccessorFSM_trans_targs_0()
{
	return new byte [] {
	    2,    0,   14,   15,   17,    3,    6,    7,    7,    7,    7,    7,
	    7,    7,    7,    7,    7,    7,    7,    7,    7,    7,    7,    7,
	   11,    4,    4,    4,    4,    4,    4,    4,    4,    5,   17,    8,
	    9,   17,   10,   12,   13,   17,   17,   16,   17,   17
	};
}

private static final byte _SyntheticAccessorFSM_trans_targs[] = init__SyntheticAccessorFSM_trans_targs_0();


private static byte[] init__SyntheticAccessorFSM_trans_actions_0()
{
	return new byte [] {
	    0,    0,    1,    0,   51,    3,    0,   27,   39,    7,    9,   11,
	   13,   15,   17,   19,   21,   23,   30,   42,   33,   45,   36,   48,
	    5,   27,   39,   30,   42,   33,   45,   36,   48,    1,   63,    1,
	    0,   66,    0,    1,    0,   60,   54,    0,   25,   57
	};
}

private static final byte _SyntheticAccessorFSM_trans_actions[] = init__SyntheticAccessorFSM_trans_actions_0();


static final int SyntheticAccessorFSM_start = 1;
static final int SyntheticAccessorFSM_first_final = 17;
static final int SyntheticAccessorFSM_error = 0;

static final int SyntheticAccessorFSM_en_main = 1;


// line 43 "SyntheticAccessorFSM.rl"

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

        
// line 235 "/home/jesusfreke/projects/smali/dexlib2/src/main/java/org/jf/dexlib2/util/SyntheticAccessorFSM.java"
	{
	cs = SyntheticAccessorFSM_start;
	}

// line 240 "/home/jesusfreke/projects/smali/dexlib2/src/main/java/org/jf/dexlib2/util/SyntheticAccessorFSM.java"
	{
	int _klen;
	int _trans = 0;
	int _acts;
	int _nacts;
	int _keys;
	int _goto_targ = 0;

	_goto: while (true) {
	switch ( _goto_targ ) {
	case 0:
	if ( p == pe ) {
		_goto_targ = 4;
		continue _goto;
	}
	if ( cs == 0 ) {
		_goto_targ = 5;
		continue _goto;
	}
case 1:
	_match: do {
	_keys = _SyntheticAccessorFSM_key_offsets[cs];
	_trans = _SyntheticAccessorFSM_index_offsets[cs];
	_klen = _SyntheticAccessorFSM_single_lengths[cs];
	if ( _klen > 0 ) {
		int _lower = _keys;
		int _mid;
		int _upper = _keys + _klen - 1;
		while (true) {
			if ( _upper < _lower )
				break;

			_mid = _lower + ((_upper-_lower) >> 1);
			if ( ( instructions.get(p).getOpcode().value) < _SyntheticAccessorFSM_trans_keys[_mid] )
				_upper = _mid - 1;
			else if ( ( instructions.get(p).getOpcode().value) > _SyntheticAccessorFSM_trans_keys[_mid] )
				_lower = _mid + 1;
			else {
				_trans += (_mid - _keys);
				break _match;
			}
		}
		_keys += _klen;
		_trans += _klen;
	}

	_klen = _SyntheticAccessorFSM_range_lengths[cs];
	if ( _klen > 0 ) {
		int _lower = _keys;
		int _mid;
		int _upper = _keys + (_klen<<1) - 2;
		while (true) {
			if ( _upper < _lower )
				break;

			_mid = _lower + (((_upper-_lower) >> 1) & ~1);
			if ( ( instructions.get(p).getOpcode().value) < _SyntheticAccessorFSM_trans_keys[_mid] )
				_upper = _mid - 2;
			else if ( ( instructions.get(p).getOpcode().value) > _SyntheticAccessorFSM_trans_keys[_mid+1] )
				_lower = _mid + 2;
			else {
				_trans += ((_mid - _keys)>>1);
				break _match;
			}
		}
		_trans += _klen;
	}
	} while (false);

	_trans = _SyntheticAccessorFSM_indicies[_trans];
	cs = _SyntheticAccessorFSM_trans_targs[_trans];

	if ( _SyntheticAccessorFSM_trans_actions[_trans] != 0 ) {
		_acts = _SyntheticAccessorFSM_trans_actions[_trans];
		_nacts = (int) _SyntheticAccessorFSM_actions[_acts++];
		while ( _nacts-- > 0 )
	{
			switch ( _SyntheticAccessorFSM_actions[_acts++] )
			{
	case 0:
// line 93 "SyntheticAccessorFSM.rl"
	{
                putRegister = ((OneRegisterInstruction)instructions.get(p)).getRegisterA();
            }
	break;
	case 1:
// line 100 "SyntheticAccessorFSM.rl"
	{
                constantValue = ((WideLiteralInstruction)instructions.get(p)).getWideLiteral();
            }
	break;
	case 2:
// line 104 "SyntheticAccessorFSM.rl"
	{
                mathType = INT;
                mathOp = ADD;
                constantValue = ((WideLiteralInstruction)instructions.get(p)).getWideLiteral();
            }
	break;
	case 3:
// line 110 "SyntheticAccessorFSM.rl"
	{ mathType = INT; }
	break;
	case 4:
// line 111 "SyntheticAccessorFSM.rl"
	{ mathType = LONG; }
	break;
	case 5:
// line 112 "SyntheticAccessorFSM.rl"
	{ mathType = FLOAT; }
	break;
	case 6:
// line 113 "SyntheticAccessorFSM.rl"
	{mathType = DOUBLE; }
	break;
	case 7:
// line 113 "SyntheticAccessorFSM.rl"
	{
                mathOp = ADD;
            }
	break;
	case 8:
// line 116 "SyntheticAccessorFSM.rl"
	{ mathType = INT; }
	break;
	case 9:
// line 117 "SyntheticAccessorFSM.rl"
	{ mathType = LONG; }
	break;
	case 10:
// line 118 "SyntheticAccessorFSM.rl"
	{ mathType = FLOAT; }
	break;
	case 11:
// line 119 "SyntheticAccessorFSM.rl"
	{mathType = DOUBLE; }
	break;
	case 12:
// line 119 "SyntheticAccessorFSM.rl"
	{
                mathOp = SUB;
            }
	break;
	case 13:
// line 123 "SyntheticAccessorFSM.rl"
	{
                mathOp = MUL;
            }
	break;
	case 14:
// line 127 "SyntheticAccessorFSM.rl"
	{
                mathOp = DIV;
            }
	break;
	case 15:
// line 131 "SyntheticAccessorFSM.rl"
	{
                mathOp = REM;
            }
	break;
	case 16:
// line 134 "SyntheticAccessorFSM.rl"
	{
                mathOp = AND;
            }
	break;
	case 17:
// line 137 "SyntheticAccessorFSM.rl"
	{
                mathOp = OR;
            }
	break;
	case 18:
// line 140 "SyntheticAccessorFSM.rl"
	{
                mathOp = XOR;
            }
	break;
	case 19:
// line 143 "SyntheticAccessorFSM.rl"
	{
                mathOp = SHL;
            }
	break;
	case 20:
// line 146 "SyntheticAccessorFSM.rl"
	{
                mathOp = SHR;
            }
	break;
	case 21:
// line 149 "SyntheticAccessorFSM.rl"
	{
                mathOp = USHR;
            }
	break;
	case 22:
// line 155 "SyntheticAccessorFSM.rl"
	{
                returnRegister = ((OneRegisterInstruction)instructions.get(p)).getRegisterA();
            }
	break;
	case 23:
// line 161 "SyntheticAccessorFSM.rl"
	{
                accessorType = SyntheticAccessorResolver.GETTER; { p += 1; _goto_targ = 5; if (true)  continue _goto;}
            }
	break;
	case 24:
// line 165 "SyntheticAccessorFSM.rl"
	{
                accessorType = SyntheticAccessorResolver.SETTER; { p += 1; _goto_targ = 5; if (true)  continue _goto;}
            }
	break;
	case 25:
// line 169 "SyntheticAccessorFSM.rl"
	{
                accessorType = SyntheticAccessorResolver.METHOD; { p += 1; _goto_targ = 5; if (true)  continue _goto;}
            }
	break;
	case 26:
// line 173 "SyntheticAccessorFSM.rl"
	{
                accessorType = getIncrementType(mathOp, mathType, constantValue, putRegister, returnRegister);
            }
	break;
	case 27:
// line 177 "SyntheticAccessorFSM.rl"
	{
                accessorType = getIncrementType(mathOp, mathType, constantValue, putRegister, returnRegister);
            }
	break;
	case 28:
// line 185 "SyntheticAccessorFSM.rl"
	{
                accessorType = mathOp; { p += 1; _goto_targ = 5; if (true)  continue _goto;}
            }
	break;
// line 480 "/home/jesusfreke/projects/smali/dexlib2/src/main/java/org/jf/dexlib2/util/SyntheticAccessorFSM.java"
			}
		}
	}

case 2:
	if ( cs == 0 ) {
		_goto_targ = 5;
		continue _goto;
	}
	if ( ++p != pe ) {
		_goto_targ = 1;
		continue _goto;
	}
case 4:
case 5:
	}
	break; }
	}

// line 198 "SyntheticAccessorFSM.rl"


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