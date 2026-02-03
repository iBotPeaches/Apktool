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
package brut.androlib.res.data;

import java.util.Arrays;
import java.util.Objects;

public class StyledString implements CharSequence {
    private final String mValue;
    private final Span[] mSpans;

    public StyledString(String value, Span[] spans) {
        assert value != null && spans != null;
        mValue = value;
        mSpans = spans;
    }

    public String getValue() {
        return mValue;
    }

    public Span[] getSpans() {
        return mSpans;
    }

    @Override
    public int length() {
        return mValue.length();
    }

    @Override
    public char charAt(int index) {
        return mValue.charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return mValue.subSequence(start, end);
    }

    @Override
    public String toString() {
        return mValue;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof StyledString) {
            StyledString other = (StyledString) obj;
            return mValue.equals(other.mValue)
                && Arrays.equals(mSpans, other.mSpans);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mValue, Arrays.hashCode(mSpans));
    }

    public static class Span {
        private final String mTag;
        private final int mFirstChar;
        private final int mLastChar;

        public Span(String tag, int firstChar, int lastChar) {
            assert tag != null;
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
    }
}
