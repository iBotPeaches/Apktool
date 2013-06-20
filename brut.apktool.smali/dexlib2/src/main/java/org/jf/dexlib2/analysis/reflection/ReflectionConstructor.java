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

package org.jf.dexlib2.analysis.reflection;

import com.google.common.collect.ImmutableSet;
import org.jf.dexlib2.analysis.reflection.util.ReflectionUtils;
import org.jf.dexlib2.base.BaseMethodParameter;
import org.jf.dexlib2.base.reference.BaseMethodReference;
import org.jf.dexlib2.iface.Annotation;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.MethodParameter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.util.AbstractList;
import java.util.List;
import java.util.Set;

public class ReflectionConstructor extends BaseMethodReference implements Method {
    private final Constructor constructor;

    public ReflectionConstructor(Constructor constructor) {
        this.constructor = constructor;
    }

    @Nonnull @Override public List<? extends MethodParameter> getParameters() {
        final Constructor method = this.constructor;
        return new AbstractList<MethodParameter>() {
            private final Class[] parameters = method.getParameterTypes();

            @Override public MethodParameter get(final int index) {
                return new BaseMethodParameter() {
                    @Nonnull @Override public Set<? extends Annotation> getAnnotations() {
                        return ImmutableSet.of();
                    }

                    @Nullable @Override public String getName() {
                        return null;
                    }

                    @Nonnull @Override public String getType() {
                        return ReflectionUtils.javaToDexName(parameters[index].getName());
                    }
                };
            }

            @Override public int size() {
                return parameters.length;
            }
        };
    }

    @Override public int getAccessFlags() {
        return constructor.getModifiers();
    }

    @Nonnull @Override public Set<? extends Annotation> getAnnotations() {
        return ImmutableSet.of();
    }

    @Nullable @Override public MethodImplementation getImplementation() {
        return null;
    }

    @Nonnull @Override public String getDefiningClass() {
        return ReflectionUtils.javaToDexName(constructor.getDeclaringClass().getName());
    }

    @Nonnull @Override public String getName() {
        return constructor.getName();
    }

    @Nonnull @Override public List<String> getParameterTypes() {
        return new AbstractList<String>() {
            private final List<? extends MethodParameter> parameters = getParameters();

            @Override public String get(int index) {
                return parameters.get(index).getType();
            }

            @Override public int size() {
                return parameters.size();
            }
        };
    }

    @Nonnull @Override public String getReturnType() {
        return "V";
    }
}
