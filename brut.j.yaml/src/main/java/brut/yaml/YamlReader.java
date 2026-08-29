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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class YamlReader {
    private final List<YamlLine> mLines;
    private int mCurrent;

    public YamlReader(InputStream in) {
        mLines = new ArrayList<>();
        Scanner scanner = new Scanner(in, StandardCharsets.UTF_8.name());
        while (scanner.hasNextLine()) {
            mLines.add(new YamlLine(scanner.nextLine()));
        }
        mLines.add(new YamlLine(null));
    }

    public void pushLine() {
        if (mCurrent > 0) {
            mCurrent--;
        }
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

    protected interface Parser<T> {
        T parse(YamlLine line);
    }

    protected interface Updater<T> {
        void update(T items, YamlReader reader);
    }

    /**
     * Read root object from start to end.
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
     * Read list. Reader stands on the list name.
     * The list data should be placed on the next line and have indentation.
     * The list data may also be indented at the same level as the name.
     */
    protected <T> void readList(Collection<T> list, Parser<T> parser) {
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
            list.add(parser.parse(line));
            nextLine();
        }
    }

    public void readStringList(Collection<String> list) {
        readList(list, YamlLine::getValue);
    }

    public void readIntList(Collection<Integer> list) {
        readList(list, YamlLine::getValueInt);
    }

    /**
     * Read object. Reader stand on the object name.
     * The object data should be placed on the next line and have indentation.
     */
    protected <T> void readObject(T obj, Updater<T> updater) {
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
        if (objIndent <= prevIndent || !line.hasColon) {
            pushLine();
            return;
        }
        updater.update(obj, this);
        while (nextLine()) {
            if (isEnd()) {
                return;
            }
            line = getLine();
            if (objIndent != line.indent || !line.hasColon) {
                pushLine();
                return;
            }
            updater.update(obj, this);
        }
    }

    public <T extends YamlSerializable> void readObject(T obj) {
        readObject(obj, YamlSerializable::readItem);
    }

    protected <T> void readMap(Map<String, T> map, Parser<T> parser) {
        readObject(map, (items, reader) -> {
            YamlLine line = reader.getLine();
            items.put(line.getKey(), parser.parse(line));
        });
    }

    public void readStringMap(Map<String, String> map) {
        readMap(map, YamlLine::getValue);
    }

    public void readIntMap(Map<String, Integer> map) {
        readMap(map, YamlLine::getValueInt);
    }

    public void readBoolMap(Map<String, Boolean> map) {
        readMap(map, YamlLine::getValueBool);
    }
}
