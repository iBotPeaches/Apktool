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
package brut.androlib;

import brut.common.BrutException;
import brut.directory.ExtFile;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.*;
import static org.junit.Assert.*;
import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;

public class NoNetworkConfigTest extends BaseTest {

    @BeforeClass
    public static void beforeClass() throws Exception {
        sTestOrigDir = new ExtFile(sTmpDir, "network_config-orig");
        sTestNewDir = new ExtFile(sTmpDir, "network_config-new");

        LOGGER.info("Unpacking network_config...");
        TestUtils.copyResourceDir(NoNetworkConfigTest.class, "network_config/none", sTestOrigDir);

        sConfig.setNetSecConf(true);

        LOGGER.info("Building network_config.apk...");
        ExtFile testApk = new ExtFile(sTmpDir, "network_config.apk");
        new ApkBuilder(sTestOrigDir, sConfig).build(testApk);

        LOGGER.info("Decoding network_config.apk...");
        new ApkDecoder(testApk, sConfig).decode(sTestNewDir);
    }

    @Test
    public void buildAndDecodeTest() {
        assertTrue(sTestNewDir.isDirectory());
    }

    @Test
    public void netSecConfGeneric() throws IOException, SAXException {
        LOGGER.info("Comparing network security configuration file...");

        String expected = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                + "<network-security-config>\n"
                + "    <base-config>\n"
                + "        <trust-anchors>\n"
                + "            <certificates src=\"system\"/>\n"
                + "            <certificates src=\"user\"/>\n"
                + "        </trust-anchors>\n"
                + "    </base-config>\n"
                + "</network-security-config>";

        File xml = new File(sTestNewDir, "res/xml/network_security_config.xml");
        String obtained = new String(Files.readAllBytes(xml.toPath()));

        assertXMLEqual(expected, obtained);
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
