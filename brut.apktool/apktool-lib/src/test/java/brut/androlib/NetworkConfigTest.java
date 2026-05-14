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

import brut.xml.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.io.File;

import org.junit.*;
import static org.junit.Assert.*;

public class NetworkConfigTest extends BaseTest {

    @BeforeClass
    public static void beforeClass() throws Exception {
        sTestOrigDir = new File(sTmpDir, "network_config-orig");
        sTestNewDir = new File(sTmpDir, "network_config-new");

        log("Unpacking network_config...");
        copyResourceDir(NetworkConfigTest.class, "network_config/existing", sTestOrigDir);

        sConfig.setNetSecConf(true);

        log("Building network_config.apk...");
        File testApk = new File(sTmpDir, "network_config.apk");
        new ApkBuilder(sTestOrigDir, sConfig).build(testApk);

        log("Decoding network_config.apk...");
        new ApkDecoder(testApk, sConfig).decode(sTestNewDir);
    }

    @Test
    public void buildAndDecodeTest() {
        assertTrue(sTestNewDir.isDirectory());
    }

    @Test
    public void netSecConfGeneric() throws Exception {
        log("Verifying network security configuration file contains user and system certificates...");

        Document doc = XmlUtils.loadDocument(new File(sTestNewDir, "res/xml/network_security_config.xml"));

        // Check if 'system' certificate exists
        String systemCertExpr = "/network-security-config/base-config/trust-anchors/certificates[@src='system']";
        Node systemCertNode = XmlUtils.evaluateXPath(doc, systemCertExpr, Node.class);
        assertNotNull(systemCertNode);

        // Check if 'user' certificate exists
        String userCertExpr = "/network-security-config/base-config/trust-anchors/certificates[@src='user']";
        Node userCertNode = XmlUtils.evaluateXPath(doc, userCertExpr, Node.class);
        assertNotNull(userCertNode);
    }

    @Test
    public void netSecConfInManifest() throws Exception {
        log("Validating network security config in Manifest...");

        Document doc = XmlUtils.loadDocument(new File(sTestNewDir, "AndroidManifest.xml"), true);
        String expression = "/manifest/application/@android:networkSecurityConfig";
        String value = XmlUtils.evaluateXPath(doc, expression, String.class);
        assertEquals("@xml/network_security_config", value);
    }
}
