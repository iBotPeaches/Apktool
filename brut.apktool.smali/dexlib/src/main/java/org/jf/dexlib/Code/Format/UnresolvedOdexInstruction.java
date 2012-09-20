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

package org.jf.dexlib.Code.Format;

import org.jf.dexlib.Code.Instruction;
import org.jf.dexlib.Util.AnnotatedOutput;

/**
 * This represents a "fixed" odexed instruction, where the object register is always null and so the correct type
 * can't be determined. Typically, these are replaced by an equivalent instruction that would have the same
 * effect (namely, an NPE)
 */
public class UnresolvedOdexInstruction extends Instruction {
    public final Instruction OriginalInstruction;
    //the register number that holds the (null) reference type that the instruction operates on
    public final int ObjectRegisterNum;

    public UnresolvedOdexInstruction(Instruction originalInstruction, int objectRegisterNumber) {
        super(originalInstruction.opcode);
        this.OriginalInstruction = originalInstruction;
        this.ObjectRegisterNum = objectRegisterNumber;
    }

    protected void writeInstruction(AnnotatedOutput out, int currentCodeAddress) {
        throw new RuntimeException("Cannot rewrite an instruction that couldn't be deodexed");
    }

    @Override
    public int getSize(int codeAddress) {
        return OriginalInstruction.getSize(codeAddress);
    }

    public Format getFormat() {
        return Format.UnresolvedOdexInstruction;
    }
}
