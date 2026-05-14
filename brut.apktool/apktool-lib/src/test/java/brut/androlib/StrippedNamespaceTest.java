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

public class StrippedNamespaceTest extends BaseTest {
    private static final String TEST_APK = "issue3533.apk";

    @BeforeClass
    public static void beforeClass() throws Exception {
        copyResourceDir(StrippedNamespaceTest.class, "issue3533", sTmpDir);
    }

    @Test
    public void checkAssignedNamespaceTest() throws Exception {
        File testApk = new File(sTmpDir, TEST_APK);
        File testDir = new File(testApk + ".out");
        new ApkDecoder(testApk, sConfig).decode(testDir);

        Document doc = XmlUtils.loadDocument(new File(testDir, "res/drawable/trap.xml"), true);
        String expression = "/selector/item/@test:is_obfuscated";
        Node node = XmlUtils.evaluateXPath(doc, expression, Node.class);
        assertNotNull(node);
    }
}
