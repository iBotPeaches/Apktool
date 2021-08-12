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

package org.jf.dexlib2.util;

import org.jf.dexlib2.MethodHandleType;
import org.jf.dexlib2.formatter.DexFormatter;
import org.jf.dexlib2.iface.reference.*;
import org.jf.dexlib2.iface.value.EncodedValue;
import org.jf.util.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

/**
 * Some utilities for generating human-readable strings for the various types of references.
 *
 * @deprecated use {@link DexFormatter} instead.
 */
@Deprecated
public final class ReferenceUtil {
    public static String getMethodDescriptor(MethodReference methodReference) {
        return getMethodDescriptor(methodReference, false);
    }

    public static String getMethodDescriptor(MethodReference methodReference, boolean useImplicitReference) {
        StringBuilder sb = new StringBuilder();
        if (!useImplicitReference) {
            sb.append(methodReference.getDefiningClass());
            sb.append("->");
        }
        sb.append(methodReference.getName());
        sb.append('(');
        for (CharSequence paramType: methodReference.getParameterTypes()) {
            sb.append(paramType);
        }
        sb.append(')');
        sb.append(methodReference.getReturnType());
        return sb.toString();
    }

    public static String getMethodProtoDescriptor(MethodProtoReference methodProtoReference) {
        StringWriter stringWriter = new StringWriter();
        try {
            writeMethodProtoDescriptor(stringWriter, methodProtoReference);
        } catch (IOException ex) {
            // IOException shouldn't happen for a StringWriter...
            throw new RuntimeException(ex);
        }
        return stringWriter.toString();
    }

    public static void writeMethodProtoDescriptor(Writer writer, MethodProtoReference methodProtoReference)
            throws IOException {
        writer.write('(');
        for (CharSequence paramType : methodProtoReference.getParameterTypes()) {
            writer.write(paramType.toString());
        }
        writer.write(')');
        writer.write(methodProtoReference.getReturnType());
    }

    public static void writeMethodDescriptor(Writer writer, MethodReference methodReference) throws IOException {
        writeMethodDescriptor(writer, methodReference, false);
    }

    public static void writeMethodDescriptor(Writer writer, MethodReference methodReference,
                                             boolean useImplicitReference) throws IOException {
        if (!useImplicitReference) {
            writer.write(methodReference.getDefiningClass());
            writer.write("->");
        }
        writer.write(methodReference.getName());
        writer.write('(');
        for (CharSequence paramType: methodReference.getParameterTypes()) {
            writer.write(paramType.toString());
        }
        writer.write(')');
        writer.write(methodReference.getReturnType());
    }

    public static String getFieldDescriptor(FieldReference fieldReference) {
        return getFieldDescriptor(fieldReference, false);
    }

    public static String getFieldDescriptor(FieldReference fieldReference, boolean useImplicitReference) {
        StringBuilder sb = new StringBuilder();
        if (!useImplicitReference) {
            sb.append(fieldReference.getDefiningClass());
            sb.append("->");
        }
        sb.append(fieldReference.getName());
        sb.append(':');
        sb.append(fieldReference.getType());
        return sb.toString();
    }

    public static String getShortFieldDescriptor(FieldReference fieldReference) {
        StringBuilder sb = new StringBuilder();
        sb.append(fieldReference.getName());
        sb.append(':');
        sb.append(fieldReference.getType());
        return sb.toString();
    }

    public static void writeFieldDescriptor(Writer writer, FieldReference fieldReference) throws IOException {
        writeFieldDescriptor(writer, fieldReference, false);
    }

    public static void writeFieldDescriptor(Writer writer, FieldReference fieldReference,
                                            boolean implicitReference) throws IOException {
        if (!implicitReference) {
            writer.write(fieldReference.getDefiningClass());
            writer.write("->");
        }
        writer.write(fieldReference.getName());
        writer.write(':');
        writer.write(fieldReference.getType());
    }

    public static String getMethodHandleString(MethodHandleReference methodHandleReference) {
        StringWriter stringWriter = new StringWriter();
        try {
            writeMethodHandle(stringWriter, methodHandleReference);
        } catch (IOException ex) {
            // IOException shouldn't happen for a StringWriter...
            throw new RuntimeException(ex);
        }
        return stringWriter.toString();
    }

    public static void writeMethodHandle(Writer writer, MethodHandleReference methodHandleReference)
            throws IOException {
        writer.write(MethodHandleType.toString(methodHandleReference.getMethodHandleType()));
        writer.write('@');

        Reference memberReference = methodHandleReference.getMemberReference();
        if (memberReference instanceof MethodReference) {
            writeMethodDescriptor(writer, (MethodReference)memberReference);
        } else {
            writeFieldDescriptor(writer, (FieldReference)memberReference);
        }
    }

    public static String getCallSiteString(CallSiteReference callSiteReference) {
        StringWriter stringWriter = new StringWriter();
        try {
            writeCallSite(stringWriter, callSiteReference);
        } catch (IOException ex) {
            // IOException shouldn't happen for a StringWriter...
            throw new RuntimeException(ex);
        }
        return stringWriter.toString();
    }

    public static void writeCallSite(Writer writer, CallSiteReference callSiteReference) throws IOException {
        writer.write(callSiteReference.getName());
        writer.write('(');
        writer.write('"');
        StringUtils.writeEscapedString(writer, callSiteReference.getMethodName());
        writer.write('"');
        writer.write(", ");
        writeMethodProtoDescriptor(writer, callSiteReference.getMethodProto());

        for (EncodedValue encodedValue : callSiteReference.getExtraArguments()) {
            writer.write(", ");
            EncodedValueUtils.writeEncodedValue(writer, encodedValue);
        }
        writer.write(")@");
        MethodHandleReference methodHandle = callSiteReference.getMethodHandle();
        if (methodHandle.getMethodHandleType() != MethodHandleType.INVOKE_STATIC) {
            throw new IllegalArgumentException("The linker method handle for a call site must be of type invoke-static");
        }
        writeMethodDescriptor(writer, (MethodReference)callSiteReference.getMethodHandle().getMemberReference());
    }

    @Nullable
    public static String getReferenceString(@Nonnull Reference reference) {
        return getReferenceString(reference, null);
    }

    @Nullable
    public static String getReferenceString(@Nonnull Reference reference, @Nullable String containingClass) {
        if (reference instanceof StringReference) {
            return String.format("\"%s\"", StringUtils.escapeString(((StringReference)reference).getString()));
        }
        if (reference instanceof TypeReference) {
            return ((TypeReference)reference).getType();
        }
        if (reference instanceof FieldReference) {
            FieldReference fieldReference = (FieldReference)reference;
            boolean useImplicitReference = fieldReference.getDefiningClass().equals(containingClass);
            return getFieldDescriptor(fieldReference, useImplicitReference);
        }
        if (reference instanceof MethodReference) {
            MethodReference methodReference = (MethodReference)reference;
            boolean useImplicitReference = methodReference.getDefiningClass().equals(containingClass);
            return getMethodDescriptor(methodReference, useImplicitReference);
        }
        if (reference instanceof MethodProtoReference) {
            MethodProtoReference methodProtoReference = (MethodProtoReference)reference;
            return getMethodProtoDescriptor(methodProtoReference);
        }
        if (reference instanceof MethodHandleReference) {
            MethodHandleReference methodHandleReference = (MethodHandleReference)reference;
            return getMethodHandleString(methodHandleReference);
        }
        if (reference instanceof CallSiteReference) {
            CallSiteReference callSiteReference = (CallSiteReference)reference;
            return getCallSiteString(callSiteReference);
        }
        return null;
    }

    private ReferenceUtil() {}
}
