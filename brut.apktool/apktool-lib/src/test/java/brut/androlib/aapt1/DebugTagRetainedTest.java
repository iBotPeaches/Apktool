/*
 *  Copyright (C) 2010 Ryszard Wiśniewski <brut.alll@gmail.com>
 *  Copyright (C) 2010 Connor Tumbleson <connor.tumbleson@gmail.com>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package brut.androlib.aapt1;

import brut.androlib.*;
import brut.directory.ExtFile;
import brut.common.BrutException;
import brut.util.OS;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;
import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;

public class DebugTagRetainedTest extends BaseTest {

    @BeforeClass
    public static void beforeClass() throws Exception {
        TestUtils.cleanFrameworkFile();
        sTmpDir = new ExtFile(OS.createTempDirectory());
        sTestOrigDir = new ExtFile(sTmpDir, "issue1235-orig");
        sTestNewDir = new ExtFile(sTmpDir, "issue1235-new");
        LOGGER.info("Unpacking issue1235...");
        TestUtils.copyResourceDir(DebugTagRetainedTest.class, "aapt1/issue1235/", sTestOrigDir);

        LOGGER.info("Building issue1235.apk...");
        ApkOptions apkOptions = new ApkOptions();
        apkOptions.debugMode = true;

        File testApk = new File(sTmpDir, "issue1235.apk");
        new Androlib(apkOptions).build(sTestOrigDir, testApk);

        LOGGER.info("Decoding issue1235.apk...");
        ApkDecoder apkDecoder = new ApkDecoder(testApk);
        apkDecoder.setOutDir(sTestNewDir);
        apkDecoder.decode();
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
    public void DebugIsTruePriorToBeingFalseTest() throws IOException, SAXException {
        String apk = "issue1235-new";

        String expected = TestUtils.replaceNewlines("<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"no\"?>" +
                "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\" android:compileSdkVersion=\"23\" " +
                "android:compileSdkVersionCodename=\"6.0-2438415\" package=\"com.ibotpeaches.issue1235\" platformBuildVersionCode=\"20\" " +
                "platformBuildVersionName=\"4.4W.2-1537038\">    <application android:debuggable=\"true\"/></manifest>");

        byte[] encoded = Files.readAllBytes(Paths.get(sTmpDir + File.separator + apk + File.separator + "AndroidManifest.xml"));
        String obtained = TestUtils.replaceNewlines(new String(encoded));

        XMLUnit.setIgnoreWhitespace(true);
        XMLUnit.setIgnoreAttributeOrder(true);
        XMLUnit.setCompareUnmatched(false);

        assertXMLEqual(expected, obtained);
    }
}