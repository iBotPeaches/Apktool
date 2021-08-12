package org.jf.dexlib2.immutable.instruction;

import org.jf.dexlib2.Format;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.iface.instruction.formats.Instruction4rcc;
import org.jf.dexlib2.iface.reference.Reference;
import org.jf.dexlib2.immutable.reference.ImmutableReference;
import org.jf.dexlib2.immutable.reference.ImmutableReferenceFactory;
import org.jf.dexlib2.util.Preconditions;

import javax.annotation.Nonnull;

public class ImmutableInstruction4rcc extends ImmutableInstruction implements Instruction4rcc {
    private static final Format FORMAT = Format.Format4rcc;

    protected final int startRegister;
    protected final int registerCount;

    @Nonnull protected final ImmutableReference reference;
    @Nonnull protected final ImmutableReference reference2;

    public ImmutableInstruction4rcc(
            @Nonnull Opcode opcode,
            int startRegister,
            int registerCount,
            @Nonnull Reference reference,
            @Nonnull Reference reference2) {
        super(opcode);
        this.startRegister = Preconditions.checkShortRegister(startRegister);
        this.registerCount = Preconditions.checkRegisterRangeCount(registerCount);
        this.reference = ImmutableReferenceFactory.of(reference);
        this.reference2 = ImmutableReferenceFactory.of(reference2);
    }

    public static ImmutableInstruction4rcc of(Instruction4rcc instruction) {
        if (instruction instanceof ImmutableInstruction4rcc) {
            return (ImmutableInstruction4rcc) instruction;
        }
        return new ImmutableInstruction4rcc(
                instruction.getOpcode(),
                instruction.getStartRegister(),
                instruction.getRegisterCount(),
                instruction.getReference(),
                instruction.getReference2());
    }

    @Override public int getStartRegister() { return startRegister; }
    @Override public int getRegisterCount() { return registerCount; }

    @Override public Reference getReference() { return reference; }
    @Override public int getReferenceType() { return opcode.referenceType; }

    @Override public Reference getReference2() { return reference2; }
    @Override public int getReferenceType2() { return opcode.referenceType2; }

    @Override public Format getFormat() { return FORMAT; }
}
