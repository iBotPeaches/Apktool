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

import com.google.common.base.Strings;
import org.jf.dexlib2.iface.reference.FieldReference;
import org.jf.dexlib2.iface.reference.MethodReference;
import org.jf.dexlib2.immutable.reference.ImmutableFieldReference;
import org.jf.dexlib2.util.TypeUtils;
import org.jf.util.ExceptionWithContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ArrayProto implements TypeProto {
    protected final ClassPath classPath;
    protected final int dimensions;
    protected final String elementType;

    public ArrayProto(@Nonnull ClassPath classPath, @Nonnull String type) {
        this.classPath = classPath;
        int i=0;
        while (type.charAt(i) == '[') {
            i++;
            if (i == type.length()) {
                throw new ExceptionWithContext("Invalid array type: %s", type);
            }
        }

        if (i == 0) {
            throw new ExceptionWithContext("Invalid array type: %s", type);
        }

        dimensions = i;
        elementType = type.substring(i);
    }

    @Override public String toString() { return getType(); }
    @Nonnull @Override public ClassPath getClassPath() { return classPath; }
    @Nonnull @Override public String getType() { return makeArrayType(elementType, dimensions); }
    public int getDimensions() { return dimensions; }
    @Override public boolean isInterface() { return false; }

    /**
     * @return The base element type of this array. E.g. This would return Ljava/lang/String; for [[Ljava/lang/String;
     */
    @Nonnull public String getElementType() { return elementType; }

    /**
     * @return The immediate element type of this array. E.g. This would return [Ljava/lang/String; for
     * [[Ljava/lang/String;
     */
    @Nonnull public String getImmediateElementType() {
        if (dimensions > 1) {
            return makeArrayType(elementType, dimensions-1);
        }
        return elementType;
    }

    @Override public boolean implementsInterface(@Nonnull String iface) {
        return iface.equals("Ljava/lang/Cloneable;") || iface.equals("Ljava/io/Serializable;");
    }

    @Nullable @Override
    public String getSuperclass() {
        return "Ljava/lang/Object;";
    }

    @Nonnull @Override
    public TypeProto getCommonSuperclass(@Nonnull TypeProto other) {
        if (other instanceof ArrayProto) {
            if (TypeUtils.isPrimitiveType(getElementType()) ||
                    TypeUtils.isPrimitiveType(((ArrayProto)other).getElementType())) {
                if (dimensions == ((ArrayProto)other).dimensions &&
                        getElementType().equals(((ArrayProto)other).getElementType())) {
                    return this;
                }
                return classPath.getClass("Ljava/lang/Object;");
            }

            if (dimensions == ((ArrayProto)other).dimensions) {
                TypeProto thisClass = classPath.getClass(elementType);
                TypeProto otherClass = classPath.getClass(((ArrayProto)other).elementType);
                TypeProto mergedClass = thisClass.getCommonSuperclass(otherClass);
                if (thisClass == mergedClass) {
                    return this;
                }
                if (otherClass == mergedClass) {
                    return other;
                }
                return classPath.getClass(makeArrayType(mergedClass.getType(), dimensions));
            }

            int dimensions = Math.min(this.dimensions, ((ArrayProto)other).dimensions);
            return classPath.getClass(makeArrayType("Ljava/lang/Object;", dimensions));
        }

        if (other instanceof ClassProto) {
            try {
                if (other.isInterface()) {
                    if (implementsInterface(other.getType())) {
                        return other;
                    }
                }
            } catch (UnresolvedClassException ex) {
                // ignore
            }
            return classPath.getClass("Ljava/lang/Object;");
        }

        // otherwise, defer to the other class' getCommonSuperclass
        return other.getCommonSuperclass(this);
    }

    private static final String BRACKETS = Strings.repeat("[", 256);

    @Nonnull
    private static String makeArrayType(@Nonnull String elementType, int dimensions) {
        return BRACKETS.substring(0, dimensions) + elementType;
    }


    @Override
    @Nullable
    public FieldReference getFieldByOffset(int fieldOffset) {
        if (fieldOffset==8) {
            return new ImmutableFieldReference(getType(), "length", "int");
        }
        return null;
    }

    @Override
    @Nullable
    public MethodReference getMethodByVtableIndex(int vtableIndex) {
        return classPath.getClass("Ljava/lang/Object;").getMethodByVtableIndex(vtableIndex);
    }
}
