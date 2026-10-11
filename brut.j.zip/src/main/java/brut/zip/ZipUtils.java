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
package brut.zip;

import java.io.InputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.CRC32;

public final class ZipUtils {
    public static final String SEPARATOR = "/";
    public static final char SEPARATOR_CHAR = '/';

    private ZipUtils() {}

    public static String normalize(String name) {
        int len = name.length();
        if (len == 0) {
            return name;
        }
        char ch = name.charAt(0);
        if (ch == SEPARATOR_CHAR || ch == '\\' || (len >= 2 && name.charAt(1) == ':'
                && ((ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z')))) {
            throw new IllegalArgumentException("Name is an absolute path.");
        }
        StringBuilder sb = null; // lazily initialized
        boolean prevSep = false;
        for (int i = 0; i < len; i++) {
            ch = name.charAt(i);
            if (ch == SEPARATOR_CHAR || ch == '\\') {
                // Repeated slashes are dropped and backslashes are rewritten as slashes.
                if (sb == null && (prevSep || ch == '\\')) {
                    sb = new StringBuilder(len).append(name, 0, i);
                }
                if (sb != null && !prevSep) {
                    sb.append(SEPARATOR_CHAR);
                }
                prevSep = true;
                continue;
            }
            if ((i == 0 || prevSep) && ch == '.') {
                char next = i + 1 < len ? name.charAt(i + 1) : 0;
                if (next == 0 || next == SEPARATOR_CHAR || next == '\\') {
                    // "." segments are dropped.
                    if (sb == null) {
                        sb = new StringBuilder(len).append(name, 0, i);
                    }
                    prevSep = true;
                    i++;
                    continue;
                }
                if (next == '.') {
                    char afterNext = i + 2 < len ? name.charAt(i + 2) : 0;
                    if (afterNext == 0 || afterNext == SEPARATOR_CHAR || afterNext == '\\') {
                        // ".." segments pop the previous segment.
                        if (i == 0 || (sb != null && sb.length() == 0)) {
                            throw new IllegalArgumentException("Name traverses outside the base.");
                        }
                        if (sb == null) {
                            sb = new StringBuilder(len).append(name, 0, i);
                        }
                        int lastSep = -1;
                        for (int j = sb.length() - 2; j >= 0; j--) {
                            if (sb.charAt(j) == SEPARATOR_CHAR) {
                                lastSep = j;
                                break;
                            }
                        }
                        sb.setLength(lastSep + 1);
                        prevSep = true;
                        i += 2;
                        continue;
                    }
                    // fallthrough
                }
                // fallthrough
            }
            if (sb != null) {
                sb.append(ch);
            }
            prevSep = false;
        }
        // If unmodified, only a trailing slash may need trimming.
        if (sb == null) {
            return prevSep ? name.substring(0, len - 1) : name;
        }
        // Otherwise, trim a trailing slash, if present, and return the rebuilt path.
        len = sb.length();
        if (len > 0 && sb.charAt(len - 1) == SEPARATOR_CHAR) {
            sb.setLength(len - 1);
        }
        return sb.toString();
    }

    public static long computeCrc(Path path) throws IOException {
        try (InputStream in = Files.newInputStream(path)) {
            return computeCrc(in);
        }
    }

    public static long computeCrc(InputStream in) throws IOException {
        CRC32 crc = new CRC32();
        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = in.read(buffer)) >= 0) {
            crc.update(buffer, 0, bytesRead);
        }
        return crc.getValue();
    }

    public static long computeCrc(byte[] data) {
        CRC32 crc = new CRC32();
        crc.update(data);
        return crc.getValue();
    }
}
