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

import java.io.File;

import org.junit.*;
import static org.junit.Assert.*;

public class DexStaticFieldValueTest extends BaseTest {

    @BeforeClass
    public static void beforeClass() throws Exception {
        sTestOrigDir = new File(sTmpDir, "issue2543-orig");
        sTestNewDir = new File(sTmpDir, "issue2543-new");

        log("Unpacking issue2543...");
        copyResourceDir(DexStaticFieldValueTest.class, "issue2543", sTestOrigDir);

        sConfig.setBaksmaliDebugMode(false);

        log("Building issue2543.jar...");
        File testJar = new File(sTmpDir, "issue2543.jar");
        new ApkBuilder(sTestOrigDir, sConfig).build(testJar);

        log("Decoding issue2543.jar...");
        new ApkDecoder(testJar, sConfig).decode(sTestNewDir);
    }

    @Test
    public void disassembleDexFileToKeepDefaultParameters() throws Exception {
        String expected =
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
          + ".end method";

        String obtained = readTextFile(new File(sTestNewDir, "smali/HelloWorld.smali"));

        assertEquals(replaceNewlines(expected), replaceNewlines(obtained));
    }
}
