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

import org.junit.*;
import static org.junit.Assert.*;

public class ApkInfoReaderTest extends BaseTest {

    private void checkStandard(ApkInfo apkInfo) {
        assertEquals("standard.apk", apkInfo.getApkFileName());
        assertEquals(1, apkInfo.getDoNotCompress().size());
        assertEquals("arsc", apkInfo.getDoNotCompress().get(0));
        assertNotNull(apkInfo.getResourcesInfo());
        assertEquals("127", apkInfo.getResourcesInfo().getPackageId());
        assertNull(apkInfo.getResourcesInfo().getPackageName());
        assertFalse(apkInfo.getResourcesInfo().isSparseEntries());
        assertNotNull(apkInfo.getSdkInfo());
        assertEquals("25", apkInfo.getSdkInfo().getMinSdkVersion());
        assertEquals("30", apkInfo.getSdkInfo().getTargetSdkVersion());
        assertNotNull(apkInfo.getUsesFramework());
        assertNotNull(apkInfo.getUsesFramework().getIds());
        assertEquals(1, apkInfo.getUsesFramework().getIds().size());
        assertEquals(1, (long) apkInfo.getUsesFramework().getIds().get(0));
        assertNull(apkInfo.getUsesFramework().getTag());
        assertNotNull(apkInfo.getVersionInfo());
        assertNull(apkInfo.getVersionInfo().getVersionCode());
        assertNull(apkInfo.getVersionInfo().getVersionName());
    }

    @Test
    public void testStandard() throws BrutException {
        ApkInfo apkInfo = ApkInfo.load(getClass().getResourceAsStream("/meta/standard.yml"));
        checkStandard(apkInfo);
        assertEquals("2.8.1", apkInfo.getVersion());
    }

    @Test
    public void testUnknownFields() throws BrutException {
        ApkInfo apkInfo = ApkInfo.load(getClass().getResourceAsStream("/meta/unknown_fields.yml"));
        checkStandard(apkInfo);
        assertEquals("2.8.1", apkInfo.getVersion());
    }

    @Test
    public void testSkipIncorrectIndent() throws BrutException {
        ApkInfo apkInfo = ApkInfo.load(getClass().getResourceAsStream("/meta/skip_incorrect_indent.yml"));
        checkStandard(apkInfo);
        assertNotEquals("2.0.0", apkInfo.getVersion());
    }

    @Test
    public void testFirstIncorrectIndent() throws BrutException {
        ApkInfo apkInfo = ApkInfo.load(getClass().getResourceAsStream("/meta/first_incorrect_indent.yml"));
        checkStandard(apkInfo);
        assertNotEquals("2.0.0", apkInfo.getVersion());
    }

    @Test
    public void testUnknownFiles() throws BrutException {
        ApkInfo apkInfo = ApkInfo.load(getClass().getResourceAsStream("/meta/unknown_files.yml"));
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
    public void testUlist_with_indent() throws BrutException {
        ApkInfo apkInfo = ApkInfo.load(getClass().getResourceAsStream("/meta/list_with_indent.yml"));
        assertEquals("2.8.0", apkInfo.getVersion());
        assertEquals("basic.apk", apkInfo.getApkFileName());
        assertNotNull(apkInfo.getUsesFramework());
        assertEquals(1, apkInfo.getUsesFramework().getIds().size());
        assertEquals(1, (long) apkInfo.getUsesFramework().getIds().get(0));
        assertEquals("tag", apkInfo.getUsesFramework().getTag());
        assertNotNull(apkInfo.getResourcesInfo());
        assertEquals("127", apkInfo.getResourcesInfo().getPackageId());
        assertEquals("com.test.basic", apkInfo.getResourcesInfo().getPackageName());
        assertTrue(apkInfo.getResourcesInfo().isSparseEntries());
        assertNotNull(apkInfo.getSdkInfo());
        assertEquals("4", apkInfo.getSdkInfo().getMinSdkVersion());
        assertEquals("22", apkInfo.getSdkInfo().getTargetSdkVersion());
        assertEquals("30", apkInfo.getSdkInfo().getMaxSdkVersion());
        assertNotNull(apkInfo.getVersionInfo());
        assertEquals("71", apkInfo.getVersionInfo().getVersionCode());
        assertEquals("1.0.70", apkInfo.getVersionInfo().getVersionName());
        assertNotNull(apkInfo.getDoNotCompress());
        assertEquals(2, apkInfo.getDoNotCompress().size());
        assertEquals("arsc", apkInfo.getDoNotCompress().get(0));
        assertEquals("png", apkInfo.getDoNotCompress().get(1));
    }
}
