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
 * Implementation of {@link Input} which reads the data from a
 * <code>byte[]</code> instance.
 *
 * <p><b>Note:</b> As per the {@link Input } interface, multi-byte
 * reads all use little-endian order.</p>
 */
public class ByteArrayInput
    implements Input {

    /** non-null; the data itself */
    private byte[] data;

    /** &gt;= 0; current read cursor */
    private int cursor;

    /**
     * Constructs an instance with the given data
     *
     * @param data non-null; data array to use for input
     */
    public ByteArrayInput(byte[] data) {
        if (data == null) {
            throw new NullPointerException("data == null");
        }

        this.data = data;
        this.cursor = 0;
    }

    /**
     * Gets the underlying <code>byte[]</code> of this instance
     *
     * @return non-null; the <code>byte[]</code>
     */
    public byte[] getArray() {
        return data;
    }

    /** {@inheritDoc} */
    public int getCursor() {
        return cursor;
    }

    /** {@inheritDoc} */
    public void setCursor(int cursor) {
        if (cursor < 0 || cursor >= data.length)
            throw new IndexOutOfBoundsException("The provided cursor value " +
                    "is not within the bounds of this instance's data array");
        this.cursor = cursor;
    }

    /** {@inheritDoc} */
    public void assertCursor(int expectedCursor) {
        if (cursor != expectedCursor) {
            throw new ExceptionWithContext("expected cursor " +
                    expectedCursor + "; actual value: " + cursor);
        }
    }

    /** {@inheritDoc} */
    public byte readByte() {
        return data[cursor++];
    }

    /** {@inheritDoc} */
    public int readShort() {
        int readAt = cursor;
        int result = ((data[readAt++] & 0xff) +
                     ((data[readAt++] & 0xff) << 8));
        cursor = readAt;
        return result;
    }

    /** {@inheritDoc} */
    public int readInt() {
        int readAt = cursor;
        int result = (data[readAt++] & 0xff) +
                     ((data[readAt++] & 0xff) << 8) +
                     ((data[readAt++] & 0xff) << 16) +
                     ((data[readAt++] & 0xff) << 24);
        cursor = readAt;
        return result;
    }

    /** {@inheritDoc} */
    public long readLong() {
        int readAt = cursor;

        long result = (data[readAt++] & 0xffL) |
                      ((data[readAt++] & 0xffL) << 8) |
                      ((data[readAt++] & 0xffL) << 16) |
                      ((data[readAt++] & 0xffL) << 24) |
                      ((data[readAt++] & 0xffL) << 32) |
                      ((data[readAt++] & 0xffL) << 40) |
                      ((data[readAt++] & 0xffL) << 48) |
                      ((data[readAt++] & 0xffL) << 56);
        cursor = readAt;
        return result;
    }


    /** {@inheritDoc} */
    public int readUnsignedOrSignedLeb128() {
        int end = cursor;
        int currentByteValue;
        int result;

        result = data[end++] & 0xff;
        if (result > 0x7f) {
            currentByteValue = data[end++] & 0xff;
            result = (result & 0x7f) | ((currentByteValue & 0x7f) << 7);
            if (currentByteValue > 0x7f) {
                currentByteValue = data[end++] & 0xff;
                result |= (currentByteValue & 0x7f) << 14;
                if (currentByteValue > 0x7f) {
                    currentByteValue = data[end++] & 0xff;
                    result |= (currentByteValue & 0x7f) << 21;
                    if (currentByteValue > 0x7f) {
                        currentByteValue = data[end++] & 0xff;
                        if (currentByteValue > 0x0f) {
                            throwInvalidLeb();
                        }
                        result |= currentByteValue << 28;
                    }
                }
            }
        } else {
            cursor = end;
            return result;
        }

        cursor = end;

        //If the last byte is 0, then this was an unsigned value (incorrectly) written in a signed format
        //The caller wants to know if this is the case, so we'll return the negated value instead
        //If there was only a single byte that had a value of 0, then we would have returned in the above
        //"else"
        if (data[end-1] == 0) {
            return ~result;
        }
        return result;
    }




    /** {@inheritDoc} */
    public int readUnsignedLeb128() {
        int end = cursor;
        int currentByteValue;
        int result;

        result = data[end++] & 0xff;
        if (result > 0x7f) {
            currentByteValue = data[end++] & 0xff;
            result = (result & 0x7f) | ((currentByteValue & 0x7f) << 7);
            if (currentByteValue > 0x7f) {
                currentByteValue = data[end++] & 0xff;
                result |= (currentByteValue & 0x7f) << 14;
                if (currentByteValue > 0x7f) {
                    currentByteValue = data[end++] & 0xff;
                    result |= (currentByteValue & 0x7f) << 21;
                    if (currentByteValue > 0x7f) {
                        currentByteValue = data[end++] & 0xff;
                        if (currentByteValue > 0x0f) {
                            throwInvalidLeb();
                        }
                        result |= currentByteValue << 28;
                    }
                }
            }
        }

        cursor = end;
        return result;
    }

    /** {@inheritDoc} */
    public int readSignedLeb128() {
        int end = cursor;
        int currentByteValue;
        int result;

        result = data[end++] & 0xff;
        if (result <= 0x7f) {
            result = (result << 25) >> 25;
        } else {
            currentByteValue = data[end++] & 0xff;
            result = (result & 0x7f) | ((currentByteValue & 0x7f) << 7);
            if (currentByteValue <= 0x7f) {
                result = (result << 18) >> 18;
            } else {
                currentByteValue = data[end++] & 0xff;
                result |= (currentByteValue & 0x7f) << 14;
                if (currentByteValue <= 0x7f) {
                    result = (result << 11) >> 11;
                } else {
                    currentByteValue = data[end++] & 0xff;
                    result |= (currentByteValue & 0x7f) << 21;
                    if (currentByteValue <= 0x7f) {
                        result = (result << 4) >> 4;
                    } else {
                        currentByteValue = data[end++] & 0xff;
                        if (currentByteValue > 0x0f) {
                            throwInvalidLeb();
                        }
                        result |= currentByteValue << 28;
                    }
                }
            }
        }

        cursor = end;
        return result;
    }

    /** {@inheritDoc} */
    public void read(byte[] bytes, int offset, int length) {
        int end = cursor + length;

        if (end > data.length) {
            throwBounds();
        }

        System.arraycopy(data, cursor, bytes, offset, length);
        cursor = end;
    }

    /** {@inheritDoc} */
    public void read(byte[] bytes) {
        int length = bytes.length;
        int end = cursor + length;

        if (end > data.length) {
            throwBounds();
        }

        System.arraycopy(data, cursor, bytes, 0, length);
        cursor = end;
    }

    /** {@inheritDoc} */
    public byte[] readBytes(int length) {
        int end = cursor + length;

        if (end > data.length) {
            throwBounds();
        }

        byte[] result = new byte[length];
        System.arraycopy(data, cursor, result, 0, length);
        cursor = end;
        return result;
    }

    /** {@inheritDoc} */
    public String realNullTerminatedUtf8String() {
        int startPosition = cursor;
        while (data[cursor] != 0) {
            cursor++;
        }
        int byteCount = cursor - startPosition;

        //skip the terminating null
        cursor++;

        return Utf8Utils.utf8BytesToString(data, startPosition, byteCount);
    }

    /** {@inheritDoc} */
    public void skipBytes(int count) {
        cursor += count;
    }

    /** {@inheritDoc} */
    public void alignTo(int alignment) {
        cursor = AlignmentUtils.alignOffset(cursor, alignment);
    }

    /**
     * Throws the excpetion for when an attempt is made to read past the
     * end of the instance.
     */
    private static void throwBounds() {
        throw new IndexOutOfBoundsException("attempt to read past the end");
    }

    /**
     * Throws the exception for when an invalid LEB128 value is encountered
     */
    private static void throwInvalidLeb() {
        throw new RuntimeException("invalid LEB128 integer encountered");
    }
}
