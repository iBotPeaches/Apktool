/*
 * [The "BSD licence"]
 * Copyright (c) 2010 Ben Gruver (JesusFreke)
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
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.jf.dexlib.Code;

import org.jf.dexlib.*;

public enum ReferenceType
{
    string(-1),
    type(0),
    field(1),
    method(2),
    none(-1);

    private int validationErrorReferenceType;

    private ReferenceType(int validationErrorReferenceType) {
        this.validationErrorReferenceType = validationErrorReferenceType;
    }

    public boolean checkItem(Item item) {
        switch (this) {
            case string:
                return item instanceof StringIdItem;
            case type:
                return item instanceof TypeIdItem;
            case field:
                return item instanceof FieldIdItem;
            case method:
                return item instanceof MethodIdItem;
        }
        return false;
    }

    public static ReferenceType fromValidationErrorReferenceType(int validationErrorReferenceType) {
        switch (validationErrorReferenceType) {
            case 0:
                return type;
            case 1:
                return field;
            case 2:
                return method;
        }
        return null;
    }

    public int getValidationErrorReferenceType() {
        if (validationErrorReferenceType == -1) {
            throw new RuntimeException("This reference type cannot be referenced from a throw-validation-error" +
                    " instruction");
        }
        return validationErrorReferenceType;
    }
}