/*
 * Copyright 2012, Google Inc.
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

public abstract class NibbleUtils {
    /**
     * Extracts the high signed 4-bit nibble from the least significant
     * byte of the given value
     * @param value the value to extract the nibble from
     * @return the extracted signed nibble value
     */
    public static int extractHighSignedNibble(int value) {
        return (value << 24) >> 28;
    }

    /**
     * Extracts the low signed 4-bit nibble from the least significant
     * byte of the given value
     * @param value the value to extract the nibble from
     * @return the extracted signed nibble value
     */
    public static int extractLowSignedNibble(int value) {
        return (value << 28) >> 28;
    }

    /**
     * Extracts the high unsigned 4-bit nibble from the least significant
     * byte of the given value
     * @param value the value to extract the nibble from
     * @return the extracted unsigned nibble value
     */
    public static int extractHighUnsignedNibble(int value) {
        return (value & 0xF0) >>> 4;
    }

    /**
     * Extracts the low unsigned 4-bit nibble from the least significant
     * byte of the given value
     * @param value the value to extract the nibble from
     * @return the extracted unsigned nibble value
     */
    public static int extractLowUnsignedNibble(int value) {
        return value & 0x0F;
    }
}
