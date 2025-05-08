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
        TestUtils.copyResourceDir(DexStaticFieldValueTest.class, "issue2543", sTestOrigDir);

        sConfig.setBaksmaliDebugMode(false);

        LOGGER.info("Building issue2543.jar...");
        ExtFile testJar = new ExtFile(sTmpDir, "issue2543.jar");
        new ApkBuilder(sTestOrigDir, sConfig).build(testJar);

        LOGGER.info("Decoding issue2543.jar...");
        new ApkDecoder(testJar, sConfig).decode(sTestNewDir);
    }

    @Test
    public void disassembleDexFileToKeepDefaultParameters() throws IOException {
        String expected = ".class public LHelloWorld;\n"
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
                + ".end method";

        File smali = new File(sTestNewDir, "smali/HelloWorld.smali");
        String obtained = new String(Files.readAllBytes(smali.toPath()));

        assertEquals(TestUtils.replaceNewlines(expected), TestUtils.replaceNewlines(obtained));
    }
}
