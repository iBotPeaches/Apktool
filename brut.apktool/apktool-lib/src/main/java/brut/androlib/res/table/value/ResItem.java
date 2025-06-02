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

import android.util.TypedValue;
import brut.androlib.res.table.ResId;
import brut.androlib.res.table.ResPackage;

import java.util.logging.Logger;

public abstract class ResItem extends ResValue {
    private static final Logger LOGGER = Logger.getLogger(ResItem.class.getName());

    public static ResItem parse(ResPackage pkg, int type, int data, String rawValue) {
        switch (type) {
            case TypedValue.TYPE_NULL:
                return data == TypedValue.DATA_NULL_EMPTY ? ResPrimitive.EMPTY : ResReference.NULL;
            case TypedValue.TYPE_REFERENCE:
            case TypedValue.TYPE_DYNAMIC_REFERENCE:
                return data != 0 || rawValue != null
                    ? new ResReference(pkg, ResId.of(data), rawValue, ResReference.Type.RESOURCE)
                    : ResReference.NULL;
            case TypedValue.TYPE_ATTRIBUTE:
            case TypedValue.TYPE_DYNAMIC_ATTRIBUTE:
                return data != 0 || rawValue != null
                    ? new ResReference(pkg, ResId.of(data), rawValue, ResReference.Type.ATTRIBUTE)
                    : ResReference.NULL;
            case TypedValue.TYPE_STRING:
                return new ResString(rawValue);
            case TypedValue.TYPE_FLOAT:
            case TypedValue.TYPE_DIMENSION:
            case TypedValue.TYPE_FRACTION:
                return new ResPrimitive(type, data);
            default:
                // Handle integer, boolean and color.
                if (type >= TypedValue.TYPE_FIRST_INT && type <= TypedValue.TYPE_LAST_INT) {
                    return new ResPrimitive(type, data);
                }
                LOGGER.warning(String.format("Invalid value type: 0x%02x", type));
                return null;
        }
    }
}
