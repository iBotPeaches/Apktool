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
package brut.androlib.res.data.ninepatch;

import brut.util.ExtDataInput;
import java.io.IOException;

public class NinePatchData {
    public final int padLeft, padRight, padTop, padBottom;
    public final int[] xDivs, yDivs;

    public NinePatchData(int padLeft, int padRight, int padTop, int padBottom, int[] xDivs, int[] yDivs) {
        this.padLeft = padLeft;
        this.padRight = padRight;
        this.padTop = padTop;
        this.padBottom = padBottom;
        this.xDivs = xDivs;
        this.yDivs = yDivs;
    }

    public static NinePatchData decode(ExtDataInput di) throws IOException {
        di.skipBytes(1); // wasDeserialized
        byte numXDivs = di.readByte();
        byte numYDivs = di.readByte();
        di.skipBytes(1); // numColors
        di.skipBytes(8); // xDivs/yDivs offset
        int padLeft = di.readInt();
        int padRight = di.readInt();
        int padTop = di.readInt();
        int padBottom = di.readInt();
        di.skipBytes(4); // colorsOffset
        int[] xDivs = di.readIntArray(numXDivs);
        int[] yDivs = di.readIntArray(numYDivs);

        return new NinePatchData(padLeft, padRight, padTop, padBottom, xDivs, yDivs);
    }
}
