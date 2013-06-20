/*
 * Copyright 2013, Google Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 *     * Neither the name of Google Inc. nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.jf.util;

import java.text.DecimalFormat;

public class NumberUtils {
    private static final int canonicalFloatNaN = Float.floatToRawIntBits(Float.NaN);
    private static final int maxFloat = Float.floatToRawIntBits(Float.MAX_VALUE);
    private static final int piFloat = Float.floatToRawIntBits((float)Math.PI);
    private static final int eFloat = Float.floatToRawIntBits((float)Math.E);

    private static final long canonicalDoubleNaN = Double.doubleToRawLongBits(Double.NaN);
    private static final long maxDouble = Double.doubleToLongBits(Double.MAX_VALUE);
    private static final long piDouble = Double.doubleToLongBits(Math.PI);
    private static final long eDouble = Double.doubleToLongBits(Math.E);

    private static final DecimalFormat format = new DecimalFormat("0.####################E0");

    public static boolean isLikelyFloat(int value) {
        // Check for some common named float values
        // We don't check for Float.MIN_VALUE, which has an integer representation of 1
        if (value == canonicalFloatNaN ||
                value == maxFloat ||
                value == piFloat ||
                value == eFloat) {
            return true;
        }

        // Check for some named integer values
        if (value == Integer.MAX_VALUE || value == Integer.MIN_VALUE) {
            return false;
        }


        // Check for likely resource id
        int packageId = value >> 24;
        int resourceType = value >> 16 & 0xff;
        int resourceId = value & 0xffff;
        if ((packageId == 0x7f || packageId == 1) && resourceType < 0x1f && resourceId < 0xfff) {
            return false;
        }

        // a non-canocical NaN is more likely to be an integer
        float floatValue = Float.intBitsToFloat(value);
        if (Float.isNaN(floatValue)) {
            return false;
        }

        // Otherwise, whichever has a shorter scientific notation representation is more likely.
        // Integer wins the tie
        String asInt = format.format(value);
        String asFloat = format.format(floatValue);

        // try to strip off any small imprecision near the end of the mantissa
        int decimalPoint = asFloat.indexOf('.');
        int exponent = asFloat.indexOf("E");
        int zeros = asFloat.indexOf("000");
        if (zeros > decimalPoint && zeros < exponent) {
            asFloat = asFloat.substring(0, zeros) + asFloat.substring(exponent);
        } else {
            int nines = asFloat.indexOf("999");
            if (nines > decimalPoint && nines < exponent) {
                asFloat = asFloat.substring(0, nines) + asFloat.substring(exponent);
            }
        }

        return asFloat.length() < asInt.length();
    }

    public static boolean isLikelyDouble(long value) {
        // Check for some common named double values
        // We don't check for Double.MIN_VALUE, which has a long representation of 1
        if (value == canonicalDoubleNaN ||
                value == maxDouble ||
                value == piDouble ||
                value == eDouble) {
            return true;
        }

        // Check for some named long values
        if (value == Long.MAX_VALUE || value == Long.MIN_VALUE) {
            return false;
        }

        // a non-canocical NaN is more likely to be an long
        double doubleValue = Double.longBitsToDouble(value);
        if (Double.isNaN(doubleValue)) {
            return false;
        }

        // Otherwise, whichever has a shorter scientific notation representation is more likely.
        // Long wins the tie
        String asLong = format.format(value);
        String asDouble = format.format(doubleValue);

        // try to strip off any small imprecision near the end of the mantissa
        int decimalPoint = asDouble.indexOf('.');
        int exponent = asDouble.indexOf("E");
        int zeros = asDouble.indexOf("000");
        if (zeros > decimalPoint && zeros < exponent) {
            asDouble = asDouble.substring(0, zeros) + asDouble.substring(exponent);
        } else {
            int nines = asDouble.indexOf("999");
            if (nines > decimalPoint && nines < exponent) {
                asDouble = asDouble.substring(0, nines) + asDouble.substring(exponent);
            }
        }

        return asDouble.length() < asLong.length();
    }
}
