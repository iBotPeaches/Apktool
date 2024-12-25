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
package brut.androlib.res.data.value;

import brut.androlib.exceptions.AndrolibException;
import brut.androlib.res.data.ResResource;
import brut.androlib.res.xml.ResValuesXmlSerializable;
import brut.androlib.res.xml.ResXmlEncoders;
import org.apache.commons.lang3.tuple.Pair;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;

public class ResPluralsValue extends ResBagValue implements ResValuesXmlSerializable {
    private static final String[] QUANTITY_MAP = { "other", "zero", "one", "two", "few", "many" };

    private static final int BAG_KEY_PLURALS_START = 0x01000004;

    private final ResScalarValue[] mItems;

    ResPluralsValue(ResReferenceValue parent, Pair<Integer, ResScalarValue>[] items) {
        super(parent);
        mItems = new ResScalarValue[6];
        for (Pair<Integer, ResScalarValue> item : items) {
            mItems[item.getLeft() - BAG_KEY_PLURALS_START] = item.getRight();
        }
    }

    @Override
    public void serializeToResValuesXml(XmlSerializer serializer, ResResource res)
            throws AndrolibException, IOException {
        serializer.startTag(null, "plurals");
        serializer.attribute(null, "name", res.getResSpec().getName());
        for (int i = 0; i < mItems.length; i++) {
            ResScalarValue item = mItems[i];
            if (item == null) {
                continue;
            }

            serializer.startTag(null, "item");
            serializer.attribute(null, "quantity", QUANTITY_MAP[i]);
            serializer.text(ResXmlEncoders.enumerateNonPositionalSubstitutionsIfRequired(item.encodeAsResXmlNonEscapedItemValue()));
            serializer.endTag(null, "item");
        }
        serializer.endTag(null, "plurals");
    }
}
