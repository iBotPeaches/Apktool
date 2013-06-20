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

package org.jf.dexlib2.writer.builder;

import org.jf.dexlib2.DebugItemType;
import org.jf.dexlib2.iface.debug.*;
import org.jf.dexlib2.immutable.debug.ImmutableEpilogueBegin;
import org.jf.dexlib2.immutable.debug.ImmutableLineNumber;
import org.jf.dexlib2.immutable.debug.ImmutablePrologueEnd;

import javax.annotation.Nullable;

public abstract interface BuilderDebugItem extends DebugItem {
    abstract static class BaseBuilderDebugItem implements BuilderDebugItem {
        final int codeAddress;

        public BaseBuilderDebugItem(int codeAddress) {
            this.codeAddress = codeAddress;
        }

        @Override public int getCodeAddress() { return codeAddress; }
    }

    public static class BuilderStartLocal extends BaseBuilderDebugItem implements StartLocal {
        final int register;
        @Nullable final BuilderStringReference name;
        @Nullable final BuilderTypeReference type;
        @Nullable final BuilderStringReference signature;

        BuilderStartLocal(int codeAddress,
                                 int register,
                                 @Nullable BuilderStringReference name,
                                 @Nullable BuilderTypeReference type,
                                 @Nullable BuilderStringReference signature) {
            super(codeAddress);
            this.register = register;
            this.name = name;
            this.type = type;
            this.signature = signature;
        }

        @Override public int getRegister() { return register; }
        @Nullable @Override public String getName() { return name==null?null:name.getString(); }
        @Nullable @Override public String getType() { return type==null?null:type.getType(); }
        @Nullable @Override public String getSignature() { return signature==null?null:signature.getString(); }

        @Override public int getDebugItemType() { return DebugItemType.START_LOCAL; }
    }

    public static class BuilderEndLocal extends BaseBuilderDebugItem implements EndLocal {
        private final int register;

        BuilderEndLocal(int codeAddress, int register) {
            super(codeAddress);
            this.register = register;
        }

        @Override public int getRegister() {
            return register;
        }

        @Override public int getDebugItemType() {
            return DebugItemType.END_LOCAL;
        }

        @Nullable @Override public String getName() {
            return null;
        }

        @Nullable @Override public String getType() {
            return null;
        }

        @Nullable @Override public String getSignature() {
            return null;
        }
    }

    public static class BuilderRestartLocal extends BaseBuilderDebugItem implements RestartLocal {
        private final int register;

        BuilderRestartLocal(int codeAddress, int register) {
            super(codeAddress);
            this.register = register;
        }

        @Override public int getRegister() {
            return register;
        }

        @Override public int getDebugItemType() {
            return DebugItemType.RESTART_LOCAL;
        }

        @Nullable @Override public String getName() {
            return null;
        }

        @Nullable @Override public String getType() {
            return null;
        }

        @Nullable @Override public String getSignature() {
            return null;
        }
    }
    
    public static class BuilderPrologueEnd extends ImmutablePrologueEnd implements BuilderDebugItem {
        BuilderPrologueEnd(int codeAddress) {
            super(codeAddress);
        }
    }

    public static class BuilderEpilogueBegin extends ImmutableEpilogueBegin implements BuilderDebugItem {
        BuilderEpilogueBegin(int codeAddress) {
            super(codeAddress);
        }
    }

    public static class BuilderLineNumber extends ImmutableLineNumber implements BuilderDebugItem {
        BuilderLineNumber(int codeAddress, int lineNumber) {
            super(codeAddress, lineNumber);
        }
    }

    public static class BuilderSetSourceFile extends BaseBuilderDebugItem implements SetSourceFile {
        @Nullable final BuilderStringReference sourceFile;

        BuilderSetSourceFile(int codeAddress,
                                    @Nullable BuilderStringReference sourceFile) {
            super(codeAddress);
            this.sourceFile = sourceFile;
        }

        @Nullable @Override public String getSourceFile() { return sourceFile==null?null:sourceFile.getString(); }

        @Override public int getDebugItemType() { return DebugItemType.SET_SOURCE_FILE; }
    }
}
