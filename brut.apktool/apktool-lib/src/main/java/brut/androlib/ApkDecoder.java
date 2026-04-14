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
import brut.androlib.meta.SdkInfo;
import brut.androlib.res.ResDecoder;
import brut.androlib.res.decoder.BinaryXmlResourceParser;
import brut.androlib.res.decoder.ManifestPullEventHandler;
import brut.androlib.res.decoder.ResXmlPullStreamDecoder;
import brut.androlib.res.xml.ResXmlSerializer;
import brut.common.BrutException;
import brut.androlib.smali.SmaliDecoder;
import brut.common.Log;
import brut.directory.Directory;
import brut.directory.DirectoryException;
import brut.directory.ExtFile;
import brut.util.BackgroundWorker;
import brut.util.OS;
import org.apache.commons.io.FilenameUtils;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

public class ApkDecoder {
    private static final String TAG = ApkDecoder.class.getName();

    private static final Pattern NO_COMPRESS_EXT_PATTERN = Pattern.compile(
        "dex|arsc|so|jpg|jpeg|png|gif|wav|mp2|mp3|ogg|aac|mpg|mpeg|mid|midi|smf|jet|"
      + "rtttl|imy|xmf|mp4|m4a|m4v|3gp|3gpp|3g2|3gpp2|amr|awb|wma|wmv|webm|webp|mkv");

    private final ExtFile mApkFile;
    private final Config mConfig;
    private final AtomicReference<AndrolibException> mFirstError;

    private ApkInfo mApkInfo;
    private SmaliDecoder mSmaliDecoder;
    private ResDecoder mResDecoder;
    private BackgroundWorker mWorker;

    public ApkDecoder(File apkFile, Config config) {
        mApkFile = new ExtFile(apkFile);
        mConfig = config;
        mFirstError = new AtomicReference<>();
    }

    public void decode(File outDir) throws AndrolibException {
        if (!mConfig.isForced() && outDir.exists()) {
            throw new OutDirExistsException(outDir.getPath());
        }
        if (!mApkFile.isFile() || !mApkFile.canRead()) {
            throw new InFileNotFoundException(mApkFile.getPath());
        }
        if (mConfig.getJobs() > 1) {
            mWorker = new BackgroundWorker(mConfig.getJobs() - 1);
        }
        try {
            mApkInfo = new ApkInfo();
            mApkInfo.setVersion(mConfig.getVersion());
            mApkInfo.setApkFile(mApkFile);
            mSmaliDecoder = new SmaliDecoder(mApkFile, mConfig.isBaksmaliDebugMode());
            mResDecoder = new ResDecoder(mApkInfo, mConfig);

            OS.rmdir(outDir);
            OS.mkdir(outDir);

            Log.i(TAG, "Using Apktool " + mConfig.getVersion() + " on " + mApkFile.getName()
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
        } finally {
            if (mWorker != null) {
                mWorker.shutdownNow();
            }
            try {
                mApkFile.close();
            } catch (DirectoryException ignored) {
            }
        }
    }

    public ApkInfo getApkInfo() {
        return mApkInfo;
    }

    private void decodeSources(File outDir) throws AndrolibException {
        if (!mApkInfo.hasSources()) {
            return;
        }

        try {
            Directory in = mApkFile.getDirectory();
            boolean allSrc = mConfig.isDecodeSourcesFull();
            boolean noSrc = mConfig.isDecodeSourcesNone();

            for (String fileName : in.getFiles(allSrc)) {
                if (allSrc ? !fileName.endsWith(".dex") : !ApkInfo.CLASSES_FILES_PATTERN.matcher(fileName).matches()) {
                    continue;
                }

                if (noSrc) {
                    copySourcesRaw(outDir, fileName);
                } else {
                    decodeSourcesSmali(outDir, fileName);
                }
            }
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    private void copySourcesRaw(File outDir, String fileName) throws AndrolibException {
        Log.i(TAG, "Copying raw " + fileName + "...");
        try {
            Directory in = mApkFile.getDirectory();

            in.copyToDir(outDir, fileName);
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    private void decodeSourcesSmali(File outDir, String fileName) throws AndrolibException {
        if (mWorker != null) {
            mWorker.submit(() -> {
                if (mFirstError.get() == null) {
                    try {
                        decodeSourcesSmaliJob(outDir, fileName);
                    } catch (AndrolibException ex) {
                        mFirstError.compareAndSet(null, ex);
                    }
                }
            });
        } else {
            decodeSourcesSmaliJob(outDir, fileName);
        }
    }

    private void decodeSourcesSmaliJob(File outDir, String fileName) throws AndrolibException {
        Log.i(TAG, "Baksmaling " + fileName + "...");
        mSmaliDecoder.decode(fileName, outDir);
    }

    private void decodeResources(File outDir) throws AndrolibException {
        if (!mApkInfo.hasResources()) {
            return;
        }

        if (mConfig.isDecodeResourcesFull()) {
            mResDecoder.decodeResources(outDir);
        } else {
            copyResourcesRaw(outDir);
        }
    }

    private void copyResourcesRaw(File outDir) throws AndrolibException {
        Log.i(TAG, "Copying raw resources.arsc...");
        try {
            Directory in = mApkFile.getDirectory();

            in.copyToDir(outDir, "resources.arsc");
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    private void decodeManifest(File outDir) throws AndrolibException {
        if (!mApkInfo.hasManifest()) {
            return;
        }

        if (!mConfig.isDecodeResourcesNone()) {
            mResDecoder.decodeManifest(outDir);
        } else {
            copyManifestRaw(outDir);
        }
    }

    private void copyManifestRaw(File outDir) throws AndrolibException {
        Log.i(TAG, "Copying raw AndroidManifest.xml...");
        try {
            Directory in = mApkFile.getDirectory();

            in.copyToDir(outDir, "AndroidManifest.xml");
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    private void copyRawFiles(File outDir) throws AndrolibException {
        try {
            Directory in = mApkFile.getDirectory();
            Set<String> dexFiles = mSmaliDecoder.getDexFiles();
            Map<String, String> resFileMap = mResDecoder.getResFileMap();
            boolean noAssets = mConfig.isDecodeAssetsNone();

            for (String dirName : ApkInfo.RAW_DIRS) {
                if (!in.containsDir(dirName) || (noAssets && dirName.equals("assets"))) {
                    continue;
                }

                Log.i(TAG, "Copying " + dirName + "...");
                for (String fileName : in.getDir(dirName).getFiles(true)) {
                    fileName = dirName + in.separator + fileName;
                    if (!ApkInfo.ORIGINAL_FILES_PATTERN.matcher(fileName).matches()
                            && !dexFiles.contains(fileName) && !resFileMap.containsKey(fileName)) {
                        in.copyToDir(outDir, fileName);
                    }
                }
            }
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    private void copyOriginalFiles(File outDir) throws AndrolibException {
        File originalDir = new File(outDir, "original");

        Log.i(TAG, "Copying original files...");
        try {
            Directory in = mApkFile.getDirectory();

            for (String fileName : in.getFiles(true)) {
                if (ApkInfo.ORIGINAL_FILES_PATTERN.matcher(fileName).matches()) {
                    in.copyToDir(originalDir, fileName);
                }
            }
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    private void copyUnknownFiles(File outDir) throws AndrolibException {
        File unknownDir = new File(outDir, "unknown");

        Log.i(TAG, "Copying unknown files...");
        try {
            Directory in = mApkFile.getDirectory();
            Set<String> dexFiles = mSmaliDecoder.getDexFiles();
            Map<String, String> resFileMap = mResDecoder.getResFileMap();

            for (String fileName : in.getFiles(true)) {
                if (!ApkInfo.STANDARD_FILES_PATTERN.matcher(fileName).matches() && !dexFiles.contains(fileName)
                        && !resFileMap.containsKey(fileName)) {
                    in.copyToDir(unknownDir, fileName);
                }
            }
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    private void writeApkInfo(File outDir) throws AndrolibException {
        // If we did not decode the manifest, store the inferred dex opcode API level.
        if (!mApkInfo.hasManifest() || mConfig.isDecodeResourcesNone()) {
            int apiLevel = mSmaliDecoder.getInferredApiLevel();
            if (apiLevel > 0) {
                mApkInfo.getSdkInfo().setMinSdkVersion(Integer.toString(apiLevel));
            }
        }

        // Record uncompressed files.
        try {
            Directory in = mApkFile.getDirectory();
            Map<String, String> resFileMap = mResDecoder.getResFileMap();
            Set<String> uncompressedExts = new HashSet<>();
            Set<String> uncompressedFiles = new HashSet<>();

            for (String fileName : in.getFiles(true)) {
                if (in.getCompressionLevel(fileName) == 0) {
                    String ext;
                    if (in.getSize(fileName) > 0 && !(ext = FilenameUtils.getExtension(fileName)).isEmpty()
                            && NO_COMPRESS_EXT_PATTERN.matcher(ext).matches()) {
                        uncompressedExts.add(ext);
                    } else {
                        uncompressedFiles.add(resFileMap.getOrDefault(fileName, fileName));
                    }
                }
            }

            // Exclude files with an already recorded extenstion.
            if (!uncompressedExts.isEmpty() && !uncompressedFiles.isEmpty()) {
                Iterator<String> it = uncompressedFiles.iterator();
                while (it.hasNext()) {
                    String fileName = it.next();
                    String ext = FilenameUtils.getExtension(fileName);
                    if (uncompressedExts.contains(ext)) {
                        it.remove();
                    }
                }
            }

            // Update apk info.
            List<String> doNotCompress = mApkInfo.getDoNotCompress();
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
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }

        inheritMissingSdkInfoFromLibraries();
        persistLibraryFiles(outDir);

        // Serialize apk info to file.
        mApkInfo.save(outDir);
    }

    private void inheritMissingSdkInfoFromLibraries() throws AndrolibException {
        SdkInfo sdkInfo = mApkInfo.getSdkInfo();
        if (!sdkInfo.isEmpty()) {
            return;
        }

        Map<String, File> libraryApkFiles = mConfig.getLibraryApkFileMap();
        if (libraryApkFiles.isEmpty()) {
            return;
        }

        for (File libraryApkFile : libraryApkFiles.values()) {
            if (libraryApkFile == null || !libraryApkFile.isFile()) {
                continue;
            }

            SdkInfo librarySdkInfo = loadSdkInfoFromApk(libraryApkFile);
            if (!librarySdkInfo.isEmpty()) {
                sdkInfo.setMinSdkVersion(librarySdkInfo.getMinSdkVersion());
                sdkInfo.setTargetSdkVersion(librarySdkInfo.getTargetSdkVersion());
                sdkInfo.setMaxSdkVersion(librarySdkInfo.getMaxSdkVersion());
                return;
            }
        }
    }

    private SdkInfo loadSdkInfoFromApk(File apkFile) throws AndrolibException {
        ApkInfo apkInfo = new ApkInfo();
        apkInfo.setApkFile(new ExtFile(apkFile));

        ResDecoder decoder = new ResDecoder(apkInfo, mConfig);
        BinaryXmlResourceParser parser = new BinaryXmlResourceParser(
            decoder.getTable(), mConfig.isIgnoreRawValues(), mConfig.isDecodeResolveLazy());
        ResXmlSerializer serial = new ResXmlSerializer(true);
        ManifestPullEventHandler handler = new ManifestPullEventHandler(apkInfo, false);
        ResXmlPullStreamDecoder streamDecoder = new ResXmlPullStreamDecoder(parser, serial, handler);

        try {
            decoder.getTable().load();
            Directory inDir = apkInfo.getApkFile().getDirectory();
            try (
                InputStream in = inDir.getFileInput("AndroidManifest.xml");
                OutputStream out = OutputStream.nullOutputStream()
            ) {
                streamDecoder.decode(in, out);
            }
        } catch (DirectoryException | IOException ex) {
            throw new AndrolibException(ex);
        } finally {
            try {
                apkInfo.getApkFile().close();
            } catch (DirectoryException ignored) {
            }
        }

        return apkInfo.getSdkInfo();
    }

    private void persistLibraryFiles(File outDir) throws AndrolibException {
        List<String> apkLibraryFiles = mApkInfo.getLibraryFiles();
        apkLibraryFiles.clear();

        Map<String, File> libraryApkFiles = mConfig.getLibraryApkFileMap();
        if (libraryApkFiles.isEmpty()) {
            return;
        }

        File libraryDir = new File(outDir, "original/apktool-libs");
        OS.mkdir(libraryDir);

        for (Map.Entry<String, File> entry : libraryApkFiles.entrySet()) {
            String packageName = entry.getKey();
            File libraryApk = entry.getValue();
            if (packageName == null || packageName.isEmpty() || libraryApk == null || !libraryApk.isFile()) {
                continue;
            }

            File outFile = new File(libraryDir, packageName + ".apk");
            try {
                OS.cpfile(libraryApk, outFile);
            } catch (BrutException ex) {
                throw new AndrolibException(ex);
            }
            apkLibraryFiles.add(packageName + ":original/apktool-libs/" + outFile.getName());
        }
    }
}
