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

import com.google.common.collect.*;
import org.jf.dexlib2.base.reference.BaseTypeReference;
import org.jf.dexlib2.iface.Annotation;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.Field;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

class PoolClassDef extends BaseTypeReference implements ClassDef {
    @Nonnull final ClassDef classDef;
    @Nonnull final TypeListPool.Key<List<String>> interfaces;
    @Nonnull final ImmutableSortedSet<Field> staticFields;
    @Nonnull final ImmutableSortedSet<Field> instanceFields;
    @Nonnull final ImmutableSortedSet<PoolMethod> directMethods;
    @Nonnull final ImmutableSortedSet<PoolMethod> virtualMethods;

    int classDefIndex = DexPool.NO_INDEX;
    int encodedArrayOffset = DexPool.NO_OFFSET;
    int annotationDirectoryOffset = DexPool.NO_OFFSET;

    PoolClassDef(@Nonnull ClassDef classDef) {
        this.classDef = classDef;

        interfaces = new TypeListPool.Key<List<String>>(ImmutableList.copyOf(classDef.getInterfaces()));
        staticFields = ImmutableSortedSet.copyOf(classDef.getStaticFields());
        instanceFields = ImmutableSortedSet.copyOf(classDef.getInstanceFields());
        directMethods = ImmutableSortedSet.copyOf(
                Iterables.transform(classDef.getDirectMethods(), PoolMethod.TRANSFORM));
        virtualMethods = ImmutableSortedSet.copyOf(
                Iterables.transform(classDef.getVirtualMethods(), PoolMethod.TRANSFORM));
    }

    @Nonnull @Override public String getType() {
        return classDef.getType();
    }

    @Override public int getAccessFlags() {
        return classDef.getAccessFlags();
    }

    @Nullable @Override public String getSuperclass() {
        return classDef.getSuperclass();
    }

    @Nonnull @Override public List<String> getInterfaces() {
        return interfaces.types;
    }

    @Nullable @Override public String getSourceFile() {
        return classDef.getSourceFile();
    }

    @Nonnull @Override public Set<? extends Annotation> getAnnotations() {
        return classDef.getAnnotations();
    }

    @Nonnull @Override public SortedSet<Field> getStaticFields() {
        return staticFields;
    }

    @Nonnull @Override public SortedSet<Field> getInstanceFields() {
        return instanceFields;
    }

    @Nonnull @Override public Collection<Field> getFields() {
        return new AbstractCollection<Field>() {
            @Nonnull @Override public Iterator<Field> iterator() {
                return Iterators.mergeSorted(
                        ImmutableList.of(staticFields.iterator(), instanceFields.iterator()),
                        Ordering.natural());
            }

            @Override public int size() {
                return staticFields.size() + instanceFields.size();
            }
        };
    }

    @Nonnull @Override public SortedSet<PoolMethod> getDirectMethods() {
        return directMethods;
    }

    @Nonnull @Override public SortedSet<PoolMethod> getVirtualMethods() {
        return virtualMethods;
    }

    @Nonnull @Override public Collection<PoolMethod> getMethods() {
        return new AbstractCollection<PoolMethod>() {
            @Nonnull @Override public Iterator<PoolMethod> iterator() {
                return Iterators.mergeSorted(
                        ImmutableList.of(directMethods.iterator(), virtualMethods.iterator()),
                        Ordering.natural());
            }

            @Override public int size() {
                return directMethods.size() + virtualMethods.size();
            }
        };
    }
}
