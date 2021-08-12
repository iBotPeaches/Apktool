/*
 * Copyright 2019, Google Inc.
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

import org.jf.dexlib2.dexbacked.DexBuffer;
import org.jf.dexlib2.util.AnnotatedBytes;

import javax.annotation.Nonnull;

public class CdexHeaderItem {

    private static final byte[] MAGIC_VALUE = new byte[] { 0x63, 0x64, 0x65, 0x78, 0x00, 0x00, 0x00, 0x00 };
    private static final int[] SUPPORTED_CDEX_VERSIONS = new int[] { 1 };

    public static final int FEATURE_FLAGS_OFFSET = 112;
    public static final int DEBUG_INFO_OFFSETS_POS_OFFSET = 116;
    public static final int DEBUG_INFO_OFFSETS_TABLE_OFFSET = 120;
    public static final int DEBUG_INFO_BASE = 124;

    /**
     * Verifies the magic value at the beginning of a cdex file
     *
     * @param buf A byte array containing at least the first 8 bytes of a cdex file
     * @param offset The offset within the buffer to the beginning of the cdex header
     * @return True if the magic value is valid
     */
    public static boolean verifyMagic(byte[] buf, int offset) {
        if (buf.length - offset < 8) {
            return false;
        }

        for (int i=0; i<4; i++) {
            if (buf[offset + i] != MAGIC_VALUE[i]) {
                return false;
            }
        }
        for (int i=4; i<7; i++) {
            if (buf[offset + i] < '0' ||
                    buf[offset + i] > '9') {
                return false;
            }
        }
        if (buf[offset + 7] != MAGIC_VALUE[7]) {
            return false;
        }

        return true;
    }

    /**
     * Gets the dex version from an odex header
     *
     * @param buf A byte array containing at least the first 7 bytes of an odex file
     * @param offset The offset within the buffer to the beginning of the odex header
     * @return The odex version if the header is valid or -1 if the header is invalid
     */
    public static int getVersion(byte[] buf, int offset) {
        if (!verifyMagic(buf, offset)) {
            return -1;
        }

        return getVersionUnchecked(buf, offset);
    }

    private static int getVersionUnchecked(byte[] buf, int offset) {
        int version = (buf[offset + 4] - '0') * 100;
        version += (buf[offset + 5] - '0') * 10;
        version += buf[offset + 6] - '0';

        return version;
    }

    public static boolean isSupportedCdexVersion(int version) {
        for (int i=0; i<SUPPORTED_CDEX_VERSIONS.length; i++) {
            if (SUPPORTED_CDEX_VERSIONS[i] == version) {
                return true;
            }
        }
        return false;
    }

    public static void annotateCdexHeaderFields(@Nonnull AnnotatedBytes out, DexBuffer buf) {
        out.annotate(4, "feature_flags: 0x%x", buf.readInt(out.getCursor()));
        out.annotate(4, "debug_info_offsets_pos: 0x%x", buf.readInt(out.getCursor()));
        out.annotate(4, "debug_info_offsets_table_offset: 0x%x", buf.readInt(out.getCursor()));
        out.annotate(4, "debug_info_base: 0x%x", buf.readInt(out.getCursor()));
    }
}
