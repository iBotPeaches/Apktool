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
import javax.annotation.Nullable;
import java.io.IOException;

public class CatchMethodItem extends MethodItem {
    private final String exceptionType;

    private final LabelMethodItem tryStartLabel;
    private final LabelMethodItem tryEndLabel;
    private final LabelMethodItem handlerLabel;

    public CatchMethodItem(@Nonnull baksmaliOptions options, @Nonnull MethodDefinition.LabelCache labelCache,
                           int codeAddress, @Nullable String exceptionType, int startAddress, int endAddress,
                           int handlerAddress) {
        super(codeAddress);
        this.exceptionType = exceptionType;

        tryStartLabel = labelCache.internLabel(new LabelMethodItem(options, startAddress, "try_start_"));

        //use the address from the last covered instruction, but make the label
        //name refer to the address of the next instruction
        tryEndLabel = labelCache.internLabel(new EndTryLabelMethodItem(options, codeAddress, endAddress));

        if (exceptionType == null) {
            handlerLabel = labelCache.internLabel(new LabelMethodItem(options, handlerAddress, "catchall_"));
        } else {
            handlerLabel = labelCache.internLabel(new LabelMethodItem(options, handlerAddress, "catch_"));
        }
    }

    public LabelMethodItem getTryStartLabel() {
        return tryStartLabel;
    }

    public LabelMethodItem getTryEndLabel() {
        return tryEndLabel;
    }

    public LabelMethodItem getHandlerLabel() {
        return handlerLabel;
    }

    public double getSortOrder() {
        //sort after instruction and end_try label
        return 102;
    }

    @Override
    public boolean writeTo(IndentingWriter writer) throws IOException {
        if (exceptionType == null) {
            writer.write(".catchall");
        } else {
            writer.write(".catch ");
            writer.write(exceptionType);
        }
        writer.write(" {");
        tryStartLabel.writeTo(writer);
        writer.write(" .. ");
        tryEndLabel.writeTo(writer);
        writer.write("} ");
        handlerLabel.writeTo(writer);
        return true;
    }
}
