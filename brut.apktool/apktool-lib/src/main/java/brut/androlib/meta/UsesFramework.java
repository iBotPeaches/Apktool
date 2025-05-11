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

import java.util.ArrayList;
import java.util.List;

public class UsesFramework implements YamlSerializable {
    private final List<Integer> mIds;
    private String mTag;

    public UsesFramework() {
        mIds = new ArrayList<>();
        clear();
    }

    public void clear() {
        mIds.clear();
        mTag = null;
    }

    public boolean isEmpty() {
        return mIds.isEmpty() && mTag == null;
    }

    @Override
    public void readItem(YamlReader reader) {
        YamlLine line = reader.getLine();
        switch (line.getKey()) {
            case "ids": {
                mIds.clear();
                reader.readIntList(mIds);
                break;
            }
            case "tag": {
                mTag = line.getValue();
                break;
            }
        }
    }

    @Override
    public void write(YamlWriter writer) {
        if (!mIds.isEmpty()) {
            writer.writeList("ids", mIds);
        }
        if (mTag != null) {
            writer.writeString("tag", mTag);
        }
    }

    public List<Integer> getIds() {
        return mIds;
    }

    public String getTag() {
        return mTag;
    }

    public void setTag(String tag) {
        mTag = tag;
    }
}
