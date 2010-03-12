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

import brut.androlib.AndrolibException;
import brut.androlib.res.data.ResPackage;
import brut.androlib.res.data.ResTable;
import brut.androlib.res.data.ResType;
import brut.androlib.res.jni.JniBagItem;
import brut.androlib.res.jni.JniEntry;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public class ResValueFactory {
    private final ResPackage mPackage;

    public ResValueFactory(ResPackage pakage_) {
        this.mPackage = pakage_;
    }

    public ResScalarValue factory(String string) {
        if (string.isEmpty()) {
            return new ResStringValue(string);
        }
        char c = string.charAt(0);
        if (c == '@' || c == '?') {
            return newReference(
                Integer.parseInt(string.substring(1)), c == '?');
        }
        if (c == '#') {
            return new ResColorValue(
                (int) Long.parseLong(string.substring(1), 16));
        }
        try {
            if (string.startsWith("0x")) {
                return new ResIntValue(
                    (int) Long.parseLong(string.substring(2), 16));
            }
            return new ResIntValue(Integer.parseInt(string));
        } catch (NumberFormatException ex) {}
        return new ResStringValue(string);
    }
    
    public ResValue factory(JniEntry entry)
            throws AndrolibException {
        if ("id".equals(entry.type)) {
            return new ResIdValue();
        }
        switch (entry.valueType) {
            case TYPE_BAG:
                return bagFactory(entry);
            case TYPE_REFERENCE:
                return newReference(entry.intVal);
            case TYPE_ATTRIBUTE:
                return newReference(entry.intVal, true);
            case TYPE_INT_BOOLEAN:
                return new ResBoolValue(entry.boolVal);
            case TYPE_INT_DEC:
            case TYPE_INT_HEX:
                return new ResIntValue(entry.intVal);
            case TYPE_FLOAT:
                return new ResFloatValue(entry.floatVal);
            case TYPE_INT_COLOR_ARGB4:
            case TYPE_INT_COLOR_ARGB8:
            case TYPE_INT_COLOR_RGB4:
            case TYPE_INT_COLOR_RGB8:
                return new ResColorValue(entry.intVal);
            case TYPE_STRING:
                if (entry.strVal.startsWith("res/")) {
                    return new ResFileValue(entry.strVal);
                }
            case TYPE_DIMENSION:
            case TYPE_FRACTION:
                return new ResStringValue(entry.strVal);
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

    private final static int TYPE_NULL = 0x00;
    private final static int TYPE_REFERENCE = 0x01;
    private final static int TYPE_ATTRIBUTE = 0x02;
    private final static int TYPE_STRING = 0x03;
    private final static int TYPE_FLOAT = 0x04;
    private final static int TYPE_DIMENSION = 0x05;
    private final static int TYPE_FRACTION = 0x06;
    private final static int TYPE_INT_DEC = 0x10;
    private final static int TYPE_INT_HEX = 0x11;
    private final static int TYPE_INT_BOOLEAN = 0x12;
    private final static int TYPE_INT_COLOR_ARGB8 = 0x1c;
    private final static int TYPE_INT_COLOR_RGB8 = 0x1d;
    private final static int TYPE_INT_COLOR_ARGB4 = 0x1e;
    private final static int TYPE_INT_COLOR_RGB4 = 0x1f;

    private final static int TYPE_BAG = -0x01;
}
