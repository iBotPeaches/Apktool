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

import brut.androlib.*;
import brut.androlib.apk.ApkInfo;
import brut.common.BrutException;
import brut.directory.ExtFile;
import brut.util.OS;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SparseFlagTest extends BaseTest {

    @Before
    public void beforeClass() throws Exception {
        TestUtils.cleanFrameworkFile();
        sTmpDir = new ExtFile(OS.createTempDirectory());
        sTestOrigDir = new ExtFile(sTmpDir, "sparse-orig");
        sTestNewDir = new ExtFile(sTmpDir, "sparse-new");
        LOGGER.info("Unpacking sparse.apk && not-sparse.apk...");
        TestUtils.copyResourceDir(SparseFlagTest.class, "decode/sparse", sTestOrigDir);
    }

    @After
    public void afterClass() throws BrutException {
        OS.rmdir(sTmpDir);
    }

    @Test
    public void decodeWithExpectationOfSparseResources() throws BrutException, IOException {
        File testApk = new File(sTestOrigDir, "sparse.apk");

        LOGGER.info("Decoding sparse.apk...");
        Config config = Config.getDefaultConfig();
        config.frameworkTag = "issue-3298";

        ApkDecoder apkDecoder = new ApkDecoder(config, testApk);
        ApkInfo apkInfo = apkDecoder.decode(sTestNewDir);

        assertTrue("Expecting sparse resources", apkInfo.sparseResources);

        LOGGER.info("Building sparse.apk...");
        new ApkBuilder(config, sTestNewDir).build(testApk);
    }

    @Test
    public void decodeWithExpectationOfNoSparseResources() throws BrutException, IOException {
        File testApk = new File(sTestOrigDir, "not-sparse.apk");

        LOGGER.info("Decoding not-sparse.apk...");
        Config config = Config.getDefaultConfig();
        config.frameworkTag = "issue-3298";

        ApkDecoder apkDecoder = new ApkDecoder(config, testApk);
        ApkInfo apkInfo = apkDecoder.decode(sTestNewDir);

        assertFalse("Expecting not-sparse resources", apkInfo.sparseResources);

        LOGGER.info("Building not-sparse.apk...");
        new ApkBuilder(config, sTestNewDir).build(testApk);
    }
}
