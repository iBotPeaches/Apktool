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

package org.jf.dexlib2.writer.pool;

import com.google.common.collect.Ordering;
import org.jf.dexlib2.iface.reference.MethodReference;
import org.jf.dexlib2.util.MethodUtil;
import org.jf.dexlib2.writer.pool.ProtoPool.Key;
import org.jf.dexlib2.writer.ProtoSection;
import org.jf.util.CharSequenceUtils;
import org.jf.util.CollectionUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

public class ProtoPool extends BaseIndexPool<Key>
        implements ProtoSection<CharSequence, CharSequence, Key, TypeListPool.Key<? extends Collection<? extends CharSequence>>> {
    @Nonnull private final StringPool stringPool;
    @Nonnull private final TypePool typePool;
    @Nonnull private final TypeListPool typeListPool;

    public ProtoPool(@Nonnull StringPool stringPool, @Nonnull TypePool typePool,
                     @Nonnull TypeListPool typeListPool) {
        this.stringPool = stringPool;
        this.typePool = typePool;
        this.typeListPool = typeListPool;
    }

    public void intern(@Nonnull MethodReference method) {
        // We can't use method directly, because it is likely a full MethodReference. We use a wrapper that computes
        // hashCode and equals based only on the prototype fields
        Key key = new Key(method);
        Integer prev = internedItems.put(key, 0);
        if (prev == null) {
            stringPool.intern(key.getShorty());
            typePool.intern(method.getReturnType());
            typeListPool.intern(method.getParameterTypes());
        }
    }

    @Nonnull @Override public CharSequence getShorty(@Nonnull Key key) {
        return key.getShorty();
    }

    @Nonnull @Override public CharSequence getReturnType(@Nonnull Key key) {
        return key.getReturnType();
    }

    @Nullable @Override public TypeListPool.Key<List<? extends CharSequence>> getParameters(@Nonnull Key key) {
        return new TypeListPool.Key<List<? extends CharSequence>>(key.getParameters());
    }

    public static class Key implements Comparable<Key> {
        @Nonnull private final MethodReference method;

        public Key(@Nonnull MethodReference method) {
            this.method = method;
        }

        @Nonnull public String getReturnType() { return method.getReturnType(); }
        @Nonnull public List<? extends CharSequence> getParameters() {
            return method.getParameterTypes();
        }

        public String getShorty() {
            return MethodUtil.getShorty(method.getParameterTypes(), method.getReturnType());
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append('(');
            for (CharSequence paramType: getParameters()) {
                sb.append(paramType);
            }
            sb.append(')');
            sb.append(getReturnType());
            return sb.toString();
        }

        @Override
        public int hashCode() {
            int hashCode = getReturnType().hashCode();
            return hashCode*31 + CharSequenceUtils.listHashCode(getParameters());
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (o instanceof Key) {
                Key other = (Key)o;
                return getReturnType().equals(other.getReturnType()) &&
                        CharSequenceUtils.listEquals(getParameters(), other.getParameters());
            }
            return false;
        }

        @Override
        public int compareTo(@Nonnull Key o) {
            int res = getReturnType().compareTo(o.getReturnType());
            if (res != 0) return res;
            return CollectionUtils.compareAsIterable(Ordering.usingToString(), getParameters(), o.getParameters());
        }
    }
}
