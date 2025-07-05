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
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.*;
import static org.junit.Assert.*;
import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;

public class ExternalEntityTest extends BaseTest {

    @BeforeClass
    public static void beforeClass() throws Exception {
        sTestOrigDir = new ExtFile(sTmpDir, "doctype-orig");
        sTestNewDir = new ExtFile(sTmpDir, "doctype-new");

        LOGGER.info("Unpacking doctype...");
        TestUtils.copyResourceDir(ExternalEntityTest.class, "doctype", sTestOrigDir);

        LOGGER.info("Building doctype.apk...");
        ExtFile testApk = new ExtFile(sTmpDir, "doctype.apk");
        new ApkBuilder(sTestOrigDir, sConfig).build(testApk);

        LOGGER.info("Decoding doctype.apk...");
        new ApkDecoder(testApk, sConfig).decode(sTestNewDir);
    }

    @Test
    public void doctypeTest() throws IOException, SAXException {
        String expected = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                + "<manifest hardwareAccelerated=\"true\" package=\"com.ibotpeaches.doctype\" platformBuildVersionCode=\"24\" platformBuildVersionName=\"6.0-2456767\"\n"
                + "  xmlns:android=\"http://schemas.android.com/apk/res/android\">\n"
                + "    <supports-screens android:anyDensity=\"true\" android:smallScreens=\"true\" android:normalScreens=\"true\" android:largeScreens=\"true\" android:resizeable=\"true\" android:xlargeScreens=\"true\" />\n"
                + "</manifest>";

        File xml = new File(sTestNewDir, "AndroidManifest.xml");
        String obtained = new String(Files.readAllBytes(xml.toPath()));

        assertXMLEqual(expected, obtained);
    }
}
