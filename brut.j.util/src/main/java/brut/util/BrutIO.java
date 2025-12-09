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
package brut.util;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.CRC32;

public final class BrutIO {

    private BrutIO() {
        // Private constructor for utility class.
    }

    public static byte[] readAndClose(InputStream in) throws IOException {
        try {
            return IOUtils.toByteArray(in);
        } finally {
            IOUtils.closeQuietly(in);
        }
    }

    public static void copyAndClose(InputStream in, OutputStream out) throws IOException {
        try {
            IOUtils.copy(in, out);
        } finally {
            IOUtils.closeQuietly(in);
            IOUtils.closeQuietly(out);
        }
    }

    public static long recursiveModifiedTime(File[] files) {
        long modified = 0;
        for (File file : files) {
            long submodified = recursiveModifiedTime(file);
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
            for (File subfile : subfiles) {
                long submodified = recursiveModifiedTime(subfile);
                if (submodified > modified) {
                    modified = submodified;
                }
            }
        }
        return modified;
    }

    public static CRC32 calculateCrc(InputStream in) throws IOException {
        CRC32 crc = new CRC32();
        int bytesRead;
        byte[] buffer = new byte[8192];
        while ((bytesRead = in.read(buffer)) != -1) {
            crc.update(buffer, 0, bytesRead);
        }
        return crc;
    }

    public static String sanitizePath(File baseDir, String path) throws InvalidPathException, IOException {
        if (path == null || path.isEmpty()) {
            throw new InvalidPathException(path, "Path is null or empty.");
        }

        Path origPath = Paths.get(path);
        if (origPath.isAbsolute()) {
            throw new InvalidPathException(path, "Absolute paths are not allowed.");
        }

        Path basePath = Paths.get(baseDir.getCanonicalPath());
        Path resolvedPath = basePath.resolve(origPath).normalize();
        if (!resolvedPath.startsWith(basePath)) {
            throw new InvalidPathException(path, "Path traverses outside the base directory");
        }

        return basePath.relativize(resolvedPath).toString();
    }
}
