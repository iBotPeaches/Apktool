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
package brut.androlib.apk;

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
        assertEquals("2.0.0", apkInfo.version);
        assertEquals("testapp.apk", apkInfo.apkFileName);
        assertFalse(apkInfo.isFrameworkApk);
        assertNotNull(apkInfo.usesFramework);
        assertEquals(1, apkInfo.usesFramework.ids.size());
        assertEquals(1, (long) apkInfo.usesFramework.ids.get(0));
        assertNotNull(apkInfo.packageInfo);
        assertEquals("127", apkInfo.packageInfo.forcedPackageId);
        assertNotNull(apkInfo.versionInfo);
        assertEquals("1", apkInfo.versionInfo.versionCode);
        assertEquals("1.0", apkInfo.versionInfo.versionName);
        assertNotNull(apkInfo.doNotCompress);
        assertEquals(5, apkInfo.doNotCompress.size());
        assertEquals("assets/0byte_file.jpg", apkInfo.doNotCompress.get(0));
        assertEquals("arsc", apkInfo.doNotCompress.get(1));
        assertEquals("png", apkInfo.doNotCompress.get(2));
        assertEquals("mp3", apkInfo.doNotCompress.get(3));
        assertEquals("stored.file", apkInfo.doNotCompress.get(4));
    }

    @Test
    public void checkApkInfoSerialization() throws BrutException, IOException {
        ApkInfo control = ApkInfo.load(
            getClass().getResourceAsStream("/apk/unknown_files.yml"));
        check(control);

        File savedApkInfo = folder.newFile("saved.yml");
        control.save(savedApkInfo);
        try (InputStream in = Files.newInputStream(savedApkInfo.toPath())) {
            check(ApkInfo.load(in));
        }
    }
}
