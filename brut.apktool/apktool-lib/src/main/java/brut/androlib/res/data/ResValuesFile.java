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

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public class ResValuesFile {
	private final ResPackage mPackage;
	private final ResType mType;
	private final ResConfig mConfig;
	private final Set<ResResource> mResources = new LinkedHashSet<ResResource>();

	public ResValuesFile(ResPackage pkg, ResType type, ResConfig config) {
		this.mPackage = pkg;
		this.mType = type;
		this.mConfig = config;
	}

	public String getPath() {
		return "values" + mConfig.getFlags().getQualifiers() + "/"
				+ mType.getName() + (mType.getName().endsWith("s") ? "" : "s")
				+ ".xml";
	}

	public Set<ResResource> listResources() {
		return mResources;
	}

	public ResType getType() {
		return mType;
	}

	public ResConfig getConfig() {
		return mConfig;
	}

	public boolean isSynthesized(ResResource res) {
		return mPackage.isSynthesized(res.getResSpec().getId());
	}

	public void addResource(ResResource res) {
		mResources.add(res);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final ResValuesFile other = (ResValuesFile) obj;
		if (this.mType != other.mType
				&& (this.mType == null || !this.mType.equals(other.mType))) {
			return false;
		}
		if (this.mConfig != other.mConfig
				&& (this.mConfig == null || !this.mConfig.equals(other.mConfig))) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		int hash = 17;
		hash = 31 * hash + (this.mType != null ? this.mType.hashCode() : 0);
		hash = 31 * hash + (this.mConfig != null ? this.mConfig.hashCode() : 0);
		return hash;
	}
}
