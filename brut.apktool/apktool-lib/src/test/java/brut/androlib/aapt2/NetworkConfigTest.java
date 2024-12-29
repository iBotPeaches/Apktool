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

import brut.androlib.ApkBuilder;
import brut.androlib.ApkDecoder;
import brut.androlib.BaseTest;
import brut.androlib.TestUtils;
import brut.common.BrutException;
import brut.directory.ExtFile;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;

import org.junit.*;
import static org.junit.Assert.*;

public class NetworkConfigTest extends BaseTest {

    @BeforeClass
    public static void beforeClass() throws Exception {
        sTestOrigDir = new ExtFile(sTmpDir, "testapp-orig");
        sTestNewDir = new ExtFile(sTmpDir, "testapp-new");

        LOGGER.info("Unpacking testapp...");
        TestUtils.copyResourceDir(NetworkConfigTest.class, "aapt2/network_config", sTestOrigDir);

        sConfig.setNetSecConf(true);

        LOGGER.info("Building testapp.apk...");
        ExtFile testApk = new ExtFile(sTmpDir, "testapp.apk");
        new ApkBuilder(sTestOrigDir, sConfig).build(testApk);

        LOGGER.info("Decoding testapp.apk...");
        new ApkDecoder(testApk, sConfig).decode(sTestNewDir);
    }

    @Test
    public void buildAndDecodeTest() {
        assertTrue(sTestNewDir.isDirectory());
    }

    @Test
    public void netSecConfGeneric() throws BrutException {
        LOGGER.info("Verifying network security configuration file contains user and system certificates...");

        // Load the XML document
        Document doc = loadDocument(new File(sTestNewDir, "res/xml/network_security_config.xml"));

        // Check if 'system' certificate exists
        String systemCertExpr = "//certificates[@src='system']";
        NodeList systemCertNodes = evaluateXPath(doc, systemCertExpr, NodeList.class);
        assertTrue(systemCertNodes.getLength() > 0);

        // Check if 'user' certificate exists
        String userCertExpr = "//certificates[@src='user']";
        NodeList userCertNodes = evaluateXPath(doc, userCertExpr, NodeList.class);
        assertTrue(userCertNodes.getLength() > 0);
    }

    @Test
    public void netSecConfInManifest() throws BrutException {
        LOGGER.info("Validating network security config in Manifest...");

        // Load the XML document
        Document doc = loadDocument(new File(sTestNewDir, "AndroidManifest.xml"));

        // Check if network security config attribute is set correctly
        Node application = doc.getElementsByTagName("application").item(0);
        NamedNodeMap attrs = application.getAttributes();
        Node netSecConfAttr = attrs.getNamedItem("android:networkSecurityConfig");
        assertEquals("@xml/network_security_config", netSecConfAttr.getNodeValue());
    }
}
