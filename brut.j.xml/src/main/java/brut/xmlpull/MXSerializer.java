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
package brut.xmlpull;

import org.xmlpull.v1.XmlSerializer;

import java.io.*;
import java.util.*;

/**
 * Implementation of XmlSerializer interface from XmlPull V1 API. This
 * implementation is optimized for performance and low memory footprint.
 *
 * <p>
 * Implemented features:
 * <ul>
 * <li>FEATURE_ATTR_VALUE_NO_ESCAPE
 * <li>FEATURE_NAMES_INTERNED - when enabled all returned names (namespaces,
 * prefixes) will be interned and it is required that all names passed as
 * arguments MUST be interned
 * </ul>
 * <p>
 * Implemented properties:
 * <ul>
 * <li>PROPERTY_DEFAULT_ENCODING
 * <li>PROPERTY_INDENTATION
 * <li>PROPERTY_LINE_SEPARATOR
 * <li>PROPERTY_LOCATION
 * </ul>
 *
 */
public class MXSerializer implements XmlSerializer {
    public static final String FEATURE_ATTR_VALUE_NO_ESCAPE = "http://xmlpull.org/v1/doc/features.html#attr-value-no-escape";
    public static final String FEATURE_NAMES_INTERNED = "http://xmlpull.org/v1/doc/features.html#names-interned";
    public static final String PROPERTY_DEFAULT_ENCODING = "http://xmlpull.org/v1/doc/properties.html#default-encoding";
    public static final String PROPERTY_INDENTATION = "http://xmlpull.org/v1/doc/properties.html#indentation";
    public static final String PROPERTY_LINE_SEPARATOR = "http://xmlpull.org/v1/doc/properties.html#line-separator";
    public static final String PROPERTY_LOCATION = "http://xmlpull.org/v1/doc/properties.html#location";

    private static final boolean TRACE_SIZING = false;
    private static final boolean TRACE_ESCAPING = false;
    private static final String XML_URI = "http://www.w3.org/XML/1998/namespace";
    private static final String XMLNS_URI = "http://www.w3.org/2000/xmlns/";

    // properties/features
    private boolean namesInterned;
    private boolean attrValueNoEscape;
    private String defaultEncoding;
    private String indentationString;
    private String lineSeparator;

    private String location;
    private Writer writer;

    private int autoDeclaredPrefixes;

    private int depth = 0;

    // element stack
    private String[] elNamespace = new String[2];
    private String[] elName = new String[elNamespace.length];
    private String[] elPrefix = new String[elNamespace.length];
    private int[] elNamespaceCount = new int[elNamespace.length];

    // namespace stack
    private int namespaceEnd = 0;
    private String[] namespacePrefix = new String[8];
    private String[] namespaceUri = new String[namespacePrefix.length];

    private boolean finished;
    private boolean pastRoot;
    private boolean setPrefixCalled;
    private boolean startTagIncomplete;

    private boolean doIndent;
    private boolean seenTag;

    private boolean seenBracket;
    private boolean seenBracketBracket;

    private static final String[] precomputedPrefixes;
    static {
        precomputedPrefixes = new String[32]; // arbitrary number ...
        for (int i = 0; i < precomputedPrefixes.length; i++) {
            precomputedPrefixes[i] = ("n" + i).intern();
        }
    }

    private final boolean checkNamesInterned = false;

    private void checkInterning(String name) {
        if (namesInterned && !Objects.equals(name, name.intern())) {
            throw new IllegalArgumentException("all names passed as arguments must be interned"
                    + "when NAMES INTERNED feature is enabled");
        }
    }

    private String getLocation() {
        return location != null ? " @" + location : "";
    }

    private void ensureElementsCapacity() {
        int elStackSize = elName.length;
        int newSize = (depth >= 7 ? 2 * depth : 8) + 2;

        if (TRACE_SIZING) {
            System.err.println(getClass().getName() + " elStackSize "
                    + elStackSize + " ==> " + newSize);
        }
        boolean needsCopying = elStackSize > 0;
        String[] arr;
        // reuse arr local variable slot
        arr = new String[newSize];
        if (needsCopying) {
            System.arraycopy(elName, 0, arr, 0, elStackSize);
        }
        elName = arr;

        arr = new String[newSize];
        if (needsCopying) {
            System.arraycopy(elPrefix, 0, arr, 0, elStackSize);
        }
        elPrefix = arr;

        arr = new String[newSize];
        if (needsCopying) {
            System.arraycopy(elNamespace, 0, arr, 0, elStackSize);
        }
        elNamespace = arr;

        int[] iarr = new int[newSize];
        if (needsCopying) {
            System.arraycopy(elNamespaceCount, 0, iarr, 0, elStackSize);
        } else {
            // special initialization
            iarr[0] = 0;
        }
        elNamespaceCount = iarr;
    }

    private void ensureNamespacesCapacity() {
        int newSize = namespaceEnd > 7 ? 2 * namespaceEnd : 8;
        if (TRACE_SIZING) {
            System.err.println(getClass().getName() + " namespaceSize " + namespacePrefix.length + " ==> " + newSize);
        }
        String[] newNamespacePrefix = new String[newSize];
        String[] newNamespaceUri = new String[newSize];
        if (namespacePrefix != null) {
            System.arraycopy(namespacePrefix, 0, newNamespacePrefix, 0, namespaceEnd);
            System.arraycopy(namespaceUri, 0, newNamespaceUri, 0, namespaceEnd);
        }
        namespacePrefix = newNamespacePrefix;
        namespaceUri = newNamespaceUri;
    }

    // use buffer to optimize writing
    private static final int BUFFER_LEN = 8192;
    private final char[] buffer = new char[BUFFER_LEN];
    private int bufidx;

    private void flushBuffer() throws IOException {
        if (bufidx > 0) {
            writer.write(buffer, 0, bufidx);
            writer.flush();
            bufidx = 0;
        }
    }

    private void write(char ch) throws IOException {
        if (bufidx >= BUFFER_LEN) {
            flushBuffer();
        }
        buffer[bufidx++] = ch;
    }

    private void write(char[] buf, int i, int length) throws IOException {
        while (length > 0) {
            if (bufidx == BUFFER_LEN) {
                flushBuffer();
            }
            int batch = BUFFER_LEN - bufidx;
            if (batch > length) {
                batch = length;
            }
            System.arraycopy(buf, i, buffer, bufidx, batch);
            i += batch;
            length -= batch;
            bufidx += batch;
        }
    }

    private void write(String str) throws IOException {
        if (str == null) {
            str = "";
        }
        write(str, 0, str.length());
    }

    private void write(String str, int i, int length) throws IOException {
        while (length > 0) {
            if (bufidx == BUFFER_LEN) {
                flushBuffer();
            }
            int batch = BUFFER_LEN - bufidx;
            if (batch > length) {
                batch = length;
            }
            str.getChars(i, i + batch, buffer, bufidx);
            i += batch;
            length -= batch;
            bufidx += batch;
        }
    }

    // precomputed variables to simplify writing indentation
    private static final int MAX_INDENT = 65;
    private int offsetNewLine;
    private int indentationJump;
    private char[] indentationBuf;
    private int maxIndentLevel;
    private boolean writeLineSeparator; // should end-of-line be written
    private boolean writeIndentation; // is indentation used?

    /**
     * For maximum efficiency when writing indents the required output is
     * pre-computed This is internal function that recomputes buffer after user
     * requested changes.
     */
    private void rebuildIndentationBuf() {
        if (!doIndent) {
            return;
        }
        int bufSize = 0;
        offsetNewLine = 0;
        if (writeLineSeparator) {
            offsetNewLine = lineSeparator.length();
            bufSize += offsetNewLine;
        }
        maxIndentLevel = 0;
        if (writeIndentation) {
            indentationJump = indentationString.length();
            maxIndentLevel = MAX_INDENT / indentationJump;
            bufSize += maxIndentLevel * indentationJump;
        }
        if (indentationBuf == null || indentationBuf.length < bufSize) {
            indentationBuf = new char[bufSize + 8];
        }
        int bufPos = 0;
        if (writeLineSeparator) {
            for (int i = 0; i < lineSeparator.length(); i++) {
                indentationBuf[bufPos++] = lineSeparator.charAt(i);
            }
        }
        if (writeIndentation) {
            for (int i = 0; i < maxIndentLevel; i++) {
                for (int j = 0; j < indentationString.length(); j++) {
                    indentationBuf[bufPos++] = indentationString.charAt(j);
                }
            }
        }
    }

    private void writeIndent() throws IOException {
        int start = writeLineSeparator ? 0 : offsetNewLine;
        int level = Math.min(depth, maxIndentLevel);

        write(indentationBuf, start, ((level - 1) * indentationJump) + offsetNewLine);
    }

    // --- public API methods

    @Override
    public void setFeature(String name, boolean state)
            throws IllegalArgumentException, IllegalStateException {
        if (name == null) {
            throw new IllegalArgumentException("feature name can not be null");
        }
        switch (name) {
            case FEATURE_ATTR_VALUE_NO_ESCAPE:
                attrValueNoEscape = state;
                break;
            case FEATURE_NAMES_INTERNED:
                namesInterned = state;
                break;
            default:
                throw new IllegalStateException("unsupported feature: " + name);
        }
    }

    @Override
    public boolean getFeature(String name) throws IllegalArgumentException {
        if (name == null) {
            throw new IllegalArgumentException("feature name can not be null");
        }
        switch (name) {
            case FEATURE_ATTR_VALUE_NO_ESCAPE:
                return attrValueNoEscape;
            case FEATURE_NAMES_INTERNED:
                return namesInterned;
            default:
                return false;
        }
    }

    @Override
    public void setProperty(String name, Object value)
            throws IllegalArgumentException, IllegalStateException {
        if (name == null) {
            throw new IllegalArgumentException("property name can not be null");
        }
        switch (name) {
            case PROPERTY_DEFAULT_ENCODING:
                defaultEncoding = (String) value;
                break;
            case PROPERTY_INDENTATION:
                indentationString = (String) value;
                break;
            case PROPERTY_LINE_SEPARATOR:
                lineSeparator = (String) value;
                break;
            case PROPERTY_LOCATION:
                location = (String) value;
                break;
            default:
                throw new IllegalStateException("unsupported property: " + name);
        }
        writeLineSeparator = lineSeparator != null && !lineSeparator.isEmpty();
        writeIndentation = indentationString != null && !indentationString.isEmpty();
        // optimize - do not write when nothing to write ...
        doIndent = indentationString != null && (writeLineSeparator || writeIndentation);
        // NOTE: when indentationString == null there is no indentation
        // (even though writeLineSeparator may be true ...)
        rebuildIndentationBuf();
        seenTag = false; // for consistency
    }

    @Override
    public Object getProperty(String name) throws IllegalArgumentException {
        if (name == null) {
            throw new IllegalArgumentException("property name can not be null");
        }
        switch (name) {
            case PROPERTY_DEFAULT_ENCODING:
                return defaultEncoding;
            case PROPERTY_INDENTATION:
                return indentationString;
            case PROPERTY_LINE_SEPARATOR:
                return lineSeparator;
            case PROPERTY_LOCATION:
                return location;
            default:
                return null;
        }
    }

    @Override
    public void setOutput(Writer writer) {
        this.writer = writer;

        // reset state
        location = null;
        autoDeclaredPrefixes = 0;
        depth = 0;

        // nullify references on all levels to allow it to be GCed
        for (int i = 0; i < elNamespaceCount.length; i++) {
            elName[i] = null;
            elPrefix[i] = null;
            elNamespace[i] = null;
            elNamespaceCount[i] = 2;
        }

        namespaceEnd = 0;

        // TODO: how to prevent from reporting this namespace?
        // this is special namespace declared for consistency with XML infoset
        namespacePrefix[namespaceEnd] = "xmlns";
        namespaceUri[namespaceEnd] = XMLNS_URI;
        ++namespaceEnd;

        namespacePrefix[namespaceEnd] = "xml";
        namespaceUri[namespaceEnd] = XML_URI;
        ++namespaceEnd;

        finished = false;
        pastRoot = false;
        setPrefixCalled = false;
        startTagIncomplete = false;
        seenTag = false;

        seenBracket = false;
        seenBracketBracket = false;
    }

    @Override
    public void setOutput(OutputStream os, String encoding) throws IOException {
        if (os == null) {
            throw new IllegalArgumentException("output stream can not be null");
        }
        if (encoding == null) {
            encoding = defaultEncoding;
        }
        setOutput(encoding != null
            ? new OutputStreamWriter(os, encoding)
            : new OutputStreamWriter(os));
    }

    @Override
    public void startDocument(String encoding, Boolean standalone) throws IOException {
        write("<?xml version=\"1.0\"");
        if (encoding == null) {
            encoding = defaultEncoding;
        }
        if (encoding != null) {
            write(" encoding=\"");
            write(encoding);
            write('"');
        }
        if (standalone != null) {
            write(" standalone=\"");
            if (standalone.booleanValue()) {
                write("yes");
            } else {
                write("no");
            }
            write('"');
        }
        write("?>");
        if (writeLineSeparator) {
            write(lineSeparator);
        }
    }

    @Override
    public void endDocument() throws IOException {
        // close all unclosed tag;
        while (depth > 0) {
            endTag(elNamespace[depth], elName[depth]);
        }
        if (writeLineSeparator) {
            write(lineSeparator);
        }
        flushBuffer();
        finished = pastRoot = startTagIncomplete = true;
    }

    @Override
    public void setPrefix(String prefix, String namespace) throws IOException {
        if (startTagIncomplete) {
            closeStartTag();
        }
        if (prefix == null) {
            prefix = "";
        }
        if (!namesInterned) {
            prefix = prefix.intern(); // will throw NPE if prefix==null
        } else if (checkNamesInterned) {
            checkInterning(prefix);
        } else if (prefix == null) {
            throw new IllegalArgumentException("prefix must be not null" + getLocation());
        }

        if (!namesInterned) {
            namespace = namespace.intern();
        } else if (checkNamesInterned) {
            checkInterning(namespace);
        } else if (namespace == null) {
            throw new IllegalArgumentException("namespace must be not null" + getLocation());
        }

        if (namespaceEnd >= namespacePrefix.length) {
            ensureNamespacesCapacity();
        }
        namespacePrefix[namespaceEnd] = prefix;
        namespaceUri[namespaceEnd] = namespace;
        ++namespaceEnd;
        setPrefixCalled = true;
    }

    @Override
    public String getPrefix(String namespace, boolean generatePrefix) {
        return getPrefix(namespace, generatePrefix, false);
    }

    private String getPrefix(String namespace, boolean generatePrefix, boolean nonEmpty) {
        if (!namesInterned) {
            // when String is interned we can do much faster namespace stack lookups ...
            namespace = namespace.intern();
        } else if (checkNamesInterned) {
            checkInterning(namespace);
        }
        if (namespace == null) {
            throw new IllegalArgumentException("namespace must be not null" + getLocation());
        } else if (namespace.isEmpty()) {
            throw new IllegalArgumentException("default namespace cannot have prefix" + getLocation());
        }

        // first check if namespace is already in scope
        for (int i = namespaceEnd - 1; i >= 0; --i) {
            if (namespace.equals(namespaceUri[i])) {
                String prefix = namespacePrefix[i];
                if (nonEmpty && prefix.isEmpty()) {
                    continue;
                }

                return prefix;
            }
        }

        // so not found it ...
        if (!generatePrefix) {
            return null;
        }
        return generatePrefix(namespace);
    }

    @Override
    public int getDepth() {
        return depth;
    }

    @Override
    public String getNamespace() {
        return elNamespace[depth];
    }

    @Override
    public String getName() {
        return elName[depth];
    }

    @Override
    public XmlSerializer startTag(String namespace, String name) throws IOException {
        if (startTagIncomplete) {
            closeStartTag();
        }
        seenBracket = seenBracketBracket = false;
        ++depth;
        if (doIndent && depth > 0 && seenTag) {
            writeIndent();
        }
        seenTag = true;
        setPrefixCalled = false;
        startTagIncomplete = true;
        if ((depth + 1) >= elName.length) {
            ensureElementsCapacity();
        }

        if (checkNamesInterned && namesInterned) {
            checkInterning(namespace);
        }

        elNamespace[depth] = (namesInterned || namespace == null) ? namespace : namespace.intern();
        if (checkNamesInterned && namesInterned) {
            checkInterning(name);
        }

        elName[depth] = (namesInterned || name == null) ? name : name.intern();
        if (writer == null) {
            throw new IllegalStateException("setOutput() must called set before serialization can start");
        }
        write('<');
        if (namespace != null) {
            if (!namespace.isEmpty()) {
                // in future make this algo a feature on serializer
                String prefix = null;
                if (depth > 0 && (namespaceEnd - elNamespaceCount[depth - 1]) == 1) {
                    // if only one prefix was declared un-declare it if the
                    // prefix is already declared on parent el with the same URI
                    String uri = namespaceUri[namespaceEnd - 1];
                    if (uri == namespace || uri.equals(namespace)) {
                        String elPfx = namespacePrefix[namespaceEnd - 1];
                        for (int pos = elNamespaceCount[depth - 1] - 1; pos >= 2; --pos) {
                            String pf = namespacePrefix[pos];
                            if (pf == elPfx || pf.equals(elPfx)) {
                                String n = namespaceUri[pos];
                                if (n == uri || n.equals(uri)) {
                                    --namespaceEnd; // un-declare namespace: this is kludge!
                                    prefix = elPfx;
                                }
                                break;
                            }
                        }
                    }
                }
                if (prefix == null) {
                    prefix = getPrefix(namespace, true, false);
                }
                // make sure that default ("") namespace to not print ":"
                if (!prefix.isEmpty()) {
                    elPrefix[depth] = prefix;
                    write(prefix);
                    write(':');
                } else {
                    elPrefix[depth] = "";
                }
            } else {
                // make sure that default namespace can be declared
                for (int i = namespaceEnd - 1; i >= 0; --i) {
                    if (namespacePrefix[i] == "") {
                        String uri = namespaceUri[i];
                        if (uri == null) {
                            setPrefix("", "");
                        } else if (!uri.isEmpty()) {
                            throw new IllegalStateException("start tag can not be written in empty default namespace "
                                    + "as default namespace is currently bound to '"
                                    + uri + "'" + getLocation());
                        }
                        break;
                    }
                }
                elPrefix[depth] = "";
            }
        } else {
            elPrefix[depth] = "";
        }
        write(name);
        return this;
    }

    private void closeStartTag() throws IOException {
        if (finished) {
            throw new IllegalArgumentException("trying to write past already finished output" + getLocation());
        }
        if (seenBracket) {
            seenBracket = seenBracketBracket = false;
        }
        if (startTagIncomplete || setPrefixCalled) {
            if (setPrefixCalled) {
                throw new IllegalArgumentException("startTag() must be called immediately after setPrefix()" + getLocation());
            }
            if (!startTagIncomplete) {
                throw new IllegalArgumentException("trying to close start tag that is not opened" + getLocation());
            }

            // write all namespace declarations!
            writeNamespaceDeclarations();
            write('>');
            elNamespaceCount[depth] = namespaceEnd;
            startTagIncomplete = false;
        }
    }

    @Override
    public XmlSerializer attribute(String namespace, String name, String value) throws IOException {
        if (!startTagIncomplete) {
            throw new IllegalArgumentException("startTag() must be called before attribute()" + getLocation());
        }
        write(' ');
        if (namespace != null && !namespace.isEmpty()) {
            if (!namesInterned) {
                namespace = namespace.intern();
            } else if (checkNamesInterned) {
                checkInterning(namespace);
            }
            String prefix = getPrefix(namespace, false, true);
            if (prefix == null) {
                // needs to declare prefix to hold default namespace
                // NOTE: attributes such as a='b' are in NO namespace
                prefix = generatePrefix(namespace);
            }
            write(prefix);
            write(':');
        }
        write(name);
        write("=\"");
        writeAttributeValue(value);
        write('"');
        return this;
    }

    @Override
    public XmlSerializer endTag(String namespace, String name) throws IOException {
        seenBracket = seenBracketBracket = false;
        if (namespace != null) {
            if (!namesInterned) {
                namespace = namespace.intern();
            } else if (checkNamesInterned) {
                checkInterning(namespace);
            }
        }

        if (name == null) {
            throw new IllegalArgumentException("end tag name can not be null" + getLocation());
        }
        if (checkNamesInterned && namesInterned) {
            checkInterning(name);
        }
        if (startTagIncomplete) {
            writeNamespaceDeclarations();
            write(" />"); // space is added to make it easier to work in XHTML!!!
        } else {
            if (doIndent && seenTag) {
                writeIndent();
            }
            write("</");
            String startTagPrefix = elPrefix[depth];
            if (!startTagPrefix.isEmpty()) {
                write(startTagPrefix);
                write(':');
            }
            write(name);
            write('>');
        }
        --depth;
        namespaceEnd = elNamespaceCount[depth];
        startTagIncomplete = false;
        seenTag = true;
        return this;
    }

    @Override
    public XmlSerializer text(String text) throws IOException {
        if (startTagIncomplete || setPrefixCalled) {
            closeStartTag();
        }
        if (doIndent && seenTag) {
            seenTag = false;
        }
        writeElementContent(text);
        return this;
    }

    @Override
    public XmlSerializer text(char[] buf, int start, int len) throws IOException {
        if (startTagIncomplete || setPrefixCalled) {
            closeStartTag();
        }
        if (doIndent && seenTag) {
            seenTag = false;
        }
        writeElementContent(buf, start, len);
        return this;
    }

    @Override
    public void cdsect(String text) throws IOException {
        if (startTagIncomplete || setPrefixCalled || seenBracket) {
            closeStartTag();
        }
        if (doIndent && seenTag) {
            seenTag = false;
        }
        write("<![CDATA[");
        write(text);
        write("]]>");
    }

    @Override
    public void entityRef(String text) throws IOException {
        if (startTagIncomplete || setPrefixCalled || seenBracket) {
            closeStartTag();
        }
        if (doIndent && seenTag) {
            seenTag = false;
        }
        write('&');
        write(text);
        write(';');
    }

    @Override
    public void processingInstruction(String text) throws IOException {
        if (startTagIncomplete || setPrefixCalled || seenBracket) {
            closeStartTag();
        }
        if (doIndent && seenTag) {
            seenTag = false;
        }
        write("<?");
        write(text);
        write("?>");
    }

    @Override
    public void comment(String text) throws IOException {
        if (startTagIncomplete || setPrefixCalled || seenBracket) {
            closeStartTag();
        }
        if (doIndent && seenTag) {
            seenTag = false;
        }
        write("<!--");
        write(text);
        write("-->");
    }

    @Override
    public void docdecl(String text) throws IOException {
        if (startTagIncomplete || setPrefixCalled || seenBracket) {
            closeStartTag();
        }
        if (doIndent && seenTag) {
            seenTag = false;
        }
        write("<!DOCTYPE");
        write(text);
        write(">");
    }

    @Override
    public void ignorableWhitespace(String text) throws IOException {
        if (startTagIncomplete || setPrefixCalled || seenBracket) {
            closeStartTag();
        }
        if (doIndent && seenTag) {
            seenTag = false;
        }
        if (text.isEmpty()) {
            throw new IllegalArgumentException("empty string is not allowed for ignorable whitespace" + getLocation());
        }
        write(text);
    }

    @Override
    public void flush() throws IOException {
        if (!finished && startTagIncomplete) {
            closeStartTag();
        }
        flushBuffer();
    }

    // --- utility methods

    private String generatePrefix(String namespace) {
        ++autoDeclaredPrefixes;
        // fast lookup uses table that was pre-initialized in static{} ....
        String prefix = autoDeclaredPrefixes < precomputedPrefixes.length
            ? precomputedPrefixes[autoDeclaredPrefixes]
            : ("n" + autoDeclaredPrefixes).intern();

        // declare prefix
        if (namespaceEnd >= namespacePrefix.length) {
            ensureNamespacesCapacity();
        }
        namespacePrefix[namespaceEnd] = prefix;
        namespaceUri[namespaceEnd] = namespace;
        ++namespaceEnd;

        return prefix;
    }

    private void writeNamespaceDeclarations() throws IOException {
        Set<String> uniqueNamespaces = new HashSet<>();
        for (int i = elNamespaceCount[depth - 1]; i < namespaceEnd; i++) {
            String prefix = namespacePrefix[i];
            String uri = namespaceUri[i];

            // Some applications as seen in #2664 have duplicated namespaces.
            // AOSP doesn't care, but the parser does. So we filter them writer.
            if (uniqueNamespaces.contains(prefix + uri)) {
                continue;
            }

            if (doIndent && uri.length() > 40) {
                writeIndent();
                write(' ');
            }
            write(" xmlns");
            if (prefix != "") {
                write(':');
                write(prefix);
            }
            write("=\"");
            writeAttributeValue(uri);
            write('"');

            uniqueNamespaces.add(prefix + uri);
        }
    }

    private void writeAttributeValue(String value) throws IOException {
        if (attrValueNoEscape) {
            write(value);
            return;
        }
        // .[&, < and " escaped],
        int pos = 0;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == '&') {
                if (i > pos) {
                    write(value.substring(pos, i));
                }
                write("&amp;");
                pos = i + 1;
            }
            if (ch == '<') {
                if (i > pos) {
                    write(value.substring(pos, i));
                }
                write("&lt;");
                pos = i + 1;
            } else if (ch == '"') {
                if (i > pos) {
                    write(value.substring(pos, i));
                }
                write("&quot;");
                pos = i + 1;
            } else if (ch < 32) {
                // in XML 1.0 only legal character are #x9 | #xA | #xD
                // and they must be escaped otherwise in attribute value they
                // are normalized to spaces
                if (ch == 13 || ch == 10 || ch == 9) {
                    if (i > pos) {
                        write(value.substring(pos, i));
                    }
                    write("&#");
                    write(Integer.toString(ch));
                    write(';');
                    pos = i + 1;
                } else {
                    if (TRACE_ESCAPING) {
                        System.err.println(getClass().getName() + " DEBUG ATTR value.len=" + value.length()
                                + " " + printable(value));
                    }
                    throw new IllegalStateException(
                            "character " + printable(ch) + " (" + Integer.toString(ch) + ") is not allowed in output"
                                    + getLocation() + " (attr value="
                                    + printable(value) + ")");
                }
            }
        }
        write(pos > 0 ? value.substring(pos) : value);
    }

    private void writeElementContent(String text) throws IOException {
        // For some reason, some non-empty, empty characters are surviving this far and getting filtered writer
        // So we are left with null, which causes an NPE
        if (text == null) {
            return;
        }

        // escape '<', '&', ']]>', <32 if necessary
        int pos = 0;
        for (int i = 0; i < text.length(); i++) {
            // TODO: check if doing char[] text.getChars() would be faster than
            // getCharAt(i) ...
            char ch = text.charAt(i);
            if (ch == ']') {
                if (seenBracket) {
                    seenBracketBracket = true;
                } else {
                    seenBracket = true;
                }
            } else {
                if (ch == '&') {
                    if (!(i < text.length() - 3 && text.charAt(i+1) == 'l'
                            && text.charAt(i+2) == 't' && text.charAt(i+3) == ';')) {
                        if (i > pos) {
                            write(text.substring(pos, i));
                        }
                        write("&amp;");
                        pos = i + 1;
                    }
                } else if (ch == '<') {
                    if (i > pos) {
                        write(text.substring(pos, i));
                    }
                    write("&lt;");
                    pos = i + 1;
                } else if (seenBracketBracket && ch == '>') {
                    if (i > pos) {
                        write(text.substring(pos, i));
                    }
                    write("&gt;");
                    pos = i + 1;
                } else if (ch < 32) {
                    // in XML 1.0 only legal character are #x9 | #xA | #xD
                    if (ch == 9 || ch == 10 || ch == 13) {
                        // pass through
                    } else {
                        if (TRACE_ESCAPING) {
                            System.err.println(getClass().getName() + " DEBUG TEXT value.len=" + text.length()
                                    + " " + printable(text));
                        }
                        throw new IllegalStateException("character " + Integer.toString(ch)
                                + " is not allowed in output" + getLocation()
                                + " (text value=" + printable(text) + ")");
                    }
                }
                if (seenBracket) {
                    seenBracketBracket = seenBracket = false;
                }
            }
        }
        write(pos > 0 ? text.substring(pos) : text);
    }

    private void writeElementContent(char[] buf, int off, int len) throws IOException {
        // escape '<', '&', ']]>'
        int end = off + len;
        int pos = off;
        for (int i = off; i < end; i++) {
            char ch = buf[i];
            if (ch == ']') {
                if (seenBracket) {
                    seenBracketBracket = true;
                } else {
                    seenBracket = true;
                }
            } else {
                if (ch == '&') {
                    if (i > pos) {
                        write(buf, pos, i - pos);
                    }
                    write("&amp;");
                    pos = i + 1;
                } else if (ch == '<') {
                    if (i > pos) {
                        write(buf, pos, i - pos);
                    }
                    write("&lt;");
                    pos = i + 1;

                } else if (seenBracketBracket && ch == '>') {
                    if (i > pos) {
                        write(buf, pos, i - pos);
                    }
                    write("&gt;");
                    pos = i + 1;
                } else if (ch < 32) {
                    // in XML 1.0 only legal character are #x9 | #xA | #xD
                    if (ch == 9 || ch == 10 || ch == 13) {
                        // pass through
                    } else {
                        if (TRACE_ESCAPING) {
                            System.err.println(getClass().getName() + " DEBUG TEXT value.len=" + len + " "
                                    + printable(new String(buf, off, len)));
                        }
                        throw new IllegalStateException("character "
                                + printable(ch) + " (" + Integer.toString(ch)
                                + ") is not allowed in output" + getLocation());
                    }
                }
                if (seenBracket) {
                    seenBracketBracket = seenBracket = false;
                }
            }
        }
        if (end > pos) {
            write(buf, pos, end - pos);
        }
    }

    private static String printable(String str) {
        if (str == null) {
            return "null";
        }
        StringBuffer retval = new StringBuffer(str.length() + 16);
        retval.append("'");
        for (int i = 0; i < str.length(); i++) {
            addPrintable(retval, str.charAt(i));
        }
        retval.append("'");
        return retval.toString();
    }

    private static String printable(char ch) {
        StringBuffer retval = new StringBuffer();
        addPrintable(retval, ch);
        return retval.toString();
    }

    private static void addPrintable(StringBuffer retval, char ch) {
        switch (ch) {
            case '\b':
                retval.append("\\b");
                break;
            case '\t':
                retval.append("\\t");
                break;
            case '\n':
                retval.append("\\n");
                break;
            case '\f':
                retval.append("\\f");
                break;
            case '\r':
                retval.append("\\r");
                break;
            case '\"':
                retval.append("\\\"");
                break;
            case '\'':
                retval.append("\\'");
                break;
            case '\\':
                retval.append("\\\\");
                break;
            default:
                if (ch < 0x20 || ch > 0x7e) {
                    String str = "0000" + Integer.toString(ch, 16);
                    retval.append("\\u").append(str.substring(str.length() - 4));
                } else {
                    retval.append(ch);
                }
                break;
        }
    }
}
