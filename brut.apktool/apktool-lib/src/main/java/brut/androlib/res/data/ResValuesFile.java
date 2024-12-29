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

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public class ResValuesFile {
    private final ResPackage mPackage;
    private final ResTypeSpec mType;
    private final ResType mConfig;
    private final Set<ResResource> mResources;

    public ResValuesFile(ResPackage pkg, ResTypeSpec type, ResType config) {
        mPackage = pkg;
        mType = type;
        mConfig = config;
        mResources = new LinkedHashSet<>();
    }

    public String getPath() {
        return "values" + mConfig.getFlags().getQualifiers() + "/"
                + mType.getName() + (mType.getName().endsWith("s") ? "" : "s")
                + ".xml";
    }

    public Set<ResResource> listResources() {
        return mResources;
    }

    public ResTypeSpec getType() {
        return mType;
    }

    public boolean isSynthesized(ResResource res) {
        return mPackage.isSynthesized(res.getResSpec().getId());
    }

    public void addResource(ResResource res) {
        mResources.add(res);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof ResValuesFile) {
            ResValuesFile other = (ResValuesFile) obj;
            return Objects.equals(mType, other.mType)
                    && Objects.equals(mConfig, other.mConfig);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mType, mConfig);
    }
}
