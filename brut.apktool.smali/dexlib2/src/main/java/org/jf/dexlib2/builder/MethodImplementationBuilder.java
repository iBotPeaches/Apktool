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

import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.reference.StringReference;
import org.jf.dexlib2.iface.reference.TypeReference;
import org.jf.dexlib2.writer.builder.BuilderStringReference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;

public class MethodImplementationBuilder {
    // Contains all named labels - both placed and unplaced
    private final HashMap<String, Label> labels = new HashMap<String, Label>();

    @Nonnull
    private final MutableMethodImplementation impl;

    private MethodLocation currentLocation;

    public MethodImplementationBuilder(int registerCount) {
        this.impl = new MutableMethodImplementation(registerCount);
        this.currentLocation = impl.instructionList.get(0);
    }

    public MethodImplementation getMethodImplementation() {
        return impl;
    }

    /**
     * Adds a new named label at the current location.
     *
     * Any previous unplaced references to a label of this name will now refer to this label/location
     *
     * @param name The name of the label to add
     * @return A LabelRef representing the label
     */
    @Nonnull
    public Label addLabel(@Nonnull String name) {
        Label label = labels.get(name);

        if (label != null) {
            if (label.isPlaced()) {
                throw new IllegalArgumentException("There is already a label with that name.");
            } else {
                currentLocation.getLabels().add(label);
            }
        } else {
            label = currentLocation.addNewLabel();
            labels.put(name, label);
        }

        return label;
    }

    /**
     * Get a reference to a label with the given name.
     *
     * If a label with that name has not been added yet, a new one is created, but is left
     * in an unplaced state. It is assumed that addLabel(name) will be called at a later
     * point to define the location of the label.
     *
     * @param name The name of the label to get
     * @return A LabelRef representing the label
     */
    @Nonnull
    public Label getLabel(@Nonnull String name) {
        Label label = labels.get(name);
        if (label == null) {
            label = new Label();
            labels.put(name, label);
        }
        return label;
    }

    public void addCatch(@Nullable TypeReference type, @Nonnull Label from,
                         @Nonnull Label to, @Nonnull Label handler) {
        impl.addCatch(type, from, to, handler);
    }

    public void addCatch(@Nullable String type, @Nonnull Label from, @Nonnull Label to,
                         @Nonnull Label handler) {
        impl.addCatch(type, from, to, handler);
    }

    public void addCatch(@Nonnull Label from, @Nonnull Label to, @Nonnull Label handler) {
        impl.addCatch(from, to, handler);
    }

    public void addLineNumber(int lineNumber) {
        currentLocation.addLineNumber(lineNumber);
    }

    public void addStartLocal(int registerNumber, @Nullable StringReference name, @Nullable TypeReference type,
                              @Nullable StringReference signature) {
        currentLocation.addStartLocal(registerNumber, name, type, signature);
    }

    public void addEndLocal(int registerNumber) {
        currentLocation.addEndLocal(registerNumber);
    }

    public void addRestartLocal(int registerNumber) {
        currentLocation.addRestartLocal(registerNumber);
    }

    public void addPrologue() {
        currentLocation.addPrologue();
    }

    public void addEpilogue() {
        currentLocation.addEpilogue();
    }

    public void addSetSourceFile(@Nullable BuilderStringReference sourceFile) {
        currentLocation.addSetSourceFile(sourceFile);
    }

    public void addInstruction(@Nullable BuilderInstruction instruction) {
        impl.addInstruction(instruction);
        currentLocation = impl.instructionList.get(impl.instructionList.size()-1);
    }
}
