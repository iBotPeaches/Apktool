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
import brut.androlib.res.table.ResEntry;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;

public class ResPrimitive extends ResItem {
    private static final Logger LOGGER = Logger.getLogger(ResPrimitive.class.getName());

    // Complex data
    private static final int COMPLEX_UNIT_MASK = 0xF;
    private static final int COMPLEX_RADIX_SHIFT = 4;
    private static final int COMPLEX_RADIX_MASK = 0x3;
    private static final int COMPLEX_MANTISSA_SHIFT = 8;
    private static final int COMPLEX_MANTISSA_MASK = 0xFFFFFF;
    private static final float MANTISSA_MULT = 1.0f / (1 << COMPLEX_MANTISSA_SHIFT);
    private static final float[] RADIX_MULTS = {
        MANTISSA_MULT, 1.0f / (1 << 7) * MANTISSA_MULT,
        1.0f / (1 << 15) * MANTISSA_MULT, 1.0f / (1 << 23) * MANTISSA_MULT
    };

    // Complex units in TYPE_DIMENSION
    private static final int COMPLEX_UNIT_PX = 0;
    private static final int COMPLEX_UNIT_DIP = 1;
    private static final int COMPLEX_UNIT_SP = 2;
    private static final int COMPLEX_UNIT_PT = 3;
    private static final int COMPLEX_UNIT_IN = 4;
    private static final int COMPLEX_UNIT_MM = 5;

    // Complex units in TYPE_FRACTION
    private static final int COMPLEX_UNIT_FRACTION = 0;
    private static final int COMPLEX_UNIT_FRACTION_PARENT = 1;

    public static final ResPrimitive NULL = new ResPrimitive(TYPE_NULL, DATA_NULL_UNDEFINED);
    public static final ResPrimitive EMPTY = new ResPrimitive(TYPE_NULL, DATA_NULL_EMPTY);
    public static final ResPrimitive FALSE = new ResPrimitive(TYPE_INT_BOOLEAN, 0);
    public static final ResPrimitive TRUE = new ResPrimitive(TYPE_INT_BOOLEAN, 0xFFFFFFFF);

    private final int mData;

    public ResPrimitive(int type, int data) {
        super(type);
        mData = data;
    }

    public int getData() {
        return mData;
    }

    @Override
    public String toXmlTextValue() {
        switch (mType) {
            case TYPE_NULL:
                return mData == DATA_NULL_EMPTY ? "@empty" : "@null";
            case TYPE_FLOAT:
                return floatToString(Float.intBitsToFloat(mData));
            case TYPE_DIMENSION: {
                String value = floatToString(complexToFloat(mData));
                int unitType = mData & COMPLEX_UNIT_MASK;
                switch (unitType) {
                    case COMPLEX_UNIT_PX:
                        value += "px";
                        break;
                    case COMPLEX_UNIT_DIP:
                        value += "dp";
                        break;
                    case COMPLEX_UNIT_SP:
                        value += "sp";
                        break;
                    case COMPLEX_UNIT_PT:
                        value += "pt";
                        break;
                    case COMPLEX_UNIT_IN:
                        value += "in";
                        break;
                    case COMPLEX_UNIT_MM:
                        value += "mm";
                        break;
                    default:
                        LOGGER.warning("Unexpected value unit: " + unitType);
                        value += "??";
                        break;
                }
                return value;
            }
            case TYPE_FRACTION: {
                String value = floatToString(complexToFloat(mData) * 100);
                int unitType = mData & COMPLEX_UNIT_MASK;
                switch (unitType) {
                    case COMPLEX_UNIT_FRACTION:
                        value += "%";
                        break;
                    case COMPLEX_UNIT_FRACTION_PARENT:
                        value += "%p";
                        break;
                    default:
                        LOGGER.warning("Unexpected value unit: " + unitType);
                        value += "??";
                        break;
                }
                return value;
            }
            case TYPE_INT_BOOLEAN:
                return mData != 0 ? "true" : "false";
        }
        if (mType >= TYPE_FIRST_COLOR_INT && mType <= TYPE_LAST_COLOR_INT) {
            switch (mType) {
                default:
                case TYPE_INT_COLOR_ARGB8:
                    return String.format("#%08x", mData);
                case TYPE_INT_COLOR_RGB8:
                    return String.format("#%06x", mData & 0xFFFFFF);
                case TYPE_INT_COLOR_ARGB4:
                    return String.format("#%x%x%x%x",
                        (mData >>> 28) & 0xF, (mData >>> 20) & 0xF,
                        (mData >>> 12) & 0xF, (mData >>> 4) & 0xF);
                case TYPE_INT_COLOR_RGB4:
                    return String.format("#%x%x%x",
                        (mData >>> 20) & 0xF, (mData >>> 12) & 0xF,
                        (mData >>> 4) & 0xF);
            }
        }
        if (mType >= TYPE_FIRST_INT && mType <= TYPE_LAST_INT) {
            switch (mType) {
                default:
                case TYPE_INT_DEC:
                    return Integer.toString(mData);
                case TYPE_INT_HEX:
                    return String.format("0x%x", mData);
            }
        }
        LOGGER.warning(String.format("Unexpected value type: 0x%02x", mType));
        return "";
    }

    private static String floatToString(float value) {
        // Use one decimal to show it's a float for exact integers.
        if (value == (long) value) {
            return String.format(Locale.ROOT, "%.1f", value);
        }
        // Use Java's default minimal string representation.
        return Float.toString(value);
    }

    private static float complexToFloat(int complex) {
        return (complex & (COMPLEX_MANTISSA_MASK << COMPLEX_MANTISSA_SHIFT))
                * RADIX_MULTS[(complex >> COMPLEX_RADIX_SHIFT) & COMPLEX_RADIX_MASK];
    }

    @Override
    public void serializeToValuesXml(XmlSerializer serial, ResEntry entry)
            throws AndrolibException, IOException {
        String type = entry.getTypeName();

        // Specify format for <item> tags when the resource type doesn't
        // directly support this value's format.
        Set<String> stdFormats = STANDARD_TYPE_FORMATS.get(type);
        String format = stdFormats != null ? getFormat() : null;
        boolean asItem = format != null && !stdFormats.contains(format);

        String tagName = asItem ? "item" : type;
        serial.startTag(null, tagName);
        if (asItem) {
            serial.attribute(null, "type", type);
        }
        serial.attribute(null, "name", entry.getName());
        if (asItem) {
            serial.attribute(null, "format", format);
        }
        serial.text(toXmlTextValue());
        serial.endTag(null, tagName);
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
