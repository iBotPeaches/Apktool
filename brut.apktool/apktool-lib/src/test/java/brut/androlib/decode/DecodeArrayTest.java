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
package brut.androlib.decode;

import brut.androlib.BaseTest;
import brut.androlib.TestUtils;
import brut.androlib.apk.ApkInfo;
import brut.androlib.res.ResourcesDecoder;
import brut.androlib.res.data.ResTable;
import brut.androlib.res.data.value.ResArrayValue;
import brut.androlib.res.data.value.ResValue;
import brut.common.BrutException;
import brut.directory.ExtFile;

import org.junit.*;
import static org.junit.Assert.*;

public class DecodeArrayTest extends BaseTest {
    private static ResTable sResTable;

    @BeforeClass
    public static void beforeClass() throws Exception {
        TestUtils.copyResourceDir(MissingVersionManifestTest.class, "decode/issue1994", sTmpDir);

        LOGGER.info("Decoding issue1994.apk...");
        ExtFile testApk = new ExtFile(sTmpDir, "issue1994.apk");
        ApkInfo testInfo = new ApkInfo(testApk);
        ResourcesDecoder resDecoder = new ResourcesDecoder(testInfo, sConfig);
        resDecoder.loadMainPkg();
        sResTable = resDecoder.getResTable();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        sResTable.getApkInfo().getApkFile().close();
    }

    @Test
    public void decodeStringArray() throws BrutException {
        ResValue value = sResTable.getResSpec(0x7f020001).getDefaultResource().getValue();
        assertTrue("Not a ResArrayValue. Found: " + value.getClass(), value instanceof ResArrayValue);
    }

    @Test
    public void decodeArray() throws BrutException {
        ResValue value = sResTable.getResSpec(0x7f020000).getDefaultResource().getValue();
        assertTrue("Not a ResArrayValue. Found: " + value.getClass(), value instanceof ResArrayValue);
    }
}
