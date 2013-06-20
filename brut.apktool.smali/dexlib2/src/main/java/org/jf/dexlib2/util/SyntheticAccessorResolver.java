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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.jf.dexlib2.AccessFlags;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.ReferenceInstruction;
import org.jf.dexlib2.iface.reference.MethodReference;
import org.jf.dexlib2.iface.reference.Reference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

public class SyntheticAccessorResolver {
    public static final int METHOD = 0;
    public static final int GETTER = 1;
    public static final int SETTER = 2;
    public static final int POSTFIX_INCREMENT = 3;
    public static final int PREFIX_INCREMENT = 4;
    public static final int POSTFIX_DECREMENT = 5;
    public static final int PREFIX_DECREMENT = 6;
    public static final int ADD_ASSIGNMENT = 7;
    public static final int SUB_ASSIGNMENT = 8;
    public static final int MUL_ASSIGNMENT = 9;
    public static final int DIV_ASSIGNMENT = 10;
    public static final int REM_ASSIGNMENT = 11;
    public static final int AND_ASSIGNMENT = 12;
    public static final int OR_ASSIGNMENT = 13;
    public static final int XOR_ASSIGNMENT = 14;
    public static final int SHL_ASSIGNMENT = 15;
    public static final int SHR_ASSIGNMENT = 16;
    public static final int USHR_ASSIGNMENT = 17;

    private final Map<String, ClassDef> classDefMap;
    private final Map<String, AccessedMember> resolvedAccessors = Maps.newConcurrentMap();

    public SyntheticAccessorResolver(Iterable<? extends ClassDef> classDefs) {
        ImmutableMap.Builder<String, ClassDef> builder = ImmutableMap.builder();

        for (ClassDef classDef: classDefs) {
            builder.put(classDef.getType(), classDef);
        }

        this.classDefMap = builder.build();
    }

    public static boolean looksLikeSyntheticAccessor(String methodName) {
        return methodName.startsWith("access$");
    }

    @Nullable
    public AccessedMember getAccessedMember(@Nonnull MethodReference methodReference) {
        String methodDescriptor = ReferenceUtil.getMethodDescriptor(methodReference);

        AccessedMember accessedMember = resolvedAccessors.get(methodDescriptor);
        if (accessedMember != null) {
            return accessedMember;
        }

        String type = methodReference.getDefiningClass();
        ClassDef classDef = classDefMap.get(type);
        if (classDef == null) {
            return null;
        }

        Method matchedMethod = null;
        MethodImplementation matchedMethodImpl = null;
        for (Method method: classDef.getMethods()) {
            MethodImplementation methodImpl = method.getImplementation();
            if (methodImpl != null) {
                if (methodReferenceEquals(method, methodReference)) {
                    matchedMethod = method;
                    matchedMethodImpl = methodImpl;
                    break;
                }
            }
        }

        if (matchedMethod == null) {
            return null;
        }

        //A synthetic accessor will be marked synthetic
        if (!AccessFlags.SYNTHETIC.isSet(matchedMethod.getAccessFlags())) {
            return null;
        }

        List<Instruction> instructions = ImmutableList.copyOf(matchedMethodImpl.getInstructions());

        int accessType = SyntheticAccessorFSM.test(instructions);

        if (accessType >= 0) {
            AccessedMember member =
                    new AccessedMember(accessType, ((ReferenceInstruction)instructions.get(0)).getReference());
            resolvedAccessors.put(methodDescriptor, member);
            return member;
        }
        return null;
    }

    public static class AccessedMember {
        public final int accessedMemberType;
        @Nonnull public final Reference accessedMember;

        public AccessedMember(int accessedMemberType, @Nonnull Reference accessedMember) {
            this.accessedMemberType = accessedMemberType;
            this.accessedMember = accessedMember;
        }
    }

    private static boolean methodReferenceEquals(@Nonnull MethodReference ref1, @Nonnull MethodReference ref2) {
        // we already know the containing class matches
        return ref1.getName().equals(ref2.getName()) &&
               ref1.getReturnType().equals(ref2.getReturnType()) &&
               ref1.getParameterTypes().equals(ref2.getParameterTypes());
    }
}
