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
import brut.androlib.res.xml.ResStringEncoder;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.util.Set;

public class ResString extends ResItem {
    public static final ResString EMPTY = new ResString("");

    private final CharSequence mValue;

    public ResString(CharSequence value) {
        super(TYPE_STRING);
        assert value != null;
        mValue = value;
    }

    public CharSequence getValue() {
        return mValue;
    }

    @Override
    public String toXmlTextValue() {
        return ResStringEncoder.encodeTextValue(mValue);
    }

    @Override
    public String toXmlAttributeValue() {
        return ResStringEncoder.encodeAttributeValue(mValue);
    }

    @Override
    public void serializeToValuesXml(XmlSerializer serial, ResEntry entry) throws AndrolibException, IOException {
        String typeName = entry.getType().getName();

        // Specify format for <item> tags when the resource type doesn't
        // directly support this value's format.
        Set<String> stdFormats = STANDARD_TYPE_FORMATS.get(typeName);
        String format = stdFormats != null ? getFormat() : null;
        boolean asItem = format != null && !stdFormats.contains(format);

        String tagName = asItem ? "item" : typeName;
        serial.startTag(null, tagName);
        if (asItem) {
            serial.attribute(null, "type", typeName);
        }
        serial.attribute(null, "name", entry.getName());
        if (asItem) {
            serial.attribute(null, "format", format);
        }
        if (!asItem && !isFormatted()) {
            serial.attribute(null, "formatted", "false");
        }
        String body = toXmlTextValue();
        if (!body.isEmpty()) {
            serial.text(body);
        }
        serial.endTag(null, tagName);
    }

    private boolean isFormatted() {
        if (mValue.length() == 0) {
            return true;
        }
        // Formatting must be disabled if the string has multiple sequential
        // format specifier.
        int[][] specs = ResStringEncoder.findFormatSpecifiers(mValue.toString());
        int[] sequential = specs[0];
        int[] positional = specs[1];
        return specs[0].length == 0 || specs[0].length + specs[1].length <= 1;
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
            return mValue.equals(other.mValue);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return mValue.hashCode();
    }
}
