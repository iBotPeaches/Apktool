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
package brut.androlib.decode;

import brut.androlib.*;
import brut.androlib.exceptions.AndrolibException;
import brut.common.BrutException;
import brut.directory.DirectoryException;
import brut.directory.ExtFile;
import brut.util.OS;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

public class DecodeKotlinCoroutinesTest extends BaseTest {
    private static final String apk = "test-kotlin-coroutines.apk";

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

        Config config = Config.getDefaultConfig();
        config.forceDelete = true;
        // decode kotlin coroutines
        ApkDecoder apkDecoder = new ApkDecoder(config, new File(sTmpDir + File.separator + apk));
        File outDir = new File(sTmpDir + File.separator + apk + ".out");
        apkDecoder.decode(outDir);
        File coroutinesExceptionHandler = new File(sTmpDir + File.separator + apk + ".out" + File.separator + "META-INF" + File.separator + "services", "kotlinx.coroutines.CoroutineExceptionHandler");
        File coroutinesMainDispatcherHandler = new File(sTmpDir + File.separator + apk + ".out" + File.separator + "META-INF" + File.separator + "services", "kotlinx.coroutines.internal.MainDispatcherFactory");

        assert (coroutinesExceptionHandler.exists());
        assert (coroutinesMainDispatcherHandler.exists());
    }

    @Test
    public void kotlinCoroutinesEncodeAfterDecodeTest() throws IOException, BrutException {

        Config config = Config.getDefaultConfig();
        config.forceDelete = true;
        // decode kotlin coroutines
        ApkDecoder apkDecoder = new ApkDecoder(config, new File(sTmpDir + File.separator + apk));
        File outDir = new File(sTmpDir + File.separator + apk + ".out");
        apkDecoder.decode(outDir);

        // build kotlin coroutines
        ExtFile testApk = new ExtFile(sTmpDir, apk + ".out");
        new ApkBuilder(config, testApk).build(null);
        String newApk = apk + ".out" + File.separator + "dist" + File.separator + apk;
        assertTrue(fileExists(newApk));

        // decode kotlin coroutines again
        apkDecoder = new ApkDecoder(config, new File(sTmpDir + File.separator + newApk));
        outDir = new File(sTmpDir + File.separator + apk + ".out.two");
        apkDecoder.decode(outDir);

        Files.readAllBytes(Paths.get(sTmpDir + File.separator + apk + ".out.two" + File.separator + "AndroidManifest.xml"));
        File coroutinesExceptionHandler = new File(sTmpDir + File.separator + apk + ".out.two" + File.separator + "META-INF" + File.separator + "services", "kotlinx.coroutines.CoroutineExceptionHandler");
        File coroutinesMainDispatcherHandler = new File(sTmpDir + File.separator + apk + ".out.two" + File.separator + "META-INF" + File.separator + "services", "kotlinx.coroutines.internal.MainDispatcherFactory");

        assert (coroutinesExceptionHandler.exists());
        assert (coroutinesMainDispatcherHandler.exists());
    }

    private boolean fileExists(String filepath) {
        return Files.exists(Paths.get(sTmpDir.getAbsolutePath() + File.separator + filepath));
    }
}
