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
package brut.androlib.decode;

import brut.androlib.ApkDecoder;
import brut.androlib.BaseTest;
import brut.androlib.Config;
import brut.androlib.TestUtils;
import brut.directory.ExtFile;
import brut.common.BrutException;
import brut.util.OS;
import java.io.File;
import java.io.IOException;

import org.junit.*;
import org.w3c.dom.Document;

import static org.junit.Assert.*;

public class ResourceModeTest extends BaseTest {

    @BeforeClass
    public static void beforeClass() throws Exception {
        TestUtils.cleanFrameworkFile();
        sTmpDir = new ExtFile(OS.createTempDirectory());
        TestUtils.copyResourceDir(ResourceModeTest.class, "decode/issue2836/", sTmpDir);
    }

    @AfterClass
    public static void afterClass() throws BrutException {
        OS.rmdir(sTmpDir);
    }

    @Test
    public void checkDecodingModeAsRemove() throws BrutException, IOException {
        String apk = "issue2836.apk";

        Config config = Config.getDefaultConfig();
        config.setDecodeResolveMode(Config.DECODE_RES_RESOLVE_REMOVE);

        // decode issue2836.apk
        ApkDecoder apkDecoder = new ApkDecoder(config, new File(sTmpDir + File.separator + apk));
        sTestOrigDir = new ExtFile(sTmpDir + File.separator + apk + "remove.out");

        File outDir = new File(sTmpDir + File.separator + apk + "remove.out");
        apkDecoder.decode(outDir);

        File stringsXml =  new File(sTestOrigDir,"res/values/strings.xml");
        assertTrue(stringsXml.isFile());

        File attrXml =  new File(sTestOrigDir,"res/values/attrs.xml");
        Document attrDocument = TestUtils.getDocumentFromFile(attrXml);
        assertEquals(3, attrDocument.getElementsByTagName("enum").getLength());

        File colorXml =  new File(sTestOrigDir,"res/values/colors.xml");
        Document colorDocument = TestUtils.getDocumentFromFile(colorXml);
        assertEquals(8, colorDocument.getElementsByTagName("color").getLength());
        assertEquals(0, colorDocument.getElementsByTagName("item").getLength());

        File publicXml =  new File(sTestOrigDir,"res/values/public.xml");
        Document publicDocument = TestUtils.getDocumentFromFile(publicXml);
        assertEquals(21, publicDocument.getElementsByTagName("public").getLength());
    }

    @Test
    public void checkDecodingModeAsDummies() throws BrutException, IOException {
        String apk = "issue2836.apk";

        Config config = Config.getDefaultConfig();
        config.setDecodeResolveMode(Config.DECODE_RES_RESOLVE_DUMMY);

        // decode issue2836.apk
        ApkDecoder apkDecoder = new ApkDecoder(config, new File(sTmpDir + File.separator + apk));
        sTestOrigDir = new ExtFile(sTmpDir + File.separator + apk + "dummies.out");

        File outDir = new File(sTmpDir + File.separator + apk + "dummies.out");
        apkDecoder.decode(outDir);

        File stringsXml =  new File(sTestOrigDir,"res/values/strings.xml");
        assertTrue(stringsXml.isFile());

        File attrXml =  new File(sTestOrigDir,"res/values/attrs.xml");
        Document attrDocument = TestUtils.getDocumentFromFile(attrXml);
        assertEquals(4, attrDocument.getElementsByTagName("enum").getLength());

        File colorXml =  new File(sTestOrigDir,"res/values/colors.xml");
        Document colorDocument = TestUtils.getDocumentFromFile(colorXml);
        assertEquals(8, colorDocument.getElementsByTagName("color").getLength());
        assertEquals(1, colorDocument.getElementsByTagName("item").getLength());

        File publicXml =  new File(sTestOrigDir,"res/values/public.xml");
        Document publicDocument = TestUtils.getDocumentFromFile(publicXml);
        assertEquals(22, publicDocument.getElementsByTagName("public").getLength());
    }

    @Test
    public void checkDecodingModeAsLeave() throws BrutException, IOException {
        String apk = "issue2836.apk";

        Config config = Config.getDefaultConfig();
        config.setDecodeResolveMode(Config.DECODE_RES_RESOLVE_RETAIN);

        // decode issue2836.apk
        ApkDecoder apkDecoder = new ApkDecoder(config, new File(sTmpDir + File.separator + apk));
        sTestOrigDir = new ExtFile(sTmpDir + File.separator + apk + "leave.out");

        File outDir = new File(sTmpDir + File.separator + apk + "leave.out");
        apkDecoder.decode(outDir);

        File stringsXml =  new File(sTestOrigDir,"res/values/strings.xml");
        assertTrue(stringsXml.isFile());

        File attrXml =  new File(sTestOrigDir,"res/values/attrs.xml");
        Document attrDocument = TestUtils.getDocumentFromFile(attrXml);
        assertEquals(4, attrDocument.getElementsByTagName("enum").getLength());

        File colorXml =  new File(sTestOrigDir,"res/values/colors.xml");
        Document colorDocument = TestUtils.getDocumentFromFile(colorXml);
        assertEquals(8, colorDocument.getElementsByTagName("color").getLength());
        assertEquals(0, colorDocument.getElementsByTagName("item").getLength());

        File publicXml =  new File(sTestOrigDir,"res/values/public.xml");
        Document publicDocument = TestUtils.getDocumentFromFile(publicXml);
        assertEquals(21, publicDocument.getElementsByTagName("public").getLength());
    }
}
