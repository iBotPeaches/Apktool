/*
 * Copyright 2018, Google Inc.
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

package org.jf.dexlib2.dexbacked.reference;

import com.google.common.collect.Lists;
import org.jf.dexlib2.ValueType;
import org.jf.dexlib2.base.reference.BaseCallSiteReference;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.dexbacked.util.EncodedArrayItemIterator;
import org.jf.dexlib2.iface.reference.MethodHandleReference;
import org.jf.dexlib2.iface.reference.MethodProtoReference;
import org.jf.dexlib2.iface.value.EncodedValue;
import org.jf.dexlib2.iface.value.MethodHandleEncodedValue;
import org.jf.dexlib2.iface.value.MethodTypeEncodedValue;
import org.jf.dexlib2.iface.value.StringEncodedValue;
import org.jf.util.ExceptionWithContext;

import javax.annotation.Nonnull;
import java.util.List;

public class DexBackedCallSiteReference extends BaseCallSiteReference {
    @Nonnull public final DexBackedDexFile dexFile;
    public final int callSiteIndex;
    public final int callSiteIdOffset;
    private int callSiteOffset = -1;

    public DexBackedCallSiteReference(DexBackedDexFile dexFile, int callSiteIndex) {
        this.dexFile = dexFile;
        this.callSiteIndex = callSiteIndex;
        this.callSiteIdOffset = dexFile.getCallSiteSection().getOffset(callSiteIndex);
    }

    @Nonnull
    @Override
    public String getName() {
        return String.format("call_site_%d", callSiteIndex);
    }

    @Nonnull
    @Override
    public MethodHandleReference getMethodHandle() {
        EncodedArrayItemIterator iter = getCallSiteIterator();
        if (iter.getItemCount() < 3) {
            throw new ExceptionWithContext("Invalid call site item: must contain at least 3 entries.");
        }

        EncodedValue encodedValue = getCallSiteIterator().getNextOrNull();
        assert encodedValue != null;
        if (encodedValue.getValueType() != ValueType.METHOD_HANDLE) {
            throw new ExceptionWithContext(
                    "Invalid encoded value type (%d) for the first item in call site %d",
                    encodedValue.getValueType(), callSiteIndex);
        }
        return ((MethodHandleEncodedValue) encodedValue).getValue();
    }

    @Nonnull
    @Override
    public String getMethodName() {
        EncodedArrayItemIterator iter = getCallSiteIterator();
        if (iter.getItemCount() < 3) {
            throw new ExceptionWithContext("Invalid call site item: must contain at least 3 entries.");
        }

        iter.skipNext();
        EncodedValue encodedValue = iter.getNextOrNull();
        assert encodedValue != null;
        if (encodedValue.getValueType() != ValueType.STRING) {
            throw new ExceptionWithContext(
                    "Invalid encoded value type (%d) for the second item in call site %d",
                    encodedValue.getValueType(), callSiteIndex);
        }
        return ((StringEncodedValue) encodedValue).getValue();
    }

    @Nonnull
    @Override
    public MethodProtoReference getMethodProto() {
        EncodedArrayItemIterator iter = getCallSiteIterator();
        if (iter.getItemCount() < 3) {
            throw new ExceptionWithContext("Invalid call site item: must contain at least 3 entries.");
        }

        iter.skipNext();
        iter.skipNext();
        EncodedValue encodedValue = iter.getNextOrNull();
        assert encodedValue != null;
        if (encodedValue.getValueType() != ValueType.METHOD_TYPE) {
            throw new ExceptionWithContext(
                    "Invalid encoded value type (%d) for the second item in call site %d",
                    encodedValue.getValueType(), callSiteIndex);
        }
        return ((MethodTypeEncodedValue) encodedValue).getValue();
    }

    @Nonnull
    @Override
    public List<? extends EncodedValue> getExtraArguments() {
        List<EncodedValue> values = Lists.newArrayList();

        EncodedArrayItemIterator iter = getCallSiteIterator();
        if (iter.getItemCount() < 3) {
            throw new ExceptionWithContext("Invalid call site item: must contain at least 3 entries.");
        }
        if (iter.getItemCount() == 3) {
            return values;
        }

        iter.skipNext();
        iter.skipNext();
        iter.skipNext();

        EncodedValue item = iter.getNextOrNull();
        while (item != null) {
            values.add(item);
            item = iter.getNextOrNull();
        }
        return values;
    }

    private EncodedArrayItemIterator getCallSiteIterator() {
        return EncodedArrayItemIterator.newOrEmpty(dexFile, getCallSiteOffset());
    }

    private int getCallSiteOffset() {
        if (callSiteOffset < 0) {
            callSiteOffset = dexFile.getBuffer().readSmallUint(callSiteIdOffset);
        }
        return callSiteOffset;
    }

    @Override
    public void validateReference() throws InvalidReferenceException {
        if (callSiteIndex < 0 || callSiteIndex >= dexFile.getCallSiteSection().size()) {
            throw new InvalidReferenceException("callsite@" + callSiteIndex);
        }
    }
}
