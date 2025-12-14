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
package brut.androlib.res.table.value;

import brut.androlib.exceptions.AndrolibException;
import brut.androlib.res.table.ResEntry;
import brut.androlib.res.xml.ResXmlEncodable;
import brut.androlib.res.xml.ResXmlEncoders;
import brut.androlib.res.xml.ValuesXmlSerializable;
import org.apache.commons.lang3.StringUtils;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.util.Objects;
import java.util.Set;

public class ResString extends ResItem implements ResXmlEncodable, ValuesXmlSerializable {
    public static final ResString EMPTY = new ResString("");

    private final String mValue;

    public ResString(String text) {
        mValue = text;
    }

    public boolean hasMultipleNonPositionalSubstitutions() {
        return ResXmlEncoders.hasMultipleNonPositionalSubstitutions(mValue);
    }

    public String encodeAsResXmlItemValueUnescaped() {
        return StringUtils.replaceEach(encodeAsResXmlValue(),
            new String[] { "&amp;", "&lt;" },
            new String[] { "&", "<" });
    }

    @Override
    public String encodeAsResXmlValue() {
        return ResXmlEncoders.encodeAsXmlValue(mValue);
    }

    @Override
    public String encodeAsResXmlItemValue() {
        return ResXmlEncoders.enumerateNonPositionalSubstitutionsIfRequired(encodeAsResXmlValue());
    }

    @Override
    public String encodeAsResXmlAttrValue() {
        String value = ResXmlEncoders.encodeAsResXmlAttrValue(mValue);
        return value != null ? value : "";
    }

    @Override
    public void serializeToValuesXml(XmlSerializer serial, ResEntry entry)
            throws AndrolibException, IOException {
        String type = entry.getTypeName();

        // Specify format for <item> tags when the resource type doesn't
        // directly support the string format.
        Set<String> standardFormats = STANDARD_TYPE_FORMATS.get(type);
        boolean asItem = standardFormats == null || !standardFormats.contains("string");

        String tagName = asItem ? "item" : type;
        serial.startTag(null, tagName);
        if (asItem) {
            serial.attribute(null, "type", type);
        }
        serial.attribute(null, "name", entry.getName());
        if (asItem) {
            serial.attribute(null, "format", "string");
        }
        if (!asItem && hasMultipleNonPositionalSubstitutions()) {
            serial.attribute(null, "formatted", "false");
        }
        String body = encodeAsResXmlValue();
        if (!body.isEmpty()) {
            serial.ignorableWhitespace(body);
        }
        serial.endTag(null, tagName);
    }

    @Override
    public String getFormat() {
        return "string";
    }

    @Override
    public String toString() {
        return String.format("ResString{value=%s}", mValue);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof ResString) {
            ResString other = (ResString) obj;
            return Objects.equals(mValue, other.mValue);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mValue);
    }
}
