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

public class CopyOriginalFilesTest extends BaseTest {
    private static final String TEST_APK = "issue1157.apk";

    @BeforeClass
    public static void beforeClass() throws Exception {
        copyResourceDir(CopyOriginalFilesTest.class, "issue1157", sTmpDir);
    }

    @Test
    public void onlyOriginalFilesAreCopiedTest() throws Exception {
        Path testDir = sTmpDir.resolve(TEST_APK + ".out");
        new ApkDecoder(sTmpDir.resolve(TEST_APK), sConfig).decode(testDir);

        // Older Apktool versions copied all of META-INF into original/.
        Path strayFile = testDir.resolve("original/META-INF/services/com.example.Plugin");
        Files.createDirectories(strayFile.getParent());
        Files.write(strayFile, "stray".getBytes(StandardCharsets.UTF_8));

        sConfig.setCopyOriginal(true);
        Path outApk = sTmpDir.resolve("copy-original.apk");
        new ApkBuilder(testDir, sConfig).build(outApk);

        Map<String, byte[]> entries = readZipEntries(outApk);
        assertTrue(entries.containsKey("META-INF/CERT.RSA"));
        assertFalse(entries.containsKey("META-INF/services/com.example.Plugin"));
    }
}
