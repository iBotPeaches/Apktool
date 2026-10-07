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

import brut.androlib.ApkFile;
import brut.androlib.Config;
import brut.androlib.exceptions.AndrolibException;
import brut.androlib.exceptions.FrameworkNotFoundException;
import brut.androlib.exceptions.InFileNotFoundException;
import brut.androlib.res.decoder.BinaryResourceParser;
import brut.androlib.res.table.ResTable;
import brut.common.Log;
import brut.util.JarUtils;
import brut.util.Pair;
import brut.util.SystemUtils;
import brut.zip.ZipArchive;
import brut.zip.ZipFileEntry;
import brut.zip.ZipUtils;
import com.google.common.io.ByteStreams;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Framework {
    private static final String TAG = Framework.class.getName();

    private static final Path DEFAULT_DIRECTORY;
    static {
        String home = System.getProperty("user.home");
        Path basePath;
        if (SystemUtils.isWindows()) {
            basePath = Paths.get(home, "AppData", "Local");
        } else if (SystemUtils.isMac()) {
            basePath = Paths.get(home, "Library");
        } else {
            // A valid XDG_DATA_HOME environment variable must be an absolute path.
            String xdgDataHome = System.getenv("XDG_DATA_HOME");
            Path xdgDataHomePath;
            if (xdgDataHome != null && (xdgDataHomePath = Paths.get(xdgDataHome)).isAbsolute()) {
                basePath = xdgDataHomePath;
            } else {
                basePath = Paths.get(home, ".local", "share");
            }
        }
        DEFAULT_DIRECTORY = basePath.resolve("apktool").resolve("framework");
    }

    private final Config mConfig;
    private Path mDirectory;

    public Framework(Config config) {
        mConfig = config;
    }

    public void install(Path apkFile) throws AndrolibException {
        if (!Files.isRegularFile(apkFile) || !Files.isReadable(apkFile)) {
            throw new InFileNotFoundException(apkFile);
        }
        try (ZipArchive zip = new ZipArchive(apkFile)) {
            ZipFileEntry file = zip.getFile("resources.arsc");
            byte[] data;
            try (InputStream in = file.getInputStream()) {
                data = ByteStreams.toByteArray(in);
            }
            ResTable table = parseAndPublicizeResources(data);
            int pkgId = table.listPackageGroups().iterator().next().getId();
            Path outFile = getDirectory().resolve(pkgId + getFileSuffix());

            try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(outFile))) {
                out.setMethod(ZipOutputStream.STORED);
                ZipEntry entry = new ZipEntry("resources.arsc");
                entry.setTime(ApkFile.TIME_MILLIS);
                entry.setSize(data.length);
                entry.setCrc(ZipUtils.computeCrc(data));
                out.putNextEntry(entry);
                out.write(data);
                out.closeEntry();

                // Copy AndroidManifest.xml to support legacy aapt.
                file = zip.getFile("AndroidManifest.xml");
                try (InputStream in = file.getInputStream()) {
                    data = ByteStreams.toByteArray(in);
                }
                entry = new ZipEntry("AndroidManifest.xml");
                entry.setTime(ApkFile.TIME_MILLIS);
                entry.setSize(data.length);
                entry.setCrc(ZipUtils.computeCrc(data));
                out.putNextEntry(entry);
                out.write(data);
                out.closeEntry();
            } catch (IOException ex) {
                try {
                    Files.deleteIfExists(outFile);
                } catch (IOException suppressed) {
                    ex.addSuppressed(suppressed);
                }
                throw ex;
            }

            Log.i(TAG, "Framework installed to: " + outFile);
        } catch (IOException ex) {
            throw new AndrolibException(ex);
        }
    }

    private ResTable parseAndPublicizeResources(byte[] data) throws AndrolibException {
        ResTable table = new ResTable(mConfig);
        BinaryResourceParser parser = new BinaryResourceParser(table, true, true);
        parser.enableCollectFlagsOffsets();
        parser.parse(new ByteArrayInputStream(data));

        if (table.getPackageGroupCount() == 0) {
            throw new AndrolibException("No packages in resources.arsc in file.");
        }

        // Publicize all entry specs.
        ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        for (Pair<Long, Integer> pair : parser.getEntrySpecFlagsOffsets()) {
            int position = pair.getLeft().intValue();
            int count = pair.getRight();
            for (int i = 0; i < count; i++, position += 4) {
                int flags = buffer.getInt(position);
                buffer.putInt(position, flags | 0x40000000); // ResTable_typeSpec::SPEC_PUBLIC
            }
        }

        return table;
    }

    public Path getDirectory() throws AndrolibException {
        if (mDirectory == null) {
            String path = mConfig.getFrameworkDirectory();
            Path dir = (path != null && !path.isEmpty()) ? Paths.get(path) : DEFAULT_DIRECTORY;
            try {
                Files.createDirectories(dir);
            } catch (IOException ex) {
                throw new AndrolibException("Could not create framework directory: " + dir, ex);
            }
            mDirectory = dir;
        }

        return mDirectory;
    }

    public Path getFile(int id) throws AndrolibException {
        return getFile(id, mConfig.getFrameworkTag());
    }

    public Path getFile(int id, String tag) throws AndrolibException {
        Path dir = getDirectory();
        Path file = dir.resolve(id + getFileSuffix(tag));
        if (Files.exists(file)) {
            return file;
        }

        // Fall back to the untagged framework.
        file = dir.resolve(id + getFileSuffix(null));
        if (Files.exists(file)) {
            return file;
        }

        // If the default framework is requested but is missing, extract the built-in one.
        if (id == 1) {
            try (InputStream in = JarUtils.getResourceAsStream(getClass(), "/prebuilt/android-framework.jar")) {
                Files.copy(in, file);
            } catch (IOException ex) {
                throw new AndrolibException(ex);
            }
            return file;
        }

        throw new FrameworkNotFoundException(id);
    }

    private String getFileSuffix() {
        return getFileSuffix(mConfig.getFrameworkTag());
    }

    private static String getFileSuffix(String tag) {
        return ((tag != null && !tag.isEmpty()) ? "-" + tag : "") + ".apk";
    }

    public void cleanDirectory() throws AndrolibException {
        try {
            for (Path file : listDirectory()) {
                Log.i(TAG, "Removing framework file: " + file.getFileName());
                Files.delete(file);
            }
        } catch (IOException ex) {
            throw new AndrolibException(ex);
        }
    }

    public List<Path> listDirectory() throws AndrolibException {
        boolean ignoreTag = mConfig.isForced();
        String suffix = ignoreTag ? getFileSuffix(null) : getFileSuffix();
        List<Path> files = new ArrayList<>();

        try (Stream<Path> stream = Files.list(getDirectory())) {
            Iterator<Path> it = stream.filter(Files::isRegularFile).sorted().iterator();
            while (it.hasNext()) {
                Path file = it.next();
                if (isValidFileName(file.getFileName().toString(), suffix, ignoreTag)) {
                    files.add(file);
                }
            }
        } catch (IOException ex) {
            throw new AndrolibException(ex);
        }

        return files;
    }

    private static boolean isValidFileName(String fileName, String suffix, boolean ignoreTag) {
        if (!fileName.endsWith(suffix)) {
            return false;
        }
        if (ignoreTag) {
            return true;
        }

        int len = fileName.length() - suffix.length();
        if (len == 0) {
            return false;
        }
        for (int i = 0; i < len; i++) {
            char ch = fileName.charAt(i);
            if (ch < '0' || ch > '9') {
                return false;
            }
        }
        return true;
    }

    public void publicizeResources(Path arscFile) throws AndrolibException {
        if (!Files.isRegularFile(arscFile) || !Files.isReadable(arscFile)) {
            throw new InFileNotFoundException(arscFile);
        }
        try {
            byte[] data = Files.readAllBytes(arscFile);
            parseAndPublicizeResources(data);
            Files.write(arscFile, data);
        } catch (IOException ex) {
            throw new AndrolibException(ex);
        }
    }
}
