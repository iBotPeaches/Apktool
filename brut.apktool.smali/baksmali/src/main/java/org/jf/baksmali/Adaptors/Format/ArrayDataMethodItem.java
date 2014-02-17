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
import org.jf.baksmali.Renderers.LongRenderer;
import org.jf.dexlib2.iface.instruction.formats.ArrayPayload;
import org.jf.util.IndentingWriter;

import java.io.IOException;
import java.util.List;

public class ArrayDataMethodItem extends InstructionMethodItem<ArrayPayload> {
    public ArrayDataMethodItem(MethodDefinition methodDef, int codeAddress, ArrayPayload instruction) {
        super(methodDef, codeAddress, instruction);
    }

    public boolean writeTo(IndentingWriter writer) throws IOException {
        int elementWidth = instruction.getElementWidth();

        writer.write(".array-data ");
        writer.printSignedIntAsDec(instruction.getElementWidth());
        writer.write('\n');

        writer.indent(4);

        List<Number> elements = instruction.getArrayElements();

        String suffix = "";
        switch (elementWidth) {
            case 1:
                suffix = "t";
                break;
            case 2:
                suffix = "s";
                break;
        }

        for (Number number: elements) {
            LongRenderer.writeSignedIntOrLongTo(writer, number.longValue());
            writer.write(suffix);
            if (elementWidth == 8) {
                writeCommentIfLikelyDouble(writer, number.longValue());
            } else if (elementWidth == 4) {
                int value = number.intValue();
                boolean isResourceId = writeCommentIfResourceId(writer, value);
                if (!isResourceId) writeCommentIfLikelyFloat(writer, value);
            }
            writer.write("\n");
        }
        writer.deindent(4);
        writer.write(".end array-data");
        return true;
    }
}
