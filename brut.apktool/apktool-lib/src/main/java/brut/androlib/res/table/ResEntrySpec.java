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

import com.google.common.collect.Sets;

import java.util.Objects;
import java.util.Set;

public class ResEntrySpec {
    private static final Set<String> INVALID_ENTRY_NAMES = Sets.newHashSet(
        "0_resource_name_obfuscated", // #3067
        "(name removed)" // #2940
    );

    public static final String DUMMY_PREFIX = "APKTOOL_DUMMY_";
    public static final String MISSING_PREFIX = "APKTOOL_MISSING_";
    public static final String RENAMED_PREFIX = "APKTOOL_RENAMED_";

    private final ResTypeSpec mTypeSpec;
    private final ResId mId;
    private final String mName;

    public ResEntrySpec(ResTypeSpec typeSpec, ResId id, String name) {
        assert typeSpec.getPackage().getId() == id.getPackageId();
        assert typeSpec.getId() == id.getTypeId();
        mTypeSpec = typeSpec;
        mId = id;
        // Some apps had their entry names collapsed to a single value in
        // the key string pool. Rename to avoid duplicates.
        if (name == null || name.isEmpty() || INVALID_ENTRY_NAMES.contains(name)) {
            mName = RENAMED_PREFIX + id;
        } else {
            mName = name;
        }
    }

    public ResTypeSpec getTypeSpec() {
        return mTypeSpec;
    }

    public ResPackage getPackage() {
        return mTypeSpec.getPackage();
    }

    public String getTypeName() {
        return mTypeSpec.getName();
    }

    public ResId getId() {
        return mId;
    }

    public String getName() {
        return mName;
    }

    public String getFullName(ResPackage relativeToPackage, boolean excludeType) {
        return getFullName(getPackage() == relativeToPackage, excludeType);
    }

    public String getFullName(boolean excludePackage, boolean excludeType) {
        return (excludePackage ? "" : getPackage().getName() + ":")
                + (excludeType ? "" : getTypeName() + "/") + mName;
    }

    public boolean isDummy() {
        return mName.startsWith(DUMMY_PREFIX);
    }

    public boolean isMissing() {
        return mName.startsWith(MISSING_PREFIX);
    }

    public boolean isRenamed() {
        return mName.startsWith(RENAMED_PREFIX);
    }

    @Override
    public String toString() {
        return String.format("ResEntrySpec{typeSpec=%s, id=%s, name=%s}",
            mTypeSpec, mId, mName);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof ResEntrySpec) {
            ResEntrySpec other = (ResEntrySpec) obj;
            return Objects.equals(mTypeSpec, other.mTypeSpec)
                    && Objects.equals(mId, other.mId)
                    && Objects.equals(mName, other.mName);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mTypeSpec, mId, mName);
    }
}
