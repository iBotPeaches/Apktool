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

package org.jf.baksmali.Adaptors.Debug;

import org.jf.baksmali.Adaptors.RegisterFormatter;
import org.jf.dexlib2.iface.debug.RestartLocal;
import org.jf.util.IndentingWriter;

import javax.annotation.Nonnull;
import java.io.IOException;

public class RestartLocalMethodItem extends DebugMethodItem {
    @Nonnull private final RestartLocal restartLocal;
    @Nonnull private final RegisterFormatter registerFormatter;

    public RestartLocalMethodItem(int codeAddress, int sortOrder, @Nonnull RegisterFormatter registerFormatter,
                              @Nonnull RestartLocal restartLocal) {
        super(codeAddress, sortOrder);
        this.restartLocal = restartLocal;
        this.registerFormatter = registerFormatter;
    }

    @Override
    public boolean writeTo(IndentingWriter writer) throws IOException {
        writer.write(".restart local ");
        registerFormatter.writeTo(writer, restartLocal.getRegister());

        String name = restartLocal.getName();
        String type = restartLocal.getType();
        String signature = restartLocal.getSignature();
        if (name != null || type != null || signature != null) {
            writer.write("    # ");
            LocalFormatter.writeLocal(writer, name, type, signature);
        }
        return true;
    }
}
