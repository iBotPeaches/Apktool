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
import brut.androlib.exceptions.AndrolibException;
import brut.androlib.exceptions.CantFindFrameworkResException;
import brut.androlib.res.decoder.ARSCDecoder;
import brut.androlib.res.data.arsc.ARSCData;
import brut.androlib.res.data.arsc.FlagsOffset;
import brut.util.BrutIO;

import java.io.*;
import java.nio.file.Files;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class Framework {
    private static final Logger LOGGER = Logger.getLogger(Framework.class.getName());

    private final Config mConfig;
    private File mFrameworkDirectory;

    public Framework(Config config) {
        mConfig = config;
    }

    public void installFramework(File frameFile) throws AndrolibException {
        installFramework(frameFile, mConfig.frameworkTag);
    }

    public void installFramework(File frameFile, String tag) throws AndrolibException {
        try (ZipFile zip = new ZipFile(frameFile)) {
            ZipEntry entry = zip.getEntry("resources.arsc");

            if (entry == null) {
                throw new AndrolibException("Could not find resources.arsc file");
            }

            byte[] data = BrutIO.readAndClose(zip.getInputStream(entry));
            ARSCDecoder decoder = new ARSCDecoder(new ByteArrayInputStream(data), null, true, true);
            ARSCData arsc = decoder.decode();
            publicizeResources(data, arsc.getFlagsOffsets());

            File outFile = new File(getFrameworkDirectory(),
                arsc.getOnePackage().getId() + (tag == null ? "" : '-' + tag) + ".apk");

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

    public void listFrameworkDirectory() throws AndrolibException {
        File dir = getFrameworkDirectory();
        if (dir == null) {
            LOGGER.severe("No framework directory found. Nothing to list.");
            return;
        }

        for (File file : Objects.requireNonNull(dir.listFiles())) {
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
        ARSCDecoder decoder = new ARSCDecoder(new ByteArrayInputStream(data), null, true, true);
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

    public File getFrameworkDirectory() throws AndrolibException {
        if (mFrameworkDirectory != null) {
            return mFrameworkDirectory;
        }

        String path;

        // use default framework path or specified on the command line
        path = mConfig.frameworkDirectory;

        File dir = new File(path);

        if (!dir.isDirectory() && dir.isFile()) {
            throw new AndrolibException("--frame-path is set to a file, not a directory.");
        }

        if (dir.getParentFile() != null && dir.getParentFile().isFile()) {
            throw new AndrolibException("Please remove file at " + dir.getParentFile());
        }

        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                if (mConfig.frameworkDirectory != null) {
                    LOGGER.severe("Could not create Framework directory: " + dir);
                }
                throw new AndrolibException(String.format(
                    "Could not create directory: (%s). Pass a writable path with --frame-path {DIR}. ", dir
                ));
            }
        }

        if (mConfig.frameworkDirectory == null) {
            if (!dir.canWrite()) {
                LOGGER.severe(String.format("WARNING: Could not write to (%1$s), using %2$s instead...",
                    dir.getAbsolutePath(), System.getProperty("java.io.tmpdir")));
                LOGGER.severe("Please be aware this is a volatile directory and frameworks could go missing, " +
                    "please utilize --frame-path if the default storage directory is unavailable");

                dir = new File(System.getProperty("java.io.tmpdir"));
            }
        }

        mFrameworkDirectory = dir;
        return dir;
    }

    public File getFrameworkApk(int id, String frameTag) throws AndrolibException {
        File dir = getFrameworkDirectory();
        File apk;

        if (frameTag != null) {
            apk = new File(dir, String.valueOf(id) + '-' + frameTag + ".apk");
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
                BrutIO.copyAndClose(getAndroidFrameworkResourcesAsStream(), Files.newOutputStream(apk.toPath()));
            } catch (IOException ex) {
                throw new AndrolibException(ex);
            }
            return apk;
        }

        throw new CantFindFrameworkResException(id);
    }

    public void emptyFrameworkDirectory() throws AndrolibException {
        File dir = getFrameworkDirectory();
        File apk;

        apk = new File(dir, "1.apk");

        if (!apk.exists()) {
            LOGGER.warning("Could not empty framework directory, no file found at: " + apk.getAbsolutePath());
        } else {
            try {
                if (apk.exists() && Objects.requireNonNull(dir.listFiles()).length > 1 && !mConfig.forceDeleteFramework) {
                    LOGGER.warning("More than default framework detected. Please run command with `--force` parameter to wipe framework directory.");
                } else {
                    for (File file : Objects.requireNonNull(dir.listFiles())) {
                        if (file.isFile() && file.getName().endsWith(".apk")) {
                            LOGGER.info("Removing " + file.getName() + " framework file...");
                            //noinspection ResultOfMethodCallIgnored
                            file.delete();
                        }
                    }
                }
            } catch (NullPointerException ex) {
                throw new AndrolibException(ex);
            }
        }
    }

    private InputStream getAndroidFrameworkResourcesAsStream() {
        return Framework.class.getResourceAsStream("/prebuilt/android-framework.jar");
    }
}
