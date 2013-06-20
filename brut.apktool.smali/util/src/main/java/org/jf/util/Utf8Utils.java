/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * As per the Apache license requirements, this file has been modified
 * from its original state.
 *
 * Such modifications are Copyright (C) 2010 Ben Gruver, and are released
 * under the original license
 */

package org.jf.util;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Constants of type <code>CONSTANT_Utf8_info</code>.
 */
public final class Utf8Utils {
    /**
     * Converts a string into its Java-style UTF-8 form. Java-style UTF-8
     * differs from normal UTF-8 in the handling of character '\0' and
     * surrogate pairs.
     *
     * @param string non-null; the string to convert
     * @return non-null; the UTF-8 bytes for it
     */
    public static byte[] stringToUtf8Bytes(String string) {
        int len = string.length();
        byte[] bytes = new byte[len * 3]; // Avoid having to reallocate.
        int outAt = 0;

        for (int i = 0; i < len; i++) {
            char c = string.charAt(i);
            if ((c != 0) && (c < 0x80)) {
                bytes[outAt] = (byte) c;
                outAt++;
            } else if (c < 0x800) {
                bytes[outAt] = (byte) (((c >> 6) & 0x1f) | 0xc0);
                bytes[outAt + 1] = (byte) ((c & 0x3f) | 0x80);
                outAt += 2;
            } else {
                bytes[outAt] = (byte) (((c >> 12) & 0x0f) | 0xe0);
                bytes[outAt + 1] = (byte) (((c >> 6) & 0x3f) | 0x80);
                bytes[outAt + 2] = (byte) ((c & 0x3f) | 0x80);
                outAt += 3;
            }
        }

        byte[] result = new byte[outAt];
        System.arraycopy(bytes, 0, result, 0, outAt);
        return result;
    }

    private static final ThreadLocal<char[]> localBuffer =
            new ThreadLocal<char[]> () {
                @Override protected char[] initialValue() {
                    // A reasonably sized initial value
                    return new char[256];
                }
            };

    /**
     * Converts an array of UTF-8 bytes into a string.
     *
     * @param bytes non-null; the bytes to convert
     * @param start the start index of the utf8 string to convert
     * @param length the length of the utf8 string to convert, not including any null-terminator that might be present
     * @return non-null; the converted string
     */
    public static String utf8BytesToString(byte[] bytes, int start, int length) {
        char[] chars = localBuffer.get();
        if (chars == null || chars.length < length) {
            chars = new char[length];
            localBuffer.set(chars);
        }
        int outAt = 0;

        for (int at = start; length > 0; /*at*/) {
            int v0 = bytes[at] & 0xFF;
            char out;
            switch (v0 >> 4) {
                case 0x00: case 0x01: case 0x02: case 0x03:
                case 0x04: case 0x05: case 0x06: case 0x07: {
                    // 0XXXXXXX -- single-byte encoding
                    length--;
                    if (v0 == 0) {
                        // A single zero byte is illegal.
                        return throwBadUtf8(v0, at);
                    }
                    out = (char) v0;
                    at++;
                    break;
                }
                case 0x0c: case 0x0d: {
                    // 110XXXXX -- two-byte encoding
                    length -= 2;
                    if (length < 0) {
                        return throwBadUtf8(v0, at);
                    }
                    int v1 = bytes[at + 1] & 0xFF;
                    if ((v1 & 0xc0) != 0x80) {
                        return throwBadUtf8(v1, at + 1);
                    }
                    int value = ((v0 & 0x1f) << 6) | (v1 & 0x3f);
                    if ((value != 0) && (value < 0x80)) {
                        /*
                         * This should have been represented with
                         * one-byte encoding.
                         */
                        return throwBadUtf8(v1, at + 1);
                    }
                    out = (char) value;
                    at += 2;
                    break;
                }
                case 0x0e: {
                    // 1110XXXX -- three-byte encoding
                    length -= 3;
                    if (length < 0) {
                        return throwBadUtf8(v0, at);
                    }
                    int v1 = bytes[at + 1] & 0xFF;
                    if ((v1 & 0xc0) != 0x80) {
                        return throwBadUtf8(v1, at + 1);
                    }
                    int v2 = bytes[at + 2] & 0xFF;
                    if ((v2 & 0xc0) != 0x80) {
                        return throwBadUtf8(v2, at + 2);
                    }
                    int value = ((v0 & 0x0f) << 12) | ((v1 & 0x3f) << 6) |
                            (v2 & 0x3f);
                    if (value < 0x800) {
                        /*
                         * This should have been represented with one- or
                         * two-byte encoding.
                         */
                        return throwBadUtf8(v2, at + 2);
                    }
                    out = (char) value;
                    at += 3;
                    break;
                }
                default: {
                    // 10XXXXXX, 1111XXXX -- illegal
                    return throwBadUtf8(v0, at);
                }
            }
            chars[outAt] = out;
            outAt++;
        }

        return new String(chars, 0, outAt);
    }

    /**
     * Converts an array of UTF-8 bytes into a string.
     *
     * @param bytes non-null; the bytes to convert
     * @param start the start index of the utf8 string to convert
     * @param utf16Length the number of utf16 characters in the string to decode
     * @return non-null; the converted string
     */
    public static String utf8BytesWithUtf16LengthToString(@Nonnull byte[] bytes, int start, int utf16Length) {
        return utf8BytesWithUtf16LengthToString(bytes, start, utf16Length, null);
    }

    /**
     * Converts an array of UTF-8 bytes into a string.
     *
     * @param bytes non-null; the bytes to convert
     * @param start the start index of the utf8 string to convert
     * @param utf16Length the number of utf16 characters in the string to decode
     * @param readLength If non-null, the first element will contain the number of bytes read after the method exits
     * @return non-null; the converted string
     */
    public static String utf8BytesWithUtf16LengthToString(@Nonnull byte[] bytes, int start, int utf16Length,
                                                          @Nullable int[] readLength) {
        char[] chars = localBuffer.get();
        if (chars == null || chars.length < utf16Length) {
            chars = new char[utf16Length];
            localBuffer.set(chars);
        }
        int outAt = 0;

        int at = 0;
        for (at = start; utf16Length > 0; utf16Length--) {
            int v0 = bytes[at] & 0xFF;
            char out;
            switch (v0 >> 4) {
                case 0x00: case 0x01: case 0x02: case 0x03:
                case 0x04: case 0x05: case 0x06: case 0x07: {
                    // 0XXXXXXX -- single-byte encoding
                    if (v0 == 0) {
                        // A single zero byte is illegal.
                        return throwBadUtf8(v0, at);
                    }
                    out = (char) v0;
                    at++;
                    break;
                }
                case 0x0c: case 0x0d: {
                    // 110XXXXX -- two-byte encoding
                    int v1 = bytes[at + 1] & 0xFF;
                    if ((v1 & 0xc0) != 0x80) {
                        return throwBadUtf8(v1, at + 1);
                    }
                    int value = ((v0 & 0x1f) << 6) | (v1 & 0x3f);
                    if ((value != 0) && (value < 0x80)) {
                        /*
                         * This should have been represented with
                         * one-byte encoding.
                         */
                        return throwBadUtf8(v1, at + 1);
                    }
                    out = (char) value;
                    at += 2;
                    break;
                }
                case 0x0e: {
                    // 1110XXXX -- three-byte encoding
                    int v1 = bytes[at + 1] & 0xFF;
                    if ((v1 & 0xc0) != 0x80) {
                        return throwBadUtf8(v1, at + 1);
                    }
                    int v2 = bytes[at + 2] & 0xFF;
                    if ((v2 & 0xc0) != 0x80) {
                        return throwBadUtf8(v2, at + 2);
                    }
                    int value = ((v0 & 0x0f) << 12) | ((v1 & 0x3f) << 6) |
                        (v2 & 0x3f);
                    if (value < 0x800) {
                        /*
                         * This should have been represented with one- or
                         * two-byte encoding.
                         */
                        return throwBadUtf8(v2, at + 2);
                    }
                    out = (char) value;
                    at += 3;
                    break;
                }
                default: {
                    // 10XXXXXX, 1111XXXX -- illegal
                    return throwBadUtf8(v0, at);
                }
            }
            chars[outAt] = out;
            outAt++;
        }

        if (readLength != null && readLength.length > 0) {
            readLength[0] = at - start;
            readLength[0] = at - start;
        }
        return new String(chars, 0, outAt);
    }

    /**
     * Helper for {@link #utf8BytesToString}, which throws the right
     * exception for a bogus utf-8 byte.
     *
     * @param value the byte value
     * @param offset the file offset
     * @return never
     * @throws IllegalArgumentException always thrown
     */
    private static String throwBadUtf8(int value, int offset) {
        throw new IllegalArgumentException("bad utf-8 byte " + Hex.u1(value) +
                                           " at offset " + Hex.u4(offset));
    }
}
