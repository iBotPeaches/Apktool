/*
 *  Copyright (C) 2010 Ryszard Wiśniewski <brut.alll@gmail.com>
 *  Copyright (C) 2010 Connor Tumbleson <connor.tumbleson@gmail.com>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package brut.androlib.res.data.value;

import brut.androlib.AndrolibException;
import brut.androlib.res.data.ResResource;
import brut.util.Duo;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.xmlpull.v1.XmlSerializer;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public class ResEnumAttr extends ResAttr {
    ResEnumAttr(ResReferenceValue parent, int type, Integer min, Integer max,
                Boolean l10n, Duo<ResReferenceValue, ResIntValue>[] items) {
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
    protected void serializeBody(XmlSerializer serializer, ResResource res)
            throws AndrolibException, IOException {
        for (Duo<ResReferenceValue, ResIntValue> duo : mItems) {
            int intVal = duo.m2.getValue();

            serializer.startTag(null, "enum");
            serializer.attribute(null, "name", duo.m1.getReferent().getName());
            serializer.attribute(null, "value", String.valueOf(intVal));
            serializer.endTag(null, "enum");
        }
    }

    private String decodeValue(int value) throws AndrolibException {
        String value2 = mItemsCache.get(value);
        if (value2 == null) {
            ResReferenceValue ref = null;
            for (Duo<ResReferenceValue, ResIntValue> duo : mItems) {
                if (duo.m2.getValue() == value) {
                    ref = duo.m1;
                    break;
                }
            }
            if (ref != null) {
                value2 = ref.getReferent().getName();
                mItemsCache.put(value, value2);
            }
        }
        return value2;
    }

    private final Duo<ResReferenceValue, ResIntValue>[] mItems;
    private final Map<Integer, String> mItemsCache = new HashMap<Integer, String>();
}
