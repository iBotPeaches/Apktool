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

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.*;
import static org.junit.Assert.*;

public class OutsideOfDirectoryEntryTest extends BaseTest {
    private static final String TEST_APK = "issue1589.apk";

    @BeforeClass
    public static void beforeClass() throws Exception {
        copyResourceDir(OutsideOfDirectoryEntryTest.class, "issue1589", sTmpDir);
    }

    @Test
    public void skippedDecodingOfInvalidFileTest() throws Exception {
        Path testApk = sTmpDir.resolve(TEST_APK);
        Path testDir = sTmpDir.resolve(testApk.getFileName() + ".out");
        new ApkDecoder(testApk, sConfig).decode(testDir);

        assertTrue(Files.isDirectory(testDir));
        assertFalse(Files.isDirectory(testDir.resolve("assets")));
    }
}
