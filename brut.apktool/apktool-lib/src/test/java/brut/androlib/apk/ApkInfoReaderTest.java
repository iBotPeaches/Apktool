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

import org.junit.*;
import static org.junit.Assert.*;

public class ApkInfoReaderTest extends BaseTest {

    private void checkStandard(ApkInfo apkInfo) {
        assertEquals("standard.apk", apkInfo.apkFileName);
        assertEquals(1, apkInfo.doNotCompress.size());
        assertEquals("arsc", apkInfo.doNotCompress.iterator().next());
        assertFalse(apkInfo.isFrameworkApk);
        assertNotNull(apkInfo.packageInfo);
        assertEquals("127", apkInfo.packageInfo.forcedPackageId);
        assertNull(apkInfo.packageInfo.renameManifestPackage);
        assertNotNull(apkInfo.sdkInfo);
        assertEquals(2, apkInfo.sdkInfo.size());
        assertEquals("25", apkInfo.sdkInfo.get("minSdkVersion"));
        assertEquals("30", apkInfo.sdkInfo.get("targetSdkVersion"));
        assertFalse(apkInfo.sharedLibrary);
        assertFalse(apkInfo.sparseResources);
        assertNotNull(apkInfo.usesFramework);
        assertNotNull(apkInfo.usesFramework.ids);
        assertEquals(1, apkInfo.usesFramework.ids.size());
        assertEquals(1, (long) apkInfo.usesFramework.ids.get(0));
        assertNull(apkInfo.usesFramework.tag);
        assertNotNull(apkInfo.versionInfo);
        assertNull(apkInfo.versionInfo.versionCode);
        assertNull(apkInfo.versionInfo.versionName);
    }

    @Test
    public void testStandard() throws BrutException {
        ApkInfo apkInfo = ApkInfo.load(
            getClass().getResourceAsStream("/apk/standard.yml"));
        checkStandard(apkInfo);
        assertEquals("2.8.1", apkInfo.version);
    }

    @Test
    public void testUnknownFields() throws BrutException {
        ApkInfo apkInfo = ApkInfo.load(
            getClass().getResourceAsStream("/apk/unknown_fields.yml"));
        checkStandard(apkInfo);
        assertEquals("2.8.1", apkInfo.version);
    }

    @Test
    public void testSkipIncorrectIndent() throws BrutException {
        ApkInfo apkInfo = ApkInfo.load(
            getClass().getResourceAsStream("/apk/skip_incorrect_indent.yml"));
        checkStandard(apkInfo);
        assertNotEquals("2.0.0", apkInfo.version);
    }

    @Test
    public void testFirstIncorrectIndent() throws BrutException {
        ApkInfo apkInfo = ApkInfo.load(
            getClass().getResourceAsStream("/apk/first_incorrect_indent.yml"));
        checkStandard(apkInfo);
        assertNotEquals("2.0.0", apkInfo.version);
    }

    @Test
    public void testUnknownFiles() throws BrutException {
        ApkInfo apkInfo = ApkInfo.load(
            getClass().getResourceAsStream("/apk/unknown_files.yml"));
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
    public void testUlist_with_indent() throws BrutException {
        ApkInfo apkInfo = ApkInfo.load(
            getClass().getResourceAsStream("/apk/list_with_indent.yml"));
        assertEquals("2.8.0", apkInfo.version);
        assertEquals("basic.apk", apkInfo.apkFileName);
        assertFalse(apkInfo.isFrameworkApk);
        assertNotNull(apkInfo.usesFramework);
        assertEquals(1, apkInfo.usesFramework.ids.size());
        assertEquals(1, (long) apkInfo.usesFramework.ids.get(0));
        assertEquals("tag", apkInfo.usesFramework.tag);
        assertNotNull(apkInfo.packageInfo);
        assertEquals("127", apkInfo.packageInfo.forcedPackageId);
        assertEquals("com.test.basic", apkInfo.packageInfo.renameManifestPackage);
        assertNotNull(apkInfo.sdkInfo);
        assertEquals(3, apkInfo.sdkInfo.size());
        assertEquals("4", apkInfo.sdkInfo.get("minSdkVersion"));
        assertEquals("30", apkInfo.sdkInfo.get("maxSdkVersion"));
        assertEquals("22", apkInfo.sdkInfo.get("targetSdkVersion"));
        assertFalse(apkInfo.sharedLibrary);
        assertTrue(apkInfo.sparseResources);
        assertNotNull(apkInfo.versionInfo);
        assertEquals("71", apkInfo.versionInfo.versionCode);
        assertEquals("1.0.70", apkInfo.versionInfo.versionName);
        assertNotNull(apkInfo.doNotCompress);
        assertEquals(2, apkInfo.doNotCompress.size());
        assertEquals("arsc", apkInfo.doNotCompress.get(0));
        assertEquals("png", apkInfo.doNotCompress.get(1));
    }
}
