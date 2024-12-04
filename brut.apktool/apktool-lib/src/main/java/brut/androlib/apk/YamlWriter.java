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

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class YamlWriter implements Closeable {
    private static final String QUOTE = "'";

    private final PrintWriter mWriter;
    private int mIndent;

    public YamlWriter(OutputStream out) {
        mWriter = new PrintWriter(new BufferedWriter(
            new OutputStreamWriter(out, StandardCharsets.UTF_8)));
    }

    @Override
    public void close() throws IOException {
        mWriter.close();
    }

    public String getIndentString() {
        // for java 11
        // return " ".repeat(mIndent);
        // for java 8
        return String.join("", Collections.nCopies(mIndent, " "));
    }

    public static String escape(String value) {
        return YamlStringEscapeUtils.escapeString(value);
    }

    public void nextIndent() {
        mIndent += 2;
    }

    public void prevIndent() {
        if (mIndent != 0) {
            mIndent -= 2;
        }
    }

    public void writeIndent() {
        mWriter.print(getIndentString());
    }

    public void writeBool(String key, boolean value) {
        writeIndent();
        String val = value ? "true": "false";
        mWriter.println(escape(key) + ": " + val);
    }

    public void writeString(String key, String value, boolean quoted) {
        writeIndent();
        if (Objects.isNull(value)) {
            mWriter.println(escape(key) + ": null");
        } else {
            if (quoted) {
                value = QUOTE + value + QUOTE;
            }
            mWriter.println(escape(key) + ": " + escape(value));
        }
    }

    public void writeString(String key, String value) {
        writeString(key, value, false);
    }

    public <T> void writeList(String key, List<T> list) {
        if (Objects.isNull(list)) {
            return;
        }
        writeIndent();
        mWriter.println(escape(key) + ":");
        for (T item : list) {
            writeIndent();
            mWriter.println("- " + item);
        }
    }

    public <T> void writeMap(String key, Map<String, T> map) {
        if (Objects.isNull(map)) {
            return;
        }
        writeIndent();
        mWriter.println(escape(key) + ":");
        nextIndent();
        for (String mapKey : map.keySet()) {
            writeString(mapKey, String.valueOf(map.get(mapKey)));
        }
        prevIndent();
    }

    public <T extends YamlSerializable> void writeObject(String key, T obj) {
        if (Objects.isNull(obj)) {
            return;
        }
        writeIndent();
        mWriter.println(escape(key) + ":");
        nextIndent();
        obj.write(this);
        prevIndent();
    }
}
