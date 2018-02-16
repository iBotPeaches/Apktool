/**
 *  Copyright (C) 2018 Ryszard Wiśniewski <brut.alll@gmail.com>
 *  Copyright (C) 2018 Connor Tumbleson <connor.tumbleson@gmail.com>
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
import brut.androlib.res.xml.ResValuesXmlSerializable;
import brut.util.Duo;
import java.io.IOException;
import org.xmlpull.v1.XmlSerializer;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public class ResBagValue extends ResValue implements ResValuesXmlSerializable {
    protected final ResReferenceValue mParent;

    public ResBagValue(ResReferenceValue parent) {
        this.mParent = parent;
    }

    @Override
    public void serializeToResValuesXml(XmlSerializer serializer,
                                        ResResource res) throws IOException, AndrolibException {
        String type = res.getResSpec().getType().getName();
        if ("style".equals(type)) {
            new ResStyleValue(mParent, new Duo[0], null)
                    .serializeToResValuesXml(serializer, res);
            return;
        }
        if ("array".equals(type)) {
            new ResArrayValue(mParent, new Duo[0]).serializeToResValuesXml(
                    serializer, res);
            return;
        }
        if ("plurals".equals(type)) {
            new ResPluralsValue(mParent, new Duo[0]).serializeToResValuesXml(
                    serializer, res);
            return;
        }

        serializer.startTag(null, "item");
        serializer.attribute(null, "type", type);
        serializer.attribute(null, "name", res.getResSpec().getName());
        serializer.endTag(null, "item");
    }

    public ResReferenceValue getParent() {
        return mParent;
    }
}
