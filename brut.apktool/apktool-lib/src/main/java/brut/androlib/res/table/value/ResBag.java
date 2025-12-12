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
package brut.androlib.res.table.value;

import brut.androlib.exceptions.AndrolibException;

import java.util.logging.Logger;

public abstract class ResBag extends ResValue {
    private static final Logger LOGGER = Logger.getLogger(ResBag.class.getName());

    protected final ResReference mParent;

    protected ResBag(ResReference parent) {
        mParent = parent;
    }

    public static ResBag parse(String typeName, ResReference parent, RawItem[] rawItems) {
        switch (typeName) {
            case "attr":
            case "^attr-private":
                return ResAttribute.parse(parent, rawItems);
            case "array":
                return ResArray.parse(parent, rawItems);
            case "plurals":
                return ResPlural.parse(parent, rawItems);
            case "style":
                return ResStyle.parse(parent, rawItems);
            default:
                LOGGER.warning("Unsupported type for bags: " + typeName);
                return null;
        }
    }

    public static class RawItem {
        private final int mKey;
        private final ResItem mValue;

        public RawItem(int key, ResItem value) {
            mKey = key;
            mValue = value;
        }

        public int getKey() {
            return mKey;
        }

        public ResItem getValue() {
            return mValue;
        }
    }

    public void resolveKeys() throws AndrolibException {
        // Stub for bags with resolvable keys.
    }
}
