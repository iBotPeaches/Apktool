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
import brut.util.Duo;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.util.Arrays;

public class ResArrayValue extends ResBagValue implements ResValuesXmlSerializable {
    ResArrayValue(ResReferenceValue parent, Duo<Integer, ResScalarValue>[] items) {
        super(parent);

        mItems = new ResScalarValue[items.length];
        for (int i = 0; i < items.length; i++) {
            mItems[i] = items[i].m2;
        }
    }

    public ResArrayValue(ResReferenceValue parent, ResScalarValue[] items) {
        super(parent);
        mItems = items;
    }

    @Override
    public void serializeToResValuesXml(XmlSerializer serializer,
                                        ResResource res) throws IOException, AndrolibException {
        String type = getType();
        type = (type == null ? "" : type + "-") + "array";
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
        for (ResScalarValue mItem : mItems) {
            serializer.startTag(null, "item");
            serializer.text(mItem.encodeAsResXmlNonEscapedItemValue());
            serializer.endTag(null, "item");
        }
        serializer.endTag(null, type);
    }

    public String getType() throws AndrolibException {
        if (mItems.length == 0) {
            return null;
        }
        String type = mItems[0].getType();
        for (ResScalarValue mItem : mItems) {
            if (mItem.encodeAsResXmlItemValue().startsWith("@string")) {
                return "string";
            } else if (mItem.encodeAsResXmlItemValue().startsWith("@drawable")) {
                return null;
            } else if (mItem.encodeAsResXmlItemValue().startsWith("@integer")) {
                return "integer";
            } else if (!"string".equals(type) && !"integer".equals(type)) {
                return null;
            } else if (!type.equals(mItem.getType())) {
                return null;
            }
        }
        if (!Arrays.asList(AllowedArrayTypes).contains(type)) {
            return "string";
        }
        return type;
    }

    private final ResScalarValue[] mItems;
    private final String[] AllowedArrayTypes = {"string", "integer"};
}
