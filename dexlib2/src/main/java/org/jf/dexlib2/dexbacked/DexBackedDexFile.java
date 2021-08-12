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
import org.jf.dexlib2.ReferenceType;
import org.jf.dexlib2.dexbacked.raw.*;
import org.jf.dexlib2.dexbacked.reference.*;
import org.jf.dexlib2.dexbacked.util.FixedSizeList;
import org.jf.dexlib2.dexbacked.util.FixedSizeSet;
import org.jf.dexlib2.iface.DexFile;
import org.jf.dexlib2.iface.reference.*;
import org.jf.dexlib2.util.DexUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.util.AbstractList;
import java.util.List;
import java.util.Set;

import static org.jf.dexlib2.writer.DexWriter.NO_OFFSET;

public class DexBackedDexFile implements DexFile {

    private final DexBuffer dexBuffer;
    private final DexBuffer dataBuffer;

    @Nonnull private final Opcodes opcodes;

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
    private final int mapOffset;
    private final int hiddenApiRestrictionsOffset;

    protected DexBackedDexFile(@Nullable Opcodes opcodes, @Nonnull byte[] buf, int offset, boolean verifyMagic) {
        dexBuffer = new DexBuffer(buf, offset);
        dataBuffer = new DexBuffer(buf, offset + getBaseDataOffset());

        int dexVersion = getVersion(buf, offset, verifyMagic);

        if (opcodes == null) {
            this.opcodes = getDefaultOpcodes(dexVersion);
        } else {
            this.opcodes = opcodes;
        }

        stringCount = dexBuffer.readSmallUint(HeaderItem.STRING_COUNT_OFFSET);
        stringStartOffset = dexBuffer.readSmallUint(HeaderItem.STRING_START_OFFSET);
        typeCount = dexBuffer.readSmallUint(HeaderItem.TYPE_COUNT_OFFSET);
        typeStartOffset = dexBuffer.readSmallUint(HeaderItem.TYPE_START_OFFSET);
        protoCount = dexBuffer.readSmallUint(HeaderItem.PROTO_COUNT_OFFSET);
        protoStartOffset = dexBuffer.readSmallUint(HeaderItem.PROTO_START_OFFSET);
        fieldCount = dexBuffer.readSmallUint(HeaderItem.FIELD_COUNT_OFFSET);
        fieldStartOffset = dexBuffer.readSmallUint(HeaderItem.FIELD_START_OFFSET);
        methodCount = dexBuffer.readSmallUint(HeaderItem.METHOD_COUNT_OFFSET);
        methodStartOffset = dexBuffer.readSmallUint(HeaderItem.METHOD_START_OFFSET);
        classCount = dexBuffer.readSmallUint(HeaderItem.CLASS_COUNT_OFFSET);
        classStartOffset = dexBuffer.readSmallUint(HeaderItem.CLASS_START_OFFSET);
        mapOffset = dexBuffer.readSmallUint(HeaderItem.MAP_OFFSET);

        MapItem mapItem = getMapItemForSection(ItemType.HIDDENAPI_CLASS_DATA_ITEM);
        if (mapItem != null) {
            hiddenApiRestrictionsOffset = mapItem.getOffset();
        } else {
            hiddenApiRestrictionsOffset = NO_OFFSET;
        }
    }

    protected DexBackedDexFile(@Nullable Opcodes opcodes, @Nonnull DexBuffer dexBuffer, @Nonnull DexBuffer dataBuffer, int offset, boolean verifyMagic) {
        this.dexBuffer = dexBuffer;
        this.dataBuffer = dataBuffer;

        byte[] headerBuf = dexBuffer.readByteRange(offset, HeaderItem.ITEM_SIZE);

        int dexVersion = getVersion(headerBuf, offset, verifyMagic);

        if (opcodes == null) {
            this.opcodes = getDefaultOpcodes(dexVersion);
        } else {
            this.opcodes = opcodes;
        }

        stringCount = dexBuffer.readSmallUint(HeaderItem.STRING_COUNT_OFFSET);
        stringStartOffset = dexBuffer.readSmallUint(HeaderItem.STRING_START_OFFSET);
        typeCount = dexBuffer.readSmallUint(HeaderItem.TYPE_COUNT_OFFSET);
        typeStartOffset = dexBuffer.readSmallUint(HeaderItem.TYPE_START_OFFSET);
        protoCount = dexBuffer.readSmallUint(HeaderItem.PROTO_COUNT_OFFSET);
        protoStartOffset = dexBuffer.readSmallUint(HeaderItem.PROTO_START_OFFSET);
        fieldCount = dexBuffer.readSmallUint(HeaderItem.FIELD_COUNT_OFFSET);
        fieldStartOffset = dexBuffer.readSmallUint(HeaderItem.FIELD_START_OFFSET);
        methodCount = dexBuffer.readSmallUint(HeaderItem.METHOD_COUNT_OFFSET);
        methodStartOffset = dexBuffer.readSmallUint(HeaderItem.METHOD_START_OFFSET);
        classCount = dexBuffer.readSmallUint(HeaderItem.CLASS_COUNT_OFFSET);
        classStartOffset = dexBuffer.readSmallUint(HeaderItem.CLASS_START_OFFSET);
        mapOffset = dexBuffer.readSmallUint(HeaderItem.MAP_OFFSET);

        MapItem mapItem = getMapItemForSection(ItemType.HIDDENAPI_CLASS_DATA_ITEM);
        if (mapItem != null) {
            hiddenApiRestrictionsOffset = mapItem.getOffset();
        } else {
            hiddenApiRestrictionsOffset = NO_OFFSET;
        }
    }

    /**
     * @return The offset that various data offsets are relative to. This is always 0 for a dex file, but may be
     * different for other related formats (e.g. cdex).
     */
    public int getBaseDataOffset() {
        return 0;
    }

    protected int getVersion(byte[] buf, int offset, boolean verifyMagic) {
        if (verifyMagic) {
            return DexUtil.verifyDexHeader(buf, offset);
        } else {
            return HeaderItem.getVersion(buf, offset);
        }
    }

    protected Opcodes getDefaultOpcodes(int version) {
        return Opcodes.forDexVersion(version);
    }

    public DexBuffer getBuffer() {
        return dexBuffer;
    }

    public DexBuffer getDataBuffer() {
        return dataBuffer;
    }

    public DexBackedDexFile(@Nullable Opcodes opcodes, @Nonnull DexBuffer buf) {
        this(opcodes, buf.buf, buf.baseOffset);
    }

    public DexBackedDexFile(@Nullable Opcodes opcodes, @Nonnull byte[] buf, int offset) {
        this(opcodes, buf, offset, false);
    }

    public DexBackedDexFile(@Nullable Opcodes opcodes, @Nonnull byte[] buf) {
        this(opcodes, buf, 0, true);
    }

    @Nonnull
    public static DexBackedDexFile fromInputStream(@Nullable Opcodes opcodes, @Nonnull InputStream is)
            throws IOException {
        DexUtil.verifyDexHeader(is);

        byte[] buf = ByteStreams.toByteArray(is);
        return new DexBackedDexFile(opcodes, buf, 0, false);
    }

    @Nonnull public Opcodes getOpcodes() {
        return opcodes;
    }

    public boolean supportsOptimizedOpcodes() {
        return false;
    }

    @Nonnull
    public Set<? extends DexBackedClassDef> getClasses() {
        return new FixedSizeSet<DexBackedClassDef>() {
            @Nonnull
            @Override
            public DexBackedClassDef readItem(int index) {
                return getClassSection().get(index);
            }

            @Override
            public int size() {
                return classCount;
            }
        };
    }

    public List<DexBackedStringReference> getStringReferences() {
        return new AbstractList<DexBackedStringReference>() {
            @Override public DexBackedStringReference get(int index) {
                if (index < 0 || index >= getStringSection().size()) {
                    throw new IndexOutOfBoundsException();
                }
                return new DexBackedStringReference(DexBackedDexFile.this, index);
            }

            @Override public int size() {
                return getStringSection().size();
            }
        };
    }

    public List<DexBackedTypeReference> getTypeReferences() {
        return new AbstractList<DexBackedTypeReference>() {
            @Override public DexBackedTypeReference get(int index) {
                if (index < 0 || index >= getTypeSection().size()) {
                    throw new IndexOutOfBoundsException();
                }
                return new DexBackedTypeReference(DexBackedDexFile.this, index);
            }

            @Override public int size() {
                return getTypeSection().size();
            }
        };
    }

    public List<? extends Reference> getReferences(int referenceType) {
        switch (referenceType) {
            case ReferenceType.STRING:
                return getStringReferences();
            case ReferenceType.TYPE:
                return getTypeReferences();
            case ReferenceType.METHOD:
                return getMethodSection();
            case ReferenceType.FIELD:
                return getFieldSection();
            default:
                throw new IllegalArgumentException(String.format("Invalid reference type: %d", referenceType));
        }
    }

    public List<MapItem> getMapItems() {
        final int mapSize = dataBuffer.readSmallUint(mapOffset);

        return new FixedSizeList<MapItem>() {
            @Override
            public MapItem readItem(int index) {
                int mapItemOffset = mapOffset + 4 + index * MapItem.ITEM_SIZE;
                return new MapItem(DexBackedDexFile.this, mapItemOffset);
            }

            @Override public int size() {
                return mapSize;
            }
        };
    }

    @Nullable
    public MapItem getMapItemForSection(int itemType) {
        for (MapItem mapItem: getMapItems()) {
            if (mapItem.getType() == itemType) {
                return mapItem;
            }
        }
        return null;
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

    private OptionalIndexedSection<String> stringSection = new OptionalIndexedSection<String>() {
        @Override
        public String get(int index) {
            int stringOffset = getOffset(index);
            int stringDataOffset = dexBuffer.readSmallUint(stringOffset);
            DexReader reader = dataBuffer.readerAt(stringDataOffset);
            int utf16Length = reader.readSmallUleb128();
            return reader.readString(utf16Length);
        }

        @Override
        public int size() {
            return stringCount;
        }

        @Nullable
        @Override
        public String getOptional(int index) {
            if (index == -1) {
                return null;
            }
            return get(index);
        }

        @Override
        public int getOffset(int index) {
            if (index < 0 || index >= size()) {
                throw new IndexOutOfBoundsException(
                        String.format("Invalid string index %d, not in [0, %d)", index, size()));
            }
            return stringStartOffset + index*StringIdItem.ITEM_SIZE;
        }
    };

    public OptionalIndexedSection<String> getStringSection() {
        return stringSection;
    }

    private OptionalIndexedSection<String> typeSection = new OptionalIndexedSection<String>() {
        @Override
        public String get(int index) {
            int typeOffset = getOffset(index);
            int stringIndex = dexBuffer.readSmallUint(typeOffset);
            return getStringSection().get(stringIndex);
        }

        @Override
        public int size() {
            return typeCount;
        }

        @Nullable
        @Override
        public String getOptional(int index) {
            if (index == -1) {
                return null;
            }
            return get(index);
        }

        @Override
        public int getOffset(int index) {
            if (index < 0 || index >= size()) {
                throw new IndexOutOfBoundsException(
                        String.format("Invalid type index %d, not in [0, %d)", index, size()));
            }
            return typeStartOffset + index * TypeIdItem.ITEM_SIZE;
        }
    };

    public OptionalIndexedSection<String> getTypeSection() {
        return typeSection;
    }

    private IndexedSection<DexBackedFieldReference> fieldSection = new IndexedSection<DexBackedFieldReference>() {
        @Override
        public DexBackedFieldReference get(int index) {
            return new DexBackedFieldReference(DexBackedDexFile.this, index);
        }

        @Override
        public int size() {
            return fieldCount;
        }

        @Override
        public int getOffset(int index) {
            if (index < 0 || index >= size()) {
                throw new IndexOutOfBoundsException(
                        String.format("Invalid field index %d, not in [0, %d)", index, size()));
            }

            return fieldStartOffset + index * FieldIdItem.ITEM_SIZE;
        }
    };

    public IndexedSection<DexBackedFieldReference> getFieldSection() {
        return fieldSection;
    }

    private IndexedSection<DexBackedMethodReference> methodSection = new IndexedSection<DexBackedMethodReference>() {
        @Override
        public DexBackedMethodReference get(int index) {
            return new DexBackedMethodReference(DexBackedDexFile.this, index);
        }

        @Override
        public int size() {
            return methodCount;
        }

        @Override
        public int getOffset(int index) {
            if (index < 0 || index >= size()) {
                throw new IndexOutOfBoundsException(
                        String.format("Invalid method index %d, not in [0, %d)", index, size()));
            }

            return methodStartOffset + index * MethodIdItem.ITEM_SIZE;
        }
    };

    public IndexedSection<DexBackedMethodReference> getMethodSection() {
        return methodSection;
    }

    private IndexedSection<DexBackedMethodProtoReference> protoSection =
            new IndexedSection<DexBackedMethodProtoReference>() {
                @Override
                public DexBackedMethodProtoReference get(int index) {
                    return new DexBackedMethodProtoReference(DexBackedDexFile.this, index);
                }

                @Override
                public int size() {
                    return protoCount;
                }

                @Override
                public int getOffset(int index) {
                    if (index < 0 || index >= size()) {
                        throw new IndexOutOfBoundsException(
                                String.format("Invalid proto index %d, not in [0, %d)", index, size()));
                    }

                    return protoStartOffset + index * ProtoIdItem.ITEM_SIZE;
                }
            };

    public IndexedSection<DexBackedMethodProtoReference> getProtoSection() {
        return protoSection;
    }

    private IndexedSection<DexBackedClassDef> classSection = new IndexedSection<DexBackedClassDef>() {
        @Override
        public DexBackedClassDef get(int index) {
            return new DexBackedClassDef(DexBackedDexFile.this, getOffset(index),
                    readHiddenApiRestrictionsOffset(index));
        }

        @Override
        public int size() {
            return classCount;
        }

        @Override
        public int getOffset(int index) {
            if (index < 0 || index >= size()) {
                throw new IndexOutOfBoundsException(
                        String.format("Invalid class index %d, not in [0, %d)", index, size()));
            }

            return classStartOffset + index * ClassDefItem.ITEM_SIZE;
        }
    };

    public IndexedSection<DexBackedClassDef> getClassSection() {
        return classSection;
    }

    private IndexedSection<DexBackedCallSiteReference> callSiteSection =
            new IndexedSection<DexBackedCallSiteReference>() {
                @Override
                public DexBackedCallSiteReference get(int index) {
                    return new DexBackedCallSiteReference(DexBackedDexFile.this, index);
                }

                @Override
                public int size() {
                    MapItem mapItem = getMapItemForSection(ItemType.CALL_SITE_ID_ITEM);
                    if (mapItem == null) {
                        return 0;
                    }
                    return mapItem.getItemCount();
                }

                @Override
                public int getOffset(int index) {
                    MapItem mapItem = getMapItemForSection(ItemType.CALL_SITE_ID_ITEM);
                    if (index < 0 || index >= size()) {
                        throw new IndexOutOfBoundsException(
                                String.format("Invalid callsite index %d, not in [0, %d)", index, size()));
                    }
                    return mapItem.getOffset() + index * CallSiteIdItem.ITEM_SIZE;
                }
            };

    public IndexedSection<DexBackedCallSiteReference> getCallSiteSection() {
        return callSiteSection;
    }

    private IndexedSection<DexBackedMethodHandleReference> methodHandleSection =
            new IndexedSection<DexBackedMethodHandleReference>() {
                @Override
                public DexBackedMethodHandleReference get(int index) {
                    return new DexBackedMethodHandleReference(DexBackedDexFile.this, index);
                }

                @Override
                public int size() {
                    MapItem mapItem = getMapItemForSection(ItemType.METHOD_HANDLE_ITEM);
                    if (mapItem == null) {
                        return 0;
                    }
                    return mapItem.getItemCount();
                }

                @Override
                public int getOffset(int index) {
                    MapItem mapItem = getMapItemForSection(ItemType.METHOD_HANDLE_ITEM);
                    if (index < 0 || index >= size()) {
                        throw new IndexOutOfBoundsException(
                                String.format("Invalid method handle index %d, not in [0, %d)", index, size()));
                    }
                    return mapItem.getOffset() + index * MethodHandleItem.ITEM_SIZE;
                }
            };

    public IndexedSection<DexBackedMethodHandleReference> getMethodHandleSection() {
        return methodHandleSection;
    }

    protected DexBackedMethodImplementation createMethodImplementation(
            @Nonnull DexBackedDexFile dexFile, @Nonnull DexBackedMethod method, int codeOffset) {
        return new DexBackedMethodImplementation(dexFile, method, codeOffset);
    }

    private int readHiddenApiRestrictionsOffset(int classIndex) {
        if (hiddenApiRestrictionsOffset == NO_OFFSET) {
            return NO_OFFSET;
        }

        int offset = dexBuffer.readInt(
                hiddenApiRestrictionsOffset +
                        HiddenApiClassDataItem.OFFSETS_LIST_OFFSET +
                        classIndex * HiddenApiClassDataItem.OFFSET_ITEM_SIZE);
        if (offset == NO_OFFSET) {
            return NO_OFFSET;
        }

        return hiddenApiRestrictionsOffset + offset;
    }

    public static abstract class OptionalIndexedSection<T> extends IndexedSection<T> {
        /**
         * @param index The index of the item, or -1 for a null item.
         * @return The value at the given index, or null if index is -1.
         * @throws IndexOutOfBoundsException if the index is out of bounds and is not -1.
         */
        @Nullable public abstract T getOptional(int index);
    }

    public static abstract class IndexedSection<T> extends AbstractList<T> {
        /**
         * @param index The index of the item to get the offset for.
         * @return The offset from the beginning of the dex file to the specified item.
         * @throws IndexOutOfBoundsException if the index is out of bounds.
         */
        public abstract int getOffset(int index);
    }
}
