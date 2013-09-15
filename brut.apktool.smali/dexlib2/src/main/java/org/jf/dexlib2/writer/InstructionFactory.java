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


package org.jf.dexlib2.writer;

import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.SwitchElement;
import org.jf.dexlib2.iface.reference.Reference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public interface InstructionFactory<Ref extends Reference> {
    Instruction makeInstruction10t(@Nonnull Opcode opcode, int codeOffset);
    Instruction makeInstruction10x(@Nonnull Opcode opcode);
    Instruction makeInstruction11n(@Nonnull Opcode opcode, int registerA, int literal);
    Instruction makeInstruction11x(@Nonnull Opcode opcode, int registerA);
    Instruction makeInstruction12x(@Nonnull Opcode opcode, int registerA, int registerB);
    Instruction makeInstruction20bc(@Nonnull Opcode opcode, int verificationError, @Nonnull Ref reference);
    Instruction makeInstruction20t(@Nonnull Opcode opcode, int codeOffset);
    Instruction makeInstruction21c(@Nonnull Opcode opcode, int registerA, @Nonnull Ref reference);
    Instruction makeInstruction21ih(@Nonnull Opcode opcode, int registerA, int literal);
    Instruction makeInstruction21lh(@Nonnull Opcode opcode, int registerA, long literal);
    Instruction makeInstruction21s(@Nonnull Opcode opcode, int registerA, int literal);
    Instruction makeInstruction21t(@Nonnull Opcode opcode, int registerA, int codeOffset);
    Instruction makeInstruction22b(@Nonnull Opcode opcode, int registerA, int registerB, int literal);
    Instruction makeInstruction22c(@Nonnull Opcode opcode, int registerA, int registerB, @Nonnull Ref reference);
    Instruction makeInstruction22s(@Nonnull Opcode opcode, int registerA, int registerB, int literal);
    Instruction makeInstruction22t(@Nonnull Opcode opcode, int registerA, int registerB, int codeOffset);
    Instruction makeInstruction22x(@Nonnull Opcode opcode, int registerA, int registerB);
    Instruction makeInstruction23x(@Nonnull Opcode opcode, int registerA, int registerB, int registerC);
    Instruction makeInstruction30t(@Nonnull Opcode opcode, int codeOffset);
    Instruction makeInstruction31c(@Nonnull Opcode opcode, int registerA, @Nonnull Ref reference);
    Instruction makeInstruction31i(@Nonnull Opcode opcode, int registerA, int literal);
    Instruction makeInstruction31t(@Nonnull Opcode opcode, int registerA, int codeOffset);
    Instruction makeInstruction32x(@Nonnull Opcode opcode, int registerA, int registerB);
    Instruction makeInstruction35c(@Nonnull Opcode opcode, int registerCount, int registerC, int registerD, int registerE,
                            int registerF, int registerG, @Nonnull Ref reference);
    Instruction makeInstruction3rc(@Nonnull Opcode opcode,  int startRegister, int registerCount,
                            @Nonnull Ref reference);
    Instruction makeInstruction51l(@Nonnull Opcode opcode, int registerA, long literal);
    Instruction makeSparseSwitchPayload(@Nullable List<? extends SwitchElement> switchElements);
    Instruction makePackedSwitchPayload(@Nullable List<? extends SwitchElement> switchElements);
    Instruction makeArrayPayload(int elementWidth, @Nullable List<Number> arrayElements);
}
