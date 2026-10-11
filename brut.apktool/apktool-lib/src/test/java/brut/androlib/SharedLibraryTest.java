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

public class SharedLibraryTest extends BaseTest {

    @BeforeClass
    public static void beforeClass() throws Exception {
        copyResourceDir(SharedLibraryTest.class, "shared_library", sTmpDir);
    }

    @Test
    public void isSharedResourceDecodingAndRebuildingWorking() throws Exception {
        // decode library.apk
        Path libraryApk = sTmpDir.resolve("library.apk");
        Path libraryDir = sTmpDir.resolve(libraryApk.getFileName() + ".out");
        new ApkDecoder(libraryApk, sConfig).decode(libraryDir);

        // build library.apk
        new ApkBuilder(libraryDir, sConfig).build(null);

        assertTrue(Files.isRegularFile(libraryDir.resolve("dist/" + libraryApk.getFileName())));

        // include library.apk as a shared library
        sConfig.getLibraryFiles().put("com.google.android.test.shared_library", new String[] { libraryApk.toAbsolutePath().toString() });

        // decode client.apk
        Path clientApk = sTmpDir.resolve("client.apk");
        Path clientDir = sTmpDir.resolve(clientApk.getFileName() + ".out");
        new ApkDecoder(clientApk, sConfig).decode(clientDir);

        // build client.apk
        new ApkBuilder(clientDir, sConfig).build(null);

        assertTrue(Files.isRegularFile(clientDir.resolve("dist/" + clientApk.getFileName())));
    }
}
