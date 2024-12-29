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

import brut.androlib.ApkBuilder;
import brut.androlib.ApkDecoder;
import brut.androlib.BaseTest;
import brut.androlib.TestUtils;
import brut.common.BrutException;
import brut.directory.ExtFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.*;
import static org.junit.Assert.*;

public class DexStaticFieldValueTest extends BaseTest {

    @BeforeClass
    public static void beforeClass() throws Exception {
        sTestOrigDir = new ExtFile(sTmpDir, "issue2543-orig");
        sTestNewDir = new ExtFile(sTmpDir, "issue2543-new");

        LOGGER.info("Unpacking issue2543...");
        TestUtils.copyResourceDir(DexStaticFieldValueTest.class, "decode/issue2543", sTestOrigDir);

        sConfig.setBaksmaliDebugMode(false);

        LOGGER.info("Building issue2543.apk...");
        ExtFile testApk = new ExtFile(sTmpDir, "issue2543.apk");
        new ApkBuilder(sTestOrigDir, sConfig).build(testApk);

        LOGGER.info("Decoding issue2543.apk...");
        new ApkDecoder(testApk, sConfig).decode(sTestNewDir);
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

        byte[] encoded = Files.readAllBytes(new File(sTestNewDir, "smali/HelloWorld.smali").toPath());
        String obtained = TestUtils.replaceNewlines(new String(encoded));

        assertEquals(expected, obtained);
    }
}
