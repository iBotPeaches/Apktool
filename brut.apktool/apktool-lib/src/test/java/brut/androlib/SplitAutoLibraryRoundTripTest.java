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

import org.junit.Test;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static org.junit.Assert.assertTrue;

public class SplitAutoLibraryRoundTripTest extends BaseTest {
    private static final String PACKAGE_NAME = "com.example.split";
    private static final String BASE_APK_NAME = "base.apk";
    private static final String SPLIT_APK_NAME = "config.xxhdpi.apk";

    @Test
    public void splitDecodeAutoPreservesDependentStyleAndStoresLibraryFile() throws Exception {
        File testDir = new File(sTmpDir, "split_auto_library_roundtrip");
        File baseDir = new File(testDir, "base");
        File splitDir = new File(testDir, "split");

        writeFixtureFile(new File(baseDir, "AndroidManifest.xml"), getManifest("Base"));
        writeFixtureFile(new File(baseDir, "apktool.yml"), getApktoolYml(BASE_APK_NAME, null));
        writeFixtureFile(new File(baseDir, "res/values/attrs.xml"), getBaseAttrsXml());
        writeFixtureFile(new File(baseDir, "res/values/styles.xml"), getBaseStylesXml());

        writeFixtureFile(new File(splitDir, "AndroidManifest.xml"), getManifest("Split"));
        writeFixtureFile(new File(splitDir, "apktool.yml"), getApktoolYml(SPLIT_APK_NAME, PACKAGE_NAME));
        writeFixtureFile(new File(splitDir, "res/values-hdpi/styles.xml"), getSplitStylesXml());

        File baseApk = new File(testDir, BASE_APK_NAME);
        new ApkBuilder(baseDir, new Config("TEST")).build(baseApk);

        Config splitBuildConfig = new Config("TEST");
        splitBuildConfig.setLibraryFiles(new String[] {
            PACKAGE_NAME + ":" + baseApk.getAbsolutePath()
        });

        File splitApk = new File(testDir, SPLIT_APK_NAME);
        new ApkBuilder(splitDir, splitBuildConfig).build(splitApk);

        File decodedSplitDir = new File(testDir, "decoded-split");
        new ApkDecoder(splitApk, new Config("TEST")).decode(decodedSplitDir);

        assertTrue(new File(decodedSplitDir, "original/apktool-libs/" + PACKAGE_NAME + ".apk").isFile());

        String decodedStylesXml = readStylesXml(decodedSplitDir);
        assertTrue(decodedStylesXml.contains("<item name=\"barLength\">18.0dp</item>"));
        assertTrue(decodedStylesXml.contains("<item name=\"drawableSize\">24.0dp</item>"));
        assertTrue(decodedStylesXml.contains("<item name=\"gapBetweenBars\">3.0dp</item>"));

        String apktoolYml = readTextFile(new File(decodedSplitDir, "apktool.yml"));
        assertTrue(apktoolYml.contains("libraryFiles:"));
        assertTrue(apktoolYml.contains("- " + PACKAGE_NAME + ":original/apktool-libs/" + PACKAGE_NAME + ".apk"));
    }

    @Test
    public void splitRoundTripKeepsOriginalStyleIdWithAutoLibraryBuild() throws Exception {
        File testDir = new File(sTmpDir, "split_auto_library_stable_ids");
        File baseDir = new File(testDir, "base");
        File splitDir = new File(testDir, "split");

        writeFixtureFile(new File(baseDir, "AndroidManifest.xml"), getManifest("Base"));
        writeFixtureFile(new File(baseDir, "apktool.yml"), getApktoolYml(BASE_APK_NAME, null));
        writeFixtureFile(new File(baseDir, "res/values/public.xml"), getBasePublicXml());
        writeFixtureFile(new File(baseDir, "res/values/styles.xml"), getBaseStableStylesXml());
        writeFixtureFile(new File(baseDir, "res/animator/design_appbar_state_list_animator.xml"), getBaseAnimatorXml());

        writeFixtureFile(new File(splitDir, "AndroidManifest.xml"), getManifest("Split"));
        writeFixtureFile(new File(splitDir, "apktool.yml"), getApktoolYml(SPLIT_APK_NAME, PACKAGE_NAME));
        writeFixtureFile(new File(splitDir, "res/values/public.xml"), getSplitPublicXml());
        writeFixtureFile(new File(splitDir, "res/values-hdpi/styles.xml"), getSplitStableStylesXml());

        File baseApk = new File(testDir, BASE_APK_NAME);
        new ApkBuilder(baseDir, new Config("TEST")).build(baseApk);

        Config splitBuildConfig = new Config("TEST");
        splitBuildConfig.setLibraryFiles(new String[] {
            PACKAGE_NAME + ":" + baseApk.getAbsolutePath()
        });

        File splitApk = new File(testDir, SPLIT_APK_NAME);
        new ApkBuilder(splitDir, splitBuildConfig).build(splitApk);

        File decodedSplitDir = new File(testDir, "decoded-split");
        new ApkDecoder(splitApk, new Config("TEST")).decode(decodedSplitDir);

        File rebuiltSplitApk = new File(testDir, "rebuilt-split.apk");
        new ApkBuilder(decodedSplitDir, new Config("TEST")).build(rebuiltSplitApk);

        Config redecodeConfig = new Config("TEST");
        redecodeConfig.setLibraryFiles(new String[] {
            PACKAGE_NAME + ":" + new File(decodedSplitDir, "original/apktool-libs/" + PACKAGE_NAME + ".apk").getAbsolutePath()
        });

        File redecodedSplitDir = new File(testDir, "redecoded-split");
        new ApkDecoder(rebuiltSplitApk, redecodeConfig).decode(redecodedSplitDir);

        String publicXml = readTextFile(new File(redecodedSplitDir, "res/values/public.xml"));
        assertTrue(publicXml.contains("name=\"SplitStyle\" id=\"0x7f1400e4\""));
    }

    @Test
    public void splitDecodeAutoPersistsSdkInfoNeededForAdaptiveIconRebuild() throws Exception {
        File testDir = new File(sTmpDir, "split_auto_library_sdk_info");
        File baseDir = new File(testDir, "base");
        File splitDir = new File(testDir, "split");
        File manifestOnlySplitDir = new File(testDir, "manifest-only-split");

        writeFixtureFile(new File(baseDir, "AndroidManifest.xml"), getManifest("Base"));
        writeFixtureFile(new File(baseDir, "apktool.yml"), getApktoolYml(BASE_APK_NAME, null, "26", "35"));
        writeFixtureFile(new File(baseDir, "res/values/strings.xml"), getBaseStringsXml());

        writeFixtureFile(new File(splitDir, "AndroidManifest.xml"), getManifest("Split"));
        writeFixtureFile(new File(splitDir, "apktool.yml"), getApktoolYml(SPLIT_APK_NAME, null, "26", "35"));
        writeFixtureFile(new File(splitDir, "res/mipmap-anydpi/ic_launcher.xml"), getAdaptiveIconXml());
        writeFixtureFile(new File(splitDir, "res/drawable/ic_launcher_background.xml"), getAdaptiveBackgroundXml());
        writeFixtureFile(new File(splitDir, "res/drawable/ic_launcher_foreground.xml"), getAdaptiveForegroundXml());

        writeFixtureFile(new File(manifestOnlySplitDir, "AndroidManifest.xml"), getManifest("Split"));
        writeFixtureFile(new File(manifestOnlySplitDir, "apktool.yml"), getApktoolYmlWithoutSdk(SPLIT_APK_NAME, null));
        writeFixtureFile(new File(manifestOnlySplitDir, "res/values/strings.xml"), getBaseStringsXml());

        File baseApk = new File(testDir, BASE_APK_NAME);
        new ApkBuilder(baseDir, new Config("TEST")).build(baseApk);

        File splitWithSdkApk = new File(testDir, "split-with-sdk.apk");
        new ApkBuilder(splitDir, new Config("TEST")).build(splitWithSdkApk);

        File splitWithoutSdkApk = new File(testDir, "split-without-sdk.apk");
        new ApkBuilder(manifestOnlySplitDir, new Config("TEST")).build(splitWithoutSdkApk);

        File splitApk = new File(testDir, SPLIT_APK_NAME);
        replaceManifestEntry(splitWithSdkApk, splitWithoutSdkApk, splitApk);

        File decodedSplitDir = new File(testDir, "decoded-split");
        new ApkDecoder(splitApk, new Config("TEST")).decode(decodedSplitDir);

        String apktoolYml = readTextFile(new File(decodedSplitDir, "apktool.yml"));
        assertTrue(apktoolYml.contains("libraryFiles:"));
        assertTrue(apktoolYml.contains("minSdkVersion: 26"));
        assertTrue(apktoolYml.contains("targetSdkVersion: 35"));

        File rebuiltSplitApk = new File(testDir, "rebuilt-split.apk");
        new ApkBuilder(decodedSplitDir, new Config("TEST")).build(rebuiltSplitApk);

        assertTrue(rebuiltSplitApk.isFile());
    }

    private static void writeFixtureFile(File file, String contents) throws Exception {
        File parentDir = file.getParentFile();
        if (parentDir != null) {
            Files.createDirectories(parentDir.toPath());
        }
        Files.write(file.toPath(), contents.getBytes(StandardCharsets.UTF_8));
    }

    private static String readStylesXml(File decodedDir) throws Exception {
        File qualifiersFile = new File(decodedDir, "res/values-hdpi/styles.xml");
        if (qualifiersFile.isFile()) {
            return readTextFile(qualifiersFile);
        }
        return readTextFile(new File(decodedDir, "res/values/styles.xml"));
    }

    private static String getManifest(String label) {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"\n"
            + "    package=\"" + PACKAGE_NAME + "\"\n"
            + "    platformBuildVersionCode=\"35\"\n"
            + "    platformBuildVersionName=\"15\">\n"
            + "    <application android:label=\"" + label + "\" />\n"
            + "</manifest>\n";
    }

    private static String getApktoolYml(String apkFileName, String usesLibrary) {
        return getApktoolYml(apkFileName, usesLibrary, "21", "35");
    }

    private static String getApktoolYml(String apkFileName, String usesLibrary, String minSdkVersion, String targetSdkVersion) {
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
        yml.append("  minSdkVersion: ").append(minSdkVersion).append('\n');
        yml.append("  targetSdkVersion: ").append(targetSdkVersion).append('\n');
        yml.append("resourcesInfo:\n");
        yml.append("  packageId: 127\n");
        yml.append("  packageName: ").append(PACKAGE_NAME).append('\n');
        yml.append("versionInfo:\n");
        yml.append("  versionCode: 1\n");
        yml.append("  versionName: 1.0\n");
        return yml.toString();
    }

    private static String getApktoolYmlWithoutSdk(String apkFileName, String usesLibrary) {
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
        yml.append("resourcesInfo:\n");
        yml.append("  packageId: 127\n");
        yml.append("  packageName: ").append(PACKAGE_NAME).append('\n');
        yml.append("versionInfo:\n");
        yml.append("  versionCode: 1\n");
        yml.append("  versionName: 1.0\n");
        return yml.toString();
    }

    private static String getBaseAttrsXml() {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<resources>\n"
            + "    <attr name=\"barLength\" format=\"dimension\" />\n"
            + "    <attr name=\"drawableSize\" format=\"dimension\" />\n"
            + "    <attr name=\"gapBetweenBars\" format=\"dimension\" />\n"
            + "</resources>\n";
    }

    private static String getBaseStylesXml() {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<resources>\n"
            + "    <style name=\"Base.Widget.AppCompat.DrawerArrowToggle.Common\" parent=\"\" />\n"
            + "</resources>\n";
    }

    private static String getBasePublicXml() {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<resources>\n"
            + "    <public type=\"animator\" name=\"design_appbar_state_list_animator\" id=\"0x7f020000\" />\n"
            + "    <public type=\"style\" name=\"BaseStyle\" id=\"0x7f140001\" />\n"
            + "</resources>\n";
    }

    private static String getBaseStableStylesXml() {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<resources>\n"
            + "    <style name=\"BaseStyle\" parent=\"\" />\n"
            + "</resources>\n";
    }

    private static String getBaseStringsXml() {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<resources>\n"
            + "    <string name=\"app_name\">Split Test</string>\n"
            + "</resources>\n";
    }

    private static String getBaseAnimatorXml() {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<selector xmlns:android=\"http://schemas.android.com/apk/res/android\">\n"
            + "    <item android:state_enabled=\"true\" android:animation=\"@android:anim/fade_in\" />\n"
            + "</selector>\n";
    }

    private static String getSplitStylesXml() {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<resources>\n"
            + "    <style name=\"Base.Widget.AppCompat.DrawerArrowToggle\" parent=\"@style/Base.Widget.AppCompat.DrawerArrowToggle.Common\">\n"
            + "        <item name=\"barLength\">18.0dp</item>\n"
            + "        <item name=\"drawableSize\">24.0dp</item>\n"
            + "        <item name=\"gapBetweenBars\">3.0dp</item>\n"
            + "    </style>\n"
            + "</resources>\n";
    }

    private static String getSplitPublicXml() {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<resources>\n"
            + "    <public type=\"style\" name=\"SplitStyle\" id=\"0x7f1400e4\" />\n"
            + "</resources>\n";
    }

    private static String getSplitStableStylesXml() {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<resources>\n"
            + "    <style name=\"SplitStyle\" parent=\"@style/BaseStyle\">\n"
            + "        <item name=\"android:paddingLeft\">1dp</item>\n"
            + "    </style>\n"
            + "</resources>\n";
    }

    private static String getAdaptiveIconXml() {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<adaptive-icon xmlns:android=\"http://schemas.android.com/apk/res/android\">\n"
            + "    <background android:drawable=\"@drawable/ic_launcher_background\" />\n"
            + "    <foreground android:drawable=\"@drawable/ic_launcher_foreground\" />\n"
            + "</adaptive-icon>\n";
    }

    private static String getAdaptiveBackgroundXml() {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<shape xmlns:android=\"http://schemas.android.com/apk/res/android\" android:shape=\"rectangle\">\n"
            + "    <solid android:color=\"#FFFFFF\" />\n"
            + "</shape>\n";
    }

    private static String getAdaptiveForegroundXml() {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
            + "<shape xmlns:android=\"http://schemas.android.com/apk/res/android\" android:shape=\"rectangle\">\n"
            + "    <solid android:color=\"#000000\" />\n"
            + "</shape>\n";
    }

    private static void replaceManifestEntry(File sourceApk, File manifestApk, File outApk) throws Exception {
        byte[] manifestBytes = readZipEntry(manifestApk, "AndroidManifest.xml");

        try (
            ZipFile sourceZip = new ZipFile(sourceApk);
            OutputStream fileOut = Files.newOutputStream(outApk.toPath());
            ZipOutputStream zipOut = new ZipOutputStream(fileOut)
        ) {
            Enumeration<? extends ZipEntry> entries = sourceZip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                ZipEntry outEntry = new ZipEntry(entry.getName());
                zipOut.putNextEntry(outEntry);

                if (entry.getName().equals("AndroidManifest.xml")) {
                    zipOut.write(manifestBytes);
                } else {
                    try (InputStream in = sourceZip.getInputStream(entry)) {
                        in.transferTo(zipOut);
                    }
                }
                zipOut.closeEntry();
            }
        }
    }

    private static byte[] readZipEntry(File apkFile, String entryName) throws Exception {
        try (ZipFile zipFile = new ZipFile(apkFile)) {
            ZipEntry entry = zipFile.getEntry(entryName);
            try (InputStream in = zipFile.getInputStream(entry)) {
                return in.readAllBytes();
            }
        }
    }
}
