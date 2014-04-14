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

package org.jf.dexlib2.writer.builder;

import org.jf.dexlib2.base.value.*;
import org.jf.dexlib2.iface.value.EncodedValue;
import org.jf.dexlib2.immutable.value.*;
import org.jf.util.ExceptionWithContext;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Set;

public abstract class BuilderEncodedValues {
    public static interface BuilderEncodedValue extends EncodedValue {
    }

    public static class BuilderAnnotationEncodedValue extends BaseAnnotationEncodedValue
            implements BuilderEncodedValue {
        @Nonnull final BuilderTypeReference typeReference;
        @Nonnull final Set<? extends BuilderAnnotationElement> elements;

        BuilderAnnotationEncodedValue(@Nonnull BuilderTypeReference typeReference,
                                      @Nonnull Set<? extends BuilderAnnotationElement> elements) {
            this.typeReference = typeReference;
            this.elements = elements;
        }

        @Nonnull @Override public String getType() {
            return typeReference.getType();
        }

        @Nonnull @Override public Set<? extends BuilderAnnotationElement> getElements() {
            return elements;
        }
    }

    public static class BuilderArrayEncodedValue extends BaseArrayEncodedValue implements BuilderEncodedValue {
        @Nonnull final List<? extends BuilderEncodedValue> elements;

        BuilderArrayEncodedValue(@Nonnull List<? extends BuilderEncodedValue> elements) {
            this.elements = elements;
        }

        @Nonnull @Override public List<? extends EncodedValue> getValue() {
            return elements;
        }
    }

    @Nonnull
    public static BuilderEncodedValue defaultValueForType(String type) {
        switch (type.charAt(0)) {
            case 'Z':
                return BuilderBooleanEncodedValue.FALSE_VALUE;
            case 'B':
                return new BuilderByteEncodedValue((byte)0);
            case 'S':
                return new BuilderShortEncodedValue((short)0);
            case 'C':
                return new BuilderCharEncodedValue((char)0);
            case 'I':
                return new BuilderIntEncodedValue(0);
            case 'J':
                return new BuilderLongEncodedValue(0);
            case 'F':
                return new BuilderFloatEncodedValue(0);
            case 'D':
                return new BuilderDoubleEncodedValue(0);
            case 'L':
            case '[':
                return BuilderNullEncodedValue.INSTANCE;
            default:
                throw new ExceptionWithContext("Unrecognized type: %s", type);
        }
    }

    public static class BuilderBooleanEncodedValue extends BaseBooleanEncodedValue
            implements BuilderEncodedValue {
        public static final BuilderBooleanEncodedValue TRUE_VALUE = new BuilderBooleanEncodedValue(true);
        public static final BuilderBooleanEncodedValue FALSE_VALUE = new BuilderBooleanEncodedValue(false);

        private final boolean value;

        private BuilderBooleanEncodedValue(boolean value) {
            this.value = value;
        }

        @Override public boolean getValue() {
            return value;
        }
    }

    public static class BuilderByteEncodedValue extends ImmutableByteEncodedValue
            implements BuilderEncodedValue {
        public BuilderByteEncodedValue(byte value) {
            super(value);
        }
    }

    public static class BuilderCharEncodedValue extends ImmutableCharEncodedValue
            implements BuilderEncodedValue {
        public BuilderCharEncodedValue(char value) {
            super(value);
        }
    }

    public static class BuilderDoubleEncodedValue extends ImmutableDoubleEncodedValue
            implements BuilderEncodedValue {
        public BuilderDoubleEncodedValue(double value) {
            super(value);
        }
    }

    public static class BuilderEnumEncodedValue extends BaseEnumEncodedValue
            implements BuilderEncodedValue {
        @Nonnull final BuilderFieldReference enumReference;

        BuilderEnumEncodedValue(@Nonnull BuilderFieldReference enumReference) {
            this.enumReference = enumReference;
        }

        @Nonnull @Override public BuilderFieldReference getValue() {
            return enumReference;
        }
    }

    public static class BuilderFieldEncodedValue extends BaseFieldEncodedValue
            implements BuilderEncodedValue {
        @Nonnull final BuilderFieldReference fieldReference;

        BuilderFieldEncodedValue(@Nonnull BuilderFieldReference fieldReference) {
            this.fieldReference = fieldReference;
        }

        @Nonnull @Override public BuilderFieldReference getValue() {
            return fieldReference;
        }
    }

    public static class BuilderFloatEncodedValue extends ImmutableFloatEncodedValue
            implements BuilderEncodedValue {
        public BuilderFloatEncodedValue(float value) {
            super(value);
        }
    }

    public static class BuilderIntEncodedValue extends ImmutableIntEncodedValue
            implements BuilderEncodedValue {
        public BuilderIntEncodedValue(int value) {
            super(value);
        }
    }

    public static class BuilderLongEncodedValue extends ImmutableLongEncodedValue
            implements BuilderEncodedValue {
        public BuilderLongEncodedValue(long value) {
            super(value);
        }
    }

    public static class BuilderMethodEncodedValue extends BaseMethodEncodedValue
            implements BuilderEncodedValue {
        @Nonnull final BuilderMethodReference methodReference;

        BuilderMethodEncodedValue(@Nonnull BuilderMethodReference methodReference) {
            this.methodReference = methodReference;
        }

        @Override public BuilderMethodReference getValue() {
            return methodReference;
        }
    }

    public static class BuilderNullEncodedValue extends BaseNullEncodedValue
            implements BuilderEncodedValue {
        public static final BuilderNullEncodedValue INSTANCE = new BuilderNullEncodedValue();

        private BuilderNullEncodedValue() {}
    }

    public static class BuilderShortEncodedValue extends ImmutableShortEncodedValue
            implements BuilderEncodedValue {
        public BuilderShortEncodedValue(short value) {
            super(value);
        }
    }

    public static class BuilderStringEncodedValue extends BaseStringEncodedValue
            implements BuilderEncodedValue {
        @Nonnull final BuilderStringReference stringReference;

        BuilderStringEncodedValue(@Nonnull BuilderStringReference stringReference) {
            this.stringReference = stringReference;
        }

        @Nonnull @Override public String getValue() {
            return stringReference.getString();
        }
    }

    public static class BuilderTypeEncodedValue extends BaseTypeEncodedValue
            implements BuilderEncodedValue {
        @Nonnull final BuilderTypeReference typeReference;

        BuilderTypeEncodedValue(@Nonnull BuilderTypeReference typeReference) {
            this.typeReference = typeReference;
        }

        @Nonnull @Override public String getValue() {
            return typeReference.getType();
        }
    }
}
