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

package org.jf.dexlib2.immutable;

import com.google.common.collect.ImmutableList;
import org.jf.dexlib2.base.BaseTryBlock;
import org.jf.dexlib2.iface.ExceptionHandler;
import org.jf.dexlib2.iface.TryBlock;
import org.jf.util.ImmutableConverter;
import org.jf.util.ImmutableUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class ImmutableTryBlock extends BaseTryBlock<ImmutableExceptionHandler> {
    protected final int startCodeAddress;
    protected final int codeUnitCount;
    @Nonnull protected final ImmutableList<? extends ImmutableExceptionHandler> exceptionHandlers;

    public ImmutableTryBlock(int startCodeAddress,
                             int codeUnitCount,
                             @Nullable List<? extends ExceptionHandler> exceptionHandlers) {
        this.startCodeAddress = startCodeAddress;
        this.codeUnitCount = codeUnitCount;
        this.exceptionHandlers = ImmutableExceptionHandler.immutableListOf(exceptionHandlers);
    }

    public ImmutableTryBlock(int startCodeAddress,
                             int codeUnitCount,
                             @Nullable ImmutableList<? extends ImmutableExceptionHandler> exceptionHandlers) {
        this.startCodeAddress = startCodeAddress;
        this.codeUnitCount = codeUnitCount;
        this.exceptionHandlers = ImmutableUtils.nullToEmptyList(exceptionHandlers);
    }

    public static ImmutableTryBlock of(TryBlock<? extends ExceptionHandler> tryBlock) {
        if (tryBlock instanceof ImmutableTryBlock) {
            return (ImmutableTryBlock)tryBlock;
        }
        return new ImmutableTryBlock(
                tryBlock.getStartCodeAddress(),
                tryBlock.getCodeUnitCount(),
                tryBlock.getExceptionHandlers());
    }

    @Override public int getStartCodeAddress() { return startCodeAddress; }
    @Override public int getCodeUnitCount() { return codeUnitCount; }

    @Nonnull @Override public ImmutableList<? extends ImmutableExceptionHandler> getExceptionHandlers() {
        return exceptionHandlers;
    }

    @Nonnull
    public static ImmutableList<ImmutableTryBlock> immutableListOf(
            @Nullable List<? extends TryBlock<? extends ExceptionHandler>> list) {
        return CONVERTER.toList(list);
    }

    private static final ImmutableConverter<ImmutableTryBlock, TryBlock<? extends ExceptionHandler>> CONVERTER =
            new ImmutableConverter<ImmutableTryBlock, TryBlock<? extends ExceptionHandler>>() {
                @Override
                protected boolean isImmutable(@Nonnull TryBlock item) {
                    return item instanceof ImmutableTryBlock;
                }

                @Nonnull
                @Override
                protected ImmutableTryBlock makeImmutable(@Nonnull TryBlock<? extends ExceptionHandler> item) {
                    return ImmutableTryBlock.of(item);
                }
            };
}
