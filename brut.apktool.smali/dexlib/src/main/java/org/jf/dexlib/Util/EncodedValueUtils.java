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

package org.jf.dexlib.Util;

public class EncodedValueUtils {
    public static byte getRequiredBytesForSignedIntegralValue(long value) {
        /*
         * Figure out how many bits are needed to represent the value,
         * including a sign bit: The bit count is subtracted from 65
         * and not 64 to account for the sign bit. The xor operation
         * has the effect of leaving non-negative values alone and
         * unary complementing negative values (so that a leading zero
         * count always returns a useful number for our present
         * purpose).
         */
        int requiredBits =
            65 - Long.numberOfLeadingZeros(value ^ (value >> 63));

        // Round up the requiredBits to a number of bytes.
        return (byte)((requiredBits + 0x07) >> 3);
    }

    public static long decodeSignedIntegralValue(byte[] bytes) {
        long value = 0;
        for (int i = 0; i < bytes.length; i++) {
            value |= (((long)(bytes[i] & 0xFF)) << (i * 8));
        }

        int shift = (8 - bytes.length) * 8;
        return  value << shift >> shift;
    }

    public static byte[] encodeSignedIntegralValue(long value) {
        int requiredBytes = getRequiredBytesForSignedIntegralValue(value);

        byte[] bytes = new byte[requiredBytes];

        for (int i = 0; i < requiredBytes; i++) {
            bytes[i] = (byte) value;
            value >>= 8;
        }
        return bytes;
    }





    public static byte getRequiredBytesForUnsignedIntegralValue(long value) {
        // Figure out how many bits are needed to represent the value.
        int requiredBits = 64 - Long.numberOfLeadingZeros(value);
        if (requiredBits == 0) {
            requiredBits = 1;
        }

        // Round up the requiredBits to a number of bytes.
        return (byte)((requiredBits + 0x07) >> 3);
    }

    public static long decodeUnsignedIntegralValue(byte[] bytes) {
        long value = 0;
        for (int i = 0; i < bytes.length; i++) {
            value |= (((long)(bytes[i] & 0xFF)) << i * 8);
        }
        return value;
    }

    public static byte[] encodeUnsignedIntegralValue(long value) {
        int requiredBytes = getRequiredBytesForUnsignedIntegralValue(value);

        byte[] bytes = new byte[requiredBytes];

        for (int i = 0; i < requiredBytes; i++) {
            bytes[i] = (byte) value;
            value >>= 8;
        }
        return bytes;
    }





    public static int getRequiredBytesForRightZeroExtendedValue(long value) {
        // Figure out how many bits are needed to represent the value.
        int requiredBits = 64 - Long.numberOfTrailingZeros(value);
        if (requiredBits == 0) {
            requiredBits = 1;
        }

        // Round up the requiredBits to a number of bytes.
        return (requiredBits + 0x07) >> 3;
    }

    public static long decodeRightZeroExtendedValue(byte[] bytes) {
        long value = 0;
        for (int i = 0; i < bytes.length; i++) {
            value |= (((long)(bytes[i] & 0xFF)) << (i * 8));
        }
        return value << (8 - bytes.length) * 8;
    }

    public static byte[] encodeRightZeroExtendedValue(long value) {
        int requiredBytes = getRequiredBytesForRightZeroExtendedValue(value);

        // Scootch the first bits to be written down to the low-order bits.
        value >>= 64 - (requiredBytes * 8);

        byte[] bytes = new byte[requiredBytes];

        for(int i = 0; i < requiredBytes; i++) {
            bytes[i] = (byte)value;
            value >>= 8;
        }
        return bytes;
    }
}
