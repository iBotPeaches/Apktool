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
package brut.androlib.meta;

import brut.yaml.*;

public class VersionInfo implements YamlSerializable {
    private String mVersionCode;
    private String mVersionName;

    public VersionInfo() {
        clear();
    }

    public void clear() {
        mVersionCode = null;
        mVersionName = null;
    }

    public boolean isEmpty() {
        return mVersionCode == null && mVersionName == null;
    }

    @Override
    public void readItem(YamlReader reader) {
        YamlLine line = reader.getLine();
        switch (line.getKey()) {
            case "versionCode": {
                mVersionCode = line.getValue();
                break;
            }
            case "versionName": {
                mVersionName = line.getValue();
                break;
            }
        }
    }

    @Override
    public void write(YamlWriter writer) {
        if (mVersionCode != null) {
            writer.writeString("versionCode", mVersionCode);
        }
        if (mVersionName != null) {
            writer.writeString("versionName", mVersionName);
        }
    }

    public String getVersionCode() {
        return mVersionCode;
    }

    public void setVersionCode(String versionCode) {
        mVersionCode = versionCode;
    }

    public String getVersionName() {
        return mVersionName;
    }

    public void setVersionName(String versionName) {
        mVersionName = versionName;
    }
}
