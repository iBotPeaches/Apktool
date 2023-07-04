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

public class ResID {
    public final int pkgId;
    public final int type;
    public final int entry;
    public final int id;

    public ResID(int pkgId, int type, int entry) {
        this(pkgId, type, entry, (pkgId << 24) + (type << 16) + entry);
    }

    public ResID(int id) {
        this((id >> 24) & 0xff, (id >> 16) & 0x000000ff, id & 0x0000ffff, id);
    }

    public ResID(int pkgId, int type, int entry, int id) {
        this.pkgId = (pkgId == 0) ? 2 : pkgId;
        this.type = type;
        this.entry = entry;
        this.id = id;
    }

    @Override
    public String toString() {
        return String.format("0x%08x", id);
    }

    @Override
    public int hashCode() {
        int hash = 17;
        hash = 31 * hash + this.id;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ResID other = (ResID) obj;
        return this.id == other.id;
    }
}
