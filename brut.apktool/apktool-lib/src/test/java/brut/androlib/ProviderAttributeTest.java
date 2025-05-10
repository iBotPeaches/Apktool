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

import brut.common.BrutException;
import brut.directory.ExtFile;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.*;
import static org.junit.Assert.*;
import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;

public class ProviderAttributeTest extends BaseTest {
    private static final String TEST_APK = "issue636.apk";

    @BeforeClass
    public static void beforeClass() throws Exception {
        TestUtils.copyResourceDir(ProviderAttributeTest.class, "issue636", sTmpDir);
    }

    @Test
    public void isProviderStringReplacementWorking() throws BrutException, IOException, SAXException {
        ExtFile testApk = new ExtFile(sTmpDir, TEST_APK);
        ExtFile testDir = new ExtFile(testApk + ".out");
        new ApkDecoder(testApk, sConfig).decode(testDir);

        new ApkBuilder(testDir, sConfig).build(null);

        ExtFile newApk = new ExtFile(testDir, "dist/" + testApk.getName());
        ExtFile newDir = new ExtFile(testApk + ".out.new");
        new ApkDecoder(newApk, sConfig).decode(newDir);

        String expected = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                + "<manifest package=\"com.ibotpeaches.issue636\" platformBuildVersionCode=\"22\" platformBuildVersionName=\"5.1-1756733\"\n"
                + "  xmlns:android=\"http://schemas.android.com/apk/res/android\">\n"
                + "    <application android:allowBackup=\"true\" android:debuggable=\"true\" android:icon=\"@mipmap/ic_launcher\" android:label=\"@string/app_name\" android:theme=\"@style/AppTheme\">\n"
                + "        <provider android:authorities=\"com.ibotpeaches.issue636.Provider\" android:exported=\"false\" android:grantUriPermissions=\"true\" android:label=\"@string/app_name\" android:multiprocess=\"false\" android:name=\"com.ibotpeaches.issue636.Provider\"/>\n"
                + "        <provider android:authorities=\"com.ibotpeaches.issue636.ProviderTwo\" android:exported=\"false\" android:grantUriPermissions=\"true\" android:label=\"@string/app_name\" android:multiprocess=\"false\" android:name=\"com.ibotpeaches.issue636.ProviderTwo\"/>\n"
                + "    </application>\n"
                + "</manifest>";

        File xml = new File(newDir, "AndroidManifest.xml");
        String obtained = new String(Files.readAllBytes(xml.toPath()));

        assertXMLEqual(expected, obtained);
    }
}
