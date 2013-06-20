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

package org.jf.dexlib2;

import com.google.common.collect.Maps;
import org.jf.util.ExceptionWithContext;

import java.util.HashMap;

public class VerificationError {
    public static final int GENERIC = 1;
    public static final int NO_SUCH_CLASS = 2;
    public static final int NO_SUCH_FIELD = 3;
    public static final int NO_SUCH_METHOD = 4;
    public static final int ILLEGAL_CLASS_ACCESS = 5;
    public static final int ILLEGAL_FIELD_ACCESS = 6;
    public static final int ILLEGAL_METHOD_ACCESS = 7;
    public static final int CLASS_CHANGE_ERROR = 8;
    public static final int INSTANTIATION_ERROR = 9;

    private static final HashMap<String, Integer> verificationErrorNames = Maps.newHashMap();

    static {
        verificationErrorNames.put("generic-error", GENERIC);
        verificationErrorNames.put("no-such-class", NO_SUCH_CLASS);
        verificationErrorNames.put("no-such-field", NO_SUCH_FIELD);
        verificationErrorNames.put("no-such-method", NO_SUCH_METHOD);
        verificationErrorNames.put("illegal-class-access", ILLEGAL_CLASS_ACCESS);
        verificationErrorNames.put("illegal-field-access", ILLEGAL_FIELD_ACCESS);
        verificationErrorNames.put("illegal-method-access", ILLEGAL_METHOD_ACCESS);
        verificationErrorNames.put("class-change-error", CLASS_CHANGE_ERROR);
        verificationErrorNames.put("instantiation-error", INSTANTIATION_ERROR);
    }

    public static String getVerificationErrorName(int verificationError) {
        switch (verificationError) {
            case GENERIC:
                return "generic-error";
            case NO_SUCH_CLASS:
                return "no-such-class";
            case NO_SUCH_FIELD:
                return "no-such-field";
            case NO_SUCH_METHOD:
                return "no-such-method";
            case ILLEGAL_CLASS_ACCESS:
                return "illegal-class-access";
            case ILLEGAL_FIELD_ACCESS:
                return "illegal-field-access";
            case ILLEGAL_METHOD_ACCESS:
                return "illegal-method-access";
            case CLASS_CHANGE_ERROR:
                return "class-change-error";
            case INSTANTIATION_ERROR:
                return "instantiation-error";
            default:
                return null;
        }
    }

    public static int getVerificationError(String verificationError) {
        Integer ret = verificationErrorNames.get(verificationError);
        if (ret == null) {
            throw new ExceptionWithContext("Invalid verification error: %s", verificationError);
        }
        return ret;
    }

    public static boolean isValidVerificationError(int verificationError) {
        return verificationError > 0 && verificationError < 10;
    }
}
