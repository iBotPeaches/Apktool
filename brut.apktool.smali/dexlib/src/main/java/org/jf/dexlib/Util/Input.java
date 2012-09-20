/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * As per the Apache license requirements, this file has been modified
 * from its original state.
 *
 * Such modifications are Copyright (C) 2010 Ben Gruver, and are released
 * under the original license
 */

package org.jf.dexlib.Util;

/**
 * Interface for a source for binary input. This is similar to
 * <code>java.util.DataInput</code>, but no <code>IOExceptions</code>
 * are declared, and multibyte input is defined to be little-endian.
 */
public interface Input {
    /**
     * Gets the current cursor position. This is the same as the number of
     * bytes read from this instance.
     *
     * @return &gt;= 0; the cursor position
     */
    public int getCursor();

    /**
     * Sets the current cursor position.
     *
     * @return &gt;= 0; the cursor position
     */
    public void setCursor(int cursor);

    /**
     * Asserts that the cursor is the given value.
     *
     * @param expectedCursor the expected cursor value
     * @throws RuntimeException thrown if <code>getCursor() !=
     * expectedCursor</code>
     */
    public void assertCursor(int expectedCursor);

    /**
     * Reads a <code>byte</code> from this instance.
     *
     * @return the byte value that was read
     */
    public byte readByte();

    /**
     * Reads a <code>short</code> from this instance.
     *
     * @return the short value that was read, as an int
     */
    public int readShort();

    /**
     * Reads an <code>int</code> from this instance.
     *
     * @return the unsigned int value that was read
     */
    public int readInt();

    /**
     * Reads a <code>long</code> from this instance.
     *
     * @return the long value that was read
     */
    public long readLong();


    /**
     * Reads a DWARFv3-style signed LEB128 integer. For details,
     * see the "Dalvik Executable Format" document or DWARF v3 section
     * 7.6.
     *
     * @return the integer value that was read
     */
    public int readSignedLeb128();

    /**
     * Reads a DWARFv3-style unsigned LEB128 integer. For details,
     * see the "Dalvik Executable Format" document or DWARF v3 section
     * 7.6.
     *
     * @return the integer value that was read
     */
    public int readUnsignedLeb128();


    /**
     * Reads a unsigned value as a DWARFv3-style LEB128 integer. It specifically
     * checks for the case when the value was incorrectly formatted as a signed
     * LEB128, and returns the appropriate unsigned value, but negated
     * @return If the value was formatted as a ULEB128, it returns the actual unsigned
     * value. Otherwise, if the value was formatted as a signed LEB128, it negates the
     * "correct" unsigned value and returns that
     */
    public int readUnsignedOrSignedLeb128();

    /**
     * reads a <code>byte[]</code> from this instance.
     *
     * @param bytes non-null; the buffer to read the data into
     * @param offset &gt;= 0; offset into <code>bytes</code> for the first
     * byte to write
     * @param length &gt;= 0; number of bytes to read
     */
    public void read(byte[] bytes, int offset, int length);

    /**
     * reads a <code>byte[]</code> from this instance. This is just
     * a convenient shorthand for <code>read(bytes, 0, bytes.length)</code>.
     *
     * @param bytes non-null; the buffer to read the data into
     */
    public void read(byte[] bytes);


    /**
     * reads a <code>byte[]</code> from this instance
     *
     * @param length &gt;= 0; number of bytes to read
     * @return a byte array containing <code>length</code> bytes
     */
    public byte[] readBytes(int length);

    /**
     * reads and decodes a null terminated utf8 string from the current cursor up to but not including
     * the next null (0) byte. The terminating null byte is read and discarded, so that after the read,
     * the cursor is positioned at the byte immediately after the terminating null
     *
     * @return a string representing the decoded value
     */
    public String realNullTerminatedUtf8String();

    /**
     * Skips the given number of bytes.
     *
     * @param count &gt;= 0; the number of bytes to skip
     */
    public void skipBytes(int count);

    /**
     * Skip extra bytes if necessary to force alignment of the output
     * cursor as given.
     *
     * @param alignment &gt; 0; the alignment; must be a power of two
     */
    public void alignTo(int alignment);
}
