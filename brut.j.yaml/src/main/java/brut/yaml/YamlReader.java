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
package brut.yaml;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class YamlReader {
    private List<YamlLine> mLines;
    private int mCurrent;

    public YamlReader(InputStream in) {
        mLines = new ArrayList<>();
        mLines.add(new YamlLine(null));
        read(in);
    }

    public void pushLine() {
        if (mCurrent > 0) {
            mCurrent--;
        }
    }

    public void read(InputStream in) {
        Scanner scanner = new Scanner(in);
        mLines = new ArrayList<>();
        while (scanner.hasNextLine()) {
            mLines.add(new YamlLine(scanner.nextLine()));
        }
        mLines.add(new YamlLine(null));
    }

    public YamlLine getLine() {
        return mLines.get(mCurrent);
    }

    public int getIndent() {
        return getLine().indent;
    }

    public boolean isEnd() {
        return getLine().isNull;
    }

    public boolean isCommentOrEmpty() {
        YamlLine line = getLine();
        return line.isEmpty || line.isComment;
    }

    public void skipInsignificant() {
        if (isEnd()) {
            return;
        }
        while (isCommentOrEmpty()) {
            mCurrent++;
            if (isEnd()) {
                break;
            }
        }
    }

    public boolean nextLine() {
        if (isEnd()) {
            return false;
        }
        for (;;) {
            mCurrent++;
            if (isCommentOrEmpty()) {
                continue;
            }
            return !isEnd();
        }
    }

    protected interface Checker {
        boolean check(YamlLine line);
    }

    protected interface Updater<T> {
        void update(T items, YamlReader reader);
    }

    /**
    * Read root object from start to end
    */
    public <T extends YamlSerializable> void readRoot(T obj) {
        if (isEnd()) {
            return;
        }
        int objIndent = 0;
        skipInsignificant();
        for (;;) {
            if (isEnd()) {
                return;
            }
            YamlLine line = getLine();
            // skip don't checked line or lines with other indent
            if (objIndent != line.indent || !line.hasColon) {
                nextLine();
                continue;
            }
            obj.readItem(this);
            nextLine();
        }
    }

    /**
     * Read object. Reader stand on the object name.
     * The object data should be placed on the next line
     * and have indent.
     */
    protected <T> void readObject(T obj, Checker check, Updater<T> updater) {
        if (isEnd()) {
            return;
        }
        int prevIndent = getIndent();
        // detect indent for the object data
        nextLine();
        YamlLine line = getLine();
        int objIndent = line.indent;
        // object data must have indent
        // otherwise stop reading
        if (objIndent <= prevIndent || !check.check(line)) {
            pushLine();
            return;
        }
        updater.update(obj, this);
        while (nextLine()) {
            if (isEnd()) {
                return;
            }
            line = getLine();
            if (objIndent != line.indent || !check.check(line)) {
                pushLine();
                return;
            }
            updater.update(obj, this);
        }
    }

    public <T extends YamlSerializable> void readObject(T obj) {
        readObject(obj, line -> line.hasColon, YamlSerializable::readItem);
    }

    /**
     * Read list. Reader stand on the object name.
     * The list data should be placed on the next line.
     * Data should have same indent. May by same with name.
     */
    protected <T> void readList(List<T> list, Updater<List<T>> updater) {
        if (isEnd()) {
            return;
        }
        int listIndent = getIndent();
        nextLine();
        int dataIndent = getIndent();
        for (;;) {
            if (isEnd()) {
                return;
            }
            // check incorrect data indent
            if (dataIndent < listIndent) {
                pushLine();
                return;
            }
            YamlLine line = getLine();
            if (dataIndent != line.indent || !line.isItem) {
                pushLine();
                return;
            }
            updater.update(list, this);
            nextLine();
        }
    }

    public void readStringList(List<String> list) {
        readList(list, (items, reader) -> items.add(reader.getLine().getValue()));
    }

    public void readIntList(List<Integer> list) {
        readList(list, (items, reader) -> items.add(reader.getLine().getValueInt()));
    }

    public void readStringMap(Map<String, String> map) {
        readObject(map, line -> line.hasColon,
            (items, reader) -> {
                YamlLine line = reader.getLine();
                items.put(line.getKey(), line.getValue());
            });
    }

    public void readBoolMap(Map<String, Boolean> map) {
        readObject(map, line -> line.hasColon,
            (items, reader) -> {
                YamlLine line = reader.getLine();
                items.put(line.getKey(), line.getValueBool());
            });
    }
}
