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
    public final int paddingLeft, paddingRight, paddingTop, paddingBottom;
    public final int[] xDivs, yDivs;

    public NinePatchData(int paddingLeft, int paddingRight, int paddingTop, int paddingBottom,
                         int[] xDivs, int[] yDivs) {
        this.paddingLeft = paddingLeft;
        this.paddingRight = paddingRight;
        this.paddingTop = paddingTop;
        this.paddingBottom = paddingBottom;
        this.xDivs = xDivs;
        this.yDivs = yDivs;
    }

    public static NinePatchData decode(ExtDataInput in) throws IOException {
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

        return new NinePatchData(paddingLeft, paddingRight, paddingTop, paddingBottom, xDivs, yDivs);
    }
}
