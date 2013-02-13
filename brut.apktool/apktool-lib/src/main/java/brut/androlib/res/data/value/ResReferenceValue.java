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
import brut.androlib.res.data.ResPackage;
import brut.androlib.res.data.ResResSpec;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public class ResReferenceValue extends ResIntValue {
	private final ResPackage mPackage;
	private final boolean mTheme;

	public ResReferenceValue(ResPackage package_, int value, String rawValue) {
		this(package_, value, rawValue, false);
	}

	public ResReferenceValue(ResPackage package_, int value, String rawValue,
			boolean theme) {
		super(value, rawValue, "reference");
		mPackage = package_;
		mTheme = theme;
	}

	@Override
	protected String encodeAsResXml() throws AndrolibException {
		if (isNull()) {
			return "@null";
		}

		ResResSpec spec = getReferent();
		boolean newId = spec.hasDefaultResource()
				&& spec.getDefaultResource().getValue() instanceof ResIdValue;

		// generate the beginning to fix @android
		String mStart = (mTheme ? '?' : '@') + (newId ? "+" : "");
		// mStart = mStart.replace("@android", "@*android");

		return mStart
				+ spec.getFullName(mPackage, mTheme
						&& spec.getType().getName().equals("attr"));
	}

	public ResResSpec getReferent() throws AndrolibException {
		return mPackage.getResTable().getResSpec(getValue());
	}

	public boolean isNull() {
		return mValue == 0;
	}
}
