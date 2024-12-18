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

public class AndroidOreoNotSparseTest extends BaseTest {

    @BeforeClass
    public static void beforeClass() throws Exception {
        sTestOrigDir = new ExtFile(sTmpDir, "issue1594-orig");
        sTestNewDir = new ExtFile(sTmpDir, "issue1594-new");

        LOGGER.info("Unpacking not_sparse.apk...");
        TestUtils.copyResourceDir(AndroidOreoNotSparseTest.class, "aapt1/issue1594", sTestOrigDir);

        sConfig.setAaptVersion(1);

        LOGGER.info("Decoding not_sparse.apk...");
        ExtFile testApk = new ExtFile(sTestOrigDir, "not_sparse.apk");
        new ApkDecoder(testApk, sConfig).decode(sTestNewDir);

        LOGGER.info("Building not_sparse.apk...");
        new ApkBuilder(sTestNewDir, sConfig).build(testApk);
    }

    @Test
    public void buildAndDecodeTest() {
        assertTrue(sTestNewDir.isDirectory());
        assertTrue(sTestOrigDir.isDirectory());
    }
}
