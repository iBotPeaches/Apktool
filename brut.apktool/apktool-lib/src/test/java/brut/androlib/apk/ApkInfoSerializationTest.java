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

import brut.androlib.exceptions.AndrolibException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.*;

import static org.junit.Assert.*;

public class ApkInfoSerializationTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void checkApkInfoSerialization() throws IOException, AndrolibException {
        ApkInfo control = ApkInfo.load(
            this.getClass().getResourceAsStream("/apk/unknown_files.yml"));
        check(control);

        File savedApkInfo = folder.newFile( "saved.yml" );
        control.save(savedApkInfo);
        try (FileInputStream fis = new FileInputStream(savedApkInfo)) {
            ApkInfo saved = ApkInfo.load(fis);
            check(saved);
        }
    }

    private void check(ApkInfo apkInfo) {
        assertEquals("2.0.0", apkInfo.version);
        assertEquals("testapp.apk", apkInfo.apkFileName);
        assertFalse(apkInfo.isFrameworkApk);
        assertNotNull(apkInfo.usesFramework);
        assertEquals(1, apkInfo.usesFramework.ids.size());
        assertEquals(1, (long)apkInfo.usesFramework.ids.get(0));
        assertNotNull(apkInfo.packageInfo);
        assertEquals("127", apkInfo.packageInfo.forcedPackageId);
        assertNotNull(apkInfo.versionInfo);
        assertEquals("1", apkInfo.versionInfo.versionCode);
        assertEquals("1.0", apkInfo.versionInfo.versionName);
        assertFalse(apkInfo.resourcesAreCompressed);
        assertNotNull(apkInfo.doNotCompress);
        assertEquals(4, apkInfo.doNotCompress.size());
        assertEquals("assets/0byte_file.jpg", apkInfo.doNotCompress.get(0));
        assertEquals("arsc", apkInfo.doNotCompress.get(1));
        assertEquals("png", apkInfo.doNotCompress.get(2));
        assertEquals("mp3", apkInfo.doNotCompress.get(3));
        assertNotNull(apkInfo.unknownFiles);
        assertEquals(7, apkInfo.unknownFiles.size());
        assertEquals("8", apkInfo.unknownFiles.get("AssetBundle/assets/a.txt"));
        assertEquals("8", apkInfo.unknownFiles.get("AssetBundle/b.txt"));
        assertEquals("8", apkInfo.unknownFiles.get("hidden.file"));
        assertEquals("8", apkInfo.unknownFiles.get("non\u007Fprintable.file"));
        assertEquals("0", apkInfo.unknownFiles.get("stored.file"));
        assertEquals("8", apkInfo.unknownFiles.get("unk_folder/unknown_file"));
        assertEquals("8", apkInfo.unknownFiles.get("lib_bug603/bug603"));
    }
}
