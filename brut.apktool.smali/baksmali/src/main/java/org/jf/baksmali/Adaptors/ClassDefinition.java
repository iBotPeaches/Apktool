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

import com.google.common.collect.Lists;
import org.jf.baksmali.baksmaliOptions;
import org.jf.dexlib2.AccessFlags;
import org.jf.dexlib2.dexbacked.DexBackedClassDef;
import org.jf.dexlib2.iface.*;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.formats.Instruction21c;
import org.jf.dexlib2.iface.reference.FieldReference;
import org.jf.dexlib2.util.ReferenceUtil;
import org.jf.util.IndentingWriter;
import org.jf.util.StringUtils;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.*;

public class ClassDefinition {
    @Nonnull public final baksmaliOptions options;
    @Nonnull public final ClassDef classDef;
    @Nonnull private final HashSet<String> fieldsSetInStaticConstructor;

    protected boolean validationErrors;

    public ClassDefinition(@Nonnull baksmaliOptions options, @Nonnull ClassDef classDef) {
        this.options = options;
        this.classDef = classDef;
        fieldsSetInStaticConstructor = findFieldsSetInStaticConstructor();
    }

    public boolean hadValidationErrors() {
        return validationErrors;
    }

    @Nonnull
    private HashSet<String> findFieldsSetInStaticConstructor() {
        HashSet<String> fieldsSetInStaticConstructor = new HashSet<String>();

        for (Method method: classDef.getDirectMethods()) {
            if (method.getName().equals("<clinit>")) {
                MethodImplementation impl = method.getImplementation();
                if (impl != null) {
                    for (Instruction instruction: impl.getInstructions()) {
                        switch (instruction.getOpcode()) {
                            case SPUT:
                            case SPUT_BOOLEAN:
                            case SPUT_BYTE:
                            case SPUT_CHAR:
                            case SPUT_OBJECT:
                            case SPUT_SHORT:
                            case SPUT_WIDE: {
                                Instruction21c ins = (Instruction21c)instruction;
                                FieldReference fieldRef = (FieldReference)ins.getReference();
                                if (fieldRef.getDefiningClass().equals((classDef.getType()))) {
                                    fieldsSetInStaticConstructor.add(ReferenceUtil.getShortFieldDescriptor(fieldRef));
                                }
                                break;
                            }
                        }
                    }
                }
            }
        }
        return fieldsSetInStaticConstructor;
    }

    public void writeTo(IndentingWriter writer) throws IOException {
        writeClass(writer);
        writeSuper(writer);
        writeSourceFile(writer);
        writeInterfaces(writer);
        writeAnnotations(writer);
        Set<String> staticFields = writeStaticFields(writer);
        writeInstanceFields(writer, staticFields);
        Set<String> directMethods = writeDirectMethods(writer);
        writeVirtualMethods(writer, directMethods);
    }

    private void writeClass(IndentingWriter writer) throws IOException {
        writer.write(".class ");
        writeAccessFlags(writer);
        writer.write(classDef.getType());
        writer.write('\n');
    }

    private void writeAccessFlags(IndentingWriter writer) throws IOException {
        for (AccessFlags accessFlag: AccessFlags.getAccessFlagsForClass(classDef.getAccessFlags())) {
            writer.write(accessFlag.toString());
            writer.write(' ');
        }
    }

    private void writeSuper(IndentingWriter writer) throws IOException {
        String superClass = classDef.getSuperclass();
        if (superClass != null) {
            writer.write(".super ");
            writer.write(superClass);
            writer.write('\n');
        }
    }

    private void writeSourceFile(IndentingWriter writer) throws IOException {
        String sourceFile = classDef.getSourceFile();
        if (sourceFile != null) {
            writer.write(".source \"");
            StringUtils.writeEscapedString(writer, sourceFile);
            writer.write("\"\n");
        }
    }

    private void writeInterfaces(IndentingWriter writer) throws IOException {
        List<String> interfaces = Lists.newArrayList(classDef.getInterfaces());
        Collections.sort(interfaces);

        if (interfaces.size() != 0) {
            writer.write('\n');
            writer.write("# interfaces\n");
            for (String interfaceName: interfaces) {
                writer.write(".implements ");
                writer.write(interfaceName);
                writer.write('\n');
            }
        }
    }

    private void writeAnnotations(IndentingWriter writer) throws IOException {
        Collection<? extends Annotation> classAnnotations = classDef.getAnnotations();
        if (classAnnotations.size() != 0) {
            writer.write("\n\n");
            writer.write("# annotations\n");
            AnnotationFormatter.writeTo(writer, classAnnotations);
        }
    }

    private Set<String> writeStaticFields(IndentingWriter writer) throws IOException {
        boolean wroteHeader = false;
        Set<String> writtenFields = new HashSet<String>();

        Iterable<? extends Field> staticFields;
        if (classDef instanceof DexBackedClassDef) {
            staticFields = ((DexBackedClassDef)classDef).getStaticFields(false);
        } else {
            staticFields = classDef.getStaticFields();
        }

        for (Field field: staticFields) {
            if (!wroteHeader) {
                writer.write("\n\n");
                writer.write("# static fields");
                wroteHeader = true;
            }
            writer.write('\n');

            boolean setInStaticConstructor;
            IndentingWriter fieldWriter = writer;
            String fieldString = ReferenceUtil.getShortFieldDescriptor(field);
            if (!writtenFields.add(fieldString)) {
                writer.write("# duplicate field ignored\n");
                fieldWriter = new CommentingIndentingWriter(writer);
                System.err.println(String.format("Ignoring duplicate field: %s->%s", classDef.getType(), fieldString));
                setInStaticConstructor = false;
            } else {
                setInStaticConstructor = fieldsSetInStaticConstructor.contains(fieldString);
            }
            FieldDefinition.writeTo(fieldWriter, field, setInStaticConstructor);
        }
        return writtenFields;
    }

    private void writeInstanceFields(IndentingWriter writer, Set<String> staticFields) throws IOException {
        boolean wroteHeader = false;
        Set<String> writtenFields = new HashSet<String>();

        Iterable<? extends Field> instanceFields;
        if (classDef instanceof DexBackedClassDef) {
            instanceFields = ((DexBackedClassDef)classDef).getInstanceFields(false);
        } else {
            instanceFields = classDef.getInstanceFields();
        }

        for (Field field: instanceFields) {
            if (!wroteHeader) {
                writer.write("\n\n");
                writer.write("# instance fields");
                wroteHeader = true;
            }
            writer.write('\n');

            IndentingWriter fieldWriter = writer;
            String fieldString = ReferenceUtil.getShortFieldDescriptor(field);
            if (!writtenFields.add(fieldString)) {
                writer.write("# duplicate field ignored\n");
                fieldWriter = new CommentingIndentingWriter(writer);
                System.err.println(String.format("Ignoring duplicate field: %s->%s", classDef.getType(), fieldString));
            } else if (staticFields.contains(fieldString)) {
                System.err.println(String.format("Duplicate static+instance field found: %s->%s",
                        classDef.getType(), fieldString));
                System.err.println("You will need to rename one of these fields, including all references.");

                writer.write("# There is both a static and instance field with this signature.\n" +
                             "# You will need to rename one of these fields, including all references.\n");
            }
            FieldDefinition.writeTo(fieldWriter, field, false);
        }
    }

    private Set<String> writeDirectMethods(IndentingWriter writer) throws IOException {
        boolean wroteHeader = false;
        Set<String> writtenMethods = new HashSet<String>();

        Iterable<? extends Method> directMethods;
        if (classDef instanceof DexBackedClassDef) {
            directMethods = ((DexBackedClassDef)classDef).getDirectMethods(false);
        } else {
            directMethods = classDef.getDirectMethods();
        }

        for (Method method: directMethods) {
            if (!wroteHeader) {
                writer.write("\n\n");
                writer.write("# direct methods");
                wroteHeader = true;
            }
            writer.write('\n');

            // TODO: check for method validation errors
            String methodString = ReferenceUtil.getShortMethodDescriptor(method);

            IndentingWriter methodWriter = writer;
            if (!writtenMethods.add(methodString)) {
                writer.write("# duplicate method ignored\n");
                methodWriter = new CommentingIndentingWriter(writer);
            }

            MethodImplementation methodImpl = method.getImplementation();
            if (methodImpl == null) {
                MethodDefinition.writeEmptyMethodTo(methodWriter, method, options);
            } else {
                MethodDefinition methodDefinition = new MethodDefinition(this, method, methodImpl);
                methodDefinition.writeTo(methodWriter);
            }
        }
        return writtenMethods;
    }

    private void writeVirtualMethods(IndentingWriter writer, Set<String> directMethods) throws IOException {
        boolean wroteHeader = false;
        Set<String> writtenMethods = new HashSet<String>();

        Iterable<? extends Method> virtualMethods;
        if (classDef instanceof DexBackedClassDef) {
            virtualMethods = ((DexBackedClassDef)classDef).getVirtualMethods(false);
        } else {
            virtualMethods = classDef.getVirtualMethods();
        }

        for (Method method: virtualMethods) {
            if (!wroteHeader) {
                writer.write("\n\n");
                writer.write("# virtual methods");
                wroteHeader = true;
            }
            writer.write('\n');

            // TODO: check for method validation errors
            String methodString = ReferenceUtil.getShortMethodDescriptor(method);

            IndentingWriter methodWriter = writer;
            if (!writtenMethods.add(methodString)) {
                writer.write("# duplicate method ignored\n");
                methodWriter = new CommentingIndentingWriter(writer);
            } else if (directMethods.contains(methodString)) {
                writer.write("# There is both a direct and virtual method with this signature.\n" +
                             "# You will need to rename one of these methods, including all references.\n");
                System.err.println(String.format("Duplicate direct+virtual method found: %s->%s",
                        classDef.getType(), methodString));
                System.err.println("You will need to rename one of these methods, including all references.");
            }

            MethodImplementation methodImpl = method.getImplementation();
            if (methodImpl == null) {
                MethodDefinition.writeEmptyMethodTo(methodWriter, method, options);
            } else {
                MethodDefinition methodDefinition = new MethodDefinition(this, method, methodImpl);
                methodDefinition.writeTo(methodWriter);
            }
        }
    }
}
