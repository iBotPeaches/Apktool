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

import brut.androlib.ApkDecoder;
import brut.androlib.BaseTest;
import brut.androlib.TestUtils;
import brut.directory.ExtFile;
import brut.common.BrutException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.*;
import static org.junit.Assert.*;

public class DecodeKotlinTest extends BaseTest {

    @BeforeClass
    public static void beforeClass() throws Exception {
        TestUtils.copyResourceDir(DecodeKotlinTest.class, "decode/testkotlin", sTmpDir);

        ExtFile testApk = new ExtFile(sTmpDir, "testkotlin.apk");
        ExtFile testDir = new ExtFile(testApk + ".out");
        new ApkDecoder(testApk, sConfig).decode(testDir);
        sTestNewDir = testDir;
    }

    @Test
    public void kotlinFolderExistsTest() {
        assertTrue(sTestNewDir.isDirectory());
        assertTrue(new File(sTestNewDir, "kotlin").isDirectory());
    }

    @Test
    public void kotlinDecodeTest() throws IOException {
        File smaliFile = new File(sTestNewDir, "smali/org/example/kotlin/mixed/KotlinActivity.smali");
        String smali = new String(Files.readAllBytes(smaliFile.toPath()));

        assertTrue(smali.contains("KotlinActivity.kt"));
    }
}
