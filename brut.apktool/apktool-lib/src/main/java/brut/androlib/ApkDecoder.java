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
import brut.androlib.res.ResourcesDecoder;
import brut.androlib.smali.SmaliDecoder;
import brut.directory.Directory;
import brut.directory.DirectoryException;
import brut.directory.ExtFile;
import brut.util.BackgroundWorker;
import brut.util.OS;
import org.apache.commons.io.FilenameUtils;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class ApkDecoder {
    private static final Logger LOGGER = Logger.getLogger(ApkDecoder.class.getName());

    private static final Pattern NO_COMPRESS_EXT_PATTERN = Pattern.compile(
        "dex|arsc|so|jpg|jpeg|png|gif|wav|mp2|mp3|ogg|aac|mpg|mpeg|mid|midi|smf|jet|"
            + "rtttl|imy|xmf|mp4|m4a|m4v|3gp|3gpp|3g2|3gpp2|amr|awb|wma|wmv|webm|webp|mkv");

    private final ExtFile mApkFile;
    private final Config mConfig;
    private final AtomicReference<AndrolibException> mFirstError;

    private ApkInfo mApkInfo;
    private ResourcesDecoder mResDecoder;
    private volatile int mMinSdkVersion;
    private BackgroundWorker mWorker;

    public ApkDecoder(ExtFile apkFile, Config config) {
        mApkFile = apkFile;
        mConfig = config;
        mFirstError = new AtomicReference<>(null);
    }

    public ApkInfo decode(File outDir) throws AndrolibException {
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
            mApkInfo = new ApkInfo(mApkFile);
            mApkInfo.setVersion(mConfig.getVersion());
            mResDecoder = new ResourcesDecoder(mApkInfo, mConfig);

            OS.rmdir(outDir);
            OS.mkdir(outDir);

            LOGGER.info("Using Apktool " + mConfig.getVersion() + " on " + mApkFile.getName()
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

            return mApkInfo;
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

    private void decodeSources(File outDir) throws AndrolibException {
        if (!mApkInfo.hasSources()) {
            return;
        }

        switch (mConfig.getDecodeSources()) {
            case NONE:
                copySourcesRaw(outDir, "classes.dex");
                break;
            case FULL:
            case ONLY_MAIN_CLASSES:
                decodeSourcesSmali(outDir, "classes.dex");
                break;
        }

        try {
            Directory in = mApkFile.getDirectory();

            for (String fileName : in.getFiles(true)) {
                if (!fileName.endsWith(".dex") || fileName.equals("classes.dex")) {
                    continue;
                }

                switch (mConfig.getDecodeSources()) {
                    case NONE:
                        copySourcesRaw(outDir, fileName);
                        break;
                    case FULL:
                        decodeSourcesSmali(outDir, fileName);
                        break;
                    case ONLY_MAIN_CLASSES:
                        if (fileName.startsWith("classes")) {
                            decodeSourcesSmali(outDir, fileName);
                        } else {
                            copySourcesRaw(outDir, fileName);
                        }
                        break;
                }
            }
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    private void copySourcesRaw(File outDir, String fileName) throws AndrolibException {
        LOGGER.info("Copying raw " + fileName + " file...");
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
        File smaliDir;
        if (fileName.equals("classes.dex")) {
            smaliDir = new File(outDir, "smali");
        } else {
            smaliDir = new File(outDir, "smali_" + fileName.substring(0, fileName.indexOf('.')));
        }

        OS.mkdir(smaliDir);

        LOGGER.info("Baksmaling " + fileName + "...");
        SmaliDecoder decoder = new SmaliDecoder(mApkFile, fileName, mConfig.isBaksmaliDebugMode());
        decoder.decode(smaliDir);

        // Record minSdkVersion if there's no AndroidManifest.xml, i.e. JARs.
        int minSdkVersion = decoder.getInferredApiLevel();
        if (mMinSdkVersion == 0 || mMinSdkVersion > minSdkVersion) {
            mMinSdkVersion = minSdkVersion;
        }
    }

    private void decodeResources(File outDir) throws AndrolibException {
        if (!mApkInfo.hasResources()) {
            return;
        }

        switch (mConfig.getDecodeResources()) {
            case NONE:
            case ONLY_MANIFEST:
                copyResourcesRaw(outDir);
                break;
            case FULL:
                mResDecoder.decodeResources(outDir);
                break;
        }
    }

    private void copyResourcesRaw(File outDir) throws AndrolibException {
        LOGGER.info("Copying raw resources...");
        try {
            Directory in = mApkFile.getDirectory();

            in.copyToDir(outDir, "resources.arsc");
            in.copyToDir(outDir, ApkInfo.RESOURCES_DIRNAMES);
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    private void decodeManifest(File outDir) throws AndrolibException {
        if (!mApkInfo.hasManifest()) {
            return;
        }

        switch (mConfig.getDecodeResources()) {
            case NONE:
                copyManifestRaw(outDir);
                break;
            case FULL:
            case ONLY_MANIFEST:
                mResDecoder.decodeManifest(outDir);
                break;
        }
    }

    private void copyManifestRaw(File outDir) throws AndrolibException {
        LOGGER.info("Copying raw manifest...");
        try {
            Directory in = mApkFile.getDirectory();

            in.copyToDir(outDir, "AndroidManifest.xml");
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    private void copyRawFiles(File outDir) throws AndrolibException {
        try {
            Map<String, String> resFileMapping = mResDecoder.getResFileMapping();
            Directory in = mApkFile.getDirectory();

            for (String dirName : ApkInfo.RAW_DIRNAMES) {
                if (!in.containsDir(dirName) || (mConfig.getDecodeAssets() == Config.DecodeAssets.NONE
                        && dirName.equals("assets"))) {
                    continue;
                }

                LOGGER.info("Copying " + dirName + "...");
                for (String fileName : in.getDir(dirName).getFiles(true)) {
                    fileName = dirName + "/" + fileName;
                    if (!ApkInfo.ORIGINAL_FILENAMES_PATTERN.matcher(fileName).matches()
                            && !resFileMapping.containsKey(fileName)) {
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

        LOGGER.info("Copying original files...");
        try {
            Directory in = mApkFile.getDirectory();

            for (String fileName : in.getFiles(true)) {
                if (ApkInfo.ORIGINAL_FILENAMES_PATTERN.matcher(fileName).matches()) {
                    in.copyToDir(originalDir, fileName);
                }
            }
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    private void copyUnknownFiles(File outDir) throws AndrolibException {
        File unknownDir = new File(outDir, "unknown");

        LOGGER.info("Copying unknown files...");
        try {
            Map<String, String> resFileMapping = mResDecoder.getResFileMapping();
            Directory in = mApkFile.getDirectory();

            for (String fileName : in.getFiles(true)) {
                if (!ApkInfo.STANDARD_FILENAMES_PATTERN.matcher(fileName).matches()
                        && !resFileMapping.containsKey(fileName)) {
                    in.copyToDir(unknownDir, fileName);
                }
            }
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    private void writeApkInfo(File outDir) throws AndrolibException {
        // In case we have no resources, we store the inferred dex opcode API level.
        if (!mApkInfo.hasResources() && mMinSdkVersion > 0) {
            mApkInfo.getSdkInfo().setMinSdkVersion(Integer.toString(mMinSdkVersion));
        }

        // Record uncompressed files.
        try {
            Map<String, String> resFileMapping = mResDecoder.getResFileMapping();
            Set<String> uncompressedExts = new HashSet<>();
            Set<String> uncompressedFiles = new HashSet<>();
            Directory in = mApkFile.getDirectory();

            for (String fileName : in.getFiles(true)) {
                if (in.getCompressionLevel(fileName) == 0) {
                    String ext;
                    if (in.getSize(fileName) > 0
                            && !(ext = FilenameUtils.getExtension(fileName)).isEmpty()
                            && NO_COMPRESS_EXT_PATTERN.matcher(ext).matches()) {
                        uncompressedExts.add(ext);
                    } else {
                        uncompressedFiles.add(resFileMapping.getOrDefault(fileName, fileName));
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

        // Serialize ApkInfo to file.
        mApkInfo.save(new File(outDir, "apktool.yml"));
    }
}
