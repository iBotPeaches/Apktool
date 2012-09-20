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
import org.jf.dexlib.Code.MultiOffsetInstruction;
import org.jf.dexlib.Code.Opcode;
import org.jf.dexlib.DexFile;
import org.jf.dexlib.Util.AnnotatedOutput;
import org.jf.dexlib.Util.NumberUtils;

import java.util.Iterator;

public class PackedSwitchDataPseudoInstruction extends Instruction implements MultiOffsetInstruction {
    public static final Instruction.InstructionFactory Factory = new Factory();
    private int firstKey;
    private int[] targets;

    @Override
    public int getSize(int codeAddress) {
        return getTargetCount() * 2 + 4 + (codeAddress % 2);
    }

    public PackedSwitchDataPseudoInstruction(int firstKey, int[] targets) {
        super(Opcode.NOP);

        if (targets.length > 0xFFFF) {
            throw new RuntimeException("The packed-switch data contains too many elements. " +
                    "The maximum number of switch elements is 65535");
        }

        this.firstKey = firstKey;
        this.targets = targets;
    }

    public PackedSwitchDataPseudoInstruction(byte[] buffer, int bufferIndex) {
        super(Opcode.NOP);

        byte opcodeByte = buffer[bufferIndex];
        if (opcodeByte != 0x00) {
            throw new RuntimeException("Invalid opcode byte for a PackedSwitchData pseudo-instruction");
        }
        byte subopcodeByte = buffer[bufferIndex+1];
        if (subopcodeByte != 0x01) {
            throw new RuntimeException("Invalid sub-opcode byte for a PackedSwitchData pseudo-instruction");
        }

        int targetCount = NumberUtils.decodeUnsignedShort(buffer, bufferIndex + 2);
        this.firstKey = NumberUtils.decodeInt(buffer, bufferIndex + 4);
        this.targets = new int[targetCount];

        for (int i = 0; i<targetCount; i++) {
            targets[i] = NumberUtils.decodeInt(buffer, bufferIndex + 8 + 4*i);
        }
    }

    protected void writeInstruction(AnnotatedOutput out, int currentCodeAddress) {
        out.alignTo(4);

        out.writeByte(0x00);
        out.writeByte(0x01);
        out.writeShort(targets.length);
        out.writeInt(firstKey);

        for (int target : targets) {
            out.writeInt(target);
        }
    }

    protected void annotateInstruction(AnnotatedOutput out, int currentCodeAddress) {
        out.annotate(getSize(currentCodeAddress)*2, "[0x" + Integer.toHexString(currentCodeAddress) + "] " +
                "packed-switch-data instruction");
    }

    public void updateTarget(int targetIndex, int targetAddressOffset) {
        targets[targetIndex] = targetAddressOffset;
    }

    public Format getFormat() {
        return Format.PackedSwitchData;
    }

    public int getTargetCount() {
        return targets.length;
    }

    public int getFirstKey() {
        return firstKey;
    }

    public int[] getTargets() {
        return targets;
    }

    public static class PackedSwitchTarget {
        public int value;
        public int targetAddressOffset;
    }

    public Iterator<PackedSwitchTarget> iterateKeysAndTargets() {
        return new Iterator<PackedSwitchTarget>() {
            final int targetCount = getTargetCount();
            int i = 0;
            int value = getFirstKey();

            PackedSwitchTarget packedSwitchTarget = new PackedSwitchTarget();

            public boolean hasNext() {
                return i<targetCount;
            }

            public PackedSwitchTarget next() {
                packedSwitchTarget.value = value++;
                packedSwitchTarget.targetAddressOffset = targets[i];
                i++;
                return packedSwitchTarget;
            }

            public void remove() {
            }
        };
    }

    private static class Factory implements Instruction.InstructionFactory {
        public Instruction makeInstruction(DexFile dexFile, Opcode opcode, byte[] buffer, int bufferIndex) {
            if (opcode != Opcode.NOP) {
                throw new RuntimeException("The opcode for a PackedSwitchDataPseudoInstruction must be NOP");
            }
            return new PackedSwitchDataPseudoInstruction(buffer, bufferIndex);
        }
    }
}
