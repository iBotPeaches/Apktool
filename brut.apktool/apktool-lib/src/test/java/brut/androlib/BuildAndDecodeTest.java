/**
 *  Copyright 2014 Ryszard Wiśniewski <brut.alll@gmail.com>
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
package brut.androlib;

import brut.androlib.res.util.ExtFile;
import brut.common.BrutException;
import brut.directory.FileDirectory;
import brut.util.OS;
import java.io.*;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.custommonkey.xmlunit.*;
import org.junit.*;
import static org.junit.Assert.*;
import org.xml.sax.SAXException;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public class BuildAndDecodeTest {

    @BeforeClass
    public static void beforeClass() throws Exception, BrutException {
        sTmpDir = new ExtFile(OS.createTempDirectory());
        sTestOrigDir = new ExtFile(sTmpDir, "testapp-orig");
        sTestNewDir = new ExtFile(sTmpDir, "testapp-new");
        LOGGER.info("Unpacking testapp...");
        TestUtils.copyResourceDir(BuildAndDecodeTest.class, "brut/apktool/testapp/", sTestOrigDir);

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
    public void buildAndDecodeTest() throws BrutException {
        assertTrue(sTestNewDir.isDirectory());
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
    public void qualifiersTest() throws BrutException {
        compareValuesFiles("values-mcc004-mnc04-en-rUS-ldrtl-sw100dp-w200dp-h300dp"
                + "-xlarge-long-land-desk-night-xhdpi-finger-keyssoft-12key"
                + "-navhidden-dpad/strings.xml");
    }

    @Test
    public void shortendedMncTest() throws BrutException {
        compareValuesFiles("values-mcc001-mnc01/strings.xml");
    }

    @Test
    public void anyDpiTest() throws BrutException, IOException {
        compareValuesFiles("values-watch/strings.xml");
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
    public void drawableXxhdpiTest() throws BrutException, IOException {
        compareResFolder("drawable-xxhdpi");
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
    }

    @Test
    public void unknownFolderTest() throws BrutException, IOException {
        compareUnknownFiles();
    }

    @Test
    public void multipleDexTest() throws BrutException, IOException {
        compareBinaryFolder("/smali_classes2", false);
    }

    @Test
    public void singleDexTest() throws BrutException, IOException {
        compareBinaryFolder("/smali", false);
    }

    @SuppressWarnings("unchecked")
    private void compareUnknownFiles()
            throws BrutException, IOException {
        Map<String, Object> control = new Androlib().readMetaFile(sTestOrigDir);
        Map<String, Object> test = new Androlib().readMetaFile(sTestNewDir);
        assertTrue(control.containsKey("unknownFiles"));
        assertTrue(test.containsKey("unknownFiles"));

        Map<String, String> control_files = (Map<String, String>)control.get("unknownFiles");
        Map<String, String> test_files = (Map<String, String>)test.get("unknownFiles");
        assertTrue(control_files.size() == test_files.size());
    }

    private boolean compareBinaryFolder(String path, boolean res)
            throws BrutException, IOException {
        String tmp = "";
        if (res) {
            tmp = File.separatorChar + "res" + File.separatorChar;
        }

        FileDirectory fileDirectory = new FileDirectory(sTestOrigDir + tmp + path);

        Set<String> files = fileDirectory.getFiles(true);
        for (String filename : files) {
            File control = new File(filename);

            // hacky fix - load test by changing name of control
            File test =  new File(control.toString().replace("testapp-orig", "testapp-new"));

            if (test.isFile() && control.isFile()) {
                if (control.hashCode() != test.hashCode()) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean compareResFolder(String path) throws BrutException, IOException {
        return compareBinaryFolder(path, true);
    }

    private boolean compareLibsFolder(String path) throws BrutException, IOException {
        return compareBinaryFolder(File.separatorChar + path,false);
    }

    private void compareValuesFiles(String path) throws BrutException {
        compareXmlFiles("res/" + path, new ElementNameAndAttributeQualifier("name"));
    }

    private void compareXmlFiles(String path) throws BrutException {
        compareXmlFiles(path, null);
    }

    private void compareXmlFiles(String path, ElementQualifier qualifier)
            throws BrutException {
        DetailedDiff diff;
        try {
            Reader control = new FileReader(new File(sTestOrigDir, path));
            Reader test = new FileReader(new File(sTestNewDir, path));

            diff = new DetailedDiff(new Diff(control, test));
        } catch (SAXException ex) {
            throw new BrutException(ex);
        } catch (IOException ex) {
            throw new BrutException(ex);
        }

        if (qualifier != null) {
            diff.overrideElementQualifier(qualifier);
        }

        assertTrue(path + ": " + diff.getAllDifferences().toString(), diff.similar());
    }

    private static ExtFile sTmpDir;
    private static ExtFile sTestOrigDir;
    private static ExtFile sTestNewDir;

    private final static Logger LOGGER = Logger.getLogger(BuildAndDecodeTest.class.getName());
}
