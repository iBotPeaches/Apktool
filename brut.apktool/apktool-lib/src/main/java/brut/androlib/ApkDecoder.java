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
import brut.androlib.exceptions.InFileNotFoundException;
import brut.androlib.exceptions.OutDirExistsException;
import brut.androlib.meta.ApkInfo;
import brut.androlib.res.ResDecoder;
import brut.androlib.smali.SmaliDecoder;
import brut.common.Log;
import brut.util.BackgroundWorker;
import brut.util.IOUtils;
import brut.zip.ZipArchiveEntry;
import brut.zip.ZipUtils;
import com.google.common.collect.ImmutableSet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.LinkOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

public class ApkDecoder {
    private static final String TAG = ApkDecoder.class.getName();

    static final String[] RAW_DIRS = { "assets", "lib" };
    private static final Pattern CLASSES_FILES_PATTERN = Pattern.compile("classes([2-9]|[1-9][0-9]+)?\\.dex");
    private static final Pattern ORIGINAL_FILES_PATTERN = Pattern.compile(
        "AndroidManifest\\.xml|META-INF/[^/]+\\.(RSA|SF|MF)|stamp-cert-sha256");
    private static final Pattern STANDARD_FILES_PATTERN = Pattern.compile(
        "resources\\.arsc|(" + String.join("|", RAW_DIRS) + ")/.*|"
      + CLASSES_FILES_PATTERN.pattern() + "|" + ORIGINAL_FILES_PATTERN.pattern());
    private static final Set<String> NO_COMPRESS_EXTS = ImmutableSet.of(
        "dex", "arsc", "so", "jpg", "jpeg", "png", "gif", "wav", "mp2", "mp3", "ogg", "aac", "mpg", "mpeg", "mid",
        "midi", "smf", "jet", "rtttl", "imy", "xmf", "mp4", "m4a", "m4v", "3gp", "3gpp", "3g2", "3gpp2", "amr", "awb",
        "wma", "wmv", "webm", "webp", "mkv");

    private final ApkFile mApkFile;
    private final Config mConfig;
    private final AtomicReference<Exception> mFirstError;

    private SmaliDecoder mSmaliDecoder;
    private ResDecoder mResDecoder;
    private BackgroundWorker mWorker;

    public ApkDecoder(Path apkFile, Config config) throws InFileNotFoundException {
        if (!Files.isRegularFile(apkFile) || !Files.isReadable(apkFile)) {
            throw new InFileNotFoundException(apkFile);
        }
        mApkFile = new ApkFile(apkFile);
        mConfig = config;
        mFirstError = new AtomicReference<>();
    }

    public ApkInfo getApkInfo() {
        return mApkFile.getApkInfo();
    }

    public void decode(Path outDir) throws AndrolibException {
        try {
            mSmaliDecoder = !mConfig.isDecodeSourcesNone()
                ? new SmaliDecoder(mApkFile.getPath(), mConfig.isBaksmaliDebugMode()) : null;
            mResDecoder = !mConfig.isDecodeResourcesNone() ? new ResDecoder(mApkFile, mConfig) : null;
            mWorker = mConfig.getJobs() > 1 ? new BackgroundWorker(mConfig.getJobs() - 1) : null;

            // We don't follow symlinks here for safety reasons.
            if (Files.exists(outDir, LinkOption.NOFOLLOW_LINKS)) {
                if (Files.isDirectory(outDir, LinkOption.NOFOLLOW_LINKS)) {
                    if (!mConfig.isForced() && IOUtils.isNonEmptyDirectory(outDir)) {
                        throw new OutDirExistsException(outDir);
                    }
                    IOUtils.deleteDirectory(outDir);
                } else {
                    if (!mConfig.isForced()) {
                        throw new OutDirExistsException(outDir);
                    }
                    Files.deleteIfExists(outDir);
                }
            }
            Files.createDirectories(outDir);

            Log.i(TAG, "Using Apktool " + mConfig.getVersion() + " on " + mApkFile.getPath().getFileName()
                     + (mWorker != null ? " with " + mConfig.getJobs() + " threads" : ""));

            decodeSources(outDir);
            decodeResources(outDir);
            decodeManifest(outDir);

            if (mWorker != null) {
                mWorker.waitForFinish();
                if (mFirstError.get() != null) {
                    throw mFirstError.get();
                }
            }

            copyOriginalFiles(outDir);
            copyRawFiles(outDir);
            copyUnknownFiles(outDir);
            writeApkInfo(outDir);
        } catch (AndrolibException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AndrolibException(ex);
        } finally {
            if (mWorker != null) {
                mWorker.shutdownNow();
            }
            try {
                mApkFile.close();
            } catch (IOException ignored) {
            }
        }
    }

    private void decodeSources(Path outDir) throws AndrolibException, IOException {
        boolean allSrc = mConfig.isDecodeSourcesFull();
        boolean noSrc = mConfig.isDecodeSourcesNone();

        Iterator<ZipArchiveEntry> it = mApkFile.entries().iterator();
        while (it.hasNext()) {
            String name = it.next().getName();
            if (allSrc ? name.endsWith(".dex") : CLASSES_FILES_PATTERN.matcher(name).matches()) {
                if (noSrc) {
                    copyRawFile(outDir, name);
                } else {
                    decodeSourcesSmali(outDir, name);
                }
            }
        }
    }

    private void decodeSourcesSmali(Path outDir, String name) throws AndrolibException {
        if (mWorker != null) {
            mWorker.submit(() -> {
                if (mFirstError.get() == null) {
                    try {
                        decodeSourcesSmaliJob(outDir, name);
                    } catch (Exception ex) {
                        mFirstError.compareAndSet(null, ex);
                    }
                }
            });
        } else {
            decodeSourcesSmaliJob(outDir, name);
        }
    }

    private void decodeSourcesSmaliJob(Path outDir, String name) throws AndrolibException {
        Log.i(TAG, "Baksmaling " + name + "...");
        mSmaliDecoder.decode(name, outDir);
    }

    private void decodeResources(Path outDir) throws AndrolibException, IOException {
        String name = "resources.arsc";
        if (!mApkFile.hasEntry(name)) {
            return;
        }

        if (!mConfig.isDecodeResourcesFull()) {
            copyRawFile(outDir, name);
        } else {
            mResDecoder.decodeResources(outDir);
        }
    }

    private void decodeManifest(Path outDir) throws AndrolibException, IOException {
        String name = "AndroidManifest.xml";
        if (!mApkFile.hasEntry(name)) {
            return;
        }

        if (mConfig.isDecodeResourcesNone()) {
            copyRawFile(outDir, name);
        } else {
            mResDecoder.decodeManifest(outDir);
        }
    }

    private void copyRawFile(Path outDir, String name) throws AndrolibException, IOException {
        Log.i(TAG, "Copying raw " + name + "...");
        extractZipEntry(mApkFile.getEntry(name), outDir, name);
    }

    private void copyOriginalFiles(Path outDir) throws AndrolibException, IOException {
        Path originalDir = outDir.resolve("original");

        Log.i(TAG, "Copying original files...");
        Iterator<ZipArchiveEntry> it = mApkFile.entries().iterator();
        while (it.hasNext()) {
            ZipArchiveEntry entry = it.next();
            String name = entry.getName();
            if (ORIGINAL_FILES_PATTERN.matcher(name).matches()) {
                extractZipEntry(entry, originalDir, name);
            }
        }
    }

    private void copyRawFiles(Path outDir) throws AndrolibException, IOException {
        Set<String> dexFiles = mSmaliDecoder != null ? mSmaliDecoder.getDexFiles() : Collections.emptySet();
        Map<String, String> resFileMap = mResDecoder != null ? mResDecoder.getResFileMap() : Collections.emptyMap();
        boolean noAssets = mConfig.isDecodeAssetsNone();

        for (String dirName : RAW_DIRS) {
            String prefix = dirName + "/";
            if (!mApkFile.hasEntries(prefix) || (noAssets && dirName.equals("assets"))) {
                continue;
            }

            Log.i(TAG, "Copying " + dirName + "...");
            Iterator<ZipArchiveEntry> it = mApkFile.entries(prefix).iterator();
            while (it.hasNext()) {
                ZipArchiveEntry entry = it.next();
                String name = entry.getName();
                if (!ORIGINAL_FILES_PATTERN.matcher(name).matches() && !dexFiles.contains(name)
                        && !resFileMap.containsKey(name)) {
                    extractZipEntry(entry, outDir, name);
                }
            }
        }
    }

    private void copyUnknownFiles(Path outDir) throws AndrolibException, IOException {
        Set<String> dexFiles = mSmaliDecoder != null ? mSmaliDecoder.getDexFiles() : Collections.emptySet();
        Map<String, String> resFileMap = mResDecoder != null ? mResDecoder.getResFileMap() : Collections.emptyMap();
        Path unknownDir = outDir.resolve("unknown");

        Log.i(TAG, "Copying unknown files...");
        Iterator<ZipArchiveEntry> it = mApkFile.entries().iterator();
        while (it.hasNext()) {
            ZipArchiveEntry entry = it.next();
            String name = entry.getName();
            if (!STANDARD_FILES_PATTERN.matcher(name).matches() && !dexFiles.contains(name)
                    && !resFileMap.containsKey(name)) {
                extractZipEntry(entry, unknownDir, name);
            }
        }
    }

    private void extractZipEntry(ZipArchiveEntry entry, Path outDir, String outName) throws IOException {
        try {
            entry.extract(outDir.resolve(ZipUtils.normalizeForFs(outName)));
        } catch (IllegalArgumentException | IOException ex) {
            Log.w(TAG, "Could not extract ZIP entry: %s (%s)", entry.getName(), ex.getMessage());
        }
    }

    private void writeApkInfo(Path outDir) throws AndrolibException, IOException {
        ApkInfo apkInfo = mApkFile.getApkInfo();
        apkInfo.setVersion(mConfig.getVersion());

        // If we did not decode the manifest, store the inferred dex opcode API level.
        if (mConfig.isDecodeResourcesNone() || !mApkFile.hasEntry("AndroidManifest.xml")) {
            int apiLevel = mSmaliDecoder != null ? mSmaliDecoder.getInferredApiLevel() : 0;
            if (apiLevel > 0) {
                apkInfo.getSdkInfo().setMinSdkVersion(Integer.toString(apiLevel));
            }
        }

        // Record uncompressed files.
        Map<String, String> resFileMap = mResDecoder != null ? mResDecoder.getResFileMap() : Collections.emptyMap();
        Set<String> uncompressedExts = new HashSet<>();
        Set<String> uncompressedFiles = new HashSet<>();

        Iterator<ZipArchiveEntry> it = mApkFile.entries().filter(ZipArchiveEntry::isStored).iterator();
        while (it.hasNext()) {
            ZipArchiveEntry entry = it.next();
            String name = entry.getName();
            name = resFileMap.getOrDefault(name, name);
            String ext;
            if (entry.getSize() > 0 && !(ext = IOUtils.getFileExtension(name, '/')).isEmpty()
                    && NO_COMPRESS_EXTS.contains(ext)) {
                uncompressedExts.add(ext);
            } else {
                uncompressedFiles.add(name);
            }
        }

        // Exclude files with an already recorded extension.
        if (!uncompressedExts.isEmpty() && !uncompressedFiles.isEmpty()) {
            uncompressedFiles.removeIf(name -> uncompressedExts.contains(IOUtils.getFileExtension(name, '/')));
        }

        // Update apk info.
        List<String> doNotCompress = apkInfo.getDoNotCompress();
        if (!uncompressedExts.isEmpty()) {
            List<String> uncompressedExtsList = new ArrayList<>(uncompressedExts);
            uncompressedExtsList.sort(null);
            doNotCompress.addAll(uncompressedExtsList);
        }
        if (!uncompressedFiles.isEmpty()) {
            List<String> uncompressedFilesList = new ArrayList<>(uncompressedFiles);
            uncompressedFilesList.sort(null);
            doNotCompress.addAll(uncompressedFilesList);
        }

        // Serialize apk info to file.
        apkInfo.save(outDir.resolve("apktool.yml"));
    }
}
