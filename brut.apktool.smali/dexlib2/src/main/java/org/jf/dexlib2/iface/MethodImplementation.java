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

package org.jf.dexlib2.iface;

import org.jf.dexlib2.iface.debug.DebugItem;
import org.jf.dexlib2.iface.instruction.Instruction;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * This class represents the implementation details of a method.
 */
public interface MethodImplementation {
    /**
     * Gets the number of registers in this method.
     *
     * @return The number of register in this method.
     */
    int getRegisterCount();

    /**
     * Gets the instructions in this method.
     *
     * @return An Iterable of the instructions in this method
     */
    @Nonnull Iterable<? extends Instruction> getInstructions();

    /**
     * Gets a list of the try blocks defined for this method.
     *
     * Try blocks may overlap freely, and do not need to be strictly nested, as in java. This is a more relaxed
     * requirement than specified by the dex format, where try blocks may not overlap, and must be specified in
     * ascending order. When writing to a dex file, the try blocks will be massaged into the appropriate format.
     *
     * In any region where there are overlapping try blocks, set of exception handlers for the overlapping region will
     * consist of the union of all handlers in any try block that covers that region.
     *
     * If multiple overlapping try blocks define a handler for the same exception type, or define a catch-all
     * handler, then those duplicate handlers must use the same handler offset.
     *
     * @return A list of the TryBlock items
     */
    @Nonnull List<? extends TryBlock<? extends ExceptionHandler>> getTryBlocks();

    /**
     * Get a list of debug items for this method.
     *
     * This generally matches the semantics of the debug_info_item in the dex specification, although in an easier to
     * digest form.
     *
     * The addresses of the DebugItems in the returned list will be in non-descending order.
     *
     * @return A list of DebugInfo items
     */
    @Nonnull Iterable<? extends DebugItem> getDebugItems();
}
