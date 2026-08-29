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
package brut.androlib.res.data;

public final class FeatureFlag {
    public final String mName;
    public final boolean mNegated;

    public FeatureFlag(String name, boolean negated) {
        assert name != null;
        mName = name;
        mNegated = negated;
    }

    public static String toString(String name, boolean negated) {
        return negated ? "!" + name : name;
    }

    @Override
    public String toString() {
        return toString(mName, mNegated);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        FeatureFlag other = (FeatureFlag) obj;
        return mName.equals(other.mName)
            && mNegated == other.mNegated;
    }

    @Override
    public int hashCode() {
        return 31 * mName.hashCode() + Boolean.hashCode(mNegated);
    }
}
