/*
 *  Copyright 2010 Ryszard Wiśniewski <brut.alll@gmail.com>.
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
 *  under the License.
 */

package brut.androlib.res.data.value;

import java.util.Map;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public class ResBagValue extends ResValue {
    protected final ResReferenceValue mParent;
    protected final Map<ResReferenceValue, ResScalarValue> mItems;

    public ResBagValue(ResReferenceValue parent,
            Map<ResReferenceValue, ResScalarValue> items) {
        this.mParent = parent;
        this.mItems = items;
    }

    public ResReferenceValue getParent() {
        return mParent;
    }

    public Map<ResReferenceValue, ResScalarValue> getItems() {
        return mItems;
    }
}
