/*
 *  Copyright (C) 2010 Ryszard Wiśniewski <brut.alll@gmail.com>
 *  Copyright (C) 2010 Connor Tumbleson <connor.tumbleson@gmail.com>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package brut.androlib.res.decoder;

import android.util.TypedValue;
import brut.androlib.Config;
import brut.androlib.exceptions.AndrolibException;
import brut.androlib.res.data.*;
import brut.androlib.res.data.arsc.*;
import brut.androlib.res.data.value.*;
import brut.util.ExtDataInputStream;
import org.apache.commons.lang3.tuple.Pair;

import java.io.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Logger;

public class ARSCDecoder {
    private static final Logger LOGGER = Logger.getLogger(ARSCDecoder.class.getName());

    private static final int ENTRY_FLAG_COMPLEX = 0x0001;
    private static final int ENTRY_FLAG_PUBLIC = 0x0002;
    private static final int ENTRY_FLAG_WEAK = 0x0004;
    private static final int ENTRY_FLAG_COMPACT = 0x0008;

    private static final int TABLE_TYPE_FLAG_SPARSE = 0x01;
    private static final int TABLE_TYPE_FLAG_OFFSET16 = 0x02;

    private static final int KNOWN_CONFIG_BYTES = 64;

    private static final int NO_ENTRY = 0xFFFFFFFF;
    private static final int NO_ENTRY_OFFSET16 = 0xFFFF;

    private final ExtDataInputStream mIn;
    private final ResTable mResTable;
    private final List<FlagsOffset> mFlagsOffsets;
    private final boolean mKeepBroken;
    private final Config mConfig;
    private final Map<Integer, ResTypeSpec> mResTypeSpecs;
    private final Map<Integer, Integer> mMissingResSpecs;
    private final Set<ResConfigFlags> mInvalidResConfigFlags;

    private ARSCHeader mHeader;
    private StringBlock mTableStrings;
    private StringBlock mTypeNames;
    private StringBlock mSpecNames;
    private ResPackage mPackage;
    private ResTypeSpec mTypeSpec;
    private ResType mType;
    private int mResId;
    private int mTypeIdOffset;

    public ARSCDecoder(InputStream in, ResTable resTable, boolean storeFlagsOffsets, boolean keepBroken) {
        mIn = ExtDataInputStream.littleEndian(in);
        mResTable = resTable;
        mFlagsOffsets = storeFlagsOffsets ? new ArrayList<>() : null;
        mKeepBroken = keepBroken;
        mConfig = resTable.getConfig();
        mResTypeSpecs = new HashMap<>();
        mMissingResSpecs = new LinkedHashMap<>();
        mInvalidResConfigFlags = new HashSet<>();
    }

    public ARSCData decode() throws AndrolibException {
        try {
            ResPackage[] pkgs = readResourceTable();
            FlagsOffset[] flagsOffsets = mFlagsOffsets != null ? mFlagsOffsets.toArray(new FlagsOffset[0]) : null;
            return new ARSCData(pkgs, flagsOffsets);
        } catch (IOException ex) {
            throw new AndrolibException("Could not decode arsc file", ex);
        }
    }

    private ResPackage[] readResourceTable() throws AndrolibException, IOException {
        Set<ResPackage> pkgs = new LinkedHashSet<>();
        ResTypeSpec typeSpec;
        int chunkNumber = 1;

        chunkLoop:
        for (;;) {
            mHeader = ARSCHeader.read(mIn);

            LOGGER.fine(String.format(
                "Chunk #%d start=0x%08x type=0x%04x chunkSize=0x%08x",
                chunkNumber++, mIn.position(), mHeader.type, mHeader.chunkSize
            ));

            switch (mHeader.type) {
                case ARSCHeader.RES_NULL_TYPE:
                    readUnknownChunk();
                    break;
                case ARSCHeader.RES_STRING_POOL_TYPE:
                    readStringPoolChunk();
                    break;
                case ARSCHeader.RES_TABLE_TYPE:
                    readTableChunk();
                    break;

                // Chunk types in RES_TABLE_TYPE
                case ARSCHeader.RES_TABLE_PACKAGE_TYPE:
                    mTypeIdOffset = 0;
                    pkgs.add(readTablePackage());
                    break;
                case ARSCHeader.RES_TABLE_TYPE_TYPE:
                    readTableType();
                    break;
                case ARSCHeader.RES_TABLE_TYPE_SPEC_TYPE:
                    typeSpec = readTableSpecType();
                    mResTypeSpecs.put(typeSpec.getId(), typeSpec);
                    break;
                case ARSCHeader.RES_TABLE_LIBRARY_TYPE:
                    readLibraryType();
                    break;
                case ARSCHeader.RES_TABLE_OVERLAYABLE_TYPE:
                    readOverlaySpec();
                    break;
                case ARSCHeader.RES_TABLE_OVERLAYABLE_POLICY_TYPE:
                    readOverlayPolicySpec();
                    break;
                case ARSCHeader.RES_TABLE_STAGED_ALIAS_TYPE:
                    readStagedAliasSpec();
                    break;
                default:
                    if (mHeader.type != ARSCHeader.RES_NONE_TYPE) {
                        LOGGER.severe(String.format("Unknown chunk type: %04x", mHeader.type));
                    }
                    break chunkLoop;
            }
        }

        if (mConfig.getDecodeResolve() == Config.DecodeResolve.DUMMY
                && mPackage != null && mPackage.getResSpecCount() > 0) {
            addMissingResSpecs();
        }

        return pkgs.toArray(new ResPackage[0]);
    }

    private void readStringPoolChunk() throws AndrolibException, IOException {
        checkChunkType(ARSCHeader.RES_STRING_POOL_TYPE);

        mTableStrings = StringBlock.readWithoutChunk(mIn, mHeader.startPosition, mHeader.headerSize, mHeader.chunkSize);
    }

    private void readTableChunk() throws AndrolibException, IOException {
        checkChunkType(ARSCHeader.RES_TABLE_TYPE);
        mIn.skipInt(); // packageCount

        mHeader.checkForUnreadHeader(mIn);
    }

    private void readUnknownChunk() throws AndrolibException, IOException {
        checkChunkType(ARSCHeader.RES_NULL_TYPE);

        mHeader.checkForUnreadHeader(mIn);

        LOGGER.warning("Skipping unknown chunk data of size " + mHeader.chunkSize);
        mIn.jumpTo(mHeader.endPosition);
    }

    private ResPackage readTablePackage() throws AndrolibException, IOException {
        checkChunkType(ARSCHeader.RES_TABLE_PACKAGE_TYPE);
        int id = mIn.readInt();
        String name = mIn.readUtf16(128);
        mIn.skipInt(); // typeStrings
        mIn.skipInt(); // lastPublicType
        mIn.skipInt(); // keyStrings
        mIn.skipInt(); // lastPublicKey

        // TypeIdOffset was added platform_frameworks_base/@f90f2f8dc36e7243b85e0b6a7fd5a590893c827e
        // which is only in split/new applications.
        int splitHeaderSize = 2 + 2 + 4 + 4 + (2 * 128) + (4 * 5); // short, short, int, int, char[128], int * 4
        if (mHeader.headerSize == splitHeaderSize) {
            mTypeIdOffset = mIn.readInt();
        }

        if (mTypeIdOffset > 0) {
            LOGGER.warning("Please report this application to Apktool for a fix: https://github.com/iBotPeaches/Apktool/issues/1728");
        }

        mHeader.checkForUnreadHeader(mIn);

        mTypeNames = StringBlock.readWithChunk(mIn);
        mSpecNames = StringBlock.readWithChunk(mIn);

        if (id == 0 && mResTable.isMainPackageLoaded()) {
            // The package ID is 0x00. That means that a shared library is being loaded,
            // so we change it to the reference package ID defined in the dynamic reference table.
            id = mResTable.getDynamicRefPackageId(name);
        }

        mResId = id << 24;
        mPackage = new ResPackage(mResTable, id, name);

        return mPackage;
    }

    private void readLibraryType() throws AndrolibException, IOException {
        checkChunkType(ARSCHeader.RES_TABLE_LIBRARY_TYPE);
        int libraryCount = mIn.readInt();

        mHeader.checkForUnreadHeader(mIn);

        for (int i = 0; i < libraryCount; i++) {
            int id = mIn.readInt();
            String name = mIn.readUtf16(128);

            mResTable.addDynamicRefPackage(id, name);

            LOGGER.fine(String.format("Shared library id: %d, name: \"%s\"", id, name));
        }
    }

    private void readStagedAliasSpec() throws IOException {
        int count = mIn.readInt();

        mHeader.checkForUnreadHeader(mIn);

        for (int i = 0; i < count; i++) {
            LOGGER.fine(String.format("Staged alias: 0x%08x -> 0x%08x", mIn.readInt(), mIn.readInt()));
        }
    }

    private void readOverlaySpec() throws AndrolibException, IOException {
        checkChunkType(ARSCHeader.RES_TABLE_OVERLAYABLE_TYPE);
        String name = mIn.readUtf16(256);
        String actor = mIn.readUtf16(256);

        mHeader.checkForUnreadHeader(mIn);

        LOGGER.fine(String.format("Overlay name: \"%s\", actor: \"%s\"", name, actor));
    }

    private void readOverlayPolicySpec() throws AndrolibException, IOException {
        checkChunkType(ARSCHeader.RES_TABLE_OVERLAYABLE_POLICY_TYPE);
        mIn.skipInt(); // policyFlags
        int count = mIn.readInt();

        mHeader.checkForUnreadHeader(mIn);

        for (int i = 0; i < count; i++) {
            LOGGER.fine(String.format("Skipping overlay (%h)", mIn.readInt()));
        }
    }

    private ResTypeSpec readTableSpecType() throws AndrolibException, IOException {
        checkChunkType(ARSCHeader.RES_TABLE_TYPE_SPEC_TYPE);
        int id = mIn.readUnsignedByte();
        mIn.skipByte(); // reserved0
        mIn.skipShort(); // typesCount
        int entryCount = mIn.readInt();

        if (mFlagsOffsets != null) {
            mFlagsOffsets.add(new FlagsOffset((int) mIn.position(), entryCount));
        }

        mHeader.checkForUnreadHeader(mIn);

        for (int i = 0; i < entryCount; i++) {
            mIn.skipInt(); // flags
        }
        mTypeSpec = new ResTypeSpec(mTypeNames.getString(id - 1), id);
        mPackage.addType(mTypeSpec);

        return mTypeSpec;
    }

    private ResType readTableType() throws AndrolibException, IOException {
        checkChunkType(ARSCHeader.RES_TABLE_TYPE_TYPE);
        int typeId = mIn.readUnsignedByte() - mTypeIdOffset;

        // #3311 - Some older applications have no TYPE_SPEC chunks, but still define TYPE chunks.
        if (mResTypeSpecs.containsKey(typeId)) {
            mTypeSpec = mResTypeSpecs.get(typeId);
        } else {
            mTypeSpec = new ResTypeSpec(mTypeNames.getString(typeId - 1), typeId);
            mResTypeSpecs.put(mTypeSpec.getId(), mTypeSpec);
            mPackage.addType(mTypeSpec);
        }
        mResId = (mResId & 0xFF000000) | mTypeSpec.getId() << 16;

        int typeFlags = mIn.readUnsignedByte();
        mIn.skipShort(); // reserved
        int entryCount = mIn.readInt();
        int entriesStart = mIn.readInt();
        long entriesStartAligned = mHeader.startPosition + entriesStart;

        ResConfigFlags flags = readConfigFlags();

        mIn.mark(mHeader.chunkSize);
        mHeader.checkForUnreadHeader(mIn);

        boolean isOffset16 = (typeFlags & TABLE_TYPE_FLAG_OFFSET16) != 0;
        boolean isSparse = (typeFlags & TABLE_TYPE_FLAG_SPARSE) != 0;

        // Only flag the application as sparse if the main package is not loaded yet.
        if (isSparse && !mResTable.isMainPackageLoaded()) {
            mResTable.getApkInfo().setSparseResources(true);
        }

        // #3372 - The offsets that are 16bit should be stored as real offsets (* 4u).
        HashMap<Integer, Integer> entryOffsetMap = new LinkedHashMap<>();
        for (int i = 0; i < entryCount; i++) {
            if (isSparse) {
                entryOffsetMap.put(mIn.readUnsignedShort(), mIn.readUnsignedShort() * 4);
            } else if (isOffset16) {
                int offset = mIn.readUnsignedShort();
                entryOffsetMap.put(i, offset == NO_ENTRY_OFFSET16 ? NO_ENTRY : offset * 4);
            } else {
                entryOffsetMap.put(i, mIn.readInt());
            }
        }

        if (mInvalidResConfigFlags.contains(flags)) {
            String resName = mTypeSpec.getName() + flags.getQualifiers();
            if (mKeepBroken) {
                LOGGER.warning("Invalid config flags detected: " + resName);
            } else {
                LOGGER.warning("Invalid config flags detected. Dropping resources: " + resName);
            }
        }

        mType = (!mInvalidResConfigFlags.contains(flags) || mKeepBroken) ? mPackage.getOrCreateConfig(flags) : null;

        // #3428 - In some applications the res entries are padded for alignment, but in #3778 it made
        // sense to align to the start of the entries to handle all cases.
        mIn.jumpTo(entriesStartAligned);
        for (int i : entryOffsetMap.keySet()) {
            mResId = (mResId & 0xFFFF0000) | i;
            int offset = entryOffsetMap.get(i);
            if (offset == NO_ENTRY) {
                mMissingResSpecs.put(mResId, typeId);
                continue;
            }

            // #3778 - In some applications the res entries are unordered and might have to jump backwards.
            long entryStart = entriesStartAligned + offset;
            if (entryStart < mIn.position()) {
                mIn.reset();
            }
            mIn.jumpTo(entryStart);

            // As seen in some recent APKs - there are more entries reported than can fit in the chunk.
            if (mIn.position() >= mHeader.endPosition) {
                int remainingEntries = entryCount - i;
                LOGGER.warning(String.format("End of chunk hit. Skipping remaining entries (%d) in type: %s",
                    remainingEntries, mTypeSpec.getName()
                ));
                break;
            }

            EntryData entryData = readEntryData();
            if (entryData != null) {
                parseEntryData(entryData);
            } else {
                mMissingResSpecs.put(mResId, typeId);
            }
        }

        // skip "TYPE 8 chunks" and/or padding data at the end of this chunk
        if (mHeader.endPosition > mIn.position()) {
            long bytesSkipped = mIn.skip(mHeader.endPosition - mIn.position());
            LOGGER.warning("Unknown data detected at end of type chunk. Skipping: " + bytesSkipped + " byte(s)");
        }

        return mType;
    }

    private EntryData readEntryData() throws AndrolibException, IOException {
        int size = mIn.readUnsignedShort();
        int flags = mIn.readUnsignedShort();

        boolean isComplex = (flags & ENTRY_FLAG_COMPLEX) != 0;
        boolean isCompact = (flags & ENTRY_FLAG_COMPACT) != 0;

        int specNamesId = mIn.readInt();
        if (specNamesId == NO_ENTRY && !isCompact) {
            return null;
        }

        // Only flag the application as compact if the main package is not loaded yet.
        if (isCompact && !mResTable.isMainPackageLoaded()) {
            mResTable.getApkInfo().setCompactEntries(true);
        }

        // #3366 - In a compactly packed entry, the key index is the size & type is higher 8 bits on flags.
        // We assume a size of 8 bytes for compact entries and the specNamesId is the data itself encoded.
        ResValue value;
        if (isCompact) {
            int type = (flags >> 8) & 0xFF;
            value = readCompactValue(type, specNamesId);

            // To keep code below happy - we know if compact then the size has the key index encoded.
            specNamesId = size;
        } else if (isComplex) {
            value = readComplexEntry();
        } else {
            value = readValue();
        }

        // #2824 - In some applications the res entries are duplicated with the 2nd being malformed.
        // AOSP skips this, so we will do the same.
        if (value == null) {
            return null;
        }

        EntryData entryData = new EntryData();
        entryData.flags = flags;
        entryData.specNamesId = specNamesId;
        entryData.value = value;
        return entryData;
    }

    private void parseEntryData(EntryData entryData) throws AndrolibException {
        int specNamesId = entryData.specNamesId;
        ResValue value = entryData.value;

        if (mTypeSpec.isString() && value instanceof ResFileValue) {
            value = new ResStringValue(value.toString(), ((ResFileValue) value).getRawIntValue());
        }
        if (mType == null) {
            return;
        }

        ResID resId = new ResID(mResId);
        ResResSpec spec;
        if (mPackage.hasResSpec(resId)) {
            spec = mPackage.getResSpec(resId);
        } else {
            spec = new ResResSpec(resId, mSpecNames.getString(specNamesId), mPackage, mTypeSpec);
            mPackage.addResSpec(spec);
            mTypeSpec.addResSpec(spec);
        }
        ResResource res = new ResResource(mType, spec, value);

        try {
            mType.addResource(res);
            spec.addResource(res);
        } catch (AndrolibException ex) {
            if (mKeepBroken) {
                mType.addResource(res, true);
                spec.addResource(res, true);
                LOGGER.warning(String.format("Duplicate Resource Detected. Ignoring duplicate: %s", res));
            } else {
                throw ex;
            }
        }
    }

    private ResBagValue readComplexEntry() throws AndrolibException, IOException {
        int parentId = mIn.readInt();
        int count = mIn.readInt();

        ResValueFactory factory = mPackage.getValueFactory();
        Pair<Integer, ResScalarValue>[] items = new Pair[count];

        for (int i = 0; i < count; i++) {
            int resId = mIn.readInt();
            ResIntBasedValue resValue = readValue();

            // #2824 - In some applications the res entries are duplicated with the 2nd being malformed.
            // AOSP skips this, so we will do the same.
            if (resValue == null) {
                continue;
            }

            if (!(resValue instanceof ResScalarValue)) {
                resValue = new ResStringValue(resValue.toString(), resValue.getRawIntValue());
            }
            items[i] = Pair.of(resId, (ResScalarValue) resValue);
        }

        return factory.bagFactory(parentId, items, mTypeSpec);
    }

    private ResIntBasedValue readCompactValue(int type, int data) throws AndrolibException {
        return type == TypedValue.TYPE_STRING
            ? mPackage.getValueFactory().factory(mTableStrings.getHTML(data), data)
            : mPackage.getValueFactory().factory(type, data, null);
    }

    private ResIntBasedValue readValue() throws AndrolibException, IOException {
        int size = mIn.readUnsignedShort();
        if (size < 8) {
            return null;
        }

        mIn.skipByte(); // reserved0
        int type = mIn.readUnsignedByte();
        int data = mIn.readInt();

        return type == TypedValue.TYPE_STRING
            ? mPackage.getValueFactory().factory(mTableStrings.getHTML(data), data)
            : mPackage.getValueFactory().factory(type, data, null);
    }

    private ResConfigFlags readConfigFlags() throws AndrolibException, IOException {
        int size = mIn.readInt();
        int read = 8;

        if (size < 8) {
            throw new AndrolibException("Config size < 8");
        }

        boolean isInvalid = false;

        int mcc = mIn.readUnsignedShort();
        int mnc = mIn.readUnsignedShort();

        String language = "";
        String region = "";
        if (size >= 12) {
            language = unpackLanguageOrRegion(mIn.readBytes(2), 'a');
            region = unpackLanguageOrRegion(mIn.readBytes(2), '0');
            read = 12;
        }

        int orientation = 0;
        int touchscreen = 0;
        if (size >= 14) {
            orientation = mIn.readUnsignedByte();
            touchscreen = mIn.readUnsignedByte();
            read = 14;
        }

        int density = 0;
        if (size >= 16) {
            density = mIn.readUnsignedShort();
            read = 16;
        }

        int keyboard = 0;
        int navigation = 0;
        int inputFlags = 0;
        int grammaticalInflection = 0;
        if (size >= 20) {
            keyboard = mIn.readUnsignedByte();
            navigation = mIn.readUnsignedByte();
            inputFlags = mIn.readUnsignedByte();
            grammaticalInflection = mIn.readUnsignedByte();
            read = 20;
        }

        int screenWidth = 0;
        int screenHeight = 0;
        int sdkVersion = 0;
        int minorVersion = 0;
        if (size >= 28) {
            screenWidth = mIn.readUnsignedShort();
            screenHeight = mIn.readUnsignedShort();
            sdkVersion = mIn.readUnsignedShort();
            minorVersion = mIn.readUnsignedShort();
            read = 28;
        }

        int screenLayout = 0;
        int uiMode = 0;
        int smallestScreenWidthDp = 0;
        if (size >= 32) {
            screenLayout = mIn.readUnsignedByte();
            uiMode = mIn.readUnsignedByte();
            smallestScreenWidthDp = mIn.readUnsignedShort();
            read = 32;
        }

        int screenWidthDp = 0;
        int screenHeightDp = 0;
        if (size >= 36) {
            screenWidthDp = mIn.readUnsignedShort();
            screenHeightDp = mIn.readUnsignedShort();
            read = 36;
        }

        String localeScript = "";
        String localeVariant = "";
        if (size >= 48) {
            localeScript = mIn.readAscii(4);
            localeVariant = mIn.readAscii(8);
            read = 48;
        }

        int screenLayout2 = 0;
        int colorMode = 0;
        if (size >= 52) {
            screenLayout2 = mIn.readUnsignedByte();
            colorMode = mIn.readUnsignedByte();
            mIn.skipShort(); // screenConfigPad2
            read = 52;
        }

        String localeNumberingSystem = "";
        if (size >= 60) {
            localeNumberingSystem = mIn.readAscii(8);
            read = 60;
        }

        int exceedingKnownSize = size - KNOWN_CONFIG_BYTES;

        if (exceedingKnownSize > 0) {
            byte[] buf = mIn.readBytes(exceedingKnownSize);
            read += exceedingKnownSize;
            BigInteger exceedingBI = new BigInteger(1, buf);

            if (exceedingBI.equals(BigInteger.ZERO)) {
                LOGGER.fine(String
                        .format("Config flags size > %d, but exceeding bytes are all zero, so it should be ok.",
                                KNOWN_CONFIG_BYTES));
            } else {
                LOGGER.warning(String.format("Config flags size > %d. Size = %d. Exceeding bytes: 0x%X.",
                        KNOWN_CONFIG_BYTES, size, exceedingBI));
                isInvalid = true;
            }
        }

        int remainingSize = size - read;
        if (remainingSize > 0) {
            mIn.skipBytes(remainingSize);
        }

        ResConfigFlags flags = new ResConfigFlags(mcc, mnc, language, region, orientation,
                touchscreen, density, keyboard, navigation, inputFlags,
                grammaticalInflection, screenWidth, screenHeight, sdkVersion,
                minorVersion, screenLayout, uiMode, smallestScreenWidthDp,
                screenWidthDp, screenHeightDp, localeScript, localeVariant,
                screenLayout2, colorMode, localeNumberingSystem);

        if (isInvalid || flags.isInvalid()) {
            mInvalidResConfigFlags.add(flags);
        }

        return flags;
    }

    private String unpackLanguageOrRegion(byte[] value, char base) {
        assert value.length == 2;
        // return empty for "any" locale
        if (value[0] == 0) {
            return "";
        }
        // if high bit is set then we have a packed 3-letter code
        if ((value[0] & 0x80) != 0) {
            byte first = (byte) (base + (value[1] & 0x1F));
            byte second = (byte) (base + ((value[1] & 0xE0) >>> 5) + ((value[0] & 0x03) << 3));
            byte third = (byte) (base + ((value[0] & 0x7C) >>> 2));

            value = new byte[] { first, second, third };
        }
        return new String(value, StandardCharsets.US_ASCII);
    }

    private void addMissingResSpecs() throws AndrolibException {
        for (int resId : mMissingResSpecs.keySet()) {
            int typeId = mMissingResSpecs.get(resId);
            String resName = "APKTOOL_DUMMY_" + Integer.toHexString(resId);
            ResID id = new ResID(resId);
            ResResSpec spec = new ResResSpec(id, resName, mPackage, mResTypeSpecs.get(typeId));

            // If we already have this resId don't add it again.
            if (!mPackage.hasResSpec(id)) {
                mPackage.addResSpec(spec);
                spec.getType().addResSpec(spec);
                ResType resType = mPackage.getOrCreateConfig(new ResConfigFlags());

                // We are going to make dummy attributes a null reference (@null) now instead of a boolean false.
                // This is because aapt2 is stricter when it comes to what we can put in an application.
                ResValue value = new ResReferenceValue(mPackage, 0, "");

                ResResource res = new ResResource(resType, spec, value);
                resType.addResource(res);
                spec.addResource(res);
            }
        }
    }

    private void checkChunkType(int expectedType) throws AndrolibException {
        if (mHeader.type != expectedType) {
            throw new AndrolibException(String.format("Invalid chunk type: expected=0x%08x, got=0x%08x",
                    expectedType, mHeader.type));
        }
    }
}
