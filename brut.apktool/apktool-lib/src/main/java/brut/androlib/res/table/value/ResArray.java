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
import brut.androlib.res.table.ResEntrySpec;
import brut.androlib.res.xml.ResXmlEncodable;
import brut.androlib.res.xml.ValuesXmlSerializable;
import com.google.common.collect.Sets;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;

public class ResArray extends ResBag implements ValuesXmlSerializable {
    private static final Logger LOGGER = Logger.getLogger(ResArray.class.getName());

    private static final Set<String> ALLOWED_ARRAY_TYPES = Sets.newHashSet("string", "integer");

    private final ResItem[] mItems;

    public ResArray(ResReference parent, ResItem[] items) {
        super(parent);
        mItems = items;
    }

    public static ResArray parse(ResReference parent, RawItem[] rawItems) {
        ResItem[] items = new ResItem[rawItems.length];

        for (int i = 0; i < rawItems.length; i++) {
            items[i] = rawItems[i].getValue();
        }

        return new ResArray(parent, items);
    }

    @Override
    public void serializeToValuesXml(XmlSerializer serial, ResEntry entry)
            throws AndrolibException, IOException {
        String tagName = resolveTagName();
        serial.startTag(null, tagName);
        serial.attribute(null, "name", entry.getName());

        // Check if this string-array needs formatted="false".
        if (tagName.equals("string-array")) {
            for (ResItem value : mItems) {
                // Only check strings, ignore references.
                if (value instanceof ResString
                        && ((ResString) value).hasMultipleNonPositionalSubstitutions()) {
                    serial.attribute(null, "formatted", "false");
                    break;
                }
            }
        }

        for (ResItem value : mItems) {
            String body;
            if (value instanceof ResString) {
                body = ((ResString) value).encodeAsResXmlItemValueUnescaped();
            } else if (value instanceof ResXmlEncodable) {
                body = ((ResXmlEncodable) value).encodeAsResXmlItemValue();
            } else {
                LOGGER.warning("Unexpected array value: " + value);
                continue;
            }

            serial.startTag(null, "item");
            serial.text(body);
            serial.endTag(null, "item");
        }

        serial.endTag(null, tagName);
    }

    private String resolveTagName() throws AndrolibException {
        String type = null;

        for (ResItem value : mItems) {
            String itemType = null;

            if (value instanceof ResReference) {
                ResEntrySpec spec = ((ResReference) value).resolve();
                if (spec == null) {
                    continue;
                }

                itemType = spec.getTypeName();
            } else if (value instanceof ValuesXmlSerializable) {
                itemType = ((ValuesXmlSerializable) value).getFormat();
            } else {
                LOGGER.warning("Unexpected array value: " + value);
                continue;
            }

            if (itemType != null && ALLOWED_ARRAY_TYPES.contains(itemType)
                    && (type == null || type.equals(itemType))) {
                type = itemType;
            } else {
                type = null;
                break;
            }
        }

        return type != null ? type + "-array" : "array";
    }

    @Override
    public String toString() {
        return String.format("ResArray{parent=%s, items=%s}", mParent, mItems);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof ResArray) {
            ResArray other = (ResArray) obj;
            return Objects.equals(mParent, other.mParent)
                    && Objects.equals(mItems, other.mItems);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mParent, mItems);
    }
}
