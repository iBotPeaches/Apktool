/**
 *  Copyright (C) 2017 Ryszard Wiśniewski <brut.alll@gmail.com>
 *  Copyright (C) 2017 Connor Tumbleson <connor.tumbleson@gmail.com>
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
public final class ResTypeSpec {
    private final String mName;
    private final Map<String, ResResSpec> mResSpecs = new LinkedHashMap<String, ResResSpec>();

    private final ResTable mResTable;
    private final ResPackage mPackage;

    private final int mId;
    private final int mEntryCount;

    public ResTypeSpec(String name, ResTable resTable, ResPackage package_, int id, int entryCount) {
        this.mName = name;
        this.mResTable = resTable;
        this.mPackage = package_;
        this.mId = id;
        this.mEntryCount = entryCount;
    }

    public String getName() {
        return mName;
    }

    public String getCleanDirectoryName() {
        for (String type: TYPES) {
            if (mName.startsWith(type)) {
                return type;
            }
        }

        return mName;
    }

    public int getId() {
        return mId;
    }

    public int getEntryCount() {
        return mEntryCount;
    }

    public boolean isString() {
        return mName.equalsIgnoreCase("string");
    }

    public boolean isAndResGuard() {
        // This is will probably easily become a cat race against AndResGuard where they change the naming of their
        // fake types and Apktool will break again. I don't have a fool proof solution since typeIds are not constant
        // so this will work for now.
        return Character.isDigit(mName.charAt(mName.length() - 1));
    }

    public ResTypeSpec findStandardType(HashMap<Integer, ResTypeSpec> resTypeSpecHashMap) {
        for (Map.Entry<Integer, ResTypeSpec> entry : resTypeSpecHashMap.entrySet()) {
            ResTypeSpec resTypeSpec = entry.getValue();

            // If the resTypeSpec is NOT AndResGuard & our current clean name (raw2 => raw) matches the
            // iteration spec, we know its the original of it.
            if (! resTypeSpec.isAndResGuard() && getCleanDirectoryName().equalsIgnoreCase(resTypeSpec.mName)) {
                return resTypeSpec;
            }
        }

        return this;
    }

    public Set<ResResSpec> listResSpecs() {
        return new LinkedHashSet<ResResSpec>(mResSpecs.values());
    }

    public ResResSpec getResSpec(String name) throws AndrolibException {
        ResResSpec spec = getResSpecUnsafe(name);
        if (spec == null) {
            throw new UndefinedResObject(String.format("resource spec: %s/%s", getName(), name));
        }
        return spec;
    }

    public ResResSpec getResSpecUnsafe(String name) {
        return mResSpecs.get(name);
    }

    public void removeResSpec(ResResSpec spec) throws AndrolibException {
        mResSpecs.remove(spec.getName());
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

    private final static String[] TYPES = new String[] {
        "animator", "anim", "color", "drawable", "dimen", "dimen", "layout", "menu", "mipmap", "raw", "values", "xml", "style"
    };
}
