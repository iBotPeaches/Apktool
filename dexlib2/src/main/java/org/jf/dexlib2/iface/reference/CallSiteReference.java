/*
 * Copyright 2018, Google Inc.
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

package org.jf.dexlib2.iface.reference;

import org.jf.dexlib2.iface.value.EncodedValue;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * This class represents a reference to a call site
 */
public interface CallSiteReference extends Reference {

    /**
     * Gets a name for this call site.
     *
     * This is an arbitrary synthetic string that serves to differentiate call sites that would otherwise be identical.
     *
     * It can be any arbitrary string, with the only requirement being that 2 different, but otherwise identical call
     * sites in the same dex file must not share the same name. Multiple non-identical call sites may use the same name
     * however.
     *
     * @return The name for this call site.
     */
    @Nonnull String getName();

    /**
     * Gets a reference to a method handle for the bootstrap linker method
     *
     * @return A MethodHandleReference to the bootstrap linker method
     */
    @Nonnull MethodHandleReference getMethodHandle();

    /**
     * @return A method name that the bootstrap linker should resolve.
     */
    @Nonnull String getMethodName();

    /**
     * @return A MethodProtoReference corresponding to the prototype of the method that the bootstrap linker should
     * resolve
     */
    @Nonnull MethodProtoReference getMethodProto();

    /**
     * @return A list of extra arguments to pass to the bootstrap linker
     */
    @Nonnull List<? extends EncodedValue> getExtraArguments();

    /**
     * Returns a hashcode for this CallSiteReference.
     *
     * This hashCode is defined to be the following:
     *
     * <pre>
     * {@code
     * int hashCode = getName().hashCode();
     * hashCode = hashCode*31 + getMethodHandle().hashCode();
     * hashCode = hashCode*31 + getMethodName().hashCode();
     * hashCode = hashCode*31 + getMethodProto().hashCode();
     * hashCode = hashCode*31 + getExtraArguments().hashCode();
     * }</pre>
     *
     * @return The hash code value for this MethodReference
     */
    @Override int hashCode();

    /**
     * Compares this CallSiteReference to another CallSiteReference for equality.
     *
     * This CallSiteReference is equal to another CallSiteReference if all of its fields are equal. That is, if
     * the return values of getMethodHandle(), getMethodName(), getMethodProto() and getExtraArguments() are all equal.
     *
     * @param o The object to be compared for equality with this CallSiteReference
     * @return true if the specified object is equal to this CallSiteReference
     */
    @Override boolean equals(@Nullable Object o);
}
