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

import brut.androlib.res.data.arsc.ARSCHeader;
import brut.androlib.res.xml.ResXmlEncoders;
import brut.util.ExtDataInput;
import com.google.common.annotations.VisibleForTesting;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class StringBlock {
    private static final Logger LOGGER = Logger.getLogger(StringBlock.class.getName());

    private static final CharsetDecoder UTF16LE_DECODER = StandardCharsets.UTF_16LE.newDecoder();
    private static final CharsetDecoder UTF8_DECODER = StandardCharsets.UTF_8.newDecoder();
    private static final CharsetDecoder CESU8_DECODER = Charset.forName("CESU8").newDecoder();

    private static final int UTF8_FLAG = 0x00000100;
    private static final int STRING_BLOCK_HEADER_SIZE = 28;

    private int[] mStringOffsets;
    private byte[] mStrings;
    private int[] mStyleOffsets;
    private int[] mStyles;
    private boolean mIsUtf8;

    public static StringBlock readWithChunk(ExtDataInput reader) throws IOException {
        long startPosition = reader.position();
        reader.skipCheckShort(ARSCHeader.RES_STRING_POOL_TYPE);
        int headerSize = reader.readShort();
        int chunkSize = reader.readInt();

        return readWithoutChunk(reader, startPosition, headerSize, chunkSize);
    }

    public static StringBlock readWithoutChunk(ExtDataInput reader, long startPosition,
                                               int headerSize, int chunkSize) throws IOException {
        // ResStringPool_header
        int stringCount = reader.readInt();
        int styleCount = reader.readInt();
        int flags = reader.readInt();
        int stringsOffset = reader.readInt();
        int stylesOffset = reader.readInt();

        // For some applications they pack the StringBlock header with more unused data at end.
        if (headerSize > STRING_BLOCK_HEADER_SIZE) {
            reader.skipBytes(headerSize - STRING_BLOCK_HEADER_SIZE);
        }

        StringBlock block = new StringBlock();
        block.mIsUtf8 = (flags & UTF8_FLAG) != 0;
        block.mStringOffsets = reader.readSafeIntArray(stringCount, startPosition + stringsOffset);

        if (styleCount != 0) {
            block.mStyleOffsets = reader.readSafeIntArray(styleCount, startPosition + stylesOffset);
        }

        // #3236 - Some applications give a style offset, but have 0 styles. Make this check more robust.
        boolean hasStyles = stylesOffset != 0 && styleCount != 0;
        int size = chunkSize - stringsOffset;

        // If we have both strings and even just a lying style offset - lets calculate the size of the strings without
        // accidentally parsing all the styles.
        if (styleCount > 0) {
            size = stylesOffset - stringsOffset;
        }

        block.mStrings = new byte[size];
        reader.readFully(block.mStrings);

        if (hasStyles) {
            size = chunkSize - stylesOffset;
            block.mStyles = reader.readIntArray(size / 4);
        }

        // In case we aren't 4 byte aligned we need to skip the remaining bytes.
        int remaining = size % 4;
        if (remaining >= 1) {
            while (remaining-- > 0) {
                reader.readByte();
            }
        }

        return block;
    }

    private StringBlock() {
    }

    @VisibleForTesting
    StringBlock(byte[] strings, boolean isUTF8) {
        mStrings = strings;
        mIsUtf8 = isUTF8;
    }

    /**
     * Returns raw string (without any styling information) at specified index.
     * @param index int
     * @return String
     */
    public String getString(int index) {
        if (index < 0 || mStringOffsets == null || index >= mStringOffsets.length) {
            return null;
        }
        int offset = mStringOffsets[index];
        int length;

        int[] val;
        if (mIsUtf8) {
            val = getUtf8(mStrings, offset);
            offset = val[0];
        } else {
            val = getUtf16(mStrings, offset);
            offset += val[0];
        }
        length = val[1];

        return decodeString(offset, length);
    }

    /**
     * @param index Location (index) of string to process to HTML
     * @return String Returns string with style tags (html-like).
     */
    public String getHTML(int index) {
        String text = getString(index);
        if (text == null) {
            return null;
        }

        int[] style = getStyle(index);
        if (style == null) {
            return ResXmlEncoders.escapeXmlChars(text);
        }

        // If the returned style is further in string, than string length. Lets skip it.
        if (style[1] > text.length()) {
            return ResXmlEncoders.escapeXmlChars(text);
        }

        // Convert styles to spans
        List<StyledString.Span> spans = new ArrayList<>(style.length / 3);
        for (int i = 0; i < style.length; i += 3) {
            spans.add(new StyledString.Span(getString(style[i]), style[i + 1], style[i + 2]));
        }
        spans.sort(null);

        return new StyledString(text, spans).toString();
    }

    /**
     * Finds index of the string. Returns -1 if the string was not found.
     *
     * @param string String to index location of
     * @return int (Returns -1 if not found)
     */
    public int find(String string) {
        if (string == null) {
            return -1;
        }
        for (int i = 0; i < mStringOffsets.length; i++) {
            int offset = mStringOffsets[i];
            int length = getShort(mStrings, offset);
            if (length != string.length()) {
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
     * Returns style information - array of int triplets, where in each triplet:
     * * first int is index of tag name ('b','i', etc.) * second int is tag
     * start index in string * third int is tag end index in string
     */
    private int[] getStyle(int index) {
        if (mStyleOffsets == null || mStyles == null|| index >= mStyleOffsets.length) {
            return null;
        }
        int offset = mStyleOffsets[index] / 4;
        int count = 0;
        int[] style;

        for (int i = offset; i < mStyles.length; i++) {
            if (mStyles[i] == -1) {
                break;
            }
            count += 1;
        }

        if (count == 0 || (count % 3) != 0) {
            return null;
        }
        style = new int[count];

        for (int i = offset, j = 0; i < mStyles.length;) {
            if (mStyles[i] == -1) {
                break;
            }
            style[j++] = mStyles[i++];
        }
        return style;
    }

    @VisibleForTesting
    String decodeString(int offset, int length) {
        try {
            ByteBuffer wrappedBuffer = ByteBuffer.wrap(mStrings, offset, length);
            return (mIsUtf8 ? UTF8_DECODER : UTF16LE_DECODER).decode(wrappedBuffer).toString();
        } catch (CharacterCodingException ex) {
            if (!mIsUtf8) {
                LOGGER.warning("Failed to decode a string at offset " + offset + " of length " + length);
                return null;
            }
        } catch (IndexOutOfBoundsException ex) {
            if (!mIsUtf8) {
                LOGGER.warning("String extends outside of pool at  " + offset + " of length " + length);
                return null;
            }
        }

        try {
            ByteBuffer wrappedBufferRetry = ByteBuffer.wrap(mStrings, offset, length);
            // in some places, Android uses 3-byte UTF-8 sequences instead of 4-bytes.
            // If decoding failed, we try to use CESU-8 decoder, which is closer to what Android actually uses.
            return CESU8_DECODER.decode(wrappedBufferRetry).toString();
        } catch (CharacterCodingException ex) {
            LOGGER.warning("Failed to decode a string with CESU-8 decoder.");
            return null;
        }
    }

    private static int getShort(byte[] array, int offset) {
        return (array[offset + 1] & 0xff) << 8 | array[offset] & 0xff;
    }

    private static int[] getUtf8(byte[] array, int offset) {
        int val = array[offset];
        int length;
        // We skip the utf16 length of the string
        if ((val & 0x80) != 0) {
            offset += 2;
        } else {
            offset += 1;
        }
        // And we read only the utf-8 encoded length of the string
        val = array[offset];
        offset += 1;
        if ((val & 0x80) != 0) {
            int low = array[offset] & 0xFF;
            length = ((val & 0x7F) << 8) + low;
            offset += 1;
        } else {
            length = val;
        }
        return new int[] { offset, length };
    }

    private static int[] getUtf16(byte[] array, int offset) {
        int val = (array[offset + 1] & 0xFF) << 8 | array[offset] & 0xFF;

        if ((val & 0x8000) != 0) {
            int high = (array[offset + 3] & 0xFF) << 8;
            int low = (array[offset + 2] & 0xFF);
            int len_value = ((val & 0x7FFF) << 16) + (high + low);
            return new int[] { 4, len_value * 2 };

        }
        return new int[] { 2, val * 2 };
    }
}
