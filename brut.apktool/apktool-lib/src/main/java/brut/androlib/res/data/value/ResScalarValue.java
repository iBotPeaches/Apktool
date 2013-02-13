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
import brut.androlib.res.xml.ResXmlEncodable;
import brut.androlib.res.xml.ResXmlEncoders;
import java.io.IOException;
import org.xmlpull.v1.XmlSerializer;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public abstract class ResScalarValue extends ResValue implements
		ResXmlEncodable, ResValuesXmlSerializable {
	protected final String mType;
	protected final String mRawValue;

	protected ResScalarValue(String type, String rawValue) {
		mType = type;
		mRawValue = rawValue;
	}

	@Override
	public String encodeAsResXmlAttr() throws AndrolibException {
		if (mRawValue != null) {
			return mRawValue;
		}
		return encodeAsResXml().replace("@android:", "@*android:");
	}

	public String encodeAsResXmlItemValue() throws AndrolibException {
		return encodeAsResXmlValue().replace("@android:", "@*android:");
	}

	@Override
	public String encodeAsResXmlValue() throws AndrolibException {
		if (mRawValue != null) {
			return mRawValue;
		}
		return encodeAsResXmlValueExt().replace("@android:", "@*android:");
	}

	public String encodeAsResXmlValueExt() throws AndrolibException {
		String rawValue = mRawValue;
		if (rawValue != null) {
			if (ResXmlEncoders.hasMultipleNonPositionalSubstitutions(rawValue)) {
				int count = 1;
				StringBuilder result = new StringBuilder();
				String tmp1[] = rawValue.split("%%", -1);
				int tmp1_sz = tmp1.length;
				for (int i = 0; i < tmp1_sz; i++) {
					String cur1 = tmp1[i];
					String tmp2[] = cur1.split("%", -1);
					int tmp2_sz = tmp2.length;
					for (int j = 0; j < tmp2_sz; j++) {
						String cur2 = tmp2[j];
						result.append(cur2);
						if (j != (tmp2_sz - 1)) {
							result.append('%').append(count).append('$');
							count++;
						}
					}
					if (i != (tmp1_sz - 1)) {
						result.append("%%");
					}
				}
				rawValue = result.toString();
			}
			return rawValue;
		}
		return encodeAsResXml();
	}

	@Override
	public void serializeToResValuesXml(XmlSerializer serializer,
			ResResource res) throws IOException, AndrolibException {
		String type = res.getResSpec().getType().getName();
		boolean item = !"reference".equals(mType) && !type.equals(mType);

		String body = encodeAsResXmlValue();

		// check for resource reference
		if (body.contains("@")) {
			if (!res.getFilePath().contains("string")) {
				item = true;
			}
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
