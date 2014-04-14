/*
 * [The "BSD licence"]
 * Copyright (c) 2010 Ben Gruver (JesusFreke)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.jf.util;

/**
 * Utilities for formatting numbers as hexadecimal.
 */
public final class Hex {
    /**
     * This class is uninstantiable.
     */
    private Hex() {
        // This space intentionally left blank.
    }

    /**
     * Formats a <code>long</code> as an 8-byte unsigned hex value.
     *
     * @param v value to format
     * @return non-null; formatted form
     */
    public static String u8(long v) {
        char[] result = new char[16];
        for (int i = 0; i < 16; i++) {
            result[15 - i] = Character.forDigit((int) v & 0x0f, 16);
            v >>= 4;
        }

        return new String(result);
    }

    /**
     * Formats an <code>int</code> as a 4-byte unsigned hex value.
     *
     * @param v value to format
     * @return non-null; formatted form
     */
    public static String u4(int v) {
        char[] result = new char[8];
        for (int i = 0; i < 8; i++) {
            result[7 - i] = Character.forDigit(v & 0x0f, 16);
            v >>= 4;
        }

        return new String(result);
    }

    /**
     * Formats an <code>int</code> as a 3-byte unsigned hex value.
     *
     * @param v value to format
     * @return non-null; formatted form
     */
    public static String u3(int v) {
        char[] result = new char[6];
        for (int i = 0; i < 6; i++) {
            result[5 - i] = Character.forDigit(v & 0x0f, 16);
            v >>= 4;
        }

        return new String(result);
    }

    /**
     * Formats an <code>int</code> as a 2-byte unsigned hex value.
     *
     * @param v value to format
     * @return non-null; formatted form
     */
    public static String u2(int v) {
        char[] result = new char[4];
        for (int i = 0; i < 4; i++) {
            result[3 - i] = Character.forDigit(v & 0x0f, 16);
            v >>= 4;
        }

        return new String(result);
    }

    /**
     * Formats an <code>int</code> as either a 2-byte unsigned hex value
     * (if the value is small enough) or a 4-byte unsigned hex value (if
     * not).
     *
     * @param v value to format
     * @return non-null; formatted form
     */
    public static String u2or4(int v) {
        if (v == (char) v) {
            return u2(v);
        } else {
            return u4(v);
        }
    }

    /**
     * Formats an <code>int</code> as a 1-byte unsigned hex value.
     *
     * @param v value to format
     * @return non-null; formatted form
     */
    public static String u1(int v) {
        char[] result = new char[2];
        for (int i = 0; i < 2; i++) {
            result[1 - i] = Character.forDigit(v & 0x0f, 16);
            v >>= 4;
        }

        return new String(result);
    }

    /**
     * Formats an <code>int</code> as a 4-bit unsigned hex nibble.
     *
     * @param v value to format
     * @return non-null; formatted form
     */
    public static String uNibble(int v) {
        char[] result = new char[1];

        result[0] = Character.forDigit(v & 0x0f, 16);
        return new String(result);
    }

    /**
     * Formats a <code>long</code> as an 8-byte signed hex value.
     *
     * @param v value to format
     * @return non-null; formatted form
     */
    public static String s8(long v) {
        char[] result = new char[17];

        if (v < 0) {
            result[0] = '-';
            v = -v;
        } else {
            result[0] = '+';
        }

        for (int i = 0; i < 16; i++) {
            result[16 - i] = Character.forDigit((int) v & 0x0f, 16);
            v >>= 4;
        }

        return new String(result);
    }

    /**
     * Formats an <code>int</code> as a 4-byte signed hex value.
     *
     * @param v value to format
     * @return non-null; formatted form
     */
    public static String s4(int v) {
        char[] result = new char[9];

        if (v < 0) {
            result[0] = '-';
            v = -v;
        } else {
            result[0] = '+';
        }

        for (int i = 0; i < 8; i++) {
            result[8 - i] = Character.forDigit(v & 0x0f, 16);
            v >>= 4;
        }

        return new String(result);
    }

    /**
     * Formats an <code>int</code> as a 2-byte signed hex value.
     *
     * @param v value to format
     * @return non-null; formatted form
     */
    public static String s2(int v) {
        char[] result = new char[5];

        if (v < 0) {
            result[0] = '-';
            v = -v;
        } else {
            result[0] = '+';
        }

        for (int i = 0; i < 4; i++) {
            result[4 - i] = Character.forDigit(v & 0x0f, 16);
            v >>= 4;
        }

        return new String(result);
    }

    /**
     * Formats an <code>int</code> as a 1-byte signed hex value.
     *
     * @param v value to format
     * @return non-null; formatted form
     */
    public static String s1(int v) {
        char[] result = new char[3];

        if (v < 0) {
            result[0] = '-';
            v = -v;
        } else {
            result[0] = '+';
        }

        for (int i = 0; i < 2; i++) {
            result[2 - i] = Character.forDigit(v & 0x0f, 16);
            v >>= 4;
        }

        return new String(result);
    }

    /**
     * Formats a hex dump of a portion of a <code>byte[]</code>. The result
     * is always newline-terminated, unless the passed-in length was zero,
     * in which case the result is always the empty string (<code>""</code>).
     *
     * @param arr non-null; array to format
     * @param offset &gt;= 0; offset to the part to dump
     * @param length &gt;= 0; number of bytes to dump
     * @param outOffset &gt;= 0; first output offset to print
     * @param bpl &gt;= 0; number of bytes of output per line
     * @param addressLength {2,4,6,8}; number of characters for each address
     * header
     * @return non-null; a string of the dump
     */
    public static String dump(byte[] arr, int offset, int length,
                              int outOffset, int bpl, int addressLength) {
        int end = offset + length;

        // twos-complement math trick: ((x < 0) || (y < 0)) <=> ((x|y) < 0)
        if (((offset | length | end) < 0) || (end > arr.length)) {
            throw new IndexOutOfBoundsException("arr.length " +
                                                arr.length + "; " +
                                                offset + "..!" + end);
        }

        if (outOffset < 0) {
            throw new IllegalArgumentException("outOffset < 0");
        }

        if (length == 0) {
            return "";
        }

        StringBuffer sb = new StringBuffer(length * 4 + 6);
        boolean bol = true;
        int col = 0;

        while (length > 0) {
            if (col == 0) {
                String astr;
                switch (addressLength) {
                    case 2:  astr = Hex.u1(outOffset); break;
                    case 4:  astr = Hex.u2(outOffset); break;
                    case 6:  astr = Hex.u3(outOffset); break;
                    default: astr = Hex.u4(outOffset); break;
                }
                sb.append(astr);
                sb.append(": ");
            } else if ((col & 1) == 0) {
                sb.append(' ');
            }
            sb.append(Hex.u1(arr[offset]));
            outOffset++;
            offset++;
            col++;
            if (col == bpl) {
                sb.append('\n');
                col = 0;
            }
            length--;
        }

        if (col != 0) {
            sb.append('\n');
        }

        return sb.toString();
    }
}