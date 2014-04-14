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

package org.jf.dexlib2.writer;

import junit.framework.Assert;
import org.jf.dexlib2.ValueType;
import org.jf.util.ExceptionWithContext;
import org.jf.util.NakedByteArrayOutputStream;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

public class DexDataWriterTest {
    private Random random;
    private NakedByteArrayOutputStream output = new NakedByteArrayOutputStream();
    private int startPosition;
    private DexDataWriter writer;

    @Before
    public void setup() throws IOException {
        // use a predefined seed, so we get a deterministic result
        random = new Random();
        output.reset();
        startPosition = 123;
        int bufferSize = 256;
        writer = new DexDataWriter(output, startPosition, bufferSize);
    }

    // Note: we use int[] rather than byte[] so that we don't have to cast every value when manually constructing an
    // array.
    private void expectData(int... bytes) throws IOException {
        Assert.assertEquals(startPosition+bytes.length, writer.getPosition());

        writer.flush();
        byte[] writtenData = output.getBuffer();

        for (int i=0; i<bytes.length; i++) {
            Assert.assertEquals(String.format("Values not equal at index %d", i), (byte)bytes[i], writtenData[i]);
        }
    }

    private void expectData(byte[] bytes) throws IOException {
        Assert.assertEquals(startPosition+bytes.length, writer.getPosition());

        writer.flush();
        byte[] writtenData = output.getBuffer();

        for (int i=0; i<bytes.length; i++) {
            Assert.assertEquals(String.format("Values not equal at index %d", i), bytes[i], writtenData[i]);
        }
    }

    @Test
    public void testWriteByte() throws IOException {
        byte[] arr = new byte[257];
        for (int i=0; i<256; i++) {
            arr[i] = (byte)i;
            writer.write(i);
        }
        arr[256] = (byte)0x80;
        writer.write(0x180);

        expectData(arr);
    }

    @Test
    public void testWriteByteArray() throws IOException {
        byte[] arr = new byte[345];
        random.nextBytes(arr);
        writer.write(arr);

        expectData(arr);
    }

    @Test
    public void testWriteByteArrayWithLengthAndOffset() throws IOException {
        byte[] arr = new byte[345];
        random.nextBytes(arr);
        writer.write(arr, 10, 300);

        expectData(Arrays.copyOfRange(arr, 10, 310));
    }

    @Test
    public void testWriteLong() throws IOException {
        writer.writeLong(0x1122334455667788L);
        writer.writeLong(-0x1122334455667788L);

        expectData(0x88, 0x77, 0x66, 0x55, 0x44, 0x33, 0x22, 0x11,
                   0x78, 0x88, 0x99, 0xAA, 0xBB, 0xCC, 0xDD, 0xEE);
    }

    @Test
    public void testWriteInt() throws IOException {
        writer.writeInt(0x11223344);
        writer.writeInt(-0x11223344);

        expectData(0x44, 0x33, 0x22, 0x11,
                   0xBC, 0xCC, 0xDD, 0xEE);
    }

    @Test
    public void testWriteShort() throws IOException {
        writer.writeShort(0);
        writer.writeShort(0x1122);
        writer.writeShort(-0x1122);
        writer.writeShort(0x7FFF);
        writer.writeShort(-0x8000);

        expectData(0x00, 0x00,
                   0x22, 0x11,
                   0xDE, 0xEE,
                   0xFF, 0x7F,
                   0x00, 0x80);
    }

    @Test(expected=ExceptionWithContext.class)
    public void testWriteShortOutOfBounds() throws IOException {
        writer.writeShort(0x8000);
    }

    @Test(expected=ExceptionWithContext.class)
    public void testWriteShortOutOfBounds2() throws IOException {
        writer.writeShort(-0x8001);
    }

    @Test
    public void testWriteUshort() throws IOException {
        writer.writeUshort(0);
        writer.writeUshort(0x1122);
        writer.writeUshort(0x8899);
        writer.writeUshort(0xFFFF);

        expectData(0x00, 0x00,
                   0x22, 0x11,
                   0x99, 0x88,
                   0xFF, 0xFF);
    }

    @Test(expected=ExceptionWithContext.class)
    public void testWriteUshortOutOfBounds() throws IOException {
        writer.writeUshort(-1);
    }

    @Test(expected=ExceptionWithContext.class)
    public void testWriteUshortOutOfBounds2() throws IOException {
        writer.writeUshort(0x10000);
    }

    @Test
    public void testWriteUbyte() throws IOException {
        writer.writeUbyte(0);
        writer.writeUbyte(1);
        writer.writeUbyte(0x12);
        writer.writeUbyte(0xFF);

        expectData(0x00, 0x01, 0x12, 0xFF);
    }

    @Test(expected=ExceptionWithContext.class)
    public void testWriteUbyteOutOfBounds() throws IOException {
        writer.writeUbyte(-1);
    }

    @Test(expected=ExceptionWithContext.class)
    public void testWriteUbyteOutOfBounds2() throws IOException {
        writer.writeUbyte(256);
    }

    @Test
    public void testWriteEncodedValueHeader() throws IOException {
        writer.writeEncodedValueHeader(0x2, 0x1);

        expectData(0x22);
    }

    private void testWriteEncodedIntHelper(int integerValue, int... encodedValue) throws IOException {
        setup();
        writer.writeEncodedInt(ValueType.INT, integerValue);

        int[] arr = new int[encodedValue.length+1];
        arr[0] = ValueType.INT | ((encodedValue.length-1) << 5);
        System.arraycopy(encodedValue, 0, arr, 1, encodedValue.length);
        expectData(arr);
    }

    @Test
    public void testWriteEncodedInt() throws IOException {
        testWriteEncodedIntHelper(0x00, 0x00);
        testWriteEncodedIntHelper(0x40, 0x40);
        testWriteEncodedIntHelper(0x7f, 0x7f);
        testWriteEncodedIntHelper(0xff, 0xff, 0x00);
        testWriteEncodedIntHelper(0xffff80, 0x80, 0xff, 0xff, 0x00);
        testWriteEncodedIntHelper(0xffffff80, 0x80);
        testWriteEncodedIntHelper(0xffffffff, 0xff);
        testWriteEncodedIntHelper(0x100, 0x00, 0x01);
        testWriteEncodedIntHelper(0x7fff, 0xff, 0x7f);
        testWriteEncodedIntHelper(0x8000, 0x00, 0x80, 0x00);
        testWriteEncodedIntHelper(0xffff8000, 0x00, 0x80);
        testWriteEncodedIntHelper(0x10000, 0x00, 0x00, 0x01);
        testWriteEncodedIntHelper(0x10203, 0x03, 0x02, 0x01);
        testWriteEncodedIntHelper(0x810203, 0x03, 0x02, 0x81, 0x00);
        testWriteEncodedIntHelper(0xff810203, 0x03, 0x02, 0x81);
        testWriteEncodedIntHelper(0x1000000, 0x00, 0x00, 0x00, 0x01);
        testWriteEncodedIntHelper(0x1020304, 0x04, 0x03, 0x02, 0x01);
        testWriteEncodedIntHelper(0x7fffffff, 0xff, 0xff, 0xff, 0x7f);
        testWriteEncodedIntHelper(0x80000000, 0x00, 0x00, 0x00, 0x80);
        testWriteEncodedIntHelper(0x80000001, 0x01, 0x00, 0x00, 0x80);
    }

    private void testWriteEncodedUintHelper(int integerValue, int... encodedValue) throws IOException {
        setup();
        writer.writeEncodedUint(ValueType.METHOD, integerValue);

        int[] arr = new int[encodedValue.length+1];
        arr[0] = ValueType.METHOD | ((encodedValue.length-1) << 5);
        System.arraycopy(encodedValue, 0, arr, 1, encodedValue.length);
        expectData(arr);
    }

    @Test
    public void testWriteEncodedUint() throws IOException {
        testWriteEncodedUintHelper(0x00, 0x00);
        testWriteEncodedUintHelper(0x01, 0x01);
        testWriteEncodedUintHelper(0x40, 0x40);
        testWriteEncodedUintHelper(0x7f, 0x7f);
        testWriteEncodedUintHelper(0x80, 0x80);
        testWriteEncodedUintHelper(0x81, 0x81);
        testWriteEncodedUintHelper(0xff, 0xff);
        testWriteEncodedUintHelper(0x100, 0x00, 0x01);
        testWriteEncodedUintHelper(0x180, 0x80, 0x01);
        testWriteEncodedUintHelper(0x8080, 0x80, 0x80);
        testWriteEncodedUintHelper(0x1234, 0x34, 0x12);
        testWriteEncodedUintHelper(0x1000, 0x00, 0x10);
        testWriteEncodedUintHelper(0x8000, 0x00, 0x80);
        testWriteEncodedUintHelper(0xff00, 0x00, 0xff);
        testWriteEncodedUintHelper(0xffff, 0xff, 0xff);
        testWriteEncodedUintHelper(0x10000, 0x00, 0x00, 0x01);
        testWriteEncodedUintHelper(0x1ffff, 0xff, 0xff, 0x01);
        testWriteEncodedUintHelper(0x80ffff, 0xff, 0xff, 0x80);
        testWriteEncodedUintHelper(0xffffff, 0xff, 0xff, 0xff);
        testWriteEncodedUintHelper(0x1000000, 0x00, 0x00, 0x00, 0x01);
        testWriteEncodedUintHelper(0x1020304, 0x04, 0x03, 0x02, 0x01);
        testWriteEncodedUintHelper(0x80000000, 0x00, 0x00, 0x00, 0x80);
        testWriteEncodedUintHelper(0x80ffffff, 0xff, 0xff, 0xff, 0x80);
        testWriteEncodedUintHelper(0xffffffff, 0xff, 0xff, 0xff, 0xff);
    }

    private void testWriteEncodedLongHelper(long longValue, int... encodedValue) throws IOException {
        setup();
        writer.writeEncodedLong(ValueType.LONG, longValue);

        int[] arr = new int[encodedValue.length+1];
        arr[0] = ValueType.LONG | ((encodedValue.length-1) << 5);
        System.arraycopy(encodedValue, 0, arr, 1, encodedValue.length);
        expectData(arr);
    }

    @Test
    public void testWriteEncodedLong() throws IOException {
        testWriteEncodedLongHelper(0x00L, 0x00);
        testWriteEncodedLongHelper(0x40L, 0x40);
        testWriteEncodedLongHelper(0x7fL, 0x7f);
        testWriteEncodedLongHelper(0xffL, 0xff, 0x00);
        testWriteEncodedLongHelper(0xffffffffffffff80L, 0x80);
        testWriteEncodedLongHelper(0xffffffffffffffffL, 0xff);

        testWriteEncodedLongHelper(0x100L, 0x00, 0x01);
        testWriteEncodedLongHelper(0x7fffL, 0xff, 0x7f);
        testWriteEncodedLongHelper(0x8000L, 0x00, 0x80, 0x00);
        testWriteEncodedLongHelper(0xffffffffffff8000L, 0x00, 0x80);

        testWriteEncodedLongHelper(0x10000L, 0x00, 0x00, 0x01);
        testWriteEncodedLongHelper(0x10203L, 0x03, 0x02, 0x01);
        testWriteEncodedLongHelper(0x810203L, 0x03, 0x02, 0x81, 0x00);
        testWriteEncodedLongHelper(0xffffffffff810203L, 0x03, 0x02, 0x81);

        testWriteEncodedLongHelper(0x1000000L, 0x00, 0x00, 0x00, 0x01);
        testWriteEncodedLongHelper(0x1020304L, 0x04, 0x03, 0x02, 0x01);
        testWriteEncodedLongHelper(0x7fffffffL, 0xff, 0xff, 0xff, 0x7f);
        testWriteEncodedLongHelper(0x80000000L, 0x00, 0x00, 0x00, 0x80, 0x00);
        testWriteEncodedLongHelper(0xffffffff80000000L, 0x00, 0x00, 0x00, 0x80);
        testWriteEncodedLongHelper(0xffffffff80000001L, 0x01, 0x00, 0x00, 0x80);

        testWriteEncodedLongHelper(0x100000000L, 0x00, 0x00, 0x00, 0x00, 0x01);
        testWriteEncodedLongHelper(0x102030405L, 0x05, 0x04, 0x03, 0x02, 0x01);
        testWriteEncodedLongHelper(0x7fffffffffL, 0xff, 0xff, 0xff, 0xff, 0x7f);
        testWriteEncodedLongHelper(0x8000000000L, 0x00, 0x00, 0x00, 0x00, 0x80, 0x00);
        testWriteEncodedLongHelper(0xffffff8000000000L, 0x00, 0x00, 0x00, 0x00, 0x80);
        testWriteEncodedLongHelper(0xffffff8000000001L, 0x01, 0x00, 0x00, 0x00, 0x80);

        testWriteEncodedLongHelper(0x10000000000L, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01);
        testWriteEncodedLongHelper(0x10203040506L, 0x06, 0x05, 0x04, 0x03, 0x02, 0x01);
        testWriteEncodedLongHelper(0x7fffffffffffL, 0xff, 0xff, 0xff, 0xff, 0xff, 0x7f);
        testWriteEncodedLongHelper(0x800000000000L, 0x00, 0x00, 0x00, 0x00, 0x00, 0x80, 0x00);
        testWriteEncodedLongHelper(0xffff800000000000L, 0x00, 0x00, 0x00, 0x00, 0x00, 0x80);
        testWriteEncodedLongHelper(0xffff800000000001L, 0x01, 0x00, 0x00, 0x00, 0x00, 0x80);

        testWriteEncodedLongHelper(0x1000000000000L, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01);
        testWriteEncodedLongHelper(0x1020304050607L, 0x07, 0x06, 0x05, 0x04, 0x03, 0x02, 0x01);
        testWriteEncodedLongHelper(0x7fffffffffffffL, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0x7f);
        testWriteEncodedLongHelper(0x80000000000000L, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x80, 0x00);
        testWriteEncodedLongHelper(0xff80000000000000L, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x80);
        testWriteEncodedLongHelper(0xff80000000000001L, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x80);

        testWriteEncodedLongHelper(0x100000000000000L, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01);
        testWriteEncodedLongHelper(0x102030405060708L, 0x08, 0x07, 0x06, 0x05, 0x04, 0x03, 0x02, 0x01);
        testWriteEncodedLongHelper(0x7fffffffffffffffL, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0x7f);
        testWriteEncodedLongHelper(0x8000000000000000L, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x80);
        testWriteEncodedLongHelper(0x8000000000000001L, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x80);
        testWriteEncodedLongHelper(0xfeffffffffffffffL, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xfe);

        testWriteEncodedLongHelper(0x123456789ABCDEF0L, 0xF0, 0xDE, 0xBC, 0x9A, 0x78, 0x56, 0x34, 0x12);
    }

    private void testWriteRightZeroExtendedIntHelper(int intValue, int... encodedValue) throws IOException {
        setup();
        writer.writeRightZeroExtendedInt(ValueType.FLOAT, intValue);

        int[] arr = new int[encodedValue.length+1];
        arr[0] = ValueType.FLOAT | ((encodedValue.length-1) << 5);
        System.arraycopy(encodedValue, 0, arr, 1, encodedValue.length);
        expectData(arr);
    }

    @Test
    public void testWriteRightZeroExtendedInt() throws IOException {
        testWriteRightZeroExtendedIntHelper(0, 0x00);

        testWriteRightZeroExtendedIntHelper(0x01000000, 0x01);
        testWriteRightZeroExtendedIntHelper(0x7f000000, 0x7f);
        testWriteRightZeroExtendedIntHelper(0x80000000, 0x80);
        testWriteRightZeroExtendedIntHelper(0xf0000000, 0xf0);
        testWriteRightZeroExtendedIntHelper(0xff000000, 0xff);

        testWriteRightZeroExtendedIntHelper(0x010000, 0x01, 0x00);
        testWriteRightZeroExtendedIntHelper(0x01100000, 0x10, 0x01);
        testWriteRightZeroExtendedIntHelper(0x7f100000, 0x10, 0x7f);
        testWriteRightZeroExtendedIntHelper(0x80100000, 0x10, 0x80);
        testWriteRightZeroExtendedIntHelper(0xf0100000, 0x10, 0xf0);
        testWriteRightZeroExtendedIntHelper(0xff100000, 0x10, 0xff);
        testWriteRightZeroExtendedIntHelper(0xff000000, 0xff);

        testWriteRightZeroExtendedIntHelper(0x0100, 0x01, 0x00, 0x00);
        testWriteRightZeroExtendedIntHelper(0x01101000, 0x10, 0x10, 0x01);
        testWriteRightZeroExtendedIntHelper(0x7f101000, 0x10, 0x10, 0x7f);
        testWriteRightZeroExtendedIntHelper(0x80101000, 0x10, 0x10, 0x80);
        testWriteRightZeroExtendedIntHelper(0xf0101000, 0x10, 0x10, 0xf0);
        testWriteRightZeroExtendedIntHelper(0xff101000, 0x10, 0x10, 0xff);

        testWriteRightZeroExtendedIntHelper(0x01, 0x01, 0x00, 0x00, 0x00);
        testWriteRightZeroExtendedIntHelper(0x80, 0x80, 0x00, 0x00, 0x00);
        testWriteRightZeroExtendedIntHelper(0xff, 0xff, 0x00, 0x00, 0x00);
        testWriteRightZeroExtendedIntHelper(0x01101010, 0x10, 0x10, 0x10, 0x01);
        testWriteRightZeroExtendedIntHelper(0x7f101010, 0x10, 0x10, 0x10, 0x7f);
        testWriteRightZeroExtendedIntHelper(0x80101010, 0x10, 0x10, 0x10, 0x80);
        testWriteRightZeroExtendedIntHelper(0xf0101010, 0x10, 0x10, 0x10, 0xf0);
        testWriteRightZeroExtendedIntHelper(0xff101010, 0x10, 0x10, 0x10, 0xff);
    }

    private void testWriteRightZeroExtendedLongHelper(long longValue, int... encodedValue) throws IOException {
        setup();
        writer.writeRightZeroExtendedLong(ValueType.DOUBLE, longValue);

        int[] arr = new int[encodedValue.length+1];
        arr[0] = ValueType.DOUBLE | ((encodedValue.length-1) << 5);
        System.arraycopy(encodedValue, 0, arr, 1, encodedValue.length);
        expectData(arr);
    }

    @Test
    public void testWriteRightZeroExtendedLong() throws IOException {
        testWriteRightZeroExtendedLongHelper(0, 0x00);

        testWriteRightZeroExtendedLongHelper(0x0100000000000000L, 0x01);
        testWriteRightZeroExtendedLongHelper(0x7f00000000000000L, 0x7f);
        testWriteRightZeroExtendedLongHelper(0x8000000000000000L, 0x80);
        testWriteRightZeroExtendedLongHelper(0xf000000000000000L, 0xf0);
        testWriteRightZeroExtendedLongHelper(0xff00000000000000L, 0xff);

        testWriteRightZeroExtendedLongHelper(0x01000000000000L, 0x01, 0x00);
        testWriteRightZeroExtendedLongHelper(0x0110000000000000L, 0x10, 0x01);
        testWriteRightZeroExtendedLongHelper(0x7f10000000000000L, 0x10, 0x7f);
        testWriteRightZeroExtendedLongHelper(0x8010000000000000L, 0x10, 0x80);
        testWriteRightZeroExtendedLongHelper(0xf010000000000000L, 0x10, 0xf0);
        testWriteRightZeroExtendedLongHelper(0xff10000000000000L, 0x10, 0xff);
        testWriteRightZeroExtendedLongHelper(0x7fff000000000000L, 0xff, 0x7f);

        testWriteRightZeroExtendedLongHelper(0x010000000000L, 0x01, 0x00, 0x00);
        testWriteRightZeroExtendedLongHelper(0x0110100000000000L, 0x10, 0x10, 0x01);
        testWriteRightZeroExtendedLongHelper(0x7f10100000000000L, 0x10, 0x10, 0x7f);
        testWriteRightZeroExtendedLongHelper(0x8010100000000000L, 0x10, 0x10, 0x80);
        testWriteRightZeroExtendedLongHelper(0xf010100000000000L, 0x10, 0x10, 0xf0);
        testWriteRightZeroExtendedLongHelper(0xff10100000000000L, 0x10, 0x10, 0xff);
        testWriteRightZeroExtendedLongHelper(0x7fffff0000000000L, 0xff, 0xff, 0x7f);

        testWriteRightZeroExtendedLongHelper(0x0100000000L, 0x01, 0x00, 0x00, 0x00);
        testWriteRightZeroExtendedLongHelper(0x0110101000000000L, 0x10, 0x10, 0x10, 0x01);
        testWriteRightZeroExtendedLongHelper(0x7f10101000000000L, 0x10, 0x10, 0x10, 0x7f);
        testWriteRightZeroExtendedLongHelper(0x8010101000000000L, 0x10, 0x10, 0x10, 0x80);
        testWriteRightZeroExtendedLongHelper(0xf010101000000000L, 0x10, 0x10, 0x10, 0xf0);
        testWriteRightZeroExtendedLongHelper(0xff10101000000000L, 0x10, 0x10, 0x10, 0xff);
        testWriteRightZeroExtendedLongHelper(0x7fffffff00000000L, 0xff, 0xff, 0xff, 0x7f);

        testWriteRightZeroExtendedLongHelper(0x01000000L, 0x01, 0x00, 0x00, 0x00, 0x00);
        testWriteRightZeroExtendedLongHelper(0x0110101010000000L, 0x10, 0x10, 0x10, 0x10, 0x01);
        testWriteRightZeroExtendedLongHelper(0x7f10101010000000L, 0x10, 0x10, 0x10, 0x10, 0x7f);
        testWriteRightZeroExtendedLongHelper(0x8010101010000000L, 0x10, 0x10, 0x10, 0x10, 0x80);
        testWriteRightZeroExtendedLongHelper(0xf010101010000000L, 0x10, 0x10, 0x10, 0x10, 0xf0);
        testWriteRightZeroExtendedLongHelper(0xff10101010000000L, 0x10, 0x10, 0x10, 0x10, 0xff);
        testWriteRightZeroExtendedLongHelper(0x7fffffffff000000L, 0xff, 0xff, 0xff, 0xff, 0x7f);

        testWriteRightZeroExtendedLongHelper(0x010000L, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00);
        testWriteRightZeroExtendedLongHelper(0x0110101010100000L, 0x10, 0x10, 0x10, 0x10, 0x10, 0x01);
        testWriteRightZeroExtendedLongHelper(0x7f10101010100000L, 0x10, 0x10, 0x10, 0x10, 0x10, 0x7f);
        testWriteRightZeroExtendedLongHelper(0x8010101010100000L, 0x10, 0x10, 0x10, 0x10, 0x10, 0x80);
        testWriteRightZeroExtendedLongHelper(0xf010101010100000L, 0x10, 0x10, 0x10, 0x10, 0x10, 0xf0);
        testWriteRightZeroExtendedLongHelper(0xff10101010100000L, 0x10, 0x10, 0x10, 0x10, 0x10, 0xff);
        testWriteRightZeroExtendedLongHelper(0x7fffffffffff0000L, 0xff, 0xff, 0xff, 0xff, 0xff, 0x7f);

        testWriteRightZeroExtendedLongHelper(0x0100L, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00);
        testWriteRightZeroExtendedLongHelper(0x0110101010101000L, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x01);
        testWriteRightZeroExtendedLongHelper(0x7f10101010101000L, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x7f);
        testWriteRightZeroExtendedLongHelper(0x8010101010101000L, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x80);
        testWriteRightZeroExtendedLongHelper(0xf010101010101000L, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0xf0);
        testWriteRightZeroExtendedLongHelper(0xff10101010101000L, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0xff);
        testWriteRightZeroExtendedLongHelper(0x7fffffffffffff00L, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0x7f);

        testWriteRightZeroExtendedLongHelper(0x01L, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00);
        testWriteRightZeroExtendedLongHelper(0x0110101010101010L, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x01);
        testWriteRightZeroExtendedLongHelper(0x7f10101010101010L, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x7f);
        testWriteRightZeroExtendedLongHelper(0x8010101010101010L, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x80);
        testWriteRightZeroExtendedLongHelper(0xf010101010101010L, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0xf0);
        testWriteRightZeroExtendedLongHelper(0xff10101010101010L, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0xff);
        testWriteRightZeroExtendedLongHelper(Long.MAX_VALUE, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0x7f);
        testWriteRightZeroExtendedLongHelper(Long.MIN_VALUE, 0x80);
        testWriteRightZeroExtendedLongHelper(-1, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff, 0xff);
    }

    private void testWriteStringHelper(String stringValue, int... encodedValue) throws IOException {
        setup();

        writer.writeString(stringValue);

        expectData(encodedValue);
    }

    @Test
    public void testWriteString() throws IOException {
        testWriteStringHelper(new String(new char[]{0x00}), 0xc0, 0x80);
        testWriteStringHelper(new String(new char[]{0x01}), 0x01);
        testWriteStringHelper(new String(new char[]{0x40}), 0x40);
        testWriteStringHelper(new String(new char[]{0x7f}), 0x7f);
        testWriteStringHelper(new String(new char[]{0x80}), 0xc2, 0x80);
        testWriteStringHelper(new String(new char[]{0x81}), 0xc2, 0x81);
        testWriteStringHelper(new String(new char[]{0x100}), 0xc4, 0x80);
        testWriteStringHelper(new String(new char[]{0x7ff}), 0xdf, 0xbf);
        testWriteStringHelper(new String(new char[]{0x800}), 0xe0, 0xa0, 0x80);
        testWriteStringHelper(new String(new char[]{0x801}), 0xe0, 0xa0, 0x81);
        testWriteStringHelper(new String(new char[]{0x1000}), 0xe1, 0x80, 0x80);
        testWriteStringHelper(new String(new char[]{0x7fff}), 0xe7, 0xbf, 0xbf);
        testWriteStringHelper(new String(new char[]{0x8000}), 0xe8, 0x80, 0x80);
        testWriteStringHelper(new String(new char[]{0x8001}), 0xe8, 0x80, 0x81);
        testWriteStringHelper(new String(new char[]{0xffff}), 0xef, 0xbf, 0xbf);
    }

    @Test
    public void testAlign() throws IOException {
        // create a new writer so we can start at file position 0
        startPosition = 0;
        writer = new DexDataWriter(output, startPosition, 256);

        writer.align();
        writer.write(1);
        writer.align();
        writer.align();

        writer.write(1);
        writer.write(2);
        writer.align();

        writer.write(1);
        writer.write(2);
        writer.write(3);
        writer.align();
        writer.align();

        writer.write(1);
        writer.write(2);
        writer.write(3);
        writer.write(4);
        writer.align();
        writer.align();
        writer.align();
        writer.align();

        writer.write(1);
        writer.align();

        expectData(0x01, 0x00, 0x00, 0x00,
                   0x01, 0x02, 0x00, 0x00,
                   0x01, 0x02, 0x03, 0x00,
                   0x01, 0x02, 0x03, 0x04,
                   0x01, 0x00, 0x00, 0x00);
    }
}
