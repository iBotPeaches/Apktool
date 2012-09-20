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
import org.jf.dexlib.Code.Format.*;
import org.jf.dexlib.Code.Instruction;
import org.jf.dexlib.Code.OffsetInstruction;
import org.jf.dexlib.CodeItem;

public class InstructionMethodItemFactory {
    private InstructionMethodItemFactory() {
    }

    public static InstructionMethodItem makeInstructionFormatMethodItem(MethodDefinition methodDefinition,
                                                                              CodeItem codeItem,
                                                                              int codeAddress,
                                                                              Instruction instruction) {
        if (instruction instanceof OffsetInstruction) {
            return new OffsetInstructionFormatMethodItem(methodDefinition.getLabelCache(), codeItem, codeAddress,
                    instruction);
        }

        switch (instruction.getFormat()) {
            case ArrayData:
                return new ArrayDataMethodItem(codeItem, codeAddress,
                        (ArrayDataPseudoInstruction)instruction);
            case PackedSwitchData:
                return new PackedSwitchMethodItem(methodDefinition, codeItem, codeAddress,
                        (PackedSwitchDataPseudoInstruction)instruction);
            case SparseSwitchData:
                return new SparseSwitchMethodItem(methodDefinition, codeItem, codeAddress,
                        (SparseSwitchDataPseudoInstruction)instruction);
            case UnresolvedOdexInstruction:
                return new UnresolvedOdexInstructionMethodItem(codeItem, codeAddress,
                        (UnresolvedOdexInstruction)instruction);
            default:
                return new InstructionMethodItem(codeItem, codeAddress, instruction);
        }
    }
}
