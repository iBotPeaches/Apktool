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
package brut.androlib.decode;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import brut.androlib.Androlib;
import brut.androlib.AndrolibException;
import brut.androlib.ApkDecoder;
import brut.androlib.BaseTest;
import brut.androlib.TestUtils;
import brut.common.BrutException;
import brut.directory.DirectoryException;
import brut.directory.ExtFile;
import brut.util.OS;

import static org.junit.Assert.assertTrue;

/**
 * @author Adib Faramarzi <adibfara@gmail.com>
 */
public class DecodeKotlinCoroutinesTest extends BaseTest {
    private static String apk = "test-kotlin-coroutines.apk";

    @BeforeClass
    public static void beforeClass() throws Exception {
        TestUtils.cleanFrameworkFile();
        sTmpDir = new ExtFile(OS.createTempDirectory());
        TestUtils.copyResourceDir(DecodeKotlinCoroutinesTest.class, "decode/kotlin-coroutines/", sTmpDir);
    }

    @AfterClass
    public static void afterClass() throws BrutException {
        OS.rmdir(sTmpDir);
    }


    @Test
    public void kotlinCoroutinesDecodeTest() throws IOException, AndrolibException, DirectoryException {

        // decode kotlin coroutines
        ApkDecoder apkDecoder = new ApkDecoder(new File(sTmpDir + File.separator + apk));
        apkDecoder.setOutDir(new File(sTmpDir + File.separator + apk + ".out"));
        apkDecoder.setForceDelete(true);
        apkDecoder.decode();
        File coroutinesExceptionHandler = new File(sTmpDir + File.separator + apk + ".out" + File.separator + "META-INF" + File.separator + "services", "kotlinx.coroutines.CoroutineExceptionHandler");
        File coroutinenMainDispatcherHandler = new File(sTmpDir + File.separator + apk + ".out" + File.separator + "META-INF" + File.separator + "services", "kotlinx.coroutines.internal.MainDispatcherFactory");

        assert (coroutinesExceptionHandler.exists());
        assert (coroutinenMainDispatcherHandler.exists());
    }

    @Test
    public void kotlinCoroutinesEncodeAfterDecodeTest() throws IOException, BrutException {

        // decode kotlin coroutines
        ApkDecoder apkDecoder = new ApkDecoder(new File(sTmpDir + File.separator + apk));
        apkDecoder.setOutDir(new File(sTmpDir + File.separator + apk + ".out"));
        apkDecoder.setForceDelete(true);
        apkDecoder.decode();

        // build kotlin coroutines
        ExtFile testApk = new ExtFile(sTmpDir, apk + ".out");
        new Androlib().build(testApk, null);
        String newApk = apk + ".out" + File.separator + "dist" + File.separator + apk;
        assertTrue(fileExists(newApk));

        // decode kotlin coroutines again
        apkDecoder = new ApkDecoder(new File(sTmpDir + File.separator + newApk));
        apkDecoder.setOutDir(new File(sTmpDir + File.separator + apk + ".out.two"));
        apkDecoder.setForceDelete(true);
        apkDecoder.decode();

        Files.readAllBytes(Paths.get(sTmpDir + File.separator + apk + ".out.two" + File.separator + "AndroidManifest.xml"));
        File coroutinesExceptionHandler = new File(sTmpDir + File.separator + apk + ".out.two" + File.separator + "META-INF" + File.separator + "services", "kotlinx.coroutines.CoroutineExceptionHandler");
        File coroutinenMainDispatcherHandler = new File(sTmpDir + File.separator + apk + ".out.two" + File.separator + "META-INF" + File.separator + "services", "kotlinx.coroutines.internal.MainDispatcherFactory");

        assert (coroutinesExceptionHandler.exists());
        assert (coroutinenMainDispatcherHandler.exists());
    }

    private boolean fileExists(String filepath) {
        return Files.exists(Paths.get(sTmpDir.getAbsolutePath() + File.separator + filepath));
    }
}