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

import brut.androlib.AndrolibException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public abstract class ResMapAttr extends ResAttr {
    private Map<Integer, String> mMap;

    public ResMapAttr(ResReferenceValue parent,
            Map<ResReferenceValue, ResScalarValue> items, int type) {
        super(parent, items, type);
    }

    protected Map<Integer, String> getItemsMap() throws AndrolibException {
        if (mMap == null) {
            loadItemsMap();
        }
        return mMap;
    }

    private void loadItemsMap() throws AndrolibException {
        mMap = new LinkedHashMap<Integer, String>();
        Iterator<Entry<ResReferenceValue, ResScalarValue>> it =
            mItems.entrySet().iterator();
        it.next();

        while (it.hasNext()) {
            Entry<ResReferenceValue, ResScalarValue> entry = it.next();
            // TODO
            if (entry.getKey().getValue() < 0x01010000) {
                continue;
            }
            mMap.put(
                ((ResIntValue) entry.getValue()).getValue(),
                entry.getKey().getReferent().getName());
        }
    }
}
