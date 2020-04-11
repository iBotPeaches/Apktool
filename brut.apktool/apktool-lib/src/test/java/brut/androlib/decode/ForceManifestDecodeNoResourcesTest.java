/*
 *  Copyright (C) 2010 Ryszard Wiśniewski <brut.alll@gmail.com>
 *  Copyright (C) 2010 Connor Tumbleson <connor.tumbleson@gmail.com>
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

import brut.androlib.ApkDecoder;
import brut.androlib.BaseTest;
import brut.androlib.TestUtils;
import brut.common.BrutException;
import brut.directory.ExtFile;
import brut.util.OS;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ForceManifestDecodeNoResourcesTest extends BaseTest {

    private byte[] xmlHeader = new byte[] {
            0x3C, // <
            0x3F, // ?
            0x78, // x
            0x6D, // m
            0x6C, // l
            0x20, // (empty)
    };

    @BeforeClass
    public static void beforeClass() throws Exception {
        TestUtils.cleanFrameworkFile();
        sTmpDir = new ExtFile(OS.createTempDirectory());
        TestUtils.copyResourceDir(ForceManifestDecodeNoResourcesTest.class, "decode/issue1680/", sTmpDir);
    }

    @AfterClass
    public static void afterClass() throws BrutException {
        OS.rmdir(sTmpDir);
    }

    @Test
    public void checkIfForceManifestWithNoResourcesWorks() throws BrutException, IOException {
        String apk = "issue1680.apk";
        String output = sTmpDir + File.separator + apk + ".out";

        // decode issue1680.apk
        decodeFile(sTmpDir + File.separator + apk, ApkDecoder.DECODE_RESOURCES_NONE,
                ApkDecoder.FORCE_DECODE_MANIFEST_FULL, output);

        // lets probe filetype of manifest, we should detect XML
        File manifestFile = new File(output + File.separator + "AndroidManifest.xml");
        byte[] magic = TestUtils.readHeaderOfFile(manifestFile, 6);
        assertTrue(Arrays.equals(this.xmlHeader, magic));

        // confirm resources.arsc still exists, as its raw
        File resourcesArsc = new File(output + File.separator + "resources.arsc");
        assertTrue(resourcesArsc.isFile());
    }

    @Test
    public void checkIfForceManifestWorksWithNoChangeToResources() throws BrutException, IOException {
        String apk = "issue1680.apk";
        String output = sTmpDir + File.separator + apk + ".out";

        // decode issue1680.apk
        decodeFile(sTmpDir + File.separator + apk, ApkDecoder.DECODE_RESOURCES_FULL,
                ApkDecoder.FORCE_DECODE_MANIFEST_FULL, output);

        // lets probe filetype of manifest, we should detect XML
        File manifestFile = new File(output + File.separator + "AndroidManifest.xml");
        byte[] magic = TestUtils.readHeaderOfFile(manifestFile, 6);
        assertTrue(Arrays.equals(this.xmlHeader, magic));

        // confirm resources.arsc does not exist
        File resourcesArsc = new File(output + File.separator + "resources.arsc");
        assertFalse(resourcesArsc.isFile());
    }

    @Test
    public void checkForceManifestToFalseWithResourcesEnabledIsIgnored() throws BrutException, IOException {
        String apk = "issue1680.apk";
        String output = sTmpDir + File.separator + apk + ".out";

        // decode issue1680.apk
        decodeFile(sTmpDir + File.separator + apk, ApkDecoder.DECODE_RESOURCES_FULL,
                ApkDecoder.FORCE_DECODE_MANIFEST_NONE, output);

        // lets probe filetype of manifest, we should detect XML
        File manifestFile = new File(output + File.separator + "AndroidManifest.xml");
        byte[] magic = TestUtils.readHeaderOfFile(manifestFile, 6);
        assertTrue(Arrays.equals(this.xmlHeader, magic));

        // confirm resources.arsc does not exist
        File resourcesArsc = new File(output + File.separator + "resources.arsc");
        assertFalse(resourcesArsc.isFile());
    }

    @Test
    public void checkBothManifestAndResourcesSetToNone() throws BrutException, IOException {
        String apk = "issue1680.apk";
        String output = sTmpDir + File.separator + apk + ".out";

        // decode issue1680.apk
        decodeFile(sTmpDir + File.separator + apk, ApkDecoder.DECODE_RESOURCES_NONE,
                ApkDecoder.FORCE_DECODE_MANIFEST_NONE, output);

        // lets probe filetype of manifest, we should not detect XML
        File manifestFile = new File(output + File.separator + "AndroidManifest.xml");
        byte[] magic = TestUtils.readHeaderOfFile(manifestFile, 6);
        assertFalse(Arrays.equals(this.xmlHeader, magic));

        // confirm resources.arsc exists
        File resourcesArsc = new File(output + File.separator + "resources.arsc");
        assertTrue(resourcesArsc.isFile());
    }

    private void decodeFile(String apk, short decodeResources, short decodeManifest, String output)
            throws BrutException, IOException {
        ApkDecoder apkDecoder = new ApkDecoder(new File(apk));
        apkDecoder.setDecodeResources(decodeResources);
        apkDecoder.setForceDecodeManifest(decodeManifest);
        apkDecoder.setForceDelete(true); // delete directory due to multiple tests.

        apkDecoder.setOutDir(new File(output));
        apkDecoder.decode();
    }
}