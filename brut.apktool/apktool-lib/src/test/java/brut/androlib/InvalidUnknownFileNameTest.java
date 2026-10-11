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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.*;
import static org.junit.Assert.*;

public class InvalidUnknownFileNameTest extends BaseTest {
    private static final String TEST_APK = "issue1157.apk";

    @BeforeClass
    public static void beforeClass() throws Exception {
        copyResourceDir(InvalidUnknownFileNameTest.class, "issue1157", sTmpDir);
    }

    @Test
    public void invalidFileNameIsSkippedTest() throws Exception {
        // NUL is rejected by every default filesystem.
        Map<String, byte[]> entries = readZipEntries(sTmpDir.resolve(TEST_APK));
        entries.put("junk/a\u0000b.bin", "junk".getBytes(StandardCharsets.UTF_8));
        entries.put("junk/valid.bin", "junk".getBytes(StandardCharsets.UTF_8));
        Path testApk = sTmpDir.resolve("invalid-name.apk");
        writeZipEntries(testApk, entries);

        Path testDir = sTmpDir.resolve(testApk.getFileName() + ".out");
        new ApkDecoder(testApk, sConfig).decode(testDir);

        assertTrue(Files.isRegularFile(testDir.resolve("unknown/junk/valid.bin")));
    }
}
