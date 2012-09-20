/*
 * [The "BSD licence"]
 * Copyright (c) 2011 Ben Gruver
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.jf.dexlib.Code;

import java.util.HashMap;

public enum VerificationErrorType {
    None(0, "no-error"),
    Generic(1, "generic-error"),
    NoClass(2, "no-such-class"),
    NoField(3, "no-such-field"),
    NoMethod(4, "no-such-method"),
    AccessClass(5, "illegal-class-access"),
    AccessField(6, "illegal-field-access"),
    AccessMethod(7, "illegal-method-access"),
    ClassChange(8, "class-change-error"),
    Instantiation(9, "instantiation-error");

    private static HashMap<String, VerificationErrorType> verificationErrorTypesByName;

    static {
        verificationErrorTypesByName = new HashMap<String, VerificationErrorType>();

        for (VerificationErrorType verificationErrorType: VerificationErrorType.values()) {
            verificationErrorTypesByName.put(verificationErrorType.getName(), verificationErrorType);
        }
    }

    private int value;
    private String name;
    private VerificationErrorType(int value, String name) {
        this.value = value;
        this.name = name;
    }

    public int getValue() {
        return value;
    }

    public String getName() {
        return name;
    }

    public static VerificationErrorType fromString(String validationErrorType) {
        return verificationErrorTypesByName.get(validationErrorType);
    }

    public static VerificationErrorType getValidationErrorType(int validationErrorType) {
        switch (validationErrorType) {
            case 0:
                return None;
            case 1:
                return Generic;
            case 2:
                return NoClass;
            case 3:
                return NoField;
            case 4:
                return NoMethod;
            case 5:
                return AccessClass;
            case 6:
                return AccessField;
            case 7:
                return AccessMethod;
            case 8:
                return ClassChange;
            case 9:
                return Instantiation;
        }
        return null;
    }
}
