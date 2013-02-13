/**
 *  Copyright 2011 Ryszard Wiśniewski <brut.alll@gmail.com>
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
public class ResArrayValue extends ResBagValue implements
		ResValuesXmlSerializable {
	private String mRawItems;

	ResArrayValue(ResReferenceValue parent, Duo<Integer, ResScalarValue>[] items) {
		super(parent);

		mItems = new ResScalarValue[items.length];
		for (int i = 0; i < items.length; i++) {
			mItems[i] = items[i].m2;
		}
	}

	public ResArrayValue(ResReferenceValue parent, ResScalarValue[] items) {
		super(parent);
		mItems = items;
	}

	@Override
	public void serializeToResValuesXml(XmlSerializer serializer,
			ResResource res) throws IOException, AndrolibException {
		String type = getType();
		type = (type == null ? "" : type + "-") + "array";
		// reference array (04 10 2012, BurgerZ)
		if ("reference-array".equals(type)) {
			type = "string-array";
		}
		// reference array (04 10 2012, BurgerZ)
		serializer.startTag(null, type);
		serializer.attribute(null, "name", res.getResSpec().getName());
		for (int i = 0; i < mItems.length; i++) {
			serializer.startTag(null, "item");
			serializer.text(mItems[i].encodeAsResXmlItemValue());
			serializer.endTag(null, "item");
		}
		serializer.endTag(null, type);
	}

	public String getType() throws AndrolibException {
		if (mItems.length == 0) {
			return null;
		}
		String type = mItems[0].getType();
		for (int i = 1; i < mItems.length; i++) {

			if (mItems[i].encodeAsResXmlItemValue().startsWith("@string")) {
				return "string";
			} else if (mItems[i].encodeAsResXmlItemValue().startsWith(
					"@drawable")) {
				return null;
			} else if (!"string".equals(type) && !"integer".equals(type)) {
				return null;
			} else if (!type.equals(mItems[i].getType())) {
				return null;
			}
		}
		return type;
	}

	private final ResScalarValue[] mItems;

	public static final int BAG_KEY_ARRAY_START = 0x02000000;
}
