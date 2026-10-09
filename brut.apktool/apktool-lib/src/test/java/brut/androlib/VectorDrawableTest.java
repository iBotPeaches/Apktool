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

public class VectorDrawableTest extends BaseTest {
    private static final String TEST_APK = "issue1456.apk";

    @BeforeClass
    public static void beforeClass() throws Exception {
        copyResourceDir(VectorDrawableTest.class, "issue1456", sTmpDir);
    }

    @Test
    public void checkIfDrawableFileDecodesProperly() throws Exception {
        Path testApk = sTmpDir.resolve(TEST_APK);
        Path testDir = sTmpDir.resolve(testApk.getFileName() + ".out");
        new ApkDecoder(testApk, sConfig).decode(testDir);

        assertTrue(Files.isRegularFile(testDir.resolve("res/drawable/ic_arrow_drop_down_black_24dp.xml")));
        assertTrue(Files.isRegularFile(testDir.resolve("res/drawable/ic_android_black_24dp.xml")));
    }
}
