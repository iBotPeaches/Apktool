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

import brut.androlib.exceptions.AndrolibException;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
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
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.Logger;

public final class ResXmlUtils {
    private static final Logger LOGGER = Logger.getLogger(ResXmlUtils.class.getName());

    private static final String FEATURE_LOAD_DTD = "http://apache.org/xml/features/nonvalidating/load-external-dtd";
    private static final String FEATURE_DISABLE_DOCTYPE_DECL = "http://apache.org/xml/features/disallow-doctype-decl";

    private ResXmlUtils() {
        // Private constructor for utility class
    }

    /**
     * Removes "debug" tag from file
     *
     * @param file AndroidManifest file
     * @throws AndrolibException Error reading Manifest file
     */
    public static void removeApplicationDebugTag(File file) throws AndrolibException {
        try {
            Document doc = loadDocument(file);
            Node application = doc.getElementsByTagName("application").item(0);

            // load attr
            NamedNodeMap attr = application.getAttributes();
            Node debugAttr = attr.getNamedItem("android:debuggable");

            // remove application:debuggable
            if (debugAttr != null) {
                attr.removeNamedItem("android:debuggable");
            }

            saveDocument(file, doc);
        } catch (SAXException | ParserConfigurationException | IOException | TransformerException ignored) {
        }
    }

    /**
     * Sets "debug" tag in the file to true
     *
     * @param file AndroidManifest file
     */
    public static void setApplicationDebugTagTrue(File file) {
        try {
            Document doc = loadDocument(file);
            Node application = doc.getElementsByTagName("application").item(0);

            // load attr
            NamedNodeMap attr = application.getAttributes();
            Node debugAttr = attr.getNamedItem("android:debuggable");

            if (debugAttr == null) {
                debugAttr = doc.createAttribute("android:debuggable");
                attr.setNamedItem(debugAttr);
            }

            // set application:debuggable to 'true
            debugAttr.setNodeValue("true");

            saveDocument(file, doc);
        } catch (SAXException | ParserConfigurationException | IOException | TransformerException ignored) {
        }
    }

    /**
     * Sets the value of the network security config in the AndroidManifest file
     *
     * @param file AndroidManifest file
     */
    public static void setNetworkSecurityConfig(File file) {
        try {
            Document doc = loadDocument(file);
            Node application = doc.getElementsByTagName("application").item(0);

            // load attr
            NamedNodeMap attr = application.getAttributes();
            Node netSecConfAttr = attr.getNamedItem("android:networkSecurityConfig");

            if (netSecConfAttr == null) {
                // there is not an already existing network security configuration
                netSecConfAttr = doc.createAttribute("android:networkSecurityConfig");
                attr.setNamedItem(netSecConfAttr);
            }

            // whether it already existed or it was created now set it to the proper value
            netSecConfAttr.setNodeValue("@xml/network_security_config");

            saveDocument(file, doc);
        } catch (SAXException | ParserConfigurationException | IOException | TransformerException ignored) {
        }
    }

    /**
     * Creates a modified network security config file that is more permissive
     *
     * @param file network security config file
     * @throws TransformerException XML file could not be edited
     * @throws IOException XML file could not be located
     * @throws SAXException XML file could not be read
     * @throws ParserConfigurationException XML nodes could be written
     */
    public static void modNetworkSecurityConfig(File file)
            throws ParserConfigurationException, TransformerException, IOException, SAXException {
        DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder();
        Document document;
        if (file.exists()) {
            document = documentBuilder.parse(file);
            document.getDocumentElement().normalize();
        } else {
            document = documentBuilder.newDocument();
        }

        Element root = (Element) document.getElementsByTagName("network-security-config").item(0);
        if (root == null) {
            root = document.createElement("network-security-config");
            document.appendChild(root);
        }

        Element baseConfig = (Element) document.getElementsByTagName("base-config").item(0);
        if (baseConfig == null) {
            baseConfig = document.createElement("base-config");
            root.appendChild(baseConfig);
        }

        Element trustAnchors = (Element) document.getElementsByTagName("trust-anchors").item(0);
        if (trustAnchors == null) {
            trustAnchors = document.createElement("trust-anchors");
            baseConfig.appendChild(trustAnchors);
        }

        NodeList certificates = document.getElementsByTagName("certificates");
        boolean hasSystemCert = false;
        boolean hasUserCert = false;
        for (int i = 0; i < certificates.getLength(); i++) {
            Element cert = (Element) certificates.item(i);
            String src = cert.getAttribute("src");
            if ("system".equals(src)) {
                hasSystemCert = true;
            } else if ("user".equals(src)) {
                hasUserCert = true;
            }
        }

        if (!hasSystemCert) {
            Element certSystem = document.createElement("certificates");
            certSystem.setAttribute("src", "system");
            trustAnchors.appendChild(certSystem);
        }

        if (!hasUserCert) {
            Element certUser = document.createElement("certificates");
            certUser.setAttribute("src", "user");
            trustAnchors.appendChild(certUser);
        }

        saveDocument(file, document);
    }

    /**
     * Any @string reference in a provider value in AndroidManifest.xml will break on
     * build, thus preventing the application from installing. This is from a bug/error
     * in AOSP where public resources cannot be part of an authorities attribute within
     * a provider tag.
     * <p>
     * This finds any reference and replaces it with the literal value found in the
     * res/values/strings.xml file.
     *
     * @param file File for AndroidManifest.xml
     */
    public static void fixingPublicAttrsInProviderAttributes(File file) {
        try {
            Document doc = loadDocument(file);
            XPath xPath = XPathFactory.newInstance().newXPath();
            XPathExpression expression = xPath.compile("/manifest/application/provider");

            Object result = expression.evaluate(doc, XPathConstants.NODESET);
            NodeList nodes = (NodeList) result;

            boolean saved = false;

            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                NamedNodeMap attrs = node.getAttributes();
                Node provider = attrs.getNamedItem("android:authorities");

                if (provider != null) {
                    saved = isSaved(file, saved, provider);
                }
            }

            // android:scheme
            xPath = XPathFactory.newInstance().newXPath();
            expression = xPath.compile("/manifest/application/activity/intent-filter/data");

            result = expression.evaluate(doc, XPathConstants.NODESET);
            nodes = (NodeList) result;

            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                NamedNodeMap attrs = node.getAttributes();
                Node provider = attrs.getNamedItem("android:scheme");

                if (provider != null) {
                    saved = isSaved(file, saved, provider);
                }
            }

            if (saved) {
                saveDocument(file, doc);
            }
        } catch (SAXException | ParserConfigurationException | IOException
                | XPathExpressionException | TransformerException ignored) {
        }
    }

    /**
     * Checks if the replacement was properly made to a node.
     *
     * @param file File we are searching for value
     * @param saved boolean on whether we need to save
     * @param provider Node we are attempting to replace
     * @return boolean
     */
    private static boolean isSaved(File file, boolean saved, Node provider) {
        String reference = provider.getNodeValue();
        String replacement = pullValueFromStrings(file.getParentFile(), reference);

        if (replacement != null) {
            provider.setNodeValue(replacement);
            saved = true;
        }
        return saved;
    }

    /**
     * Finds key in strings.xml file and returns text value
     *
     * @param apkDir Root directory of apk
     * @param key String reference (ie @string/foo)
     * @return String|null
     */
    public static String pullValueFromStrings(File apkDir, String key) {
        if (key == null || ! key.contains("@")) {
            return null;
        }

        File file = new File(apkDir, "/res/values/strings.xml");
        key = key.replace("@string/", "");

        if (!file.exists()) {
            return null;
        }
        try {
            Document doc = loadDocument(file);
            XPath xPath = XPathFactory.newInstance().newXPath();
            XPathExpression expression = xPath.compile("/resources/string[@name=\"" + key + "\"]/text()");

            Object result = expression.evaluate(doc, XPathConstants.STRING);
            return result != null ? (String) result : null;
        } catch (SAXException | ParserConfigurationException | IOException | XPathExpressionException ignored) {
            return null;
        }
    }

    /**
     * Finds key in integers.xml file and returns text value
     *
     * @param apkDir Root directory of apk
     * @param key Integer reference (ie @integer/foo)
     * @return String|null
     */
    public static String pullValueFromIntegers(File apkDir, String key) {
        if (key == null || ! key.contains("@")) {
            return null;
        }

        File file = new File(apkDir, "/res/values/integers.xml");
        key = key.replace("@integer/", "");

        if (!file.exists()) {
            return null;
        }
        try {
            Document doc = loadDocument(file);
            XPath xPath = XPathFactory.newInstance().newXPath();
            XPathExpression expression = xPath.compile("/resources/integer[@name=\"" + key + "\"]/text()");

            Object result = expression.evaluate(doc, XPathConstants.STRING);
            return result != null ? (String) result : null;

        } catch (SAXException | ParserConfigurationException | IOException | XPathExpressionException ignored) {
            return null;
        }
    }

    /**
     * Removes attributes like "versionCode" and "versionName" from file.
     *
     * @param file File representing AndroidManifest.xml
     */
    public static void removeManifestVersions(File file) {
        try {
            Document doc = loadDocument(file);
            Node manifest = doc.getFirstChild();
            NamedNodeMap attr = manifest.getAttributes();
            Node vCode = attr.getNamedItem("android:versionCode");
            Node vName = attr.getNamedItem("android:versionName");

            if (vCode != null) {
                attr.removeNamedItem("android:versionCode");
            }
            if (vName != null) {
                attr.removeNamedItem("android:versionName");
            }
            saveDocument(file, doc);

        } catch (SAXException | ParserConfigurationException | IOException | TransformerException ignored) {
        }
    }

    /**
     * Replaces package value with passed packageOriginal string
     *
     * @param file File for AndroidManifest.xml
     * @param packageOriginal Package name to replace
     */
    public static void renameManifestPackage(File file, String packageOriginal) {
        try {
            Document doc = loadDocument(file);

            // Get the manifest line
            Node manifest = doc.getFirstChild();

            // update package attribute
            NamedNodeMap attr = manifest.getAttributes();
            Node nodeAttr = attr.getNamedItem("package");
            nodeAttr.setNodeValue(packageOriginal);
            saveDocument(file, doc);
        } catch (SAXException | ParserConfigurationException | IOException | TransformerException ignored) {
        }
    }

    /**
     * Finds all feature flags set on permissions in AndroidManifest.xml.
     *
     * @param file File for AndroidManifest.xml
     */
    public static List<String> pullManifestFeatureFlags(File file) {
        try {
            Document doc = loadDocument(file);
            XPath xPath = XPathFactory.newInstance().newXPath();
            XPathExpression expression = xPath.compile("/manifest/permission");

            Object result = expression.evaluate(doc, XPathConstants.NODESET);
            NodeList nodes = (NodeList) result;

            List<String> featureFlags = new ArrayList<>();

            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                NamedNodeMap attrs = node.getAttributes();
                Node featureFlagAttr = attrs.getNamedItem("android:featureFlag");

                if (featureFlagAttr != null) {
                    featureFlags.add(featureFlagAttr.getNodeValue());
                }
            }

            return featureFlags;
        } catch (SAXException | ParserConfigurationException | IOException | XPathExpressionException ignored) {
            return null;
        }
    }

    /**
     *
     * @param file File to load into Document
     * @return Document
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     */
    public static Document loadDocument(File file)
            throws IOException, SAXException, ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(FEATURE_DISABLE_DOCTYPE_DECL, true);
        factory.setFeature(FEATURE_LOAD_DTD, false);

        try {
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, " ");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, " ");
        } catch (IllegalArgumentException ex) {
            LOGGER.warning("JAXP 1.5 Support is required to validate XML");
        }

        DocumentBuilder builder = factory.newDocumentBuilder();
        // Not using the parse(File) method on purpose, so that we can control when
        // to close it. Somehow parse(File) does not seem to close the file in all cases.
        try (InputStream in = Files.newInputStream(file.toPath())) {
            return builder.parse(in);
        }
    }

    /**
     *
     * @param file File to save Document to (ie AndroidManifest.xml)
     * @param doc Document being saved
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException
     * @throws TransformerException
     */
    private static void saveDocument(File file, Document doc)
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
}
