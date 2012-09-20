/**
 *  Copyright 2010 Ryszard Wiśniewski <brut.alll@gmail.com>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package brut.util;

import java.io.*;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public class ExtDataInput extends DataInputDelegate {
    public ExtDataInput(InputStream in) {
        this((DataInput) new DataInputStream(in));
    }

    public ExtDataInput(DataInput delegate) {
        super(delegate);
    }

    public int[] readIntArray(int length) throws IOException {
        int[] array = new int[length];
        for(int i = 0; i < length; i++) {
            array[i] = readInt();
        }
        return array;
    }

    public void skipInt() throws IOException {
        skipBytes(4);
    }

    public void skipCheckInt(int expected) throws IOException {
        int got = readInt();
        if (got != expected) {
            throw new IOException(String.format(
                "Expected: 0x%08x, got: 0x%08x", expected, got));
        }
    }

    public void skipCheckShort(short expected) throws IOException {
        short got = readShort();
        if (got != expected) {
            throw new IOException(String.format(
                "Expected: 0x%08x, got: 0x%08x", expected, got));
        }
    }

    public void skipCheckByte(byte expected) throws IOException {
        byte got = readByte();
        if (got != expected) {
            throw new IOException(String.format(
                "Expected: 0x%08x, got: 0x%08x", expected, got));
        }
    }

    public String readNulEndedString(int length, boolean fixed)
            throws IOException {
        StringBuilder string = new StringBuilder(16);
        while(length-- != 0) {
            short ch = readShort();
            if (ch == 0) {
                break;
            }
            string.append((char) ch);
        }
        if (fixed) {
            skipBytes(length * 2);
        }

        return string.toString();
    }
}
