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

import brut.androlib.exceptions.AndrolibException;
import brut.androlib.res.data.ResResource;
import brut.androlib.res.xml.ResXmlEncoders;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.util.regex.Pattern;

public class ResStringValue extends ResScalarValue {
    private static final Pattern ALL_DIGITS = Pattern.compile("\\d{9,}");

    public ResStringValue(String value, int rawValue) {
        this(value, rawValue, "string");
    }

    public ResStringValue(String value, int rawValue, String type) {
        super(type, rawValue, value);
    }

    @Override
    public String encodeAsResXmlAttr() {
        return checkIfStringIsNumeric(ResXmlEncoders.encodeAsResXmlAttr(mRawValue));
    }

    @Override
    public String encodeAsResXmlItemValue() {
        return ResXmlEncoders.enumerateNonPositionalSubstitutionsIfRequired(ResXmlEncoders.encodeAsXmlValue(mRawValue));
    }

    @Override
    public String encodeAsResXmlValue() {
        return ResXmlEncoders.encodeAsXmlValue(mRawValue);
    }

    @Override
    protected String encodeAsResXml() throws AndrolibException {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void serializeExtraXmlAttrs(XmlSerializer serializer, ResResource res)
            throws IOException {
        if (ResXmlEncoders.hasMultipleNonPositionalSubstitutions(mRawValue)) {
            serializer.attribute(null, "formatted", "false");
        }
    }

    private String checkIfStringIsNumeric(String val) {
        if (val == null || val.isEmpty()) {
            return val;
        }
        return ALL_DIGITS.matcher(val).matches() ? "\\ " + val : val;
    }
}
