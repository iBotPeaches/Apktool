/*
 *  Copyright (C) 2010 Ryszard Wiśniewski <brut.alll@gmail.com>
 *  Copyright (C) 2010 Connor Tumbleson <connor.tumbleson@gmail.com>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package brut.androlib.aapt1;

import brut.androlib.Androlib;
import brut.androlib.ApkDecoder;
import brut.androlib.BaseTest;
import brut.androlib.TestUtils;
import brut.androlib.meta.MetaInfo;
import brut.directory.ExtFile;
import brut.common.BrutException;
import brut.util.OS;

import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Map;

import brut.util.OSDetection;
import org.junit.*;

import static org.junit.Assert.*;
import static org.junit.Assume.*;

import javax.imageio.ImageIO;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public class BuildAndDecodeTest extends BaseTest {

    @BeforeClass
    public static void beforeClass() throws Exception {
        TestUtils.cleanFrameworkFile();

        sTmpDir = new ExtFile(OS.createTempDirectory());
        sTestOrigDir = new ExtFile(sTmpDir, "testapp-orig");
        sTestNewDir = new ExtFile(sTmpDir, "testapp-new");
        LOGGER.info("Unpacking testapp...");
        TestUtils.copyResourceDir(BuildAndDecodeTest.class, "aapt1/testapp/", sTestOrigDir);

        LOGGER.info("Building testapp.apk...");
        File testApk = new File(sTmpDir, "testapp.apk");
        new Androlib().build(sTestOrigDir, testApk);

        LOGGER.info("Decoding testapp.apk...");
        ApkDecoder apkDecoder = new ApkDecoder(testApk);
        apkDecoder.setOutDir(sTestNewDir);
        apkDecoder.decode();
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
        Map<String, String> strs = TestUtils.parseStringsXml(new File(sTestNewDir, "res/values-en/strings.xml"));

        // long_string6 should be exactly 0x8888 chars of "a"
        // the valuesExtraLongTest() should handle this
        // but such an edge case, want a specific test
        String aaaa = strs.get("long_string6");
        assertEquals(0x8888, aaaa.length());
    }

    @Test
    public void storedMp3FilesAreNotCompressedTest() throws BrutException {
        ExtFile extFile = new ExtFile(sTmpDir, "testapp.apk");
        Integer built = extFile.getDirectory().getCompressionLevel("res/raw/rain.mp3");
        assertEquals(new Integer(0), built);
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
        checkFolderExists("res/values-fr");
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
        File fontXml = new File((sTestNewDir + "/res/font"), "lobster.xml");
        File fontFile = new File((sTestNewDir + "/res/font"), "lobster_regular.otf");

        // Per #1662, ensure font file is not encoded.
        assertTrue(fontXml.isFile());
        compareXmlFiles("/res/font/lobster.xml");

        // If we properly skipped decoding the font (otf) file, this file should not exist
        assertFalse((new File((sTestNewDir + "/res/values"), "fonts.xml")).isFile());
        assertTrue(fontFile.isFile());
    }

    @Test
    public void drawableNoDpiTest() throws BrutException, IOException {
        compareResFolder("drawable-nodpi");
    }

    @Test
    public void drawableAnyDpiTest() throws BrutException, IOException {
        compareResFolder("drawable-anydpi");
    }

    @Test
    public void drawableNumberedDpiTest() throws BrutException, IOException {
        compareResFolder("drawable-534dpi");
    }

    @Test
    public void drawableLdpiTest() throws BrutException, IOException {
        compareResFolder("drawable-ldpi");
    }

    @Test
    public void drawableMdpiTest() throws BrutException, IOException {
        compareResFolder("drawable-mdpi");
    }

    @Test
    public void drawableTvdpiTest() throws BrutException, IOException {
        compareResFolder("drawable-tvdpi");
    }

    @Test
    public void drawableXhdpiTest() throws BrutException, IOException {
        compareResFolder("drawable-xhdpi");
    }

    @Test
    public void ninePatchImageColorTest() throws IOException {
        char slash = File.separatorChar;
        String location = slash + "res" + slash + "drawable-xhdpi" + slash;

        File control = new File((sTestOrigDir + location), "9patch.9.png");
        File test =  new File((sTestNewDir + location), "9patch.9.png");

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
        char slash = File.separatorChar;
        String location = slash + "res" + slash + "drawable-xhdpi" + slash;

        File control = new File((sTestOrigDir + location), "btn_zoom_up_normal.9.png");
        File test = new File((sTestNewDir + location), "btn_zoom_up_normal.9.png");

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
        char slash = File.separatorChar;
        String location = slash + "res" + slash + "drawable-xxhdpi" + slash;

        File control = new File((sTestOrigDir + location), "textfield_activated_holo_dark.9.png");
        File test = new File((sTestNewDir + location), "textfield_activated_holo_dark.9.png");

        BufferedImage controlImage = ImageIO.read(control);
        BufferedImage testImage = ImageIO.read(test);

        // Check entire image as we cannot mess this up
        final int w = controlImage.getWidth(),
                  h = controlImage.getHeight();

        final int[] controlImageGrid = controlImage.getRGB(0, 0, w, h, null, 0, w);
        final int[] testImageGrid = testImage.getRGB(0, 0, w, h, null, 0, w);

        for (int i = 0; i < controlImageGrid.length; i++) {
            assertEquals("Image lost Optical Bounds at i = " + i, controlImageGrid[i], testImageGrid[i]);
        }
    }

    @Test
    public void robust9patchTest() throws IOException {
        String[] ninePatches = {"ic_notification_overlay.9.png", "status_background.9.png",
                "search_bg_transparent.9.png", "screenshot_panel.9.png", "recents_lower_gradient.9.png"};

        char slash = File.separatorChar;
        String location = slash + "res" + slash + "drawable-xxhdpi" + slash;

        for (String ninePatch : ninePatches) {
            File control = new File((sTestOrigDir + location), ninePatch);
            File test = new File((sTestNewDir + location), ninePatch);

            BufferedImage controlImage = ImageIO.read(control);
            BufferedImage testImage = ImageIO.read(test);

            int w = controlImage.getWidth(), h = controlImage.getHeight();

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

    @Test
    public void confirmZeroByteFileIsNotStored() throws BrutException {
        MetaInfo metaInfo = new Androlib().readMetaFile(sTestNewDir);

        for (String item : metaInfo.doNotCompress) {
            assertNotSame(item, "empty");
        }
    }

    @Test
    public void drawableXxhdpiTest() throws BrutException, IOException {
        compareResFolder("drawable-xxhdpi");
    }

    @Test
    public void drawableQualifierXxhdpiTest() throws BrutException, IOException {
        compareResFolder("drawable-xxhdpi-v4");
    }

    @Test
    public void drawableXxxhdpiTest() throws BrutException, IOException {
        compareResFolder("drawable-xxxhdpi");
    }

    @Test
    public void resRawTest() throws BrutException, IOException {
        compareResFolder("raw");
    }

    @Test
    public void libsTest() throws BrutException, IOException {
        compareLibsFolder("libs");
        compareLibsFolder("lib");
    }

    @Test
    public void unknownFolderTest() throws BrutException {
        compareUnknownFiles();
    }

    @Test
    public void fileAssetTest() throws BrutException, IOException {
        compareAssetsFolder("txt");
    }

    @Test
    public void unicodeAssetTest() throws BrutException, IOException {
        assumeTrue(! OSDetection.isWindows());
        compareAssetsFolder("unicode-txt");
    }

    @Test
    public void multipleDexTest() throws BrutException, IOException {
        compareBinaryFolder("/smali_classes2", false);
    }

    @Test
    public void singleDexTest() throws BrutException, IOException {
        compareBinaryFolder("/smali", false);
    }

    @Test
    public void confirmKotlinFolderPersistsTest() {
        checkFolderExists("/kotlin");
    }
}
