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

package org.jf.smali;

import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.Ints;
import org.jf.dexlib2.base.BaseMethodParameter;
import org.jf.dexlib2.iface.Annotation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.Set;

public class SmaliMethodParameter extends BaseMethodParameter implements WithRegister {
    public final int register;
    @Nonnull public final String type;
    @Nonnull public Set<? extends Annotation> annotations;
    @Nullable public String name;

    public SmaliMethodParameter(int register, @Nonnull String type) {
        this.register = register;
        this.type = type;
        this.annotations = ImmutableSet.of();
    }

    @Override public int getRegister() { return register; }
    @Nonnull @Override public String getType() { return type; }
    @Nonnull @Override public Set<? extends Annotation> getAnnotations() { return annotations; }
    @Nullable @Override public String getName() { return name; }
    @Nullable @Override public String getSignature() { return null; }

    public static final Comparator<WithRegister> COMPARATOR = new Comparator<WithRegister>() {
        @Override public int compare(WithRegister o1, WithRegister o2) {
            return Ints.compare(o1.getRegister(), o2.getRegister());
        }
    };
}
