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

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.*;
import static org.junit.Assert.*;
import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;

public class DebuggableTrueAddedTest extends BaseTest {

    @BeforeClass
    public static void beforeClass() throws Exception {
        sTestOrigDir = sTmpDir.resolve("issue2328-debuggable-missing-orig");
        sTestNewDir = sTmpDir.resolve("issue2328-debuggable-missing-new");

        log("Unpacking issue2328-debuggable-missing...");
        copyResourceDir(DebuggableTrueAddedTest.class, "issue2328/debuggable-missing", sTestOrigDir);

        sConfig.setDebuggable(true);
        sConfig.setVerbose(true);

        log("Building issue2328-debuggable-missing.apk...");
        Path testApk = sTmpDir.resolve("issue2328-debuggable-missing.apk");
        new ApkBuilder(sTestOrigDir, sConfig).build(testApk);

        log("Decoding issue2328-debuggable-missing.apk...");
        new ApkDecoder(testApk, sConfig).decode(sTestNewDir);
    }

    @Test
    public void buildAndDecodeTest() {
        assertTrue(Files.isDirectory(sTestNewDir));
    }

    @Test
    public void debugIsTruePriorToBeingFalseTest() throws Exception {
        String expected =
            "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
          + "<manifest package=\"com.ibotpeaches.issue2328\" platformBuildVersionCode=\"20\" platformBuildVersionName=\"4.4W.2-1537038\"\n"
          + "  xmlns:android=\"http://schemas.android.com/apk/res/android\">\n"
          + "    <application android:debuggable=\"true\"/>\n"
          + "</manifest>";

        String obtained = readTextFile(sTestNewDir.resolve("AndroidManifest.xml"));

        assertXMLEqual(expected, obtained);
    }
}
