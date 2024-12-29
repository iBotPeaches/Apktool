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
import brut.androlib.Config;
import brut.androlib.TestUtils;
import brut.androlib.exceptions.AndrolibException;
import brut.common.BrutException;
import brut.directory.ExtFile;

import org.junit.*;
import static org.junit.Assert.*;

public class DuplicateDexTest extends BaseTest {
    private static final String TEST_APK = "duplicatedex.apk";

    @BeforeClass
    public static void beforeClass() throws Exception {
        LOGGER.info("Unpacking " + TEST_APK + "...");
        TestUtils.copyResourceDir(DuplicateDexTest.class, "decode/duplicatedex", sTmpDir);
    }

    @Test(expected = AndrolibException.class)
    public void decodeAllSourcesShouldThrowException() throws BrutException {
        LOGGER.info("Decoding " + TEST_APK + "...");
        ExtFile testApk = new ExtFile(sTmpDir, TEST_APK);
        ExtFile testDir = new ExtFile(testApk + ".out");
        new ApkDecoder(testApk, sConfig).decode(testDir);

        LOGGER.info("Building " + TEST_APK + "...");
        new ApkBuilder(testDir, sConfig).build(null);
    }

    @Test
    public void decodeUsingOnlyMainClassesMode() throws BrutException {
        sConfig.setForceDelete(true);
        sConfig.setDecodeSources(Config.DECODE_SOURCES_SMALI_ONLY_MAIN_CLASSES);

        LOGGER.info("Decoding " + TEST_APK + "...");
        ExtFile testApk = new ExtFile(sTmpDir, TEST_APK);
        ExtFile testDir = new ExtFile(testApk + ".out.main");
        new ApkDecoder(testApk, sConfig).decode(testDir);

        LOGGER.info("Building " + TEST_APK + "...");
        new ApkBuilder(testDir, sConfig).build(null);
    }

}
