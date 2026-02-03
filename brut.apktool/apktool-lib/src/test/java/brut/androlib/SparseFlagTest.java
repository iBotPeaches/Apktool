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
package brut.androlib;

import brut.androlib.meta.ApkInfo;

import java.io.File;

import org.junit.*;
import static org.junit.Assert.*;

public class SparseFlagTest extends BaseTest {

    @BeforeClass
    public static void beforeClass() throws Exception {
        log("Unpacking sparse.apk && not-sparse.apk...");
        copyResourceDir(SparseFlagTest.class, "sparse", sTmpDir);
    }

    @Test
    public void decodeWithExpectationOfSparseEntries() throws Exception {
        sConfig.setFrameworkTag("issue-3298");

        log("Decoding sparse.apk...");
        File testApk = new File(sTmpDir, "sparse.apk");
        File testDir = new File(testApk + ".out");
        ApkDecoder apkDecoder = new ApkDecoder(testApk, sConfig);
        apkDecoder.decode(testDir);
        ApkInfo apkInfo = apkDecoder.getApkInfo();

        assertTrue("Expecting sparse entries", apkInfo.getResourcesInfo().isSparseEntries());

        log("Building sparse.apk...");
        new ApkBuilder(testDir, sConfig).build(null);
    }

    @Test
    public void decodeWithExpectationOfNoSparseEntries() throws Exception {
        sConfig.setFrameworkTag("issue-3298");

        log("Decoding not-sparse.apk...");
        File testApk = new File(sTmpDir, "not-sparse.apk");
        File testDir = new File(testApk + ".out");
        ApkDecoder apkDecoder = new ApkDecoder(testApk, sConfig);
        apkDecoder.decode(testDir);
        ApkInfo apkInfo = apkDecoder.getApkInfo();

        assertFalse("Expecting not-sparse entries", apkInfo.getResourcesInfo().isSparseEntries());

        log("Building not-sparse.apk...");
        new ApkBuilder(testDir, sConfig).build(null);
    }
}
