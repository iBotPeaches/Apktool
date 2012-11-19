/*
 * [The "BSD licence"]
 * Copyright (c) 2011 Ben Gruver
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

import org.jf.dexlib.*;
import org.jf.dexlib.Code.Format.Instruction22c;
import org.jf.dexlib.Code.Instruction;
import org.jf.dexlib.Code.InstructionWithReference;
import org.jf.dexlib.Util.AccessFlags;

import java.util.HashMap;

public class SyntheticAccessorResolver {
    public static final int METHOD = 0;
    public static final int GETTER = 1;
    public static final int SETTER = 2;

    private final DexFileClassMap classMap;
    private final HashMap<MethodIdItem, AccessedMember> resolvedAccessors = new HashMap<MethodIdItem, AccessedMember>();

    public SyntheticAccessorResolver(DexFile dexFile) {
        classMap = new DexFileClassMap(dexFile);
    }

    public static boolean looksLikeSyntheticAccessor(MethodIdItem methodIdItem) {
        return methodIdItem.getMethodName().getStringValue().startsWith("access$");
    }

    public AccessedMember getAccessedMember(MethodIdItem methodIdItem) {
        AccessedMember accessedMember = resolvedAccessors.get(methodIdItem);
        if (accessedMember != null) {
            return accessedMember;
        }

        ClassDefItem classDefItem = classMap.getClassDefByType(methodIdItem.getContainingClass());
        if (classDefItem == null) {
            return null;
        }

        ClassDataItem classDataItem = classDefItem.getClassData();
        if (classDataItem == null) {
            return null;
        }

        ClassDataItem.EncodedMethod encodedMethod = classDataItem.findDirectMethodByMethodId(methodIdItem);
        if (encodedMethod == null) {
            return null;
        }

        //A synthetic accessor will be marked synthetic
        if ((encodedMethod.accessFlags & AccessFlags.SYNTHETIC.getValue()) == 0) {
            return null;
        }

        Instruction[] instructions = encodedMethod.codeItem.getInstructions();

        //TODO: add support for odexed formats
        switch (instructions[0].opcode.format) {
            case Format35c:
            case Format3rc: {
                //a synthetic method access should be either 2 or 3 instructions, depending on if the method returns
                //anything or not
                if (instructions.length < 2 || instructions.length > 3) {
                    return null;
                }
                InstructionWithReference instruction = (InstructionWithReference)instructions[0];
                Item referencedItem = instruction.getReferencedItem();
                if (!(referencedItem instanceof  MethodIdItem)) {
                    return null;
                }
                MethodIdItem referencedMethodIdItem = (MethodIdItem)referencedItem;

                accessedMember = new AccessedMember(METHOD, referencedMethodIdItem);
                resolvedAccessors.put(methodIdItem, accessedMember);
                return accessedMember;
            }
            case Format22c: {
                //a synthetic field access should be exactly 2 instructions. The set/put, and then the return
                if (instructions.length != 2) {
                    return null;
                }
                Instruction22c instruction = (Instruction22c)instructions[0];
                Item referencedItem = instruction.getReferencedItem();
                if (!(referencedItem instanceof FieldIdItem)) {
                    return null;
                }
                FieldIdItem referencedFieldIdItem = (FieldIdItem)referencedItem;

                if (instruction.opcode.setsRegister() || instruction.opcode.setsWideRegister()) {
                    //If the instruction sets a register, that means it is a getter - it gets the field value and
                    //stores it in the register
                    accessedMember = new AccessedMember(GETTER, referencedFieldIdItem);
                } else {
                    accessedMember = new AccessedMember(SETTER, referencedFieldIdItem);
                }

                resolvedAccessors.put(methodIdItem, accessedMember);
                return accessedMember;
            }
            default:
                return null;
        }
    }

    public static class AccessedMember {
        public final int accessedMemberType;
        public final Item accessedMember;

        public AccessedMember(int accessedMemberType, Item accessedMember) {
            this.accessedMemberType = accessedMemberType;
            this.accessedMember = accessedMember;
        }
    }
}
