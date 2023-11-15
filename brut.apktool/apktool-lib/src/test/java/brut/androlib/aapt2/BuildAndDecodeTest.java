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
package brut.androlib.aapt2;

import brut.androlib.*;
import brut.androlib.apk.ApkInfo;
import brut.androlib.Config;
import brut.common.BrutException;
import brut.directory.ExtFile;
import brut.util.OS;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;

public class BuildAndDecodeTest extends BaseTest {

    @BeforeClass
    public static void beforeClass() throws Exception {
        TestUtils.cleanFrameworkFile();

        sTmpDir = new ExtFile(OS.createTempDirectory());
        sTestOrigDir = new ExtFile(sTmpDir, "testapp-orig");
        sTestNewDir = new ExtFile(sTmpDir, "testapp-new");
        LOGGER.info("Unpacking testapp...");
        TestUtils.copyResourceDir(BuildAndDecodeTest.class, "aapt2/testapp/", sTestOrigDir);

        Config config = Config.getDefaultConfig();
        config.useAapt2 = true;
        config.verbose = true;

        LOGGER.info("Building testapp.apk...");
        File testApk = new File(sTmpDir, "testapp.apk");
        new ApkBuilder(config, sTestOrigDir).build(testApk);

        LOGGER.info("Decoding testapp.apk...");
        ApkDecoder apkDecoder = new ApkDecoder(testApk);
        apkDecoder.decode(sTestNewDir);
    }

    @AfterClass
    public static void afterClass() throws BrutException {
        OS.rmdir(sTmpDir);
    }

    @Test
    public void buildAndDecodeTest() {
        assertTrue(sTestNewDir.isDirectory());
    }

    @Test
    public void valuesColorsTest() throws BrutException {
        compareValuesFiles("values/colors.xml");
    }

    @Test
    public void valuesStringsTest() throws BrutException {
        compareValuesFiles("values/strings.xml");
    }

    @Test
    public void valuesMaxLengthTest() throws BrutException {
        compareValuesFiles("values-es/strings.xml");
    }

    @Test
    public void valuesBcp47LanguageVariantTest() throws BrutException {
        compareValuesFiles("values-b+iw+660/strings.xml");
    }

    @Test
    public void valuesGrammaticalGenderTest() throws BrutException {
        compareValuesFiles("values-neuter/strings.xml");
        compareValuesFiles("values-feminine/strings.xml");
    }

    @Test
    public void valuesBcp47LanguageScriptRegionVariantTest() throws BrutException {
        compareValuesFiles("values-b+ast+Latn+IT+AREVELA/strings.xml");
        compareValuesFiles("values-b+ast+Hant+IT+ARABEXT/strings.xml");
    }

    @Test
    public void confirmZeroByteFileExtensionIsNotStored() throws BrutException {
        ApkInfo apkInfo = ApkInfo.load(sTestNewDir);
        assertFalse(apkInfo.doNotCompress.contains("jpg"));
    }

    @Test
    public void confirmZeroByteFileIsStored() throws BrutException {
        ApkInfo apkInfo = ApkInfo.load(sTestNewDir);
        assertTrue(apkInfo.doNotCompress.contains("assets/0byte_file.jpg"));
    }

    @Test
    public void navigationResourceTest() throws BrutException {
        compareXmlFiles("res/navigation/nav_graph.xml");
    }

    @Test
    public void xmlIdsEmptyTest() throws BrutException {
        compareXmlFiles("res/values/ids.xml");
    }

    @Test
    public void leadingDollarSignResourceNameTest() throws BrutException {
        compareXmlFiles("res/drawable/$avd_hide_password__0.xml");
        compareXmlFiles("res/drawable/$avd_show_password__0.xml");
        compareXmlFiles("res/drawable/$avd_show_password__1.xml");
        compareXmlFiles("res/drawable/$avd_show_password__2.xml");
        compareXmlFiles("res/drawable/avd_show_password.xml");
    }

    @Test
    public void samsungQmgFilesHandledTest() throws IOException, BrutException {
        compareBinaryFolder("drawable-xhdpi", true);
    }

    @Test
    public void confirmManifestStructureTest() throws BrutException {
        compareXmlFiles("AndroidManifest.xml");
    }

    @Test
    public void xmlXsdFileTest() throws BrutException {
        compareXmlFiles("res/xml/ww_box_styles_schema.xsd");
    }

    @Test
    public void xmlAccessibilityTest() throws BrutException {
        compareXmlFiles("res/xml/accessibility_service_config.xml");
    }

    @Test
    public void multipleDexTest() throws BrutException, IOException {
        compareBinaryFolder("/smali_classes2", false);
        compareBinaryFolder("/smali_classes3", false);

        File classes2Dex = new File(sTestOrigDir, "build/apk/classes2.dex");
        File classes3Dex = new File(sTestOrigDir, "build/apk/classes3.dex");

        assertTrue(classes2Dex.isFile());
        assertTrue(classes3Dex.isFile());
    }

    @Test
    public void singleDexTest() throws BrutException, IOException {
        compareBinaryFolder("/smali", false);

        File classesDex = new File(sTestOrigDir, "build/apk/classes.dex");
        assertTrue(classesDex.isFile());
    }

    @Test
    public void unknownFolderTest() throws BrutException {
        compareUnknownFiles();
    }

    @Test
    public void confirmPlatformManifestValuesTest() throws IOException, SAXException, ParserConfigurationException {
        Document doc = loadDocument(new File(sTestNewDir + "/AndroidManifest.xml"));
        Node application = doc.getElementsByTagName("manifest").item(0);
        NamedNodeMap attr = application.getAttributes();

        Node platformBuildVersionNameAttr = attr.getNamedItem("platformBuildVersionName");
        assertEquals("6.0-2438415", platformBuildVersionNameAttr.getNodeValue());

        Node platformBuildVersionCodeAttr = attr.getNamedItem("platformBuildVersionCode");
        assertEquals("23", platformBuildVersionCodeAttr.getNodeValue());

        Node compileSdkVersionAttr = attr.getNamedItem("compileSdkVersion");
        assertNull("compileSdkVersion should be stripped via aapt2", compileSdkVersionAttr);

        Node compileSdkVersionCodenameAttr = attr.getNamedItem("compileSdkVersionCodename");
        assertNull("compileSdkVersionCodename should be stripped via aapt2", compileSdkVersionCodenameAttr);
    }
}
