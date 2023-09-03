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
import brut.androlib.res.data.value.ResFileValue;
import brut.androlib.res.data.value.ResValueFactory;
import brut.androlib.res.xml.ResValuesXmlSerializable;
import brut.util.Duo;
import java.util.*;
import java.util.logging.Logger;

public class ResPackage {
    private final ResTable mResTable;
    private final int mId;
    private final String mName;
    private final Map<ResID, ResResSpec> mResSpecs = new LinkedHashMap<>();
    private final Map<ResConfigFlags, ResType> mConfigs = new LinkedHashMap<>();
    private final Map<String, ResTypeSpec> mTypes = new LinkedHashMap<>();
    private final Set<ResID> mSynthesizedRes = new HashSet<>();

    private ResValueFactory mValueFactory;

    public ResPackage(ResTable resTable, int id, String name) {
        this.mResTable = resTable;
        this.mId = id;
        this.mName = name;
    }

    public List<ResResSpec> listResSpecs() {
        return new ArrayList<>(mResSpecs.values());
    }

    public boolean hasResSpec(ResID resID) {
        return mResSpecs.containsKey(resID);
    }

    public ResResSpec getResSpec(ResID resID) throws UndefinedResObjectException {
        ResResSpec spec = mResSpecs.get(resID);
        if (spec == null) {
            throw new UndefinedResObjectException("resource spec: " + resID.toString());
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

    public Set<ResResource> listFiles() {
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
        Map<Duo<ResTypeSpec, ResType>, ResValuesFile> ret = new HashMap<>();
        for (ResResSpec spec : mResSpecs.values()) {
            for (ResResource res : spec.listResources()) {
                if (res.getValue() instanceof ResValuesXmlSerializable) {
                    ResTypeSpec type = res.getResSpec().getType();
                    ResType config = res.getConfig();
                    Duo<ResTypeSpec, ResType> key = new Duo<>(type, config);
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
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ResPackage other = (ResPackage) obj;
        if (!Objects.equals(this.mResTable, other.mResTable)) {
            return false;
        }
        return this.mId == other.mId;
    }

    @Override
    public int hashCode() {
        int hash = 17;
        hash = 31 * hash + (this.mResTable != null ? this.mResTable.hashCode() : 0);
        hash = 31 * hash + this.mId;
        return hash;
    }

    public ResValueFactory getValueFactory() {
        if (mValueFactory == null) {
            mValueFactory = new ResValueFactory(this);
        }
        return mValueFactory;
    }

    private final static Logger LOGGER = Logger.getLogger(ResPackage.class.getName());
}
