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

public class SkipAssetTest extends BaseTest {
    private static final String TEST_APK = "issue1605.apk";

    @BeforeClass
    public static void beforeClass() throws Exception {
        copyResourceDir(SkipAssetTest.class, "issue1605", sTmpDir);
    }

    @Test
    public void checkIfEnablingSkipAssetWorks() throws Exception {
        sConfig.setDecodeAssets(Config.DecodeAssets.NONE);

        Path testApk = sTmpDir.resolve(TEST_APK);
        Path testDir = sTmpDir.resolve(testApk.getFileName() + ".out.none");
        new ApkDecoder(testApk, sConfig).decode(testDir);

        assertFalse(Files.isRegularFile(testDir.resolve("assets/kotlin.kotlin_builtins")));
        assertFalse(Files.isRegularFile(testDir.resolve("assets/ranges/ranges.kotlin_builtins")));
    }

    @Test
    public void checkControl() throws Exception {
        sConfig.setDecodeAssets(Config.DecodeAssets.FULL);

        Path testApk = sTmpDir.resolve(TEST_APK);
        Path testDir = sTmpDir.resolve(testApk.getFileName() + ".out.full");
        new ApkDecoder(testApk, sConfig).decode(testDir);

        assertTrue(Files.isRegularFile(testDir.resolve("assets/kotlin.kotlin_builtins")));
        assertTrue(Files.isRegularFile(testDir.resolve("assets/ranges/ranges.kotlin_builtins")));
    }
}
