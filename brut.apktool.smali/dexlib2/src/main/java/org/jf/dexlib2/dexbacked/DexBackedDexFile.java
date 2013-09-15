/*
 * Copyright 2012, Google Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 *     * Neither the name of Google Inc. nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.jf.dexlib2.dexbacked;

import com.google.common.io.ByteStreams;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.dexbacked.raw.*;
import org.jf.dexlib2.dexbacked.util.FixedSizeSet;
import org.jf.dexlib2.iface.DexFile;
import org.jf.util.ExceptionWithContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

public class DexBackedDexFile extends BaseDexBuffer implements DexFile {
    private final Opcodes opcodes;

    private final int stringCount;
    private final int stringStartOffset;
    private final int typeCount;
    private final int typeStartOffset;
    private final int protoCount;
    private final int protoStartOffset;
    private final int fieldCount;
    private final int fieldStartOffset;
    private final int methodCount;
    private final int methodStartOffset;
    private final int classCount;
    private final int classStartOffset;

    private DexBackedDexFile(Opcodes opcodes, @Nonnull byte[] buf, int offset, boolean verifyMagic) {
        super(buf);

        this.opcodes = opcodes;

        if (verifyMagic) {
            verifyMagicAndByteOrder(buf, offset);
        }

        stringCount = readSmallUint(HeaderItem.STRING_COUNT_OFFSET);
        stringStartOffset = readSmallUint(HeaderItem.STRING_START_OFFSET);
        typeCount = readSmallUint(HeaderItem.TYPE_COUNT_OFFSET);
        typeStartOffset = readSmallUint(HeaderItem.TYPE_START_OFFSET);
        protoCount = readSmallUint(HeaderItem.PROTO_COUNT_OFFSET);
        protoStartOffset = readSmallUint(HeaderItem.PROTO_START_OFFSET);
        fieldCount = readSmallUint(HeaderItem.FIELD_COUNT_OFFSET);
        fieldStartOffset = readSmallUint(HeaderItem.FIELD_START_OFFSET);
        methodCount = readSmallUint(HeaderItem.METHOD_COUNT_OFFSET);
        methodStartOffset = readSmallUint(HeaderItem.METHOD_START_OFFSET);
        classCount = readSmallUint(HeaderItem.CLASS_COUNT_OFFSET);
        classStartOffset = readSmallUint(HeaderItem.CLASS_START_OFFSET);
    }

    public DexBackedDexFile(@Nonnull Opcodes opcodes, @Nonnull BaseDexBuffer buf) {
        this(opcodes, buf.buf);
    }

    public DexBackedDexFile(@Nonnull Opcodes opcodes, @Nonnull byte[] buf, int offset) {
        this(opcodes, buf, offset, false);
    }

    public DexBackedDexFile(@Nonnull Opcodes opcodes, @Nonnull byte[] buf) {
        this(opcodes, buf, 0, true);
    }

    public static DexBackedDexFile fromInputStream(@Nonnull Opcodes opcodes, @Nonnull InputStream is)
            throws IOException {
        if (!is.markSupported()) {
            throw new IllegalArgumentException("InputStream must support mark");
        }
        is.mark(44);
        byte[] partialHeader = new byte[44];
        try {
            ByteStreams.readFully(is, partialHeader);
        } catch (EOFException ex) {
            throw new NotADexFile("File is too short");
        } finally {
            is.reset();
        }

        verifyMagicAndByteOrder(partialHeader, 0);

        byte[] buf = ByteStreams.toByteArray(is);
        return new DexBackedDexFile(opcodes, buf, 0, false);
    }

    public Opcodes getOpcodes() {
        return opcodes;
    }

    public boolean isOdexFile() {
        return false;
    }

    @Nonnull
    @Override
    public Set<? extends DexBackedClassDef> getClasses() {
        return new FixedSizeSet<DexBackedClassDef>() {
            @Nonnull
            @Override
            public DexBackedClassDef readItem(int index) {
                return new DexBackedClassDef(DexBackedDexFile.this, getClassDefItemOffset(index));
            }

            @Override
            public int size() {
                return classCount;
            }
        };
    }

    private static void verifyMagicAndByteOrder(@Nonnull byte[] buf, int offset) {
        if (!HeaderItem.verifyMagic(buf, offset)) {
            StringBuilder sb = new StringBuilder("Invalid magic value:");
            for (int i=0; i<8; i++) {
                sb.append(String.format(" %02x", buf[i]));
            }
            throw new NotADexFile(sb.toString());
        }

        int endian = HeaderItem.getEndian(buf, offset);
        if (endian == HeaderItem.BIG_ENDIAN_TAG) {
            throw new ExceptionWithContext("Big endian dex files are not currently supported");
        }

        if (endian != HeaderItem.LITTLE_ENDIAN_TAG) {
            throw new ExceptionWithContext("Invalid endian tag: 0x%x", endian);
        }
    }

    public int getStringIdItemOffset(int stringIndex) {
        if (stringIndex < 0 || stringIndex >= stringCount) {
            throw new InvalidItemIndex(stringIndex, "String index out of bounds: %d", stringIndex);
        }
        return stringStartOffset + stringIndex*StringIdItem.ITEM_SIZE;
    }

    public int getTypeIdItemOffset(int typeIndex) {
        if (typeIndex < 0 || typeIndex >= typeCount) {
            throw new InvalidItemIndex(typeIndex, "Type index out of bounds: %d", typeIndex);
        }
        return typeStartOffset + typeIndex*TypeIdItem.ITEM_SIZE;
    }

    public int getFieldIdItemOffset(int fieldIndex) {
        if (fieldIndex < 0 || fieldIndex >= fieldCount) {
            throw new InvalidItemIndex(fieldIndex, "Field index out of bounds: %d", fieldIndex);
        }
        return fieldStartOffset + fieldIndex*FieldIdItem.ITEM_SIZE;
    }

    public int getMethodIdItemOffset(int methodIndex) {
        if (methodIndex < 0 || methodIndex >= methodCount) {
            throw new InvalidItemIndex(methodIndex, "Method findex out of bounds: %d", methodIndex);
        }
        return methodStartOffset + methodIndex*MethodIdItem.ITEM_SIZE;
    }

    public int getProtoIdItemOffset(int protoIndex) {
        if (protoIndex < 0 || protoIndex >= protoCount) {
            throw new InvalidItemIndex(protoIndex, "Proto index out of bounds: %d", protoIndex);
        }
        return protoStartOffset + protoIndex*ProtoIdItem.ITEM_SIZE;
    }

    public int getClassDefItemOffset(int classIndex) {
        if (classIndex < 0 || classIndex >= classCount) {
            throw new InvalidItemIndex(classIndex, "Class index out of bounds: %d", classIndex);
        }
        return classStartOffset + classIndex*ClassDefItem.ITEM_SIZE;
    }

    public int getClassCount() {
        return classCount;
    }

    @Nonnull
    public String getString(int stringIndex) {
        int stringOffset = getStringIdItemOffset(stringIndex);
        int stringDataOffset = readSmallUint(stringOffset);
        DexReader reader = readerAt(stringDataOffset);
        int utf16Length = reader.readSmallUleb128();
        return reader.readString(utf16Length);
    }

    @Nullable
    public String getOptionalString(int stringIndex) {
        if (stringIndex == -1) {
            return null;
        }
        return getString(stringIndex);
    }

    @Nonnull
    public String getType(int typeIndex) {
        int typeOffset = getTypeIdItemOffset(typeIndex);
        int stringIndex = readSmallUint(typeOffset);
        return getString(stringIndex);
    }

    @Nullable
    public String getOptionalType(int typeIndex) {
        if (typeIndex == -1) {
            return null;
        }
        return getType(typeIndex);
    }

    @Override
    @Nonnull
    public DexReader readerAt(int offset) {
        return new DexReader(this, offset);
    }

    public static class NotADexFile extends RuntimeException {
        public NotADexFile() {
        }

        public NotADexFile(Throwable cause) {
            super(cause);
        }

        public NotADexFile(String message) {
            super(message);
        }

        public NotADexFile(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class InvalidItemIndex extends ExceptionWithContext {
        private final int itemIndex;

        public InvalidItemIndex(int itemIndex) {
            super("");
            this.itemIndex = itemIndex;
        }

        public InvalidItemIndex(int itemIndex, String message, Object... formatArgs) {
            super(message, formatArgs);
            this.itemIndex = itemIndex;
        }

        public int getInvalidIndex() {
            return itemIndex;
        }
    }
}
