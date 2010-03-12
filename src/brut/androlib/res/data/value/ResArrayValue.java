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
import java.util.Iterator;
import java.util.Map;
import org.xmlpull.v1.XmlSerializer;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public class ResArrayValue extends ResBagValue implements ResXmlSerializable {
    public ResArrayValue(ResReferenceValue parent,
            Map<ResReferenceValue, ResScalarValue> items) {
        super(parent, items);
    }

    public void serializeToXml(XmlSerializer serializer, ResResource res)
            throws IOException, AndrolibException {
        String type = getType();
        type = (type == null ? "" : type + "-") + "array";

        serializer.startTag(null, type);
        serializer.attribute(null, "name", res.getResSpec().getName());
        for (ResScalarValue item : mItems.values()) {
            serializer.startTag(null, "item");
            serializer.text(item.toResXmlFormat());
            serializer.endTag(null, "item");
        }
        serializer.endTag(null, type);
    }

    public String getType() {
        if (mItems.size() == 0) {
            return null;
        }
        Iterator<ResScalarValue> it = mItems.values().iterator();
        String type = it.next().getType();
        while (it.hasNext()) {
            String itemType = it.next().getType();
            if (! type.equals(itemType)) {
                return null;
            }
        }
        return type;
    }
}
