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
package brut.androlib.res.decoder;

import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;

public class StringBlockWithSurrogatePairInUtf8Test {
    @Test
    public void decodeSingleOctet() {
        final String actual = new StringBlock("abcDEF123".getBytes(StandardCharsets.UTF_8), true).decodeString(0, 9);
        assertEquals("Incorrect decoding", "abcDEF123", actual);
    }

    @Test
    public void decodeTwoOctets() {
        final String actual0 = new StringBlock(new byte[] {	(byte) 0xC2, (byte) 0x80}, true).decodeString(0, 2);
        assertEquals("Incorrect decoding", "\u0080", actual0);

        final String actual1 = new StringBlock(new byte[] {	(byte) 0xDF, (byte) 0xBF}, true).decodeString(0, 2);
        assertEquals("Incorrect decoding", "\u07FF", actual1);
    }

    @Test
    public void decodeThreeOctets() {
        final String actual0 = new StringBlock(new byte[] {	(byte) 0xE0, (byte) 0xA0, (byte) 0x80}, true).decodeString(0, 3);
        assertEquals("Incorrect decoding", "\u0800", actual0);

        final String actual1 = new StringBlock(new byte[] {	(byte) 0xEF, (byte) 0xBF, (byte) 0xBF}, true).decodeString(0, 3);
        assertEquals("Incorrect decoding", "\uFFFF", actual1);
    }

    @Test
    public void decodeSurrogatePair_when_givesAsThreeOctetsFromInvalidRangeOfUtf8() {
        // See: https://github.com/iBotPeaches/Apktool/issues/2299
        final String actual = new StringBlock(new byte[] {	(byte) 0xED, (byte) 0xA0, (byte) 0xBD, (byte) 0xED, (byte) 0xB4, (byte) 0x86}, true).decodeString(0, 6);
        assertEquals("Incorrect decoding", "\uD83D\uDD06", actual);

        // See: https://github.com/iBotPeaches/Apktool/issues/2546
        final byte[] bytesWithCharactersBeforeSurrogatePair = {'G', 'o', 'o', 'd', ' ', 'm', 'o', 'r', 'n', 'i', 'n', 'g', '!', ' ',
                (byte) 0xED, (byte) 0xA0, (byte) 0xBD, (byte) 0xED, (byte) 0xB1, (byte) 0x8B,
                ' ', 'S', 'u', 'n', ' ',
                (byte) 0xED, (byte) 0xA0, (byte) 0xBC, (byte) 0xED, (byte) 0xBC, (byte) 0x9E
        };
        final String actual2 = new StringBlock(bytesWithCharactersBeforeSurrogatePair, true).decodeString(0, 31);

        // D83D -> 0xED 0xA0 0xBD
        // DC4B -> 0xED 0xB1 0x8B
        // D83C -> 0xED 0xA0 0xBC
        // DF1E -> 0xED 0xBC 0x9E
        assertEquals("Incorrect decoding when there are valid characters before the surrogate pair",
                "Good morning! \uD83D\uDC4B Sun \uD83C\uDF1E", actual2);
    }

    @Test
    public void decodeSurrogatePair_when_givesAsThreeOctetsFromTheValidRangeOfUtf8() {
        // \u10FFFF is encoded in UTF-8 as "0xDBFF 0xDFFF" (4-byte encoding),
        // but when used in Android resources which are encoded in UTF-8, 3-byte encoding is used,
        // so each of these is encoded as 3-bytes
        final String actual = new StringBlock(new byte[] {	(byte) 0xED, (byte) 0xAF, (byte) 0xBF, (byte) 0xED, (byte) 0xBF, (byte) 0xBF}, true).decodeString(0, 6);
        assertEquals("Incorrect decoding", "\uDBFF\uDFFF", actual);
    }
}
