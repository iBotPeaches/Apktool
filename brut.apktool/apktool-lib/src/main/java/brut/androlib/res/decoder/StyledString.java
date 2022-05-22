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
package brut.androlib.res.decoder;

import brut.androlib.res.xml.ResXmlEncoders;
import com.google.common.base.Splitter;
import com.google.common.base.Splitter.MapSplitter;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class StyledString {
    private final String mText;
    private final List<Span> mSpans;

    public StyledString(String text, List<Span> spans) {
        this.mText = text;
        this.mSpans = spans;
    }

    String getText() {
        return mText;
    }

    List<Span> getSpans() {
        return mSpans;
    }

    @Override
    public String toString() {
        return new Decoder().decode(this);
    }

    public static class Span {
        private static final MapSplitter ATTRIBUTES_SPLITTER =
            Splitter.on(';').withKeyValueSeparator(Splitter.on('=').limit(2));

        private final String tag;
        private final int firstChar;
        private final int lastChar;

        Span(String tag, int firstChar, int lastChar) {
            this.tag = tag;
            this.firstChar = firstChar;
            this.lastChar = lastChar;
        }

        String getTag() {
            return tag;
        }

        int getFirstChar() {
            return firstChar;
        }

        int getLastChar() {
            return lastChar;
        }

        String getName() {
            int separatorIdx = tag.indexOf(';');
            return separatorIdx == -1 ? tag : tag.substring(0, separatorIdx);
        }

        Map<String, String> getAttributes() {
            int separatorIdx = tag.indexOf(';');
            return separatorIdx == -1 ? null : ATTRIBUTES_SPLITTER.split(
                    tag.substring(separatorIdx + 1, tag.endsWith(";") ? tag.length() - 1 : tag.length()));
        }
    }

    private static class Decoder {
        private String text;
        private StringBuilder xmlValue;
        private int lastOffset;

        String decode(StyledString styledString) {
            text = styledString.getText();
            xmlValue = new StringBuilder(text.length() * 2);
            lastOffset = 0;

            // recurse top-level tags
            PeekingIterator<Span> it = Iterators.peekingIterator(styledString.getSpans().iterator());
            while (it.hasNext()) {
                decodeIterate(it);
            }

            // write the remaining encoded raw text
            if (lastOffset < text.length()) {
                xmlValue.append(ResXmlEncoders.escapeXmlChars(text.substring(lastOffset)));
            }
            return xmlValue.toString();
        }

        private void decodeIterate(PeekingIterator<Span> it) {
            Span span = it.next();
            int spanStart = span.getFirstChar();
            int spanEnd = span.getLastChar() + 1;
            Map<String, String> attributes = span.getAttributes();

            // write encoded raw text preceding the opening tag
            if (spanStart > lastOffset) {
                xmlValue.append(ResXmlEncoders.escapeXmlChars(text.substring(lastOffset, spanStart)));
            }
            lastOffset = spanStart;

            // write opening tag
            xmlValue.append('<');
            xmlValue.append(span.getName());
            if (attributes != null) {
                for (Map.Entry<String, String> attrEntry : attributes.entrySet()) {
                    xmlValue.append(' ');
                    xmlValue.append(attrEntry.getKey());
                    xmlValue.append("=\"");
                    xmlValue.append(ResXmlEncoders.escapeXmlChars(attrEntry.getValue()));
                    xmlValue.append('"');
                }
            }
            // if an opening tag is followed by a matching closing tag, write as an empty-element tag
            if (spanStart == spanEnd) {
                xmlValue.append("/>");
                return;
            }
            xmlValue.append('>');

            // recurse nested tags
            while (it.hasNext() && it.peek().getFirstChar() < spanEnd) {
                decodeIterate(it);
            }

            // write encoded raw text preceding the closing tag
            if (spanEnd > lastOffset) {
                xmlValue.append(ResXmlEncoders.escapeXmlChars(text.substring(lastOffset, spanEnd)));
            }
            lastOffset = spanEnd;

            // write closing tag
            xmlValue.append("</");
            xmlValue.append(span.getName());
            xmlValue.append('>');
        }
    }

    private static final Logger LOGGER = Logger.getLogger(StyledString.class.getName());
}
