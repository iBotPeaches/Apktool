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
package brut.androlib.res.data;

import brut.androlib.exceptions.AndrolibException;
import brut.androlib.exceptions.UndefinedResObjectException;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class ResResSpec {
    private final ResID mId;
    private final String mName;
    private final ResPackage mPackage;
    private final ResTypeSpec mType;
    private final Map<ResConfigFlags, ResResource> mResources = new LinkedHashMap<>();
    private static final Set<String> EMPTY_RESOURCE_NAMES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
        "0_resource_name_obfuscated",
        "(name removed)"
    )));

    public ResResSpec(ResID id, String name, ResPackage pkg, ResTypeSpec type) {
        this.mId = id;
        String cleanName;
        name = EMPTY_RESOURCE_NAMES.contains(name) ? null : name;

        ResResSpec resResSpec = type.getResSpecUnsafe(name);
        if (resResSpec != null) {
            cleanName = String.format("APKTOOL_DUPLICATE_%s_%s", type, id.toString());
        } else {
            cleanName = ((name == null || name.isEmpty()) ? ("APKTOOL_DUMMYVAL_" + id.toString()) : name);
        }

        this.mName = cleanName;
        this.mPackage = pkg;
        this.mType = type;
    }

    public Set<ResResource> listResources() {
        return new LinkedHashSet<>(mResources.values());
    }

    public ResResource getResource(ResType config) throws AndrolibException {
        return getResource(config.getFlags());
    }

    public ResResource getResource(ResConfigFlags config) throws AndrolibException {
        ResResource res = mResources.get(config);
        if (res == null) {
            throw new UndefinedResObjectException(String.format("resource: spec=%s, config=%s", this, config));
        }
        return res;
    }

    public ResResource getDefaultResource() throws AndrolibException {
        return getResource(new ResConfigFlags());
    }

    public boolean hasDefaultResource() {
        return mResources.containsKey(new ResConfigFlags());
    }

    public String getFullName(ResPackage relativeToPackage, boolean excludeType) {
        return getFullName(getPackage().equals(relativeToPackage), excludeType);
    }

    public String getFullName(boolean excludePackage, boolean excludeType) {
        return (excludePackage ? "" : getPackage().getName() + ":")
                + (excludeType ? "" : getType().getName() + "/") + getName();
    }

    public ResID getId() {
        return mId;
    }

    public String getName() {
        return StringUtils.replace(mName, "\"", "q");
    }

    public ResPackage getPackage() {
        return mPackage;
    }

    public ResTypeSpec getType() {
        return mType;
    }

    public boolean isDummyResSpec() {
        return getName().startsWith("APKTOOL_DUMMY_");
    }

    public void addResource(ResResource res) throws AndrolibException {
        addResource(res, false);
    }

    public void addResource(ResResource res, boolean overwrite) throws AndrolibException {
        ResConfigFlags flags = res.getConfig().getFlags();
        if (mResources.put(flags, res) != null && !overwrite) {
            throw new AndrolibException(String.format("Multiple resources: spec=%s, config=%s", this, flags));
        }
    }

    @Override
    public String toString() {
        return mId.toString() + " " + mType.toString() + "/" + mName;
    }
}
