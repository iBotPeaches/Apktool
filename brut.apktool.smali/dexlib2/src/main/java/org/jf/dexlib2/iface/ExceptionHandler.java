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

import org.jf.dexlib2.iface.reference.TypeReference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * This class represents an individual exception handler entry, in a try block.
 */
public interface ExceptionHandler extends Comparable<ExceptionHandler> {
    /**
     * Gets the type of exception that is handled by this handler.
     *
     * @return The type of exception that is handled by this handler, or null if this is a catch-all handler.
     */
    @Nullable String getExceptionType();

    /**
     * Gets the type of exception that is handled by this handler.
     *
     * @return A TypeReference to the type of exception that is handled by this handler, or null if this is a
     * catch-all handler.
     */
    @Nullable TypeReference getExceptionTypeReference();

    /**
     * Gets the code offset of the handler.
     *
     * @return The offset of the handler from the the beginning of the bytecode for the method. The offset will be in
     * terms of 16-bit code units
     */
    int getHandlerCodeAddress();

    /**
     * Returns a hashcode for this ExceptionHandler.
     *
     * This hashCode is defined to be the following:
     *
     * <pre>
     * {@code
     * String exceptionType = getExceptionType();
     * int hashCode = exceptionType==null?0:exceptionType.hashCode();
     * return hashCode*31 + getHandlerCodeAddress();
     * }</pre>
     *
     * @return The hash code value for this ExceptionHandler
     */
    @Override int hashCode();

    /**
     * Compares this ExceptionHandler to another ExceptionHandler for equality.
     *
     * This ExceptionHandler is equal to another ExceptionHandler if all of it's "fields" are equal. That is, if
     * the return values of getExceptionType() and getHandlerCodeAddress() are both equal.
     *
     * @param o The object to be compared for equality with this ExceptionHandler
     * @return true if the specified object is equal to this ExceptionHandler
     */
    @Override boolean equals(@Nullable Object o);

    /**
     * Compare this ExceptionHandler to another ExceptionHandler.
     *
     * The comparison is based on the comparison of the return values of getExceptionType() and
     * getHandlerCodeAddress() in that order. A null value for getExceptionType() compares after a non-null value.
     *
     * @param o The ExceptionHandler to compare with this ExceptionHandler
     * @return An integer representing the result of the comparison
     */
    @Override int compareTo(@Nonnull ExceptionHandler o);
}
