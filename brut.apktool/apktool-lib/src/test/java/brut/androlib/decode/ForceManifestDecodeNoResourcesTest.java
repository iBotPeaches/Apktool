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
import brut.androlib.Config;
import brut.androlib.TestUtils;
import brut.common.BrutException;
import brut.directory.ExtFile;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.junit.*;
import static org.junit.Assert.*;

public class ForceManifestDecodeNoResourcesTest extends BaseTest {
    private static final String TEST_APK = "issue1680.apk";

    private static final byte[] XML_HEADER = {
            0x3C, // <
            0x3F, // ?
            0x78, // x
            0x6D, // m
            0x6C, // l
            0x20, // (empty)
    };

    @BeforeClass
    public static void beforeClass() throws Exception {
        TestUtils.copyResourceDir(ForceManifestDecodeNoResourcesTest.class, "decode/issue1680", sTmpDir);
    }

    @Test
    public void checkIfForceManifestWithNoResourcesWorks() throws BrutException, IOException {
        sConfig.setForceDelete(true);
        sConfig.setDecodeResources(Config.DECODE_RESOURCES_NONE);
        sConfig.setForceDecodeManifest(Config.FORCE_DECODE_MANIFEST_FULL);

        ExtFile testApk = new ExtFile(sTmpDir, TEST_APK);
        ExtFile testDir = new ExtFile(testApk + ".out");
        new ApkDecoder(testApk, sConfig).decode(testDir);

        // assert that manifest is XML
        assertArrayEquals(XML_HEADER, TestUtils.readHeaderOfFile(new File(testDir, "AndroidManifest.xml"), 6));

        // assert that resources.arsc exists
        assertTrue(new File(testDir, "resources.arsc").isFile());
    }

    @Test
    public void checkIfForceManifestWorksWithNoChangeToResources() throws BrutException, IOException {
        sConfig.setForceDelete(true);
        sConfig.setDecodeResources(Config.DECODE_RESOURCES_FULL);
        sConfig.setForceDecodeManifest(Config.FORCE_DECODE_MANIFEST_FULL);

        ExtFile testApk = new ExtFile(sTmpDir, TEST_APK);
        ExtFile testDir = new ExtFile(testApk + ".out");
        new ApkDecoder(testApk, sConfig).decode(testDir);

        // assert that manifest is XML
        assertArrayEquals(XML_HEADER, TestUtils.readHeaderOfFile(new File(testDir, "AndroidManifest.xml"), 6));

        // assert that resources.arsc does not exist
        assertFalse(new File(testDir, "resources.arsc").isFile());
    }

    @Test
    public void checkForceManifestToFalseWithResourcesEnabledIsIgnored() throws BrutException, IOException {
        sConfig.setForceDelete(true);
        sConfig.setDecodeResources(Config.DECODE_RESOURCES_FULL);
        sConfig.setForceDecodeManifest(Config.FORCE_DECODE_MANIFEST_NONE);

        ExtFile testApk = new ExtFile(sTmpDir, TEST_APK);
        ExtFile testDir = new ExtFile(testApk + ".out");
        new ApkDecoder(testApk, sConfig).decode(testDir);

        // assert that manifest is XML
        assertArrayEquals(XML_HEADER, TestUtils.readHeaderOfFile(new File(testDir, "AndroidManifest.xml"), 6));

        // assert that resources.arsc does not exist
        assertFalse(new File(testDir, "resources.arsc").isFile());
    }

    @Test
    public void checkBothManifestAndResourcesSetToNone() throws BrutException, IOException {
        sConfig.setForceDelete(true);
        sConfig.setDecodeResources(Config.DECODE_RESOURCES_NONE);
        sConfig.setForceDecodeManifest(Config.FORCE_DECODE_MANIFEST_NONE);

        ExtFile testApk = new ExtFile(sTmpDir, TEST_APK);
        ExtFile testDir = new ExtFile(testApk + ".out");
        new ApkDecoder(testApk, sConfig).decode(testDir);

        // assert that manifest is not XML
        assertFalse(Arrays.equals(XML_HEADER, TestUtils.readHeaderOfFile(new File(testDir, "AndroidManifest.xml"), 6)));

        // assert that resources.arsc exists
        assertTrue(new File(testDir, "resources.arsc").isFile());
    }
}
