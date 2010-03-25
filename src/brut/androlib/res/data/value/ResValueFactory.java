/*
 *  Copyright 2010 Ryszard Wiśniewski <brut.alll@gmail.com>.
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
 *  under the License.
 */

package brut.androlib.res.data.value;

import android.util.TypedValue;
import brut.androlib.AndrolibException;
import brut.androlib.res.data.ResPackage;
import java.util.Map;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public class ResValueFactory {
    private final ResPackage mPackage;

    public ResValueFactory(ResPackage pakage_) {
        this.mPackage = pakage_;
    }

    public ResScalarValue factory(int type, int value)
            throws AndrolibException {
        switch (type) {
            case TypedValue.TYPE_REFERENCE:
                return newReference(value);
            case TypedValue.TYPE_ATTRIBUTE:
                return newReference(value, true);
            case TypedValue.TYPE_FLOAT:
                return new ResFloatValue(Float.intBitsToFloat(value));
            case TypedValue.TYPE_DIMENSION:
                return new ResDimenValue(value);
            case TypedValue.TYPE_FRACTION:
                return new ResFractionValue(value);
            case TypedValue.TYPE_INT_BOOLEAN:
                return new ResBoolValue(value != 0);
        }

        if (type >= TypedValue.TYPE_FIRST_COLOR_INT
                && type <= TypedValue.TYPE_LAST_COLOR_INT) {
            return new ResColorValue(value);
        }
        if (type >= TypedValue.TYPE_FIRST_INT
                && type <= TypedValue.TYPE_LAST_INT) {
            return new ResIntValue(value);
        }

        throw new AndrolibException("Invalid value type: "+ type);
    }

    public ResValue factory(String value) {
        if (value.startsWith("res/")) {
            return new ResFileValue(value);
        }
        return new ResStringValue(value);
    }

    public ResBagValue bagFactory(String type, int parent,
            Map<ResReferenceValue, ResScalarValue> items) {
        ResReferenceValue parentVal = newReference(parent);

        if ("array".equals(type)) {
            return new ResArrayValue(parentVal, items);
        }
        if ("style".equals(type)) {
            return new ResStyleValue(parentVal, items);
        }
        if ("plurals".equals(type)) {
            return new ResPluralsValue(parentVal, items);
        }
        if ("attr".equals(type)) {
            return ResAttrFactory.factory(parentVal, items);
        }
        return new ResBagValue(parentVal, items);
    }

    public ResReferenceValue newReference(int resID) {
        return newReference(resID, false);
    }

    public ResReferenceValue newReference(int resID, boolean theme) {
        return new ResReferenceValue(mPackage, resID, theme);
    }
}
