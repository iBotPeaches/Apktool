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

package org.jf.dexlib2.builder;

import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.iface.instruction.OffsetInstruction;

import javax.annotation.Nonnull;

public abstract class BuilderOffsetInstruction extends BuilderInstruction implements OffsetInstruction {
    @Nonnull
    protected final Label target;

    public BuilderOffsetInstruction(@Nonnull Opcode opcode,
                                    @Nonnull Label target) {
        super(opcode);
        this.target = target;
    }

    @Override public int getCodeOffset() {
        int codeOffset = internalGetCodeOffset();
        if ((this.getCodeUnits() == 1 && (codeOffset < Byte.MIN_VALUE || codeOffset > Byte.MAX_VALUE)) ||
            (this.getCodeUnits() == 2 && (codeOffset < Short.MIN_VALUE || codeOffset > Short.MAX_VALUE))) {
            throw new IllegalStateException("Target is out of range");
        }
        return codeOffset;
    }


    int internalGetCodeOffset() {
        return target.getCodeAddress() - this.getLocation().getCodeAddress();
    }

    @Nonnull
    public Label getTarget() {
        return target;
    }
}
