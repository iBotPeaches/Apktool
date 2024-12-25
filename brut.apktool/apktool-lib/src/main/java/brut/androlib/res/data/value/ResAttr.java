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
import brut.androlib.res.data.ResPackage;
import brut.androlib.res.data.ResResource;
import brut.androlib.res.xml.ResValuesXmlSerializable;
import org.apache.commons.lang3.tuple.Pair;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;

public class ResAttr extends ResBagValue implements ResValuesXmlSerializable {
    private static final int BAG_KEY_ATTR_MIN = 0x01000001;
    private static final int BAG_KEY_ATTR_MAX = 0x01000002;
    private static final int BAG_KEY_ATTR_L10N = 0x01000003;

    private static final int TYPE_REFERENCE = 0x01;
    private static final int TYPE_STRING = 0x02;
    private static final int TYPE_INT = 0x04;
    private static final int TYPE_BOOL = 0x08;
    private static final int TYPE_COLOR = 0x10;
    private static final int TYPE_FLOAT = 0x20;
    private static final int TYPE_DIMEN = 0x40;
    private static final int TYPE_FRACTION = 0x80;
    private static final int TYPE_ANY_STRING = 0xee;

    private static final int TYPE_ENUM = 0x00010000;
    private static final int TYPE_FLAGS = 0x00020000;

    private final int mType;
    private final Integer mMin;
    private final Integer mMax;
    private final Boolean mL10n;

    ResAttr(ResReferenceValue parent, int type, Integer min, Integer max, Boolean l10n) {
        super(parent);
        mType = type;
        mMin = min;
        mMax = max;
        mL10n = l10n;
    }

    public String convertToResXmlFormat(ResScalarValue value) throws AndrolibException {
        return null;
    }

    @Override
    public void serializeToResValuesXml(XmlSerializer serializer, ResResource res)
            throws AndrolibException, IOException {
        String type = getTypeAsString();

        serializer.startTag(null, "attr");
        serializer.attribute(null, "name", res.getResSpec().getName());
        if (type != null) {
            serializer.attribute(null, "format", type);
        }
        if (mMin != null) {
            serializer.attribute(null, "min", mMin.toString());
        }
        if (mMax != null) {
            serializer.attribute(null, "max", mMax.toString());
        }
        if (mL10n != null && mL10n) {
            serializer.attribute(null, "localization", "suggested");
        }
        serializeBody(serializer, res);
        serializer.endTag(null, "attr");
    }

    public static ResAttr factory(ResReferenceValue parent, Pair<Integer, ResScalarValue>[] items,
                                  ResValueFactory factory) throws AndrolibException {
        Integer min = null, max = null;
        Boolean l10n = null;
        int i = 1;
        for (; i < items.length; i++) {
            Pair<Integer, ResScalarValue> item = items[i];
            switch (item.getLeft()) {
                case BAG_KEY_ATTR_MIN:
                    min = item.getRight().getRawIntValue();
                    continue;
                case BAG_KEY_ATTR_MAX:
                    max = item.getRight().getRawIntValue();
                    continue;
                case BAG_KEY_ATTR_L10N:
                    l10n = item.getRight().getRawIntValue() != 0;
                    continue;
            }
            break;
        }

        // #2806 - Make sure we handle int-based values and not just ResIntValue
        int rawValue = items[0].getRight().getRawIntValue();
        int scalarType = rawValue & 0xffff;

        if (i == items.length) {
            return new ResAttr(parent, scalarType, min, max, l10n);
        }
        ResPackage pkg = parent.getPackage();
        Pair<ResReferenceValue, ResScalarValue>[] attrItems = new Pair[items.length - i];
        for (int j = 0; i < items.length; i++, j++) {
            Pair<Integer, ResScalarValue> item = items[i];
            int resId = item.getLeft();
            pkg.addSynthesizedRes(resId);
            attrItems[j] = Pair.of(factory.newReference(resId, null), item.getRight());
        }
        switch (rawValue & 0xff0000) {
            case TYPE_ENUM:
                return new ResEnumAttr(parent, scalarType, min, max, l10n, attrItems);
            case TYPE_FLAGS:
                return new ResFlagsAttr(parent, scalarType, min, max, l10n, attrItems);
        }

        throw new AndrolibException("Could not decode attr value");
    }

    protected void serializeBody(XmlSerializer serializer, ResResource res)
            throws AndrolibException, IOException {
        // stub
    }

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
}
