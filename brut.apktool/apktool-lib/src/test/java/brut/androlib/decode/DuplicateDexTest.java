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
import brut.androlib.exceptions.AndrolibException;
import brut.common.BrutException;
import brut.directory.ExtFile;
import brut.util.OS;
import org.junit.*;

import java.io.File;
import java.io.IOException;

public class DuplicateDexTest extends BaseTest {

    @Before
    public void beforeClass() throws Exception {
        TestUtils.cleanFrameworkFile();
        sTmpDir = new ExtFile(OS.createTempDirectory());
        sTestOrigDir = new ExtFile(sTmpDir, "duplicatedex-orig");
        sTestNewDir = new ExtFile(sTmpDir, "duplicatedex-new");
        LOGGER.info("Unpacking duplicatedex.apk...");
        TestUtils.copyResourceDir(DuplicateDexTest.class, "decode/duplicatedex", sTestOrigDir);
    }

    @After
    public void afterClass() throws BrutException {
        OS.rmdir(sTmpDir);
    }

    @Test(expected = AndrolibException.class)
    public void decodeAllSourcesShouldThrowException() throws BrutException, IOException {
        File testApk = new File(sTestOrigDir, "duplicatedex.apk");

        LOGGER.info("Decoding duplicatedex.apk...");
        ApkDecoder apkDecoder = new ApkDecoder(testApk);
        apkDecoder.decode(sTestNewDir);

        LOGGER.info("Building duplicatedex.apk...");
        Config config = Config.getDefaultConfig();
        new ApkBuilder(config, sTestNewDir).build(testApk);
    }

    @Test
    public void decodeUsingOnlyMainClassesMode() throws BrutException, IOException {
        File testApk = new File(sTestOrigDir, "duplicatedex.apk");

        LOGGER.info("Decoding duplicatedex.apk...");
        Config config = Config.getDefaultConfig();
        config.decodeSources = Config.DECODE_SOURCES_SMALI_ONLY_MAIN_CLASSES;

        ApkDecoder apkDecoder = new ApkDecoder(config, testApk);
        apkDecoder.decode(sTestNewDir);

        LOGGER.info("Building duplicatedex.apk...");
        new ApkBuilder(config, sTestNewDir).build(testApk);
    }

}
