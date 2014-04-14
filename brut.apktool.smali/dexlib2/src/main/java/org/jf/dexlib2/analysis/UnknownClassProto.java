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

package org.jf.dexlib2.analysis;

import org.jf.dexlib2.iface.reference.FieldReference;
import org.jf.dexlib2.iface.reference.MethodReference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class UnknownClassProto implements TypeProto {
    @Nonnull protected final ClassPath classPath;

    public UnknownClassProto(@Nonnull ClassPath classPath) {
        this.classPath = classPath;
    }

    @Override public String toString() { return "Ujava/lang/Object;"; }
    @Nonnull @Override public ClassPath getClassPath() { return classPath; }
    @Nullable @Override public String getSuperclass() { return null; }
    @Override public boolean isInterface() { return false; }
    @Override public boolean implementsInterface(@Nonnull String iface) { return false; }

    @Nonnull @Override public TypeProto getCommonSuperclass(@Nonnull TypeProto other) {
        if (other.getType().equals("Ljava/lang/Object;")) {
            return other;
        }
        if (other instanceof ArrayProto) {
            // if it's an array class, it's safe to assume this unknown class isn't related, and so
            // java.lang.Object is the only possible superclass
            return classPath.getClass("Ljava/lang/Object;");
        }
        return this;
    }

    @Nonnull @Override public String getType() {
        // use the otherwise used U prefix for an unknown/unresolvable class
        return "Ujava/lang/Object;";
    }

    @Override
    @Nullable
    public FieldReference getFieldByOffset(int fieldOffset) {
        return classPath.getClass("Ljava/lang/Object;").getFieldByOffset(fieldOffset);
    }

    @Override
    @Nullable
    public MethodReference getMethodByVtableIndex(int vtableIndex) {
        return classPath.getClass("Ljava/lang/Object;").getMethodByVtableIndex(vtableIndex);
    }
}
