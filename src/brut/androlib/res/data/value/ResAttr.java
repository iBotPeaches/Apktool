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
import org.xmlpull.v1.XmlSerializer;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public class ResAttr extends ResBagValue implements ResXmlSerializable {
    private final int mType;

    public ResAttr(ResReferenceValue parent,
            Map<ResReferenceValue, ResScalarValue> items, int type) {
        super(parent, items);
        mType = type;
    }

    public String convertToResXmlFormat(ResScalarValue value)
            throws AndrolibException {
        return value.toResXmlFormat();
    }

    public void serializeToXml(XmlSerializer serializer, ResResource res)
            throws IOException, AndrolibException {
        String type = getTypeAsString();

        serializer.startTag(null, "attr");
        serializer.attribute(null, "name", res.getResSpec().getName());
        if (type != null) {
            serializer.attribute(null, "format", type);
        }
        serializeBody(serializer, res);
        serializer.endTag(null, "attr");
    }

    protected void serializeBody(XmlSerializer serializer, ResResource res)
            throws AndrolibException, IOException {}

    protected String getTypeAsString() {
        String s = "";
        if ((mType & TYPE_REFERENCE) != 0) {
            s += "|reference";
        }
        if ((mType & TYPE_STRING) != 0) {
            s += "|string";
        }
        if ((mType & TYPE_INT) != 0) {
            s += "|integer";
        }
        if ((mType & TYPE_BOOL) != 0) {
            s += "|boolean";
        }
        if ((mType & TYPE_COLOR) != 0) {
            s += "|color";
        }
        if ((mType & TYPE_FLOAT) != 0) {
            s += "|float";
        }
        if ((mType & TYPE_DIMEN) != 0) {
            s += "|dimension";
        }
        if ((mType & TYPE_FRACTION) != 0) {
            s += "|fraction";
        }
        if (s.isEmpty()) {
            return null;
        }
        return s.substring(1);
    }

    private final static int TYPE_REFERENCE = 0x01;
    private final static int TYPE_STRING = 0x02;
    private final static int TYPE_INT = 0x04;
    private final static int TYPE_BOOL = 0x08;
    private final static int TYPE_COLOR = 0x10;
    private final static int TYPE_FLOAT = 0x20;
    private final static int TYPE_DIMEN = 0x40;
    private final static int TYPE_FRACTION = 0x80;
    private final static int TYPE_ANY_STRING = 0xee;
}
