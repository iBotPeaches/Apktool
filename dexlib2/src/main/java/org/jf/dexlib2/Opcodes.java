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

package org.jf.dexlib2;

import com.google.common.collect.Maps;
import com.google.common.collect.RangeMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.HashMap;

import static org.jf.dexlib2.VersionMap.NO_VERSION;
import static org.jf.dexlib2.VersionMap.mapApiToArtVersion;
import static org.jf.dexlib2.VersionMap.mapArtVersionToApi;

public class Opcodes {

    /**
     * Either the api level for dalvik opcodes, or the art version for art opcodes
     */
    public final int api;
    public final int artVersion;
    @Nonnull private final Opcode[] opcodesByValue = new Opcode[256];
    @Nonnull private final EnumMap<Opcode, Short> opcodeValues;
    @Nonnull private final HashMap<String, Opcode> opcodesByName;

    @Nonnull
    public static Opcodes forApi(int api) {
        return new Opcodes(api, NO_VERSION);
    }

    @Nonnull
    public static Opcodes forArtVersion(int artVersion) {
        return new Opcodes(NO_VERSION, artVersion);
    }

    @Nonnull
    public static Opcodes forDexVersion(int dexVersion) {
        int api = VersionMap.mapDexVersionToApi(dexVersion);
        if (api == NO_VERSION) {
            throw new RuntimeException("Unsupported dex version " + dexVersion);
        }
        return new Opcodes(api, NO_VERSION);
    }

    /**
     * @return a default Opcodes instance for when the exact Opcodes to use doesn't matter or isn't known
     */
    @Nonnull
    public static Opcodes getDefault() {
        // The last pre-art api
        return forApi(20);
    }

    private Opcodes(int api, int artVersion) {
        if (api >= 21) {
            this.api = api;
            this.artVersion = mapApiToArtVersion(api);
        } else if (artVersion >= 0 && artVersion < 39) {
            this.api = mapArtVersionToApi(artVersion);
            this.artVersion = artVersion;
        } else {
            this.api = api;
            this.artVersion = artVersion;
        }

        opcodeValues = new EnumMap<Opcode, Short>(Opcode.class);
        opcodesByName = Maps.newHashMap();

        int version;
        if (isArt()) {
            version = this.artVersion;
        } else {
            version = this.api;
        }

        for (Opcode opcode: Opcode.values()) {
            RangeMap<Integer, Short> versionToValueMap;

            if (isArt()) {
                versionToValueMap = opcode.artVersionToValueMap;
            } else {
                versionToValueMap = opcode.apiToValueMap;
            }

            Short opcodeValue = versionToValueMap.get(version);
            if (opcodeValue != null) {
                if (!opcode.format.isPayloadFormat) {
                    opcodesByValue[opcodeValue] = opcode;
                }
                opcodeValues.put(opcode, opcodeValue);
                opcodesByName.put(opcode.name.toLowerCase(), opcode);
            }
        }
    }

    @Nullable
    public Opcode getOpcodeByName(@Nonnull String opcodeName) {
        return opcodesByName.get(opcodeName.toLowerCase());
    }

    @Nullable
    public Opcode getOpcodeByValue(int opcodeValue) {
        switch (opcodeValue) {
            case 0x100:
                return Opcode.PACKED_SWITCH_PAYLOAD;
            case 0x200:
                return Opcode.SPARSE_SWITCH_PAYLOAD;
            case 0x300:
                return Opcode.ARRAY_PAYLOAD;
            default:
                if (opcodeValue >= 0 && opcodeValue < opcodesByValue.length) {
                    return opcodesByValue[opcodeValue];
                }
                return null;
        }
    }

    @Nullable
    public Short getOpcodeValue(@Nonnull Opcode opcode) {
        return opcodeValues.get(opcode);
    }

    public boolean isArt() {
        return artVersion != NO_VERSION;
    }
}
