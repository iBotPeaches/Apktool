/**
 *  Copyright (C) 2017 Ryszard Wiśniewski <brut.alll@gmail.com>
 *  Copyright (C) 2017 Connor Tumbleson <connor.tumbleson@gmail.com>
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
package brut.androlib;

import brut.directory.ExtFile;
import brut.common.BrutException;
import brut.util.OS;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;

/**
 * @author Connor Tumbleson <connor.tumbleson@gmail.com>
 */
public class MinifiedArscTest {

    @BeforeClass
    public static void beforeClass() throws Exception {
        TestUtils.cleanFrameworkFile();
        sTmpDir = new ExtFile(OS.createTempDirectory());
        TestUtils.copyResourceDir(MinifiedArscTest.class, "brut/apktool/issue1157/", sTmpDir);

        String apk = "issue1157.apk";
        sDecodedDir = new ExtFile(sTmpDir, "issue1157");

        // decode issue1157.apk
        ApkDecoder apkDecoder = new ApkDecoder(new ExtFile(sTmpDir, apk));
        apkDecoder.setOutDir(sDecodedDir);

        // this should not raise an exception:
        apkDecoder.decode();
    }

    @AfterClass
    public static void afterClass() throws BrutException {
        OS.rmdir(sTmpDir);
    }

    @Test
    public void checkIfMinifiedArscLayoutFileMatchesTest() throws IOException {
        String expected = TestUtils.replaceNewlines("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<PreferenceScreen\n" +
                "    xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                "    xmlns:app=\"http://schemas.android.com/apk/res/com.ibotpeaches.issue1157\"\n" +
                "    android:max=\"100\" app:bar=\"1.0\"\n" +
                "/>");

        byte[] encoded = Files.readAllBytes(Paths.get(sDecodedDir + File.separator + "res" + File.separator + "xml" + File.separator + "custom.xml"));
        String obtained = TestUtils.replaceNewlines(new String(encoded));
        assertEquals(expected, obtained);
    }

    private static ExtFile sDecodedDir;
    private static ExtFile sTmpDir;
}