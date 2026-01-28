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

import brut.androlib.res.data.StyledString;
import brut.androlib.res.table.value.ResAttribute;
import brut.util.TextUtils;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.regex.Pattern;

public final class ResStringEncoder {
    private static final Pattern TAG_SPLIT_PATTERN = Pattern.compile(
        ";(?=[\\p{L}_][\\p{L}\\p{N}_.-]*=)");

    private ResStringEncoder() {
        // Private constructor for utility class.
    }

    public static String encodeTextValue(CharSequence text) {
        return text instanceof StyledString
            ? encodeStyledString((StyledString) text)
            : encodeRawString(text.toString(), 0);
    }

    public static String encodeAttributeValue(CharSequence text) {
        return encodeAttributeValue(text, ResAttribute.ATTR_TYPE_ANY);
    }

    public static String encodeAttributeValue(CharSequence text, int attrType) {
        return encodeRawString(text.toString(), attrType);
    }

    private static String encodeStyledString(StyledString styledStr) {
        String str = styledStr.getValue();
        StyledString.Span[] spans = styledStr.getSpans();
        int len = str.length();
        if (len == 0 && spans.length == 0) {
            return "";
        }

        StringBuilder out = new StringBuilder(len * 2);
        Deque<StyledString.Span> stack = new ArrayDeque<>();

        int offset = 0;
        for (int i = 0; i <= spans.length; i++) {
            int prevOffset = offset;
            StyledString.Span span;
            if (i < spans.length) {
                span = spans[i];
                offset = span.getFirstChar();
            } else {
                span = null;
                offset = len;
            }

            // Close nested spans that end before the current offset.
            while (!stack.isEmpty() && stack.peek().getLastChar() < offset) {
                StyledString.Span prevSpan = stack.pop();
                int prevSpanEnd = prevSpan.getLastChar() + 1;

                // Flush any remaining text inside this span.
                if (prevOffset < prevSpanEnd) {
                    appendEscapedString(out, str, prevOffset, prevSpanEnd, 0, true);
                    prevOffset = prevSpanEnd;
                }

                // Write the closing tag.
                String prevTag = prevSpan.getTag();
                int prevTagEnd = prevTag.indexOf(';');
                if (prevTagEnd == -1) {
                    prevTagEnd = prevTag.length();
                }
                out.append("</").append(prevTag, 0, prevTagEnd).append('>');
            }

            // Ignore spans that start beyond nested spans.
            if (prevOffset > offset) {
                continue;
            }

            // Flush any text between tags.
            if (prevOffset < offset) {
                appendEscapedString(out, str, prevOffset, offset, 0, true);
            }

            // Break if all spans have been handled.
            if (span == null) {
                break;
            }

            // Start current span.
            int spanEnd = span.getLastChar() + 1;
            String tag = span.getTag();
            int tagEnd = tag.indexOf(';');
            if (tagEnd == -1) {
                tagEnd = tag.length();
            }
            // Ignore this span if the tag is missing.
            if (tagEnd == 0) {
                continue;
            }

            // Write the opening tag.
            out.append('<').append(tag, 0, tagEnd);

            // Append the attributes.
            if (tagEnd < tag.length()) {
                for (String attr : TAG_SPLIT_PATTERN.split(tag.substring(tagEnd + 1))) {
                    int attrLen = attr.length();
                    if (attrLen == 0) {
                        continue;
                    }
                    int nameEnd = attr.indexOf('=');
                    if (nameEnd == -1) {
                        continue;
                    }
                    int valueStart = nameEnd + 1;
                    out.append(' ').append(attr, 0, valueStart).append('"');
                    if (valueStart < attrLen) {
                        appendTagAttributeValue(out, attr, valueStart, attrLen);
                    }
                    out.append('"');
                }
            }

            // Write as a self-closing tag if needed.
            if (offset == spanEnd) {
                out.append("/>");
                continue;
            }

            // Push onto the stack for inner text and possible nested spans.
            out.append('>');
            stack.push(span);
        }

        return out.toString();
    }

    private static String encodeRawString(String str, int attrType) {
        int len = str.length();
        if (len == 0) {
            return "";
        }

        StringBuilder out = new StringBuilder(len * 2);
        appendEscapedString(out, str, 0, len, attrType, false);

        // Raw strings might get encoded as typed values in edge cases.
        // We skip this if the string has been quoted.
        if (out.charAt(0) != '"' && isAmbiguousString(out, attrType)) {
            out.insert(0, '\\');
        }

        return out.toString();
    }

    private static void appendEscapedString(StringBuilder out, String str, int start, int end,
                                            int attrType, boolean styled) {
        int len = str.length();
        int offset = out.length();
        boolean quote = false;
        char ch = 0, prev = 0, prev2 = 0;
        for (int i = start; i < end; i++, prev2 = prev, prev = ch) {
            ch = str.charAt(i);
            if (ch == '\n') {
                if (attrType != 0) {
                    out.append("\\n");
                } else {
                    out.append(ch);
                    quote = true;
                }
                continue;
            } else if (ch == '\t') {
                out.append("\\t");
                continue;
            } else if (TextUtils.isPrintableChar(ch)) {
                if (ch == '\\') {
                    out.append('\\');
                    // fallthrough
                } else if (attrType == 0) {
                    // The following are used for values XMLs only. The serializer will handle
                    // attribute values.
                    if (ch == ' ') {
                        // Normal strings collapse whitespace and trim both ends, while styled
                        // strings only collapse whitespace.
                        if (prev == ' ' || (!styled && (i == 0 || i == len - 1))) {
                            quote = true;
                        }
                        // fallthrough
                    } else if (ch == '\'') {
                        quote = true;
                        // fallthrough
                    } else if (ch == '"') {
                        out.append('\\');
                        // fallthrough
                    } else if (ch == '&') {
                        out.append("&amp;");
                        continue;
                    } else if (ch == '<') {
                        out.append("&lt;");
                        continue;
                    } else if (ch == '>' && prev == ']' && prev2 == ']') {
                        out.append("&gt;");
                        continue;
                    }
                }
                out.append(ch);
                continue;
            } else if (Character.isHighSurrogate(ch) && i < end - 1) {
                // Is this high surrogate followed by a valid low surrogate?
                char low = str.charAt(i + 1);
                if (Character.isLowSurrogate(low)) {
                    out.append(ch);
                    out.append(low);
                    i++;
                    continue;
                }
            }
            // Skip writing trailing \u0000 if we are at end of string.
            if (ch == 0 && i == len - 1) {
                break;
            }
            // Java-style Unicode escape the non-printable character.
            out.append(String.format("\\u%04x", (int) ch));
        }
        if (quote) {
            out.insert(offset, '"').append('"');
        }
    }

    private static boolean isAmbiguousString(CharSequence text, int attrType) {
        int len = text.length();
        char ch = text.charAt(0);

        // Check for a reference.
        // Note: We don't check attribute type here because a reference is valid for any type.
        if (ch == '@') {
            if (len == 5) {
                if (text.charAt(1) == 'n' && text.charAt(2) == 'u' && text.charAt(3) == 'l'
                        && text.charAt(4) == 'l') {
                    return true;
                }
            } else if (len == 6) {
                if (text.charAt(1) == 'e' && text.charAt(2) == 'm' && text.charAt(3) == 'p'
                        && text.charAt(4) == 't' && text.charAt(5) == 'y') {
                    return true;
                }
            }
            for (int i = 1; i < len; i++) {
                ch = text.charAt(i);
                if (ch == '/') {
                    return true;
                }
            }
            return false;
        }
        if (ch == '?') {
            return len > 1;
        }

        // The following can only be ambiguous in attribute values.
        // Note: We can't escape a boolean in attribute values since \t is a tab.
        if (attrType == 0) {
            return false;
        }

        // Check for a color.
        if (ch == '#') {
            if ((attrType & ResAttribute.ATTR_TYPE_COLOR) != 0) {
                try {
                    TextUtils.parseColor(text, 0, len);
                    return true;
                } catch (NumberFormatException ignored) {
                }
            }
            return false;
        }

        // Check for an integer.
        if ((attrType & ResAttribute.ATTR_TYPE_INTEGER) != 0) {
            try {
                TextUtils.parseInt(text, 0, len);
                return true;
            } catch (NumberFormatException ignored) {
            }
        }

        // Check for a float or a complex value.
        boolean checkFloat = (attrType & ResAttribute.ATTR_TYPE_FLOAT) != 0;
        boolean checkDimen = (attrType & ResAttribute.ATTR_TYPE_DIMENSION) != 0;
        boolean checkFraction = (attrType & ResAttribute.ATTR_TYPE_FRACTION) != 0;
        if (checkFloat || checkDimen || checkFraction) {
            int suffixLen = 0;
            if (checkDimen) {
                String suffix = TextUtils.matchSuffix(
                    text, "px", "dp", "dip", "sp", "pt", "in", "mm");
                if (suffix != null) {
                    suffixLen = suffix.length();
                }
            }
            if (checkFraction && suffixLen == 0) {
                String suffix = TextUtils.matchSuffix(text, "%", "%p");
                if (suffix != null) {
                    suffixLen = suffix.length();
                }
            }
            if ((checkFloat && suffixLen == 0) || suffixLen > 0) {
                try {
                    TextUtils.parseFloat(text, 0, len - suffixLen);
                    return true;
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return false;
    }

    private static void appendTagAttributeValue(StringBuilder out, String str, int start, int end) {
        char ch = 0, prev = 0, prev2 = 0;
        for (int i = start; i < end; i++, prev2 = prev, prev = ch) {
            ch = str.charAt(i);
            if (ch == '\n') {
                out.append("&#xA;");
            } else if (ch == '\r') {
                out.append("&#xD;");
            } else if (ch == '\t') {
                out.append("&#x9;");
            } else if (ch == '"') {
                out.append("&quot;");
            } else if (ch == '&') {
                out.append("&amp;");
            } else if (ch == '<') {
                out.append("&lt;");
            } else if (ch == '>' && prev == ']' && prev2 == ']') {
                out.append("&gt;");
            } else {
                out.append(ch);
            }
        }
    }

    public static String normalizeFormatSpecifiers(String str) {
        int len = str.length();
        if (len == 0) {
            return str;
        }

        int[][] specs = findFormatSpecifiers(str);
        int[] seq = specs[0];
        int[] pos = specs[1];
        if (seq.length == 0 || seq.length + pos.length < 2) {
            return str;
        }

        StringBuilder out = new StringBuilder(len + seq.length * 2);
        int i = 0;
        int count = 0;
        for (int j : seq) {
            out.append(str, i, ++j).append(++count).append('$');
            i = j;
        }

        out.append(str, i, len);
        return out.toString();
    }

    /**
     * Returns a pair of:
     * 1. An array of offsets of sequential format specifiers.
     * (Sequential is any "%" which is neither "%%" nor "%\d+\$")
     * 2. An array of offsets of positional format specifiers.
     */
    public static int[][] findFormatSpecifiers(String str) {
        int[] sequential = new int[4];
        int sequentialCount = 0;
        int[] positional = new int[4];
        int positionalCount = 0;

        int len = str.length();
        int i, j = 0;
        while ((i = str.indexOf('%', j)) != -1) {
            j = i + 1;
            if (j == len) {
                if (sequentialCount == sequential.length) {
                    sequential = Arrays.copyOf(sequential, sequential.length + 4);
                }
                sequential[sequentialCount++] = i - 1;
                break;
            }

            char ch = str.charAt(j++);
            if (ch == '%') {
                continue;
            }
            if (ch >= '0' && ch <= '9' && j < len) {
                while ((ch = str.charAt(j++)) >= '0' && ch <= '9' && j < len);
                if (ch == '$') {
                    if (positionalCount == positional.length) {
                        positional = Arrays.copyOf(positional, positional.length + 4);
                    }
                    positional[positionalCount++] = i;
                    continue;
                }
            }

            if (sequentialCount == sequential.length) {
                sequential = Arrays.copyOf(sequential, sequential.length + 4);
            }
            sequential[sequentialCount++] = i;
        }

        return new int[][] {
            Arrays.copyOf(sequential, sequentialCount),
            Arrays.copyOf(positional, positionalCount)
        };
    }
}
