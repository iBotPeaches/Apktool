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

package org.jf.baksmali;

import com.google.common.collect.Lists;
import org.jf.dexlib2.analysis.ClassPath;
import org.jf.dexlib2.analysis.InlineMethodResolver;
import org.jf.dexlib2.util.SyntheticAccessorResolver;

import java.util.Arrays;
import java.util.List;

public class baksmaliOptions {
    // register info values
    public static final int ALL = 1;
    public static final int ALLPRE = 2;
    public static final int ALLPOST = 4;
    public static final int ARGS = 8;
    public static final int DEST = 16;
    public static final int MERGE = 32;
    public static final int FULLMERGE = 64;

    public int apiLevel = 15;
    public String outputDirectory = "out";
    public List<String> bootClassPathDirs = Lists.newArrayList();

    public List<String> bootClassPathEntries = Lists.newArrayList();
    public List<String> extraClassPathEntries = Lists.newArrayList();

    public boolean noParameterRegisters = false;
    public boolean useLocalsDirective = false;
    public boolean useSequentialLabels = false;
    public boolean outputDebugInfo = true;
    public boolean addCodeOffsets = false;
    public boolean noAccessorComments = false;
    public boolean deodex = false;
    public boolean ignoreErrors = false;
    public boolean checkPackagePrivateAccess = false;
    public InlineMethodResolver inlineResolver = null;
    public int registerInfo = 0;
    public ClassPath classPath = null;
    public int jobs = -1;

    public SyntheticAccessorResolver syntheticAccessorResolver = null;

    public void setBootClassPath(String bootClassPath) {
        bootClassPathEntries = Lists.newArrayList(bootClassPath.split(":"));
    }

    public void addExtraClassPath(String extraClassPath) {
        if (extraClassPath.startsWith(":")) {
            extraClassPath = extraClassPath.substring(1);
        }
        extraClassPathEntries.addAll(Arrays.asList(extraClassPath.split(":")));
    }
}
