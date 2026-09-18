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
package brut.yaml;

import brut.util.TextUtils;

public final class YamlUtils {

    private YamlUtils() {
        // Private constructor for utility class.
    }

    public static String escapeString(String str) {
        if (str == null) {
            return null;
        }
        int len = str.length();
        if (len == 0) {
            return "\"\"";
        }
        StringBuilder sb = new StringBuilder(len * 2);
        boolean quote = false;
        for (int i = 0; i < len; i++) {
            char ch = str.charAt(i);
            switch (ch) {
                case '\0': // Null
                    sb.append("\\0");
                    quote = true;
                    continue;
                case '\u0007': // Bell
                    sb.append("\\a");
                    quote = true;
                    continue;
                case '\b': // Backspace
                    sb.append("\\b");
                    quote = true;
                    continue;
                case '\t': // Character Tabulation
                    sb.append("\\t");
                    quote = true;
                    continue;
                case '\n': // Line Feed
                    sb.append("\\n");
                    quote = true;
                    continue;
                case '\u000B': // Line Tabulation
                    sb.append("\\v");
                    quote = true;
                    continue;
                case '\f': // Form Feed
                    sb.append("\\f");
                    quote = true;
                    continue;
                case '\r': // Carriage Return
                    sb.append("\\r");
                    quote = true;
                    continue;
                case '\u001B': // Escape
                    sb.append("\\e");
                    quote = true;
                    continue;
                case '\u0085': // Next Line
                    sb.append("\\N");
                    quote = true;
                    continue;
                case '\u00A0': // No-Break Space
                    sb.append("\\_");
                    quote = true;
                    continue;
                case '\u2028': // Line Separator
                    sb.append("\\L");
                    quote = true;
                    continue;
                case '\u2029': // Paragraph Separator
                    sb.append("\\P");
                    quote = true;
                    continue;
            }
            if (TextUtils.isPrintableChar(ch)) {
                sb.append(ch);
                continue;
            }
            if (Character.isHighSurrogate(ch) && i + 1 < len) {
                // Is this high surrogate followed by a valid low surrogate?
                char low = str.charAt(i + 1);
                if (Character.isLowSurrogate(low)) {
                    sb.append(ch).append(low);
                    i++;
                    continue;
                }
                // fallthrough
            }
            // Java-style Unicode escape the non-printable character.
            sb.append("\\u")
                .append(Character.forDigit(ch >>> 12, 16))
                .append(Character.forDigit(ch >>> 8 & 0xF, 16))
                .append(Character.forDigit(ch >>> 4 & 0xF, 16))
                .append(Character.forDigit(ch & 0xF, 16));
            quote = true;
        }
        if (quote) {
            str = sb.toString();
            len = str.length();
            sb = new StringBuilder(len * 2);
            sb.append('"');
            for (int i = 0; i < len; i++) {
                char ch = str.charAt(i);
                if (ch == '\\' || ch == '"') {
                    sb.append('\\');
                }
                sb.append(ch);
            }
            sb.append('"');
        }
        return sb.toString();
    }

    public static String unescapeString(String str) {
        if (str == null) {
            return null;
        }
        int len = str.length();
        if (len == 0) {
            return str;
        }
        char quote = str.charAt(0);
        if (quote != '\'' && quote != '"') {
            return str;
        }
        if (len > 1) {
            StringBuilder sb = new StringBuilder(len - 2);
            for (int i = 1; i < len; i++) {
                char ch = str.charAt(i);
                // Double-quoted: \ indicates an escape sequence.
                if (quote == '"' && ch == '\\') {
                    if (++i == len) {
                        throw new IllegalArgumentException("Unterminated escape sequence.");
                    }
                    ch = str.charAt(i);
                    switch (ch) {
                        case ' ':
                            sb.append(' ');
                            continue;
                        case '"':
                            sb.append('"');
                            continue;
                        case '/':
                            sb.append('/');
                            continue;
                        case '\\':
                            sb.append('\\');
                            continue;
                        case '0':
                            sb.append('\0'); // Null
                            continue;
                        case 'a':
                            sb.append('\u0007'); // Bell
                            continue;
                        case 'b':
                            sb.append('\b'); // Backspace
                            continue;
                        case 't':
                            sb.append('\t'); // Character Tabulation
                            continue;
                        case 'n':
                            sb.append('\n'); // Line Feed
                            continue;
                        case 'v':
                            sb.append('\u000B'); // Line Tabulation
                            continue;
                        case 'f':
                            sb.append('\f'); // Form Feed
                            continue;
                        case 'r':
                            sb.append('\r'); // Carriage Return
                            continue;
                        case 'e':
                            sb.append('\u001B'); // Escape
                            continue;
                        case 'N':
                            sb.append('\u0085'); // Next Line
                            continue;
                        case '_':
                            sb.append('\u00A0'); // No-Break Space
                            continue;
                        case 'L':
                            sb.append('\u2028'); // Line Separator
                            continue;
                        case 'P':
                            sb.append('\u2029'); // Paragraph Separator
                            continue;
                        case 'x':
                            i += 2;
                            if (i >= len) {
                                throw new IllegalArgumentException("Invalid \\x escape sequence.");
                            }
                            sb.append((char) parseHex(str, i - 1, 2));
                            continue;
                        case 'u':
                            i += 4;
                            if (i >= len) {
                                throw new IllegalArgumentException("Invalid \\u escape sequence.");
                            }
                            sb.append((char) parseHex(str, i - 3, 4));
                            continue;
                        case 'U':
                            i += 8;
                            if (i >= len) {
                                throw new IllegalArgumentException("Invalid \\U escape sequence.");
                            }
                            int codePoint = parseHex(str, i - 7, 8);
                            if (!Character.isValidCodePoint(codePoint)) {
                                throw new IllegalArgumentException("Invalid Unicode code point.");
                            }
                            sb.appendCodePoint(codePoint);
                            continue;
                    }
                    throw new IllegalArgumentException("Unknown escape sequence: \\" + ch);
                }
                if (ch != quote) {
                    sb.append(ch);
                    continue;
                }
                // Single-quoted: '' represents a literal apostrophe.
                if (quote == '\'' && i + 1 < len && str.charAt(i + 1) == '\'') {
                    sb.append('\'');
                    i++;
                    continue;
                }
                if (i == len - 1) {
                    return sb.toString();
                }
                throw new IllegalArgumentException("Unexpected character after closing quote.");
            }
        }
        throw new IllegalArgumentException("Unterminated quoted string.");
    }

    private static int parseHex(String str, int start, int len) {
        int value = 0;
        for (int i = start, end = start + len; i < end; i++) {
            int digit = Character.digit(str.charAt(i), 16);
            if (digit < 0) {
                throw new IllegalArgumentException("Invalid hexadecimal escape sequence.");
            }
            value = (value << 4) | digit;
        }
        return value;
    }
}
