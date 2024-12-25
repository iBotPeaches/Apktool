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
package brut.androlib.res;

import brut.androlib.Config;
import brut.androlib.apk.ApkInfo;
import brut.androlib.exceptions.AndrolibException;
import brut.androlib.exceptions.CantFindFrameworkResException;
import brut.androlib.res.decoder.ARSCDecoder;
import brut.androlib.res.data.ResTable;
import brut.androlib.res.data.arsc.ARSCData;
import brut.androlib.res.data.arsc.FlagsOffset;
import brut.util.BrutIO;
import brut.util.OS;

import java.io.*;
import java.nio.file.Files;
import java.util.logging.Logger;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class Framework {
    private static final Logger LOGGER = Logger.getLogger(Framework.class.getName());

    private final Config mConfig;
    private File mDirectory;

    public Framework(Config config) {
        mConfig = config;
    }

    public void install(File frameFile) throws AndrolibException {
        install(frameFile, mConfig.getFrameworkTag());
    }

    public void install(File frameFile, String tag) throws AndrolibException {
        try (ZipFile zip = new ZipFile(frameFile)) {
            ZipEntry entry = zip.getEntry("resources.arsc");
            if (entry == null) {
                throw new AndrolibException("Could not find resources.arsc file");
            }

            byte[] data = BrutIO.readAndClose(zip.getInputStream(entry));
            ResTable resTable = new ResTable(new ApkInfo(), mConfig);
            ARSCDecoder decoder = new ARSCDecoder(new ByteArrayInputStream(data), resTable, true, true);
            ARSCData arsc = decoder.decode();
            publicizeResources(data, arsc.getFlagsOffsets());

            File outFile = new File(getDirectory(),
                arsc.getOnePackage().getId() + (tag != null ? "-" + tag : "") + ".apk");

            try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(outFile.toPath()))) {
                out.setMethod(ZipOutputStream.STORED);
                CRC32 crc = new CRC32();
                crc.update(data);
                entry = new ZipEntry("resources.arsc");
                entry.setSize(data.length);
                entry.setMethod(ZipEntry.STORED);
                entry.setCrc(crc.getValue());
                out.putNextEntry(entry);
                out.write(data);
                out.closeEntry();

                // write fake AndroidManifest.xml file to support original aapt
                entry = zip.getEntry("AndroidManifest.xml");
                if (entry != null) {
                    byte[] manifest = BrutIO.readAndClose(zip.getInputStream(entry));
                    CRC32 manifestCrc = new CRC32();
                    manifestCrc.update(manifest);
                    entry.setSize(manifest.length);
                    entry.setCompressedSize(-1);
                    entry.setCrc(manifestCrc.getValue());
                    out.putNextEntry(entry);
                    out.write(manifest);
                    out.closeEntry();
                }
            }

            LOGGER.info("Framework installed to: " + outFile);
        } catch (IOException ex) {
            throw new AndrolibException(ex);
        }
    }

    public void listDirectory() throws AndrolibException {
        File dir = getDirectory();
        if (dir == null) {
            LOGGER.severe("No framework directory found. Nothing to list.");
            return;
        }

        for (File file : dir.listFiles()) {
            if (file.isFile() && file.getName().endsWith(".apk")) {
                LOGGER.info(file.getName());
            }
        }
    }

    public void publicizeResources(File arscFile) throws AndrolibException {
        byte[] data = new byte[(int) arscFile.length()];

        try (
            InputStream in = Files.newInputStream(arscFile.toPath());
            OutputStream out = Files.newOutputStream(arscFile.toPath())
        ) {
            //noinspection ResultOfMethodCallIgnored
            in.read(data);
            publicizeResources(data);
            out.write(data);
        } catch (IOException ex){
            throw new AndrolibException(ex);
        }
    }

    private void publicizeResources(byte[] data) throws AndrolibException {
        ResTable resTable = new ResTable(new ApkInfo(), mConfig);
        ARSCDecoder decoder = new ARSCDecoder(new ByteArrayInputStream(data), resTable, true, true);
        ARSCData arsc = decoder.decode();
        publicizeResources(data, arsc.getFlagsOffsets());
    }

    public void publicizeResources(byte[] data, FlagsOffset[] flagsOffsets) {
        for (FlagsOffset flags : flagsOffsets) {
            int offset = flags.offset + 3;
            int end = offset + 4 * flags.count;
            while (offset < end) {
                data[offset] |= (byte) 0x40;
                offset += 4;
            }
        }
    }

    public File getDirectory() throws AndrolibException {
        if (mDirectory == null) {
            File dir = new File(mConfig.getFrameworkDirectory());

            if (dir.exists() && !dir.isDirectory()) {
                throw new AndrolibException("Framework path is not a directory: " + dir);
            }

            File parent = dir.getParentFile();
            if (parent != null && parent.exists() && !parent.isDirectory()) {
                throw new AndrolibException("Framework path's parent is not a directory: " + parent);
            }

            if (!dir.exists() && !dir.mkdirs()) {
                throw new AndrolibException("Could not create framework directory: " + dir);
            }

            mDirectory = dir;
        }

        return mDirectory;
    }

    public File getApkFile(int id, String frameTag) throws AndrolibException {
        File dir = getDirectory();
        File apk;

        if (frameTag != null) {
            apk = new File(dir, id + "-" + frameTag + ".apk");
            if (apk.exists()) {
                return apk;
            }
        }

        apk = new File(dir, id + ".apk");
        if (apk.exists()) {
            return apk;
        }

        if (id == 1) {
            try {
                BrutIO.copyAndClose(getAndroidFrameworkAsStream(), Files.newOutputStream(apk.toPath()));
            } catch (IOException ex) {
                throw new AndrolibException(ex);
            }
            return apk;
        }

        throw new CantFindFrameworkResException(id);
    }

    private InputStream getAndroidFrameworkAsStream() {
        return getClass().getResourceAsStream("/prebuilt/android-framework.jar");
    }

    public void emptyDirectory() throws AndrolibException {
        File dir = getDirectory();
        File apk = new File(dir, "1.apk");

        if (!apk.exists()) {
            LOGGER.warning("Could not empty framework directory, no file found at: " + apk);
            return;
        }

        File[] files = dir.listFiles();

        if (apk.exists() && files.length > 1 && !mConfig.isForceDeleteFramework()) {
            LOGGER.warning("More than default framework detected. Please run command with `--force` parameter to wipe framework directory.");
            return;
        }

        for (File file : files) {
            if (file.isFile() && file.getName().endsWith(".apk")) {
                LOGGER.info("Removing framework file: " + file.getName());
                OS.rmfile(file);
            }
        }
    }
}
