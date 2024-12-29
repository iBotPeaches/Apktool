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

import brut.androlib.Config;
import brut.androlib.exceptions.AndrolibException;
import brut.androlib.exceptions.UndefinedResObjectException;
import brut.androlib.res.data.value.ResFileValue;
import brut.androlib.res.data.value.ResValueFactory;
import brut.androlib.res.xml.ResValuesXmlSerializable;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.logging.Logger;

public class ResPackage {
    private static final Logger LOGGER = Logger.getLogger(ResPackage.class.getName());

    private final ResTable mResTable;
    private final int mId;
    private final String mName;
    private final Map<ResID, ResResSpec> mResSpecs;
    private final Map<ResConfigFlags, ResType> mConfigs;
    private final Map<String, ResTypeSpec> mTypes;
    private final Set<ResID> mSynthesizedRes;

    private ResValueFactory mValueFactory;

    public ResPackage(ResTable resTable, int id, String name) {
        mResTable = resTable;
        mId = id;
        mName = name;
        mResSpecs = new LinkedHashMap<>();
        mConfigs = new LinkedHashMap<>();
        mTypes = new LinkedHashMap<>();
        mSynthesizedRes = new HashSet<>();
    }

    public Config getConfig() {
        return mResTable.getConfig();
    }

    public List<ResResSpec> listResSpecs() {
        return new ArrayList<>(mResSpecs.values());
    }

    public boolean hasResSpec(ResID resId) {
        return mResSpecs.containsKey(resId);
    }

    public ResResSpec getResSpec(ResID resId) throws UndefinedResObjectException {
        ResResSpec spec = mResSpecs.get(resId);
        if (spec == null) {
            throw new UndefinedResObjectException("resource spec: " + resId);
        }
        return spec;
    }

    public int getResSpecCount() {
        return mResSpecs.size();
    }

    public ResType getOrCreateConfig(ResConfigFlags flags) {
        ResType config = mConfigs.get(flags);
        if (config == null) {
            config = new ResType(flags);
            mConfigs.put(flags, config);
        }
        return config;
    }

    public ResTypeSpec getType(String typeName) throws AndrolibException {
        ResTypeSpec type = mTypes.get(typeName);
        if (type == null) {
            throw new UndefinedResObjectException("type: " + typeName);
        }
        return type;
    }

    public Collection<ResResource> listFiles() {
        Set<ResResource> ret = new HashSet<>();
        for (ResResSpec spec : mResSpecs.values()) {
            for (ResResource res : spec.listResources()) {
                if (res.getValue() instanceof ResFileValue) {
                    ret.add(res);
                }
            }
        }
        return ret;
    }

    public Collection<ResValuesFile> listValuesFiles() {
        Map<Pair<ResTypeSpec, ResType>, ResValuesFile> ret = new HashMap<>();
        for (ResResSpec spec : mResSpecs.values()) {
            for (ResResource res : spec.listResources()) {
                if (res.getValue() instanceof ResValuesXmlSerializable) {
                    ResTypeSpec type = res.getResSpec().getType();
                    ResType config = res.getConfig();
                    Pair<ResTypeSpec, ResType> key = Pair.of(type, config);
                    ResValuesFile values = ret.get(key);
                    if (values == null) {
                        values = new ResValuesFile(this, type, config);
                        ret.put(key, values);
                    }
                    values.addResource(res);
                }
            }
        }
        return ret.values();
    }

    public ResTable getResTable() {
        return mResTable;
    }

    public int getId() {
        return mId;
    }

    public String getName() {
        return mName;
    }

    boolean isSynthesized(ResID resId) {
        return mSynthesizedRes.contains(resId);
    }

    public void addResSpec(ResResSpec spec) throws AndrolibException {
        if (mResSpecs.put(spec.getId(), spec) != null) {
            throw new AndrolibException("Multiple resource specs: " + spec);
        }
    }

    public void addType(ResTypeSpec type) {
        if (mTypes.containsKey(type.getName())) {
            LOGGER.warning("Multiple types detected! " + type + " ignored!");
        } else {
            mTypes.put(type.getName(), type);
        }
    }

    public void addSynthesizedRes(int resId) {
        mSynthesizedRes.add(new ResID(resId));
    }

    @Override
    public String toString() {
        return mName;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof ResPackage) {
            ResPackage other = (ResPackage) obj;
            return Objects.equals(mResTable, other.mResTable)
                    && mId == other.mId;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mResTable, mId);
    }

    public ResValueFactory getValueFactory() {
        if (mValueFactory == null) {
            mValueFactory = new ResValueFactory(this);
        }
        return mValueFactory;
    }
}
