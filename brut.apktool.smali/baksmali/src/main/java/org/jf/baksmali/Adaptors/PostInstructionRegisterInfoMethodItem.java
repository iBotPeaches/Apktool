/*
 * [The "BSD licence"]
 * Copyright (c) 2010 Ben Gruver
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

package org.jf.baksmali.Adaptors;

import org.jf.baksmali.baksmaliOptions;
import org.jf.dexlib2.analysis.AnalyzedInstruction;
import org.jf.dexlib2.analysis.RegisterType;
import org.jf.util.IndentingWriter;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.BitSet;

public class PostInstructionRegisterInfoMethodItem extends MethodItem {
    @Nonnull private final RegisterFormatter registerFormatter;
    @Nonnull private final AnalyzedInstruction analyzedInstruction;

    public PostInstructionRegisterInfoMethodItem(@Nonnull RegisterFormatter registerFormatter,
                                                 @Nonnull AnalyzedInstruction analyzedInstruction,
                                                 int codeAddress) {
        super(codeAddress);
        this.registerFormatter = registerFormatter;
        this.analyzedInstruction = analyzedInstruction;
    }

    @Override
    public double getSortOrder() {
        return 100.1;
    }

    @Override
    public boolean writeTo(IndentingWriter writer) throws IOException {
        int registerInfo = registerFormatter.options.registerInfo;
        int registerCount = analyzedInstruction.getRegisterCount();
        BitSet registers = new BitSet(registerCount);

        if ((registerInfo & baksmaliOptions.ALL) != 0) {
            registers.set(0, registerCount);
        } else {
            if ((registerInfo & baksmaliOptions.ALLPOST) != 0) {
                registers.set(0, registerCount);
            } else if ((registerInfo & baksmaliOptions.DEST) != 0) {
                addDestRegs(registers, registerCount);
            }
        }

        return writeRegisterInfo(writer, registers);
    }

    private void addDestRegs(BitSet printPostRegister, int registerCount) {
        for (int registerNum=0; registerNum<registerCount; registerNum++) {
            if (!analyzedInstruction.getPreInstructionRegisterType(registerNum).equals(
                    analyzedInstruction.getPostInstructionRegisterType(registerNum))) {
                printPostRegister.set(registerNum);
            }
        }
    }

    private boolean writeRegisterInfo(IndentingWriter writer, BitSet registers) throws IOException {
        int registerNum = registers.nextSetBit(0);
        if (registerNum < 0) {
            return false;
        }

        writer.write('#');
        for (; registerNum >= 0; registerNum = registers.nextSetBit(registerNum + 1)) {
            RegisterType registerType = analyzedInstruction.getPostInstructionRegisterType(registerNum);

            registerFormatter.writeTo(writer, registerNum);
            writer.write('=');
            registerType.writeTo(writer);
            writer.write(';');
        }
        return true;
    }
}
