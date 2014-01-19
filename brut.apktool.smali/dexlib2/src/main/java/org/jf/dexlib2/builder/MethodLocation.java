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

import com.google.common.collect.ImmutableList;
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

    // We end up creating and keeping around a *lot* of MethodLocation objects
    // when building a new dex file, so it's worth the trouble of lazily creating
    // the labels and debugItems lists only when they are needed

    @Nullable
    private List<Label> labels = null;
    @Nullable
    private List<BuilderDebugItem> debugItems = null;

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

    @Nonnull
    private List<Label> getLabels(boolean mutable) {
        if (labels == null) {
            if (mutable) {
                labels = new ArrayList<Label>(1);
                return labels;
            }
            return ImmutableList.of();
        }
        return labels;
    }

    @Nonnull
    private List<BuilderDebugItem> getDebugItems(boolean mutable) {
        if (debugItems == null) {
            if (mutable) {
                debugItems = new ArrayList<BuilderDebugItem>(1);
                return debugItems;
            }
            return ImmutableList.of();
        }
        return debugItems;
    }

    void mergeInto(@Nonnull MethodLocation other) {
        if (this.labels != null || other.labels != null) {
            List<Label> otherLabels = other.getLabels(true);
            for (Label label: this.getLabels(false)) {
                label.location = other;
                otherLabels.add(label);
            }
            this.labels = null;
        }

        if (this.debugItems != null || other.labels != null) {
            // We need to keep the debug items in the same order. We add the other debug items to this list, then reassign
            // the list.
            List<BuilderDebugItem> debugItems = getDebugItems(true);
            for (BuilderDebugItem debugItem: debugItems) {
                debugItem.location = other;
            }
            debugItems.addAll(other.getDebugItems(false));
            other.debugItems = debugItems;
            this.debugItems = null;
        }
    }

    @Nonnull
    public Set<Label> getLabels() {
        return new AbstractSet<Label>() {
            @Nonnull
            @Override public Iterator<Label> iterator() {
                final Iterator<Label> it = getLabels(false).iterator();

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
                return getLabels(false).size();
            }

            @Override public boolean add(@Nonnull Label label) {
                if (label.isPlaced()) {
                    throw new IllegalArgumentException("Cannot add a label that is already placed. You must remove " +
                            "it from its current location first.");
                }
                label.location = MethodLocation.this;
                getLabels(true).add(label);
                return true;
            }
        };
    }

    @Nonnull
    public Label addNewLabel() {
        Label label = new Label(this);
        getLabels(true).add(label);
        return label;
    }

    @Nonnull
    public Set<BuilderDebugItem> getDebugItems() {
        return new AbstractSet<BuilderDebugItem>() {
            @Nonnull
            @Override public Iterator<BuilderDebugItem> iterator() {
                final Iterator<BuilderDebugItem> it = getDebugItems(false).iterator();

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
                return getDebugItems(false).size();
            }

            @Override public boolean add(@Nonnull BuilderDebugItem debugItem) {
                if (debugItem.location != null) {
                    throw new IllegalArgumentException("Cannot add a debug item that has already been added to a " +
                            "method. You must remove it from its current location first.");
                }
                debugItem.location = MethodLocation.this;
                getDebugItems(true).add(debugItem);
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

    public void addSetSourceFile(@Nullable StringReference sourceFile) {
        getDebugItems().add(new BuilderSetSourceFile(sourceFile));
    }
}
