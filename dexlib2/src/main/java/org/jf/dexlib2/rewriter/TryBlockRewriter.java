/*
 * Copyright 2014, Google Inc.
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

package org.jf.dexlib2.rewriter;

import org.jf.dexlib2.base.BaseTryBlock;
import org.jf.dexlib2.iface.ExceptionHandler;
import org.jf.dexlib2.iface.TryBlock;

import javax.annotation.Nonnull;
import java.util.List;

public class TryBlockRewriter implements Rewriter<TryBlock<? extends ExceptionHandler>> {
    @Nonnull protected final Rewriters rewriters;

    public TryBlockRewriter(@Nonnull Rewriters rewriters) {
        this.rewriters = rewriters;
    }

    @Nonnull @Override public TryBlock<? extends ExceptionHandler> rewrite(
            @Nonnull TryBlock<? extends ExceptionHandler> tryBlock) {
        return new RewrittenTryBlock(tryBlock);
    }

    protected class RewrittenTryBlock extends BaseTryBlock<ExceptionHandler> {
        @Nonnull protected TryBlock<? extends ExceptionHandler> tryBlock;

        public RewrittenTryBlock(@Nonnull TryBlock<? extends ExceptionHandler> tryBlock) {
            this.tryBlock = tryBlock;
        }

        @Override public int getStartCodeAddress() {
            return tryBlock.getStartCodeAddress();
        }

        @Override public int getCodeUnitCount() {
            return tryBlock.getCodeUnitCount();
        }

        @Override @Nonnull public List<? extends ExceptionHandler> getExceptionHandlers() {
            return RewriterUtils.rewriteList(rewriters.getExceptionHandlerRewriter(), tryBlock.getExceptionHandlers());
        }
    }
}
