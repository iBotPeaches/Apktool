package brut.androlib.apk;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class YamlWriter implements AutoCloseable {

    private int mIndent = 0;
    private final PrintWriter mWriter;
    private final String QUOTE = "'";

    public YamlWriter(OutputStream out) {
        mWriter = new PrintWriter(new BufferedWriter(
            new OutputStreamWriter(out, StandardCharsets.UTF_8)));
    }

    @Override
    public void close() throws Exception {
        mWriter.close();
    }

    public int getIndent() {
        return mIndent;
    }

    public String getIndentString() {
        // for java 11
        // return " ".repeat(mIndent);
        // for java 8
        return String.join("", Collections.nCopies(mIndent, " "));
    }

    public void nextIndent() {
        mIndent += 2;
    }

    public void prevIndent() {
        if (mIndent != 0)
            mIndent -= 2;
    }

    public void writeIndent() {
        mWriter.print(getIndentString());
    }

    public void writeInt(String key, int value) {
        writeIndent();
        mWriter.println(key + ": " + value);
    }

    public void writeBool(String key, boolean value) {
        writeIndent();
        String val = value ? "true": "false";
        mWriter.println(key + ": " + val);
    }

    public void writeString(String key, String value, boolean quoted) {
        writeIndent();
        if (Objects.isNull(value)) {
            mWriter.println(key + ": null");
        } else {
            value = YamlStringEscapeUtils.escapeString(value);
            if (quoted)
                value = QUOTE + value + QUOTE;
            mWriter.println(YamlStringEscapeUtils.escapeString(key) + ": " + value);
        }
    }

    public void writeString(String key, String value) {
        writeString(key, value, false);
    }

    public <T> void writeList(String key, List<T> list) {
        if (Objects.isNull(list))
            return;
        writeIndent();
        mWriter.println(key + ":");
        for (T item: list) {
            writeIndent();
            mWriter.println("- " +  item);
        }
    }

    public <K, V> void writeCommonMap(String key, Map<K, V> map) {
        if (Objects.isNull(map))
            return;
        writeIndent();
        mWriter.println(key + ":");
        nextIndent();
        for (K mapKey: map.keySet()) {
            writeIndent();
            mWriter.println(mapKey + ": " +  map.get(mapKey));
        }
        prevIndent();
    }

    public void writeStringMap(String key, Map<String, String> map) {
        if (Objects.isNull(map))
            return;
        writeIndent();
        mWriter.println(key + ":");
        nextIndent();
        for (String mapKey: map.keySet()) {
            writeString(mapKey, map.get(mapKey));
//            writeIndent();
//            String val = map.get(mapKey);
//            mapKey = YamlStringEscapeUtils.escapeString(mapKey);
//            val = YamlStringEscapeUtils.escapeString(val);
//            mWriter.println(mapKey + ": " +  val);
        }
        prevIndent();
    }

    public <T extends YamlSerializable> void writeObject(String key, T obj) {
        if (Objects.isNull(obj))
            return;
        writeIndent();
        mWriter.println(key + ":");
        nextIndent();
        obj.write(this);
        prevIndent();
    }
}
