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

import brut.androlib.exceptions.AndrolibException;
import brut.androlib.exceptions.FrameworkNotFoundException;
import brut.androlib.exceptions.UndefinedResObjectException;
import brut.androlib.res.data.ResChunkHeader;
import brut.androlib.res.data.ResStringPool;
import brut.androlib.res.table.*;
import brut.androlib.res.table.value.*;
import brut.androlib.res.xml.ResXmlUtils;
import brut.util.BinaryDataInputStream;
import com.google.common.io.BaseEncoding;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.InputStream;
import java.io.IOException;
import java.io.Reader;
import java.util.logging.Logger;

public class BinaryXmlResourceParser implements XmlPullParser {
    private static final Logger LOGGER = Logger.getLogger(BinaryXmlResourceParser.class.getName());

    private static final String E_NOT_SUPPORTED = "Method is not supported.";

    private static final int ANDROID_PKG_ID = 0x01;
    private static final int PRIVATE_PKG_ID = 0x7F;

    private final ResTable mTable;
    private final boolean mIgnoreRawValues;
    private final boolean mSkipUnresolved;
    private final NamespaceStack mNamespaces;

    private BinaryDataInputStream mIn;
    private ResChunkPullParser mParser;
    private ResStringPool mStringPool;
    private ResId[] mResourceMap;
    private boolean mHasRawValues;
    private AndrolibException mFirstError;

    private int mEventType;
    private int mLineNumber;
    private int mNamespaceIndex;
    private int mNameIndex;
    private int mIdIndex;
    private int mClassIndex;
    private int mStyleIndex;
    private Attribute[] mAttributes;

    public BinaryXmlResourceParser(ResTable table, boolean ignoreRawValues, boolean skipUnresolved) {
        mTable = table;
        mIgnoreRawValues = ignoreRawValues;
        mSkipUnresolved = skipUnresolved;
        mNamespaces = new NamespaceStack();
        resetEventInfo();
    }

    public AndrolibException getFirstError() {
        return mFirstError;
    }

    // XmlPullParser

    @Override
    public void setFeature(String name, boolean state) throws XmlPullParserException {
        throw new XmlPullParserException(E_NOT_SUPPORTED);
    }

    @Override
    public boolean getFeature(String name) {
        return false;
    }

    @Override
    public void setProperty(String name, Object value) throws XmlPullParserException {
        throw new XmlPullParserException(E_NOT_SUPPORTED);
    }

    @Override
    public Object getProperty(String name) {
        return null;
    }

    @Override
    public void setInput(Reader in) throws XmlPullParserException {
        throw new XmlPullParserException(E_NOT_SUPPORTED);
    }

    @Override
    public void setInput(InputStream inputStream, String inputEncoding)
            throws XmlPullParserException {
        if (inputEncoding != null) {
            throw new XmlPullParserException(E_NOT_SUPPORTED);
        }

        reset();
        mIn = new BinaryDataInputStream(inputStream);
        mParser = new ResChunkPullParser(mIn);
        try {
            if (!nextChunk()) {
                throw new IOException("Input file is empty.");
            }

            if (mParser.chunkType() != ResChunkHeader.RES_XML_TYPE) {
                throw new IOException("Unexpected chunk: " + mParser.chunkName()
                        + " (expected: RES_XML_TYPE)");
            }
        } catch (IOException ex) {
            throw new XmlPullParserException("Could not initialize parser.", this, ex);
        }

        mParser = new ResChunkPullParser(mIn, mParser.dataSize());
    }

    @Override
    public String getInputEncoding() {
        return null;
    }

    @Override
    public void defineEntityReplacementText(String entityName, String replacementText)
            throws XmlPullParserException {
        throw new XmlPullParserException(E_NOT_SUPPORTED);
    }

    @Override
    public int getNamespaceCount(int depth) {
        return mNamespaces.getCount(depth);
    }

    @Override
    public String getNamespacePrefix(int pos) {
        if (mStringPool == null) {
            return null;
        }
        int prefixIdx = mNamespaces.getPrefix(pos);
        return mStringPool.getString(prefixIdx);
    }

    @Override
    public String getNamespaceUri(int pos) {
        if (mStringPool == null) {
            return null;
        }
        int uriIdx = mNamespaces.getUri(pos);
        return mStringPool.getString(uriIdx);
    }

    @Override
    public String getNamespace(String prefix) {
        throw new RuntimeException(E_NOT_SUPPORTED);
    }

    @Override
    public int getDepth() {
        return mNamespaces.getDepth();
    }

    @Override
    public String getPositionDescription() {
        return "XML line #" + getLineNumber();
    }

    @Override
    public int getLineNumber() {
        return mLineNumber;
    }

    @Override
    public int getColumnNumber() {
        return -1;
    }

    @Override
    public boolean isWhitespace() throws XmlPullParserException {
        if (mEventType != TEXT) {
            throw new XmlPullParserException(
                "Parser must be on TEXT to get text.", this, null);
        }
        String text = getText();
        if (text == null) {
            return true;
        }
        int len = text.length();
        for (int i = 0; i < len; i++) {
            if (!Character.isWhitespace(text.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String getText() {
        if (mStringPool == null || mEventType != TEXT) {
            return null;
        }
        return mStringPool.getString(mNameIndex);
    }

    @Override
    public char[] getTextCharacters(int[] holderForStartAndLength) {
        String text = getText();
        if (text == null) {
            return null;
        }
        int len = text.length();
        holderForStartAndLength[0] = 0;
        holderForStartAndLength[1] = len;
        char[] chars = new char[len];
        text.getChars(0, len, chars, 0);
        return chars;
    }

    @Override
    public String getNamespace() {
        if (mStringPool == null || (mEventType != START_TAG && mEventType != END_TAG)) {
            return null;
        }
        return mStringPool.getString(mNamespaceIndex);
    }

    @Override
    public String getName() {
        if (mStringPool == null || (mEventType != START_TAG && mEventType != END_TAG)) {
            return null;
        }
        return mStringPool.getString(mNameIndex);
    }

    @Override
    public String getPrefix() {
        if (mStringPool == null || (mEventType != START_TAG && mEventType != END_TAG)) {
            return null;
        }
        int prefixIdx = mNamespaces.findPrefix(mNamespaceIndex);
        return mStringPool.getString(prefixIdx);
    }

    @Override
    public boolean isEmptyElementTag() {
        return false;
    }

    @Override
    public int getAttributeCount() {
        if (mEventType != START_TAG) {
            return -1;
        }
        return mAttributes != null ? mAttributes.length : 0;
    }

    @Override
    public String getAttributeNamespace(int index) {
        Attribute attr = getAttribute(index);
        if (attr == null) {
            return NO_NAMESPACE;
        }

        ResId nameId = getAttributeNameResourceId(index);
        int pkgId = nameId.getPackageId();

        // #2972 - If the namespace index is -1, the attribute is not present, but if the
        // attribute is from system we can resolve it to the default namespace.
        // This may prove to be too aggressive as we scope the entire system namespace,
        // but it's better than not resolving it at all.
        if (attr.ns < 0) {
            if (pkgId == PRIVATE_PKG_ID) {
                return getNonDefaultNamespaceUri(index);
            }
            if (pkgId == ANDROID_PKG_ID) {
                return ResXmlUtils.ANDROID_RES_NS;
            }
            return NO_NAMESPACE;
        }

        // Minifiers like removing the namespace, so we will fall back to default namespace
        // unless the package ID of the resource is private. We will grab the non-standard one.
        String uri = mStringPool != null ? mStringPool.getString(attr.ns) : null;
        if (uri != null && !uri.isEmpty()) {
            return uri;
        }
        if (pkgId == PRIVATE_PKG_ID) {
            return getNonDefaultNamespaceUri(index);
        }
        return ResXmlUtils.ANDROID_RES_NS;
    }

    @Override
    public String getAttributeName(int index) {
        Attribute attr = getAttribute(index);
        if (attr == null || attr.name < 0) {
            return "";
        }

        ResId nameId = getAttributeNameResourceId(index);

        // Android prefers the resource table value over what the string pool has.
        // This can be seen quite often in obfuscated apps where values such as:
        // <item android:state_enabled="true" app:state_collapsed="false" app:state_collapsible="true">
        // Are improperly decoded when trusting the string pool.
        // Leveraging the resource table allows us to get the proper value.
        // <item android:state_enabled="true" app:d2="false" app:d3="true">
        String name;
        try {
            name = resolveResourceName(nameId);
            if (name != null) {
                return name;
            }
        } catch (AndrolibException ignored) {
        }

        // Couldn't decode from resource table, fall back to string pool.
        name = mStringPool != null ? mStringPool.getString(attr.name) : null;
        if (name == null) {
            name = "";
        }

        // In certain optimized apps, some attributes's specs are removed despite being used.
        // Inject a generic spec for the attribute, otherwise we can't rebuild.
        if (nameId != ResId.NULL) {
            try {
                ResPackage pkg = mTable.getMainPackage();

                // #2836 - Skip item if the resource cannot be resolved.
                if (mSkipUnresolved || nameId.getPackageId() != pkg.getId()) {
                    LOGGER.warning(String.format(
                        "Unresolved attr reference: ns=%s, name=%s, id=%s",
                        getAttributePrefix(index), name, nameId));
                    return name;
                }

                if (name.isEmpty()) {
                    name = ResEntrySpec.DUMMY_PREFIX + nameId;
                }
                name = pkg.addEntrySpec(nameId, name).getName();
                pkg.addEntry(nameId, ResConfig.DEFAULT, ResAttribute.DEFAULT);
            } catch (AndrolibException ex) {
                if (mFirstError == null) {
                    mFirstError = ex;
                }
                LOGGER.warning(String.format(
                    "Could not add missing attr: ns=%s, name=%s, id=%s",
                    getAttributePrefix(index), name, nameId));
            }
        }

        return name;
    }

    @Override
    public String getAttributePrefix(int index) {
        if (mStringPool == null) {
            return "";
        }
        Attribute attr = getAttribute(index);
        if (attr == null || attr.ns < 0) {
            return "";
        }
        int prefixIdx = mNamespaces.findPrefix(attr.ns);
        String prefix = mStringPool.getString(prefixIdx);
        if (prefix == null) {
            return "";
        }
        return prefix;
    }

    @Override
    public String getAttributeType(int index) {
        return "CDATA";
    }

    @Override
    public boolean isAttributeDefault(int index) {
        return false;
    }

    @Override
    public String getAttributeValue(int index) {
        Attribute attr = getAttribute(index);
        if (attr == null) {
            return "";
        }

        // Use the raw value if preserved (rare for modern apps).
        if (mHasRawValues && !mIgnoreRawValues) {
            String rawValue = mStringPool != null ? mStringPool.getString(attr.rawValue) : null;
            if (rawValue != null) {
                return rawValue;
            }
        }

        // Try to decode the typed value from the resource table.
        ResItem value = null;
        String name = null;
        String decoded = null;
        try {
            ResPackage pkg = mTable.getMainPackage();
            if (pkg == null) {
                // If no main package, we load "android" package instead.
                pkg = mTable.getPackage(1);
            }

            if (attr.valueType == ResValue.TYPE_STRING) {
                CharSequence strValue = mStringPool != null
                    ? mStringPool.getText(attr.valueData) : null;
                value = strValue != null ? new ResString(strValue) : null;
            } else {
                value = ResItem.parse(pkg, attr.valueType, attr.valueData);
            }

            if (value != null) {
                ResId nameId = getAttributeNameResourceId(index);
                if (nameId != ResId.NULL) {
                    // We need the attribute entry's value to format this value.
                    try {
                        ResEntry nameEntry = mTable.getDefaultEntry(nameId);
                        ResEntrySpec nameSpec = nameEntry.getSpec();
                        name = nameSpec.getName();
                        ResValue nameValue = nameEntry.getValue();
                        if (nameValue instanceof ResAttribute) {
                            ResAttribute nameAttr = (ResAttribute) nameValue;

                            // Add the value type to the attribute if needed.
                            boolean isExplicitType;
                            switch (attr.valueType) {
                                case ResValue.TYPE_NULL:
                                case ResValue.TYPE_REFERENCE:
                                case ResValue.TYPE_DYNAMIC_REFERENCE:
                                case ResValue.TYPE_ATTRIBUTE:
                                case ResValue.TYPE_DYNAMIC_ATTRIBUTE:
                                    isExplicitType = false;
                                    break;
                                default:
                                    isExplicitType = true;
                                    break;
                            }
                            if (isExplicitType && !nameAttr.hasSymbolsForValue(value)) {
                                nameAttr.addValueType(attr.valueType);
                            }

                            decoded = nameAttr.formatAsAttributeValue(value);
                        } else {
                            LOGGER.warning("Unexpected attribute name spec: " + nameSpec);
                        }
                    } catch (UndefinedResObjectException ignored) {
                    }
                } else {
                    // Format the value with the default attribute.
                    decoded = ResAttribute.DEFAULT.formatAsAttributeValue(value);
                }
            }
        } catch (AndrolibException ex) {
            if (mFirstError == null) {
                mFirstError = ex;
            }
        }

        if (decoded == null) {
            if (name == null) {
                name = mStringPool != null ? mStringPool.getString(attr.name) : null;
            }

            LOGGER.warning(String.format(
                "Could not decode attribute value: ns=%s, name=%s, type=0x%02x, value=0x%08x",
                getAttributePrefix(index), name, attr.valueType, attr.valueData));

            if (value != null) {
                // Format the value with the default attribute.
                try {
                    decoded = ResAttribute.DEFAULT.formatAsAttributeValue(value);
                } catch (AndrolibException ignored) {
                }
            }
            if (decoded == null) {
                decoded = "";
            }
        }

        return decoded;
    }

    @Override
    public String getAttributeValue(String namespace, String name) {
        if (mEventType != START_TAG) {
            throw new IndexOutOfBoundsException("Parser must be on START_TAG to get attributes.");
        }
        if (mAttributes == null || mStringPool == null || name == null) {
            return "";
        }
        int uriIdx = mStringPool.findString(namespace);
        int nameIdx = mStringPool.findString(name);
        for (int i = 0; i < mAttributes.length; i++) {
            Attribute attr = mAttributes[i];
            if (attr != null && uriIdx == attr.ns && nameIdx == attr.name) {
                return getAttributeValue(i);
            }
        }
        return "";
    }

    @Override
    public int getEventType() {
        return mEventType;
    }

    @Override
    public int next() throws XmlPullParserException, IOException {
        if (mIn == null) {
            throw new XmlPullParserException("Parser is not opened.", this, null);
        }
        try {
            return doNext();
        } catch (IOException ex) {
            reset();
            throw ex;
        }
    }

    @Override
    public int nextToken() throws XmlPullParserException, IOException {
        return next();
    }

    @Override
    public void require(int type, String namespace, String name) throws XmlPullParserException {
        if (type != mEventType || (namespace != null && !namespace.equals(getNamespace()))
                || (name != null && !name.equals(getName()))) {
            throw new XmlPullParserException(TYPES[type] + " is expected.", this, null);
        }
    }

    @Override
    public String nextText() throws XmlPullParserException, IOException {
        if (mEventType != START_TAG) {
            throw new XmlPullParserException(
                "Parser must be on START_TAG to read next text.", this, null);
        }
        int eventType = next();
        if (eventType == END_TAG) {
            return "";
        }
        if (eventType != TEXT) {
            throw new XmlPullParserException(
                "Parser must be on TEXT or END_TAG to read text.", this, null);
        }
        String result = getText();
        eventType = next();
        if (eventType != END_TAG) {
            throw new XmlPullParserException(
                "Event TEXT must be immediately followed by END_TAG.", this, null);
        }
        return result;
    }

    @Override
    public int nextTag() throws XmlPullParserException, IOException {
        int eventType = next();
        if (eventType == TEXT && isWhitespace()) {
            eventType = next();
        }
        if (eventType != START_TAG && eventType != END_TAG) {
            throw new XmlPullParserException("Expected start or end tag.", this, null);
        }
        return eventType;
    }

    // Utility methods

    private String getNonDefaultNamespaceUri(int pos) {
        String prefix = getNamespacePrefix(pos);
        if (prefix == null) {
            // If we are here. There is some clever obfuscation going on.
            // Our reference points to the namespace are gone.
            // We have the namespaces that can't be touched in the opening tag.
            // Though no known way to correlate them at this time.
            // So return the res-auto namespace.
            return ResXmlUtils.ANDROID_RES_NS_AUTO;
        }
        return getNamespaceUri(pos);
    }

    private Attribute getAttribute(int index) {
        if (mEventType != START_TAG) {
            throw new IndexOutOfBoundsException("Parser must be on START_TAG to get attributes.");
        }
        if (mAttributes == null || index < 0 || index >= mAttributes.length) {
            throw new IndexOutOfBoundsException(String.format(
                "Attribute index out of range: index=%d, length=%d",
                index, mAttributes != null ? mAttributes.length : 0));
        }
        return mAttributes[index];
    }

    private ResId getAttributeNameResourceId(int index) {
        if (mResourceMap == null) {
            return ResId.NULL;
        }
        Attribute attr = getAttribute(index);
        if (attr == null || attr.name < 0 || attr.name >= mResourceMap.length) {
            return ResId.NULL;
        }
        return mResourceMap[attr.name];
    }

    private String resolveResourceName(ResId id) throws AndrolibException {
        if (id != ResId.NULL) {
            try {
                return mTable.getEntrySpec(id).getName();
            } catch (UndefinedResObjectException | FrameworkNotFoundException ignored) {
            }
        }
        return null;
    }

    private void reset() {
        mIn = null;
        mParser = null;
        mStringPool = null;
        mResourceMap = null;
        resetEventInfo();
        mNamespaces.reset();
    }

    private void resetEventInfo() {
        mEventType = START_DOCUMENT;
        mLineNumber = -1;
        mNamespaceIndex = -1;
        mNameIndex = -1;
        mIdIndex = -1;
        mClassIndex = -1;
        mStyleIndex = -1;
        mAttributes = null;
    }

    private boolean nextChunk() throws IOException {
        // Skip padding or unknown data at the end of current chunk.
        if (mParser.isChunk()) {
            int skipped = mParser.skipChunk();
            if (skipped > 0) {
                LOGGER.fine(String.format(
                    "Skipped unknown %d bytes at end of %s chunk.",
                    skipped, mParser.chunkName()));
            }
        }

        // Reset previous event data.
        int lastEventType = mEventType;
        if (lastEventType != -1) {
            resetEventInfo();
        }

        // Stop if all root-level namespaces were popped.
        if (lastEventType == END_TAG && mNamespaces.getDepth() == 0
                && mNamespaces.getCurrentCount() == 0) {
            return false;
        }

        // Parse next chunk.
        while (mParser.next()) {
            // Skip unknown or unsupported chunks.
            if (mParser.chunkType() == ResChunkHeader.RES_NULL_TYPE) {
                LOGGER.fine(String.format(
                    "Skipping unknown chunk (%s) of %d bytes at 0x%08x.",
                    mParser.chunkName(), mParser.chunkSize(), mParser.chunkStart()));
                mParser.skipChunk();
                continue;
            }

            // Return this chunk.
            LOGGER.fine(String.format(
                "Chunk at 0x%08x: %s (%d bytes)",
                mParser.chunkStart(), mParser.chunkName(), mParser.chunkSize()));
            return true;
        }

        // End of chunks.
        return false;
    }

    private int doNext() throws IOException {
        if (mEventType == END_DOCUMENT) {
            return END_DOCUMENT;
        }
        if (mEventType == END_TAG) {
            mNamespaces.decrementDepth();
        }

        while (nextChunk()) {
            switch (mParser.chunkType()) {
                case ResChunkHeader.RES_STRING_POOL_TYPE:
                    mStringPool = ResStringPool.parse(mParser);
                    continue;
                case ResChunkHeader.RES_XML_RESOURCE_MAP_TYPE:
                    skipUnreadHeader();

                    mResourceMap = new ResId[mParser.dataSize() / 4];
                    for (int i = 0; i < mResourceMap.length; i++) {
                        mResourceMap[i] = ResId.of(mIn.readInt());
                    }
                    continue;
            }

            if (mParser.chunkType() < ResChunkHeader.RES_XML_FIRST_CHUNK_TYPE
                    || mParser.chunkType() > ResChunkHeader.RES_XML_LAST_CHUNK_TYPE) {
                skipUnexpectedChunk();
                continue;
            }

            // ResXMLTree_node
            mLineNumber = mIn.readInt();
            mIn.skipInt(); // comment

            switch (mParser.chunkType()) {
                case ResChunkHeader.RES_XML_START_NAMESPACE_TYPE: {
                    // ResXMLTree_namespaceExt
                    int prefix = mIn.readInt();
                    int uri = mIn.readInt();

                    skipUnreadHeader();

                    mNamespaces.push(prefix, uri);
                    continue;
                }
                case ResChunkHeader.RES_XML_END_NAMESPACE_TYPE:
                    // ResXMLTree_namespaceExt
                    mIn.skipInt(); // prefix
                    mIn.skipInt(); // uri

                    skipUnreadHeader();

                    mNamespaces.pop();
                    continue;
                case ResChunkHeader.RES_XML_START_ELEMENT_TYPE: {
                    long startPosition = mIn.position();
                    // ResXMLTree_attrExt
                    mNamespaceIndex = mIn.readInt();
                    mNameIndex = mIn.readInt();
                    int attributeStart = mIn.readUnsignedShort();
                    int attributeSize = mIn.readUnsignedShort();
                    int attributeCount = mIn.readUnsignedShort();
                    mIdIndex = mIn.readUnsignedShort();
                    mClassIndex = mIn.readUnsignedShort();
                    mStyleIndex = mIn.readUnsignedShort();

                    skipUnreadHeader();

                    // Align the stream with the start of the attributes.
                    mIn.jumpTo(startPosition + attributeStart);

                    mAttributes = new Attribute[attributeCount];
                    for (int i = 0; i < attributeCount; i++) {
                        Attribute attr = Attribute.read(mIn);

                        if (attributeSize > Attribute.SIZE) {
                            int skipped = mIn.skipBytes(attributeSize - Attribute.SIZE);
                            LOGGER.fine(String.format(
                                "Skipped unknown %d bytes in attribute.", skipped));
                        }

                        // Check if the app preserved raw attribute values.
                        if (attr.valueType == ResValue.TYPE_STRING
                                ? attr.valueData != attr.rawValue : attr.rawValue >= 0) {
                            mHasRawValues = true;
                        }

                        mAttributes[i] = attr;
                    }

                    mNamespaces.incrementDepth();
                    return mEventType = START_TAG;
                }
                case ResChunkHeader.RES_XML_END_ELEMENT_TYPE:
                    // ResXMLTree_endElementExt
                    mNamespaceIndex = mIn.readInt();
                    mNameIndex = mIn.readInt();

                    skipUnreadHeader();
                    return mEventType = END_TAG;
                case ResChunkHeader.RES_XML_CDATA_TYPE:
                    // ResXMLTree_cdataExt
                    mNameIndex = mIn.readInt();
                    mIn.skipInt(); // size, res0, type
                    mIn.skipInt(); // data

                    skipUnreadHeader();
                    return mEventType = TEXT;
                default:
                    skipUnexpectedChunk();
                    continue;
            }
        }

        LOGGER.fine(String.format("End of chunks at 0x%08x", mIn.position()));

        if (mIn.available() > 0) {
            LOGGER.fine(String.format(
                "Ignoring trailing data at 0x%08x.", mIn.position()));
        }

        // Flag the app if it preserved raw attribute values.
        if (mHasRawValues && !mIgnoreRawValues) {
            mTable.getApkInfo().getResourcesInfo().setKeepRawValues(true);
        }

        return mEventType = END_DOCUMENT;
    }

    private void skipUnexpectedChunk() throws IOException {
        LOGGER.warning(String.format(
            "Skipping unexpected %s chunk of %d bytes at 0x%08x.",
            mParser.chunkName(), mParser.chunkSize(), mParser.chunkStart()));
        mParser.skipChunk();
    }

    private void skipUnreadHeader() throws IOException {
        // Some apps lie about the reported size of their chunk header.
        // Trusting the header size is misleading, so compare to what we actually read in the
        // header vs reported and skip the rest.
        int bytesRead = (int) (mIn.position() - mParser.chunkStart());
        readExceedingBytes("Chunk header", mParser.headerSize(), bytesRead);
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

    private static final class NamespaceStack {
        private static final int INITIAL_CAPACITY = 32;

        private int[] mData;
        private int mDataLength;
        private int mDepth;

        public NamespaceStack() {
            mData = new int[INITIAL_CAPACITY];
            reset();
        }

        public void reset() {
            mDataLength = 0;
            mDepth = -1;
            incrementDepth();
        }

        public int getDepth() {
            return mDepth;
        }

        public void incrementDepth() {
            ensureCapacity();
            int offset = mDataLength;
            mData[offset] = 0;
            mData[offset + 1] = 0;
            mDataLength += 2;
            mDepth++;
        }

        private void ensureCapacity() {
            if (mData.length - mDataLength >= 2) {
                return;
            }
            int[] newData = new int[mData.length + INITIAL_CAPACITY];
            System.arraycopy(mData, 0, newData, 0, mDataLength);
            mData = newData;
        }

        public void decrementDepth() {
            if (mDataLength == 0) {
                return;
            }
            int offset = mDataLength - 1;
            int count = mData[offset];
            mDataLength -= (2 + count * 2);
            mDepth--;
        }

        public int getCount(int depth) {
            if (mDataLength == 0 || depth <= 0) {
                return 0;
            }
            if (depth > mDepth) {
                depth = mDepth;
            }
            int total = 0;
            int offset = 0;
            for (; depth > 0; --depth) {
                int count = mData[offset];
                total += count;
                offset += (2 + count * 2);
            }
            return total;
        }

        public int getCurrentCount() {
            if (mDataLength == 0) {
                return 0;
            }
            int offset = mDataLength - 1;
            return mData[offset];
        }

        public void push(int prefix, int uri) {
            ensureCapacity();
            int offset = mDataLength - 1;
            int count = mData[offset];
            mData[offset - 1 - count * 2] = count + 1;
            mData[offset] = prefix;
            mData[offset + 1] = uri;
            mData[offset + 2] = count + 1;
            mDataLength += 2;
        }

        public boolean pop() {
            if (mDataLength == 0) {
                return false;
            }
            int offset = mDataLength - 1;
            int count = mData[offset];
            if (count == 0) {
                return false;
            }
            count--;
            offset -= 2;
            mData[offset] = count;
            offset -= (1 + count * 2);
            mData[offset] = count;
            mDataLength -= 2;
            return true;
        }

        public int getPrefix(int index) {
            return get(index, true);
        }

        public int getUri(int index) {
            return get(index, false);
        }

        private int get(int index, boolean isPrefix) {
            if (mDataLength == 0 || index < 0) {
                return -1;
            }
            int offset = 0;
            for (int i = mDepth; i >= 0; i--) {
                int count = mData[offset];
                if (index >= count) {
                    index -= count;
                    offset += (2 + count * 2);
                    continue;
                }
                offset += (1 + index * 2);
                if (!isPrefix) {
                    offset++;
                }
                return mData[offset];
            }
            return -1;
        }

        public int findPrefix(int uri) {
            return find(uri, true);
        }

        public int findUri(int prefix) {
            return find(prefix, false);
        }

        private int find(int prefixOrUri, boolean isPrefix) {
            if (mDataLength == 0) {
                return -1;
            }
            int offset = mDataLength - 1;
            for (int i = mDepth; i >= 0; i--) {
                int count = mData[offset];
                offset -= 2;
                for (; count > 0; --count) {
                    if (isPrefix) {
                        if (mData[offset + 1] == prefixOrUri) {
                            return mData[offset];
                        }
                    } else {
                        if (mData[offset] == prefixOrUri) {
                            return mData[offset + 1];
                        }
                    }
                    offset -= 2;
                }
            }
            return -1;
        }
    }

    private static final class Attribute {
        public static final int SIZE = 20;

        public final int ns;
        public final int name;
        public final int rawValue;
        public final int valueType;
        public final int valueData;

        public Attribute(int ns, int name, int rawValue, int valueType, int valueData) {
            this.ns = ns;
            this.name = name;
            this.rawValue = rawValue;
            this.valueType = valueType;
            this.valueData = valueData;
        }

        public static Attribute read(BinaryDataInputStream in) throws IOException {
            // ResXMLTree_attribute
            int ns = in.readInt();
            int name = in.readInt();
            int rawValue = in.readInt();
            // Res_value
            int valueSize = in.readUnsignedShort();
            if (valueSize < 8) {
                return null;
            }
            in.skipByte(); // res0
            int valueType = in.readUnsignedByte();
            int valueData = in.readInt();

            return new Attribute(ns, name, rawValue, valueType, valueData);
        }
    }
}
