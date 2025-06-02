/*
 *  Copyright (C) 2010 Ryszard Wiśniewski <brut.alll@gmail.com>
 *  Copyright (C) 2010 Connor Tumbleson <connor.tumbleson@gmail.com>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package brut.util;

import java.io.DataInput;
import java.io.IOException;

public interface ExtDataInput extends DataInput {
    long position();

    void jumpTo(long pos) throws IOException;

    void skipByte() throws IOException;

    void skipShort() throws IOException;

    void skipInt() throws IOException;

    byte[] readBytes(int len) throws IOException;

    int[] readIntArray(int len) throws IOException;

    String readAscii(int len) throws IOException;

    String readUtf16(int len) throws IOException;
}
