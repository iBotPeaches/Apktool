/*
 * [The "BSD licence"]
 * Copyright (c) 2010 Ben Gruver (JesusFreke)
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

import java.util.Arrays;

public class OdexHeader {

    /**
     * the possible file format magic numbers, represented as the low-order bytes of a string.
     */
    public static final byte[] MAGIC_35 = new byte[] {0x64, 0x65, 0x79, 0x0A, 0x30, 0x33, 0x35, 0x00}; //"dey\n035" + '\0';
    public static final byte[] MAGIC_36 = new byte[] {0x64, 0x65, 0x79, 0x0A, 0x30, 0x33, 0x36, 0x00}; //"dey\n036" + '\0';

    public final byte[] magic;
    public final int dexOffset;
    public final int dexLength;
    public final int depsOffset;
    public final int depsLength;
    public final int auxOffset;
    public final int auxLength;
    public final int flags;

    public final int version;

    public OdexHeader(Input in) {
        magic = in.readBytes(8);

        if (Arrays.equals(MAGIC_35, magic)) {
            version = 35;
        } else if (Arrays.equals(MAGIC_36, magic)) {
            version = 36;
        } else {
            throw new RuntimeException("The magic value is not one of the expected values");
        }

        dexOffset = in.readInt();
        dexLength = in.readInt();
        depsOffset = in.readInt();
        depsLength = in.readInt();
        auxOffset = in.readInt();
        auxLength = in.readInt();
        flags = in.readInt();
        in.readInt(); //padding
    }
}
