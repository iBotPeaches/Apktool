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

package org.jf.dexlib;

import org.jf.dexlib.Debug.DebugInstructionIterator;
import org.jf.dexlib.Debug.DebugOpcode;
import org.jf.dexlib.Util.AnnotatedOutput;
import org.jf.dexlib.Util.ByteArrayInput;
import org.jf.dexlib.Util.Input;
import org.jf.dexlib.Util.Leb128Utils;

import java.util.ArrayList;
import java.util.List;

public class DebugInfoItem extends Item<DebugInfoItem> {
    private int lineStart;
    private StringIdItem[] parameterNames;
    private byte[] encodedDebugInfo;
    private Item[] referencedItems;

    private CodeItem parent = null;

    /**
     * Creates a new uninitialized <code>DebugInfoInfo</code>
     * @param dexFile The <code>DexFile</code> that this item belongs to
     */
    public DebugInfoItem(DexFile dexFile) {
        super(dexFile);
    }

    /**
     * Creates a new <code>DebugInfoItem</code> with the given values
     * @param dexFile The <code>DexFile</code> that this item belongs to
     * @param lineStart the initial value for the line number register for the debug info machine
     * @param parameterNames an array of the names of the associated method's parameters. The entire parameter
     * can be null if no parameter info is available, or any element can be null to indicate no info for that parameter
     * @param encodedDebugInfo the debug info, encoded as a byte array
     * @param referencedItems an array of the items referenced by instructions, in order of occurance in the encoded
     * debug info
     */
    private DebugInfoItem(DexFile dexFile,
                         int lineStart,
                         StringIdItem[] parameterNames,
                         byte[] encodedDebugInfo,
                         Item[] referencedItems) {
        super(dexFile);
        this.lineStart = lineStart;
        this.parameterNames = parameterNames;
        this.encodedDebugInfo = encodedDebugInfo;
        this.referencedItems = referencedItems;
    }

    /**
     * Returns a new <code>DebugInfoItem</code> with the given values
     * @param dexFile The <code>DexFile</code> that this item belongs to
     * @param lineStart the initial value for the line number register for the debug info machine
     * @param parameterNames an array of the names of the associated method's parameters. The entire parameter
     * can be null if no parameter info is available, or any element can be null to indicate no info for that parameter
     * @param encodedDebugInfo the debug info, encoded as a byte array
     * @param referencedItems an array of the items referenced by instructions, in order of occurance in the encoded
     * debug info
     * @return a new <code>DebugInfoItem</code> with the given values
     */
    public static DebugInfoItem internDebugInfoItem(DexFile dexFile,
                         int lineStart,
                         StringIdItem[] parameterNames,
                         byte[] encodedDebugInfo,
                         Item[] referencedItems) {
        DebugInfoItem debugInfoItem = new DebugInfoItem(dexFile, lineStart, parameterNames, encodedDebugInfo,
                referencedItems);
        return dexFile.DebugInfoItemsSection.intern(debugInfoItem);
    }

    /** {@inheritDoc} */
    protected void readItem(Input in, ReadContext readContext) {
        lineStart = in.readUnsignedLeb128();
        parameterNames = new StringIdItem[in.readUnsignedLeb128()];
        IndexedSection<StringIdItem> stringIdSection = dexFile.StringIdsSection;
        for (int i=0; i<parameterNames.length; i++) {
            parameterNames[i] = stringIdSection.getOptionalItemByIndex(in.readUnsignedLeb128() - 1);
        }

        int start = in.getCursor();
        final List<Item> referencedItemsList = new ArrayList<Item>(50);
        DebugInstructionIterator.IterateInstructions(in,
                new DebugInstructionIterator.ProcessRawDebugInstructionDelegate() {
                    @Override
                    public void ProcessStartLocal(int startDebugOffset, int length, int registerNum, int nameIndex,
                                                  int typeIndex, boolean registerIsSigned) {
                        if (nameIndex != -1) {
                            referencedItemsList.add(dexFile.StringIdsSection.getItemByIndex(nameIndex));
                        }
                        if (typeIndex != -1) {
                            referencedItemsList.add(dexFile.TypeIdsSection.getItemByIndex(typeIndex));
                        }
                    }

                    @Override
                    public void ProcessStartLocalExtended(int startDebugOffset, int length, int registerNume,
                                                          int nameIndex, int typeIndex, int signatureIndex,
                                                          boolean registerIsSigned) {
                        if (nameIndex != -1) {
                            referencedItemsList.add(dexFile.StringIdsSection.getItemByIndex(nameIndex));
                        }
                        if (typeIndex != -1) {
                            referencedItemsList.add(dexFile.TypeIdsSection.getItemByIndex(typeIndex));
                        }
                        if (signatureIndex != -1) {
                            referencedItemsList.add(dexFile.StringIdsSection.getItemByIndex(signatureIndex));
                        }
                    }

                    @Override
                    public void ProcessSetFile(int startDebugOffset, int length, int nameIndex) {
                        if (nameIndex != -1) {
                            referencedItemsList.add(dexFile.StringIdsSection.getItemByIndex(nameIndex));
                        }
                    }
                });

        referencedItems = new Item[referencedItemsList.size()];
        referencedItemsList.toArray(referencedItems);

        int length = in.getCursor() - start;
        in.setCursor(start);
        encodedDebugInfo = in.readBytes(length);
    }



    /** {@inheritDoc} */
    protected int placeItem(int offset) {
        offset += Leb128Utils.unsignedLeb128Size(lineStart);
        offset += Leb128Utils.unsignedLeb128Size(parameterNames.length);
        for (StringIdItem parameterName: parameterNames) {
            int indexp1;
            if (parameterName == null) {
                indexp1 = 0;
            } else {
                indexp1 = parameterName.getIndex() + 1;
            }
            offset += Leb128Utils.unsignedLeb128Size(indexp1);
        }

        //make a subclass so we can keep track of and access the computed length
        class ProcessDebugInstructionDelegateWithLength extends
                DebugInstructionIterator.ProcessRawDebugInstructionDelegate {
            public int length = 0;
        }
        ProcessDebugInstructionDelegateWithLength pdidwl;

        //final referencedItems = this.referencedItems;

        DebugInstructionIterator.IterateInstructions(new ByteArrayInput(encodedDebugInfo),
                pdidwl = new ProcessDebugInstructionDelegateWithLength() {
                    private int referencedItemsPosition = 0;

                    @Override
                    public void ProcessStaticOpcode(DebugOpcode opcode, int startDebugOffset, int length) {
                        this.length+=length;
                    }

                    @Override
                    public void ProcessStartLocal(int startDebugOffset, int length, int registerNum, int nameIndex,
                                                  int typeIndex, boolean registerIsSigned) {
                        this.length++;
                        if (dexFile.getPreserveSignedRegisters() && registerIsSigned) {
                            this.length += Leb128Utils.signedLeb128Size(registerNum);
                        } else {
                            this.length+=Leb128Utils.unsignedLeb128Size(registerNum);
                        }
                        if (nameIndex != -1) {
                            this.length+=
                               Leb128Utils.unsignedLeb128Size(referencedItems[referencedItemsPosition++].getIndex()+1);
                        } else {
                            this.length++;
                        }
                        if (typeIndex != -1) {
                            this.length+=
                                Leb128Utils.unsignedLeb128Size(referencedItems[referencedItemsPosition++].getIndex()+1);
                        } else {
                            this.length++;
                        }

                    }

                    @Override
                    public void ProcessStartLocalExtended(int startDebugOffset, int length, int registerNum, int nameIndex,
                                                          int typeIndex, int signatureIndex,
                                                          boolean registerIsSigned) {
                        this.length++;
                        if (dexFile.getPreserveSignedRegisters() && registerIsSigned) {
                            this.length += Leb128Utils.signedLeb128Size(registerNum);
                        } else {
                            this.length+=Leb128Utils.unsignedLeb128Size(registerNum);
                        }
                        if (nameIndex != -1) {
                            this.length+=
                               Leb128Utils.unsignedLeb128Size(referencedItems[referencedItemsPosition++].getIndex()+1);
                        } else {
                            this.length++;
                        }
                        if (typeIndex != -1) {
                            this.length+=
                               Leb128Utils.unsignedLeb128Size(referencedItems[referencedItemsPosition++].getIndex()+1);
                        } else {
                            this.length++;
                        }
                        if (signatureIndex != -1) {
                            this.length+=
                               Leb128Utils.unsignedLeb128Size(referencedItems[referencedItemsPosition++].getIndex()+1);
                        } else {
                            this.length++;
                        }
                    }

                    @Override
                    public void ProcessSetFile(int startDebugOffset, int length, int nameIndex) {
                        this.length++;
                        if (nameIndex != -1) {
                            this.length+=
                               Leb128Utils.unsignedLeb128Size(referencedItems[referencedItemsPosition++].getIndex()+1);
                        } else {
                            this.length++;
                        }
                    }
                });
        return offset + pdidwl.length;
    }

    /** {@inheritDoc} */
    protected void writeItem(final AnnotatedOutput out) {
        if (out.annotates()) {
            writeItemWithAnnotations(out);
        } else {
            writeItemWithNoAnnotations(out);
        }
    }

    /**
     * Replaces the encoded debug info for this DebugInfoItem. It is expected that the new debug info is compatible
     * with the existing information, i.e. lineStart, referencedItems, parameterNames
     * @param encodedDebugInfo the new encoded debug info
     */
    protected void setEncodedDebugInfo(byte[] encodedDebugInfo) {
        //TODO: I would rather replace this method with some way of saying "The (code) instruction at address changed from A bytes to B bytes. Fixup the debug info accordingly"

        this.encodedDebugInfo = encodedDebugInfo;
    }

    /**
     * Helper method that writes the item, without writing annotations
     * @param out the AnnotatedOutput object
     */
    private void writeItemWithNoAnnotations(final AnnotatedOutput out) {
        out.writeUnsignedLeb128(lineStart);
        out.writeUnsignedLeb128(parameterNames.length);
        for (StringIdItem parameterName: parameterNames) {
            int indexp1;
            if (parameterName == null) {
                indexp1 = 0;
            } else {
                indexp1 = parameterName.getIndex() + 1;
            }
            out.writeUnsignedLeb128(indexp1);
        }

        DebugInstructionIterator.IterateInstructions(new ByteArrayInput(encodedDebugInfo),
                new DebugInstructionIterator.ProcessRawDebugInstructionDelegate() {
                    private int referencedItemsPosition = 0;

                    @Override
                    public void ProcessStaticOpcode(DebugOpcode opcode, int startDebugOffset, int length) {
                        out.write(encodedDebugInfo, startDebugOffset, length);
                    }

                    @Override
                    public void ProcessStartLocal(int startDebugOffset, int length, int registerNum, int nameIndex,
                                                  int typeIndex, boolean registerIsSigned) {
                        out.writeByte(DebugOpcode.DBG_START_LOCAL.value);
                        if (dexFile.getPreserveSignedRegisters() && registerIsSigned) {
                            out.writeSignedLeb128(registerNum);
                        } else {
                            out.writeUnsignedLeb128(registerNum);
                        }
                        if (nameIndex != -1) {
                            out.writeUnsignedLeb128(referencedItems[referencedItemsPosition++].getIndex() + 1);
                        } else {
                            out.writeByte(0);
                        }
                        if (typeIndex != -1) {
                            out.writeUnsignedLeb128(referencedItems[referencedItemsPosition++].getIndex() + 1);
                        } else {
                            out.writeByte(0);
                        }
                    }

                    @Override
                    public void ProcessStartLocalExtended(int startDebugOffset, int length, int registerNum, int nameIndex,
                                                          int typeIndex, int signatureIndex,
                                                          boolean registerIsSigned) {
                        out.writeByte(DebugOpcode.DBG_START_LOCAL_EXTENDED.value);
                        if (dexFile.getPreserveSignedRegisters() && registerIsSigned) {
                            out.writeSignedLeb128(registerNum);
                        } else {
                            out.writeUnsignedLeb128(registerNum);
                        }
                        if (nameIndex != -1) {
                            out.writeUnsignedLeb128(referencedItems[referencedItemsPosition++].getIndex() + 1);
                        } else {
                            out.writeByte(0);
                        }
                        if (typeIndex != -1) {
                            out.writeUnsignedLeb128(referencedItems[referencedItemsPosition++].getIndex() + 1);
                        } else {
                            out.writeByte(0);
                        }
                        if (signatureIndex != -1) {
                            out.writeUnsignedLeb128(referencedItems[referencedItemsPosition++].getIndex() + 1);
                        } else {
                            out.writeByte(0);
                        }
                    }

                    @Override
                    public void ProcessSetFile(int startDebugOffset, int length, int nameIndex) {
                        out.writeByte(DebugOpcode.DBG_SET_FILE.value);
                        if (nameIndex != -1) {
                            out.writeUnsignedLeb128(referencedItems[referencedItemsPosition++].getIndex() + 1);
                        } else {
                            out.writeByte(0);
                        }
                    }
                });
    }

    /**
     * Helper method that writes and annotates the item
     * @param out the AnnotatedOutput object
     */
    private void writeItemWithAnnotations(final AnnotatedOutput out) {
        out.annotate(0, parent.getParent().method.getMethodString());
        out.annotate("line_start: 0x" + Integer.toHexString(lineStart) + " (" + lineStart + ")");
        out.writeUnsignedLeb128(lineStart);
        out.annotate("parameters_size: 0x" + Integer.toHexString(parameterNames.length) + " (" + parameterNames.length
                + ")");
        out.writeUnsignedLeb128(parameterNames.length);
        int index = 0;
        for (StringIdItem parameterName: parameterNames) {
            int indexp1;
            if (parameterName == null) {
                out.annotate("[" + index++ +"] parameterName: ");
                indexp1 = 0;
            } else {
                out.annotate("[" + index++ +"] parameterName: " + parameterName.getStringValue());
                indexp1 = parameterName.getIndex() + 1;
            }
            out.writeUnsignedLeb128(indexp1);
        }

        DebugInstructionIterator.IterateInstructions(new ByteArrayInput(encodedDebugInfo),
                new DebugInstructionIterator.ProcessRawDebugInstructionDelegate() {
                    private int referencedItemsPosition = 0;

                    @Override
                    public void ProcessEndSequence(int startDebugOffset) {
                        out.annotate("DBG_END_SEQUENCE");
                        out.writeByte(DebugOpcode.DBG_END_SEQUENCE.value);
                    }

                    @Override
                    public void ProcessAdvancePC(int startDebugOffset, int length, int addressDiff) {
                        out.annotate("DBG_ADVANCE_PC");
                        out.writeByte(DebugOpcode.DBG_ADVANCE_PC.value);
                        out.indent();
                        out.annotate("addr_diff: 0x" + Integer.toHexString(addressDiff) + " (" + addressDiff + ")");
                        out.writeUnsignedLeb128(addressDiff);
                        out.deindent();
                    }

                    @Override
                    public void ProcessAdvanceLine(int startDebugOffset, int length, int lineDiff) {
                        out.annotate("DBG_ADVANCE_LINE");
                        out.writeByte(DebugOpcode.DBG_ADVANCE_LINE.value);
                        out.indent();
                        out.annotate("line_diff: 0x" + Integer.toHexString(lineDiff) + " (" + lineDiff + ")");
                        out.writeSignedLeb128(lineDiff);
                        out.deindent();
                    }

                    @Override
                    public void ProcessStartLocal(int startDebugOffset, int length, int registerNum, int nameIndex,
                                                  int typeIndex, boolean registerIsSigned) {
                        out.annotate("DBG_START_LOCAL");
                        out.writeByte(DebugOpcode.DBG_START_LOCAL.value);
                        out.indent();
                        out.annotate("register_num: 0x" + Integer.toHexString(registerNum) + " (" + registerNum + ")");
                        if (dexFile.getPreserveSignedRegisters() && registerIsSigned) {
                            out.writeSignedLeb128(registerNum);
                        } else {
                            out.writeUnsignedLeb128(registerNum);
                        }
                        if (nameIndex != -1) {
                            Item nameItem = referencedItems[referencedItemsPosition++];
                            assert nameItem instanceof StringIdItem;
                            out.annotate("name: " + ((StringIdItem)nameItem).getStringValue());
                            out.writeUnsignedLeb128(nameItem.getIndex() + 1);
                        } else {
                            out.annotate("name: ");
                            out.writeByte(0);
                        }
                        if (typeIndex != -1) {
                            Item typeItem = referencedItems[referencedItemsPosition++];
                            assert typeItem instanceof TypeIdItem;
                            out.annotate("type: " + ((TypeIdItem)typeItem).getTypeDescriptor());
                            out.writeUnsignedLeb128(typeItem.getIndex() + 1);
                        } else {
                            out.annotate("type: ");
                            out.writeByte(0);
                        }
                        out.deindent();
                    }

                    @Override
                    public void ProcessStartLocalExtended(int startDebugOffset, int length, int registerNum,
                                                          int nameIndex, int typeIndex, int signatureIndex,
                                                          boolean registerIsSigned) {
                        out.annotate("DBG_START_LOCAL_EXTENDED");
                        out.writeByte(DebugOpcode.DBG_START_LOCAL_EXTENDED.value);
                        out.indent();
                        out.annotate("register_num: 0x" + Integer.toHexString(registerNum) + " (" + registerNum + ")");
                        if (dexFile.getPreserveSignedRegisters() && registerIsSigned) {
                            out.writeSignedLeb128(registerNum);
                        } else {
                            out.writeUnsignedLeb128(registerNum);
                        }
                        if (nameIndex != -1) {
                            Item nameItem = referencedItems[referencedItemsPosition++];
                            assert nameItem instanceof StringIdItem;
                            out.annotate("name: " + ((StringIdItem)nameItem).getStringValue());
                            out.writeUnsignedLeb128(nameItem.getIndex() + 1);
                        } else {
                            out.annotate("name: ");
                            out.writeByte(0);
                        }
                        if (typeIndex != -1) {
                            Item typeItem = referencedItems[referencedItemsPosition++];
                            assert typeItem instanceof TypeIdItem;
                            out.annotate("type: " + ((TypeIdItem)typeItem).getTypeDescriptor());
                            out.writeUnsignedLeb128(typeItem.getIndex() + 1);
                        } else {
                            out.annotate("type: ");
                            out.writeByte(0);
                        }
                        if (signatureIndex != -1) {
                            Item signatureItem = referencedItems[referencedItemsPosition++];
                            assert signatureItem instanceof StringIdItem;
                            out.annotate("signature: " + ((StringIdItem)signatureItem).getStringValue());
                            out.writeUnsignedLeb128(signatureItem.getIndex() + 1);
                        } else {
                            out.annotate("signature: ");
                            out.writeByte(0);
                        }
                        out.deindent();
                    }

                    @Override
                    public void ProcessEndLocal(int startDebugOffset, int length, int registerNum,
                                                boolean registerIsSigned) {
                        out.annotate("DBG_END_LOCAL");
                        out.writeByte(DebugOpcode.DBG_END_LOCAL.value);
                        out.annotate("register_num: 0x" + Integer.toHexString(registerNum) + " (" + registerNum + ")");
                        if (registerIsSigned) {
                            out.writeSignedLeb128(registerNum);
                        } else {
                            out.writeUnsignedLeb128(registerNum);
                        }
                    }

                    @Override
                    public void ProcessRestartLocal(int startDebugOffset, int length, int registerNum,
                                                    boolean registerIsSigned) {
                        out.annotate("DBG_RESTART_LOCAL");
                        out.writeByte(DebugOpcode.DBG_RESTART_LOCAL.value);
                        out.annotate("register_num: 0x" + Integer.toHexString(registerNum) + " (" + registerNum + ")");
                        if (registerIsSigned) {
                            out.writeSignedLeb128(registerNum);
                        } else {
                            out.writeUnsignedLeb128(registerNum);
                        }
                    }

                    @Override
                    public void ProcessSetPrologueEnd(int startDebugOffset) {
                        out.annotate("DBG_SET_PROLOGUE_END");
                        out.writeByte(DebugOpcode.DBG_SET_PROLOGUE_END.value);
                    }

                    @Override
                    public void ProcessSetEpilogueBegin(int startDebugOffset) {
                        out.annotate("DBG_SET_EPILOGUE_BEGIN");
                        out.writeByte(DebugOpcode.DBG_SET_EPILOGUE_BEGIN.value);
                    }

                    @Override
                    public void ProcessSetFile(int startDebugOffset, int length, int nameIndex) {
                        out.annotate("DBG_SET_FILE");
                        out.writeByte(DebugOpcode.DBG_SET_FILE.value);
                        if (nameIndex != -1) {
                            Item sourceItem = referencedItems[referencedItemsPosition++];
                            assert sourceItem instanceof StringIdItem;
                            out.annotate("source_file: \"" + ((StringIdItem)sourceItem).getStringValue() + "\"");
                            out.writeUnsignedLeb128(sourceItem.getIndex() + 1);
                        } else {
                            out.annotate("source_file: ");
                            out.writeByte(0);
                        }
                    }

                    @Override
                    public void ProcessSpecialOpcode(int startDebugOffset, int debugOpcode, int lineDiff,
                                                     int addressDiff) {
                        out.annotate("DBG_SPECIAL_OPCODE: line_diff=0x" + Integer.toHexString(lineDiff) + "(" +
                                lineDiff +"),addressDiff=0x" + Integer.toHexString(addressDiff) + "(" + addressDiff +
                                ")");
                        out.writeByte(debugOpcode);
                    }
                });
    }




    /** {@inheritDoc} */
    public ItemType getItemType() {
        return ItemType.TYPE_DEBUG_INFO_ITEM;
    }

    /** {@inheritDoc} */
    public String getConciseIdentity() {
        return "debug_info_item @0x" + Integer.toHexString(getOffset());
    }

    /** {@inheritDoc} */
    public int compareTo(DebugInfoItem other) {
        if (parent == null) {
            if (other.parent == null) {
                return 0;
            }
            return -1;
        }
        if (other.parent == null) {
            return 1;
        }
        return parent.compareTo(other.parent);
    }

    /**
     * Set the <code>CodeItem</code> that this <code>DebugInfoItem</code> is associated with
     * @param codeItem the <code>CodeItem</code> that this <code>DebugInfoItem</code> is associated with
     */
    protected void setParent(CodeItem codeItem) {
        this.parent = codeItem;
    }

    /**
     * @return the initial value for the line number register for the debug info machine
     */
    public int getLineStart() {
        return lineStart;
    }

    /**
     * @return the debug info, encoded as a byte array
     */
    public byte[] getEncodedDebugInfo() {
        return encodedDebugInfo;
    }

    /**
     * @return an array of the items referenced by instructions, in order of occurance in the encoded debug info
     */
    public Item[] getReferencedItems() {
        return referencedItems;
    }

    /**
     * @return an array of the names of the associated method's parameters. The array can be null if no parameter info
     * is available, or any element can be null to indicate no info for that parameter
     */
    public StringIdItem[] getParameterNames() {
        return parameterNames;
    }
}
