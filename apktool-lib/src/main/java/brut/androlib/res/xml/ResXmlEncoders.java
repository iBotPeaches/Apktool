/**
 *  Copyright 2011 Ryszard Wiśniewski <brut.alll@gmail.com>
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

import java.awt.event.KeyEvent;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public final class ResXmlEncoders {

    public static String escapeXmlChars(String str) {
        return str.replace("&", "&amp;").replace("<", "&lt;");
    }

    public static String encodeAsResXmlAttr(String str) {
        if (str.isEmpty()) {
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
        if (str.isEmpty()) {
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
        boolean enclose = false;
        boolean wasSpace = true;
        for (char c : chars) {
            if (isInStyleTag) {
                if (c == '>') {
                    isInStyleTag = false;
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
                        break;
                    default:
                        if (!isPrintableChar(c)) {
                            out.append(String.format("\\u%04x", (int) c));
                            continue;
                        }
                }
            }
            out.append(c);
        }

        if (enclose || wasSpace) {
            out.insert(0, '"').append('"');
        }

        return out.toString();
    }

    private static boolean isPrintableChar(char c) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(c);
        return !Character.isISOControl(c)
                && c != KeyEvent.CHAR_UNDEFINED
                && block != null
                && block != Character.UnicodeBlock.SPECIALS;
    }
}
