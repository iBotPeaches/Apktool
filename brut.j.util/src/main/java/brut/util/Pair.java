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

import java.util.Objects;

public class Pair<L, R> {
    private final L mLeft;
    private final R mRight;

    private Pair(L left, R right) {
        mLeft = left;
        mRight = right;
    }

    public static <L, R> Pair<L, R> of(L left, R right) {
        return new Pair<>(left, right);
    }

    public L getLeft() {
        return mLeft;
    }

    public R getRight() {
        return mRight;
    }

    @Override
    public String toString() {
        return "(" + String.valueOf(mLeft) + "," + String.valueOf(mRight) + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof Pair) {
            Pair<?, ?> other = (Pair<?, ?>) obj;
            return Objects.equals(mLeft, other.mLeft)
                && Objects.equals(mRight, other.mRight);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 31 * Objects.hashCode(mLeft) + Objects.hashCode(mRight);
    }
}
