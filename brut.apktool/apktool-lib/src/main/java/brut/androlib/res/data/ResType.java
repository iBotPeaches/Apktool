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

package brut.androlib.res.data;

import brut.androlib.AndrolibException;
import brut.androlib.err.UndefinedResObject;
import java.util.*;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public final class ResType {
	private final String mName;
	private final Map<String, ResResSpec> mResSpecs = new LinkedHashMap<String, ResResSpec>();

	private final ResTable mResTable;
	private final ResPackage mPackage;

	public ResType(String name, ResTable resTable, ResPackage package_) {
		this.mName = name;
		this.mResTable = resTable;
		this.mPackage = package_;
	}

	public String getName() {
		return mName;
	}

	public Set<ResResSpec> listResSpecs() {
		return new LinkedHashSet<ResResSpec>(mResSpecs.values());
	}

	public ResResSpec getResSpec(String name) throws AndrolibException {
		ResResSpec spec = mResSpecs.get(name);
		if (spec == null) {
			throw new UndefinedResObject(String.format("resource spec: %s/%s",
					getName(), name));
		}
		return spec;
	}

	public void addResSpec(ResResSpec spec) throws AndrolibException {
		if (mResSpecs.put(spec.getName(), spec) != null) {
			throw new AndrolibException(String.format(
					"Multiple res specs: %s/%s", getName(), spec.getName()));
		}
	}

	@Override
	public String toString() {
		return mName;
	}
}
