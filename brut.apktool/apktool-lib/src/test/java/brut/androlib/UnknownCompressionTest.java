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

import brut.androlib.ApkFile;

import java.nio.file.Path;

import org.junit.*;
import static org.junit.Assert.*;

public class UnknownCompressionTest extends BaseTest {
    private static ApkFile sTestApk;
    private static ApkFile sNewApk;

    @BeforeClass
    public static void beforeClass() throws Exception {
        copyResourceDir(UnknownCompressionTest.class, "unknown_compression", sTmpDir);

        sConfig.setFrameworkDirectory(sTmpDir.toAbsolutePath().toString());

        log("Building unknown_compression.apk...");
        Path testApk = sTmpDir.resolve("unknown_compression.apk");
        Path testDir = sTmpDir.resolve(testApk.getFileName() + ".out");
        new ApkDecoder(testApk, sConfig).decode(testDir);

        log("Decoding unknown_compression.apk...");
        new ApkBuilder(testDir, sConfig).build(null);

        sTestApk = new ApkFile(testApk);
        sNewApk = new ApkFile(testDir.resolve("dist/" + testApk.getFileName()));
    }

    @AfterClass
    public static void afterClass() throws Exception {
        sTestApk.close();
        sNewApk.close();
    }

    @Test
    public void pkmExtensionDeflatedTest() throws Exception {
        String name = "assets/bin/Data/test.pkm";
        int control = sTestApk.getFile(name).getMethod();
        int rebuilt = sNewApk.getFile(name).getMethod();

        // Check that control = rebuilt (both deflated)
        // Add extra check for checking not equal to 0, just in case control gets broken
        assertEquals(control, rebuilt);
        assertNotEquals(0, rebuilt);
    }

    @Test
    public void doubleExtensionStoredTest() throws Exception {
        String name = "assets/bin/Data/two.extension.file";
        int control = sTestApk.getFile(name).getMethod();
        int rebuilt = sNewApk.getFile(name).getMethod();

        // Check that control = rebuilt (both stored)
        // Add extra check for checking = 0 to enforce check for stored just in case control breaks
        assertEquals(control, rebuilt);
        assertEquals(0, rebuilt);
    }

    @Test
    public void confirmJsonFileIsDeflatedTest() throws Exception {
        String name = "test.json";
        int control = sTestApk.getFile(name).getMethod();
        int rebuilt = sNewApk.getFile(name).getMethod();

        assertEquals(control, rebuilt);
        assertEquals(8, rebuilt);
    }

    @Test
    public void confirmPngFileIsStoredTest() throws Exception {
        String name = "950x150.png";
        int control = sTestApk.getFile(name).getMethod();
        int rebuilt = sNewApk.getFile(name).getMethod();

        assertNotEquals(control, rebuilt);
        assertEquals(0, rebuilt);
    }
}
