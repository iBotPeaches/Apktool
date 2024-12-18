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

import java.io.File;
import java.io.IOException;

import org.junit.*;
import static org.junit.Assert.*;

public class LargeIntsInManifestTest extends BaseTest {
    private static final String TEST_APK = "issue767.apk";

    @BeforeClass
    public static void beforeClass() throws Exception {
        TestUtils.copyResourceDir(LargeIntsInManifestTest.class, "aapt1/issue767", sTmpDir);
    }

    @Test
    public void checkIfLargeIntsAreHandledTest() throws BrutException {
        sConfig.setAaptVersion(1);

        ExtFile testApk = new ExtFile(sTmpDir, TEST_APK);
        ExtFile testDir = new ExtFile(testApk + ".out");
        new ApkDecoder(testApk, sConfig).decode(testDir);

        new ApkBuilder(testDir, sConfig).build(null);

        ExtFile newApk = new ExtFile(testDir, "dist/" + testApk.getName());
        ExtFile newDir = new ExtFile(testApk + ".out.new");
        new ApkDecoder(newApk, sConfig).decode(newDir);

        compareXmlFiles(testDir, newDir, "AndroidManifest.xml");
    }
}
