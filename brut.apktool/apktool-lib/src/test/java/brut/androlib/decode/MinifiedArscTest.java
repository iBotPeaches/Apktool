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
package brut.androlib.decode;

import brut.androlib.ApkDecoder;
import brut.androlib.BaseTest;
import brut.androlib.TestUtils;
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
public class MinifiedArscTest extends BaseTest {

    @BeforeClass
    public static void beforeClass() throws Exception {
        TestUtils.cleanFrameworkFile();
        sTmpDir = new ExtFile(OS.createTempDirectory());
        TestUtils.copyResourceDir(MinifiedArscTest.class, "decode/issue1157/", sTmpDir);

        String apk = "issue1157.apk";
        sTestNewDir = new ExtFile(sTmpDir, "issue1157");

        // decode issue1157.apk
        ApkDecoder apkDecoder = new ApkDecoder(new ExtFile(sTmpDir, apk));
        apkDecoder.setForceDelete(true);
        apkDecoder.setOutDir(sTestNewDir);

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
                "<LinearLayout n1:orientation=\"vertical\" n1:layout_width=\"fill_parent\" n1:layout_height=\"fill_parent\"\n" +
                "  xmlns:n1=\"http://schemas.android.com/apk/res/android\">\n" +
                "    <com.ibotpeaches.issue1157.MyCustomView n1:max=\"100\" n2:default_value=\"1.0\" n2:max_value=\"5.0\" n2:min_value=\"0.2\" xmlns:n2=\"http://schemas.android.com/apk/res-auto\" />\n" +
                "</LinearLayout>");

        byte[] encoded = Files.readAllBytes(Paths.get(sTestNewDir + File.separator + "res" + File.separator + "xml" + File.separator + "custom.xml"));
        String obtained = TestUtils.replaceNewlines(new String(encoded));
        assertEquals(expected, obtained);
    }
}