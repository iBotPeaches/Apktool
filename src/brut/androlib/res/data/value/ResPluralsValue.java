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
import brut.androlib.res.AndrolibResources;
import brut.androlib.res.data.ResResource;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.xmlpull.v1.XmlSerializer;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public class ResPluralsValue extends ResBagValue implements ResXmlSerializable {
    public ResPluralsValue(ResReferenceValue parent,
            Map<ResReferenceValue, ResScalarValue> items) {
        super(parent, items);
    }

    public void serializeToXml(XmlSerializer serializer, ResResource res)
            throws IOException, AndrolibException {
        serializer.startTag(null, "plurals");
        serializer.attribute(null, "name", res.getResSpec().getName());
        for (Entry<String, String> entry : getPluralsMap().entrySet()) {
            serializer.startTag(null, "item");
            serializer.attribute(null, "quantity", entry.getKey());
            serializer.text(entry.getValue());
            serializer.endTag(null, "item");
        }
        serializer.endTag(null, "plurals");
    }

    private Map<String, String> getPluralsMap() {
        Map<String, String> plurals = new LinkedHashMap<String, String>();
        for (Entry<ResReferenceValue, ResScalarValue> entry
                : mItems.entrySet()) {
            String quantity = getQuantityMap()[
                (entry.getKey().getValue() & 0xffff) - 4];
            if (quantity != null) {
                String value = ((ResStringValue) entry.getValue()).getValue();
                plurals.put(quantity, AndrolibResources.escapeForResXml(value));
                    
            }
        }
        return plurals;
    }

    private static String[] getQuantityMap() {
        if (quantityMap == null) {
            quantityMap = new String[]
                {"other", "zero", "one", "two", "few", "many"};
        }
        return quantityMap;
    }

    private static String[] quantityMap;
}
