/*
 * Copyright 2016, Google Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 * Neither the name of Google Inc. nor the names of its
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

package org.jf.dexlib2.iface;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;

/**
 * This class represents a dex container that can contain multiple, named dex files
 */
public interface MultiDexContainer<T extends DexFile> {
    /**
     * @return A list of the names of dex entries in this container
     */
    @Nonnull List<String> getDexEntryNames() throws IOException;

    /**
     * Gets the dex entry with the given name
     *
     * @param entryName The name of the entry
     * @return A DexFile, or null if no entry with that name is found
     */
    @Nullable DexEntry<T> getEntry(@Nonnull String entryName) throws IOException;

    /**
     * This class represents a dex file entry in a MultiDexContainer
     */
    interface DexEntry<T extends DexFile> {
        /**
         * @return The name of this entry within its container
         */
        @Nonnull String getEntryName();

        /**
         * @return The dex file associated with this entry
         */
        @Nonnull T getDexFile();

        /**
         * @return The MultiDexContainer that contains this dex file
         */
        @Nonnull MultiDexContainer<? extends T> getContainer();
    }
}
