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
import brut.androlib.res.AndrolibResources;
import brut.androlib.res.data.value.ResValue;
import java.util.*;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public class ResTable {
	private final AndrolibResources mAndRes;

	private final Map<Integer, ResPackage> mPackagesById = new HashMap<Integer, ResPackage>();
	private final Map<String, ResPackage> mPackagesByName = new HashMap<String, ResPackage>();
	private final Set<ResPackage> mMainPackages = new LinkedHashSet<ResPackage>();
	private final Set<ResPackage> mFramePackages = new LinkedHashSet<ResPackage>();

	private String mFrameTag;

	private Map<String, String> mSdkInfo = new LinkedHashMap<String, String>();
	private Map<String, String> mPackageInfo = new LinkedHashMap<String, String>();

	public ResTable() {
		mAndRes = null;
	}

	public ResTable(AndrolibResources andRes) {
		mAndRes = andRes;
	}

	public ResResSpec getResSpec(int resID) throws AndrolibException {
		return getResSpec(new ResID(resID));
	}

	public ResResSpec getResSpec(ResID resID) throws AndrolibException {
		return getPackage(resID.package_).getResSpec(resID);
	}

	public Set<ResPackage> listMainPackages() {
		return mMainPackages;
	}

	public Set<ResPackage> listFramePackages() {
		return mFramePackages;
	}

	public ResPackage getPackage(int id) throws AndrolibException {
		ResPackage pkg = mPackagesById.get(id);
		if (pkg != null) {
			return pkg;
		}
		if (mAndRes != null) {
			return mAndRes.loadFrameworkPkg(this, id, mFrameTag);
		}
		throw new UndefinedResObject(String.format("package: id=%d", id));
	}

	public ResPackage getPackage(String name) throws AndrolibException {
		ResPackage pkg = mPackagesByName.get(name);
		if (pkg == null) {
			throw new UndefinedResObject("package: name=" + name);
		}
		return pkg;
	}

	public boolean hasPackage(int id) {
		return mPackagesById.containsKey(id);
	}

	public boolean hasPackage(String name) {
		return mPackagesByName.containsKey(name);
	}

	public ResValue getValue(String package_, String type, String name)
			throws AndrolibException {
		return getPackage(package_).getType(type).getResSpec(name)
				.getDefaultResource().getValue();
	}

	public void addPackage(ResPackage pkg, boolean main)
			throws AndrolibException {
		Integer id = pkg.getId();
		if (mPackagesById.containsKey(id)) {
			throw new AndrolibException("Multiple packages: id="
					+ id.toString());
		}
		String name = pkg.getName();
		if (mPackagesByName.containsKey(name)) {
			throw new AndrolibException("Multiple packages: name=" + name);
		}

		mPackagesById.put(id, pkg);
		mPackagesByName.put(name, pkg);
		if (main) {
			mMainPackages.add(pkg);
		} else {
			mFramePackages.add(pkg);
		}
	}

	public void setFrameTag(String tag) {
		mFrameTag = tag;
	}

	public Map<String, String> getSdkInfo() {
		return mSdkInfo;
	}

	public void addSdkInfo(String key, String value) {
		mSdkInfo.put(key, value);
	}

	public void clearSdkInfo() {
		mSdkInfo.clear();
	}

	public void addPackageInfo(String key, String value) {
		mPackageInfo.put(key, value);
	}

	public Map<String, String> getPackageInfo() {
		return mPackageInfo;
	}

	public boolean isPackageInfoValueSet(String key) {
		return (mPackageInfo.containsKey(key));
	}
}
