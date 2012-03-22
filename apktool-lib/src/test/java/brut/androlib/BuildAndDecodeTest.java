/**
 *  Copyright 2011 Ryszard Wiśniewski <brut.alll@gmail.com>
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
import brut.util.OS;
import java.io.*;
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
    public static void beforeClass() throws BrutException {
        sTmpDir = new ExtFile(OS.createTempDirectory());
        sTestOrigDir = new ExtFile(sTmpDir, "testapp-orig");
        sTestNewDir = new ExtFile(sTmpDir, "testapp-new");
        File testApk = new File(sTmpDir, "testapp.apk");

        LOGGER.info("Unpacking testapp...");
        TestUtils.copyResourceDir(BuildAndDecodeTest.class,
                "brut/apktool/testapp/", sTestOrigDir);

        LOGGER.info("Building testapp.apk...");
        new Androlib().build(sTestOrigDir, testApk, false, false);

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
    public void valuesArraysTest() throws BrutException {
        compareValuesFiles("values-mcc001/arrays.xml");
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
    public void valuesIdsTest() throws BrutException {
        compareValuesFiles("values-mcc001/ids.xml");
    }

    @Test
    public void valuesIntegersTest() throws BrutException {
        compareValuesFiles("values-mcc001/integers.xml");
    }

    @Test
    public void valuesStringsTest() throws BrutException {
        compareValuesFiles("values-mcc001/strings.xml");
    }

    @Test
    public void valuesReferencesTest() throws BrutException {
        compareValuesFiles("values-mcc002/strings.xml");
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
        compareValuesFiles("values-mcc004-mnc4-en-rUS-sw100dp-w200dp-h300dp" +
                "-xlarge-long-land-television-night-xhdpi-finger-keyssoft-12key" +
                "-navhidden-dpad/strings.xml");
    }

    private void compareValuesFiles(String path) throws BrutException {
        compareXmlFiles("res/" + path,
                new ElementNameAndAttributeQualifier("name"));
    }

    private void compareXmlFiles(String path) throws BrutException {
        compareXmlFiles(path, null);
    }

    private void compareXmlFiles(String path,
            ElementQualifier qualifier) throws BrutException {
        DetailedDiff diff;
        try {
            Reader control = new FileReader(
                    new File(sTestOrigDir, path));
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

        assertTrue(path + ": " +
                diff.getAllDifferences().toString(), diff.similar());
    }

    private static ExtFile sTmpDir;
    private static ExtFile sTestOrigDir;
    private static ExtFile sTestNewDir;

    private final static Logger LOGGER =
            Logger.getLogger(BuildAndDecodeTest.class.getName());
}
