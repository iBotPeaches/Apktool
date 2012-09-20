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

import org.jf.dexlib.DebugInfoItem;
import org.jf.dexlib.DexFile;
import org.jf.dexlib.StringIdItem;
import org.jf.dexlib.TypeIdItem;
import org.jf.dexlib.Util.ByteArrayInput;
import org.jf.dexlib.Util.Input;

public class DebugInstructionIterator {
    /**
     * This method decodes the debug instructions in the given byte array and iterates over them, calling
     * the ProcessDebugInstructionDelegate.ProcessDebugInstruction method for each instruction
     * @param in an Input object that the debug instructions can be read from
     * @param processDebugInstruction a <code>ProcessDebugInstructionDelegate</code> object that gets called
     * for each instruction that is encountered
     */
    public static void IterateInstructions(Input in, ProcessRawDebugInstructionDelegate processDebugInstruction) {
        int startDebugOffset;

        while(true)
        {
            startDebugOffset = in.getCursor();
            byte debugOpcode = in.readByte();

            switch (debugOpcode) {
                case 0x00:
                {
                    processDebugInstruction.ProcessEndSequence(startDebugOffset);
                    return;
                }
                case 0x01:
                {
                    int codeAddressDiff = in.readUnsignedLeb128();
                    processDebugInstruction.ProcessAdvancePC(startDebugOffset, in.getCursor() - startDebugOffset,
                            codeAddressDiff);
                    break;
                }
                case 0x02:
                {
                    int lineDiff = in.readSignedLeb128();
                    processDebugInstruction.ProcessAdvanceLine(startDebugOffset, in.getCursor() - startDebugOffset,
                            lineDiff);
                    break;
                }
                case 0x03:
                {
                    int registerNum = in.readUnsignedOrSignedLeb128();
                    boolean isSignedRegister = false;
                    if (registerNum < 0) {
                        isSignedRegister = true;
                        registerNum = ~registerNum;
                    }
                    int nameIndex = in.readUnsignedLeb128() - 1;
                    int typeIndex = in.readUnsignedLeb128() - 1;
                    processDebugInstruction.ProcessStartLocal(startDebugOffset, in.getCursor() - startDebugOffset,
                            registerNum, nameIndex, typeIndex, isSignedRegister);
                    break;
                }
                case 0x04:
                {
                    int registerNum = in.readUnsignedOrSignedLeb128();
                    boolean isSignedRegister = false;
                    if (registerNum < 0) {
                        isSignedRegister = true;
                        registerNum = ~registerNum;
                    }
                    int nameIndex = in.readUnsignedLeb128() - 1;
                    int typeIndex = in.readUnsignedLeb128() - 1;
                    int signatureIndex = in.readUnsignedLeb128() - 1;
                    processDebugInstruction.ProcessStartLocalExtended(startDebugOffset,
                            in.getCursor() - startDebugOffset, registerNum, nameIndex, typeIndex, signatureIndex,
                            isSignedRegister);
                    break;
                }
                case 0x05:
                {
                    int registerNum = in.readUnsignedOrSignedLeb128();
                    boolean isSignedRegister = false;
                    if (registerNum < 0) {
                        isSignedRegister = true;
                        registerNum = ~registerNum;
                    }
                    processDebugInstruction.ProcessEndLocal(startDebugOffset, in.getCursor() - startDebugOffset,
                            registerNum, isSignedRegister);
                    break;
                }
                case 0x06:
                {
                    int registerNum = in.readUnsignedOrSignedLeb128();
                    boolean isSignedRegister = false;
                    if (registerNum < 0) {
                        isSignedRegister = true;
                        registerNum = ~registerNum;
                    }
                    processDebugInstruction.ProcessRestartLocal(startDebugOffset, in.getCursor() - startDebugOffset,
                            registerNum, isSignedRegister);
                    break;
                }
                case 0x07:
                {
                    processDebugInstruction.ProcessSetPrologueEnd(startDebugOffset);
                    break;
                }
                case 0x08:
                {
                    processDebugInstruction.ProcessSetEpilogueBegin(startDebugOffset);
                    break;
                }
                case 0x09:
                {
                    int nameIndex = in.readUnsignedLeb128();
                    processDebugInstruction.ProcessSetFile(startDebugOffset, in.getCursor() - startDebugOffset,
                            nameIndex);
                    break;
                }
                default:
                {
                    int base = ((debugOpcode & 0xFF) - 0x0A);
                    processDebugInstruction.ProcessSpecialOpcode(startDebugOffset, debugOpcode, (base % 15) - 4, base / 15);
                }
            }
        }
    }

    /**
     * This method decodes the debug instructions in the given byte array and iterates over them, calling
     * the ProcessDebugInstructionDelegate.ProcessDebugInstruction method for each instruction
     * @param debugInfoItem the <code>DebugInfoItem</code> to iterate over
     * @param registerCount the number of registers in the method that the given debug info is for
     * @param processDecodedDebugInstruction a <code>ProcessDebugInstructionDelegate</code> object that gets called
     * for each instruction that is encountered
     */
    public static void DecodeInstructions(DebugInfoItem debugInfoItem, int registerCount,
                                           ProcessDecodedDebugInstructionDelegate processDecodedDebugInstruction) {
        int startDebugOffset;
        int currentCodeAddress = 0;
        int line = debugInfoItem.getLineStart();
        Input in = new ByteArrayInput(debugInfoItem.getEncodedDebugInfo());
        DexFile dexFile = debugInfoItem.getDexFile();

        Local[] locals = new Local[registerCount];

        while(true)
        {
            startDebugOffset = in.getCursor();
            byte debugOpcode = in.readByte();

            switch (DebugOpcode.getDebugOpcodeByValue(debugOpcode)) {
                case DBG_END_SEQUENCE:
                {
                    return;
                }
                case DBG_ADVANCE_PC:
                {
                    int codeAddressDiff = in.readUnsignedLeb128();
                    currentCodeAddress += codeAddressDiff;
                    break;
                }
                case DBG_ADVANCE_LINE:
                {
                    int lineDiff = in.readSignedLeb128();
                    line += lineDiff;
                    break;
                }
                case DBG_START_LOCAL:
                {
                    int registerNum = in.readUnsignedLeb128();
                    StringIdItem name = dexFile.StringIdsSection.getOptionalItemByIndex(in.readUnsignedLeb128() - 1);
                    TypeIdItem type = dexFile.TypeIdsSection.getOptionalItemByIndex(in.readUnsignedLeb128() - 1);
                    locals[registerNum] = new Local(registerNum, name, type, null);
                    processDecodedDebugInstruction.ProcessStartLocal(currentCodeAddress,
                            in.getCursor() - startDebugOffset, registerNum, name, type);
                    break;
                }
                case DBG_START_LOCAL_EXTENDED:
                {
                    int registerNum = in.readUnsignedLeb128();
                    StringIdItem name = dexFile.StringIdsSection.getOptionalItemByIndex(in.readUnsignedLeb128() - 1);
                    TypeIdItem type = dexFile.TypeIdsSection.getOptionalItemByIndex(in.readUnsignedLeb128() - 1);
                    StringIdItem signature =
                            dexFile.StringIdsSection.getOptionalItemByIndex(in.readUnsignedLeb128() - 1);
                    locals[registerNum] = new Local(registerNum, name, type, signature);
                    processDecodedDebugInstruction.ProcessStartLocalExtended(currentCodeAddress,
                            in.getCursor() - startDebugOffset, registerNum, name, type, signature);
                    break;
                }
                case DBG_END_LOCAL:
                {
                    int registerNum = in.readUnsignedLeb128();
                    Local local = locals[registerNum];
                    if (local == null) {
                        processDecodedDebugInstruction.ProcessEndLocal(currentCodeAddress, in.getCursor() - startDebugOffset, registerNum,
                                null, null, null);
                    } else {
                        processDecodedDebugInstruction.ProcessEndLocal(currentCodeAddress, in.getCursor() - startDebugOffset, registerNum,
                                local.name, local.type, local.signature);
                    }
                    break;
                }
                case DBG_RESTART_LOCAL:
                {
                    int registerNum = in.readUnsignedLeb128();
                    Local local = locals[registerNum];
                    if (local == null) {
                        processDecodedDebugInstruction.ProcessRestartLocal(currentCodeAddress, in.getCursor() - startDebugOffset,
                                registerNum, null, null, null);
                    } else {
                        processDecodedDebugInstruction.ProcessRestartLocal(currentCodeAddress, in.getCursor() - startDebugOffset,
                                registerNum, local.name, local.type, local.signature);
                    }

                    break;
                }
                case DBG_SET_PROLOGUE_END:
                {
                    processDecodedDebugInstruction.ProcessSetPrologueEnd(currentCodeAddress);
                    break;
                }
                case DBG_SET_EPILOGUE_BEGIN:
                {
                    processDecodedDebugInstruction.ProcessSetEpilogueBegin(currentCodeAddress);
                    break;
                }
                case DBG_SET_FILE:
                {
                    StringIdItem name = dexFile.StringIdsSection.getOptionalItemByIndex(in.readUnsignedLeb128() - 1);
                    processDecodedDebugInstruction.ProcessSetFile(currentCodeAddress, in.getCursor() - startDebugOffset, name);
                    break;
                }
                case DBG_SPECIAL_OPCODE:
                {
                    int base = ((debugOpcode & 0xFF) - 0x0A);
                    currentCodeAddress += base / 15;
                    line += (base % 15) - 4;
                    processDecodedDebugInstruction.ProcessLineEmit(currentCodeAddress, line);
                }
            }
        }
    }

    public static class ProcessRawDebugInstructionDelegate
    {
        //TODO: add javadocs
        public void ProcessEndSequence(int startDebugOffset) {
            ProcessStaticOpcode(DebugOpcode.DBG_END_SEQUENCE, startDebugOffset, 1);
        }

        public void ProcessAdvancePC(int startDebugOffset, int length, int codeAddressDiff) {
            ProcessStaticOpcode(DebugOpcode.DBG_ADVANCE_PC, startDebugOffset, length);
        }

        public void ProcessAdvanceLine(int startDebugOffset, int length, int lineDiff) {
            ProcessStaticOpcode(DebugOpcode.DBG_ADVANCE_LINE, startDebugOffset, length);
        }

        public void ProcessStartLocal(int startDebugOffset, int length, int registerNum, int nameIndex, int typeIndex,
                                      boolean registerIsSigned) {
        }

        public void ProcessStartLocalExtended(int startDebugOffset, int length, int registerNum, int nameIndex,
                                              int typeIndex,int signatureIndex, boolean registerIsSigned) {
        }

        public void ProcessEndLocal(int startDebugOffset, int length, int registerNum, boolean registerIsSigned) {
            ProcessStaticOpcode(DebugOpcode.DBG_END_LOCAL, startDebugOffset, length);
        }

        public void ProcessRestartLocal(int startDebugOffset, int length, int registerNum, boolean registerIsSigned) {
            ProcessStaticOpcode(DebugOpcode.DBG_RESTART_LOCAL, startDebugOffset, length);
        }

        public void ProcessSetPrologueEnd(int startDebugOffset) {
            ProcessStaticOpcode(DebugOpcode.DBG_SET_PROLOGUE_END, startDebugOffset, 1);
        }

        public void ProcessSetEpilogueBegin(int startDebugOffset) {
            ProcessStaticOpcode(DebugOpcode.DBG_SET_EPILOGUE_BEGIN, startDebugOffset, 1);
        }

        public void ProcessSetFile(int startDebugOffset, int length, int nameIndex) {
        }

        public void ProcessSpecialOpcode(int startDebugOffset, int debugOpcode, int lineDiff, int codeAddressDiff) {
            ProcessStaticOpcode(DebugOpcode.DBG_SPECIAL_OPCODE, startDebugOffset, 1);
        }

        public void ProcessStaticOpcode(DebugOpcode debugOpcode, int startDebugOffset, int length) {
        }
    }

    public static class ProcessDecodedDebugInstructionDelegate
    {
        public void ProcessStartLocal(int codeAddress, int length, int registerNum, StringIdItem name,
                                      TypeIdItem type) {
        }

        public void ProcessStartLocalExtended(int codeAddress, int length, int registerNum, StringIdItem name,
                                              TypeIdItem type, StringIdItem signature) {
        }

        public void ProcessEndLocal(int codeAddress, int length, int registerNum, StringIdItem name, TypeIdItem type,
                                    StringIdItem signature) {
        }

        public void ProcessRestartLocal(int codeAddress, int length, int registerNum, StringIdItem name,
                                        TypeIdItem type, StringIdItem signature) {
        }

        public void ProcessSetPrologueEnd(int codeAddress) {
        }

        public void ProcessSetEpilogueBegin(int codeAddress) {
        }

        public void ProcessSetFile(int codeAddress, int length, StringIdItem name) {
        }

        public void ProcessLineEmit(int codeAddress, int line) {
        }
    }

    private static class Local {
        public final int register;
        public final StringIdItem name;
        public final TypeIdItem type;
        public final StringIdItem signature;
        public Local(int register, StringIdItem name, TypeIdItem type, StringIdItem signature) {
            this.register = register;
            this.name = name;
            this.type = type;
            this.signature = signature;
        }

    }
}
