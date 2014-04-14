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

package org.jf.dexlib2.dexbacked.raw;

import org.jf.dexlib2.dexbacked.BaseDexBuffer;

public class OdexHeaderItem {
    public static final int ITEM_SIZE = 40;

    public static final byte[][] MAGIC_VALUES= new byte[][] {
            new byte[] {0x64, 0x65, 0x79, 0x0A, 0x30, 0x33, 0x35, 0x00}, // "dey\n035\0"
            new byte[] {0x64, 0x65, 0x79, 0x0A, 0x30, 0x33, 0x36, 0x00}  // "dey\n036\0"
    };

    public static final int MAGIC_OFFSET = 0;
    public static final int MAGIC_LENGTH = 8;
    public static final int DEX_OFFSET = 8;
    public static final int DEX_LENGTH_OFFSET = 12;
    public static final int DEPENDENCIES_OFFSET = 16;
    public static final int DEPENDENCIES_LENGTH_OFFSET = 20;
    public static final int AUX_OFFSET = 24;
    public static final int AUX_LENGTH_OFFSET = 28;
    public static final int FLAGS_OFFSET = 32;

    public static int getVersion(byte[] magic) {
        if (magic.length < 8) {
            return 0;
        }

        boolean matches = true;
        for (int i=0; i<MAGIC_VALUES.length; i++) {
            byte[] expected = MAGIC_VALUES[i];
            matches = true;
            for (int j=0; j<8; j++) {
                if (magic[j] != expected[j]) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                return i==0?35:36;
            }
        }
        return 0;
    }

    public static boolean verifyMagic(byte[] buf) {
        // verifies the magic value
        return getVersion(buf) != 0;
    }

    public static int getDexOffset(byte[] buf) {
        BaseDexBuffer bdb = new BaseDexBuffer(buf);
        return bdb.readSmallUint(DEX_OFFSET);
    }

    public static int getDependenciesOffset(byte[] buf) {
        BaseDexBuffer bdb = new BaseDexBuffer(buf);
        return bdb.readSmallUint(DEPENDENCIES_OFFSET);
    }
}
