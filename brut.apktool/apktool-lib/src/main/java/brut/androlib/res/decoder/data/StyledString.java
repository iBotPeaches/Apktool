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
package brut.androlib.res.decoder.data;

import brut.androlib.res.xml.ResXmlEncoders;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class StyledString implements CharSequence {
    private static final Logger LOGGER = Logger.getLogger(StyledString.class.getName());

    private final String mText;
    private final List<Span> mSpans;

    private StringBuilder mBuffer;
    private int mLastOffset;
    private String mDecodedText;

    public StyledString(String text, List<Span> spans) {
        mText = text;
        mSpans = spans;
    }

    @Override
    public char charAt(int index) {
        return decode().charAt(index);
    }

    @Override
    public int length() {
        return decode().length();
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return decode().subSequence(start, end);
    }

    @Override
    public String toString() {
        return decode();
    }

    private String decode() {
        if (mDecodedText != null) {
            return mDecodedText;
        }

        int len = mText.length();
        mBuffer = new StringBuilder(len * 2);
        mLastOffset = 0;

        // Recurse top-level tags.
        PeekingIterator<Span> it = Iterators.peekingIterator(mSpans.iterator());
        while (it.hasNext()) {
            decodeIterate(it);
        }

        // Write the remaining encoded raw text.
        if (mLastOffset < len) {
            mBuffer.append(ResXmlEncoders.escapeXmlChars(mText.substring(mLastOffset)));
        }

        return mDecodedText = mBuffer.toString();
    }

    private void decodeIterate(PeekingIterator<Span> it) {
        Span span = it.next();
        String name = span.getName();
        Map<String, String> attributes = span.getAttributes();
        int spanStart = span.getFirstChar();
        int spanEnd = span.getLastChar() + 1;

        // Write encoded raw text preceding the opening tag.
        if (spanStart > mLastOffset) {
            mBuffer.append(ResXmlEncoders.escapeXmlChars(mText.substring(mLastOffset, spanStart)));
        }
        mLastOffset = spanStart;

        // Write opening tag.
        mBuffer.append('<').append(name);
        if (attributes != null) {
            for (Map.Entry<String, String> entry : attributes.entrySet()) {
                mBuffer.append(' ').append(entry.getKey()).append("=\"")
                       .append(ResXmlEncoders.escapeXmlChars(entry.getValue()))
                       .append('"');
            }
        }
        // If an opening tag is followed by a matching closing tag, write as an empty-element tag.
        if (spanStart == spanEnd) {
            mBuffer.append("/>");
            return;
        }
        mBuffer.append('>');

        // Recurse nested tags.
        while (it.hasNext() && it.peek().getFirstChar() < spanEnd) {
            decodeIterate(it);
        }

        // Write encoded raw text preceding the closing tag.
        int len = mText.length();
        if (spanEnd > mLastOffset && len >= spanEnd) {
            mBuffer.append(ResXmlEncoders.escapeXmlChars(mText.substring(mLastOffset, spanEnd)));
        } else if (len >= mLastOffset && len < spanEnd) {
            LOGGER.warning("Span (" + name + ") exceeds text length " + len);
            mBuffer.append(ResXmlEncoders.escapeXmlChars(mText.substring(mLastOffset)));
        }
        mLastOffset = spanEnd;

        // Write closing tag.
        mBuffer.append("</").append(name).append('>');
    }

    public static class Span {
        private static final Splitter.MapSplitter ATTRIBUTES_SPLITTER =
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
    }
}
