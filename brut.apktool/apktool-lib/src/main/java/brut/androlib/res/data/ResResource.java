/*
 *  Copyright (C) 2010 Ryszard Wiśniewski <brut.alll@gmail.com>
 *  Copyright (C) 2010 Connor Tumbleson <connor.tumbleson@gmail.com>
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
import brut.androlib.res.data.value.ResValue;

public class ResResource {
    private final ResType mConfig;
    private final ResResSpec mResSpec;
    private final ResValue mValue;

    public ResResource(ResType config, ResResSpec spec, ResValue value) {
        this.mConfig = config;
        this.mResSpec = spec;
        this.mValue = value;
    }

    public String getFilePath() {
        return mResSpec.getType().getName() + mConfig.getFlags().getQualifiers() + "/" + mResSpec.getName();
    }

    public ResType getConfig() {
        return mConfig;
    }

    public ResResSpec getResSpec() {
        return mResSpec;
    }

    public ResValue getValue() {
        return mValue;
    }

    public void replace(ResValue value) throws AndrolibException {
        ResResource res = new ResResource(mConfig, mResSpec, value);
        mConfig.addResource(res, true);
        mResSpec.addResource(res, true);
    }

    @Override
    public String toString() {
        return getFilePath();
    }
}
