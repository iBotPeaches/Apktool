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
package brut.androlib.aapt1;

import brut.androlib.ApkBuilder;
import brut.androlib.ApkDecoder;
import brut.androlib.BaseTest;
import brut.androlib.TestUtils;
import brut.directory.ExtFile;
import brut.common.BrutException;

import org.junit.*;
import static org.junit.Assert.*;

public class BuildAndDecodeJarTest extends BaseTest {

    @BeforeClass
    public static void beforeClass() throws Exception {
        sTestOrigDir = new ExtFile(sTmpDir, "testjar-orig");
        sTestNewDir = new ExtFile(sTmpDir, "testjar-new");

        LOGGER.info("Unpacking testjar...");
        TestUtils.copyResourceDir(BuildAndDecodeJarTest.class, "aapt1/testjar", sTestOrigDir);

        sConfig.setAaptVersion(1);

        LOGGER.info("Building testjar.jar...");
        ExtFile testJar = new ExtFile(sTmpDir, "testjar.jar");
        new ApkBuilder(sTestOrigDir, sConfig).build(testJar);

        LOGGER.info("Decoding testjar.jar...");
        new ApkDecoder(testJar, sConfig).decode(sTestNewDir);
    }

    @Test
    public void buildAndDecodeTest() {
        assertTrue(sTestNewDir.isDirectory());
    }
}
