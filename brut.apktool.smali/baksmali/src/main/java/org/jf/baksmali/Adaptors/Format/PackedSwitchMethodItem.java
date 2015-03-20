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

import org.jf.baksmali.Adaptors.CommentingIndentingWriter;
import org.jf.baksmali.Adaptors.LabelMethodItem;
import org.jf.baksmali.Adaptors.MethodDefinition;
import org.jf.dexlib2.iface.instruction.SwitchElement;
import org.jf.dexlib2.iface.instruction.formats.PackedSwitchPayload;
import org.jf.util.IndentingWriter;
import org.jf.baksmali.Renderers.IntegerRenderer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PackedSwitchMethodItem extends InstructionMethodItem<PackedSwitchPayload> {
    private final List<PackedSwitchTarget> targets;
    private final int firstKey;

    // Whether this sparse switch instruction should be commented out because it is never referenced
    private boolean commentedOut;

    public PackedSwitchMethodItem(MethodDefinition methodDef, int codeAddress, PackedSwitchPayload instruction) {
        super(methodDef, codeAddress, instruction);

        int baseCodeAddress = methodDef.getPackedSwitchBaseAddress(codeAddress);

        targets = new ArrayList<PackedSwitchTarget>();

        boolean first = true;
        int firstKey = 0;
        if (baseCodeAddress >= 0) {
            for (SwitchElement switchElement: instruction.getSwitchElements()) {
                if (first) {
                    firstKey = switchElement.getKey();
                    first = false;
                }
                LabelMethodItem label = methodDef.getLabelCache().internLabel(
                        new LabelMethodItem(methodDef.classDef.options, baseCodeAddress + switchElement.getOffset(),
                                "pswitch_"));
                targets.add(new PackedSwitchLabelTarget(label));
            }
        } else {
            commentedOut = true;
            for (SwitchElement switchElement: instruction.getSwitchElements()) {
                if (first) {
                    firstKey = switchElement.getKey();
                    first = false;
                }
                targets.add(new PackedSwitchOffsetTarget(switchElement.getOffset()));
            }
        }
        this.firstKey = firstKey;
    }

    @Override
    public boolean writeTo(IndentingWriter writer) throws IOException {
        if (commentedOut) {
            writer = new CommentingIndentingWriter(writer);
        }
        writer.write(".packed-switch ");
        IntegerRenderer.writeTo(writer, firstKey);
        writer.indent(4);
        writer.write('\n');
        int key = firstKey;
        for (PackedSwitchTarget target: targets) {
            target.writeTargetTo(writer);
            writeCommentIfResourceId(writer, key);
            writer.write('\n');
            key++;
        }
        writer.deindent(4);
        writer.write(".end packed-switch");
        return true;
    }

    private static abstract class PackedSwitchTarget {
        public abstract void writeTargetTo(IndentingWriter writer) throws IOException;
    }

    private static class PackedSwitchLabelTarget extends PackedSwitchTarget {
        private final LabelMethodItem target;
        public PackedSwitchLabelTarget(LabelMethodItem target) {
            this.target = target;
        }
        public void writeTargetTo(IndentingWriter writer) throws IOException {
            target.writeTo(writer);
        }
    }

    private static class PackedSwitchOffsetTarget extends PackedSwitchTarget {
        private final int target;
        public PackedSwitchOffsetTarget(int target) {
            this.target = target;
        }
        public void writeTargetTo(IndentingWriter writer) throws IOException {
            if (target >= 0) {
                writer.write('+');
            }
            writer.printSignedIntAsDec(target);
        }
    }
}
