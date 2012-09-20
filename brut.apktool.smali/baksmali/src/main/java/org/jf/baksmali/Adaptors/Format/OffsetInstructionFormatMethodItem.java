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

import org.jf.baksmali.Adaptors.LabelMethodItem;
import org.jf.baksmali.Adaptors.MethodDefinition;
import org.jf.util.IndentingWriter;
import org.jf.dexlib.Code.Instruction;
import org.jf.dexlib.Code.OffsetInstruction;
import org.jf.dexlib.Code.Opcode;
import org.jf.dexlib.CodeItem;

import java.io.IOException;

public class OffsetInstructionFormatMethodItem<T extends Instruction & OffsetInstruction>
        extends InstructionMethodItem<T> {
    protected LabelMethodItem label;

    public OffsetInstructionFormatMethodItem(MethodDefinition.LabelCache labelCache, CodeItem codeItem, int codeAddress,
                                             T instruction) {
        super(codeItem, codeAddress, instruction);

        label = new LabelMethodItem(codeAddress + instruction.getTargetAddressOffset(), getLabelPrefix());
        label = labelCache.internLabel(label);
    }

    @Override
    protected void writeTargetLabel(IndentingWriter writer) throws IOException {
        label.writeTo(writer);
    }

    public LabelMethodItem getLabel() {
        return label;
    }

    private String getLabelPrefix() {
        switch (instruction.getFormat()) {
            case Format10t:
            case Format20t:
            case Format30t:
                return "goto_";
            case Format21t:
            case Format22t:
                return "cond_";
            case Format31t:
                if (instruction.opcode == Opcode.FILL_ARRAY_DATA) {
                    return "array_";
                }
                if (instruction.opcode == Opcode.PACKED_SWITCH) {
                    return "pswitch_data_";
                }
                assert instruction.opcode == Opcode.SPARSE_SWITCH;
                return "sswitch_data_";
        }

        assert false;
        return null;
    }
}
