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

package org.jf.baksmali.Adaptors;

import org.jf.dexlib.Util.Utf8Utils;
import org.jf.util.CommentingIndentingWriter;
import org.jf.util.IndentingWriter;
import org.jf.dexlib.*;
import org.jf.dexlib.Code.Analysis.ValidationException;
import org.jf.dexlib.Code.Format.Instruction21c;
import org.jf.dexlib.Code.Format.Instruction41c;
import org.jf.dexlib.Code.Instruction;
import org.jf.dexlib.EncodedValue.EncodedValue;
import org.jf.dexlib.Util.AccessFlags;
import org.jf.dexlib.Util.SparseArray;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;

public class ClassDefinition {
    private ClassDefItem classDefItem;
    @Nullable
    private ClassDataItem classDataItem;

    private SparseArray<FieldIdItem> fieldsSetInStaticConstructor;

    protected boolean validationErrors;

    public ClassDefinition(ClassDefItem classDefItem) {
        this.classDefItem = classDefItem;
        this.classDataItem = classDefItem.getClassData();
        findFieldsSetInStaticConstructor();
    }

    public boolean hadValidationErrors() {
        return validationErrors;
    }

    private void findFieldsSetInStaticConstructor() {
        fieldsSetInStaticConstructor = new SparseArray<FieldIdItem>();

        if (classDataItem == null) {
            return;
        }

        for (ClassDataItem.EncodedMethod directMethod: classDataItem.getDirectMethods()) {
            if (directMethod.method.getMethodName().getStringValue().equals("<clinit>") &&
                    directMethod.codeItem != null) {
                for (Instruction instruction: directMethod.codeItem.getInstructions()) {
                    switch (instruction.opcode) {
                        case SPUT:
                        case SPUT_BOOLEAN:
                        case SPUT_BYTE:
                        case SPUT_CHAR:
                        case SPUT_OBJECT:
                        case SPUT_SHORT:
                        case SPUT_WIDE: {
                            Instruction21c ins = (Instruction21c)instruction;
                            FieldIdItem fieldIdItem = (FieldIdItem)ins.getReferencedItem();
                            fieldsSetInStaticConstructor.put(fieldIdItem.getIndex(), fieldIdItem);
                            break;
                        }
                        case SPUT_JUMBO:
                        case SPUT_BOOLEAN_JUMBO:
                        case SPUT_BYTE_JUMBO:
                        case SPUT_CHAR_JUMBO:
                        case SPUT_OBJECT_JUMBO:
                        case SPUT_SHORT_JUMBO:
                        case SPUT_WIDE_JUMBO: {
                            Instruction41c ins = (Instruction41c)instruction;
                            FieldIdItem fieldIdItem = (FieldIdItem)ins.getReferencedItem();
                            fieldsSetInStaticConstructor.put(fieldIdItem.getIndex(), fieldIdItem);
                            break;
                        }
                    }
                }
            }
        }
    }

    public void writeTo(IndentingWriter writer) throws IOException {
        writeClass(writer);
        writeSuper(writer);
        writeSourceFile(writer);
        writeInterfaces(writer);
        writeAnnotations(writer);
        writeStaticFields(writer);
        writeInstanceFields(writer);
        writeDirectMethods(writer);
        writeVirtualMethods(writer);
    }

    private void writeClass(IndentingWriter writer) throws IOException {
        writer.write(".class ");
        writeAccessFlags(writer);
        writer.write(classDefItem.getClassType().getTypeDescriptor());
        writer.write('\n');
    }

    private void writeAccessFlags(IndentingWriter writer) throws IOException {
        for (AccessFlags accessFlag: AccessFlags.getAccessFlagsForClass(classDefItem.getAccessFlags())) {
            writer.write(accessFlag.toString());
            writer.write(' ');
        }
    }

    private void writeSuper(IndentingWriter writer) throws IOException {
        TypeIdItem superClass = classDefItem.getSuperclass();
        if (superClass != null) {
            writer.write(".super ");
            writer.write(superClass.getTypeDescriptor());
            writer.write('\n');
        }
    }

    private void writeSourceFile(IndentingWriter writer) throws IOException {
        StringIdItem sourceFile = classDefItem.getSourceFile();
        if (sourceFile != null) {
            writer.write(".source \"");
            Utf8Utils.writeEscapedString(writer, sourceFile.getStringValue());
            writer.write("\"\n");
        }
    }

    private void writeInterfaces(IndentingWriter writer) throws IOException {
        TypeListItem interfaceList = classDefItem.getInterfaces();
        if (interfaceList == null) {
            return;
        }

        List<TypeIdItem> interfaces = interfaceList.getTypes();
        if (interfaces == null || interfaces.size() == 0) {
            return;
        }

        writer.write('\n');
        writer.write("# interfaces\n");
        for (TypeIdItem typeIdItem: interfaceList.getTypes()) {
            writer.write(".implements ");
            writer.write(typeIdItem.getTypeDescriptor());
            writer.write('\n');
        }
    }

    private void writeAnnotations(IndentingWriter writer) throws IOException {
        AnnotationDirectoryItem annotationDirectory = classDefItem.getAnnotations();
        if (annotationDirectory == null) {
            return;
        }

        AnnotationSetItem annotationSet = annotationDirectory.getClassAnnotations();
        if (annotationSet == null) {
            return;
        }

        writer.write("\n\n");
        writer.write("# annotations\n");
        AnnotationFormatter.writeTo(writer, annotationSet);
    }

    private void writeStaticFields(IndentingWriter writer) throws IOException {
        if (classDataItem == null) {
            return;
        }
        //if classDataItem is not null, then classDefItem won't be null either
        assert(classDefItem != null);

        EncodedArrayItem encodedStaticInitializers = classDefItem.getStaticFieldInitializers();

        EncodedValue[] staticInitializers;
        if (encodedStaticInitializers != null) {
            staticInitializers = encodedStaticInitializers.getEncodedArray().values;
        } else {
            staticInitializers = new EncodedValue[0];
        }

        List<ClassDataItem.EncodedField> encodedFields = classDataItem.getStaticFields();
        if (encodedFields.size() == 0) {
            return;
        }

        writer.write("\n\n");
        writer.write("# static fields\n");

        for (int i=0; i<encodedFields.size(); i++) {
            if (i > 0) {
                writer.write('\n');
            }

            ClassDataItem.EncodedField field = encodedFields.get(i);
            EncodedValue encodedValue = null;
            if (i < staticInitializers.length) {
                encodedValue = staticInitializers[i];
            }
            AnnotationSetItem fieldAnnotations = null;
            AnnotationDirectoryItem annotations = classDefItem.getAnnotations();
            if (annotations != null) {
                fieldAnnotations = annotations.getFieldAnnotations(field.field);
            }

            IndentingWriter fieldWriter = writer;
            // the encoded fields are sorted, so we just have to compare with the previous one to detect duplicates
            if (i > 0 && field.equals(encodedFields.get(i-1))) {
                fieldWriter = new CommentingIndentingWriter(writer, "#");
                fieldWriter.write("Ignoring field with duplicate signature\n");
                System.err.println(String.format("Warning: class %s has duplicate static field %s, Ignoring.",
                        classDefItem.getClassType().getTypeDescriptor(), field.field.getShortFieldString()));
            }

            boolean setInStaticConstructor =
                    fieldsSetInStaticConstructor.get(field.field.getIndex()) != null;

            FieldDefinition.writeTo(fieldWriter, field, encodedValue, fieldAnnotations, setInStaticConstructor);
        }
    }

    private void writeInstanceFields(IndentingWriter writer) throws IOException {
        if (classDataItem == null) {
            return;
        }

        List<ClassDataItem.EncodedField> encodedFields = classDataItem.getInstanceFields();
        if (encodedFields.size() == 0) {
            return;
        }

        writer.write("\n\n");
        writer.write("# instance fields\n");
        for (int i=0; i<encodedFields.size(); i++) {
            ClassDataItem.EncodedField field = encodedFields.get(i);

            if (i > 0) {
                writer.write('\n');
            }

            AnnotationSetItem fieldAnnotations = null;
            AnnotationDirectoryItem annotations = classDefItem.getAnnotations();
            if (annotations != null) {
                fieldAnnotations = annotations.getFieldAnnotations(field.field);
            }

            IndentingWriter fieldWriter = writer;
            // the encoded fields are sorted, so we just have to compare with the previous one to detect duplicates
            if (i > 0 && field.equals(encodedFields.get(i-1))) {
                fieldWriter = new CommentingIndentingWriter(writer, "#");
                fieldWriter.write("Ignoring field with duplicate signature\n");
                System.err.println(String.format("Warning: class %s has duplicate instance field %s, Ignoring.",
                        classDefItem.getClassType().getTypeDescriptor(), field.field.getShortFieldString()));
            }

            FieldDefinition.writeTo(fieldWriter, field, null, fieldAnnotations, false);
        }
    }

    private void writeDirectMethods(IndentingWriter writer) throws IOException {
        if (classDataItem == null) {
            return;
        }

        List<ClassDataItem.EncodedMethod> directMethods = classDataItem.getDirectMethods();
        if (directMethods.size() == 0) {
            return;
        }

        writer.write("\n\n");
        writer.write("# direct methods\n");
        writeMethods(writer, directMethods);
    }

    private void writeVirtualMethods(IndentingWriter writer) throws IOException {
        if (classDataItem == null) {
            return;
        }

        List<ClassDataItem.EncodedMethod> virtualMethods = classDataItem.getVirtualMethods();

        if (virtualMethods.size() == 0) {
            return;
        }

        writer.write("\n\n");
        writer.write("# virtual methods\n");
        writeMethods(writer, virtualMethods);
    }

    private void writeMethods(IndentingWriter writer, List<ClassDataItem.EncodedMethod> methods) throws IOException {
        for (int i=0; i<methods.size(); i++) {
            ClassDataItem.EncodedMethod method = methods.get(i);
            if (i > 0) {
                writer.write('\n');
            }

            AnnotationSetItem methodAnnotations = null;
            AnnotationSetRefList parameterAnnotations = null;
            AnnotationDirectoryItem annotations = classDefItem.getAnnotations();
            if (annotations != null) {
                methodAnnotations = annotations.getMethodAnnotations(method.method);
                parameterAnnotations = annotations.getParameterAnnotations(method.method);
            }

            IndentingWriter methodWriter = writer;
            // the encoded methods are sorted, so we just have to compare with the previous one to detect duplicates
            if (i > 0 && method.equals(methods.get(i-1))) {
                methodWriter = new CommentingIndentingWriter(writer, "#");
                methodWriter.write("Ignoring method with duplicate signature\n");
                System.err.println(String.format("Warning: class %s has duplicate method %s, Ignoring.",
                        classDefItem.getClassType().getTypeDescriptor(), method.method.getShortMethodString()));
            }

            MethodDefinition methodDefinition = new MethodDefinition(method);
            methodDefinition.writeTo(methodWriter, methodAnnotations, parameterAnnotations);

            ValidationException validationException = methodDefinition.getValidationException();
            if (validationException != null) {
                System.err.println(String.format("Error while disassembling method %s. Continuing.",
                        method.method.getMethodString()));
                validationException.printStackTrace(System.err);
                this.validationErrors = true;
            }
        }
    }
}
