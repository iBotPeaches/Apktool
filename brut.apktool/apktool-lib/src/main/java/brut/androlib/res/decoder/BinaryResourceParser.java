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
import brut.androlib.meta.ApkInfo;
import brut.androlib.res.decoder.data.*;
import brut.androlib.res.table.*;
import brut.androlib.res.table.value.*;
import brut.util.ExtDataInputStream;

import java.io.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Logger;

public class BinaryResourceParser {
    private static final Logger LOGGER = Logger.getLogger(BinaryResourceParser.class.getName());

    private static final int NO_ENTRY = 0xFFFFFFFF;
    private static final int NO_ENTRY_OFFSET16 = 0xFFFF;

    // If set, the entry is sparse, and encodes both the entry ID and offset into each entry,
    // and a binary search is used to find the key. Only available on platforms >= O.
    // Mark any types that use this with a v26 qualifier to prevent runtime issues on older platforms.
    private static final int TYPE_FLAG_SPARSE = 0x01;
    // If set, the offsets to the entries are encoded in 16-bit, real_offset = offset * 4u
    // An 16-bit offset of 0xffffu means a NO_ENTRY.
    private static final int TYPE_FLAG_OFFSET16 = 0x02;

    // If set, this is a complex entry, holding a set of name/value mappings.
    // It is followed by an array of ResTable_map structures.
    private static final int ENTRY_FLAG_COMPLEX = 0x0001;
    // If set, this resource has been declared public, so libraries are allowed to reference it.
    private static final int ENTRY_FLAG_PUBLIC = 0x0002;
    // If set, this is a weak resource and may be overriden by strong resources of the same name/type.
    // This is only useful during linking with other resource tables.
    private static final int ENTRY_FLAG_WEAK = 0x0004;
    // If set, this is a compact entry with data type and value directly encoded in the this entry.
    private static final int ENTRY_FLAG_COMPACT = 0x0008;

    private static final int CONFIG_KNOWN_MAX_SIZE = 64;

    private final ResTable mTable;
    private final boolean mKeepBroken;
    private final boolean mRecordFlagsOffsets;

    private ExtDataInputStream mIn;
    private List<ResPackage> mPackages;
    private List<FlagsOffset> mFlagsOffsets;
    private Set<ResConfig> mInvalidConfigs;
    private Set<ResId> mMissingEntrySpecs;
    private int mChunkCount;
    private ResChunkHeader mChunkHeader;
    private ResStringPool mTableStrings;
    private ResPackage mPackage;
    private int mTypeIdOffset;
    private ResStringPool mTypeStrings;
    private ResStringPool mKeyStrings;
    private ResTypeSpec mTypeSpec;
    private ResType mType;
    private ResId mEntryId;

    public BinaryResourceParser(ResTable table, boolean keepBroken, boolean recordFlagsOffsets) {
        mTable = table;
        mKeepBroken = keepBroken;
        mRecordFlagsOffsets = recordFlagsOffsets;
    }

    public List<ResPackage> getPackages() {
        return mPackages;
    }

    public List<FlagsOffset> getFlagsOffsets() {
        return mFlagsOffsets;
    }

    public void parse(InputStream in) throws AndrolibException {
        reset();
        mIn = ExtDataInputStream.littleEndian(in);

        try {
            if (!readChunkHeader()) {
                throw new AndrolibException("Input file is empty");
            }

            if (mChunkHeader.type != ResChunkHeader.RES_TABLE_TYPE) {
                throw new AndrolibException(String.format(
                    "Unexpected chunk type: 0x%08x (expected: 0x%08x)",
                    mChunkHeader.type, ResChunkHeader.RES_TABLE_TYPE));
            }

            readTable();

            LOGGER.fine(String.format("End of Chunks pos=0x%08x", mIn.position()));

            if (mIn.available() > 0) {
                LOGGER.warning(String.format(
                    "Ignoring trailing data at 0x%08x", mIn.position()));
            }
        } catch (IOException ex) {
            throw new AndrolibException("Could not decode arsc file", ex);
        }
    }

    public void reset() {
        mIn = null;
        mPackages = new ArrayList<>();
        mFlagsOffsets = mRecordFlagsOffsets ? new ArrayList<>() : null;
        mInvalidConfigs = new HashSet<>();
        mMissingEntrySpecs = new HashSet<>();
        mChunkCount = 0;
        mChunkHeader = null;
        mTableStrings = null;
        mPackage = null;
        mTypeIdOffset = 0;
        mTypeStrings = null;
        mKeyStrings = null;
        mTypeSpec = null;
        mType = null;
        mEntryId = null;
    }

    private boolean readChunkHeader() throws IOException {
        try {
            mChunkHeader = ResChunkHeader.read(mIn);
        } catch (EOFException ignored) {
            return false;
        }

        LOGGER.fine(String.format(
            "Chunk #%d pos=0x%08x type=0x%04x size=%d", ++mChunkCount,
            mChunkHeader.startPos, mChunkHeader.type, mChunkHeader.size));

        return true;
    }

    private void readTable() throws AndrolibException, IOException {
        // ResTable_header
        int packageCount = mIn.readInt();

        checkForUnreadHeader();

        mTableStrings = null;

        while (readChunkHeader()) {
            switch (mChunkHeader.type) {
                case ResChunkHeader.RES_NULL_TYPE:
                    readUnknown();
                    break;
                case ResChunkHeader.RES_STRING_POOL_TYPE:
                    readStringPool();
                    break;
                case ResChunkHeader.RES_TABLE_PACKAGE_TYPE:
                    readPackage();
                    break;
                case ResChunkHeader.RES_TABLE_TYPE_TYPE:
                    readType();
                    break;
                case ResChunkHeader.RES_TABLE_TYPE_SPEC_TYPE:
                    readTypeSpec();
                    break;
                case ResChunkHeader.RES_TABLE_LIBRARY_TYPE:
                    readLibrary();
                    break;
                case ResChunkHeader.RES_TABLE_OVERLAYABLE_TYPE:
                    readOverlayable();
                    break;
                case ResChunkHeader.RES_TABLE_OVERLAYABLE_POLICY_TYPE:
                    readOverlayablePolicy();
                    break;
                case ResChunkHeader.RES_TABLE_STAGED_ALIAS_TYPE:
                    readStagedAlias();
                    break;
                default:
                    throw new AndrolibException(String.format(
                        "Unknown chunk type: %04x", mChunkHeader.type));
            }
        }

        injectMissingEntrySpecs();

        if (mPackages.size() != packageCount) {
            LOGGER.warning(String.format(
                "Unexpected package count: %d (expected: %d)",
                mPackages.size(), packageCount));
        }
    }

    private void readUnknown() throws IOException {
        checkForUnreadHeader();

        LOGGER.warning(String.format(
            "Skipping unknown chunk at 0x%08x of %d bytes",
            mChunkHeader.startPos, mChunkHeader.size));
        mIn.jumpTo(mChunkHeader.endPos);
    }

    private void readStringPool() throws AndrolibException, IOException {
        ResStringPool stringPool = ResStringPool.read(mIn, mChunkHeader);

        if (mTableStrings == null) {
            mTableStrings = stringPool;
        } else if (mTypeStrings == null) {
            mTypeStrings = stringPool;
        } else if (mKeyStrings == null) {
            mKeyStrings = stringPool;
        } else {
            throw new AndrolibException(String.format(
                "Unexpected string pool chunk at 0x%08x", mChunkHeader.startPos));
        }
    }

    private void readPackage() throws AndrolibException, IOException {
        // Clean up after a previous package.
        injectMissingEntrySpecs();
        mMissingEntrySpecs.clear();
        mInvalidConfigs.clear();
        mType = null;
        mTypeSpec = null;
        mKeyStrings = null;
        mTypeStrings = null;

        // ResTable_package
        int id = mIn.readInt();
        String name = mIn.readUtf16(128);
        mIn.skipInt(); // typeStrings
        mIn.skipInt(); // lastPublicType
        mIn.skipInt(); // keyStrings
        mIn.skipInt(); // lastPublicKey

        // TypeIdOffset was added platform_frameworks_base/@f90f2f8dc36e7243b85e0b6a7fd5a590893c827e
        // which is only in split/new apps.
        // sizeof(ResTable_package) = short + short + int + int + char[128] + int * 5 = 288
        if (mChunkHeader.headerSize >= 288) {
            mTypeIdOffset = mIn.readInt();

            if (mTypeIdOffset > 0) {
                LOGGER.warning("Please report this app to Apktool for a fix: "
                        + "https://github.com/iBotPeaches/Apktool/issues/1728");
            }
        } else {
            mTypeIdOffset = 0;
        }

        checkForUnreadHeader();

        if (id == 0 && mTable.isMainPackageLoaded()) {
            // The package ID is 0x00. That means that a shared library is being loaded,
            // so we change it to the reference package ID defined in the dynamic reference table.
            id = mTable.getDynamicRefPackageId(name);
        }

        mPackage = new ResPackage(mTable, id, name);
        mPackages.add(mPackage);
        mEntryId = ResId.of(id << 24);
    }

    private void readTypeSpec() throws AndrolibException, IOException {
        // Clean up after a previous type spec.
        mType = null;

        // ResTable_typeSpec
        int id = mIn.readUnsignedByte();
        mIn.skipByte(); // res0
        mIn.skipShort(); // typesCount
        int entryCount = mIn.readInt();

        if (mFlagsOffsets != null) {
            mFlagsOffsets.add(new FlagsOffset(mIn.position(), entryCount));
        }

        checkForUnreadHeader();

        for (int i = 0; i < entryCount; i++) {
            mIn.skipInt(); // flags
        }

        mTypeSpec = mPackage.addTypeSpec(id, mTypeStrings.getString(id - 1));
        mEntryId = mEntryId.withTypeId(id);
    }

    private void readType() throws AndrolibException, IOException {
        // ResTable_type
        int id = mIn.readUnsignedByte() - mTypeIdOffset;
        int flags = mIn.readUnsignedByte();
        mIn.skipShort(); // reserved
        int entryCount = mIn.readInt();
        int entriesStart = mIn.readInt();
        ResConfig config = readConfig();

        mIn.mark(mChunkHeader.size);
        checkForUnreadHeader();

        boolean isOffset16 = (flags & TYPE_FLAG_OFFSET16) != 0;
        boolean isSparse = (flags & TYPE_FLAG_SPARSE) != 0;

        // Only flag the app as sparse if the main package is not loaded yet.
        if (isSparse && !mTable.isMainPackageLoaded()) {
            mTable.getApkInfo().getResourcesInfo().setSparseResources(true);
        }

        // #3372 - The offsets that are 16bit should be stored as real offsets (* 4u).
        HashMap<Integer, Integer> entryOffsetMap = new LinkedHashMap<>();
        for (int i = 0; i < entryCount; i++) {
            int index, offset;

            if (isSparse) {
                index = mIn.readUnsignedShort();
                offset = mIn.readUnsignedShort() * 4;
            } else {
                index = i;

                if (isOffset16) {
                    offset = mIn.readUnsignedShort();
                    offset = offset == NO_ENTRY_OFFSET16 ? NO_ENTRY : offset * 4;
                } else {
                    offset = mIn.readInt();
                }
            }

            entryOffsetMap.put(index, offset);
        }

        // #3311 - Some older apps have no TYPE_SPEC chunks, but still define TYPE chunks.
        if (!mPackage.hasTypeSpec(id)) {
            mTypeSpec = mPackage.addTypeSpec(id, mTypeStrings.getString(id - 1));
        }

        if (mInvalidConfigs.contains(config)) {
            String dirName = mTypeSpec.getName() + config.getQualifiers();
            if (mKeepBroken) {
                LOGGER.warning("Invalid res config detected: " + dirName);
                mType = mPackage.addType(id, config);
            } else {
                LOGGER.warning("Invalid res config detected. Dropping resources: " + dirName);
                mType = null;
            }
        } else {
            mType = mPackage.addType(id, config);
        }

        mEntryId = mEntryId.withTypeId(id);

        // #3428 - In some apps the res entries are padded for alignment, but in #3778
        // it made sense to align to the start of the entries to handle all cases.
        long entriesStartAligned = mChunkHeader.startPos + entriesStart;
        mIn.jumpTo(entriesStartAligned);
        for (int index : entryOffsetMap.keySet()) {
            mEntryId = mEntryId.withEntryId(index);

            int offset = entryOffsetMap.get(index);
            if (offset == NO_ENTRY) {
                if (!mPackage.hasEntrySpec(mEntryId)) {
                    mMissingEntrySpecs.add(mEntryId);
                }
                continue;
            }

            // #3778 - In some apps the res entries are unordered and might have to jump
            // backwards.
            long entryStart = entriesStartAligned + offset;
            if (entryStart < mIn.position()) {
                mIn.reset();
            }
            mIn.jumpTo(entryStart);

            // As seen in some recent APKs - there are more entries reported than can fit
            // in the chunk.
            if (mIn.position() >= mChunkHeader.endPos) {
                int remainingEntries = entryCount - index;
                LOGGER.warning(String.format(
                    "End of chunk hit. Skipping remaining %d entries in type: %s",
                    remainingEntries, mTypeSpec.getName()));
                break;
            }

            ResValue value = readEntry();
            if (value == null && !mPackage.hasEntrySpec(mEntryId)) {
                mMissingEntrySpecs.add(mEntryId);
            }
        }

        // Skip "TYPE 8 chunks" and/or padding data at the end of this chunk.
        if (mChunkHeader.endPos > mIn.position()) {
            long bytesSkipped = mIn.skip(mChunkHeader.endPos - mIn.position());
            LOGGER.warning(String.format(
                "Skipping unknown %d bytes at end of type chunk.", bytesSkipped));
        }
    }

    private ResConfig readConfig() throws AndrolibException, IOException {
        // ResTable_config
        int size = mIn.readInt();
        if (size < 8) {
            throw new AndrolibException("Config size < 8");
        }

        int read = 8;
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

        int exceedingKnownSize = size - CONFIG_KNOWN_MAX_SIZE;

        if (exceedingKnownSize > 0) {
            byte[] buf = mIn.readBytes(exceedingKnownSize);
            read += exceedingKnownSize;
            BigInteger exceedingBI = new BigInteger(1, buf);

            if (exceedingBI.equals(BigInteger.ZERO)) {
                LOGGER.fine(String.format(
                    "Config flags size of %d exceeds %d, but exceeding bytes are all zero.",
                    size, CONFIG_KNOWN_MAX_SIZE));
            } else {
                LOGGER.warning(String.format(
                    "Config flags size of %d exceeds %d. Exceeding bytes: %X",
                    size, CONFIG_KNOWN_MAX_SIZE, exceedingBI));
                isInvalid = true;
            }
        }

        int remainingSize = size - read;
        if (remainingSize > 0) {
            mIn.skipBytes(remainingSize);
        }

        ResConfig flags = new ResConfig(
            mcc, mnc, language, region, orientation,
            touchscreen, density, keyboard, navigation, inputFlags,
            grammaticalInflection, screenWidth, screenHeight, sdkVersion,
            minorVersion, screenLayout, uiMode, smallestScreenWidthDp,
            screenWidthDp, screenHeightDp, localeScript, localeVariant,
            screenLayout2, colorMode, localeNumberingSystem);

        if (isInvalid || flags.isInvalid()) {
            mInvalidConfigs.add(flags);
        }

        return flags;
    }

    private String unpackLanguageOrRegion(byte[] in, char base) {
        assert in.length == 2;
        // Return empty for "any" locale.
        if (in[0] == 0) {
            return "";
        }

        // If high bit is set then we have a packed 3-letter code.
        if ((in[0] & 0x80) != 0) {
            in = new byte[] {
                (byte) (base + (in[1] & 0x1F)),
                (byte) (base + ((in[1] & 0xE0) >>> 5) + ((in[0] & 0x03) << 3)),
                (byte) (base + ((in[0] & 0x7C) >>> 2))
            };
        }

        return new String(in, StandardCharsets.US_ASCII);
    }

    private ResValue readEntry() throws AndrolibException, IOException {
        // ResTable_entry
        int size = mIn.readUnsignedShort();
        int flags = mIn.readUnsignedShort();
        int key = mIn.readInt();

        boolean isComplex = (flags & ENTRY_FLAG_COMPLEX) != 0;
        boolean isCompact = (flags & ENTRY_FLAG_COMPACT) != 0;

        if (key == NO_ENTRY && !isCompact) {
            return null;
        }

        // Only flag the app as compact if the main package is not loaded yet.
        if (isCompact && !mTable.isMainPackageLoaded()) {
            mTable.getApkInfo().getResourcesInfo().setCompactEntries(true);
        }

        ResValue value;
        if (isComplex) {
            value = readMapEntry();
        } else if (isCompact) {
            // In a compactly packed entry, the key index is the size & type is higher
            // 8 bits on flags. We assume a size of 8 bytes for compact entries and the
            // key index is the data itself encoded.
            int type = (flags >>> 8) & 0xFF;
            value = parseValue(type, key, false);

            // If compact then the size has the key index encoded.
            key = size;
        } else {
            value = readValue(false);
        }

        // #2824 - In some apps the res entries are duplicated with the 2nd being malformed.
        // AOSP skips this, so we will do the same.
        if (value == null) {
            return null;
        }

        if (mType != null) {
            ResConfig config = mType.getConfig();

            boolean overwrite;
            if (mPackage.hasEntrySpec(mEntryId)) {
                overwrite = mKeepBroken && mPackage.hasEntry(mEntryId, config);
            } else {
                mPackage.addEntrySpec(mEntryId, mKeyStrings.getString(key));
                mMissingEntrySpecs.remove(mEntryId);
                overwrite = false;
            }

            mPackage.addEntry(mEntryId, config, value, overwrite);
        }

        return value;
    }

    private ResValue readMapEntry() throws IOException {
        String typeName = mType.getName();
        // ResTable_map_entry
        int parentId = mIn.readInt();
        int count = mIn.readInt();

        // Some apps store ID resource values generated for enum/flag items in attribute
        // resources as empty maps. Replace with a placeholder value.
        if (typeName.equals("id")) {
            return new ResCustom("id");
        }

        ResReference parent = new ResReference(mPackage, ResId.of(parentId));
        ResBag.RawItem[] rawItems = new ResBag.RawItem[count];
        int rawItemsCount = 0;

        for (int i = 0; i < count; i++) {
            // ResTable_map
            int name = mIn.readInt();
            ResItem value = (ResItem) readValue(true);

            // #2824 - In some apps the res entries are duplicated with the 2nd being malformed.
            // AOSP skips this, so we will do the same.
            if (value == null) {
                continue;
            }

            rawItems[rawItemsCount++] = new ResBag.RawItem(name, value);
        }

        if (rawItemsCount < rawItems.length) {
            rawItems = Arrays.copyOf(rawItems, rawItemsCount);
        }

        return ResBag.parse(typeName, parent, rawItems);
    }

    private ResValue readValue(boolean inBag) throws IOException {
        // Res_value
        int size = mIn.readUnsignedShort();
        if (size < 8) {
            return null;
        }

        mIn.skipByte(); // res0
        int type = mIn.readUnsignedByte();
        int data = mIn.readInt();

        return parseValue(type, data, inBag);
    }

    private ResValue parseValue(int type, int data, boolean inBag) {
        String typeName = mType.getName();
        String rawValue = null;

        // ID resource values are either encoded as a boolean (false) or a resource reference.
        // A boolean (false) is no longer allowed in XML, replace with a placeholder value.
        // A resource reference is handled normally, unless it's @null.
        if (typeName.equals("id") && (data == 0 || (type != TypedValue.TYPE_REFERENCE
                && type != TypedValue.TYPE_DYNAMIC_REFERENCE))) {
            return new ResCustom("id");
        }

        // Special handling for strings and file references.
        if (type == TypedValue.TYPE_STRING) {
            rawValue = mTableStrings.getHTML(data);

            // If a string is not allowed here, assume it's a file reference.
            // ResFileDecoder will replace it if it's an invalid file reference.
            if (!inBag && rawValue != null && !rawValue.isEmpty() && !typeName.equals("string")) {
                return new ResFileReference(rawValue);
            }
        }

        return ResItem.parse(mPackage, type, data, rawValue);
    }

    private void readLibrary() throws IOException {
        // ResTable_lib_header
        int count = mIn.readInt();

        checkForUnreadHeader();

        for (int i = 0; i < count; i++) {
            // ResTable_lib_entry
            int packageId = mIn.readInt();
            String packageName = mIn.readUtf16(128);

            if (packageId != 0) {
                mTable.addDynamicRefPackage(packageId, packageName);
            }
        }
    }

    private void readOverlayable() throws IOException {
        // ResTable_overlayable_header
        String name = mIn.readUtf16(256);
        String actor = mIn.readUtf16(256);

        checkForUnreadHeader();

        LOGGER.fine(String.format(
            "Skipping overlayable name: \"%s\", actor: \"%s\"", name, actor));
    }

    private void readOverlayablePolicy() throws AndrolibException, IOException {
        // ResTable_overlayable_policy_header
        mIn.skipInt(); // policy_flags
        int entry_count = mIn.readInt();

        checkForUnreadHeader();

        for (int i = 0; i < entry_count; i++) {
            int id = mIn.readInt();

            LOGGER.fine(String.format("Skipping overlayable policy: 0x%08x", id));
        }
    }

    private void readStagedAlias() throws IOException {
        // ResTable_staged_alias_header
        int count = mIn.readInt();

        checkForUnreadHeader();

        for (int i = 0; i < count; i++) {
            // ResTable_staged_alias_entry
            int stagedResId = mIn.readInt();
            int finalizedResId = mIn.readInt();

            LOGGER.fine(String.format(
                "Skipping staged alias: 0x%08x -> 0x%08x", stagedResId, finalizedResId));
        }
    }

    private void checkForUnreadHeader() throws IOException {
        // Some apps lie about the reported size of their chunk header.
        // Trusting the chunkSize is misleading, so compare to what we actually read in the header vs
        // reported and skip the rest. However, this runs after each chunk and not every chunk reading
        // has a specific distinction between the header and the body.
        int actualHeaderSize = (int) (mIn.position() - mChunkHeader.startPos);
        int exceedingSize = mChunkHeader.headerSize - actualHeaderSize;
        if (exceedingSize <= 0) {
            return;
        }

        byte[] buf = mIn.readBytes(exceedingSize);
        BigInteger exceedingBI = new BigInteger(1, buf);

        if (exceedingBI.equals(BigInteger.ZERO)) {
            LOGGER.fine(String.format("Chunk header size: %d bytes, read: %d bytes, but exceeding bytes are all zero.",
                mChunkHeader.headerSize, actualHeaderSize));
        } else {
            LOGGER.warning(String.format("Chunk header size: %d bytes, read: %d bytes. Exceeding bytes: %X",
                mChunkHeader.headerSize, actualHeaderSize, exceedingBI));
        }
    }

    private void injectMissingEntrySpecs() throws AndrolibException {
        if (mPackage == null || mTable.getConfig().getDecodeResolve() != Config.DecodeResolve.DUMMY) {
            return;
        }

        for (ResId id : mMissingEntrySpecs) {
            mPackage.addEntrySpec(id, ResEntrySpec.DUMMY_PREFIX + id);
            mPackage.addEntry(id, ResConfig.DEFAULT, ResReference.NULL);
        }
    }
}
