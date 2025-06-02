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

import android.content.res.XmlResourceParser;
import android.util.TypedValue;
import brut.androlib.exceptions.AndrolibException;
import brut.androlib.exceptions.FrameworkNotFoundException;
import brut.androlib.exceptions.UndefinedResObjectException;
import brut.androlib.res.decoder.data.*;
import brut.androlib.res.table.*;
import brut.androlib.res.table.value.*;
import brut.androlib.res.xml.ResXmlEncodable;
import brut.androlib.res.xml.ResXmlEncoders;
import brut.util.ExtDataInputStream;
import org.xmlpull.v1.XmlPullParserException;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Binary xml files parser.
 *
 * <p>Parser has only two states: (1) Operational state, which parser
 * obtains after first successful call to next() and retains until
 * open(), close(), or failed call to next(). (2) Closed state, which
 * parser obtains after open(), close(), or failed call to next(). In
 * this state methods return invalid values or throw exceptions.
 */
public class BinaryXmlResourceParser implements XmlResourceParser {
    private static final Logger LOGGER = Logger.getLogger(BinaryXmlResourceParser.class.getName());

    public static final String ANDROID_RES_NS = "http://schemas.android.com/apk/res/android";
    private static final String ANDROID_RES_NS_AUTO = "http://schemas.android.com/apk/res-auto";
    private static final String E_NOT_SUPPORTED = "Method is not supported.";

    // ResXMLTree_attribute
    private static final int ATTRIBUTE_IX_NAMESPACE_URI = 0; // ns
    private static final int ATTRIBUTE_IX_NAME = 1; // name
    private static final int ATTRIBUTE_IX_VALUE_STRING = 2; // rawValue
    private static final int ATTRIBUTE_IX_VALUE_TYPE = 3; // (size/res0/dataType)
    private static final int ATTRIBUTE_IX_VALUE_DATA = 4; // data
    private static final int ATTRIBUTE_LENGTH = 5;

    private static final int PRIVATE_PKG_ID = 0x7F;

    private final ResTable mTable;
    private final NamespaceStack mNamespaces;

    private boolean mIsOperational;
    private boolean mHasEncounteredStartElement;
    private ExtDataInputStream mIn;
    private ResStringPool mStringPool;
    private int[] mResourceIds;
    private boolean mDecreaseDepth;
    private AndrolibException mFirstError;

    // All values are essentially indices, e.g. mNameIndex is an index of name in mStringPool.
    private int mEvent;
    private int mLineNumber;
    private int mNameIndex;
    private int mNamespaceIndex;
    private int[] mAttributes;
    private int mIdIndex;
    private int mClassIndex;
    private int mStyleIndex;

    public BinaryXmlResourceParser(ResTable table) {
        mTable = table;
        mNamespaces = new NamespaceStack();
        resetEventInfo();
    }

    public ResTable getTable() {
        return mTable;
    }

    public AndrolibException getFirstError() {
        return mFirstError;
    }

    public void open(InputStream stream) {
        close();
        if (stream != null) {
            mIn = ExtDataInputStream.littleEndian(stream);
        }
    }

    @Override
    public void close() {
        if (!mIsOperational) {
            return;
        }
        mIsOperational = false;
        mHasEncounteredStartElement = false;
        mIn = null;
        mStringPool = null;
        mResourceIds = null;
        mNamespaces.reset();
        resetEventInfo();
    }

    @Override
    public int next() throws XmlPullParserException, IOException {
        if (mIn == null) {
            throw new XmlPullParserException("Parser is not opened.", this, null);
        }
        try {
            doNext();
            return mEvent;
        } catch (IOException ex) {
            close();
            throw ex;
        }
    }

    @Override
    public int nextToken() throws XmlPullParserException, IOException {
        return next();
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

    @Override
    public String nextText() throws XmlPullParserException, IOException {
        if (getEventType() != START_TAG) {
            throw new XmlPullParserException("Parser must be on START_TAG to read next text.", this, null);
        }
        int eventType = next();
        if (eventType == TEXT) {
            String result = getText();
            eventType = next();
            if (eventType != END_TAG) {
                throw new XmlPullParserException("Event TEXT must be immediately followed by END_TAG.", this, null);
            }
            return result;
        }
        if (eventType == END_TAG) {
            return "";
        }
        throw new XmlPullParserException("Parser must be on START_TAG or TEXT to read text.", this, null);
    }

    @Override
    public void require(int type, String namespace, String name) throws XmlPullParserException {
        if (type != getEventType() || (namespace != null && !namespace.equals(getNamespace()))
                || (name != null && !name.equals(getName()))) {
            throw new XmlPullParserException(TYPES[type] + " is expected.", this, null);
        }
    }

    @Override
    public int getDepth() {
        return mNamespaces.getDepth() - 1;
    }

    @Override
    public int getEventType() {
        return mEvent;
    }

    @Override
    public int getLineNumber() {
        return mLineNumber;
    }

    @Override
    public String getName() {
        if (mNameIndex == -1 || (mEvent != START_TAG && mEvent != END_TAG)) {
            return null;
        }
        return mStringPool.getString(mNameIndex);
    }

    @Override
    public String getText() {
        if (mNameIndex == -1 || mEvent != TEXT) {
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
        holderForStartAndLength[0] = 0;
        holderForStartAndLength[1] = text.length();
        char[] chars = new char[text.length()];
        text.getChars(0, text.length(), chars, 0);
        return chars;
    }

    @Override
    public String getNamespace() {
        return mStringPool.getString(mNamespaceIndex);
    }

    @Override
    public String getPrefix() {
        int prefix = mNamespaces.findPrefix(mNamespaceIndex);
        return mStringPool.getString(prefix);
    }

    @Override
    public String getPositionDescription() {
        return "XML line #" + getLineNumber();
    }

    @Override
    public int getNamespaceCount(int depth) {
        return mNamespaces.getAccumulatedCount(depth);
    }

    @Override
    public String getNamespacePrefix(int pos) {
        int prefix = mNamespaces.getPrefix(pos);
        return mStringPool.getString(prefix);
    }

    @Override
    public String getNamespaceUri(int pos) {
        int uri = mNamespaces.getUri(pos);
        return mStringPool.getString(uri);
    }

    @Override
    public String getClassAttribute() {
        if (mClassIndex == -1) {
            return null;
        }
        int offset = getAttributeOffset(mClassIndex);
        int value = mAttributes[offset + ATTRIBUTE_IX_VALUE_STRING];
        return mStringPool.getString(value);
    }

    @Override
    public String getIdAttribute() {
        if (mIdIndex == -1) {
            return null;
        }
        int offset = getAttributeOffset(mIdIndex);
        int value = mAttributes[offset + ATTRIBUTE_IX_VALUE_STRING];
        return mStringPool.getString(value);
    }

    @Override
    public int getIdAttributeResourceValue(int defaultValue) {
        if (mIdIndex == -1) {
            return defaultValue;
        }
        int offset = getAttributeOffset(mIdIndex);
        int valueType = mAttributes[offset + ATTRIBUTE_IX_VALUE_TYPE];
        if (valueType != TypedValue.TYPE_REFERENCE) {
            return defaultValue;
        }
        return mAttributes[offset + ATTRIBUTE_IX_VALUE_DATA];
    }

    @Override
    public int getStyleAttribute() {
        if (mStyleIndex == -1) {
            return 0;
        }
        int offset = getAttributeOffset(mStyleIndex);
        return mAttributes[offset + ATTRIBUTE_IX_VALUE_DATA];
    }

    @Override
    public int getAttributeCount() {
        if (mEvent != START_TAG) {
            return -1;
        }
        return mAttributes.length / ATTRIBUTE_LENGTH;
    }

    @Override
    public String getAttributeNamespace(int index) {
        int offset = getAttributeOffset(index);
        int namespace = mAttributes[offset + ATTRIBUTE_IX_NAMESPACE_URI];

        // #2972 - If the namespace index is -1, the attribute is not present, but if the attribute is from system
        // we can resolve it to the default namespace. This may prove to be too aggressive as we scope the entire
        // system namespace, but it is better than not resolving it at all.
        ResId id = ResId.of(getAttributeNameResource(index));
        int pkgId = id.getPackageId();
        if (namespace == -1 && pkgId == 1) {
            return ANDROID_RES_NS;
        }
        if (namespace == -1) {
            return "";
        }

        // Minifiers like removing the namespace, so we will default to default namespace
        // unless the pkgId of the resource is private. We will grab the non-standard one.
        String value = mStringPool.getString(namespace);
        if (value != null && !value.isEmpty()) {
            return value;
        }
        if (pkgId == PRIVATE_PKG_ID) {
            return getNonDefaultNamespaceUri(offset);
        }
        return ANDROID_RES_NS;
    }

    private String getNonDefaultNamespaceUri(int offset) {
        String prefix = mStringPool.getString(mNamespaces.getPrefix(offset));
        if (prefix == null) {
            // If we are here. There is some clever obfuscation going on. Our reference points to the namespace are gone.
            // Normally we could take the index * attributeCount to get an offset.
            // That would point to the URI in the ResStringPool table, but that is empty.
            // We have the namespaces that can't be touched in the opening tag.
            // Though no known way to correlate them at this time.
            // So return the res-auto namespace.
            return ANDROID_RES_NS_AUTO;
        }
        return mStringPool.getString(mNamespaces.getUri(offset));
    }

    @Override
    public String getAttributePrefix(int index) {
        int offset = getAttributeOffset(index);
        int uri = mAttributes[offset + ATTRIBUTE_IX_NAMESPACE_URI];
        int prefix = mNamespaces.findPrefix(uri);
        if (prefix == -1) {
            return "";
        }
        return mStringPool.getString(prefix);
    }

    @Override
    public String getAttributeName(int index) {
        int offset = getAttributeOffset(index);
        int name = mAttributes[offset + ATTRIBUTE_IX_NAME];
        if (name == -1) {
            return "";
        }

        String resourceMapValue;
        String stringBlockValue = mStringPool.getString(name);
        int nameId = getAttributeNameResource(index);

        try {
            resourceMapValue = decodeFromResourceId(nameId);
        } catch (AndrolibException ignored) {
            resourceMapValue = null;
        }

        // Android prefers the resource map value over what the String block has.
        // This can be seen quite often in obfuscated apps where values such as:
        // <item android:state_enabled="true" app:state_collapsed="false" app:state_collapsible="true">
        // Are improperly decoded when trusting the String block.
        // Leveraging the resource map allows us to get the proper value.
        // <item android:state_enabled="true" app:d2="false" app:d3="true">
        if (resourceMapValue != null) {
            return resourceMapValue;
        }

        if (stringBlockValue != null) {
            return stringBlockValue;
        }

        // If it was not found in either, then we have a bogus resource.
        return ResEntrySpec.MISSING_PREFIX + String.format("0x%08x", nameId);
    }

    private String decodeFromResourceId(int resId) throws AndrolibException {
        if (resId != 0) {
            try {
                return mTable.getEntrySpec(ResId.of(resId)).getName();
            } catch (UndefinedResObjectException | FrameworkNotFoundException ignored) {
            }
        }
        return null;
    }

    @Override
    public int getAttributeNameResource(int index) {
        int offset = getAttributeOffset(index);
        int name = mAttributes[offset + ATTRIBUTE_IX_NAME];
        if (mResourceIds == null || name < 0 || name >= mResourceIds.length) {
            return 0;
        }
        return mResourceIds[name];
    }

    // Non-standard API
    protected int getAttributeValueType(int index) {
        int offset = getAttributeOffset(index);
        return mAttributes[offset + ATTRIBUTE_IX_VALUE_TYPE];
    }

    @Override
    public String getAttributeValue(int index) {
        int offset = getAttributeOffset(index);
        int valueType = mAttributes[offset + ATTRIBUTE_IX_VALUE_TYPE];
        int valueData = mAttributes[offset + ATTRIBUTE_IX_VALUE_DATA];
        int valueRaw = mAttributes[offset + ATTRIBUTE_IX_VALUE_STRING];

        try {
            String stringBlockValue = valueRaw != -1
                ? ResXmlEncoders.escapeXmlChars(mStringPool.getString(valueRaw)) : null;
            String resourceMapValue = null;

            // Ensure we only track down obfuscated values for reference/attribute type values. Otherwise, we might
            // spam lookups against resource table for invalid IDs.
            if (valueType == TypedValue.TYPE_REFERENCE
                    || valueType == TypedValue.TYPE_DYNAMIC_REFERENCE
                    || valueType == TypedValue.TYPE_ATTRIBUTE
                    || valueType == TypedValue.TYPE_DYNAMIC_ATTRIBUTE) {
                resourceMapValue = decodeFromResourceId(valueData);
            }

            // Try to decode from resource table.
            int nameId = getAttributeNameResource(index);
            ResItem value = ResItem.parse(mTable.getCurrentPackage(), valueType, valueData,
                getPreferredString(stringBlockValue, resourceMapValue));

            String decoded = null;
            if (nameId != 0 && value instanceof ResPrimitive) {
                try {
                    // We need the attribute entry's value to format this value.
                    ResId id = ResId.of(nameId);
                    ResEntrySpec nameSpec = mTable.getEntrySpec(id);
                    ResValue nameDefValue = mTable.getDefaultEntry(id).getValue();

                    if (nameDefValue instanceof ResAttribute) {
                        decoded = ((ResAttribute) nameDefValue).convertToResXmlFormat(value);
                    } else {
                        LOGGER.warning("Unexpected attribute name: " + nameSpec);
                    }
                } catch (UndefinedResObjectException ignored) {
                }
            }
            if (decoded == null && value instanceof ResXmlEncodable) {
                decoded = ((ResXmlEncodable) value).encodeAsResXmlAttrValue();
            }
            if (decoded != null) {
                return decoded;
            }
        } catch (AndrolibException ex) {
            setFirstError(ex);
        }
        LOGGER.warning(String.format(
            "Could not decode attr value: ns=%s, name=%s, value=0x%08x",
            getAttributePrefix(index), getAttributeName(index), valueData));
        return ResXmlEncoders.coerceToString(valueType, valueData);
    }

    @Override
    public boolean getAttributeBooleanValue(int index, boolean defaultValue) {
        return getAttributeIntValue(index, defaultValue ? 1 : 0) != 0;
    }

    @Override
    public float getAttributeFloatValue(int index, float defaultValue) {
        int offset = getAttributeOffset(index);
        int valueType = mAttributes[offset + ATTRIBUTE_IX_VALUE_TYPE];
        if (valueType != TypedValue.TYPE_FLOAT) {
            return defaultValue;
        }
        return Float.intBitsToFloat(mAttributes[offset + ATTRIBUTE_IX_VALUE_DATA]);
    }

    @Override
    public int getAttributeIntValue(int index, int defaultValue) {
        int offset = getAttributeOffset(index);
        int valueType = mAttributes[offset + ATTRIBUTE_IX_VALUE_TYPE];
        if (valueType < TypedValue.TYPE_FIRST_INT || valueType > TypedValue.TYPE_LAST_INT) {
            return defaultValue;
        }
        return mAttributes[offset + ATTRIBUTE_IX_VALUE_DATA];
    }

    @Override
    public int getAttributeUnsignedIntValue(int index, int defaultValue) {
        return getAttributeIntValue(index, defaultValue);
    }

    @Override
    public int getAttributeResourceValue(int index, int defaultValue) {
        int offset = getAttributeOffset(index);
        int valueType = mAttributes[offset + ATTRIBUTE_IX_VALUE_TYPE];
        if (valueType != TypedValue.TYPE_REFERENCE) {
            return defaultValue;
        }
        return mAttributes[offset + ATTRIBUTE_IX_VALUE_DATA];
    }

    @Override
    public String getAttributeValue(String namespace, String attribute) {
        int index = findAttribute(namespace, attribute);
        if (index == -1) {
            return "";
        }
        return getAttributeValue(index);
    }

    @Override
    public boolean getAttributeBooleanValue(String namespace, String attribute, boolean defaultValue) {
        int index = findAttribute(namespace, attribute);
        if (index == -1) {
            return defaultValue;
        }
        return getAttributeBooleanValue(index, defaultValue);
    }

    @Override
    public float getAttributeFloatValue(String namespace, String attribute, float defaultValue) {
        int index = findAttribute(namespace, attribute);
        if (index == -1) {
            return defaultValue;
        }
        return getAttributeFloatValue(index, defaultValue);
    }

    @Override
    public int getAttributeIntValue(String namespace, String attribute, int defaultValue) {
        int index = findAttribute(namespace, attribute);
        if (index == -1) {
            return defaultValue;
        }
        return getAttributeIntValue(index, defaultValue);
    }

    @Override
    public int getAttributeUnsignedIntValue(String namespace, String attribute, int defaultValue) {
        int index = findAttribute(namespace, attribute);
        if (index == -1) {
            return defaultValue;
        }
        return getAttributeUnsignedIntValue(index, defaultValue);
    }

    @Override
    public int getAttributeResourceValue(String namespace, String attribute, int defaultValue) {
        int index = findAttribute(namespace, attribute);
        if (index == -1) {
            return defaultValue;
        }
        return getAttributeResourceValue(index, defaultValue);
    }

    @Override
    public int getAttributeListValue(int index, String[] options, int defaultValue) {
        return 0;
    }

    @Override
    public int getAttributeListValue(String namespace, String attribute, String[] options, int defaultValue) {
        return 0;
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
    public void setInput(InputStream stream, String inputEncoding) {
        open(stream);
    }

    @Override
    public void setInput(Reader reader) throws XmlPullParserException {
        throw new XmlPullParserException(E_NOT_SUPPORTED);
    }

    @Override
    public String getInputEncoding() {
        return null;
    }

    @Override
    public int getColumnNumber() {
        return -1;
    }

    @Override
    public boolean isEmptyElementTag() {
        return false;
    }

    @Override
    public boolean isWhitespace() {
        return false;
    }

    @Override
    public void defineEntityReplacementText(String entityName, String replacementText)
            throws XmlPullParserException {
        throw new XmlPullParserException(E_NOT_SUPPORTED);
    }

    @Override
    public String getNamespace(String prefix) {
        throw new RuntimeException(E_NOT_SUPPORTED);
    }

    @Override
    public Object getProperty(String name) {
        return null;
    }

    @Override
    public void setProperty(String name, Object value) throws XmlPullParserException {
        throw new XmlPullParserException(E_NOT_SUPPORTED);
    }

    @Override
    public boolean getFeature(String name) {
        return false;
    }

    @Override
    public void setFeature(String name, boolean state) throws XmlPullParserException {
        throw new XmlPullParserException(E_NOT_SUPPORTED);
    }

    private int getAttributeOffset(int index) {
        if (mEvent != START_TAG) {
            throw new IndexOutOfBoundsException("Current event is not START_TAG.");
        }
        int offset = index * ATTRIBUTE_LENGTH;
        if (offset >= mAttributes.length) {
            throw new IndexOutOfBoundsException("Invalid attribute index (" + index + ").");
        }
        return offset;
    }

    private int findAttribute(String namespace, String attribute) {
        if (mStringPool == null || attribute == null) {
            return -1;
        }
        int name = mStringPool.find(attribute);
        if (name == -1) {
            return -1;
        }
        int uri = namespace != null ? mStringPool.find(namespace) : -1;
        int offset = 0;
        while (offset < mAttributes.length) {
            if (name == mAttributes[offset + ATTRIBUTE_IX_NAME]
                    && (uri == -1 || uri == mAttributes[offset + ATTRIBUTE_IX_NAMESPACE_URI])) {
                return offset / ATTRIBUTE_LENGTH;
            }
            offset += ATTRIBUTE_LENGTH;
        }
        return -1;
    }

    private static String getPreferredString(String stringBlockValue, String resourceMapValue) {
        String value = stringBlockValue;

        if (stringBlockValue != null && resourceMapValue != null) {
            int slashPos = stringBlockValue.lastIndexOf('/');
            int colonPos = stringBlockValue.lastIndexOf(':');

            // Handle a value with a format of "@yyy/xxx", but avoid "@yyy/zzz:xxx"
            if (slashPos != -1) {
                if (colonPos == -1) {
                    String type = stringBlockValue.substring(0, slashPos);
                    value = type + "/" + resourceMapValue;
                }
            } else if (!stringBlockValue.equals(resourceMapValue)) {
                value = resourceMapValue;
            }
        }
        return value;
    }

    private void resetEventInfo() {
        mEvent = -1;
        mLineNumber = -1;
        mNameIndex = -1;
        mNamespaceIndex = -1;
        mAttributes = null;
        mIdIndex = -1;
        mClassIndex = -1;
        mStyleIndex = -1;
    }

    private void doNext() throws IOException {
        if (mStringPool == null) {
            mIn.skipInt(); // XML Chunk AXML Type
            mIn.skipInt(); // Chunk Size

            mStringPool = ResStringPool.read(mIn);
            mNamespaces.increaseDepth();
            mIsOperational = true;
        }

        if (mEvent == END_DOCUMENT) {
            return;
        }

        int event = mEvent;
        resetEventInfo();

        for (;;) {
            if (mDecreaseDepth) {
                mDecreaseDepth = false;
                mNamespaces.decreaseDepth();
            }

            // Fake END_DOCUMENT event.
            if (event == END_TAG && mNamespaces.getDepth() == 1 && mNamespaces.getCurrentCount() == 0) {
                mEvent = END_DOCUMENT;
                break;
            }

            // #2070 - Some apps have 2 start namespaces, but only 1 end namespace.
            if (mIn.available() == 0) {
                LOGGER.warning(String.format("AXML hit unexpected end of file at byte: 0x%X", mIn.position()));
                mEvent = END_DOCUMENT;
                break;
            }

            long chunkStart = mIn.position();
            int chunkType;
            int headerSize = 0;
            if (event == START_DOCUMENT) {
                // Fake event, see CHUNK_XML_START_TAG handler.
                chunkType = ResChunkHeader.RES_XML_START_ELEMENT_TYPE;
            } else {
                chunkType = mIn.readUnsignedShort();
                headerSize = mIn.readUnsignedShort();
            }

            if (chunkType == ResChunkHeader.RES_XML_RESOURCE_MAP_TYPE) {
                int chunkSize = mIn.readInt();
                if (chunkSize < 8 || (chunkSize % 4) != 0) {
                    throw new IOException("Invalid resource ids size (" + chunkSize + ").");
                }
                mResourceIds = mIn.readIntArray(chunkSize / 4 - 2);
                continue;
            }

            ResChunkHeader chunkHeader;
            if (chunkType < ResChunkHeader.RES_XML_FIRST_CHUNK_TYPE || chunkType > ResChunkHeader.RES_XML_LAST_CHUNK_TYPE) {
                chunkHeader = new ResChunkHeader(chunkType, headerSize, mIn.readInt(), chunkStart);
                mIn.jumpTo(chunkHeader.endPos);
                LOGGER.warning(String.format("Chunk type %s is not supported, skipping...", chunkHeader.type));
                break;
            }

            // Fake START_DOCUMENT event.
            if (chunkType == ResChunkHeader.RES_XML_START_ELEMENT_TYPE && event == -1) {
                mEvent = START_DOCUMENT;
                break;
            }

            // Read remainder of ResXMLTree_node
            chunkHeader = new ResChunkHeader(chunkType, headerSize, mIn.readInt(), chunkStart);
            mLineNumber = mIn.readInt();
            mIn.skipInt(); // Optional XML Comment

            if (chunkType == ResChunkHeader.RES_XML_START_NAMESPACE_TYPE || chunkType == ResChunkHeader.RES_XML_END_NAMESPACE_TYPE) {
                if (chunkType == ResChunkHeader.RES_XML_START_NAMESPACE_TYPE) {
                    int prefix = mIn.readInt();
                    int uri = mIn.readInt();
                    mNamespaces.push(prefix, uri);
                } else {
                    // #3838 - Some apps have a bogus element prior to the START_ELEMENT event. This breaks parsing &
                    // until we have a robust chunk parser to handle this, this skip will suffice for now.
                    if (!mHasEncounteredStartElement) {
                        LOGGER.warning(String.format(
                            "Skipping end namespace event at %d, element has not been encountered.", chunkHeader.endPos));
                        mIn.jumpTo(chunkHeader.endPos);
                        break;
                    }

                    mIn.skipInt(); // prefix
                    mIn.skipInt(); // uri
                    mNamespaces.pop();
                }

                // Check for larger header than we read. We know the current header is 0x10 bytes, but some apps
                // are packed with a larger header of unknown data.
                if (headerSize > 0x10) {
                    int bytesToSkip = headerSize - 0x10;
                    LOGGER.warning(String.format(
                        "AXML START/END namespace header larger than 0x10 bytes, skipping %d bytes.", bytesToSkip));
                    mIn.skipBytes(bytesToSkip);
                }
                continue;
            }

            if (chunkType == ResChunkHeader.RES_XML_START_ELEMENT_TYPE) {
                mHasEncounteredStartElement = true;
                mNamespaceIndex = mIn.readInt();
                mNameIndex = mIn.readInt();
                mIn.skipShort(); // attributeStart
                int attributeSize = mIn.readUnsignedShort();
                int attributeCount = mIn.readUnsignedShort();
                mIdIndex = mIn.readUnsignedShort();
                mClassIndex = mIn.readUnsignedShort();
                mStyleIndex = mIn.readUnsignedShort();
                mAttributes = mIn.readIntArray(attributeCount * ATTRIBUTE_LENGTH);
                for (int i = ATTRIBUTE_IX_VALUE_TYPE; i < mAttributes.length; ) {
                    mAttributes[i] >>>= 24;
                    i += ATTRIBUTE_LENGTH;
                }

                int byteAttrSizeRead = attributeCount * ATTRIBUTE_LENGTH * 4;
                int byteAttrSizeReported = attributeSize * attributeCount;

                // Check for misleading chunk sizes
                if (byteAttrSizeRead < byteAttrSizeReported) {
                    int bytesToSkip = byteAttrSizeReported - byteAttrSizeRead;
                    mIn.skipBytes(bytesToSkip);
                    LOGGER.fine("Skipping " + bytesToSkip + " unknown bytes in attributes area.");
                }

                mNamespaces.increaseDepth();
                mEvent = START_TAG;
                break;
            }

            if (chunkType == ResChunkHeader.RES_XML_END_ELEMENT_TYPE) {
                mNamespaceIndex = mIn.readInt();
                mNameIndex = mIn.readInt();
                mEvent = END_TAG;
                mDecreaseDepth = true;
                break;
            }

            if (chunkType == ResChunkHeader.RES_XML_CDATA_TYPE) {
                mNameIndex = mIn.readInt();
                mIn.skipInt();
                mIn.skipInt();
                mEvent = TEXT;
                break;
            }
        }
    }

    private void setFirstError(AndrolibException error) {
        if (mFirstError == null) {
            mFirstError = error;
        }
    }
}
