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

import brut.androlib.ApkBuilder;
import brut.androlib.ApkDecoder;
import brut.androlib.BaseTest;
import brut.androlib.TestUtils;
import brut.androlib.apk.ApkInfo;
import brut.common.BrutException;
import brut.directory.ExtFile;

import org.junit.*;
import static org.junit.Assert.*;

public class SparseFlagTest extends BaseTest {

    @BeforeClass
    public static void beforeClass() throws Exception {
        LOGGER.info("Unpacking sparse.apk && not-sparse.apk...");
        TestUtils.copyResourceDir(SparseFlagTest.class, "decode/sparse", sTmpDir);
    }

    @Test
    public void decodeWithExpectationOfSparseResources() throws BrutException {
        sConfig.setFrameworkTag("issue-3298");

        LOGGER.info("Decoding sparse.apk...");
        ExtFile testApk = new ExtFile(sTmpDir, "sparse.apk");
        ExtFile testDir = new ExtFile(testApk + ".out");
        ApkInfo apkInfo = new ApkDecoder(testApk, sConfig).decode(testDir);

        assertTrue("Expecting sparse resources", apkInfo.sparseResources);

        LOGGER.info("Building sparse.apk...");
        new ApkBuilder(testDir, sConfig).build(null);
    }

    @Test
    public void decodeWithExpectationOfNoSparseResources() throws BrutException {
        sConfig.setFrameworkTag("issue-3298");

        LOGGER.info("Decoding not-sparse.apk...");
        ExtFile testApk = new ExtFile(sTmpDir, "not-sparse.apk");
        ExtFile testDir = new ExtFile(testApk + ".out");
        ApkInfo apkInfo = new ApkDecoder(testApk, sConfig).decode(testDir);

        assertFalse("Expecting not-sparse resources", apkInfo.sparseResources);

        LOGGER.info("Building not-sparse.apk...");
        new ApkBuilder(testDir, sConfig).build(null);
    }
}
