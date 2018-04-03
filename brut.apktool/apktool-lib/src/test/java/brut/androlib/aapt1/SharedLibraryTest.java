/**
 *  Copyright (C) 2018 Ryszard Wiśniewski <brut.alll@gmail.com>
 *  Copyright (C) 2018 Connor Tumbleson <connor.tumbleson@gmail.com>
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

import brut.androlib.*;
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

        ApkOptions apkOptions = new ApkOptions();
        apkOptions.frameworkFolderLocation = sTmpDir.getAbsolutePath();
        apkOptions.frameworkTag = "building";

        new Androlib(apkOptions).installFramework(new File(sTmpDir + File.separator + apkName));

        assertTrue(fileExists("2-building.apk"));
    }

    @Test
    public void isFrameworkInstallingWorking() throws AndrolibException {
        String apkName = "library.apk";

        ApkOptions apkOptions = new ApkOptions();
        apkOptions.frameworkFolderLocation = sTmpDir.getAbsolutePath();

        new Androlib(apkOptions).installFramework(new File(sTmpDir + File.separator + apkName));

        assertTrue(fileExists("2.apk"));
    }

    @Test
    public void isSharedResourceDecodingAndRebuildingWorking() throws IOException, BrutException {
        String library = "library.apk";
        String client = "client.apk";

        // setup apkOptions
        ApkOptions apkOptions = new ApkOptions();
        apkOptions.frameworkFolderLocation = sTmpDir.getAbsolutePath();
        apkOptions.frameworkTag = "shared";

        // install library/framework
        new Androlib(apkOptions).installFramework(new File(sTmpDir + File.separator + library));
        assertTrue(fileExists("2-shared.apk"));

        // decode client.apk
        ApkDecoder apkDecoder = new ApkDecoder(new File(sTmpDir + File.separator + client));
        apkDecoder.setOutDir(new File(sTmpDir + File.separator + client + ".out"));
        apkDecoder.setFrameworkDir(apkOptions.frameworkFolderLocation);
        apkDecoder.setFrameworkTag(apkOptions.frameworkTag);
        apkDecoder.decode();

        // decode library.apk
        ApkDecoder libraryDecoder = new ApkDecoder(new File(sTmpDir + File.separator + library));
        libraryDecoder.setOutDir(new File(sTmpDir + File.separator + library + ".out"));
        libraryDecoder.setFrameworkDir(apkOptions.frameworkFolderLocation);
        libraryDecoder.setFrameworkTag(apkOptions.frameworkTag);
        libraryDecoder.decode();

        // build client.apk
        ExtFile clientApk = new ExtFile(sTmpDir, client + ".out");
        new Androlib(apkOptions).build(clientApk, null);
        assertTrue(fileExists(client + ".out" + File.separator + "dist" + File.separator + client));

        // build library.apk (shared library)
        ExtFile libraryApk = new ExtFile(sTmpDir, library + ".out");
        new Androlib(apkOptions).build(libraryApk, null);
        assertTrue(fileExists(library + ".out" + File.separator + "dist" + File.separator + library));
    }

    private boolean fileExists(String filepath) {
        return Files.exists(Paths.get(sTmpDir.getAbsolutePath() + File.separator + filepath));
    }
}
