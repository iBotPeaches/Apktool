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
import brut.xml.XmlUtils;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import java.io.*;
import java.util.*;
import java.util.logging.Logger;

public final class ResXmlUtils {
    private static final Logger LOGGER = Logger.getLogger(ResXmlUtils.class.getName());

    private ResXmlUtils() {
        // Private constructor for utility class
    }

    /**
     * Removes "debuggable" attribute for application.
     *
     * @param file File for AndroidManifest.xml
     */
    public static void removeApplicationDebugTag(File file) {
        try {
            Document doc = XmlUtils.loadDocument(file);
            Node application = doc.getElementsByTagName("application").item(0);
            NamedNodeMap attrs = application.getAttributes();
            boolean changed = false;

            Node debugAttr = attrs.getNamedItem("android:debuggable");
            if (debugAttr != null) {
                attrs.removeNamedItem("android:debuggable");
                changed = true;
            }

            if (changed) {
                XmlUtils.saveDocument(doc, file);
            }
        } catch (IOException | SAXException | ParserConfigurationException | TransformerException ignored) {
        }
    }

    /**
     * Sets "debuggable" attribute to true for application.
     *
     * @param file File for AndroidManifest.xml
     */
    public static void setApplicationDebugTagTrue(File file) {
        try {
            Document doc = XmlUtils.loadDocument(file);
            Node application = doc.getElementsByTagName("application").item(0);
            NamedNodeMap attrs = application.getAttributes();
            boolean changed = false;

            Node debugAttr = attrs.getNamedItem("android:debuggable");
            if (debugAttr == null) {
                debugAttr = doc.createAttribute("android:debuggable");
                debugAttr.setNodeValue("true");
                attrs.setNamedItem(debugAttr);
                changed = true;
            } else if (!debugAttr.getNodeValue().equals("true")) {
                debugAttr.setNodeValue("true");
                changed = true;
            }

            if (changed) {
                XmlUtils.saveDocument(doc, file);
            }
        } catch (IOException | SAXException | ParserConfigurationException | TransformerException ignored) {
        }
    }

    /**
     * Sets the network security config attribute for application.
     *
     * @param file File for AndroidManifest.xml
     */
    public static void setNetworkSecurityConfig(File file) {
        try {
            Document doc = XmlUtils.loadDocument(file);
            Node application = doc.getElementsByTagName("application").item(0);
            NamedNodeMap attrs = application.getAttributes();
            boolean changed = false;

            Node netSecConfAttr = attrs.getNamedItem("android:networkSecurityConfig");
            if (netSecConfAttr == null) {
                netSecConfAttr = doc.createAttribute("android:networkSecurityConfig");
                netSecConfAttr.setNodeValue("@xml/network_security_config");
                attrs.setNamedItem(netSecConfAttr);
                changed = true;
            } else if (!netSecConfAttr.getNodeValue().equals("@xml/network_security_config")) {
                netSecConfAttr.setNodeValue("@xml/network_security_config");
                changed = true;
            }

            if (changed) {
                XmlUtils.saveDocument(doc, file);
            }
        } catch (IOException | SAXException | ParserConfigurationException | TransformerException ignored) {
        }
    }

    /**
     * Modifies a network security config to be more permissive.
     *
     * @param file Network security config file
     */
    public static void modNetworkSecurityConfig(File file) {
        try {
            Document doc;
            if (file.exists()) {
                doc = XmlUtils.loadDocument(file);
                doc.getDocumentElement().normalize();
            } else {
                doc = XmlUtils.newDocument();
            }
            boolean changed = false;

            Element root = (Element) doc.getElementsByTagName("network-security-config").item(0);
            if (root == null) {
                root = doc.createElement("network-security-config");
                doc.appendChild(root);
                changed = true;
            }

            Element baseConfig = (Element) doc.getElementsByTagName("base-config").item(0);
            if (baseConfig == null) {
                baseConfig = doc.createElement("base-config");
                root.appendChild(baseConfig);
                changed = true;
            }

            Element trustAnchors = (Element) doc.getElementsByTagName("trust-anchors").item(0);
            if (trustAnchors == null) {
                trustAnchors = doc.createElement("trust-anchors");
                baseConfig.appendChild(trustAnchors);
                changed = true;
            }

            NodeList certificates = doc.getElementsByTagName("certificates");
            boolean hasSystemCert = false;
            boolean hasUserCert = false;
            for (int i = 0; i < certificates.getLength(); i++) {
                Element cert = (Element) certificates.item(i);
                String src = cert.getAttribute("src");
                if (src.equals("system")) {
                    hasSystemCert = true;
                } else if (src.equals("user")) {
                    hasUserCert = true;
                }
            }

            if (!hasSystemCert) {
                Element certSystem = doc.createElement("certificates");
                certSystem.setAttribute("src", "system");
                trustAnchors.appendChild(certSystem);
                changed = true;
            }

            if (!hasUserCert) {
                Element certUser = doc.createElement("certificates");
                certUser.setAttribute("src", "user");
                trustAnchors.appendChild(certUser);
                changed = true;
            }

            if (changed) {
                XmlUtils.saveDocument(doc, file);
            }
        } catch (IOException | SAXException | ParserConfigurationException | TransformerException ignored) {
        }
    }

    /**
     * Removes attributes like "versionCode" and "versionName" from file.
     *
     * @param file File for AndroidManifest.xml
     */
    public static void removeManifestVersions(File file) {
        try {
            Document doc = XmlUtils.loadDocument(file);
            Node manifest = doc.getFirstChild();
            NamedNodeMap attrs = manifest.getAttributes();
            boolean changed = false;

            Node versionCodeAttr = attrs.getNamedItem("android:versionCode");
            if (versionCodeAttr != null) {
                attrs.removeNamedItem("android:versionCode");
                changed = true;
            }

            Node versionNameAttr = attrs.getNamedItem("android:versionName");
            if (versionNameAttr != null) {
                attrs.removeNamedItem("android:versionName");
                changed = true;
            }

            if (changed) {
                XmlUtils.saveDocument(doc, file);
            }
        } catch (IOException | SAXException | ParserConfigurationException | TransformerException ignored) {
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
            Document doc = XmlUtils.loadDocument(file);
            Node manifest = doc.getFirstChild();
            NamedNodeMap attrs = manifest.getAttributes();
            boolean changed = false;

            Node packageAttr = attrs.getNamedItem("package");
            if (!packageAttr.getNodeValue().equals(packageOriginal)) {
                packageAttr.setNodeValue(packageOriginal);
                changed = true;
            }

            if (changed) {
                XmlUtils.saveDocument(doc, file);
            }
        } catch (IOException | SAXException | ParserConfigurationException | TransformerException ignored) {
        }
    }

    /**
     * Finds all feature flags set on permissions in AndroidManifest.xml.
     *
     * @param file File for AndroidManifest.xml
     * @return String[]|null
     */
    public static String[] pullManifestFeatureFlags(File file) {
        try {
            Document doc = XmlUtils.loadDocument(file, true);
            String expression = "/manifest//@android:featureFlag";
            NodeList nodes = XmlUtils.evaluateXPath(doc, expression, NodeList.class);

            String[] featureFlags = new String[nodes.getLength()];

            for (int i = 0; i < featureFlags.length; i++) {
                featureFlags[i] = nodes.item(i).getNodeValue();
            }

            return featureFlags;
        } catch (IOException | SAXException | ParserConfigurationException | XPathExpressionException ignored) {
            return null;
        }
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
            Document doc = XmlUtils.loadDocument(file, true);
            boolean changed = false;

            String expression = "/manifest/application/provider/@android:authorities";
            NodeList nodes = XmlUtils.evaluateXPath(doc, expression, NodeList.class);

            for (int i = 0; i < nodes.getLength(); i++) {
                if (replaceStringReference(file, nodes.item(i))) {
                    changed = true;
                }
            }

            expression = "/manifest/application/activity/intent-filter/data/@android:scheme";
            nodes = XmlUtils.evaluateXPath(doc, expression, NodeList.class);

            for (int i = 0; i < nodes.getLength(); i++) {
                if (replaceStringReference(file, nodes.item(i))) {
                    changed = true;
                }
            }

            if (changed) {
                XmlUtils.saveDocument(doc, file);
            }
        } catch (IOException | SAXException | ParserConfigurationException | XPathExpressionException
                | TransformerException ignored) {
        }
    }

    /**
     * Replaces a string reference in a node with the referenced string.
     * Returns true if the replacement was properly made to a node, false otherwise.
     *
     * @param file File we are searching for value
     * @param node Node with a string reference
     * @return boolean
     */
    private static boolean replaceStringReference(File file, Node node) {
        String replacement = pullValueFromStrings(file.getParentFile(), node.getNodeValue());
        if (replacement == null) {
            return false;
        }

        node.setNodeValue(replacement);
        return true;
    }

    /**
     * Finds key in strings.xml file and returns text value.
     *
     * @param apkDir Root directory of apk
     * @param key String reference (ie @string/foo)
     * @return String|null
     */
    public static String pullValueFromStrings(File apkDir, String key) {
        return pullValueFromXml(new File(apkDir, "res/values/strings.xml"), "string", key);
    }

    /**
     * Finds key in integers.xml file and returns text value.
     *
     * @param apkDir Root directory of apk
     * @param key Integer reference (ie @integer/foo)
     * @return String|null
     */
    public static String pullValueFromIntegers(File apkDir, String key) {
        return pullValueFromXml(new File(apkDir, "res/values/integers.xml"), "integer", key);
    }

    /**
     *
     * @param file File to pull the value from
     * @param type Resource type
     * @param key Resource reference
     * @return String|null
     */
    private static String pullValueFromXml(File file, String type, String key) {
        if (!file.isFile() || key == null || !key.contains("@")) {
            return null;
        }

        key = key.replace("@" + type + "/", "");
        try {
            Document doc = XmlUtils.loadDocument(file);
            String expression = String.format("/resources/%s[@name='%s']/text()", type, key);

            return XmlUtils.evaluateXPath(doc, expression, String.class);
        } catch (IOException | SAXException | ParserConfigurationException | XPathExpressionException ignored) {
            return null;
        }
    }
}
