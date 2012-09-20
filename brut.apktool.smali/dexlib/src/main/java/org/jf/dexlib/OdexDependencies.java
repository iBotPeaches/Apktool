/*
 * [The "BSD licence"]
 * Copyright (c) 2010 Ben Gruver
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.jf.dexlib;

import org.jf.dexlib.Util.Input;

import java.io.UnsupportedEncodingException;

public class OdexDependencies {
    public final int modificationTime;
    public final int crc;
    public final int dalvikBuild;

    private final String[] dependencies;
    private final byte[][] dependencyChecksums;

    public OdexDependencies (Input in) {
        modificationTime = in.readInt();
        crc = in.readInt();
        dalvikBuild = in.readInt();

        int dependencyCount = in.readInt();

        dependencies = new String[dependencyCount];
        dependencyChecksums = new byte[dependencyCount][];

        for (int i=0; i<dependencyCount; i++) {
            int stringLength = in.readInt();

            try {
                dependencies[i] = new String(in.readBytes(stringLength), 0, stringLength-1, "US-ASCII");
            } catch (UnsupportedEncodingException ex) {
                throw new RuntimeException(ex);
            }
            dependencyChecksums[i] = in.readBytes(20);
        }
    }

    public int getDependencyCount() {
        return dependencies.length;
    }

    public String getDependency(int index) {
        return dependencies[index];
    }

    public byte[] getDependencyChecksum(int index) {
        return dependencyChecksums[index].clone();
    }
}
