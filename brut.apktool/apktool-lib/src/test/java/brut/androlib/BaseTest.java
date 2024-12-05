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

import brut.androlib.apk.ApkInfo;
import brut.common.BrutException;
import brut.directory.ExtFile;
import brut.directory.FileDirectory;
import org.custommonkey.xmlunit.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.nio.file.Files;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import static org.junit.Assert.*;

public class BaseTest {
    protected static final Logger LOGGER = Logger.getLogger(BaseTest.class.getName());

    private static final String ACCESS_EXTERNAL_DTD = "http://javax.xml.XMLConstants/property/accessExternalDTD";
    private static final String ACCESS_EXTERNAL_SCHEMA = "http://javax.xml.XMLConstants/property/accessExternalSchema";
    private static final String FEATURE_LOAD_DTD = "http://apache.org/xml/features/nonvalidating/load-external-dtd";
    private static final String FEATURE_DISABLE_DOCTYPE_DECL = "http://apache.org/xml/features/disallow-doctype-decl";

    protected static ExtFile sTmpDir;
    protected static ExtFile sTestOrigDir;
    protected static ExtFile sTestNewDir;

    protected void compareBinaryFolder(String path, boolean res) throws BrutException, IOException {
        boolean exists = true;

        String prefixPath = "";
        if (res) {
            prefixPath = File.separatorChar + "res" + File.separatorChar;
        }

        String location = prefixPath + path;

        FileDirectory fileDirectory = new FileDirectory(sTestOrigDir, location);

        Set<String> files = fileDirectory.getFiles(true);
        for (String fileName : files) {

            File control = new File((sTestOrigDir + location), fileName);
            File test =  new File((sTestNewDir + location), fileName);

            if (! test.isFile() || ! control.isFile()) {
                exists = false;
            }
        }

        assertTrue(exists);
    }

    protected void compareResFolder(String path) throws BrutException, IOException {
        compareBinaryFolder(path, true);
    }

    protected void compareLibsFolder(String path) throws BrutException, IOException {
        compareBinaryFolder(File.separatorChar + path, false);
    }

    protected void compareAssetsFolder(String path) throws BrutException, IOException {
        compareBinaryFolder(File.separatorChar + "assets" + File.separatorChar + path, false);
    }

    protected void compareUnknownFiles() throws BrutException, IOException {
        compareBinaryFolder(File.separatorChar + "unknown", false);
    }

    protected void compareValuesFiles(String path) throws BrutException {
        compareXmlFiles("res/" + path, new ElementNameAndAttributeQualifier("name"));
    }

    protected void compareXmlFiles(String path) throws BrutException {
        compareXmlFiles(path, null);
    }

    protected  void checkFolderExists(String path) {
        File f =  new File(sTestNewDir, path);

        assertTrue(f.isDirectory());
    }

    protected boolean isTransparent(int pixel) {
        return pixel >> 24 == 0x00;
    }

    private void compareXmlFiles(String path, ElementQualifier qualifier) throws BrutException {
        DetailedDiff diff;
        try {
            Reader control = new FileReader(new File(sTestOrigDir, path));
            Reader test = new FileReader(new File(sTestNewDir, path));

            XMLUnit.setEnableXXEProtection(true);

            if (qualifier == null) {
                XMLUnit.setIgnoreWhitespace(true);
                XMLUnit.setIgnoreAttributeOrder(true);
                XMLUnit.setCompareUnmatched(false);
                assertXMLEqual(control, test);
                return;
            }

            diff = new DetailedDiff(new Diff(control, test));
        } catch (SAXException | IOException ex) {
            throw new BrutException(ex);
        }

        diff.overrideElementQualifier(qualifier);
        assertTrue(path + ": " + diff.getAllDifferences().toString(), diff.similar());
    }

    protected static Document loadDocument(File file)
            throws IOException, SAXException, ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(FEATURE_DISABLE_DOCTYPE_DECL, true);
        factory.setFeature(FEATURE_LOAD_DTD, false);

        try {
            factory.setAttribute(ACCESS_EXTERNAL_DTD, " ");
            factory.setAttribute(ACCESS_EXTERNAL_SCHEMA, " ");
        } catch (IllegalArgumentException ex) {
            LOGGER.warning("JAXP 1.5 Support is required to validate XML");
        }

        DocumentBuilder builder = factory.newDocumentBuilder();
        // Not using the parse(File) method on purpose, so that we can control when
        // to close it. Somehow parse(File) does not seem to close the file in all cases.
        try (InputStream in = Files.newInputStream(file.toPath())) {
            return builder.parse(in);
        }
    }

    protected static int getStringEntryCount(Document doc, String key) {
        int count = 0;
        Element resources = doc.getDocumentElement();
        for (int i = 0; i < resources.getChildNodes().getLength(); i++) {
            if (resources.getChildNodes().item(i).getNodeName().equals(key)) {
                count++;
            }
        }
        return count;
    }

    protected static boolean resourceNameContains(Element element, String name) {
        if (element.hasAttribute("name") && element.getAttribute("name").contains(name)) {
            return true;
        }
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE
                    && resourceNameContains((Element) child, name)) {
                return true;
            }
        }
        return false;
    }
}
