/*
 * [The "BSD licence"]
 * Copyright (c) 2010 Ben Gruver
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
 * INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.jf.dexlib.Code.Analysis;

import org.jf.dexlib.Code.OdexedInvokeInline;
import org.jf.dexlib.Code.OdexedInvokeVirtual;

import static org.jf.dexlib.Code.Analysis.DeodexUtil.Static;
import static org.jf.dexlib.Code.Analysis.DeodexUtil.Virtual;
import static org.jf.dexlib.Code.Analysis.DeodexUtil.Direct;

public abstract class InlineMethodResolver {
    public static InlineMethodResolver createInlineMethodResolver(DeodexUtil deodexUtil, int odexVersion) {
        if (odexVersion == 35) {
            return new InlineMethodResolver_version35(deodexUtil);
        } else if (odexVersion == 36) {
            return new InlineMethodResolver_version36(deodexUtil);
        } else {
            throw new RuntimeException(String.format("odex version %d is not supported yet", odexVersion));
        }
    }

    protected InlineMethodResolver() {
    }

    public abstract DeodexUtil.InlineMethod resolveExecuteInline(AnalyzedInstruction instruction);

    private static class InlineMethodResolver_version35 extends InlineMethodResolver
    {
        private final DeodexUtil.InlineMethod[] inlineMethods;

        public InlineMethodResolver_version35(DeodexUtil deodexUtil) {
            inlineMethods = new DeodexUtil.InlineMethod[] {
                new DeodexUtil.InlineMethod(Static, "Lorg/apache/harmony/dalvik/NativeTestTarget;", "emptyInlineMethod", "", "V"),
                new DeodexUtil.InlineMethod(Virtual, "Ljava/lang/String;", "charAt", "I", "C"),
                new DeodexUtil.InlineMethod(Virtual, "Ljava/lang/String;", "compareTo", "Ljava/lang/String;", "I"),
                new DeodexUtil.InlineMethod(Virtual, "Ljava/lang/String;", "equals", "Ljava/lang/Object;", "Z"),
                new DeodexUtil.InlineMethod(Virtual, "Ljava/lang/String;", "length", "", "I"),
                new DeodexUtil.InlineMethod(Static, "Ljava/lang/Math;", "abs", "I", "I"),
                new DeodexUtil.InlineMethod(Static, "Ljava/lang/Math;", "abs", "J", "J"),
                new DeodexUtil.InlineMethod(Static, "Ljava/lang/Math;", "abs", "F", "F"),
                new DeodexUtil.InlineMethod(Static, "Ljava/lang/Math;", "abs", "D", "D"),
                new DeodexUtil.InlineMethod(Static, "Ljava/lang/Math;", "min", "II", "I"),
                new DeodexUtil.InlineMethod(Static, "Ljava/lang/Math;", "max", "II", "I"),
                new DeodexUtil.InlineMethod(Static, "Ljava/lang/Math;", "sqrt", "D", "D"),
                new DeodexUtil.InlineMethod(Static, "Ljava/lang/Math;", "cos", "D", "D"),
                new DeodexUtil.InlineMethod(Static, "Ljava/lang/Math;", "sin", "D", "D")
            };
        }

        @Override
        public DeodexUtil.InlineMethod resolveExecuteInline(AnalyzedInstruction analyzedInstruction) {
            assert analyzedInstruction.instruction instanceof OdexedInvokeInline;

            OdexedInvokeInline instruction = (OdexedInvokeInline)analyzedInstruction.instruction;
            int inlineIndex = instruction.getInlineIndex();

            if (inlineIndex < 0 || inlineIndex >= inlineMethods.length) {
                throw new RuntimeException("Invalid inline index: " + inlineIndex);
            }
            return inlineMethods[inlineIndex];
        }
    }

    private static class InlineMethodResolver_version36 extends InlineMethodResolver
    {
        private final DeodexUtil.InlineMethod[] inlineMethods;
        private final DeodexUtil.InlineMethod indexOfIMethod;
        private final DeodexUtil.InlineMethod indexOfIIMethod;
        private final DeodexUtil.InlineMethod fastIndexOfMethod;
        private final DeodexUtil.InlineMethod isEmptyMethod;


        public InlineMethodResolver_version36(DeodexUtil deodexUtil) {
            //The 5th and 6th entries differ between froyo and gingerbread. We have to look at the parameters being
            //passed to distinguish between them.

            //froyo
            indexOfIMethod = new DeodexUtil.InlineMethod(Virtual, "Ljava/lang/String;", "indexOf", "I", "I");
            indexOfIIMethod = new DeodexUtil.InlineMethod(Virtual, "Ljava/lang/String;", "indexOf", "II", "I");

            //gingerbread
            fastIndexOfMethod = new DeodexUtil.InlineMethod(Direct, "Ljava/lang/String;", "fastIndexOf", "II", "I");
            isEmptyMethod = new DeodexUtil.InlineMethod(Virtual, "Ljava/lang/String;", "isEmpty", "", "Z");

            inlineMethods = new DeodexUtil.InlineMethod[] {
                new DeodexUtil.InlineMethod(Static, "Lorg/apache/harmony/dalvik/NativeTestTarget;", "emptyInlineMethod", "", "V"),
                new DeodexUtil.InlineMethod(Virtual, "Ljava/lang/String;", "charAt", "I", "C"),
                new DeodexUtil.InlineMethod(Virtual, "Ljava/lang/String;", "compareTo", "Ljava/lang/String;", "I"),
                new DeodexUtil.InlineMethod(Virtual, "Ljava/lang/String;", "equals", "Ljava/lang/Object;", "Z"),
                //froyo: deodexUtil.new InlineMethod(Virtual, "Ljava/lang/String;", "indexOf", "I", "I"),
                //gingerbread: deodexUtil.new InlineMethod(Virtual, "Ljava/lang/String;", "fastIndexOf", "II", "I"),
                null,
                //froyo: deodexUtil.new InlineMethod(Virtual, "Ljava/lang/String;", "indexOf", "II", "I"),
                //gingerbread: deodexUtil.new InlineMethod(Virtual, "Ljava/lang/String;", "isEmpty", "", "Z"),
                null,
                new DeodexUtil.InlineMethod(Virtual, "Ljava/lang/String;", "length", "", "I"),
                new DeodexUtil.InlineMethod(Static, "Ljava/lang/Math;", "abs", "I", "I"),
                new DeodexUtil.InlineMethod(Static, "Ljava/lang/Math;", "abs", "J", "J"),
                new DeodexUtil.InlineMethod(Static, "Ljava/lang/Math;", "abs", "F", "F"),
                new DeodexUtil.InlineMethod(Static, "Ljava/lang/Math;", "abs", "D", "D"),
                new DeodexUtil.InlineMethod(Static, "Ljava/lang/Math;", "min", "II", "I"),
                new DeodexUtil.InlineMethod(Static, "Ljava/lang/Math;", "max", "II", "I"),
                new DeodexUtil.InlineMethod(Static, "Ljava/lang/Math;", "sqrt", "D", "D"),
                new DeodexUtil.InlineMethod(Static, "Ljava/lang/Math;", "cos", "D", "D"),
                new DeodexUtil.InlineMethod(Static, "Ljava/lang/Math;", "sin", "D", "D"),
                new DeodexUtil.InlineMethod(Static, "Ljava/lang/Float;", "floatToIntBits", "F", "I"),
                new DeodexUtil.InlineMethod(Static, "Ljava/lang/Float;", "floatToRawIntBits", "F", "I"),
                new DeodexUtil.InlineMethod(Static, "Ljava/lang/Float;", "intBitsToFloat", "I", "F"),
                new DeodexUtil.InlineMethod(Static, "Ljava/lang/Double;", "doubleToLongBits", "D", "J"),
                new DeodexUtil.InlineMethod(Static, "Ljava/lang/Double;", "doubleToRawLongBits", "D", "J"),
                new DeodexUtil.InlineMethod(Static, "Ljava/lang/Double;", "longBitsToDouble", "J", "D"),
                new DeodexUtil.InlineMethod(Static, "Ljava/lang/StrictMath;", "abs", "I", "I"),
                new DeodexUtil.InlineMethod(Static, "Ljava/lang/StrictMath;", "abs", "J", "J"),
                new DeodexUtil.InlineMethod(Static, "Ljava/lang/StrictMath;", "abs", "F", "F"),
                new DeodexUtil.InlineMethod(Static, "Ljava/lang/StrictMath;", "abs", "D", "D"),
                new DeodexUtil.InlineMethod(Static, "Ljava/lang/StrictMath;", "min", "II", "I"),
                new DeodexUtil.InlineMethod(Static, "Ljava/lang/StrictMath;", "max", "II", "I"),
                new DeodexUtil.InlineMethod(Static, "Ljava/lang/StrictMath;", "sqrt", "D", "D"),
            };
        }

        @Override
        public DeodexUtil.InlineMethod resolveExecuteInline(AnalyzedInstruction analyzedInstruction) {
            assert analyzedInstruction.instruction instanceof OdexedInvokeInline;

            OdexedInvokeInline instruction = (OdexedInvokeInline)analyzedInstruction.instruction;
            int inlineIndex = instruction.getInlineIndex();

            if (inlineIndex < 0 || inlineIndex >= inlineMethods.length) {
                throw new RuntimeException("Invalid method index: " + inlineIndex);
            }

            if (inlineIndex == 4) {
                int parameterCount = getParameterCount(instruction);
                if (parameterCount == 2) {
                    return indexOfIMethod;
                } else if (parameterCount == 3) {
                    return fastIndexOfMethod;
                } else {
                    throw new RuntimeException("Could not determine the correct inline method to use");
                }
            } else if (inlineIndex == 5) {
                int parameterCount = getParameterCount(instruction);
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

        private int getParameterCount(OdexedInvokeInline instruction) {
            return instruction.getRegCount();
        }
    }
}
