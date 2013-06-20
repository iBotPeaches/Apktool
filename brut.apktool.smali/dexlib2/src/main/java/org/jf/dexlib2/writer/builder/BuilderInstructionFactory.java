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

package org.jf.dexlib2.writer.builder;

import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.iface.instruction.SwitchElement;
import org.jf.dexlib2.writer.InstructionFactory;
import org.jf.dexlib2.writer.builder.BuilderInstruction.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class BuilderInstructionFactory implements InstructionFactory<BuilderInstruction, BuilderReference> {
    public static final BuilderInstructionFactory INSTANCE = new BuilderInstructionFactory();

    private BuilderInstructionFactory() {
    }

    public BuilderInstruction10t makeInstruction10t(@Nonnull Opcode opcode,
                                                    int codeOffset) {
        return new BuilderInstruction10t(opcode, codeOffset);
    }

    public BuilderInstruction10x makeInstruction10x(@Nonnull Opcode opcode) {
        return new BuilderInstruction10x(opcode);
    }

    public BuilderInstruction11n makeInstruction11n(@Nonnull Opcode opcode,
                                                      int registerA,
                                                      int literal) {
        return new BuilderInstruction11n(opcode, registerA, literal);
    }

    public BuilderInstruction11x makeInstruction11x(@Nonnull Opcode opcode,
                                                      int registerA) {
        return new BuilderInstruction11x(opcode, registerA);
    }

    public BuilderInstruction12x makeInstruction12x(@Nonnull Opcode opcode,
                                                      int registerA,
                                                      int registerB) {
        return new BuilderInstruction12x(opcode, registerA, registerB);
    }

    public BuilderInstruction20bc makeInstruction20bc(@Nonnull Opcode opcode,
                                                        int verificationError,
                                                        @Nonnull BuilderReference reference) {
        return new BuilderInstruction20bc(opcode, verificationError, reference);
    }

    public BuilderInstruction20t makeInstruction20t(@Nonnull Opcode opcode,
                                                      int codeOffset) {
        return new BuilderInstruction20t(opcode, codeOffset);
    }

    public BuilderInstruction21c makeInstruction21c(@Nonnull Opcode opcode,
                                                      int registerA,
                                                      @Nonnull BuilderReference reference) {
        return new BuilderInstruction21c(opcode, registerA, reference);
    }

    public BuilderInstruction21ih makeInstruction21ih(@Nonnull Opcode opcode,
                                                        int registerA,
                                                        int literal) {
        return new BuilderInstruction21ih(opcode, registerA, literal);
    }

    public BuilderInstruction21lh makeInstruction21lh(@Nonnull Opcode opcode,
                                                        int registerA,
                                                        long literal) {
        return new BuilderInstruction21lh(opcode, registerA, literal);
    }

    public BuilderInstruction21s makeInstruction21s(@Nonnull Opcode opcode,
                                                      int registerA,
                                                      int literal) {
        return new BuilderInstruction21s(opcode, registerA, literal);
    }

    public BuilderInstruction21t makeInstruction21t(@Nonnull Opcode opcode,
                                                      int registerA,
                                                      int codeOffset) {
        return new BuilderInstruction21t(opcode, registerA, codeOffset);
    }

    public BuilderInstruction22b makeInstruction22b(@Nonnull Opcode opcode,
                                                      int registerA,
                                                      int registerB,
                                                      int literal) {
        return new BuilderInstruction22b(opcode, registerA, registerB, literal);
    }

    public BuilderInstruction22c makeInstruction22c(@Nonnull Opcode opcode,
                                                      int registerA,
                                                      int registerB,
                                                      @Nonnull BuilderReference reference) {
        return new BuilderInstruction22c(opcode, registerA, registerB, reference);
    }

    public BuilderInstruction22s makeInstruction22s(@Nonnull Opcode opcode,
                                                      int registerA,
                                                      int registerB,
                                                      int literal) {
        return new BuilderInstruction22s(opcode, registerA, registerB, literal);
    }

    public BuilderInstruction22t makeInstruction22t(@Nonnull Opcode opcode,
                                                      int registerA,
                                                      int registerB,
                                                      int codeOffset) {
        return new BuilderInstruction22t(opcode, registerA, registerB, codeOffset);
    }

    public BuilderInstruction22x makeInstruction22x(@Nonnull Opcode opcode,
                                                      int registerA,
                                                      int registerB) {
        return new BuilderInstruction22x(opcode, registerA, registerB);
    }

    public BuilderInstruction23x makeInstruction23x(@Nonnull Opcode opcode,
                                                      int registerA,
                                                      int registerB,
                                                      int registerC) {
        return new BuilderInstruction23x(opcode, registerA, registerB, registerC);
    }

    public BuilderInstruction30t makeInstruction30t(@Nonnull Opcode opcode,
                                                      int codeOffset) {
        return new BuilderInstruction30t(opcode, codeOffset);
    }

    public BuilderInstruction31c makeInstruction31c(@Nonnull Opcode opcode,
                                                      int registerA,
                                                      @Nonnull BuilderReference reference) {
        return new BuilderInstruction31c(opcode, registerA, reference);
    }

    public BuilderInstruction31i makeInstruction31i(@Nonnull Opcode opcode,
                                                      int registerA,
                                                      int literal) {
        return new BuilderInstruction31i(opcode, registerA, literal);
    }

    public BuilderInstruction31t makeInstruction31t(@Nonnull Opcode opcode,
                                                      int registerA,
                                                      int codeOffset) {
        return new BuilderInstruction31t(opcode, registerA, codeOffset);
    }

    public BuilderInstruction32x makeInstruction32x(@Nonnull Opcode opcode,
                                                      int registerA,
                                                      int registerB) {
        return new BuilderInstruction32x(opcode, registerA, registerB);
    }

    public BuilderInstruction35c makeInstruction35c(@Nonnull Opcode opcode,
                                                      int registerCount,
                                                      int registerC,
                                                      int registerD,
                                                      int registerE,
                                                      int registerF,
                                                      int registerG,
                                                      @Nonnull BuilderReference reference) {
        return new BuilderInstruction35c(opcode, registerCount, registerC, registerD, registerE, registerF, registerG,
                reference);
    }

    public BuilderInstruction3rc makeInstruction3rc(@Nonnull Opcode opcode,
                                                      int startRegister,
                                                      int registerCount,
                                                      @Nonnull BuilderReference reference) {
        return new BuilderInstruction3rc(opcode, startRegister, registerCount, reference);
    }

    public BuilderInstruction51l makeInstruction51l(@Nonnull Opcode opcode,
                                                    int registerA,
                                                    long literal) {
        return new BuilderInstruction51l(opcode, registerA, literal);
    }

    public BuilderSparseSwitchPayload makeSparseSwitchPayload(@Nullable List<? extends SwitchElement> switchElements) {
        return new BuilderSparseSwitchPayload(switchElements);
    }

    public BuilderPackedSwitchPayload makePackedSwitchPayload(@Nullable List<? extends SwitchElement> switchElements) {
        return new BuilderPackedSwitchPayload(switchElements);
    }

    public BuilderArrayPayload makeArrayPayload(int elementWidth,
                                                @Nullable List<Number> arrayElements) {
        return new BuilderArrayPayload(elementWidth, arrayElements);
    }
}
