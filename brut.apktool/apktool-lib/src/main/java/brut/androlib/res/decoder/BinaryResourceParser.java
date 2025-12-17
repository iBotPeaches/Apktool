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
import brut.androlib.exceptions.UndefinedResObjectException;
import brut.androlib.meta.ApkInfo;
import brut.androlib.res.decoder.data.FlagsOffset;
import brut.androlib.res.decoder.data.ResChunkHeader;
import brut.androlib.res.decoder.data.ResStringPool;
import brut.androlib.res.table.*;
import brut.androlib.res.table.value.*;
import brut.util.BinaryDataInputStream;
import com.google.common.io.BaseEncoding;
import org.apache.commons.lang3.tuple.Pair;

import java.io.*;
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
    // If set, this is a weak resource and may be overridden by strong resources of the same name/type.
    // This is only useful during linking with other resource tables.
    private static final int ENTRY_FLAG_WEAK = 0x0004;
    // If set, this is a compact entry with data type and value directly encoded in the entry.
    private static final int ENTRY_FLAG_COMPACT = 0x0008;
    // If set, this resource relies on an android feature flag.
    // This should not be encountered in most cases (#3993)
    private static final int ENTRY_FLAG_FEATUREFLAG = 0x0010;

    private final ResTable mTable;
    private final boolean mKeepBroken;
    private final boolean mRecordFlagsOffsets;

    private BinaryDataInputStream mIn;
    private List<ResPackage> mPackages;
    private List<FlagsOffset> mFlagsOffsets;
    private Set<ResConfig> mInvalidConfigs;
    private Set<ResId> mMissingEntrySpecs;
    private ResStringPool mTableStrings;
    private ResPackage mPackage;
    private int mTypeIdOffset;
    private ResStringPool mTypeStrings;
    private ResStringPool mKeyStrings;

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
        mIn = new BinaryDataInputStream(in);

        ResChunkPullParser parser = new ResChunkPullParser(mIn);
        try {
            if (!nextChunk(parser)) {
                throw new AndrolibException("Input file is empty.");
            }

            if (parser.chunkType() != ResChunkHeader.RES_TABLE_TYPE) {
                throw new AndrolibException("Unexpected chunk: " + parser.chunkName()
                        + " (expected: RES_TABLE_TYPE)");
            }

            parseTable(parser);

            LOGGER.fine(String.format("End of chunks at 0x%08x", mIn.position()));

            // We can't use remaining() here, the length of the main stream is unknown.
            if (mIn.available() > 0) {
                LOGGER.warning(String.format(
                    "Ignoring trailing data at 0x%08x.", mIn.position()));
            }
        } catch (IOException ex) {
            throw new AndrolibException("Could not decode arsc file.", ex);
        }
    }

    public void reset() {
        mIn = null;
        mPackages = new ArrayList<>();
        mFlagsOffsets = mRecordFlagsOffsets ? new ArrayList<>() : null;
        mInvalidConfigs = new HashSet<>();
        mMissingEntrySpecs = new HashSet<>();
        mTableStrings = null;
        mPackage = null;
        mTypeIdOffset = 0;
        mTypeStrings = null;
        mKeyStrings = null;
    }

    private boolean nextChunk(ResChunkPullParser parser) throws IOException {
        // Skip padding or unknown data at the end of current chunk.
        if (parser.isChunk()) {
            int skipped = parser.skipChunk();
            if (skipped > 0) {
                LOGGER.warning(String.format(
                    "Skipped unknown %d bytes at end of %s chunk.",
                    skipped, parser.chunkName()));
            }
        }

        // Move to next chunk.
        if (!parser.next()) {
            return false;
        }

        // Skip unknown or unsupported chunks.
        if (parser.chunkType() == ResChunkHeader.RES_NULL_TYPE) {
            LOGGER.warning(String.format(
                "Skipping unknown chunk of %d bytes at 0x%08x.",
                parser.chunkSize(), parser.chunkStart()));
            parser.skipChunk();
            return nextChunk(parser);
        }

        LOGGER.fine(String.format(
            "Chunk at 0x%08x: %s (%d bytes)",
            parser.chunkStart(), parser.chunkName(), parser.chunkSize()));
        return true;
    }

    private void parseTable(ResChunkPullParser parser) throws AndrolibException, IOException {
        // ResTable_header
        int packageCount = mIn.readInt();

        skipUnreadHeader(parser);

        parser = new ResChunkPullParser(mIn, parser.dataSize());
        while (nextChunk(parser)) {
            switch (parser.chunkType()) {
                case ResChunkHeader.RES_STRING_POOL_TYPE:
                    parseStringPool(parser);
                    break;
                case ResChunkHeader.RES_TABLE_PACKAGE_TYPE:
                    parsePackage(parser);
                    break;
                default:
                    skipUnexpectedChunk(parser);
                    break;
            }
        }

        if (mPackages.size() != packageCount) {
            LOGGER.warning(String.format(
                "Unexpected package count: %d (expected: %d)", mPackages.size(), packageCount));
        }
    }

    private void parseStringPool(ResChunkPullParser parser) throws AndrolibException, IOException {
        ResStringPool stringPool = ResStringPool.parse(parser);

        if (mTableStrings == null) {
            mTableStrings = stringPool;
        } else if (mTypeStrings == null) {
            mTypeStrings = stringPool;
        } else if (mKeyStrings == null) {
            mKeyStrings = stringPool;
        } else {
            skipUnexpectedChunk(parser);
        }
    }

    private void parsePackage(ResChunkPullParser parser) throws AndrolibException, IOException {
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
        if (parser.headerSize() >= 288) {
            mTypeIdOffset = mIn.readInt();

            if (mTypeIdOffset > 0) {
                LOGGER.warning("Please report this app to Apktool for a fix: "
                        + "https://github.com/iBotPeaches/Apktool/issues/1728");
            }
        } else {
            mTypeIdOffset = 0;
        }

        skipUnreadHeader(parser);

        if (id == 0 && mTable.isMainPackageLoaded()) {
            // The package ID is 0x00. That means that a shared library is being loaded,
            // so we change it to the reference package ID defined in the dynamic reference table.
            id = mTable.getDynamicRefPackageId(name);
        }

        mPackage = new ResPackage(mTable, id, name);
        mPackages.add(mPackage);

        parser = new ResChunkPullParser(mIn, parser.dataSize());
        while (nextChunk(parser)) {
            switch (parser.chunkType()) {
                case ResChunkHeader.RES_STRING_POOL_TYPE:
                    parseStringPool(parser);
                    break;
                case ResChunkHeader.RES_TABLE_TYPE_SPEC_TYPE:
                    parseTypeSpec(parser);
                    break;
                case ResChunkHeader.RES_TABLE_TYPE_TYPE:
                    parseType(parser);
                    break;
                case ResChunkHeader.RES_TABLE_LIBRARY_TYPE:
                    parseLibrary(parser);
                    break;
                case ResChunkHeader.RES_TABLE_OVERLAYABLE_TYPE:
                    parseOverlayable(parser);
                    break;
                case ResChunkHeader.RES_TABLE_STAGED_ALIAS_TYPE:
                    parseStagedAliases(parser);
                    break;
                default:
                    skipUnexpectedChunk(parser);
                    break;
            }
        }

        // Clean up.
        injectDummyEntrySpecs();
        mInvalidConfigs.clear();
        mKeyStrings = null;
        mTypeStrings = null;
        mPackage = null;
    }

    private void parseTypeSpec(ResChunkPullParser parser) throws AndrolibException, IOException {
        // ResTable_typeSpec
        int id = mIn.readUnsignedByte();
        mIn.skipByte(); // res0
        mIn.skipShort(); // typesCount
        int entryCount = mIn.readInt();

        skipUnreadHeader(parser);

        if (mFlagsOffsets != null) {
            mFlagsOffsets.add(new FlagsOffset(mIn.position(), entryCount));
        }

        for (int i = 0; i < entryCount; i++) {
            mIn.skipInt(); // flags
        }

        mPackage.addTypeSpec(id, mTypeStrings.getString(id - 1));
    }

    private void parseType(ResChunkPullParser parser) throws AndrolibException, IOException {
        // ResTable_type
        int id = mIn.readUnsignedByte() - mTypeIdOffset;
        int flags = mIn.readUnsignedByte();
        mIn.skipShort(); // reserved
        int entryCount = mIn.readInt();
        int entriesStart = mIn.readInt();
        ResConfig config = parseConfig();

        skipUnreadHeader(parser);

        // #3311 - Some older apps have no TYPE_SPEC chunks, but still define TYPE chunks.
        ResTypeSpec typeSpec;
        try {
            typeSpec = mPackage.getTypeSpec(id);
        } catch (UndefinedResObjectException ignored) {
            typeSpec = mPackage.addTypeSpec(id, mTypeStrings.getString(id - 1));
        }

        String typeName = typeSpec.getName();
        ResType type;
        if (mInvalidConfigs.contains(config)) {
            if (mKeepBroken) {
                LOGGER.warning(String.format(
                    "Invalid resource config detected: %s %s", typeName, config));
                type = mPackage.addType(id, config);
            } else {
                LOGGER.warning(String.format(
                    "Invalid resource config detected. Dropping resources: %s %s",
                    typeName, config));
                type = null;
            }
        } else {
            type = mPackage.addType(id, config);
        }

        boolean isOffset16 = (flags & TYPE_FLAG_OFFSET16) != 0;
        boolean isSparse = (flags & TYPE_FLAG_SPARSE) != 0;

        // Only flag the app as sparse if the main package is not loaded yet.
        if (isSparse && !mTable.isMainPackageLoaded()) {
            mTable.getApkInfo().getResourcesInfo().setSparseEntries(true);
        }

        // #3778 - In some apps the res entries are unordered and might have to jump
        // backwards. We simply pre-sort them by offset.
        Map<Integer, List<Integer>> entryOffsets = new TreeMap<>();
        for (int i = 0; i < entryCount; i++) {
            int index, offset;

            // #3372 - 16-bit offsets should be stored as real offsets (* 4u).
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

            List<Integer> indexes = entryOffsets.get(offset);
            if (indexes == null) {
                indexes = new ArrayList<>();
                entryOffsets.put(offset, indexes);
            }
            indexes.add(index);
        }

        // Exclude NO_ENTRY indexes.
        List<Integer> indexes = entryOffsets.get(NO_ENTRY);
        if (indexes != null) {
            if (type != null) {
                for (int index : indexes) {
                    ResId entryId = ResId.of(mPackage.getId(), id, index);

                    if (!mPackage.hasEntrySpec(entryId)) {
                        mMissingEntrySpecs.add(entryId);
                    }
                }
            }

            entryOffsets.remove(NO_ENTRY);
            // Update the entry count for logging.
            entryCount -= indexes.size();
        }

        // Parse the remaining entries.
        for (Map.Entry<Integer, List<Integer>> entryOffset : entryOffsets.entrySet()) {
            int offset = entryOffset.getKey();
            indexes = entryOffset.getValue();

            // #3428 - In some apps the res entries are padded for alignment, but in #3778
            // it made sense to align to the start of the entries to handle all cases.
            long entryStart = parser.chunkStart() + entriesStart + offset;

            // As seen in some recent APKs - there are more entries reported than can fit
            // in the chunk.
            if (entryStart >= parser.chunkEnd()) {
                LOGGER.warning(String.format(
                    "End of chunk hit. Skipping remaining %d entries in type: %s",
                    entryCount, typeName));
                break;
            }

            // Align the stream with the start of the entry.
            mIn.jumpTo(entryStart);

            Pair<Integer, ResValue> entry = parseEntry(typeName);
            int key = entry.getLeft();
            ResValue value = entry.getRight();

            // Add all entries with the parsed value, or discard them if the config was invalid.
            if (type != null) {
                for (int index : indexes) {
                    ResId entryId = ResId.of(mPackage.getId(), id, index);

                    // #2824 - In some apps the res entries are duplicated with the 2nd being
                    // malformed. AOSP skips this, so we will do the same.
                    if (value == null) {
                        if (!mPackage.hasEntrySpec(entryId)) {
                            mMissingEntrySpecs.add(entryId);
                        }
                        continue;
                    }

                    // The same entry can never be added more than once.
                    if (mPackage.hasEntry(entryId, config)) {
                        LOGGER.warning(String.format(
                            "Ignoring repeated entry: id=%s, config=%s", id, config));
                        continue;
                    }

                    if (!mPackage.hasEntrySpec(entryId)) {
                        mPackage.addEntrySpec(entryId, mKeyStrings.getString(key));
                        mMissingEntrySpecs.remove(entryId);
                    }
                    mPackage.addEntry(entryId, config, value);
                }
            }

            // Update the entry count for logging.
            entryCount -= indexes.size();
        }
    }

    private ResConfig parseConfig() throws AndrolibException, IOException {
        long startPosition = mIn.position();
        // ResTable_config
        int size = mIn.readInt();
        if (size < 8) {
            throw new AndrolibException("Config size < 8");
        }

        int mcc = mIn.readUnsignedShort();
        int mnc = mIn.readUnsignedShort();

        String language = "";
        String region = "";
        if (size >= 12) {
            language = unpackLanguageOrRegion(mIn.readBytes(2), 'a');
            region = unpackLanguageOrRegion(mIn.readBytes(2), '0');
        }

        int orientation = 0;
        int touchscreen = 0;
        if (size >= 14) {
            orientation = mIn.readUnsignedByte();
            touchscreen = mIn.readUnsignedByte();
        }

        int density = 0;
        if (size >= 16) {
            density = mIn.readUnsignedShort();
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
        }

        int screenLayout = 0;
        int uiMode = 0;
        int smallestScreenWidthDp = 0;
        if (size >= 32) {
            screenLayout = mIn.readUnsignedByte();
            uiMode = mIn.readUnsignedByte();
            smallestScreenWidthDp = mIn.readUnsignedShort();
        }

        int screenWidthDp = 0;
        int screenHeightDp = 0;
        if (size >= 36) {
            screenWidthDp = mIn.readUnsignedShort();
            screenHeightDp = mIn.readUnsignedShort();
        }

        String localeScript = "";
        String localeVariant = "";
        if (size >= 48) {
            localeScript = mIn.readAscii(4);
            localeVariant = mIn.readAscii(8);
        }

        int screenLayout2 = 0;
        int colorMode = 0;
        if (size >= 52) {
            screenLayout2 = mIn.readUnsignedByte();
            colorMode = mIn.readUnsignedByte();
            mIn.skipShort(); // screenConfigPad2
        }

        // Data beyond this point is non-standard.
        int bytesRead = (int) (mIn.position() - startPosition);
        byte[] unknown = readExceedingBytes("Config", size, bytesRead);

        ResConfig config = new ResConfig(
            mcc, mnc, language, region, orientation,
            touchscreen, density, keyboard, navigation, inputFlags,
            grammaticalInflection, screenWidth, screenHeight, sdkVersion,
            minorVersion, screenLayout, uiMode, smallestScreenWidthDp,
            screenWidthDp, screenHeightDp, localeScript, localeVariant,
            screenLayout2, colorMode, unknown);

        if (config.isInvalid()) {
            mInvalidConfigs.add(config);
        }

        return config;
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

    private Pair<Integer, ResValue> parseEntry(String typeName) throws IOException {
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
            value = parseBag(typeName);
        } else if (isCompact) {
            // In a compactly packed entry, the key index is the size & type is higher
            // 8 bits on flags. We assume a size of 8 bytes for compact entries and the
            // key index is the data itself encoded.
            int type = (flags >>> 8) & 0xFF;
            value = parseItem(typeName, false, type, key);

            // If compact then the size has the key index encoded.
            key = size;
        } else {
            value = parseItem(typeName, false);
        }

        return Pair.of(key, value);
    }

    private ResValue parseBag(String typeName) throws IOException {
        // ResTable_map_entry
        int parentId = mIn.readInt();
        int count = mIn.readInt();

        // Some apps store ID resource values generated for enum/flag items in attribute
        // resources as empty maps. Replace with a placeholder value.
        if (typeName.equals("id")) {
            return ResCustom.ID;
        }

        ResReference parent = new ResReference(mPackage, ResId.of(parentId));
        ResBag.RawItem[] rawItems = new ResBag.RawItem[count];
        int rawItemsCount = 0;

        for (int i = 0; i < count; i++) {
            // ResTable_map
            int name = mIn.readInt();
            ResItem value = (ResItem) parseItem(typeName, true);

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

    private ResValue parseItem(String typeName, boolean inBag) throws IOException {
        // Res_value
        int size = mIn.readUnsignedShort();
        if (size < 8) {
            return null;
        }
        mIn.skipByte(); // res0
        int type = mIn.readUnsignedByte();
        int data = mIn.readInt();

        return parseItem(typeName, inBag, type, data);
    }

    private ResValue parseItem(String typeName, boolean inBag, int type, int data) {
        String rawValue = null;

        // ID resource values are either encoded as a boolean (false) or a resource reference.
        // A boolean (false) is no longer allowed in XML, replace with a placeholder value.
        // A resource reference is handled normally, unless it's @null.
        if (typeName.equals("id") && (data == 0 || (type != TypedValue.TYPE_REFERENCE
                && type != TypedValue.TYPE_DYNAMIC_REFERENCE))) {
            return ResCustom.ID;
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

    private void parseLibrary(ResChunkPullParser parser) throws IOException {
        // ResTable_lib_header
        int count = mIn.readInt();

        skipUnreadHeader(parser);

        for (int i = 0; i < count; i++) {
            // ResTable_lib_entry
            int packageId = mIn.readInt();
            String packageName = mIn.readUtf16(128);

            if (packageId != 0) {
                mTable.addDynamicRefPackage(packageId, packageName);
            }
        }
    }

    private void parseOverlayable(ResChunkPullParser parser) throws AndrolibException, IOException {
        // ResTable_overlayable_header
        String name = mIn.readUtf16(256);
        String actor = mIn.readUtf16(256);

        skipUnreadHeader(parser);

        // Avoid conflicts by reusing overlayables.
        ResOverlayable overlayable;
        try {
            overlayable = mPackage.getOverlayable(name);
        } catch (UndefinedResObjectException ignored) {
            overlayable = mPackage.addOverlayable(name, actor);
        }

        parser = new ResChunkPullParser(mIn, parser.dataSize());
        while (nextChunk(parser)) {
            if (parser.chunkType() != ResChunkHeader.RES_TABLE_OVERLAYABLE_POLICY_TYPE) {
                skipUnexpectedChunk(parser);
                continue;
            }

            // ResTable_overlayable_policy_header
            int flags = mIn.readInt();
            int entryCount = mIn.readInt();

            skipUnreadHeader(parser);

            ResId[] entries = new ResId[entryCount];
            int entriesCount = 0;

            for (int i = 0; i < entryCount; i++) {
                int id = mIn.readInt();

                if (id != 0) {
                    entries[entriesCount++] = ResId.of(id);
                }
            }

            if (entriesCount < entries.length) {
                entries = Arrays.copyOf(entries, entriesCount);
            }

            overlayable.addPolicy(flags, entries);
        }
    }

    private void parseStagedAliases(ResChunkPullParser parser) throws IOException {
        // ResTable_staged_alias_header
        int count = mIn.readInt();

        skipUnreadHeader(parser);

        for (int i = 0; i < count; i++) {
            // ResTable_staged_alias_entry
            int stagedResId = mIn.readInt();
            int finalizedResId = mIn.readInt();

            LOGGER.fine(String.format(
                "Skipping staged alias: 0x%08x -> 0x%08x", stagedResId, finalizedResId));
        }
    }

    private void skipUnexpectedChunk(ResChunkPullParser parser) throws IOException {
        LOGGER.warning(String.format(
            "Skipping unexpected %s chunk of %d bytes at 0x%08x.",
            parser.chunkName(), parser.chunkSize(), parser.chunkStart()));
        parser.skipChunk();
    }

    private void skipUnreadHeader(ResChunkPullParser parser) throws IOException {
        // Some apps lie about the reported size of their chunk header.
        // Trusting the header size is misleading, so compare to what we actually read in the
        // header vs reported and skip the rest. However, this runs after each chunk and not
        // every chunk reading has a specific distinction between the header and the body.
        int bytesRead = (int) (mIn.position() - parser.chunkStart());
        readExceedingBytes("Chunk header", parser.headerSize(), bytesRead);
    }

    private byte[] readExceedingBytes(String name, int size, int bytesRead) throws IOException {
        int bytesExceeding = size - bytesRead;
        if (bytesExceeding > 0) {
            byte[] buf = mIn.readBytes(bytesExceeding);
            for (int i = 0; i < buf.length; i++) {
                if (buf[i] != 0) {
                    LOGGER.warning(String.format(
                        "%s size: %d bytes, read: %d bytes. Exceeding bytes: %s",
                        name, size, bytesRead, BaseEncoding.base16().encode(buf)));
                    return buf;
                }
            }
        }
        return null;
    }

    private void injectDummyEntrySpecs() throws AndrolibException {
        if (mTable.getConfig().getDecodeResolve() == Config.DecodeResolve.GREEDY) {
            ResReference parent = new ResReference(mPackage, ResId.NULL);
            ResBag.RawItem[] rawItems = new ResBag.RawItem[0];

            for (ResId id : mMissingEntrySpecs) {
                ResTypeSpec typeSpec = mPackage.getTypeSpec(id.getTypeId());
                String typeName = typeSpec.getName();
                ResValue value;
                if (typeName.equals("id")) {
                    value = ResCustom.ID;
                } else if (typeName.equals("string")) {
                    value = ResString.EMPTY;
                } else if (typeSpec.isBagType()) {
                    value = ResBag.parse(typeName, parent, rawItems);
                } else {
                    value = ResReference.NULL;
                }

                mPackage.addEntrySpec(id, ResEntrySpec.DUMMY_PREFIX + id);
                mPackage.addEntry(id, ResConfig.DEFAULT, value);
            }
        }

        mMissingEntrySpecs.clear();
    }
}
