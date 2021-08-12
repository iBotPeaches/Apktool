package org.jf.dexlib2.immutable.instruction;

import org.jf.dexlib2.Format;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.iface.instruction.formats.Instruction45cc;
import org.jf.dexlib2.iface.reference.Reference;
import org.jf.dexlib2.immutable.reference.ImmutableReference;
import org.jf.dexlib2.immutable.reference.ImmutableReferenceFactory;
import org.jf.dexlib2.util.Preconditions;

import javax.annotation.Nonnull;

public class ImmutableInstruction45cc extends ImmutableInstruction implements Instruction45cc {
    public static final Format FORMAT = Format.Format45cc;

    protected final int registerCount;
    protected final int registerC;
    protected final int registerD;
    protected final int registerE;
    protected final int registerF;
    protected final int registerG;
    @Nonnull protected final ImmutableReference reference;
    @Nonnull protected final ImmutableReference reference2;

    public ImmutableInstruction45cc(@Nonnull Opcode opcode,
                                    int registerCount,
                                    int registerC,
                                    int registerD,
                                    int registerE,
                                    int registerF,
                                    int registerG,
                                    @Nonnull Reference reference,
                                    @Nonnull Reference reference2) {
        super(opcode);
        this.registerCount = Preconditions.check35cAnd45ccRegisterCount(registerCount);
        this.registerC = (registerCount>0) ? Preconditions.checkNibbleRegister(registerC) : 0;
        this.registerD = (registerCount>1) ? Preconditions.checkNibbleRegister(registerD) : 0;
        this.registerE = (registerCount>2) ? Preconditions.checkNibbleRegister(registerE) : 0;
        this.registerF = (registerCount>3) ? Preconditions.checkNibbleRegister(registerF) : 0;
        this.registerG = (registerCount>4) ? Preconditions.checkNibbleRegister(registerG) : 0;
        this.reference = ImmutableReferenceFactory.of(reference);
        this.reference2 = ImmutableReferenceFactory.of(reference2);
    }

    public static ImmutableInstruction45cc of(Instruction45cc instruction) {
        if (instruction instanceof ImmutableInstruction45cc) {
            return (ImmutableInstruction45cc) instruction;
        } else {
            return new ImmutableInstruction45cc(
                    instruction.getOpcode(),
                    instruction.getRegisterCount(),
                    instruction.getRegisterC(),
                    instruction.getRegisterD(),
                    instruction.getRegisterE(),
                    instruction.getRegisterF(),
                    instruction.getRegisterG(),
                    instruction.getReference(),
                    instruction.getReference2());
        }
    }

    @Override public int getRegisterCount() { return registerCount; }
    @Override public int getRegisterC() { return registerC; }
    @Override public int getRegisterD() { return registerD; }
    @Override public int getRegisterE() { return registerE; }
    @Override public int getRegisterF() { return registerF; }
    @Override public int getRegisterG() { return registerG; }

    @Override public ImmutableReference getReference() { return reference; }
    @Override public int getReferenceType() { return opcode.referenceType; }

    @Override public ImmutableReference getReference2() { return reference2; }
    @Override public int getReferenceType2() { return opcode.referenceType2; }

    @Override public Format getFormat() { return FORMAT; }
}
