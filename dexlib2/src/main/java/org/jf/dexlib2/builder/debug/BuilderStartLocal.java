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

package org.jf.dexlib2.builder.debug;

import org.jf.dexlib2.DebugItemType;
import org.jf.dexlib2.builder.BuilderDebugItem;
import org.jf.dexlib2.iface.debug.StartLocal;
import org.jf.dexlib2.iface.reference.StringReference;
import org.jf.dexlib2.iface.reference.TypeReference;

import javax.annotation.Nullable;

public class BuilderStartLocal extends BuilderDebugItem implements StartLocal {
    private final int register;
    @Nullable private final StringReference name;
    @Nullable private final TypeReference type;
    @Nullable private final StringReference signature;

    public BuilderStartLocal(int register,
                             @Nullable StringReference name,
                             @Nullable TypeReference type,
                             @Nullable StringReference signature) {
        this.register = register;
        this.name = name;
        this.type = type;
        this.signature = signature;
    }

    @Override public int getRegister() { return register; }

    @Nullable @Override public StringReference getNameReference() { return name; }
    @Nullable @Override public TypeReference getTypeReference() { return type; }
    @Nullable @Override public StringReference getSignatureReference() { return signature; }

    @Nullable @Override public String getName() {
        return name==null?null:name.getString();
    }

    @Nullable @Override public String getType() {
        return type==null?null:type.getType();
    }

    @Nullable @Override public String getSignature() {
        return signature==null?null:signature.getString();
    }

    @Override public int getDebugItemType() { return DebugItemType.START_LOCAL; }
}
