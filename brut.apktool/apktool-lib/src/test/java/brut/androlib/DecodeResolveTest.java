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

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.*;
import static org.junit.Assert.*;

public class DecodeResolveTest extends BaseTest {
    private static final String TEST_APK = "issue2836.apk";

    @BeforeClass
    public static void beforeClass() throws Exception {
        copyResourceDir(DecodeResolveTest.class, "issue2836", sTmpDir);
    }

    @Test
    public void decodeResolveDefaultTest() throws Exception {
        sConfig.setDecodeResolve(Config.DecodeResolve.DEFAULT);

        Path testApk = sTmpDir.resolve(TEST_APK);
        Path testDir = sTmpDir.resolve(testApk.getFileName() + ".out.default");
        new ApkDecoder(testApk, sConfig).decode(testDir);

        assertTrue(Files.isRegularFile(testDir.resolve("res/values/strings.xml")));

        Document attrDocument = XmlUtils.loadDocument(testDir.resolve("res/values/attrs.xml"));
        assertEquals(4, attrDocument.getElementsByTagName("enum").getLength());

        Document colorDocument = XmlUtils.loadDocument(testDir.resolve("res/values/colors.xml"));
        assertEquals(8, colorDocument.getElementsByTagName("color").getLength());

        Document publicDocument = XmlUtils.loadDocument(testDir.resolve("res/values/public.xml"));
        assertEquals(22, publicDocument.getElementsByTagName("public").getLength());
    }

    @Test
    public void decodeResolveGreedyTest() throws Exception {
        sConfig.setDecodeResolve(Config.DecodeResolve.GREEDY);

        Path testApk = sTmpDir.resolve(TEST_APK);
        Path testDir = sTmpDir.resolve(testApk.getFileName() + ".out.greedy");
        new ApkDecoder(testApk, sConfig).decode(testDir);

        assertTrue(Files.isRegularFile(testDir.resolve("res/values/strings.xml")));

        Path attrXml = testDir.resolve("res/values/attrs.xml");
        Document attrDocument = XmlUtils.loadDocument(attrXml);
        assertEquals(4, attrDocument.getElementsByTagName("enum").getLength());

        Path colorXml = testDir.resolve("res/values/colors.xml");
        Document colorDocument = XmlUtils.loadDocument(colorXml);
        assertEquals(9, colorDocument.getElementsByTagName("color").getLength());

        Path publicXml = testDir.resolve("res/values/public.xml");
        Document publicDocument = XmlUtils.loadDocument(publicXml);
        assertEquals(23, publicDocument.getElementsByTagName("public").getLength());
    }

    @Test
    public void decodeResolveLazyTest() throws Exception {
        sConfig.setDecodeResolve(Config.DecodeResolve.LAZY);

        Path testApk = sTmpDir.resolve(TEST_APK);
        Path testDir = sTmpDir.resolve(testApk.getFileName() + ".out.lazy");
        new ApkDecoder(testApk, sConfig).decode(testDir);

        assertTrue(Files.isRegularFile(testDir.resolve("res/values/strings.xml")));

        Path attrXml = testDir.resolve("res/values/attrs.xml");
        Document attrDocument = XmlUtils.loadDocument(attrXml);
        assertEquals(3, attrDocument.getElementsByTagName("enum").getLength());

        Path colorXml = testDir.resolve("res/values/colors.xml");
        Document colorDocument = XmlUtils.loadDocument(colorXml);
        assertEquals(8, colorDocument.getElementsByTagName("color").getLength());

        Path publicXml = testDir.resolve("res/values/public.xml");
        Document publicDocument = XmlUtils.loadDocument(publicXml);
        assertEquals(21, publicDocument.getElementsByTagName("public").getLength());
    }
}
