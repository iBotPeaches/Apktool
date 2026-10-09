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
import brut.common.Log;
import brut.util.IOUtils;

import java.io.InputStream;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.stream.Stream;

import org.junit.*;
import static org.junit.Assert.assertTrue;
import org.custommonkey.xmlunit.*;
import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;

public abstract class BaseTest {
    private static final String TAG = "TEST";

    protected static Config sConfig;
    protected static Path sTmpDir;
    protected static Path sTestOrigDir;
    protected static Path sTestNewDir;

    static {
        XMLUnit.setEnableXXEProtection(true);
        XMLUnit.setIgnoreAttributeOrder(true);
        XMLUnit.setIgnoreWhitespace(true);
    }

    private static void cleanFrameworkFile() throws Exception {
        Files.deleteIfExists(new Framework(sConfig).getDirectory().resolve("1.apk"));
    }

    @BeforeClass
    public static void beforeEachClass() throws Exception {
        sConfig = new Config(TAG);
        cleanFrameworkFile();

        sTmpDir = Files.createTempDirectory("BRUT");
    }

    @AfterClass
    public static void afterEachClass() throws Exception {
        sTestOrigDir = null;
        sTestNewDir = null;

        IOUtils.deleteDirectory(sTmpDir);
        sTmpDir = null;

        cleanFrameworkFile();
        sConfig = null;
    }

    @Before
    public void beforeEachTest() {
        sConfig = new Config(TAG);
    }

    protected static void log(String message) {
        Log.i(TAG, message);
    }

    protected static void log(String message, Object... args) {
        Log.i(TAG, message, args);
    }

    protected static void copyResourceDir(Class<?> clz, String path, Path dest) throws Exception {
        Path src = Paths.get(clz.getClassLoader().getResource(path).toURI());
        IOUtils.copyDirectory(src, dest);
    }

    protected static String readTextFile(Path file) throws Exception {
        return new String(Files.readAllBytes(file));
    }

    protected static byte[] readHeaderOfFile(Path file, int size) throws Exception {
        byte[] buffer = new byte[size];

        try (InputStream in = Files.newInputStream(file)) {
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

    protected static void compareBinaryFolder(Path controlDir, Path testDir, String path) throws Exception {
        Path control = controlDir.resolve(path);
        Path test = testDir.resolve(path);

        try (Stream<Path> stream = Files.walk(control)) {
            Iterator<Path> it = stream.filter(Files::isRegularFile).iterator();
            while (it.hasNext()) {
                String fileName = control.relativize(it.next()).toString();

                assertTrue(Files.isRegularFile(test.resolve(fileName)));
            }
        }
    }

    protected static void compareValuesFiles(String path) throws Exception {
        compareValuesFiles(sTestOrigDir, sTestNewDir, path);
    }

    protected static void compareValuesFiles(Path controlDir, Path testDir, String path) throws Exception {
        compareXmlFiles(controlDir, testDir, "res/" + path, new ElementNameAndAttributeQualifier("name"));
    }

    protected static void compareXmlFiles(String path) throws Exception {
        compareXmlFiles(sTestOrigDir, sTestNewDir, path, null);
    }

    protected static void compareXmlFiles(Path controlDir, Path testDir, String path) throws Exception {
        compareXmlFiles(controlDir, testDir, path, null);
    }

    private static void compareXmlFiles(Path controlDir, Path testDir, String path, ElementQualifier qualifier) throws Exception {
        try (
            Reader control = Files.newBufferedReader(controlDir.resolve(path));
            Reader test = Files.newBufferedReader(testDir.resolve(path))
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
