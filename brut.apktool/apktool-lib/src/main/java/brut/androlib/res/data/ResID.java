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

public class ResID extends Number implements Comparable<ResID> {
    private final int mId;

    public ResID(int id) {
        mId = id;
    }

    public int getPackageId() {
        int pkgId = (mId >> 24) & 0xff;
        return pkgId == 0 ? 2 : pkgId;
    }

    public int getType() {
        return (mId >> 16) & 0x000000ff;
    }

    public int getEntry() {
        return mId & 0x0000ffff;
    }

    @Override
    public int intValue() {
        return mId;
    }

    @Override
    public long longValue() {
        return mId;
    }

    @Override
    public float floatValue() {
        return mId;
    }

    @Override
    public double doubleValue() {
        return mId;
    }

    @Override
    public String toString() {
        return String.format("0x%08x", mId);
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(mId);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof ResID) {
            ResID other = (ResID) obj;
            return mId == other.mId;
        }
        return false;
    }

    @Override
    public int compareTo(ResID other) {
        return Integer.compare(mId, other.mId);
    }
}
