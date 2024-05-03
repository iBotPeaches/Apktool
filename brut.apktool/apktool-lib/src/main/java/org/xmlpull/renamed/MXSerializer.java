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
package org.xmlpull.renamed;

import org.xmlpull.v1.XmlSerializer;

import java.io.*;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Implementation of XmlSerializer interface from XmlPull V1 API. This
 * implementation is optimized for performance and low memory footprint.
 *
 * <p>
 * Implemented features:
 * <ul>
 * <li>FEATURE_NAMES_INTERNED - when enabled all returned names (namespaces,
 * prefixes) will be interned and it is required that all names passed as
 * arguments MUST be interned
 * <li>FEATURE_SERIALIZER_ATTVALUE_USE_APOSTROPHE
 * </ul>
 * <p>
 * Implemented properties:
 * <ul>
 * <li>PROPERTY_SERIALIZER_INDENTATION
 * <li>PROPERTY_SERIALIZER_LINE_SEPARATOR
 * </ul>
 *
 */
public class MXSerializer implements XmlSerializer {
	protected final static String XML_URI = "http://www.w3.org/XML/1998/namespace";
	protected final static String XMLNS_URI = "http://www.w3.org/2000/xmlns/";
	private static final boolean TRACE_SIZING = false;
	private static final boolean TRACE_ESCAPING = false;

	protected final String FEATURE_SERIALIZER_ATTVALUE_USE_APOSTROPHE = "http://xmlpull.org/v1/doc/features.html#serializer-attvalue-use-apostrophe";
	protected final String FEATURE_NAMES_INTERNED = "http://xmlpull.org/v1/doc/features.html#names-interned";
	protected final String PROPERTY_SERIALIZER_INDENTATION = "http://xmlpull.org/v1/doc/properties.html#serializer-indentation";
	protected final String PROPERTY_SERIALIZER_LINE_SEPARATOR = "http://xmlpull.org/v1/doc/properties.html#serializer-line-separator";
	protected final static String PROPERTY_LOCATION = "http://xmlpull.org/v1/doc/properties.html#location";

	// properties/features
	protected boolean namesInterned;
	protected boolean attributeUseApostrophe;
	protected String indentationString = null; // " ";
	protected String lineSeparator = "\n";

	protected String location;
	protected Writer out;

	protected int autoDeclaredPrefixes;

	protected int depth = 0;

	// element stack
	protected String[] elNamespace = new String[2];
	protected String[] elName = new String[elNamespace.length];
	protected String[] elPrefix = new String[elNamespace.length];
	protected int[] elNamespaceCount = new int[elNamespace.length];

	// namespace stack
	protected int namespaceEnd = 0;
	protected String[] namespacePrefix = new String[8];
	protected String[] namespaceUri = new String[namespacePrefix.length];

	protected boolean finished;
	protected boolean pastRoot;
	protected boolean setPrefixCalled;
	protected boolean startTagIncomplete;

	protected boolean doIndent;
	protected boolean seenTag;

	protected boolean seenBracket;
	protected boolean seenBracketBracket;

	// buffer output if needed to write escaped String see text(String)
	private static final int BUF_LEN = Runtime.getRuntime().freeMemory() > 1000000L ? 8 * 1024 : 256;
	protected char[] buf = new char[BUF_LEN];

	protected static final String[] precomputedPrefixes;

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

	protected void reset() {
		location = null;
		out = null;
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

	protected void ensureElementsCapacity() {
		final int elStackSize = elName.length;
		final int newSize = (depth >= 7 ? 2 * depth : 8) + 2;

		if (TRACE_SIZING) {
			System.err.println(getClass().getName() + " elStackSize "
					+ elStackSize + " ==> " + newSize);
		}
		final boolean needsCopying = elStackSize > 0;
		String[] arr;
		// reuse arr local variable slot
		arr = new String[newSize];
		if (needsCopying)
			System.arraycopy(elName, 0, arr, 0, elStackSize);
		elName = arr;

		arr = new String[newSize];
		if (needsCopying)
			System.arraycopy(elPrefix, 0, arr, 0, elStackSize);
		elPrefix = arr;

		arr = new String[newSize];
		if (needsCopying)
			System.arraycopy(elNamespace, 0, arr, 0, elStackSize);
		elNamespace = arr;

		final int[] iarr = new int[newSize];
		if (needsCopying) {
			System.arraycopy(elNamespaceCount, 0, iarr, 0, elStackSize);
		} else {
			// special initialization
			iarr[0] = 0;
		}
		elNamespaceCount = iarr;
	}

	protected void ensureNamespacesCapacity() { // int size) {
		final int newSize = namespaceEnd > 7 ? 2 * namespaceEnd : 8;
		if (TRACE_SIZING) {
			System.err.println(getClass().getName() + " namespaceSize " + namespacePrefix.length + " ==> " + newSize);
		}
		final String[] newNamespacePrefix = new String[newSize];
		final String[] newNamespaceUri = new String[newSize];
		if (namespacePrefix != null) {
			System.arraycopy(namespacePrefix, 0, newNamespacePrefix, 0, namespaceEnd);
			System.arraycopy(namespaceUri, 0, newNamespaceUri, 0, namespaceEnd);
		}
		namespacePrefix = newNamespacePrefix;
		namespaceUri = newNamespaceUri;
	}

	@Override
	public void setFeature(String name, boolean state)
			throws IllegalArgumentException, IllegalStateException {
		if (name == null) {
			throw new IllegalArgumentException("feature name can not be null");
		}
		if (FEATURE_NAMES_INTERNED.equals(name)) {
			namesInterned = state;
		} else if (FEATURE_SERIALIZER_ATTVALUE_USE_APOSTROPHE.equals(name)) {
			attributeUseApostrophe = state;
		} else {
			throw new IllegalStateException("unsupported feature " + name);
		}
	}

	@Override
	public boolean getFeature(String name) throws IllegalArgumentException {
		if (name == null) {
			throw new IllegalArgumentException("feature name can not be null");
		}
		if (FEATURE_NAMES_INTERNED.equals(name)) {
			return namesInterned;
		} else if (FEATURE_SERIALIZER_ATTVALUE_USE_APOSTROPHE.equals(name)) {
			return attributeUseApostrophe;
		} else {
			return false;
		}
	}

	// precomputed variables to simplify writing indentation
	protected int offsetNewLine;
	protected int indentationJump;
	protected char[] indentationBuf;
	protected int maxIndentLevel;
	protected boolean writeLineSepartor; // should end-of-line be written
	protected boolean writeIndentation; // is indentation used?

	/**
	 * For maximum efficiency when writing indents the required output is
	 * pre-computed This is internal function that recomputes buffer after user
	 * requested chnages.
	 */
	protected void rebuildIndentationBuf() {
		if (!doIndent)
			return;
		final int maxIndent = 65; // hardcoded maximum indentation size in characters
		int bufSize = 0;
		offsetNewLine = 0;
		if (writeLineSepartor) {
			offsetNewLine = lineSeparator.length();
			bufSize += offsetNewLine;
		}
		maxIndentLevel = 0;
		if (writeIndentation) {
			indentationJump = indentationString.length();
			maxIndentLevel = maxIndent / indentationJump;
			bufSize += maxIndentLevel * indentationJump;
		}
		if (indentationBuf == null || indentationBuf.length < bufSize) {
			indentationBuf = new char[bufSize + 8];
		}
		int bufPos = 0;
		if (writeLineSepartor) {
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

	protected void writeIndent() throws IOException {
		final int start = writeLineSepartor ? 0 : offsetNewLine;
		final int level = Math.min(depth, maxIndentLevel);

		out.write(indentationBuf, start, ((level - 1) * indentationJump) + offsetNewLine);
	}

	@Override
	public void setProperty(String name, Object value)
			throws IllegalArgumentException, IllegalStateException {
		if (name == null) {
			throw new IllegalArgumentException("property name can not be null");
		}
        switch (name) {
            case PROPERTY_SERIALIZER_INDENTATION:
                indentationString = (String) value;
                break;
            case PROPERTY_SERIALIZER_LINE_SEPARATOR:
                lineSeparator = (String) value;
                break;
            case PROPERTY_LOCATION:
                location = (String) value;
                break;
            default:
                throw new IllegalStateException("unsupported property " + name);
        }
		writeLineSepartor = lineSeparator != null && lineSeparator.length() > 0;
		writeIndentation = indentationString != null
				&& indentationString.length() > 0;
		// optimize - do not write when nothing to write ...
		doIndent = indentationString != null
				&& (writeLineSepartor || writeIndentation);
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
            case PROPERTY_SERIALIZER_INDENTATION:
                return indentationString;
            case PROPERTY_SERIALIZER_LINE_SEPARATOR:
                return lineSeparator;
            case PROPERTY_LOCATION:
                return location;
            default:
                return null;
        }
	}

	private String getLocation() {
		return location != null ? " @" + location : "";
	}

	// this is special method that can be accessed directly to retrieve Writer
	// serializer is using
	public Writer getWriter() {
		return out;
	}

	@Override
	public void setOutput(Writer writer) {
		reset();
		out = writer;
	}

	@Override
	public void setOutput(OutputStream os, String encoding) throws IOException {
		if (os == null)
			throw new IllegalArgumentException("output stream can not be null");
		reset();
		if (encoding != null) {
			out = new OutputStreamWriter(os, encoding);
		} else {
			out = new OutputStreamWriter(os);
		}
	}

	@Override
	public void startDocument(String encoding, Boolean standalone)
			throws IOException {
		if (attributeUseApostrophe) {
			out.write("<?xml version='1.0'");
		} else {
			out.write("<?xml version=\"1.0\"");
		}
		if (encoding != null) {
			out.write(" encoding=");
			out.write(attributeUseApostrophe ? '\'' : '"');
			out.write(encoding);
			out.write(attributeUseApostrophe ? '\'' : '"');
		}
		if (standalone != null) {
			out.write(" standalone=");
			out.write(attributeUseApostrophe ? '\'' : '"');
			if (standalone) {
				out.write("yes");
			} else {
				out.write("no");
			}
			out.write(attributeUseApostrophe ? '\'' : '"');
		}
		out.write("?>");
	}

	@Override
	public void endDocument() throws IOException {
		// close all unclosed tag;
		while (depth > 0) {
			endTag(elNamespace[depth], elName[depth]);
		}
		finished = pastRoot = startTagIncomplete = true;
		out.flush();
	}

	@Override
	public void setPrefix(String prefix, String namespace) throws IOException {
		if (startTagIncomplete)
			closeStartTag();

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

	protected String lookupOrDeclarePrefix(String namespace) {
		return getPrefix(namespace, true);
	}

	@Override
	public String getPrefix(String namespace, boolean generatePrefix) {
		return getPrefix(namespace, generatePrefix, false);
	}

	protected String getPrefix(String namespace, boolean generatePrefix,
			boolean nonEmpty) {
		if (!namesInterned) {
			// when String is interned we can do much faster namespace stack lookups ...
			namespace = namespace.intern();
		} else if (checkNamesInterned) {
			checkInterning(namespace);
		}
		if (namespace == null) {
			throw new IllegalArgumentException("namespace must be not null" + getLocation());
		} else if (namespace.length() == 0) {
			throw new IllegalArgumentException("default namespace cannot have prefix" + getLocation());
		}

		// first check if namespace is already in scope
		for (int i = namespaceEnd - 1; i >= 0; --i) {
			if (namespace.equals(namespaceUri[i])) {
				final String prefix = namespacePrefix[i];
				if (nonEmpty && prefix.length() == 0) {
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

	private String generatePrefix(String namespace) {
        ++autoDeclaredPrefixes;
        // fast lookup uses table that was pre-initialized in static{} ....
        final String prefix = autoDeclaredPrefixes < precomputedPrefixes.length
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
	public XmlSerializer startTag(String namespace, String name)
			throws IOException {
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

		if (checkNamesInterned && namesInterned)
			checkInterning(namespace);

		elNamespace[depth] = (namesInterned || namespace == null) ? namespace : namespace.intern();
		if (checkNamesInterned && namesInterned)
			checkInterning(name);

		elName[depth] = (namesInterned || name == null) ? name : name.intern();
		if (out == null) {
			throw new IllegalStateException("setOutput() must called set before serialization can start");
		}
		out.write('<');
		if (namespace != null) {
			if (namespace.length() > 0) {
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
					prefix = lookupOrDeclarePrefix(namespace);
				}
				// make sure that default ("") namespace to not print ":"
				if (prefix.length() > 0) {
					elPrefix[depth] = prefix;
					out.write(prefix);
					out.write(':');
				} else {
					elPrefix[depth] = "";
				}
			} else {
				// make sure that default namespace can be declared
				for (int i = namespaceEnd - 1; i >= 0; --i) {
					if (namespacePrefix[i] == "") {
						final String uri = namespaceUri[i];
						if (uri == null) {
							setPrefix("", "");
						} else if (uri.length() > 0) {
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
		out.write(name);
		return this;
	}

	@Override
	public XmlSerializer attribute(String namespace, String name, String value)
			throws IOException {
		if (!startTagIncomplete) {
			throw new IllegalArgumentException("startTag() must be called before attribute()" + getLocation());
		}
		out.write(' ');
		if (namespace != null && namespace.length() > 0) {
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
			out.write(prefix);
			out.write(':');
		}
		out.write(name);
		out.write('=');
		out.write(attributeUseApostrophe ? '\'' : '"');
		writeAttributeValue(value, out);
		out.write(attributeUseApostrophe ? '\'' : '"');
		return this;
	}

	protected void closeStartTag() throws IOException {
		if (finished) {
			throw new IllegalArgumentException("trying to write past already finished output"
					+ getLocation());
		}
		if (seenBracket) {
			seenBracket = seenBracketBracket = false;
		}
		if (startTagIncomplete || setPrefixCalled) {
			if (setPrefixCalled) {
				throw new IllegalArgumentException("startTag() must be called immediately after setPrefix()"
						+ getLocation());
			}
			if (!startTagIncomplete) {
				throw new IllegalArgumentException("trying to close start tag that is not opened"
						+ getLocation());
			}

			// write all namespace declarations!
			writeNamespaceDeclarations();
			out.write('>');
			elNamespaceCount[depth] = namespaceEnd;
			startTagIncomplete = false;
		}
	}

	protected void writeNamespaceDeclarations() throws IOException {
        Set<String> uniqueNamespaces = new HashSet<>();
		for (int i = elNamespaceCount[depth - 1]; i < namespaceEnd; i++) {
            String prefix = namespacePrefix[i];
            String uri = namespaceUri[i];

            // Some applications as seen in #2664 have duplicated namespaces.
            // AOSP doesn't care, but the parser does. So we filter them out.
            if (uniqueNamespaces.contains(prefix + uri)) {
                continue;
            }

			if (doIndent && uri.length() > 40) {
				writeIndent();
				out.write(" ");
			}
			if (prefix != "") {
				out.write(" xmlns:");
				out.write(prefix);
				out.write('=');
			} else {
				out.write(" xmlns=");
			}
			out.write(attributeUseApostrophe ? '\'' : '"');

			// NOTE: escaping of namespace value the same way as attributes!!!!
			writeAttributeValue(uri, out);
			out.write(attributeUseApostrophe ? '\'' : '"');

            uniqueNamespaces.add(prefix + uri);
		}
	}

	@Override
	public XmlSerializer endTag(String namespace, String name)
			throws IOException {
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
			out.write(" />"); // space is added to make it easier to work in XHTML!!!
        } else {
			if (doIndent && seenTag) {
				writeIndent();
			}
			out.write("</");
			String startTagPrefix = elPrefix[depth];
			if (startTagPrefix.length() > 0) {
				out.write(startTagPrefix);
				out.write(':');
			}
			out.write(name);
			out.write('>');
        }
        --depth;
        namespaceEnd = elNamespaceCount[depth];
		startTagIncomplete = false;
		seenTag = true;
		return this;
	}

	@Override
	public XmlSerializer text(String text) throws IOException {
		if (startTagIncomplete || setPrefixCalled)
			closeStartTag();
		if (doIndent && seenTag)
			seenTag = false;
		writeElementContent(text, out);
		return this;
	}

	@Override
	public XmlSerializer text(char[] buf, int start, int len)
			throws IOException {
		if (startTagIncomplete || setPrefixCalled)
			closeStartTag();
		if (doIndent && seenTag)
			seenTag = false;
		writeElementContent(buf, start, len, out);
		return this;
	}

	@Override
	public void cdsect(String text) throws IOException {
		if (startTagIncomplete || setPrefixCalled || seenBracket)
			closeStartTag();
		if (doIndent && seenTag)
			seenTag = false;
		out.write("<![CDATA[");
		out.write(text); // escape?
		out.write("]]>");
	}

	@Override
	public void entityRef(String text) throws IOException {
		if (startTagIncomplete || setPrefixCalled || seenBracket)
			closeStartTag();
		if (doIndent && seenTag)
			seenTag = false;
		out.write('&');
		out.write(text); // escape?
		out.write(';');
	}

	@Override
	public void processingInstruction(String text) throws IOException {
		if (startTagIncomplete || setPrefixCalled || seenBracket)
			closeStartTag();
		if (doIndent && seenTag)
			seenTag = false;
		out.write("<?");
		out.write(text); // escape?
		out.write("?>");
	}

	@Override
	public void comment(String text) throws IOException {
		if (startTagIncomplete || setPrefixCalled || seenBracket)
			closeStartTag();
		if (doIndent && seenTag)
			seenTag = false;
		out.write("<!--");
		out.write(text); // escape?
		out.write("-->");
	}

	@Override
	public void docdecl(String text) throws IOException {
		if (startTagIncomplete || setPrefixCalled || seenBracket)
			closeStartTag();
		if (doIndent && seenTag)
			seenTag = false;
		out.write("<!DOCTYPE");
		out.write(text); // escape?
		out.write(">");
	}

	@Override
	public void ignorableWhitespace(String text) throws IOException {
		if (startTagIncomplete || setPrefixCalled || seenBracket)
			closeStartTag();
		if (doIndent && seenTag)
			seenTag = false;
		if (text.length() == 0) {
			throw new IllegalArgumentException("empty string is not allowed for ignorable whitespace" + getLocation());
		}
		out.write(text); // no escape?
	}

	@Override
	public void flush() throws IOException {
		if (!finished && startTagIncomplete)
			closeStartTag();
		out.flush();
	}

	// --- utility methods

	protected void writeAttributeValue(String value, Writer out)
			throws IOException {
		// .[apostrophe and <, & escaped],
		final char quot = attributeUseApostrophe ? '\'' : '"';
		final String quotEntity = attributeUseApostrophe ? "&apos;" : "&quot;";

		int pos = 0;
		for (int i = 0; i < value.length(); i++) {
			char ch = value.charAt(i);
			if (ch == '&') {
				if (i > pos)
					out.write(value.substring(pos, i));
				out.write("&amp;");
				pos = i + 1;
			}
			if (ch == '<') {
				if (i > pos)
					out.write(value.substring(pos, i));
				out.write("&lt;");
				pos = i + 1;
			} else if (ch == quot) {
				if (i > pos)
					out.write(value.substring(pos, i));
				out.write(quotEntity);
				pos = i + 1;
			} else if (ch < 32) {
				// in XML 1.0 only legal character are #x9 | #xA | #xD
				// and they must be escaped otherwise in attribute value they
				// are normalized to spaces
				if (ch == 13 || ch == 10 || ch == 9) {
					if (i > pos)
						out.write(value.substring(pos, i));
					out.write("&#");
					out.write(Integer.toString(ch));
					out.write(';');
					pos = i + 1;
				} else {
					if (TRACE_ESCAPING)
						System.err.println(getClass().getName() + " DEBUG ATTR value.len=" + value.length()
								+ " " + printable(value));

					throw new IllegalStateException(
							"character " + printable(ch) + " (" + Integer.toString(ch) + ") is not allowed in output"
									+ getLocation() + " (attr value="
									+ printable(value) + ")");
				}
			}
		}
		if (pos > 0) {
			out.write(value.substring(pos));
		} else {
			out.write(value); // this is shortcut to the most common case
		}
	}

	protected void writeElementContent(String text, Writer out)
			throws IOException {

		// For some reason, some non-empty, empty characters are surviving this far and getting filtered out
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
					    if (i > pos)
					        out.write(text.substring(pos, i));
					    out.write("&amp;");
					    pos = i + 1;
                    }
				} else if (ch == '<') {
					if (i > pos)
						out.write(text.substring(pos, i));
					out.write("&lt;");
					pos = i + 1;
				} else if (seenBracketBracket && ch == '>') {
					if (i > pos)
						out.write(text.substring(pos, i));
					out.write("&gt;");
					pos = i + 1;
				} else if (ch < 32) {
					// in XML 1.0 only legal character are #x9 | #xA | #xD
					if (ch == 9 || ch == 10 || ch == 13) {
						// pass through
					} else {
						if (TRACE_ESCAPING)
							System.err.println(getClass().getName() + " DEBUG TEXT value.len=" + text.length()
									+ " " + printable(text));
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
		if (pos > 0) {
			out.write(text.substring(pos));
		} else {
			out.write(text); // this is shortcut to the most common case
		}

	}

	protected void writeElementContent(char[] buf, int off, int len, Writer out)
			throws IOException {
		// escape '<', '&', ']]>'
		final int end = off + len;
		int pos = off;
		for (int i = off; i < end; i++) {
			final char ch = buf[i];
			if (ch == ']') {
				if (seenBracket) {
					seenBracketBracket = true;
				} else {
					seenBracket = true;
				}
			} else {
				if (ch == '&') {
					if (i > pos) {
						out.write(buf, pos, i - pos);
					}
					out.write("&amp;");
					pos = i + 1;
				} else if (ch == '<') {
					if (i > pos) {
						out.write(buf, pos, i - pos);
					}
					out.write("&lt;");
					pos = i + 1;

				} else if (seenBracketBracket && ch == '>') {
					if (i > pos) {
						out.write(buf, pos, i - pos);
					}
					out.write("&gt;");
					pos = i + 1;
				} else if (ch < 32) {
					// in XML 1.0 only legal character are #x9 | #xA | #xD
					if (ch == 9 || ch == 10 || ch == 13) {
						// pass through
					} else {
						if (TRACE_ESCAPING)
							System.err.println(getClass().getName() + " DEBUG TEXT value.len=" + len + " "
									+ printable(new String(buf, off, len)));
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
			out.write(buf, pos, end - pos);
		}
	}

	protected static String printable(String s) {
		if (s == null) {
			return "null";
		}
		StringBuffer retval = new StringBuffer(s.length() + 16);
		retval.append("'");
		for (int i = 0; i < s.length(); i++) {
			addPrintable(retval, s.charAt(i));
		}
		retval.append("'");
		return retval.toString();
	}

	protected static String printable(char ch) {
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
				final String ss = "0000" + Integer.toString(ch, 16);
				retval.append("\\u").append(ss.substring(ss.length() - 4));
			} else {
				retval.append(ch);
			}
		}
	}
}
