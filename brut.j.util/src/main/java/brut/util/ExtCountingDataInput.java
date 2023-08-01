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

import org.apache.commons.io.input.CountingInputStream;
import com.google.common.io.LittleEndianDataInputStream;

import java.io.DataInput;
import java.io.IOException;
import java.util.logging.Logger;

public class ExtCountingDataInput extends ExtDataInput {
    private final CountingInputStream mCountIn;

    public ExtCountingDataInput(LittleEndianDataInputStream in) {
        this(new CountingInputStream(in));
    }

    public ExtCountingDataInput(CountingInputStream countIn) {
        // We need to explicitly cast to DataInput as otherwise the constructor is ambiguous.
        // We choose DataInput instead of InputStream as ExtDataInput wraps an InputStream in
        // a DataInputStream which is big-endian and ignores the little-endian behavior.
        super((DataInput) new LittleEndianDataInputStream(countIn));
        mCountIn = countIn;
    }

    public int position() {
        return mCountIn.getCount();
    }

    public int remaining() throws IOException {
        return mCountIn.available();
    }

    public long skip(int bytes) throws IOException {
        return mCountIn.skip(bytes);
    }

    public int[] readSafeIntArray(int length, long maxPosition) throws IOException {
        int[] array = new int[length];

        for (int i = 0; i < length; i++) {
            // #3236 - In some applications we have more strings than fit into the block. This function takes
            // an expected max position and if we are past it, we return early during processing.
            if (position() >= maxPosition) {
                LOGGER.warning(String.format("Bad string block: string entry is at %d, past end at %d",
                    position(), maxPosition)
                );
                return array;
            }

            array[i] = readInt();
        }
        return array;
    }

    private static final Logger LOGGER = Logger.getLogger(ExtCountingDataInput.class.getName());
}
