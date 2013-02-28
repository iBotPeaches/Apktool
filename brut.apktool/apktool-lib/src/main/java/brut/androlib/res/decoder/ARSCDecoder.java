/**
 *  Copyright 2011 Ryszard Wiśniewski <brut.alll@gmail.com>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package brut.androlib.res.decoder;

import android.util.TypedValue;
import brut.androlib.AndrolibException;
import brut.androlib.res.data.*;
import brut.androlib.res.data.value.*;
import brut.util.Duo;
import brut.androlib.res.data.ResTable;
import brut.util.ExtDataInput;
import com.mindprod.ledatastream.LEDataInputStream;
import java.io.*;
import java.math.BigInteger;
import java.util.*;
import java.util.logging.Logger;
import org.apache.commons.io.input.CountingInputStream;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public class ARSCDecoder {
	public static ARSCData decode(InputStream arscStream,
			boolean findFlagsOffsets, boolean keepBroken)
			throws AndrolibException {
		return decode(arscStream, findFlagsOffsets, keepBroken, new ResTable());
	}

	public static ARSCData decode(InputStream arscStream,
			boolean findFlagsOffsets, boolean keepBroken, ResTable resTable)
			throws AndrolibException {
		try {
			ARSCDecoder decoder = new ARSCDecoder(arscStream, resTable,
					findFlagsOffsets, keepBroken);
			ResPackage[] pkgs = decoder.readTable();
			return new ARSCData(pkgs, decoder.mFlagsOffsets == null ? null
					: decoder.mFlagsOffsets.toArray(new FlagsOffset[0]),
					resTable);
		} catch (IOException ex) {
			throw new AndrolibException("Could not decode arsc file", ex);
		}
	}

	private ARSCDecoder(InputStream arscStream, ResTable resTable,
			boolean storeFlagsOffsets, boolean keepBroken) {
		if (storeFlagsOffsets) {
			arscStream = mCountIn = new CountingInputStream(arscStream);
			mFlagsOffsets = new ArrayList<FlagsOffset>();
		} else {
			mCountIn = null;
			mFlagsOffsets = null;
		}
		mIn = new ExtDataInput(new LEDataInputStream(arscStream));
		mResTable = resTable;
		mKeepBroken = keepBroken;
	}

	private ResPackage[] readTable() throws IOException, AndrolibException {
		nextChunkCheckType(Header.TYPE_TABLE);
		int packageCount = mIn.readInt();

		mTableStrings = StringBlock.read(mIn);
		ResPackage[] packages = new ResPackage[packageCount];

		nextChunk();
		for (int i = 0; i < packageCount; i++) {
			packages[i] = readPackage();
		}

		// store package
		if (this.mResTable.isPackageInfoValueSet("cur_package") != true) {
			this.mResTable.addPackageInfo("cur_package", packages[0].getName());
		}
		return packages;
	}

	private ResPackage readPackage() throws IOException, AndrolibException {
		checkChunkType(Header.TYPE_PACKAGE);
		int id = (byte) mIn.readInt();
		String name = mIn.readNulEndedString(128, true);
		/* typeNameStrings */mIn.skipInt();
		/* typeNameCount */mIn.skipInt();
		/* specNameStrings */mIn.skipInt();
		/* specNameCount */mIn.skipInt();

		mTypeNames = StringBlock.read(mIn);
		mSpecNames = StringBlock.read(mIn);

		mResId = id << 24;
		mPkg = new ResPackage(mResTable, id, name);

		nextChunk();
		while (mHeader.type == Header.TYPE_TYPE) {
			readType();
		}

		return mPkg;
	}

	private ResType readType() throws AndrolibException, IOException {
		checkChunkType(Header.TYPE_TYPE);
		byte id = mIn.readByte();
		mIn.skipBytes(3);
		int entryCount = mIn.readInt();

		mMissingResSpecs = new boolean[entryCount];
		Arrays.fill(mMissingResSpecs, true);

		if (mFlagsOffsets != null) {
			mFlagsOffsets.add(new FlagsOffset(mCountIn.getCount(), entryCount));
		}
		/* flags */mIn.skipBytes(entryCount * 4);

		mResId = (0xff000000 & mResId) | id << 16;
		mType = new ResType(mTypeNames.getString(id - 1), mResTable, mPkg);
		mPkg.addType(mType);

		while (nextChunk().type == Header.TYPE_CONFIG) {
			readConfig();
		}

		addMissingResSpecs();

		return mType;
	}

	private ResConfig readConfig() throws IOException, AndrolibException {
		checkChunkType(Header.TYPE_CONFIG);
		/* typeId */mIn.skipInt();
		int entryCount = mIn.readInt();
		/* entriesStart */mIn.skipInt();

		ResConfigFlags flags = readConfigFlags();
		int[] entryOffsets = mIn.readIntArray(entryCount);

		if (flags.isInvalid) {
			String resName = mType.getName() + flags.getQualifiers();
			if (mKeepBroken) {
				LOGGER.warning("Invalid config flags detected: " + resName);
			} else {
				LOGGER.warning("Invalid config flags detected. Dropping resources: "
						+ resName);
			}
		}

		mConfig = flags.isInvalid && !mKeepBroken ? null : mPkg
				.getOrCreateConfig(flags);

		for (int i = 0; i < entryOffsets.length; i++) {
			if (entryOffsets[i] != -1) {
				mMissingResSpecs[i] = false;
				mResId = (mResId & 0xffff0000) | i;
				readEntry();
			}
		}

		return mConfig;
	}

	private void readEntry() throws IOException, AndrolibException {
		/* size */mIn.skipBytes(2);
		short flags = mIn.readShort();
		int specNamesId = mIn.readInt();

		ResValue value = (flags & ENTRY_FLAG_COMPLEX) == 0 ? readValue()
				: readComplexEntry();

		if (mConfig == null) {
			return;
		}

		ResID resId = new ResID(mResId);
		ResResSpec spec;
		if (mPkg.hasResSpec(resId)) {
			spec = mPkg.getResSpec(resId);
		} else {
			spec = new ResResSpec(resId, mSpecNames.getString(specNamesId),
					mPkg, mType);
			mPkg.addResSpec(spec);
			mType.addResSpec(spec);
		}
		ResResource res = new ResResource(mConfig, spec, value);

		mConfig.addResource(res);
		spec.addResource(res);
		mPkg.addResource(res);
	}

	private ResBagValue readComplexEntry() throws IOException,
			AndrolibException {
		int parent = mIn.readInt();
		int count = mIn.readInt();

		ResValueFactory factory = mPkg.getValueFactory();
		Duo<Integer, ResScalarValue>[] items = new Duo[count];
		for (int i = 0; i < count; i++) {
			items[i] = new Duo<Integer, ResScalarValue>(mIn.readInt(),
					(ResScalarValue) readValue());
		}

		return factory.bagFactory(parent, items);
	}

	private ResValue readValue() throws IOException, AndrolibException {
		/* size */mIn.skipCheckShort((short) 8);
		/* zero */mIn.skipCheckByte((byte) 0);
		byte type = mIn.readByte();
		int data = mIn.readInt();

		return type == TypedValue.TYPE_STRING ? mPkg.getValueFactory().factory(
				mTableStrings.getHTML(data)) : mPkg.getValueFactory().factory(
				type, data, null);
	}

	private ResConfigFlags readConfigFlags() throws IOException,
			AndrolibException {
		int size = mIn.readInt();
		if (size < 28) {
			throw new AndrolibException("Config size < 28");
		}

		boolean isInvalid = false;

		short mcc = mIn.readShort();
		short mnc = mIn.readShort();

		char[] language = new char[] { (char) mIn.readByte(),
				(char) mIn.readByte() };
		char[] country = new char[] { (char) mIn.readByte(),
				(char) mIn.readByte() };

		byte orientation = mIn.readByte();
		byte touchscreen = mIn.readByte();
		short density = mIn.readShort();

		byte keyboard = mIn.readByte();
		byte navigation = mIn.readByte();
		byte inputFlags = mIn.readByte();
		/* inputPad0 */mIn.skipBytes(1);

		short screenWidth = mIn.readShort();
		short screenHeight = mIn.readShort();

		short sdkVersion = mIn.readShort();
		/* minorVersion, now must always be 0 */mIn.skipBytes(2);

		byte screenLayout = 0;
		byte uiMode = 0;
		short smallestScreenWidthDp = 0;
		if (size >= 32) {
			screenLayout = mIn.readByte();
			uiMode = mIn.readByte();
			smallestScreenWidthDp = mIn.readShort();
		}

		short screenWidthDp = 0;
		short screenHeightDp = 0;

		if (size >= 36) {
			screenWidthDp = mIn.readShort();
			screenHeightDp = mIn.readShort();
		}

		short layoutDirection = 0;
		if (size >= 38 && sdkVersion >= 17
				&& !this.mPkg.getName().equalsIgnoreCase("com.htc")) {
			layoutDirection = mIn.readShort();
		}

		int exceedingSize = size - KNOWN_CONFIG_BYTES;
		if (exceedingSize > 0) {
			byte[] buf = new byte[exceedingSize];
			mIn.readFully(buf);
			BigInteger exceedingBI = new BigInteger(1, buf);

			if (exceedingBI.equals(BigInteger.ZERO)) {
				LOGGER.fine(String
						.format("Config flags size > %d, but exceeding bytes are all zero, so it should be ok.",
								KNOWN_CONFIG_BYTES));
			} else {
				LOGGER.warning(String.format(
						"Config flags size > %d. Exceeding bytes: 0x%X.",
						KNOWN_CONFIG_BYTES, exceedingBI));
				isInvalid = true;
			}
		}

		return new ResConfigFlags(mcc, mnc, language, country, layoutDirection,
				orientation, touchscreen, density, keyboard, navigation,
				inputFlags, screenWidth, screenHeight, sdkVersion,
				screenLayout, uiMode, smallestScreenWidthDp, screenWidthDp,
				screenHeightDp, isInvalid);
	}

	private void addMissingResSpecs() throws AndrolibException {
		int resId = mResId & 0xffff0000;

		for (int i = 0; i < mMissingResSpecs.length; i++) {
			if (!mMissingResSpecs[i]) {
				continue;
			}

			ResResSpec spec = new ResResSpec(new ResID(resId | i),
					String.format("APKTOOL_DUMMY_%04x", i), mPkg, mType);
			mPkg.addResSpec(spec);
			mType.addResSpec(spec);

			if (mConfig == null) {
			    mConfig = mPkg.getOrCreateConfig(new ResConfigFlags());
			}

			ResValue value = new ResBoolValue(false, null);
			ResResource res = new ResResource(mConfig, spec, value);

			mPkg.addResource(res);
			mConfig.addResource(res);
			spec.addResource(res);
		}
	}

	private Header nextChunk() throws IOException {
		return mHeader = Header.read(mIn);
	}

	private void checkChunkType(int expectedType) throws AndrolibException {
		if (mHeader.type != expectedType) {
			throw new AndrolibException(String.format(
					"Invalid chunk type: expected=0x%08x, got=0x%08x",
					expectedType, mHeader.type));
		}
	}

	private void nextChunkCheckType(int expectedType) throws IOException,
			AndrolibException {
		nextChunk();
		checkChunkType(expectedType);
	}

	private final ExtDataInput mIn;
	private final ResTable mResTable;
	private final CountingInputStream mCountIn;
	private final List<FlagsOffset> mFlagsOffsets;
	private final boolean mKeepBroken;

	private Header mHeader;
	private StringBlock mTableStrings;
	private StringBlock mTypeNames;
	private StringBlock mSpecNames;
	private ResPackage mPkg;
	private ResType mType;
	private ResConfig mConfig;
	private int mResId;
	private boolean[] mMissingResSpecs;

	private final static short ENTRY_FLAG_COMPLEX = 0x0001;

	public static class Header {
		public final short type;
		public final int chunkSize;

		public Header(short type, int size) {
			this.type = type;
			this.chunkSize = size;
		}

		public static Header read(ExtDataInput in) throws IOException {
			short type;
			try {
				type = in.readShort();
			} catch (EOFException ex) {
				return new Header(TYPE_NONE, 0);
			}
			in.skipBytes(2);
			return new Header(type, in.readInt());
		}

		public final static short TYPE_NONE = -1, TYPE_TABLE = 0x0002,
				TYPE_PACKAGE = 0x0200, TYPE_TYPE = 0x0202,
				TYPE_CONFIG = 0x0201;
	}

	public static class FlagsOffset {
		public final int offset;
		public final int count;

		public FlagsOffset(int offset, int count) {
			this.offset = offset;
			this.count = count;
		}
	}

	private static final Logger LOGGER = Logger.getLogger(ARSCDecoder.class
			.getName());
	private static final int KNOWN_CONFIG_BYTES = 38;

	public static class ARSCData {

		public ARSCData(ResPackage[] packages, FlagsOffset[] flagsOffsets,
				ResTable resTable) {
			mPackages = packages;
			mFlagsOffsets = flagsOffsets;
			mResTable = resTable;
		}

		public FlagsOffset[] getFlagsOffsets() {
			return mFlagsOffsets;
		}

		public ResPackage[] getPackages() {
			return mPackages;
		}

		public ResPackage getOnePackage() throws AndrolibException {
			if (mPackages.length != 1) {
				throw new AndrolibException(
						"Arsc file contains zero or multiple packages");
			}
			return mPackages[0];
		}

		public ResTable getResTable() {
			return mResTable;
		}

		private final ResPackage[] mPackages;
		private final FlagsOffset[] mFlagsOffsets;
		private final ResTable mResTable;
	}
}