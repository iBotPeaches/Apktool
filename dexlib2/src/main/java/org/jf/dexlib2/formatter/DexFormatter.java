/*
 * Copyright 2021, Google Inc.
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

package org.jf.dexlib2.formatter;

import org.jf.dexlib2.iface.reference.*;
import org.jf.dexlib2.iface.value.EncodedValue;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

/**
 * This class handles formatting and getting strings for various types of items in a dex file.
 */
public class DexFormatter {

    public static final DexFormatter INSTANCE = new DexFormatter();

    /**
     * Gets a {@link DexFormattedWriter} for writing formatted strings to a {@link Writer}, with the same settings as this Formatter.
     *
     * @param writer The {@link Writer} that the {@link DexFormattedWriter} will write to.
     */
    public DexFormattedWriter getWriter(Writer writer) {
        return new DexFormattedWriter(writer);
    }

    public String getMethodDescriptor(MethodReference methodReference) {
        StringWriter writer = new StringWriter();
        try {
            getWriter(writer).writeMethodDescriptor(methodReference);
        } catch (IOException e) {
            throw new AssertionError("Unexpected IOException");
        }
        return writer.toString();
    }

    public String getShortMethodDescriptor(MethodReference methodReference) {
        StringWriter writer = new StringWriter();
        try {
            getWriter(writer).writeShortMethodDescriptor(methodReference);
        } catch (IOException e) {
            throw new AssertionError("Unexpected IOException");
        }
        return writer.toString();
    }

    public String getMethodProtoDescriptor(MethodProtoReference protoReference) {
        StringWriter writer = new StringWriter();
        try {
            getWriter(writer).writeMethodProtoDescriptor(protoReference);
        } catch (IOException e) {
            throw new AssertionError("Unexpected IOException");
        }
        return writer.toString();
    }

    public String getFieldDescriptor(FieldReference fieldReference) {
        StringWriter writer = new StringWriter();
        try {
            getWriter(writer).writeFieldDescriptor(fieldReference);
        } catch (IOException e) {
            throw new AssertionError("Unexpected IOException");
        }
        return writer.toString();
    }

    public String getShortFieldDescriptor(FieldReference fieldReference) {
        StringWriter writer = new StringWriter();
        try {
            getWriter(writer).writeShortFieldDescriptor(fieldReference);
        } catch (IOException e) {
            throw new AssertionError("Unexpected IOException");
        }
        return writer.toString();
    }

    public String getMethodHandle(MethodHandleReference methodHandleReference) {
        StringWriter writer = new StringWriter();
        try {
            getWriter(writer).writeMethodHandle(methodHandleReference);
        } catch (IOException e) {
            throw new AssertionError("Unexpected IOException");
        }
        return writer.toString();
    }

    public String getCallSite(CallSiteReference callSiteReference) {
        StringWriter writer = new StringWriter();
        try {
            getWriter(writer).writeCallSite(callSiteReference);
        } catch (IOException e) {
            throw new AssertionError("Unexpected IOException");
        }
        return writer.toString();
    }

    public String getType(CharSequence type) {
        StringWriter writer = new StringWriter();
        try {
            getWriter(writer).writeType(type);
        } catch (IOException e) {
            throw new AssertionError("Unexpected IOException");
        }
        return writer.toString();
    }

    public String getQuotedString(CharSequence string) {
        StringWriter writer = new StringWriter();
        try {
            getWriter(writer).writeQuotedString(string);
        } catch (IOException e) {
            throw new AssertionError("Unexpected IOException");
        }
        return writer.toString();
    }

    public String getEncodedValue(EncodedValue encodedValue) {
        StringWriter writer = new StringWriter();
        try {
            getWriter(writer).writeEncodedValue(encodedValue);
        } catch (IOException e) {
            throw new AssertionError("Unexpected IOException");
        }
        return writer.toString();
    }

    public String getReference(Reference reference) {
        StringWriter writer = new StringWriter();
        try {
            getWriter(writer).writeReference(reference);
        } catch (IOException e) {
            throw new AssertionError("Unexpected IOException");
        }
        return writer.toString();
    }
}
