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
import brut.androlib.meta.SdkInfo;
import brut.androlib.meta.VersionInfo;
import brut.util.IOUtils;
import brut.xml.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.nio.file.Files;
import java.nio.file.Path;

public final class ResXmlUtils {
    public static final String ANDROID_RES_NS = "http://schemas.android.com/apk/res/android";
    public static final String ANDROID_RES_NS_AUTO = "http://schemas.android.com/apk/res-auto";

    private ResXmlUtils() {}

    public static void injectUsesSdkTag(Path file, SdkInfo sdkInfo) throws AndrolibException {
        try {
            Document doc = XmlUtils.loadDocument(file);
            Element root = doc.getDocumentElement();
            boolean changed = false;

            Element usesSdk = XmlUtils.getFirstChildElement(root, "uses-sdk");
            if (usesSdk == null) {
                usesSdk = doc.createElement("uses-sdk");
                root.insertBefore(usesSdk, root.getFirstChild());
                root.insertBefore(doc.createTextNode("\n    "), usesSdk);
                changed = true;
            }

            String minSdkVersion = sdkInfo.getMinSdkVersion();
            if (minSdkVersion != null && !usesSdk.getAttribute("android:minSdkVersion").equals(minSdkVersion)) {
                usesSdk.setAttribute("android:minSdkVersion", minSdkVersion);
                changed = true;
            }

            String targetSdkVersion = sdkInfo.getTargetSdkVersion();
            if (targetSdkVersion != null
                    && !usesSdk.getAttribute("android:targetSdkVersion").equals(targetSdkVersion)) {
                usesSdk.setAttribute("android:targetSdkVersion", targetSdkVersion);
                changed = true;
            }

            String maxSdkVersion = sdkInfo.getMaxSdkVersion();
            if (maxSdkVersion != null && !usesSdk.getAttribute("android:maxSdkVersion").equals(maxSdkVersion)) {
                usesSdk.setAttribute("android:maxSdkVersion", maxSdkVersion);
                changed = true;
            }

            if (changed) {
                XmlUtils.saveDocument(doc, file);
            }
        } catch (Exception ex) {
            throw new AndrolibException(ex);
        }
    }

    public static void injectVersionAttributes(Path file, VersionInfo versionInfo) throws AndrolibException {
        try {
            Document doc = XmlUtils.loadDocument(file);
            Element root = doc.getDocumentElement();
            boolean changed = false;

            int versionCode = versionInfo.getVersionCode();
            if (versionCode >= 0 && !root.getAttribute("android:versionCode").equals(Integer.toString(versionCode))) {
                root.setAttribute("android:versionCode", Integer.toString(versionCode));
                changed = true;
            }

            String versionName = versionInfo.getVersionName();
            if (versionName != null && !root.getAttribute("android:versionName").equals(versionName)) {
                root.setAttribute("android:versionName", versionName);
                changed = true;
            }

            if (changed) {
                XmlUtils.saveDocument(doc, file);
            }
        } catch (Exception ex) {
            throw new AndrolibException(ex);
        }
    }

    public static void injectDebuggableAttribute(Path file) throws AndrolibException {
        try {
            Document doc = XmlUtils.loadDocument(file);
            Element root = doc.getDocumentElement();
            Element application = XmlUtils.getFirstChildElement(root, "application");
            boolean changed = false;

            if (!application.getAttribute("android:debuggable").equals("true")) {
                application.setAttribute("android:debuggable", "true");
                changed = true;
            }

            if (changed) {
                XmlUtils.saveDocument(doc, file);
            }
        } catch (Exception ex) {
            throw new AndrolibException(ex);
        }
    }

    public static void injectNetworkSecurityConfig(Path file, Path apkDir) throws AndrolibException {
        try {
            injectNetworkSecurityConfigAttribute(file);

            Path netSecConf = apkDir.resolve("res/xml/network_security_config.xml");
            IOUtils.createParentDirectories(netSecConf);
            injectNetworkSecurityConfigXml(netSecConf);
        } catch (AndrolibException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AndrolibException(ex);
        }
    }

    private static void injectNetworkSecurityConfigAttribute(Path file) throws AndrolibException {
        try {
            Document doc = XmlUtils.loadDocument(file);
            Element root = doc.getDocumentElement();
            Element application = XmlUtils.getFirstChildElement(root, "application");
            boolean changed = false;

            if (!application.getAttribute("android:networkSecurityConfig").equals("@xml/network_security_config")) {
                application.setAttribute("android:networkSecurityConfig", "@xml/network_security_config");
                changed = true;
            }

            if (changed) {
                XmlUtils.saveDocument(doc, file);
            }
        } catch (Exception ex) {
            throw new AndrolibException(ex);
        }
    }

    private static void injectNetworkSecurityConfigXml(Path file) throws AndrolibException {
        try {
            Document doc;
            if (Files.exists(file)) {
                doc = XmlUtils.loadDocument(file);
                doc.getDocumentElement().normalize();
            } else {
                doc = XmlUtils.newDocument();
            }
            boolean changed = false;

            Element root = doc.getDocumentElement();
            if (root == null || !root.getTagName().equals("network-security-config")) {
                if (root != null) {
                    doc.removeChild(root);
                }
                root = doc.createElement("network-security-config");
                doc.appendChild(root);
                changed = true;
            }

            Element baseConfig = XmlUtils.getFirstChildElement(root, "base-config");
            if (baseConfig == null) {
                baseConfig = doc.createElement("base-config");
                root.appendChild(baseConfig);
                changed = true;
            }

            Element trustAnchors = XmlUtils.getFirstChildElement(baseConfig, "trust-anchors");
            if (trustAnchors == null) {
                trustAnchors = doc.createElement("trust-anchors");
                baseConfig.appendChild(trustAnchors);
                changed = true;
            }

            boolean hasSystemCert = false;
            boolean hasUserCert = false;
            for (Element cert : XmlUtils.getChildElements(trustAnchors, "certificates")) {
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
        } catch (Exception ex) {
            throw new AndrolibException(ex);
        }
    }

    public static void replaceReferencesInAttributes(Path file, Path apkDir) throws AndrolibException {
        try {
            Document doc = XmlUtils.loadDocument(file, true);
            boolean changed = false;

            String expression = String.join(" | ",
                "/manifest/application/provider/@android:authorities",
                "/manifest/application/activity/intent-filter/data/@android:scheme");
            NodeList nodes = XmlUtils.evaluateXPath(doc, expression, NodeList.class);

            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                String value = pullValueFromStrings(apkDir, node.getNodeValue());
                if (value != null) {
                    node.setNodeValue(value);
                    changed = true;
                }
            }

            if (changed) {
                XmlUtils.saveDocument(doc, file);
            }
        } catch (AndrolibException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AndrolibException(ex);
        }
    }

    public static String pullValueFromStrings(Path apkDir, String key) throws AndrolibException {
        return pullValueFromXml(apkDir.resolve("res/values/strings.xml"), "string", key);
    }

    public static String pullValueFromIntegers(Path apkDir, String key) throws AndrolibException {
        return pullValueFromXml(apkDir.resolve("res/values/integers.xml"), "integer", key);
    }

    private static String pullValueFromXml(Path file, String type, String key) throws AndrolibException {
        if (!Files.isRegularFile(file) || key == null || key.indexOf('@') == -1) {
            return null;
        }

        key = key.replace("@" + type + "/", "");
        try {
            Document doc = XmlUtils.loadDocument(file);
            String expression = String.format("/resources/%s[@name='%s']/text()", type, key);

            return XmlUtils.evaluateXPath(doc, expression, String.class);
        } catch (Exception ex) {
            throw new AndrolibException(ex);
        }
    }
}
