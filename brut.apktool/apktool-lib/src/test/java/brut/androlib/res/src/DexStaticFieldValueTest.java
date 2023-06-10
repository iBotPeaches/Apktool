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
package brut.androlib.res.src;

import brut.androlib.*;
import brut.androlib.aapt2.BuildAndDecodeTest;
import brut.androlib.Config;
import brut.common.BrutException;
import brut.directory.ExtFile;
import brut.util.OS;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;

public class DexStaticFieldValueTest extends BaseTest {

    @BeforeClass
    public static void beforeClass() throws Exception {
        TestUtils.cleanFrameworkFile();

        sTmpDir = new ExtFile(OS.createTempDirectory());
        sTestOrigDir = new ExtFile(sTmpDir, "issue2543-orig");
        sTestNewDir = new ExtFile(sTmpDir, "issue2543-new");
        LOGGER.info("Unpacking issue2543...");
        TestUtils.copyResourceDir(BuildAndDecodeTest.class, "decode/issue2543/", sTestOrigDir);

        Config config = Config.getDefaultConfig();

        LOGGER.info("Building issue2543.apk...");
        File testApk = new File(sTmpDir, "issue2543.apk");
        new ApkBuilder(config, sTestOrigDir).build(testApk);

        LOGGER.info("Decoding issue2543.apk...");
        config.baksmaliDebugMode = false;
        ApkDecoder apkDecoder = new ApkDecoder(config, new ExtFile(testApk));
        apkDecoder.decode(sTestNewDir);
    }

    @AfterClass
    public static void afterClass() throws BrutException {
        OS.rmdir(sTmpDir);
    }

    @Test
    public void disassembleDexFileToKeepDefaultParameters() throws IOException {
        String expected = TestUtils.replaceNewlines(
                ".class public LHelloWorld;\n"
                        + ".super Ljava/lang/Object;\n"
                        + "\n"
                        + "\n"
                        + "# static fields\n"
                        + ".field private static b:Z = false\n"
                        + "\n"
                        + ".field private static c:Z = true\n"
                        + "\n"
                        + "\n"
                        + "# direct methods\n"
                        + ".method public static main([Ljava/lang/String;)V\n"
                        + "    .locals 1\n"
                        + "\n"
                        + "    return-void\n"
                        + ".end method");

        byte[] encoded = Files.readAllBytes(Paths.get(sTestNewDir + File.separator + "smali" + File.separator
            + "HelloWorld.smali"));

        String obtained = TestUtils.replaceNewlines(new String(encoded));
        assertEquals(expected, obtained);
    }
}
