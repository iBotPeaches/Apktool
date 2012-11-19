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
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.jf.dexlib.Code.Format;

import org.jf.dexlib.Code.Instruction;

public enum Format {
    Format10t(Instruction10t.Factory, 2),
    Format10x(Instruction10x.Factory, 2),
    Format11n(Instruction11n.Factory, 2),
    Format11x(Instruction11x.Factory, 2),
    Format12x(Instruction12x.Factory, 2),
    Format20bc(Instruction20bc.Factory, 4),
    Format20t(Instruction20t.Factory, 4),
    Format21c(Instruction21c.Factory, 4),
    Format21h(Instruction21h.Factory, 4),
    Format21s(Instruction21s.Factory, 4),
    Format21t(Instruction21t.Factory, 4),
    Format22b(Instruction22b.Factory, 4),
    Format22c(Instruction22c.Factory, 4),
    Format22cs(Instruction22cs.Factory, 4),
    Format22s(Instruction22s.Factory, 4),
    Format22t(Instruction22t.Factory, 4),
    Format22x(Instruction22x.Factory, 4),
    Format23x(Instruction23x.Factory, 4),
    Format30t(Instruction30t.Factory, 6),
    Format31c(Instruction31c.Factory, 6),
    Format31i(Instruction31i.Factory, 6),
    Format31t(Instruction31t.Factory, 6),
    Format32x(Instruction32x.Factory, 6),
    Format35c(Instruction35c.Factory, 6),
    Format35mi(Instruction35mi.Factory, 6),
    Format35ms(Instruction35ms.Factory, 6),
    Format3rc(Instruction3rc.Factory, 6),
    Format3rmi(Instruction3rmi.Factory, 6),
    Format3rms(Instruction3rms.Factory, 6),
    Format41c(Instruction41c.Factory, 8),
    Format51l(Instruction51l.Factory, 10),
    Format52c(Instruction52c.Factory, 10),
    Format5rc(Instruction5rc.Factory, 10),
    ArrayData(null, -1, true),
    PackedSwitchData(null, -1, true),
    SparseSwitchData(null, -1, true),
    UnresolvedOdexInstruction(null, -1, false),
    ;

    public final Instruction.InstructionFactory Factory;
    public final int size;
    public final boolean variableSizeFormat;

    private Format(Instruction.InstructionFactory factory, int size) {
        this(factory, size, false);
    }

    private Format(Instruction.InstructionFactory factory, int size, boolean variableSizeFormat) {
        this.Factory = factory;
        this.size = size;
        this.variableSizeFormat = variableSizeFormat;
    }
}
