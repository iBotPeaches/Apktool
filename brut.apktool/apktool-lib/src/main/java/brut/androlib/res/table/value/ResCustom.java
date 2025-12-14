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
import brut.androlib.res.xml.ValuesXmlSerializable;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.util.Objects;

public class ResCustom extends ResValue implements ValuesXmlSerializable {
    public static final ResCustom ID = new ResCustom("id");

    private final String mType;
    private final String mValue;
    private final boolean mAsItem;

    public ResCustom(String type) {
        this(type, null, false);
    }

    public ResCustom(String type, boolean asItem) {
        this(type, null, asItem);
    }

    public ResCustom(String type, String value) {
        this(type, value, false);
    }

    public ResCustom(String type, String value, boolean asItem) {
        mType = type;
        mValue = value;
        mAsItem = asItem;
    }

    @Override
    public void serializeToValuesXml(XmlSerializer serial, ResEntry entry)
            throws AndrolibException, IOException {
        String tagName = mAsItem ? "item" : mType;
        serial.startTag(null, tagName);
        if (mAsItem) {
            serial.attribute(null, "type", mType);
        }
        serial.attribute(null, "name", entry.getName());
        if (mValue != null) {
            serial.text(mValue);
        }
        serial.endTag(null, tagName);
    }

    @Override
    public String toString() {
        return String.format("ResCustom{type=%s, value=%s, asItem=%s}",
            mType, mValue, mAsItem);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof ResCustom) {
            ResCustom other = (ResCustom) obj;
            return Objects.equals(mType, other.mType)
                    && Objects.equals(mValue, other.mValue)
                    && mAsItem == other.mAsItem;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mType, mValue, mAsItem);
    }
}
