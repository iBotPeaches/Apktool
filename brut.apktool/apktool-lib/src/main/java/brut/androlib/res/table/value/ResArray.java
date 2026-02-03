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
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.util.Arrays;

public class ResArray extends ResBag {
    private final ResItem[] mItems;

    public ResArray(ResReference parent, ResItem[] items) {
        super(parent);
        assert items != null;
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
    public void serializeToValuesXml(XmlSerializer serial, ResEntry entry) throws AndrolibException, IOException {
        String format = resolveFormat();

        // It's safe to use the format as the array type since the only typed arrays are string-array and integer-array.
        String tagName = "array";
        if (format != null) {
            switch (format) {
                case "string":
                case "integer":
                    tagName = format + "-" + tagName;
                    break;
            }
        }

        serial.startTag(null, tagName);
        serial.attribute(null, "name", entry.getName());

        for (ResItem value : mItems) {
            serial.startTag(null, "item");
            serial.text(value.toXmlTextValue());
            serial.endTag(null, "item");
        }

        serial.endTag(null, tagName);
    }

    private String resolveFormat() {
        String format = null;

        for (ResItem value : mItems) {
            String itemFormat = null;

            if (value instanceof ResReference) {
                // The reference format is ambiguous. Since the only typed arrays are string-array and integer-array,
                // we can infer a more specific format from the type of the referenced entry spec without mapping it
                // explicitly to a format.
                try {
                    ResEntrySpec spec = ((ResReference) value).resolve();
                    if (spec != null) {
                        itemFormat = spec.getTypeSpec().getName();
                    }
                } catch (AndrolibException ignored) {
                }
            } else {
                itemFormat = value.getFormat();
            }

            // Ignore @null and @empty.
            if (itemFormat == null) {
                continue;
            }

            if (format == null) {
                format = itemFormat;
            } else if (!format.equals(itemFormat)) {
                // Items with differing formats imply the array is generic.
                format = null;
                break;
            }
        }

        return format;
    }

    @Override
    public String toString() {
        return String.format("ResArray{items=%s}", Arrays.toString(mItems));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof ResArray) {
            ResArray other = (ResArray) obj;
            return Arrays.equals(mItems, other.mItems);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(mItems);
    }
}
