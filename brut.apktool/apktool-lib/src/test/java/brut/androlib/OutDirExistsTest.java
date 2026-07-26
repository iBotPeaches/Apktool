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

import brut.androlib.exceptions.OutDirExistsException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.junit.*;
import static org.junit.Assert.*;

public class OutDirExistsTest extends BaseTest {
    private static final String TEST_APK = "issue2701.apk";

    private File testApk;
    private File outDir;

    @BeforeClass
    public static void beforeClass() throws Exception {
        copyResourceDir(OutDirExistsTest.class, "issue2701", sTmpDir);
    }

    @Before
    public void setUp() throws Exception {
        testApk = new File(sTmpDir, TEST_APK);
        outDir = new File(sTmpDir, TEST_APK + ".out_" + System.nanoTime());
    }

    @Test
    public void decodeToNonExistentDir() throws Exception {
        new ApkDecoder(testApk, sConfig).decode(outDir);

        assertTrue(outDir.isDirectory());
        assertTrue(new File(outDir, "AndroidManifest.xml").isFile());
    }

    @Test
    public void decodeToExistingEmptyDir() throws Exception {
        assertTrue(outDir.mkdirs());

        new ApkDecoder(testApk, sConfig).decode(outDir);

        assertTrue(new File(outDir, "AndroidManifest.xml").isFile());
    }

    @Test(expected = OutDirExistsException.class)
    public void decodeToExistingNonEmptyDirThrows() throws Exception {
        assertTrue(outDir.mkdirs());
        touchFile(new File(outDir, "dummy.txt"));

        new ApkDecoder(testApk, sConfig).decode(outDir);
    }

    @Test
    public void decodeToExistingNonEmptyDirWithForce() throws Exception {
        assertTrue(outDir.mkdirs());
        touchFile(new File(outDir, "dummy.txt"));

        sConfig.setForced(true);
        new ApkDecoder(testApk, sConfig).decode(outDir);

        assertTrue(new File(outDir, "AndroidManifest.xml").isFile());
    }

    @Test(expected = OutDirExistsException.class)
    public void decodeToFileInsteadOfDirThrows() throws Exception {
        File fileOutDir = new File(sTmpDir, "not_a_dir_" + System.nanoTime());
        touchFile(fileOutDir);

        new ApkDecoder(testApk, sConfig).decode(fileOutDir);
    }

    private static void touchFile(File file) throws IOException {
        try (FileWriter fw = new FileWriter(file)) {
            fw.write("content");
        }
    }
}
