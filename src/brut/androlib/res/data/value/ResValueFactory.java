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
import brut.androlib.res.jni.JniBagItem;
import brut.androlib.res.jni.JniEntry;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

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
    
    public ResValue factory(JniEntry entry)
            throws AndrolibException {
        switch (entry.valueType) {
            case TYPE_BAG:
                return bagFactory(entry);
            case TypedValue.TYPE_REFERENCE:
                return newReference(entry.intVal);
            case TypedValue.TYPE_ATTRIBUTE:
                return newReference(entry.intVal, true);
            case TypedValue.TYPE_INT_BOOLEAN:
                return new ResBoolValue(entry.boolVal);
            case TypedValue.TYPE_INT_DEC:
            case TypedValue.TYPE_INT_HEX:
                return new ResIntValue(entry.intVal);
            case TypedValue.TYPE_FLOAT:
                return new ResFloatValue(entry.floatVal);
            case TypedValue.TYPE_INT_COLOR_ARGB4:
            case TypedValue.TYPE_INT_COLOR_ARGB8:
            case TypedValue.TYPE_INT_COLOR_RGB4:
            case TypedValue.TYPE_INT_COLOR_RGB8:
                return new ResColorValue(entry.intVal);
            case TypedValue.TYPE_STRING:
                if (entry.strVal.startsWith("res/")) {
                    return new ResFileValue(entry.strVal);
                }
            case TypedValue.TYPE_DIMENSION:
                return new ResStringValue(entry.strVal, "dimen");
            case TypedValue.TYPE_FRACTION:
                return new ResStringValue(entry.strVal, "fraction");
        }
        throw new AndrolibException(String.format(
            "Unknown value type for %s/%s: ",
            entry.type, entry.name, String.valueOf(entry.valueType)));
    }

    private ResValue bagFactory(JniEntry entry)
            throws AndrolibException {
        ResReferenceValue parent = newReference(entry.bagParent);
        Map<ResReferenceValue, ResScalarValue> items =
            convertItems(entry.bagItems);
        String type = entry.type;

        if ("array".equals(type)) {
            return new ResArrayValue(parent, items);
        }
        if ("style".equals(type)) {
            return new ResStyleValue(parent, items);
        }
        if ("plurals".equals(type)) {
            return new ResPluralsValue(parent, items);
        }
        if ("attr".equals(type)) {
            return ResAttrFactory.factory(parent, items);
        }
        return new ResBagValue(parent, items);
    }

    private ResReferenceValue newReference(int resID) {
        return newReference(resID, false);
    }

    private ResReferenceValue newReference(int resID, boolean theme) {
        return new ResReferenceValue(mPackage, resID, theme);
    }

    private Map<ResReferenceValue, ResScalarValue> convertItems(
            JniBagItem[] jniItems) throws AndrolibException {
        Map<ResReferenceValue, ResScalarValue> items =
            new LinkedHashMap<ResReferenceValue, ResScalarValue>();
        for (int i = 0; i < jniItems.length; i++) {
            JniBagItem jniItem = jniItems[i];
            items.put(newReference(jniItem.resID),
                (ResScalarValue) factory(jniItem.entry));
        }
        return items;
    }

    private static Integer parseInt(String s) {
        return parseInt(s, true);
    }

    private static Integer parseInt(String s, boolean hex) {
        if (s.startsWith("0x")) {
            s = s.substring(2);
            hex = true;
        } else if (decPattern.matcher(s).matches()) {
            return Integer.parseInt(s);
        }
        if (hex && hexPattern.matcher(s).matches()) {
            return (int) Long.parseLong(s, 16);
        }
        return null;
    }

    private final static Pattern decPattern =
        Pattern.compile("-?(?:[0-2]|)\\d{1,9}");
    private final static Pattern hexPattern =
        Pattern.compile("-?[0-9a-fA-F]{1,8}");
    private final static Pattern resIdPattern =
        Pattern.compile("\\+?(?:(.+?):|)([^:]+?)/(.+?)");

    private final static int TYPE_BAG = -0x01;
}
