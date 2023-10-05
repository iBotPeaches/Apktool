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

import brut.androlib.*;
import brut.androlib.Config;
import brut.directory.ExtFile;
import brut.common.BrutException;
import brut.util.OS;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertTrue;

public class AndroidOreoNotSparseTest extends BaseTest {
    @BeforeClass
    public static void beforeClass() throws Exception {
        TestUtils.cleanFrameworkFile();
        sTmpDir = new ExtFile(OS.createTempDirectory());
        sTestOrigDir = new ExtFile(sTmpDir, "issue1594-orig");
        sTestNewDir = new ExtFile(sTmpDir, "issue1594-new");
        LOGGER.info("Unpacking not_sparse.apk...");
        TestUtils.copyResourceDir(AndroidOreoNotSparseTest.class, "aapt1/issue1594", sTestOrigDir);

        File testApk = new File(sTestOrigDir, "not_sparse.apk");

        LOGGER.info("Decoding not_sparse.apk...");
        ApkDecoder apkDecoder = new ApkDecoder(testApk);
        apkDecoder.decode(sTestNewDir);

        LOGGER.info("Building not_sparse.apk...");
        Config config = Config.getDefaultConfig();
        config.useAapt2 = false;
        new ApkBuilder(config, sTestNewDir).build(testApk);
    }

    @AfterClass
    public static void afterClass() throws BrutException {
        OS.rmdir(sTmpDir);
    }

    @Test
    public void buildAndDecodeTest() {
        assertTrue(sTestNewDir.isDirectory());
        assertTrue(sTestOrigDir.isDirectory());
    }
}
