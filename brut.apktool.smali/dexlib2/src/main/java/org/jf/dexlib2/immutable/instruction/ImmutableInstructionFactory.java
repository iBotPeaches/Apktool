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

package org.jf.dexlib2.immutable.instruction;

import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.iface.instruction.SwitchElement;
import org.jf.dexlib2.iface.reference.Reference;
import org.jf.dexlib2.writer.InstructionFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class ImmutableInstructionFactory implements InstructionFactory<Reference> {
    public static final ImmutableInstructionFactory INSTANCE = new ImmutableInstructionFactory();

    private ImmutableInstructionFactory() {
    }

    public ImmutableInstruction10t makeInstruction10t(@Nonnull Opcode opcode,
                                                      int codeOffset) {
        return new ImmutableInstruction10t(opcode, codeOffset);
    }

    public ImmutableInstruction10x makeInstruction10x(@Nonnull Opcode opcode) {
        return new ImmutableInstruction10x(opcode);
    }

    public ImmutableInstruction11n makeInstruction11n(@Nonnull Opcode opcode,
                                                      int registerA,
                                                      int literal) {
        return new ImmutableInstruction11n(opcode, registerA, literal);
    }

    public ImmutableInstruction11x makeInstruction11x(@Nonnull Opcode opcode,
                                                      int registerA) {
        return new ImmutableInstruction11x(opcode, registerA);
    }

    public ImmutableInstruction12x makeInstruction12x(@Nonnull Opcode opcode,
                                                      int registerA,
                                                      int registerB) {
        return new ImmutableInstruction12x(opcode, registerA, registerB);
    }

    public ImmutableInstruction20bc makeInstruction20bc(@Nonnull Opcode opcode,
                                                        int verificationError,
                                                        @Nonnull Reference reference) {
        return new ImmutableInstruction20bc(opcode, verificationError, reference);
    }

    public ImmutableInstruction20t makeInstruction20t(@Nonnull Opcode opcode,
                                                      int codeOffset) {
        return new ImmutableInstruction20t(opcode, codeOffset);
    }

    public ImmutableInstruction21c makeInstruction21c(@Nonnull Opcode opcode,
                                                      int registerA,
                                                      @Nonnull Reference reference) {
        return new ImmutableInstruction21c(opcode, registerA, reference);
    }

    public ImmutableInstruction21ih makeInstruction21ih(@Nonnull Opcode opcode,
                                                        int registerA,
                                                        int literal) {
        return new ImmutableInstruction21ih(opcode, registerA, literal);
    }

    public ImmutableInstruction21lh makeInstruction21lh(@Nonnull Opcode opcode,
                                                        int registerA,
                                                        long literal) {
        return new ImmutableInstruction21lh(opcode, registerA, literal);
    }

    public ImmutableInstruction21s makeInstruction21s(@Nonnull Opcode opcode,
                                                      int registerA,
                                                      int literal) {
        return new ImmutableInstruction21s(opcode, registerA, literal);
    }

    public ImmutableInstruction21t makeInstruction21t(@Nonnull Opcode opcode,
                                                      int registerA,
                                                      int codeOffset) {
        return new ImmutableInstruction21t(opcode, registerA, codeOffset);
    }

    public ImmutableInstruction22b makeInstruction22b(@Nonnull Opcode opcode,
                                                      int registerA,
                                                      int registerB,
                                                      int literal) {
        return new ImmutableInstruction22b(opcode, registerA, registerB, literal);
    }

    public ImmutableInstruction22c makeInstruction22c(@Nonnull Opcode opcode,
                                                      int registerA,
                                                      int registerB,
                                                      @Nonnull Reference reference) {
        return new ImmutableInstruction22c(opcode, registerA, registerB, reference);
    }

    public ImmutableInstruction22s makeInstruction22s(@Nonnull Opcode opcode,
                                                      int registerA,
                                                      int registerB,
                                                      int literal) {
        return new ImmutableInstruction22s(opcode, registerA, registerB, literal);
    }

    public ImmutableInstruction22t makeInstruction22t(@Nonnull Opcode opcode,
                                                      int registerA,
                                                      int registerB,
                                                      int codeOffset) {
        return new ImmutableInstruction22t(opcode, registerA, registerB, codeOffset);
    }

    public ImmutableInstruction22x makeInstruction22x(@Nonnull Opcode opcode,
                                                      int registerA,
                                                      int registerB) {
        return new ImmutableInstruction22x(opcode, registerA, registerB);
    }

    public ImmutableInstruction23x makeInstruction23x(@Nonnull Opcode opcode,
                                                      int registerA,
                                                      int registerB,
                                                      int registerC) {
        return new ImmutableInstruction23x(opcode, registerA, registerB, registerC);
    }

    public ImmutableInstruction30t makeInstruction30t(@Nonnull Opcode opcode,
                                                      int codeOffset) {
        return new ImmutableInstruction30t(opcode, codeOffset);
    }

    public ImmutableInstruction31c makeInstruction31c(@Nonnull Opcode opcode,
                                                      int registerA,
                                                      @Nonnull Reference reference) {
        return new ImmutableInstruction31c(opcode, registerA, reference);
    }

    public ImmutableInstruction31i makeInstruction31i(@Nonnull Opcode opcode,
                                                      int registerA,
                                                      int literal) {
        return new ImmutableInstruction31i(opcode, registerA, literal);
    }

    public ImmutableInstruction31t makeInstruction31t(@Nonnull Opcode opcode,
                                                      int registerA,
                                                      int codeOffset) {
        return new ImmutableInstruction31t(opcode, registerA, codeOffset);
    }

    public ImmutableInstruction32x makeInstruction32x(@Nonnull Opcode opcode,
                                                      int registerA,
                                                      int registerB) {
        return new ImmutableInstruction32x(opcode, registerA, registerB);
    }

    public ImmutableInstruction35c makeInstruction35c(@Nonnull Opcode opcode,
                                                      int registerCount,
                                                      int registerC,
                                                      int registerD,
                                                      int registerE,
                                                      int registerF,
                                                      int registerG,
                                                      @Nonnull Reference reference) {
        return new ImmutableInstruction35c(opcode, registerCount, registerC, registerD, registerE, registerF, registerG,
                reference);
    }

    public ImmutableInstruction3rc makeInstruction3rc(@Nonnull Opcode opcode,
                                                      int startRegister,
                                                      int registerCount,
                                                      @Nonnull Reference reference) {
        return new ImmutableInstruction3rc(opcode, startRegister, registerCount, reference);
    }

    public ImmutableInstruction51l makeInstruction51l(@Nonnull Opcode opcode,
                                                      int registerA,
                                                      long literal) {
        return new ImmutableInstruction51l(opcode, registerA, literal);
    }

    public ImmutableSparseSwitchPayload makeSparseSwitchPayload(@Nullable List<? extends SwitchElement> switchElements) {
        return new ImmutableSparseSwitchPayload(switchElements);
    }

    public ImmutablePackedSwitchPayload makePackedSwitchPayload(@Nullable List<? extends SwitchElement> switchElements) {
        return new ImmutablePackedSwitchPayload(switchElements);
    }

    public ImmutableArrayPayload makeArrayPayload(int elementWidth,
                                                @Nullable List<Number> arrayElements) {
        return new ImmutableArrayPayload(elementWidth, arrayElements);
    }
}
