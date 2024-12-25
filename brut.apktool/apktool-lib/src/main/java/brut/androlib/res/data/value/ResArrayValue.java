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
import com.google.common.collect.Sets;
import org.apache.commons.lang3.tuple.Pair;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.util.Set;

public class ResArrayValue extends ResBagValue implements ResValuesXmlSerializable {
    private static final Set<String> ALLOWED_ARRAY_TYPES = Sets.newHashSet("string", "integer");

    private final ResScalarValue[] mItems;

    ResArrayValue(ResReferenceValue parent, Pair<Integer, ResScalarValue>[] items) {
        super(parent);
        mItems = new ResScalarValue[items.length];
        for (int i = 0; i < items.length; i++) {
            mItems[i] = items[i].getRight();
        }
    }

    public ResArrayValue(ResReferenceValue parent, ResScalarValue[] items) {
        super(parent);
        mItems = items;
    }

    @Override
    public void serializeToResValuesXml(XmlSerializer serializer, ResResource res)
            throws AndrolibException, IOException {
        String type = getType();
        type = (type != null ? type + "-" : "") + "array";
        serializer.startTag(null, type);
        serializer.attribute(null, "name", res.getResSpec().getName());

        // lets check if we need to add formatted="false" to this array
        for (ResScalarValue item : mItems) {
            if (item.hasMultipleNonPositionalSubstitutions()) {
                serializer.attribute(null, "formatted", "false");
                break;
            }
        }

        // add <item>'s
        for (ResScalarValue item : mItems) {
            serializer.startTag(null, "item");
            serializer.text(item.encodeAsResXmlNonEscapedItemValue());
            serializer.endTag(null, "item");
        }
        serializer.endTag(null, type);
    }

    public String getType() throws AndrolibException {
        if (mItems.length == 0) {
            return null;
        }
        String type = mItems[0].getType();
        for (ResScalarValue item : mItems) {
            if (item.encodeAsResXmlItemValue().startsWith("@string")) {
                return "string";
            } else if (item.encodeAsResXmlItemValue().startsWith("@drawable")) {
                return null;
            } else if (item.encodeAsResXmlItemValue().startsWith("@integer")) {
                return "integer";
            } else if (!"string".equals(type) && !"integer".equals(type)) {
                return null;
            } else if (!type.equals(item.getType())) {
                return null;
            }
        }
        if (!ALLOWED_ARRAY_TYPES.contains(type)) {
            return "string";
        }
        return type;
    }
}
