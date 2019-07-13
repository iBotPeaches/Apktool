/**
 *  Copyright (C) 2019 Ryszard Wiśniewski <brut.alll@gmail.com>
 *  Copyright (C) 2019 Connor Tumbleson <connor.tumbleson@gmail.com>
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
package brut.androlib.aapt1;

import brut.androlib.Androlib;
import brut.androlib.ApkDecoder;
import brut.androlib.BaseTest;
import brut.androlib.TestUtils;
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

public class DefaultBaksmaliVariableTest extends BaseTest {

    @BeforeClass
    public static void beforeClass() throws Exception {
        sTmpDir = new ExtFile(OS.createTempDirectory());
        sTestOrigDir = new ExtFile(sTmpDir, "testjar-orig");
        sTestNewDir = new ExtFile(sTmpDir, "testjar-new");
        LOGGER.info("Unpacking testjar...");
        TestUtils.copyResourceDir(DefaultBaksmaliVariableTest.class, "aapt1/issue1481/", sTestOrigDir);

        LOGGER.info("Building issue1481.jar...");
        File testJar = new File(sTmpDir, "issue1481.jar");
        new Androlib().build(sTestOrigDir, testJar);

        LOGGER.info("Decoding issue1481.jar...");
        ApkDecoder apkDecoder = new ApkDecoder(testJar);
        apkDecoder.setOutDir(sTestNewDir);
        apkDecoder.decode();
    }

    @AfterClass
    public static void afterClass() throws BrutException {
        OS.rmdir(sTmpDir);
    }

    @Test
    public void confirmBaksmaliParamsAreTheSame() throws BrutException, IOException {
        String expected = TestUtils.replaceNewlines(".class public final Lcom/ibotpeaches/issue1481/BuildConfig;\n" +
                ".super Ljava/lang/Object;\n" +
                ".source \"BuildConfig.java\"\n" +
                "\n" +
                "\n" +
                "# static fields\n" +
                ".field public static final APPLICATION_ID:Ljava/lang/String; = \"com.ibotpeaches.issue1481\"\n" +
                "\n" +
                ".field public static final BUILD_TYPE:Ljava/lang/String; = \"debug\"\n" +
                "\n" +
                ".field public static final DEBUG:Z\n" +
                "\n" +
                ".field public static final FLAVOR:Ljava/lang/String; = \"\"\n" +
                "\n" +
                ".field public static final VERSION_CODE:I = 0x1\n" +
                "\n" +
                ".field public static final VERSION_NAME:Ljava/lang/String; = \"1.0\"\n" +
                "\n" +
                "\n" +
                "# direct methods\n" +
                ".method static constructor <clinit>()V\n" +
                "    .locals 1\n" +
                "\n" +
                "    .prologue\n" +
                "    .line 7\n" +
                "    const-string v0, \"true\"\n" +
                "\n" +
                "    invoke-static {v0}, Ljava/lang/Boolean;->parseBoolean(Ljava/lang/String;)Z\n" +
                "\n" +
                "    move-result v0\n" +
                "\n" +
                "    sput-boolean v0, Lcom/ibotpeaches/issue1481/BuildConfig;->DEBUG:Z\n" +
                "\n" +
                "    return-void\n" +
                ".end method\n" +
                "\n" +
                ".method public constructor <init>()V\n" +
                "    .locals 0\n" +
                "\n" +
                "    .prologue\n" +
                "    .line 6\n" +
                "    invoke-direct {p0}, Ljava/lang/Object;-><init>()V\n" +
                "\n" +
                "    return-void\n" +
                ".end method");

        byte[] encoded = Files.readAllBytes(Paths.get(sTestNewDir + File.separator + "smali" + File.separator
        + "com" + File.separator + "ibotpeaches" + File.separator + "issue1481" + File.separator + "BuildConfig.smali"));

        String obtained = TestUtils.replaceNewlines(new String(encoded));
        assertEquals(expected, obtained);
    }
}
