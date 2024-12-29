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

import brut.androlib.Config;
import brut.androlib.res.Framework;
import brut.common.BrutException;
import brut.directory.ExtFile;
import brut.util.OS;
import brut.xml.XmlUtils;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.logging.Logger;

import org.junit.*;
import static org.junit.Assert.*;

import org.custommonkey.xmlunit.*;
import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;

public class BaseTest {
    protected static final Logger LOGGER = Logger.getLogger(BaseTest.class.getName());

    protected static Config sConfig;
    protected static File sTmpDir;
    protected static ExtFile sTestOrigDir;
    protected static ExtFile sTestNewDir;

    private static void cleanFrameworkFile() throws BrutException {
        File apkFile = new File(new Framework(sConfig).getDirectory(), "1.apk");
        if (apkFile.isFile()) {
            OS.rmfile(apkFile.getAbsolutePath());
        }
    }

    @BeforeClass
    public static void beforeEachClass() throws Exception {
        sConfig = new Config();
        cleanFrameworkFile();

        sTmpDir = OS.createTempDirectory();
    }

    @AfterClass
    public static void afterEachClass() throws Exception {
        if (sTestOrigDir != null) {
            sTestOrigDir.close();
            sTestOrigDir = null;
        }

        if (sTestNewDir != null) {
            sTestNewDir.close();
            sTestNewDir = null;
        }

        OS.rmdir(sTmpDir);
        sTmpDir = null;

        cleanFrameworkFile();
        sConfig = null;
    }

    @Before
    public void beforeEachTest() throws Exception {
        sConfig = new Config();
    }

    protected void compareBinaryFolder(String path) throws BrutException {
        compareBinaryFolder(sTestOrigDir, sTestNewDir, path);
    }

    protected void compareBinaryFolder(File controlDir, File testDir, String path) throws BrutException {
        ExtFile controlBase = new ExtFile(controlDir, path);
        File testBase = new File(testDir, path);

        boolean exists = true;

        for (String fileName : controlBase.getDirectory().getFiles(true)) {
            File control = new File(controlBase, fileName);
            File test = new File(testBase, fileName);

            if (!control.isFile() || !test.isFile()) {
                exists = false;
            }
        }

        assertTrue(exists);
    }

    protected void compareValuesFiles(String path) throws BrutException {
        compareValuesFiles(sTestOrigDir, sTestNewDir, path);
    }

    protected void compareValuesFiles(File controlDir, File testDir, String path) throws BrutException {
        compareXmlFiles(controlDir, testDir, "res/" + path, new ElementNameAndAttributeQualifier("name"));
    }

    protected void compareXmlFiles(String path) throws BrutException {
        compareXmlFiles(sTestOrigDir, sTestNewDir, path);
    }

    protected void compareXmlFiles(File controlDir, File testDir, String path) throws BrutException {
        compareXmlFiles(controlDir, testDir, path, null);
    }

    private void compareXmlFiles(File controlDir, File testDir, String path, ElementQualifier qualifier)
            throws BrutException {
        try {
            Reader control = new FileReader(new File(controlDir, path));
            Reader test = new FileReader(new File(testDir, path));

            XMLUnit.setEnableXXEProtection(true);

            if (qualifier == null) {
                XMLUnit.setIgnoreWhitespace(true);
                XMLUnit.setIgnoreAttributeOrder(true);
                XMLUnit.setCompareUnmatched(false);

                assertXMLEqual(control, test);
                return;
            }

            DetailedDiff diff = new DetailedDiff(new Diff(control, test));
            diff.overrideElementQualifier(qualifier);

            assertTrue(path + ": " + diff.getAllDifferences().toString(), diff.similar());
        } catch (IOException | SAXException ex) {
            throw new BrutException(ex);
        }
    }

    protected static Document loadDocument(File file) throws BrutException {
        try {
            return XmlUtils.loadDocument(file);
        } catch (IOException | SAXException | ParserConfigurationException ex) {
            throw new BrutException(ex);
        }
    }

    protected static <T> T evaluateXPath(Document doc, String expression, Class<T> returnType)
            throws BrutException {
        try {
            return XmlUtils.evaluateXPath(doc, expression, returnType);
        } catch (XPathExpressionException ex) {
            throw new BrutException(ex);
        }
    }
}
