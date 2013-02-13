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

package brut.androlib.res.decoder;

import brut.androlib.AndrolibException;
import brut.androlib.res.data.ResPackage;
import brut.androlib.res.data.value.ResAttr;
import brut.androlib.res.data.value.ResScalarValue;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public class ResAttrDecoder {
	public String decode(int type, int value, String rawValue, int attrResId)
			throws AndrolibException {
		ResScalarValue resValue = mCurrentPackage.getValueFactory().factory(
				type, value, rawValue);

		String decoded = null;
		if (attrResId != 0) {
			ResAttr attr = (ResAttr) getCurrentPackage().getResTable()
					.getResSpec(attrResId).getDefaultResource().getValue();
			decoded = attr.convertToResXmlFormat(resValue);
		}

		return decoded != null ? decoded : resValue.encodeAsResXmlAttr();
	}

	public ResPackage getCurrentPackage() throws AndrolibException {
		if (mCurrentPackage == null) {
			throw new AndrolibException("Current package not set");
		}
		return mCurrentPackage;
	}

	public void setCurrentPackage(ResPackage currentPackage) {
		mCurrentPackage = currentPackage;
	}

	private ResPackage mCurrentPackage;
}
