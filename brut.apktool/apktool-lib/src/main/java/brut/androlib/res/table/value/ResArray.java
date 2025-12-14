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
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.util.Objects;
import java.util.logging.Logger;

public class ResArray extends ResBag implements ValuesXmlSerializable {
    private static final Logger LOGGER = Logger.getLogger(ResArray.class.getName());

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
        // Since only string and integer arrays are supported,
        // it's safe to use the format as an array type.
        String format = resolveFormat();
        if (format != null) {
            switch (format) {
                case "string":
                case "integer":
                    break;
                default:
                    format = null;
                    break;
            }
        }

        String tagName = (format != null ? format + "-" : "") + "array";
        serial.startTag(null, tagName);
        serial.attribute(null, "name", entry.getName());

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

    private String resolveFormat() {
        String format = null;

        for (ResItem value : mItems) {
            String valueFormat = null;

            if (value instanceof ResReference) {
                // Try to get a more specific format from to the type of the
                // referenced entry spec.
                try {
                    ResEntrySpec spec = ((ResReference) value).resolve();
                    if (spec != null) {
                        // Since only string and integer arrays are supported,
                        // it's safe to compare with the referenecd type name.
                        valueFormat = spec.getTypeName();
                    }
                } catch (AndrolibException ignored) {
                }
            } else {
                valueFormat = value.getFormat();
            }

            // Ignore @null and @empty.
            if (valueFormat == null) {
                continue;
            }

            if (format == null) {
                format = valueFormat;
            } else if (!format.equals(valueFormat)) {
                // The items don't share the same format.
                format = null;
                break;
            }
        }

        return format;
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
