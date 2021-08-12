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

package org.jf.dexlib2.iface;

import org.jf.dexlib2.iface.debug.LocalInfo;
import org.jf.dexlib2.iface.reference.TypeReference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;

/**
 * This class represents a method parameter.
 *
 * It also acts as a TypeReference to the type of this parameter. Any equality/comparison is based on its identity as a
 * TypeReference, and should not take into account any details other than the parameter type.
 *
 * It also acts as a LocalInfo object, and conceptually defines the debug information for any parameter register at the
 * beginning of the method.
 */
public interface MethodParameter extends TypeReference, LocalInfo {
    /**
     * The type of this method parameter.
     *
     * This may be any type, including primitive or array types, other than the void (V) type.
     *
     * @return The type of this method parameter
     */
    @Nonnull String getType();

    /**
     * Gets a set of the annotations that are applied to this parameter.
     *
     * The annotations in the returned set are guaranteed to have unique types.
     *
     * @return A set of the annotations that are applied to this parameter
     */
    @Nonnull Set<? extends Annotation> getAnnotations();

    /**
     * Gets the name of this parameter, if available.
     *
     * @return The name of this parameter, or null if the name is not available.
     */
    @Nullable String getName();

    /**
     * Gets the signature of this parameter, if available.
     *
     * The signature of a parameter is defined to be the concatenated version of the dalvik.annotation.Signature
     * annotation applied to this parameter, or null if there is no dalvik.annotation.Signature annotation.
     *
     * @return The signature of this parameter, or null if not available
     */
    @Nullable String getSignature();
}
