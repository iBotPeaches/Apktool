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
import org.jf.dexlib.Code.Opcode;
import org.jf.dexlib.DexFile;
import org.jf.dexlib.Util.AnnotatedOutput;
import org.jf.dexlib.Util.NumberUtils;

import java.util.Iterator;

public class ArrayDataPseudoInstruction extends Instruction {
    public static final Instruction.InstructionFactory Factory = new Factory();
    private int elementWidth;
    private byte[] encodedValues;

    @Override
    public int getSize(int codeAddress) {
        return ((encodedValues.length + 1)/2) + 4 + (codeAddress % 2);
    }

    public ArrayDataPseudoInstruction(int elementWidth, byte[] encodedValues) {
        super(Opcode.NOP);

        if (encodedValues.length % elementWidth != 0) {
            throw new RuntimeException("There are not a whole number of " + elementWidth + " byte elements");
        }

        this.elementWidth = elementWidth;
        this.encodedValues = encodedValues;
    }

    public ArrayDataPseudoInstruction(byte[] buffer, int bufferIndex) {
        super(Opcode.NOP);

        byte opcodeByte = buffer[bufferIndex];
        if (opcodeByte != 0x00) {
            throw new RuntimeException("Invalid opcode byte for an ArrayData pseudo-instruction");
        }

        byte subopcodeByte = buffer[bufferIndex+1];
        if (subopcodeByte != 0x03) {
            throw new RuntimeException("Invalid sub-opcode byte for an ArrayData pseudo-instruction");
        }

        this.elementWidth = NumberUtils.decodeUnsignedShort(buffer, bufferIndex+2);
        int elementCount = NumberUtils.decodeInt(buffer, bufferIndex+4);
        this.encodedValues = new byte[elementCount * elementWidth];
        System.arraycopy(buffer, bufferIndex+8, encodedValues, 0, elementCount * elementWidth);
    }

    protected void writeInstruction(AnnotatedOutput out, int currentCodeAddress) {
        out.alignTo(4);

        int elementCount = encodedValues.length / elementWidth;

        out.writeByte(0x00);
        out.writeByte(0x03);
        out.writeShort(elementWidth);
        out.writeInt(elementCount);
        out.write(encodedValues);

        //make sure we're written out an even number of bytes
        out.alignTo(2);
    }

    protected void annotateInstruction(AnnotatedOutput out, int currentCodeAddress) {
        out.annotate(getSize(currentCodeAddress)*2, "[0x" + Integer.toHexString(currentCodeAddress) + "] " +
                "fill-array-data instruction");
    }

    public Format getFormat() {
        return Format.ArrayData;
    }

    public int getElementWidth() {
        return elementWidth;
    }

    public int getElementCount() {
        return encodedValues.length / elementWidth;
    }

    public static class ArrayElement {
        public final byte[] buffer;
        public int bufferIndex;
        public final int elementWidth;
        public ArrayElement(byte[] buffer, int elementWidth) {
            this.buffer = buffer;
            this.elementWidth = elementWidth;
        }
    }

    public Iterator<ArrayElement> getElements() {
        return new Iterator<ArrayElement>() {
            final int elementCount = getElementCount();
            int i=0;
            int position=0;
            final ArrayElement arrayElement = new ArrayElement(encodedValues, getElementWidth());

            public boolean hasNext() {
                return i<elementCount;
            }

            public ArrayElement next() {
                arrayElement.bufferIndex = position;
                position += arrayElement.elementWidth;
                i++;
                return arrayElement;
            }

            public void remove() {
            }
        };
    }

    private static class Factory implements Instruction.InstructionFactory {
        public Instruction makeInstruction(DexFile dexFile, Opcode opcode, byte[] buffer, int bufferIndex) {
            if (opcode != Opcode.NOP) {
                throw new RuntimeException("The opcode for an ArrayDataPseudoInstruction must be NOP");
            }
            return new ArrayDataPseudoInstruction(buffer, bufferIndex);
        }
    }
}
