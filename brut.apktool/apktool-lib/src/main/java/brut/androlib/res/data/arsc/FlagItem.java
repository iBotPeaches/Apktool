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
package brut.androlib.res.data.arsc;

import brut.androlib.exceptions.AndrolibException;
import brut.androlib.res.data.value.ResReferenceValue;

public class FlagItem {
    public final ResReferenceValue ref;
    public final int flag;
    public String value;

    public FlagItem(ResReferenceValue ref, int flag) {
        this.ref = ref;
        this.flag = flag;
    }

    public String getValue() throws AndrolibException {
        if (value == null) {
            if (ref.referentIsNull()) {
                return String.format("APKTOOL_MISSING_0x%08x", ref.getRawIntValue());
            }
            value = ref.getReferent().getName();
        }
        return value;
    }
}
