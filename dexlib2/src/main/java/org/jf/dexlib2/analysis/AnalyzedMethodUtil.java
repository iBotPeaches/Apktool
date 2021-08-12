/*
 * Copyright 2015, Google Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 * Neither the name of Google Inc. nor the names of its
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

package org.jf.dexlib2.analysis;

import org.jf.dexlib2.AccessFlags;
import org.jf.dexlib2.analysis.util.TypeProtoUtils;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.util.MethodUtil;
import org.jf.dexlib2.util.TypeUtils;

import javax.annotation.Nonnull;

public class AnalyzedMethodUtil {
    public static boolean canAccess(@Nonnull TypeProto type, @Nonnull Method virtualMethod, boolean checkPackagePrivate,
                                    boolean checkProtected, boolean checkClass) {
        if (checkPackagePrivate && MethodUtil.isPackagePrivate(virtualMethod)) {
            String otherPackage = TypeUtils.getPackage(virtualMethod.getDefiningClass());
            String thisPackage = TypeUtils.getPackage(type.getType());
            if (!otherPackage.equals(thisPackage)) {
                return false;
            }
        }

        if (checkProtected && (virtualMethod.getAccessFlags() & AccessFlags.PROTECTED.getValue()) != 0) {
            if (!TypeProtoUtils.extendsFrom(type, virtualMethod.getDefiningClass())) {
                return false;
            }
        }

        if (checkClass) {
            ClassPath classPath = type.getClassPath();
            ClassDef methodClassDef = classPath.getClassDef(virtualMethod.getDefiningClass());
            if (!TypeUtils.canAccessClass(type.getType(), methodClassDef)) {
                return false;
            }
        }

        return true;
    }
}
