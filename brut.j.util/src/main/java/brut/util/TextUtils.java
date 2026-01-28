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
package brut.util;

public final class TextUtils {
    private static final int SINGLE_PRECISION_BIAS = 127;
    private static final int SINGLE_PRECISION_EXP_MIN = -126;
    private static final int SINGLE_PRECISION_EXP_MAX = 127;
    private static final int FLOAT_NEGATIVE_MASK = 0x80000000;
    private static final int MANTISSA_EXPONENT_ADJUST_VALUE = 0xFFFFFF;
    private static final int DENORMAL_EXPONENT_ADJUST_VALUE = MANTISSA_EXPONENT_ADJUST_VALUE - 1;
    private static final long SCALAR_SHIFT_1 = 1L << 60;
    private static final long SCALAR_SHIFT_2 = 1L << 61;
    private static final long SCALAR_SHIFT_3 = 1L << 62;
    private static final long SCALAR_SHIFT_4 = 1L << 63;
    private static final long DEC_SIGNIFICAND_LEADING_BIT = 1L << 59;
    private static final int DEC_SIGNIFICAND_DOWN_SHIFT = 36;
    private static final long ROUND_VALUE = 1L << 35;

    private static final long[] POSITIVE_SIGNIFICANDS = {
        0X0800000000000000L, 0X0A00000000000000L, 0X0C80000000000000L, 0X0FA0000000000000L,
        0X09C4000000000000L, 0X0C35000000000000L, 0X0F42400000000000L, 0X0989680000000000L,
        0X0BEBC20000000000L, 0X0EE6B28000000000L, 0X09502F9000000000L, 0X0BA43B7400000000L,
        0X0E8D4A5100000000L, 0X09184E72A0000000L, 0X0B5E620F48000000L, 0X0E35FA931A000000L,
        0X08E1BC9BF0400000L, 0X0B1A2BC2EC500000L, 0X0DE0B6B3A7640000L, 0X08AC7230489E8000L,
        0X0AD78EBC5AC62000L, 0X0D8D726B7177A800L, 0X0878678326EAC900L, 0X0A968163F0A57B40L,
        0X0D3C21BCECCEDA10L, 0X084595161401484AL, 0X0A56FA5B99019A5CL, 0X0CECB8F27F4200F3L,
        0X0813F3978F894098L, 0X0A18F07D736B90BEL, 0X0C9F2C9CD04674EDL, 0X0FC6F7C404581229L,
        0X09DC5ADA82B70B59L, 0X0C5371912364CE30L, 0X0F684DF56C3E01BCL, 0X09A130B963A6C115L,
        0X0C097CE7BC90715BL, 0X0F0BDC21ABB48DB2L, 0X096769950B50D88FL
    };
    private static final int[] POSITIVE_SHIFTS = {
        0, 3, 6, 9, 13, 16, 19, 23, 26, 29, 33, 36, 39, 43, 46, 49, 53, 56, 59, 63, 66, 69, 73, 76,
        79, 83, 86, 89, 93, 96, 99, 102, 106, 109, 112, 116, 119, 122, 126
    };
    private static final long[] NEGATIVE_SIGNIFICANDS = {
        0X0CCCCCCCCCCCCCCCL, 0X0A3D70A3D70A3D70L, 0X083126E978D4FDF3L, 0X0D1B71758E219652L,
        0X0A7C5AC471B47842L, 0X08637BD05AF6C69BL, 0X0D6BF94D5E57A42BL, 0X0ABCC77118461CEFL,
        0X089705F4136B4A59L, 0X0DBE6FECEBDEDD5BL, 0X0AFEBFF0BCB24AAFL, 0X08CBCCC096F5088CL,
        0X0E12E13424BB40E1L, 0X0B424DC35095CD80L, 0X0901D7CF73AB0ACDL, 0X0E69594BEC44DE15L,
        0X0B877AA3236A4B44L, 0X09392EE8E921D5D0L, 0X0EC1E4A7DB69561AL, 0X0BCE5086492111AEL,
        0X0971DA05074DA7BEL, 0X0F1C90080BAF72CBL, 0X0C16D9A0095928A2L, 0X09ABE14CD44753B5L,
        0X0F79687AED3EEC55L, 0X0C612062576589DDL, 0X09E74D1B791E07E4L, 0X0FD87B5F28300CA0L,
        0X0CAD2F7F5359A3B3L, 0X0A2425FF75E14FC3L, 0X081CEB32C4B43FCFL, 0X0CFB11EAD453994BL,
        0X0A6274BBDD0FADD6L, 0X084EC3C97DA624ABL, 0X0D4AD2DBFC3D0778L, 0X0AA242499697392DL,
        0X0881CEA14545C757L, 0X0D9C7DCED53C7225L, 0X0AE397D8AA96C1B7L, 0X08B61313BBABCE2CL,
        0X0DF01E85F912E37AL, 0X0B267ED1940F1C61L, 0X08EB98A7A9A5B04EL, 0X0E45C10C42A2B3B0L,
        0X0B6B00D69BB55C8DL, 0X09226712162AB070L, 0X0E9D71B689DDE71AL
    };
    private static final int[] NEGATIVE_SHIFTS = {
        -4, -7, -10, -14, -17, -20, -24, -27, -30, -34, -37, -40, -44, -47, -50, -54, -57, -60, -64,
        -67, -70, -74, -77, -80, -84, -87, -90, -94, -97, -100, -103, -107, -110, -113, -117, -120,
        -123, -127, -130, -133, -137, -140, -143, -147, -150, -153, -157
    };

    private TextUtils() {
        // Private constructor for utility class.
    }

    public static String matchSuffix(CharSequence text, String... suffixes) {
        int len = text.length();
        if (len == 0 || suffixes.length == 0) {
            return null;
        }

        for (String suffix : suffixes) {
            int suffixLen = suffix.length();
            if (suffixLen == 0 || suffixLen > len) {
                continue;
            }
            boolean matched = true;
            for (int i = 0; i < suffixLen; i++) {
                if (text.charAt(len - suffixLen + i) != suffix.charAt(i)) {
                    matched = false;
                    break;
                }
            }
            if (matched) {
                return suffix;
            }
        }
        return null;
    }

    public static boolean isPrintableChar(char ch) {
        return (ch >= 0x20 && ch <= 0x7E) || (ch >= 0xA0 && ch <= 0xD7FF)
                || (ch >= 0xE000 && ch <= 0xFDCF) || (ch >= 0xFDF0 && ch <= 0xFFFD);
    }

    public static int parseHex(int codePoint) {
        if (codePoint >= '0' && codePoint <= '9') {
            return (codePoint - '0');
        }
        if (codePoint >= 'A' && codePoint <= 'F') {
            return (codePoint - 'A') + 10;
        }
        if (codePoint >= 'a' && codePoint <= 'f') {
            return (codePoint - 'a') + 10;
        }
        return -1;
    }

    public static int parseColor(CharSequence text) throws NumberFormatException {
        return parseColor(text, 0, text.length());
    }

    public static int parseColor(CharSequence text, int start, int end)
            throws NumberFormatException {
        if (start > end || end > text.length()) {
            throw new IndexOutOfBoundsException();
        }
        if (start == end || text.charAt(0) != '#') {
            throw new NumberFormatException();
        }

        int i = start + 1;
        int value = 0;

        switch (end - start) {
            case 4: // #RGB
                for (; i < end; i++) {
                    int hex = parseHex(text.charAt(i));
                    if (hex == -1) {
                        throw new NumberFormatException();
                    }
                    value = (value << 8) | (hex | (hex << 4));
                }
                value |= 0xFF000000;
                break;
            case 5: // #ARGB
                for (; i < end; i++) {
                    int hex = parseHex(text.charAt(i));
                    if (hex == -1) {
                        throw new NumberFormatException();
                    }
                    value = (value << 8) | (hex | (hex << 4));
                }
                break;
            case 7: // #RRGGBB
                for (; i < end; i++) {
                    int hex = parseHex(text.charAt(i));
                    if (hex == -1) {
                        throw new NumberFormatException();
                    }
                    value = (value << 4) | hex;
                }
                value |= 0xFF000000;
                break;
            case 9: // #AARRGGBB
                for (; i < end; i++) {
                    int hex = parseHex(text.charAt(i));
                    if (hex == -1) {
                        throw new NumberFormatException();
                    }
                    value = (value << 4) | hex;
                }
                break;
            default:
                throw new NumberFormatException();
        }

        return value;
    }

    public static int parseInt(CharSequence text) throws NumberFormatException {
        return parseInt(text, 0, text.length());
    }

    public static int parseInt(CharSequence text, int start, int end)
            throws NumberFormatException {
        if (start > end || end > text.length()) {
            throw new IndexOutOfBoundsException();
        }
        if (start == end) {
            throw new NumberFormatException();
        }

        int i = start;
        boolean negative = text.charAt(i) == '-';
        if ((negative || text.charAt(i) == '+') && ++i == end) {
            throw new NumberFormatException();
        }

        if (i + 1 < end && text.charAt(i) == '0'
                && (text.charAt(i + 1) == 'x' || text.charAt(i + 1) == 'X')) {
            if (negative || (i += 2) == end) {
                throw new NumberFormatException();
            }
            return parseIntHex(text, i, end);
        }

        return parseIntDec(text, i, end, negative);
    }

    private static int parseIntDec(CharSequence text, int start, int end, boolean negative)
            throws NumberFormatException {
        long value = 0;

        for (int i = start; i < end; i++) {
            char ch = text.charAt(i);
            if (ch < '0' || ch > '9') {
                throw new NumberFormatException();
            }

            value = value * 10 + (ch - '0');

            if (negative ? -value < Integer.MIN_VALUE : value > Integer.MAX_VALUE) {
                throw new NumberFormatException();
            }
        }

        return (int) (negative ? -value : value);
    }

    private static int parseIntHex(CharSequence text, int start, int end)
            throws NumberFormatException {
        long value = 0;

        for (int i = start; i < end; i++) {
            int hex = parseHex(text.charAt(i));
            if (hex == -1) {
                throw new NumberFormatException();
            }

            value = (value << 4) + hex;

            if (value > 0xFFFFFFFFL) {
                throw new NumberFormatException();
            }
        }

        return (int) value;
    }

    public static float parseFloat(CharSequence text) throws NumberFormatException {
        return parseFloat(text, 0, text.length());
    }

    public static float parseFloat(CharSequence text, int start, int end)
            throws NumberFormatException {
        if (start > end || end > text.length()) {
            throw new IndexOutOfBoundsException();
        }
        if (start == end) {
            throw new NumberFormatException();
        }

        int i = 0;
        boolean negative = text.charAt(i) == '-';
        if ((negative || text.charAt(i) == '+') && ++i == end) {
            throw new NumberFormatException();
        }

        if (i + 1 < end && text.charAt(i) == '0'
                && (text.charAt(i + 1) == 'x' || text.charAt(i + 1) == 'X')) {
            if ((i += 2) == end) {
                throw new NumberFormatException();
            }
            return parseFloatHex(text, i, end, negative);
        }

        return parseFloatDec(text, i, end, negative);
    }

    private static float parseFloatDec(CharSequence text, int start, int end, boolean negative)
            throws NumberFormatException {
        int i = start;
        int significandBegin = i;
        boolean significandStart = false;
        boolean validSignificand = false;
        boolean subInteger = false;
        int mostSignificantExponent = 0;

        while (i < end && text.charAt(i) != 'e' && text.charAt(i) != 'E') {
            if (text.charAt(i) == '.') {
                if (subInteger) {
                    throw new NumberFormatException();
                }

                subInteger = true;
                i++;
                continue;
            }

            if (significandStart && !subInteger) {
                ++mostSignificantExponent;
            } else if (!significandStart && subInteger) {
                --mostSignificantExponent;
            }

            if (text.charAt(i) < '0' || text.charAt(i) > '9') {
                throw new NumberFormatException();
            }

            validSignificand = true;
            if (text.charAt(i) != '0' && !significandStart) {
                significandStart = true;
                significandBegin = i;
            }

            i++;
        }
        if (!validSignificand) {
            throw new NumberFormatException();
        }

        int significandEnd = i;
        int declaredExponent = 0;
        if (i < end) {
            if (i == end - 1) {
                throw new NumberFormatException();
            }

            i++;
            boolean negativeExponent = text.charAt(i) == '-';
            if (negativeExponent || text.charAt(i) == '+') {
                i++;
            }

            while (i < end) {
                if (text.charAt(i) < '0' || text.charAt(i) > '9') {
                    throw new NumberFormatException();
                }

                int currentValue = text.charAt(i) - '0';
                declaredExponent = declaredExponent * 10 + currentValue;
                i++;
            }
            if (negativeExponent) {
                declaredExponent = -declaredExponent;
            }
        }

        if (!significandStart) {
            return negative ? -0.0f : 0.0f;
        }

        mostSignificantExponent += declaredExponent;
        i = significandBegin;

        long significandValue = getValueByExponent(mostSignificantExponent);
        if (significandValue == 0L) {
            throw new NumberFormatException();
        }

        int baseBinaryShift = getShiftByExponent(mostSignificantExponent);
        significandValue *= text.charAt(i) - '0';
        int significandScalarShift = getScalarShiftByValue(significandValue);
        significandValue >>>= significandScalarShift;
        baseBinaryShift += significandScalarShift;

        int currentExponent = mostSignificantExponent - 1;
        i++;
        while (i < significandEnd) {
            if (text.charAt(i) < '1' || text.charAt(i) > '9') {
                if (text.charAt(i) == '0') {
                    --currentExponent;
                }

                i++;
                continue;
            }

            long scalarValue = text.charAt(i) - '0';
            long currentValue = getValueByExponent(currentExponent) * scalarValue;
            int relativeDownShift = baseBinaryShift - getShiftByExponent(currentExponent);
            int scalarShift = getScalarShiftByValue(currentValue);
            currentValue = currentValue >>> scalarShift;
            relativeDownShift -= scalarShift;
            if (relativeDownShift > 59) {
                break;
            }

            significandValue += currentValue >>> relativeDownShift;
            if ((significandValue & SCALAR_SHIFT_1) != 0L) {
                significandValue = significandValue >>> 1;
                baseBinaryShift += 1;
            }

            --currentExponent;
            i++;
        }

        significandValue += ROUND_VALUE;
        if ((significandValue & SCALAR_SHIFT_1) != 0L) {
            significandValue = significandValue >>> 1;
            baseBinaryShift += 1;
        }

        if (baseBinaryShift < SINGLE_PRECISION_EXP_MIN
                || baseBinaryShift > SINGLE_PRECISION_EXP_MAX) {
            throw new NumberFormatException();
        }

        int mantissa = (int) ((significandValue & ~DEC_SIGNIFICAND_LEADING_BIT)
                >>> DEC_SIGNIFICAND_DOWN_SHIFT);
        int biasedExp = baseBinaryShift + SINGLE_PRECISION_BIAS;
        return Float.intBitsToFloat(
            (negative ? FLOAT_NEGATIVE_MASK : 0) | mantissa | (biasedExp << 23));
    }

    private static float parseFloatHex(CharSequence text, int start, int end, boolean negative)
            throws NumberFormatException {
        int i = start;
        int currentMantissa = 0;
        int currentSkew = 0;
        int mantissaBits = 0;
        boolean mantissaStart = false;
        boolean subInteger = false;
        boolean validMantissa = false;

        while (i < end && text.charAt(i) != 'p') {
            int indexValue = parseHex(text.charAt(i));
            if (indexValue == -1) {
                if (text.charAt(i) != '.' || subInteger) {
                    throw new NumberFormatException();
                }

                subInteger = true;
                i++;
                continue;
            }

            validMantissa = true;
            if (!mantissaStart && indexValue != 0) {
                mantissaStart = true;
                currentMantissa = indexValue;

                if (indexValue >= 8) {
                    currentSkew -= 1;
                    mantissaBits = 3;
                } else if (indexValue >= 4) {
                    currentSkew -= 2;
                    mantissaBits = 2;
                } else if (indexValue >= 2) {
                    currentSkew -= 3;
                    mantissaBits = 1;
                } else {
                    currentSkew -= 4;
                    mantissaBits = 0;
                }
            } else if (mantissaStart && mantissaBits < 24) {
                currentMantissa = (currentMantissa << 4) + indexValue;
                mantissaBits += 4;
            }

            if (mantissaStart && !subInteger) {
                currentSkew += 4;
            } else if (!mantissaStart && subInteger) {
                currentSkew -= 4;
            }

            i++;
        }

        if (!validMantissa) {
            throw new NumberFormatException();
        }

        int declaredExponent = 0;
        if (i < end) {
            if (i == end - 1) {
                throw new NumberFormatException();
            }
            i++;

            boolean negativeExponent = text.charAt(i) == '-';
            if (negativeExponent || text.charAt(i) == '+') {
                i++;
            }

            while (i < end) {
                if (text.charAt(i) < '0' || text.charAt(i) > '9') {
                    throw new NumberFormatException();
                }

                int currentValue = text.charAt(i) - '0';
                declaredExponent = declaredExponent * 10 + currentValue;
                i++;
            }

            if (negativeExponent) {
                declaredExponent = -declaredExponent;
            }
        }

        if (24 - mantissaBits < 0) {
            currentMantissa >>>= (mantissaBits - 24);
        } else {
            currentMantissa <<= (24 - mantissaBits);
        }
        currentMantissa = currentMantissa & MANTISSA_EXPONENT_ADJUST_VALUE;

        int exponent;
        if (currentMantissa == MANTISSA_EXPONENT_ADJUST_VALUE
                || (currentMantissa == DENORMAL_EXPONENT_ADJUST_VALUE
                && declaredExponent + currentSkew + 1 == SINGLE_PRECISION_EXP_MIN)) {
            currentMantissa = 0;
            exponent = declaredExponent + currentSkew + 1;
        } else {
            currentMantissa = ((currentMantissa + 1) >>> 1);
            exponent = declaredExponent + currentSkew;
        }

        if (!mantissaStart) {
            return negative ? -0.0f : 0.0f;
        }

        if (exponent < SINGLE_PRECISION_EXP_MIN || exponent > SINGLE_PRECISION_EXP_MAX) {
            throw new NumberFormatException();
        }

        int biasedExp = exponent + SINGLE_PRECISION_BIAS;
        return Float.intBitsToFloat(
            (negative ? FLOAT_NEGATIVE_MASK : 0) | currentMantissa | (biasedExp << 23));
    }

    private static long getValueByExponent(int exponent) {
        if (exponent < POSITIVE_SIGNIFICANDS.length) {
            if (exponent >= 0) {
                return POSITIVE_SIGNIFICANDS[exponent];
            }
            if (exponent >= -NEGATIVE_SIGNIFICANDS.length) {
                return NEGATIVE_SIGNIFICANDS[-exponent - 1];
            }
        }
        return 0L;
    }

    private static int getShiftByExponent(int exponent) {
        if (exponent < POSITIVE_SIGNIFICANDS.length) {
            if (exponent >= 0) {
                return POSITIVE_SHIFTS[exponent];
            }
            if (exponent >= -NEGATIVE_SIGNIFICANDS.length) {
                return NEGATIVE_SHIFTS[-exponent - 1];
            }
        }
        return 0;
    }

    private static int getScalarShiftByValue(long valueToNormalize) {
        if ((valueToNormalize & SCALAR_SHIFT_4) != 0L) {
            return 4;
        }
        if ((valueToNormalize & SCALAR_SHIFT_3) != 0L) {
            return 3;
        }
        if ((valueToNormalize & SCALAR_SHIFT_2) != 0L) {
            return 2;
        }
        if ((valueToNormalize & SCALAR_SHIFT_1) != 0L) {
            return 1;
        }
        return 0;
    }
}
