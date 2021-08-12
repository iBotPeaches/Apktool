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

package org.jf.dexlib2.builder;

import org.jf.dexlib2.builder.debug.*;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.reference.StringReference;
import org.jf.dexlib2.iface.reference.TypeReference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class MethodLocation {
    @Nullable BuilderInstruction instruction;
    int codeAddress;
    int index;

    private final LocatedItems<Label> labels;
    private final LocatedItems<BuilderDebugItem> debugItems;

    MethodLocation(@Nullable BuilderInstruction instruction, int codeAddress, int index) {
        this.debugItems = new LocatedDebugItems();
        this.labels = new LocatedLabels();
        this.instruction = instruction;
        this.codeAddress = codeAddress;
        this.index = index;
    }

    @Nullable
    public Instruction getInstruction() {
        return instruction;
    }

    public int getCodeAddress() {
        return codeAddress;
    }

    public int getIndex() {
        return index;
    }

    void mergeInto(@Nonnull MethodLocation nextLocation) {
        labels.mergeItemsIntoNext(nextLocation, nextLocation.labels);
        debugItems.mergeItemsIntoNext(nextLocation, nextLocation.debugItems);
    }

    @Nonnull
    public Set<Label> getLabels() {
        return labels.getModifiableItems(MethodLocation.this);
    }

    @Nonnull
    public Label addNewLabel() {
        Label newLabel = new Label();
        getLabels().add(newLabel);
        return newLabel;
    }

    @Nonnull
    public Set<BuilderDebugItem> getDebugItems() {
        return debugItems.getModifiableItems(MethodLocation.this);
    }

    public void addLineNumber(int lineNumber) {
        getDebugItems().add(new BuilderLineNumber(lineNumber));
    }

    public void addStartLocal(int registerNumber, @Nullable StringReference name, @Nullable TypeReference type,
                              @Nullable StringReference signature) {
        getDebugItems().add(new BuilderStartLocal(registerNumber, name, type, signature));
    }

    public void addEndLocal(int registerNumber) {
        getDebugItems().add(new BuilderEndLocal(registerNumber));
    }

    public void addRestartLocal(int registerNumber) {
        getDebugItems().add(new BuilderRestartLocal(registerNumber));
    }

    public void addPrologue() {
        getDebugItems().add(new BuilderPrologueEnd());
    }

    public void addEpilogue() {
        getDebugItems().add(new BuilderEpilogueBegin());
    }

    public void addSetSourceFile(@Nullable StringReference sourceFile) {
        getDebugItems().add(new BuilderSetSourceFile(sourceFile));
    }
}
