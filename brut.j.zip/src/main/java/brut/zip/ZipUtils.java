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

import brut.util.SystemUtils;

import java.io.InputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.CRC32;

public final class ZipUtils {
    private static final String[] WINDOWS_RESERVED_NAMES = {
        "CON", "CONIN$", "CONOUT$", "PRN", "AUX", "NUL", "COM", "LPT"
    };
    private static final int WINDOWS_RESERVED_WITH_PORT_INDEX = 6;
    private static final char PUA_BASE = 0xF000;

    private ZipUtils() {}

    public static String normalizeForFs(String name) {
        int len = name.length();
        if (len == 0) {
            throw new IllegalArgumentException("Name is empty.");
        }
        if (isAbsolutePathForFs(name)) {
            throw new IllegalArgumentException("Name represents an absolute path.");
        }
        char pathSep = SystemUtils.IS_OS_WINDOWS ? '\\' : '/';
        StringBuilder sb = null; // lazily initialized
        boolean prevSep = true; // starts as true so leading slashes are dropped
        for (int i = 0; i < len; i++) {
            char ch = name.charAt(i);
            if (ch == 0) {
                throw new IllegalArgumentException("Name contains a null character.");
            }
            if (ch == '/') {
                // Repeated slashes are dropped. Windows: Slashes are rewritten as backslashes.
                if (sb == null && (prevSep || pathSep != '/')) {
                    sb = new StringBuilder(len).append(name, 0, i);
                }
                if (sb != null && !prevSep) {
                    sb.append(pathSep);
                }
                prevSep = true;
                continue;
            }
            if (prevSep && ch == '.') {
                char next;
                if (i + 1 == len || (next = name.charAt(i + 1)) == '/') {
                    // "." segments are dropped.
                    if (sb == null) {
                        sb = new StringBuilder(len).append(name, 0, i);
                    }
                    i++;
                    continue;
                }
                if (next == '.' && (i + 2 == len || name.charAt(i + 2) == '/')) {
                    throw new IllegalArgumentException("Name contains a '..' segment.");
                }
                // fallthrough
            }
            if (SystemUtils.IS_OS_WINDOWS) {
                if (prevSep) {
                    int segEnd = name.indexOf('/', i);
                    if (segEnd == -1) {
                        segEnd = len;
                    }
                    if (isWindowsReservedDeviceName(name, i, segEnd)) {
                        throw new IllegalArgumentException("Name contains a reserved device name.");
                    }
                }
                // Windows: Map reserved characters to the private-use characters that stand in for them.
                if (ch >= PUA_BASE && ch < PUA_BASE + 0x100 && isWindowsReservedChar((char) (ch - PUA_BASE))) {
                    throw new IllegalArgumentException("Name contains a reserved private-use character.");
                }
                if (isWindowsReservedChar(ch)) {
                    if (sb == null) {
                        sb = new StringBuilder(len).append(name, 0, i);
                    }
                    ch = (char) (PUA_BASE + ch);
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
        // Otherwise, trim a trailing slash, if present, and return the rebuilt name.
        len = sb.length();
        if (len > 0 && sb.charAt(len - 1) == pathSep) {
            sb.setLength(--len);
        }
        if (len == 0) {
            throw new IllegalArgumentException("Name is effectively empty.");
        }
        return sb.toString();
    }

    private static boolean isAbsolutePathForFs(String name) {
        char ch = name.charAt(0);
        return ch == '/' || (SystemUtils.IS_OS_WINDOWS && name.length() >= 2 && name.charAt(1) == ':'
            && ((ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z')));
    }

    private static boolean isWindowsReservedDeviceName(String name, int start, int end) {
        if (start < 0 || start > end || end > name.length()) {
            throw new IndexOutOfBoundsException();
        }
        if (start == end) {
            return false;
        }
        for (int i = 0; i < WINDOWS_RESERVED_NAMES.length; i++) {
            String reserved = WINDOWS_RESERVED_NAMES[i];
            int len = reserved.length();
            if (len > end - start) {
                continue;
            }
            int j = 0;
            for (; j < len; j++) {
                char ch = name.charAt(start + j);
                if (ch >= 'a' && ch <= 'z') {
                    ch -= ('a' - 'A');
                }
                if (ch != reserved.charAt(j)) {
                    break;
                }
            }
            if (j != len) {
                continue;
            }
            j += start;
            if (i >= WINDOWS_RESERVED_WITH_PORT_INDEX) {
                if (j == end) {
                    continue;
                }
                char ch = name.charAt(j);
                if (ch < '1' || (ch > '9' && ch != '¹' && ch != '²' && ch != '³')) {
                    continue;
                }
                j++;
            }
            for (;;) {
                if (j == end) {
                    return true;
                }
                char ch = name.charAt(j++);
                if (ch == '.') {
                    return true;
                }
                if (ch != ' ') {
                    break;
                }
            }
        }
        return false;
    }

    private static boolean isWindowsReservedChar(char ch) {
        switch (ch) {
            case '<':
            case '>':
            case ':':
            case '"':
            case '/':
            case '\\':
            case '|':
            case '?':
            case '*':
                return true;
            default:
                return ch < 0x20;
        }
    }

    public static String normalizeForZip(String name) {
        int len = name.length();
        if (len == 0) {
            throw new IllegalArgumentException("Name is empty.");
        }
        if (!SystemUtils.IS_OS_WINDOWS) {
            return name;
        }
        StringBuilder sb = null; // lazily initialized
        for (int i = 0; i < len; i++) {
            char ch = name.charAt(i);
            char mapped = ch;
            if (ch == '\\') {
                mapped = '/';
            } else if (ch >= PUA_BASE && ch < PUA_BASE + 0x100 && isWindowsReservedChar((char) (ch - PUA_BASE))) {
                // Map private-use characters back to the reserved characters they stand in for.
                mapped = (char) (ch - PUA_BASE);
            }
            if (mapped != ch && sb == null) {
                sb = new StringBuilder(len).append(name, 0, i);
            }
            if (sb != null) {
                sb.append(mapped);
            }
        }
        return sb != null ? sb.toString() : name;
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
