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

package org.jf.dexlib.Debug;

public enum DebugOpcode {
    DBG_END_SEQUENCE((byte)0x00),
    DBG_ADVANCE_PC((byte)0x01),
    DBG_ADVANCE_LINE((byte)0x02),
    DBG_START_LOCAL((byte)0x03),
    DBG_START_LOCAL_EXTENDED((byte)0x04),
    DBG_END_LOCAL((byte)0x05),
    DBG_RESTART_LOCAL((byte)0x06),
    DBG_SET_PROLOGUE_END((byte)0x07),
    DBG_SET_EPILOGUE_BEGIN((byte)0x08),
    DBG_SET_FILE((byte)0x09),
    DBG_SPECIAL_OPCODE((byte)0x0A);

    private static DebugOpcode[] opcodesByValue;

    static {
        opcodesByValue = new DebugOpcode[11];

        for (DebugOpcode debugOpcode: DebugOpcode.values()) {
            opcodesByValue[debugOpcode.value & 0xFF] = debugOpcode;
        }
    }

    public static DebugOpcode getDebugOpcodeByValue(byte debugOpcodeValue) {
        debugOpcodeValue = (byte)Math.min(debugOpcodeValue & 0xFF, 0x0A);
        return opcodesByValue[debugOpcodeValue];
    }

    public final byte value;

    DebugOpcode(byte value) {
        this.value = value;
    }
}
