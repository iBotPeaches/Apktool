/**
 *  Copyright (C) 2017 Ryszard Wiśniewski <brut.alll@gmail.com>
 *  Copyright (C) 2017 Connor Tumbleson <connor.tumbleson@gmail.com>
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

import brut.androlib.meta.MetaInfo;
import brut.common.BrutException;
import brut.directory.ExtFile;
import brut.directory.FileDirectory;
import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.ElementNameAndAttributeQualifier;
import org.custommonkey.xmlunit.ElementQualifier;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class BaseTest {

    void compareUnknownFiles() throws BrutException {
        MetaInfo control = new Androlib().readMetaFile(sTestOrigDir);
        MetaInfo test = new Androlib().readMetaFile(sTestNewDir);
        assertNotNull(control.unknownFiles);
        assertNotNull(test.unknownFiles);

        Map<String, String> controlFiles = control.unknownFiles;
        Map<String, String> testFiles = test.unknownFiles;
        assertTrue(controlFiles.size() == testFiles.size());

        // Make sure that the compression methods are still the same
        for (Map.Entry<String, String> controlEntry : controlFiles.entrySet()) {
            assertTrue(controlEntry.getValue().equals(testFiles.get(controlEntry.getKey())));
        }
    }

    void compareBinaryFolder(String path, boolean res) throws BrutException, IOException {
        Boolean exists = true;

        String prefixPath = "";
        if (res) {
            prefixPath = File.separatorChar + "res" + File.separatorChar;
        }

        String location = prefixPath + path;

        FileDirectory fileDirectory = new FileDirectory(sTestOrigDir, location);

        Set<String> files = fileDirectory.getFiles(true);
        for (String filename : files) {

            File control = new File((sTestOrigDir + location), filename);
            File test =  new File((sTestNewDir + location), filename);

            if (! test.isFile() || ! control.isFile()) {
                exists = false;
            }
        }

        assertTrue(exists);
    }

    void compareResFolder(String path) throws BrutException, IOException {
        compareBinaryFolder(path, true);
    }

    void compareLibsFolder(String path) throws BrutException, IOException {
        compareBinaryFolder(File.separatorChar + path, false);
    }

    void compareAssetsFolder(String path) throws BrutException, IOException {
        compareBinaryFolder(File.separatorChar + "assets" + File.separatorChar + path, false);
    }

    void compareValuesFiles(String path) throws BrutException {
        compareXmlFiles("res/" + path, new ElementNameAndAttributeQualifier("name"));
    }

    void compareXmlFiles(String path) throws BrutException {
        compareXmlFiles(path, null);
    }

    void checkFolderExists(String path) {
        File f =  new File(sTestNewDir, path);

        assertTrue(f.isDirectory());
    }

    boolean isTransparent(int pixel) {
        return pixel >> 24 == 0x00;
    }

    private void compareXmlFiles(String path, ElementQualifier qualifier) throws BrutException {
        DetailedDiff diff;
        try {
            Reader control = new FileReader(new File(sTestOrigDir, path));
            Reader test = new FileReader(new File(sTestNewDir, path));

            diff = new DetailedDiff(new Diff(control, test));
        } catch (SAXException | IOException ex) {
            throw new BrutException(ex);
        }

        if (qualifier != null) {
            diff.overrideElementQualifier(qualifier);
        }

        assertTrue(path + ": " + diff.getAllDifferences().toString(), diff.similar());
    }

    protected static ExtFile sTmpDir;
    protected static ExtFile sTestOrigDir;
    protected static ExtFile sTestNewDir;

    protected final static Logger LOGGER = Logger.getLogger(BaseTest.class.getName());
}
