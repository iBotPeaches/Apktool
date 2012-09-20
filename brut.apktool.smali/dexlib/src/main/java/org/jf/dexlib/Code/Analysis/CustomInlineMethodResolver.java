/*
 * Copyright 2011, Google Inc.
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

package org.jf.dexlib.Code.Analysis;

import org.jf.dexlib.Code.OdexedInvokeInline;
import org.jf.dexlib.Code.OdexedInvokeVirtual;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CustomInlineMethodResolver extends InlineMethodResolver {
    private DeodexUtil.InlineMethod[] inlineMethods;

    public CustomInlineMethodResolver(String inlineTable) {
        FileReader fr = null;
        try {
            fr = new FileReader(inlineTable);
        } catch (FileNotFoundException ex) {
            throw new RuntimeException("Could not find inline table file: " + inlineTable);
        }

        List<String> lines = new ArrayList<String>();

        BufferedReader br = new BufferedReader(fr);

        try {
            String line = br.readLine();

            while (line != null) {
                if (line.length() > 0) {
                    lines.add(line);
                }

                line = br.readLine();
            }
        } catch (IOException ex) {
            throw new RuntimeException("Error while reading file: " + inlineTable, ex);
        }

        inlineMethods = new DeodexUtil.InlineMethod[lines.size()];

        for (int i=0; i<inlineMethods.length; i++) {
            inlineMethods[i] = parseAndResolveInlineMethod(lines.get(i));
        }
    }

    @Override
    public DeodexUtil.InlineMethod resolveExecuteInline(AnalyzedInstruction analyzedInstruction) {
        assert analyzedInstruction.instruction instanceof OdexedInvokeInline;

        OdexedInvokeInline instruction = (OdexedInvokeInline)analyzedInstruction.instruction;
        int methodIndex = instruction.getInlineIndex();

        if (methodIndex < 0 || methodIndex >= inlineMethods.length) {
            throw new RuntimeException("Invalid method index: " + methodIndex);
        }
        return inlineMethods[methodIndex];
    }

    private static final Pattern longMethodPattern = Pattern.compile("(L[^;]+;)->([^(]+)\\(([^)]*)\\)(.+)");

    private DeodexUtil.InlineMethod parseAndResolveInlineMethod(String inlineMethod) {
        Matcher m = longMethodPattern.matcher(inlineMethod);
        if (!m.matches()) {
            assert false;
            throw new RuntimeException("Invalid method descriptor: " + inlineMethod);
        }

        String className = m.group(1);
        String methodName = m.group(2);
        String methodParams = m.group(3);
        String methodRet = m.group(4);

        ClassPath.ClassDef classDef = ClassPath.getClassDef(className, false);
        int methodType = classDef.getMethodType(String.format("%s(%s)%s", methodName, methodParams, methodRet));

        if (methodType == -1) {
            throw new RuntimeException("Cannot resolve inline method: " + inlineMethod);
        }

        return new DeodexUtil.InlineMethod(methodType, className, methodName, methodParams, methodRet);
    }
}
