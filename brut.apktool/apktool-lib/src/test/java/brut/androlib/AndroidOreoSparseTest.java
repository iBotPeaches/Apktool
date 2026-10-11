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

public class AndroidOreoSparseTest extends BaseTest {

    @BeforeClass
    public static void beforeClass() throws Exception {
        sTestOrigDir = sTmpDir.resolve("issue1594-orig");
        sTestNewDir = sTmpDir.resolve("issue1594-new");

        log("Unpacking sparse.apk...");
        copyResourceDir(AndroidOreoSparseTest.class, "issue1594", sTestOrigDir);

        log("Decoding sparse.apk...");
        Path testApk = sTestOrigDir.resolve("sparse.apk");
        new ApkDecoder(testApk, sConfig).decode(sTestNewDir);

        log("Building sparse.apk...");
        new ApkBuilder(sTestNewDir, sConfig).build(testApk);
    }

    @Test
    public void buildAndDecodeTest() {
        assertTrue(Files.isDirectory(sTestNewDir));
        assertTrue(Files.isDirectory(sTestOrigDir));
    }

    @Test
    public void ensureStringsOreoTest() {
        assertTrue(Files.isRegularFile(sTestNewDir.resolve("res/values-v26/strings.xml")));
    }
}
