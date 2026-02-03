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

import brut.androlib.res.decoder.ResChunkPullParser;
import brut.common.Log;
import brut.util.BinaryDataInputStream;
import com.google.common.annotations.VisibleForTesting;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class ResStringPool {
    private static final String TAG = ResStringPool.class.getName();

    private static final CharsetDecoder UTF16LE_DECODER = StandardCharsets.UTF_16LE.newDecoder();
    private static final CharsetDecoder UTF8_DECODER = StandardCharsets.UTF_8.newDecoder();
    private static final CharsetDecoder CESU8_DECODER = Charset.forName("CESU8").newDecoder();

    private static final int UTF8_FLAG = 0x00000100;
    private static final int HEADER_SIZE = 28;

    private final int[] mStringOffsets;
    private final byte[] mStrings;
    private final int[] mStyleOffsets;
    private final int[] mStyles;
    private final boolean mIsUtf8;

    private ResStringPool(int[] stringOffsets, byte[] strings, int[] styleOffsets, int[] styles, boolean isUtf8) {
        mStringOffsets = stringOffsets;
        mStrings = strings;
        mStyleOffsets = styleOffsets;
        mStyles = styles;
        mIsUtf8 = isUtf8;
    }

    @VisibleForTesting
    ResStringPool(byte[] strings, boolean isUtf8) {
        this(new int[0], strings, new int[0], new int[0], isUtf8);
    }

    public static ResStringPool parse(ResChunkPullParser parser) throws IOException {
        BinaryDataInputStream in = parser.stream();
        // ResStringPool_header
        int stringCount = in.readInt();
        int styleCount = in.readInt();
        int flags = in.readInt();
        int stringsOffset = in.readInt();
        int stylesOffset = in.readInt();

        // For some apps they pack the chunk header with more unused data at end.
        int skipped = parser.skipHeader();
        if (skipped > 0) {
            Log.d(TAG, "Skipped unknown %s bytes at end of %s chunk header.", skipped, parser.chunkName());
        }

        int[] stringOffsets = readIntArraySafe(in, stringCount, parser.chunkStart() + stringsOffset);
        int[] styleOffsets = readIntArraySafe(in, styleCount, parser.chunkStart() + stylesOffset);

        // If we have both strings and even just a lying style offset - let's calculate the size of the strings without
        // accidentally parsing all the styles.
        int size = parser.chunkSize() - stringsOffset;
        if (styleCount > 0) {
            size = stylesOffset - stringsOffset;
        }

        byte[] strings = in.readBytes(size);

        // #3236 - Some apps give a styles offset, but have 0 styles. Make this check more robust.
        int[] styles;
        if (stylesOffset > 0 && styleCount > 0) {
            size = parser.chunkSize() - stylesOffset;
            styles = in.readIntArray(size / 4);
        } else {
            styles = new int[0];
        }

        // In case we aren't 4 byte aligned we need to skip the padding bytes.
        in.skipBytes(size % 4);

        boolean isUtf8 = (flags & UTF8_FLAG) != 0;

        return new ResStringPool(stringOffsets, strings, styleOffsets, styles, isUtf8);
    }

    private static int[] readIntArraySafe(BinaryDataInputStream in, int len, long maxPosition) throws IOException {
        int[] arr = new int[len];
        for (int i = 0; i < len; i++) {
            // #3236 - Some apps have more strings than can fit into the block. This function takes an expected max
            // position and if we are past it, we return early during processing.
            if (in.position() >= maxPosition) {
                Log.d(TAG, "Bad string block: string entry is at %s, past end at %s", in.position(), maxPosition);
                return arr;
            }

            arr[i] = in.readInt();
        }
        return arr;
    }

    public CharSequence getText(int index) {
        String string = getString(index);
        if (string == null) {
            return null;
        }

        // Return the raw string if it has no styles.
        int[] style = getStyle(index);
        if (style == null) {
            return string;
        }

        // Convert the styles to spans.
        int len = string.length();
        StyledString.Span[] spans = new StyledString.Span[style.length / 3];
        int spansCount = 0;

        for (int i = 0; i < style.length; i += 3) {
            String tag = getString(style[i]);
            int firstChar = style[i + 1];
            int lastChar = style[i + 2];

            // Ignore the style if it's not in range.
            if (firstChar < 0 || firstChar > len || lastChar > len) {
                continue;
            }

            spans[spansCount++] = new StyledString.Span(tag, firstChar, lastChar);
        }

        if (spansCount < spans.length) {
            spans = Arrays.copyOf(spans, spansCount);
        }

        return new StyledString(string, spans);
    }

    public String getString(int index) {
        if (index < 0 || index >= mStringOffsets.length || mStrings.length == 0) {
            return null;
        }

        int offset = mStringOffsets[index];
        int[] val;
        if (mIsUtf8) {
            val = getUtf8(mStrings, offset);
            offset = val[0];
        } else {
            val = getUtf16(mStrings, offset);
            offset += val[0];
        }

        int length = val[1];
        return decodeString(offset, length);
    }

    @VisibleForTesting
    String decodeString(int offset, int length) {
        try {
            ByteBuffer buffer = ByteBuffer.wrap(mStrings, offset, length);
            return (mIsUtf8 ? UTF8_DECODER : UTF16LE_DECODER).decode(buffer).toString();
        } catch (CharacterCodingException ignored) {
            if (!mIsUtf8) {
                Log.w(TAG, "Failed to decode a string at offset %s of length %s", offset, length);
                return null;
            }
        } catch (IndexOutOfBoundsException ignored) {
            if (!mIsUtf8) {
                Log.w(TAG, "String extends outside of pool at %s of length %s", offset, length);
                return null;
            }
        }

        // In some cases, Android uses 3-byte UTF-8 sequences instead of 4-bytes.
        // If decoding failed, we try to use CESU-8 decoder, which is closer to what Android
        // actually uses.
        try {
            ByteBuffer buffer = ByteBuffer.wrap(mStrings, offset, length);
            return CESU8_DECODER.decode(buffer).toString();
        } catch (CharacterCodingException ignored) {
            Log.w(TAG, "Failed to decode a string with CESU-8 decoder.");
            return null;
        }
    }

    public int findString(String string) {
        if (string == null || mStringOffsets.length == 0 || mStrings.length == 0) {
            return -1;
        }

        int len = string.length();
        for (int i = 0; i < mStringOffsets.length; i++) {
            int offset = mStringOffsets[i];
            int length = getShort(mStrings, offset);
            if (length != len) {
                continue;
            }

            int j = 0;
            for (; j < length; j++) {
                offset += 2;
                if (string.charAt(j) != getShort(mStrings, offset)) {
                    break;
                }
            }
            if (j == length) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Returns style information as an array of int triplets:
     * First int is index of tag name ('b','i', etc.).
     * Second int is tag start index in string.
     * Third int is tag end index in string.
     */
    private int[] getStyle(int index) {
        if (index < 0 || index >= mStyleOffsets.length || mStyles.length == 0) {
            return null;
        }

        // Make sure not to count a partial triplet.
        int offset = mStyleOffsets[index] / 4;
        int count = 0;
        for (int i = offset; i + 2 < mStyles.length; i += 3) {
            if (mStyles[i] < 0) {
                break;
            }
            count++;
        }
        if (count == 0) {
            return null;
        }

        int[] style = new int[count * 3];
        System.arraycopy(mStyles, offset, style, 0, style.length);
        return style;
    }

    private static int getShort(byte[] array, int offset) {
        return ((array[offset + 1] & 0xFF) << 8) | (array[offset] & 0xFF);
    }

    private static int[] getUtf8(byte[] array, int offset) {
        int val = array[offset];

        // Skip the UTF-16 length of the string.
        if ((val & 0x80) != 0) {
            offset += 2;
        } else {
            offset++;
        }

        // Read the UTF-8 length of the string.
        val = array[offset];
        offset++;
        int length;
        if ((val & 0x80) != 0) {
            int low = array[offset] & 0xFF;
            length = ((val & 0x7F) << 8) + low;
            offset++;
        } else {
            length = val;
        }

        return new int[] { offset, length };
    }

    private static int[] getUtf16(byte[] array, int offset) {
        int val = ((array[offset + 1] & 0xFF) << 8) | (array[offset] & 0xFF);

        if ((val & 0x8000) != 0) {
            int high = (array[offset + 3] & 0xFF) << 8;
            int low = array[offset + 2] & 0xFF;
            int len_value = ((val & 0x7FFF) << 16) + high + low;
            return new int[] { 4, len_value * 2 };
        }

        return new int[] { 2, val * 2 };
    }
}
