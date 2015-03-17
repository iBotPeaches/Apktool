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

import org.jf.dexlib2.iface.reference.MethodReference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

/**
 * This class represents a specific method definition in a class.
 *
 * It also acts as a MethodReference to itself. Any equality/comparison is based on its identity as a MethodReference,
 * and shouldn't take into account any non-MethodReference specifics of this method.
 */
public interface Method extends MethodReference, Member {
    /**
     * Gets the type of the class that defines this method.
     *
     * @return The type of the class that defines this method
     */
    @Override @Nonnull String getDefiningClass();

    /**
     * Gets the name of this method.
     *
     * @return The name of this method
     */
    @Override @Nonnull String getName();

    /**
     * Gets a list of the parameters of this method.
     *
     * As per the MethodReference interface, the MethodParameter objects contained in the returned list also act
     * as a simple reference to the type of the parameter. However, the MethodParameter object can also contain
     * additional information about the parameter.
     *
     * Note: In some implementations, the returned list is likely to *not* provide efficient random access.
     *
     * @return A list of MethodParameter objects, representing the parameters of this method.
     */
    @Nonnull List<? extends MethodParameter> getParameters();

    /**
     * Gets the return type of this method.
     *
     * @return The return type of this method.
     */
    @Override @Nonnull String getReturnType();

    /**
     * Gets the access flags for this method.
     *
     * This will be a combination of the AccessFlags.* flags that are marked as compatible for use with a method.
     *
     * @return The access flags for this method
     */
    @Override int getAccessFlags();

    /**
     * Gets a set of the annotations that are applied to this method.
     *
     * The annotations in the returned set are guaranteed to have unique types.
     *
     * @return A set of the annotations that are applied to this method
     */
    @Override @Nonnull Set<? extends Annotation> getAnnotations();

    /**
     * Gets a MethodImplementation object that defines the implementation of the method.
     *
     * If this is an abstract method in an abstract class, or an interface method in an interface definition, then the
     * method has no implementation, and this will return null.
     *
     * @return A MethodImplementation object defining the implementation of this method, or null if the method has no
     * implementation
     */
    @Nullable MethodImplementation getImplementation();
}
