/**
 *  Copyright (C) 2019 Ryszard Wiśniewski <brut.alll@gmail.com>
 *  Copyright (C) 2019 Connor Tumbleson <connor.tumbleson@gmail.com>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package brut.androlib.res.data;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public class ResID {
    public final int package_;
    public final int type;
    public final int entry;

    public final int id;

    public ResID(int package_, int type, int entry) {
        this(package_, type, entry, (package_ << 24) + (type << 16) + entry);
    }

    public ResID(int id) {
        this((id >> 24) & 0xff, (id >> 16) & 0x000000ff, id & 0x0000ffff, id);
    }

    public ResID(int package_, int type, int entry, int id) {
        this.package_ = (package_ == 0) ? 2 : package_;
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
        if (this.id != other.id) {
            return false;
        }
        return true;
    }
}
