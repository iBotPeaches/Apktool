/**
 *  Copyright (C) 2019 Ryszard Wiśniewski <brut.alll@gmail.com>
 *  Copyright (C) 2019 Connor Tumbleson <connor.tumbleson@gmail.com>
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
package brut.androlib.aapt1;

import brut.androlib.*;
import brut.directory.ExtFile;
import brut.common.BrutException;
import brut.util.OS;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

/**
 * @author Connor Tumbleson <connor.tumbleson@gmail.com>
 */
public class UnknownCompressionTest extends BaseTest {

    @BeforeClass
    public static void beforeClass() throws Exception {
        TestUtils.cleanFrameworkFile();
        sTmpDir = new ExtFile(OS.createTempDirectory());
        TestUtils.copyResourceDir(UnknownCompressionTest.class, "aapt1/unknown_compression/", sTmpDir);

        String apk = "deflated_unknowns.apk";
        ApkOptions apkOptions = new ApkOptions();
        apkOptions.frameworkFolderLocation = sTmpDir.getAbsolutePath();

        sTestOrigDir = new ExtFile(sTmpDir, apk);

        // decode deflated_unknowns.apk
        ApkDecoder apkDecoder = new ApkDecoder(sTestOrigDir);
        apkDecoder.setOutDir(new File(sTestOrigDir.getAbsolutePath() + ".out"));
        apkDecoder.decode();

        // build deflated_unknowns
        ExtFile clientApkFolder = new ExtFile(sTestOrigDir.getAbsolutePath() + ".out");
        new Androlib(apkOptions).build(clientApkFolder, null);
        sTestNewDir = new ExtFile(clientApkFolder, "dist" + File.separator + apk);
    }

    @AfterClass
    public static void afterClass() throws BrutException {
        OS.rmdir(sTmpDir);
    }

    @Test
    public void pkmExtensionDeflatedTest() throws BrutException, IOException {
        Integer control = sTestOrigDir.getDirectory().getCompressionLevel("assets/bin/Data/test.pkm");
        Integer rebuilt = sTestNewDir.getDirectory().getCompressionLevel("assets/bin/Data/test.pkm");

        // Check that control = rebuilt (both deflated)
        // Add extra check for checking not equal to 0, just in case control gets broken
        assertEquals(control, rebuilt);
        assertNotSame(0, rebuilt);
    }

    @Test
    public void doubleExtensionStoredTest() throws BrutException, IOException {
        Integer control = sTestOrigDir.getDirectory().getCompressionLevel("assets/bin/Data/two.extension.file");
        Integer rebuilt = sTestNewDir.getDirectory().getCompressionLevel("assets/bin/Data/two.extension.file");

        // Check that control = rebuilt (both stored)
        // Add extra check for checking = 0 to enforce check for stored just in case control breaks
        assertEquals(control, rebuilt);
        assertEquals(new Integer(0), rebuilt);
    }

    @Test
    public void confirmJsonFileIsDeflatedTest() throws BrutException, IOException {
        Integer control = sTestOrigDir.getDirectory().getCompressionLevel("test.json");
        Integer rebuilt = sTestNewDir.getDirectory().getCompressionLevel("test.json");

        assertEquals(control, rebuilt);
        assertEquals(new Integer(8), rebuilt);
    }

    @Test
    public void confirmPngFileIsCorrectlyDeflatedTest() throws BrutException, IOException {
        Integer control = sTestOrigDir.getDirectory().getCompressionLevel("950x150.png");
        Integer rebuilt = sTestNewDir.getDirectory().getCompressionLevel("950x150.png");

        assertEquals(control, rebuilt);
        assertEquals(new Integer(8), rebuilt);
    }
}