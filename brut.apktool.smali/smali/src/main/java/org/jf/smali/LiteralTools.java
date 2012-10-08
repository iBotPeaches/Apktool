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
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.jf.smali;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LiteralTools
{
    public static byte parseByte(String byteLiteral)
            throws NumberFormatException {
        if (byteLiteral == null) {
            throw new NumberFormatException("string is null");
        }
        if (byteLiteral.length() == 0) {
            throw new NumberFormatException("string is blank");
        }

        char[] byteChars;
        if (byteLiteral.toUpperCase().endsWith("T")) {
            byteChars = byteLiteral.substring(0, byteLiteral.length()-1).toCharArray();
        } else {
            byteChars = byteLiteral.toCharArray();
        }

        int position = 0;
        int radix = 10;
        boolean negative = false;
        if (byteChars[position] == '-') {
            position++;
            negative = true;
        }

        if (byteChars[position] == '0') {
            position++;
            if (position == byteChars.length) {
                return 0;
            } else if (byteChars[position] == 'x' || byteChars[position] == 'X') {
                radix = 16;
                position++;
            } else if (Character.digit(byteChars[position], 8) >= 0) {
                radix = 8;
            }
        }

        byte result = 0;
        byte shiftedResult;
        int digit;
        byte maxValue = (byte)(Byte.MAX_VALUE / (radix / 2));

        while (position < byteChars.length) {
            digit = Character.digit(byteChars[position], radix);
            if (digit < 0) {
                throw new NumberFormatException("The string contains invalid an digit - '" + byteChars[position] + "'");
            }
            shiftedResult = (byte)(result * radix);
            if (result > maxValue) {
                throw new NumberFormatException(byteLiteral + " cannot fit into a byte");
            }
            if (shiftedResult < 0 && shiftedResult >= -digit) {
                throw new NumberFormatException(byteLiteral + " cannot fit into a byte");
            }
            result = (byte)(shiftedResult + digit);
            position++;
        }

        if (negative) {
            //allow -0x80, which is = 0x80
            if (result == Byte.MIN_VALUE) {
                return result;
            } else if (result < 0) {
                throw new NumberFormatException(byteLiteral + " cannot fit into a byte");
            }
            return (byte)(result * -1);
        } else {
            return result;
        }
    }

    public static short parseShort(String shortLiteral)
            throws NumberFormatException {
        if (shortLiteral == null) {
            throw new NumberFormatException("string is null");
        }
        if (shortLiteral.length() == 0) {
            throw new NumberFormatException("string is blank");
        }

        char[] shortChars;
        if (shortLiteral.toUpperCase().endsWith("S")) {
            shortChars = shortLiteral.substring(0, shortLiteral.length()-1).toCharArray();
        } else {
            shortChars = shortLiteral.toCharArray();
        }

        int position = 0;
        int radix = 10;
        boolean negative = false;
        if (shortChars[position] == '-') {
            position++;
            negative = true;
        }

        if (shortChars[position] == '0') {
            position++;
            if (position == shortChars.length) {
                return 0;
            } else if (shortChars[position] == 'x' || shortChars[position] == 'X') {
                radix = 16;
                position++;
            } else if (Character.digit(shortChars[position], 8) >= 0) {
                radix = 8;
            }
        }

        short result = 0;
        short shiftedResult;
        int digit;
        short maxValue = (short)(Short.MAX_VALUE / (radix / 2));

        while (position < shortChars.length) {
            digit = Character.digit(shortChars[position], radix);
            if (digit < 0) {
                throw new NumberFormatException("The string contains invalid an digit - '" + shortChars[position] + "'");
            }
            shiftedResult = (short)(result * radix);
            if (result > maxValue) {
                throw new NumberFormatException(shortLiteral + " cannot fit into a short");
            }
            if (shiftedResult < 0 && shiftedResult >= -digit) {
                throw new NumberFormatException(shortLiteral + " cannot fit into a short");
            }
            result = (short)(shiftedResult + digit);
            position++;
        }

        if (negative) {
            //allow -0x8000, which is = 0x8000
            if (result == Short.MIN_VALUE) {
                return result;
            } else if (result < 0) {
                throw new NumberFormatException(shortLiteral + " cannot fit into a short");
            }
            return (short)(result * -1);
        } else {
            return result;
        }
    }

    public static int parseInt(String intLiteral)
            throws NumberFormatException {
        if (intLiteral == null) {
            throw new NumberFormatException("string is null");
        }
        if (intLiteral.length() == 0) {
            throw new NumberFormatException("string is blank");
        }

        char[] intChars = intLiteral.toCharArray();
        int position = 0;
        int radix = 10;
        boolean negative = false;
        if (intChars[position] == '-') {
            position++;
            negative = true;
        }

        if (intChars[position] == '0') {
            position++;
            if (position == intChars.length) {
                return 0;
            } else if (intChars[position] == 'x' || intChars[position] == 'X') {
                radix = 16;
                position++;
            } else if (Character.digit(intChars[position], 8) >= 0) {
                radix = 8;
            }
        }

        int result = 0;
        int shiftedResult;
        int digit;
        int maxValue = Integer.MAX_VALUE / (radix / 2);

        while (position < intChars.length) {
            digit = Character.digit(intChars[position], radix);
            if (digit < 0) {
                throw new NumberFormatException("The string contains an invalid digit - '" + intChars[position] + "'");
            }
            shiftedResult = result * radix;
            if (result > maxValue) {
                throw new NumberFormatException(intLiteral + " cannot fit into an int");
            }
            if (shiftedResult < 0 && shiftedResult >= -digit) {
                throw new NumberFormatException(intLiteral + " cannot fit into an int");
            }
            result = shiftedResult + digit;
            position++;
        }

        if (negative) {
            //allow -0x80000000, which is = 0x80000000
            if (result == Integer.MIN_VALUE) {
                return result;
            } else if (result < 0) {
                throw new NumberFormatException(intLiteral + " cannot fit into an int");
            }
            return result * -1;
        } else {
            return result;
        }
    }

    public static long parseLong(String longLiteral)
            throws NumberFormatException {
        if (longLiteral == null) {
            throw new NumberFormatException("string is null");
        }
        if (longLiteral.length() == 0) {
            throw new NumberFormatException("string is blank");
        }

        char[] longChars;
        if (longLiteral.toUpperCase().endsWith("L")) {
            longChars = longLiteral.substring(0, longLiteral.length()-1).toCharArray();
        } else {
            longChars = longLiteral.toCharArray();
        }

        int position = 0;
        int radix = 10;
        boolean negative = false;
        if (longChars[position] == '-') {
            position++;
            negative = true;
        }

        if (longChars[position] == '0') {
            position++;
            if (position == longChars.length) {
                return 0;
            } else if (longChars[position] == 'x' || longChars[position] == 'X') {
                radix = 16;
                position++;
            } else if (Character.digit(longChars[position], 8) >= 0) {
                radix = 8;
            }
        }

        long result = 0;
        long shiftedResult;
        int digit;
        long maxValue = Long.MAX_VALUE / (radix / 2);

        while (position < longChars.length) {
            digit = Character.digit(longChars[position], radix);
            if (digit < 0) {
                throw new NumberFormatException("The string contains an invalid digit - '" + longChars[position] + "'");
            }
            shiftedResult = result * radix;
            if (result > maxValue) {
                throw new NumberFormatException(longLiteral + " cannot fit into a long");
            }
            if (shiftedResult < 0 && shiftedResult >= -digit) {
                throw new NumberFormatException(longLiteral + " cannot fit into a long");
            }
            result = shiftedResult + digit;
            position++;
        }

        if (negative) {
            //allow -0x8000000000000000, which is = 0x8000000000000000
            if (result == Long.MIN_VALUE) {
                return result;
            } else if (result < 0) {
                throw new NumberFormatException(longLiteral + " cannot fit into a long");
            }
            return result * -1;
        } else {
            return result;
        }
    }

    private static Pattern specialFloatRegex = Pattern.compile("((-)?infinityf)|(nanf)", Pattern.CASE_INSENSITIVE);
    public static float parseFloat(String floatString) {
        Matcher m = specialFloatRegex.matcher(floatString);
        if (m.matches()) {
            //got an infinity
            if (m.start(1) != -1) {
                if (m.start(2) != -1) {
                    return Float.NEGATIVE_INFINITY;
                } else {
                    return Float.POSITIVE_INFINITY;
                }
            } else {
                return Float.NaN;
            }
        }
        return Float.parseFloat(floatString);
    }

    private static Pattern specialDoubleRegex = Pattern.compile("((-)?infinityd?)|(nand?)", Pattern.CASE_INSENSITIVE);
    public static double parseDouble(String doubleString) {
        Matcher m = specialDoubleRegex.matcher(doubleString);
        if (m.matches()) {
            //got an infinity
            if (m.start(1) != -1) {
                if (m.start(2) != -1) {
                    return Double.NEGATIVE_INFINITY;
                } else {
                    return Double.POSITIVE_INFINITY;
                }
            } else {
                return Double.NaN;
            }
        }
        return Double.parseDouble(doubleString);
    }

    public static byte[] longToBytes(long value) {
        byte[] bytes = new byte[8];

        for (int i=0; value != 0; i++) {
            bytes[i] = (byte)value;
            value = value >>> 8;
        }
        return bytes;
    }

    public static byte[] intToBytes(int value) {
        byte[] bytes = new byte[4];

        for (int i=0; value != 0; i++) {
            bytes[i] = (byte)value;
            value = value >>> 8;
        }
        return bytes;
    }

    public static byte[] shortToBytes(short value) {
        byte[] bytes = new byte[2];

        bytes[0] = (byte)value;
        bytes[1] = (byte)(value >>> 8);
        return bytes;
    }

    public static byte[] floatToBytes(float value) {
        return intToBytes(Float.floatToRawIntBits(value));
    }

    public static byte[] doubleToBytes(double value) {
        return longToBytes(Double.doubleToRawLongBits(value));
    }

    public static byte[] charToBytes(char value) {
        return shortToBytes((short)value);
    }

    public static byte[] boolToBytes(boolean value) {
        if (value) {
            return new byte[] { 0x01 };
        } else {
            return new byte[] { 0x00 };
        }
    }

    public static void checkInt(long value) {
        if (value > 0xFFFFFFFF || value < -0x80000000) {
            throw new NumberFormatException(Long.toString(value) + " cannot fit into an int");
        }
    }

    public static void checkShort(long value) {
        if (value > 0xFFFF | value < -0x8000) {
            throw new NumberFormatException(Long.toString(value) + " cannot fit into a short");
        }
    }

    public static void checkByte(long value) {
        if (value > 0xFF | value < -0x80) {
            throw new NumberFormatException(Long.toString(value) + " cannot fit into a byte");
        }
    }

    public static void checkNibble(long value) {
        if (value > 0x0F | value < -0x08) {
            throw new NumberFormatException(Long.toString(value) + " cannot fit into a nibble");
        }
    }
}
