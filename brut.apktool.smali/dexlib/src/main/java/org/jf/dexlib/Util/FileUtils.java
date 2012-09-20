/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * As per the Apache license requirements, this file has been modified
 * from its original state.
 *
 * Such modifications are Copyright (C) 2010 Ben Gruver, and are released
 * under the original license
 */

package org.jf.dexlib.Util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * File I/O utilities.
 */
public final class FileUtils {
    /**
     * This class is uninstantiable.
     */
    private FileUtils() {
        // This space intentionally left blank.
    }

    /**
     * Reads the named file, translating {@link IOException} to a
     * {@link RuntimeException} of some sort.
     *
     * @param fileName non-null; name of the file to read
     * @return non-null; contents of the file
     */
    public static byte[] readFile(String fileName)
            throws IOException {
        File file = new File(fileName);
        return readFile(file);
    }

    /**
     * Reads the given file, translating {@link IOException} to a
     * {@link RuntimeException} of some sort.
     *
     * @param file non-null; the file to read
     * @return non-null; contents of the file
     */
    public static byte[] readFile(File file)
            throws IOException {
        return readFile(file, 0, -1);
    }

    /**
     * Reads the specified block from the given file, translating
     * {@link IOException} to a {@link RuntimeException} of some sort.
     *
     * @param file non-null; the file to read
     * @param offset the offset to begin reading
     * @param length the number of bytes to read, or -1 to read to the
     * end of the file
     * @return non-null; contents of the file
     */
    public static byte[] readFile(File file, int offset, int length)
            throws IOException {
        if (!file.exists()) {
            throw new RuntimeException(file + ": file not found");
        }

        if (!file.isFile()) {
            throw new RuntimeException(file + ": not a file");
        }

        if (!file.canRead()) {
            throw new RuntimeException(file + ": file not readable");
        }

        long longLength = file.length();
        int fileLength = (int) longLength;
        if (fileLength != longLength) {
            throw new RuntimeException(file + ": file too long");
        }

        if (length == -1) {
            length = fileLength - offset;
        }

        if (offset + length > fileLength) {
            throw new RuntimeException(file + ": file too short");
        }

        FileInputStream in = new FileInputStream(file);

        int at = offset;
        while(at > 0) {
            long amt = in.skip(at);
            if (amt == -1) {
                throw new RuntimeException(file + ": unexpected EOF");
            }
            at -= amt;
        }

        byte[] result = readStream(in, length);

        in.close();

        return result;
    }

    public static byte[] readStream(InputStream in, int length)
            throws IOException {
        byte[] result = new byte[length];
        int at=0;

        while (length > 0) {
            int amt = in.read(result, at, length);
            if (amt == -1) {
                throw new RuntimeException("unexpected EOF");
            }
            at += amt;
            length -= amt;
        }

        return result;
    }
}
