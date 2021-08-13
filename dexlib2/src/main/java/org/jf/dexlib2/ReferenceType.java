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

package org.jf.dexlib2;

import org.jf.dexlib2.iface.reference.*;
import org.jf.util.ExceptionWithContext;

public final class ReferenceType {
    public static final int STRING = 0;
    public static final int TYPE = 1;
    public static final int FIELD = 2;
    public static final int METHOD = 3;
    public static final int METHOD_PROTO = 4;
    public static final int CALL_SITE = 5;
    public static final int METHOD_HANDLE = 6;
    public static final int NONE = 7;

    public static int getReferenceType(Reference reference) {
        if (reference instanceof StringReference) {
            return STRING;
        } else if (reference instanceof TypeReference) {
            return TYPE;
        } else if (reference instanceof FieldReference) {
            return FIELD;
        } else if (reference instanceof MethodReference) {
            return METHOD;
        } else if (reference instanceof MethodProtoReference) {
            return METHOD_PROTO;
        } else if (reference instanceof CallSiteReference) {
            return CALL_SITE;
        } else if (reference instanceof MethodHandleReference) {
            return METHOD_HANDLE;
        } else {
            throw new IllegalStateException("Invalid reference");
        }
    }

    /**
     * Validate a specific reference type. Note that the NONE placeholder is specifically not considered valid here.
     *
     * @throws InvalidReferenceTypeException
     */
    public static void validateReferenceType(int referenceType) {
        if (referenceType < 0 || referenceType > 4) {
            throw new InvalidReferenceTypeException(referenceType);
        }
    }

    public static class InvalidReferenceTypeException extends ExceptionWithContext {
        private final int referenceType;

        public InvalidReferenceTypeException(int referenceType) {
            super("Invalid reference type: %d", referenceType);
            this.referenceType = referenceType;
        }

        public InvalidReferenceTypeException(int referenceType, String message, Object... formatArgs) {
            super(message, formatArgs);
            this.referenceType = referenceType;
        }

        public int getReferenceType() {
            return referenceType;
        }
    }

    private ReferenceType() {}
}