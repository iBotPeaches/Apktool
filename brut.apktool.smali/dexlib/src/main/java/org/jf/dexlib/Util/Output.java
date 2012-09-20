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

package org.jf.dexlib.Util;

/**
 * Interface for a sink for binary output. This is similar to
 * <code>java.util.DataOutput</code>, but no <code>IOExceptions</code>
 * are declared, and multibyte output is defined to be little-endian.
 */
public interface Output {
    /**
     * Gets the current cursor position. This is the same as the number of
     * bytes written to this instance.
     *
     * @return &gt;= 0; the cursor position
     */
    public int getCursor();

    /**
     * Asserts that the cursor is the given value.
     *
     * @param expectedCursor the expected cursor value
     * @throws RuntimeException thrown if <code>getCursor() !=
     * expectedCursor</code>
     */
    public void assertCursor(int expectedCursor);

    /**
     * Writes a <code>byte</code> to this instance.
     *
     * @param value the value to write; all but the low 8 bits are ignored
     */
    public void writeByte(int value);

    /**
     * Writes a <code>short</code> to this instance.
     *
     * @param value the value to write; all but the low 16 bits are ignored
     */
    public void writeShort(int value);

    /**
     * Writes an <code>int</code> to this instance.
     *
     * @param value the value to write
     */
    public void writeInt(int value);

    /**
     * Writes a <code>long</code> to this instance.
     *
     * @param value the value to write
     */
    public void writeLong(long value);

    /**
     * Writes a DWARFv3-style unsigned LEB128 integer. For details,
     * see the "Dalvik Executable Format" document or DWARF v3 section
     * 7.6.
     *
     * @param value value to write, treated as an unsigned value
     * @return 1..5; the number of bytes actually written
     */
    public int writeUnsignedLeb128(int value);

    /**
     * Writes a DWARFv3-style unsigned LEB128 integer. For details,
     * see the "Dalvik Executable Format" document or DWARF v3 section
     * 7.6.
     *
     * @param value value to write
     * @return 1..5; the number of bytes actually written
     */
    public int writeSignedLeb128(int value);

    /**
     * Writes a {@link org.jf.dexlib.Util.ByteArray} to this instance.
     *
     * @param bytes non-null; the array to write
     */
    public void write(ByteArray bytes);

    /**
     * Writes a portion of a <code>byte[]</code> to this instance.
     *
     * @param bytes non-null; the array to write
     * @param offset &gt;= 0; offset into <code>bytes</code> for the first
     * byte to write
     * @param length &gt;= 0; number of bytes to write
     */
    public void write(byte[] bytes, int offset, int length);

    /**
     * Writes a <code>byte[]</code> to this instance. This is just
     * a convenient shorthand for <code>write(bytes, 0, bytes.length)</code>.
     *
     * @param bytes non-null; the array to write
     */
    public void write(byte[] bytes);

    /**
     * Writes the given number of <code>0</code> bytes.
     *
     * @param count &gt;= 0; the number of zeroes to write
     */
    public void writeZeroes(int count);

    /**
     * Adds extra bytes if necessary (with value <code>0</code>) to
     * force alignment of the output cursor as given.
     *
     * @param alignment &gt; 0; the alignment; must be a power of two
     */
    public void alignTo(int alignment);
}