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

package org.jf.dexlib2.immutable;

import com.google.common.collect.*;
import org.jf.dexlib2.base.reference.BaseTypeReference;
import org.jf.dexlib2.iface.Annotation;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.Field;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.util.FieldUtil;
import org.jf.dexlib2.util.MethodUtil;
import org.jf.util.ImmutableConverter;
import org.jf.util.ImmutableUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class ImmutableClassDef extends BaseTypeReference implements ClassDef {
    @Nonnull protected final String type;
    protected final int accessFlags;
    @Nullable protected final String superclass;
    @Nonnull protected final ImmutableList<String> interfaces;
    @Nullable protected final String sourceFile;
    @Nonnull protected final ImmutableSet<? extends ImmutableAnnotation> annotations;
    @Nonnull protected final ImmutableSortedSet<? extends ImmutableField> staticFields;
    @Nonnull protected final ImmutableSortedSet<? extends ImmutableField> instanceFields;
    @Nonnull protected final ImmutableSortedSet<? extends ImmutableMethod> directMethods;
    @Nonnull protected final ImmutableSortedSet<? extends ImmutableMethod> virtualMethods;

    public ImmutableClassDef(@Nonnull String type,
                             int accessFlags,
                             @Nullable String superclass,
                             @Nullable Collection<String> interfaces,
                             @Nullable String sourceFile,
                             @Nullable Collection<? extends Annotation> annotations,
                             @Nullable Iterable<? extends Field> fields,
                             @Nullable Iterable<? extends Method> methods) {
        if (fields == null) {
            fields = ImmutableList.of();
        }
        if (methods == null) {
            methods = ImmutableList.of();
        }

        this.type = type;
        this.accessFlags = accessFlags;
        this.superclass = superclass;
        this.interfaces = interfaces==null ? ImmutableList.<String>of() : ImmutableList.copyOf(interfaces);
        this.sourceFile = sourceFile;
        this.annotations = ImmutableAnnotation.immutableSetOf(annotations);
        this.staticFields = ImmutableField.immutableSetOf(Iterables.filter(fields, FieldUtil.FIELD_IS_STATIC));
        this.instanceFields = ImmutableField.immutableSetOf(Iterables.filter(fields, FieldUtil.FIELD_IS_INSTANCE));
        this.directMethods = ImmutableMethod.immutableSetOf(Iterables.filter(methods, MethodUtil.METHOD_IS_DIRECT));
        this.virtualMethods = ImmutableMethod.immutableSetOf(Iterables.filter(methods, MethodUtil.METHOD_IS_VIRTUAL));
    }

    public ImmutableClassDef(@Nonnull String type,
                             int accessFlags,
                             @Nullable String superclass,
                             @Nullable Collection<String> interfaces,
                             @Nullable String sourceFile,
                             @Nullable Collection<? extends Annotation> annotations,
                             @Nullable Iterable<? extends Field> staticFields,
                             @Nullable Iterable<? extends Field> instanceFields,
                             @Nullable Iterable<? extends Method> directMethods,
                             @Nullable Iterable<? extends Method> virtualMethods) {
        this.type = type;
        this.accessFlags = accessFlags;
        this.superclass = superclass;
        this.interfaces = interfaces==null ? ImmutableList.<String>of() : ImmutableList.copyOf(interfaces);
        this.sourceFile = sourceFile;
        this.annotations = ImmutableAnnotation.immutableSetOf(annotations);
        this.staticFields = ImmutableField.immutableSetOf(staticFields);
        this.instanceFields = ImmutableField.immutableSetOf(instanceFields);
        this.directMethods = ImmutableMethod.immutableSetOf(directMethods);
        this.virtualMethods = ImmutableMethod.immutableSetOf(virtualMethods);
    }

    public ImmutableClassDef(@Nonnull String type,
                             int accessFlags,
                             @Nullable String superclass,
                             @Nullable ImmutableList<String> interfaces,
                             @Nullable String sourceFile,
                             @Nullable ImmutableSet<? extends ImmutableAnnotation> annotations,
                             @Nullable ImmutableSortedSet<? extends ImmutableField> staticFields,
                             @Nullable ImmutableSortedSet<? extends ImmutableField> instanceFields,
                             @Nullable ImmutableSortedSet<? extends ImmutableMethod> directMethods,
                             @Nullable ImmutableSortedSet<? extends ImmutableMethod> virtualMethods) {
        this.type = type;
        this.accessFlags = accessFlags;
        this.superclass = superclass;
        this.interfaces = ImmutableUtils.nullToEmptyList(interfaces);
        this.sourceFile = sourceFile;
        this.annotations = ImmutableUtils.nullToEmptySet(annotations);
        this.staticFields = ImmutableUtils.nullToEmptySortedSet(staticFields);
        this.instanceFields = ImmutableUtils.nullToEmptySortedSet(instanceFields);
        this.directMethods = ImmutableUtils.nullToEmptySortedSet(directMethods);
        this.virtualMethods = ImmutableUtils.nullToEmptySortedSet(virtualMethods);
    }

    public static ImmutableClassDef of(ClassDef classDef) {
        if (classDef instanceof ImmutableClassDef) {
            return (ImmutableClassDef)classDef;
        }
        return new ImmutableClassDef(
                classDef.getType(),
                classDef.getAccessFlags(),
                classDef.getSuperclass(),
                classDef.getInterfaces(),
                classDef.getSourceFile(),
                classDef.getAnnotations(),
                classDef.getStaticFields(),
                classDef.getInstanceFields(),
                classDef.getDirectMethods(),
                classDef.getVirtualMethods());
    }

    @Nonnull @Override public String getType() { return type; }
    @Override public int getAccessFlags() { return accessFlags; }
    @Nullable @Override public String getSuperclass() { return superclass; }
    @Nonnull @Override public ImmutableList<String> getInterfaces() { return interfaces; }
    @Nullable @Override public String getSourceFile() { return sourceFile; }
    @Nonnull @Override public ImmutableSet<? extends ImmutableAnnotation> getAnnotations() { return annotations; }
    @Nonnull @Override public ImmutableSet<? extends ImmutableField> getStaticFields() { return staticFields; }
    @Nonnull @Override public ImmutableSet<? extends ImmutableField> getInstanceFields() { return instanceFields; }
    @Nonnull @Override public ImmutableSet<? extends ImmutableMethod> getDirectMethods() { return directMethods; }
    @Nonnull @Override public ImmutableSet<? extends ImmutableMethod> getVirtualMethods() { return virtualMethods; }

    @Nonnull
    @Override
    public Collection<? extends ImmutableField> getFields() {
        return new AbstractCollection<ImmutableField>() {
            @Nonnull
            @Override
            public Iterator<ImmutableField> iterator() {
                return Iterators.concat(staticFields.iterator(), instanceFields.iterator());
            }

            @Override public int size() {
                return staticFields.size() + instanceFields.size();
            }
        };
    }

    @Nonnull
    @Override
    public Collection<? extends ImmutableMethod> getMethods() {
        return new AbstractCollection<ImmutableMethod>() {
            @Nonnull
            @Override
            public Iterator<ImmutableMethod> iterator() {
                return Iterators.concat(directMethods.iterator(), virtualMethods.iterator());
            }

            @Override public int size() {
                return directMethods.size() + virtualMethods.size();
            }
        };
    }

    @Nonnull
    public static ImmutableSet<ImmutableClassDef> immutableSetOf(@Nullable Iterable<? extends ClassDef> iterable) {
        return CONVERTER.toSet(iterable);
    }

    private static final ImmutableConverter<ImmutableClassDef, ClassDef> CONVERTER =
            new ImmutableConverter<ImmutableClassDef, ClassDef>() {
                @Override
                protected boolean isImmutable(@Nonnull ClassDef item) {
                    return item instanceof ImmutableClassDef;
                }

                @Nonnull
                @Override
                protected ImmutableClassDef makeImmutable(@Nonnull ClassDef item) {
                    return ImmutableClassDef.of(item);
                }
            };
}
