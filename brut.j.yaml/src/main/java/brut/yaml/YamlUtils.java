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

    public static String encodeString(String str) {
        if (str == null) {
            return null;
        }
        int len = str.length();
        if (len == 0) {
            return "''";
        }
        // If the string starts with a quote or has leading/trailing whitespace, it must be quoted.
        char ch = str.charAt(0);
        char quote = (ch == '\'' || ch == '"' || Character.isWhitespace(ch)
            || (len > 1 && Character.isWhitespace(str.charAt(len - 1)))) ? '\'' : 0;
        // Scan for characters that force double-quoting.
        for (int i = 0; i < len; i++) {
            ch = str.charAt(i);
            // No-Break Space, Line Separator, and Paragraph Separator are printable but must be escaped.
            if (ch == '\u00A0' || ch == '\u2028' || ch == '\u2029') {
                quote = '"';
                break;
            }
            if (TextUtils.isPrintableChar(ch)) {
                continue;
            }
            // Is this a high surrogate followed by a valid low surrogate?
            if (Character.isHighSurrogate(ch) && i + 1 < len && Character.isLowSurrogate(str.charAt(i + 1))) {
                i++;
                continue;
            }
            // A non-printable character must be escaped.
            quote = '"';
            break;
        }
        if (quote == '\'') {
            // Single-quote: escape ' by doubling it.
            StringBuilder sb = new StringBuilder(len * 2 + 2);
            sb.append('\'');
            for (int i = 0; i < len; i++) {
                ch = str.charAt(i);
                if (ch == '\'') {
                    sb.append("''");
                } else {
                    sb.append(ch);
                }
            }
            sb.append('\'');
            str = sb.toString();
        } else if (quote == '"') {
            // Double-quote: escape \, ", and non-printable characters.
            StringBuilder sb = new StringBuilder(len * 2 + 2);
            sb.append('"');
            for (int i = 0; i < len; i++) {
                ch = str.charAt(i);
                switch (ch) {
                    case '\\':
                        sb.append("\\\\");
                        continue;
                    case '"':
                        sb.append("\\\"");
                        continue;
                    case '\0': // Null
                        sb.append("\\0");
                        continue;
                    case '\u0007': // Bell
                        sb.append("\\a");
                        continue;
                    case '\b': // Backspace
                        sb.append("\\b");
                        continue;
                    case '\t': // Character Tabulation
                        sb.append("\\t");
                        continue;
                    case '\n': // Line Feed
                        sb.append("\\n");
                        continue;
                    case '\u000B': // Line Tabulation
                        sb.append("\\v");
                        continue;
                    case '\f': // Form Feed
                        sb.append("\\f");
                        continue;
                    case '\r': // Carriage Return
                        sb.append("\\r");
                        continue;
                    case '\u001B': // Escape
                        sb.append("\\e");
                        continue;
                    case '\u0085': // Next Line
                        sb.append("\\N");
                        continue;
                    case '\u00A0': // No-Break Space
                        sb.append("\\_");
                        continue;
                    case '\u2028': // Line Separator
                        sb.append("\\L");
                        continue;
                    case '\u2029': // Paragraph Separator
                        sb.append("\\P");
                        continue;
                }
                if (TextUtils.isPrintableChar(ch)) {
                    sb.append(ch);
                    continue;
                }
                // Is this a high surrogate followed by a valid low surrogate?
                if (Character.isHighSurrogate(ch) && i + 1 < len) {
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
            }
            sb.append('"');
            str = sb.toString();
        }
        return str;
    }

    public static String decodeString(String str) {
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
