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
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import org.xmlpull.v1.XmlSerializer;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public class ResSetAttr extends ResMapAttr {
    public ResSetAttr(ResReferenceValue parent,
            Map<ResReferenceValue, ResScalarValue> items, int type) {
        super(parent, items, type);
    }

    @Override
    public String convertToResXmlFormat(ResScalarValue value)
            throws AndrolibException {
        if (! (value instanceof ResIntValue)) {
            return super.convertToResXmlFormat(value);
        }
        int intVal = ((ResIntValue) value).getValue();
        String strVal = "";
        for (Entry<Integer, String> entry : getItemsMap().entrySet()) {
            int flag = entry.getKey();
            if ((intVal & flag) == flag) {
                strVal += "|" + entry.getValue();
            }
        }
        if (strVal.isEmpty()) {
            return "";
        }
        return strVal.substring(1);
    }

    @Override
    protected void serializeBody(XmlSerializer serializer, ResResource res)
            throws AndrolibException, IOException {
        for (Entry<Integer, String> entry : getItemsMap().entrySet()) {
            serializer.startTag(null, "flag");
            serializer.attribute(null, "name", entry.getValue());
            serializer.attribute(null, "value",
                String.format("0x%08x", entry.getKey()));
            serializer.endTag(null, "flag");
        }
    }
}
