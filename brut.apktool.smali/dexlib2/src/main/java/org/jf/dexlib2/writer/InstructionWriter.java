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

package org.jf.dexlib2.writer;

import org.jf.dexlib2.ReferenceType;
import org.jf.dexlib2.iface.instruction.ReferenceInstruction;
import org.jf.dexlib2.iface.instruction.SwitchElement;
import org.jf.dexlib2.iface.instruction.formats.*;
import org.jf.dexlib2.iface.reference.FieldReference;
import org.jf.dexlib2.iface.reference.MethodReference;
import org.jf.dexlib2.iface.reference.StringReference;
import org.jf.dexlib2.iface.reference.TypeReference;
import org.jf.util.ExceptionWithContext;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.List;

public class InstructionWriter<StringRef extends StringReference, TypeRef extends TypeReference,
        FieldRefKey extends FieldReference, MethodRefKey extends MethodReference> {
    @Nonnull private final DexDataWriter writer;
    @Nonnull private final StringSection<?, StringRef> stringSection;
    @Nonnull private final TypeSection<?, ?, TypeRef> typeSection;
    @Nonnull private final FieldSection<?, ?, FieldRefKey, ?> fieldSection;
    @Nonnull private final MethodSection<?, ?, ?, MethodRefKey, ?> methodSection;

    @Nonnull static <StringRef extends StringReference, TypeRef extends TypeReference, FieldRefKey extends FieldReference, MethodRefKey extends MethodReference>
            InstructionWriter<StringRef, TypeRef, FieldRefKey, MethodRefKey>
            makeInstructionWriter(
                @Nonnull DexDataWriter writer,
                @Nonnull StringSection<?, StringRef> stringSection,
                @Nonnull TypeSection<?, ?, TypeRef> typeSection,
                @Nonnull FieldSection<?, ?, FieldRefKey, ?> fieldSection,
                @Nonnull MethodSection<?, ?, ?, MethodRefKey, ?> methodSection) {
        return new InstructionWriter<StringRef, TypeRef, FieldRefKey, MethodRefKey>(
                writer, stringSection, typeSection, fieldSection, methodSection);
    }

    InstructionWriter(@Nonnull DexDataWriter writer,
                      @Nonnull StringSection<?, StringRef> stringSection,
                      @Nonnull TypeSection<?, ?, TypeRef> typeSection,
                      @Nonnull FieldSection<?, ?, FieldRefKey, ?> fieldSection,
                      @Nonnull MethodSection<?, ?, ?, MethodRefKey, ?> methodSection) {
        this.writer = writer;
        this.stringSection = stringSection;
        this.typeSection = typeSection;
        this.fieldSection = fieldSection;
        this.methodSection = methodSection;
    }

    public void write(@Nonnull Instruction10t instruction) {
        try {
            writer.write(instruction.getOpcode().value);
            writer.write(instruction.getCodeOffset());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@Nonnull Instruction10x instruction) {
        try {
            writer.write(instruction.getOpcode().value);
            writer.write(0);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@Nonnull Instruction11n instruction) {
        try {
            writer.write(instruction.getOpcode().value);
            writer.write(packNibbles(instruction.getRegisterA(), instruction.getNarrowLiteral()));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@Nonnull Instruction11x instruction) {
        try {
            writer.write(instruction.getOpcode().value);
            writer.write(instruction.getRegisterA());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@Nonnull Instruction12x instruction) {
        try {
            writer.write(instruction.getOpcode().value);
            writer.write(packNibbles(instruction.getRegisterA(), instruction.getRegisterB()));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@Nonnull Instruction20bc instruction) {
        try {
            writer.write(instruction.getOpcode().value);
            writer.write(instruction.getVerificationError());
            writer.writeUshort(getReferenceIndex(instruction));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@Nonnull Instruction20t instruction) {
        try {
            writer.write(instruction.getOpcode().value);
            writer.write(0);
            writer.writeShort(instruction.getCodeOffset());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@Nonnull Instruction21c instruction) {
        try {
            writer.write(instruction.getOpcode().value);
            writer.write(instruction.getRegisterA());
            writer.writeUshort(getReferenceIndex(instruction));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@Nonnull Instruction21ih instruction) {
        try {
            writer.write(instruction.getOpcode().value);
            writer.write(instruction.getRegisterA());
            writer.writeShort(instruction.getHatLiteral());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@Nonnull Instruction21lh instruction) {
        try {
            writer.write(instruction.getOpcode().value);
            writer.write(instruction.getRegisterA());
            writer.writeShort(instruction.getHatLiteral());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@Nonnull Instruction21s instruction) {
        try {
            writer.write(instruction.getOpcode().value);
            writer.write(instruction.getRegisterA());
            writer.writeShort(instruction.getNarrowLiteral());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@Nonnull Instruction21t instruction) {
        try {
            writer.write(instruction.getOpcode().value);
            writer.write(instruction.getRegisterA());
            writer.writeShort(instruction.getCodeOffset());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@Nonnull Instruction22b instruction) {
        try {
            writer.write(instruction.getOpcode().value);
            writer.write(instruction.getRegisterA());
            writer.write(instruction.getRegisterB());
            writer.write(instruction.getNarrowLiteral());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@Nonnull Instruction22c instruction) {
        try {
            writer.write(instruction.getOpcode().value);
            writer.write(packNibbles(instruction.getRegisterA(), instruction.getRegisterB()));
            writer.writeUshort(getReferenceIndex(instruction));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@Nonnull Instruction22s instruction) {
        try {
            writer.write(instruction.getOpcode().value);
            writer.write(packNibbles(instruction.getRegisterA(), instruction.getRegisterB()));
            writer.writeShort(instruction.getNarrowLiteral());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@Nonnull Instruction22t instruction) {
        try {
            writer.write(instruction.getOpcode().value);
            writer.write(packNibbles(instruction.getRegisterA(), instruction.getRegisterB()));
            writer.writeShort(instruction.getCodeOffset());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@Nonnull Instruction22x instruction) {
        try {
            writer.write(instruction.getOpcode().value);
            writer.write(instruction.getRegisterA());
            writer.writeUshort(instruction.getRegisterB());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@Nonnull Instruction23x instruction) {
        try {
            writer.write(instruction.getOpcode().value);
            writer.write(instruction.getRegisterA());
            writer.write(instruction.getRegisterB());
            writer.write(instruction.getRegisterC());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@Nonnull Instruction30t instruction) {
        try {
            writer.write(instruction.getOpcode().value);
            writer.write(0);
            writer.writeInt(instruction.getCodeOffset());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@Nonnull Instruction31c instruction) {
        try {
            writer.write(instruction.getOpcode().value);
            writer.write(instruction.getRegisterA());
            writer.writeInt(getReferenceIndex(instruction));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@Nonnull Instruction31i instruction) {
        try {
            writer.write(instruction.getOpcode().value);
            writer.write(instruction.getRegisterA());
            writer.writeInt(instruction.getNarrowLiteral());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@Nonnull Instruction31t instruction) {
        try {
            writer.write(instruction.getOpcode().value);
            writer.write(instruction.getRegisterA());
            writer.writeInt(instruction.getCodeOffset());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@Nonnull Instruction32x instruction) {
        try {
            writer.write(instruction.getOpcode().value);
            writer.write(0);
            writer.writeUshort(instruction.getRegisterA());
            writer.writeUshort(instruction.getRegisterB());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@Nonnull Instruction35c instruction) {
        try {
            writer.write(instruction.getOpcode().value);
            writer.write(packNibbles(instruction.getRegisterG(), instruction.getRegisterCount()));
            writer.writeUshort(getReferenceIndex(instruction));
            writer.write(packNibbles(instruction.getRegisterC(), instruction.getRegisterD()));
            writer.write(packNibbles(instruction.getRegisterE(), instruction.getRegisterF()));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@Nonnull Instruction3rc instruction) {
        try {
            writer.write(instruction.getOpcode().value);
            writer.write(instruction.getRegisterCount());
            writer.writeUshort(getReferenceIndex(instruction));
            writer.writeUshort(instruction.getStartRegister());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@Nonnull Instruction51l instruction) {
        try {
            writer.write(instruction.getOpcode().value);
            writer.write(instruction.getRegisterA());
            writer.writeLong(instruction.getWideLiteral());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@Nonnull ArrayPayload instruction) {
        try {
            writer.writeUshort(instruction.getOpcode().value);
            writer.writeUshort(instruction.getElementWidth());
            List<Number> elements = instruction.getArrayElements();
            writer.writeInt(elements.size());
            switch (instruction.getElementWidth()) {
                case 1:
                    for (Number element: elements) {
                        writer.write(element.byteValue());
                    }
                    break;
                case 2:
                    for (Number element: elements) {
                        writer.writeShort(element.shortValue());
                    }
                    break;
                case 4:
                    for (Number element: elements) {
                        writer.writeInt(element.intValue());
                    }
                    break;
                case 8:
                    for (Number element: elements) {
                        writer.writeLong(element.longValue());
                    }
                    break;
            }
            if ((writer.getPosition() & 1) != 0) {
                writer.write(0);
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@Nonnull SparseSwitchPayload instruction) {
        try {
            writer.writeUbyte(0);
            writer.writeUbyte(instruction.getOpcode().value >> 8);
            List<? extends SwitchElement> elements = instruction.getSwitchElements();
            writer.writeUshort(elements.size());
            for (SwitchElement element: elements) {
                writer.writeInt(element.getKey());
            }
            for (SwitchElement element: elements) {
                writer.writeInt(element.getOffset());
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@Nonnull PackedSwitchPayload instruction) {
        try {
            writer.writeUbyte(0);
            writer.writeUbyte(instruction.getOpcode().value >> 8);
            List<? extends SwitchElement> elements = instruction.getSwitchElements();
            writer.writeUshort(elements.size());
            if (elements.size() == 0) {
                writer.writeInt(0);
            } else {
                writer.writeInt(elements.get(0).getKey());
                for (SwitchElement element: elements) {
                    writer.writeInt(element.getOffset());
                }
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static int packNibbles(int a, int b) {
        return (b << 4) | a;
    }

    private int getReferenceIndex(ReferenceInstruction referenceInstruction) {
        switch (referenceInstruction.getOpcode().referenceType) {
            case ReferenceType.FIELD:
                return fieldSection.getItemIndex((FieldRefKey)referenceInstruction.getReference());
            case ReferenceType.METHOD:
                return methodSection.getItemIndex((MethodRefKey)referenceInstruction.getReference());
            case ReferenceType.STRING:
                return stringSection.getItemIndex((StringRef)referenceInstruction.getReference());
            case ReferenceType.TYPE:
                return typeSection.getItemIndex((TypeRef)referenceInstruction.getReference());
            default:
                throw new ExceptionWithContext("Unknown reference type: %d",
                        referenceInstruction.getOpcode().referenceType);
        }
    }
}
