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
package brut.androlib;

import brut.androlib.meta.ApkInfo;
import brut.androlib.res.ResourcesDecoder;
import brut.androlib.res.table.ResId;
import brut.androlib.res.table.ResTable;
import brut.common.BrutException;
import brut.directory.ExtFile;
import brut.util.OS;

import org.junit.*;
import static org.junit.Assert.*;

public class NonStandardPkgIdTest extends BaseTest {
    private static ResTable sTable;

    @BeforeClass
    public static void beforeClass() throws Exception {
        sTestOrigDir = new ExtFile(sTmpDir, "pkgid8-orig");
        sTestNewDir = new ExtFile(sTmpDir, "pkgid8-new");

        LOGGER.info("Unpacking pkgid8...");
        TestUtils.copyResourceDir(NonStandardPkgIdTest.class, "pkgid8", sTestOrigDir);

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
        sTable = resDecoder.getTable();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        sTable.getApkInfo().getApkFile().close();
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
        assertEquals(0x80, sTable.getMainPackage().getId());

        assertEquals(0x80, sTable.getEntrySpec(ResId.of(0x80020000)).getPackage().getId());
        assertEquals(0x80, sTable.getEntrySpec(ResId.of(0x80020001)).getPackage().getId());
        assertEquals(0x80, sTable.getEntrySpec(ResId.of(0x80030000)).getPackage().getId());
    }
}
