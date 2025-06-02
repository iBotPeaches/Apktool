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
package brut.androlib.meta;

import brut.androlib.BaseTest;
import brut.common.BrutException;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class ApkInfoSerializationTest extends BaseTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private void check(ApkInfo apkInfo) {
        assertEquals("2.0.0", apkInfo.getVersion());
        assertEquals("testapp.apk", apkInfo.getApkFileName());
        assertNotNull(apkInfo.getUsesFramework());
        assertEquals(1, apkInfo.getUsesFramework().getIds().size());
        assertEquals(1, (long) apkInfo.getUsesFramework().getIds().get(0));
        assertNotNull(apkInfo.getResourcesInfo());
        assertEquals("127", apkInfo.getResourcesInfo().getPackageId());
        assertNotNull(apkInfo.getVersionInfo());
        assertEquals("1", apkInfo.getVersionInfo().getVersionCode());
        assertEquals("1.0", apkInfo.getVersionInfo().getVersionName());
        assertNotNull(apkInfo.getDoNotCompress());
        assertEquals(5, apkInfo.getDoNotCompress().size());
        assertEquals("assets/0byte_file.jpg", apkInfo.getDoNotCompress().get(0));
        assertEquals("arsc", apkInfo.getDoNotCompress().get(1));
        assertEquals("png", apkInfo.getDoNotCompress().get(2));
        assertEquals("mp3", apkInfo.getDoNotCompress().get(3));
        assertEquals("stored.file", apkInfo.getDoNotCompress().get(4));
    }

    @Test
    public void checkApkInfoSerialization() throws BrutException, IOException {
        ApkInfo control = ApkInfo.load(getClass().getResourceAsStream("/meta/unknown_files.yml"));
        check(control);

        File savedApkInfo = folder.newFile("saved.yml");
        control.save(savedApkInfo);
        try (InputStream in = Files.newInputStream(savedApkInfo.toPath())) {
            check(ApkInfo.load(in));
        }
    }
}
