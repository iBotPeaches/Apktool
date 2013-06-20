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

package org.jf.baksmali.Adaptors;

import org.jf.baksmali.baksmaliOptions;
import org.jf.util.IndentingWriter;

import javax.annotation.Nonnull;
import java.io.IOException;

/**
 * This class contains the logic used for formatting registers
 */
public class RegisterFormatter {
    @Nonnull public final baksmaliOptions options;
    public final int registerCount;
    public final int parameterRegisterCount;

    public RegisterFormatter(@Nonnull baksmaliOptions options, int registerCount, int parameterRegisterCount) {
        this.options = options;
        this.registerCount = registerCount;
        this.parameterRegisterCount = parameterRegisterCount;
    }

    /**
     * Write out the register range value used by Format3rc. If baksmali.noParameterRegisters is true, it will always
     * output the registers in the v<n> format. But if false, then it will check if *both* registers are parameter
     * registers, and if so, use the p<n> format for both. If only the last register is a parameter register, it will
     * use the v<n> format for both, otherwise it would be confusing to have something like {v20 .. p1}
     * @param writer the <code>IndentingWriter</code> to write to
     * @param startRegister the first register in the range
     * @param lastRegister the last register in the range
     */
    public void writeRegisterRange(IndentingWriter writer, int startRegister, int lastRegister) throws IOException {
        if (!options.noParameterRegisters) {
            assert startRegister <= lastRegister;

            if (startRegister >= registerCount - parameterRegisterCount) {
                writer.write("{p");
                writer.printSignedIntAsDec(startRegister - (registerCount - parameterRegisterCount));
                writer.write(" .. p");
                writer.printSignedIntAsDec(lastRegister - (registerCount - parameterRegisterCount));
                writer.write('}');
                return;
            }
        }
        writer.write("{v");
        writer.printSignedIntAsDec(startRegister);
        writer.write(" .. v");
        writer.printSignedIntAsDec(lastRegister);
        writer.write('}');
    }

    /**
     * Writes a register with the appropriate format. If baksmali.noParameterRegisters is true, then it will always
     * output a register in the v<n> format. If false, then it determines if the register is a parameter register,
     * and if so, formats it in the p<n> format instead.
     *
     * @param writer the <code>IndentingWriter</code> to write to
     * @param register the register number
     */
    public void writeTo(IndentingWriter writer, int register) throws IOException {
        if (!options.noParameterRegisters) {
            if (register >= registerCount - parameterRegisterCount) {
                writer.write('p');
                writer.printSignedIntAsDec((register - (registerCount - parameterRegisterCount)));
                return;
            }
        }
        writer.write('v');
        writer.printSignedIntAsDec(register);
    }
}
