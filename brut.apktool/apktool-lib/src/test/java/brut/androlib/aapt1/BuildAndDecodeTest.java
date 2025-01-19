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
package brut.androlib.aapt1;

import brut.androlib.ApkBuilder;
import brut.androlib.ApkDecoder;
import brut.androlib.BaseTest;
import brut.androlib.TestUtils;
import brut.androlib.apk.ApkInfo;
import brut.common.BrutException;
import brut.directory.ExtFile;
import brut.util.OSDetection;
import org.w3c.dom.Document;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.*;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

public class BuildAndDecodeTest extends BaseTest {

    @BeforeClass
    public static void beforeClass() throws Exception {
        sTestOrigDir = new ExtFile(sTmpDir, "testapp-orig");
        sTestNewDir = new ExtFile(sTmpDir, "testapp-new");

        LOGGER.info("Unpacking testapp...");
        TestUtils.copyResourceDir(BuildAndDecodeTest.class, "aapt1/testapp", sTestOrigDir);

        sConfig.setAaptVersion(1);

        LOGGER.info("Building testapp.apk...");
        ExtFile testApk = new ExtFile(sTmpDir, "testapp.apk");
        new ApkBuilder(sTestOrigDir, sConfig).build(testApk);

        LOGGER.info("Decoding testapp.apk...");
        new ApkDecoder(testApk, sConfig).decode(sTestNewDir);
    }

    @Test
    public void buildAndDecodeTest() {
        assertTrue(sTestNewDir.isDirectory());
    }

    @Test
    public void manifestTaggingNotSupressed() throws BrutException {
        compareXmlFiles("AndroidManifest.xml");
    }

    @Test
    public void valuesAnimsTest() throws BrutException {
        compareValuesFiles("values-mcc001/anims.xml");
    }

    @Test
    public void valuesArraysTest() throws BrutException {
        compareValuesFiles("values-mcc001/arrays.xml");
    }

    @Test
    public void valuesArraysCastingTest() throws BrutException {
        compareValuesFiles("values-mcc002/arrays.xml");
        compareValuesFiles("values-mcc003/arrays.xml");
    }

    @Test
    public void valuesAttrsTest() throws BrutException {
        compareValuesFiles("values/attrs.xml");
    }

    @Test
    public void valuesBoolsTest() throws BrutException {
        compareValuesFiles("values-mcc001/bools.xml");
    }

    @Test
    public void valuesColorsTest() throws BrutException {
        compareValuesFiles("values-mcc001/colors.xml");
    }

    @Test
    public void bug702Test() throws BrutException {
        compareValuesFiles("values-mcc001-mnc00/strings.xml");
    }

    @Test
    public void valuesDimensTest() throws BrutException {
        compareValuesFiles("values-mcc001/dimens.xml");
    }

    @Test
    public void valuesDrawablesTest() throws BrutException {
        compareValuesFiles("values-mcc001/drawables.xml");
    }

    @Test
    public void valuesIdsTest() throws BrutException {
        compareValuesFiles("values-mcc001/ids.xml");
    }

    @Test
    public void valuesIntegersTest() throws BrutException {
        compareValuesFiles("values-mcc001/integers.xml");
    }

    @Test
    public void valuesLayoutsTest() throws BrutException {
        compareValuesFiles("values-mcc001/layouts.xml");
    }

    @Test
    public void xmlPluralsTest() throws BrutException {
        compareValuesFiles("values-mcc001/plurals.xml");
    }

    @Test
    public void miuiRegressionTest() throws BrutException {
        compareValuesFiles("values-godzillaui/strings.xml");
    }

    @Test
    public void valuesStringsTest() throws BrutException {
        compareValuesFiles("values-mcc001/strings.xml");
    }

    @Test
    public void valuesStylesTest() throws BrutException {
        compareValuesFiles("values-mcc001/styles.xml");
    }

    @Test
    public void valuesReferencesTest() throws BrutException {
        compareValuesFiles("values-mcc002/strings.xml");
    }

    @Test
    public void valuesExtraLongTest() throws BrutException {
        compareValuesFiles("values-en/strings.xml");
    }

    @Test
    public void valuesExtraLongExactLengthTest() throws BrutException {
        Document doc = loadDocument(new File(sTestNewDir, "res/values-en/strings.xml"));

        // long_string6 should be exactly 0x8888 chars of "a"
        // the valuesExtraLongTest() should handle this
        // but such an edge case, want a specific test
        String expression = "/resources/string[@name='long_string6']/text()";
        String str = evaluateXPath(doc, expression, String.class);
        assertEquals(0x8888, str.length());
    }

    @Test
    public void storedMp3FilesAreNotCompressedTest() throws BrutException, IOException {
        try (ExtFile testApk = new ExtFile(sTmpDir, "testapp.apk")) {
            Integer compLevel = testApk.getDirectory().getCompressionLevel("res/raw/rain.mp3");
            assertEquals(Integer.valueOf(0), compLevel);
        }
    }

    @Test
    public void crossTypeTest() throws BrutException {
        compareValuesFiles("values-mcc003/strings.xml");
        compareValuesFiles("values-mcc003/integers.xml");
        compareValuesFiles("values-mcc003/bools.xml");
    }

    @Test
    public void xmlLiteralsTest() throws BrutException {
        compareXmlFiles("res/xml/literals.xml");
    }

    @Test
    public void xmlReferencesTest() throws BrutException {
        compareXmlFiles("res/xml/references.xml");
    }

    @Test
    public void xmlAccessibilityTest() throws BrutException {
        compareXmlFiles("res/xml/accessibility_service_config.xml");
    }

    @Test
    public void xmlXsdFileTest() throws BrutException {
        compareXmlFiles("res/xml/ww_box_styles_schema.xsd");
    }

    @Test
    public void xmlIdsEmptyTest() throws BrutException {
        compareXmlFiles("res/values/ids.xml");
    }

    @Test
    public void xmlReferenceAttributeTest() throws BrutException {
        compareXmlFiles("res/layout/issue1040.xml");
    }

    @Test
    public void xmlCustomAttributeTest() throws BrutException {
        compareXmlFiles("res/layout/issue1063.xml");
    }

    @Test
    public void xmlSmallNumbersDontEscapeTest() throws BrutException {
        compareXmlFiles("res/layout/issue1130.xml");
    }

    @Test
    public void xmlUniformAutoTextTest() throws BrutException {
        compareXmlFiles("res/layout/issue1674.xml");
    }

    @Test(expected = AssertionError.class)
    public void xmlFillParentBecomesMatchTest() throws BrutException {
        compareXmlFiles("res/layout/issue1274.xml");
    }

    @Test
    public void xmlCustomAttrsNotAndroidTest() throws BrutException {
        compareXmlFiles("res/layout/issue1157.xml");
    }

    @Test
    public void qualifiersTest() throws BrutException {
        compareValuesFiles("values-mcc004-mnc4-en-rUS-ldrtl-sw100dp-w200dp-h300dp"
                + "-long-round-highdr-land-desk-night-xhdpi-finger-keyssoft-12key"
                + "-navhidden-dpad-v26/strings.xml");
    }

    @Test
    public void shortendedMncTest() throws BrutException {
        compareValuesFiles("values-mcc001-mnc1/strings.xml");
    }

    @Test
    public void shortMncHtcTest() throws BrutException {
        compareValuesFiles("values-mnc1/strings.xml");
    }

    @Test
    public void shortMncv2Test() throws BrutException {
        compareValuesFiles("values-mcc238-mnc6/strings.xml");
    }

    @Test
    public void longMncTest() throws BrutException {
        compareValuesFiles("values-mcc238-mnc870/strings.xml");
    }

    @Test
    public void anyDpiTest() throws BrutException {
        compareValuesFiles("values-watch/strings.xml");
    }

    @Test
    public void packed3CharsTest() throws BrutException {
        compareValuesFiles("values-ast-rES/strings.xml");
    }

    @Test
    public void rightToLeftTest() throws BrutException {
        compareValuesFiles("values-ldrtl/strings.xml");
    }

    @Test
    public void scriptBcp47Test() throws BrutException {
        compareValuesFiles("values-b+en+Latn+US/strings.xml");
    }

    @Test
    public void threeLetterLangBcp47Test() throws BrutException {
        compareValuesFiles("values-ast/strings.xml");
    }

    @Test
    public void androidOStringTest() throws BrutException {
        compareValuesFiles("values-ast/strings.xml");
    }

    @Test
    public void twoLetterNotHandledAsBcpTest() {
        assertTrue(new File(sTestNewDir, "res/values-fr").isDirectory());
    }

    @Test
    public void twoLetterLangBcp47Test() throws BrutException {
        compareValuesFiles("values-en-rUS/strings.xml");
    }

    @Test
    public void variantBcp47Test() throws BrutException {
        compareValuesFiles("values-b+en+US+POSIX/strings.xml");
    }

    @Test
    public void fourpartBcp47Test() throws BrutException {
        compareValuesFiles("values-b+ast+Latn+IT+AREVELA/strings.xml");
    }

    @Test
    public void RegionLocaleBcp47Test() throws BrutException {
        compareValuesFiles("values-b+en+Latn+419/strings.xml");
    }

    @Test
    public void numericalRegionBcp47Test() throws BrutException {
        compareValuesFiles("values-b+eng+419/strings.xml");
    }

    @Test
    public void api23ConfigurationsTest() throws BrutException {
        compareValuesFiles("values-round/strings.xml");
        compareValuesFiles("values-notround/strings.xml");
    }

    @Test
    public void api26ConfigurationsTest() throws BrutException {
        compareValuesFiles("values-widecg-v26/strings.xml");
        compareValuesFiles("values-lowdr-v26/strings.xml");
        compareValuesFiles("values-nowidecg-v26/strings.xml");
        compareValuesFiles("values-vrheadset-v26/strings.xml");
    }

    @Test
    public void fontTest() throws BrutException {
        File fontXml = new File(sTestNewDir, "res/font/lobster.xml");
        File fontFile = new File(sTestNewDir, "res/font/lobster_regular.otf");

        // Per #1662, ensure font file is not encoded.
        assertTrue(fontXml.isFile());
        compareXmlFiles("/res/font/lobster.xml");

        // If we properly skipped decoding the font (otf) file, this file should not exist
        assertFalse(new File(sTestNewDir, "res/values/fonts.xml").isFile());
        assertTrue(fontFile.isFile());
    }

    @Test
    public void drawableNoDpiTest() throws BrutException {
        compareBinaryFolder("res/drawable-nodpi");
    }

    @Test
    public void drawableAnyDpiTest() throws BrutException {
        compareBinaryFolder("res/drawable-anydpi");
    }

    @Test
    public void drawableNumberedDpiTest() throws BrutException {
        compareBinaryFolder("res/drawable-534dpi");
    }

    @Test
    public void drawableLdpiTest() throws BrutException {
        compareBinaryFolder("res/drawable-ldpi");
    }

    @Test
    public void drawableMdpiTest() throws BrutException {
        compareBinaryFolder("res/drawable-mdpi");
    }

    @Test
    public void drawableTvdpiTest() throws BrutException {
        compareBinaryFolder("res/drawable-tvdpi");
    }

    @Test
    public void drawableXhdpiTest() throws BrutException {
        compareBinaryFolder("res/drawable-xhdpi");
    }

    @Test
    public void ninePatchImageColorTest() throws IOException {
        String fileName = "res/drawable-xhdpi/9patch.9.png";

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
    public void issue1508Test() throws IOException {
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
    public void issue1511Test() throws IOException {
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
    public void robust9patchTest() throws IOException {
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
        return pixel >> 24 == 0x00;
    }

    @Test
    public void confirmZeroByteFileExtensionIsNotStored() throws BrutException {
        ApkInfo testInfo = ApkInfo.load(sTestNewDir);
        assertFalse(testInfo.doNotCompress.contains("jpg"));
    }

    @Test
    public void confirmZeroByteFileIsStored() throws BrutException {
        ApkInfo testInfo = ApkInfo.load(sTestNewDir);
        assertTrue(testInfo.doNotCompress.contains("assets/0byte_file.jpg"));
    }

    @Test
    public void drawableXxhdpiTest() throws BrutException {
        compareBinaryFolder("res/drawable-xxhdpi");
    }

    @Test
    public void drawableQualifierXxhdpiTest() throws BrutException {
        compareBinaryFolder("res/drawable-xxhdpi-v4");
    }

    @Test
    public void drawableXxxhdpiTest() throws BrutException {
        compareBinaryFolder("res/drawable-xxxhdpi");
    }

    @Test
    public void resRawTest() throws BrutException {
        compareBinaryFolder("res/raw");
    }

    @Test
    public void libsTest() throws BrutException {
        compareBinaryFolder("lib");
    }

    @Test
    public void unknownFolderTest() throws BrutException {
        compareBinaryFolder("unknown");
    }

    @Test
    public void fileAssetTest() throws BrutException {
        compareBinaryFolder("assets/txt");
    }

    @Test
    public void unicodeAssetTest() throws BrutException {
        assumeTrue(!OSDetection.isWindows());
        compareBinaryFolder("assets/unicode-txt");
    }

    @Test
    public void multipleDexTest() throws BrutException {
        compareBinaryFolder("smali_classes2");
        compareBinaryFolder("smali_classes3");
        assertTrue(new File(sTestOrigDir, "build/apk/classes2.dex").isFile());
        assertTrue(new File(sTestOrigDir, "build/apk/classes3.dex").isFile());
    }

    @Test
    public void singleDexTest() throws BrutException {
        compareBinaryFolder("smali");
        assertTrue(new File(sTestOrigDir, "build/apk/classes.dex").isFile());
    }
}
