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

package brut.androlib.res.jni;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public class JniEntry {
    public final int resID;
    public final String name;
    public final String type;

    public final int valueType;

    public final boolean boolVal;
    public final int intVal;
    public final float floatVal;
    public final String strVal;

    public final int bagParent;
    public final JniBagItem[] bagItems;

    public JniEntry(int resID, String name, String type, int valueType,
            boolean boolVal, int intVal, float floatVal, String strVal,
            int bagParent, JniBagItem[] bagItems) {
        this.resID = resID;
        this.name = name;
        this.type = type;
        this.valueType = valueType;
        this.boolVal = boolVal;
        this.intVal = intVal;
        this.floatVal = floatVal;
        this.strVal = strVal;
        this.bagParent = bagParent;
        this.bagItems = bagItems;
    }
}
