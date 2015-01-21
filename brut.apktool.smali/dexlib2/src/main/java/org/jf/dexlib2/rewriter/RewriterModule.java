/*
 * Copyright 2014, Google Inc.
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

package org.jf.dexlib2.rewriter;

import org.jf.dexlib2.iface.*;
import org.jf.dexlib2.iface.debug.DebugItem;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.reference.FieldReference;
import org.jf.dexlib2.iface.reference.MethodReference;
import org.jf.dexlib2.iface.value.EncodedValue;

import javax.annotation.Nonnull;

public class RewriterModule {
    @Nonnull public Rewriter<ClassDef> getClassDefRewriter(@Nonnull Rewriters rewriters) {
        return new ClassDefRewriter(rewriters);
    }

    @Nonnull public Rewriter<Field> getFieldRewriter(@Nonnull Rewriters rewriters) {
        return new FieldRewriter(rewriters);
    }

    @Nonnull public Rewriter<Method> getMethodRewriter(@Nonnull Rewriters rewriters) {
        return new MethodRewriter(rewriters);
    }

    @Nonnull public Rewriter<MethodParameter> getMethodParameterRewriter(@Nonnull Rewriters rewriters) {
        return new MethodParameterRewriter(rewriters);
    }

    @Nonnull public Rewriter<MethodImplementation> getMethodImplementationRewriter(@Nonnull Rewriters rewriters) {
        return new MethodImplementationRewriter(rewriters);
    }

    @Nonnull public Rewriter<Instruction> getInstructionRewriter(@Nonnull Rewriters rewriters) {
        return new InstructionRewriter(rewriters);
    }

    @Nonnull public Rewriter<TryBlock<? extends ExceptionHandler>> getTryBlockRewriter(@Nonnull Rewriters rewriters) {
        return new TryBlockRewriter(rewriters);
    }

    @Nonnull public Rewriter<ExceptionHandler> getExceptionHandlerRewriter(@Nonnull Rewriters rewriters) {
        return new ExceptionHandlerRewriter(rewriters);
    }

    @Nonnull public Rewriter<DebugItem> getDebugItemRewriter(@Nonnull Rewriters rewriters) {
        return new DebugItemRewriter(rewriters);
    }

    @Nonnull public Rewriter<String> getTypeRewriter(@Nonnull Rewriters rewriters) {
        return new TypeRewriter();
    }

    @Nonnull public Rewriter<FieldReference> getFieldReferenceRewriter(@Nonnull Rewriters rewriters) {
        return new FieldReferenceRewriter(rewriters);
    }

    @Nonnull public Rewriter<MethodReference> getMethodReferenceRewriter(@Nonnull Rewriters rewriters) {
        return new MethodReferenceRewriter(rewriters);
    }

    @Nonnull public Rewriter<Annotation> getAnnotationRewriter(@Nonnull Rewriters rewriters) {
        return new AnnotationRewriter(rewriters);
    }

    @Nonnull public Rewriter<AnnotationElement> getAnnotationElementRewriter(@Nonnull Rewriters rewriters) {
        return new AnnotationElementRewriter(rewriters);
    }

    @Nonnull public Rewriter<EncodedValue> getEncodedValueRewriter(@Nonnull Rewriters rewriters) {
        return new EncodedValueRewriter(rewriters);
    }
}
