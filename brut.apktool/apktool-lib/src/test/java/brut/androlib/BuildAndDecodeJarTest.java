/**
 *  Copyright 2014 Ryszard Wiśniewski <brut.alll@gmail.com>
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
package brut.androlib;

import brut.androlib.res.util.ExtFile;
import brut.common.BrutException;
import brut.util.OS;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.logging.Logger;

import static org.junit.Assert.assertTrue;

/**
 * @author Connor Tumbleson <connor.tumbleson@gmail.com>
 */
public class BuildAndDecodeJarTest {

    @BeforeClass
    public static void beforeClass() throws Exception, BrutException {
        sTmpDir = new ExtFile(OS.createTempDirectory());
        sTestOrigDir = new ExtFile(sTmpDir, "testjar-orig");
        sTestNewDir = new ExtFile(sTmpDir, "testjar-new");
        LOGGER.info("Unpacking testjar...");
        TestUtils.copyResourceDir(BuildAndDecodeJarTest.class, "brut/apktool/testjar/", sTestOrigDir);

        LOGGER.info("Building testjar.jar...");
        File testJar = new File(sTmpDir, "testjar.jar");
        new Androlib().build(sTestOrigDir, testJar);

        LOGGER.info("Decoding testjar.jar...");
        ApkDecoder apkDecoder = new ApkDecoder(testJar);
        apkDecoder.setOutDir(sTestNewDir);
        apkDecoder.decode();
    }

    @AfterClass
    public static void afterClass() throws BrutException {
        OS.rmdir(sTmpDir);
    }

    @Test
    public void buildAndDecodeTest() throws BrutException {
        assertTrue(sTestNewDir.isDirectory());
    }

    private static ExtFile sTmpDir;
    private static ExtFile sTestOrigDir;
    private static ExtFile sTestNewDir;

    private final static Logger LOGGER = Logger.getLogger(BuildAndDecodeJarTest.class.getName());
}
