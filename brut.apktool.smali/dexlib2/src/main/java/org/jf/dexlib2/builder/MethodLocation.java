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

import com.google.common.collect.Lists;
import org.jf.dexlib2.builder.debug.*;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.reference.StringReference;
import org.jf.dexlib2.iface.reference.TypeReference;
import org.jf.dexlib2.writer.builder.BuilderStringReference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class MethodLocation {
    @Nullable BuilderInstruction instruction;
    int codeAddress;
    int index;

    private List<Label> labels = Lists.newArrayList();
    List<BuilderDebugItem> debugItems = Lists.newArrayList();

    MethodLocation(@Nullable BuilderInstruction instruction, int codeAddress, int index) {
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

    void mergeInto(@Nonnull MethodLocation other) {
        for (Label label: labels) {
            label.location = other;
            other.labels.add(label);
        }

        // We need to keep the debug items in the same order. We add the other debug items to this list, then reassign
        // the list.
        for (BuilderDebugItem debugItem: debugItems) {
            debugItem.location = other;
        }
        debugItems.addAll(other.debugItems);
        other.debugItems = debugItems;
    }

    @Nonnull
    public Set<Label> getLabels() {
        return new AbstractSet<Label>() {
            @Nonnull
            @Override public Iterator<Label> iterator() {
                final Iterator<Label> it = labels.iterator();

                return new Iterator<Label>() {
                    private @Nullable Label currentLabel = null;

                    @Override public boolean hasNext() {
                        return it.hasNext();
                    }

                    @Override public Label next() {
                        currentLabel = it.next();
                        return currentLabel;
                    }

                    @Override public void remove() {
                        if (currentLabel != null) {
                            currentLabel.location = null;
                        }
                        it.remove();
                    }
                };
            }

            @Override public int size() {
                return labels.size();
            }

            @Override public boolean add(@Nonnull Label label) {
                if (label.isPlaced()) {
                    throw new IllegalArgumentException("Cannot add a label that is already placed. You must remove " +
                            "it from its current location first.");
                }
                label.location = MethodLocation.this;
                labels.add(label);
                return true;
            }
        };
    }

    @Nonnull
    public Label addNewLabel() {
        Label label = new Label(this);
        labels.add(label);
        return label;
    }

    @Nonnull
    public Set<BuilderDebugItem> getDebugItems() {
        return new AbstractSet<BuilderDebugItem>() {
            @Nonnull
            @Override public Iterator<BuilderDebugItem> iterator() {
                final Iterator<BuilderDebugItem> it = debugItems.iterator();

                return new Iterator<BuilderDebugItem>() {
                    private @Nullable BuilderDebugItem currentDebugItem = null;

                    @Override public boolean hasNext() {
                        return it.hasNext();
                    }

                    @Override public BuilderDebugItem next() {
                        currentDebugItem = it.next();
                        return currentDebugItem;
                    }

                    @Override public void remove() {
                        if (currentDebugItem != null) {
                            currentDebugItem.location = null;
                        }
                        it.remove();
                    }
                };
            }

            @Override public int size() {
                return labels.size();
            }

            @Override public boolean add(@Nonnull BuilderDebugItem debugItem) {
                if (debugItem.location != null) {
                    throw new IllegalArgumentException("Cannot add a debug item that has already been added to a " +
                            "method. You must remove it from its current location first.");
                }
                debugItem.location = MethodLocation.this;
                debugItems.add(debugItem);
                return true;
            }
        };
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

    public void addSetSourceFile(@Nullable BuilderStringReference sourceFile) {
        getDebugItems().add(new BuilderSetSourceFile(sourceFile));
    }
}
