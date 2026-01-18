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

import brut.androlib.meta.ApkInfo;
import brut.directory.ExtFile;
import brut.util.OSDetection;
import brut.xml.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

import org.junit.*;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

public class BuildAndDecodeApkTest extends BaseTest {
    private static ExtFile sTestApk;

    @BeforeClass
    public static void beforeClass() throws Exception {
        sTestOrigDir = new File(sTmpDir, "testapp-orig");
        sTestNewDir = new File(sTmpDir, "testapp-new");

        LOGGER.info("Unpacking testapp...");
        copyResourceDir(BuildAndDecodeApkTest.class, "testapp", sTestOrigDir);

        sConfig.setVerbose(true);

        LOGGER.info("Building testapp.apk...");
        sTestApk = new ExtFile(sTmpDir, "testapp.apk");
        new ApkBuilder(sTestOrigDir, sConfig).build(sTestApk);

        LOGGER.info("Decoding testapp.apk...");
        new ApkDecoder(sTestApk, sConfig).decode(sTestNewDir);
    }

    @AfterClass
    public static void afterClass() throws Exception {
        sTestApk.close();
    }

    @Test
    public void buildAndDecodeTest() {
        assertTrue(sTestNewDir.isDirectory());
    }

    @Test
    public void confirmFeatureFlagsRecorded() throws Exception {
        ApkInfo testInfo = ApkInfo.load(sTestNewDir);
        assertTrue(testInfo.getFeatureFlags().get("brut.feature.permission"));
        assertTrue(testInfo.getFeatureFlags().get("brut.feature.activity"));
    }

    @Test
    public void confirmZeroByteFileExtensionIsNotStored() throws Exception {
        ApkInfo testInfo = ApkInfo.load(sTestNewDir);
        assertFalse(testInfo.getDoNotCompress().contains("jpg"));
    }

    @Test
    public void confirmZeroByteFileIsStored() throws Exception {
        ApkInfo testInfo = ApkInfo.load(sTestNewDir);
        assertTrue(testInfo.getDoNotCompress().contains("assets/0byte_file.jpg"));
    }

    @Test
    public void confirmManifestStructureTest() throws Exception {
        compareXmlFiles("AndroidManifest.xml");
    }

    @Test
    public void confirmPlatformManifestValuesTest() throws Exception {
        Document doc = XmlUtils.loadDocument(new File(sTestNewDir, "AndroidManifest.xml"));
        Node application = doc.getElementsByTagName("manifest").item(0);
        NamedNodeMap attrs = application.getAttributes();

        Node platformBuildVersionNameAttr = attrs.getNamedItem("platformBuildVersionName");
        assertEquals("6.0-2438415", platformBuildVersionNameAttr.getNodeValue());

        Node platformBuildVersionCodeAttr = attrs.getNamedItem("platformBuildVersionCode");
        assertEquals("23", platformBuildVersionCodeAttr.getNodeValue());

        Node compileSdkVersionAttr = attrs.getNamedItem("compileSdkVersion");
        assertNull("compileSdkVersion should have been stripped", compileSdkVersionAttr);

        Node compileSdkVersionCodenameAttr = attrs.getNamedItem("compileSdkVersionCodename");
        assertNull("compileSdkVersionCodename should have been stripped", compileSdkVersionCodenameAttr);
    }

    @Test
    public void valuesAnimsTest() throws Exception {
        compareValuesFiles("values-mcc001/anims.xml");
    }

    @Test
    public void valuesArraysTest() throws Exception {
        compareValuesFiles("values-mcc001/arrays.xml");
    }

    @Test
    public void valuesArraysCastingTest() throws Exception {
        compareValuesFiles("values-mcc002/arrays.xml");
        compareValuesFiles("values-mcc003/arrays.xml");
    }

    @Test
    public void valuesAttrsTest() throws Exception {
        compareValuesFiles("values/attrs.xml");
    }

    @Test
    public void valuesBoolsTest() throws Exception {
        compareValuesFiles("values-mcc001/bools.xml");
    }

    @Test
    public void valuesColorsTest() throws Exception {
        compareValuesFiles("values/colors.xml");
        compareValuesFiles("values-mcc001/colors.xml");
    }

    @Test
    public void valuesDimensTest() throws Exception {
        compareValuesFiles("values-mcc001/dimens.xml");
    }

    @Test
    public void valuesDrawablesTest() throws Exception {
        compareValuesFiles("values-mcc001/drawables.xml");
    }

    @Test
    public void valuesIdsTest() throws Exception {
        compareValuesFiles("values-mcc001/ids.xml");
    }

    @Test
    public void valuesIntegersTest() throws Exception {
        compareValuesFiles("values-mcc001/integers.xml");
    }

    @Test
    public void valuesLayoutsTest() throws Exception {
        compareValuesFiles("values-mcc001/layouts.xml");
    }

    @Test
    public void valuesPluralsTest() throws Exception {
        compareValuesFiles("values-mcc001/plurals.xml");
    }

    @Test
    public void valuesOverlayableTest() throws Exception {
        compareValuesFiles("values/overlayable.xml");
    }

    @Test
    public void valuesStringsTest() throws Exception {
        compareValuesFiles("values/strings.xml");
        compareValuesFiles("values-mcc001/strings.xml");
    }

    @Test
    public void valuesStylesTest() throws Exception {
        compareValuesFiles("values-mcc001/styles.xml");
    }

    @Test
    public void valuesExtraLongTest() throws Exception {
        compareValuesFiles("values-en/strings.xml");
    }

    @Test
    public void valuesMaxLengthTest() throws Exception {
        Document doc = XmlUtils.loadDocument(new File(sTestNewDir, "res/values-en/strings.xml"));

        // long_string_32767 should be exactly 0x7FFF chars of "a",
        // which is the longest allowed length for UTF-8 strings.
        // String longer than that is replaced with "STRING_TOO_LARGE".
        // valuesExtraLongTest covers this scenario, but we want a specific test
        // for such an edge case.
        String expression = "/resources/string[@name='long_string_32767']/text()";
        String str = XmlUtils.evaluateXPath(doc, expression, String.class);
        assertEquals(0x7FFF, str.length());
    }

    @Test
    public void valuesGrammaticalGenderTest() throws Exception {
        compareValuesFiles("values-neuter/strings.xml");
        compareValuesFiles("values-feminine/strings.xml");
    }

    @Test
    public void bug702Test() throws Exception {
        compareValuesFiles("values-mcc001-mnc00/strings.xml");
    }

    @Test
    public void valuesReferencesTest() throws Exception {
        compareValuesFiles("values-mcc002/strings.xml");
    }

    @Test
    public void crossTypeTest() throws Exception {
        compareValuesFiles("values-mcc003/strings.xml");
        compareValuesFiles("values-mcc003/integers.xml");
        compareValuesFiles("values-mcc003/bools.xml");
    }

    @Test
    public void qualifiersTest() throws Exception {
        compareValuesFiles("values-mcc004-mnc04-en-rUS-ldrtl-sw100dp-w200dp-h300dp"
                + "-long-round-highdr-land-desk-night-xhdpi-finger-keyssoft-12key"
                + "-navhidden-dpad-v26/strings.xml");
    }

    @Test
    public void shortendedMncTest() throws Exception {
        compareValuesFiles("values-mcc001-mnc01/strings.xml");
    }

    @Test
    public void shortMncHtcTest() throws Exception {
        compareValuesFiles("values-mnc01/strings.xml");
    }

    @Test
    public void shortMncv2Test() throws Exception {
        compareValuesFiles("values-mcc238-mnc06/strings.xml");
    }

    @Test
    public void longMncTest() throws Exception {
        compareValuesFiles("values-mcc238-mnc870/strings.xml");
    }

    @Test
    public void anyDpiTest() throws Exception {
        compareValuesFiles("values-watch/strings.xml");
    }

    @Test
    public void packed3CharsTest() throws Exception {
        compareValuesFiles("values-ast-rES/strings.xml");
    }

    @Test
    public void rightToLeftTest() throws Exception {
        compareValuesFiles("values-ldrtl/strings.xml");
    }

    @Test
    public void threeLetterLangBcp47Test() throws Exception {
        compareValuesFiles("values-ast/strings.xml");
    }

    @Test
    public void androidOStringTest() throws Exception {
        compareValuesFiles("values-ast/strings.xml");
    }

    @Test
    public void twoLetterNotHandledAsBcpTest() {
        assertTrue(new File(sTestNewDir, "res/values-fr").isDirectory());
    }

    @Test
    public void twoLetterLangBcp47Test() throws Exception {
        compareValuesFiles("values-en-rUS/strings.xml");
    }

    @Test
    public void scriptBcp47Test() throws Exception {
        compareValuesFiles("values-b+en+Latn+US/strings.xml");
    }

    @Test
    public void regionLocaleBcp47Test() throws Exception {
        compareValuesFiles("values-b+en+Latn+419/strings.xml");
    }

    @Test
    public void numericalRegionBcp47Test() throws Exception {
        compareValuesFiles("values-b+eng+419/strings.xml");
    }

    @Test
    public void variantBcp47Test() throws Exception {
        compareValuesFiles("values-b+en+US+posix/strings.xml");
    }

    @Test
    public void valuesBcp47LanguageVariantTest() throws Exception {
        compareValuesFiles("values-b+iw+660/strings.xml");
    }

    @Test
    public void valuesBcp47LanguageScriptRegionVariantTest() throws Exception {
        compareValuesFiles("values-b+ast+Latn+IT+arevela/strings.xml");
        compareValuesFiles("values-b+ast+Hant+IT+arabext/strings.xml");
    }

    @Test
    public void api23ConfigurationsTest() throws Exception {
        compareValuesFiles("values-round/strings.xml");
        compareValuesFiles("values-notround/strings.xml");
    }

    @Test
    public void api26ConfigurationsTest() throws Exception {
        compareValuesFiles("values-widecg-v26/strings.xml");
        compareValuesFiles("values-lowdr-v26/strings.xml");
        compareValuesFiles("values-nowidecg-v26/strings.xml");
        compareValuesFiles("values-vrheadset-v26/strings.xml");
    }

    @Test
    public void leadingDollarSignResourceNameTest() throws Exception {
        compareXmlFiles("res/drawable/$avd_hide_password__0.xml");
        compareXmlFiles("res/drawable/$avd_show_password__0.xml");
        compareXmlFiles("res/drawable/$avd_show_password__1.xml");
        compareXmlFiles("res/drawable/$avd_show_password__2.xml");
        compareXmlFiles("res/drawable/avd_show_password.xml");
    }

    @Test
    public void fontTest() throws Exception {
        File fontXml = new File(sTestNewDir, "res/font/lobster.xml");
        File fontFile = new File(sTestNewDir, "res/font/lobster_regular.otf");

        // Per #1662, ensure font file is not encoded.
        assertTrue(fontXml.isFile());
        compareXmlFiles("res/font/lobster.xml");

        // If we properly skipped decoding the font (otf) file, this file should not exist
        assertFalse(new File(sTestNewDir, "res/values/fonts.xml").isFile());
        assertTrue(fontFile.isFile());
    }

    @Test
    public void xmlReferenceAttributeTest() throws Exception {
        compareXmlFiles("res/layout/issue1040.xml");
    }

    @Test
    public void xmlCustomAttributeTest() throws Exception {
        compareXmlFiles("res/layout/issue1063.xml");
    }

    @Test
    public void xmlCustomAttrsNotAndroidTest() throws Exception {
        compareXmlFiles("res/layout/issue1157.xml");
    }

    @Test
    public void xmlExpectMatchParentTest() throws Exception {
        compareXmlFiles("res/layout/issue1274.xml");
    }

    @Test
    public void xmlUniformAutoTextTest() throws Exception {
        compareXmlFiles("res/layout/issue1674.xml");
    }

    @Test
    public void navigationResourceTest() throws Exception {
        compareXmlFiles("res/navigation/nav_graph.xml");
    }

    @Test
    public void xmlXsdFileTest() throws Exception {
        compareXmlFiles("res/xml/ww_box_styles_schema.xsd");
    }

    @Test
    public void xmlLiteralsTest() throws Exception {
        compareXmlFiles("res/xml/literals.xml");
    }

    @Test
    public void xmlReferencesTest() throws Exception {
        compareXmlFiles("res/xml/references.xml");
    }

    @Test
    public void xmlAccessibilityTest() throws Exception {
        compareXmlFiles("res/xml/accessibility_service_config.xml");
    }

    @Test
    public void drawableNoDpiTest() throws Exception {
        compareBinaryFolder("res/drawable-nodpi");
    }

    @Test
    public void drawableAnyDpiTest() throws Exception {
        compareBinaryFolder("res/drawable-anydpi");
    }

    @Test
    public void drawableNumberedDpiTest() throws Exception {
        compareBinaryFolder("res/drawable-534dpi");
    }

    @Test
    public void drawableLdpiTest() throws Exception {
        compareBinaryFolder("res/drawable-ldpi");
    }

    @Test
    public void drawableMdpiTest() throws Exception {
        compareBinaryFolder("res/drawable-mdpi");
    }

    @Test
    public void drawableTvdpiTest() throws Exception {
        compareBinaryFolder("res/drawable-tvdpi");
    }

    @Test
    public void drawableXhdpiTest() throws Exception {
        compareBinaryFolder("res/drawable-xhdpi");
    }

    @Test
    public void ninePatchImageColorTest() throws Exception {
        String fileName = "res/drawable-xhdpi/ninepatch.9.png";

        File control = new File(sTestOrigDir, fileName);
        File test = new File(sTestNewDir, fileName);

        BufferedImage controlImage = ImageIO.read(control);
        BufferedImage testImage = ImageIO.read(test);

        // lets start with 0,0 - empty
        assertEquals(controlImage.getRGB(0, 0), testImage.getRGB(0, 0));

        // then with 30, 0 - black
        assertEquals(controlImage.getRGB(30, 0), testImage.getRGB(30, 0));

        // then 30, 30 - blue
        assertEquals(controlImage.getRGB(30, 30), testImage.getRGB(30, 30));
    }

    @Test
    public void issue1508Test() throws Exception {
        String fileName = "res/drawable-xhdpi/btn_zoom_up_normal.9.png";

        File control = new File(sTestOrigDir, fileName);
        File test = new File(sTestNewDir, fileName);

        BufferedImage controlImage = ImageIO.read(control);
        BufferedImage testImage = ImageIO.read(test);

        // 0, 0 = clear
        assertEquals(controlImage.getRGB(0, 0), testImage.getRGB(0, 0));

        // 30, 0 = black line
        assertEquals(controlImage.getRGB(0, 30), testImage.getRGB(0, 30));

        // 30, 30 = greyish button
        assertEquals(controlImage.getRGB(30, 30), testImage.getRGB(30, 30));
    }

    @Test
    public void issue1511Test() throws Exception {
        String fileName = "res/drawable-xxhdpi/textfield_activated_holo_dark.9.png";

        File control = new File(sTestOrigDir, fileName);
        File test = new File(sTestNewDir, fileName);

        BufferedImage controlImage = ImageIO.read(control);
        BufferedImage testImage = ImageIO.read(test);

        // Check entire image as we cannot mess this up
        int w = controlImage.getWidth();
        int h = controlImage.getHeight();

        int[] controlImageGrid = controlImage.getRGB(0, 0, w, h, null, 0, w);
        int[] testImageGrid = testImage.getRGB(0, 0, w, h, null, 0, w);

        for (int i = 0; i < controlImageGrid.length; i++) {
            assertEquals("Image lost Optical Bounds at i = " + i, controlImageGrid[i], testImageGrid[i]);
        }
    }

    @Test
    public void robust9patchTest() throws Exception {
        String[] ninePatches = {
                "ic_notification_overlay.9.png",
                "status_background.9.png",
                "search_bg_transparent.9.png",
                "screenshot_panel.9.png",
                "recents_lower_gradient.9.png",
        };

        for (String ninePatch : ninePatches) {
            String fileName = "res/drawable-xxhdpi/" + ninePatch;

            File control = new File(sTestOrigDir, fileName);
            File test = new File(sTestNewDir, fileName);

            BufferedImage controlImage = ImageIO.read(control);
            BufferedImage testImage = ImageIO.read(test);

            int w = controlImage.getWidth();
            int h = controlImage.getHeight();

            // Check the entire horizontal line
            for (int i = 1; i < w; i++) {
                if (isTransparent(controlImage.getRGB(i, 0))) {
                    assertTrue(isTransparent(testImage.getRGB(i, 0)));
                } else {
                    assertEquals("Image lost npTc chunk on image " + ninePatch + " at (x, y) (" + i + "," + 0 + ")",
                            controlImage.getRGB(i, 0), testImage.getRGB(i, 0));
                }
            }

            // Check the entire vertical line
            for (int i = 1; i < h; i++) {
                if (isTransparent(controlImage.getRGB(0, i))) {
                    assertTrue(isTransparent(testImage.getRGB(0, i)));
                } else {
                    assertEquals("Image lost npTc chunk on image " + ninePatch + " at (x, y) (" + 0 + "," + i + ")",
                            controlImage.getRGB(0, i), testImage.getRGB(0, i));
                }
            }
        }
    }

    private static boolean isTransparent(int pixel) {
        return pixel >> 24 == 0;
    }

    @Test
    public void drawableXxhdpiTest() throws Exception {
        compareBinaryFolder("res/drawable-xxhdpi");
    }

    @Test
    public void drawableQualifierXxhdpiTest() throws Exception {
        compareBinaryFolder("res/drawable-xxhdpi-v4");
    }

    @Test
    public void drawableXxxhdpiTest() throws Exception {
        compareBinaryFolder("res/drawable-xxxhdpi");
    }

    @Test
    public void resRawTest() throws Exception {
        compareBinaryFolder("res/raw");
    }

    @Test
    public void storedMp3FilesAreNotCompressedTest() throws Exception {
        assertEquals(0, sTestApk.getDirectory().getCompressionLevel("res/raw/rain.mp3"));
    }

    @Test
    public void libsTest() throws Exception {
        compareBinaryFolder("lib");
    }

    @Test
    public void fileAssetTest() throws Exception {
        compareBinaryFolder("assets/txt");
    }

    @Test
    public void unicodeAssetTest() throws Exception {
        assumeTrue(!OSDetection.isWindows());
        compareBinaryFolder("assets/unicode-txt");
    }

    @Test
    public void unknownFolderTest() throws Exception {
        compareBinaryFolder("unknown");
    }

    @Test
    public void multipleDexTest() throws Exception {
        compareBinaryFolder("smali_classes2");
        compareBinaryFolder("smali_classes3");
        assertTrue(new File(sTestOrigDir, "build/apk/classes2.dex").isFile());
        assertTrue(new File(sTestOrigDir, "build/apk/classes3.dex").isFile());
    }

    @Test
    public void singleDexTest() throws Exception {
        compareBinaryFolder("smali");
        assertTrue(new File(sTestOrigDir, "build/apk/classes.dex").isFile());
    }
}
