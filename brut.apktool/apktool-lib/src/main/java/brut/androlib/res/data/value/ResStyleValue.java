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
import brut.androlib.res.data.ResResSpec;
import brut.androlib.res.data.ResResource;
import brut.androlib.res.xml.ResValuesXmlSerializable;
import org.apache.commons.lang3.tuple.Pair;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

public class ResStyleValue extends ResBagValue implements ResValuesXmlSerializable {
    private static final Logger LOGGER = Logger.getLogger(ResStyleValue.class.getName());

    private final Pair<ResReferenceValue, ResScalarValue>[] mItems;

    ResStyleValue(ResReferenceValue parent, Pair<Integer, ResScalarValue>[] items, ResValueFactory factory) {
        super(parent);
        mItems = new Pair[items.length];
        for (int i = 0; i < items.length; i++) {
            Pair<Integer, ResScalarValue> item = items[i];
            mItems[i] = Pair.of(factory.newReference(item.getLeft(), null), item.getRight());
        }
    }

    @Override
    public void serializeToResValuesXml(XmlSerializer serializer, ResResource res)
            throws AndrolibException, IOException {
        serializer.startTag(null, "style");
        serializer.attribute(null, "name", res.getResSpec().getName());
        if (!mParent.isNull() && !mParent.referentIsNull()) {
            serializer.attribute(null, "parent", mParent.encodeAsResXmlAttr());
        } else if (res.getResSpec().getName().indexOf('.') != -1) {
            serializer.attribute(null, "parent", "");
        }

        Set<String> processedNames = new HashSet<>();
        for (Pair<ResReferenceValue, ResScalarValue> item : mItems) {
            ResReferenceValue ref = item.getLeft();
            ResScalarValue val = item.getRight();
            ResResSpec spec = ref.getReferent();

            if (spec == null) {
                LOGGER.fine(String.format("null style reference: ref=0x%08x(%s), val=0x%08x(%s)",
                    ref.getRawIntValue(), ref.getType(), val.getRawIntValue(), val.getType()));
                continue;
            }

            String name;
            String value = null;

            ResValue resource = spec.getDefaultResource().getValue();
            if (resource instanceof ResReferenceValue) {
                continue;
            } else if (resource instanceof ResAttr) {
                ResAttr attr = (ResAttr) resource;
                value = attr.convertToResXmlFormat(val);
                name = spec.getFullName(res.getResSpec().getPackage(), true);
            } else {
                name = "@" + spec.getFullName(res.getResSpec().getPackage(), false);
            }

            // #3400 - Skip duplicate values, commonly seen are duplicate key-pairs on styles.
            if (!mConfig.isAnalysisMode() && processedNames.contains(name)) {
                continue;
            }

            if (value == null) {
                value = val.encodeAsResXmlValue();
            }

            if (value == null) {
                continue;
            }

            serializer.startTag(null, "item");
            serializer.attribute(null, "name", name);
            serializer.text(value);
            serializer.endTag(null, "item");

            processedNames.add(name);
        }
        serializer.endTag(null, "style");
        processedNames.clear();
    }
}
