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

import brut.androlib.res.table.value.ResValue;

import java.util.Objects;

public class ResEntry {
    private final ResType mType;
    private final ResEntrySpec mSpec;
    private ResValue mValue; // might be updated later

    public ResEntry(ResType type, ResEntrySpec spec, ResValue value) {
        assert type != null && spec != null && type.getSpec() == spec.getTypeSpec() && value != null;
        mType = type;
        mSpec = spec;
        mValue = value;
    }

    public ResPackage getPackage() {
        return mType.getPackage();
    }

    public ResType getType() {
        return mType;
    }

    public ResEntrySpec getSpec() {
        return mSpec;
    }

    public int getId() {
        return mSpec.getId();
    }

    public ResId getResId() {
        return mSpec.getResId();
    }

    public String getName() {
        return mSpec.getName();
    }

    public ResValue getValue() {
        return mValue;
    }

    public void setValue(ResValue value) {
        mValue = value;
    }

    @Override
    public String toString() {
        return String.format("ResEntry{type=%s, spec=%s, value=%s}", mType, mSpec, mValue);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof ResEntry) {
            ResEntry other = (ResEntry) obj;
            return mType.equals(other.mType)
                && mSpec.equals(other.mSpec);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mType, mSpec);
    }
}
