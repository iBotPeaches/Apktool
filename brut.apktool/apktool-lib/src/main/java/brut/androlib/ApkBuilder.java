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

import brut.androlib.exceptions.AndrolibException;
import brut.androlib.meta.MetaInfo;
import brut.androlib.meta.UsesFramework;
import brut.androlib.res.AndrolibResources;
import brut.androlib.res.Framework;
import brut.androlib.res.data.ResConfigFlags;
import brut.androlib.res.xml.ResXmlPatcher;
import brut.androlib.src.SmaliBuilder;
import brut.common.BrutException;
import brut.common.InvalidUnknownFileException;
import brut.common.RootUnknownFileException;
import brut.common.TraversalUnknownFileException;
import brut.directory.Directory;
import brut.directory.DirectoryException;
import brut.directory.ExtFile;
import brut.util.BrutIO;
import brut.util.OS;
import org.apache.commons.io.FileUtils;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.Logger;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class ApkBuilder {
    private final static Logger LOGGER = Logger.getLogger(ApkBuilder.class.getName());

    private final AndrolibResources mAndRes;
    private final Config config;
    private int mMinSdkVersion = 0;
    private final ExtFile mApkDir;

    private final static String APK_DIRNAME = "build/apk";
    private final static String UNK_DIRNAME = "unknown";
    private final static String[] APK_RESOURCES_FILENAMES = new String[] {
        "resources.arsc", "AndroidManifest.xml", "res", "r", "R" };
    private final static String[] APK_RESOURCES_WITHOUT_RES_FILENAMES = new String[] {
        "resources.arsc", "AndroidManifest.xml" };
    private final static String[] APP_RESOURCES_FILENAMES = new String[] {
        "AndroidManifest.xml", "res" };
    private final static String[] APK_MANIFEST_FILENAMES = new String[] {
        "AndroidManifest.xml" };

    public ApkBuilder(ExtFile apkDir) {
        this(Config.getDefaultConfig(), apkDir);
    }

    public ApkBuilder(Config config, ExtFile apkDir) {
        this.config = config;
        mAndRes = new AndrolibResources(config);
        mApkDir = apkDir;
    }

    public void build(File outFile)
            throws BrutException {
        LOGGER.info("Using Apktool " + ApktoolProperties.getVersion());

        MetaInfo meta = MetaInfo.readMetaFile(mApkDir);
        config.isFramework = meta.isFrameworkApk;
        config.resourcesAreCompressed = meta.compressionType;
        config.doNotCompress = meta.doNotCompress;

        mAndRes.setSdkInfo(meta.sdkInfo);
        mAndRes.setPackageId(meta.packageInfo);
        mAndRes.setPackageRenamed(meta.packageInfo);
        mAndRes.setVersionInfo(meta.versionInfo);
        mAndRes.setSharedLibrary(meta.sharedLibrary);
        mAndRes.setSparseResources(meta.sparseResources);

        if (meta.sdkInfo != null && meta.sdkInfo.get("minSdkVersion") != null) {
            String minSdkVersion = meta.sdkInfo.get("minSdkVersion");
            mMinSdkVersion = mAndRes.getMinSdkVersionFromAndroidCodename(meta, minSdkVersion);
        }

        if (outFile == null) {
            String outFileName = meta.apkFileName;
            outFile = new File(mApkDir, "dist" + File.separator + (outFileName == null ? "out.apk" : outFileName));
        }

        //noinspection ResultOfMethodCallIgnored
        new File(mApkDir, APK_DIRNAME).mkdirs();
        File manifest = new File(mApkDir, "AndroidManifest.xml");
        File manifestOriginal = new File(mApkDir, "AndroidManifest.xml.orig");

        buildSources(mApkDir);
        buildNonDefaultSources(mApkDir);
        buildManifestFile(mApkDir, manifest, manifestOriginal);
        buildResources(mApkDir, meta.usesFramework);
        buildLibs(mApkDir);
        buildCopyOriginalFiles(mApkDir);
        buildApk(mApkDir, outFile);

        // we must go after the Apk is built, and copy the files in via Zip
        // this is because Aapt won't add files it doesn't know (ex unknown files)
        buildUnknownFiles(mApkDir, outFile, meta);

        // we copied the AndroidManifest.xml to AndroidManifest.xml.orig so we can edit it
        // lets restore the unedited one, to not change the original
        if (manifest.isFile() && manifest.exists() && manifestOriginal.isFile()) {
            try {
                if (new File(mApkDir, "AndroidManifest.xml").delete()) {
                    FileUtils.moveFile(manifestOriginal, manifest);
                }
            } catch (IOException ex) {
                throw new AndrolibException(ex.getMessage());
            }
        }
        LOGGER.info("Built apk into: " + outFile.getPath());
    }

    private void buildManifestFile(File appDir, File manifest, File manifestOriginal)
            throws AndrolibException {

        // If we decoded in "raw", we cannot patch AndroidManifest
        if (new File(appDir, "resources.arsc").exists()) {
            return;
        }
        if (manifest.isFile() && manifest.exists()) {
            try {
                if (manifestOriginal.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    manifestOriginal.delete();
                }
                FileUtils.copyFile(manifest, manifestOriginal);
                ResXmlPatcher.fixingPublicAttrsInProviderAttributes(manifest);
            } catch (IOException ex) {
                throw new AndrolibException(ex.getMessage());
            }
        }
    }

    public void buildSources(File appDir)
            throws AndrolibException {
        if (!buildSourcesRaw(appDir, "classes.dex") && !buildSourcesSmali(appDir, "smali", "classes.dex")) {
            LOGGER.warning("Could not find sources");
        }
    }

    public void buildNonDefaultSources(ExtFile appDir)
            throws AndrolibException {
        try {
            // loop through any smali_ directories for multi-dex apks
            Map<String, Directory> dirs = appDir.getDirectory().getDirs();
            for (Map.Entry<String, Directory> directory : dirs.entrySet()) {
                String name = directory.getKey();
                if (name.startsWith("smali_")) {
                    String filename = name.substring(name.indexOf("_") + 1) + ".dex";

                    if (!buildSourcesRaw(appDir, filename) && !buildSourcesSmali(appDir, name, filename)) {
                        LOGGER.warning("Could not find sources");
                    }
                }
            }

            // loop through any classes#.dex files for multi-dex apks
            File[] dexFiles = appDir.listFiles();
            if (dexFiles != null) {
                for (File dex : dexFiles) {

                    // skip classes.dex because we have handled it in buildSources()
                    if (dex.getName().endsWith(".dex") && ! dex.getName().equalsIgnoreCase("classes.dex")) {
                        buildSourcesRaw(appDir, dex.getName());
                    }
                }
            }
        } catch(DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    public boolean buildSourcesRaw(File appDir, String filename)
            throws AndrolibException {
        File working = new File(appDir, filename);
        if (!working.exists()) {
            return false;
        }
        File stored = new File(appDir, APK_DIRNAME + "/" + filename);
        if (config.forceBuildAll || isModified(working, stored)) {
            LOGGER.info("Copying " + appDir.toString() + " " + filename + " file...");
            try {
                BrutIO.copyAndClose(Files.newInputStream(working.toPath()), Files.newOutputStream(stored.toPath()));
                return true;
            } catch (IOException ex) {
                throw new AndrolibException(ex);
            }
        }
        return true;
    }

    public boolean buildSourcesSmali(File appDir, String folder, String filename)
            throws AndrolibException {
        ExtFile smaliDir = new ExtFile(appDir, folder);
        if (!smaliDir.exists()) {
            return false;
        }
        File dex = new File(appDir, APK_DIRNAME + "/" + filename);
        if (! config.forceBuildAll) {
            LOGGER.info("Checking whether sources has changed...");
        }
        if (config.forceBuildAll || isModified(smaliDir, dex)) {
            LOGGER.info("Smaling " + folder + " folder into " + filename + "...");
            //noinspection ResultOfMethodCallIgnored
            dex.delete();
            SmaliBuilder.build(smaliDir, dex, config.forceApi > 0 ? config.forceApi : mMinSdkVersion);
        }
        return true;
    }

    public void buildResources(ExtFile appDir, UsesFramework usesFramework)
            throws BrutException {
        if (!buildResourcesRaw(appDir) && !buildResourcesFull(appDir, usesFramework)
                && !buildManifest(appDir, usesFramework)) {
            LOGGER.warning("Could not find resources");
        }
    }

    public boolean buildResourcesRaw(ExtFile appDir)
            throws AndrolibException {
        try {
            if (!new File(appDir, "resources.arsc").exists()) {
                return false;
            }
            File apkDir = new File(appDir, APK_DIRNAME);
            if (! config.forceBuildAll) {
                LOGGER.info("Checking whether resources has changed...");
            }
            if (config.forceBuildAll || isModified(newFiles(APK_RESOURCES_FILENAMES, appDir),
                    newFiles(APK_RESOURCES_FILENAMES, apkDir))) {
                LOGGER.info("Copying raw resources...");
                appDir.getDirectory().copyToDir(apkDir, APK_RESOURCES_FILENAMES);
            }
            return true;
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    public boolean buildResourcesFull(File appDir, UsesFramework usesFramework)
            throws AndrolibException {
        try {
            if (!new File(appDir, "res").exists()) {
                return false;
            }
            if (! config.forceBuildAll) {
                LOGGER.info("Checking whether resources has changed...");
            }
            File apkDir = new File(appDir, APK_DIRNAME);
            File resourceFile = new File(apkDir.getParent(), "resources.zip");

            if (config.forceBuildAll || isModified(newFiles(APP_RESOURCES_FILENAMES, appDir),
                    newFiles(APK_RESOURCES_FILENAMES, apkDir)) || (config.isAapt2() && !isFile(resourceFile))) {
                LOGGER.info("Building resources...");

                if (config.debugMode) {
                    if (config.isAapt2()) {
                        LOGGER.info("Using aapt2 - setting 'debuggable' attribute to 'true' in AndroidManifest.xml");
                        ResXmlPatcher.setApplicationDebugTagTrue(new File(appDir, "AndroidManifest.xml"));
                    } else {
                        ResXmlPatcher.removeApplicationDebugTag(new File(appDir, "AndroidManifest.xml"));
                    }
                }

                if (config.netSecConf) {
                    MetaInfo meta = MetaInfo.readMetaFile(new ExtFile(appDir));
                    if (meta.sdkInfo != null && meta.sdkInfo.get("targetSdkVersion") != null) {
                        if (Integer.parseInt(meta.sdkInfo.get("targetSdkVersion")) < ResConfigFlags.SDK_NOUGAT) {
                            LOGGER.warning("Target SDK version is lower than 24! Network Security Configuration might be ignored!");
                        }
                    }
                    File netSecConfOrig = new File(appDir, "res/xml/network_security_config.xml");
                    if (netSecConfOrig.exists()) {
                        LOGGER.info("Replacing existing network_security_config.xml!");
                        //noinspection ResultOfMethodCallIgnored
                        netSecConfOrig.delete();
                    }
                    ResXmlPatcher.modNetworkSecurityConfig(netSecConfOrig);
                    ResXmlPatcher.setNetworkSecurityConfig(new File(appDir, "AndroidManifest.xml"));
                    LOGGER.info("Added permissive network security config in manifest");
                }

                File apkFile = File.createTempFile("APKTOOL", null);
                //noinspection ResultOfMethodCallIgnored
                apkFile.delete();
                //noinspection ResultOfMethodCallIgnored
                resourceFile.delete();

                File ninePatch = new File(appDir, "9patch");
                if (!ninePatch.exists()) {
                    ninePatch = null;
                }
                mAndRes.aaptPackage(apkFile, new File(appDir,
                                "AndroidManifest.xml"), new File(appDir, "res"),
                        ninePatch, null, parseUsesFramework(usesFramework));

                ExtFile tmpExtFile = new ExtFile(apkFile);
                Directory tmpDir = tmpExtFile.getDirectory();

                // Sometimes an application is built with a resources.arsc file with no resources,
                // Apktool assumes it will have a rebuilt arsc file, when it doesn't. So if we
                // encounter a copy error, move to a warning and continue on. (#1730)
                try {
                    tmpDir.copyToDir(apkDir,
                            tmpDir.containsDir("res") ? APK_RESOURCES_FILENAMES
                                    : APK_RESOURCES_WITHOUT_RES_FILENAMES);
                } catch (DirectoryException ex) {
                    LOGGER.warning(ex.getMessage());
                } finally {
                    tmpExtFile.close();
                }

                // delete tmpDir
                //noinspection ResultOfMethodCallIgnored
                apkFile.delete();
            }
            return true;
        } catch (IOException | BrutException | ParserConfigurationException | TransformerException | SAXException ex) {
            throw new AndrolibException(ex);
        }
    }

    public boolean buildManifestRaw(ExtFile appDir)
            throws AndrolibException {
        try {
            File apkDir = new File(appDir, APK_DIRNAME);
            LOGGER.info("Copying raw AndroidManifest.xml...");
            appDir.getDirectory().copyToDir(apkDir, APK_MANIFEST_FILENAMES);
            return true;
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    public boolean buildManifest(ExtFile appDir, UsesFramework usesFramework)
            throws BrutException {
        try {
            if (!new File(appDir, "AndroidManifest.xml").exists()) {
                return false;
            }
            if (! config.forceBuildAll) {
                LOGGER.info("Checking whether resources has changed...");
            }

            File apkDir = new File(appDir, APK_DIRNAME);

            if (config.forceBuildAll || isModified(newFiles(APK_MANIFEST_FILENAMES, appDir),
                    newFiles(APK_MANIFEST_FILENAMES, apkDir))) {
                LOGGER.info("Building AndroidManifest.xml...");

                File apkFile = File.createTempFile("APKTOOL", null);
                //noinspection ResultOfMethodCallIgnored
                apkFile.delete();

                File ninePatch = new File(appDir, "9patch");
                if (!ninePatch.exists()) {
                    ninePatch = null;
                }

                mAndRes.aaptPackage(apkFile, new File(appDir,
                                "AndroidManifest.xml"), null, ninePatch, null,
                        parseUsesFramework(usesFramework));

                Directory tmpDir = new ExtFile(apkFile).getDirectory();
                tmpDir.copyToDir(apkDir, APK_MANIFEST_FILENAMES);

                //noinspection ResultOfMethodCallIgnored
                apkFile.delete();
            }
            return true;
        } catch (IOException | DirectoryException ex) {
            throw new AndrolibException(ex);
        } catch (AndrolibException ex) {
            LOGGER.warning("Parse AndroidManifest.xml failed, treat it as raw file.");
            return buildManifestRaw(appDir);
        }
    }

    public void buildLibs(File appDir) throws AndrolibException {
        buildLibrary(appDir, "lib");
        buildLibrary(appDir, "libs");
        buildLibrary(appDir, "kotlin");
        buildLibrary(appDir, "META-INF/services");
    }

    public void buildLibrary(File appDir, String folder) throws AndrolibException {
        File working = new File(appDir, folder);

        if (! working.exists()) {
            return;
        }

        File stored = new File(appDir, APK_DIRNAME + "/" + folder);
        if (config.forceBuildAll || isModified(working, stored)) {
            LOGGER.info("Copying libs... (/" + folder + ")");
            try {
                OS.rmdir(stored);
                OS.cpdir(working, stored);
            } catch (BrutException ex) {
                throw new AndrolibException(ex);
            }
        }
    }

    public void buildCopyOriginalFiles(File appDir)
            throws AndrolibException {
        if (config.copyOriginalFiles) {
            File originalDir = new File(appDir, "original");
            if (originalDir.exists()) {
                try {
                    LOGGER.info("Copy original files...");
                    Directory in = (new ExtFile(originalDir)).getDirectory();
                    if (in.containsFile("AndroidManifest.xml")) {
                        LOGGER.info("Copy AndroidManifest.xml...");
                        in.copyToDir(new File(appDir, APK_DIRNAME), "AndroidManifest.xml");
                    }
                    if (in.containsFile("stamp-cert-sha256")) {
                        LOGGER.info("Copy stamp-cert-sha256...");
                        in.copyToDir(new File(appDir, APK_DIRNAME), "stamp-cert-sha256");
                    }
                    if (in.containsDir("META-INF")) {
                        LOGGER.info("Copy META-INF...");
                        in.copyToDir(new File(appDir, APK_DIRNAME), "META-INF");
                    }
                } catch (DirectoryException ex) {
                    throw new AndrolibException(ex);
                }
            }
        }
    }

    public void buildUnknownFiles(File appDir, File outFile, MetaInfo meta)
            throws AndrolibException {
        if (meta.unknownFiles != null) {
            LOGGER.info("Copying unknown files/dir...");

            Map<String, String> files = meta.unknownFiles;
            File tempFile = new File(outFile.getParent(), outFile.getName() + ".apktool_temp");
            boolean renamed = outFile.renameTo(tempFile);
            if (!renamed) {
                throw new AndrolibException("Unable to rename temporary file");
            }

            try (
                    ZipFile inputFile = new ZipFile(tempFile);
                    ZipOutputStream actualOutput = new ZipOutputStream(Files.newOutputStream(outFile.toPath()))
            ) {
                copyExistingFiles(inputFile, actualOutput);
                copyUnknownFiles(appDir, actualOutput, files);
            } catch (IOException | BrutException ex) {
                throw new AndrolibException(ex);
            }

            // Remove our temporary file.
            //noinspection ResultOfMethodCallIgnored
            tempFile.delete();
        }
    }

    private void copyExistingFiles(ZipFile inputFile, ZipOutputStream outputFile) throws IOException {
        // First, copy the contents from the existing outFile:
        Enumeration<? extends ZipEntry> entries = inputFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = new ZipEntry(entries.nextElement());

            // We can't reuse the compressed size because it depends on compression sizes.
            entry.setCompressedSize(-1);
            outputFile.putNextEntry(entry);

            // No need to create directory entries in the final apk
            if (! entry.isDirectory()) {
                BrutIO.copy(inputFile, outputFile, entry);
            }

            outputFile.closeEntry();
        }
    }

    private void copyUnknownFiles(File appDir, ZipOutputStream outputFile, Map<String, String> files)
            throws BrutException, IOException {
        File unknownFileDir = new File(appDir, UNK_DIRNAME);

        // loop through unknown files
        for (Map.Entry<String,String> unknownFileInfo : files.entrySet()) {
            File inputFile;

            try {
                inputFile = new File(unknownFileDir, BrutIO.sanitizeUnknownFile(unknownFileDir, unknownFileInfo.getKey()));
            } catch (RootUnknownFileException | InvalidUnknownFileException | TraversalUnknownFileException exception) {
                LOGGER.warning(String.format("Skipping file %s (%s)", unknownFileInfo.getKey(), exception.getMessage()));
                continue;
            }

            if (inputFile.isDirectory()) {
                continue;
            }

            ZipEntry newEntry = new ZipEntry(unknownFileInfo.getKey());
            int method = Integer.parseInt(unknownFileInfo.getValue());
            LOGGER.fine(String.format("Copying unknown file %s with method %d", unknownFileInfo.getKey(), method));
            if (method == ZipEntry.STORED) {
                newEntry.setMethod(ZipEntry.STORED);
                newEntry.setSize(inputFile.length());
                newEntry.setCompressedSize(-1);
                BufferedInputStream unknownFile = new BufferedInputStream(Files.newInputStream(inputFile.toPath()));
                CRC32 crc = BrutIO.calculateCrc(unknownFile);
                newEntry.setCrc(crc.getValue());
                unknownFile.close();
            } else {
                newEntry.setMethod(ZipEntry.DEFLATED);
            }
            outputFile.putNextEntry(newEntry);

            BrutIO.copy(inputFile, outputFile);
            outputFile.closeEntry();
        }
    }

    public void buildApk(File appDir, File outApk) throws AndrolibException {
        LOGGER.info("Building apk file...");
        if (outApk.exists()) {
            //noinspection ResultOfMethodCallIgnored
            outApk.delete();
        } else {
            File outDir = outApk.getParentFile();
            if (outDir != null && !outDir.exists()) {
                //noinspection ResultOfMethodCallIgnored
                outDir.mkdirs();
            }
        }
        File assetDir = new File(appDir, "assets");
        if (!assetDir.exists()) {
            assetDir = null;
        }
        mAndRes.zipPackage(outApk, new File(appDir, APK_DIRNAME), assetDir);
    }

    private File[] parseUsesFramework(UsesFramework usesFramework)
            throws AndrolibException {
        if (usesFramework == null) {
            return null;
        }

        List<Integer> ids = usesFramework.ids;
        if (ids == null || ids.isEmpty()) {
            return null;
        }

        Framework framework = new Framework(config);
        String tag = usesFramework.tag;
        File[] files = new File[ids.size()];
        int i = 0;
        for (int id : ids) {
            files[i++] = framework.getFrameworkApk(id, tag);
        }
        return files;
    }

    private boolean isModified(File working, File stored) {
        return ! stored.exists() || BrutIO.recursiveModifiedTime(working) > BrutIO .recursiveModifiedTime(stored);
    }

    private boolean isFile(File working) {
        return working.exists();
    }

    private boolean isModified(File[] working, File[] stored) {
        for (File file : stored) {
            if (!file.exists()) {
                return true;
            }
        }
        return BrutIO.recursiveModifiedTime(working) > BrutIO.recursiveModifiedTime(stored);
    }

    private File[] newFiles(String[] names, File dir) {
        File[] files = new File[names.length];
        for (int i = 0; i < names.length; i++) {
            files[i] = new File(dir, names[i]);
        }
        return files;
    }

    public void close() throws IOException {
        mAndRes.close();
    }
}
