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

import brut.androlib.res.table.value.ResItem;
import com.google.common.collect.Sets;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class ResTypeSpec {
    private static final Map<String, Set<String>> STANDARD_TYPE_FORMATS = new HashMap<>();
    static {
        STANDARD_TYPE_FORMATS.put("bool", Sets.newHashSet("boolean"));
        STANDARD_TYPE_FORMATS.put("color", Sets.newHashSet("color"));
        STANDARD_TYPE_FORMATS.put("dimen", Sets.newHashSet("float", "fraction", "dimension"));
        STANDARD_TYPE_FORMATS.put("drawable", Sets.newHashSet("color"));
        STANDARD_TYPE_FORMATS.put("fraction", Sets.newHashSet("float", "fraction", "dimension"));
        STANDARD_TYPE_FORMATS.put("integer", Sets.newHashSet("integer"));
        STANDARD_TYPE_FORMATS.put("string", Sets.newHashSet("string"));
    }

    private final ResPackage mPackage;
    private final int mId;
    private final String mName;

    public ResTypeSpec(ResPackage pkg, int id, String name) {
        assert pkg != null && id > 0 && name != null;
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

    public boolean isValueCompatible(ResItem value) {
        // Bag types don't support item values.
        if (isBagType()) {
            return false;
        }
        String format = value.getFormat();
        if (format == null) {
            return false;
        }
        // All item types support the reference format.
        if (format.equals("reference")) {
            return true;
        }
        Set<String> typeFormats = STANDARD_TYPE_FORMATS.get(mName);
        return typeFormats != null && typeFormats.contains(format);
    }

    @Override
    public String toString() {
        return String.format("ResTypeSpec{pkg=%s, id=0x%02x, name=%s}", mPackage, mId, mName);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof ResTypeSpec) {
            ResTypeSpec other = (ResTypeSpec) obj;
            return mPackage.equals(other.mPackage)
                && mId == other.mId
                && mName.equals(other.mName);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mPackage, mId, mName);
    }
}
