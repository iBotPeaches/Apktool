/*
 *  Copyright 2010 Ryszard Wiśniewski <brut.alll@gmail.com>.
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
 *  under the License.
 */

package brut.androlib.res.data;

import brut.androlib.AndrolibException;
import brut.androlib.err.UndefinedResObject;
import brut.androlib.res.data.value.ResValue;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public class ResTable {
    private final Map<Integer, ResPackage> mPackagesById =
        new HashMap<Integer, ResPackage>();
    private final Map<String, ResPackage> mPackagesByName =
        new HashMap<String, ResPackage>();
    private final Set<ResPackage> mMainPackages =
        new LinkedHashSet<ResPackage>();

    public ResTable() {
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

    public ResPackage getPackage(int id) throws AndrolibException {
        ResPackage pkg = mPackagesById.get(id);
        if (pkg == null) {
            throw new UndefinedResObject(String.format(
                "package: id=%d", id));
        }
        return pkg;
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
            throw new AndrolibException(
                "Multiple packages: id=" + id.toString());
        }
        String name = pkg.getName();
        if (mPackagesByName.containsKey(name)) {
            throw new AndrolibException("Multiple packages: name=" + name);
        }

        mPackagesById.put(id, pkg);
        mPackagesByName.put(name, pkg);
        if (main) {
            mMainPackages.add(pkg);
        }
    }
}
