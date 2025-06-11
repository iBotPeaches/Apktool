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

public final class NinePatchData {
    public static final int MAGIC = 0x6E705463; // npTc
    public static final int COLOR_TICK = 0xFF000000; // solid black

    public final int[] xDivs, yDivs;
    public final int paddingLeft, paddingRight, paddingTop, paddingBottom;

    public NinePatchData(int[] xDivs, int[] yDivs, int paddingLeft, int paddingRight, int paddingTop,
                     int paddingBottom) {
        this.xDivs = xDivs;
        this.yDivs = yDivs;
        this.paddingLeft = paddingLeft;
        this.paddingRight = paddingRight;
        this.paddingTop = paddingTop;
        this.paddingBottom = paddingBottom;
    }

    public static NinePatchData read(BinaryDataInputStream in) throws IOException {
        in.skipByte(); // wasDeserialized
        int numXDivs = in.readUnsignedByte();
        int numYDivs = in.readUnsignedByte();
        in.skipByte(); // numColors
        in.skipInt(); // xDivsOffset
        in.skipInt(); // yDivsOffset
        int paddingLeft = in.readInt();
        int paddingRight = in.readInt();
        int paddingTop = in.readInt();
        int paddingBottom = in.readInt();
        in.skipInt(); // colorsOffset
        int[] xDivs = in.readIntArray(numXDivs);
        int[] yDivs = in.readIntArray(numYDivs);

        return new NinePatchData(xDivs, yDivs, paddingLeft, paddingRight, paddingTop, paddingBottom);
    }
}
