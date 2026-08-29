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
package brut.androlib.res.table;

import brut.androlib.res.data.FeatureFlag;

import java.util.Objects;

public class ResType {
    private final ResTypeSpec mSpec;
    private final ResConfig mConfig;
    private final FeatureFlag mFlag;

    public ResType(ResTypeSpec spec, ResConfig config, FeatureFlag flag) {
        assert spec != null && config != null;
        mSpec = spec;
        mConfig = config;
        mFlag = flag;
    }

    public ResPackage getPackage() {
        return mSpec.getPackage();
    }

    public ResTypeSpec getSpec() {
        return mSpec;
    }

    public int getId() {
        return mSpec.getId();
    }

    public String getName() {
        return mSpec.getName();
    }

    public ResConfig getConfig() {
        return mConfig;
    }

    public FeatureFlag getFlag() {
        return mFlag;
    }

    @Override
    public String toString() {
        return String.format("ResType{spec=%s, config=%s, flag=%s}", mSpec, mConfig, mFlag);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ResType other = (ResType) obj;
        return mSpec.equals(other.mSpec)
            && mConfig.equals(other.mConfig)
            && Objects.equals(mFlag, other.mFlag);
    }

    @Override
    public int hashCode() {
        int result = mSpec.hashCode();
        result = 31 * result + mConfig.hashCode();
        result = 31 * result + Objects.hashCode(mFlag);
        return result;
    }
}
