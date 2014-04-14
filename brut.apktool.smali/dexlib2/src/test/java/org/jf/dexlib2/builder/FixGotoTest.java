package org.jf.dexlib2.builder;

import com.google.common.collect.Lists;
import junit.framework.Assert;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.builder.instruction.BuilderInstruction10t;
import org.jf.dexlib2.builder.instruction.BuilderInstruction10x;
import org.jf.dexlib2.builder.instruction.BuilderInstruction20t;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.OffsetInstruction;
import org.junit.Test;

import java.util.List;

public class FixGotoTest {
    @Test
    public void testFixGotoToGoto16() {
        MethodImplementationBuilder builder = new MethodImplementationBuilder(1);

        Label gotoTarget = builder.getLabel("gotoTarget");
        builder.addInstruction(new BuilderInstruction10t(Opcode.GOTO, gotoTarget));

        for (int i=0; i<500; i++) {
            builder.addInstruction(new BuilderInstruction10x(Opcode.NOP));
        }

        builder.addLabel("gotoTarget");
        builder.addInstruction(new BuilderInstruction10x(Opcode.RETURN_VOID));

        MethodImplementation impl = builder.getMethodImplementation();

        List<? extends Instruction> instructions = Lists.newArrayList(impl.getInstructions());
        Assert.assertEquals(502, instructions.size());

        Assert.assertEquals(Opcode.GOTO_16, instructions.get(0).getOpcode());
        Assert.assertEquals(502, ((OffsetInstruction)instructions.get(0)).getCodeOffset());
    }

    @Test
    public void testFixGotoToGoto32() {
        MethodImplementationBuilder builder = new MethodImplementationBuilder(1);

        Label gotoTarget = builder.getLabel("gotoTarget");
        builder.addInstruction(new BuilderInstruction10t(Opcode.GOTO, gotoTarget));

        for (int i=0; i<70000; i++) {
            builder.addInstruction(new BuilderInstruction10x(Opcode.NOP));
        }

        builder.addLabel("gotoTarget");
        builder.addInstruction(new BuilderInstruction10x(Opcode.RETURN_VOID));

        MethodImplementation impl = builder.getMethodImplementation();

        List<? extends Instruction> instructions = Lists.newArrayList(impl.getInstructions());
        Assert.assertEquals(70002, instructions.size());

        Assert.assertEquals(Opcode.GOTO_32, instructions.get(0).getOpcode());
        Assert.assertEquals(70003, ((OffsetInstruction)instructions.get(0)).getCodeOffset());
    }

    @Test
    public void testFixGoto16ToGoto32() {
        MethodImplementationBuilder builder = new MethodImplementationBuilder(1);

        Label gotoTarget = builder.getLabel("gotoTarget");
        builder.addInstruction(new BuilderInstruction20t(Opcode.GOTO_16, gotoTarget));

        for (int i=0; i<70000; i++) {
            builder.addInstruction(new BuilderInstruction10x(Opcode.NOP));
        }

        builder.addLabel("gotoTarget");
        builder.addInstruction(new BuilderInstruction10x(Opcode.RETURN_VOID));

        MethodImplementation impl = builder.getMethodImplementation();

        List<? extends Instruction> instructions = Lists.newArrayList(impl.getInstructions());
        Assert.assertEquals(70002, instructions.size());

        Assert.assertEquals(Opcode.GOTO_32, instructions.get(0).getOpcode());
        Assert.assertEquals(70003, ((OffsetInstruction)instructions.get(0)).getCodeOffset());
    }

    @Test
    public void testFixGotoCascading() {
        MethodImplementationBuilder builder = new MethodImplementationBuilder(1);

        Label goto16Target = builder.getLabel("goto16Target");
        builder.addInstruction(new BuilderInstruction20t(Opcode.GOTO_16, goto16Target));

        for (int i=0; i<1000; i++) {
            builder.addInstruction(new BuilderInstruction10x(Opcode.NOP));
        }

        Label gotoTarget = builder.getLabel("gotoTarget");
        builder.addInstruction(new BuilderInstruction10t(Opcode.GOTO, gotoTarget));

        for (int i=0; i<499; i++) {
            builder.addInstruction(new BuilderInstruction10x(Opcode.NOP));
        }

        builder.addLabel("gotoTarget");

        for (int i=0; i<31265; i++) {
            builder.addInstruction(new BuilderInstruction10x(Opcode.NOP));
        }

        builder.addLabel("goto16Target");
        builder.addInstruction(new BuilderInstruction10x(Opcode.RETURN_VOID));

        MethodImplementation impl = builder.getMethodImplementation();

        List<? extends Instruction> instructions = Lists.newArrayList(impl.getInstructions());
        Assert.assertEquals(32767, instructions.size());

        Assert.assertEquals(Opcode.GOTO_32, instructions.get(0).getOpcode());
        Assert.assertEquals(32769, ((OffsetInstruction)instructions.get(0)).getCodeOffset());

    }
}
