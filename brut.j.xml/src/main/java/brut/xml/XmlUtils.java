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

import org.w3c.dom.*;
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
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.Logger;

public final class XmlUtils {
    private static final Logger LOGGER = Logger.getLogger("");

    private static final String FEATURE_DISALLOW_DOCTYPE_DECL = "http://apache.org/xml/features/disallow-doctype-decl";
    private static final String FEATURE_LOAD_EXTERNAL_DTD = "http://apache.org/xml/features/nonvalidating/load-external-dtd";

    private XmlUtils() {
        // Private constructor for utility class
    }

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
        } catch (IllegalArgumentException ex) {
            LOGGER.warning("JAXP 1.5 Support is required to validate XML");
        }

        return factory.newDocumentBuilder();
    }

    public static Document newDocument() throws SAXException, ParserConfigurationException {
        return newDocument(false);
    }

    public static Document newDocument(boolean nsAware) throws SAXException, ParserConfigurationException {
        return newDocumentBuilder(nsAware).newDocument();
    }

    public static Document loadDocument(File file)
            throws IOException, SAXException, ParserConfigurationException {
        return loadDocument(file, false);
    }

    public static Document loadDocument(File file, boolean nsAware)
            throws IOException, SAXException, ParserConfigurationException {
        DocumentBuilder builder = newDocumentBuilder(nsAware);
        // Not using the parse(File) method on purpose, so that we can control when
        // to close it. Somehow parse(File) does not seem to close the file in all cases.
        try (InputStream in = Files.newInputStream(file.toPath())) {
            return builder.parse(in);
        }
    }

    public static void saveDocument(Document doc, File file)
            throws IOException, SAXException, ParserConfigurationException, TransformerException {
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

        byte[] xmlDecl = "<?xml version=\"1.0\" encoding=\"utf-8\"?>".getBytes(StandardCharsets.US_ASCII);
        byte[] newLine = System.getProperty("line.separator").getBytes(StandardCharsets.US_ASCII);

        try (OutputStream out = Files.newOutputStream(file.toPath())) {
            out.write(xmlDecl);
            out.write(newLine);
            transformer.transform(new DOMSource(doc), new StreamResult(out));
            out.write(newLine);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T evaluateXPath(Document doc, String expression, Class<T> returnType)
            throws XPathExpressionException {
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
                return prefix != null
                        ? Collections.singleton(prefix).iterator()
                        : Collections.emptyIterator();
            }
        });

        return (T) xPath.evaluate(expression, doc, type);
    }
}
