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

import brut.androlib.*;
import brut.directory.ExtFile;
import brut.common.BrutException;
import brut.util.OS;
import java.io.File;
import java.io.IOException;

import org.junit.*;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;

import static org.junit.Assert.*;

public class LargeCompactResourceTest extends BaseTest {

    @BeforeClass
    public static void beforeClass() throws Exception {
        TestUtils.cleanFrameworkFile();
        sTmpDir = new ExtFile(OS.createTempDirectory());
        TestUtils.copyResourceDir(CompactResourceTest.class, "decode/issue3705/", sTmpDir);
    }

    @AfterClass
    public static void afterClass() throws BrutException {
        OS.rmdir(sTmpDir);
    }

    @Test
    public void checkIfDecodeSucceeds() throws BrutException, IOException, ParserConfigurationException, SAXException {
        String apk = "issue3705.apk";
        ExtFile testApk = new ExtFile(sTmpDir, apk);

        // decode issue3705.apk
        ApkDecoder apkDecoder = new ApkDecoder(testApk);
        sTestOrigDir = new ExtFile(sTmpDir + File.separator + apk + ".out");

        File outDir = new File(sTmpDir + File.separator + apk + ".out");
        apkDecoder.decode(outDir);

        Document doc = loadDocument(new File(sTestOrigDir + "/res/values/strings.xml"));
        assertFalse(resourceNameContains(doc.getDocumentElement(), "APKTOOL"));

        Config config = Config.getDefaultConfig();
        LOGGER.info("Building issue3705.apk...");
        new ApkBuilder(sTestOrigDir, config).build(testApk);
    }
}
