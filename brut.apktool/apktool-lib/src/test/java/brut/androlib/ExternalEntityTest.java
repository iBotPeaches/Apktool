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
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;

/**
 * @author Connor Tumbleson <connor.tumbleson@gmail.com>
 */
public class ExternalEntityTest {

    @BeforeClass
    public static void beforeClass() throws Exception {
        sOrigDir = new ExtFile(OS.createTempDirectory());
        TestUtils.copyResourceDir(ExternalEntityTest.class, "brut/apktool/doctype/", sOrigDir);

        LOGGER.info("Building doctype.apk...");
        File testApk = new File(sOrigDir, "doctype.apk");
        new Androlib().build(sOrigDir, testApk);

        LOGGER.info("Decoding doctype.apk...");
        ApkDecoder apkDecoder = new ApkDecoder(testApk);
        apkDecoder.setOutDir(new File(sOrigDir + File.separator + "output"));
        apkDecoder.decode();
    }

    @AfterClass
    public static void afterClass() throws BrutException {
        OS.rmdir(sOrigDir);
    }

    @Test
    public void doctypeTest() throws BrutException, IOException {

        String expected = TestUtils.replaceNewlines("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<manifest android:versionCode=\"1\" android:versionName=\"1.0\" hardwareAccelerated=\"true\" package=\"com.ibotpeaches.doctype\" platformBuildVersionCode=\"23\" platformBuildVersionName=\"6.0-2438415\"\n" +
                "  xmlns:android=\"http://schemas.android.com/apk/res/android\">\n" +
                "    <supports-screens android:anyDensity=\"true\" android:smallScreens=\"true\" android:normalScreens=\"true\" android:largeScreens=\"true\" android:resizeable=\"true\" android:xlargeScreens=\"true\" />\n" +
                "</manifest>");

        byte[] encoded = Files.readAllBytes(Paths.get(sOrigDir + File.separator + "output" + File.separator + "AndroidManifest.xml"));
        String obtained = TestUtils.replaceNewlines(new String(encoded));
        assertEquals(expected, obtained);
    }

    private static ExtFile sOrigDir;

    private final static Logger LOGGER = Logger.getLogger(ExternalEntityTest.class.getName());
}