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

import com.google.common.collect.ImmutableList;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.instruction.InlineIndexInstruction;
import org.jf.dexlib2.iface.instruction.VariableRegisterInstruction;
import org.jf.dexlib2.immutable.ImmutableMethod;
import org.jf.dexlib2.immutable.ImmutableMethodParameter;
import org.jf.dexlib2.immutable.util.ParamUtil;

import javax.annotation.Nonnull;

public abstract class InlineMethodResolver {
    // These are the possible values for the accessFlag field on a resolved inline method
    // We can't use, e.g. AccessFlags.STATIC.value, because we need them to be a constant in order to use them as cases
    // in switch statements
    public static final int STATIC = 0x8; // AccessFlags.STATIC.value;
    public static final int VIRTUAL = 0x1; // AccessFlags.PUBLIC.value;
    public static final int DIRECT = 0x2; // AccessFlags.PRIVATE.value;

    @Nonnull
    public static InlineMethodResolver createInlineMethodResolver(int odexVersion) {
        if (odexVersion == 35) {
            return new InlineMethodResolver_version35();
        } else if (odexVersion == 36) {
            return new InlineMethodResolver_version36();
        } else {
            throw new RuntimeException(String.format("odex version %d is not supported yet", odexVersion));
        }
    }

    protected InlineMethodResolver() {
    }

    @Nonnull
    private static Method inlineMethod(int accessFlags, @Nonnull String cls, @Nonnull String name,
                                       @Nonnull String params, @Nonnull String returnType) {
        ImmutableList<ImmutableMethodParameter> paramList = ImmutableList.copyOf(ParamUtil.parseParamString(params));
        return new ImmutableMethod(cls, name, paramList, returnType, accessFlags, null, null);
    }

    @Nonnull public abstract Method resolveExecuteInline(@Nonnull AnalyzedInstruction instruction);

    private static class InlineMethodResolver_version35 extends InlineMethodResolver
    {
        private final Method[] inlineMethods;

        public InlineMethodResolver_version35() {
            inlineMethods = new Method[] {
                inlineMethod(STATIC, "Lorg/apache/harmony/dalvik/NativeTestTarget;", "emptyInlineMethod", "", "V"),
                inlineMethod(VIRTUAL, "Ljava/lang/String;", "charAt", "I", "C"),
                inlineMethod(VIRTUAL, "Ljava/lang/String;", "compareTo", "Ljava/lang/String;", "I"),
                inlineMethod(VIRTUAL, "Ljava/lang/String;", "equals", "Ljava/lang/Object;", "Z"),
                inlineMethod(VIRTUAL, "Ljava/lang/String;", "length", "", "I"),
                inlineMethod(STATIC, "Ljava/lang/Math;", "abs", "I", "I"),
                inlineMethod(STATIC, "Ljava/lang/Math;", "abs", "J", "J"),
                inlineMethod(STATIC, "Ljava/lang/Math;", "abs", "F", "F"),
                inlineMethod(STATIC, "Ljava/lang/Math;", "abs", "D", "D"),
                inlineMethod(STATIC, "Ljava/lang/Math;", "min", "II", "I"),
                inlineMethod(STATIC, "Ljava/lang/Math;", "max", "II", "I"),
                inlineMethod(STATIC, "Ljava/lang/Math;", "sqrt", "D", "D"),
                inlineMethod(STATIC, "Ljava/lang/Math;", "cos", "D", "D"),
                inlineMethod(STATIC, "Ljava/lang/Math;", "sin", "D", "D")
            };
        }

        @Override
        @Nonnull
        public Method resolveExecuteInline(@Nonnull AnalyzedInstruction analyzedInstruction) {
            InlineIndexInstruction instruction = (InlineIndexInstruction)analyzedInstruction.instruction;
            int inlineIndex = instruction.getInlineIndex();

            if (inlineIndex < 0 || inlineIndex >= inlineMethods.length) {
                throw new RuntimeException("Invalid inline index: " + inlineIndex);
            }
            return inlineMethods[inlineIndex];
        }
    }

    private static class InlineMethodResolver_version36 extends InlineMethodResolver
    {
        private final Method[] inlineMethods;
        private final Method indexOfIMethod;
        private final Method indexOfIIMethod;
        private final Method fastIndexOfMethod;
        private final Method isEmptyMethod;

        public InlineMethodResolver_version36() {
            //The 5th and 6th entries differ between froyo and gingerbread. We have to look at the parameters being
            //passed to distinguish between them.

            //froyo
            indexOfIMethod = inlineMethod(VIRTUAL, "Ljava/lang/String;", "indexOf", "I", "I");
            indexOfIIMethod = inlineMethod(VIRTUAL, "Ljava/lang/String;", "indexOf", "II", "I");

            //gingerbread
            fastIndexOfMethod = inlineMethod(DIRECT, "Ljava/lang/String;", "fastIndexOf", "II", "I");
            isEmptyMethod = inlineMethod(VIRTUAL, "Ljava/lang/String;", "isEmpty", "", "Z");

            inlineMethods = new Method[] {
                inlineMethod(STATIC, "Lorg/apache/harmony/dalvik/NativeTestTarget;", "emptyInlineMethod", "", "V"),
                inlineMethod(VIRTUAL, "Ljava/lang/String;", "charAt", "I", "C"),
                inlineMethod(VIRTUAL, "Ljava/lang/String;", "compareTo", "Ljava/lang/String;", "I"),
                inlineMethod(VIRTUAL, "Ljava/lang/String;", "equals", "Ljava/lang/Object;", "Z"),
                //froyo: deodexUtil.new InlineMethod(VIRTUAL, "Ljava/lang/String;", "indexOf", "I", "I"),
                //gingerbread: deodexUtil.new InlineMethod(VIRTUAL, "Ljava/lang/String;", "fastIndexOf", "II", "I"),
                null,
                //froyo: deodexUtil.new InlineMethod(VIRTUAL, "Ljava/lang/String;", "indexOf", "II", "I"),
                //gingerbread: deodexUtil.new InlineMethod(VIRTUAL, "Ljava/lang/String;", "isEmpty", "", "Z"),
                null,
                inlineMethod(VIRTUAL, "Ljava/lang/String;", "length", "", "I"),
                inlineMethod(STATIC, "Ljava/lang/Math;", "abs", "I", "I"),
                inlineMethod(STATIC, "Ljava/lang/Math;", "abs", "J", "J"),
                inlineMethod(STATIC, "Ljava/lang/Math;", "abs", "F", "F"),
                inlineMethod(STATIC, "Ljava/lang/Math;", "abs", "D", "D"),
                inlineMethod(STATIC, "Ljava/lang/Math;", "min", "II", "I"),
                inlineMethod(STATIC, "Ljava/lang/Math;", "max", "II", "I"),
                inlineMethod(STATIC, "Ljava/lang/Math;", "sqrt", "D", "D"),
                inlineMethod(STATIC, "Ljava/lang/Math;", "cos", "D", "D"),
                inlineMethod(STATIC, "Ljava/lang/Math;", "sin", "D", "D"),
                inlineMethod(STATIC, "Ljava/lang/Float;", "floatToIntBits", "F", "I"),
                inlineMethod(STATIC, "Ljava/lang/Float;", "floatToRawIntBits", "F", "I"),
                inlineMethod(STATIC, "Ljava/lang/Float;", "intBitsToFloat", "I", "F"),
                inlineMethod(STATIC, "Ljava/lang/Double;", "doubleToLongBits", "D", "J"),
                inlineMethod(STATIC, "Ljava/lang/Double;", "doubleToRawLongBits", "D", "J"),
                inlineMethod(STATIC, "Ljava/lang/Double;", "longBitsToDouble", "J", "D"),
                inlineMethod(STATIC, "Ljava/lang/StrictMath;", "abs", "I", "I"),
                inlineMethod(STATIC, "Ljava/lang/StrictMath;", "abs", "J", "J"),
                inlineMethod(STATIC, "Ljava/lang/StrictMath;", "abs", "F", "F"),
                inlineMethod(STATIC, "Ljava/lang/StrictMath;", "abs", "D", "D"),
                inlineMethod(STATIC, "Ljava/lang/StrictMath;", "min", "II", "I"),
                inlineMethod(STATIC, "Ljava/lang/StrictMath;", "max", "II", "I"),
                inlineMethod(STATIC, "Ljava/lang/StrictMath;", "sqrt", "D", "D"),
            };
        }

        @Override
        @Nonnull
        public Method resolveExecuteInline(@Nonnull AnalyzedInstruction analyzedInstruction) {
            InlineIndexInstruction instruction = (InlineIndexInstruction)analyzedInstruction.instruction;
            int inlineIndex = instruction.getInlineIndex();

            if (inlineIndex < 0 || inlineIndex >= inlineMethods.length) {
                throw new RuntimeException("Invalid method index: " + inlineIndex);
            }

            if (inlineIndex == 4) {
                int parameterCount = ((VariableRegisterInstruction)instruction).getRegisterCount();
                if (parameterCount == 2) {
                    return indexOfIMethod;
                } else if (parameterCount == 3) {
                    return fastIndexOfMethod;
                } else {
                    throw new RuntimeException("Could not determine the correct inline method to use");
                }
            } else if (inlineIndex == 5) {
                int parameterCount = ((VariableRegisterInstruction)instruction).getRegisterCount();
                if (parameterCount == 3) {
                    return indexOfIIMethod;
                } else if (parameterCount == 1) {
                    return isEmptyMethod;
                } else {
                    throw new RuntimeException("Could not determine the correct inline method to use");
                }
            }

            return inlineMethods[inlineIndex];
        }
    }
}
