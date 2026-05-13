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
package brut.androlib.res.decoder;

import brut.androlib.BaseTest;
import brut.androlib.ApkDecoder;
import brut.androlib.Config;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;

import org.junit.*;
import static org.junit.Assert.*;

public class StrippedNsTest extends BaseTest {
    private static final String TEST_APK = "issue3533.apk";
    private static final String TEST_RES = "res/drawable/trap.xml";

    private static final byte[] XML_HEADER = {
        0x3C, // <
        0x3F, // ?
        0x78, // x
        0x6D, // m
        0x6C, // l
        0x20, // (empty)
    };

    @BeforeClass
    public static void beforeClass() throws Exception {
        copyResourceDir(StrippedNsTest.class, "issue3533", sTmpDir);
    }

    @Test
    public void assertAssignsCorrectNamespace() throws Exception {
        File testApk = new File(sTmpDir, TEST_APK);
        File testDir = new File(testApk + ".out.none");
        new ApkDecoder(testApk, sConfig).decode(testDir);

        File decodedXml = new File(testDir, TEST_RES);
        assertTrue(decodedXml.isFile());
        assertTrue(Arrays.equals(XML_HEADER, readHeaderOfFile(decodedXml, 6)));

        String content = new String(Files.readAllBytes(decodedXml.toPath()), "UTF-8");

        // Ensure the wiped namespace was decoded as "test" and not "android"
        assertTrue(content.contains("test:is_obfuscated"));
        assertFalse(content.contains("android:is_obfuscated"));
    }
}
