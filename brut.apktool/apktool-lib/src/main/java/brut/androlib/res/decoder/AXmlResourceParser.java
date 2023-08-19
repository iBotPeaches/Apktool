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
import brut.androlib.exceptions.CantFindFrameworkResException;
import brut.androlib.exceptions.UndefinedResObjectException;
import brut.androlib.res.data.ResID;
import brut.androlib.res.data.ResResSpec;
import brut.androlib.res.data.ResTable;
import brut.androlib.res.data.arsc.ARSCHeader;
import brut.androlib.res.data.axml.NamespaceStack;
import brut.androlib.res.data.value.ResAttr;
import brut.androlib.res.data.value.ResScalarValue;
import brut.androlib.res.xml.ResXmlEncoders;
import brut.util.ExtCountingDataInput;
import com.google.common.io.LittleEndianDataInputStream;
import org.xmlpull.v1.XmlPullParserException;

import java.io.*;
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
public class AXmlResourceParser implements XmlResourceParser {

    public AXmlResourceParser(ResTable resTable) {
        mResTable = resTable;
        resetEventInfo();
    }

    public AndrolibException getFirstError() {
        return mFirstError;
    }

    public ResTable getResTable() {
        return mResTable;
    }

    public void open(InputStream stream) {
        close();
        if (stream != null) {
            mIn = new ExtCountingDataInput(new LittleEndianDataInputStream(stream));
        }
    }

    @Override
    public void close() {
        if (!isOperational) {
            return;
        }
        isOperational = false;
        mIn = null;
        mStringBlock = null;
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
        } catch (IOException e) {
            close();
            throw e;
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
        } else if (eventType == END_TAG) {
            return "";
        } else {
            throw new XmlPullParserException("Parser must be on START_TAG or TEXT to read text.", this, null);
        }
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
    public int getEventType(){
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
        return mStringBlock.getString(mNameIndex);
    }

    @Override
    public String getText() {
        if (mNameIndex == -1 || mEvent != TEXT) {
            return null;
        }
        return mStringBlock.getString(mNameIndex);
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
        return mStringBlock.getString(mNamespaceIndex);
    }

    @Override
    public String getPrefix() {
        int prefix = mNamespaces.findPrefix(mNamespaceIndex);
        return mStringBlock.getString(prefix);
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
        return mStringBlock.getString(prefix);
    }

    @Override
    public String getNamespaceUri(int pos) {
        int uri = mNamespaces.getUri(pos);
        return mStringBlock.getString(uri);
    }

    @Override
    public String getClassAttribute() {
        if (mClassIndex == -1) {
            return null;
        }
        int offset = getAttributeOffset(mClassIndex);
        int value = mAttributes[offset + ATTRIBUTE_IX_VALUE_STRING];
        return mStringBlock.getString(value);
    }

    @Override
    public String getIdAttribute() {
        if (mIdIndex == -1) {
            return null;
        }
        int offset = getAttributeOffset(mIdIndex);
        int value = mAttributes[offset + ATTRIBUTE_IX_VALUE_STRING];
        return mStringBlock.getString(value);
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
        ResID resId = new ResID(getAttributeNameResource(index));
        if (namespace == -1 && resId.pkgId == 1) {
            return ANDROID_RES_NS;
        }

        if (namespace == -1) {
            return "";
        }

        // Minifiers like removing the namespace, so we will default to default namespace
        // unless the pkgId of the resource is private. We will grab the non-standard one.
        String value = mStringBlock.getString(namespace);

        if (value == null || value.isEmpty()) {
            if (resId.pkgId == PRIVATE_PKG_ID) {
                return getNonDefaultNamespaceUri(offset);
            } else {
                return ANDROID_RES_NS;
            }
        }

        return value;
    }

    public String decodeFromResourceId(int attrResId) throws AndrolibException {
        if (attrResId != 0) {
            try {
                ResResSpec resResSpec = mResTable.getResSpec(attrResId);
                if (resResSpec != null) {
                    return resResSpec.getName();
                }
            } catch (UndefinedResObjectException | CantFindFrameworkResException ignored) {}
        }

        return null;
    }

    private String getNonDefaultNamespaceUri(int offset) {
        String prefix = mStringBlock.getString(mNamespaces.getPrefix(offset));
        if (prefix != null) {
            return  mStringBlock.getString(mNamespaces.getUri(offset));
        }

        // If we are here. There is some clever obfuscation going on. Our reference points to the namespace are gone.
        // Normally we could take the index * attributeCount to get an offset.
        // That would point to the URI in the StringBlock table, but that is empty.
        // We have the namespaces that can't be touched in the opening tag.
        // Though no known way to correlate them at this time.
        // So return the res-auto namespace.
        return ANDROID_RES_NS_AUTO;
    }

    @Override
    public String getAttributePrefix(int index) {
        int offset = getAttributeOffset(index);
        int uri = mAttributes[offset + ATTRIBUTE_IX_NAMESPACE_URI];
        int prefix = mNamespaces.findPrefix(uri);
        if (prefix == -1) {
            return "";
        }
        return mStringBlock.getString(prefix);
    }

    @Override
    public String getAttributeName(int index) {
        int offset = getAttributeOffset(index);
        int name = mAttributes[offset + ATTRIBUTE_IX_NAME];
        if (name == -1) {
            return "";
        }

        String resourceMapValue;
        String stringBlockValue = mStringBlock.getString(name);
        int resourceId = getAttributeNameResource(index);

        try {
            resourceMapValue = decodeFromResourceId(resourceId);
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

        // In this case we have a bogus resource. If it was not found in either.
        return "APKTOOL_MISSING_" + Integer.toHexString(resourceId);
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

    @Override
    public int getAttributeValueType(int index) {
        int offset = getAttributeOffset(index);
        return mAttributes[offset + ATTRIBUTE_IX_VALUE_TYPE];
    }

    @Override
    public int getAttributeValueData(int index) {
        int offset = getAttributeOffset(index);
        return mAttributes[offset + ATTRIBUTE_IX_VALUE_DATA];
    }

    @Override
    public String getAttributeValue(int index) {
        int offset = getAttributeOffset(index);
        int valueType = mAttributes[offset + ATTRIBUTE_IX_VALUE_TYPE];
        int valueData = mAttributes[offset + ATTRIBUTE_IX_VALUE_DATA];
        int valueRaw = mAttributes[offset + ATTRIBUTE_IX_VALUE_STRING];

        try {
            String stringBlockValue = valueRaw == -1 ? null : ResXmlEncoders.escapeXmlChars(mStringBlock.getString(valueRaw));
            String resourceMapValue = null;

            // Ensure we only track down obfuscated values for reference/attribute type values. Otherwise, we might
            // spam lookups against resource table for invalid ids.
            if (valueType == TypedValue.TYPE_REFERENCE || valueType == TypedValue.TYPE_DYNAMIC_REFERENCE ||
                valueType == TypedValue.TYPE_ATTRIBUTE || valueType == TypedValue.TYPE_DYNAMIC_ATTRIBUTE) {
                resourceMapValue = decodeFromResourceId(valueData);
            }
            String value = getPreferredString(stringBlockValue, resourceMapValue);

            // try to decode from resource table
            int attrResId = getAttributeNameResource(index);
            ResScalarValue resValue = mResTable.getCurrentResPackage()
                .getValueFactory().factory(valueType, valueData, value);

            String decoded = null;
            if (attrResId > 0) {
                try {
                    ResAttr attr = (ResAttr) mResTable.getResSpec(attrResId).getDefaultResource().getValue();

                    decoded = attr.convertToResXmlFormat(resValue);
                } catch (UndefinedResObjectException | ClassCastException ignored) {}
            }

            return decoded != null ? decoded : resValue.encodeAsResXmlAttr();

        } catch (AndrolibException ex) {
            setFirstError(ex);
            LOGGER.log(Level.WARNING, String.format("Could not decode attr value, using undecoded value "
                            + "instead: ns=%s, name=%s, value=0x%08x",
                    getAttributePrefix(index),
                    getAttributeName(index),
                    valueData), ex);
        }
        return TypedValue.coerceToString(valueType, valueData);
    }

    @Override
    public boolean getAttributeBooleanValue(int index, boolean defaultValue) {
        return getAttributeIntValue(index, defaultValue ? 1 : 0) != 0;
    }

    @Override
    public float getAttributeFloatValue(int index, float defaultValue) {
        int offset = getAttributeOffset(index);
        int valueType = mAttributes[offset + ATTRIBUTE_IX_VALUE_TYPE];
        if (valueType == TypedValue.TYPE_FLOAT) {
            int valueData = mAttributes[offset + ATTRIBUTE_IX_VALUE_DATA];
            return Float.intBitsToFloat(valueData);
        }
        return defaultValue;
    }

    @Override
    public int getAttributeIntValue(int index, int defaultValue) {
        int offset = getAttributeOffset(index);
        int valueType = mAttributes[offset + ATTRIBUTE_IX_VALUE_TYPE];
        if (valueType >= TypedValue.TYPE_FIRST_INT && valueType <= TypedValue.TYPE_LAST_INT) {
            return mAttributes[offset + ATTRIBUTE_IX_VALUE_DATA];
        }
        return defaultValue;
    }

    @Override
    public int getAttributeUnsignedIntValue(int index, int defaultValue) {
        return getAttributeIntValue(index, defaultValue);
    }

    @Override
    public int getAttributeResourceValue(int index, int defaultValue) {
        int offset = getAttributeOffset(index);
        int valueType = mAttributes[offset + ATTRIBUTE_IX_VALUE_TYPE];
        if (valueType == TypedValue.TYPE_REFERENCE) {
            return mAttributes[offset + ATTRIBUTE_IX_VALUE_DATA];
        }
        return defaultValue;
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
    public boolean getFeature(String feature) {
        return false;
    }

    @Override
    public void setFeature(String name, boolean value) throws XmlPullParserException {
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
        if (mStringBlock == null || attribute == null) {
            return -1;
        }
        int name = mStringBlock.find(attribute);
        if (name == -1) {
            return -1;
        }
        int uri = (namespace != null) ? mStringBlock.find(namespace) : -1;
        for (int o = 0; o != mAttributes.length; o += ATTRIBUTE_LENGTH) {
            if (name == mAttributes[o + ATTRIBUTE_IX_NAME]
                    && (uri == -1 || uri == mAttributes[o + ATTRIBUTE_IX_NAMESPACE_URI])) {
                return o / ATTRIBUTE_LENGTH;
            }
        }
        return -1;
    }

    private static String getPreferredString(String stringBlockValue, String resourceMapValue) {
        String value = stringBlockValue;

        if (stringBlockValue != null && resourceMapValue != null) {
            int slashPos = stringBlockValue.lastIndexOf("/");
            int colonPos = stringBlockValue.lastIndexOf(":");

            // Handle a value with a format of "@yyy/xxx", but avoid "@yyy/zzz:xxx"
            if (slashPos != -1) {
                if (colonPos == -1) {
                    String type = stringBlockValue.substring(0, slashPos);
                    value = type + "/" + resourceMapValue;
                }
            } else if (! stringBlockValue.equals(resourceMapValue)) {
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
        if (mStringBlock == null) {
            mIn.skipInt(); // XML Chunk AXML Type
            mIn.skipInt(); // Chunk Size

            mStringBlock = StringBlock.readWithChunk(mIn);
            mNamespaces.increaseDepth();
            isOperational = true;
        }

        if (mEvent == END_DOCUMENT) {
            return;
        }

        int event = mEvent;
        resetEventInfo();

        while (true) {
            if (m_decreaseDepth) {
                m_decreaseDepth = false;
                mNamespaces.decreaseDepth();
            }

            // Fake END_DOCUMENT event.
            if (event == END_TAG && mNamespaces.getDepth() == 1 && mNamespaces.getCurrentCount() == 0) {
                mEvent = END_DOCUMENT;
                break;
            }

            // #2070 - Some applications have 2 start namespaces, but only 1 end namespace.
            if (mIn.remaining() == 0) {
                LOGGER.warning(String.format("AXML hit unexpected end of file at byte: 0x%X", mIn.position()));
                mEvent = END_DOCUMENT;
                break;
            }

            int chunkType;
            int headerSize = 0;
            if (event == START_DOCUMENT) {
                // Fake event, see CHUNK_XML_START_TAG handler.
                chunkType = ARSCHeader.RES_XML_START_ELEMENT_TYPE;
            } else {
                chunkType = mIn.readShort();
                headerSize = mIn.readShort();
            }

            if (chunkType == ARSCHeader.RES_XML_RESOURCE_MAP_TYPE) {
                int chunkSize = mIn.readInt();
                if (chunkSize < 8 || (chunkSize % 4) != 0) {
                    throw new IOException("Invalid resource ids size (" + chunkSize + ").");
                }
                mResourceIds = mIn.readIntArray(chunkSize / 4 - 2);
                continue;
            }

            if (chunkType < ARSCHeader.RES_XML_FIRST_CHUNK_TYPE || chunkType > ARSCHeader.RES_XML_LAST_CHUNK_TYPE) {
                int chunkSize = mIn.readInt();
                mIn.skipBytes(chunkSize - 8);
                LOGGER.warning(String.format("Unknown chunk type at: (0x%08x) skipping...", mIn.position()));
                break;
            }

            // Fake START_DOCUMENT event.
            if (chunkType == ARSCHeader.RES_XML_START_ELEMENT_TYPE && event == -1) {
                mEvent = START_DOCUMENT;
                break;
            }

            // Read remainder of ResXMLTree_node
            mIn.skipInt(); // chunkSize
            mLineNumber = mIn.readInt();
            mIn.skipInt(); // Optional XML Comment

            if (chunkType == ARSCHeader.RES_XML_START_NAMESPACE_TYPE || chunkType == ARSCHeader.RES_XML_END_NAMESPACE_TYPE) {
                if (chunkType == ARSCHeader.RES_XML_START_NAMESPACE_TYPE) {
                    int prefix = mIn.readInt();
                    int uri = mIn.readInt();
                    mNamespaces.push(prefix, uri);
                } else {
                    mIn.skipInt(); // prefix
                    mIn.skipInt(); // uri
                    mNamespaces.pop();
                }

                // Check for larger header than we read. We know the current header is 0x10 bytes, but some apps
                // are packed with a larger header of unknown data.
                if (headerSize > 0x10) {
                    int bytesToSkip = headerSize - 0x10;
                    LOGGER.warning(String.format("AXML header larger than 0x10 bytes, skipping %d bytes.", bytesToSkip));
                    mIn.skipBytes(bytesToSkip);
                }
                continue;
            }


            if (chunkType == ARSCHeader.RES_XML_START_ELEMENT_TYPE) {
                mNamespaceIndex = mIn.readInt();
                mNameIndex = mIn.readInt();
                mIn.skipShort(); // attributeStart
                int attributeSize = mIn.readShort();
                int attributeCount = mIn.readShort();
                mIdIndex = mIn.readShort();
                mClassIndex = mIn.readShort();
                mStyleIndex = mIn.readShort();
                mAttributes = mIn.readIntArray(attributeCount * ATTRIBUTE_LENGTH);
                for (int i = ATTRIBUTE_IX_VALUE_TYPE; i < mAttributes.length; ) {
                    mAttributes[i] = (mAttributes[i] >>> 24);
                    i += ATTRIBUTE_LENGTH;
                }

                int byteAttrSizeRead = (attributeCount * ATTRIBUTE_LENGTH) * 4;
                int byteAttrSizeReported = (attributeSize * attributeCount);

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

            if (chunkType == ARSCHeader.RES_XML_END_ELEMENT_TYPE) {
                mNamespaceIndex = mIn.readInt();
                mNameIndex = mIn.readInt();
                mEvent = END_TAG;
                m_decreaseDepth = true;
                break;
            }

            if (chunkType == ARSCHeader.RES_XML_CDATA_TYPE) {
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

    private ExtCountingDataInput mIn;
    private final ResTable mResTable;
    private AndrolibException mFirstError;

    private boolean isOperational = false;
    private StringBlock mStringBlock;
    private int[] mResourceIds;
    private final NamespaceStack mNamespaces = new NamespaceStack();
    private boolean m_decreaseDepth;

    // All values are essentially indices, e.g. mNameIndex is an index of name in mStringBlock.
    private int mEvent;
    private int mLineNumber;
    private int mNameIndex;
    private int mNamespaceIndex;
    private int[] mAttributes;
    private int mIdIndex;
    private int mClassIndex;
    private int mStyleIndex;

    private final static Logger LOGGER = Logger.getLogger(AXmlResourceParser.class.getName());
    private static final String E_NOT_SUPPORTED = "Method is not supported.";

    // ResXMLTree_attribute
    private static final int ATTRIBUTE_IX_NAMESPACE_URI = 0; // ns
    private static final int ATTRIBUTE_IX_NAME = 1; // name
    private static final int ATTRIBUTE_IX_VALUE_STRING = 2; // rawValue
    private static final int ATTRIBUTE_IX_VALUE_TYPE = 3; // (size/res0/dataType)
    private static final int ATTRIBUTE_IX_VALUE_DATA = 4; // data
    private static final int ATTRIBUTE_LENGTH = 5;

    private static final int PRIVATE_PKG_ID = 0x7F;

    private static final String ANDROID_RES_NS_AUTO = "http://schemas.android.com/apk/res-auto";
    private static final String ANDROID_RES_NS = "http://schemas.android.com/apk/res/android";
}
