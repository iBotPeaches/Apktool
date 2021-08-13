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

import org.jf.dexlib2.base.BaseExceptionHandler;
import org.jf.dexlib2.iface.reference.TypeReference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class BuilderExceptionHandler extends BaseExceptionHandler {
    @Nonnull protected final Label handler;

    private BuilderExceptionHandler(@Nonnull Label handler) {
        this.handler = handler;
    }

    @Nonnull
    public Label getHandler() {
        return handler;
    }

    static BuilderExceptionHandler newExceptionHandler(@Nullable final TypeReference exceptionType,
                                                @Nonnull Label handler) {
        if (exceptionType == null) {
            return newExceptionHandler(handler);
        }
        return new BuilderExceptionHandler(handler) {
            @Nullable @Override public String getExceptionType() {
                return exceptionType.getType();
            }

            @Override public int getHandlerCodeAddress() {
                return handler.getCodeAddress();
            }

            @Nullable @Override public TypeReference getExceptionTypeReference() {
                return exceptionType;
            }
        };
    }

    static BuilderExceptionHandler newExceptionHandler(@Nonnull Label handler) {
        return new BuilderExceptionHandler(handler) {
            @Nullable @Override public String getExceptionType() {
                return null;
            }

            @Override public int getHandlerCodeAddress() {
                return handler.getCodeAddress();
            }
        };
    }

    static BuilderExceptionHandler newExceptionHandler(@Nullable final String exceptionType,
                                                @Nonnull Label handler) {
        if (exceptionType == null) {
            return newExceptionHandler(handler);
        }
        return new BuilderExceptionHandler(handler) {
            @Nullable @Override public String getExceptionType() {
                return exceptionType;
            }

            @Override public int getHandlerCodeAddress() {
                return handler.getCodeAddress();
            }
        };
    }
}
