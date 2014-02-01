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

package org.jf.util;

import junit.framework.Assert;
import org.junit.Test;

import java.nio.charset.Charset;

public class ClassFileNameHandlerTest {
    private final Charset UTF8 = Charset.forName("UTF-8");

    @Test
    public void test1ByteEncodings() {
        StringBuilder sb = new StringBuilder();
        for (int i=0; i<100; i++) {
            sb.append((char)i);
        }

        String result = ClassFileNameHandler.shortenPathComponent(sb.toString(), 5);
        Assert.assertEquals(95, result.getBytes(UTF8).length);
        Assert.assertEquals(95, result.length());
    }

    @Test
    public void test2ByteEncodings() {
        StringBuilder sb = new StringBuilder();
        for (int i=0x80; i<0x80+100; i++) {
            sb.append((char)i);
        }

        // remove a total of 3 2-byte characters, and then add back in the 1-byte '#'
        String result = ClassFileNameHandler.shortenPathComponent(sb.toString(), 4);
        Assert.assertEquals(200, sb.toString().getBytes(UTF8).length);
        Assert.assertEquals(195, result.getBytes(UTF8).length);
        Assert.assertEquals(98, result.length());

        // remove a total of 3 2-byte characters, and then add back in the 1-byte '#'
        result = ClassFileNameHandler.shortenPathComponent(sb.toString(), 5);
        Assert.assertEquals(200, sb.toString().getBytes(UTF8).length);
        Assert.assertEquals(195, result.getBytes(UTF8).length);
        Assert.assertEquals(98, result.length());
    }

    @Test
    public void test3ByteEncodings() {
        StringBuilder sb = new StringBuilder();
        for (int i=0x800; i<0x800+100; i++) {
            sb.append((char)i);
        }

        // remove a total of 3 3-byte characters, and then add back in the 1-byte '#'
        String result = ClassFileNameHandler.shortenPathComponent(sb.toString(), 6);
        Assert.assertEquals(300, sb.toString().getBytes(UTF8).length);
        Assert.assertEquals(292, result.getBytes(UTF8).length);
        Assert.assertEquals(98, result.length());

        // remove a total of 3 3-byte characters, and then add back in the 1-byte '#'
        result = ClassFileNameHandler.shortenPathComponent(sb.toString(), 7);
        Assert.assertEquals(300, sb.toString().getBytes(UTF8).length);
        Assert.assertEquals(292, result.getBytes(UTF8).length);
        Assert.assertEquals(98, result.length());
    }

    public void test4ByteEncodings() {
        StringBuilder sb = new StringBuilder();
        for (int i=0x10000; i<0x10000+100; i++) {
            sb.appendCodePoint(i);
        }

        // we remove 3 codepoints == 6 characters == 12 bytes, and then add back in the 1-byte '#'
        String result = ClassFileNameHandler.shortenPathComponent(sb.toString(), 8);
        Assert.assertEquals(400, sb.toString().getBytes(UTF8).length);
        Assert.assertEquals(389, result.getBytes(UTF8).length);
        Assert.assertEquals(98, result.length());

        // we remove 3 codepoints == 6 characters == 12 bytes, and then add back in the 1-byte '#'
        result = ClassFileNameHandler.shortenPathComponent(sb.toString(), 7);
        Assert.assertEquals(400, sb.toString().getBytes(UTF8).length);
        Assert.assertEquals(3892, result.getBytes(UTF8).length);
        Assert.assertEquals(98, result.length());
    }
}
