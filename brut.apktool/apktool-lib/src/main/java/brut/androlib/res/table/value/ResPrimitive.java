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
import brut.androlib.exceptions.AndrolibException;
import brut.androlib.res.table.ResEntry;
import brut.androlib.res.xml.ResXmlEncodable;
import brut.androlib.res.xml.ResXmlEncoders;
import brut.androlib.res.xml.ValuesXmlSerializable;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;

public class ResPrimitive extends ResItem implements ResXmlEncodable, ValuesXmlSerializable {
    private static final Logger LOGGER = Logger.getLogger(ResPrimitive.class.getName());

    public static final ResPrimitive EMPTY = new ResPrimitive(TypedValue.TYPE_NULL, TypedValue.DATA_NULL_EMPTY);
    public static final ResPrimitive FALSE = new ResPrimitive(TypedValue.TYPE_INT_BOOLEAN, 0);
    public static final ResPrimitive TRUE = new ResPrimitive(TypedValue.TYPE_INT_BOOLEAN, 0xFFFFFFFF);

    private final int mType;
    private final int mData;

    public ResPrimitive(int type, int data) {
        mType = type;
        mData = data;
    }

    public int getType() {
        return mType;
    }

    public int getData() {
        return mData;
    }

    @Override
    public String encodeAsResXmlValue() {
        return ResXmlEncoders.coerceToString(mType, mData);
    }

    @Override
    public void serializeToValuesXml(XmlSerializer serial, ResEntry entry)
            throws AndrolibException, IOException {
        String type = entry.getTypeName();

        // Specify format for <item> tags when the resource type doesn't
        // directly support this primitive format.
        String format = getFormat();
        boolean asItem;
        if (format != null) {
            Set<String> standardFormats = STANDARD_TYPE_FORMATS.get(type);
            asItem = standardFormats == null || !standardFormats.contains(format);
        } else {
            asItem = false;
        }

        String tagName = asItem ? "item" : type;
        serial.startTag(null, tagName);
        if (asItem) {
            serial.attribute(null, "type", type);
        }
        serial.attribute(null, "name", entry.getName());
        if (asItem) {
            serial.attribute(null, "format", format);
        }
        serial.text(encodeAsResXmlValue());
        serial.endTag(null, tagName);
    }

    @Override
    public String getFormat() {
        switch (mType) {
            case TypedValue.TYPE_FLOAT:
                return "float";
            case TypedValue.TYPE_DIMENSION:
                return "dimension";
            case TypedValue.TYPE_FRACTION:
                return "fraction";
            case TypedValue.TYPE_INT_BOOLEAN:
                return "boolean";
            default:
                if (mType >= TypedValue.TYPE_FIRST_COLOR_INT && mType <= TypedValue.TYPE_LAST_COLOR_INT) {
                    return "color";
                }
                if (mType >= TypedValue.TYPE_FIRST_INT && mType <= TypedValue.TYPE_LAST_INT) {
                    return "integer";
                }
                if (mType == TypedValue.TYPE_NULL && mData == TypedValue.DATA_NULL_EMPTY) {
                    return null;
                }
                LOGGER.warning(String.format("Unexpected value type: 0x%02x", mType));
                return null;
        }
    }

    @Override
    public String toString() {
        return String.format("ResPrimitive{type=0x%02x, data=0x%08x}", mType, mData);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof ResPrimitive) {
            ResPrimitive other = (ResPrimitive) obj;
            return mType == other.mType
                    && mData == other.mData;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mType, mData);
    }
}
