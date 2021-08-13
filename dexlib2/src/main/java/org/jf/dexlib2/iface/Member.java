/*
 * Copyright 2015, Google Inc.
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

import org.jf.dexlib2.HiddenApiRestriction;

import javax.annotation.Nonnull;
import java.util.Set;

/**
 * This class represents a generic class member
 */
public interface Member extends Annotatable {
    /**
     * Gets the type of the class that defines this member.
     *
     * @return The type of the class that defines this member
     */
    @Nonnull String getDefiningClass();

    /**
     * Gets the name of this member.
     *
     * @return The name of this field
     */
    @Nonnull String getName();

    /**
     * Gets the access flags for this member.
     *
     * This will be a combination of the AccessFlags.* flags that are marked as compatible for use with this type
     * of member.
     *
     * @return The access flags for this member
     */
    int getAccessFlags();

    /**
     * Gets the hidden api restrictions for this member.
     *
     * This will contain at most 1 normal flag (with isDomainSpecificApiFlag() = false), and 1
     * domain-specific api flag (with isDomainSpecificApiFlag() = true)
     *
     * @return A set of the hidden api restrictions for this member.
     */
    @Nonnull Set<HiddenApiRestriction> getHiddenApiRestrictions();
}
