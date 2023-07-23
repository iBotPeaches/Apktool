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
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import static org.junit.Assert.assertTrue;

public class DebuggableTrueAddedTest extends BaseTest {

    @BeforeClass
    public static void beforeClass() throws Exception {
        TestUtils.cleanFrameworkFile();
        sTmpDir = new ExtFile(OS.createTempDirectory());
        sTestOrigDir = new ExtFile(sTmpDir, "issue2328-debuggable-missing-orig");
        sTestNewDir = new ExtFile(sTmpDir, "issue2328-debuggable-missing-new");
        LOGGER.info("Unpacking issue2328-debuggable-missing...");
        TestUtils.copyResourceDir(DebuggableTrueAddedTest.class, "aapt2/issue2328/debuggable-missing", sTestOrigDir);

        LOGGER.info("Building issue2328-debuggable-missing.apk...");
        Config config = Config.getDefaultConfig();
        config.debugMode = true;
        config.useAapt2 = true;
        config.verbose = true;

        File testApk = new File(sTmpDir, "issue2328-debuggable-missing.apk");
        new ApkBuilder(config, sTestOrigDir).build(testApk);

        LOGGER.info("Decoding issue2328-debuggable-missing.apk...");
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
    public void DebugIsTruePriorToBeingFalseTest() throws IOException, SAXException {
        String apk = "issue2328-debuggable-missing-new";

        String expected = TestUtils.replaceNewlines("<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"no\"?>" +
            "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\" " +
            "package=\"com.ibotpeaches.issue2328\" platformBuildVersionCode=\"20\" " +
            "platformBuildVersionName=\"4.4W.2-1537038\">    <application android:debuggable=\"true\"/></manifest>");

        byte[] encoded = Files.readAllBytes(Paths.get(sTmpDir + File.separator + apk + File.separator + "AndroidManifest.xml"));
        String obtained = TestUtils.replaceNewlines(new String(encoded));

        XMLUnit.setIgnoreWhitespace(true);
        XMLUnit.setIgnoreAttributeOrder(true);
        XMLUnit.setCompareUnmatched(false);

        assertXMLEqual(expected, obtained);
    }
}
