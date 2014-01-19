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

import org.jf.dexlib2.ValueType;
import org.jf.dexlib2.base.value.*;
import org.jf.dexlib2.iface.AnnotationElement;
import org.jf.dexlib2.iface.reference.FieldReference;
import org.jf.dexlib2.iface.reference.MethodReference;
import org.jf.dexlib2.iface.value.*;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Set;

public class EncodedValueRewriter implements Rewriter<EncodedValue> {
    @Nonnull protected final Rewriters rewriters;

    public EncodedValueRewriter(@Nonnull Rewriters rewriters) {
        this.rewriters = rewriters;
    }

    @Nonnull @Override public EncodedValue rewrite(@Nonnull EncodedValue encodedValue) {
        switch (encodedValue.getValueType()) {
            case ValueType.TYPE:
                return new RewrittenTypeEncodedValue((TypeEncodedValue)encodedValue);
            case ValueType.FIELD:
                return new RewrittenFieldEncodedValue((FieldEncodedValue)encodedValue);
            case ValueType.METHOD:
                return new RewrittenMethodEncodedValue((MethodEncodedValue)encodedValue);
            case ValueType.ENUM:
                return new RewrittenEnumEncodedValue((EnumEncodedValue)encodedValue);
            case ValueType.ARRAY:
                return new RewrittenArrayEncodedValue((ArrayEncodedValue)encodedValue);
            case ValueType.ANNOTATION:
                return new RewrittenAnnotationEncodedValue((AnnotationEncodedValue)encodedValue);
            default:
                return encodedValue;
        }
    }

    protected class RewrittenTypeEncodedValue extends BaseTypeEncodedValue {
        @Nonnull protected TypeEncodedValue typeEncodedValue;

        public RewrittenTypeEncodedValue(@Nonnull TypeEncodedValue typeEncodedValue) {
            this.typeEncodedValue = typeEncodedValue;
        }

        @Override @Nonnull public String getValue() {
            return rewriters.getTypeRewriter().rewrite(typeEncodedValue.getValue());
        }
    }

    protected class RewrittenFieldEncodedValue extends BaseFieldEncodedValue {
        @Nonnull protected FieldEncodedValue fieldEncodedValue;

        public RewrittenFieldEncodedValue(@Nonnull FieldEncodedValue fieldEncodedValue) {
            this.fieldEncodedValue = fieldEncodedValue;
        }

        @Override @Nonnull public FieldReference getValue() {
            return rewriters.getFieldReferenceRewriter().rewrite(fieldEncodedValue.getValue());
        }
    }

    protected class RewrittenEnumEncodedValue extends BaseEnumEncodedValue {
        @Nonnull protected EnumEncodedValue enumEncodedValue;

        public RewrittenEnumEncodedValue(@Nonnull EnumEncodedValue enumEncodedValue) {
            this.enumEncodedValue = enumEncodedValue;
        }

        @Override @Nonnull public FieldReference getValue() {
            return rewriters.getFieldReferenceRewriter().rewrite(enumEncodedValue.getValue());
        }
    }

    protected class RewrittenMethodEncodedValue extends BaseMethodEncodedValue {
        @Nonnull protected MethodEncodedValue methodEncodedValue;

        public RewrittenMethodEncodedValue(@Nonnull MethodEncodedValue methodEncodedValue) {
            this.methodEncodedValue = methodEncodedValue;
        }

        @Override @Nonnull public MethodReference getValue() {
            return rewriters.getMethodReferenceRewriter().rewrite(methodEncodedValue.getValue());
        }
    }

    protected class RewrittenArrayEncodedValue extends BaseArrayEncodedValue {
        @Nonnull protected ArrayEncodedValue arrayEncodedValue;

        public RewrittenArrayEncodedValue(@Nonnull ArrayEncodedValue arrayEncodedValue) {
            this.arrayEncodedValue = arrayEncodedValue;
        }

        @Override @Nonnull public List<? extends EncodedValue> getValue() {
            return RewriterUtils.rewriteList(EncodedValueRewriter.this, arrayEncodedValue.getValue());
        }
    }

    protected class RewrittenAnnotationEncodedValue extends BaseAnnotationEncodedValue {
        @Nonnull protected AnnotationEncodedValue annotationEncodedValue;

        public RewrittenAnnotationEncodedValue(@Nonnull AnnotationEncodedValue annotationEncodedValue) {
            this.annotationEncodedValue = annotationEncodedValue;
        }

        @Nonnull @Override public String getType() {
            return rewriters.getTypeRewriter().rewrite(annotationEncodedValue.getType());
        }

        @Nonnull @Override public Set<? extends AnnotationElement> getElements() {
            return RewriterUtils.rewriteSet(rewriters.getAnnotationElementRewriter(),
                    annotationEncodedValue.getElements());
        }
    }
}
