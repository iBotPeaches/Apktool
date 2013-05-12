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
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
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
		TestUtils.copyResourceDir(BuildAndDecodeTest.class,
				"brut/apktool/testapp/", sTestOrigDir);
	}

	@AfterClass
	public static void afterClass() throws BrutException {
		OS.rmdir(sTmpDir);
	}

	@Test
	public void isAaptInstalledTest() throws Exception {
		assertEquals(true, isAaptPresent());
	}

	@Test
	public void encodeAndDecodeTest() throws BrutException, IOException {

		LOGGER.info("Building testapp.apk...");
		File testApk = new File(sTmpDir, "testapp.apk");
		new Androlib().build(sTestOrigDir, testApk,
				BuildAndDecodeTest.returnStock(),"");

		LOGGER.info("Decoding testapp.apk...");
		ApkDecoder apkDecoder = new ApkDecoder(testApk);
		apkDecoder.setOutDir(sTestNewDir);
		apkDecoder.decode();
	}

    @Test
    public void valuesAnimsTest() throws BrutException {
        compareValuesFiles("values-mcc001/anims.xml");
    }

	@Test
	public void valuesArraysTest() throws BrutException {
		compareValuesFiles("values-mcc001/arrays.xml");
		compareValuesFiles("values-mcc002/arrays.xml");
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
		compareValuesFiles("values-mcc004-mnc4-en-rUS-ldrtl-sw100dp-w200dp-h300dp"
				+ "-xlarge-long-land-desk-night-xhdpi-finger-keyssoft-12key"
				+ "-navhidden-dpad/strings.xml");
	}

    @Test
    public void drawableNoDpiTest() throws BrutException, IOException {
        compareResFolder("drawable-nodpi");
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
    public void resRawTest() throws BrutException, IOException {
        compareResFolder("raw");
    }

    @Test
    public void libsTest() throws BrutException, IOException {
        compareLibsFolder("libs");
    }

	private static boolean isAaptPresent() throws Exception {
		boolean result = true;
		try {
			Process proc = Runtime.getRuntime().exec("aapt");
			BufferedReader br = new BufferedReader(new InputStreamReader(
					proc.getErrorStream()));
			String line = null;
			while ((line = br.readLine()) != null) {
			}
		} catch (Exception ex) {
			result = false;
		}
		return result;
	}

    private void compareBinaryFolder(String path, boolean res) throws BrutException, IOException {

        String tmp = "";
        if (res) {
            tmp = File.separatorChar + "res" + File.separatorChar;
        }

        Files.walkFileTree(Paths.get(sTestOrigDir.toPath() + tmp +  path), new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {

                // hacky fix - load test by changing name of control
                File control = file.toFile();
                File test =  new File(file.toString().replace("testapp-orig", "testapp-new"));

                if (test.isFile()) {
                    if (control.hashCode() != test.hashCode()) {
                        sResult = false;
                        return FileVisitResult.TERMINATE;
                    }
                }   else {
                    sResult = false;
                    return FileVisitResult.TERMINATE;
                }
                return FileVisitResult.CONTINUE;
            }

        });
    }

    private boolean compareResFolder(String path) throws BrutException, IOException {
        sResult = true;
        compareBinaryFolder(path, true);
        return sResult;
    }

    private boolean compareLibsFolder(String path) throws BrutException, IOException {
        sResult = true;
        compareBinaryFolder(File.separatorChar + path,false);
        return sResult;
    }

	private void compareValuesFiles(String path) throws BrutException {
		compareXmlFiles("res/" + path, new ElementNameAndAttributeQualifier(
				"name"));
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

		assertTrue(path + ": " + diff.getAllDifferences().toString(),
				diff.similar());
	}

	private static HashMap<String, Boolean> returnStock() throws BrutException {
		HashMap<String, Boolean> tmp = new HashMap<String, Boolean>();
		tmp.put("forceBuildAll", false);
		tmp.put("debug", false);
		tmp.put("verbose", false);
		tmp.put("framework", false);
		tmp.put("update", false);
		tmp.put("copyOriginal", false);

		return tmp;
	}

	private static ExtFile sTmpDir;
	private static ExtFile sTestOrigDir;
	private static ExtFile sTestNewDir;

    private static boolean sResult;

	private final static Logger LOGGER = Logger
			.getLogger(BuildAndDecodeTest.class.getName());
}
