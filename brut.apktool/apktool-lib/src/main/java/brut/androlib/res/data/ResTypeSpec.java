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
import java.util.*;

public final class ResTypeSpec {

    public static final String RES_TYPE_NAME_ARRAY = "array";
    public static final String RES_TYPE_NAME_ATTR = "attr";
    public static final String RES_TYPE_NAME_ATTR_PRIVATE = "^attr-private";
    public static final String RES_TYPE_NAME_PLURALS = "plurals";
    public static final String RES_TYPE_NAME_STRING = "string";
    public static final String RES_TYPE_NAME_STYLES = "style";

    private final String mName;
    private final Map<String, ResResSpec> mResSpecs = new LinkedHashMap<>();

    private final int mId;

    public ResTypeSpec(String name, int id) {
        this.mName = name;
        this.mId = id;
    }

    public String getName() {
        return mName;
    }

    public int getId() {
        return mId;
    }

    public boolean isString() {
        return mName.equalsIgnoreCase(RES_TYPE_NAME_STRING);
    }

    public ResResSpec getResSpec(String name) throws AndrolibException {
        ResResSpec spec = getResSpecUnsafe(name);
        if (spec == null) {
            throw new UndefinedResObjectException(String.format("resource spec: %s/%s", getName(), name));
        }
        return spec;
    }

    public ResResSpec getResSpecUnsafe(String name) {
        return mResSpecs.get(name);
    }

    public void addResSpec(ResResSpec spec) throws AndrolibException {
        if (mResSpecs.put(spec.getName(), spec) != null) {
            throw new AndrolibException(String.format("Multiple res specs: %s/%s", getName(), spec.getName()));
        }
    }

    @Override
    public String toString() {
        return mName;
    }
}
