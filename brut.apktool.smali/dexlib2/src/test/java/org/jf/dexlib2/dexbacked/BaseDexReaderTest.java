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

public class BaseDexReaderTest {
    @Test
    public void testSizedInt() {
        performSizedIntTest(0, new byte[]{0x00});
        performSizedIntTest(0, new byte[]{0x00, 0x00});
        performSizedIntTest(0, new byte[]{0x00, 0x00, 0x00});
        performSizedIntTest(0, new byte[]{0x00, 0x00, 0x00, 0x00});
        performSizedIntTest(1, new byte[]{0x01});
        performSizedIntTest(1, new byte[]{0x01, 0x00, 0x00, 0x00});
        performSizedIntTest(0x40, new byte[]{0x40});
        performSizedIntTest(0x7f, new byte[]{0x7f});
        performSizedIntTest(0xffffff80, new byte[]{(byte)0x80});
        performSizedIntTest(-1, new byte[]{(byte)0xff});

        performSizedIntTest(0x100, new byte[]{0x00, 0x01});
        performSizedIntTest(0x110, new byte[]{0x10, 0x01});
        performSizedIntTest(0x7f01, new byte[]{0x01, 0x7f});
        performSizedIntTest(0xffff8001, new byte[]{0x01, (byte)0x80});
        performSizedIntTest(0xffffff10, new byte[]{0x10, (byte)0xff});

        performSizedIntTest(0x11001, new byte[]{0x01, 0x10, 0x01});
        performSizedIntTest(0x7f0110, new byte[]{0x10, 0x01, 0x7f});
        performSizedIntTest(0xff801001, new byte[]{0x01, 0x10, (byte)0x80});
        performSizedIntTest(0xffff0110, new byte[]{0x10, 0x01, (byte)0xff});

        performSizedIntTest(0x1003002, new byte[]{0x02, 0x30, 0x00, 0x01});
        performSizedIntTest(0x7f110230, new byte[]{0x30, 0x02, 0x11, 0x7f});
        performSizedIntTest(0x80112233, new byte[]{0x33, 0x22, 0x11, (byte)0x80});
        performSizedIntTest(-1, new byte[]{(byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff});
    }

    private void performSizedIntTest(int expectedValue, byte[] buf) {
        BaseDexBuffer dexBuf = new BaseDexBuffer(buf);
        BaseDexReader reader = dexBuf.readerAt(0);
        Assert.assertEquals(expectedValue, reader.readSizedInt(buf.length));
    }

    @Test
    public void testSizedIntFailure() {
        // wrong size
        performSizedIntFailureTest(new byte[]{});
        performSizedIntFailureTest(new byte[]{0x00, 0x00, 0x00, 0x00, 0x00});
        performSizedIntFailureTest(new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00});
        performSizedIntFailureTest(new byte[]{0x12, 0x34, 0x56, 0x12, 0x34, 0x56, 0x78});
    }

    private void performSizedIntFailureTest(byte[] buf) {
        BaseDexBuffer dexBuf = new BaseDexBuffer(buf);
        BaseDexReader reader = dexBuf.readerAt(0);
        try {
            reader.readSizedInt(buf.length);
            Assert.fail();
        } catch (ExceptionWithContext ex) {
            // expected
        }
    }

    @Test
    public void testSizedSmallUint() {
        performSizedSmallUintTest(0, new byte[]{0x00});
        performSizedSmallUintTest(0, new byte[]{0x00, 0x00});
        performSizedSmallUintTest(0, new byte[]{0x00, 0x00, 0x00});
        performSizedSmallUintTest(0, new byte[]{0x00, 0x00, 0x00, 0x00});
        performSizedSmallUintTest(1, new byte[]{0x01});
        performSizedSmallUintTest(1, new byte[]{0x01, 0x00, 0x00, 0x00});
        performSizedSmallUintTest(0x40, new byte[]{0x40});
        performSizedSmallUintTest(0x7f, new byte[]{0x7f});
        performSizedSmallUintTest(0x80, new byte[]{(byte)0x80});
        performSizedSmallUintTest(0xff, new byte[]{(byte)0xff});

        performSizedSmallUintTest(0x100, new byte[]{0x00, 0x01});
        performSizedSmallUintTest(0x110, new byte[]{0x10, 0x01});
        performSizedSmallUintTest(0x7f01, new byte[]{0x01, 0x7f});
        performSizedSmallUintTest(0x8001, new byte[]{0x01, (byte)0x80});
        performSizedSmallUintTest(0xff10, new byte[]{0x10, (byte)0xff});

        performSizedSmallUintTest(0x11001, new byte[]{0x01, 0x10, 0x01});
        performSizedSmallUintTest(0x7f0110, new byte[]{0x10, 0x01, 0x7f});
        performSizedSmallUintTest(0x801001, new byte[]{0x01, 0x10, (byte)0x80});
        performSizedSmallUintTest(0xff0110, new byte[]{0x10, 0x01, (byte)0xff});

        performSizedSmallUintTest(0x1003002, new byte[]{0x02, 0x30, 0x00, 0x01});
        performSizedSmallUintTest(0x7f110230, new byte[]{0x30, 0x02, 0x11, 0x7f});
        performSizedSmallUintTest(Integer.MAX_VALUE, new byte[]{(byte)0xff, (byte)0xff, (byte)0xff, 0x7f});
    }

    private void performSizedSmallUintTest(int expectedValue, byte[] buf) {
        BaseDexBuffer dexBuf = new BaseDexBuffer(buf);
        BaseDexReader reader = dexBuf.readerAt(0);
        Assert.assertEquals(expectedValue, reader.readSizedSmallUint(buf.length));
    }

    @Test
    public void testSizedSmallUintFailure() {
        // wrong size
        performSizedSmallUintFailureTest(new byte[]{});
        performSizedSmallUintFailureTest(new byte[]{0x00, 0x00, 0x00, 0x00, 0x00});
        performSizedSmallUintFailureTest(new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00});
        performSizedSmallUintFailureTest(new byte[]{0x12, 0x34, 0x56, 0x12, 0x34, 0x56, 0x78});

        // MSB set
        performSizedSmallUintFailureTest(new byte[]{0x00, 0x00, 0x00, (byte)0x80});
        performSizedSmallUintFailureTest(new byte[]{(byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff});
    }

    private void performSizedSmallUintFailureTest(byte[] buf) {
        BaseDexBuffer dexBuf = new BaseDexBuffer(buf);
        BaseDexReader reader = dexBuf.readerAt(0);
        try {
            reader.readSizedSmallUint(buf.length);
            Assert.fail();
        } catch (ExceptionWithContext ex) {
            // expected
        }
    }

    @Test
    public void testSizedRightExtendedInt() {
        performSizedRightExtendedIntTest(0, new byte[]{0x00});
        performSizedRightExtendedIntTest(0, new byte[]{0x00, 0x00});
        performSizedRightExtendedIntTest(0, new byte[]{0x00, 0x00, 0x00});
        performSizedRightExtendedIntTest(0, new byte[]{0x00, 0x00, 0x00, 0x00});

        performSizedRightExtendedIntTest(0x01000000, new byte[]{0x01});
        performSizedRightExtendedIntTest(0x7f000000, new byte[]{0x7f});
        performSizedRightExtendedIntTest(0x80000000, new byte[]{(byte) 0x80});
        performSizedRightExtendedIntTest(0xf0000000, new byte[]{(byte) 0xf0});
        performSizedRightExtendedIntTest(0xff000000, new byte[]{(byte) 0xff});

        performSizedRightExtendedIntTest(0x010000, new byte[]{0x01, 0x00});
        performSizedRightExtendedIntTest(0x01100000, new byte[]{0x10, 0x01});
        performSizedRightExtendedIntTest(0x7f100000, new byte[]{0x10, 0x7f});
        performSizedRightExtendedIntTest(0x80100000, new byte[]{0x10, (byte) 0x80});
        performSizedRightExtendedIntTest(0xf0100000, new byte[]{0x10, (byte) 0xf0});
        performSizedRightExtendedIntTest(0xff100000, new byte[]{0x10, (byte) 0xff});
        performSizedRightExtendedIntTest(0xff000000, new byte[]{0x00, (byte) 0xff});

        performSizedRightExtendedIntTest(0x0100, new byte[]{0x01, 0x00, 0x00});
        performSizedRightExtendedIntTest(0x01101000, new byte[]{0x10, 0x10, 0x01});
        performSizedRightExtendedIntTest(0x7f101000, new byte[]{0x10, 0x10, 0x7f});
        performSizedRightExtendedIntTest(0x80101000, new byte[]{0x10, 0x10, (byte) 0x80});
        performSizedRightExtendedIntTest(0xf0101000, new byte[]{0x10, 0x10, (byte) 0xf0});
        performSizedRightExtendedIntTest(0xff101000, new byte[]{0x10, 0x10, (byte) 0xff});
        performSizedRightExtendedIntTest(0xff000000, new byte[]{0x00, 0x00, (byte) 0xff});

        performSizedRightExtendedIntTest(0x01, new byte[]{0x01, 0x00, 0x00, 0x00});
        performSizedRightExtendedIntTest(0x80, new byte[]{(byte) 0x80, 0x00, 0x00, 0x00});
        performSizedRightExtendedIntTest(0xff, new byte[]{(byte) 0xff, 0x00, 0x00, 0x00});
        performSizedRightExtendedIntTest(0x01101010, new byte[]{0x10, 0x10, 0x10, 0x01});
        performSizedRightExtendedIntTest(0x7f101010, new byte[]{0x10, 0x10, 0x10, 0x7f});
        performSizedRightExtendedIntTest(0x80101010, new byte[]{0x10, 0x10, 0x10, (byte) 0x80});
        performSizedRightExtendedIntTest(0xf0101010, new byte[]{0x10, 0x10, 0x10, (byte) 0xf0});
        performSizedRightExtendedIntTest(0xff101010, new byte[]{0x10, 0x10, 0x10, (byte) 0xff});
        performSizedRightExtendedIntTest(0xff000000, new byte[]{0x00, 0x00, 0x00, (byte) 0xff});
    }

    private void performSizedRightExtendedIntTest(int expectedValue, byte[] buf) {
        BaseDexBuffer dexBuf = new BaseDexBuffer(buf);
        BaseDexReader reader = dexBuf.readerAt(0);
        Assert.assertEquals(expectedValue, reader.readSizedRightExtendedInt(buf.length));
    }

    @Test
    public void testSizedRightExtendedIntFailure() {
        // wrong size
        performSizedRightExtendedIntFailureTest(new byte[]{});
        performSizedRightExtendedIntFailureTest(new byte[]{0x00, 0x00, 0x00, 0x00, 0x00});
        performSizedRightExtendedIntFailureTest(new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00});
        performSizedRightExtendedIntFailureTest(new byte[]{0x12, 0x34, 0x56, 0x12, 0x34, 0x56, 0x78});
    }

    private void performSizedRightExtendedIntFailureTest(byte[] buf) {
        BaseDexBuffer dexBuf = new BaseDexBuffer(buf);
        BaseDexReader reader = dexBuf.readerAt(0);
        try {
            reader.readSizedRightExtendedInt(buf.length);
            Assert.fail();
        } catch (ExceptionWithContext ex) {
            // expected
        }
    }

    @Test
    public void testSizedRightExtendedLong() {
        performSizedRightExtendedLongTest(0, new byte[]{0x00});
        performSizedRightExtendedLongTest(0, new byte[]{0x00, 0x00});
        performSizedRightExtendedLongTest(0, new byte[]{0x00, 0x00, 0x00});
        performSizedRightExtendedLongTest(0, new byte[]{0x00, 0x00, 0x00, 0x00});
        performSizedRightExtendedLongTest(0, new byte[]{0x00, 0x00, 0x00, 0x00, 0x00});
        performSizedRightExtendedLongTest(0, new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00});
        performSizedRightExtendedLongTest(0, new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00});
        performSizedRightExtendedLongTest(0, new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00});

        performSizedRightExtendedLongTest(0x0100000000000000L, new byte[]{0x01});
        performSizedRightExtendedLongTest(0x7f00000000000000L, new byte[]{0x7f});
        performSizedRightExtendedLongTest(0x8000000000000000L, new byte[]{(byte)0x80});
        performSizedRightExtendedLongTest(0xf000000000000000L, new byte[]{(byte)0xf0});
        performSizedRightExtendedLongTest(0xff00000000000000L, new byte[]{(byte)0xff});

        performSizedRightExtendedLongTest(0x01000000000000L, new byte[]{0x01, 0x00});
        performSizedRightExtendedLongTest(0x0110000000000000L, new byte[]{0x10, 0x01});
        performSizedRightExtendedLongTest(0x7f10000000000000L, new byte[]{0x10, 0x7f});
        performSizedRightExtendedLongTest(0x8010000000000000L, new byte[]{0x10, (byte)0x80});
        performSizedRightExtendedLongTest(0xf010000000000000L, new byte[]{0x10, (byte)0xf0});
        performSizedRightExtendedLongTest(0xff10000000000000L, new byte[]{0x10, (byte)0xff});
        performSizedRightExtendedLongTest(0xff00000000000000L, new byte[]{0x00, (byte)0xff});
        performSizedRightExtendedLongTest(0x7fff000000000000L, new byte[]{(byte)0xff, (byte)0x7f});

        performSizedRightExtendedLongTest(0x010000000000L, new byte[]{0x01, 0x00, 0x00});
        performSizedRightExtendedLongTest(0x0110100000000000L, new byte[]{0x10, 0x10, 0x01});
        performSizedRightExtendedLongTest(0x7f10100000000000L, new byte[]{0x10, 0x10, 0x7f});
        performSizedRightExtendedLongTest(0x8010100000000000L, new byte[]{0x10, 0x10, (byte)0x80});
        performSizedRightExtendedLongTest(0xf010100000000000L, new byte[]{0x10, 0x10, (byte)0xf0});
        performSizedRightExtendedLongTest(0xff10100000000000L, new byte[]{0x10, 0x10, (byte)0xff});
        performSizedRightExtendedLongTest(0xff00000000000000L, new byte[]{0x00, 0x00, (byte)0xff});
        performSizedRightExtendedLongTest(0x7fffff0000000000L, new byte[]{(byte)0xff, (byte)0xff, (byte)0x7f});

        performSizedRightExtendedLongTest(0x0100000000L, new byte[]{0x01, 0x00, 0x00, 0x00});
        performSizedRightExtendedLongTest(0x0110101000000000L, new byte[]{0x10, 0x10, 0x10, 0x01});
        performSizedRightExtendedLongTest(0x7f10101000000000L, new byte[]{0x10, 0x10, 0x10, 0x7f});
        performSizedRightExtendedLongTest(0x8010101000000000L, new byte[]{0x10, 0x10, 0x10, (byte)0x80});
        performSizedRightExtendedLongTest(0xf010101000000000L, new byte[]{0x10, 0x10, 0x10, (byte)0xf0});
        performSizedRightExtendedLongTest(0xff10101000000000L, new byte[]{0x10, 0x10, 0x10, (byte)0xff});
        performSizedRightExtendedLongTest(0xff00000000000000L, new byte[]{0x00, 0x00, 0x00, (byte)0xff});
        performSizedRightExtendedLongTest(0x7fffffff00000000L, new byte[]{(byte)0xff, (byte)0xff, (byte)0xff, (byte)0x7f});

        performSizedRightExtendedLongTest(0x01000000L, new byte[]{0x01, 0x00, 0x00, 0x00, 0x00});
        performSizedRightExtendedLongTest(0x0110101010000000L, new byte[]{0x10, 0x10, 0x10, 0x10, 0x01});
        performSizedRightExtendedLongTest(0x7f10101010000000L, new byte[]{0x10, 0x10, 0x10, 0x10, 0x7f});
        performSizedRightExtendedLongTest(0x8010101010000000L, new byte[]{0x10, 0x10, 0x10, 0x10, (byte)0x80});
        performSizedRightExtendedLongTest(0xf010101010000000L, new byte[]{0x10, 0x10, 0x10, 0x10, (byte)0xf0});
        performSizedRightExtendedLongTest(0xff10101010000000L, new byte[]{0x10, 0x10, 0x10, 0x10, (byte)0xff});
        performSizedRightExtendedLongTest(0xff00000000000000L, new byte[]{0x00, 0x00, 0x00, 0x00, (byte)0xff});
        performSizedRightExtendedLongTest(0x7fffffffff000000L, new byte[]{(byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0x7f});

        performSizedRightExtendedLongTest(0x010000L, new byte[]{0x01, 0x00, 0x00, 0x00, 0x00, 0x00});
        performSizedRightExtendedLongTest(0x0110101010100000L, new byte[]{0x10, 0x10, 0x10, 0x10, 0x10, 0x01});
        performSizedRightExtendedLongTest(0x7f10101010100000L, new byte[]{0x10, 0x10, 0x10, 0x10, 0x10, 0x7f});
        performSizedRightExtendedLongTest(0x8010101010100000L, new byte[]{0x10, 0x10, 0x10, 0x10, 0x10, (byte)0x80});
        performSizedRightExtendedLongTest(0xf010101010100000L, new byte[]{0x10, 0x10, 0x10, 0x10, 0x10, (byte)0xf0});
        performSizedRightExtendedLongTest(0xff10101010100000L, new byte[]{0x10, 0x10, 0x10, 0x10, 0x10, (byte)0xff});
        performSizedRightExtendedLongTest(0xff00000000000000L, new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, (byte)0xff});
        performSizedRightExtendedLongTest(0x7fffffffffff0000L, new byte[]{(byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0x7f});

        performSizedRightExtendedLongTest(0x0100L, new byte[]{0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00});
        performSizedRightExtendedLongTest(0x0110101010101000L, new byte[]{0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x01});
        performSizedRightExtendedLongTest(0x7f10101010101000L, new byte[]{0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x7f});
        performSizedRightExtendedLongTest(0x8010101010101000L, new byte[]{0x10, 0x10, 0x10, 0x10, 0x10, 0x10, (byte)0x80});
        performSizedRightExtendedLongTest(0xf010101010101000L, new byte[]{0x10, 0x10, 0x10, 0x10, 0x10, 0x10, (byte)0xf0});
        performSizedRightExtendedLongTest(0xff10101010101000L, new byte[]{0x10, 0x10, 0x10, 0x10, 0x10, 0x10, (byte)0xff});
        performSizedRightExtendedLongTest(0xff00000000000000L, new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, (byte)0xff});
        performSizedRightExtendedLongTest(0x7fffffffffffff00L, new byte[]{(byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0x7f});

        performSizedRightExtendedLongTest(0x01L, new byte[]{0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00});
        performSizedRightExtendedLongTest(0x0110101010101010L, new byte[]{0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x01});
        performSizedRightExtendedLongTest(0x7f10101010101010L, new byte[]{0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x7f});
        performSizedRightExtendedLongTest(0x8010101010101010L, new byte[]{0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, (byte)0x80});
        performSizedRightExtendedLongTest(0xf010101010101010L, new byte[]{0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, (byte)0xf0});
        performSizedRightExtendedLongTest(0xff10101010101010L, new byte[]{0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, (byte)0xff});
        performSizedRightExtendedLongTest(0xff00000000000000L, new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, (byte)0xff});
        performSizedRightExtendedLongTest(Long.MAX_VALUE, new byte[]{(byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0x7f});
        performSizedRightExtendedLongTest(Long.MIN_VALUE, new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, (byte)0x80});
        performSizedRightExtendedLongTest(-1, new byte[]{(byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff});
    }

    private void performSizedRightExtendedLongTest(long expectedValue, byte[] buf) {
        BaseDexBuffer dexBuf = new BaseDexBuffer(buf);
        BaseDexReader reader = dexBuf.readerAt(0);
        Assert.assertEquals(expectedValue, reader.readSizedRightExtendedLong(buf.length));
    }

    @Test
    public void testSizedRightExtendedLongFailure() {
        // wrong size
        performSizedRightExtendedLongFailureTest(new byte[]{});
        performSizedRightExtendedLongFailureTest(new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00});
        performSizedRightExtendedLongFailureTest(new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00});
        performSizedRightExtendedLongFailureTest(new byte[]{0x12, 0x34, 0x56, 0x12, 0x34, 0x56, 0x78, (byte)0x89, (byte)0x90, 0x01});
    }

    private void performSizedRightExtendedLongFailureTest(byte[] buf) {
        BaseDexBuffer dexBuf = new BaseDexBuffer(buf);
        BaseDexReader reader = dexBuf.readerAt(0);
        try {
            reader.readSizedRightExtendedLong(buf.length);
            Assert.fail();
        } catch (ExceptionWithContext ex) {
            // expected
        }
    }

    @Test
    public void testSizedLong() {
        performSizedLongTest(0, new byte[]{0x00});
        performSizedLongTest(0, new byte[]{0x00, 0x00});
        performSizedLongTest(0, new byte[]{0x00, 0x00, 0x00});
        performSizedLongTest(0, new byte[]{0x00, 0x00, 0x00, 0x00});
        performSizedLongTest(0, new byte[]{0x00, 0x00, 0x00, 0x00, 0x00});
        performSizedLongTest(0, new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00});
        performSizedLongTest(0, new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00});
        performSizedLongTest(0, new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00});

        performSizedLongTest(0x01L, new byte[]{0x01});
        performSizedLongTest(0x7fL, new byte[]{0x7f});
        performSizedLongTest(0xffffffffffffff80L, new byte[]{(byte)0x80});
        performSizedLongTest(0xfffffffffffffff0L, new byte[]{(byte)0xf0});
        performSizedLongTest(0xffffffffffffffffL, new byte[]{(byte)0xff});

        performSizedLongTest(0x01L, new byte[]{0x01, 0x00});
        performSizedLongTest(0x0110L, new byte[]{0x10, 0x01});
        performSizedLongTest(0x7f10L, new byte[]{0x10, 0x7f});
        performSizedLongTest(0xffffffffffff8010L, new byte[]{0x10, (byte)0x80});
        performSizedLongTest(0xfffffffffffff010L, new byte[]{0x10, (byte)0xf0});
        performSizedLongTest(0xffffffffffffff10L, new byte[]{0x10, (byte)0xff});
        performSizedLongTest(0xffffffffffffff00L, new byte[]{0x00, (byte)0xff});
        performSizedLongTest(0x7fffL, new byte[]{(byte)0xff, (byte)0x7f});

        performSizedLongTest(0x01L, new byte[]{0x01, 0x00, 0x00});
        performSizedLongTest(0x011010L, new byte[]{0x10, 0x10, 0x01});
        performSizedLongTest(0x7f1010L, new byte[]{0x10, 0x10, 0x7f});
        performSizedLongTest(0xffffffffff801010L, new byte[]{0x10, 0x10, (byte)0x80});
        performSizedLongTest(0xfffffffffff01010L, new byte[]{0x10, 0x10, (byte)0xf0});
        performSizedLongTest(0xffffffffffff1010L, new byte[]{0x10, 0x10, (byte)0xff});
        performSizedLongTest(0xffffffffffff0000L, new byte[]{0x00, 0x00, (byte)0xff});
        performSizedLongTest(0x7fffffL, new byte[]{(byte)0xff, (byte)0xff, (byte)0x7f});

        performSizedLongTest(0x01L, new byte[]{0x01, 0x00, 0x00, 0x00});
        performSizedLongTest(0x01101010L, new byte[]{0x10, 0x10, 0x10, 0x01});
        performSizedLongTest(0x7f101010L, new byte[]{0x10, 0x10, 0x10, 0x7f});
        performSizedLongTest(0xffffffff80101010L, new byte[]{0x10, 0x10, 0x10, (byte)0x80});
        performSizedLongTest(0xfffffffff0101010l, new byte[]{0x10, 0x10, 0x10, (byte)0xf0});
        performSizedLongTest(0xffffffffff101010L, new byte[]{0x10, 0x10, 0x10, (byte)0xff});
        performSizedLongTest(0xffffffffff000000L, new byte[]{0x00, 0x00, 0x00, (byte)0xff});
        performSizedLongTest(0x7fffffffL, new byte[]{(byte)0xff, (byte)0xff, (byte)0xff, (byte)0x7f});

        performSizedLongTest(0x01, new byte[]{0x01, 0x00, 0x00, 0x00, 0x00});
        performSizedLongTest(0x0110101010L, new byte[]{0x10, 0x10, 0x10, 0x10, 0x01});
        performSizedLongTest(0x7f10101010L, new byte[]{0x10, 0x10, 0x10, 0x10, 0x7f});
        performSizedLongTest(0xffffff8010101010L, new byte[]{0x10, 0x10, 0x10, 0x10, (byte)0x80});
        performSizedLongTest(0xfffffff010101010L, new byte[]{0x10, 0x10, 0x10, 0x10, (byte)0xf0});
        performSizedLongTest(0xffffffff10101010L, new byte[]{0x10, 0x10, 0x10, 0x10, (byte)0xff});
        performSizedLongTest(0xffffffff00000000L, new byte[]{0x00, 0x00, 0x00, 0x00, (byte)0xff});
        performSizedLongTest(0x7fffffffffL, new byte[]{(byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0x7f});

        performSizedLongTest(0x01L, new byte[]{0x01, 0x00, 0x00, 0x00, 0x00, 0x00});
        performSizedLongTest(0x011010101010L, new byte[]{0x10, 0x10, 0x10, 0x10, 0x10, 0x01});
        performSizedLongTest(0x7f1010101010L, new byte[]{0x10, 0x10, 0x10, 0x10, 0x10, 0x7f});
        performSizedLongTest(0xffff801010101010L, new byte[]{0x10, 0x10, 0x10, 0x10, 0x10, (byte)0x80});
        performSizedLongTest(0xfffff01010101010L, new byte[]{0x10, 0x10, 0x10, 0x10, 0x10, (byte)0xf0});
        performSizedLongTest(0xffffff1010101010L, new byte[]{0x10, 0x10, 0x10, 0x10, 0x10, (byte)0xff});
        performSizedLongTest(0xffffff0000000000L, new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, (byte)0xff});
        performSizedLongTest(0x7fffffffffffL, new byte[]{(byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0x7f});

        performSizedLongTest(0x01L, new byte[]{0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00});
        performSizedLongTest(0x01101010101010L, new byte[]{0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x01});
        performSizedLongTest(0x7f101010101010L, new byte[]{0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x7f});
        performSizedLongTest(0xff80101010101010L, new byte[]{0x10, 0x10, 0x10, 0x10, 0x10, 0x10, (byte)0x80});
        performSizedLongTest(0xfff0101010101010L, new byte[]{0x10, 0x10, 0x10, 0x10, 0x10, 0x10, (byte)0xf0});
        performSizedLongTest(0xffff101010101010L, new byte[]{0x10, 0x10, 0x10, 0x10, 0x10, 0x10, (byte)0xff});
        performSizedLongTest(0xffff000000000000L, new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, (byte)0xff});
        performSizedLongTest(0x7fffffffffffffL, new byte[]{(byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0x7f});

        performSizedLongTest(0x01L, new byte[]{0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00});
        performSizedLongTest(0x0110101010101010L, new byte[]{0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x01});
        performSizedLongTest(0x7f10101010101010L, new byte[]{0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x7f});
        performSizedLongTest(0x8010101010101010L, new byte[]{0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, (byte)0x80});
        performSizedLongTest(0xf010101010101010L, new byte[]{0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, (byte)0xf0});
        performSizedLongTest(0xff10101010101010L, new byte[]{0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, (byte)0xff});
        performSizedLongTest(0xff00000000000000L, new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, (byte)0xff});
        performSizedLongTest(Long.MAX_VALUE, new byte[]{(byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0x7f});
        performSizedLongTest(Long.MIN_VALUE, new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, (byte)0x80});
        performSizedLongTest(-1, new byte[]{(byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff});
    }

    private void performSizedLongTest(long expectedValue, byte[] buf) {
        BaseDexBuffer dexBuf = new BaseDexBuffer(buf);
        BaseDexReader reader = dexBuf.readerAt(0);
        Assert.assertEquals(expectedValue, reader.readSizedLong(buf.length));
    }

    @Test
    public void testSizedLongFailure() {
        // wrong size
        performSizedLongFailureTest(new byte[]{});
        performSizedLongFailureTest(new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00});
        performSizedLongFailureTest(new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00});
        performSizedLongFailureTest(new byte[]{0x12, 0x34, 0x56, 0x12, 0x34, 0x56, 0x78, (byte)0x89, (byte)0x90, 0x01});
    }

    private void performSizedLongFailureTest(byte[] buf) {
        BaseDexBuffer dexBuf = new BaseDexBuffer(buf);
        BaseDexReader reader = dexBuf.readerAt(0);
        try {
            reader.readSizedLong(buf.length);
            Assert.fail();
        } catch (ExceptionWithContext ex) {
            // expected
        }
    }
}
