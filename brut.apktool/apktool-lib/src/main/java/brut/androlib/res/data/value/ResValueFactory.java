/**
 *  Copyright (C) 2018 Ryszard Wiśniewski <brut.alll@gmail.com>
 *  Copyright (C) 2018 Connor Tumbleson <connor.tumbleson@gmail.com>
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
package brut.androlib.res.data.value;

import android.util.TypedValue;
import brut.androlib.AndrolibException;
import brut.androlib.res.data.ResPackage;
import brut.util.Duo;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public class ResValueFactory {
    private final ResPackage mPackage;

    public ResValueFactory(ResPackage package_) {
        this.mPackage = package_;
    }

    public ResScalarValue factory(int type, int value, String rawValue) throws AndrolibException {
        switch (type) {
            case TypedValue.TYPE_NULL:
                if (value == TypedValue.DATA_NULL_UNDEFINED) { // Special case $empty as explicitly defined empty value
                    return new ResStringValue(null, value);
                } else if (value == TypedValue.DATA_NULL_EMPTY) {
                    return new ResEmptyValue(value, rawValue, type);
                }
                return new ResReferenceValue(mPackage, 0, null);
            case TypedValue.TYPE_REFERENCE:
                return newReference(value, rawValue);
            case TypedValue.TYPE_ATTRIBUTE:
                return newReference(value, rawValue, true);
            case TypedValue.TYPE_STRING:
                return new ResStringValue(rawValue, value);
            case TypedValue.TYPE_FLOAT:
                return new ResFloatValue(Float.intBitsToFloat(value), value, rawValue);
            case TypedValue.TYPE_DIMENSION:
                return new ResDimenValue(value, rawValue);
            case TypedValue.TYPE_FRACTION:
                return new ResFractionValue(value, rawValue);
            case TypedValue.TYPE_INT_BOOLEAN:
                return new ResBoolValue(value != 0, value, rawValue);
            case TypedValue.TYPE_DYNAMIC_REFERENCE:
                return newReference(value, rawValue);
            case TypedValue.TYPE_DYNAMIC_ATTRIBUTE:
                return newReference(value, rawValue, true);
        }

        if (type >= TypedValue.TYPE_FIRST_COLOR_INT && type <= TypedValue.TYPE_LAST_COLOR_INT) {
            return new ResColorValue(value, rawValue);
        }
        if (type >= TypedValue.TYPE_FIRST_INT && type <= TypedValue.TYPE_LAST_INT) {
            return new ResIntValue(value, rawValue, type);
        }

        throw new AndrolibException("Invalid value type: " + type);
    }

    public ResIntBasedValue factory(String value, int rawValue) {
        if (value == null) {
            return new ResFileValue("", rawValue);
        }
        if (value.startsWith("res/")) {
            return new ResFileValue(value, rawValue);
        }
        if (value.startsWith("r/") || value.startsWith("R/")) { //AndroResGuard
            return new ResFileValue(value, rawValue);
        }
        return new ResStringValue(value, rawValue);
    }

    public ResBagValue bagFactory(int parent, Duo<Integer, ResScalarValue>[] items) throws AndrolibException {
        ResReferenceValue parentVal = newReference(parent, null);

        if (items.length == 0) {
            return new ResBagValue(parentVal);
        }
        int key = items[0].m1;
        if (key == ResAttr.BAG_KEY_ATTR_TYPE) {
            return ResAttr.factory(parentVal, items, this, mPackage);
        }
        // Android O Preview added an unknown enum for ResTable_map. This is hardcoded as 0 for now.
        if (key == ResArrayValue.BAG_KEY_ARRAY_START || key == 0) {
            return new ResArrayValue(parentVal, items);
        }
        if (key >= ResPluralsValue.BAG_KEY_PLURALS_START && key <= ResPluralsValue.BAG_KEY_PLURALS_END) {
            return new ResPluralsValue(parentVal, items);
        }
        return new ResStyleValue(parentVal, items, this);
    }

    public ResReferenceValue newReference(int resID, String rawValue) {
        return newReference(resID, rawValue, false);
    }

    public ResReferenceValue newReference(int resID, String rawValue, boolean theme) {
        return new ResReferenceValue(mPackage, resID, rawValue, theme);
    }
}
