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
import brut.androlib.exceptions.FrameworkNotFoundException;
import brut.androlib.meta.ApkInfo;
import brut.androlib.res.decoder.BinaryResourceParser;
import brut.androlib.res.decoder.data.FlagsOffset;
import brut.androlib.res.table.ResPackage;
import brut.androlib.res.table.ResTable;
import brut.util.BrutIO;
import brut.util.OS;
import brut.util.OSDetection;
import com.google.common.primitives.Ints;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Logger;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class Framework {
    private static final Logger LOGGER = Logger.getLogger(Framework.class.getName());

    private static final File DEFAULT_DIRECTORY;

    static {
        String userHome = System.getProperty("user.home");
        Path defDir;
        if (OSDetection.isMacOSX()) {
            defDir = Paths.get(userHome, "Library", "apktool", "framework");
        } else if (OSDetection.isWindows()) {
            defDir = Paths.get(userHome, "AppData", "Local", "apktool", "framework");
        } else {
            String xdgDataHome = System.getenv("XDG_DATA_HOME");
            if (xdgDataHome != null) {
                defDir = Paths.get(xdgDataHome, "apktool", "framework");
            } else {
                defDir = Paths.get(userHome, ".local", "share", "apktool", "framework");
            }
        }
        DEFAULT_DIRECTORY = defDir.toFile();
    }

    private final Config mConfig;
    private File mDirectory;

    public Framework(Config config) {
        mConfig = config;
    }

    public void install(File apkFile) throws AndrolibException {
        try (ZipFile zip = new ZipFile(apkFile)) {
            ZipEntry entry = zip.getEntry("resources.arsc");
            if (entry == null) {
                throw new AndrolibException("Could not find resources.arsc in file: " + apkFile);
            }

            byte[] data = BrutIO.readAndClose(zip.getInputStream(entry));
            BinaryResourceParser parser = parseResources(data);
            publicizeResources(data, parser.getFlagsOffsets());

            List<ResPackage> pkgs = parser.getPackages();
            if (pkgs.isEmpty()) {
                throw new AndrolibException("No packages in resources.arsc in file: " + apkFile);
            }

            ResPackage pkg = selectPackageWithMostEntrySpecs(pkgs);
            File outFile = new File(getDirectory(), pkg.getId() + getApkSuffix());

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

                // Write fake AndroidManifest.xml file to support original aapt.
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

    private BinaryResourceParser parseResources(byte[] data) throws AndrolibException {
        ResTable table = new ResTable(new ApkInfo(), mConfig);
        BinaryResourceParser parser = new BinaryResourceParser(table, true, true);
        parser.parse(new ByteArrayInputStream(data));
        return parser;
    }

    private ResPackage selectPackageWithMostEntrySpecs(List<ResPackage> pkgs) {
        ResPackage ret = pkgs.get(0);
        int count = 0;

        for (ResPackage pkg : pkgs) {
            if (pkg.getEntrySpecCount() > count) {
                count = pkg.getEntrySpecCount();
                ret = pkg;
            }
        }

        return ret;
    }

    private void publicizeResources(byte[] data, List<FlagsOffset> flagsOffsets) {
        for (FlagsOffset flags : flagsOffsets) {
            int offset = ((int) flags.offset) + 3;
            int end = offset + 4 * flags.count;

            while (offset < end) {
                data[offset] |= (byte) 0x40;
                offset += 4;
            }
        }
    }

    public File getDirectory() throws AndrolibException {
        if (mDirectory == null) {
            String path = mConfig.getFrameworkDirectory();
            File dir = (path != null && !path.isEmpty()) ? new File(path) : DEFAULT_DIRECTORY;

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

    public File getApkFile(int id) throws AndrolibException {
        return getApkFile(id, mConfig.getFrameworkTag());
    }

    public File getApkFile(int id, String tag) throws AndrolibException {
        File dir = getDirectory();
        File apkFile = new File(dir, id + getApkSuffix(tag));
        if (apkFile.exists()) {
            return apkFile;
        }

        // Fall back to the untagged framework.
        apkFile = new File(dir, id + getApkSuffix(null));
        if (apkFile.exists()) {
            return apkFile;
        }

        // If the default framework is requested but is missing, extract the built-in one.
        if (id == 1) {
            try {
                BrutIO.copyAndClose(getAndroidFrameworkAsStream(), Files.newOutputStream(apkFile.toPath()));
            } catch (IOException ex) {
                throw new AndrolibException(ex);
            }
            return apkFile;
        }

        throw new FrameworkNotFoundException(id);
    }

    private String getApkSuffix() {
        return getApkSuffix(mConfig.getFrameworkTag());
    }

    private String getApkSuffix(String tag) {
        return ((tag != null && !tag.isEmpty()) ? "-" + tag : "") + ".apk";
    }

    private InputStream getAndroidFrameworkAsStream() {
        return getClass().getResourceAsStream("/prebuilt/android-framework.jar");
    }

    public void cleanDirectory() throws AndrolibException {
        for (File apkFile : listDirectory()) {
            LOGGER.info("Removing framework file: " + apkFile.getName());
            OS.rmfile(apkFile);
        }
    }

    public List<File> listDirectory() throws AndrolibException {
        boolean ignoreTag = mConfig.isForced();
        String suffix = ignoreTag ? getApkSuffix(null) : getApkSuffix();
        List<File> apkFiles = new ArrayList<>();

        for (File file : getDirectory().listFiles()) {
            if (file.isFile() && isValidApkName(file.getName(), suffix, ignoreTag)) {
                apkFiles.add(file);
            }
        }

        return apkFiles;
    }

    private boolean isValidApkName(String fileName, String suffix, boolean ignoreTag) {
        if (!fileName.endsWith(suffix)) {
            return false;
        }
        if (ignoreTag) {
            return true;
        }

        String baseName = fileName.substring(0, fileName.length() - suffix.length());
        Integer id = Ints.tryParse(baseName);
        return id != null && id > 0;
    }

    public void publicizeResources(File arscFile) throws AndrolibException {
        byte[] data = new byte[(int) arscFile.length()];

        try (InputStream in = Files.newInputStream(arscFile.toPath())) {
            //noinspection ResultOfMethodCallIgnored
            in.read(data);
        } catch (IOException ex) {
            throw new AndrolibException(ex);
        }

        BinaryResourceParser parser = parseResources(data);
        publicizeResources(data, parser.getFlagsOffsets());

        try (OutputStream out = Files.newOutputStream(arscFile.toPath())) {
            out.write(data);
        } catch (IOException ex) {
            throw new AndrolibException(ex);
        }
    }
}
