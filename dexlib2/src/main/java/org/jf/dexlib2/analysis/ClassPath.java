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

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.analysis.reflection.ReflectionClassDef;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.immutable.ImmutableDexFile;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

public class ClassPath {
    @Nonnull private final TypeProto unknownClass;
    @Nonnull private List<ClassProvider> classProviders;
    private final boolean checkPackagePrivateAccess;
    public final int oatVersion;

    public static final int NOT_ART = -1;
    public static final int NOT_SPECIFIED = -2;

    /**
     * Creates a new ClassPath instance that can load classes from the given providers
     *
     * @param classProviders A varargs array of ClassProviders. When loading a class, these providers will be searched
     *                       in order
     */
    public ClassPath(ClassProvider... classProviders) throws IOException {
        this(Arrays.asList(classProviders), false, NOT_ART);
    }

    /**
     * Creates a new ClassPath instance that can load classes from the given providers
     *
     * @param classProviders An iterable of ClassProviders. When loading a class, these providers will be searched in
     *                       order
     */
    public ClassPath(Iterable<ClassProvider> classProviders) throws IOException {
        this(classProviders, false, NOT_ART);
    }

    /**
     * Creates a new ClassPath instance that can load classes from the given providers
     *
     * @param classProviders An iterable of ClassProviders. When loading a class, these providers will be searched in
     *                       order
     * @param checkPackagePrivateAccess Whether checkPackagePrivateAccess is needed, enabled for ONLY early API 17 by
     *                                  default
     * @param oatVersion The applicable oat version, or NOT_ART
     */
    public ClassPath(@Nonnull Iterable<? extends ClassProvider> classProviders, boolean checkPackagePrivateAccess,
                     int oatVersion) {
        // add fallbacks for certain special classes that must be present
        unknownClass = new UnknownClassProto(this);
        loadedClasses.put(unknownClass.getType(), unknownClass);
        this.checkPackagePrivateAccess = checkPackagePrivateAccess;
        this.oatVersion = oatVersion;

        loadPrimitiveType("Z");
        loadPrimitiveType("B");
        loadPrimitiveType("S");
        loadPrimitiveType("C");
        loadPrimitiveType("I");
        loadPrimitiveType("J");
        loadPrimitiveType("F");
        loadPrimitiveType("D");
        loadPrimitiveType("L");

        this.classProviders = Lists.newArrayList(classProviders);
        this.classProviders.add(getBasicClasses());
    }

    private void loadPrimitiveType(String type) {
        loadedClasses.put(type, new PrimitiveProto(this, type));
    }

    private static ClassProvider getBasicClasses() {
        // fallbacks for some special classes that we assume are present
        return new DexClassProvider(new ImmutableDexFile(Opcodes.getDefault(), ImmutableSet.of(
                new ReflectionClassDef(Class.class),
                new ReflectionClassDef(Cloneable.class),
                new ReflectionClassDef(Object.class),
                new ReflectionClassDef(Serializable.class),
                new ReflectionClassDef(String.class),
                new ReflectionClassDef(Throwable.class))));
    }

    public boolean isArt() {
        return oatVersion != NOT_ART;
    }

    @Nonnull
    public TypeProto getClass(@Nonnull CharSequence type) {
        return loadedClasses.getUnchecked(type.toString());
    }

    private final CacheLoader<String, TypeProto> classLoader = new CacheLoader<String, TypeProto>() {
        @Override public TypeProto load(String type) throws Exception {
            if (type.charAt(0) == '[') {
                return new ArrayProto(ClassPath.this, type);
            } else {
                return new ClassProto(ClassPath.this, type);
            }
        }
    };

    @Nonnull private LoadingCache<String, TypeProto> loadedClasses = CacheBuilder.newBuilder().build(classLoader);

    @Nonnull
    public ClassDef getClassDef(String type) {
        for (ClassProvider provider: classProviders) {
            ClassDef classDef = provider.getClassDef(type);
            if (classDef != null) {
                return classDef;
            }
        }
        throw new UnresolvedClassException("Could not resolve class %s", type);
    }

    @Nonnull
    public TypeProto getUnknownClass() {
        return unknownClass;
    }

    public boolean shouldCheckPackagePrivateAccess() {
        return checkPackagePrivateAccess;
    }

    private final Supplier<OdexedFieldInstructionMapper> fieldInstructionMapperSupplier = Suppliers.memoize(
            new Supplier<OdexedFieldInstructionMapper>() {
                @Override public OdexedFieldInstructionMapper get() {
                    return new OdexedFieldInstructionMapper(isArt());
                }
            });

    @Nonnull
    public OdexedFieldInstructionMapper getFieldInstructionMapper() {
        return fieldInstructionMapperSupplier.get();
    }
}
