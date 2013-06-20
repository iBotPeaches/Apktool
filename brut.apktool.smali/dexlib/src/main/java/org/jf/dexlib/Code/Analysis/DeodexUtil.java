/*
 * [The "BSD licence"]
 * Copyright (c) 2010 Ben Gruver (JesusFreke)
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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DeodexUtil {
    public static final int Virtual = 0;
    public static final int Direct = 1;
    public static final int Static = 2;

    private final InlineMethodResolver inlineMethodResolver;

    public final DexFile dexFile;

    public DeodexUtil(DexFile dexFile) {
        this.dexFile = dexFile;
        OdexHeader odexHeader = dexFile.getOdexHeader();
        if (odexHeader == null) {
            //if there isn't an odex header, why are we creating an DeodexUtil object?
            assert false;
            throw new RuntimeException("Cannot create a DeodexUtil object for a dex file without an odex header");
        }
        inlineMethodResolver = InlineMethodResolver.createInlineMethodResolver(this, odexHeader.version);
    }

    public DeodexUtil(DexFile dexFile, InlineMethodResolver inlineMethodResolver) {
        this.dexFile = dexFile;
        this.inlineMethodResolver = inlineMethodResolver;
    }

    public InlineMethod lookupInlineMethod(AnalyzedInstruction instruction) {
        return inlineMethodResolver.resolveExecuteInline(instruction);
    }

    public FieldIdItem lookupField(ClassPath.ClassDef classDef, int fieldOffset) {
        ClassPath.FieldDef field = classDef.getInstanceField(fieldOffset);
        if (field == null) {
            return null;
        }

        return parseAndResolveField(classDef, field);
    }

    private static final Pattern shortMethodPattern = Pattern.compile("([^(]+)\\(([^)]*)\\)(.+)");

    public MethodIdItem lookupVirtualMethod(ClassPath.ClassDef classDef, int methodIndex) {
        String method = classDef.getVirtualMethod(methodIndex);
        if (method == null) {
            return null;
        }

        Matcher m = shortMethodPattern.matcher(method);
        if (!m.matches()) {
            assert false;
            throw new RuntimeException("Invalid method descriptor: " + method);
        }

        String methodName = m.group(1);
        String methodParams = m.group(2);
        String methodRet = m.group(3);

        if (classDef instanceof ClassPath.UnresolvedClassDef) {
            //if this is an unresolved class, the only way getVirtualMethod could have found a method is if the virtual
            //method being looked up was a method on java.lang.Object.
            classDef = ClassPath.getClassDef("Ljava/lang/Object;");
        } else if (classDef.isInterface()) {
            classDef = classDef.getSuperclass();
            assert classDef != null;
        }

        return parseAndResolveMethod(classDef, methodName, methodParams, methodRet);
    }

    private MethodIdItem parseAndResolveMethod(ClassPath.ClassDef classDef, String methodName, String methodParams,
                                               String methodRet) {
        StringIdItem methodNameItem = StringIdItem.lookupStringIdItem(dexFile, methodName);
        if (methodNameItem == null) {
            return null;
        }

        LinkedList<TypeIdItem> paramList = new LinkedList<TypeIdItem>();

        for (int i=0; i<methodParams.length(); i++) {
            TypeIdItem typeIdItem;

            switch (methodParams.charAt(i)) {
                case 'Z':
                case 'B':
                case 'S':
                case 'C':
                case 'I':
                case 'J':
                case 'F':
                case 'D':
                    typeIdItem = TypeIdItem.lookupTypeIdItem(dexFile, methodParams.substring(i,i+1));
                    break;
                case 'L':
                {
                    int end = methodParams.indexOf(';', i);
                    if (end == -1) {
                        throw new RuntimeException("invalid parameter in the method");
                    }

                    typeIdItem = TypeIdItem.lookupTypeIdItem(dexFile, methodParams.substring(i, end+1));
                    i = end;
                    break;
                }
                case '[':
                {
                    int end;
                    int typeStart = i+1;
                    while (typeStart < methodParams.length() && methodParams.charAt(typeStart) == '[') {
                        typeStart++;
                    }
                    switch (methodParams.charAt(typeStart)) {
                        case 'Z':
                        case 'B':
                        case 'S':
                        case 'C':
                        case 'I':
                        case 'J':
                        case 'F':
                        case 'D':
                            end = typeStart;
                            break;
                        case 'L':
                            end = methodParams.indexOf(';', typeStart);
                            if (end == -1) {
                                throw new RuntimeException("invalid parameter in the method");
                            }
                            break;
                        default:
                            throw new RuntimeException("invalid parameter in the method");
                    }

                    typeIdItem = TypeIdItem.lookupTypeIdItem(dexFile, methodParams.substring(i, end+1));
                    i = end;
                    break;
                }
                default:
                    throw new RuntimeException("invalid parameter in the method");
            }

            if (typeIdItem == null) {
                return null;
            }
            paramList.add(typeIdItem);
        }

        TypeListItem paramListItem = null;
        if (paramList.size() > 0) {
            paramListItem = TypeListItem.lookupTypeListItem(dexFile, paramList);
            if (paramListItem == null) {
                return null;
            }
        }

        TypeIdItem retType = TypeIdItem.lookupTypeIdItem(dexFile, methodRet);
        if (retType == null) {
            return null;
        }

        ProtoIdItem protoItem = ProtoIdItem.lookupProtoIdItem(dexFile, retType, paramListItem);
        if (protoItem == null) {
            return null;
        }

        ClassPath.ClassDef methodClassDef = classDef;

        do {
            TypeIdItem classTypeItem = TypeIdItem.lookupTypeIdItem(dexFile, methodClassDef.getClassType());

            if (classTypeItem != null) {
                MethodIdItem methodIdItem = MethodIdItem.lookupMethodIdItem(dexFile, classTypeItem, protoItem, methodNameItem);
                if (methodIdItem != null) {
                    return methodIdItem;
                }
            }

            methodClassDef = methodClassDef.getSuperclass();
        } while (methodClassDef != null);
        return null;
    }

    private FieldIdItem parseAndResolveField(ClassPath.ClassDef classDef, ClassPath.FieldDef field) {
        String definingClass = field.definingClass;
        String fieldName = field.name;
        String fieldType = field.type;

        StringIdItem fieldNameItem = StringIdItem.lookupStringIdItem(dexFile, fieldName);
        if (fieldNameItem == null) {
            return null;
        }

        TypeIdItem fieldTypeItem = TypeIdItem.lookupTypeIdItem(dexFile, fieldType);
        if (fieldTypeItem == null) {
            return null;
        }

        ClassPath.ClassDef fieldClass = classDef;

        ArrayList<ClassPath.ClassDef> parents = new ArrayList<ClassPath.ClassDef>();
        parents.add(fieldClass);

        while (fieldClass != null && !fieldClass.getClassType().equals(definingClass)) {
            fieldClass = fieldClass.getSuperclass();
            parents.add(fieldClass);
        }

        for (int i=parents.size()-1; i>=0; i--) {
            fieldClass = parents.get(i);

            TypeIdItem classTypeItem = TypeIdItem.lookupTypeIdItem(dexFile, fieldClass.getClassType());
            if (classTypeItem == null) {
                continue;
            }

            FieldIdItem fieldIdItem = FieldIdItem.lookupFieldIdItem(dexFile, classTypeItem, fieldTypeItem, fieldNameItem);
            if (fieldIdItem != null) {
                return fieldIdItem;
            }
        }
        return null;
    }

    public static class InlineMethod {
        public final int methodType;
        public final String classType;
        public final String methodName;
        public final String parameters;
        public final String returnType;

        private MethodIdItem methodIdItem = null;

        InlineMethod(int methodType, String classType, String methodName, String parameters,
                               String returnType) {
            this.methodType = methodType;
            this.classType = classType;
            this.methodName = methodName;
            this.parameters = parameters;
            this.returnType = returnType;
        }

        public MethodIdItem getMethodIdItem(DeodexUtil deodexUtil) {
            if (methodIdItem == null) {
                loadMethod(deodexUtil);
            }
            return methodIdItem;
        }

        private void loadMethod(DeodexUtil deodexUtil) {
            ClassPath.ClassDef classDef = ClassPath.getClassDef(classType);

            this.methodIdItem = deodexUtil.parseAndResolveMethod(classDef, methodName, parameters, returnType);
        }

        public String getMethodString() {
            return String.format("%s->%s(%s)%s", classType, methodName, parameters, returnType);
        }
    }
}
