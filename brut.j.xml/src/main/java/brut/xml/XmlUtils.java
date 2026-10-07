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
package brut.xml;

import brut.common.Log;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Predicate;

public final class XmlUtils {
    public static final String XML_PROLOG = "<?xml version=\"1.0\" encoding=\"utf-8\"?>";
    public static final String XML_PREFIX = "xml";
    public static final String XML_URI = "http://www.w3.org/XML/1998/namespace";
    public static final String XMLNS_PREFIX = "xmlns";
    public static final String XMLNS_URI = "http://www.w3.org/2000/xmlns/";

    private static final String FEATURE_DISALLOW_DOCTYPE_DECL =
        "http://apache.org/xml/features/disallow-doctype-decl";
    private static final String FEATURE_LOAD_EXTERNAL_DTD =
        "http://apache.org/xml/features/nonvalidating/load-external-dtd";

    private XmlUtils() {}

    private static DocumentBuilder newDocumentBuilder(boolean nsAware)
            throws SAXException, ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(nsAware);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature(FEATURE_DISALLOW_DOCTYPE_DECL, true);
        factory.setFeature(FEATURE_LOAD_EXTERNAL_DTD, false);

        try {
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        } catch (IllegalArgumentException ignored) {
            Log.w(Log.ROOT, "JAXP 1.5 Support is required to validate XML");
        }

        return factory.newDocumentBuilder();
    }

    public static Document newDocument() throws SAXException, ParserConfigurationException {
        return newDocument(false);
    }

    public static Document newDocument(boolean nsAware) throws SAXException, ParserConfigurationException {
        return newDocumentBuilder(nsAware).newDocument();
    }

    public static Document parseDocument(String xml) throws IOException, SAXException, ParserConfigurationException {
        return parseDocument(xml, false);
    }

    public static Document parseDocument(String xml, boolean nsAware)
            throws IOException, SAXException, ParserConfigurationException {
        Objects.requireNonNull(xml, "xml");
        DocumentBuilder builder = newDocumentBuilder(nsAware);
        StringReader reader = new StringReader(xml);
        return builder.parse(new InputSource(reader));
    }

    public static Document loadDocument(Path path) throws IOException, SAXException, ParserConfigurationException {
        return loadDocument(path, false);
    }

    public static Document loadDocument(Path path, boolean nsAware)
            throws IOException, SAXException, ParserConfigurationException {
        Objects.requireNonNull(path, "path");
        DocumentBuilder builder = newDocumentBuilder(nsAware);
        try (InputStream in = Files.newInputStream(path)) {
            return builder.parse(new InputSource(in));
        }
    }

    public static void saveDocument(Document doc, Path path)
            throws IOException, SAXException, ParserConfigurationException, TransformerException {
        Objects.requireNonNull(doc, "doc");
        Objects.requireNonNull(path, "path");
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

        byte[] xmlDecl = XML_PROLOG.getBytes(StandardCharsets.US_ASCII);
        byte[] newLine = System.lineSeparator().getBytes(StandardCharsets.US_ASCII);

        try (OutputStream out = Files.newOutputStream(path)) {
            out.write(xmlDecl);
            out.write(newLine);
            transformer.transform(new DOMSource(doc), new StreamResult(out));
            out.write(newLine);
        }
    }

    public static Iterable<Element> getChildElements(Element parent) {
        return getChildElements(parent, element -> true);
    }

    public static Iterable<Element> getChildElements(Element parent, String name) {
        Objects.requireNonNull(name, "name");
        return getChildElements(parent, element -> name.equals(element.getTagName()));
    }

    public static Iterable<Element> getChildElements(Element parent, String ns, String name) {
        Objects.requireNonNull(name, "name");
        String normalizedNs = normalizeNamespace(ns);
        return getChildElements(parent, element ->
            Objects.equals(normalizedNs, normalizeNamespace(element.getNamespaceURI()))
                && name.equals(element.getLocalName()));
    }

    public static Iterable<Element> getChildElements(Element parent, Predicate<Element> filter) {
        Objects.requireNonNull(parent, "parent");
        Objects.requireNonNull(filter, "filter");
        return () -> new Iterator<Element>() {
            private Node current = parent.getFirstChild();
            private Element next = null;

            @Override
            public boolean hasNext() {
                while (next == null && current != null) {
                    Node node = current;
                    current = node.getNextSibling();
                    if (node.getNodeType() == Node.ELEMENT_NODE) {
                        Element element = (Element) node;
                        if (filter.test(element)) {
                            next = element;
                        }
                    }
                }
                return next != null;
            }

            @Override
            public Element next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                Element element = next;
                next = null;
                return element;
            }
        };
    }

    public static Element getFirstChildElement(Element parent) {
        Iterator<Element> it = getChildElements(parent).iterator();
        return it.hasNext() ? it.next() : null;
    }

    public static Element getFirstChildElement(Element parent, String name) {
        Iterator<Element> it = getChildElements(parent, name).iterator();
        return it.hasNext() ? it.next() : null;
    }

    public static Element getFirstChildElement(Element parent, String ns, String name) {
        Iterator<Element> it = getChildElements(parent, ns, name).iterator();
        return it.hasNext() ? it.next() : null;
    }

    public static Element getFirstChildElement(Element parent, Predicate<Element> filter) {
        Iterator<Element> it = getChildElements(parent, filter).iterator();
        return it.hasNext() ? it.next() : null;
    }

    /**
     * Some parsers may return an empty string when a namespace is unsupported, which can confuse serializers.
     * This method normalizes empty strings to be null.
     */
    public static String normalizeNamespace(String namespace) {
        return (namespace != null && !namespace.isEmpty()) ? namespace : null;
    }

    @SuppressWarnings("unchecked")
    public static <T> T evaluateXPath(Document doc, String expression, Class<T> returnType)
            throws XPathExpressionException {
        Objects.requireNonNull(doc, "doc");
        Objects.requireNonNull(expression, "expression");
        Objects.requireNonNull(returnType, "returnType");
        QName type;
        if (returnType == Node.class) {
            type = XPathConstants.NODE;
        } else if (returnType == NodeList.class) {
            type = XPathConstants.NODESET;
        } else if (returnType == String.class) {
            type = XPathConstants.STRING;
        } else if (returnType == Double.class) {
            type = XPathConstants.NUMBER;
        } else if (returnType == Boolean.class) {
            type = XPathConstants.BOOLEAN;
        } else {
            throw new IllegalArgumentException("Unexpected return type: " + returnType.getName());
        }

        XPath xPath = XPathFactory.newInstance().newXPath();
        xPath.setNamespaceContext(new NamespaceContext() {
            @Override
            public String getNamespaceURI(String prefix) {
                return doc.lookupNamespaceURI(prefix);
            }

            @Override
            public String getPrefix(String namespaceURI) {
                return doc.lookupPrefix(namespaceURI);
            }

            @Override
            public Iterator<String> getPrefixes(String namespaceURI) {
                String prefix = getPrefix(namespaceURI);
                return prefix != null ? Collections.singleton(prefix).iterator() : Collections.emptyIterator();
            }
        });

        return (T) xPath.evaluate(expression, doc, type);
    }
}
