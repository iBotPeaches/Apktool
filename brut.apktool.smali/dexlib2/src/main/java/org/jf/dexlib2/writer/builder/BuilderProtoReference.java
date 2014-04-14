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

package org.jf.dexlib2.writer.builder;

import com.google.common.collect.Ordering;
import org.jf.dexlib2.writer.DexWriter;
import org.jf.util.CharSequenceUtils;
import org.jf.util.CollectionUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class BuilderProtoReference implements BuilderProtoPool.ProtoKey, Comparable<BuilderProtoReference> {
    @Nonnull final BuilderStringReference shorty;
    @Nonnull final BuilderTypeList parameterTypes;
    @Nonnull final BuilderTypeReference returnType;
    int index = DexWriter.NO_INDEX;

    public BuilderProtoReference(@Nonnull BuilderStringReference shorty, @Nonnull BuilderTypeList parameterTypes,
                                 @Nonnull BuilderTypeReference returnType) {
        this.shorty = shorty;
        this.parameterTypes = parameterTypes;
        this.returnType = returnType;
    }

    @Nonnull @Override public List<? extends CharSequence> getParameterTypes() {
        return parameterTypes;
    }

    @Nonnull @Override public String getReturnType() {
        return returnType.getType();
    }

    @Override
    public int hashCode() {
        int hashCode = getReturnType().hashCode();
        return hashCode*31 + getParameterTypes().hashCode();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (o != null && o instanceof BuilderProtoReference) {
            BuilderProtoReference other = (BuilderProtoReference)o;
            return returnType.equals(other.returnType) &&
                   CharSequenceUtils.listEquals(parameterTypes, other.parameterTypes);
        }
        return false;
    }

    @Override
    public int compareTo(@Nonnull BuilderProtoReference o) {
        int res = returnType.compareTo(o.returnType);
        if (res != 0) return res;
        return CollectionUtils.compareAsIterable(Ordering.usingToString(), parameterTypes, o.parameterTypes);
    }
}
