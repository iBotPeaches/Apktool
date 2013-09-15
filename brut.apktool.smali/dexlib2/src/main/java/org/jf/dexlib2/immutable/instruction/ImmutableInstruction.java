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

package org.jf.dexlib2.immutable.instruction;

import com.google.common.collect.ImmutableList;
import org.jf.dexlib2.Format;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.formats.*;
import org.jf.dexlib2.util.Preconditions;
import org.jf.util.ImmutableConverter;

import javax.annotation.Nonnull;

public abstract class ImmutableInstruction implements Instruction {
    @Nonnull protected final Opcode opcode;

    protected ImmutableInstruction(@Nonnull Opcode opcode) {
        Preconditions.checkFormat(opcode, getFormat());
        this.opcode = opcode;
    }

    @Nonnull
    public static ImmutableInstruction of(Instruction instruction) {
        if (instruction instanceof ImmutableInstruction) {
            return (ImmutableInstruction)instruction;
        }

        switch (instruction.getOpcode().format) {
            case Format10t:
                return ImmutableInstruction10t.of((Instruction10t)instruction);
            case Format10x:
                if (instruction instanceof UnknownInstruction) {
                    return ImmutableUnknownInstruction.of((UnknownInstruction)instruction);
                }
                return ImmutableInstruction10x.of((Instruction10x)instruction);
            case Format11n:
                return ImmutableInstruction11n.of((Instruction11n)instruction);
            case Format11x:
                return ImmutableInstruction11x.of((Instruction11x)instruction);
            case Format12x:
                return ImmutableInstruction12x.of((Instruction12x)instruction);
            case Format20bc:
                return ImmutableInstruction20bc.of((Instruction20bc)instruction);
            case Format20t:
                return ImmutableInstruction20t.of((Instruction20t)instruction);
            case Format21c:
                return ImmutableInstruction21c.of((Instruction21c)instruction);
            case Format21ih:
                return ImmutableInstruction21ih.of((Instruction21ih)instruction);
            case Format21lh:
                return ImmutableInstruction21lh.of((Instruction21lh)instruction);
            case Format21s:
                return ImmutableInstruction21s.of((Instruction21s)instruction);
            case Format21t:
                return ImmutableInstruction21t.of((Instruction21t)instruction);
            case Format22b:
                return ImmutableInstruction22b.of((Instruction22b)instruction);
            case Format22c:
                return ImmutableInstruction22c.of((Instruction22c)instruction);
            case Format22cs:
                return ImmutableInstruction22cs.of((Instruction22cs)instruction);
            case Format22s:
                return ImmutableInstruction22s.of((Instruction22s)instruction);
            case Format22t:
                return ImmutableInstruction22t.of((Instruction22t)instruction);
            case Format22x:
                return ImmutableInstruction22x.of((Instruction22x)instruction);
            case Format23x:
                return ImmutableInstruction23x.of((Instruction23x)instruction);
            case Format30t:
                return ImmutableInstruction30t.of((Instruction30t)instruction);
            case Format31c:
                return ImmutableInstruction31c.of((Instruction31c)instruction);
            case Format31i:
                return ImmutableInstruction31i.of((Instruction31i)instruction);
            case Format31t:
                return ImmutableInstruction31t.of((Instruction31t)instruction);
            case Format32x:
                return ImmutableInstruction32x.of((Instruction32x)instruction);
            case Format35c:
                return ImmutableInstruction35c.of((Instruction35c)instruction);
            case Format35mi:
                return ImmutableInstruction35mi.of((Instruction35mi)instruction);
            case Format35ms:
                return ImmutableInstruction35ms.of((Instruction35ms)instruction);
            case Format3rc:
                return ImmutableInstruction3rc.of((Instruction3rc)instruction);
            case Format3rmi:
                return ImmutableInstruction3rmi.of((Instruction3rmi)instruction);
            case Format3rms:
                return ImmutableInstruction3rms.of((Instruction3rms)instruction);
            case Format51l:
                return ImmutableInstruction51l.of((Instruction51l)instruction);
            case PackedSwitchPayload:
                return ImmutablePackedSwitchPayload.of((PackedSwitchPayload) instruction);
            case SparseSwitchPayload:
                return ImmutableSparseSwitchPayload.of((SparseSwitchPayload) instruction);
            case ArrayPayload:
                return ImmutableArrayPayload.of((ArrayPayload) instruction);
            default:
                throw new RuntimeException("Unexpected instruction type");
        }
    }

    @Nonnull public Opcode getOpcode() {
        return opcode;
    }

    public abstract Format getFormat();

    public int getCodeUnits() {
        return getFormat().size / 2;
    }

    @Nonnull
    public static ImmutableList<ImmutableInstruction> immutableListOf(Iterable<? extends Instruction> list) {
        return CONVERTER.toList(list);
    }

    private static final ImmutableConverter<ImmutableInstruction, Instruction> CONVERTER =
            new ImmutableConverter<ImmutableInstruction, Instruction>() {
                @Override
                protected boolean isImmutable(@Nonnull Instruction item) {
                    return item instanceof ImmutableInstruction;
                }

                @Nonnull
                @Override
                protected ImmutableInstruction makeImmutable(@Nonnull Instruction item) {
                    return ImmutableInstruction.of(item);
                }
            };
}
