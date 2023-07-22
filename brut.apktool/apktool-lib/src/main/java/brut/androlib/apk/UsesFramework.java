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
package brut.androlib.apk;

import brut.androlib.exceptions.AndrolibException;

import java.util.ArrayList;
import java.util.List;

public class UsesFramework implements YamlSerializable {
    public List<Integer> ids;
    public String tag;

    @Override
    public void readItem(YamlReader reader) throws AndrolibException {
        YamlLine line = reader.getLine();
        switch (line.getKey()) {
            case "ids": {
                ids = new ArrayList<>();
                reader.readIntList(ids);
                break;
            }
            case "tag": {
                tag = line.getValue();
                break;
            }
        }
    }

    @Override
    public void write(YamlWriter writer) {
        writer.writeList("ids", ids);
        writer.writeString("tag", tag);
    }
}
