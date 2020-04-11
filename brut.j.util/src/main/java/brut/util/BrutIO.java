/*
 *  Copyright (C) 2010 Ryszard Wiśniewski <brut.alll@gmail.com>
 *  Copyright (C) 2010 Connor Tumbleson <connor.tumbleson@gmail.com>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package brut.util;

import java.io.*;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import brut.common.BrutException;
import brut.common.InvalidUnknownFileException;
import brut.common.RootUnknownFileException;
import brut.common.TraversalUnknownFileException;
import org.apache.commons.io.IOUtils;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public class BrutIO {
    public static void copyAndClose(InputStream in, OutputStream out)
            throws IOException {
        try {
            IOUtils.copy(in, out);
        } finally {
            IOUtils.closeQuietly(in);
            IOUtils.closeQuietly(out);
        }
    }

    public static long recursiveModifiedTime(File[] files) {
        long modified = 0;
        for (int i = 0; i < files.length; i++) {
            long submodified = recursiveModifiedTime(files[i]);
            if (submodified > modified) {
                modified = submodified;
            }
        }
        return modified;
    }

    public static long recursiveModifiedTime(File file) {
        long modified = file.lastModified();
        if (file.isDirectory()) {
            File[] subfiles = file.listFiles();
            for (int i = 0; i < subfiles.length; i++) {
                long submodified = recursiveModifiedTime(subfiles[i]);
                if (submodified > modified) {
                    modified = submodified;
                }
            }
        }
        return modified;
    }

    public static CRC32 calculateCrc(InputStream input) throws IOException {
        CRC32 crc = new CRC32();
        int bytesRead;
        byte[] buffer = new byte[8192];
        while((bytesRead = input.read(buffer)) != -1) {
            crc.update(buffer, 0, bytesRead);
        }
        return crc;
    }

    public static String sanitizeUnknownFile(final File directory, final String entry) throws IOException, BrutException {
        if (entry.length() == 0) {
            throw new InvalidUnknownFileException("Invalid Unknown File - " + entry);
        }

        if (new File(entry).isAbsolute()) {
            throw new RootUnknownFileException("Absolute Unknown Files is not allowed - " + entry);
        }

        final String canonicalDirPath = directory.getCanonicalPath() + File.separator;
        final String canonicalEntryPath = new File(directory, entry).getCanonicalPath();

        if (!canonicalEntryPath.startsWith(canonicalDirPath)) {
            throw new TraversalUnknownFileException("Directory Traversal is not allowed - " + entry);
        }

        // https://stackoverflow.com/q/2375903/455008
        return canonicalEntryPath.substring(canonicalDirPath.length());
    }

    public static String normalizePath(String path) {
        char separator = File.separatorChar;

        if (separator != '/') {
            return path.replace(separator, '/');
        }

        return path;
    }

    public static void copy(File inputFile, ZipOutputStream outputFile) throws IOException {
        try (
                FileInputStream fis = new FileInputStream(inputFile)
        ) {
            IOUtils.copy(fis, outputFile);
        }
    }

    public static void copy(ZipFile inputFile, ZipOutputStream outputFile, ZipEntry entry) throws IOException {
        try (
                InputStream is = inputFile.getInputStream(entry)
        ) {
            IOUtils.copy(is, outputFile);
        }
    }

}
