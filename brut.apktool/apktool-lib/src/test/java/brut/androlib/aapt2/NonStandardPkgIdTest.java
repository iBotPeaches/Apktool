/**
 *  Copyright (C) 2018 Ryszard Wiśniewski <brut.alll@gmail.com>
 *  Copyright (C) 2018 Connor Tumbleson <connor.tumbleson@gmail.com>
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
package brut.androlib.aapt2;

import brut.androlib.*;
import brut.androlib.res.data.ResTable;
import brut.common.BrutException;
import brut.directory.ExtFile;
import brut.util.OS;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

public class NonStandardPkgIdTest extends BaseTest {

    @BeforeClass
    public static void beforeClass() throws Exception {
        TestUtils.cleanFrameworkFile();

        sTmpDir = new ExtFile(OS.createTempDirectory());
        sTmpDir.deleteOnExit();

        sTestOrigDir = new ExtFile(sTmpDir, "pkgid8-orig");
        sTestNewDir = new ExtFile(sTmpDir, "pkgid8-new");
        LOGGER.info("Unpacking pkgid8...");
        TestUtils.copyResourceDir(BuildAndDecodeTest.class, "aapt2/pkgid8/", sTestOrigDir);

        ApkOptions apkOptions = new ApkOptions();
        apkOptions.useAapt2 = true;
        apkOptions.verbose = true;

        LOGGER.info("Building pkgid8.apk...");
        File testApk = new File(sTmpDir, "pkgid8.apk");
        new Androlib(apkOptions).build(sTestOrigDir, testApk);

        LOGGER.info("Decoding pkgid8.apk...");
        ApkDecoder apkDecoder = new ApkDecoder(testApk);
        apkDecoder.setOutDir(sTestNewDir);
        apkDecoder.decode();
        mResTable = apkDecoder.getResTable();
    }

    @AfterClass
    public static void afterClass() throws BrutException {
        OS.rmdir(sTmpDir);
    }

    @Test
    public void buildAndDecodeTest() {
        assertTrue(sTestNewDir.isDirectory());
    }

    @Test
    public void valuesStringsTest() throws BrutException {
        compareValuesFiles("values/strings.xml");
    }

    @Test
    public void confirmManifestStructureTest() throws BrutException {
        compareXmlFiles("AndroidManifest.xml");
    }

    @Test
    public void confirmResourcesAreFromPkgId8() throws AndrolibException {
        assertEquals(0x80, mResTable.getPackageId());

        assertEquals(0x80, mResTable.getResSpec(0x80020000).getPackage().getId());
        assertEquals(0x80, mResTable.getResSpec(0x80020001).getPackage().getId());
        assertEquals(0x80, mResTable.getResSpec(0x80030000).getPackage().getId());
    }

    private static ResTable mResTable;
}
