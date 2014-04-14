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

package org.jf.dexlib2.dexbacked;

import junit.framework.Assert;
import org.jf.util.ExceptionWithContext;
import org.junit.Test;

public class BaseDexReaderLeb128Test {
    @Test
    public void testUleb128() {
        performTest(0x0, new byte[]{0x0, 0x11}, 1);
        performTest(0x1, new byte[]{0x1, 0x11}, 1);
        performTest(0x3f, new byte[]{0x3f, 0x11}, 1);
        performTest(0x40, new byte[]{0x40, 0x11}, 1);
        performTest(0x70, new byte[]{0x70, 0x11}, 1);
        performTest(0x7f, new byte[]{0x7f, 0x11}, 1);

        performTest(0x80, new byte[]{(byte)0x80, 0x1, 0x11}, 2);
        performTest(0x100, new byte[]{(byte)0x80, 0x2, 0x11}, 2);
        performTest(0x800, new byte[]{(byte)0x80, 0x10, 0x11}, 2);
        performTest(0x1f80, new byte[]{(byte)0x80, 0x3f, 0x11}, 2);
        performTest(0x2000, new byte[]{(byte)0x80, 0x40, 0x11}, 2);
        performTest(0x2080, new byte[]{(byte)0x80, 0x41, 0x11}, 2);
        performTest(0x3800, new byte[]{(byte)0x80, 0x70, 0x11}, 2);
        performTest(0x3f80, new byte[]{(byte)0x80, 0x7f, 0x11}, 2);

        performTest(0xff, new byte[]{(byte)0xff, 0x1, 0x11}, 2);
        performTest(0x17f, new byte[]{(byte)0xff, 0x2, 0x11}, 2);
        performTest(0x87f, new byte[]{(byte)0xff, 0x10, 0x11}, 2);
        performTest(0x1fff, new byte[]{(byte)0xff, 0x3f, 0x11}, 2);
        performTest(0x207f, new byte[]{(byte)0xff, 0x40, 0x11}, 2);
        performTest(0x20ff, new byte[]{(byte)0xff, 0x41, 0x11}, 2);
        performTest(0x387f, new byte[]{(byte)0xff, 0x70, 0x11}, 2);
        performTest(0x3fff, new byte[]{(byte)0xff, 0x7f, 0x11}, 2);

        performTest(0x4000, new byte[]{(byte)0x80, (byte)0x80, 0x1, 0x11}, 3);
        performTest(0x8000, new byte[]{(byte)0x80, (byte)0x80, 0x2, 0x11}, 3);
        performTest(0x40000, new byte[]{(byte)0x80, (byte)0x80, 0x10, 0x11}, 3);
        performTest(0xfc000, new byte[]{(byte)0x80, (byte)0x80, 0x3f, 0x11}, 3);
        performTest(0x100000, new byte[]{(byte)0x80, (byte)0x80, 0x40, 0x11}, 3);
        performTest(0x104000, new byte[]{(byte)0x80, (byte)0x80, 0x41, 0x11}, 3);
        performTest(0x1c0000, new byte[]{(byte)0x80, (byte)0x80, 0x70, 0x11}, 3);
        performTest(0x1fc000, new byte[]{(byte)0x80, (byte)0x80, 0x7f, 0x11}, 3);

        performTest(0x7fff, new byte[]{(byte)0xff, (byte)0xff, 0x1, 0x11}, 3);
        performTest(0xbfff, new byte[]{(byte)0xff, (byte)0xff, 0x2, 0x11}, 3);
        performTest(0x43fff, new byte[]{(byte)0xff, (byte)0xff, 0x10, 0x11}, 3);
        performTest(0xfffff, new byte[]{(byte)0xff, (byte)0xff, 0x3f, 0x11}, 3);
        performTest(0x103fff, new byte[]{(byte)0xff, (byte)0xff, 0x40, 0x11}, 3);
        performTest(0x107fff, new byte[]{(byte)0xff, (byte)0xff, 0x41, 0x11}, 3);
        performTest(0x1c3fff, new byte[]{(byte)0xff, (byte)0xff, 0x70, 0x11}, 3);
        performTest(0x1fffff, new byte[]{(byte)0xff, (byte)0xff, 0x7f, 0x11}, 3);

        performTest(0x200000, new byte[]{(byte)0x80, (byte)0x80, (byte)0x80, 0x1, 0x11}, 4);
        performTest(0x400000, new byte[]{(byte)0x80, (byte)0x80, (byte)0x80, 0x2, 0x11}, 4);
        performTest(0x2000000, new byte[]{(byte)0x80, (byte)0x80, (byte)0x80, 0x10, 0x11}, 4);
        performTest(0x7e00000, new byte[]{(byte)0x80, (byte)0x80, (byte)0x80, 0x3f, 0x11}, 4);
        performTest(0x8000000, new byte[]{(byte)0x80, (byte)0x80, (byte)0x80, 0x40, 0x11}, 4);
        performTest(0x8200000, new byte[]{(byte)0x80, (byte)0x80, (byte)0x80, 0x41, 0x11}, 4);
        performTest(0xe000000, new byte[]{(byte)0x80, (byte)0x80, (byte)0x80, 0x70, 0x11}, 4);
        performTest(0xfe00000, new byte[]{(byte)0x80, (byte)0x80, (byte)0x80, 0x7f, 0x11}, 4);

        performTest(0x3fffff, new byte[]{(byte)0xff, (byte)0xff, (byte)0xff, 0x1, 0x11}, 4);
        performTest(0x5fffff, new byte[]{(byte)0xff, (byte)0xff, (byte)0xff, 0x2, 0x11}, 4);
        performTest(0x21fffff, new byte[]{(byte)0xff, (byte)0xff, (byte)0xff, 0x10, 0x11}, 4);
        performTest(0x7ffffff, new byte[]{(byte)0xff, (byte)0xff, (byte)0xff, 0x3f, 0x11}, 4);
        performTest(0x81fffff, new byte[]{(byte)0xff, (byte)0xff, (byte)0xff, 0x40, 0x11}, 4);
        performTest(0x83fffff, new byte[]{(byte)0xff, (byte)0xff, (byte)0xff, 0x41, 0x11}, 4);
        performTest(0xe1fffff, new byte[]{(byte)0xff, (byte)0xff, (byte)0xff, 0x70, 0x11}, 4);
        performTest(0xfffffff, new byte[]{(byte)0xff, (byte)0xff, (byte)0xff, 0x7f, 0x11}, 4);

        performTest(0x10000000, new byte[]{(byte)0x80, (byte)0x80, (byte)0x80, (byte)0x80, 0x1, 0x11}, 5);
        performTest(0x20000000, new byte[]{(byte)0x80, (byte)0x80, (byte)0x80, (byte)0x80, 0x2, 0x11}, 5);
        performTest(0x70000000, new byte[]{(byte)0x80, (byte)0x80, (byte)0x80, (byte)0x80, 0x7, 0x11}, 5);
        performTest(0x70000000, new byte[]{(byte)0x80, (byte)0x80, (byte)0x80, (byte)0x80, 0x17, 0x11}, 5);
        performTest(0x70000000, new byte[]{(byte)0x80, (byte)0x80, (byte)0x80, (byte)0x80, 0x47, 0x11}, 5);
        performTest(0x70000000, new byte[]{(byte)0x80, (byte)0x80, (byte)0x80, (byte)0x80, 0x77, 0x11}, 5);

        performTest(0x1fffffff, new byte[]{(byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, 0x1, 0x11}, 5);
        performTest(0x2fffffff, new byte[]{(byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, 0x2, 0x11}, 5);
        performTest(0x7fffffff, new byte[]{(byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, 0x7, 0x11}, 5);
        performTest(0x7fffffff, new byte[]{(byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, 0x17, 0x11}, 5);
        performTest(0x7fffffff, new byte[]{(byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, 0x47, 0x11}, 5);
        performTest(0x7fffffff, new byte[]{(byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, 0x77, 0x11}, 5);

        performTest(0xcc, new byte[]{(byte)0xcc, 0x1});
        performTest(0x3b67, new byte[]{(byte)0xe7, 0x76});
        performTest(0x1b857589, new byte[]{(byte)0x89, (byte)0xeb, (byte)0x95, (byte)0xdc, 0x1});
        performTest(0x375d82e5, new byte[]{(byte)0xe5, (byte)0x85, (byte)0xf6, (byte)0xba, 0x3});
        performTest(0x5524da90, new byte[]{(byte)0x90, (byte)0xb5, (byte)0x93, (byte)0xa9, 0x5});
        performTest(0x35, new byte[]{0x35});
        performTest(0xd7, new byte[]{(byte)0xd7, 0x1});
        performTest(0x63, new byte[]{0x63});
        performTest(0x22cb5b, new byte[]{(byte)0xdb, (byte)0x96, (byte)0x8b, 0x1});
        performTest(0x585e, new byte[]{(byte)0xde, (byte)0xb0, 0x1});
        performTest(0x5d62a965, new byte[]{(byte)0xe5, (byte)0xd2, (byte)0x8a, (byte)0xeb, 0x5});
        performTest(0x6af172db, new byte[]{(byte)0xdb, (byte)0xe5, (byte)0xc5, (byte)0xd7, 0x6});
        performTest(0xe, new byte[]{0xe});
        performTest(0xb75f7a, new byte[]{(byte)0xfa, (byte)0xbe, (byte)0xdd, 0x5});
        performTest(0x8604, new byte[]{(byte)0x84, (byte)0x8c, 0x2});
        performTest(0x31624026, new byte[]{(byte)0xa6, (byte)0x80, (byte)0x89, (byte)0x8b, 0x3});
        performTest(0x8d, new byte[]{(byte)0x8d, 0x1});
        performTest(0xc0, new byte[]{(byte)0xc0, 0x1});
        performTest(0xd7618cb, new byte[]{(byte)0xcb, (byte)0xb1, (byte)0xd8, 0x6b});
        performTest(0xff, new byte[]{(byte)0xff, 0x1});
        performTest(0x5c923e42, new byte[]{(byte)0xc2, (byte)0xfc, (byte)0xc8, (byte)0xe4, 0x5});
        performTest(0x91, new byte[]{(byte)0x91, 0x1});
        performTest(0xbe0f97, new byte[]{(byte)0x97, (byte)0x9f, (byte)0xf8, 0x5});
        performTest(0x88bc786, new byte[]{(byte)0x86, (byte)0x8f, (byte)0xaf, 0x44});
        performTest(0x8caa9a, new byte[]{(byte)0x9a, (byte)0xd5, (byte)0xb2, 0x4});
        performTest(0x4aee, new byte[]{(byte)0xee, (byte)0x95, 0x1});
        performTest(0x438c86, new byte[]{(byte)0x86, (byte)0x99, (byte)0x8e, 0x2});
        performTest(0xc0, new byte[]{(byte)0xc0, 0x1});
        performTest(0xb486, new byte[]{(byte)0x86, (byte)0xe9, 0x2});
        performTest(0x83fd, new byte[]{(byte)0xfd, (byte)0x87, 0x2});
        performTest(0x7b, new byte[]{0x7b});
        performTest(0x1dc84e14, new byte[]{(byte)0x94, (byte)0x9c, (byte)0xa1, (byte)0xee, 0x1});
        performTest(0x2dfc, new byte[]{(byte)0xfc, 0x5b});
        performTest(0x88, new byte[]{(byte)0x88, 0x1});
        performTest(0x919e, new byte[]{(byte)0x9e, (byte)0xa3, 0x2});
        performTest(0x2fcf, new byte[]{(byte)0xcf, 0x5f});
        performTest(0xf00674, new byte[]{(byte)0xf4, (byte)0x8c, (byte)0xc0, 0x7});
        performTest(0xed5f7d, new byte[]{(byte)0xfd, (byte)0xbe, (byte)0xb5, 0x7});
        performTest(0xdbd9, new byte[]{(byte)0xd9, (byte)0xb7, 0x3});
        performTest(0xa1, new byte[]{(byte)0xa1, 0x1});
        performTest(0xf6f76c, new byte[]{(byte)0xec, (byte)0xee, (byte)0xdb, 0x7});
        performTest(0x1eed6f, new byte[]{(byte)0xef, (byte)0xda, 0x7b});
        performTest(0x95c, new byte[]{(byte)0xdc, 0x12});
        performTest(0x1e, new byte[]{0x1e});
        performTest(0xe5, new byte[]{(byte)0xe5, 0x1});
        performTest(0x2f2f13, new byte[]{(byte)0x93, (byte)0xde, (byte)0xbc, 0x1});
        performTest(0x19, new byte[]{0x19});
        performTest(0x3f, new byte[]{0x3f});
        performTest(0x75e3, new byte[]{(byte)0xe3, (byte)0xeb, 0x1});
        performTest(0x67a4c4, new byte[]{(byte)0xc4, (byte)0xc9, (byte)0x9e, 0x3});
        performTest(0xb948, new byte[]{(byte)0xc8, (byte)0xf2, 0x2});
        performTest(0x34b1c9de, new byte[]{(byte)0xde, (byte)0x93, (byte)0xc7, (byte)0xa5, 0x3});
        performTest(0x58f0, new byte[]{(byte)0xf0, (byte)0xb1, 0x1});
        performTest(0x0, new byte[]{0x0});
        performTest(0x9ab3e5, new byte[]{(byte)0xe5, (byte)0xe7, (byte)0xea, 0x4});
        performTest(0x4c4a8a3d, new byte[]{(byte)0xbd, (byte)0x94, (byte)0xaa, (byte)0xe2, 0x4});
        performTest(0x99, new byte[]{(byte)0x99, 0x1});
        performTest(0x1a67e9, new byte[]{(byte)0xe9, (byte)0xcf, 0x69});
        performTest(0x5ddb2d, new byte[]{(byte)0xad, (byte)0xb6, (byte)0xf7, 0x2});
        performTest(0xeccb680, new byte[]{(byte)0x80, (byte)0xed, (byte)0xb2, 0x76});
        performTest(0x6910bbf0, new byte[]{(byte)0xf0, (byte)0xf7, (byte)0xc2, (byte)0xc8, 0x6});
        performTest(0xc5, new byte[]{(byte)0xc5, 0x1});
        performTest(0xdd7225, new byte[]{(byte)0xa5, (byte)0xe4, (byte)0xf5, 0x6});
        performTest(0x4561ea2e, new byte[]{(byte)0xae, (byte)0xd4, (byte)0x87, (byte)0xab, 0x4});
        performTest(0x7f4f08, new byte[]{(byte)0x88, (byte)0x9e, (byte)0xfd, 0x3});
        performTest(0x197f, new byte[]{(byte)0xff, 0x32});
        performTest(0xb8ad13, new byte[]{(byte)0x93, (byte)0xda, (byte)0xe2, 0x5});
        performTest(0x3c8d5db4, new byte[]{(byte)0xb4, (byte)0xbb, (byte)0xb5, (byte)0xe4, 0x3});
        performTest(0x7e4bdf7d, new byte[]{(byte)0xfd, (byte)0xbe, (byte)0xaf, (byte)0xf2, 0x7});
        performTest(0x1e8e23, new byte[]{(byte)0xa3, (byte)0x9c, 0x7a});
        performTest(0x1602, new byte[]{(byte)0x82, 0x2c});
        performTest(0xe2, new byte[]{(byte)0xe2, 0x1});
        performTest(0x38e9, new byte[]{(byte)0xe9, 0x71});
        performTest(0xbf8665, new byte[]{(byte)0xe5, (byte)0x8c, (byte)0xfe, 0x5});
        performTest(0x43, new byte[]{0x43});
        performTest(0xc9d96c, new byte[]{(byte)0xec, (byte)0xb2, (byte)0xa7, 0x6});
        performTest(0x4bd170, new byte[]{(byte)0xf0, (byte)0xa2, (byte)0xaf, 0x2});
        performTest(0x86c11b, new byte[]{(byte)0x9b, (byte)0x82, (byte)0x9b, 0x4});
        performTest(0x1a2611e7, new byte[]{(byte)0xe7, (byte)0xa3, (byte)0x98, (byte)0xd1, 0x1});
        performTest(0xff2f6a, new byte[]{(byte)0xea, (byte)0xde, (byte)0xfc, 0x7});
        performTest(0x6f051635, new byte[]{(byte)0xb5, (byte)0xac, (byte)0x94, (byte)0xf8, 0x6});
        performTest(0x75bf, new byte[]{(byte)0xbf, (byte)0xeb, 0x1});
        performTest(0xe8ce45, new byte[]{(byte)0xc5, (byte)0x9c, (byte)0xa3, 0x7});
        performTest(0x2946a1d8, new byte[]{(byte)0xd8, (byte)0xc3, (byte)0x9a, (byte)0xca, 0x2});
        performTest(0xe2, new byte[]{(byte)0xe2, 0x1});
        performTest(0x44ee, new byte[]{(byte)0xee, (byte)0x89, 0x1});
        performTest(0x447a, new byte[]{(byte)0xfa, (byte)0x88, 0x1});
        performTest(0x917, new byte[]{(byte)0x97, 0x12});
        performTest(0x25, new byte[]{0x25});
        performTest(0x52c2b8eb, new byte[]{(byte)0xeb, (byte)0xf1, (byte)0x8a, (byte)0x96, 0x5});
        performTest(0x17dabee4, new byte[]{(byte)0xe4, (byte)0xfd, (byte)0xea, (byte)0xbe, 0x1});
        performTest(0x9d6a, new byte[]{(byte)0xea, (byte)0xba, 0x2});
        performTest(0xc4b12d, new byte[]{(byte)0xad, (byte)0xe2, (byte)0x92, 0x6});
        performTest(0xc9561d, new byte[]{(byte)0x9d, (byte)0xac, (byte)0xa5, 0x6});
        performTest(0x88a7, new byte[]{(byte)0xa7, (byte)0x91, 0x2});
        performTest(0x527d8f7a, new byte[]{(byte)0xfa, (byte)0x9e, (byte)0xf6, (byte)0x93, 0x5});
        performTest(0x2c31, new byte[]{(byte)0xb1, 0x58});
        performTest(0x3b8c, new byte[]{(byte)0x8c, 0x77});
        performTest(0xc228, new byte[]{(byte)0xa8, (byte)0x84, 0x3});
        performTest(0xd730d3, new byte[]{(byte)0xd3, (byte)0xe1, (byte)0xdc, 0x6});
    }

    @Test
    public void testUleb128Failure() {
        // result doesn't fit into a signed int
        performFailureTest(new byte[]{(byte)0x80, (byte)0x80, (byte)0x80, (byte)0x80, 0x8, 0x11});
        performFailureTest(new byte[]{(byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, 0x8, 0x11});
        performFailureTest(new byte[]{(byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, 0x9, 0x11});
        performFailureTest(new byte[]{(byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, 0xa, 0x11});
        performFailureTest(new byte[]{(byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, 0xb, 0x11});
        performFailureTest(new byte[]{(byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, 0xc, 0x11});
        performFailureTest(new byte[]{(byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, 0xd, 0x11});
        performFailureTest(new byte[]{(byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, 0xe, 0x11});
        performFailureTest(new byte[]{(byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, 0xf, 0x11});
        performFailureTest(new byte[]{(byte)0x80, (byte)0x80, (byte)0x80, (byte)0x80, 0x18, 0x11});
        performFailureTest(new byte[]{(byte)0x80, (byte)0x80, (byte)0x80, (byte)0x80, 0x29, 0x11});
        performFailureTest(new byte[]{(byte)0x80, (byte)0x80, (byte)0x80, (byte)0x80, 0x7a, 0x11});

        // MSB of last byte is set
        performFailureTest(new byte[]{(byte)0x80, (byte)0x80, (byte)0x80, (byte)0x80, (byte)0x80, 0x11});
        performFailureTest(new byte[]{(byte)0x80, (byte)0x80, (byte)0x80, (byte)0x80, (byte)0x81, 0x11});
        performFailureTest(new byte[]{(byte)0x80, (byte)0x80, (byte)0x80, (byte)0x80, (byte)0xa0, 0x11});
        performFailureTest(new byte[]{(byte)0x80, (byte)0x80, (byte)0x80, (byte)0x80, (byte)0xf0, 0x11});
        performFailureTest(new byte[]{(byte)0x80, (byte)0x80, (byte)0x80, (byte)0x80, (byte)0xff, 0x11});
        performFailureTest(new byte[]{(byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, 0x11});
    }


    private void performTest(int expectedValue, byte[] buf) {
        performTest(expectedValue, buf, buf.length);
    }

    private void performTest(int expectedValue, byte[] buf, int expectedLength) {
        BaseDexBuffer dexBuf = new BaseDexBuffer(buf);
        BaseDexReader reader = dexBuf.readerAt(0);
        Assert.assertEquals(expectedValue, reader.readSmallUleb128());
        Assert.assertEquals(expectedLength, reader.getOffset());

        reader = dexBuf.readerAt(0);
        reader.skipUleb128();
        Assert.assertEquals(expectedLength, reader.getOffset());
    }

    private void performFailureTest(byte[] buf) {
        BaseDexBuffer dexBuf = new BaseDexBuffer(buf);
        BaseDexReader reader = dexBuf.readerAt(0);
        try {
            reader.readSmallUleb128();
            Assert.fail();
        } catch (ExceptionWithContext ex) {
            // expected exception
        }
    }
}
