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
package brut.androlib.res.decoder.data;

import brut.util.BinaryDataInputStream;

import java.io.IOException;

public final class LayoutBounds {
    public static final int MAGIC = 0x6E704C62; // npLb
    public static final int COLOR_TICK = 0xFFFF0000; // solid red

    public final int left, top, right, bottom;

    public LayoutBounds(int left, int top, int right, int bottom) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }

    public static LayoutBounds read(BinaryDataInputStream in) throws IOException {
        int left = Integer.reverseBytes(in.readInt());
        int top = Integer.reverseBytes(in.readInt());
        int right = Integer.reverseBytes(in.readInt());
        int bottom = Integer.reverseBytes(in.readInt());

        return new LayoutBounds(left, top, right, bottom);
    }
}
