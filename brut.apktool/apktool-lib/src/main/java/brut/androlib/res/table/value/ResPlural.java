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
import brut.androlib.res.xml.ResStringEncoder;
import brut.common.Log;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.util.Arrays;

public class ResPlural extends ResBag {
    private static final String TAG = ResPlural.class.getName();

    private static final int ATTR_OTHER = 0x01000004;
    private static final int ATTR_ZERO = 0x01000005;
    private static final int ATTR_ONE = 0x01000006;
    private static final int ATTR_TWO = 0x01000007;
    private static final int ATTR_FEW = 0x01000008;
    private static final int ATTR_MANY = 0x01000009;

    private final RawItem[] mItems;

    public ResPlural(ResReference parent, RawItem[] items) {
        super(parent);
        assert items != null;
        mItems = items;
    }

    public static ResPlural parse(ResReference parent, RawItem[] rawItems) {
        return new ResPlural(parent, rawItems);
    }

    @Override
    public void serializeToValuesXml(XmlSerializer serial, ResEntry entry) throws AndrolibException, IOException {
        String tagName = "plurals";
        serial.startTag(null, tagName);
        serial.attribute(null, "name", entry.getName());

        for (RawItem item : mItems) {
            int key = item.getKey();
            String quantity;
            switch (key) {
                case ATTR_OTHER:
                    quantity = "other";
                    break;
                case ATTR_ZERO:
                    quantity = "zero";
                    break;
                case ATTR_ONE:
                    quantity = "one";
                    break;
                case ATTR_TWO:
                    quantity = "two";
                    break;
                case ATTR_FEW:
                    quantity = "few";
                    break;
                case ATTR_MANY:
                    quantity = "many";
                    break;
                default:
                    Log.w(TAG, "Invalid plurals key: 0x%08x", key);
                    continue;
            }

            ResItem value = item.getValue();
            String body = value.toXmlTextValue();
            if (!body.isEmpty()) {
                body = ResStringEncoder.normalizeFormatSpecifiers(body);
            }

            serial.startTag(null, "item");
            serial.attribute(null, "quantity", quantity);
            serial.text(body);
            serial.endTag(null, "item");
        }

        serial.endTag(null, tagName);
    }

    @Override
    public String toString() {
        return String.format("ResPlural{items=%s}", Arrays.toString(mItems));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof ResPlural) {
            ResPlural other = (ResPlural) obj;
            return Arrays.equals(mItems, other.mItems);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(mItems);
    }
}
