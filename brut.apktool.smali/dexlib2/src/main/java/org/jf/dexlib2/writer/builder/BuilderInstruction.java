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

import com.google.common.collect.ImmutableList;
import org.jf.dexlib2.Format;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.SwitchElement;
import org.jf.dexlib2.iface.instruction.formats.*;
import org.jf.dexlib2.immutable.instruction.*;
import org.jf.dexlib2.util.Preconditions;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public interface BuilderInstruction extends Instruction {
    static abstract class BaseBuilderInstruction implements BuilderInstruction {
        @Nonnull protected final Opcode opcode;

        public BaseBuilderInstruction(@Nonnull Opcode opcode) {
            this.opcode = opcode;
        }

        @Nonnull public abstract Format getFormat();

        @Nonnull @Override public Opcode getOpcode() {
            return opcode;
        }

        @Override public int getCodeUnits() {
            return getFormat().size/2;
        }
    }

    public static class BuilderInstruction10t extends ImmutableInstruction10t implements BuilderInstruction {
        public BuilderInstruction10t(@Nonnull Opcode opcode, int codeOffset) {
            super(opcode, codeOffset);
        }
    }

    public static class BuilderInstruction10x extends ImmutableInstruction10x implements BuilderInstruction {
        public BuilderInstruction10x(@Nonnull Opcode opcode) {
            super(opcode);
        }
    }

    public static class BuilderInstruction11n extends ImmutableInstruction11n implements BuilderInstruction {
        public BuilderInstruction11n(@Nonnull Opcode opcode, int registerA, int literal) {
            super(opcode, registerA, literal);
        }
    }

    public static class BuilderInstruction11x extends ImmutableInstruction11x implements BuilderInstruction {
        public BuilderInstruction11x(@Nonnull Opcode opcode, int registerA) {
            super(opcode, registerA);
        }
    }

    public static class BuilderInstruction12x extends ImmutableInstruction12x implements BuilderInstruction {
        public BuilderInstruction12x(@Nonnull Opcode opcode, int registerA, int registerB) {
            super(opcode, registerA, registerB);
        }
    }

    public static class BuilderInstruction20bc extends BaseBuilderInstruction implements Instruction20bc {
        public static final Format FORMAT = Format.Format20bc;

        protected final int verificationError;
        @Nonnull protected final BuilderReference reference;

        public BuilderInstruction20bc(@Nonnull Opcode opcode,
                                      int verificationError,
                                      @Nonnull BuilderReference reference) {
            super(opcode);
            Preconditions.checkFormat(opcode, FORMAT);
            this.verificationError = Preconditions.checkVerificationError(verificationError);
            this.reference = Preconditions.checkReference(opcode.referenceType, reference);
        }

        @Override public int getVerificationError() { return verificationError; }
        @Nonnull @Override public BuilderReference getReference() { return reference; }

        @Nonnull @Override public Format getFormat() { return FORMAT; }
    }

    public static class BuilderInstruction20t extends ImmutableInstruction20t implements BuilderInstruction {
        public BuilderInstruction20t(@Nonnull Opcode opcode, int codeOffset) {
            super(opcode, codeOffset);
        }
    }

    public static class BuilderInstruction21c extends BaseBuilderInstruction implements Instruction21c {
        public static final Format FORMAT = Format.Format21c;

        protected final int registerA;
        @Nonnull protected final BuilderReference reference;

        public BuilderInstruction21c(@Nonnull Opcode opcode,
                                     int registerA,
                                     @Nonnull BuilderReference reference) {
            super(opcode);
            Preconditions.checkFormat(opcode, FORMAT);
            this.registerA = Preconditions.checkByteRegister(registerA);
            this.reference = Preconditions.checkReference(opcode.referenceType, reference);
        }

        @Override public int getRegisterA() { return registerA; }
        @Nonnull @Override public BuilderReference getReference() { return reference; }

        @Nonnull @Override public Format getFormat() { return FORMAT; }
    }

    public static class BuilderInstruction21ih extends ImmutableInstruction21ih implements BuilderInstruction {
        public BuilderInstruction21ih(@Nonnull Opcode opcode, int registerA, int literal) {
            super(opcode, registerA, literal);
        }
    }

    public static class BuilderInstruction21lh extends ImmutableInstruction21lh implements BuilderInstruction {
        public BuilderInstruction21lh(@Nonnull Opcode opcode, int registerA, long literal) {
            super(opcode, registerA, literal);
        }
    }

    public static class BuilderInstruction21s extends ImmutableInstruction21s implements BuilderInstruction {
        public BuilderInstruction21s(@Nonnull Opcode opcode, int registerA, int literal) {
            super(opcode, registerA, literal);
        }
    }

    public static class BuilderInstruction21t extends ImmutableInstruction21t implements BuilderInstruction {
        public BuilderInstruction21t(@Nonnull Opcode opcode, int registerA, int codeOffset) {
            super(opcode, registerA, codeOffset);
        }
    }

    public static class BuilderInstruction22b extends ImmutableInstruction22b implements BuilderInstruction {
        public BuilderInstruction22b(@Nonnull Opcode opcode, int registerA, int registerB, int literal) {
            super(opcode, registerA, registerB, literal);
        }
    }

    public static class BuilderInstruction22c extends BaseBuilderInstruction implements Instruction22c {
        public static final Format FORMAT = Format.Format22c;

        protected final int registerA;
        protected final int registerB;
        @Nonnull protected final BuilderReference reference;

        public BuilderInstruction22c(@Nonnull Opcode opcode,
                                     int registerA,
                                     int registerB,
                                     @Nonnull BuilderReference reference) {
            super(opcode);
            Preconditions.checkFormat(opcode, FORMAT);
            this.registerA = Preconditions.checkNibbleRegister(registerA);
            this.registerB = Preconditions.checkNibbleRegister(registerB);
            this.reference = Preconditions.checkReference(opcode.referenceType, reference);
        }

        @Override public int getRegisterA() { return registerA; }
        @Override public int getRegisterB() { return registerB; }
        @Nonnull @Override public BuilderReference getReference() { return reference; }

        @Nonnull @Override public Format getFormat() { return FORMAT; }
    }

    public static class BuilderInstruction22s extends ImmutableInstruction22s implements BuilderInstruction {
        public BuilderInstruction22s(@Nonnull Opcode opcode, int registerA, int registerB, int literal) {
            super(opcode, registerA, registerB, literal);
        }
    }

    public static class BuilderInstruction22t extends ImmutableInstruction22t implements BuilderInstruction {
        public BuilderInstruction22t(@Nonnull Opcode opcode, int registerA, int registerB, int codeOffset) {
            super(opcode, registerA, registerB, codeOffset);
        }
    }

    public static class BuilderInstruction22x extends ImmutableInstruction22x implements BuilderInstruction {
        public BuilderInstruction22x(@Nonnull Opcode opcode, int registerA, int registerB) {
            super(opcode, registerA, registerB);
        }
    }

    public static class BuilderInstruction23x extends ImmutableInstruction23x implements BuilderInstruction {
        public BuilderInstruction23x(@Nonnull Opcode opcode, int registerA, int registerB, int registerC) {
            super(opcode, registerA, registerB, registerC);
        }
    }

    public static class BuilderInstruction30t extends ImmutableInstruction30t implements BuilderInstruction {
        public BuilderInstruction30t(@Nonnull Opcode opcode, int codeOffset) {
            super(opcode, codeOffset);
        }
    }

    public static class BuilderInstruction31c extends BaseBuilderInstruction implements Instruction31c {
        public static final Format FORMAT = Format.Format31c;

        protected final int registerA;
        @Nonnull protected final BuilderReference reference;

        public BuilderInstruction31c(@Nonnull Opcode opcode,
                                     int registerA,
                                     @Nonnull BuilderReference reference) {
            super(opcode);
            Preconditions.checkFormat(opcode, FORMAT);
            this.registerA = Preconditions.checkByteRegister(registerA);
            this.reference = Preconditions.checkReference(opcode.referenceType, reference);
        }

        @Override public int getRegisterA() { return registerA; }
        @Nonnull @Override public BuilderReference getReference() { return reference; }

        @Nonnull @Override public Format getFormat() { return FORMAT; }
    }

    public static class BuilderInstruction31i extends ImmutableInstruction31i implements BuilderInstruction {
        public BuilderInstruction31i(@Nonnull Opcode opcode, int registerA, int literal) {
            super(opcode, registerA, literal);
        }
    }

    public static class BuilderInstruction31t extends ImmutableInstruction31t implements BuilderInstruction {
        public BuilderInstruction31t(@Nonnull Opcode opcode, int registerA, int codeOffset) {
            super(opcode, registerA, codeOffset);
        }
    }

    public static class BuilderInstruction32x extends ImmutableInstruction32x implements BuilderInstruction {
        public BuilderInstruction32x(@Nonnull Opcode opcode, int registerA, int registerB) {
            super(opcode, registerA, registerB);
        }
    }

    public static class BuilderInstruction35c extends BaseBuilderInstruction implements Instruction35c {
        public static final Format FORMAT = Format.Format35c;

        protected final int registerCount;
        protected final int registerC;
        protected final int registerD;
        protected final int registerE;
        protected final int registerF;
        protected final int registerG;
        @Nonnull protected final BuilderReference reference;

        public BuilderInstruction35c(@Nonnull Opcode opcode,
                                     int registerCount,
                                     int registerC,
                                     int registerD,
                                     int registerE,
                                     int registerF,
                                     int registerG,
                                     @Nonnull BuilderReference reference) {
            super(opcode);
            Preconditions.checkFormat(opcode, FORMAT);
            this.registerCount = Preconditions.check35cRegisterCount(registerCount);
            this.registerC = (registerCount>0) ? Preconditions.checkNibbleRegister(registerC) : 0;
            this.registerD = (registerCount>1) ? Preconditions.checkNibbleRegister(registerD) : 0;
            this.registerE = (registerCount>2) ? Preconditions.checkNibbleRegister(registerE) : 0;
            this.registerF = (registerCount>3) ? Preconditions.checkNibbleRegister(registerF) : 0;
            this.registerG = (registerCount>4) ? Preconditions.checkNibbleRegister(registerG) : 0;
            this.reference = Preconditions.checkReference(opcode.referenceType, reference);
        }

        @Override public int getRegisterCount() { return registerCount; }
        @Override public int getRegisterC() { return registerC; }
        @Override public int getRegisterD() { return registerD; }
        @Override public int getRegisterE() { return registerE; }
        @Override public int getRegisterF() { return registerF; }
        @Override public int getRegisterG() { return registerG; }
        @Nonnull @Override public BuilderReference getReference() { return reference; }

        @Nonnull @Override public Format getFormat() { return FORMAT; }
    }

    public static class BuilderInstruction3rc extends BaseBuilderInstruction implements Instruction3rc {
        public static final Format FORMAT = Format.Format3rc;

        private final int startRegister;
        private final int registerCount;

        @Nonnull protected final BuilderReference reference;

        public BuilderInstruction3rc(@Nonnull Opcode opcode,
                                     int startRegister,
                                     int registerCount,
                                     @Nonnull BuilderReference reference) {
            super(opcode);

            Preconditions.checkFormat(opcode, FORMAT);
            this.startRegister = Preconditions.checkShortRegister(startRegister);
            this.registerCount = Preconditions.checkRegisterRangeCount(registerCount);
            this.reference = Preconditions.checkReference(opcode.referenceType, reference);
        }

        @Nonnull @Override public BuilderReference getReference() {
            return reference;
        }

        @Override public int getStartRegister() {
            return startRegister;
        }

        @Override public int getRegisterCount() {
            return registerCount;
        }

        @Nonnull @Override public Format getFormat() {
            return FORMAT;
        }
    }

    public static class BuilderInstruction51l extends ImmutableInstruction51l implements BuilderInstruction {
        public BuilderInstruction51l(@Nonnull Opcode opcode, int registerA, long literal) {
            super(opcode, registerA, literal);
        }
    }

    public static class BuilderArrayPayload extends BaseBuilderInstruction implements ArrayPayload {
        public static final Format FORMAT = Format.ArrayPayload;
        private final int elementWidth;
        @Nonnull private final List<Number> arrayElements;

        public BuilderArrayPayload(int elementWidth, @Nullable List<Number> arrayElements) {
            super(Opcode.ARRAY_PAYLOAD);
            this.elementWidth = elementWidth;
            if (arrayElements == null) {
                arrayElements = ImmutableList.of();
            }
            this.arrayElements = arrayElements;
        }

        @Override public int getElementWidth() {
            return elementWidth;
        }

        @Nonnull @Override public List<Number> getArrayElements() {
            return arrayElements;
        }

        @Nonnull @Override public Format getFormat() {
            return FORMAT;
        }

        @Override public int getCodeUnits() {
            return 4 + (elementWidth * arrayElements.size() + 1) / 2;
        }
    }

    public static class BuilderPackedSwitchPayload extends BaseBuilderInstruction implements PackedSwitchPayload {
        public static final Format FORMAT = Format.PackedSwitchPayload;
        @Nonnull private final List<? extends SwitchElement> elements;

        public BuilderPackedSwitchPayload(@Nullable List<? extends SwitchElement> switchElements) {
            super(Opcode.PACKED_SWITCH_PAYLOAD);
            if (switchElements == null) {
                switchElements = ImmutableList.of();
            }
            this.elements = switchElements;
        }

        @Nonnull @Override public List<? extends SwitchElement> getSwitchElements() {
            return elements;
        }

        @Nonnull @Override public Format getFormat() {
            return FORMAT;
        }

        @Override public int getCodeUnits() {
            return 4 + elements.size() * 2;
        }
    }

    public static class BuilderSparseSwitchPayload extends BaseBuilderInstruction implements SparseSwitchPayload {
        public static final Format FORMAT = Format.SparseSwitchPayload;
        @Nonnull private final List<? extends SwitchElement> elements;

        public BuilderSparseSwitchPayload(@Nullable List<? extends SwitchElement> switchElements) {
            super(Opcode.SPARSE_SWITCH_PAYLOAD);
            if (switchElements == null) {
                switchElements = ImmutableList.of();
            }
            this.elements = switchElements;
        }

        @Nonnull @Override public List<? extends SwitchElement> getSwitchElements() {
            return elements;
        }

        @Nonnull @Override public Format getFormat() {
            return FORMAT;
        }

        @Override public int getCodeUnits() {
            return 2 + elements.size() * 4;
        }
    }
}
