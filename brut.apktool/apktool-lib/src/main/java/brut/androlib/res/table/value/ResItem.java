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
package brut.androlib.res.table.value;

import brut.androlib.exceptions.AndrolibException;
import brut.androlib.res.table.ResId;
import brut.androlib.res.table.ResPackage;
import brut.androlib.res.xml.ValuesXmlSerializable;
import com.google.common.collect.Sets;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public abstract class ResItem extends ResValue implements ValuesXmlSerializable {
    private static final Logger LOGGER = Logger.getLogger(ResItem.class.getName());

    protected static final Map<String, Set<String>> STANDARD_TYPE_FORMATS = new HashMap<>();

    static {
        STANDARD_TYPE_FORMATS.put("bool", Sets.newHashSet("boolean"));
        STANDARD_TYPE_FORMATS.put("color", Sets.newHashSet("color"));
        STANDARD_TYPE_FORMATS.put("dimen", Sets.newHashSet("float", "fraction", "dimension"));
        STANDARD_TYPE_FORMATS.put("drawable", Sets.newHashSet("color"));
        STANDARD_TYPE_FORMATS.put("fraction", Sets.newHashSet("float", "fraction", "dimension"));
        STANDARD_TYPE_FORMATS.put("integer", Sets.newHashSet("integer"));
        STANDARD_TYPE_FORMATS.put("string", Sets.newHashSet("string"));
    }

    protected final int mType;

    protected ResItem(int type) {
        mType = type;
    }

    public static ResItem parse(ResPackage pkg, int type, int data) {
        assert type != TYPE_STRING;
        switch (type) {
            case TYPE_NULL:
                return data == DATA_NULL_EMPTY ? ResPrimitive.EMPTY : ResPrimitive.NULL;
            case TYPE_REFERENCE:
            case TYPE_DYNAMIC_REFERENCE:
                return new ResReference(pkg, ResId.of(data));
            case TYPE_ATTRIBUTE:
            case TYPE_DYNAMIC_ATTRIBUTE:
                return new ResReference(pkg, ResId.of(data), true);
            case TYPE_FLOAT:
            case TYPE_DIMENSION:
            case TYPE_FRACTION:
                return new ResPrimitive(type, data);
        }
        // Handle integer, boolean and color.
        if (type >= TYPE_FIRST_INT && type <= TYPE_LAST_INT) {
            return new ResPrimitive(type, data);
        }
        LOGGER.warning(String.format("Invalid value type: 0x%02x", type));
        return null;
    }

    public int getType() {
        return mType;
    }

    public String getFormat() {
        switch (mType) {
            case TYPE_NULL:
                return null;
            case TYPE_REFERENCE:
            case TYPE_DYNAMIC_REFERENCE:
            case TYPE_ATTRIBUTE:
            case TYPE_DYNAMIC_ATTRIBUTE:
                return "reference";
            case TYPE_STRING:
                return "string";
            case TYPE_FLOAT:
                return "float";
            case TYPE_DIMENSION:
                return "dimension";
            case TYPE_FRACTION:
                return "fraction";
            case TYPE_INT_BOOLEAN:
                return "boolean";
        }
        if (mType >= TYPE_FIRST_COLOR_INT && mType <= TYPE_LAST_COLOR_INT) {
            return "color";
        }
        if (mType >= TYPE_FIRST_INT && mType <= TYPE_LAST_INT) {
            return "integer";
        }
        LOGGER.warning(String.format("Unexpected value type: 0x%02x", mType));
        return null;
    }

    // Must never return null.
    public abstract String toXmlTextValue() throws AndrolibException;

    // Must never return null.
    public String toXmlAttributeValue() throws AndrolibException {
        return toXmlTextValue();
    }
}
