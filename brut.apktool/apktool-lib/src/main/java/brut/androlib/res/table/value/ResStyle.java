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

import brut.androlib.Config;
import brut.androlib.exceptions.AndrolibException;
import brut.androlib.exceptions.UndefinedResObjectException;
import brut.androlib.res.table.ResEntry;
import brut.androlib.res.table.ResEntrySpec;
import brut.androlib.res.table.ResId;
import brut.androlib.res.table.ResPackage;
import brut.androlib.res.xml.ValuesXmlSerializable;
import org.apache.commons.lang3.tuple.Pair;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;

public class ResStyle extends ResBag implements ValuesXmlSerializable {
    private static final Logger LOGGER = Logger.getLogger(ResStyle.class.getName());

    private final Item[] mItems;

    public ResStyle(ResReference parent, Item[] items) {
        super(parent);
        mItems = items;
    }

    public static ResStyle parse(ResReference parent, RawItem[] rawItems) {
        Item[] items = new Item[rawItems.length];
        ResPackage pkg = parent.getPackage();

        for (int i = 0; i < rawItems.length; i++) {
            RawItem rawItem = rawItems[i];
            // A reference to the XML attribute.
            int keyId = rawItem.getKey();
            ResReference key = new ResReference(pkg, ResId.of(keyId));
            ResItem value = rawItem.getValue();

            items[i] = new Item(key, value);
        }

        return new ResStyle(parent, items);
    }

    public static class Item {
        private final ResReference mKey;
        private final ResItem mValue;

        public Item(ResReference key, ResItem value) {
            mKey = key;
            mValue = value;
        }

        public ResReference getKey() {
            return mKey;
        }

        public ResItem getValue() {
            return mValue;
        }
    }

    @Override
    public void serializeToValuesXml(XmlSerializer serial, ResEntry entry)
            throws AndrolibException, IOException {
        Config config = mParent.getPackage().getTable().getConfig();
        boolean skipDuplicates = !config.isAnalysisMode();

        serial.startTag(null, "style");
        serial.attribute(null, "name", entry.getName());
        if (mParent.resolve() != null) {
            serial.attribute(null, "parent", mParent.encodeAsResXmlAttrValue());
        } else if (entry.getName().indexOf('.') != -1) {
            serial.attribute(null, "parent", "");
        }

        Set<String> processedNames = new HashSet<>();
        for (Item item : mItems) {
            ResReference key = item.getKey();
            ResEntrySpec keySpec = key.resolve();
            ResItem value = item.getValue();

            // #2836 - Support skipping items if the resource cannot be identified.
            if (keySpec == null) {
                LOGGER.warning(String.format("null style reference: key=%s, value=%s", key, value));
                continue;
            }

            String name = keySpec.getFullName(entry.getPackage(), true);

            // #3400 - Skip duplicate items in styles.
            if (skipDuplicates && processedNames.contains(name)) {
                continue;
            }

            String body = null;
            try {
                // We need the attribute entry's value to format the item's value.
                ResValue keyDefValue =
                    keySpec.getPackage().getDefaultEntry(keySpec.getId()).getValue();

                if (keyDefValue instanceof ResAttribute) {
                    body = ((ResAttribute) keyDefValue).formatValue(value, true);
                } else {
                    LOGGER.warning("Unexpected style item key: " + keySpec);
                }
            } catch (UndefinedResObjectException ignored) {
            }
            if (body == null) {
                // Fall back to default attribute.
                body = ResAttribute.DEFAULT.formatValue(value, true);
            }
            if (body == null) {
                continue;
            }

            serial.startTag(null, "item");
            serial.attribute(null, "name", name);
            serial.text(body);
            serial.endTag(null, "item");

            processedNames.add(name);
        }

        serial.endTag(null, "style");
    }

    @Override
    public String toString() {
        return String.format("ResStyle{parent=%s, items=%s}", mParent, mItems);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof ResStyle) {
            ResStyle other = (ResStyle) obj;
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
