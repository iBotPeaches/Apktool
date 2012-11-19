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
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.jf.dexlib.Code;

import org.jf.dexlib.Code.Format.Format;

import java.util.HashMap;

public enum Opcode
{
    NOP((short)0x00, "nop", ReferenceType.none, Format.Format10x, Opcode.CAN_CONTINUE),
    MOVE((short)0x01, "move", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    MOVE_FROM16((short)0x02, "move/from16", ReferenceType.none, Format.Format22x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    MOVE_16((short)0x03, "move/16", ReferenceType.none, Format.Format32x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    MOVE_WIDE((short)0x04, "move-wide", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    MOVE_WIDE_FROM16((short)0x05, "move-wide/from16", ReferenceType.none, Format.Format22x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    MOVE_WIDE_16((short)0x06, "move-wide/16", ReferenceType.none, Format.Format32x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    MOVE_OBJECT((short)0x07, "move-object", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    MOVE_OBJECT_FROM16((short)0x08, "move-object/from16", ReferenceType.none, Format.Format22x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    MOVE_OBJECT_16((short)0x09, "move-object/16", ReferenceType.none, Format.Format32x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    MOVE_RESULT((short)0x0a, "move-result", ReferenceType.none, Format.Format11x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    MOVE_RESULT_WIDE((short)0x0b, "move-result-wide", ReferenceType.none, Format.Format11x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    MOVE_RESULT_OBJECT((short)0x0c, "move-result-object", ReferenceType.none, Format.Format11x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    MOVE_EXCEPTION((short)0x0d, "move-exception", ReferenceType.none, Format.Format11x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    RETURN_VOID((short)0x0e, "return-void", ReferenceType.none, Format.Format10x),
    RETURN((short)0x0f, "return", ReferenceType.none, Format.Format11x),
    RETURN_WIDE((short)0x10, "return-wide", ReferenceType.none, Format.Format11x),
    RETURN_OBJECT((short)0x11, "return-object", ReferenceType.none, Format.Format11x),
    CONST_4((short)0x12, "const/4", ReferenceType.none, Format.Format11n, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    CONST_16((short)0x13, "const/16", ReferenceType.none, Format.Format21s, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    CONST((short)0x14, "const", ReferenceType.none, Format.Format31i, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    CONST_HIGH16((short)0x15, "const/high16", ReferenceType.none, Format.Format21h, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    CONST_WIDE_16((short)0x16, "const-wide/16", ReferenceType.none, Format.Format21s, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    CONST_WIDE_32((short)0x17, "const-wide/32", ReferenceType.none, Format.Format31i, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    CONST_WIDE((short)0x18, "const-wide", ReferenceType.none, Format.Format51l, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    CONST_WIDE_HIGH16((short)0x19, "const-wide/high16", ReferenceType.none, Format.Format21h, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    CONST_STRING((short)0x1a, "const-string", ReferenceType.string, Format.Format21c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER, (short)0x1b),
    CONST_STRING_JUMBO((short)0x1b, "const-string/jumbo", ReferenceType.string, Format.Format31c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    CONST_CLASS((short)0x1c, "const-class", ReferenceType.type, Format.Format21c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER, (short)0xff00),
    MONITOR_ENTER((short)0x1d, "monitor-enter", ReferenceType.none, Format.Format11x, Opcode.CAN_THROW | Opcode.CAN_CONTINUE),
    MONITOR_EXIT((short)0x1e, "monitor-exit", ReferenceType.none, Format.Format11x, Opcode.CAN_THROW | Opcode.CAN_CONTINUE),
    CHECK_CAST((short)0x1f, "check-cast", ReferenceType.type, Format.Format21c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER, (short)0xff01),
    INSTANCE_OF((short)0x20, "instance-of", ReferenceType.type, Format.Format22c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER, (short)0xff02),
    ARRAY_LENGTH((short)0x21, "array-length", ReferenceType.none, Format.Format12x, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    NEW_INSTANCE((short)0x22, "new-instance", ReferenceType.type, Format.Format21c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER, (short)0xff03),
    NEW_ARRAY((short)0x23, "new-array", ReferenceType.type, Format.Format22c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER, (short)0xff04),
    FILLED_NEW_ARRAY((short)0x24, "filled-new-array", ReferenceType.type, Format.Format35c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_RESULT),
    FILLED_NEW_ARRAY_RANGE((short)0x25, "filled-new-array/range", ReferenceType.type, Format.Format3rc, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_RESULT, (short)0xff05),
    FILL_ARRAY_DATA((short)0x26, "fill-array-data", ReferenceType.none, Format.Format31t, Opcode.CAN_CONTINUE),
    THROW((short)0x27, "throw", ReferenceType.none, Format.Format11x, Opcode.CAN_THROW),
    GOTO((short)0x28, "goto", ReferenceType.none, Format.Format10t),
    GOTO_16((short)0x29, "goto/16", ReferenceType.none, Format.Format20t),
    GOTO_32((short)0x2a, "goto/32", ReferenceType.none, Format.Format30t),
    PACKED_SWITCH((short)0x2b, "packed-switch", ReferenceType.none, Format.Format31t, Opcode.CAN_CONTINUE),
    SPARSE_SWITCH((short)0x2c, "sparse-switch", ReferenceType.none, Format.Format31t, Opcode.CAN_CONTINUE),
    CMPL_FLOAT((short)0x2d, "cmpl-float", ReferenceType.none, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    CMPG_FLOAT((short)0x2e, "cmpg-float", ReferenceType.none, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    CMPL_DOUBLE((short)0x2f, "cmpl-double", ReferenceType.none, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    CMPG_DOUBLE((short)0x30, "cmpg-double", ReferenceType.none, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    CMP_LONG((short)0x31, "cmp-long", ReferenceType.none, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    IF_EQ((short)0x32, "if-eq", ReferenceType.none, Format.Format22t, Opcode.CAN_CONTINUE),
    IF_NE((short)0x33, "if-ne", ReferenceType.none, Format.Format22t, Opcode.CAN_CONTINUE),
    IF_LT((short)0x34, "if-lt", ReferenceType.none, Format.Format22t, Opcode.CAN_CONTINUE),
    IF_GE((short)0x35, "if-ge", ReferenceType.none, Format.Format22t, Opcode.CAN_CONTINUE),
    IF_GT((short)0x36, "if-gt", ReferenceType.none, Format.Format22t, Opcode.CAN_CONTINUE),
    IF_LE((short)0x37, "if-le", ReferenceType.none, Format.Format22t, Opcode.CAN_CONTINUE),
    IF_EQZ((short)0x38, "if-eqz", ReferenceType.none, Format.Format21t, Opcode.CAN_CONTINUE),
    IF_NEZ((short)0x39, "if-nez", ReferenceType.none, Format.Format21t, Opcode.CAN_CONTINUE),
    IF_LTZ((short)0x3a, "if-ltz", ReferenceType.none, Format.Format21t, Opcode.CAN_CONTINUE),
    IF_GEZ((short)0x3b, "if-gez", ReferenceType.none, Format.Format21t, Opcode.CAN_CONTINUE),
    IF_GTZ((short)0x3c, "if-gtz", ReferenceType.none, Format.Format21t, Opcode.CAN_CONTINUE),
    IF_LEZ((short)0x3d, "if-lez", ReferenceType.none, Format.Format21t, Opcode.CAN_CONTINUE),
    AGET((short)0x44, "aget", ReferenceType.none, Format.Format23x, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    AGET_WIDE((short)0x45, "aget-wide", ReferenceType.none, Format.Format23x, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    AGET_OBJECT((short)0x46, "aget-object", ReferenceType.none, Format.Format23x, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    AGET_BOOLEAN((short)0x47, "aget-boolean", ReferenceType.none, Format.Format23x, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    AGET_BYTE((short)0x48, "aget-byte", ReferenceType.none, Format.Format23x, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    AGET_CHAR((short)0x49, "aget-char", ReferenceType.none, Format.Format23x, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    AGET_SHORT((short)0x4a, "aget-short", ReferenceType.none, Format.Format23x, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    APUT((short)0x4b, "aput", ReferenceType.none, Format.Format23x, Opcode.CAN_THROW | Opcode.CAN_CONTINUE),
    APUT_WIDE((short)0x4c, "aput-wide", ReferenceType.none, Format.Format23x, Opcode.CAN_THROW | Opcode.CAN_CONTINUE),
    APUT_OBJECT((short)0x4d, "aput-object", ReferenceType.none, Format.Format23x, Opcode.CAN_THROW | Opcode.CAN_CONTINUE),
    APUT_BOOLEAN((short)0x4e, "aput-boolean", ReferenceType.none, Format.Format23x, Opcode.CAN_THROW | Opcode.CAN_CONTINUE),
    APUT_BYTE((short)0x4f, "aput-byte", ReferenceType.none, Format.Format23x, Opcode.CAN_THROW | Opcode.CAN_CONTINUE),
    APUT_CHAR((short)0x50, "aput-char", ReferenceType.none, Format.Format23x, Opcode.CAN_THROW | Opcode.CAN_CONTINUE),
    APUT_SHORT((short)0x51, "aput-short", ReferenceType.none, Format.Format23x, Opcode.CAN_THROW | Opcode.CAN_CONTINUE),
    IGET((short)0x52, "iget", ReferenceType.field, Format.Format22c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER, (short)0xff06),
    IGET_WIDE((short)0x53, "iget-wide", ReferenceType.field, Format.Format22c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER, (short)0xff07),
    IGET_OBJECT((short)0x54, "iget-object", ReferenceType.field, Format.Format22c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER, (short)0xff08),
    IGET_BOOLEAN((short)0x55, "iget-boolean", ReferenceType.field, Format.Format22c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER, (short)0xff09),
    IGET_BYTE((short)0x56, "iget-byte", ReferenceType.field, Format.Format22c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER, (short)0xff0a),
    IGET_CHAR((short)0x57, "iget-char", ReferenceType.field, Format.Format22c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER, (short)0xff0b),
    IGET_SHORT((short)0x58, "iget-short", ReferenceType.field, Format.Format22c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER, (short)0xff0c),
    IPUT((short)0x59, "iput", ReferenceType.field, Format.Format22c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE, (short)0xff0d),
    IPUT_WIDE((short)0x5a, "iput-wide", ReferenceType.field, Format.Format22c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE, (short)0xff0e),
    IPUT_OBJECT((short)0x5b, "iput-object", ReferenceType.field, Format.Format22c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE, (short)0xff0f),
    IPUT_BOOLEAN((short)0x5c, "iput-boolean", ReferenceType.field, Format.Format22c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE, (short)0xff10),
    IPUT_BYTE((short)0x5d, "iput-byte", ReferenceType.field, Format.Format22c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE, (short)0xff11),
    IPUT_CHAR((short)0x5e, "iput-char", ReferenceType.field, Format.Format22c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE, (short)0xff12),
    IPUT_SHORT((short)0x5f, "iput-short", ReferenceType.field, Format.Format22c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE, (short)0xff13),
    SGET((short)0x60, "sget", ReferenceType.field, Format.Format21c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER, (short)0xff14),
    SGET_WIDE((short)0x61, "sget-wide", ReferenceType.field, Format.Format21c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER, (short)0xff15),
    SGET_OBJECT((short)0x62, "sget-object", ReferenceType.field, Format.Format21c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER, (short)0xff16),
    SGET_BOOLEAN((short)0x63, "sget-boolean", ReferenceType.field, Format.Format21c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER, (short)0xff17),
    SGET_BYTE((short)0x64, "sget-byte", ReferenceType.field, Format.Format21c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER, (short)0xff18),
    SGET_CHAR((short)0x65, "sget-char", ReferenceType.field, Format.Format21c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER, (short)0xff19),
    SGET_SHORT((short)0x66, "sget-short", ReferenceType.field, Format.Format21c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER, (short)0xff1a),
    SPUT((short)0x67, "sput", ReferenceType.field, Format.Format21c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE, (short)0xff1b),
    SPUT_WIDE((short)0x68, "sput-wide", ReferenceType.field, Format.Format21c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE, (short)0xff1c),
    SPUT_OBJECT((short)0x69, "sput-object", ReferenceType.field, Format.Format21c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE, (short)0xff1d),
    SPUT_BOOLEAN((short)0x6a, "sput-boolean", ReferenceType.field, Format.Format21c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE, (short)0xff1e),
    SPUT_BYTE((short)0x6b, "sput-byte", ReferenceType.field, Format.Format21c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE, (short)0xff1f),
    SPUT_CHAR((short)0x6c, "sput-char", ReferenceType.field, Format.Format21c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE, (short)0xff20),
    SPUT_SHORT((short)0x6d, "sput-short", ReferenceType.field, Format.Format21c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE, (short)0xff21),
    INVOKE_VIRTUAL((short)0x6e, "invoke-virtual", ReferenceType.method, Format.Format35c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_RESULT),
    INVOKE_SUPER((short)0x6f, "invoke-super", ReferenceType.method, Format.Format35c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_RESULT),
    INVOKE_DIRECT((short)0x70, "invoke-direct", ReferenceType.method, Format.Format35c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_RESULT | Opcode.CAN_INITIALIZE_REFERENCE),
    INVOKE_STATIC((short)0x71, "invoke-static", ReferenceType.method, Format.Format35c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_RESULT),
    INVOKE_INTERFACE((short)0x72, "invoke-interface", ReferenceType.method, Format.Format35c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_RESULT),
    INVOKE_VIRTUAL_RANGE((short)0x74, "invoke-virtual/range", ReferenceType.method, Format.Format3rc, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_RESULT, (short)0xff22),
    INVOKE_SUPER_RANGE((short)0x75, "invoke-super/range", ReferenceType.method, Format.Format3rc, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_RESULT, (short)0xff23),
    INVOKE_DIRECT_RANGE((short)0x76, "invoke-direct/range", ReferenceType.method, Format.Format3rc, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_RESULT | Opcode.CAN_INITIALIZE_REFERENCE, (short)0xff24),
    INVOKE_STATIC_RANGE((short)0x77, "invoke-static/range", ReferenceType.method, Format.Format3rc, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_RESULT, (short)0xff25),
    INVOKE_INTERFACE_RANGE((short)0x78, "invoke-interface/range", ReferenceType.method, Format.Format3rc, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_RESULT, (short)0xff26),
    NEG_INT((short)0x7b, "neg-int", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    NOT_INT((short)0x7c, "not-int", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    NEG_LONG((short)0x7d, "neg-long", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    NOT_LONG((short)0x7e, "not-long", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    NEG_FLOAT((short)0x7f, "neg-float", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    NEG_DOUBLE((short)0x80, "neg-double", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    INT_TO_LONG((short)0x81, "int-to-long", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    INT_TO_FLOAT((short)0x82, "int-to-float", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    INT_TO_DOUBLE((short)0x83, "int-to-double", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    LONG_TO_INT((short)0x84, "long-to-int", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    LONG_TO_FLOAT((short)0x85, "long-to-float", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    LONG_TO_DOUBLE((short)0x86, "long-to-double", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    FLOAT_TO_INT((short)0x87, "float-to-int", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    FLOAT_TO_LONG((short)0x88, "float-to-long", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    FLOAT_TO_DOUBLE((short)0x89, "float-to-double", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    DOUBLE_TO_INT((short)0x8a, "double-to-int", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    DOUBLE_TO_LONG((short)0x8b, "double-to-long", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    DOUBLE_TO_FLOAT((short)0x8c, "double-to-float", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    INT_TO_BYTE((short)0x8d, "int-to-byte", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    INT_TO_CHAR((short)0x8e, "int-to-char", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    INT_TO_SHORT((short)0x8f, "int-to-short", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    ADD_INT((short)0x90, "add-int", ReferenceType.none, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    SUB_INT((short)0x91, "sub-int", ReferenceType.none, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    MUL_INT((short)0x92, "mul-int", ReferenceType.none, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    DIV_INT((short)0x93, "div-int", ReferenceType.none, Format.Format23x, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    REM_INT((short)0x94, "rem-int", ReferenceType.none, Format.Format23x, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    AND_INT((short)0x95, "and-int", ReferenceType.none, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    OR_INT((short)0x96, "or-int", ReferenceType.none, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    XOR_INT((short)0x97, "xor-int", ReferenceType.none, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    SHL_INT((short)0x98, "shl-int", ReferenceType.none, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    SHR_INT((short)0x99, "shr-int", ReferenceType.none, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    USHR_INT((short)0x9a, "ushr-int", ReferenceType.none, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    ADD_LONG((short)0x9b, "add-long", ReferenceType.none, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    SUB_LONG((short)0x9c, "sub-long", ReferenceType.none, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    MUL_LONG((short)0x9d, "mul-long", ReferenceType.none, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    DIV_LONG((short)0x9e, "div-long", ReferenceType.none, Format.Format23x, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    REM_LONG((short)0x9f, "rem-long", ReferenceType.none, Format.Format23x, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    AND_LONG((short)0xa0, "and-long", ReferenceType.none, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    OR_LONG((short)0xa1, "or-long", ReferenceType.none, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    XOR_LONG((short)0xa2, "xor-long", ReferenceType.none, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    SHL_LONG((short)0xa3, "shl-long", ReferenceType.none, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    SHR_LONG((short)0xa4, "shr-long", ReferenceType.none, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    USHR_LONG((short)0xa5, "ushr-long", ReferenceType.none, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    ADD_FLOAT((short)0xa6, "add-float", ReferenceType.none, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    SUB_FLOAT((short)0xa7, "sub-float", ReferenceType.none, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    MUL_FLOAT((short)0xa8, "mul-float", ReferenceType.none, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    DIV_FLOAT((short)0xa9, "div-float", ReferenceType.none, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    REM_FLOAT((short)0xaa, "rem-float", ReferenceType.none, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    ADD_DOUBLE((short)0xab, "add-double", ReferenceType.none, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    SUB_DOUBLE((short)0xac, "sub-double", ReferenceType.none, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    MUL_DOUBLE((short)0xad, "mul-double", ReferenceType.none, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    DIV_DOUBLE((short)0xae, "div-double", ReferenceType.none, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    REM_DOUBLE((short)0xaf, "rem-double", ReferenceType.none, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    ADD_INT_2ADDR((short)0xb0, "add-int/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    SUB_INT_2ADDR((short)0xb1, "sub-int/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    MUL_INT_2ADDR((short)0xb2, "mul-int/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    DIV_INT_2ADDR((short)0xb3, "div-int/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    REM_INT_2ADDR((short)0xb4, "rem-int/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    AND_INT_2ADDR((short)0xb5, "and-int/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    OR_INT_2ADDR((short)0xb6, "or-int/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    XOR_INT_2ADDR((short)0xb7, "xor-int/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    SHL_INT_2ADDR((short)0xb8, "shl-int/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    SHR_INT_2ADDR((short)0xb9, "shr-int/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    USHR_INT_2ADDR((short)0xba, "ushr-int/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    ADD_LONG_2ADDR((short)0xbb, "add-long/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    SUB_LONG_2ADDR((short)0xbc, "sub-long/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    MUL_LONG_2ADDR((short)0xbd, "mul-long/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    DIV_LONG_2ADDR((short)0xbe, "div-long/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    REM_LONG_2ADDR((short)0xbf, "rem-long/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    AND_LONG_2ADDR((short)0xc0, "and-long/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    OR_LONG_2ADDR((short)0xc1, "or-long/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    XOR_LONG_2ADDR((short)0xc2, "xor-long/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    SHL_LONG_2ADDR((short)0xc3, "shl-long/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    SHR_LONG_2ADDR((short)0xc4, "shr-long/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    USHR_LONG_2ADDR((short)0xc5, "ushr-long/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    ADD_FLOAT_2ADDR((short)0xc6, "add-float/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    SUB_FLOAT_2ADDR((short)0xc7, "sub-float/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    MUL_FLOAT_2ADDR((short)0xc8, "mul-float/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    DIV_FLOAT_2ADDR((short)0xc9, "div-float/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    REM_FLOAT_2ADDR((short)0xca, "rem-float/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    ADD_DOUBLE_2ADDR((short)0xcb, "add-double/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    SUB_DOUBLE_2ADDR((short)0xcc, "sub-double/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    MUL_DOUBLE_2ADDR((short)0xcd, "mul-double/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    DIV_DOUBLE_2ADDR((short)0xce, "div-double/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    REM_DOUBLE_2ADDR((short)0xcf, "rem-double/2addr", ReferenceType.none, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    ADD_INT_LIT16((short)0xd0, "add-int/lit16", ReferenceType.none, Format.Format22s, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    RSUB_INT((short)0xd1, "rsub-int", ReferenceType.none, Format.Format22s, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    MUL_INT_LIT16((short)0xd2, "mul-int/lit16", ReferenceType.none, Format.Format22s, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    DIV_INT_LIT16((short)0xd3, "div-int/lit16", ReferenceType.none, Format.Format22s, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    REM_INT_LIT16((short)0xd4, "rem-int/lit16", ReferenceType.none, Format.Format22s, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    AND_INT_LIT16((short)0xd5, "and-int/lit16", ReferenceType.none, Format.Format22s, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    OR_INT_LIT16((short)0xd6, "or-int/lit16", ReferenceType.none, Format.Format22s, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    XOR_INT_LIT16((short)0xd7, "xor-int/lit16", ReferenceType.none, Format.Format22s, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    ADD_INT_LIT8((short)0xd8, "add-int/lit8", ReferenceType.none, Format.Format22b, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    RSUB_INT_LIT8((short)0xd9, "rsub-int/lit8", ReferenceType.none, Format.Format22b, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    MUL_INT_LIT8((short)0xda, "mul-int/lit8", ReferenceType.none, Format.Format22b, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    DIV_INT_LIT8((short)0xdb, "div-int/lit8", ReferenceType.none, Format.Format22b, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    REM_INT_LIT8((short)0xdc, "rem-int/lit8", ReferenceType.none, Format.Format22b, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    AND_INT_LIT8((short)0xdd, "and-int/lit8", ReferenceType.none, Format.Format22b, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    OR_INT_LIT8((short)0xde, "or-int/lit8", ReferenceType.none, Format.Format22b, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    XOR_INT_LIT8((short)0xdf, "xor-int/lit8", ReferenceType.none, Format.Format22b, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    SHL_INT_LIT8((short)0xe0, "shl-int/lit8", ReferenceType.none, Format.Format22b, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    SHR_INT_LIT8((short)0xe1, "shr-int/lit8", ReferenceType.none, Format.Format22b, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    USHR_INT_LIT8((short)0xe2, "ushr-int/lit8", ReferenceType.none, Format.Format22b, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),

    IGET_VOLATILE((short)0xe3, "iget-volatile", ReferenceType.field, Format.Format22c, Opcode.ODEX_ONLY | Opcode.ODEXED_INSTANCE_VOLATILE | Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    IPUT_VOLATILE((short)0xe4, "iput-volatile", ReferenceType.field, Format.Format22c, Opcode.ODEX_ONLY | Opcode.ODEXED_INSTANCE_VOLATILE | Opcode.CAN_THROW | Opcode.CAN_CONTINUE),
    SGET_VOLATILE((short)0xe5, "sget-volatile", ReferenceType.field, Format.Format21c, Opcode.ODEX_ONLY | Opcode.ODEXED_STATIC_VOLATILE | Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    SPUT_VOLATILE((short)0xe6, "sput-volatile", ReferenceType.field, Format.Format21c, Opcode.ODEX_ONLY | Opcode.ODEXED_STATIC_VOLATILE | Opcode.CAN_THROW | Opcode.CAN_CONTINUE),
    IGET_OBJECT_VOLATILE((short)0xe7, "iget-object-volatile", ReferenceType.field, Format.Format22c, Opcode.ODEX_ONLY | Opcode.ODEXED_INSTANCE_VOLATILE | Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    IGET_WIDE_VOLATILE((short)0xe8, "iget-wide-volatile", ReferenceType.field, Format.Format22c, Opcode.ODEX_ONLY | Opcode.ODEXED_INSTANCE_VOLATILE | Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    IPUT_WIDE_VOLATILE((short)0xe9, "iput-wide-volatile", ReferenceType.field, Format.Format22c, Opcode.ODEX_ONLY | Opcode.ODEXED_INSTANCE_VOLATILE | Opcode.CAN_THROW | Opcode.CAN_CONTINUE),
    SGET_WIDE_VOLATILE((short)0xea, "sget-wide-volatile", ReferenceType.field, Format.Format21c, Opcode.ODEX_ONLY | Opcode.ODEXED_STATIC_VOLATILE | Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    SPUT_WIDE_VOLATILE((short)0xeb, "sput-wide-volatile", ReferenceType.field, Format.Format21c, Opcode.ODEX_ONLY | Opcode.ODEXED_STATIC_VOLATILE | Opcode.CAN_THROW | Opcode.CAN_CONTINUE),

    THROW_VERIFICATION_ERROR((short)0xed, "throw-verification-error", ReferenceType.none, Format.Format20bc, Opcode.ODEX_ONLY | Opcode.CAN_THROW),
    EXECUTE_INLINE((short)0xee, "execute-inline", ReferenceType.none,  Format.Format35mi, Opcode.ODEX_ONLY | Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_RESULT),
    EXECUTE_INLINE_RANGE((short)0xef, "execute-inline/range", ReferenceType.none,  Format.Format3rmi,  Opcode.ODEX_ONLY | Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_RESULT),
    INVOKE_DIRECT_EMPTY((short)0xf0, "invoke-direct-empty", ReferenceType.method,  Format.Format35c, Opcode.ODEX_ONLY | Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_RESULT | Opcode.CAN_INITIALIZE_REFERENCE),
    INVOKE_OBJECT_INIT_RANGE((short)0xf0, "invoke-object-init/range", ReferenceType.method,  Format.Format3rc, Opcode.ODEX_ONLY | Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_RESULT | Opcode.CAN_INITIALIZE_REFERENCE),
    RETURN_VOID_BARRIER((short)0xf1, "return-void-barrier", ReferenceType.none, Format.Format10x, Opcode.ODEX_ONLY),
    IGET_QUICK((short)0xf2, "iget-quick", ReferenceType.none,  Format.Format22cs, Opcode.ODEX_ONLY | Opcode.ODEXED_INSTANCE_QUICK | Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    IGET_WIDE_QUICK((short)0xf3, "iget-wide-quick", ReferenceType.none,  Format.Format22cs, Opcode.ODEX_ONLY | Opcode.ODEXED_INSTANCE_QUICK | Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    IGET_OBJECT_QUICK((short)0xf4, "iget-object-quick", ReferenceType.none,  Format.Format22cs, Opcode.ODEX_ONLY | Opcode.ODEXED_INSTANCE_QUICK | Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    IPUT_QUICK((short)0xf5, "iput-quick", ReferenceType.none,  Format.Format22cs, Opcode.ODEX_ONLY | Opcode.ODEXED_INSTANCE_QUICK | Opcode.CAN_THROW | Opcode.CAN_CONTINUE),
    IPUT_WIDE_QUICK((short)0xf6, "iput-wide-quick", ReferenceType.none,  Format.Format22cs, Opcode.ODEX_ONLY | Opcode.ODEXED_INSTANCE_QUICK | Opcode.CAN_THROW | Opcode.CAN_CONTINUE),
    IPUT_OBJECT_QUICK((short)0xf7, "iput-object-quick", ReferenceType.none,  Format.Format22cs, Opcode.ODEX_ONLY | Opcode.ODEXED_INSTANCE_QUICK | Opcode.CAN_THROW | Opcode.CAN_CONTINUE),
    INVOKE_VIRTUAL_QUICK((short)0xf8, "invoke-virtual-quick", ReferenceType.none,  Format.Format35ms, Opcode.ODEX_ONLY | Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_RESULT),
    INVOKE_VIRTUAL_QUICK_RANGE((short)0xf9, "invoke-virtual-quick/range", ReferenceType.none,  Format.Format3rms, Opcode.ODEX_ONLY | Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_RESULT),
    INVOKE_SUPER_QUICK((short)0xfa, "invoke-super-quick", ReferenceType.none,  Format.Format35ms, Opcode.ODEX_ONLY | Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_RESULT),
    INVOKE_SUPER_QUICK_RANGE((short)0xfb, "invoke-super-quick/range", ReferenceType.none,  Format.Format3rms, Opcode.ODEX_ONLY | Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_RESULT),

    IPUT_OBJECT_VOLATILE((short)0xfc, "iput-object-volatile", ReferenceType.field, Format.Format22c, Opcode.ODEX_ONLY | Opcode.ODEXED_INSTANCE_VOLATILE | Opcode.CAN_THROW | Opcode.CAN_CONTINUE),
    SGET_OBJECT_VOLATILE((short)0xfd, "sget-object-volatile", ReferenceType.field, Format.Format21c, Opcode.ODEX_ONLY | Opcode.ODEXED_STATIC_VOLATILE | Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    SPUT_OBJECT_VOLATILE((short)0xfe, "sput-object-volatile", ReferenceType.field, Format.Format21c, Opcode.ODEX_ONLY | Opcode.ODEXED_STATIC_VOLATILE | Opcode.CAN_THROW | Opcode.CAN_CONTINUE),

    CONST_CLASS_JUMBO((short)0xff00, "const-class/jumbo", ReferenceType.type, Format.Format41c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.JUMBO_OPCODE),
    CHECK_CAST_JUMBO((short)0xff01, "check-cast/jumbo", ReferenceType.type, Format.Format41c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.JUMBO_OPCODE),
    INSTANCE_OF_JUMBO((short)0xff02, "instance-of/jumbo", ReferenceType.type, Format.Format52c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.JUMBO_OPCODE),
    NEW_INSTANCE_JUMBO((short)0xff03, "new-instance/jumbo", ReferenceType.type, Format.Format41c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.JUMBO_OPCODE),
    NEW_ARRAY_JUMBO((short)0xff04, "new-array/jumbo", ReferenceType.type, Format.Format52c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.JUMBO_OPCODE),
    FILLED_NEW_ARRAY_JUMBO((short)0xff05, "filled-new-array/jumbo", ReferenceType.type, Format.Format5rc, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_RESULT | Opcode.JUMBO_OPCODE),
    IGET_JUMBO((short)0xff06, "iget/jumbo", ReferenceType.field, Format.Format52c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.JUMBO_OPCODE),
    IGET_WIDE_JUMBO((short)0xff07, "iget-wide/jumbo", ReferenceType.field, Format.Format52c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER | Opcode.JUMBO_OPCODE),
    IGET_OBJECT_JUMBO((short)0xff08, "iget-object/jumbo", ReferenceType.field, Format.Format52c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.JUMBO_OPCODE),
    IGET_BOOLEAN_JUMBO((short)0xff09, "iget-boolean/jumbo", ReferenceType.field, Format.Format52c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.JUMBO_OPCODE),
    IGET_BYTE_JUMBO((short)0xff0a, "iget-byte/jumbo", ReferenceType.field, Format.Format52c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.JUMBO_OPCODE),
    IGET_CHAR_JUMBO((short)0xff0b, "iget-char/jumbo", ReferenceType.field, Format.Format52c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.JUMBO_OPCODE),
    IGET_SHORT_JUMBO((short)0xff0c, "iget-short/jumbo", ReferenceType.field, Format.Format52c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.JUMBO_OPCODE),
    IPUT_JUMBO((short)0xff0d, "iput/jumbo", ReferenceType.field, Format.Format52c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.JUMBO_OPCODE),
    IPUT_WIDE_JUMBO((short)0xff0e, "iput-wide/jumbo", ReferenceType.field, Format.Format52c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.JUMBO_OPCODE),
    IPUT_OBJECT_JUMBO((short)0xff0f, "iput-object/jumbo", ReferenceType.field, Format.Format52c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.JUMBO_OPCODE),
    IPUT_BOOLEAN_JUMBO((short)0xff10, "iput-boolean/jumbo", ReferenceType.field, Format.Format52c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.JUMBO_OPCODE),
    IPUT_BYTE_JUMBO((short)0xff11, "iput-byte/jumbo", ReferenceType.field, Format.Format52c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.JUMBO_OPCODE),
    IPUT_CHAR_JUMBO((short)0xff12, "iput-char/jumbo", ReferenceType.field, Format.Format52c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.JUMBO_OPCODE),
    IPUT_SHORT_JUMBO((short)0xff13, "iput-short/jumbo", ReferenceType.field, Format.Format52c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.JUMBO_OPCODE),
    SGET_JUMBO((short)0xff14, "sget/jumbo", ReferenceType.field, Format.Format41c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.JUMBO_OPCODE),
    SGET_WIDE_JUMBO((short)0xff15, "sget-wide/jumbo", ReferenceType.field, Format.Format41c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER | Opcode.JUMBO_OPCODE),
    SGET_OBJECT_JUMBO((short)0xff16, "sget-object/jumbo", ReferenceType.field, Format.Format41c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.JUMBO_OPCODE),
    SGET_BOOLEAN_JUMBO((short)0xff17, "sget-boolean/jumbo", ReferenceType.field, Format.Format41c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.JUMBO_OPCODE),
    SGET_BYTE_JUMBO((short)0xff18, "sget-byte/jumbo", ReferenceType.field, Format.Format41c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.JUMBO_OPCODE),
    SGET_CHAR_JUMBO((short)0xff19, "sget-char/jumbo", ReferenceType.field, Format.Format41c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.JUMBO_OPCODE),
    SGET_SHORT_JUMBO((short)0xff1a, "sget-short/jumbo", ReferenceType.field, Format.Format41c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.JUMBO_OPCODE),
    SPUT_JUMBO((short)0xff1b, "sput/jumbo", ReferenceType.field, Format.Format41c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.JUMBO_OPCODE),
    SPUT_WIDE_JUMBO((short)0xff1c, "sput-wide/jumbo", ReferenceType.field, Format.Format41c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.JUMBO_OPCODE),
    SPUT_OBJECT_JUMBO((short)0xff1d, "sput-object/jumbo", ReferenceType.field, Format.Format41c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.JUMBO_OPCODE),
    SPUT_BOOLEAN_JUMBO((short)0xff1e, "sput-boolean/jumbo", ReferenceType.field, Format.Format41c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.JUMBO_OPCODE),
    SPUT_BYTE_JUMBO((short)0xff1f, "sput-byte/jumbo", ReferenceType.field, Format.Format41c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.JUMBO_OPCODE),
    SPUT_CHAR_JUMBO((short)0xff20, "sput-char/jumbo", ReferenceType.field, Format.Format41c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.JUMBO_OPCODE),
    SPUT_SHORT_JUMBO((short)0xff21, "sput-short/jumbo", ReferenceType.field, Format.Format41c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.JUMBO_OPCODE),
    INVOKE_VIRTUAL_JUMBO((short)0xff22, "invoke-virtual/jumbo", ReferenceType.method, Format.Format5rc, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_RESULT | Opcode.JUMBO_OPCODE),
    INVOKE_SUPER_JUMBO((short)0xff23, "invoke-super/jumbo", ReferenceType.method, Format.Format5rc, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_RESULT | Opcode.JUMBO_OPCODE),
    INVOKE_DIRECT_JUMBO((short)0xff24, "invoke-direct/jumbo", ReferenceType.method, Format.Format5rc, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_RESULT | Opcode.JUMBO_OPCODE | Opcode.CAN_INITIALIZE_REFERENCE),
    INVOKE_STATIC_JUMBO((short)0xff25, "invoke-static/jumbo", ReferenceType.method, Format.Format5rc, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_RESULT | Opcode.JUMBO_OPCODE),
    INVOKE_INTERFACE_JUMBO((short)0xff26, "invoke-interface/jumbo", ReferenceType.method, Format.Format5rc, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_RESULT | Opcode.JUMBO_OPCODE),

    INVOKE_OBJECT_INIT_JUMBO((short)0xfff2, "invoke-object-init/jumbo", ReferenceType.method,  Format.Format5rc, Opcode.ODEX_ONLY | Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_RESULT | Opcode.JUMBO_OPCODE | Opcode.CAN_INITIALIZE_REFERENCE),
    IGET_VOLATILE_JUMBO((short)0xfff3, "iget-volatile/jumbo", ReferenceType.field, Format.Format52c, Opcode.ODEX_ONLY | Opcode.ODEXED_INSTANCE_VOLATILE | Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.JUMBO_OPCODE),
    IGET_WIDE_VOLATILE_JUMBO((short)0xfff4, "iget-wide-volatile/jumbo", ReferenceType.field, Format.Format52c, Opcode.ODEX_ONLY | Opcode.ODEXED_INSTANCE_VOLATILE | Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER | Opcode.JUMBO_OPCODE),
    IGET_OBJECT_VOLATILE_JUMBO((short)0xfff5, "iget-object-volatile/jumbo", ReferenceType.field, Format.Format52c, Opcode.ODEX_ONLY | Opcode.ODEXED_INSTANCE_VOLATILE | Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.JUMBO_OPCODE),
    IPUT_VOLATILE_JUMBO((short)0xfff6, "iput-volatile/jumbo", ReferenceType.field, Format.Format52c, Opcode.ODEX_ONLY | Opcode.ODEXED_INSTANCE_VOLATILE | Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.JUMBO_OPCODE),
    IPUT_WIDE_VOLATILE_JUMBO((short)0xfff7, "iput-wide-volatile/jumbo", ReferenceType.field, Format.Format52c, Opcode.ODEX_ONLY | Opcode.ODEXED_INSTANCE_VOLATILE | Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.JUMBO_OPCODE),
    IPUT_OBJECT_VOLATILE_JUMBO((short)0xfff8, "iput-object-volatile/jumbo", ReferenceType.field, Format.Format52c, Opcode.ODEX_ONLY | Opcode.ODEXED_INSTANCE_VOLATILE | Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.JUMBO_OPCODE),
    SGET_VOLATILE_JUMBO((short)0xfff9, "sget-volatile/jumbo", ReferenceType.field, Format.Format41c, Opcode.ODEX_ONLY | Opcode.ODEXED_STATIC_VOLATILE | Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.JUMBO_OPCODE),
    SGET_WIDE_VOLATILE_JUMBO((short)0xfffa, "sget-wide-volatile/jumbo", ReferenceType.field, Format.Format41c, Opcode.ODEX_ONLY | Opcode.ODEXED_STATIC_VOLATILE | Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER | Opcode.JUMBO_OPCODE),
    SGET_OBJECT_VOLATILE_JUMBO((short)0xfffb, "sget-object-volatile/jumbo", ReferenceType.field, Format.Format41c, Opcode.ODEX_ONLY | Opcode.ODEXED_STATIC_VOLATILE | Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.JUMBO_OPCODE),
    SPUT_VOLATILE_JUMBO((short)0xfffc, "sput-volatile/jumbo", ReferenceType.field, Format.Format41c, Opcode.ODEX_ONLY | Opcode.ODEXED_STATIC_VOLATILE | Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.JUMBO_OPCODE),
    SPUT_WIDE_VOLATILE_JUMBO((short)0xfffd, "sput-wide-volatile/jumbo", ReferenceType.field, Format.Format41c, Opcode.ODEX_ONLY | Opcode.ODEXED_STATIC_VOLATILE | Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.JUMBO_OPCODE),
    SPUT_OBJECT_VOLATILE_JUMBO((short)0xfffe, "sput-object-volatile/jumbo", ReferenceType.field, Format.Format41c, Opcode.ODEX_ONLY | Opcode.ODEXED_STATIC_VOLATILE | Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.JUMBO_OPCODE);

    private static Opcode[] opcodesByValue;
    private static Opcode[] expandedOpcodesByValue;
    private static HashMap<Integer, Opcode> opcodesByName;

    //if the instruction can throw an exception
    public static final int CAN_THROW = 0x1;
    //if the instruction is an odex only instruction
    public static final int ODEX_ONLY = 0x2;
    //if execution can continue to the next instruction
    public static final int CAN_CONTINUE = 0x4;
    //if the instruction sets the "hidden" result register
    public static final int SETS_RESULT = 0x8;
    //if the instruction sets the value of it's first register
    public static final int SETS_REGISTER = 0x10;
    //if the instruction sets the value of it's first register to a wide type
    public static final int SETS_WIDE_REGISTER = 0x20;
    //if the instruction is an odexed iget-quick/iput-quick instruction
    public static final int ODEXED_INSTANCE_QUICK = 0x40;
    //if the instruction is an odexed iget-volatile/iput-volatile instruction
    public static final int ODEXED_INSTANCE_VOLATILE = 0x80;
    //if the instruction is an odexed sget-volatile/sput-volatile instruction
    public static final int ODEXED_STATIC_VOLATILE = 0x100;
    //if the instruction is a jumbo instruction
    public static final int JUMBO_OPCODE = 0x200;
    //if the instruction can initialize an uninitialized object reference
    public static final int CAN_INITIALIZE_REFERENCE = 0x400;

    static {
        opcodesByValue = new Opcode[256];
        expandedOpcodesByValue = new Opcode[256];
        opcodesByName = new HashMap<Integer, Opcode>();

        for (Opcode opcode: Opcode.values()) {
            //INVOKE_DIRECT_EMPTY was changed to INVOKE_OBJECT_INIT_RANGE in ICS
            if (opcode != INVOKE_DIRECT_EMPTY) {
                if (((opcode.value >> 8) & 0xFF) == 0x00) {
                    opcodesByValue[opcode.value & 0xFF] = opcode;
                } else {
                    assert ((opcode.value >> 8) & 0xFF) == 0xFF;
                    expandedOpcodesByValue[opcode.value & 0xFF] = opcode;
                }
                opcodesByName.put(opcode.name.hashCode(), opcode);
            }
        }
    }

    public static Opcode getOpcodeByName(String opcodeName) {
        return opcodesByName.get(opcodeName.toLowerCase().hashCode());
    }

    public static Opcode getOpcodeByValue(short opcodeValue) {
        if (((opcodeValue >> 8) & 0xFF) == 0x00) {
            return opcodesByValue[opcodeValue & 0xFF];
        } else {
            assert ((opcodeValue >> 8) & 0xFF) == 0xFF;
            return expandedOpcodesByValue[opcodeValue & 0xFF];
        }
    }

    private static void removeOpcodes(Opcode... toRemove) {
        for (Opcode opcode: toRemove) {
            opcodesByName.remove(opcode.name.toLowerCase().hashCode());

            if (((opcode.value >> 8) & 0xFF) == 0x00) {
                opcodesByValue[opcode.value] = null;
            } else {
                expandedOpcodesByValue[opcode.value & 0xFF] = null;
            }
        }
    }

    private static void addOpcodes(Opcode... toAdd) {
        for (Opcode opcode: toAdd) {
            if (((opcode.value >> 8) & 0xFF) == 0x00) {
                opcodesByValue[opcode.value & 0xFF] = opcode;
            } else {
                assert ((opcode.value >> 8) & 0xFF) == 0xFF;
                expandedOpcodesByValue[opcode.value & 0xFF] = opcode;
            }
            opcodesByName.put(opcode.name.hashCode(), opcode);
        }
    }

    /**
     * This will add/remove/replace various opcodes in the value/name maps as needed,
     * based on the idiosyncrasies of that api level
     * @param apiLevel
     */
    public static void updateMapsForApiLevel(int apiLevel, boolean includeJumbo) {
        if (apiLevel < 5) {
            removeOpcodes(THROW_VERIFICATION_ERROR);
        }
        if (apiLevel < 8) {
            removeOpcodes(EXECUTE_INLINE_RANGE);
        }
        if (apiLevel < 9) {
            removeOpcodes(IGET_VOLATILE, IPUT_VOLATILE, SGET_VOLATILE, SPUT_VOLATILE, IGET_OBJECT_VOLATILE,
                    IGET_WIDE_VOLATILE, IPUT_WIDE_VOLATILE, SGET_WIDE_VOLATILE, SPUT_WIDE_VOLATILE,
                    IPUT_OBJECT_VOLATILE, SGET_OBJECT_VOLATILE, SPUT_OBJECT_VOLATILE);
        }
        if (apiLevel < 11) {
            removeOpcodes(RETURN_VOID_BARRIER);
        }
        if (apiLevel < 14) {
            removeOpcodes(INVOKE_OBJECT_INIT_RANGE);
            addOpcodes(INVOKE_DIRECT_EMPTY);
        }
        if (apiLevel < 14 || !includeJumbo) {
            removeOpcodes(CONST_CLASS_JUMBO, CHECK_CAST_JUMBO, INSTANCE_OF_JUMBO, NEW_INSTANCE_JUMBO,
                    NEW_ARRAY_JUMBO, FILLED_NEW_ARRAY_JUMBO, IGET_JUMBO, IGET_WIDE_JUMBO, IGET_OBJECT_JUMBO,
                    IGET_BOOLEAN_JUMBO, IGET_BYTE_JUMBO, IGET_CHAR_JUMBO, IGET_SHORT_JUMBO, IPUT_JUMBO, IPUT_WIDE_JUMBO,
                    IPUT_OBJECT_JUMBO, IPUT_BOOLEAN_JUMBO, IPUT_BYTE_JUMBO, IPUT_CHAR_JUMBO, IPUT_SHORT_JUMBO,
                    SGET_JUMBO, SGET_WIDE_JUMBO, SGET_OBJECT_JUMBO, SGET_BOOLEAN_JUMBO, SGET_BYTE_JUMBO,
                    SGET_CHAR_JUMBO, SGET_SHORT_JUMBO, SPUT_JUMBO, SPUT_WIDE_JUMBO, SPUT_OBJECT_JUMBO,
                    SPUT_BOOLEAN_JUMBO, SPUT_BYTE_JUMBO, SPUT_CHAR_JUMBO, SPUT_SHORT_JUMBO, INVOKE_VIRTUAL_JUMBO,
                    INVOKE_SUPER_JUMBO, INVOKE_DIRECT_JUMBO, INVOKE_STATIC_JUMBO, INVOKE_INTERFACE_JUMBO,
                    INVOKE_OBJECT_INIT_JUMBO, IGET_VOLATILE_JUMBO, IGET_WIDE_VOLATILE_JUMBO,
                    IGET_OBJECT_VOLATILE_JUMBO, IPUT_VOLATILE_JUMBO, IPUT_WIDE_VOLATILE_JUMBO,
                    IPUT_OBJECT_VOLATILE_JUMBO, SGET_VOLATILE_JUMBO, SGET_WIDE_VOLATILE_JUMBO,
                    SGET_OBJECT_VOLATILE_JUMBO, SPUT_VOLATILE_JUMBO, SPUT_WIDE_VOLATILE_JUMBO,
                    SPUT_OBJECT_VOLATILE_JUMBO);
        }
    }

    public final short value;
    public final String name;
    public final ReferenceType referenceType;
    public final Format format;
    public final int flags;
    private final short jumboOpcode;

    Opcode(short opcodeValue, String opcodeName, ReferenceType referenceType, Format format) {
        this(opcodeValue, opcodeName, referenceType, format, 0);
    }

    Opcode(short opcodeValue, String opcodeName, ReferenceType referenceType, Format format, int flags) {
        this(opcodeValue, opcodeName, referenceType, format, flags, (short)-1);
    }

    Opcode(short opcodeValue, String opcodeName, ReferenceType referenceType, Format format, int flags, short jumboOpcodeValue) {
        this.value = opcodeValue;
        this.name = opcodeName;
        this.referenceType = referenceType;
        this.format = format;
        this.flags = flags;
        this.jumboOpcode = jumboOpcodeValue;
    }

    public final boolean canThrow() {
        return (flags & CAN_THROW) != 0;
    }

    public final boolean odexOnly() {
        return (flags & ODEX_ONLY) != 0;
    }

    public final boolean canContinue() {
        return (flags & CAN_CONTINUE) != 0;
    }

    public final boolean setsResult() {
        return (flags & SETS_RESULT) != 0;
    }

    public final boolean setsRegister() {
        return (flags & SETS_REGISTER) != 0;
    }

    public final boolean setsWideRegister() {
        return (flags & SETS_WIDE_REGISTER) != 0;
    }

    public final boolean isOdexedInstanceQuick() {
        return (flags & ODEXED_INSTANCE_QUICK) != 0;
    }

    public final boolean isOdexedInstanceVolatile() {
        return (flags & ODEXED_INSTANCE_VOLATILE) != 0;
    }

    public final boolean isOdexedStaticVolatile() {
        return (flags & ODEXED_STATIC_VOLATILE) != 0;
    }

    public final boolean isJumboOpcode() {
        return (flags & JUMBO_OPCODE) != 0;
    }

    public final boolean canInitializeReference() {
        return (flags & CAN_INITIALIZE_REFERENCE) != 0;
    }

    public final boolean hasJumboOpcode() {
        return jumboOpcode != -1 && Opcode.getOpcodeByValue(jumboOpcode) != null;
    }

    public final Opcode getJumboOpcode() {
        return Opcode.getOpcodeByValue(jumboOpcode);
    }
}
