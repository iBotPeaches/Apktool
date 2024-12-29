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
package brut.androlib.res.data.value;

import brut.androlib.exceptions.AndrolibException;
import brut.androlib.exceptions.UndefinedResObjectException;
import brut.androlib.res.data.ResPackage;
import brut.androlib.res.data.ResResSpec;

public class ResReferenceValue extends ResIntValue {
    private final ResPackage mPackage;
    private final boolean mTheme;

    public ResReferenceValue(ResPackage pkg, int value, String rawValue) {
        this(pkg, value, rawValue, false);
    }

    public ResReferenceValue(ResPackage pkg, int value, String rawValue, boolean theme) {
        super(value, rawValue, "reference");
        mPackage = pkg;
        mTheme = theme;
    }

    @Override
    protected String encodeAsResXml() throws AndrolibException {
        ResResSpec spec = !isNull() ? getReferent() : null;
        if (spec == null) {
            return "@null";
        }

        String prefix = mTheme ? "?" : "@";
        boolean excludeType = mTheme && spec.getType().getName().equals("attr");

        return prefix + spec.getFullName(mPackage, excludeType);
    }

    public ResPackage getPackage() {
        return mPackage;
    }

    public ResResSpec getReferent() throws AndrolibException {
        try {
            return mPackage.getResTable().getResSpec(getValue());
        } catch (UndefinedResObjectException ex) {
            return null;
        }
    }

    public boolean isNull() {
        return mValue == 0;
    }

    public boolean referentIsNull() throws AndrolibException {
        return getReferent() == null;
    }
}
