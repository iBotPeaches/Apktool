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

public final class ValueType {
    public static final int BYTE = 0x00;
    public static final int SHORT = 0x02;
    public static final int CHAR = 0x03;
    public static final int INT = 0x04;
    public static final int LONG = 0x06;
    public static final int FLOAT = 0x10;
    public static final int DOUBLE = 0x11;
    public static final int METHOD_TYPE = 0x15;
    public static final int METHOD_HANDLE = 0x16;
    public static final int STRING = 0x17;
    public static final int TYPE = 0x18;
    public static final int FIELD = 0x19;
    public static final int METHOD = 0x1a;
    public static final int ENUM = 0x1b;
    public static final int ARRAY = 0x1c;
    public static final int ANNOTATION = 0x1d;
    public static final int NULL = 0x1e;
    public static final int BOOLEAN = 0x1f;

    private ValueType() {}

    public static String getValueTypeName(int valueType) {
        switch (valueType) {
            case BYTE:
                return "byte";
            case SHORT:
                return "short";
            case CHAR:
                return "char";
            case INT:
                return "int";
            case LONG:
                return "long";
            case FLOAT:
                return "float";
            case DOUBLE:
                return "double";
            case METHOD_TYPE:
                return "method_type";
            case METHOD_HANDLE:
                return "method_handle";
            case STRING:
                return "string";
            case TYPE:
                return "type";
            case FIELD:
                return "field";
            case METHOD:
                return "method";
            case ENUM:
                return "enum";
            case ARRAY:
                return "array";
            case ANNOTATION:
                return "annotation";
            case NULL:
                return "null";
            case BOOLEAN:
                return "boolean";
            default:
                throw new IllegalArgumentException("Unknown encoded value type: " + valueType);
        }
    }
}
