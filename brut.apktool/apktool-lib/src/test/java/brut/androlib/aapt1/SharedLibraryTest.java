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
package brut.androlib.aapt1;

import brut.androlib.*;
import brut.androlib.exceptions.AndrolibException;
import brut.androlib.res.Framework;
import brut.directory.ExtFile;
import brut.common.BrutException;
import brut.util.OS;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

public class SharedLibraryTest extends BaseTest {

    @BeforeClass
    public static void beforeClass() throws BrutException {
        TestUtils.cleanFrameworkFile();
        sTmpDir = new ExtFile(OS.createTempDirectory());
        TestUtils.copyResourceDir(SharedLibraryTest.class, "aapt1/shared_libraries/", sTmpDir);
    }

    @AfterClass
    public static void afterClass() throws BrutException {
        OS.rmdir(sTmpDir);
    }

    @Test
    public void isFrameworkTaggingWorking() throws AndrolibException {
        String apkName = "library.apk";

        Config config = Config.getDefaultConfig();
        config.frameworkDirectory = sTmpDir.getAbsolutePath();
        config.frameworkTag = "building";

        new Framework(config).installFramework(new File(sTmpDir + File.separator + apkName));

        assertTrue(fileExists("2-building.apk"));
    }

    @Test
    public void isFrameworkInstallingWorking() throws AndrolibException {
        String apkName = "library.apk";

        Config config = Config.getDefaultConfig();
        config.frameworkDirectory = sTmpDir.getAbsolutePath();

        new Framework(config).installFramework(new File(sTmpDir + File.separator + apkName));

        assertTrue(fileExists("2.apk"));
    }

    @Test
    public void isSharedResourceDecodingAndRebuildingWorking() throws IOException, BrutException {
        String library = "library.apk";
        String client = "client.apk";

        // setup apkOptions
        Config config = Config.getDefaultConfig();
        config.frameworkDirectory = sTmpDir.getAbsolutePath();
        config.frameworkTag = "shared";
        config.useAapt2 = false;

        // install library/framework
        new Framework(config).installFramework(new File(sTmpDir + File.separator + library));
        assertTrue(fileExists("2-shared.apk"));

        // decode client.apk
        ApkDecoder apkDecoder = new ApkDecoder(config, new ExtFile(sTmpDir + File.separator + client));
        File outDir = new File(sTmpDir + File.separator + client + ".out");
        apkDecoder.decode(outDir);

        // decode library.apk
        ApkDecoder libraryDecoder = new ApkDecoder(config, new ExtFile(sTmpDir + File.separator + library));
        outDir = new File(sTmpDir + File.separator + library + ".out");
        libraryDecoder.decode(outDir);

        // build client.apk
        ExtFile clientApk = new ExtFile(sTmpDir, client + ".out");
        new ApkBuilder(config, clientApk).build(null);
        assertTrue(fileExists(client + ".out" + File.separator + "dist" + File.separator + client));

        // build library.apk (shared library)
        ExtFile libraryApk = new ExtFile(sTmpDir, library + ".out");
        new ApkBuilder(config, libraryApk).build(null);
        assertTrue(fileExists(library + ".out" + File.separator + "dist" + File.separator + library));
    }

    private boolean fileExists(String filepath) {
        return Files.exists(Paths.get(sTmpDir.getAbsolutePath() + File.separator + filepath));
    }
}
