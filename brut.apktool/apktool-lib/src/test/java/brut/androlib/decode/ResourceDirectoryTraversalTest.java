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
import brut.common.BrutException;
import brut.directory.ExtFile;

import java.io.File;

import org.junit.*;
import static org.junit.Assert.*;

public class ResourceDirectoryTraversalTest extends BaseTest {
    private static final String TEST_APK = "GHSA-2hqv-2xv4-5h5w.apk";

    @BeforeClass
    public static void beforeClass() throws Exception {
        TestUtils.copyResourceDir(ResourceDirectoryTraversalTest.class, "decode/arbitrary-write", sTmpDir);
    }

    @Test
    public void checkIfMaliciousRawFileIsDisassembledProperly() throws BrutException {
        sConfig.setForceDelete(true);

        ExtFile testApk = new ExtFile(sTmpDir, TEST_APK);
        ExtFile testDir = new ExtFile(testApk + ".out");
        new ApkDecoder(testApk, sConfig).decode(testDir);

        assertTrue(new File(testDir, "res/raw/poc").exists());
    }
}
