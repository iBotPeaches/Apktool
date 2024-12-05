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

public class OpticalInset {
    public final int layoutBoundsLeft, layoutBoundsTop, layoutBoundsRight, layoutBoundsBottom;

    public OpticalInset(int layoutBoundsLeft, int layoutBoundsTop, int layoutBoundsRight, int layoutBoundsBottom) {
        this.layoutBoundsLeft = layoutBoundsLeft;
        this.layoutBoundsTop = layoutBoundsTop;
        this.layoutBoundsRight = layoutBoundsRight;
        this.layoutBoundsBottom = layoutBoundsBottom;
    }

    public static OpticalInset decode(ExtDataInput in) throws IOException {
        int layoutBoundsLeft = Integer.reverseBytes(in.readInt());
        int layoutBoundsTop = Integer.reverseBytes(in.readInt());
        int layoutBoundsRight = Integer.reverseBytes(in.readInt());
        int layoutBoundsBottom = Integer.reverseBytes(in.readInt());
        return new OpticalInset(layoutBoundsLeft, layoutBoundsTop, layoutBoundsRight, layoutBoundsBottom);
    }
}
