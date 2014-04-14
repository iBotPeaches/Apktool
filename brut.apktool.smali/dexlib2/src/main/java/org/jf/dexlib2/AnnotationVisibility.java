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

package org.jf.dexlib2;

import org.jf.util.ExceptionWithContext;

public final class AnnotationVisibility {
    public static final int BUILD = 0;
    public static final int RUNTIME = 1;
    public static final int SYSTEM = 2;

    private static String[] NAMES = new String[] {"build", "runtime", "system"};

    public static String getVisibility(int visibility) {
        if (visibility < 0 || visibility >= NAMES.length) {
            throw new ExceptionWithContext("Invalid annotation visibility %d", visibility);
        }
        return NAMES[visibility];
    }

    public static int getVisibility(String visibility) {
        visibility = visibility.toLowerCase();
        if (visibility.equals("build")) {
            return BUILD;
        }
        if (visibility.equals("runtime")) {
            return RUNTIME;
        }
        if (visibility.equals("system")) {
            return SYSTEM;
        }
        throw new ExceptionWithContext("Invalid annotation visibility: %s", visibility);
    }

    private AnnotationVisibility() {}
}
