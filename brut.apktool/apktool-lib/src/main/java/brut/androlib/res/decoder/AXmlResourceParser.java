/*
 *  Copyright (C) 2010 Ryszard Wiśniewski <brut.alll@gmail.com>
 *  Copyright (C) 2010 Connor Tumbleson <connor.tumbleson@gmail.com>
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

import android.content.res.XmlResourceParser;
import android.util.TypedValue;
import brut.androlib.AndrolibException;
import brut.androlib.res.data.ResID;
import brut.androlib.res.xml.ResXmlEncoders;
import brut.util.ExtDataInput;
import com.google.common.io.LittleEndianDataInputStream;
import java.io.DataInput;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.xmlpull.v1.XmlPullParserException;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 * @author Dmitry Skiba
 *
 *         Binary xml files parser.
 *
 *         Parser has only two states: (1) Operational state, which parser
 *         obtains after first successful call to next() and retains until
 *         open(), close(), or failed call to next(). (2) Closed state, which
 *         parser obtains after open(), close(), or failed call to next(). In
 *         this state methods return invalid values or throw exceptions.
 *
 *         TODO: * check all methods in closed state
 *
 */
public class AXmlResourceParser implements XmlResourceParser {

    public AXmlResourceParser() {
        resetEventInfo();
    }

    public AXmlResourceParser(InputStream stream) {
        this();
        open(stream);
    }

    public AndrolibException getFirstError() {
        return mFirstError;
    }

    public ResAttrDecoder getAttrDecoder() {
        return mAttrDecoder;
    }

    public void setAttrDecoder(ResAttrDecoder attrDecoder) {
        mAttrDecoder = attrDecoder;
    }

    public void open(InputStream stream) {
        close();
        if (stream != null) {
            // We need to explicitly cast to DataInput as otherwise the constructor is ambiguous.
            // We choose DataInput instead of InputStream as ExtDataInput wraps an InputStream in
            // a DataInputStream which is big-endian and ignores the little-endian behavior.
            m_reader = new ExtDataInput((DataInput) new LittleEndianDataInputStream(stream));
        }
    }

    @Override
    public void close() {
        if (!m_operational) {
            return;
        }
        m_operational = false;
        m_reader = null;
        m_strings = null;
        m_resourceIDs = null;
        m_namespaces.reset();
        resetEventInfo();
    }

    // ///////////////////////////////// iteration
    @Override
    public int next() throws XmlPullParserException, IOException {
        if (m_reader == null) {
            throw new XmlPullParserException("Parser is not opened.", this, null);
        }
        try {
            doNext();
            return m_event;
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
    public void require(int type, String namespace, String name)
            throws XmlPullParserException, IOException {
        if (type != getEventType() || (namespace != null && !namespace.equals(getNamespace()))
                || (name != null && !name.equals(getName()))) {
            throw new XmlPullParserException(TYPES[type] + " is expected.", this, null);
        }
    }

    @Override
    public int getDepth() {
        return m_namespaces.getDepth() - 1;
    }

    @Override
    public int getEventType() throws XmlPullParserException {
        return m_event;
    }

    @Override
    public int getLineNumber() {
        return m_lineNumber;
    }

    @Override
    public String getName() {
        if (m_name == -1 || (m_event != START_TAG && m_event != END_TAG)) {
            return null;
        }
        return m_strings.getString(m_name);
    }

    @Override
    public String getText() {
        if (m_name == -1 || m_event != TEXT) {
            return null;
        }
        return m_strings.getString(m_name);
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
        return m_strings.getString(m_namespaceUri);
    }

    @Override
    public String getPrefix() {
        int prefix = m_namespaces.findPrefix(m_namespaceUri);
        return m_strings.getString(prefix);
    }

    @Override
    public String getPositionDescription() {
        return "XML line #" + getLineNumber();
    }

    @Override
    public int getNamespaceCount(int depth) throws XmlPullParserException {
        return m_namespaces.getAccumulatedCount(depth);
    }

    @Override
    public String getNamespacePrefix(int pos) throws XmlPullParserException {
        int prefix = m_namespaces.getPrefix(pos);
        return m_strings.getString(prefix);
    }

    @Override
    public String getNamespaceUri(int pos) throws XmlPullParserException {
        int uri = m_namespaces.getUri(pos);
        return m_strings.getString(uri);
    }

    // ///////////////////////////////// attributes
    @Override
    public String getClassAttribute() {
        if (m_classAttribute == -1) {
            return null;
        }
        int offset = getAttributeOffset(m_classAttribute);
        int value = m_attributes[offset + ATTRIBUTE_IX_VALUE_STRING];
        return m_strings.getString(value);
    }

    @Override
    public String getIdAttribute() {
        if (m_idAttribute == -1) {
            return null;
        }
        int offset = getAttributeOffset(m_idAttribute);
        int value = m_attributes[offset + ATTRIBUTE_IX_VALUE_STRING];
        return m_strings.getString(value);
    }

    @Override
    public int getIdAttributeResourceValue(int defaultValue) {
        if (m_idAttribute == -1) {
            return defaultValue;
        }
        int offset = getAttributeOffset(m_idAttribute);
        int valueType = m_attributes[offset + ATTRIBUTE_IX_VALUE_TYPE];
        if (valueType != TypedValue.TYPE_REFERENCE) {
            return defaultValue;
        }
        return m_attributes[offset + ATTRIBUTE_IX_VALUE_DATA];
    }

    @Override
    public int getStyleAttribute() {
        if (m_styleAttribute == -1) {
            return 0;
        }
        int offset = getAttributeOffset(m_styleAttribute);
        return m_attributes[offset + ATTRIBUTE_IX_VALUE_DATA];
    }

    @Override
    public int getAttributeCount() {
        if (m_event != START_TAG) {
            return -1;
        }
        return m_attributes.length / ATTRIBUTE_LENGTH;
    }

    @Override
    public String getAttributeNamespace(int index) {
        int offset = getAttributeOffset(index);
        int namespace = m_attributes[offset + ATTRIBUTE_IX_NAMESPACE_URI];
        if (namespace == -1) {
            return "";
        }

        // Minifiers like removing the namespace, so we will default to default namespace
        // unless the pkgId of the resource is private. We will grab the non-standard one.
        String value = m_strings.getString(namespace);

        if (value.length() == 0) {
            ResID resourceId = new ResID(getAttributeNameResource(index));
            if (resourceId.package_ == PRIVATE_PKG_ID) {
                value = getNonDefaultNamespaceUri();
            } else {
                value = android_ns;
            }
        }

        return value;
    }

    private String getNonDefaultNamespaceUri() {
        int offset = m_namespaces.getCurrentCount() + 1;
        String prefix = m_strings.getString(m_namespaces.get(offset, true));

        if (! prefix.equalsIgnoreCase("android")) {
            return  m_strings.getString(m_namespaces.get(offset, false));
        }

        return android_ns;
    }

    @Override
    public String getAttributePrefix(int index) {
        int offset = getAttributeOffset(index);
        int uri = m_attributes[offset + ATTRIBUTE_IX_NAMESPACE_URI];
        int prefix = m_namespaces.findPrefix(uri);
        if (prefix == -1) {
            return "";
        }
        return m_strings.getString(prefix);
    }

    @Override
    public String getAttributeName(int index) {
        int offset = getAttributeOffset(index);
        int name = m_attributes[offset + ATTRIBUTE_IX_NAME];
        if (name == -1) {
            return "";
        }

        String value = m_strings.getString(name);

        // some attributes will return "", we must rely on the resource_id and refer to the frameworks
        // to match the resource id to the name. ex: 0x101021C = versionName
        if (value.length() != 0 && !android_ns.equals(getAttributeNamespace(index))) {
            return value;
        } else {
            try {
                value = mAttrDecoder.decodeManifestAttr(getAttributeNameResource(index));
            } catch (AndrolibException e) {
            }
            return value;
        }
    }

    @Override
    public int getAttributeNameResource(int index) {
        int offset = getAttributeOffset(index);
        int name = m_attributes[offset + ATTRIBUTE_IX_NAME];
        if (m_resourceIDs == null || name < 0 || name >= m_resourceIDs.length) {
            return 0;
        }
        return m_resourceIDs[name];
    }

    @Override
    public int getAttributeValueType(int index) {
        int offset = getAttributeOffset(index);
        return m_attributes[offset + ATTRIBUTE_IX_VALUE_TYPE];
    }

    @Override
    public int getAttributeValueData(int index) {
        int offset = getAttributeOffset(index);
        return m_attributes[offset + ATTRIBUTE_IX_VALUE_DATA];
    }

    @Override
    public String getAttributeValue(int index) {
        int offset = getAttributeOffset(index);
        int valueType = m_attributes[offset + ATTRIBUTE_IX_VALUE_TYPE];
        int valueData = m_attributes[offset + ATTRIBUTE_IX_VALUE_DATA];
        int valueRaw = m_attributes[offset + ATTRIBUTE_IX_VALUE_STRING];

        if (mAttrDecoder != null) {
            try {
                return mAttrDecoder.decode(
                        valueType,
                        valueData,
                        valueRaw == -1 ? null : ResXmlEncoders.escapeXmlChars(m_strings.getString(valueRaw)),
                        getAttributeNameResource(index));
            } catch (AndrolibException ex) {
                setFirstError(ex);
                LOGGER.log(Level.WARNING, String.format("Could not decode attr value, using undecoded value "
                                + "instead: ns=%s, name=%s, value=0x%08x",
                        getAttributePrefix(index),
                        getAttributeName(index),
                        valueData), ex);
            }
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
        int valueType = m_attributes[offset + ATTRIBUTE_IX_VALUE_TYPE];
        if (valueType == TypedValue.TYPE_FLOAT) {
            int valueData = m_attributes[offset + ATTRIBUTE_IX_VALUE_DATA];
            return Float.intBitsToFloat(valueData);
        }
        return defaultValue;
    }

    @Override
    public int getAttributeIntValue(int index, int defaultValue) {
        int offset = getAttributeOffset(index);
        int valueType = m_attributes[offset + ATTRIBUTE_IX_VALUE_TYPE];
        if (valueType >= TypedValue.TYPE_FIRST_INT && valueType <= TypedValue.TYPE_LAST_INT) {
            return m_attributes[offset + ATTRIBUTE_IX_VALUE_DATA];
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
        int valueType = m_attributes[offset + ATTRIBUTE_IX_VALUE_TYPE];
        if (valueType == TypedValue.TYPE_REFERENCE) {
            return m_attributes[offset + ATTRIBUTE_IX_VALUE_DATA];
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
        // TODO implement
        return 0;
    }

    @Override
    public int getAttributeListValue(String namespace, String attribute, String[] options, int defaultValue) {
        // TODO implement
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

    // ///////////////////////////////// dummies
    @Override
    public void setInput(InputStream stream, String inputEncoding)
            throws XmlPullParserException {
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
    public boolean isEmptyElementTag() throws XmlPullParserException {
        return false;
    }

    @Override
    public boolean isWhitespace() throws XmlPullParserException {
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
    public void setProperty(String name, Object value)
            throws XmlPullParserException {
        throw new XmlPullParserException(E_NOT_SUPPORTED);
    }

    @Override
    public boolean getFeature(String feature) {
        return false;
    }

    @Override
    public void setFeature(String name, boolean value)
            throws XmlPullParserException {
        throw new XmlPullParserException(E_NOT_SUPPORTED);
    }

    // /////////////////////////////////////////// implementation
    /**
     * Namespace stack, holds prefix+uri pairs, as well as depth information.
     * All information is stored in one int[] array. Array consists of depth
     * frames: Data=DepthFrame*; DepthFrame=Count+[Prefix+Uri]*+Count;
     * Count='count of Prefix+Uri pairs'; Yes, count is stored twice, to enable
     * bottom-up traversal. increaseDepth adds depth frame, decreaseDepth
     * removes it. push/pop operations operate only in current depth frame.
     * decreaseDepth removes any remaining (not pop'ed) namespace pairs. findXXX
     * methods search all depth frames starting from the last namespace pair of
     * current depth frame. All functions that operate with int, use -1 as
     * 'invalid value'.
     *
     * !! functions expect 'prefix'+'uri' pairs, not 'uri'+'prefix' !!
     *
     */
    private static final class NamespaceStack {

        public NamespaceStack() {
            m_data = new int[32];
        }

        public final void reset() {
            m_dataLength = 0;
            m_count = 0;
            m_depth = 0;
        }

        public final int getTotalCount() {
            return m_count;
        }

        public final int getCurrentCount() {
            if (m_dataLength == 0) {
                return 0;
            }
            int offset = m_dataLength - 1;
            return m_data[offset];
        }

        public final int getAccumulatedCount(int depth) {
            if (m_dataLength == 0 || depth < 0) {
                return 0;
            }
            if (depth > m_depth) {
                depth = m_depth;
            }
            int accumulatedCount = 0;
            int offset = 0;
            for (; depth != 0; --depth) {
                int count = m_data[offset];
                accumulatedCount += count;
                offset += (2 + count * 2);
            }
            return accumulatedCount;
        }

        public final void push(int prefix, int uri) {
            if (m_depth == 0) {
                increaseDepth();
            }
            ensureDataCapacity(2);
            int offset = m_dataLength - 1;
            int count = m_data[offset];
            m_data[offset - 1 - count * 2] = count + 1;
            m_data[offset] = prefix;
            m_data[offset + 1] = uri;
            m_data[offset + 2] = count + 1;
            m_dataLength += 2;
            m_count += 1;
        }

        public final boolean pop(int prefix, int uri) {
            if (m_dataLength == 0) {
                return false;
            }
            int offset = m_dataLength - 1;
            int count = m_data[offset];
            for (int i = 0, o = offset - 2; i != count; ++i, o -= 2) {
                if (m_data[o] != prefix || m_data[o + 1] != uri) {
                    continue;
                }
                count -= 1;
                if (i == 0) {
                    m_data[o] = count;
                    o -= (1 + count * 2);
                    m_data[o] = count;
                } else {
                    m_data[offset] = count;
                    offset -= (1 + 2 + count * 2);
                    m_data[offset] = count;
                    System.arraycopy(m_data, o + 2, m_data, o, m_dataLength - o);
                }
                m_dataLength -= 2;
                m_count -= 1;
                return true;
            }
            return false;
        }

        public final boolean pop() {
            if (m_dataLength == 0) {
                return false;
            }
            int offset = m_dataLength - 1;
            int count = m_data[offset];
            if (count == 0) {
                return false;
            }
            count -= 1;
            offset -= 2;
            m_data[offset] = count;
            offset -= (1 + count * 2);
            m_data[offset] = count;
            m_dataLength -= 2;
            m_count -= 1;
            return true;
        }

        public final int getPrefix(int index) {
            return get(index, true);
        }

        public final int getUri(int index) {
            return get(index, false);
        }

        public final int findPrefix(int uri) {
            return find(uri, false);
        }

        public final int findUri(int prefix) {
            return find(prefix, true);
        }

        public final int getDepth() {
            return m_depth;
        }

        public final void increaseDepth() {
            ensureDataCapacity(2);
            int offset = m_dataLength;
            m_data[offset] = 0;
            m_data[offset + 1] = 0;
            m_dataLength += 2;
            m_depth += 1;
        }

        public final void decreaseDepth() {
            if (m_dataLength == 0) {
                return;
            }
            int offset = m_dataLength - 1;
            int count = m_data[offset];
            if ((offset - 1 - count * 2) == 0) {
                return;
            }
            m_dataLength -= 2 + count * 2;
            m_count -= count;
            m_depth -= 1;
        }

        private void ensureDataCapacity(int capacity) {
            int available = (m_data.length - m_dataLength);
            if (available > capacity) {
                return;
            }
            int newLength = (m_data.length + available) * 2;
            int[] newData = new int[newLength];
            System.arraycopy(m_data, 0, newData, 0, m_dataLength);
            m_data = newData;
        }

        private final int find(int prefixOrUri, boolean prefix) {
            if (m_dataLength == 0) {
                return -1;
            }
            int offset = m_dataLength - 1;
            for (int i = m_depth; i != 0; --i) {
                int count = m_data[offset];
                offset -= 2;
                for (; count != 0; --count) {
                    if (prefix) {
                        if (m_data[offset] == prefixOrUri) {
                            return m_data[offset + 1];
                        }
                    } else {
                        if (m_data[offset + 1] == prefixOrUri) {
                            return m_data[offset];
                        }
                    }
                    offset -= 2;
                }
            }
            return -1;
        }

        private final int get(int index, boolean prefix) {
            if (m_dataLength == 0 || index < 0) {
                return -1;
            }
            int offset = 0;
            for (int i = m_depth; i != 0; --i) {
                int count = m_data[offset];
                if (index >= count) {
                    index -= count;
                    offset += (2 + count * 2);
                    continue;
                }
                offset += (1 + index * 2);
                if (!prefix) {
                    offset += 1;
                }
                return m_data[offset];
            }
            return -1;
        }

        private int[] m_data;
        private int m_dataLength;
        private int m_count;
        private int m_depth;
    }

    final StringBlock getStrings() {
        return m_strings;
    }

    private final int getAttributeOffset(int index) {
        if (m_event != START_TAG) {
            throw new IndexOutOfBoundsException("Current event is not START_TAG.");
        }
        int offset = index * ATTRIBUTE_LENGTH;
        if (offset >= m_attributes.length) {
            throw new IndexOutOfBoundsException("Invalid attribute index (" + index + ").");
        }
        return offset;
    }

    private final int findAttribute(String namespace, String attribute) {
        if (m_strings == null || attribute == null) {
            return -1;
        }
        int name = m_strings.find(attribute);
        if (name == -1) {
            return -1;
        }
        int uri = (namespace != null) ? m_strings.find(namespace) : -1;
        for (int o = 0; o != m_attributes.length; o += ATTRIBUTE_LENGTH) {
            if (name == m_attributes[o + ATTRIBUTE_IX_NAME]
                    && (uri == -1 || uri == m_attributes[o + ATTRIBUTE_IX_NAMESPACE_URI])) {
                return o / ATTRIBUTE_LENGTH;
            }
        }
        return -1;
    }

    private final void resetEventInfo() {
        m_event = -1;
        m_lineNumber = -1;
        m_name = -1;
        m_namespaceUri = -1;
        m_attributes = null;
        m_idAttribute = -1;
        m_classAttribute = -1;
        m_styleAttribute = -1;
    }

    private final void doNext() throws IOException {
        // Delayed initialization.
        if (m_strings == null) {
            m_reader.skipCheckInt(CHUNK_AXML_FILE, CHUNK_AXML_FILE_BROKEN);

			/*
			 * chunkSize
			 */
            m_reader.skipInt();
            m_strings = StringBlock.read(m_reader);
            m_namespaces.increaseDepth();
            m_operational = true;
        }

        if (m_event == END_DOCUMENT) {
            return;
        }

        int event = m_event;
        resetEventInfo();

        while (true) {
            if (m_decreaseDepth) {
                m_decreaseDepth = false;
                m_namespaces.decreaseDepth();
            }

            // Fake END_DOCUMENT event.
            if (event == END_TAG && m_namespaces.getDepth() == 1 && m_namespaces.getCurrentCount() == 0) {
                m_event = END_DOCUMENT;
                break;
            }

            int chunkType;
            if (event == START_DOCUMENT) {
                // Fake event, see CHUNK_XML_START_TAG handler.
                chunkType = CHUNK_XML_START_TAG;
            } else {
                chunkType = m_reader.readInt();
            }

            if (chunkType == CHUNK_RESOURCEIDS) {
                int chunkSize = m_reader.readInt();
                if (chunkSize < 8 || (chunkSize % 4) != 0) {
                    throw new IOException("Invalid resource ids size (" + chunkSize + ").");
                }
                m_resourceIDs = m_reader.readIntArray(chunkSize / 4 - 2);
                continue;
            }

            if (chunkType < CHUNK_XML_FIRST || chunkType > CHUNK_XML_LAST) {
                throw new IOException("Invalid chunk type (" + chunkType + ").");
            }

            // Fake START_DOCUMENT event.
            if (chunkType == CHUNK_XML_START_TAG && event == -1) {
                m_event = START_DOCUMENT;
                break;
            }

            // Common header.
			/* chunkSize */m_reader.skipInt();
            int lineNumber = m_reader.readInt();
			/* 0xFFFFFFFF */m_reader.skipInt();

            if (chunkType == CHUNK_XML_START_NAMESPACE || chunkType == CHUNK_XML_END_NAMESPACE) {
                if (chunkType == CHUNK_XML_START_NAMESPACE) {
                    int prefix = m_reader.readInt();
                    int uri = m_reader.readInt();
                    m_namespaces.push(prefix, uri);
                } else {
					/* prefix */m_reader.skipInt();
					/* uri */m_reader.skipInt();
                    m_namespaces.pop();
                }
                continue;
            }

            m_lineNumber = lineNumber;

            if (chunkType == CHUNK_XML_START_TAG) {
                m_namespaceUri = m_reader.readInt();
                m_name = m_reader.readInt();
				/* flags? */m_reader.skipInt();
                int attributeCount = m_reader.readInt();
                m_idAttribute = (attributeCount >>> 16) - 1;
                attributeCount &= 0xFFFF;
                m_classAttribute = m_reader.readInt();
                m_styleAttribute = (m_classAttribute >>> 16) - 1;
                m_classAttribute = (m_classAttribute & 0xFFFF) - 1;
                m_attributes = m_reader.readIntArray(attributeCount * ATTRIBUTE_LENGTH);
                for (int i = ATTRIBUTE_IX_VALUE_TYPE; i < m_attributes.length; ) {
                    m_attributes[i] = (m_attributes[i] >>> 24);
                    i += ATTRIBUTE_LENGTH;
                }
                m_namespaces.increaseDepth();
                m_event = START_TAG;
                break;
            }

            if (chunkType == CHUNK_XML_END_TAG) {
                m_namespaceUri = m_reader.readInt();
                m_name = m_reader.readInt();
                m_event = END_TAG;
                m_decreaseDepth = true;
                break;
            }

            if (chunkType == CHUNK_XML_TEXT) {
                m_name = m_reader.readInt();
				/* ? */m_reader.skipInt();
				/* ? */m_reader.skipInt();
                m_event = TEXT;
                break;
            }
        }
    }

    private void setFirstError(AndrolibException error) {
        if (mFirstError == null) {
            mFirstError = error;
        }
    }

    // ///////////////////////////////// data
	/*
	 * All values are essentially indices, e.g. m_name is an index of name in
	 * m_strings.
	 */
    private ExtDataInput m_reader;
    private ResAttrDecoder mAttrDecoder;
    private AndrolibException mFirstError;

    private boolean m_operational = false;
    private StringBlock m_strings;
    private int[] m_resourceIDs;
    private NamespaceStack m_namespaces = new NamespaceStack();
    private String android_ns = "http://schemas.android.com/apk/res/android";
    private boolean m_decreaseDepth;
    private int m_event;
    private int m_lineNumber;
    private int m_name;
    private int m_namespaceUri;
    private int[] m_attributes;
    private int m_idAttribute;
    private int m_classAttribute;
    private int m_styleAttribute;

    private final static Logger LOGGER = Logger.getLogger(AXmlResourceParser.class.getName());
    private static final String E_NOT_SUPPORTED = "Method is not supported.";
    private static final int ATTRIBUTE_IX_NAMESPACE_URI = 0,
            ATTRIBUTE_IX_NAME = 1, ATTRIBUTE_IX_VALUE_STRING = 2,
            ATTRIBUTE_IX_VALUE_TYPE = 3, ATTRIBUTE_IX_VALUE_DATA = 4,
            ATTRIBUTE_LENGTH = 5;

    private static final int CHUNK_AXML_FILE = 0x00080003, CHUNK_AXML_FILE_BROKEN = 0x00080001,
            CHUNK_RESOURCEIDS = 0x00080180, CHUNK_XML_FIRST = 0x00100100,
            CHUNK_XML_START_NAMESPACE = 0x00100100,
            CHUNK_XML_END_NAMESPACE = 0x00100101,
            CHUNK_XML_START_TAG = 0x00100102, CHUNK_XML_END_TAG = 0x00100103,
            CHUNK_XML_TEXT = 0x00100104, CHUNK_XML_LAST = 0x00100104;

    private static final int PRIVATE_PKG_ID = 0x7F;
}
