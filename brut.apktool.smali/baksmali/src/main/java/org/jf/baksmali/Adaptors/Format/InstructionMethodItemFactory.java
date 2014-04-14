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

package org.jf.baksmali.Adaptors.Format;

import org.jf.baksmali.Adaptors.MethodDefinition;
import org.jf.dexlib2.analysis.UnresolvedOdexInstruction;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.OffsetInstruction;
import org.jf.dexlib2.iface.instruction.formats.ArrayPayload;
import org.jf.dexlib2.iface.instruction.formats.PackedSwitchPayload;
import org.jf.dexlib2.iface.instruction.formats.SparseSwitchPayload;

public class InstructionMethodItemFactory {
    private InstructionMethodItemFactory() {
    }

    public static InstructionMethodItem makeInstructionFormatMethodItem(
            MethodDefinition methodDef, int codeAddress, Instruction instruction) {

        if (instruction instanceof OffsetInstruction) {
            return new OffsetInstructionFormatMethodItem(methodDef.classDef.options, methodDef, codeAddress,
                    (OffsetInstruction)instruction);
        }

        if (instruction instanceof UnresolvedOdexInstruction) {
            return new UnresolvedOdexInstructionMethodItem(methodDef, codeAddress,
                    (UnresolvedOdexInstruction)instruction);
        }

        switch (instruction.getOpcode().format) {
            case ArrayPayload:
                return new ArrayDataMethodItem(methodDef, codeAddress, (ArrayPayload)instruction);
            case PackedSwitchPayload:
                return new PackedSwitchMethodItem(methodDef, codeAddress, (PackedSwitchPayload)instruction);
            case SparseSwitchPayload:
                return new SparseSwitchMethodItem(methodDef, codeAddress, (SparseSwitchPayload)instruction);
            default:
                return new InstructionMethodItem<Instruction>(methodDef, codeAddress, instruction);
        }
    }
}
