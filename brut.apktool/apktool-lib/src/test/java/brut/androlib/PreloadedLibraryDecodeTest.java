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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class PreloadedLibraryDecodeTest extends BaseTest {
    private static final String LIBRARY_NAME = "com.example.preload";

    @Test
    public void isExplicitLibraryIncludePreservedInDecodedMeta() throws Exception {
        File testDir = new File(sTmpDir, "preloaded_library_decode");
        File libraryDir = new File(testDir, "library");
        File mainDir = new File(testDir, "main");

        writeFixtureFile(new File(libraryDir, "AndroidManifest.xml"), getManifest("Lib"));
        writeFixtureFile(new File(libraryDir, "apktool.yml"), getApktoolYml("preload-lib.apk", null));
        writeFixtureFile(new File(libraryDir, "res/drawable/lib_icon.xml"), getLibraryDrawable());

        writeFixtureFile(new File(mainDir, "AndroidManifest.xml"), getManifest("Main"));
        writeFixtureFile(new File(mainDir, "apktool.yml"), getApktoolYml("preload-main.apk", LIBRARY_NAME));
        writeFixtureFile(new File(mainDir, "res/values/drawables.xml"), getMainDrawables());

        File libraryApk = new File(testDir, "preload-lib.apk");
        new ApkBuilder(libraryDir, sConfig).build(libraryApk);

        sConfig.setLibraryFiles(new String[] {
            LIBRARY_NAME + ":" + libraryApk.getAbsolutePath()
        });

        File mainApk = new File(testDir, "preload-main.apk");
        new ApkBuilder(mainDir, sConfig).build(mainApk);

        File decodedDir = new File(testDir, "decoded");
        new ApkDecoder(mainApk, sConfig).decode(decodedDir);

        String apktoolYml = readTextFile(new File(decodedDir, "apktool.yml"));
        assertTrue(apktoolYml.contains("usesLibrary:"));
        assertTrue(apktoolYml.contains("- " + LIBRARY_NAME));
    }

    private static void writeFixtureFile(File file, String contents) throws Exception {
        File parentDir = file.getParentFile();
        if (parentDir != null) {
            Files.createDirectories(parentDir.toPath());
        }
        Files.write(file.toPath(), contents.getBytes(StandardCharsets.UTF_8));
    }

    private static String getManifest(String label) {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
            + "    package=\"" + LIBRARY_NAME + "\"\n"
            + "    platformBuildVersionCode=\"35\"\n"
            + "    platformBuildVersionName=\"15\">\n"
            + "    <application android:label=\"" + label + "\" />\n"
            + "</manifest>\n";
    }

    private static String getApktoolYml(String apkFileName, String usesLibrary) {
        StringBuilder yml = new StringBuilder();
        yml.append("version: 2.0.0\n");
        yml.append("apkFileName: ").append(apkFileName).append('\n');
        yml.append("usesFramework:\n");
        yml.append("  ids:\n");
        yml.append("  - 1\n");
        if (usesLibrary != null) {
            yml.append("usesLibrary:\n");
            yml.append("- ").append(usesLibrary).append('\n');
        }
        yml.append("sdkInfo:\n");
        yml.append("  minSdkVersion: 21\n");
        yml.append("  targetSdkVersion: 35\n");
        yml.append("resourcesInfo:\n");
        yml.append("  packageId: 127\n");
        yml.append("versionInfo:\n");
        yml.append("  versionCode: 1\n");
        yml.append("  versionName: 1.0\n");
        return yml.toString();
    }

    private static String getLibraryDrawable() {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<shape xmlns:android=\"http://schemas.android.com/apk/res/android\" android:shape=\"rectangle\">\n"
            + "    <size android:width=\"24dp\" android:height=\"24dp\" />\n"
            + "    <solid android:color=\"#ff0000\" />\n"
            + "</shape>\n";
    }

    private static String getMainDrawables() {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<resources>\n"
            + "    <drawable name=\"alias_icon\">@drawable/lib_icon</drawable>\n"
            + "</resources>\n";
    }
}
