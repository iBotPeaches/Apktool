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
import brut.androlib.res.table.ResId;
import brut.androlib.res.table.ResPackage;
import brut.androlib.res.xml.ResStringEncoder;
import brut.common.Log;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.util.Objects;

public class ResAttribute extends ResBag {
    private static final String TAG = ResAttribute.class.getName();

    private static final int ATTR_TYPE = 0x01000000;
    private static final int ATTR_MIN = 0x01000001;
    private static final int ATTR_MAX = 0x01000002;
    private static final int ATTR_L10N = 0x01000003;

    public static final int ATTR_TYPE_ANY = 0x0000FFFF;
    public static final int ATTR_TYPE_REFERENCE = 1 << 0; // 0x01
    public static final int ATTR_TYPE_STRING = 1 << 1; // 0x02
    public static final int ATTR_TYPE_INTEGER = 1 << 2; // 0x04
    public static final int ATTR_TYPE_BOOLEAN = 1 << 3; // 0x08
    public static final int ATTR_TYPE_COLOR = 1 << 4; // 0x10
    public static final int ATTR_TYPE_FLOAT = 1 << 5; // 0x20
    public static final int ATTR_TYPE_DIMENSION = 1 << 6; // 0x40
    public static final int ATTR_TYPE_FRACTION = 1 << 7; // 0x80
    public static final int ATTR_TYPE_ENUM = 1 << 16; // 0x00010000
    public static final int ATTR_TYPE_FLAGS = 1 << 17; // 0x00020000

    private static final int[] ATTR_TYPE_MASKS = {
        ATTR_TYPE_STRING, ATTR_TYPE_INTEGER, ATTR_TYPE_BOOLEAN, ATTR_TYPE_COLOR, ATTR_TYPE_FLOAT,
        ATTR_TYPE_DIMENSION, ATTR_TYPE_FRACTION, ATTR_TYPE_REFERENCE
    };
    private static final String[] ATTR_TYPE_NAMES = {
        "string", "integer", "boolean", "color", "float", "dimension", "fraction", "reference"
    };

    private static final int ATTR_L10N_NOT_REQUIRED = 0;
    private static final int ATTR_L10N_SUGGESTED = 1;

    public static final ResAttribute DEFAULT = new ResAttribute(
        null, ATTR_TYPE_ANY, Integer.MIN_VALUE, Integer.MAX_VALUE, ATTR_L10N_NOT_REQUIRED);

    protected int mType; // might be updated later
    protected final int mMin;
    protected final int mMax;
    protected final int mL10n;

    public ResAttribute(ResReference parent, int type, int min, int max, int l10n) {
        super(parent);
        mType = type;
        mMin = min;
        mMax = max;
        mL10n = l10n;
    }

    public static ResAttribute parse(ResReference parent, RawItem[] rawItems) {
        int type = ATTR_TYPE_ANY;
        int min = Integer.MIN_VALUE;
        int max = Integer.MAX_VALUE;
        int l10n = ATTR_L10N_NOT_REQUIRED;

        int i = 0, n = rawItems.length;
        for (; i < n; i++) {
            RawItem rawItem = rawItems[i];
            ResPrimitive value = (ResPrimitive) rawItem.getValue();

            switch (rawItem.getKey()) {
                case ATTR_TYPE:
                    type = value.getData();
                    continue;
                case ATTR_MIN:
                    min = value.getData();
                    continue;
                case ATTR_MAX:
                    max = value.getData();
                    continue;
                case ATTR_L10N:
                    l10n = value.getData();
                    continue;
            }
            break;
        }
        if (i == n) {
            // The attribute doesn't have any symbols.
            return new ResAttribute(parent, type, min, max, l10n);
        }

        Symbol[] symbols = new Symbol[n - i];
        ResPackage pkg = parent.getPackage();

        for (int j = 0; i < n; i++, j++) {
            RawItem rawItem = rawItems[i];
            // The name of the symbol as a reference to a generated
            // ID resource value.
            int nameId = rawItem.getKey();
            ResReference name = new ResReference(pkg, ResId.of(nameId));
            ResPrimitive value = (ResPrimitive) rawItem.getValue();

            symbols[j] = new Symbol(name, value);
        }

        if ((type & ATTR_TYPE_ENUM) != 0) {
            return new ResEnum(parent, type, min, max, l10n, symbols);
        } else if ((type & ATTR_TYPE_FLAGS) != 0) {
            return new ResFlags(parent, type, min, max, l10n, symbols);
        } else {
            Log.w(TAG, "Invalid attribute type: 0x%08x", type);
            return new ResAttribute(parent, type, min, max, l10n);
        }
    }

    public static class Symbol {
        private final ResReference mKey;
        private final ResPrimitive mValue;

        public Symbol(ResReference key, ResPrimitive value) {
            assert key != null && value != null;
            mKey = key;
            mValue = value;
        }

        public ResReference getKey() {
            return mKey;
        }

        public ResPrimitive getValue() {
            return mValue;
        }
    }

    public void addValueType(int valueType) {
        if ((mType & ATTR_TYPE_ANY) == ATTR_TYPE_ANY) {
            return;
        }
        switch (valueType) {
            case TYPE_NULL:
            case TYPE_REFERENCE:
            case TYPE_DYNAMIC_REFERENCE:
            case TYPE_ATTRIBUTE:
            case TYPE_DYNAMIC_ATTRIBUTE:
                mType |= ATTR_TYPE_REFERENCE;
                return;
            case TYPE_STRING:
                mType |= ATTR_TYPE_STRING;
                return;
            case TYPE_FLOAT:
                mType |= ATTR_TYPE_FLOAT;
                return;
            case TYPE_DIMENSION:
                mType |= ATTR_TYPE_DIMENSION;
                return;
            case TYPE_FRACTION:
                mType |= ATTR_TYPE_FRACTION;
                return;
            case TYPE_INT_BOOLEAN:
                mType |= ATTR_TYPE_BOOLEAN;
                return;
            default:
                if (valueType >= TYPE_FIRST_COLOR_INT && valueType <= TYPE_LAST_COLOR_INT) {
                    mType |= ATTR_TYPE_COLOR;
                } else if (valueType >= TYPE_FIRST_INT && valueType <= TYPE_LAST_INT) {
                    mType |= ATTR_TYPE_INTEGER;
                }
                return;
        }
    }

    public boolean hasSymbolsForValue(ResItem value) {
        return getSymbolsForValue(value) != null;
    }

    protected Symbol[] getSymbolsForValue(ResItem value) {
        // Stub for attribute types with symbols.
        return null;
    }

    public String formatAsTextValue(ResItem value) throws AndrolibException {
        return formatValue(value, false);
    }

    public String formatAsAttributeValue(ResItem value) throws AndrolibException {
        return formatValue(value, true);
    }

    private String formatValue(ResItem value, boolean asAttrValue) throws AndrolibException {
        String formatted = formatValueFromSymbols(value);
        if (formatted != null) {
            return formatted;
        }

        // Ensure strings are escaped for attribute values according to the attribute type.
        if (asAttrValue && value instanceof ResString) {
            CharSequence strValue = ((ResString) value).getValue();
            return ResStringEncoder.encodeAttributeValue(strValue, mType);
        }

        return asAttrValue ? value.toXmlAttributeValue() : value.toXmlTextValue();
    }

    protected String formatValueFromSymbols(ResItem value) throws AndrolibException {
        // Stub for attribute types with symbols.
        return null;
    }

    @Override
    public void serializeToValuesXml(XmlSerializer serial, ResEntry entry) throws AndrolibException, IOException {
        String tagName = "attr";
        serial.startTag(null, tagName);
        serial.attribute(null, "name", entry.getName());
        String format = renderFormat();
        if (format != null) {
            serial.attribute(null, "format", format);
        }
        if (mMin != Integer.MIN_VALUE) {
            serial.attribute(null, "min", Integer.toString(mMin));
        }
        if (mMax != Integer.MAX_VALUE) {
            serial.attribute(null, "max", Integer.toString(mMax));
        }
        if (mL10n == ATTR_L10N_SUGGESTED) {
            serial.attribute(null, "localization", "suggested");
        }
        serializeSymbolsToValuesXml(serial, entry);
        serial.endTag(null, tagName);
    }

    private String renderFormat() {
        if ((mType & ATTR_TYPE_ANY) == ATTR_TYPE_ANY) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ATTR_TYPE_MASKS.length; i++) {
            if ((mType & ATTR_TYPE_MASKS[i]) != 0) {
                if (sb.length() > 0) {
                    sb.append('|');
                }
                sb.append(ATTR_TYPE_NAMES[i]);
            }
        }
        if (sb.length() == 0) {
            return null;
        }
        return sb.toString();
    }

    protected void serializeSymbolsToValuesXml(XmlSerializer serial, ResEntry entry)
            throws AndrolibException, IOException {
        // Stub for attribute types with symbols.
    }

    @Override
    public String toString() {
        return String.format("ResAttribute{type=0x%04x, min=%s, max=%s, l10n=%s}", mType, mMin, mMax, mL10n);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof ResAttribute) {
            ResAttribute other = (ResAttribute) obj;
            return mType == other.mType
                && mMin == other.mMin
                && mMax == other.mMax
                && mL10n == other.mL10n;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mType, mMin, mMax, mL10n);
    }
}
