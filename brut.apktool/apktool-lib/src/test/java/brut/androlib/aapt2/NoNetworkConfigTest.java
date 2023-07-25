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
import brut.androlib.Config;
import brut.common.BrutException;
import brut.directory.ExtFile;
import brut.util.OS;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class NoNetworkConfigTest extends BaseTest {

    @BeforeClass
    public static void beforeClass() throws Exception {
        TestUtils.cleanFrameworkFile();

        sTmpDir = new ExtFile(OS.createTempDirectory());
        sTestOrigDir = new ExtFile(sTmpDir, "testapp-orig");
        sTestNewDir = new ExtFile(sTmpDir, "testapp-new");
        LOGGER.info("Unpacking testapp...");
        TestUtils.copyResourceDir(NoNetworkConfigTest.class, "aapt2/testapp/", sTestOrigDir);

        LOGGER.info("Building testapp.apk...");
        Config config = Config.getDefaultConfig();
        config.netSecConf = true;
        config.useAapt2 = true;
        File testApk = new File(sTmpDir, "testapp.apk");
        new ApkBuilder(config, sTestOrigDir).build(testApk);

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
    public void netSecConfGeneric() throws IOException, SAXException {
        LOGGER.info("Comparing network security configuration file...");
        String expected = TestUtils.replaceNewlines("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>" +
            "<network-security-config><base-config><trust-anchors><certificates src=\"system\"/><certificates src=\"us" +
            "er\"/></trust-anchors></base-config></network-security-config>");

        byte[] encoded = Files.readAllBytes(Paths.get(String.valueOf(sTestNewDir), "res/xml/network_security_config.xml"));
        String obtained = TestUtils.replaceNewlines(new String(encoded));

        XMLUnit.setIgnoreWhitespace(true);
        XMLUnit.setIgnoreAttributeOrder(true);
        XMLUnit.setCompareUnmatched(false);

        assertXMLEqual(expected, obtained);
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
