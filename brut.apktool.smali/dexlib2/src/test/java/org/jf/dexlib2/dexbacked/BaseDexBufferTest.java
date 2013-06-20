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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Random;

public class BaseDexBufferTest {
    @Test
    public void testReadSmallUintSuccess() {
        BaseDexBuffer dexBuf = new BaseDexBuffer(new byte[] {0x11, 0x22, 0x33, 0x44});
        Assert.assertEquals(0x44332211, dexBuf.readSmallUint(0));

        dexBuf = new BaseDexBuffer(new byte[] {0x00, 0x00, 0x00, 0x00});
        Assert.assertEquals(0, dexBuf.readSmallUint(0));

        dexBuf = new BaseDexBuffer(new byte[] {(byte)0xff, (byte)0xff, (byte)0xff, 0x7f});
        Assert.assertEquals(0x7fffffff, dexBuf.readSmallUint(0));
    }

    @Test(expected=ExceptionWithContext.class)
    public void testReadSmallUintTooLarge1() {
        BaseDexBuffer dexBuf = new BaseDexBuffer(new byte[] {0x00, 0x00, 0x00, (byte)0x80});
        dexBuf.readSmallUint(0);
    }

    @Test(expected=ExceptionWithContext.class)
    public void testReadSmallUintTooLarge2() {
        BaseDexBuffer dexBuf = new BaseDexBuffer(new byte[] {(byte)0xff, (byte)0xff, (byte)0xff, (byte)0x80});
        dexBuf.readSmallUint(0);
    }

    @Test(expected=ExceptionWithContext.class)
    public void testReadSmallUintTooLarge3() {
        BaseDexBuffer dexBuf = new BaseDexBuffer(new byte[] {(byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff});
        dexBuf.readSmallUint(0);
    }

    @Test
    public void testReadOptionalUintSuccess() {
        BaseDexBuffer dexBuf = new BaseDexBuffer(new byte[] {0x11, 0x22, 0x33, 0x44});
        Assert.assertEquals(0x44332211, dexBuf.readSmallUint(0));

        dexBuf = new BaseDexBuffer(new byte[] {0x00, 0x00, 0x00, 0x00});
        Assert.assertEquals(0, dexBuf.readSmallUint(0));

        dexBuf = new BaseDexBuffer(new byte[] {(byte)0xff, (byte)0xff, (byte)0xff, 0x7f});
        Assert.assertEquals(0x7fffffff, dexBuf.readSmallUint(0));

        dexBuf = new BaseDexBuffer(new byte[] {(byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff});
        Assert.assertEquals(-1, dexBuf.readOptionalUint(0));
    }

    @Test(expected=ExceptionWithContext.class)
    public void testReadOptionalUintTooLarge1() {
        BaseDexBuffer dexBuf = new BaseDexBuffer(new byte[] {0x00, 0x00, 0x00, (byte)0x80});
        dexBuf.readSmallUint(0);
    }

    @Test(expected=ExceptionWithContext.class)
    public void testReadOptionalUintTooLarge2() {
        BaseDexBuffer dexBuf = new BaseDexBuffer(new byte[] {(byte)0xff, (byte)0xff, (byte)0xff, (byte)0x80});
        dexBuf.readSmallUint(0);
    }

    @Test(expected=ExceptionWithContext.class)
    public void testReadOptionalUintTooLarge3() {
        BaseDexBuffer dexBuf = new BaseDexBuffer(new byte[] {(byte)0xfe, (byte)0xff, (byte)0xff, (byte)0xff});
        dexBuf.readSmallUint(0);
    }

    @Test
    public void testReadUshort() {
        BaseDexBuffer dexBuf = new BaseDexBuffer(new byte[] {0x11, 0x22});
        Assert.assertEquals(dexBuf.readUshort(0), 0x2211);

        dexBuf = new BaseDexBuffer(new byte[] {0x00, 0x00});
        Assert.assertEquals(dexBuf.readUshort(0), 0);

        dexBuf = new BaseDexBuffer(new byte[] {(byte)0xff, (byte)0xff});
        Assert.assertEquals(dexBuf.readUshort(0), 0xffff);

        dexBuf = new BaseDexBuffer(new byte[] {(byte)0x00, (byte)0x80});
        Assert.assertEquals(dexBuf.readUshort(0), 0x8000);

        dexBuf = new BaseDexBuffer(new byte[] {(byte)0xff, (byte)0x7f});
        Assert.assertEquals(dexBuf.readUshort(0), 0x7fff);
    }

    @Test
    public void testReadUbyte() {
        byte[] buf = new byte[1];
        BaseDexBuffer dexBuf = new BaseDexBuffer(buf);

        for (int i=0; i<=0xff; i++) {
            buf[0] = (byte)i;
            Assert.assertEquals(i, dexBuf.readUbyte(0));
        }
    }

    @Test
    public void testReadLong() {
        BaseDexBuffer dexBuf = new BaseDexBuffer(new byte[] {0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77});
        Assert.assertEquals(0x7766554433221100L, dexBuf.readLong(0));

        dexBuf = new BaseDexBuffer(new byte[] {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00});
        Assert.assertEquals(0, dexBuf.readLong(0));

        dexBuf = new BaseDexBuffer(new byte[] {(byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff,
                (byte)0xff, (byte)0xff, (byte)0xff, 0x7f});
        Assert.assertEquals(Long.MAX_VALUE, dexBuf.readLong(0));

        dexBuf = new BaseDexBuffer(new byte[] {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, (byte)0x80});
        Assert.assertEquals(Long.MIN_VALUE, dexBuf.readLong(0));

        dexBuf = new BaseDexBuffer(new byte[] {(byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff,
                (byte)0xff, (byte)0xff, (byte)0xff, (byte)0x80});
        Assert.assertEquals(0x80ffffffffffffffL, dexBuf.readLong(0));

        dexBuf = new BaseDexBuffer(new byte[] {(byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff,
                (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff});
        Assert.assertEquals(-1, dexBuf.readLong(0));

    }

    @Test
    public void testReadInt() {
        BaseDexBuffer dexBuf = new BaseDexBuffer(new byte[] {0x11, 0x22, 0x33, 0x44});
        Assert.assertEquals(0x44332211, dexBuf.readInt(0));

        dexBuf = new BaseDexBuffer(new byte[] {0x00, 0x00, 0x00, 0x00});
        Assert.assertEquals(0, dexBuf.readInt(0));

        dexBuf = new BaseDexBuffer(new byte[] {(byte)0xff, (byte)0xff, (byte)0xff, 0x7f});
        Assert.assertEquals(Integer.MAX_VALUE, dexBuf.readInt(0));

        dexBuf = new BaseDexBuffer(new byte[] {0x00, 0x00, 0x00, (byte)0x80});
        Assert.assertEquals(Integer.MIN_VALUE, dexBuf.readInt(0));

        dexBuf = new BaseDexBuffer(new byte[] {(byte)0xff, (byte)0xff, (byte)0xff, (byte)0x80});
        Assert.assertEquals(0x80ffffff, dexBuf.readInt(0));

        dexBuf = new BaseDexBuffer(new byte[] {(byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff});
        Assert.assertEquals(-1, dexBuf.readInt(0));
    }

    @Test
    public void testReadShort() {
        BaseDexBuffer dexBuf = new BaseDexBuffer(new byte[] {0x11, 0x22});
        Assert.assertEquals(dexBuf.readShort(0), 0x2211);

        dexBuf = new BaseDexBuffer(new byte[] {0x00, 0x00});
        Assert.assertEquals(dexBuf.readShort(0), 0);

        dexBuf = new BaseDexBuffer(new byte[] {(byte)0xff, (byte)0xff});
        Assert.assertEquals(dexBuf.readShort(0), -1);

        dexBuf = new BaseDexBuffer(new byte[] {(byte)0x00, (byte)0x80});
        Assert.assertEquals(dexBuf.readShort(0), Short.MIN_VALUE);

        dexBuf = new BaseDexBuffer(new byte[] {(byte)0xff, (byte)0x7f});
        Assert.assertEquals(dexBuf.readShort(0), 0x7fff);

        dexBuf = new BaseDexBuffer(new byte[] {(byte)0xff, (byte)0x80});
        Assert.assertEquals(dexBuf.readShort(0), 0xffff80ff);
    }

    @Test
    public void testReadByte() {
        byte[] buf = new byte[1];
        BaseDexBuffer dexBuf = new BaseDexBuffer(buf);

        for (int i=0; i<=0xff; i++) {
            buf[0] = (byte)i;
            Assert.assertEquals((byte)i, dexBuf.readByte(0));
        }
    }

    @Test
    public void testReadRandom() {
        Random r = new Random(1234567890);
        ByteBuffer byteBuf = ByteBuffer.allocateDirect(4).order(ByteOrder.LITTLE_ENDIAN);
        byte[] buf = new byte[4];
        BaseDexBuffer dexBuf = new BaseDexBuffer(buf);

        for (int i=0; i<10000; i++) {
            int val = r.nextInt();
            byteBuf.putInt(0, val);
            byteBuf.position(0);
            byteBuf.get(buf);

            boolean expectException = val < 0;
            try {
                int returnedVal = dexBuf.readSmallUint(0);
                Assert.assertFalse(String.format("Didn't throw an exception for value: %x", val), expectException);
                Assert.assertEquals(val, returnedVal);
            } catch (Exception ex) {
                Assert.assertTrue(String.format("Threw an exception for value: %x", val), expectException);
            }

            Assert.assertEquals(val, dexBuf.readInt(0));

            Assert.assertEquals(val & 0xFFFF, dexBuf.readUshort(0));
            Assert.assertEquals((val >> 8) & 0xFFFF, dexBuf.readUshort(1));
            Assert.assertEquals((val >> 16) & 0xFFFF, dexBuf.readUshort(2));

            Assert.assertEquals((short)val, dexBuf.readShort(0));
            Assert.assertEquals((short)(val >> 8), dexBuf.readShort(1));
            Assert.assertEquals((short)(val >> 16), dexBuf.readShort(2));
        }
    }

    @Test
    public void testReadLongRandom() {
        Random r = new Random(1234567890);
        ByteBuffer byteBuf = ByteBuffer.allocateDirect(8).order(ByteOrder.LITTLE_ENDIAN);
        byte[] buf = new byte[8];
        BaseDexBuffer dexBuf = new BaseDexBuffer(buf);

        for (int i=0; i<10000; i++) {
            int val = r.nextInt();
            byteBuf.putLong(0, val);
            byteBuf.position(0);
            byteBuf.get(buf);

            Assert.assertEquals(val, dexBuf.readLong(0));
        }
    }
}
