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

package org.jf.dexlib2;

public enum Opcode
{
    NOP((short)0x00, "nop", ReferenceType.NONE, Format.Format10x, Opcode.CAN_CONTINUE),
    MOVE((short)0x01, "move", ReferenceType.NONE, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    MOVE_FROM16((short)0x02, "move/from16", ReferenceType.NONE, Format.Format22x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    MOVE_16((short)0x03, "move/16", ReferenceType.NONE, Format.Format32x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    MOVE_WIDE((short)0x04, "move-wide", ReferenceType.NONE, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    MOVE_WIDE_FROM16((short)0x05, "move-wide/from16", ReferenceType.NONE, Format.Format22x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    MOVE_WIDE_16((short)0x06, "move-wide/16", ReferenceType.NONE, Format.Format32x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    MOVE_OBJECT((short)0x07, "move-object", ReferenceType.NONE, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    MOVE_OBJECT_FROM16((short)0x08, "move-object/from16", ReferenceType.NONE, Format.Format22x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    MOVE_OBJECT_16((short)0x09, "move-object/16", ReferenceType.NONE, Format.Format32x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    MOVE_RESULT((short)0x0a, "move-result", ReferenceType.NONE, Format.Format11x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    MOVE_RESULT_WIDE((short)0x0b, "move-result-wide", ReferenceType.NONE, Format.Format11x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    MOVE_RESULT_OBJECT((short)0x0c, "move-result-object", ReferenceType.NONE, Format.Format11x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    MOVE_EXCEPTION((short)0x0d, "move-exception", ReferenceType.NONE, Format.Format11x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    RETURN_VOID((short)0x0e, "return-void", ReferenceType.NONE, Format.Format10x),
    RETURN((short)0x0f, "return", ReferenceType.NONE, Format.Format11x),
    RETURN_WIDE((short)0x10, "return-wide", ReferenceType.NONE, Format.Format11x),
    RETURN_OBJECT((short)0x11, "return-object", ReferenceType.NONE, Format.Format11x),
    CONST_4((short)0x12, "const/4", ReferenceType.NONE, Format.Format11n, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    CONST_16((short)0x13, "const/16", ReferenceType.NONE, Format.Format21s, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    CONST((short)0x14, "const", ReferenceType.NONE, Format.Format31i, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    CONST_HIGH16((short)0x15, "const/high16", ReferenceType.NONE, Format.Format21ih, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    CONST_WIDE_16((short)0x16, "const-wide/16", ReferenceType.NONE, Format.Format21s, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    CONST_WIDE_32((short)0x17, "const-wide/32", ReferenceType.NONE, Format.Format31i, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    CONST_WIDE((short)0x18, "const-wide", ReferenceType.NONE, Format.Format51l, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    CONST_WIDE_HIGH16((short)0x19, "const-wide/high16", ReferenceType.NONE, Format.Format21lh, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    CONST_STRING((short)0x1a, "const-string", ReferenceType.STRING, Format.Format21c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER, (short)0x1b),
    CONST_STRING_JUMBO((short)0x1b, "const-string/jumbo", ReferenceType.STRING, Format.Format31c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    CONST_CLASS((short)0x1c, "const-class", ReferenceType.TYPE, Format.Format21c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    MONITOR_ENTER((short)0x1d, "monitor-enter", ReferenceType.NONE, Format.Format11x, Opcode.CAN_THROW | Opcode.CAN_CONTINUE),
    MONITOR_EXIT((short)0x1e, "monitor-exit", ReferenceType.NONE, Format.Format11x, Opcode.CAN_THROW | Opcode.CAN_CONTINUE),
    CHECK_CAST((short)0x1f, "check-cast", ReferenceType.TYPE, Format.Format21c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    INSTANCE_OF((short)0x20, "instance-of", ReferenceType.TYPE, Format.Format22c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    ARRAY_LENGTH((short)0x21, "array-length", ReferenceType.NONE, Format.Format12x, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    NEW_INSTANCE((short)0x22, "new-instance", ReferenceType.TYPE, Format.Format21c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    NEW_ARRAY((short)0x23, "new-array", ReferenceType.TYPE, Format.Format22c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    FILLED_NEW_ARRAY((short)0x24, "filled-new-array", ReferenceType.TYPE, Format.Format35c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_RESULT),
    FILLED_NEW_ARRAY_RANGE((short)0x25, "filled-new-array/range", ReferenceType.TYPE, Format.Format3rc, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_RESULT),
    FILL_ARRAY_DATA((short)0x26, "fill-array-data", ReferenceType.NONE, Format.Format31t, Opcode.CAN_CONTINUE),
    THROW((short)0x27, "throw", ReferenceType.NONE, Format.Format11x, Opcode.CAN_THROW),
    GOTO((short)0x28, "goto", ReferenceType.NONE, Format.Format10t),
    GOTO_16((short)0x29, "goto/16", ReferenceType.NONE, Format.Format20t),
    GOTO_32((short)0x2a, "goto/32", ReferenceType.NONE, Format.Format30t),
    PACKED_SWITCH((short)0x2b, "packed-switch", ReferenceType.NONE, Format.Format31t, Opcode.CAN_CONTINUE),
    SPARSE_SWITCH((short)0x2c, "sparse-switch", ReferenceType.NONE, Format.Format31t, Opcode.CAN_CONTINUE),
    CMPL_FLOAT((short)0x2d, "cmpl-float", ReferenceType.NONE, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    CMPG_FLOAT((short)0x2e, "cmpg-float", ReferenceType.NONE, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    CMPL_DOUBLE((short)0x2f, "cmpl-double", ReferenceType.NONE, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    CMPG_DOUBLE((short)0x30, "cmpg-double", ReferenceType.NONE, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    CMP_LONG((short)0x31, "cmp-long", ReferenceType.NONE, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    IF_EQ((short)0x32, "if-eq", ReferenceType.NONE, Format.Format22t, Opcode.CAN_CONTINUE),
    IF_NE((short)0x33, "if-ne", ReferenceType.NONE, Format.Format22t, Opcode.CAN_CONTINUE),
    IF_LT((short)0x34, "if-lt", ReferenceType.NONE, Format.Format22t, Opcode.CAN_CONTINUE),
    IF_GE((short)0x35, "if-ge", ReferenceType.NONE, Format.Format22t, Opcode.CAN_CONTINUE),
    IF_GT((short)0x36, "if-gt", ReferenceType.NONE, Format.Format22t, Opcode.CAN_CONTINUE),
    IF_LE((short)0x37, "if-le", ReferenceType.NONE, Format.Format22t, Opcode.CAN_CONTINUE),
    IF_EQZ((short)0x38, "if-eqz", ReferenceType.NONE, Format.Format21t, Opcode.CAN_CONTINUE),
    IF_NEZ((short)0x39, "if-nez", ReferenceType.NONE, Format.Format21t, Opcode.CAN_CONTINUE),
    IF_LTZ((short)0x3a, "if-ltz", ReferenceType.NONE, Format.Format21t, Opcode.CAN_CONTINUE),
    IF_GEZ((short)0x3b, "if-gez", ReferenceType.NONE, Format.Format21t, Opcode.CAN_CONTINUE),
    IF_GTZ((short)0x3c, "if-gtz", ReferenceType.NONE, Format.Format21t, Opcode.CAN_CONTINUE),
    IF_LEZ((short)0x3d, "if-lez", ReferenceType.NONE, Format.Format21t, Opcode.CAN_CONTINUE),
    AGET((short)0x44, "aget", ReferenceType.NONE, Format.Format23x, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    AGET_WIDE((short)0x45, "aget-wide", ReferenceType.NONE, Format.Format23x, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    AGET_OBJECT((short)0x46, "aget-object", ReferenceType.NONE, Format.Format23x, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    AGET_BOOLEAN((short)0x47, "aget-boolean", ReferenceType.NONE, Format.Format23x, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    AGET_BYTE((short)0x48, "aget-byte", ReferenceType.NONE, Format.Format23x, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    AGET_CHAR((short)0x49, "aget-char", ReferenceType.NONE, Format.Format23x, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    AGET_SHORT((short)0x4a, "aget-short", ReferenceType.NONE, Format.Format23x, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    APUT((short)0x4b, "aput", ReferenceType.NONE, Format.Format23x, Opcode.CAN_THROW | Opcode.CAN_CONTINUE),
    APUT_WIDE((short)0x4c, "aput-wide", ReferenceType.NONE, Format.Format23x, Opcode.CAN_THROW | Opcode.CAN_CONTINUE),
    APUT_OBJECT((short)0x4d, "aput-object", ReferenceType.NONE, Format.Format23x, Opcode.CAN_THROW | Opcode.CAN_CONTINUE),
    APUT_BOOLEAN((short)0x4e, "aput-boolean", ReferenceType.NONE, Format.Format23x, Opcode.CAN_THROW | Opcode.CAN_CONTINUE),
    APUT_BYTE((short)0x4f, "aput-byte", ReferenceType.NONE, Format.Format23x, Opcode.CAN_THROW | Opcode.CAN_CONTINUE),
    APUT_CHAR((short)0x50, "aput-char", ReferenceType.NONE, Format.Format23x, Opcode.CAN_THROW | Opcode.CAN_CONTINUE),
    APUT_SHORT((short)0x51, "aput-short", ReferenceType.NONE, Format.Format23x, Opcode.CAN_THROW | Opcode.CAN_CONTINUE),
    IGET((short)0x52, "iget", ReferenceType.FIELD, Format.Format22c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    IGET_WIDE((short)0x53, "iget-wide", ReferenceType.FIELD, Format.Format22c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    IGET_OBJECT((short)0x54, "iget-object", ReferenceType.FIELD, Format.Format22c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    IGET_BOOLEAN((short)0x55, "iget-boolean", ReferenceType.FIELD, Format.Format22c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    IGET_BYTE((short)0x56, "iget-byte", ReferenceType.FIELD, Format.Format22c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    IGET_CHAR((short)0x57, "iget-char", ReferenceType.FIELD, Format.Format22c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    IGET_SHORT((short)0x58, "iget-short", ReferenceType.FIELD, Format.Format22c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    IPUT((short)0x59, "iput", ReferenceType.FIELD, Format.Format22c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE),
    IPUT_WIDE((short)0x5a, "iput-wide", ReferenceType.FIELD, Format.Format22c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE),
    IPUT_OBJECT((short)0x5b, "iput-object", ReferenceType.FIELD, Format.Format22c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE),
    IPUT_BOOLEAN((short)0x5c, "iput-boolean", ReferenceType.FIELD, Format.Format22c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE),
    IPUT_BYTE((short)0x5d, "iput-byte", ReferenceType.FIELD, Format.Format22c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE),
    IPUT_CHAR((short)0x5e, "iput-char", ReferenceType.FIELD, Format.Format22c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE),
    IPUT_SHORT((short)0x5f, "iput-short", ReferenceType.FIELD, Format.Format22c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE),
    SGET((short)0x60, "sget", ReferenceType.FIELD, Format.Format21c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    SGET_WIDE((short)0x61, "sget-wide", ReferenceType.FIELD, Format.Format21c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    SGET_OBJECT((short)0x62, "sget-object", ReferenceType.FIELD, Format.Format21c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    SGET_BOOLEAN((short)0x63, "sget-boolean", ReferenceType.FIELD, Format.Format21c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    SGET_BYTE((short)0x64, "sget-byte", ReferenceType.FIELD, Format.Format21c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    SGET_CHAR((short)0x65, "sget-char", ReferenceType.FIELD, Format.Format21c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    SGET_SHORT((short)0x66, "sget-short", ReferenceType.FIELD, Format.Format21c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    SPUT((short)0x67, "sput", ReferenceType.FIELD, Format.Format21c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE),
    SPUT_WIDE((short)0x68, "sput-wide", ReferenceType.FIELD, Format.Format21c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE),
    SPUT_OBJECT((short)0x69, "sput-object", ReferenceType.FIELD, Format.Format21c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE),
    SPUT_BOOLEAN((short)0x6a, "sput-boolean", ReferenceType.FIELD, Format.Format21c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE),
    SPUT_BYTE((short)0x6b, "sput-byte", ReferenceType.FIELD, Format.Format21c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE),
    SPUT_CHAR((short)0x6c, "sput-char", ReferenceType.FIELD, Format.Format21c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE),
    SPUT_SHORT((short)0x6d, "sput-short", ReferenceType.FIELD, Format.Format21c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE),
    INVOKE_VIRTUAL((short)0x6e, "invoke-virtual", ReferenceType.METHOD, Format.Format35c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_RESULT),
    INVOKE_SUPER((short)0x6f, "invoke-super", ReferenceType.METHOD, Format.Format35c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_RESULT),
    INVOKE_DIRECT((short)0x70, "invoke-direct", ReferenceType.METHOD, Format.Format35c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_RESULT | Opcode.CAN_INITIALIZE_REFERENCE),
    INVOKE_STATIC((short)0x71, "invoke-static", ReferenceType.METHOD, Format.Format35c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_RESULT),
    INVOKE_INTERFACE((short)0x72, "invoke-interface", ReferenceType.METHOD, Format.Format35c, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_RESULT),
    INVOKE_VIRTUAL_RANGE((short)0x74, "invoke-virtual/range", ReferenceType.METHOD, Format.Format3rc, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_RESULT),
    INVOKE_SUPER_RANGE((short)0x75, "invoke-super/range", ReferenceType.METHOD, Format.Format3rc, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_RESULT),
    INVOKE_DIRECT_RANGE((short)0x76, "invoke-direct/range", ReferenceType.METHOD, Format.Format3rc, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_RESULT | Opcode.CAN_INITIALIZE_REFERENCE),
    INVOKE_STATIC_RANGE((short)0x77, "invoke-static/range", ReferenceType.METHOD, Format.Format3rc, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_RESULT),
    INVOKE_INTERFACE_RANGE((short)0x78, "invoke-interface/range", ReferenceType.METHOD, Format.Format3rc, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_RESULT),
    NEG_INT((short)0x7b, "neg-int", ReferenceType.NONE, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    NOT_INT((short)0x7c, "not-int", ReferenceType.NONE, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    NEG_LONG((short)0x7d, "neg-long", ReferenceType.NONE, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    NOT_LONG((short)0x7e, "not-long", ReferenceType.NONE, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    NEG_FLOAT((short)0x7f, "neg-float", ReferenceType.NONE, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    NEG_DOUBLE((short)0x80, "neg-double", ReferenceType.NONE, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    INT_TO_LONG((short)0x81, "int-to-long", ReferenceType.NONE, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    INT_TO_FLOAT((short)0x82, "int-to-float", ReferenceType.NONE, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    INT_TO_DOUBLE((short)0x83, "int-to-double", ReferenceType.NONE, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    LONG_TO_INT((short)0x84, "long-to-int", ReferenceType.NONE, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    LONG_TO_FLOAT((short)0x85, "long-to-float", ReferenceType.NONE, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    LONG_TO_DOUBLE((short)0x86, "long-to-double", ReferenceType.NONE, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    FLOAT_TO_INT((short)0x87, "float-to-int", ReferenceType.NONE, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    FLOAT_TO_LONG((short)0x88, "float-to-long", ReferenceType.NONE, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    FLOAT_TO_DOUBLE((short)0x89, "float-to-double", ReferenceType.NONE, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    DOUBLE_TO_INT((short)0x8a, "double-to-int", ReferenceType.NONE, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    DOUBLE_TO_LONG((short)0x8b, "double-to-long", ReferenceType.NONE, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    DOUBLE_TO_FLOAT((short)0x8c, "double-to-float", ReferenceType.NONE, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    INT_TO_BYTE((short)0x8d, "int-to-byte", ReferenceType.NONE, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    INT_TO_CHAR((short)0x8e, "int-to-char", ReferenceType.NONE, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    INT_TO_SHORT((short)0x8f, "int-to-short", ReferenceType.NONE, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    ADD_INT((short)0x90, "add-int", ReferenceType.NONE, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    SUB_INT((short)0x91, "sub-int", ReferenceType.NONE, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    MUL_INT((short)0x92, "mul-int", ReferenceType.NONE, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    DIV_INT((short)0x93, "div-int", ReferenceType.NONE, Format.Format23x, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    REM_INT((short)0x94, "rem-int", ReferenceType.NONE, Format.Format23x, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    AND_INT((short)0x95, "and-int", ReferenceType.NONE, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    OR_INT((short)0x96, "or-int", ReferenceType.NONE, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    XOR_INT((short)0x97, "xor-int", ReferenceType.NONE, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    SHL_INT((short)0x98, "shl-int", ReferenceType.NONE, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    SHR_INT((short)0x99, "shr-int", ReferenceType.NONE, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    USHR_INT((short)0x9a, "ushr-int", ReferenceType.NONE, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    ADD_LONG((short)0x9b, "add-long", ReferenceType.NONE, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    SUB_LONG((short)0x9c, "sub-long", ReferenceType.NONE, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    MUL_LONG((short)0x9d, "mul-long", ReferenceType.NONE, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    DIV_LONG((short)0x9e, "div-long", ReferenceType.NONE, Format.Format23x, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    REM_LONG((short)0x9f, "rem-long", ReferenceType.NONE, Format.Format23x, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    AND_LONG((short)0xa0, "and-long", ReferenceType.NONE, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    OR_LONG((short)0xa1, "or-long", ReferenceType.NONE, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    XOR_LONG((short)0xa2, "xor-long", ReferenceType.NONE, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    SHL_LONG((short)0xa3, "shl-long", ReferenceType.NONE, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    SHR_LONG((short)0xa4, "shr-long", ReferenceType.NONE, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    USHR_LONG((short)0xa5, "ushr-long", ReferenceType.NONE, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    ADD_FLOAT((short)0xa6, "add-float", ReferenceType.NONE, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    SUB_FLOAT((short)0xa7, "sub-float", ReferenceType.NONE, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    MUL_FLOAT((short)0xa8, "mul-float", ReferenceType.NONE, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    DIV_FLOAT((short)0xa9, "div-float", ReferenceType.NONE, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    REM_FLOAT((short)0xaa, "rem-float", ReferenceType.NONE, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    ADD_DOUBLE((short)0xab, "add-double", ReferenceType.NONE, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    SUB_DOUBLE((short)0xac, "sub-double", ReferenceType.NONE, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    MUL_DOUBLE((short)0xad, "mul-double", ReferenceType.NONE, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    DIV_DOUBLE((short)0xae, "div-double", ReferenceType.NONE, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    REM_DOUBLE((short)0xaf, "rem-double", ReferenceType.NONE, Format.Format23x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    ADD_INT_2ADDR((short)0xb0, "add-int/2addr", ReferenceType.NONE, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    SUB_INT_2ADDR((short)0xb1, "sub-int/2addr", ReferenceType.NONE, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    MUL_INT_2ADDR((short)0xb2, "mul-int/2addr", ReferenceType.NONE, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    DIV_INT_2ADDR((short)0xb3, "div-int/2addr", ReferenceType.NONE, Format.Format12x, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    REM_INT_2ADDR((short)0xb4, "rem-int/2addr", ReferenceType.NONE, Format.Format12x, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    AND_INT_2ADDR((short)0xb5, "and-int/2addr", ReferenceType.NONE, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    OR_INT_2ADDR((short)0xb6, "or-int/2addr", ReferenceType.NONE, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    XOR_INT_2ADDR((short)0xb7, "xor-int/2addr", ReferenceType.NONE, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    SHL_INT_2ADDR((short)0xb8, "shl-int/2addr", ReferenceType.NONE, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    SHR_INT_2ADDR((short)0xb9, "shr-int/2addr", ReferenceType.NONE, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    USHR_INT_2ADDR((short)0xba, "ushr-int/2addr", ReferenceType.NONE, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    ADD_LONG_2ADDR((short)0xbb, "add-long/2addr", ReferenceType.NONE, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    SUB_LONG_2ADDR((short)0xbc, "sub-long/2addr", ReferenceType.NONE, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    MUL_LONG_2ADDR((short)0xbd, "mul-long/2addr", ReferenceType.NONE, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    DIV_LONG_2ADDR((short)0xbe, "div-long/2addr", ReferenceType.NONE, Format.Format12x, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    REM_LONG_2ADDR((short)0xbf, "rem-long/2addr", ReferenceType.NONE, Format.Format12x, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    AND_LONG_2ADDR((short)0xc0, "and-long/2addr", ReferenceType.NONE, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    OR_LONG_2ADDR((short)0xc1, "or-long/2addr", ReferenceType.NONE, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    XOR_LONG_2ADDR((short)0xc2, "xor-long/2addr", ReferenceType.NONE, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    SHL_LONG_2ADDR((short)0xc3, "shl-long/2addr", ReferenceType.NONE, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    SHR_LONG_2ADDR((short)0xc4, "shr-long/2addr", ReferenceType.NONE, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    USHR_LONG_2ADDR((short)0xc5, "ushr-long/2addr", ReferenceType.NONE, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    ADD_FLOAT_2ADDR((short)0xc6, "add-float/2addr", ReferenceType.NONE, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    SUB_FLOAT_2ADDR((short)0xc7, "sub-float/2addr", ReferenceType.NONE, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    MUL_FLOAT_2ADDR((short)0xc8, "mul-float/2addr", ReferenceType.NONE, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    DIV_FLOAT_2ADDR((short)0xc9, "div-float/2addr", ReferenceType.NONE, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    REM_FLOAT_2ADDR((short)0xca, "rem-float/2addr", ReferenceType.NONE, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    ADD_DOUBLE_2ADDR((short)0xcb, "add-double/2addr", ReferenceType.NONE, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    SUB_DOUBLE_2ADDR((short)0xcc, "sub-double/2addr", ReferenceType.NONE, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    MUL_DOUBLE_2ADDR((short)0xcd, "mul-double/2addr", ReferenceType.NONE, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    DIV_DOUBLE_2ADDR((short)0xce, "div-double/2addr", ReferenceType.NONE, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    REM_DOUBLE_2ADDR((short)0xcf, "rem-double/2addr", ReferenceType.NONE, Format.Format12x, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    ADD_INT_LIT16((short)0xd0, "add-int/lit16", ReferenceType.NONE, Format.Format22s, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    RSUB_INT((short)0xd1, "rsub-int", ReferenceType.NONE, Format.Format22s, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    MUL_INT_LIT16((short)0xd2, "mul-int/lit16", ReferenceType.NONE, Format.Format22s, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    DIV_INT_LIT16((short)0xd3, "div-int/lit16", ReferenceType.NONE, Format.Format22s, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    REM_INT_LIT16((short)0xd4, "rem-int/lit16", ReferenceType.NONE, Format.Format22s, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    AND_INT_LIT16((short)0xd5, "and-int/lit16", ReferenceType.NONE, Format.Format22s, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    OR_INT_LIT16((short)0xd6, "or-int/lit16", ReferenceType.NONE, Format.Format22s, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    XOR_INT_LIT16((short)0xd7, "xor-int/lit16", ReferenceType.NONE, Format.Format22s, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    ADD_INT_LIT8((short)0xd8, "add-int/lit8", ReferenceType.NONE, Format.Format22b, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    RSUB_INT_LIT8((short)0xd9, "rsub-int/lit8", ReferenceType.NONE, Format.Format22b, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    MUL_INT_LIT8((short)0xda, "mul-int/lit8", ReferenceType.NONE, Format.Format22b, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    DIV_INT_LIT8((short)0xdb, "div-int/lit8", ReferenceType.NONE, Format.Format22b, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    REM_INT_LIT8((short)0xdc, "rem-int/lit8", ReferenceType.NONE, Format.Format22b, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    AND_INT_LIT8((short)0xdd, "and-int/lit8", ReferenceType.NONE, Format.Format22b, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    OR_INT_LIT8((short)0xde, "or-int/lit8", ReferenceType.NONE, Format.Format22b, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    XOR_INT_LIT8((short)0xdf, "xor-int/lit8", ReferenceType.NONE, Format.Format22b, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    SHL_INT_LIT8((short)0xe0, "shl-int/lit8", ReferenceType.NONE, Format.Format22b, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    SHR_INT_LIT8((short)0xe1, "shr-int/lit8", ReferenceType.NONE, Format.Format22b, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    USHR_INT_LIT8((short)0xe2, "ushr-int/lit8", ReferenceType.NONE, Format.Format22b, Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),

    IGET_VOLATILE((short)0xe3, "iget-volatile", minApi(9), ReferenceType.FIELD, Format.Format22c, Opcode.ODEX_ONLY | Opcode.ODEXED_INSTANCE_VOLATILE | Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    IPUT_VOLATILE((short)0xe4, "iput-volatile", minApi(9), ReferenceType.FIELD, Format.Format22c, Opcode.ODEX_ONLY | Opcode.ODEXED_INSTANCE_VOLATILE | Opcode.CAN_THROW | Opcode.CAN_CONTINUE),
    SGET_VOLATILE((short)0xe5, "sget-volatile", minApi(9), ReferenceType.FIELD, Format.Format21c, Opcode.ODEX_ONLY | Opcode.ODEXED_STATIC_VOLATILE | Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    SPUT_VOLATILE((short)0xe6, "sput-volatile", minApi(9), ReferenceType.FIELD, Format.Format21c, Opcode.ODEX_ONLY | Opcode.ODEXED_STATIC_VOLATILE | Opcode.CAN_THROW | Opcode.CAN_CONTINUE),
    IGET_OBJECT_VOLATILE((short)0xe7, "iget-object-volatile", minApi(9), ReferenceType.FIELD, Format.Format22c, Opcode.ODEX_ONLY | Opcode.ODEXED_INSTANCE_VOLATILE | Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    IGET_WIDE_VOLATILE((short)0xe8, "iget-wide-volatile", minApi(9), ReferenceType.FIELD, Format.Format22c, Opcode.ODEX_ONLY | Opcode.ODEXED_INSTANCE_VOLATILE | Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    IPUT_WIDE_VOLATILE((short)0xe9, "iput-wide-volatile", minApi(9), ReferenceType.FIELD, Format.Format22c, Opcode.ODEX_ONLY | Opcode.ODEXED_INSTANCE_VOLATILE | Opcode.CAN_THROW | Opcode.CAN_CONTINUE),
    SGET_WIDE_VOLATILE((short)0xea, "sget-wide-volatile", minApi(9), ReferenceType.FIELD, Format.Format21c, Opcode.ODEX_ONLY | Opcode.ODEXED_STATIC_VOLATILE | Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    SPUT_WIDE_VOLATILE((short)0xeb, "sput-wide-volatile", minApi(9), ReferenceType.FIELD, Format.Format21c, Opcode.ODEX_ONLY | Opcode.ODEXED_STATIC_VOLATILE | Opcode.CAN_THROW | Opcode.CAN_CONTINUE),

    THROW_VERIFICATION_ERROR((short)0xed, "throw-verification-error", minApi(5), ReferenceType.NONE, Format.Format20bc, Opcode.ODEX_ONLY | Opcode.CAN_THROW),
    EXECUTE_INLINE((short)0xee, "execute-inline", ReferenceType.NONE,  Format.Format35mi, Opcode.ODEX_ONLY | Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_RESULT),
    EXECUTE_INLINE_RANGE((short)0xef, "execute-inline/range", minApi(8), ReferenceType.NONE,  Format.Format3rmi,  Opcode.ODEX_ONLY | Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_RESULT),
    INVOKE_DIRECT_EMPTY((short)0xf0, "invoke-direct-empty", maxApi(13), ReferenceType.METHOD,  Format.Format35c, Opcode.ODEX_ONLY | Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_RESULT | Opcode.CAN_INITIALIZE_REFERENCE),
    INVOKE_OBJECT_INIT_RANGE((short)0xf0, "invoke-object-init/range", minApi(14), ReferenceType.METHOD,  Format.Format3rc, Opcode.ODEX_ONLY | Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_RESULT | Opcode.CAN_INITIALIZE_REFERENCE),
    RETURN_VOID_BARRIER((short)0xf1, "return-void-barrier", minApi(11), ReferenceType.NONE, Format.Format10x, Opcode.ODEX_ONLY),
    IGET_QUICK((short)0xf2, "iget-quick", ReferenceType.NONE,  Format.Format22cs, Opcode.ODEX_ONLY | Opcode.ODEXED_INSTANCE_QUICK | Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    IGET_WIDE_QUICK((short)0xf3, "iget-wide-quick", maxApi(22), ReferenceType.NONE,  Format.Format22cs, Opcode.ODEX_ONLY | Opcode.ODEXED_INSTANCE_QUICK | Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER | Opcode.SETS_WIDE_REGISTER),
    IGET_OBJECT_QUICK((short)0xf4, "iget-object-quick", maxApi(22), ReferenceType.NONE,  Format.Format22cs, Opcode.ODEX_ONLY | Opcode.ODEXED_INSTANCE_QUICK | Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    IPUT_QUICK((short)0xf5, "iput-quick", maxApi(22), ReferenceType.NONE,  Format.Format22cs, Opcode.ODEX_ONLY | Opcode.ODEXED_INSTANCE_QUICK | Opcode.CAN_THROW | Opcode.CAN_CONTINUE),
    IPUT_WIDE_QUICK((short)0xf6, "iput-wide-quick", maxApi(22), ReferenceType.NONE,  Format.Format22cs, Opcode.ODEX_ONLY | Opcode.ODEXED_INSTANCE_QUICK | Opcode.CAN_THROW | Opcode.CAN_CONTINUE),
    IPUT_OBJECT_QUICK((short)0xf7, "iput-object-quick", maxApi(22), ReferenceType.NONE,  Format.Format22cs, Opcode.ODEX_ONLY | Opcode.ODEXED_INSTANCE_QUICK | Opcode.CAN_THROW | Opcode.CAN_CONTINUE),
    INVOKE_VIRTUAL_QUICK((short)0xf8, "invoke-virtual-quick", maxApi(22), ReferenceType.NONE,  Format.Format35ms, Opcode.ODEX_ONLY | Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_RESULT),
    INVOKE_VIRTUAL_QUICK_RANGE((short)0xf9, "invoke-virtual-quick/range", maxApi(22), ReferenceType.NONE,  Format.Format3rms, Opcode.ODEX_ONLY | Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_RESULT),
    INVOKE_SUPER_QUICK((short)0xfa, "invoke-super-quick", ReferenceType.NONE,  Format.Format35ms, Opcode.ODEX_ONLY | Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_RESULT),
    INVOKE_SUPER_QUICK_RANGE((short)0xfb, "invoke-super-quick/range", ReferenceType.NONE,  Format.Format3rms, Opcode.ODEX_ONLY | Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_RESULT),

    IPUT_OBJECT_VOLATILE((short)0xfc, "iput-object-volatile", minApi(9), ReferenceType.FIELD, Format.Format22c, Opcode.ODEX_ONLY | Opcode.ODEXED_INSTANCE_VOLATILE | Opcode.CAN_THROW | Opcode.CAN_CONTINUE),
    SGET_OBJECT_VOLATILE((short)0xfd, "sget-object-volatile", minApi(9), ReferenceType.FIELD, Format.Format21c, Opcode.ODEX_ONLY | Opcode.ODEXED_STATIC_VOLATILE | Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_REGISTER),
    SPUT_OBJECT_VOLATILE((short)0xfe, "sput-object-volatile", minApi(9), ReferenceType.FIELD, Format.Format21c, Opcode.ODEX_ONLY | Opcode.ODEXED_STATIC_VOLATILE | Opcode.CAN_THROW | Opcode.CAN_CONTINUE),

    PACKED_SWITCH_PAYLOAD((short)0x100, "packed-switch-payload", ReferenceType.NONE, Format.PackedSwitchPayload, 0),
    SPARSE_SWITCH_PAYLOAD((short)0x200, "sparse-switch-payload", ReferenceType.NONE, Format.SparseSwitchPayload, 0),
    ARRAY_PAYLOAD((short)0x300, "array-payload", ReferenceType.NONE, Format.ArrayPayload, 0),

    // Reuse the deprecated f3-ff opcodes in Art:
    INVOKE_LAMBDA((short)0xf3, "invoke-lambda", minApi(23), ReferenceType.NONE, Format.Format25x, Opcode.CAN_THROW | Opcode.CAN_CONTINUE | Opcode.SETS_RESULT | Opcode.EXPERIMENTAL),
    // TODO: What about JUMBO support if the string ID is too large?
    CAPTURE_VARIABLE((short)0xf5, "capture-variable", minApi(23), ReferenceType.STRING, Format.Format21c, Opcode.EXPERIMENTAL),
    CREATE_LAMBDA((short)0xf6, "create-lambda", minApi(23), ReferenceType.METHOD, Format.Format21c, Opcode.SETS_REGISTER | Opcode.EXPERIMENTAL),
    // TODO: do we need a capture/liberate wide?
    LIBERATE_VARIABLE((short)0xf7, "liberate-variable", minApi(23), ReferenceType.STRING, Format.Format22c, Opcode.SETS_REGISTER | Opcode.EXPERIMENTAL),
    BOX_LAMBDA((short)0xf8, "box-lambda", minApi(23), ReferenceType.NONE, Format.Format22x, Opcode.SETS_REGISTER | Opcode.EXPERIMENTAL),
    UNBOX_LAMBDA((short)0xf9, "unbox-lambda", minApi(23), ReferenceType.TYPE, Format.Format22c, Opcode.SETS_REGISTER | Opcode.EXPERIMENTAL);

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
    //if the instruction is experimental (not potentially supported by Android runtime yet)
    public static final int EXPERIMENTAL = 0x800;

    private static final int ALL_APIS = 0xFFFF0000;

    private static int minApi(int api) {
        return 0xFFFF0000 | (api & 0xFFFF);
    }

    private static int maxApi(int api) {
        return api << 16;
    }

    public final short value;
    public final String name;
    // high 16-bits is the max api, low 16-bits is the min api
    public final int apiConstraints;
    public final int referenceType;
    public final Format format;
    public final int flags;

    Opcode(short opcodeValue, String opcodeName, int referenceType, Format format) {
        this(opcodeValue, opcodeName, ALL_APIS, referenceType, format, 0, (short)-1);
    }

    Opcode(short opcodeValue, String opcodeName, int referenceType, Format format, int flags) {
        this(opcodeValue, opcodeName, ALL_APIS, referenceType, format, flags, (short)-1);
    }

    Opcode(short opcodeValue, String opcodeName, int referenceType, Format format, int flags, short jumboOpcodeValue) {
        this(opcodeValue, opcodeName, ALL_APIS, referenceType, format, flags, jumboOpcodeValue);
    }

    Opcode(short opcodeValue, String opcodeName, int apiConstraints, int referenceType, Format format) {
        this(opcodeValue, opcodeName, apiConstraints, referenceType, format, 0, (short)-1);
    }

    Opcode(short opcodeValue, String opcodeName, int apiConstraints, int referenceType, Format format, int flags) {
        this(opcodeValue, opcodeName, apiConstraints, referenceType, format, flags, (short)-1);
    }

    Opcode(short opcodeValue, String opcodeName, int apiConstraints, int referenceType, Format format, int flags,
           short jumboOpcodeValue) {
        this.value = opcodeValue;
        this.name = opcodeName;
        this.apiConstraints = apiConstraints;
        this.referenceType = referenceType;
        this.format = format;
        this.flags = flags;
        // TODO: implement jumbo opcodes for dexlib2 and uncomment
        // this.jumboOpcode = jumboOpcodeValue;
    }

    /**
     * @return the minimum api level that can use this opcode (inclusive)
     */
    public int getMinApi() {
        return apiConstraints & 0xFFFF;
    }

    /**
     * @return the maximum api level that can to use this opcode (inclusive)
     */
    public int getMaxApi() {
        return apiConstraints >>> 16;
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

    public final boolean isExperimental() {
        return (flags & EXPERIMENTAL) != 0;
    }
}
