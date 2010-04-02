/*
 *  Copyright 2010 Ryszard Wiśniewski <brut.alll@gmail.com>.
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
 *  under the License.
 */

package brut.androlib.res.data.value;

import brut.androlib.AndrolibException;
import brut.androlib.res.data.ResResource;
import brut.util.Duo;
import java.io.IOException;
import org.xmlpull.v1.XmlSerializer;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public class ResFlagsAttr extends ResAttr {
    ResFlagsAttr(ResReferenceValue parent, int type, Integer min, Integer max, Boolean l10n, Duo<ResReferenceValue, ResIntValue>[] items) {
        super(parent, type, min, max, l10n);

        mItems = new FlagItem[items.length];
        for (int i = 0; i < items.length; i++) {
            mItems[i] = new FlagItem(items[i].m1, items[i].m2.getValue());
        }
    }

    @Override
    public String convertToResXmlFormat(ResScalarValue value)
            throws AndrolibException {
        if (! (value instanceof ResIntValue)) {
            return super.convertToResXmlFormat(value);
        }
        int intVal = ((ResIntValue) value).getValue();
        String strVal = "";
        for (int i = 0; i < mItems.length; i++) {
            FlagItem item = mItems[i];

            if ((intVal & item.flag) == item.flag) {
                strVal += "|" + item.getValue();
            }
        }
        if (strVal.isEmpty()) {
            return strVal;
        }
        return strVal.substring(1);
    }

    @Override
    protected void serializeBody(XmlSerializer serializer, ResResource res)
            throws AndrolibException, IOException {
        for (int i = 0; i < mItems.length; i++) {
            FlagItem item = mItems[i];

            serializer.startTag(null, "flag");
            serializer.attribute(null, "name", item.getValue());
            serializer.attribute(null, "value",
                String.format("0x%08x", item.flag));
            serializer.endTag(null, "flag");
        }
    }


    private final FlagItem[] mItems;


    private static class FlagItem {
        public final ResReferenceValue ref;
        public final int flag;
        public String value;

        public FlagItem(ResReferenceValue ref, int flag) {
            this.ref = ref;
            this.flag = flag;
        }

        public String getValue() throws AndrolibException {
            if (value == null) {
                value = ref.getReferent().getName();
            }
            return value;
        }
    }
}
