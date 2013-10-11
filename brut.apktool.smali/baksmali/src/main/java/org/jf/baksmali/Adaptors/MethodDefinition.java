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

import com.google.common.collect.ImmutableList;
import org.jf.baksmali.Adaptors.Debug.DebugMethodItem;
import org.jf.baksmali.Adaptors.Format.InstructionMethodItemFactory;
import org.jf.baksmali.baksmaliOptions;
import org.jf.dexlib2.AccessFlags;
import org.jf.dexlib2.Format;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.ReferenceType;
import org.jf.dexlib2.analysis.AnalysisException;
import org.jf.dexlib2.analysis.AnalyzedInstruction;
import org.jf.dexlib2.analysis.MethodAnalyzer;
import org.jf.dexlib2.iface.*;
import org.jf.dexlib2.iface.debug.DebugItem;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.OffsetInstruction;
import org.jf.dexlib2.iface.instruction.ReferenceInstruction;
import org.jf.dexlib2.iface.reference.MethodReference;
import org.jf.dexlib2.util.InstructionOffsetMap;
import org.jf.dexlib2.util.ReferenceUtil;
import org.jf.dexlib2.util.SyntheticAccessorResolver;
import org.jf.dexlib2.util.TypeUtils;
import org.jf.util.ExceptionWithContext;
import org.jf.util.IndentingWriter;
import org.jf.util.SparseIntArray;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.*;

public class MethodDefinition {
    @Nonnull public final ClassDefinition classDef;
    @Nonnull public final Method method;
    @Nonnull public final MethodImplementation methodImpl;
    @Nonnull public final ImmutableList<Instruction> instructions;
    @Nonnull public final ImmutableList<MethodParameter> methodParameters;
    public RegisterFormatter registerFormatter;

    @Nonnull private final LabelCache labelCache = new LabelCache();

    @Nonnull private final SparseIntArray packedSwitchMap;
    @Nonnull private final SparseIntArray sparseSwitchMap;
    @Nonnull private final InstructionOffsetMap instructionOffsetMap;

    public MethodDefinition(@Nonnull ClassDefinition classDef, @Nonnull Method method,
                            @Nonnull MethodImplementation methodImpl) {
        this.classDef = classDef;
        this.method = method;
        this.methodImpl = methodImpl;

        try {
            //TODO: what about try/catch blocks inside the dead code? those will need to be commented out too. ugh.

            instructions = ImmutableList.copyOf(methodImpl.getInstructions());
            methodParameters = ImmutableList.copyOf(method.getParameters());

            packedSwitchMap = new SparseIntArray(0);
            sparseSwitchMap = new SparseIntArray(0);
            instructionOffsetMap = new InstructionOffsetMap(instructions);

            for (int i=0; i<instructions.size(); i++) {
                Instruction instruction = instructions.get(i);

                Opcode opcode = instruction.getOpcode();
                if (opcode == Opcode.PACKED_SWITCH) {
                    int codeOffset = instructionOffsetMap.getInstructionCodeOffset(i);
                    int targetOffset = codeOffset + ((OffsetInstruction)instruction).getCodeOffset();
                    targetOffset = findSwitchPayload(targetOffset, Opcode.PACKED_SWITCH_PAYLOAD);
                    packedSwitchMap.append(targetOffset, codeOffset);
                } else if (opcode == Opcode.SPARSE_SWITCH) {
                    int codeOffset = instructionOffsetMap.getInstructionCodeOffset(i);
                    int targetOffset = codeOffset + ((OffsetInstruction)instruction).getCodeOffset();
                    targetOffset = findSwitchPayload(targetOffset, Opcode.SPARSE_SWITCH_PAYLOAD);
                    sparseSwitchMap.append(targetOffset, codeOffset);
                }
            }
        }catch (Exception ex) {
            String methodString;
            try {
                methodString = ReferenceUtil.getMethodDescriptor(method);
            } catch (Exception ex2) {
                throw ExceptionWithContext.withContext(ex, "Error while processing method");
            }
            throw ExceptionWithContext.withContext(ex, "Error while processing method %s", methodString);
        }
    }

    public static void writeEmptyMethodTo(IndentingWriter writer, Method method,
                                          baksmaliOptions options) throws IOException {
        writer.write(".method ");
        writeAccessFlags(writer, method.getAccessFlags());
        writer.write(method.getName());
        writer.write("(");
        ImmutableList<MethodParameter> methodParameters = ImmutableList.copyOf(method.getParameters());
        for (MethodParameter parameter: methodParameters) {
            writer.write(parameter.getType());
        }
        writer.write(")");
        writer.write(method.getReturnType());
        writer.write('\n');

        writer.indent(4);
        writeParameters(writer, method, methodParameters, options);
        AnnotationFormatter.writeTo(writer, method.getAnnotations());
        writer.deindent(4);
        writer.write(".end method\n");
    }

    public void writeTo(IndentingWriter writer) throws IOException {
        int parameterRegisterCount = 0;
        if (!AccessFlags.STATIC.isSet(method.getAccessFlags())) {
            parameterRegisterCount++;
        }

        writer.write(".method ");
        writeAccessFlags(writer, method.getAccessFlags());
        writer.write(method.getName());
        writer.write("(");
        for (MethodParameter parameter: methodParameters) {
            String type = parameter.getType();
            writer.write(type);
            parameterRegisterCount++;
            if (TypeUtils.isWideType(type)) {
                parameterRegisterCount++;
            }
        }
        writer.write(")");
        writer.write(method.getReturnType());
        writer.write('\n');

        writer.indent(4);
        if (classDef.options.useLocalsDirective) {
            writer.write(".locals ");
            writer.printSignedIntAsDec(methodImpl.getRegisterCount() - parameterRegisterCount);
        } else {
            writer.write(".registers ");
            writer.printSignedIntAsDec(methodImpl.getRegisterCount());
        }
        writer.write('\n');
        writeParameters(writer, method, methodParameters, classDef.options);

        if (registerFormatter == null) {
            registerFormatter = new RegisterFormatter(classDef.options, methodImpl.getRegisterCount(),
                    parameterRegisterCount);
        }

        AnnotationFormatter.writeTo(writer, method.getAnnotations());

        writer.write('\n');

        List<MethodItem> methodItems = getMethodItems();
        for (MethodItem methodItem: methodItems) {
            if (methodItem.writeTo(writer)) {
                writer.write('\n');
            }
        }
        writer.deindent(4);
        writer.write(".end method\n");
    }

    private int findSwitchPayload(int targetOffset, Opcode type) {
        int targetIndex = instructionOffsetMap.getInstructionIndexAtCodeOffset(targetOffset);

        //TODO: does dalvik let you pad with multiple nops?
        //TODO: does dalvik let a switch instruction point to a non-payload instruction?

        Instruction instruction = instructions.get(targetIndex);
        if (instruction.getOpcode() != type) {
            // maybe it's pointing to a NOP padding instruction. Look at the next instruction
            if (instruction.getOpcode() == Opcode.NOP) {
                targetIndex += 1;
                if (targetIndex < instructions.size()) {
                    instruction = instructions.get(targetIndex);
                    if (instruction.getOpcode() == type) {
                        return instructionOffsetMap.getInstructionCodeOffset(targetIndex);
                    }
                }
            }
            throw new ExceptionWithContext("No switch payload at offset 0x%x", targetOffset);
        } else {
            return targetOffset;
        }
    }

    private static void writeAccessFlags(IndentingWriter writer, int accessFlags)
            throws IOException {
        for (AccessFlags accessFlag: AccessFlags.getAccessFlagsForMethod(accessFlags)) {
            writer.write(accessFlag.toString());
            writer.write(' ');
        }
    }

    private static void writeParameters(IndentingWriter writer, Method method,
                                        List<? extends MethodParameter> parameters,
                                        baksmaliOptions options) throws IOException {
        boolean isStatic = AccessFlags.STATIC.isSet(method.getAccessFlags());
        int registerNumber = isStatic?0:1;
        for (MethodParameter parameter: parameters) {
            String parameterType = parameter.getType();
            String parameterName = parameter.getName();
            Collection<? extends Annotation> annotations = parameter.getAnnotations();
            if (parameterName != null || annotations.size() != 0) {
                writer.write(".param p");
                writer.printSignedIntAsDec(registerNumber);

                if (parameterName != null && options.outputDebugInfo) {
                    writer.write(", ");
                    ReferenceFormatter.writeStringReference(writer, parameterName);
                }
                writer.write("    # ");
                writer.write(parameterType);
                writer.write("\n");
                if (annotations.size() > 0) {
                    writer.indent(4);
                    AnnotationFormatter.writeTo(writer, annotations);
                    writer.deindent(4);
                    writer.write(".end param\n");
                }
            }

            registerNumber++;
            if (TypeUtils.isWideType(parameterType)) {
                registerNumber++;
            }
        }
    }

    @Nonnull public LabelCache getLabelCache() {
        return labelCache;
    }

    public int getPackedSwitchBaseAddress(int packedSwitchPayloadCodeOffset) {
        return packedSwitchMap.get(packedSwitchPayloadCodeOffset, -1);
    }

    public int getSparseSwitchBaseAddress(int sparseSwitchPayloadCodeOffset) {
        return sparseSwitchMap.get(sparseSwitchPayloadCodeOffset, -1);
    }

    private List<MethodItem> getMethodItems() {
        ArrayList<MethodItem> methodItems = new ArrayList<MethodItem>();

        if ((classDef.options.registerInfo != 0) || (classDef.options.deodex && needsAnalyzed())) {
            addAnalyzedInstructionMethodItems(methodItems);
        } else {
            addInstructionMethodItems(methodItems);
        }

        addTries(methodItems);
        if (classDef.options.outputDebugInfo) {
            addDebugInfo(methodItems);
        }

        if (classDef.options.useSequentialLabels) {
            setLabelSequentialNumbers();
        }

        for (LabelMethodItem labelMethodItem: labelCache.getLabels()) {
            methodItems.add(labelMethodItem);
        }

        Collections.sort(methodItems);

        return methodItems;
    }

    private boolean needsAnalyzed() {
        for (Instruction instruction: methodImpl.getInstructions()) {
            if (instruction.getOpcode().odexOnly()) {
                return true;
            }
        }
        return false;
    }

    private void addInstructionMethodItems(List<MethodItem> methodItems) {
        int currentCodeAddress = 0;
        for (int i=0; i<instructions.size(); i++) {
            Instruction instruction = instructions.get(i);

            MethodItem methodItem = InstructionMethodItemFactory.makeInstructionFormatMethodItem(this,
                    currentCodeAddress, instruction);

            methodItems.add(methodItem);

            if (i != instructions.size() - 1) {
                methodItems.add(new BlankMethodItem(currentCodeAddress));
            }

            if (classDef.options.addCodeOffsets) {
                methodItems.add(new MethodItem(currentCodeAddress) {

                    @Override
                    public double getSortOrder() {
                        return -1000;
                    }

                    @Override
                    public boolean writeTo(IndentingWriter writer) throws IOException {
                        writer.write("#@");
                        writer.printUnsignedLongAsHex(codeAddress & 0xFFFFFFFFL);
                        return true;
                    }
                });
            }

            if (!classDef.options.noAccessorComments && (instruction instanceof ReferenceInstruction)) {
                Opcode opcode = instruction.getOpcode();

                if (opcode.referenceType == ReferenceType.METHOD) {
                    MethodReference methodReference =
                            (MethodReference)((ReferenceInstruction)instruction).getReference();

                    if (SyntheticAccessorResolver.looksLikeSyntheticAccessor(methodReference.getName())) {
                        SyntheticAccessorResolver.AccessedMember accessedMember =
                                classDef.options.syntheticAccessorResolver.getAccessedMember(methodReference);
                        if (accessedMember != null) {
                            methodItems.add(new SyntheticAccessCommentMethodItem(accessedMember, currentCodeAddress));
                        }
                    }
                }
            }

            currentCodeAddress += instruction.getCodeUnits();
        }
    }

    private void addAnalyzedInstructionMethodItems(List<MethodItem> methodItems) {
        MethodAnalyzer methodAnalyzer = new MethodAnalyzer(classDef.options.classPath, method,
                classDef.options.inlineResolver);

        AnalysisException analysisException = methodAnalyzer.getAnalysisException();
        if (analysisException != null) {
            // TODO: need to keep track of whether any errors occurred, so we can exit with a non-zero result
            methodItems.add(new CommentMethodItem(
                    String.format("AnalysisException: %s", analysisException.getMessage()),
                    analysisException.codeAddress, Integer.MIN_VALUE));
            analysisException.printStackTrace(System.err);
        }

        List<AnalyzedInstruction> instructions = methodAnalyzer.getAnalyzedInstructions();

        int currentCodeAddress = 0;
        for (int i=0; i<instructions.size(); i++) {
            AnalyzedInstruction instruction = instructions.get(i);

            MethodItem methodItem = InstructionMethodItemFactory.makeInstructionFormatMethodItem(
                    this, currentCodeAddress, instruction.getInstruction());

            methodItems.add(methodItem);

            if (instruction.getInstruction().getOpcode().format == Format.UnresolvedOdexInstruction) {
                methodItems.add(new CommentedOutMethodItem(
                        InstructionMethodItemFactory.makeInstructionFormatMethodItem(
                                this, currentCodeAddress, instruction.getOriginalInstruction())));
            }

            if (i != instructions.size() - 1) {
                methodItems.add(new BlankMethodItem(currentCodeAddress));
            }

            if (classDef.options.addCodeOffsets) {
                methodItems.add(new MethodItem(currentCodeAddress) {

                    @Override
                    public double getSortOrder() {
                        return -1000;
                    }

                    @Override
                    public boolean writeTo(IndentingWriter writer) throws IOException {
                        writer.write("#@");
                        writer.printUnsignedLongAsHex(codeAddress & 0xFFFFFFFFL);
                        return true;
                    }
                });
            }

            if (classDef.options.registerInfo != 0 &&
                    !instruction.getInstruction().getOpcode().format.isPayloadFormat) {
                methodItems.add(
                        new PreInstructionRegisterInfoMethodItem(classDef.options.registerInfo,
                                methodAnalyzer, registerFormatter, instruction, currentCodeAddress));

                methodItems.add(
                        new PostInstructionRegisterInfoMethodItem(registerFormatter, instruction, currentCodeAddress));
            }

            currentCodeAddress += instruction.getInstruction().getCodeUnits();
        }
    }

    private void addTries(List<MethodItem> methodItems) {
        List<? extends TryBlock<? extends ExceptionHandler>> tryBlocks = methodImpl.getTryBlocks();
        if (tryBlocks.size() == 0) {
            return;
        }

        int lastInstructionAddress = instructionOffsetMap.getInstructionCodeOffset(instructions.size() - 1);
        int codeSize = lastInstructionAddress + instructions.get(instructions.size() - 1).getCodeUnits();

        for (TryBlock<? extends ExceptionHandler> tryBlock: tryBlocks) {
            int startAddress = tryBlock.getStartCodeAddress();
            int endAddress = startAddress + tryBlock.getCodeUnitCount();

            if (startAddress >= codeSize) {
                throw new RuntimeException(String.format("Try start offset %d is past the end of the code block.",
                        startAddress));
            }
            // Note: not >=. endAddress == codeSize is valid, when the try covers the last instruction
            if (endAddress > codeSize) {
                throw new RuntimeException(String.format("Try end offset %d is past the end of the code block.",
                        endAddress));
            }

            /**
             * The end address points to the address immediately after the end of the last
             * instruction that the try block covers. We want the .catch directive and end_try
             * label to be associated with the last covered instruction, so we need to get
             * the address for that instruction
             */

            int lastCoveredIndex = instructionOffsetMap.getInstructionIndexAtCodeOffset(endAddress - 1, false);
            int lastCoveredAddress = instructionOffsetMap.getInstructionCodeOffset(lastCoveredIndex);

            for (ExceptionHandler handler: tryBlock.getExceptionHandlers()) {
                int handlerAddress = handler.getHandlerCodeAddress();
                if (handlerAddress >= codeSize) {
                    throw new ExceptionWithContext(
                            "Exception handler offset %d is past the end of the code block.", handlerAddress);
                }

                //use the address from the last covered instruction
                CatchMethodItem catchMethodItem = new CatchMethodItem(classDef.options, labelCache, lastCoveredAddress,
                        handler.getExceptionType(), startAddress, endAddress, handlerAddress);
                methodItems.add(catchMethodItem);
            }
        }
    }

    private void addDebugInfo(final List<MethodItem> methodItems) {
        for (DebugItem debugItem: methodImpl.getDebugItems()) {
            methodItems.add(DebugMethodItem.build(registerFormatter, debugItem));
        }
    }

    private void setLabelSequentialNumbers() {
        HashMap<String, Integer> nextLabelSequenceByType = new HashMap<String, Integer>();
        ArrayList<LabelMethodItem> sortedLabels = new ArrayList<LabelMethodItem>(labelCache.getLabels());

        //sort the labels by their location in the method
        Collections.sort(sortedLabels);

        for (LabelMethodItem labelMethodItem: sortedLabels) {
            Integer labelSequence = nextLabelSequenceByType.get(labelMethodItem.getLabelPrefix());
            if (labelSequence == null) {
                labelSequence = 0;
            }
            labelMethodItem.setLabelSequence(labelSequence);
            nextLabelSequenceByType.put(labelMethodItem.getLabelPrefix(), labelSequence + 1);
        }
    }

    public static class LabelCache {
        protected HashMap<LabelMethodItem, LabelMethodItem> labels = new HashMap<LabelMethodItem, LabelMethodItem>();

        public LabelCache() {
        }

        public LabelMethodItem internLabel(LabelMethodItem labelMethodItem) {
            LabelMethodItem internedLabelMethodItem = labels.get(labelMethodItem);
            if (internedLabelMethodItem != null) {
                return internedLabelMethodItem;
            }
            labels.put(labelMethodItem, labelMethodItem);
            return labelMethodItem;
        }


        public Collection<LabelMethodItem> getLabels() {
            return labels.values();
        }
    }
}
