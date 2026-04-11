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

import brut.androlib.meta.ApkInfo;
import brut.androlib.res.ResDecoder;
import brut.androlib.res.table.ResEntrySpec;
import brut.androlib.res.table.ResId;
import brut.androlib.res.table.ResPackage;
import brut.androlib.res.table.ResPackageGroup;
import brut.androlib.res.table.ResTable;
import brut.androlib.res.table.value.ResPrimitive;
import brut.androlib.res.table.value.ResReference;
import brut.androlib.res.table.value.ResString;
import brut.androlib.res.table.value.ResStyle;
import brut.androlib.res.xml.ResXmlSerializer;
import brut.directory.Directory;
import brut.directory.FileDirectory;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.Method;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SplitPackageDecodePreserveTest extends BaseTest {
    private static final int PKG_ID_APP = 0x7f;
    private static final int TYPE_ID_STYLE = 0x02;
    private static final int TYPE_ID_ATTR = 0x04;
    private static final int TYPE_ID_STRING = 0x05;

    private static final String SYNTHETIC_ATTR_NAME = ResEntrySpec.DUMMY_PREFIX + "0x7f040001";
    private static final String SPLIT_DUMMY_STRING_NAME = ResEntrySpec.DUMMY_PREFIX + "0x7f050000";
    private static final String SPLIT_REAL_STRING_NAME = "split_label";

    @Test
    public void decodeWithLibraries_keepsSamePackageSplitResourcesAndSkipsParserDummies() throws Exception {
        Config config = new Config("TEST");
        config.setLibraryFiles(new String[] {
            "com.example.split:" + new File(sTmpDir, "config.xxhdpi.apk").getAbsolutePath()
        });

        ResDecoder decoder = new ResDecoder(new ApkInfo(), config);
        ResTable table = decoder.getTable();

        ResPackageGroup appGroup = table.addPackageGroup(PKG_ID_APP, "app");
        ResPackage basePkg = appGroup.getBasePackage();
        ResPackage splitPkg = appGroup.addSubPackage();

        basePkg.addTypeSpec(TYPE_ID_STYLE, "style");
        basePkg.addEntrySpec(TYPE_ID_STYLE, 0x0000, "MainMenuStyle");

        splitPkg.addTypeSpec(TYPE_ID_ATTR, "attr");

        basePkg.addTypeSpec(TYPE_ID_STRING, "string");
        basePkg.addEntrySpec(TYPE_ID_STRING, 0x0000, "real_name");
        basePkg.addEntry(TYPE_ID_STRING, 0x0000, new ResString("real"));

        splitPkg.addTypeSpec(TYPE_ID_STRING, "string");
        splitPkg.addEntrySpec(TYPE_ID_STRING, 0x0000, SPLIT_DUMMY_STRING_NAME);
        splitPkg.addEntry(TYPE_ID_STRING, 0x0000, ResString.EMPTY);
        splitPkg.addEntrySpec(TYPE_ID_STRING, 0x0001, SPLIT_REAL_STRING_NAME);
        splitPkg.addEntry(TYPE_ID_STRING, 0x0001, new ResString("split"));

        ResReference parent = new ResReference(basePkg, ResId.of(PKG_ID_APP, TYPE_ID_STYLE, 0x0001));
        ResReference missingKey = new ResReference(basePkg, ResId.of(PKG_ID_APP, TYPE_ID_ATTR, 0x0001));
        ResStyle style = new ResStyle(parent, new ResStyle.Item[] {
            new ResStyle.Item(missingKey, ResPrimitive.FALSE)
        });
        basePkg.addEntry(TYPE_ID_STYLE, 0x0000, style);

        style.resolveKeys();

        File outDirFile = new File(sTmpDir, "split-preserve-" + System.nanoTime());
        assertTrue(outDirFile.mkdirs());
        Directory outDir = new FileDirectory(outDirFile);

        invokeGenerator(decoder, "generateValuesXmls", basePkg, outDir, new ResXmlSerializer(false));

        String attrsXml = readTextFile(new File(outDirFile, "res/values/attrs.xml"));
        assertTrue(attrsXml.contains(SYNTHETIC_ATTR_NAME));

        String stylesXml = readTextFile(new File(outDirFile, "res/values/styles.xml"));
        assertTrue(stylesXml.contains(SYNTHETIC_ATTR_NAME));

        String stringsXml = readTextFile(new File(outDirFile, "res/values/strings.xml"));
        assertTrue(stringsXml.contains("real_name"));
        assertTrue(stringsXml.contains(SPLIT_REAL_STRING_NAME));
        assertFalse(stringsXml.contains(SPLIT_DUMMY_STRING_NAME));

        invokeGenerator(decoder, "generatePublicXml", basePkg, outDir, new ResXmlSerializer(false));

        String publicXml = readTextFile(new File(outDirFile, "res/values/public.xml"));
        assertTrue(publicXml.contains("real_name"));
        assertTrue(publicXml.contains(SPLIT_REAL_STRING_NAME));
        assertTrue(publicXml.contains(SYNTHETIC_ATTR_NAME));
        assertFalse(publicXml.contains(SPLIT_DUMMY_STRING_NAME));
    }

    private static void invokeGenerator(ResDecoder decoder, String methodName, ResPackage pkg, Directory outDir,
            ResXmlSerializer serial) throws Exception {
        Method method = ResDecoder.class.getDeclaredMethod(methodName, ResPackage.class, Directory.class,
            ResXmlSerializer.class);
        method.setAccessible(true);
        method.invoke(decoder, pkg, outDir, serial);
    }
}
