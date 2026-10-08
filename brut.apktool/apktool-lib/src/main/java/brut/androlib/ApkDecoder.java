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
import brut.zip.ZipFileEntry;
import com.google.common.collect.Sets;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.LinkOption;
import java.util.ArrayList;
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
    private static final Set<String> NO_COMPRESS_EXTS = Sets.newHashSet(
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
            mSmaliDecoder = new SmaliDecoder(mApkFile.getPath(), mConfig.isBaksmaliDebugMode());
            mResDecoder = new ResDecoder(mApkFile, mConfig);
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

        Iterator<ZipFileEntry> it = (allSrc ? mApkFile.walkFiles() : mApkFile.listFiles()).iterator();
        while (it.hasNext()) {
            String fileName = it.next().getName();
            if (allSrc ? fileName.endsWith(".dex") : CLASSES_FILES_PATTERN.matcher(fileName).matches()) {
                if (noSrc) {
                    copySourcesRaw(outDir, fileName);
                } else {
                    decodeSourcesSmali(outDir, fileName);
                }
            }
        }
    }

    private void copySourcesRaw(Path outDir, String fileName) throws AndrolibException, IOException {
        Log.i(TAG, "Copying raw " + fileName + "...");
        mApkFile.extract(outDir, fileName);
    }

    private void decodeSourcesSmali(Path outDir, String fileName) throws AndrolibException {
        if (mWorker != null) {
            mWorker.submit(() -> {
                if (mFirstError.get() == null) {
                    try {
                        decodeSourcesSmaliJob(outDir, fileName);
                    } catch (Exception ex) {
                        mFirstError.compareAndSet(null, ex);
                    }
                }
            });
        } else {
            decodeSourcesSmaliJob(outDir, fileName);
        }
    }

    private void decodeSourcesSmaliJob(Path outDir, String fileName) throws AndrolibException {
        Log.i(TAG, "Baksmaling " + fileName + "...");
        mSmaliDecoder.decode(fileName, outDir);
    }

    private void decodeResources(Path outDir) throws AndrolibException, IOException {
        if (!mApkFile.containsFile("resources.arsc")) {
            return;
        }

        if (mConfig.isDecodeResourcesFull()) {
            mResDecoder.decodeResources(outDir);
        } else {
            copyResourcesRaw(outDir);
        }
    }

    private void copyResourcesRaw(Path outDir) throws AndrolibException, IOException {
        Log.i(TAG, "Copying raw resources.arsc...");
        mApkFile.extract(outDir, "resources.arsc");
    }

    private void decodeManifest(Path outDir) throws AndrolibException, IOException {
        if (!mApkFile.containsFile("AndroidManifest.xml")) {
            return;
        }

        if (!mConfig.isDecodeResourcesNone()) {
            mResDecoder.decodeManifest(outDir);
        } else {
            copyManifestRaw(outDir);
        }
    }

    private void copyManifestRaw(Path outDir) throws AndrolibException, IOException {
        Log.i(TAG, "Copying raw AndroidManifest.xml...");
        mApkFile.extract(outDir, "AndroidManifest.xml");
    }

    private void copyOriginalFiles(Path outDir) throws AndrolibException, IOException {
        Path originalDir = outDir.resolve("original");

        Log.i(TAG, "Copying original files...");
        Iterator<ZipFileEntry> it = mApkFile.walkFiles().iterator();
        while (it.hasNext()) {
            ZipFileEntry file = it.next();
            String fileName = file.getName();
            if (ORIGINAL_FILES_PATTERN.matcher(fileName).matches()) {
                file.extract(originalDir.resolve(fileName));
            }
        }
    }

    private void copyRawFiles(Path outDir) throws AndrolibException, IOException {
        Set<String> dexFiles = mSmaliDecoder.getDexFiles();
        Map<String, String> resFileMap = mResDecoder.getResFileMap();
        boolean noAssets = mConfig.isDecodeAssetsNone();

        for (String dirName : RAW_DIRS) {
            if (!mApkFile.containsDir(dirName) || (noAssets && dirName.equals("assets"))) {
                continue;
            }

            Log.i(TAG, "Copying " + dirName + "...");
            Iterator<ZipFileEntry> it = mApkFile.getDir(dirName).walkFiles().iterator();
            while (it.hasNext()) {
                ZipFileEntry file = it.next();
                String fileName = file.getName();
                if (!ORIGINAL_FILES_PATTERN.matcher(fileName).matches()
                        && !dexFiles.contains(fileName) && !resFileMap.containsKey(fileName)) {
                    file.extract(outDir.resolve(fileName));
                }
            }
        }
    }

    private void copyUnknownFiles(Path outDir) throws AndrolibException, IOException {
        Set<String> dexFiles = mSmaliDecoder.getDexFiles();
        Map<String, String> resFileMap = mResDecoder.getResFileMap();
        Path unknownDir = outDir.resolve("unknown");

        Log.i(TAG, "Copying unknown files...");
        Iterator<ZipFileEntry> it = mApkFile.walkFiles().iterator();
        while (it.hasNext()) {
            ZipFileEntry file = it.next();
            String fileName = file.getName();
            if (!STANDARD_FILES_PATTERN.matcher(fileName).matches() && !dexFiles.contains(fileName)
                    && !resFileMap.containsKey(fileName)) {
                file.extract(unknownDir.resolve(fileName));
            }
        }
    }

    private void writeApkInfo(Path outDir) throws AndrolibException, IOException {
        ApkInfo apkInfo = mApkFile.getApkInfo();
        apkInfo.setVersion(mConfig.getVersion());

        // If we did not decode the manifest, store the inferred dex opcode API level.
        if (!mApkFile.containsFile("AndroidManifest.xml") || mConfig.isDecodeResourcesNone()) {
            int apiLevel = mSmaliDecoder.getInferredApiLevel();
            if (apiLevel > 0) {
                apkInfo.getSdkInfo().setMinSdkVersion(Integer.toString(apiLevel));
            }
        }

        // Record uncompressed files.
        Map<String, String> resFileMap = mResDecoder.getResFileMap();
        Set<String> uncompressedExts = new HashSet<>();
        Set<String> uncompressedFiles = new HashSet<>();

        Iterator<ZipFileEntry> it = mApkFile.walkFiles().filter(ZipFileEntry::isStored).iterator();
        while (it.hasNext()) {
            ZipFileEntry file = it.next();
            String fileName = file.getName();
            fileName = resFileMap.getOrDefault(fileName, fileName);
            String ext;
            if (file.getSize() > 0 && !(ext = IOUtils.getFileExtension(fileName)).isEmpty()
                    && NO_COMPRESS_EXTS.contains(ext)) {
                uncompressedExts.add(ext);
            } else {
                uncompressedFiles.add(fileName);
            }
        }

        // Exclude files with an already recorded extension.
        if (!uncompressedExts.isEmpty() && !uncompressedFiles.isEmpty()) {
            uncompressedFiles.removeIf(fileName -> uncompressedExts.contains(IOUtils.getFileExtension(fileName)));
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
