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
package brut.androlib.aapt2;

import brut.androlib.*;
import brut.common.BrutException;
import brut.directory.ExtFile;
import brut.util.OS;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.*;

public class NetworkConfigTest extends BaseTest {

    @BeforeClass
    public static void beforeClass() throws Exception {
        TestUtils.cleanFrameworkFile();

        sTmpDir = new ExtFile(OS.createTempDirectory());
        sTestOrigDir = new ExtFile(sTmpDir, "testapp-orig");
        sTestNewDir = new ExtFile(sTmpDir, "testapp-new");
        LOGGER.info("Unpacking testapp...");
        TestUtils.copyResourceDir(NetworkConfigTest.class, "aapt2/network_config/", sTestOrigDir);

        LOGGER.info("Building testapp.apk...");
        Config config = Config.getDefaultConfig();
        config.netSecConf = true;
        ExtFile testApk = new ExtFile(sTmpDir, "testapp.apk");
        new ApkBuilder(sTestOrigDir, config).build(testApk);

        LOGGER.info("Decoding testapp.apk...");
        ApkDecoder apkDecoder = new ApkDecoder(testApk);
        apkDecoder.decode(sTestNewDir);
    }

    @AfterClass
    public static void afterClass() throws BrutException {
        OS.rmdir(sTmpDir);
    }

    @Test
    public void buildAndDecodeTest() {
        assertTrue(sTestNewDir.isDirectory());
    }

    @Test
    public void netSecConfGeneric() throws IOException, SAXException, ParserConfigurationException, XPathExpressionException {
        LOGGER.info("Verifying network security configuration file contains user and system certificates...");

        byte[] encoded = Files.readAllBytes(Paths.get(String.valueOf(sTestNewDir), "res/xml/network_security_config.xml"));
        String obtained = new String(encoded);

        // Load the XML document
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(obtained.getBytes()));

        // XPath expression to check for user and system certificates
        XPath xPath = XPathFactory.newInstance().newXPath();

        // Check if 'system' certificate exists
        XPathExpression systemCertExpr = xPath.compile("//certificates[@src='system']");
        NodeList systemCertNodes = (NodeList) systemCertExpr.evaluate(doc, XPathConstants.NODESET);
        assertTrue(systemCertNodes.getLength() > 0);

        // Check if 'user' certificate exists
        XPathExpression userCertExpr = xPath.compile("//certificates[@src='user']");
        NodeList userCertNodes = (NodeList) userCertExpr.evaluate(doc, XPathConstants.NODESET);
        assertTrue(userCertNodes.getLength() > 0);
    }

    @Test
    public void netSecConfInManifest() throws IOException, ParserConfigurationException, SAXException {
        LOGGER.info("Validating network security config in Manifest...");
        Document doc = loadDocument(new File(sTestNewDir + "/AndroidManifest.xml"));
        Node application = doc.getElementsByTagName("application").item(0);
        NamedNodeMap attr = application.getAttributes();
        Node debugAttr = attr.getNamedItem("android:networkSecurityConfig");
        assertEquals("@xml/network_security_config", debugAttr.getNodeValue());
    }
}
