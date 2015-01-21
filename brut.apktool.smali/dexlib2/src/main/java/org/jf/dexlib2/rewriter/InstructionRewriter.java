/*
 * Copyright 2014, Google Inc.
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

package org.jf.dexlib2.rewriter;

import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.ReferenceType;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.ReferenceInstruction;
import org.jf.dexlib2.iface.instruction.formats.*;
import org.jf.dexlib2.iface.reference.FieldReference;
import org.jf.dexlib2.iface.reference.MethodReference;
import org.jf.dexlib2.iface.reference.Reference;
import org.jf.dexlib2.iface.reference.TypeReference;

import javax.annotation.Nonnull;

public class InstructionRewriter implements Rewriter<Instruction> {
    @Nonnull protected final Rewriters rewriters;

    public InstructionRewriter(@Nonnull Rewriters rewriters) {
        this.rewriters = rewriters;
    }

    @Nonnull @Override public Instruction rewrite(@Nonnull Instruction instruction) {
        if (instruction instanceof ReferenceInstruction) {
            switch (instruction.getOpcode().format) {
                case Format20bc:
                    return new RewrittenInstruction20bc((Instruction20bc)instruction);
                case Format21c:
                    return new RewrittenInstruction21c((Instruction21c)instruction);
                case Format22c:
                    return new RewrittenInstruction22c((Instruction22c)instruction);
                case Format31c:
                    return new RewrittenInstruction31c((Instruction31c)instruction);
                case Format35c:
                    return new RewrittenInstruction35c((Instruction35c)instruction);
                case Format3rc:
                    return new RewrittenInstruction3rc((Instruction3rc)instruction);
                default:
                    throw new IllegalArgumentException();
            }
        }
        return instruction;
    }

    protected class BaseRewrittenReferenceInstruction<T extends ReferenceInstruction>
            implements ReferenceInstruction {
        @Nonnull protected T instruction;

        protected BaseRewrittenReferenceInstruction(@Nonnull T instruction) {
            this.instruction = instruction;
        }

        @Override @Nonnull public Reference getReference() {
            switch (getReferenceType()) {
                case ReferenceType.TYPE:
                    return RewriterUtils.rewriteTypeReference(rewriters.getTypeRewriter(),
                            (TypeReference)instruction.getReference());
                case ReferenceType.FIELD:
                    return rewriters.getFieldReferenceRewriter().rewrite((FieldReference)instruction.getReference());
                case ReferenceType.METHOD:
                    return rewriters.getMethodReferenceRewriter().rewrite((MethodReference)instruction.getReference());
                case ReferenceType.STRING:
                    return instruction.getReference();
                default:
                    throw new IllegalArgumentException();
            }
        }

        @Override public int getReferenceType() {
            return instruction.getReferenceType();
        }

        @Override public Opcode getOpcode() {
            return instruction.getOpcode();
        }

        @Override public int getCodeUnits() {
            return instruction.getCodeUnits();
        }
    }

    protected class RewrittenInstruction20bc extends BaseRewrittenReferenceInstruction<Instruction20bc>
            implements Instruction20bc {
        public RewrittenInstruction20bc(@Nonnull Instruction20bc instruction) {
            super(instruction);
        }

        @Override public int getVerificationError() {
            return instruction.getVerificationError();
        }
    }

    protected class RewrittenInstruction21c extends BaseRewrittenReferenceInstruction<Instruction21c>
            implements Instruction21c {
        public RewrittenInstruction21c(@Nonnull Instruction21c instruction) {
            super(instruction);
        }

        public int getRegisterA() {
            return instruction.getRegisterA();
        }
    }

    protected class RewrittenInstruction22c extends BaseRewrittenReferenceInstruction<Instruction22c>
            implements Instruction22c {
        public RewrittenInstruction22c(@Nonnull Instruction22c instruction) {
            super(instruction);
        }

        public int getRegisterA() {
            return instruction.getRegisterA();
        }

        public int getRegisterB() {
            return instruction.getRegisterB();
        }
    }

    protected class RewrittenInstruction31c extends BaseRewrittenReferenceInstruction<Instruction31c>
            implements Instruction31c {
        public RewrittenInstruction31c(@Nonnull Instruction31c instruction) {
            super(instruction);
        }

        public int getRegisterA() {
            return instruction.getRegisterA();
        }
    }

    protected class RewrittenInstruction35c extends BaseRewrittenReferenceInstruction<Instruction35c>
            implements Instruction35c {
        public RewrittenInstruction35c(@Nonnull Instruction35c instruction) {
            super(instruction);
        }

        public int getRegisterC() {
            return instruction.getRegisterC();
        }

        public int getRegisterE() {
            return instruction.getRegisterE();
        }

        public int getRegisterG() {
            return instruction.getRegisterG();
        }

        public int getRegisterCount() {
            return instruction.getRegisterCount();
        }

        public int getRegisterD() {
            return instruction.getRegisterD();
        }

        public int getRegisterF() {
            return instruction.getRegisterF();
        }
    }

    protected class RewrittenInstruction3rc extends BaseRewrittenReferenceInstruction<Instruction3rc>
            implements Instruction3rc {
        public RewrittenInstruction3rc(@Nonnull Instruction3rc instruction) {
            super(instruction);
        }

        public int getStartRegister() {
            return instruction.getStartRegister();
        }

        public int getRegisterCount() {
            return instruction.getRegisterCount();
        }
    }
}
