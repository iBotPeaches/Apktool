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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.analysis.reflection.ReflectionClassDef;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.DexFile;
import org.jf.dexlib2.immutable.ImmutableDexFile;
import org.jf.util.ExceptionWithContext;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClassPath {
    @Nonnull private final TypeProto unknownClass;
    @Nonnull private HashMap<String, ClassDef> availableClasses = Maps.newHashMap();
    private int api;

    /**
     * Creates a new ClassPath instance that can load classes from the given dex files
     *
     * @param classPath An array of DexFile objects. When loading a class, these dex files will be searched in order
     */
    public ClassPath(DexFile... classPath) throws IOException {
        this(Lists.newArrayList(classPath), 15);
    }

    /**
     * Creates a new ClassPath instance that can load classes from the given dex files
     *
     * @param classPath An iterable of DexFile objects. When loading a class, these dex files will be searched in order
     * @param api API level
     */
    public ClassPath(@Nonnull Iterable<DexFile> classPath, int api) {
        // add fallbacks for certain special classes that must be present
        Iterable<DexFile> dexFiles = Iterables.concat(classPath, Lists.newArrayList(getBasicClasses()));

        unknownClass = new UnknownClassProto(this);
        loadedClasses.put(unknownClass.getType(), unknownClass);
        this.api = api;

        loadPrimitiveType("Z");
        loadPrimitiveType("B");
        loadPrimitiveType("S");
        loadPrimitiveType("C");
        loadPrimitiveType("I");
        loadPrimitiveType("J");
        loadPrimitiveType("F");
        loadPrimitiveType("D");
        loadPrimitiveType("L");

        for (DexFile dexFile: dexFiles) {
            for (ClassDef classDef: dexFile.getClasses()) {
                ClassDef prev = availableClasses.get(classDef.getType());
                if (prev == null) {
                    availableClasses.put(classDef.getType(), classDef);
                }
            }
        }
    }

    private void loadPrimitiveType(String type) {
        loadedClasses.put(type, new PrimitiveProto(this, type));
    }

    private static DexFile getBasicClasses() {
        // fallbacks for some special classes that we assume are present
        return new ImmutableDexFile(ImmutableSet.of(
                new ReflectionClassDef(Class.class),
                new ReflectionClassDef(Cloneable.class),
                new ReflectionClassDef(Object.class),
                new ReflectionClassDef(Serializable.class),
                new ReflectionClassDef(String.class),
                new ReflectionClassDef(Throwable.class)));
    }

    @Nonnull
    public TypeProto getClass(CharSequence type) {
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
        ClassDef ret = availableClasses.get(type);
        if (ret == null) {
            throw new UnresolvedClassException("Could not resolve class %s", type);
        }
        return ret;
    }

    @Nonnull
    public TypeProto getUnknownClass() {
        return unknownClass;
    }

    public int getApi() {
        return api;
    }

    @Nonnull
    public static ClassPath fromClassPath(Iterable<String> classPathDirs, Iterable<String> classPath, DexFile dexFile,
                                          int api) {
        ArrayList<DexFile> dexFiles = Lists.newArrayList();

        for (String classPathEntry: classPath) {
            dexFiles.add(loadClassPathEntry(classPathDirs, classPathEntry, api));
        }
        dexFiles.add(dexFile);
        return new ClassPath(dexFiles, api);
    }

    private static final Pattern dalvikCacheOdexPattern = Pattern.compile("@([^@]+)@classes.dex$");

    @Nonnull
    private static DexFile loadClassPathEntry(@Nonnull Iterable<String> classPathDirs,
                                              @Nonnull String bootClassPathEntry, int api) {
        File rawEntry = new File(bootClassPathEntry);
        // strip off the path - we only care about the filename
        String entryName = rawEntry.getName();

        // if it's a dalvik-cache entry, grab the name of the jar/apk
        if (entryName.endsWith("@classes.dex")) {
            Matcher m = dalvikCacheOdexPattern.matcher(entryName);

            if (!m.find()) {
                throw new ExceptionWithContext(String.format("Cannot parse dependency value %s", bootClassPathEntry));
            }

            entryName = m.group(1);
        }

        int extIndex = entryName.lastIndexOf(".");

        String baseEntryName;
        if (extIndex == -1) {
            baseEntryName = entryName;
        } else {
            baseEntryName = entryName.substring(0, extIndex);
        }

        for (String classPathDir: classPathDirs) {
            for (String ext: new String[]{"", ".odex", ".jar", ".apk", ".zip"}) {
                File file = new File(classPathDir, baseEntryName + ext);

                if (file.exists() && file.isFile()) {
                    if (!file.canRead()) {
                        System.err.println(String.format(
                                "warning: cannot open %s for reading. Will continue looking.", file.getPath()));
                    } else {
                        try {
                            return DexFileFactory.loadDexFile(file, api);
                        } catch (DexFileFactory.NoClassesDexException ex) {
                            // ignore and continue
                        } catch (Exception ex) {
                            throw ExceptionWithContext.withContext(ex,
                                    "Error while reading boot class path entry \"%s\"", bootClassPathEntry);
                        }
                    }
                }
            }
        }
        throw new ExceptionWithContext("Cannot locate boot class path file %s", bootClassPathEntry);
    }
}
