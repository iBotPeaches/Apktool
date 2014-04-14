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

package org.jf.dexlib2.analysis.util;

import org.jf.dexlib2.analysis.TypeProto;
import org.jf.dexlib2.analysis.UnresolvedClassException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class TypeProtoUtils {
    /**
     * Get the chain of superclasses of the given class. The first element will be the immediate superclass followed by
     * it's superclass, etc. up to java.lang.Object.
     *
     * Returns an empty iterable if called on java.lang.Object or a primitive.
     *
     * If any class in the superclass chain can't be resolved, the iterable will return Ujava/lang/Object; to represent
     * the unknown class.
     *
     * @return An iterable containing the superclasses of this class.
     */
    @Nonnull
    public static Iterable<TypeProto> getSuperclassChain(@Nonnull final TypeProto typeProto) {
        return new Iterable<TypeProto>() {

            @Override public Iterator<TypeProto> iterator() {
                return new Iterator<TypeProto>() {
                    @Nullable private TypeProto type = getSuperclassAsTypeProto(typeProto);

                    @Override public boolean hasNext() {
                        return type != null;
                    }

                    @Override public TypeProto next() {
                        TypeProto type = this.type;
                        if (type == null) {
                            throw new NoSuchElementException();
                        }

                        this.type = getSuperclassAsTypeProto(type);
                        return type;
                    }

                    @Override public void remove() {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }

    @Nullable
    public static TypeProto getSuperclassAsTypeProto(@Nonnull TypeProto type) {
        try {
            String next = type.getSuperclass();
            if (next != null) {
                return type.getClassPath().getClass(next);
            } else {
                return null;
            }
        } catch (UnresolvedClassException ex) {
            return type.getClassPath().getUnknownClass();
        }
    }
}
