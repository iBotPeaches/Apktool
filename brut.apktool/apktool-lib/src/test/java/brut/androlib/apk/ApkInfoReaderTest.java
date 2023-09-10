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
import org.junit.Test;

import static org.junit.Assert.*;

public class ApkInfoReaderTest {

    private void checkStandard(ApkInfo apkInfo) {
        assertEquals("standard.apk", apkInfo.apkFileName);
        assertFalse(apkInfo.resourcesAreCompressed);
        assertEquals(1, apkInfo.doNotCompress.size());
        assertEquals("resources.arsc", apkInfo.doNotCompress.iterator().next());
        assertFalse(apkInfo.isFrameworkApk);
        assertNotNull(apkInfo.packageInfo);
        assertEquals("127", apkInfo.packageInfo.forcedPackageId);
        assertNull(apkInfo.packageInfo.renameManifestPackage);
        assertNotNull(apkInfo.getSdkInfo());
        assertEquals(2, apkInfo.getSdkInfo().size());
        assertEquals("25", apkInfo.getSdkInfo().get("minSdkVersion"));
        assertEquals("30", apkInfo.getSdkInfo().get("targetSdkVersion"));
        assertFalse(apkInfo.sharedLibrary);
        assertFalse(apkInfo.sparseResources);
        assertNotNull(apkInfo.usesFramework);
        assertNotNull(apkInfo.usesFramework.ids);
        assertEquals(1, apkInfo.usesFramework.ids.size());
        assertEquals(1, (long)apkInfo.usesFramework.ids.get(0));
        assertNull(apkInfo.usesFramework.tag);
        assertNotNull(apkInfo.versionInfo);
        assertNull(apkInfo.versionInfo.versionCode);
        assertNull(apkInfo.versionInfo.versionName);
    }

    @Test
    public void testStandard() throws AndrolibException {
        ApkInfo apkInfo = ApkInfo.load(
            this.getClass().getResourceAsStream("/apk/standard.yml"));
        checkStandard(apkInfo);
        assertEquals("2.8.1", apkInfo.version);
    }

    @Test
    public void testUnknownFields() throws AndrolibException {
        ApkInfo apkInfo = ApkInfo.load(
            this.getClass().getResourceAsStream("/apk/unknown_fields.yml"));
        checkStandard(apkInfo);
        assertEquals("2.8.1", apkInfo.version);
    }

    @Test
    public void testSkipIncorrectIndent() throws AndrolibException {
        ApkInfo apkInfo = ApkInfo.load(
            this.getClass().getResourceAsStream("/apk/skip_incorrect_indent.yml"));
        checkStandard(apkInfo);
        assertNotEquals("2.0.0", apkInfo.version);
    }

    @Test
    public void testFirstIncorrectIndent() throws AndrolibException {
        ApkInfo apkInfo = ApkInfo.load(
            this.getClass().getResourceAsStream("/apk/first_incorrect_indent.yml"));
        checkStandard(apkInfo);
        assertNotEquals("2.0.0", apkInfo.version);
    }

    @Test
    public void testUnknownFiles() throws AndrolibException {
        ApkInfo apkInfo = ApkInfo.load(
            this.getClass().getResourceAsStream("/apk/unknown_files.yml"));
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

    @Test
    public void testUlist_with_indent() throws AndrolibException {
        ApkInfo apkInfo = ApkInfo.load(
            this.getClass().getResourceAsStream("/apk/list_with_indent.yml"));
        assertEquals("2.8.0", apkInfo.version);
        assertEquals("basic.apk", apkInfo.apkFileName);
        assertFalse(apkInfo.isFrameworkApk);
        assertNotNull(apkInfo.usesFramework);
        assertEquals(1, apkInfo.usesFramework.ids.size());
        assertEquals(1, (long)apkInfo.usesFramework.ids.get(0));
        assertEquals("tag", apkInfo.usesFramework.tag);
        assertNotNull(apkInfo.packageInfo);
        assertEquals("127", apkInfo.packageInfo.forcedPackageId);
        assertEquals("com.test.basic", apkInfo.packageInfo.renameManifestPackage);
        assertNotNull(apkInfo.getSdkInfo());
        assertEquals(3, apkInfo.getSdkInfo().size());
        assertEquals("4", apkInfo.getSdkInfo().get("minSdkVersion"));
        assertEquals("30", apkInfo.getSdkInfo().get("maxSdkVersion"));
        assertEquals("22", apkInfo.getSdkInfo().get("targetSdkVersion"));
        assertFalse(apkInfo.sharedLibrary);
        assertTrue(apkInfo.sparseResources);
        assertNotNull(apkInfo.unknownFiles);
        assertEquals(1, apkInfo.unknownFiles.size());
        assertEquals("1", apkInfo.unknownFiles.get("hidden.file"));
        assertNotNull(apkInfo.versionInfo);
        assertEquals("71", apkInfo.versionInfo.versionCode);
        assertEquals("1.0.70", apkInfo.versionInfo.versionName);
        assertNotNull(apkInfo.doNotCompress);
        assertEquals(2, apkInfo.doNotCompress.size());
        assertEquals("resources.arsc", apkInfo.doNotCompress.get(0));
        assertEquals("png", apkInfo.doNotCompress.get(1));
    }
}
