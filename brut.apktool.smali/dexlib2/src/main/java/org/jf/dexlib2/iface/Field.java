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

import org.jf.dexlib2.iface.reference.FieldReference;
import org.jf.dexlib2.iface.value.EncodedValue;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;

/**
 * This class represents a specific field definition in a class.
 *
 * It also acts as a FieldReference to itself. Any equality/comparison is based on its identity as a FieldReference,
 * and shouldn't take into account any non-FieldReference specifics of this field.
 */
public interface Field extends FieldReference, Member {
    /**
     * Gets the type of the class that defines this field.
     *
     * @return The type of the class that defines this field
     */
    @Override @Nonnull String getDefiningClass();

    /**
     * Gets the name of this field.
     *
     * @return The name of this field
     */
    @Override @Nonnull String getName();

    /**
     * Gets the type of this field.
     *
     * @return The type of this field
     */
    @Override @Nonnull String getType();

    /**
     * Gets the access flags for this field.
     *
     * This will be a combination of the AccessFlags.* flags that are marked as compatible for use with a field.
     *
     * @return The access flags for this field
     */
    @Override int getAccessFlags();

    /**
     * Gets the initial value for this field, if available.
     *
     * Only static field may have an initial value set, but are not required to have an initial value.
     *
     * @return The initial value for this field, or null if this field is not a static field, or if this static field
     * does not have an initial value.
     */
    @Nullable EncodedValue getInitialValue();

    /**
     * Gets a set of the annotations that are applied to this field.
     *
     * The annotations in the returned set are guaranteed to have unique types.
     *
     * @return A set of the annotations that are applied to this field
     */
    @Override @Nonnull Set<? extends Annotation> getAnnotations();
}
