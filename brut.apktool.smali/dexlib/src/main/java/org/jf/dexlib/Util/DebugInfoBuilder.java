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

package org.jf.dexlib.Util;

import org.jf.dexlib.*;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is intended to provide an easy to use container to build up a method's debug info. You can easily add
 * an "event" at a specific address, where an event is something like a line number, start/end local, etc.
 * The events must be added such that the code addresses increase monotonically. This matches how a parser would
 * generally behave, and is intended to increase performance.
 */
public class DebugInfoBuilder
{
    private static final int LINE_BASE = -4;
    private static final int LINE_RANGE = 15;
    private static final int FIRST_SPECIAL = 0x0a;

    private int lineStart = 0;
    private ArrayList<String> parameterNames = new ArrayList<String>();
    private ArrayList<Event> events = new ArrayList<Event>();
    private int lastAddress = 0;

    private boolean hasData;

    private int currentAddress;
    private int currentLine;

    public DebugInfoBuilder() {
    }

    private void checkAddress(int address) {
        if (lastAddress > address) {
            throw new RuntimeException("Cannot add an event with an address before the address of the prior event");
        }
    }

    public void addParameterName(String parameterName) {
        if (parameterName != null) {
            hasData = true;
        }

        parameterNames.add(parameterName);
    }

    public void addLine(int address, int line) {
        hasData = true;

        checkAddress(address);

        if (lineStart == 0) {
            lineStart = line;
        }

        events.add(new LineEvent(address, line));
    }

    public void addLocal(int address, int registerNumber, String localName, String localType) {
        hasData = true;

        checkAddress(address);

        events.add(new StartLocalEvent(address, registerNumber, localName, localType));
    }

    public void addLocalExtended(int address, int registerNumber, String localName, String localType,
                                String signature) {
        hasData = true;

        checkAddress(address);

        events.add(new StartLocalExtendedEvent(address, registerNumber, localName, localType, signature));
    }

    public void addEndLocal(int address, int registerNumber) {
        hasData = true;

        checkAddress(address);

        events.add(new EndLocalEvent(address, registerNumber));
    }

    public void addRestartLocal(int address, int registerNumber) {
        hasData = true;

        checkAddress(address);

        events.add(new RestartLocalEvent(address, registerNumber));
    }

    public void addPrologue(int address) {
        hasData = true;

        checkAddress(address);

        events.add(new PrologueEvent(address));
    }

    public void addEpilogue(int address) {
        hasData = true;

        checkAddress(address);

        events.add(new EpilogueEvent(address));
    }

    public void addSetFile(int address, String fileName) {
        hasData = true;

        checkAddress(address);

        events.add(new SetFileEvent(address, fileName));
    }

    public int getParameterNameCount() {
        return parameterNames.size();
    }

    public DebugInfoItem encodeDebugInfo(DexFile dexFile) {
        if (!hasData) {
            return null;
        }

        ByteArrayOutput out = new ByteArrayOutput();
        StringIdItem[] parameterNamesArray = new StringIdItem[parameterNames.size()];
        ArrayList<Item> referencedItems = new ArrayList<Item>();

        if (lineStart == 0) {
            lineStart = 1;
        }

        currentLine = lineStart;

        for (Event event: events) {
            event.emit(dexFile, out, referencedItems);
        }
        emitEndSequence(out);

        int index = 0;
        for (String parameterName: parameterNames) {
            if (parameterName == null) {
                parameterNamesArray[index++] = null;
            } else {
                parameterNamesArray[index++] = StringIdItem.internStringIdItem(dexFile, parameterName);
            }
        }

        Item[] referencedItemsArray = new Item[referencedItems.size()];
        referencedItems.toArray(referencedItemsArray);
        return DebugInfoItem.internDebugInfoItem(dexFile, lineStart, parameterNamesArray, out.toByteArray(),
                referencedItemsArray);
    }

    public static byte calculateSpecialOpcode(int lineDelta, int addressDelta) {
        return (byte)(FIRST_SPECIAL + (addressDelta * LINE_RANGE) + (lineDelta - LINE_BASE));
    }

    private interface Event
    {
        int getAddress();
        void emit(DexFile dexFile, Output out, List<Item> referencedItems);
    }

    private void emitEndSequence(Output out) {
        out.writeByte(0);
    }

    private void emitAdvancePC(Output out, int address) {
        int addressDelta = address-currentAddress;

        if (addressDelta > 0) {
            out.writeByte(1);
            out.writeUnsignedLeb128(addressDelta);
            currentAddress = address;
        }
    }

    private void emitAdvanceLine(Output out, int lineDelta) {
        out.writeByte(2);
        out.writeSignedLeb128(lineDelta);
    }

    private void emitStartLocal(Output out, int registerNum) {
        out.writeByte(3);
        out.writeUnsignedLeb128(registerNum);
        out.writeByte(1);
        out.writeByte(1);
    }

    private void emitStartLocalExtended(Output out, int registerNum) {
        out.writeByte(4);
        out.writeUnsignedLeb128(registerNum);
        out.writeByte(1);
        out.writeByte(1);
        out.writeByte(1);
    }

    private void emitEndLocal(Output out, int registerNum) {
        out.writeByte(5);
        out.writeUnsignedLeb128(registerNum);
    }

    private void emitRestartLocal(Output out, int registerNum) {
        out.writeByte(6);
        out.writeUnsignedLeb128(registerNum);
    }

    private void emitSetPrologueEnd(Output out) {
        out.writeByte(7);
    }

    private void emitSetEpilogueBegin(Output out) {
        out.writeByte(8);
    }

    private void emitSetFile(Output out) {
        out.writeByte(9);
        out.writeByte(1);
    }

    private void emitSpecialOpcode(Output out, byte opcode) {
        out.writeByte(opcode);
    }

    private class LineEvent implements Event
    {
        private final int address;
        private final int line;

        public LineEvent(int address, int line) {
            this.address = address;
            this.line = line;
        }

        public int getAddress() {
            return address;
        }

        public void emit(DexFile dexFile, Output out, List<Item> referencedItems) {
            int lineDelta = line - currentLine;
            int addressDelta = address - currentAddress;

            if (lineDelta < -4 || lineDelta > 10) {
                emitAdvanceLine(out, lineDelta);
                lineDelta = 0;
            }
            if (lineDelta < 2 && addressDelta > 16 || lineDelta > 1 && addressDelta > 15) {
                emitAdvancePC(out, address);
                addressDelta = 0;
            }

            //TODO: need to handle the case when the line delta is larger than a signed int
            emitSpecialOpcode(out, calculateSpecialOpcode(lineDelta, addressDelta));

            currentAddress = address;
            currentLine = line;
        }
    }

    private class StartLocalEvent implements Event
    {
        private final int address;
        private final int registerNum;
        private final String localName;
        private final String localType;

        public StartLocalEvent(int address, int registerNum, String localName, String localType) {
            this.address = address;
            this.registerNum = registerNum;
            this.localName = localName;
            this.localType = localType;
        }

        public int getAddress() {
            return address;
        }

        public void emit(DexFile dexFile, Output out, List<Item> referencedItems) {
            emitAdvancePC(out, address);
            emitStartLocal(out, registerNum);
            referencedItems.add(localName==null?null:StringIdItem.internStringIdItem(dexFile, localName));
            referencedItems.add(localType==null?null:TypeIdItem.internTypeIdItem(dexFile,
                    StringIdItem.internStringIdItem(dexFile, localType)));
        }
    }

    private class StartLocalExtendedEvent implements Event
    {
        private final int address;
        private final int registerNum;
        private final String localName;
        private final String localType;
        private final String signature;

        public StartLocalExtendedEvent(int address, int registerNum, String localName, String localType,
                                       String signature) {
            this.address = address;
            this.registerNum = registerNum;
            this.localName = localName;
            this.localType = localType;
            this.signature = signature;
        }

        public int getAddress() {
            return address;
        }

        public void emit(DexFile dexFile, Output out, List<Item> referencedItems) {
            emitAdvancePC(out, address);
            emitStartLocalExtended(out, registerNum);
            if (localName != null) {
                referencedItems.add(StringIdItem.internStringIdItem(dexFile, localName));
            }
            if (localType != null) {
                referencedItems.add(TypeIdItem.internTypeIdItem(dexFile,
                    StringIdItem.internStringIdItem(dexFile, localType)));
            }
            if (signature != null) {
                referencedItems.add(StringIdItem.internStringIdItem(dexFile, signature));
            }
        }
    }

    private class EndLocalEvent implements Event
    {
        private final int address;
        private final int registerNum;

        public EndLocalEvent(int address, int registerNum) {
            this.address = address;
            this.registerNum = registerNum;
        }

        public int getAddress() {
            return address;
        }

        public void emit(DexFile dexFile, Output out, List<Item> referencedItems) {
            emitAdvancePC(out, address);
            emitEndLocal(out, registerNum);
        }
    }

    private class RestartLocalEvent implements Event
    {
        private final int address;
        private final int registerNum;

        public RestartLocalEvent(int address, int registerNum) {
            this.address = address;
            this.registerNum = registerNum;
        }

        public int getAddress() {
            return address;
        }

        public void emit(DexFile dexFile, Output out, List<Item> referencedItems) {
            emitAdvancePC(out, address);
            emitRestartLocal(out, registerNum);
        }
    }

    private class PrologueEvent implements Event
    {
        private final int address;

        public PrologueEvent(int address) {
            this.address = address;
        }

        public int getAddress() {
            return address;
        }

        public void emit(DexFile dexFile, Output out, List<Item> referencedItems) {
            emitAdvancePC(out, address);
            emitSetPrologueEnd(out);
        }
    }

    private class EpilogueEvent implements Event
    {
        private final int address;

        public EpilogueEvent(int address) {
            this.address = address;
        }

        public int getAddress() {
            return address;
        }

        public void emit(DexFile dexFile, Output out, List<Item> referencedItems) {
            emitAdvancePC(out, address);
            emitSetEpilogueBegin(out);
        }
    }

    private class SetFileEvent implements Event
    {
        private final int address;
        private final String fileName;

        public SetFileEvent(int address, String fileName) {
            this.address = address;
            this.fileName = fileName;
        }

        public int getAddress() {
            return address;
        }

        public void emit(DexFile dexFile, Output out, List<Item> referencedItems) {
            emitAdvancePC(out, address);
            emitSetFile(out);
            if (fileName != null) {
                referencedItems.add(StringIdItem.internStringIdItem(dexFile, fileName));
            }
        }
    }
}
