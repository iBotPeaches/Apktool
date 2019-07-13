/**
 *  Copyright (C) 2019 Ryszard Wiśniewski <brut.alll@gmail.com>
 *  Copyright (C) 2019 Connor Tumbleson <connor.tumbleson@gmail.com>
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
import java.util.*;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public class ResType {
    private final ResConfigFlags mFlags;
    private final Map<ResResSpec, ResResource> mResources = new LinkedHashMap<ResResSpec, ResResource>();

    public ResType(ResConfigFlags flags) {
        this.mFlags = flags;
    }

    public Set<ResResource> listResources() {
        return new LinkedHashSet<ResResource>(mResources.values());
    }

    public ResResource getResource(ResResSpec spec) throws AndrolibException {
        ResResource res = mResources.get(spec);
        if (res == null) {
            throw new UndefinedResObject(String.format("resource: spec=%s, config=%s", spec, this));
        }
        return res;
    }

    public Set<ResResSpec> listResSpecs() {
        return mResources.keySet();
    }

    public ResConfigFlags getFlags() {
        return mFlags;
    }

    public void addResource(ResResource res) throws AndrolibException {
        addResource(res, false);
    }

    public void removeResource(ResResource res) throws AndrolibException {
        ResResSpec spec = res.getResSpec();
        mResources.remove(spec);
    }

    public void addResource(ResResource res, boolean overwrite) throws AndrolibException {
        ResResSpec spec = res.getResSpec();
        if (mResources.put(spec, res) != null && !overwrite) {
            throw new AndrolibException(String.format("Multiple resources: spec=%s, config=%s", spec, this));
        }
    }

    @Override
    public String toString() {
        return mFlags.toString();
    }
}
