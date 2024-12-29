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
package brut.androlib.aapt2;

import brut.androlib.ApkBuilder;
import brut.androlib.BaseTest;
import brut.androlib.TestUtils;
import brut.androlib.apk.ApkInfo;
import brut.androlib.res.ResourcesDecoder;
import brut.androlib.res.data.ResTable;
import brut.common.BrutException;
import brut.directory.ExtFile;
import brut.util.OS;

import org.junit.*;
import static org.junit.Assert.*;

public class NonStandardPkgIdTest extends BaseTest {
    private static ResTable sResTable;

    @BeforeClass
    public static void beforeClass() throws Exception {
        sTestOrigDir = new ExtFile(sTmpDir, "pkgid8-orig");
        sTestNewDir = new ExtFile(sTmpDir, "pkgid8-new");

        LOGGER.info("Unpacking pkgid8...");
        TestUtils.copyResourceDir(BuildAndDecodeTest.class, "aapt2/pkgid8", sTestOrigDir);

        sConfig.setVerbose(true);

        LOGGER.info("Building pkgid8.apk...");
        ExtFile testApk = new ExtFile(sTmpDir, "pkgid8.apk");
        new ApkBuilder(sTestOrigDir, sConfig).build(testApk);

        LOGGER.info("Decoding pkgid8.apk...");
        ApkInfo testInfo = new ApkInfo(testApk);
        ResourcesDecoder resDecoder = new ResourcesDecoder(testInfo, sConfig);
        OS.mkdir(sTestNewDir);
        resDecoder.decodeResources(sTestNewDir);
        resDecoder.decodeManifest(sTestNewDir);
        sResTable = resDecoder.getResTable();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        sResTable.getApkInfo().getApkFile().close();
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
    public void confirmResourcesAreFromPkgId8() throws BrutException {
        assertEquals(0x80, sResTable.getPackageId());

        assertEquals(0x80, sResTable.getResSpec(0x80020000).getPackage().getId());
        assertEquals(0x80, sResTable.getResSpec(0x80020001).getPackage().getId());
        assertEquals(0x80, sResTable.getResSpec(0x80030000).getPackage().getId());
    }
}
