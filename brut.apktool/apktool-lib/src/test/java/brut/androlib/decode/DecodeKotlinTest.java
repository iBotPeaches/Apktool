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
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertTrue;

/**
 * @author Connor Tumbleson <connor.tumbleson@gmail.com>
 */
public class DecodeKotlinTest extends BaseTest {

    @BeforeClass
    public static void beforeClass() throws Exception {
        TestUtils.cleanFrameworkFile();
        sTmpDir = new ExtFile(OS.createTempDirectory());
        TestUtils.copyResourceDir(DecodeKotlinTest.class, "decode/testkotlin/", sTmpDir);

        String apk = "testkotlin.apk";

        // decode testkotlin.apk
        ApkDecoder apkDecoder = new ApkDecoder(new File(sTmpDir + File.separator + apk));
        sTestNewDir = new ExtFile(sTmpDir + File.separator + apk + ".out");

        apkDecoder.setOutDir(new File(sTmpDir + File.separator + apk + ".out"));
        apkDecoder.decode();
    }

    @AfterClass
    public static void afterClass() throws BrutException {
        OS.rmdir(sTmpDir);
    }

    @Test
    public void kotlinFolderExistsTest() {
        assertTrue(sTestNewDir.isDirectory());

        File testKotlinFolder = new File(sTestNewDir, "kotlin");
        assertTrue(testKotlinFolder.isDirectory());
    }

    @Test
    public void kotlinDecodeTest() throws IOException {
        File kotlinActivity = new File(sTestNewDir, "smali/org/example/kotlin/mixed/KotlinActivity.smali");

        assertTrue(FileUtils.readFileToString(kotlinActivity).contains("KotlinActivity.kt"));
    }
}