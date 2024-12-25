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

import brut.androlib.Config;
import brut.androlib.exceptions.AndrolibException;
import brut.androlib.res.data.ResResSpec;
import brut.androlib.res.data.ResResource;
import org.apache.commons.lang3.tuple.Pair;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class ResEnumAttr extends ResAttr {
    private static final Logger LOGGER = Logger.getLogger(ResEnumAttr.class.getName());

    private final Pair<ResReferenceValue, ResScalarValue>[] mItems;
    private final Map<Integer, String> mItemsCache;

    ResEnumAttr(ResReferenceValue parent, int type, Integer min, Integer max, Boolean l10n,
                Pair<ResReferenceValue, ResScalarValue>[] items) {
        super(parent, type, min, max, l10n);
        mItems = items;
        mItemsCache = new HashMap<>();
    }

    @Override
    public String convertToResXmlFormat(ResScalarValue value)
            throws AndrolibException {
        if (value instanceof ResIntValue) {
            String decoded = decodeValue(((ResIntValue) value).getValue());
            if (decoded != null) {
                return decoded;
            }
        }
        return super.convertToResXmlFormat(value);
    }

    @Override
    protected void serializeBody(XmlSerializer serializer, ResResource res)
            throws AndrolibException, IOException {
        for (Pair<ResReferenceValue, ResScalarValue> item : mItems) {
            ResReferenceValue ref = item.getLeft();
            ResScalarValue val = item.getRight();

            // #2836 - Support skipping items if the resource cannot be identified.
            ResResSpec referent = ref.getReferent();
            if (referent == null && mConfig.getDecodeResolveMode() == Config.DECODE_RES_RESOLVE_REMOVE) {
                LOGGER.fine(String.format("null enum reference: ref=0x%08x(%s), val=0x%08x(%s)",
                    ref.getRawIntValue(), ref.getType(), val.getRawIntValue(), val.getType()));
                continue;
            }

            serializer.startTag(null, "enum");
            serializer.attribute(null, "name", referent != null
                ? referent.getName() : String.format("APKTOOL_MISSING_0x%08x", ref.getRawIntValue()));
            serializer.attribute(null, "value", String.valueOf(val.getRawIntValue()));
            serializer.endTag(null, "enum");
        }
    }

    private String decodeValue(int value) throws AndrolibException {
        String decoded = mItemsCache.get(value);
        if (decoded == null) {
            ResReferenceValue ref = null;
            for (Pair<ResReferenceValue, ResScalarValue> item : mItems) {
                ResScalarValue val = item.getRight();
                if (val.getRawIntValue() == value) {
                    ref = item.getLeft();
                    break;
                }
            }
            if (ref != null && !ref.referentIsNull()) {
                decoded = ref.getReferent().getName();
                mItemsCache.put(value, decoded);
            }
        }
        return decoded;
    }
}
