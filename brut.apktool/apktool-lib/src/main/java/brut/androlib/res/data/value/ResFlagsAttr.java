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
import brut.androlib.res.data.arsc.FlagItem;
import org.apache.commons.lang3.tuple.Pair;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.logging.Logger;

public class ResFlagsAttr extends ResAttr {
    private static final Logger LOGGER = Logger.getLogger(ResFlagsAttr.class.getName());

    private final FlagItem[] mItems;
    private FlagItem[] mZeroFlags;
    private FlagItem[] mFlags;

    ResFlagsAttr(ResReferenceValue parent, int type, Integer min, Integer max, Boolean l10n,
                 Pair<ResReferenceValue, ResScalarValue>[] items) {
        super(parent, type, min, max, l10n);
        mItems = new FlagItem[items.length];
        for (int i = 0; i < items.length; i++) {
            Pair<ResReferenceValue, ResScalarValue> item = items[i];
            mItems[i] = new FlagItem(item.getLeft(), item.getRight().getRawIntValue());
        }
    }

    @Override
    public String convertToResXmlFormat(ResScalarValue value) throws AndrolibException {
        if (value instanceof ResReferenceValue) {
            return value.encodeAsResXml();
        }
        if (!(value instanceof ResIntValue)) {
            return super.convertToResXmlFormat(value);
        }
        loadFlags();
        int intVal = ((ResIntValue) value).getValue();

        if (intVal == 0) {
            return renderFlags(mZeroFlags);
        }

        FlagItem[] flagItems = new FlagItem[mFlags.length];
        int[] flags = new int[mFlags.length];
        int flagsCount = 0;
        for (FlagItem flagItem : mFlags) {
            int flag = flagItem.flag;

            if ((intVal & flag) != flag) {
                continue;
            }

            if (!isSubpartOf(flag, flags)) {
                flags[flagsCount] = flag;
                flagItems[flagsCount++] = flagItem;
            }
        }
        return renderFlags(Arrays.copyOf(flagItems, flagsCount));
    }

    @Override
    protected void serializeBody(XmlSerializer serializer, ResResource res)
            throws AndrolibException, IOException {
        for (FlagItem item : mItems) {
            ResResSpec referent = item.ref.getReferent();

            // #2836 - Support skipping items if the resource cannot be identified.
            if (referent == null && mConfig.getDecodeResolveMode() == Config.DECODE_RES_RESOLVE_REMOVE) {
                LOGGER.fine(String.format("null flag reference: 0x%08x(%s)", item.ref.getValue(), item.ref.getType()));
                continue;
            }

            serializer.startTag(null, "flag");
            serializer.attribute(null, "name", item.getValue());
            serializer.attribute(null, "value", String.format("0x%08x", item.flag));
            serializer.endTag(null, "flag");
        }
    }

    private boolean isSubpartOf(int flag, int[] flags) {
        for (int f : flags) {
            if ((f & flag) == flag) {
                return true;
            }
        }
        return false;
    }

    private String renderFlags(FlagItem[] flags) throws AndrolibException {
        StringBuilder sb = new StringBuilder();
        for (FlagItem flag : flags) {
            sb.append("|").append(flag.getValue());
        }
        if (sb.length() == 0) {
            return sb.toString();
        }
        return sb.substring(1);
    }

    private void loadFlags() {
        if (mFlags != null) {
            return;
        }

        FlagItem[] zeroFlags = new FlagItem[mItems.length];
        int zeroFlagsCount = 0;
        FlagItem[] flags = new FlagItem[mItems.length];
        int flagsCount = 0;

        for (FlagItem item : mItems) {
            if (item.flag == 0) {
                zeroFlags[zeroFlagsCount++] = item;
            } else {
                flags[flagsCount++] = item;
            }
        }

        mZeroFlags = Arrays.copyOf(zeroFlags, zeroFlagsCount);
        mFlags = Arrays.copyOf(flags, flagsCount);

        Arrays.sort(mFlags, Comparator.comparingInt((FlagItem item) -> Integer.bitCount(item.flag)).reversed());
    }
}
