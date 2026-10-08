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
import brut.androlib.exceptions.InDirNotFoundException;
import brut.androlib.meta.ApkInfo;
import brut.androlib.meta.SdkInfo;
import brut.androlib.res.AaptInvoker;
import brut.androlib.res.AaptManager;
import brut.androlib.res.data.ResChunkHeader;
import brut.androlib.res.xml.ResXmlUtils;
import brut.androlib.smali.SmaliBuilder;
import brut.common.Log;
import brut.util.BackgroundWorker;
import brut.util.BinaryDataInputStream;
import brut.util.IOUtils;
import brut.zip.ZipArchive;
import brut.zip.ZipUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ApkBuilder {
    private static final String TAG = ApkBuilder.class.getName();

    private final Path mApkDir;
    private final Path mBuildDir;
    private final Path mOutDir;
    private final Config mConfig;
    private final AtomicReference<Exception> mFirstError;

    private ApkInfo mApkInfo;
    private SmaliBuilder mSmaliBuilder;
    private AaptInvoker mAaptInvoker;
    private BackgroundWorker mWorker;

    public ApkBuilder(Path apkDir, Config config) throws InDirNotFoundException {
        if (!Files.isDirectory(apkDir) || !Files.isReadable(apkDir)) {
            throw new InDirNotFoundException(apkDir);
        }
        mApkDir = apkDir;
        mBuildDir = mApkDir.resolve("build");
        mOutDir = mBuildDir.resolve("apk");
        mConfig = config;
        mFirstError = new AtomicReference<>();
    }

    public void build(Path outFile) throws AndrolibException {
        try {
            mApkInfo = ApkInfo.load(mApkDir.resolve("apktool.yml"));
            mSmaliBuilder = new SmaliBuilder(mApkInfo.getSdkInfo().getMinSdkVersionInt());
            mAaptInvoker = new AaptInvoker(mApkInfo, mConfig);
            mWorker = mConfig.getJobs() > 1 ? new BackgroundWorker(mConfig.getJobs() - 1) : null;

            if (mConfig.isForced()) {
                IOUtils.deleteDirectory(mBuildDir);
            }
            Files.createDirectories(mOutDir);

            Log.i(TAG, "Using Apktool " + mConfig.getVersion() + " on " + IOUtils.resolveDirectoryName(mApkDir)
                     + (mWorker != null ? " with " + mConfig.getJobs() + " threads" : ""));

            buildSources();
            buildResources();

            if (mWorker != null) {
                mWorker.waitForFinish();
                if (mFirstError.get() != null) {
                    throw mFirstError.get();
                }
            }

            copyOriginalFiles();
            buildApkFile(outFile);
        } catch (AndrolibException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AndrolibException(ex);
        } finally {
            if (mWorker != null) {
                mWorker.shutdownNow();
            }
        }
    }

    private void buildSources() throws AndrolibException, IOException {
        // Copy raw dex files.
        Set<String> dexFiles = new HashSet<>();
        try (Stream<Path> stream = Files.list(mApkDir)) {
            Iterator<Path> it = stream.filter(Files::isRegularFile).sorted().iterator();
            while (it.hasNext()) {
                String fileName = it.next().getFileName().toString();
                if (fileName.endsWith(".dex")) {
                    copySourcesRaw(fileName);
                    dexFiles.add(fileName);
                }
            }
        }

        // Build smali dirs.
        try (Stream<Path> stream = Files.list(mApkDir)) {
            Iterator<Path> it = stream.filter(Files::isDirectory).sorted().iterator();
            while (it.hasNext()) {
                String dirName = it.next().getFileName().toString();
                String fileName;
                if (dirName.equals("smali")) {
                    fileName = "classes.dex";
                } else if (dirName.startsWith("smali_")) {
                    fileName = dirName.substring(6).replace("@", mOutDir.getFileSystem().getSeparator()) + ".dex";
                    try {
                        fileName = IOUtils.sanitizePath(mOutDir, fileName);
                    } catch (IllegalArgumentException ignored) {
                        Log.w(TAG, "Smali folder name resolves to invalid dex path: %s -> %s", dirName, fileName);
                        continue;
                    }
                } else {
                    continue;
                }

                if (!dexFiles.contains(fileName)) {
                    buildSourcesSmali(dirName, fileName);
                }
            }
        }
    }

    private void copySourcesRaw(String fileName) throws IOException {
        Path dexFile = mApkDir.resolve(fileName);
        Path outDexFile = mOutDir.resolve(fileName);
        if (!isFileNewer(dexFile, outDexFile)) {
            Log.i(TAG, fileName + " has not changed.");
            return;
        }

        Log.i(TAG, "Copying raw " + fileName + "...");
        Files.copy(dexFile, outDexFile, StandardCopyOption.REPLACE_EXISTING);
    }

    private void buildSourcesSmali(String dirName, String fileName) throws AndrolibException, IOException {
        if (mWorker != null) {
            mWorker.submit(() -> {
                if (mFirstError.get() == null) {
                    try {
                        buildSourcesSmaliJob(dirName, fileName);
                    } catch (Exception ex) {
                        mFirstError.compareAndSet(null, ex);
                    }
                }
            });
        } else {
            buildSourcesSmaliJob(dirName, fileName);
        }
    }

    private void buildSourcesSmaliJob(String dirName, String fileName) throws AndrolibException, IOException {
        Path smaliDir = mApkDir.resolve(dirName);
        Path dexFile = mOutDir.resolve(fileName);
        if (!isFileNewer(smaliDir, dexFile)) {
            Log.i(TAG, dirName + " has not changed.");
            return;
        }

        Log.i(TAG, "Smaling " + dirName + " folder into " + fileName + "...");
        Files.deleteIfExists(dexFile);
        mSmaliBuilder.build(smaliDir, dexFile);
    }

    private void buildResources() throws AndrolibException, IOException {
        Path manifest = mApkDir.resolve("AndroidManifest.xml");
        if (!Files.isRegularFile(manifest)) {
            return;
        }

        // Check if manifest is binary XML.
        boolean isBinaryManifest;
        try (BinaryDataInputStream in = new BinaryDataInputStream(Files.newInputStream(manifest))) {
            isBinaryManifest = ResChunkHeader.read(in).type == ResChunkHeader.RES_XML_TYPE;
        }

        // Copy raw manifest if it's binary XML.
        if (isBinaryManifest) {
            copyManifestRaw(manifest);
        }

        // Copy raw resources if possible.
        Path arscFile = mApkDir.resolve("resources.arsc");
        if (Files.isRegularFile(arscFile)) {
            copyResourcesRaw(arscFile);
            return;
        }

        // We cannot build if manifest is binary XML.
        if (isBinaryManifest) {
            return;
        }

        // Build only manifest if no resources.
        Path resDir = mApkDir.resolve("res");
        if (!Files.isDirectory(resDir)) {
            buildManifestOnly(manifest);
            return;
        }

        // Build manifest and resources.
        buildResourcesFully(manifest, arscFile, resDir);
    }

    private void copyManifestRaw(Path manifest) throws IOException {
        Path outManifest = mOutDir.resolve(mApkDir.relativize(manifest));
        if (!isFileNewer(manifest, outManifest)) {
            Log.i(TAG, "AndroidManifest.xml has not changed.");
            return;
        }

        Log.i(TAG, "Copying raw AndroidManifest.xml...");
        Files.copy(manifest, outManifest, StandardCopyOption.REPLACE_EXISTING);
    }

    private void copyResourcesRaw(Path arscFile) throws IOException {
        Path outArscFile = mOutDir.resolve(mApkDir.relativize(arscFile));
        if (!isFileNewer(arscFile, outArscFile)) {
            Log.i(TAG, "resources.arsc has not changed.");
            return;
        }

        Log.i(TAG, "Copying raw resources.arsc...");
        Files.copy(arscFile, outArscFile, StandardCopyOption.REPLACE_EXISTING);
    }

    private void buildManifestOnly(Path manifest) throws AndrolibException, IOException {
        Path outManifest = mOutDir.resolve(mApkDir.relativize(manifest));
        if (!isFileNewer(manifest, outManifest)) {
            Log.i(TAG, "AndroidManifest.xml has not changed.");
            return;
        }

        Path tmpManifest = mBuildDir.resolve(manifest.getFileName());
        Files.copy(manifest, tmpManifest, StandardCopyOption.REPLACE_EXISTING);

        ResXmlUtils.injectUsesSdkTag(tmpManifest, mApkInfo.getSdkInfo());
        ResXmlUtils.injectVersionAttributes(tmpManifest, mApkInfo.getVersionInfo());
        ResXmlUtils.replaceReferencesInAttributes(tmpManifest, mApkDir);

        if (mConfig.isDebuggable()) {
            Log.i(TAG, "Setting 'debuggable' attribute to 'true' in AndroidManifest.xml...");
            ResXmlUtils.injectDebuggableAttribute(tmpManifest);
        }

        Log.i(TAG, "Building AndroidManifest.xml with " + AaptManager.getBinaryName() + "...");

        Path tmpFile = Files.createTempFile("APKTOOL", null);
        try {
            Files.deleteIfExists(outManifest);
            mAaptInvoker.invoke(tmpFile, tmpManifest, null);

            try (ZipArchive tmpZip = new ZipArchive(tmpFile)) {
                tmpZip.extract(mOutDir, mOutDir.relativize(outManifest).toString());
            }
        } finally {
            Files.deleteIfExists(tmpFile);
        }
    }

    private void buildResourcesFully(Path manifest, Path arscFile, Path resDir) throws AndrolibException, IOException {
        Path outManifest = mOutDir.resolve(mApkDir.relativize(manifest));
        Path outArscFile = mOutDir.resolve(mApkDir.relativize(arscFile));
        Path outResDir = mOutDir.resolve(mApkDir.relativize(resDir));
        if (!isFileNewer(manifest, outManifest) && !isFileNewer(resDir, outResDir)) {
            Log.i(TAG, "AndroidManifest.xml and resources have not changed.");
            return;
        }

        Path tmpManifest = mBuildDir.resolve(manifest.getFileName());
        Files.copy(manifest, tmpManifest, StandardCopyOption.REPLACE_EXISTING);

        ResXmlUtils.injectUsesSdkTag(tmpManifest, mApkInfo.getSdkInfo());
        ResXmlUtils.injectVersionAttributes(tmpManifest, mApkInfo.getVersionInfo());
        ResXmlUtils.replaceReferencesInAttributes(tmpManifest, mApkDir);

        if (mConfig.isDebuggable()) {
            Log.i(TAG, "Setting 'debuggable' attribute to 'true' in AndroidManifest.xml...");
            ResXmlUtils.injectDebuggableAttribute(tmpManifest);
        }

        if (mConfig.isNetSecConf()) {
            Log.i(TAG, "Adding permissive network security config in manifest...");
            ResXmlUtils.injectNetworkSecurityConfig(tmpManifest, mApkDir);

            if (mApkInfo.getSdkInfo().getTargetSdkVersionInt() < SdkInfo.SDK_NOUGAT) {
                Log.w(TAG, "Target SDK version is lower than 24, Network Security Configuration might be ignored!");
            }
        }

        Log.i(TAG, "Building resources with " + AaptManager.getBinaryName() + "...");

        Path tmpFile = Files.createTempFile("APKTOOL", null);
        try {
            Files.deleteIfExists(outManifest);
            Files.deleteIfExists(outArscFile);
            IOUtils.deleteDirectory(outResDir);
            mAaptInvoker.invoke(tmpFile, tmpManifest, resDir);

            try (ZipArchive tmpZip = new ZipArchive(tmpFile)) {
                tmpZip.extract(mOutDir, mOutDir.relativize(outManifest).toString(),
                    mOutDir.relativize(outArscFile).toString(), mOutDir.relativize(outResDir).toString());
            }
        } finally {
            Files.deleteIfExists(tmpFile);
        }
    }

    private void copyOriginalFiles() throws IOException {
        if (!mConfig.isCopyOriginal()) {
            return;
        }

        Path originalDir = mApkDir.resolve("original");
        if (!Files.isDirectory(originalDir)) {
            return;
        }

        Log.i(TAG, "Copying original files...");
        IOUtils.copyDirectory(originalDir, mOutDir);
    }

    private void buildApkFile(Path outFile) throws AndrolibException, IOException {
        if (mConfig.isNoApk()) {
            return;
        }

        if (outFile == null) {
            String apkName = mApkInfo.getApkFileName();
            outFile = mApkDir.resolve("dist/" + (apkName != null ? apkName : "out.apk"));
        }
        if (!Files.deleteIfExists(outFile)) {
            IOUtils.createParentDirectories(outFile);
        }

        Predicate<String> shouldCompress;
        if (mApkInfo.getDoNotCompress().isEmpty()) {
            shouldCompress = entryName -> true;
        } else {
            // Convert to set for fast lookup.
            Set<String> doNotCompress = new HashSet<>(mApkInfo.getDoNotCompress());
            shouldCompress = entryName -> !doNotCompress.contains(entryName)
                && !doNotCompress.contains(IOUtils.getFileExtension(entryName));
        }

        Log.i(TAG, "Building apk file...");
        try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(outFile))) {
            // Zip aapt2 output files.
            zipDir(mOutDir, null, out, shouldCompress);

            // Zip standard raw files.
            for (String dirName : ApkDecoder.RAW_DIRS) {
                Path rawDir = mApkDir.resolve(dirName);
                if (Files.isDirectory(rawDir)) {
                    Log.i(TAG, "Importing " + dirName + "...");
                    zipDir(mApkDir, dirName, out, shouldCompress);
                }
            }

            // Zip unknown files.
            Path unknownDir = mApkDir.resolve("unknown");
            if (Files.isDirectory(unknownDir)) {
                Log.i(TAG, "Importing unknown files...");
                zipDir(unknownDir, null, out, shouldCompress);
            }
        } catch (IOException ex) {
            try {
                Files.deleteIfExists(outFile);
            } catch (IOException suppressed) {
                ex.addSuppressed(suppressed);
            }
            throw ex;
        }
        Log.i(TAG, "Built apk into: " + outFile);
    }

    private static void zipDir(Path baseDir, String dirName, ZipOutputStream out, Predicate<String> shouldCompress)
            throws IOException {
        Path dir = dirName != null ? baseDir.resolve(dirName) : baseDir;
        try (Stream<Path> stream = Files.walk(dir)) {
            Iterator<Path> it = stream.filter(Files::isRegularFile).sorted().iterator();
            while (it.hasNext()) {
                String fileName = baseDir.relativize(it.next()).toString();
                zipFile(baseDir, fileName, out, shouldCompress);
            }
        }
    }

    private static void zipFile(Path baseDir, String fileName, ZipOutputStream out, Predicate<String> shouldCompress)
            throws IOException {
        Path file = baseDir.resolve(fileName);
        String entryName = ZipUtils.normalize(fileName);
        ZipEntry entry = new ZipEntry(entryName);
        entry.setTime(ApkFile.TIME_MILLIS);
        if (shouldCompress.test(entryName)) {
            entry.setMethod(ZipEntry.DEFLATED);
        } else {
            entry.setMethod(ZipEntry.STORED);
            entry.setSize(Files.size(file));
            entry.setCrc(ZipUtils.computeCrc(file));
        }
        out.putNextEntry(entry);
        Files.copy(file, out);
        out.closeEntry();
    }

    private static boolean isFileNewer(Path file, Path reference) throws IOException {
        return !Files.exists(reference) || IOUtils.recursiveModifiedTime(file) > IOUtils.recursiveModifiedTime(reference);
    }
}
