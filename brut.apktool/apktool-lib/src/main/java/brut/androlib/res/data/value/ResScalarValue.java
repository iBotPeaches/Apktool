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
import brut.androlib.res.xml.ResValuesXmlSerializable;
import brut.androlib.res.xml.ResXmlEncodable;
import brut.androlib.res.xml.ResXmlEncoders;
import java.io.IOException;
import org.xmlpull.v1.XmlSerializer;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public abstract class ResScalarValue extends ResIntBasedValue implements
        ResXmlEncodable, ResValuesXmlSerializable {
    protected final String mType;
    protected final String mRawValue;

    protected ResScalarValue(String type, int rawIntValue, String rawValue) {
        super(rawIntValue);
        mType = type;
        mRawValue = rawValue;
    }

    @Override
    public String encodeAsResXmlAttr() throws AndrolibException {
        if (mRawValue != null) {
            return mRawValue;
        }
        return encodeAsResXml();
    }

    public String encodeAsResXmlItemValue() throws AndrolibException {
        return encodeAsResXmlValue();
    }

    @Override
    public String encodeAsResXmlValue() throws AndrolibException {
        if (mRawValue != null) {
            return mRawValue;
        }
        return encodeAsResXml();
    }

    public String encodeAsResXmlNonEscapedItemValue() throws AndrolibException {
        return encodeAsResXmlValue().replace("&amp;", "&").replace("&lt;","<");
    }

    public boolean hasMultipleNonPositionalSubstitutions() throws AndrolibException {
        return ResXmlEncoders.hasMultipleNonPositionalSubstitutions(mRawValue);
    }

    @Override
    public void serializeToResValuesXml(XmlSerializer serializer,
                                        ResResource res) throws IOException, AndrolibException {
        String type = res.getResSpec().getType().getName();
        boolean item = !"reference".equals(mType) && !type.equals(mType);

        String body = encodeAsResXmlValue();

        // check for resource reference
        if (!type.equalsIgnoreCase("color")) {
            if (body.contains("@")) {
                if (!res.getFilePath().contains("string")) {
                    item = true;
                }
            }
        }

        // Android does not allow values (false) for ids.xml anymore
        // https://issuetracker.google.com/issues/80475496
        // But it decodes as a ResBoolean, which makes no sense. So force it to empty
        if (type.equalsIgnoreCase("id") && !body.isEmpty()) {
            body = "";
        }

        // check for using attrib as node or item
        String tagName = item ? "item" : type;

        serializer.startTag(null, tagName);
        if (item) {
            serializer.attribute(null, "type", type);
        }
        serializer.attribute(null, "name", res.getResSpec().getName());

        serializeExtraXmlAttrs(serializer, res);

        if (!body.isEmpty()) {
            serializer.ignorableWhitespace(body);
        }

        serializer.endTag(null, tagName);
    }

    public String getType() {
        return mType;
    }

    protected void serializeExtraXmlAttrs(XmlSerializer serializer,
                                          ResResource res) throws IOException {
    }

    protected abstract String encodeAsResXml() throws AndrolibException;
}
