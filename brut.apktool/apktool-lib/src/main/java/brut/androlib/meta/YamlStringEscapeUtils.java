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
package brut.androlib.meta;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.text.translate.CharSequenceTranslator;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

public class YamlStringEscapeUtils {

    public static String escapeString(String str) {
        return escapeJavaStyleString(str, false, false);
    }

    /**
     * @param str String to escape values in, may be null
     * @param escapeSingleQuotes escapes single quotes if <code>true</code>
     * @param escapeForwardSlash TODO
     * @return the escaped string
     */
    private static String escapeJavaStyleString(String str, boolean escapeSingleQuotes, boolean escapeForwardSlash) {
        if (str == null) {
            return null;
        }
        try {
            StringWriter writer = new StringWriter(str.length() * 2);
            escapeJavaStyleString(writer, str, escapeSingleQuotes, escapeForwardSlash);
            return writer.toString();
        } catch (IOException ioe) {
            // this should never ever happen while writing to a StringWriter
            throw new RuntimeException(ioe);
        }
    }

    /**
     * @param out write to receieve the escaped string
     * @param str String to escape values in, may be null
     * @param escapeSingleQuote escapes single quotes if <code>true</code>
     * @param escapeForwardSlash TODO
     * @throws IOException if an IOException occurs
     */
    private static void escapeJavaStyleString(Writer out, String str, boolean escapeSingleQuote,
                                              boolean escapeForwardSlash) throws IOException {
        if (out == null) {
            throw new IllegalArgumentException("The Writer must not be null");
        }
        if (str == null) {
            return;
        }
        int sz;
        sz = str.length();
        for (int i = 0; i < sz; i++) {
            char ch = str.charAt(i);
            // "[^\t\n\r\u0020-\u007E\u0085\u00A0-\uD7FF\uE000-\uFFFD]"
            // handle unicode
            if (ch > 0xFFFD) {
                out.write("\\u" + CharSequenceTranslator.hex(ch));
            } else if (ch > 0xD7FF && ch < 0xE000) {
                out.write("\\u" + CharSequenceTranslator.hex(ch));
            } else if (ch > 0x7E && ch != 0x85 && ch < 0xA0) {
                out.write("\\u00" + CharSequenceTranslator.hex(ch));
            } else if (ch < 32) {
                switch (ch) {
                    case '\t' :
                        out.write('\\');
                        out.write('t');
                        break;
                    case '\n' :
                        out.write('\\');
                        out.write('n');
                        break;
                    case '\r' :
                        out.write('\\');
                        out.write('r');
                        break;
                    default :
                        if (ch > 0xf) {
                            out.write("\\u00" + CharSequenceTranslator.hex(ch));
                        } else {
                            out.write("\\u000" + CharSequenceTranslator.hex(ch));
                        }
                        break;
                }
            } else {
                switch (ch) {
                    case '\'' :
                        if (escapeSingleQuote) {
                            out.write('\\');
                        }
                        out.write('\'');
                        break;
                    case '"' :
                        out.write('\\');
                        out.write('"');
                        break;
                    case '\\' :
                        out.write('\\');
                        out.write('\\');
                        break;
                    case '/' :
                        if (escapeForwardSlash) {
                            out.write('\\');
                        }
                        out.write('/');
                        break;
                    default :
                        out.write(ch);
                        break;
                }
            }
        }
    }

    /**
     * <p>Unescapes any Java literals found in the <code>String</code>.
     * For example, it will turn a sequence of <code>'\'</code> and
     * <code>'n'</code> into a newline character, unless the <code>'\'</code>
     * is preceded by another <code>'\'</code>.</p>
     *
     * @param str  the <code>String</code> to unescape, may be null
     * @return a new unescaped <code>String</code>, <code>null</code> if null string input
     */
    public static String unescapeString(String str) {
        return StringEscapeUtils.unescapeJava(str);
    }
}