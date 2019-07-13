/**
 *  Copyright (C) 2019 Ryszard Wiśniewski <brut.alll@gmail.com>
 *  Copyright (C) 2019 Connor Tumbleson <connor.tumbleson@gmail.com>
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
package brut.androlib.res.xml;

import brut.util.Duo;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public final class ResXmlEncoders {

    public static String escapeXmlChars(String str) {
        return StringUtils.replace(StringUtils.replace(str, "&", "&amp;"), "<", "&lt;");
    }

    public static String encodeAsResXmlAttr(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        char[] chars = str.toCharArray();
        StringBuilder out = new StringBuilder(str.length() + 10);

        switch (chars[0]) {
            case '#':
            case '@':
            case '?':
                out.append('\\');
        }

        for (char c : chars) {
            switch (c) {
                case '\\':
                    out.append('\\');
                    break;
                case '"':
                    out.append("&quot;");
                    continue;
                case '\n':
                    out.append("\\n");
                    continue;
                default:
                    if (!isPrintableChar(c)) {
                        out.append(String.format("\\u%04x", (int) c));
                        continue;
                    }
            }
            out.append(c);
        }

        return out.toString();
    }

    public static String encodeAsXmlValue(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        char[] chars = str.toCharArray();
        StringBuilder out = new StringBuilder(str.length() + 10);

        switch (chars[0]) {
            case '#':
            case '@':
            case '?':
                out.append('\\');
        }

        boolean isInStyleTag = false;
        int startPos = 0;
        boolean enclose = false;
        boolean wasSpace = true;
        for (char c : chars) {
            if (isInStyleTag) {
                if (c == '>') {
                    isInStyleTag = false;
                    startPos = out.length() + 1;
                    enclose = false;
                }
            } else if (c == ' ') {
                if (wasSpace) {
                    enclose = true;
                }
                wasSpace = true;
            } else {
                wasSpace = false;
                switch (c) {
                    case '\\':
                        out.append('\\');
                        break;
                    case '\'':
                    case '\n':
                        enclose = true;
                        break;
                    case '"':
                        out.append('\\');
                        break;
                    case '<':
                        isInStyleTag = true;
                        if (enclose) {
                            out.insert(startPos, '"').append('"');
                        }
                        break;
                    default:
                        if (!isPrintableChar(c)) {

                            // lets not write trailing \u0000 if we are at end of string
                            if ((out.length() + 1) == str.length() && c == '\u0000') {
                                continue;
                            }
                            out.append(String.format("\\u%04x", (int) c));
                            continue;
                        }
                }
            }
            out.append(c);
        }

        if (enclose || wasSpace) {
            out.insert(startPos, '"').append('"');
        }
        return out.toString();
    }

    public static boolean hasMultipleNonPositionalSubstitutions(String str) {
        Duo<List<Integer>, List<Integer>> tuple = findSubstitutions(str, 4);
        return ! tuple.m1.isEmpty() && tuple.m1.size() + tuple.m2.size() > 1;
    }

    public static String enumerateNonPositionalSubstitutionsIfRequired(String str) {
        Duo<List<Integer>, List<Integer>> tuple = findSubstitutions(str, 4);
        if (tuple.m1.isEmpty() || tuple.m1.size() + tuple.m2.size() < 2) {
            return str;
        }
        List<Integer> subs = tuple.m1;

        StringBuilder out = new StringBuilder();
        int pos = 0;
        int count = 0;
        for (Integer sub : subs) {
            out.append(str.substring(pos, ++sub)).append(++count).append('$');
            pos = sub;
        }
        out.append(str.substring(pos));

        return out.toString();
    }

    /**
     * It returns a tuple of:
     *   - a list of offsets of non positional substitutions. non-pos is defined as any "%" which isn't "%%" nor "%\d+\$"
     *   - a list of offsets of positional substitutions
     */
    private static Duo<List<Integer>, List<Integer>> findSubstitutions(String str, int nonPosMax) {
        if (nonPosMax == -1) {
            nonPosMax = Integer.MAX_VALUE;
        }
        int pos;
        int pos2 = 0;
        List<Integer> nonPositional = new ArrayList<>();
        List<Integer> positional = new ArrayList<>();

        if (str == null) {
            return new Duo<>(nonPositional, positional);
        }

        int length = str.length();

        while ((pos = str.indexOf('%', pos2)) != -1) {
            pos2 = pos + 1;
            if (pos2 == length) {
                nonPositional.add(pos);
                break;
            }
            char c = str.charAt(pos2++);
            if (c == '%') {
                continue;
            }
            if (c >= '0' && c <= '9' && pos2 < length) {
                while ((c = str.charAt(pos2++)) >= '0' && c <= '9' && pos2 < length);
                if (c == '$') {
                    positional.add(pos);
                    continue;
                }
            }

            nonPositional.add(pos);
            if (nonPositional.size() >= nonPosMax) {
                break;
            }
        }

        return new Duo<>(nonPositional, positional);
    }

    private static boolean isPrintableChar(char c) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(c);
        return !Character.isISOControl(c) && c != KeyEvent.CHAR_UNDEFINED
                && block != null && block != Character.UnicodeBlock.SPECIALS;
    }
}
