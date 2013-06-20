/*
 * Copyright 2012, Google Inc.
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

package org.jf.dexlib2.immutable.debug;

import com.google.common.collect.ImmutableList;
import org.jf.dexlib2.DebugItemType;
import org.jf.dexlib2.iface.debug.*;
import org.jf.util.ExceptionWithContext;
import org.jf.util.ImmutableConverter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class ImmutableDebugItem implements DebugItem {
    protected final int codeAddress;

    public ImmutableDebugItem(int codeAddress) {
        this.codeAddress = codeAddress;
    }

    @Nonnull
    public static ImmutableDebugItem of(DebugItem debugItem) {
        if (debugItem instanceof ImmutableDebugItem) {
            return (ImmutableDebugItem)debugItem;
        }
        switch (debugItem.getDebugItemType()) {
            case DebugItemType.START_LOCAL:
                return ImmutableStartLocal.of((StartLocal)debugItem);
            case DebugItemType.END_LOCAL:
                return ImmutableEndLocal.of((EndLocal)debugItem);
            case DebugItemType.RESTART_LOCAL:
                return ImmutableRestartLocal.of((RestartLocal)debugItem);
            case DebugItemType.PROLOGUE_END:
                return ImmutablePrologueEnd.of((PrologueEnd)debugItem);
            case DebugItemType.EPILOGUE_BEGIN:
                return ImmutableEpilogueBegin.of((EpilogueBegin)debugItem);
            case DebugItemType.SET_SOURCE_FILE:
                return ImmutableSetSourceFile.of((SetSourceFile)debugItem);
            case DebugItemType.LINE_NUMBER:
                return ImmutableLineNumber.of((LineNumber)debugItem);
            default:
                throw new ExceptionWithContext("Invalid debug item type: %d", debugItem.getDebugItemType());
        }
    }

    @Override public int getCodeAddress() { return codeAddress; }

    @Nonnull
    public static ImmutableList<ImmutableDebugItem> immutableListOf(@Nullable Iterable<? extends DebugItem> list) {
        return CONVERTER.toList(list);
    }

    private static final ImmutableConverter<ImmutableDebugItem, DebugItem> CONVERTER =
            new ImmutableConverter<ImmutableDebugItem, DebugItem>() {
                @Override
                protected boolean isImmutable(@Nonnull DebugItem item) {
                    return item instanceof ImmutableDebugItem;
                }

                @Nonnull
                @Override
                protected ImmutableDebugItem makeImmutable(@Nonnull DebugItem item) {
                    return ImmutableDebugItem.of(item);
                }
            };
}
