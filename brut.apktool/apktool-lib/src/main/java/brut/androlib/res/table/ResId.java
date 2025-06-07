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

import java.util.HashMap;
import java.util.Map;

public class ResId extends Number implements Comparable<ResId> {
    private static final Map<Integer, ResId> sCache = new HashMap<>();

    public static final ResId NULL = new ResId(0);

    private final int mId;

    public static ResId of(int id) {
        return id != 0 ? sCache.computeIfAbsent(id, ResId::new) : NULL;
    }

    public static ResId of(int pkgId, int typeId, int entryId) {
        assert (pkgId & 0xFF) == pkgId;
        assert (typeId & 0xFF) == typeId;
        assert (entryId & 0xFFFF) == entryId;
        return ResId.of((pkgId << 24) | (typeId << 16) | entryId);
    }

    private ResId(int id) {
        mId = id;
    }

    public int getPackageId() {
        return (mId >>> 24) & 0xFF;
    }

    public int getTypeId() {
        return (mId >>> 16) & 0xFF;
    }

    public int getEntryId() {
        return mId & 0xFFFF;
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
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof ResId) {
            ResId other = (ResId) obj;
            return mId == other.mId;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(mId);
    }

    @Override
    public int compareTo(ResId other) {
        return Integer.compare(mId, other.mId);
    }
}
