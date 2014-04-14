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

import com.google.common.collect.ImmutableList;
import org.jf.dexlib2.writer.DexWriter;
import org.jf.dexlib2.writer.TypeListSection;
import org.jf.dexlib2.writer.pool.TypeListPool.Key;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Iterator;

public class TypeListPool extends BaseNullableOffsetPool<Key<? extends Collection<? extends CharSequence>>>
        implements TypeListSection<CharSequence, Key<? extends Collection<? extends CharSequence>>> {
    @Nonnull private final TypePool typePool;

    public TypeListPool(@Nonnull TypePool typePool) {
        this.typePool = typePool;
    }

    public void intern(@Nonnull Collection<? extends CharSequence> types) {
        if (types.size() > 0) {
            Key<? extends Collection<? extends CharSequence>> key = new Key<Collection<? extends CharSequence>>(types);
            Integer prev = internedItems.put(key, 0);
            if (prev == null) {
                for (CharSequence type: types) {
                    typePool.intern(type);
                }
            }
        }
    }

    @Nonnull @Override
    public Collection<? extends CharSequence> getTypes(Key<? extends Collection<? extends CharSequence>> typesKey) {
        if (typesKey == null) {
            return ImmutableList.of();
        }
        return typesKey.types;
    }

    @Override public int getNullableItemOffset(@Nullable Key<? extends Collection<? extends CharSequence>> key) {
        if (key == null || key.types.size() == 0) {
            return DexWriter.NO_OFFSET;
        } else {
            return super.getNullableItemOffset(key);
        }
    }

    public static class Key<TypeCollection extends Collection<? extends CharSequence>>
            implements Comparable<Key<? extends Collection<? extends CharSequence>>> {
        @Nonnull TypeCollection types;

        public Key(@Nonnull TypeCollection types) {
            this.types = types;
        }

        @Override
        public int hashCode() {
            int hashCode = 1;
            for (CharSequence type: types) {
                hashCode = hashCode*31 + type.toString().hashCode();
            }
            return hashCode;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof Key) {
                Key<? extends Collection<? extends CharSequence>> other =
                        (Key<? extends Collection<? extends CharSequence>>)o;
                if (types.size() != other.types.size()) {
                    return false;
                }
                Iterator<? extends CharSequence> otherTypes = other.types.iterator();
                for (CharSequence type: types) {
                    if (!type.toString().equals(otherTypes.next().toString())) {
                        return false;
                    }
                }
                return true;
            }
            return false;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (CharSequence type: types) {
                sb.append(type.toString());
            }
            return sb.toString();
        }

        @Override
        public int compareTo(Key<? extends Collection<? extends CharSequence>> o) {
            Iterator<? extends CharSequence> other = o.types.iterator();
            for (CharSequence type: types) {
                if (!other.hasNext()) {
                    return 1;
                }
                int comparison = type.toString().compareTo(other.next().toString());
                if (comparison != 0) {
                    return comparison;
                }
            }
            if (other.hasNext()) {
                return -1;
            }
            return 0;
        }
    }
}
