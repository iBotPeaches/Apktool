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

import org.junit.*;
import static org.junit.Assert.*;

public class UnknownCompressionTest extends BaseTest {

    @BeforeClass
    public static void beforeClass() throws Exception {
        TestUtils.copyResourceDir(UnknownCompressionTest.class, "aapt1/unknown_compression", sTmpDir);

        sConfig.setFrameworkDirectory(sTmpDir.getAbsolutePath());
        sConfig.setAaptVersion(1);

        LOGGER.info("Building deflated_unknowns.apk...");
        sTestOrigDir = new ExtFile(sTmpDir, "deflated_unknowns.apk");
        ExtFile testDir = new ExtFile(sTestOrigDir + ".out");
        new ApkDecoder(sTestOrigDir, sConfig).decode(testDir);

        LOGGER.info("Decoding deflated_unknowns.apk...");
        new ApkBuilder(testDir, sConfig).build(null);
        sTestNewDir = new ExtFile(testDir, "dist/" + sTestOrigDir.getName());
    }

    @Test
    public void pkmExtensionDeflatedTest() throws BrutException {
        String fileName = "assets/bin/Data/test.pkm";
        Integer control = sTestOrigDir.getDirectory().getCompressionLevel(fileName);
        Integer rebuilt = sTestNewDir.getDirectory().getCompressionLevel(fileName);

        // Check that control = rebuilt (both deflated)
        // Add extra check for checking not equal to 0, just in case control gets broken
        assertEquals(control, rebuilt);
        assertNotSame(Integer.valueOf(0), rebuilt);
    }

    @Test
    public void doubleExtensionStoredTest() throws BrutException {
        String fileName = "assets/bin/Data/two.extension.file";
        Integer control = sTestOrigDir.getDirectory().getCompressionLevel(fileName);
        Integer rebuilt = sTestNewDir.getDirectory().getCompressionLevel(fileName);

        // Check that control = rebuilt (both stored)
        // Add extra check for checking = 0 to enforce check for stored just in case control breaks
        assertEquals(control, rebuilt);
        assertEquals(Integer.valueOf(0), rebuilt);
    }

    @Test
    public void confirmJsonFileIsDeflatedTest() throws BrutException {
        String fileName = "test.json";
        Integer control = sTestOrigDir.getDirectory().getCompressionLevel(fileName);
        Integer rebuilt = sTestNewDir.getDirectory().getCompressionLevel(fileName);

        assertEquals(control, rebuilt);
        assertEquals(Integer.valueOf(8), rebuilt);
    }

    @Test
    public void confirmPngFileIsStoredTest() throws BrutException {
        String fileName = "950x150.png";
        Integer control = sTestOrigDir.getDirectory().getCompressionLevel(fileName);
        Integer rebuilt = sTestNewDir.getDirectory().getCompressionLevel(fileName);

        assertNotSame(control, rebuilt);
        assertEquals(Integer.valueOf(0), rebuilt);
    }
}
