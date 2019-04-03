/**
 *  Copyright (C) 2018 Ryszard Wiśniewski <brut.alll@gmail.com>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package brut.androlib.decode;

import brut.androlib.*;
import brut.androlib.aapt1.AndroidOreoSparseTest;
import brut.androlib.aapt2.BuildAndDecodeTest;
import brut.androlib.meta.MetaInfo;
import brut.common.BrutException;
import brut.directory.ExtFile;
import brut.util.OS;
import org.apache.commons.lang3.StringUtils;
import org.junit.*;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertTrue;

public class DuplicateDexTest extends BaseTest {

    @Before
    public void beforeClass() throws Exception {
        TestUtils.cleanFrameworkFile();
        sTmpDir = new ExtFile(OS.createTempDirectory());
        sTestOrigDir = new ExtFile(sTmpDir, "duplicatedex-orig");
        sTestNewDir = new ExtFile(sTmpDir, "duplicatedex-new");
        LOGGER.info("Unpacking sparse.apk...");
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
        apkDecoder.setOutDir(sTestNewDir);
        apkDecoder.decode();

        LOGGER.info("Building duplicatedex.apk...");
        ApkOptions apkOptions = new ApkOptions();
        new Androlib(apkOptions).build(sTestNewDir, testApk);
    }

    @Test
    public void decodeUsingOnlyMainClassesMode() throws BrutException, IOException {
        File testApk = new File(sTestOrigDir, "duplicatedex.apk");

        LOGGER.info("Decoding duplicatedex.apk...");
        ApkDecoder apkDecoder = new ApkDecoder(testApk);
        apkDecoder.setDecodeSources(ApkDecoder.DECODE_SOURCES_SMALI_ONLY_MAIN_CLASSES);
        apkDecoder.setOutDir(sTestNewDir);
        apkDecoder.decode();

        LOGGER.info("Building duplicatedex.apk...");
        ApkOptions apkOptions = new ApkOptions();
        new Androlib(apkOptions).build(sTestNewDir, testApk);
    }

}