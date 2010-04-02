/*
 *  Copyright 2010 Ryszard Wiśniewski <brut.alll@gmail.com>.
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
 *  under the License.
 */
package brut.androlib.res.decoder;

import android.util.TypedValue;
import brut.androlib.AndrolibException;
import brut.androlib.res.data.*;
import brut.androlib.res.data.value.*;
import brut.util.Duo;
import brut.util.ExtDataInput;
import com.mindprod.ledatastream.LEDataInputStream;
import java.io.*;
import java.util.logging.Logger;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public class ARSCDecoder {
    public static ResPackage[] decode(InputStream arscStream, ResTable resTable)
            throws AndrolibException {
        try {
            return new ARSCDecoder(arscStream, resTable).readTable();
        } catch (IOException ex) {
            throw new AndrolibException("Could not decode arsc file", ex);
        }
    }

    private ARSCDecoder(InputStream arscStream, ResTable resTable) {
        mIn = new ExtDataInput(new LEDataInputStream(arscStream));
        mResTable = resTable;
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

        return packages;
    }

    private ResPackage readPackage() throws IOException, AndrolibException {
        checkChunkType(Header.TYPE_PACKAGE);
        int id = (byte) mIn.readInt();
        String name = mIn.readNulEndedString(128, true);
        /*typeNameStrings*/ mIn.skipInt();
        /*typeNameCount*/ mIn.skipInt();
        /*specNameStrings*/ mIn.skipInt();
        /*specNameCount*/ mIn.skipInt();

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
        byte id =  mIn.readByte();
        mIn.skipBytes(3);
        int entryCount = mIn.readInt();
        /*flags*/ mIn.skipBytes(entryCount * 4);

        mResId = (0xff000000 & mResId) | id << 16;
        mType = new ResType(mTypeNames.getString(id - 1), mResTable, mPkg);
        mPkg.addType(mType);

        while (nextChunk().type == Header.TYPE_CONFIG) {
            readConfig();
        }

        return mType;
    }

    private ResConfig readConfig() throws IOException, AndrolibException {
        checkChunkType(Header.TYPE_CONFIG);
        /*typeId*/ mIn.skipInt();
        int entryCount = mIn.readInt();
        /*entriesStart*/ mIn.skipInt();

        ResConfigFlags flags = readConfigFlags();
        int[] entryOffsets = mIn.readIntArray(entryCount);

        ResConfig config;
        if (mPkg.hasConfig(flags)) {
            config = mPkg.getConfig(flags);
        } else {
            config = new ResConfig(flags);
            mPkg.addConfig(config);
        }
        mConfig = config;

        for (int i = 0; i < entryOffsets.length; i++) {
            if (entryOffsets[i] != -1) {
                mResId = (mResId & 0xffff0000) | i;
                readEntry();
            }
        }
        
        return config;
    }

    private void readEntry() throws IOException, AndrolibException {
        /*size*/ mIn.skipBytes(2);
        short flags = mIn.readShort();
        int specNamesId = mIn.readInt();

        ResID resId = new ResID(mResId);
        ResResSpec spec;
        if (mPkg.hasResSpec(resId)) {
            spec = mPkg.getResSpec(resId);
        } else {
            spec = new ResResSpec(
                resId, mSpecNames.getString(specNamesId), mPkg, mType);
            mPkg.addResSpec(spec);
            mType.addResSpec(spec);
        }

        ResValue value = (flags & ENTRY_FLAG_COMPLEX) == 0 ?
            readValue() : readComplexEntry();
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
            items[i] = new Duo<Integer, ResScalarValue>(
                mIn.readInt(), (ResScalarValue) readValue());
        }

        return factory.bagFactory(parent, items);
    }

    private ResValue readValue() throws IOException, AndrolibException {
        /*size*/ mIn.skipCheckShort((short) 8);
        /*zero*/ mIn.skipCheckByte((byte) 0);
        byte type = mIn.readByte();
        int data = mIn.readInt();

        return type == TypedValue.TYPE_STRING ?
            mPkg.getValueFactory().factory(mTableStrings.getString(data)) :
            mPkg.getValueFactory().factory(type, data);
    }

    private ResConfigFlags readConfigFlags() throws IOException, AndrolibException {
        int size = mIn.readInt();
        if (size < 28) {
            throw new AndrolibException("Config size < 28");
        } else if (size > 32) {
            LOGGER.warning("Config size > 32");
        }

        short mcc = mIn.readShort();
        short mnc = mIn.readShort();

        char[] language = new char[]{
            (char) mIn.readByte(), (char) mIn.readByte()};
        char[] country = new char[]{
            (char) mIn.readByte(), (char) mIn.readByte()};

        byte orientation = mIn.readByte();
        byte touchscreen = mIn.readByte();
        short density = mIn.readShort();

        byte keyboard = mIn.readByte();
        byte navigation = mIn.readByte();
        byte inputFlags = mIn.readByte();
        mIn.skipBytes(1);

        short screenWidth = mIn.readShort();
        short screenHeight = mIn.readShort();

        short sdkVersion = mIn.readShort();
        mIn.skipBytes(2);

        byte screenLayout = 0;
        if (size >= 32) {
            screenLayout = mIn.readByte();
            mIn.skipBytes(3);
        }

        if (size > 32) {
            mIn.skipBytes(size - 32);
        }

        return new ResConfigFlags(mcc, mnc, language, country, orientation,
            touchscreen, density, keyboard, navigation, inputFlags,
            screenWidth, screenHeight, screenLayout, sdkVersion);
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

    private void nextChunkCheckType(int expectedType)
            throws IOException, AndrolibException {
        nextChunk();
        checkChunkType(expectedType);
    }

    private final ExtDataInput mIn;
    private final ResTable mResTable;

    private Header mHeader;
    private StringBlock mTableStrings;
    private StringBlock mTypeNames;
    private StringBlock mSpecNames;
    private ResPackage mPkg;
    private ResType mType;
    private ResConfig mConfig;
    private int mResId;


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

        public final static short
            TYPE_NONE = -1,
            TYPE_TABLE = 0x0002,
            TYPE_PACKAGE = 0x0200,
            TYPE_TYPE = 0x0202,
            TYPE_CONFIG = 0x0201;
    }

    private static final Logger LOGGER =
        Logger.getLogger(ARSCDecoder.class.getName());
}
