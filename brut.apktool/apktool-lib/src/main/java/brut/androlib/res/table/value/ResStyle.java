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
import brut.androlib.res.table.ResId;
import brut.androlib.res.table.ResPackage;
import brut.common.Log;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class ResStyle extends ResBag {
    private static final String TAG = ResStyle.class.getName();

    private final Item[] mItems;

    public ResStyle(ResReference parent, Item[] items) {
        super(parent);
        assert parent != null && items != null;
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
            assert key != null && value != null;
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
    public void resolveKeys() throws AndrolibException {
        ResPackage pkg = mParent.getPackage();
        boolean skipUnresolved = pkg.getTable().getConfig().isDecodeResolveLazy();

        for (Item item : mItems) {
            ResReference key = item.getKey();
            if (key.resolveEntry() != null) {
                continue;
            }

            ResId keyId = key.getResId();

            // #2836 - Skip item if the resource cannot be resolved.
            if (skipUnresolved || keyId.pkgId() != pkg.getId()) {
                Log.w(TAG, "Unresolved style reference: key=%s, value=%s", key, item.getValue());
                continue;
            }

            pkg.addEntrySpec(keyId.typeId(), keyId.entryId(), ResEntrySpec.DUMMY_PREFIX + keyId);
            pkg.addEntry(keyId.typeId(), keyId.entryId(), ResAttribute.DEFAULT);
        }
    }

    @Override
    public void serializeToValuesXml(XmlSerializer serial, ResEntry entry) throws AndrolibException, IOException {
        String tagName = "style";
        serial.startTag(null, tagName);
        serial.attribute(null, "name", entry.getName());
        if (mParent.resolve() != null) {
            serial.attribute(null, "parent", mParent.toXmlAttributeValue());
        } else if (entry.getName().indexOf('.') != -1) {
            serial.attribute(null, "parent", "");
        }

        ResPackage pkg = mParent.getPackage();
        boolean skipDuplicates = !pkg.getTable().getConfig().isAnalysisMode();
        Set<ResId> processedKeys = new HashSet<>();
        for (Item item : mItems) {
            ResReference key = item.getKey();
            ResEntry keyEntry = key.resolveEntry();
            if (keyEntry == null) {
                continue;
            }

            ResId keyId = key.getResId();

            // #3400 - Skip duplicate items in styles.
            if (skipDuplicates && processedKeys.contains(keyId)) {
                continue;
            }

            processedKeys.add(keyId);

            boolean includePackage = pkg.getGroup() != keyEntry.getPackage().getGroup();
            String keyName = (includePackage ? keyEntry.getPackage().getName() + ":" : "") + keyEntry.getName();

            ResItem value = item.getValue();
            String body;
            if (keyEntry.getValue() instanceof ResAttribute) {
                // Format the value with the attribute entry's value.
                ResAttribute keyValue = (ResAttribute) keyEntry.getValue();
                body = keyValue.formatAsTextValue(value);
            } else {
                Log.w(TAG, "Unexpected style item key: " + keyEntry);
                // Format the value with the default attribute.
                body = ResAttribute.DEFAULT.formatAsTextValue(value);
            }

            serial.startTag(null, "item");
            serial.attribute(null, "name", keyName);
            serial.text(body);
            serial.endTag(null, "item");
        }

        serial.endTag(null, tagName);
    }

    @Override
    public String toString() {
        return String.format("ResStyle{parent=%s, items=%s}", mParent, Arrays.toString(mItems));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof ResStyle) {
            ResStyle other = (ResStyle) obj;
            return mParent.equals(other.mParent)
                && Arrays.equals(mItems, other.mItems);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mParent, Arrays.hashCode(mItems));
    }
}
