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
    private static final Logger LOGGER = Logger.getLogger(StyledString.class.getName());

    private final String mText;
    private final List<Span> mSpans;

    public StyledString(String text, List<Span> spans) {
        mText = text;
        mSpans = spans;
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

    public static class Span implements Comparable<Span> {
        private static final MapSplitter ATTRIBUTES_SPLITTER =
            Splitter.on(';').omitEmptyStrings().withKeyValueSeparator(Splitter.on('=').limit(2));

        private final String mTag;
        private final int mFirstChar;
        private final int mLastChar;

        public Span(String tag, int firstChar, int lastChar) {
            mTag = tag;
            mFirstChar = firstChar;
            mLastChar = lastChar;
        }

        public String getTag() {
            return mTag;
        }

        public int getFirstChar() {
            return mFirstChar;
        }

        public int getLastChar() {
            return mLastChar;
        }

        public String getName() {
            int separatorIdx = mTag.indexOf(';');
            return separatorIdx == -1 ? mTag : mTag.substring(0, separatorIdx);
        }

        public Map<String, String> getAttributes() {
            int separatorIdx = mTag.indexOf(';');
            return separatorIdx != -1 ? ATTRIBUTES_SPLITTER.split(
                mTag.substring(separatorIdx + 1, mTag.endsWith(";") ? mTag.length() - 1 : mTag.length())
            ) : null;
        }

        @Override
        public int compareTo(Span other) {
            int res = Integer.compare(mFirstChar, other.mFirstChar);
            if (res != 0) {
                return res;
            }
            res = Integer.compare(mLastChar, other.mLastChar);
            if (res != 0) {
                return -res;
            }
            return -mTag.compareTo(other.mTag);
        }
    }

    private static class Decoder {
        private String mText;
        private StringBuilder mXmlValue;
        private int mLastOffset;

        public String decode(StyledString styledString) {
            mText = styledString.getText();
            mXmlValue = new StringBuilder(mText.length() * 2);
            mLastOffset = 0;

            // recurse top-level tags
            PeekingIterator<Span> it = Iterators.peekingIterator(styledString.getSpans().iterator());
            while (it.hasNext()) {
                decodeIterate(it);
            }

            // write the remaining encoded raw mText
            if (mLastOffset < mText.length()) {
                mXmlValue.append(ResXmlEncoders.escapeXmlChars(mText.substring(mLastOffset)));
            }
            return mXmlValue.toString();
        }

        private void decodeIterate(PeekingIterator<Span> it) {
            Span span = it.next();
            String name = span.getName();
            Map<String, String> attributes = span.getAttributes();
            int spanStart = span.getFirstChar();
            int spanEnd = span.getLastChar() + 1;

            // write encoded raw mText preceding the opening tag
            if (spanStart > mLastOffset) {
                mXmlValue.append(ResXmlEncoders.escapeXmlChars(mText.substring(mLastOffset, spanStart)));
            }
            mLastOffset = spanStart;

            // write opening tag
            mXmlValue.append('<').append(name);
            if (attributes != null) {
                for (Map.Entry<String, String> attrEntry : attributes.entrySet()) {
                    mXmlValue.append(' ').append(attrEntry.getKey()).append("=\"")
                            .append(ResXmlEncoders.escapeXmlChars(attrEntry.getValue())).append('"');
                }
            }
            // if an opening tag is followed by a matching closing tag, write as an empty-element tag
            if (spanStart == spanEnd) {
                mXmlValue.append("/>");
                return;
            }
            mXmlValue.append('>');

            // recurse nested tags
            while (it.hasNext() && it.peek().getFirstChar() < spanEnd) {
                decodeIterate(it);
            }

            // write encoded raw mText preceding the closing tag
            if (spanEnd > mLastOffset && mText.length() >= spanEnd) {
                mXmlValue.append(ResXmlEncoders.escapeXmlChars(mText.substring(mLastOffset, spanEnd)));
            } else if (mText.length() >= mLastOffset && mText.length() < spanEnd) {
                LOGGER.warning("Span (" + name + ") exceeds mText length " + mText.length());
                mXmlValue.append(ResXmlEncoders.escapeXmlChars(mText.substring(mLastOffset)));
            }
            mLastOffset = spanEnd;

            // write closing tag
            mXmlValue.append("</").append(name).append('>');
        }
    }
}
