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

package org.jf.dexlib2.iface.reference;

/**
 * This class is the base interface for field/method/string/type references in a dex file.
 */
public interface Reference {

    /**
     * Verifies that this reference is valid.
     *
     * @throws InvalidReferenceException If the reference is not valid.
     */
    void validateReference() throws InvalidReferenceException;

    class InvalidReferenceException extends Exception {
        private final String invalidReferenceRepresentation;

        public InvalidReferenceException(String invalidReferenceRepresentation) {
            super("Invalid reference");
            this.invalidReferenceRepresentation = invalidReferenceRepresentation;
        }

        public InvalidReferenceException(String invalidReferenceRepresentation, String msg) {
            super(msg);
            this.invalidReferenceRepresentation = invalidReferenceRepresentation;
        }

        public InvalidReferenceException(String invalidReferenceRepresentation, String s, Throwable throwable) {
            super(s, throwable);
            this.invalidReferenceRepresentation = invalidReferenceRepresentation;
        }

        public InvalidReferenceException(String invalidReferenceRepresentation, Throwable throwable) {
            super(throwable);
            this.invalidReferenceRepresentation = invalidReferenceRepresentation;
        }

        /**
         * @return A string representation of the invalid reference. This should be a human-readable string that gives
         * enough information to identify the reference in question.
         *
         * The format of the string is not specified, although as an illustrative example "string@123" could be
         * used for a reference to the string at index 123.
         */
        public String getInvalidReferenceRepresentation() {
            return invalidReferenceRepresentation;
        }
    }
}
