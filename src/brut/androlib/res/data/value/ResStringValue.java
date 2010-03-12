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

import brut.androlib.res.AndrolibResources;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public class ResStringValue extends ResScalarValue
        implements ResXmlSerializable {
    private final String mValue;

    public ResStringValue(String value) {
        super("string");
        this.mValue = value;
    }

    public String getValue() {
        return mValue;
    }

    @Override
    public String toResXmlFormat() {
        if (mValue.isEmpty()) {
            return "";
        }
        return AndrolibResources.escapeForResXml(mValue);
    }
}
