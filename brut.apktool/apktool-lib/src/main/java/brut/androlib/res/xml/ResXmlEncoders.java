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
package brut.androlib.res.xml;

import android.util.TypedValue;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public final class ResXmlEncoders {
    private static final Logger LOGGER = Logger.getLogger(ResXmlEncoders.class.getName());

    private ResXmlEncoders() {
        // Private constructor for utility class.
    }

    public static String coerceToString(int type, int data) {
        switch (type) {
            case TypedValue.TYPE_NULL:
                return data == TypedValue.DATA_NULL_EMPTY ? "@empty" : "@null";
            case TypedValue.TYPE_REFERENCE:
            case TypedValue.TYPE_DYNAMIC_REFERENCE:
                return data != 0 ? "@" + data : "@null";
            case TypedValue.TYPE_ATTRIBUTE:
            case TypedValue.TYPE_DYNAMIC_ATTRIBUTE:
                return "?" + data;
            case TypedValue.TYPE_STRING:
                throw new IllegalArgumentException("Unexpected data type: TYPE_STRING");
            case TypedValue.TYPE_FLOAT:
                return Float.toString(Float.intBitsToFloat(data));
            case TypedValue.TYPE_DIMENSION:
                return TypedValue.coerceDimensionToString(data);
            case TypedValue.TYPE_FRACTION:
                return TypedValue.coerceFractionToString(data);
        }

        if (type >= TypedValue.TYPE_FIRST_COLOR_INT && type <= TypedValue.TYPE_LAST_COLOR_INT) {
            switch (type) {
                default:
                case TypedValue.TYPE_INT_COLOR_ARGB8:
                    return String.format("#%08x", data);
                case TypedValue.TYPE_INT_COLOR_RGB8:
                    return String.format("#%06x", data & 0xFFFFFF);
                case TypedValue.TYPE_INT_COLOR_ARGB4:
                    return String.format("#%x%x%x%x",
                        (data >>> 28) & 0xF, (data >>> 20) & 0xF,
                        (data >>> 12) & 0xF, (data >>> 4) & 0xF);
                case TypedValue.TYPE_INT_COLOR_RGB4:
                    return String.format("#%x%x%x",
                        (data >>> 20) & 0xF, (data >>> 12) & 0xF,
                        (data >>> 4) & 0xF);
            }
        }

        if (type >= TypedValue.TYPE_FIRST_INT && type <= TypedValue.TYPE_LAST_INT) {
            switch (type) {
                default:
                case TypedValue.TYPE_INT_DEC:
                    return Integer.toString(data);
                case TypedValue.TYPE_INT_HEX:
                    return String.format("0x%x", data);
                case TypedValue.TYPE_INT_BOOLEAN:
                    return data != 0 ? "true" : "false";
            }
        }

        LOGGER.warning(String.format("Unsupported data type: 0x%02x", type));
        return null;
    }

    public static String encodeAsXmlValue(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        int len = str.length();
        StringBuilder sb = new StringBuilder(len + 10);

        switch (str.charAt(0)) {
            case '#':
            case '@':
            case '?':
                sb.append('\\');
                break;
        }

        boolean isInStyleTag = false;
        int startPos = 0;
        boolean enclose = false;
        boolean wasSpace = true;
        for (int i = 0; i < len; i++) {
            char ch = str.charAt(i);
            if (isInStyleTag) {
                if (ch == '>') {
                    isInStyleTag = false;
                    startPos = sb.length() + 1;
                    enclose = false;
                }
            } else if (ch == ' ') {
                if (wasSpace) {
                    enclose = true;
                }
                wasSpace = true;
            } else {
                wasSpace = false;
                switch (ch) {
                    case '\\':
                    case '"':
                        sb.append('\\');
                        break;
                    case '\'':
                    case '\n':
                        enclose = true;
                        break;
                    case '<':
                        isInStyleTag = true;
                        if (enclose) {
                            sb.insert(startPos, '"').append('"');
                        }
                        break;
                    default:
                        if (isPrintableChar(ch)) {
                            break;
                        }
                        // Skip writing trailing \u0000 if we are at end of string.
                        if ((sb.length() + 1) == len && ch == '\u0000') {
                            continue;
                        }
                        sb.append(String.format("\\u%04x", (int) ch));
                        continue;
                }
            }
            sb.append(ch);
        }

        if (enclose || wasSpace) {
            sb.insert(startPos, '"').append('"');
        }
        return sb.toString();
    }

    public static String encodeAsResXmlAttrValue(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        int len = str.length();
        StringBuilder sb = new StringBuilder(len + 10);

        switch (str.charAt(0)) {
            case '#':
            case '@':
            case '?':
                sb.append('\\');
                break;
        }

        for (int i = 0; i < len; i++) {
            char ch = str.charAt(i);
            switch (ch) {
                case '\\':
                    sb.append('\\');
                    break;
                case '"':
                    sb.append("&quot;");
                    continue;
                case '\n':
                    sb.append("\\n");
                    continue;
                default:
                    if (isPrintableChar(ch)) {
                        break;
                    }
                    sb.append(String.format("\\u%04x", (int) ch));
                    continue;
            }
            sb.append(ch);
        }

        return sb.toString();
    }

    public static String escapeXmlChars(String str) {
        return StringUtils.replaceEach(str,
            new String[] { "&", "<", "]]>" },
            new String[] { "&amp;", "&lt;", "]]&gt;" });
    }

    public static boolean hasMultipleNonPositionalSubstitutions(String str) {
        Pair<List<Integer>, List<Integer>> subs = findSubstitutions(str, 4);
        List<Integer> nonPositional = subs.getLeft();
        List<Integer> positional = subs.getRight();
        return !nonPositional.isEmpty() && nonPositional.size() + positional.size() > 1;
    }

    public static String enumerateNonPositionalSubstitutionsIfRequired(String str) {
        Pair<List<Integer>, List<Integer>> subs = findSubstitutions(str, 4);
        List<Integer> nonPositional = subs.getLeft();
        List<Integer> positional = subs.getRight();
        if (nonPositional.isEmpty() || nonPositional.size() + positional.size() < 2) {
            return str;
        }

        StringBuilder sb = new StringBuilder();
        int pos = 0;
        int count = 0;
        for (int pos2 : nonPositional) {
            sb.append(str, pos, ++pos2).append(++count).append('$');
            pos = pos2;
        }
        sb.append(str.substring(pos));

        return sb.toString();
    }

    /**
     * Returns a pair of:
     * - A list of offsets of non-positional substitutions.
     *   non-positional is defined as any "%" which isn't "%%" nor "%\d+\$".
     * - A list of offsets of positional substitutions.
     */
    private static Pair<List<Integer>, List<Integer>> findSubstitutions(String str, int nonPosMax) {
        if (nonPosMax == -1) {
            nonPosMax = Integer.MAX_VALUE;
        }

        List<Integer> nonPositional = new ArrayList<>();
        List<Integer> positional = new ArrayList<>();
        if (str == null) {
            return Pair.of(nonPositional, positional);
        }

        int len = str.length();
        int pos, pos2 = 0;
        while ((pos = str.indexOf('%', pos2)) != -1) {
            pos2 = pos + 1;
            if (pos2 == len) {
                nonPositional.add(pos);
                break;
            }
            char ch = str.charAt(pos2++);
            if (ch == '%') {
                continue;
            }
            if (ch >= '0' && ch <= '9' && pos2 < len) {
                while ((ch = str.charAt(pos2++)) >= '0' && ch <= '9' && pos2 < len);
                if (ch == '$') {
                    positional.add(pos);
                    continue;
                }
            }

            nonPositional.add(pos);
            if (nonPositional.size() >= nonPosMax) {
                break;
            }
        }

        return Pair.of(nonPositional, positional);
    }

    private static boolean isPrintableChar(char ch) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(ch);
        return !Character.isISOControl(ch) && ch != KeyEvent.CHAR_UNDEFINED
                && block != null && block != Character.UnicodeBlock.SPECIALS;
    }
}
