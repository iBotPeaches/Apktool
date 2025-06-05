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
package brut.androlib.res.table;

import java.util.Objects;

public class ResTypeSpec {
    private final ResPackage mPackage;
    private final int mId;
    private final String mName;

    public ResTypeSpec(ResPackage pkg, int id, String name) {
        mPackage = pkg;
        mId = id;
        mName = name;
    }

    public ResPackage getPackage() {
        return mPackage;
    }

    public int getId() {
        return mId;
    }

    public String getName() {
        return mName;
    }

    public boolean isBagType() {
        switch (mName) {
            case "attr":
            case "^attr-private":
            case "array":
            case "plurals":
            case "style":
                return true;
            default:
                return false;
        }
    }

    @Override
    public String toString() {
        return String.format("ResTypeSpec{pkg=%s, id=0x%02x, name=%s}",
            mPackage, mId, mName);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof ResTypeSpec) {
            ResTypeSpec other = (ResTypeSpec) obj;
            return Objects.equals(mPackage, other.mPackage)
                    && mId == other.mId
                    && Objects.equals(mName, other.mName);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mPackage, mId, mName);
    }
}
