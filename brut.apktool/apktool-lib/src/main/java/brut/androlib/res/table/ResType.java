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

import java.util.Objects;

public class ResType {
    private final ResTypeSpec mSpec;
    private final ResConfig mConfig;

    public ResType(ResTypeSpec spec, ResConfig config) {
        mSpec = spec;
        mConfig = config;
    }

    public ResTypeSpec getSpec() {
        return mSpec;
    }

    public ResPackage getPackage() {
        return mSpec.getPackage();
    }

    public int getId() {
        return mSpec.getId();
    }

    public String getName() {
        return mSpec.getName();
    }

    public boolean isBagType() {
        return mSpec.isBagType();
    }

    public ResConfig getConfig() {
        return mConfig;
    }

    @Override
    public String toString() {
        return String.format("ResType{spec=%s, config=%s}", mSpec, mConfig);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof ResType) {
            ResType other = (ResType) obj;
            return Objects.equals(mSpec, other.mSpec)
                    && Objects.equals(mConfig, other.mConfig);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mSpec, mConfig);
    }
}
