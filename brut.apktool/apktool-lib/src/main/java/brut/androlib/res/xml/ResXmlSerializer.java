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
package brut.androlib.res.xml;

import brut.util.TextUtils;
import brut.xml.XmlUtils;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.logging.Logger;

public class ResXmlSerializer implements XmlSerializer {
    private static final Logger LOGGER = Logger.getLogger(ResXmlSerializer.class.getName());

    private static final String E_NOT_SUPPORTED = "Method is not supported.";

    private static final int BUFFER_SIZE = 8192;

    private final boolean mAutoEscape;
    private final char[] mBuffer;
    private boolean[] mIndent;
    private String[] mElementStack;
    private int[] mNamespaceCounts;
    private String[] mNamespaceStack;

    private Writer mWriter;
    private int mDepth;
    private boolean mPending;
    private int mAutoNamespace;
    private int mBufferIndex;

    public ResXmlSerializer(boolean autoEscape) {
        mAutoEscape = autoEscape;
        mBuffer = new char[BUFFER_SIZE];
        mIndent = new boolean[4];
        mElementStack = new String[12];
        mNamespaceCounts = new int[6];
        mNamespaceStack = new String[12];
    }

    // XmlSerializer

    @Override
    public void setFeature(String name, boolean state) {
        throw new IllegalStateException(E_NOT_SUPPORTED);
    }

    @Override
    public boolean getFeature(String name) {
        return false;
    }

    @Override
    public void setProperty(String name, Object value) {
        throw new IllegalStateException(E_NOT_SUPPORTED);
    }

    @Override
    public Object getProperty(String name) {
        return null;
    }

    @Override
    public void setOutput(OutputStream os, String encoding) throws IOException {
        if (encoding != null && !encoding.equalsIgnoreCase(StandardCharsets.UTF_8.name())) {
            throw new UnsupportedOperationException();
        }
        setOutput(new OutputStreamWriter(os, StandardCharsets.UTF_8));
    }

    @Override
    public void setOutput(Writer writer) {
        mWriter = writer;
        mDepth = 0;
        mIndent[0] = true;
        mPending = false;
        mAutoNamespace = 0;
        mNamespaceCounts[0] = 3;
        mNamespaceCounts[1] = 3;
        mNamespaceStack[0] = "";
        mNamespaceStack[1] = "";
        mNamespaceStack[2] = XmlUtils.XML_PREFIX;
        mNamespaceStack[3] = XmlUtils.XML_URI;
        mNamespaceStack[4] = XmlUtils.XMLNS_PREFIX;
        mNamespaceStack[5] = XmlUtils.XMLNS_URI;
    }

    @Override
    public void startDocument(String encoding, Boolean standalone) throws IOException {
        write(XmlUtils.XML_PROLOG);
    }

    @Override
    public void endDocument() throws IOException {
        while (mDepth > 0) {
            endTag(mElementStack[mDepth * 3 - 3], mElementStack[mDepth * 3 - 1]);
        }
        write(System.lineSeparator());
        flush();
    }

    @Override
    public void setPrefix(String prefix, String namespace) throws IOException {
        check(false);
        if (prefix == null) {
            prefix = "";
        }
        if (namespace == null) {
            namespace = "";
        }

        // Ignore identical prefix definitions.
        if (prefix.equals(getPrefix(namespace, true, false))) {
            return;
        }

        int i = (mNamespaceCounts[mDepth + 1]++) << 1;
        if (mNamespaceStack.length < i + 1) {
            String[] newStack = new String[mNamespaceStack.length + 16];
            System.arraycopy(mNamespaceStack, 0, newStack, 0, i);
            mNamespaceStack = newStack;
        }

        mNamespaceStack[i++] = prefix;
        mNamespaceStack[i] = namespace;
    }

    @Override
    public String getPrefix(String namespace, boolean generatePrefix) {
        try {
            return getPrefix(namespace, false, generatePrefix);
        }
        catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private String getPrefix(String namespace, boolean includeDefault, boolean generatePrefix)
            throws IOException {
        for (int i = mNamespaceCounts[mDepth + 1] * 2 - 2; i >= 0; i -= 2) {
            if (mNamespaceStack[i + 1].equals(namespace)
                    && (includeDefault || !mNamespaceStack[i].isEmpty())) {
                String candidate = mNamespaceStack[i];
                for (int j = i + 2; j < mNamespaceCounts[mDepth + 1] * 2; j++) {
                    if (mNamespaceStack[j].equals(candidate)) {
                        candidate = null;
                        break;
                    }
                }
                if (candidate != null) {
                    return candidate;
                }
            }
        }

        if (!generatePrefix) {
            return null;
        }

        String prefix;
        if (namespace.isEmpty()) {
            prefix = "";
        } else {
            do {
                prefix = "n" + (mAutoNamespace++);
                for (int i = mNamespaceCounts[mDepth + 1] * 2 - 2; i >= 0; i -= 2) {
                    if (prefix.equals(mNamespaceStack[i])) {
                        prefix = null;
                        break;
                    }
                }
            }
            while (prefix == null);
        }

        boolean pending = mPending;
        mPending = false;
        setPrefix(prefix, namespace);
        mPending = pending;
        return prefix;
    }

    @Override
    public int getDepth() {
        return mPending ? mDepth + 1 : mDepth;
    }

    @Override
    public String getNamespace() {
        int depth = getDepth();
        return depth > 0 ? mElementStack[depth * 3 - 3] : null;
    }

    @Override
    public String getName() {
        int depth = getDepth();
        return depth > 0 ? mElementStack[depth * 3 - 1] : null;
    }

    @Override
    public XmlSerializer startTag(String namespace, String name) throws IOException {
        check(false);
        writeIndent();

        int i = mDepth * 3;
        if (mElementStack.length < i + 3) {
            String[] newStack = new String[mElementStack.length + 12];
            System.arraycopy(mElementStack, 0, newStack, 0, i);
            mElementStack = newStack;
        }

        String prefix = namespace != null ? getPrefix(namespace, true, true) : "";
        if (namespace != null && namespace.isEmpty()) {
            for (int j = mNamespaceCounts[mDepth]; j < mNamespaceCounts[mDepth + 1]; j++) {
                if (mNamespaceStack[j * 2].isEmpty() && !mNamespaceStack[j * 2 + 1].isEmpty()) {
                    throw new IllegalStateException(
                        "Could not set default namespace for elements in no namespace.");
                }
            }
        }

        mElementStack[i++] = namespace;
        mElementStack[i++] = prefix;
        mElementStack[i] = name;

        write('<');
        if (!prefix.isEmpty()) {
            write(prefix);
            write(':');
        }
        write(name);

        mIndent[mDepth] = true;
        mPending = true;
        return this;
    }

    @Override
    public XmlSerializer endTag(String namespace, String name) throws IOException {
        if (!mPending) {
            mDepth--;
        }
        if ((namespace == null && mElementStack[mDepth * 3] != null)
                || (namespace != null && !namespace.equals(mElementStack[mDepth * 3]))
                || !mElementStack[mDepth * 3 + 2].equals(name)) {
            throw new IllegalArgumentException(
                "</{" + namespace + "}" + name + "> does not match start.");
        }

        if (mPending) {
            check(true);
            mDepth--;
        } else {
            if (mIndent[mDepth + 1]) {
                writeIndent();
            }
            write("</");
            String prefix = mElementStack[mDepth * 3 + 1];
            if (!prefix.isEmpty()) {
                write(prefix);
                write(':');
            }
            write(name);
            write('>');
        }

        mNamespaceCounts[mDepth + 1] = mNamespaceCounts[mDepth];
        return this;
    }

    @Override
    public XmlSerializer attribute(String namespace, String name, String value) throws IOException {
        if (!mPending) {
            throw new IllegalStateException("Illegal position for attribute.");
        }
        if (namespace == null) {
            namespace = "";
        }

        write(' ');
        String prefix = !namespace.isEmpty() ? getPrefix(namespace, false, true) : "";
        if (!prefix.isEmpty()) {
            write(prefix);
            write(':');
        }
        write(name);
        write("=\"");
        if (mAutoEscape) {
            writeEscaped(value, true);
        } else {
            write(value);
        }
        write('"');
        return this;
    }

    @Override
    public XmlSerializer text(String text) throws IOException {
        check(false);
        mIndent[mDepth] = false;
        if (mAutoEscape) {
            writeEscaped(text, false);
        } else {
            write(text);
        }
        return this;
    }

    @Override
    public XmlSerializer text(char[] buf, int start, int len) throws IOException {
        return text(new String(buf, start, len));
    }

    @Override
    public void cdsect(String text) {
        throw new IllegalStateException(E_NOT_SUPPORTED);
    }

    @Override
    public void entityRef(String text) {
        throw new IllegalStateException(E_NOT_SUPPORTED);
    }

    @Override
    public void processingInstruction(String text) {
        throw new IllegalStateException(E_NOT_SUPPORTED);
    }

    @Override
    public void comment(String text) {
        throw new IllegalStateException(E_NOT_SUPPORTED);
    }

    @Override
    public void docdecl(String text) {
        throw new IllegalStateException(E_NOT_SUPPORTED);
    }

    @Override
    public void ignorableWhitespace(String text) {
        throw new IllegalStateException(E_NOT_SUPPORTED);
    }

    @Override
    public void flush() throws IOException {
        check(false);
        flushBuffer();
    }

    // Utility methods

    private void check(boolean close) throws IOException {
        if (!mPending) {
            return;
        }
        if (mIndent[mDepth] && mNamespaceCounts[mDepth] < mNamespaceCounts[mDepth + 1]) {
            writeIndent();
            write(' ');
        }

        mPending = false;
        mDepth++;

        if (mIndent.length <= mDepth) {
            boolean[] newIndent = new boolean[mDepth + 4];
            System.arraycopy(mIndent, 0, newIndent, 0, mDepth);
            mIndent = newIndent;
        }
        mIndent[mDepth] = mIndent[mDepth - 1];

        for (int i = mNamespaceCounts[mDepth - 1]; i < mNamespaceCounts[mDepth]; i++) {
            String prefix = mNamespaceStack[i * 2];
            String uri = mNamespaceStack[i * 2 + 1];
            write(" xmlns");
            if (!prefix.isEmpty()) {
                write(':');
                write(prefix);
            } else if (getNamespace().isEmpty() && !uri.isEmpty()) {
                throw new IllegalStateException(
                    "Could not set default namespace for elements in no namespace.");
            }
            write("=\"");
            if (mAutoEscape) {
                writeEscaped(uri, true);
            } else {
                write(uri);
            }
            write('"');
        }

        if (mNamespaceCounts.length <= mDepth + 1) {
            int[] newCounts = new int[mDepth + 8];
            System.arraycopy(mNamespaceCounts, 0, newCounts, 0, mDepth + 1);
            mNamespaceCounts = newCounts;
        }

        mNamespaceCounts[mDepth + 1] = mNamespaceCounts[mDepth];

        if (close) {
            write(" />");
        } else {
            write('>');
        }
    }

    private void flushBuffer() throws IOException {
        if (mBufferIndex > 0) {
            mWriter.write(mBuffer, 0, mBufferIndex);
            mWriter.flush();
            mBufferIndex = 0;
        }
    }

    private void writeIndent() throws IOException {
        write(System.lineSeparator());
        int len = mDepth * 4;
        while (len > 0) {
            if (mBufferIndex == BUFFER_SIZE) {
                flushBuffer();
            }
            int batch = BUFFER_SIZE - mBufferIndex;
            if (batch > len) {
                batch = len;
            }
            Arrays.fill(mBuffer, mBufferIndex, mBufferIndex + batch, ' ');
            len -= batch;
            mBufferIndex += batch;
        }
    }

    private void write(char ch) throws IOException {
        if (mBufferIndex >= BUFFER_SIZE) {
            flushBuffer();
        }
        mBuffer[mBufferIndex++] = ch;
    }

    private void write(String str) throws IOException {
        write(str, 0, str.length());
    }

    private void write(String str, int start, int len) throws IOException {
        while (len > 0) {
            if (mBufferIndex == BUFFER_SIZE) {
                flushBuffer();
            }
            int batch = BUFFER_SIZE - mBufferIndex;
            if (batch > len) {
                batch = len;
            }
            str.getChars(start, start + batch, mBuffer, mBufferIndex);
            start += batch;
            len -= batch;
            mBufferIndex += batch;
        }
    }

    /**
     * Only used to safely serialize manifest and resource XMLs.
     * Must be disabled when serializing values XMLs.
     */
    private void writeEscaped(String str, boolean attr) throws IOException {
        char ch = 0, prev = 0, prev2 = 0;
        for (int i = 0, n = str.length(); i < n; i++, prev2 = prev, prev = ch) {
            ch = str.charAt(i);
            if (ch == '\n') {
                if (attr) {
                    write("&#xA;");
                    continue;
                }
                // fallthrough
            } else if (ch == '\r') {
                if (attr) {
                    write("&#xD;");
                    continue;
                }
                // fallthrough
            } else if (ch == '\t') {
                if (attr) {
                    write("&#x9;");
                    continue;
                }
                // fallthrough
            } else if (ch == '"') {
                if (attr) {
                    write("&quot;");
                    continue;
                }
                // fallthrough
            } else if (ch == '&') {
                write("&amp;");
                continue;
            } else if (ch == '<') {
                write("&lt;");
                continue;
            } else if (ch == '>') {
                if (prev == ']' && prev2 == ']') {
                    write("&gt;");
                    continue;
                }
                // fallthrough
            } else if (TextUtils.isPrintableChar(ch)) {
                // fallthrough
            } else if (Character.isHighSurrogate(ch) && i < n - 1) {
                // Is this high surrogate followed by a valid low surrogate?
                char low = str.charAt(i + 1);
                if (Character.isLowSurrogate(low)) {
                    write(ch);
                    write(low);
                    i++;
                } else {
                    LOGGER.warning(
                        "Bad surrogate pair (U+" + Integer.toHexString((int) ch)
                            + " U+" + Integer.toHexString((int) low) + ")");
                }
                continue;
            } else {
                LOGGER.warning(
                    "Illegal character (U+" + Integer.toHexString((int) ch) + ")");
                continue;
            }
            write(ch);
        }
    }
}
