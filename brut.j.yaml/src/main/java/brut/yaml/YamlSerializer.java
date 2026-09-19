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

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

public class YamlSerializer implements Closeable {
    private final BufferedWriter mWriter;
    private int mDepth;
    private boolean mClosed;

    public YamlSerializer(OutputStream out) {
        mWriter = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
    }

    @Override
    public void close() throws IOException {
        mWriter.close();
        mClosed = true;
    }

    private static String escapeKey(String key) {
        if (key == null) {
            throw new IllegalArgumentException("Key is null.");
        }
        return YamlUtils.escapeString(key.trim());
    }

    private static String escapeValue(String value) {
        return value != null ? YamlUtils.escapeString(value.trim()) : "null";
    }

    private void writeIndent() throws IOException {
        for (int i = 0; i < mDepth; i++) {
            mWriter.write("  ");
        }
    }

    private <T> void writeEntry(String key, T value, Function<T, String> mapper) throws IOException {
        if (mClosed) {
            throw new IllegalStateException();
        }
        writeIndent();
        mWriter.write(escapeKey(key));
        mWriter.write(": ");
        mWriter.write(mapper.apply(value));
        mWriter.newLine();
    }

    public void writeString(String key, String value) throws IOException {
        writeEntry(key, value, val -> escapeValue(val));
    }

    public void writeInt(String key, int value) throws IOException {
        writeEntry(key, value, val -> val.toString());
    }

    public void writeBool(String key, boolean value) throws IOException {
        writeEntry(key, value, val -> val ? "true" : "false");
    }

    private interface Consumer<T> {
        void accept(T obj, YamlSerializer serial) throws IOException;
    }

    private <T> void writeObject(String key, T obj, Consumer<T> consumer) throws IOException {
        if (mClosed) {
            throw new IllegalStateException();
        }
        writeIndent();
        mWriter.write(escapeKey(key));
        mWriter.write(':');
        mWriter.newLine();
        mDepth++;
        consumer.accept(obj, this);
        mDepth--;
    }

    public <T extends YamlSerializable> void writeObject(String key, T obj) throws IOException {
        writeObject(key, obj, YamlSerializable::serialize);
    }

    private <T> void writeMap(String key, Map<String, T> map, Function<T, String> mapper) throws IOException {
        writeObject(key, map, (obj, serial) -> {
            for (Map.Entry<String, T> entry : obj.entrySet()) {
                serial.writeEntry(entry.getKey(), entry.getValue(), mapper);
            }
        });
    }

    public void writeStringMap(String key, Map<String, String> map) throws IOException {
        writeMap(key, map, val -> escapeValue(val));
    }

    public void writeIntMap(String key, Map<String, Integer> map) throws IOException {
        writeMap(key, map, val -> val.toString());
    }

    public void writeBoolMap(String key, Map<String, Boolean> map) throws IOException {
        writeMap(key, map, val -> val ? "true" : "false");
    }

    public <T> void writeSeq(String key, Collection<T> coll, Function<T, String> mapper) throws IOException {
        if (mClosed) {
            throw new IllegalStateException();
        }
        writeIndent();
        mWriter.write(escapeKey(key));
        mWriter.write(':');
        mWriter.newLine();
        for (T item : coll) {
            writeIndent();
            mWriter.write("- ");
            mWriter.write(mapper.apply(item));
            mWriter.newLine();
        }
    }

    public void writeStringSeq(String key, Collection<String> coll) throws IOException {
        writeSeq(key, coll, val -> escapeValue(val));
    }

    public void writeIntSeq(String key, Collection<Integer> coll) throws IOException {
        writeSeq(key, coll, val -> val.toString());
    }
}
