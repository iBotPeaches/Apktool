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

package org.jf.dexlib.Util;

public class NumberUtils {

    /**
     * Decodes the high signed 4-bit nibble from the given byte
     * @param b the byte to decode
     * @return the decoded signed nibble
     */
    public static byte decodeHighSignedNibble(byte b) {
        return (byte)(b >> 4);
    }

    /**
     * Decodes the low signed 4-bit nibble from the given byte
     * @param b the byte to decode
     * @return the decoded signed nibble
     */
    public static byte decodeLowSignedNibble(byte b) {
        return (byte)(((byte)(b << 4)) >> 4);
    }

    /**
     * Decodes the high unsigned 4-bit nibble from the given byte
     * @param b the byte to decode
     * @return the decoded unsigned nibble
     */
    public static byte decodeHighUnsignedNibble(byte b) {
        return (byte)((b & 0xFF) >>> 4);
    }

    /**
     * Decodes the low unsigned 4-bit nibble from the given byte
     * @param b the byte to decode
     * @return the decoded unsigned nibble
     */
    public static byte decodeLowUnsignedNibble(byte b) {
        return (byte)(b & 0x0F);
    }

    /**
     * Decodes an unsigned byte from a signed byte
     * @param b the signed byte to decode
     * @return the decoded unsigned byte as a short
     */
    public static short decodeUnsignedByte(byte b) {
        return (short)(b & 0xFF);
    }

    /**
     * Decodes a signed short value from 2 individual bytes
     * The parameters are in order from least significant byte to most significant byte
     * @param lsb the least significant byte
     * @param msb the most significant byte
     * @return the decoded signed short value
     */
    public static short decodeShort(byte lsb, byte msb) {
        return (short)
               (    (lsb & 0xFF) |
                    (msb << 8)
               );
    }

    /**
     * Decodes a signed short value in little endian format from the given byte array at the given index.
     * @param bytes the byte array
     * @param index the index of the first byte of the signed short value to decode
     * @return the decoded signed short value
     */
    public static short decodeShort(byte[] bytes, int index) {
        return (short)
               (    (bytes[index++] & 0xFF) |
                    (bytes[index] << 8)
               );
    }

    /**
     * Decodes an unsigned short value from 2 individual bytes
     * The parameters are in order from least significant byte to most significant byte
     * @param lsb the least significant byte
     * @param msb the most significant byte
     * @return the decoded unsigned short value as an int
     */
    public static int decodeUnsignedShort(byte lsb, byte msb) {
        return  (   (lsb & 0xFF) |
                    ((msb & 0xFF) << 8)
                );
    }

    /**
     * Decodes an unsigned short value in little endian format from the given byte array at the given index.
     * @param bytes the byte array
     * @param index the index of the first byte of the unsigned short value to decode
     * @return the decoded unsigned short value as an int
     */
    public static int decodeUnsignedShort(byte[] bytes, int index) {
        return  (   (bytes[index++] & 0xFF) |
                    ((bytes[index] & 0xFF) << 8)
                );
    }

    /**
     * Decodes a signed integer value from 4 individual bytes
     * The parameters are in order from least significant byte to most significant byte
     * @param lsb the least significant byte
     * @param mlsb the middle least significant byte
     * @param mmsb the middle most significant byte
     * @param msb the most significant byte
     * @return the decoded signed integer value
     */
    public static int decodeInt(byte lsb, byte mlsb, byte mmsb, byte msb) {
        return (lsb & 0xFF) |
               ((mlsb & 0xFF) << 8) |
               ((mmsb & 0xFF) << 16) |
               (msb << 24);
    }

    /**
     * Decodes a signed integer value in little endian format from the given byte array at the given index.
     * @param bytes the byte array
     * @param index the index of the first byte of the signed integer value to decode
     * @return the decoded signed integer value
     */
    public static int decodeInt(byte[] bytes, int index) {
        return (bytes[index++]  & 0xFF) |
               ((bytes[index++] & 0xFF) << 8) |
               ((bytes[index++] & 0xFF) << 16) |
               (bytes[index] << 24);
    }

    /**
     * Decodes a signed long value from 8 individual bytes
     * The parameters are in order from least significant byte to most significant byte
     * @param llsb the lower least significant byte
     * @param lmlsb the lower middle least significant byte
     * @param lmmsb the lower middle most significant byte
     * @param lgsb the lower greater significant byte
     * @param glsb the greater least significant byte
     * @param gmlsb the greater middle least significant byte
     * @param gmmsb the greater middle most significant byte
     * @param gmsb the greater most significant byte
     * @return the decoded signed long value
     */
    public static long decodeLong(byte llsb, byte lmlsb, byte lmmsb, byte lgsb, byte glsb, byte gmlsb, byte gmmsb,
                                  byte gmsb) {
        return  (llsb   & 0xFFL) |
                ((lmlsb & 0xFFL) << 8) |
                ((lmmsb & 0xFFL) << 16) |
                ((lgsb  & 0xFFL) << 24) |
                ((glsb  & 0xFFL) << 32) |
                ((gmlsb & 0xFFL) << 40) |
                ((gmmsb & 0xFFL) << 48) |
                (((long)gmsb) << 56);
    }

    /**
     * Decodes a signed long value in little endian format from the given byte array at the given index.
     * @param bytes the byte array
     * @param index the index of the first byte of the signed long value to decode
     * @return the decoded signed long value
     */
    public static long decodeLong(byte[] bytes, int index) {
        return  (bytes[index++] & 0xFFL) |
                ((bytes[index++] & 0xFFL) << 8) |
                ((bytes[index++] & 0xFFL) << 16) |
                ((bytes[index++] & 0xFFL) << 24) |
                ((bytes[index++] & 0xFFL) << 32) |
                ((bytes[index++] & 0xFFL) << 40) |
                ((bytes[index++] & 0xFFL) << 48) |
                (((long)bytes[index]) << 56);
    }
}
