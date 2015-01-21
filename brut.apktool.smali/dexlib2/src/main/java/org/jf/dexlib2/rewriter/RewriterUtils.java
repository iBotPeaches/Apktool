/*
 * Copyright 2014, Google Inc.
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

package org.jf.dexlib2.rewriter;

import org.jf.dexlib2.base.reference.BaseTypeReference;
import org.jf.dexlib2.iface.reference.TypeReference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class RewriterUtils {
    @Nullable
    public static <T> T rewriteNullable(@Nonnull Rewriter<T> rewriter, @Nullable T value) {
        return value==null?null:rewriter.rewrite(value);
    }

    public static <T> Set<T> rewriteSet(@Nonnull final Rewriter<T> rewriter,
                                        @Nonnull final Set<? extends T> set) {
        return new AbstractSet<T>() {
            @Nonnull @Override public Iterator<T> iterator() {
                final Iterator<? extends T> iterator = set.iterator();
                return new Iterator<T>() {
                    @Override public boolean hasNext() {
                        return iterator.hasNext();
                    }

                    @Override public T next() {
                        return rewriteNullable(rewriter, iterator.next());
                    }

                    @Override public void remove() {
                        iterator.remove();
                    }
                };
            }

            @Override public int size() {
                return set.size();
            }
        };
    }

    public static <T> List<T> rewriteList(@Nonnull final Rewriter<T> rewriter,
                                        @Nonnull final List<? extends T> list) {
        return new AbstractList<T>() {
            @Override public T get(int i) {
                return rewriteNullable(rewriter, list.get(i));
            }

            @Override public int size() {
                return list.size();
            }
        };
    }

    public static <T> Iterable<T> rewriteIterable(@Nonnull final Rewriter<T> rewriter,
                                                  @Nonnull final Iterable<? extends T> iterable) {
        return new Iterable<T>() {
            @Override public Iterator<T> iterator() {
                final Iterator<? extends T> iterator = iterable.iterator();
                return new Iterator<T>() {
                    @Override public boolean hasNext() {
                        return iterator.hasNext();
                    }

                    @Override public T next() {
                        return rewriteNullable(rewriter, iterator.next());
                    }

                    @Override public void remove() {
                        iterator.remove();
                    }
                };
            }
        };
    }

    public static TypeReference rewriteTypeReference(@Nonnull final Rewriter<String> typeRewriter,
                                                     @Nonnull final TypeReference typeReference) {
        return new BaseTypeReference() {
            @Nonnull @Override public String getType() {
                return typeRewriter.rewrite(typeReference.getType());
            }
        };
    }
}


