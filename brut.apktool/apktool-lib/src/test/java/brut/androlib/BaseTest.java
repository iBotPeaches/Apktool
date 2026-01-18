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
import brut.directory.FileDirectory;
import brut.util.OS;

import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.util.logging.Logger;

import org.junit.*;
import static org.junit.Assert.assertTrue;
import org.custommonkey.xmlunit.*;
import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;

public class BaseTest {
    protected static final Logger LOGGER = Logger.getLogger(BaseTest.class.getName());

    protected static Config sConfig;
    protected static File sTmpDir;
    protected static File sTestOrigDir;
    protected static File sTestNewDir;

    static {
        XMLUnit.setEnableXXEProtection(true);
        XMLUnit.setIgnoreAttributeOrder(true);
        XMLUnit.setIgnoreWhitespace(true);
    }

    private static void cleanFrameworkFile() throws Exception {
        File apkFile = new File(new Framework(sConfig).getDirectory(), "1.apk");
        if (apkFile.isFile()) {
            OS.rmfile(apkFile.getAbsolutePath());
        }
    }

    @BeforeClass
    public static void beforeEachClass() throws Exception {
        sConfig = new Config("TEST");
        cleanFrameworkFile();

        sTmpDir = OS.createTempDirectory();
    }

    @AfterClass
    public static void afterEachClass() throws Exception {
        sTestOrigDir = null;
        sTestNewDir = null;

        OS.rmdir(sTmpDir);
        sTmpDir = null;

        cleanFrameworkFile();
        sConfig = null;
    }

    @Before
    public void beforeEachTest() {
        sConfig = new Config("TEST");
    }

    protected static void copyResourceDir(Class<?> clz, String dirPath, File outDir) throws Exception {
        if (clz == null) {
            clz = Class.class;
        }

        URL dirURL = clz.getClassLoader().getResource(dirPath);
        if (dirURL != null && dirURL.getProtocol().equals("file")) {
            String jarPath = URLDecoder.decode(dirURL.getFile(), "UTF-8");
            new FileDirectory(jarPath).copyToDir(outDir);
            return;
        }

        if (dirURL == null) {
            String className = clz.getName().replace('.', '/') + ".class";
            dirURL = clz.getClassLoader().getResource(className);
        }

        if (dirURL.getProtocol().equals("jar")) {
            String jarPath = URLDecoder.decode(dirURL.getPath().substring(5, dirURL.getPath().indexOf('!')), "UTF-8");
            new FileDirectory(jarPath).copyToDir(outDir);
        }
    }

    protected static String readTextFile(File file) throws Exception {
        return new String(Files.readAllBytes(file.toPath()));
    }

    protected static byte[] readHeaderOfFile(File file, int size) throws Exception {
        byte[] buffer = new byte[size];

        try (InputStream in = Files.newInputStream(file.toPath())) {
            if (in.read(buffer) != buffer.length) {
                throw new IOException("File size too small for buffer length: " + size);
            }
        }

        return buffer;
    }

    protected static String replaceNewlines(String value) {
        return value.replaceAll("[\n\r]", "");
    }

    protected static void compareBinaryFolder(String path) throws Exception {
        compareBinaryFolder(sTestOrigDir, sTestNewDir, path);
    }

    protected static void compareBinaryFolder(File controlDir, File testDir, String path) throws Exception {
        File controlBase = new File(controlDir, path);
        File testBase = new File(testDir, path);

        boolean exists = true;

        for (String fileName : new FileDirectory(controlBase).getFiles(true)) {
            File control = new File(controlBase, fileName);
            File test = new File(testBase, fileName);

            if (!control.isFile() || !test.isFile()) {
                exists = false;
            }
        }

        assertTrue(exists);
    }

    protected static void compareValuesFiles(String path) throws Exception {
        compareValuesFiles(sTestOrigDir, sTestNewDir, path);
    }

    protected static void compareValuesFiles(File controlDir, File testDir, String path) throws Exception {
        compareXmlFiles(controlDir, testDir, "res/" + path, new ElementNameAndAttributeQualifier("name"));
    }

    protected static void compareXmlFiles(String path) throws Exception {
        compareXmlFiles(sTestOrigDir, sTestNewDir, path, null);
    }

    protected static void compareXmlFiles(File controlDir, File testDir, String path) throws Exception {
        compareXmlFiles(controlDir, testDir, path, null);
    }

    private static void compareXmlFiles(File controlDir, File testDir, String path, ElementQualifier qualifier) throws Exception {
        try (
            Reader control = new FileReader(new File(controlDir, path));
            Reader test = new FileReader(new File(testDir, path))
        ) {
            if (qualifier == null) {
                assertXMLEqual(control, test);
                return;
            }

            DetailedDiff diff = new DetailedDiff(new Diff(control, test));
            diff.overrideElementQualifier(qualifier);

            assertTrue(path + ": " + diff.getAllDifferences().toString(), diff.similar());
        }
    }
}
