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
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ResValuesFile other = (ResValuesFile) obj;
        if (!Objects.equals(mType, other.mType)) {
            return false;
        }
        return Objects.equals(mConfig, other.mConfig);
    }

    @Override
    public int hashCode() {
        int hash = 17;
        hash = 31 * hash + (mType != null ? mType.hashCode() : 0);
        hash = 31 * hash + (mConfig != null ? mConfig.hashCode() : 0);
        return hash;
    }
}
