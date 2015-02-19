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

package org.jf.dexlib2;

public enum Format {
    Format10t(2),
    Format10x(2),
    Format11n(2),
    Format11x(2),
    Format12x(2),
    Format20bc(4),
    Format20t(4),
    Format21c(4),
    Format21ih(4),
    Format21lh(4),
    Format21s(4),
    Format21t(4),
    Format22b(4),
    Format22c(4),
    Format22cs(4),
    Format22s(4),
    Format22t(4),
    Format22x(4),
    Format23x(4),
    Format25x(4),
    Format30t(6),
    Format31c(6),
    Format31i(6),
    Format31t(6),
    Format32x(6),
    Format35c(6),
    Format35mi(6),
    Format35ms(6),
    Format3rc(6),
    Format3rmi(6),
    Format3rms(6),
    Format51l(10),
    ArrayPayload(-1, true),
    PackedSwitchPayload(-1, true),
    SparseSwitchPayload(-1, true),
    UnresolvedOdexInstruction(-1);

    public final int size;
    public final boolean isPayloadFormat;

    private Format(int size) {
        this(size, false);
    }

    private Format(int size, boolean isPayloadFormat) {
        this.size = size;
        this.isPayloadFormat = isPayloadFormat;
    }
}
