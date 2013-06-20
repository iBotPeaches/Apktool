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

import com.google.common.base.Preconditions;
import org.jf.dexlib.Util.AnnotatedOutput;
import org.jf.dexlib.Util.Input;
import org.jf.util.StringUtils;

public class HeaderItem extends Item<HeaderItem> {
    /**
     * the file format magic number, represented as the
     * low-order bytes of a string
     */
    public static final byte[][] MAGIC_VALUES = new byte[][] {
            new byte[] {0x64, 0x65, 0x78, 0x0A, 0x30, 0x33, 0x35, 0x00}, //"dex\n035" + '\0';
            new byte[] {0x64, 0x65, 0x78, 0x0A, 0x30, 0x33, 0x36, 0x00}}; //"dex\n036" + '\0';


    /** size of this section, in bytes */
    private static final int HEADER_SIZE = 0x70;

    /** the endianness constants */
    private static final int LITTLE_ENDIAN = 0x12345678;
    private static final int BIG_ENDIAN = 0x78563412;

    /* Which magic value to use when writing out the header item */
    private int magic_index = 0;

    private boolean checksumSignatureSet = false;
    private int checksum;
    private byte[] signature;

    /**
     * Create a new uninitialized <code>HeaderItem</code>
     * @param dexFile The <code>DexFile</code> containing this <code>HeaderItem</code>
     */
    protected HeaderItem(final DexFile dexFile) {
        super(dexFile);
    }

    /** {@inheritDoc} */
    protected void readItem(Input in, ReadContext readContext) {
        byte[] readMagic = in.readBytes(8);

        boolean success = false;
        for (int i=0; i<MAGIC_VALUES.length; i++) {
            byte[] magic_value = MAGIC_VALUES[i];
            boolean matched = true;
            for (int j=0; j<8; j++) {
                if (magic_value[j] != readMagic[j]) {
                    matched = false;
                    break;
                }
            }
            if (matched) {
                success = true;
                magic_index = i;
                break;
            }
        }

        if (!success) {
            throw new RuntimeException("Unrecognized dex magic value");
        }

        checksum = in.readInt(); //checksum
        signature = in.readBytes(20); //signature
        checksumSignatureSet = true;

        in.readInt(); //filesize
        in.readInt(); //header size

        int endianTag = in.readInt();
        if (endianTag == BIG_ENDIAN) {
            throw new RuntimeException("This dex file is big endian. Only little endian is currently supported.");
        } else if (endianTag != LITTLE_ENDIAN) {
            throw new RuntimeException("The endian tag is not 0x12345678 or 0x78563412");
        }

        //link_size + link_off
        if ((in.readInt() | in.readInt()) != 0) {
            System.err.println("This dex file has a link section, which is not supported. Ignoring.");
        }

        int sectionSize;
        int sectionOffset;

        //map_offset
        sectionOffset = in.readInt();
        readContext.addSection(ItemType.TYPE_MAP_LIST, 1, sectionOffset);

        //string_id_item
        sectionSize = in.readInt();
        sectionOffset = in.readInt();
        readContext.addSection(ItemType.TYPE_STRING_ID_ITEM, sectionSize, sectionOffset);

        //type_id_item
        sectionSize = in.readInt();
        sectionOffset = in.readInt();
        readContext.addSection(ItemType.TYPE_TYPE_ID_ITEM, sectionSize, sectionOffset);

        //proto_id_item
        sectionSize = in.readInt();
        sectionOffset = in.readInt();
        readContext.addSection(ItemType.TYPE_PROTO_ID_ITEM, sectionSize, sectionOffset);

        //field_id_item
        sectionSize = in.readInt();
        sectionOffset = in.readInt();
        readContext.addSection(ItemType.TYPE_FIELD_ID_ITEM, sectionSize, sectionOffset);

        //method_id_item
        sectionSize = in.readInt();
        sectionOffset = in.readInt();
        readContext.addSection(ItemType.TYPE_METHOD_ID_ITEM, sectionSize, sectionOffset);

        //class_data_item
        sectionSize = in.readInt();
        sectionOffset = in.readInt();
        readContext.addSection(ItemType.TYPE_CLASS_DEF_ITEM, sectionSize, sectionOffset);

        in.readInt(); //data_size
        in.readInt(); //data_off
    }

    /**
     * Sets the dex version number.
     *
     * 35 is the default.
     * 36 is for dex files that use extended opcodes (only works with ICS+)
     *
     * @param version - must be either 35 or 36
     */
    public void setVersion(int version) {
        if (version == 35) {
            magic_index = 0;
            return;
        }
        if (version == 36) {
            magic_index = 1;
            return;
        }
        throw new RuntimeException("Invalid dex version number passed to setVersion");
    }

    /** {@inheritDoc} */
    protected int placeItem(int offset) {
        return HEADER_SIZE;
    }

    /** {@inheritDoc} */
    protected void writeItem(AnnotatedOutput out) {
        StringBuilder magicBuilder = new StringBuilder();
        for (int i=0; i<8; i++) {
            magicBuilder.append((char)MAGIC_VALUES[magic_index][i]);
        }

        out.annotate("magic: " + StringUtils.escapeString(magicBuilder.toString()));
        out.write(MAGIC_VALUES[magic_index]);

        out.annotate("checksum");
        out.writeInt(0);

        out.annotate("signature");
        out.write(new byte[20]);

        out.annotate("file_size: 0x" + Integer.toHexString(dexFile.getFileSize()) + " (" + dexFile.getFileSize() +
                " bytes)");
        out.writeInt(dexFile.getFileSize());

        out.annotate("header_size: 0x" + Integer.toHexString(HEADER_SIZE));
        out.writeInt(HEADER_SIZE);

        out.annotate("endian_tag: 0x" + Integer.toHexString(LITTLE_ENDIAN));
        out.writeInt(LITTLE_ENDIAN);

        out.annotate("link_size: 0");
        out.writeInt(0);

        out.annotate("link_off: 0");
        out.writeInt(0);

        out.annotate("map_off: 0x" + Integer.toHexString(dexFile.MapItem.getOffset()));
        out.writeInt(dexFile.MapItem.getOffset());

        out.annotate("string_ids_size: " + dexFile.StringIdsSection.getItems().size());
        out.writeInt(dexFile.StringIdsSection.getItems().size());

        out.annotate("string_ids_off: 0x" + Integer.toHexString(dexFile.StringIdsSection.getOffset()));
        out.writeInt(dexFile.StringIdsSection.getOffset());

        out.annotate("type_ids_size: " + dexFile.TypeIdsSection.getItems().size());
        out.writeInt(dexFile.TypeIdsSection.getItems().size());

        out.annotate("type_ids_off: 0x" + Integer.toHexString(dexFile.TypeIdsSection.getOffset()));
        out.writeInt(dexFile.TypeIdsSection.getOffset());

        out.annotate("proto_ids_size: " + dexFile.ProtoIdsSection.getItems().size());
        out.writeInt(dexFile.ProtoIdsSection.getItems().size());

        out.annotate("proto_ids_off: 0x" + Integer.toHexString(dexFile.ProtoIdsSection.getOffset()));
        out.writeInt(dexFile.ProtoIdsSection.getOffset());

        out.annotate("field_ids_size: " + dexFile.FieldIdsSection.getItems().size());
        out.writeInt(dexFile.FieldIdsSection.getItems().size());

        out.annotate("field_ids_off: 0x" + Integer.toHexString(dexFile.FieldIdsSection.getOffset()));
        out.writeInt(dexFile.FieldIdsSection.getOffset());

        out.annotate("method_ids_size: " + dexFile.MethodIdsSection.getItems().size());
        out.writeInt(dexFile.MethodIdsSection.getItems().size());

        out.annotate("method_ids_off: 0x" + Integer.toHexString(dexFile.MethodIdsSection.getOffset()));
        out.writeInt(dexFile.MethodIdsSection.getOffset());

        out.annotate("class_defs_size: " + dexFile.ClassDefsSection.getItems().size());
        out.writeInt(dexFile.ClassDefsSection.getItems().size());

        out.annotate("class_defs_off: 0x" + Integer.toHexString(dexFile.ClassDefsSection.getOffset()));
        out.writeInt(dexFile.ClassDefsSection.getOffset());

        out.annotate("data_size: 0x" + Integer.toHexString(dexFile.getDataSize()) + " (" + dexFile.getDataSize() +
                " bytes)");
        out.writeInt(dexFile.getDataSize());

        out.annotate("data_off: 0x" + Integer.toHexString(dexFile.getDataOffset()));
        out.writeInt(dexFile.getDataOffset());
    }

    /** {@inheritDoc} */
    public ItemType getItemType() {
        return ItemType.TYPE_HEADER_ITEM;
    }

    /** {@inheritDoc} */
    public String getConciseIdentity() {
        return "header_item";
    }

    /** {@inheritDoc} */
    public int compareTo(HeaderItem o) {
        //there is only 1 header item
        return 0;
    }

    /**
     * Get the checksum that was originally stored as part of this header item
     *
     * Note that this should only be called if this HeaderItem is from a DexFile that was read from disk, as opposed
     * to one that is created from scratch.
     *
     * @return The addler32 checksum (as an integer) of the dex file
     */
    public int getChecksum() {
        Preconditions.checkState(checksumSignatureSet,
                "This can only be called on a DexFile that was read from disk.");
        return checksum;
    }

    /**
     * Get the signature that was originally stored as part of this header item
     *
     * Note that this should only be called if this HeaderItem is from a DexFile that was read from disk, as opposed
     * to one that is created from scratch.
     *
     * @return The sha1 checksum of the dex file, as a 20-element byte array
     */
    public byte[] getSignature() {
        Preconditions.checkState(checksumSignatureSet,
                "This can only be called on a DexFile that was read from disk.");
        return signature;
    }

}
