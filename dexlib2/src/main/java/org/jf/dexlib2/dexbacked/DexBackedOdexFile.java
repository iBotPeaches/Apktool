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

package org.jf.dexlib2.dexbacked;

import com.google.common.io.ByteStreams;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.dexbacked.raw.OdexHeaderItem;
import org.jf.dexlib2.dexbacked.util.VariableSizeList;
import org.jf.dexlib2.util.DexUtil;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;

public class DexBackedOdexFile extends DexBackedDexFile {
    private static final int DEPENDENCY_COUNT_OFFSET = 12;
    private static final int DEPENDENCY_START_OFFSET = 16;

    private final byte[] odexBuf;

    public DexBackedOdexFile(@Nonnull Opcodes opcodes, @Nonnull byte[] odexBuf, byte[] dexBuf) {
        super(opcodes, dexBuf);

        this.odexBuf = odexBuf;
    }

    @Override public boolean supportsOptimizedOpcodes() {
        return true;
    }

    @Nonnull public List<String> getDependencies() {
        final int dexOffset = OdexHeaderItem.getDexOffset(odexBuf);
        final int dependencyOffset = OdexHeaderItem.getDependenciesOffset(odexBuf) - dexOffset;

        DexBuffer fromStartBuffer = new DexBuffer(getBuffer().buf, 0);
        int dependencyCount = fromStartBuffer.readInt(dependencyOffset + DEPENDENCY_COUNT_OFFSET);

        return new VariableSizeList<String>(
                this.getDataBuffer(), dependencyOffset + DEPENDENCY_START_OFFSET, dependencyCount) {
            @Override protected String readNextItem(@Nonnull DexReader reader, int index) {
                int length = reader.readInt();
                int offset = reader.getOffset();
                reader.moveRelative(length + 20);
                try {
                    return new String(fromStartBuffer.buf, offset, length-1, "US-ASCII");
                } catch (UnsupportedEncodingException ex) {
                    throw new RuntimeException(ex);
                }
            }
        };
    }

    @Nonnull public static DexBackedOdexFile fromInputStream(@Nonnull Opcodes opcodes, @Nonnull InputStream is)
            throws IOException {
        DexUtil.verifyOdexHeader(is);

        is.reset();
        byte[] odexBuf = new byte[OdexHeaderItem.ITEM_SIZE];
        ByteStreams.readFully(is, odexBuf);
        int dexOffset = OdexHeaderItem.getDexOffset(odexBuf);
        if (dexOffset > OdexHeaderItem.ITEM_SIZE) {
            ByteStreams.skipFully(is, dexOffset - OdexHeaderItem.ITEM_SIZE);
        }

        byte[] dexBuf = ByteStreams.toByteArray(is);

        return new DexBackedOdexFile(opcodes, odexBuf, dexBuf);
    }

    public int getOdexVersion() {
        return OdexHeaderItem.getVersion(odexBuf, 0);
    }

    public static class NotAnOdexFile extends RuntimeException {
        public NotAnOdexFile() {
        }

        public NotAnOdexFile(Throwable cause) {
            super(cause);
        }

        public NotAnOdexFile(String message) {
            super(message);
        }

        public NotAnOdexFile(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
