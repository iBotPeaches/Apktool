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

package org.jf.dexlib2.writer;

import org.jf.dexlib2.builder.MutableMethodImplementation;
import org.jf.dexlib2.iface.ExceptionHandler;
import org.jf.dexlib2.iface.TryBlock;
import org.jf.dexlib2.iface.debug.DebugItem;
import org.jf.dexlib2.iface.instruction.Instruction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface ClassSection<StringKey extends CharSequence, TypeKey extends CharSequence, TypeListKey, ClassKey,
        FieldKey, MethodKey, AnnotationSetKey, EncodedValue> extends IndexSection<ClassKey> {
    @Nonnull Collection<? extends ClassKey> getSortedClasses();

    @Nullable Map.Entry<? extends ClassKey, Integer> getClassEntryByType(@Nullable TypeKey key);

    @Nonnull TypeKey getType(@Nonnull ClassKey key);
    int getAccessFlags(@Nonnull ClassKey key);
    @Nullable TypeKey getSuperclass(@Nonnull ClassKey key);
    @Nullable TypeListKey getSortedInterfaces(@Nonnull ClassKey key);
    @Nullable StringKey getSourceFile(@Nonnull ClassKey key);
    @Nullable Collection<? extends EncodedValue> getStaticInitializers(@Nonnull ClassKey key);

    @Nonnull Collection<? extends FieldKey> getSortedStaticFields(@Nonnull ClassKey key);
    @Nonnull Collection<? extends FieldKey> getSortedInstanceFields(@Nonnull ClassKey key);
    @Nonnull Collection<? extends FieldKey> getSortedFields(@Nonnull ClassKey key);
    @Nonnull Collection<? extends MethodKey> getSortedDirectMethods(@Nonnull ClassKey key);
    @Nonnull Collection<? extends MethodKey> getSortedVirtualMethods(@Nonnull ClassKey key);
    @Nonnull Collection<? extends MethodKey> getSortedMethods(@Nonnull ClassKey key);

    int getFieldAccessFlags(@Nonnull FieldKey key);
    int getMethodAccessFlags(@Nonnull MethodKey key);

    @Nullable AnnotationSetKey getClassAnnotations(@Nonnull ClassKey key);
    @Nullable AnnotationSetKey getFieldAnnotations(@Nonnull FieldKey key);
    @Nullable AnnotationSetKey getMethodAnnotations(@Nonnull MethodKey key);
    @Nullable List<? extends AnnotationSetKey> getParameterAnnotations(@Nonnull MethodKey key);

    @Nullable Iterable<? extends DebugItem> getDebugItems(@Nonnull MethodKey key);
    @Nullable Iterable<? extends StringKey> getParameterNames(@Nonnull MethodKey key);

    int getRegisterCount(@Nonnull MethodKey key);
    @Nullable Iterable<? extends Instruction> getInstructions(@Nonnull MethodKey key);
    @Nonnull List<? extends TryBlock<? extends ExceptionHandler>> getTryBlocks(@Nonnull MethodKey key);
    @Nullable TypeKey getExceptionType(@Nonnull ExceptionHandler handler);
    @Nonnull MutableMethodImplementation makeMutableMethodImplementation(@Nonnull MethodKey key);

    void setEncodedArrayOffset(@Nonnull ClassKey key, int offset);
    int getEncodedArrayOffset(@Nonnull ClassKey key);

    void setAnnotationDirectoryOffset(@Nonnull ClassKey key, int offset);
    int getAnnotationDirectoryOffset(@Nonnull ClassKey key);

    void setAnnotationSetRefListOffset(@Nonnull MethodKey key, int offset);
    int getAnnotationSetRefListOffset(@Nonnull MethodKey key);

    void setCodeItemOffset(@Nonnull MethodKey key, int offset);
    int getCodeItemOffset(@Nonnull MethodKey key);

    void writeDebugItem(@Nonnull DebugWriter<StringKey, TypeKey> writer, DebugItem debugItem) throws IOException;
}
