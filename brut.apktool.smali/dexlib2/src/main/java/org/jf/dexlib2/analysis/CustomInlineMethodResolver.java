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

package org.jf.dexlib2.analysis;

import com.google.common.io.Files;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.instruction.InlineIndexInstruction;
import org.jf.dexlib2.immutable.ImmutableMethod;
import org.jf.dexlib2.immutable.ImmutableMethodParameter;
import org.jf.dexlib2.immutable.reference.ImmutableMethodReference;
import org.jf.dexlib2.immutable.util.ParamUtil;

import javax.annotation.Nonnull;
import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CustomInlineMethodResolver extends InlineMethodResolver {
    @Nonnull private final ClassPath classPath;
    @Nonnull private final Method[] inlineMethods;

    public CustomInlineMethodResolver(@Nonnull ClassPath classPath, @Nonnull String inlineTable) {
        this.classPath = classPath;

        StringReader reader = new StringReader(inlineTable);
        List<String> lines = new ArrayList<String>();

        BufferedReader br = new BufferedReader(reader);

        try {
            String line = br.readLine();

            while (line != null) {
                if (line.length() > 0) {
                    lines.add(line);
                }

                line = br.readLine();
            }
        } catch (IOException ex) {
            throw new RuntimeException("Error while parsing inline table", ex);
        }

        inlineMethods = new Method[lines.size()];

        for (int i=0; i<inlineMethods.length; i++) {
            inlineMethods[i] = parseAndResolveInlineMethod(lines.get(i));
        }
    }

    public CustomInlineMethodResolver(@Nonnull ClassPath classPath, @Nonnull File inlineTable) throws IOException {
        this(classPath, Files.toString(inlineTable, Charset.forName("UTF-8")));
    }

    @Override
    @Nonnull
    public Method resolveExecuteInline(@Nonnull AnalyzedInstruction analyzedInstruction) {
        InlineIndexInstruction instruction = (InlineIndexInstruction)analyzedInstruction.instruction;
        int methodIndex = instruction.getInlineIndex();

        if (methodIndex < 0 || methodIndex >= inlineMethods.length) {
            throw new RuntimeException("Invalid method index: " + methodIndex);
        }
        return inlineMethods[methodIndex];
    }

    private static final Pattern longMethodPattern = Pattern.compile("(L[^;]+;)->([^(]+)\\(([^)]*)\\)(.+)");

    @Nonnull
    private Method parseAndResolveInlineMethod(@Nonnull String inlineMethod) {
        Matcher m = longMethodPattern.matcher(inlineMethod);
        if (!m.matches()) {
            assert false;
            throw new RuntimeException("Invalid method descriptor: " + inlineMethod);
        }

        String className = m.group(1);
        String methodName = m.group(2);
        Iterable<ImmutableMethodParameter> methodParams = ParamUtil.parseParamString(m.group(3));
        String methodRet = m.group(4);
        ImmutableMethodReference methodRef = new ImmutableMethodReference(className, methodName, methodParams,
                methodRet);

        int accessFlags = 0;

        boolean resolved = false;
        TypeProto typeProto = classPath.getClass(className);
        if (typeProto instanceof ClassProto) {
            ClassDef classDef = ((ClassProto)typeProto).getClassDef();
            for (Method method: classDef.getMethods()) {
                if (method.equals(methodRef)) {
                    resolved = true;
                    accessFlags = method.getAccessFlags();
                    break;
                }
            }
        }

        if (!resolved) {
            throw new RuntimeException("Cannot resolve inline method: " + inlineMethod);
        }

        return new ImmutableMethod(className, methodName, methodParams, methodRet, accessFlags, null, null);
    }
}
