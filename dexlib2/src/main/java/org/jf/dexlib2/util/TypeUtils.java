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

package org.jf.dexlib2.util;

import org.jf.dexlib2.AccessFlags;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.reference.TypeReference;

import javax.annotation.Nonnull;

public final class TypeUtils {
    public static boolean isWideType(@Nonnull String type) {
        char c = type.charAt(0);
        return c == 'J' || c == 'D';
    }

    public static boolean isWideType(@Nonnull TypeReference type) {
        return isWideType(type.getType());
    }

    public static boolean isPrimitiveType(String type) {
        return type.length() == 1;
    }

    @Nonnull
    public static String getPackage(@Nonnull String type) {
        int lastSlash = type.lastIndexOf('/');
        if (lastSlash < 0) {
            return "";
        }
        return type.substring(1, lastSlash);
    }

    public static boolean canAccessClass(@Nonnull String accessorType, @Nonnull ClassDef accesseeClassDef) {
        if (AccessFlags.PUBLIC.isSet(accesseeClassDef.getAccessFlags())) {
            return true;
        }

        // Classes can only be public or package private. Any private or protected inner classes are actually
        // package private.
        return getPackage(accesseeClassDef.getType()).equals(getPackage(accessorType));
    }

    private TypeUtils() {}
}
