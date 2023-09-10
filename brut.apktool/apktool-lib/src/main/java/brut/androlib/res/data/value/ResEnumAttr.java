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
import brut.util.Duo;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class ResEnumAttr extends ResAttr {
    ResEnumAttr(ResReferenceValue parent, int type, Integer min, Integer max,
                Boolean l10n, Duo<ResReferenceValue, ResScalarValue>[] items) {
        super(parent, type, min, max, l10n);
        mItems = items;
    }

    @Override
    public String convertToResXmlFormat(ResScalarValue value)
            throws AndrolibException {
        if (value instanceof ResIntValue) {
            String ret = decodeValue(((ResIntValue) value).getValue());
            if (ret != null) {
                return ret;
            }
        }
        return super.convertToResXmlFormat(value);
    }

    @Override
    protected void serializeBody(XmlSerializer serializer, ResResource res) throws AndrolibException, IOException {
        for (Duo<ResReferenceValue, ResScalarValue> duo : mItems) {
            int intVal = duo.m2.getRawIntValue();

            // #2836 - Support skipping items if the resource cannot be identified.
            ResResSpec m1Referent = duo.m1.getReferent();
            if (m1Referent == null && shouldRemoveUnknownRes()) {
                LOGGER.fine(String.format("null enum reference: m1=0x%08x(%s), m2=0x%08x(%s)",
                    duo.m1.getRawIntValue(), duo.m1.getType(), duo.m2.getRawIntValue(), duo.m2.getType()));
                continue;
            }

            serializer.startTag(null, "enum");
            serializer.attribute(null, "name",
                m1Referent != null ? m1Referent.getName() : String.format("APKTOOL_MISSING_0x%08x", duo.m1.getRawIntValue())
            );
            serializer.attribute(null, "value", String.valueOf(intVal));
            serializer.endTag(null, "enum");
        }
    }

    private String decodeValue(int value) throws AndrolibException {
        String value2 = mItemsCache.get(value);
        if (value2 == null) {
            ResReferenceValue ref = null;
            for (Duo<ResReferenceValue, ResScalarValue> duo : mItems) {
                if (duo.m2.getRawIntValue() == value) {
                    ref = duo.m1;
                    break;
                }
            }
            if (ref != null && !ref.referentIsNull()) {
                value2 = ref.getReferent().getName();
                mItemsCache.put(value, value2);
            }
        }
        return value2;
    }

    private final Duo<ResReferenceValue, ResScalarValue>[] mItems;
    private final Map<Integer, String> mItemsCache = new HashMap<>();

    private static final Logger LOGGER = Logger.getLogger(ResEnumAttr.class.getName());
}
