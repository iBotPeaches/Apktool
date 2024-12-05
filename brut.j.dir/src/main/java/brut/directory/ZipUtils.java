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
package brut.directory;

import brut.common.BrutException;
import brut.common.InvalidUnknownFileException;
import brut.common.RootUnknownFileException;
import brut.common.TraversalUnknownFileException;
import brut.util.BrutIO;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.file.Files;
import java.util.Collection;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class ZipUtils {
    private static final Logger LOGGER = Logger.getLogger("");

    private ZipUtils() {
        // Private constructor for utility class
    }

    public static void zipDir(File dir, ZipOutputStream out, Collection<String> doNotCompress)
            throws IOException {
        zipDir(dir, null, out, doNotCompress);
    }

    public static void zipDir(File baseDir, String dirName, ZipOutputStream out, Collection<String> doNotCompress)
            throws IOException {
        File dir;
        if (dirName == null || dirName.isEmpty()) {
            dir = baseDir;
        } else {
            dir = new File(baseDir, dirName);
        }
        if (!dir.isDirectory()) {
            return;
        }

        for (File file : dir.listFiles()) {
            String fileName = baseDir.toURI().relativize(file.toURI()).getPath();

            if (file.isDirectory()) {
                zipDir(baseDir, fileName, out, doNotCompress);
            } else if (file.isFile()) {
                zipFile(baseDir, fileName, out, doNotCompress != null && !doNotCompress.isEmpty()
                    ? entryName -> doNotCompress.contains(entryName)
                            || doNotCompress.contains(FilenameUtils.getExtension(entryName))
                    : entryName -> false);
            }
        }
    }

    public static void zipFile(File baseDir, String fileName, ZipOutputStream out, boolean doNotCompress)
            throws IOException {
        zipFile(baseDir, fileName, out, entryName -> doNotCompress);
    }

    private static void zipFile(File baseDir, String fileName, ZipOutputStream out, Predicate<String> doNotCompress)
            throws IOException {
        try {
            String validFileName = BrutIO.sanitizePath(baseDir, fileName);
            if (validFileName.isEmpty()) {
                return;
            }

            File file = new File(baseDir, validFileName);
            if (!file.isFile()) {
                return;
            }

            String entryName = BrutIO.adaptSeparatorToUnix(validFileName);
            ZipEntry zipEntry = new ZipEntry(entryName);

            if (doNotCompress.test(entryName)) {
                zipEntry.setMethod(ZipEntry.STORED);
                zipEntry.setSize(file.length());
                try (BufferedInputStream in = new BufferedInputStream(Files.newInputStream(file.toPath()))) {
                    CRC32 crc = BrutIO.calculateCrc(in);
                    zipEntry.setCrc(crc.getValue());
                }
            } else {
                zipEntry.setMethod(ZipEntry.DEFLATED);
            }

            out.putNextEntry(zipEntry);
            try (InputStream in = Files.newInputStream(file.toPath())) {
                IOUtils.copy(in, out);
            }
            out.closeEntry();
        } catch (RootUnknownFileException | InvalidUnknownFileException | TraversalUnknownFileException ex) {
            LOGGER.warning(String.format("Skipping file %s (%s)", fileName, ex.getMessage()));
        }
    }
}
