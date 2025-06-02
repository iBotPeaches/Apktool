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
import brut.androlib.res.xml.ResXmlEncodable;
import brut.androlib.res.xml.ValuesXmlSerializable;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.util.Objects;
import java.util.logging.Logger;

public class ResAttribute extends ResBag implements ValuesXmlSerializable {
    private static final Logger LOGGER = Logger.getLogger(ResAttribute.class.getName());

    private static final int ATTR_TYPE = 0x01000000;
    private static final int ATTR_MIN = 0x01000001;
    private static final int ATTR_MAX = 0x01000002;
    private static final int ATTR_L10N = 0x01000003;

    private static final int TYPE_ANY = 0x0000FFFF;
    private static final int TYPE_REFERENCE = 1 << 0; // 0x01
    private static final int TYPE_STRING = 1 << 1; // 0x02
    private static final int TYPE_INT = 1 << 2; // 0x04
    private static final int TYPE_BOOL = 1 << 3; // 0x08
    private static final int TYPE_COLOR = 1 << 4; // 0x10
    private static final int TYPE_FLOAT = 1 << 5; // 0x20
    private static final int TYPE_DIMEN = 1 << 6; // 0x40
    private static final int TYPE_FRACTION = 1 << 7; // 0x80
    private static final int TYPE_ENUM = 1 << 16; // 0x00010000
    private static final int TYPE_FLAGS = 1 << 17; // 0x00020000

    private static final int L10N_NOT_REQUIRED = 0;
    private static final int L10N_SUGGESTED = 1;

    private static final int[] TYPE_MASKS = {
        TYPE_STRING, TYPE_INT, TYPE_BOOL, TYPE_COLOR, TYPE_FLOAT, TYPE_DIMEN, TYPE_FRACTION, TYPE_REFERENCE
    };
    private static final String[] TYPE_FORMATS = {
        "string", "integer", "boolean", "color", "float", "dimension", "fraction", "reference"
    };

    protected final int mType;
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
        int type = TYPE_ANY;
        int min = Integer.MIN_VALUE;
        int max = Integer.MAX_VALUE;
        int l10n = L10N_NOT_REQUIRED;

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
            // The attribute doesn't have any enum/flag symbols.
            return new ResAttribute(parent, type, min, max, l10n);
        }

        Symbol[] symbols = new Symbol[n - i];
        ResPackage pkg = parent.getPackage();

        for (int j = 0; i < n; i++, j++) {
            RawItem rawItem = rawItems[i];
            // The name of the enum/flag symbol as a reference to a
            // generated ID resource value.
            ResId nameId = ResId.of(rawItem.getKey());
            ResReference name = new ResReference(pkg, nameId, ResReference.Type.RESOURCE);
            ResPrimitive value = (ResPrimitive) rawItem.getValue();

            symbols[j] = new Symbol(name, value);

            // Exclude the generated ID resource values from output.
            // This is optional, but makes ids.xml cleaner.
            pkg.addSynthesizedEntry(nameId);
        }

        if ((type & TYPE_ENUM) != 0) {
            return new ResEnum(parent, type, min, max, l10n, symbols);
        } else if ((type & TYPE_FLAGS) != 0) {
            return new ResFlags(parent, type, min, max, l10n, symbols);
        } else {
            LOGGER.warning(String.format("Invalid attribute type: 0x%08x", type));
            return new ResAttribute(parent, type, min, max, l10n);
        }
    }

    public static class Symbol {
        private final ResReference mKey;
        private final ResPrimitive mValue;

        public Symbol(ResReference key, ResPrimitive value) {
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

    public String convertToResXmlFormat(ResItem value) throws AndrolibException {
        if (value instanceof ResXmlEncodable) {
            return ((ResXmlEncodable) value).encodeAsResXmlValue();
        }
        return null;
    }

    @Override
    public void serializeToValuesXml(XmlSerializer serial, ResEntry entry)
            throws AndrolibException, IOException {
        serial.startTag(null, "attr");
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
        if (mL10n == L10N_SUGGESTED) {
            serial.attribute(null, "localization", "suggested");
        }
        serializeItemsToResValuesXml(serial, entry);
        serial.endTag(null, "attr");
    }

    private String renderFormat() {
        if ((mType & TYPE_ANY) == 0) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < TYPE_MASKS.length; i++) {
            if ((mType & TYPE_MASKS[i]) != 0) {
                if (sb.length() > 0) {
                    sb.append('|');
                }
                sb.append(TYPE_FORMATS[i]);
            }
        }
        if (sb.length() == 0) {
            return null;
        }
        return sb.toString();
    }

    public void serializeItemsToResValuesXml(XmlSerializer serial, ResEntry entry)
            throws AndrolibException, IOException {
        // stub
    }

    @Override
    public String toString() {
        return String.format("ResAttribute{parent=%s, type=0x%04x, min=%d, max=%d, l10n=%d}",
            mParent, mType, mMin, mMax, mL10n);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof ResAttribute) {
            ResAttribute other = (ResAttribute) obj;
            return Objects.equals(mParent, other.mParent)
                    && mType == other.mType
                    && mMin == other.mMin
                    && mMax == other.mMax
                    && mL10n == other.mL10n;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mParent, mType, mMin, mMax, mL10n);
    }
}
